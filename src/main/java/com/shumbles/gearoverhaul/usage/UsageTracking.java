package com.shumbles.gearoverhaul.usage;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.usage.UsageGate.Kind;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Feeds gameplay events into the Level-10 {@link UsageGate}. All server-side. Per-tick
 * armor progress (helmet submersion, leggings distance) is sampled every tick but written
 * to the stack only once a second, to avoid an inventory-sync packet every tick.
 */
public final class UsageTracking {
	private static final Map<UUID, Vec3d> lastPos = new HashMap<>();
	private static final Map<UUID, Double> distanceAccum = new HashMap<>();
	private static final Map<UUID, Integer> submergedAccum = new HashMap<>();
	private static int tickCounter;

	private UsageTracking() {
	}

	public static void register() {
		registerBlockBreak();
		registerDeaths();
		registerDamage();
		registerTick();
		Heirloom.LOGGER.info("[Usage] Level-10 milestone tracking registered.");
	}

	// ---- mining: axe / pickaxe / shovel -------------------------------------

	private static void registerBlockBreak() {
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world.isClient() || !(player instanceof ServerPlayerEntity sp)) {
				return;
			}
			ItemStack tool = sp.getMainHandStack();
			Kind kind = UsageGate.kindOf(tool);
			if (kind == null || !UsageGate.tracks(tool, kind)) {
				return;
			}
			switch (kind) {
				case AXE -> {
					if (UsageGate.isLog(state)) {
						UsageGate.add(tool, 1);
					}
				}
				case SHOVEL -> {
					if (UsageGate.isEarth(state)) {
						UsageGate.add(tool, 1);
					}
				}
				case PICKAXE -> {
					if (UsageGate.isCoalOrCopperOre(state)) {
						UsageGate.reset(tool); // streak broken
					} else if (UsageGate.isQualifyingOre(state)) {
						UsageGate.add(tool, 1);
					}
				}
				default -> {
				}
			}
		});
	}

	// ---- kills: sword / bow / trident, + chestplate death reset -------------

	private static void registerDeaths() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity victim) {
				ItemStack chest = victim.getEquippedStack(EquipmentSlot.CHEST);
				if (UsageGate.tracks(chest, Kind.CHESTPLATE)) {
					UsageGate.reset(chest); // dying resets absorbed-damage progress
				}
				return;
			}

			Entity src = source.getSource();
			if (src instanceof TridentEntity trident) {
				ItemStack stack = trident.getWeaponStack();
				if (UsageGate.tracks(stack, Kind.TRIDENT)) {
					UsageGate.add(stack, 1); // thrown-trident kill (any range)
				}
				return;
			}
			if (!(source.getAttacker() instanceof ServerPlayerEntity p)) {
				return;
			}
			if (src instanceof PersistentProjectileEntity) {
				if (p.squaredDistanceTo(entity) > 100.0) { // > 10 blocks
					ItemStack bow = heldOfKind(p, Kind.BOW);
					if (!bow.isEmpty()) {
						UsageGate.add(bow, 1);
					}
				}
			} else if (src == p) { // direct melee
				ItemStack sword = heldMainOfKind(p, Kind.SWORD);
				if (!sword.isEmpty()) {
					UsageGate.add(sword, 1);
				}
			}
		});
	}

	// ---- damage: sword reset / chest absorb / boots fall / crossbow / mace ---

	private static void registerDamage() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, taken, blocked) -> {
			if (taken <= 0.0f) {
				return;
			}
			if (entity instanceof ServerPlayerEntity victim) {
				ItemStack sword = heldMainOfKind(victim, Kind.SWORD);
				if (!sword.isEmpty()) {
					UsageGate.reset(sword); // streak broken by taking damage
				}
				ItemStack chest = victim.getEquippedStack(EquipmentSlot.CHEST);
				if (UsageGate.tracks(chest, Kind.CHESTPLATE)) {
					UsageGate.add(chest, Math.round(taken));
				}
				if (source.isOf(DamageTypes.FALL) && victim.getHealth() > 0.0f && victim.getHealth() <= 1.0f) {
					ItemStack boots = victim.getEquippedStack(EquipmentSlot.FEET);
					if (UsageGate.tracks(boots, Kind.BOOTS)) {
						UsageGate.complete(boots);
					}
				}
				return;
			}

			if (!(source.getAttacker() instanceof ServerPlayerEntity p)) {
				return;
			}
			Entity src = source.getSource();
			if (src instanceof PersistentProjectileEntity && !(src instanceof TridentEntity)) {
				ItemStack crossbow = heldOfKind(p, Kind.CROSSBOW);
				if (!crossbow.isEmpty()) {
					UsageGate.add(crossbow, 1); // bolt hit, any range
				}
			} else if (src == p && p.fallDistance > 0.0) { // melee while falling
				ItemStack mace = heldMainOfKind(p, Kind.MACE);
				if (!mace.isEmpty()) {
					UsageGate.add(mace, 1);
				}
			}
		});
	}

	// ---- per-tick: helmet submersion + leggings distance --------------------

	private static void registerTick() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter++;
			boolean flush = tickCounter % 20 == 0; // write to items once a second
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				UUID id = p.getUuid();

				// Sample submersion.
				ItemStack helmet = p.getEquippedStack(EquipmentSlot.HEAD);
				if (UsageGate.tracks(helmet, Kind.HELMET) && p.isSubmergedInWater()) {
					submergedAccum.merge(id, 1, Integer::sum);
				}

				// Sample horizontal distance on foot.
				Vec3d cur = new Vec3d(p.getX(), p.getY(), p.getZ());
				Vec3d prev = lastPos.put(id, cur);
				if (prev != null && !p.hasVehicle()) {
					double dx = cur.x - prev.x;
					double dz = cur.z - prev.z;
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > 0.0 && d < 8.0) { // ignore teleports
						distanceAccum.merge(id, d, Double::sum);
					}
				}

				if (flush) {
					int subs = submergedAccum.getOrDefault(id, 0);
					if (subs > 0 && UsageGate.tracks(helmet, Kind.HELMET)) {
						UsageGate.add(helmet, subs);
					}
					submergedAccum.remove(id);

					double acc = distanceAccum.getOrDefault(id, 0.0);
					int whole = (int) Math.floor(acc);
					ItemStack legs = p.getEquippedStack(EquipmentSlot.LEGS);
					if (whole > 0 && UsageGate.tracks(legs, Kind.LEGGINGS)) {
						UsageGate.add(legs, whole);
					}
					distanceAccum.put(id, acc - whole);
				}
			}
		});
	}

	// ---- helpers ------------------------------------------------------------

	private static ItemStack heldMainOfKind(PlayerEntity p, Kind kind) {
		ItemStack main = p.getMainHandStack();
		return UsageGate.tracks(main, kind) ? main : ItemStack.EMPTY;
	}

	private static ItemStack heldOfKind(PlayerEntity p, Kind kind) {
		ItemStack main = p.getMainHandStack();
		if (UsageGate.tracks(main, kind)) {
			return main;
		}
		ItemStack off = p.getOffHandStack();
		return UsageGate.tracks(off, kind) ? off : ItemStack.EMPTY;
	}
}

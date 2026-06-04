package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The mace's Level-20 rite: slam three <b>distinct</b> Striking Plates (tagged, invulnerable
 * armor stands) in a row without touching the ground. A plate is "slammed" by attacking it with a
 * tempered mace while falling — the first slam must come from a long drop (~{@link #FIRST_FALL}
 * blocks); each slam flings you back up to reach the next. Three distinct plates completes the
 * rite (19 → 20); landing on the ground breaks the run.
 *
 * <p>Server-side. Combo progress is held per-player in memory.
 */
public final class MaceRitual {
	public static final String PLATE_TAG = "heirloom_striking_plate";
	public static final int REQUIRED = 3;
	public static final double LAUNCH = 2.7;       // upward velocity per slam (wind-burst-like)
	public static final double FIRST_FALL = 50.0;  // the opening slam must fall this far
	public static final double MIN_FALL = 5.0;     // later slams just need a real fall

	private static final Map<UUID, List<UUID>> combos = new HashMap<>();

	private MaceRitual() {
	}

	public static boolean isEligibleMace(ItemStack stack) {
		return UsageGate.kindOf(stack) == UsageGate.Kind.MACE
			&& Tempering.getLevel(stack) == Rituals.RITUAL_FROM;
	}

	private static boolean isStrikingPlate(Entity entity) {
		return entity instanceof ArmorStandEntity && entity.getCommandTags().contains(PLATE_TAG);
	}

	public static void register() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient() || !(player instanceof ServerPlayerEntity sp) || !isStrikingPlate(entity)) {
				return ActionResult.PASS;
			}
			// Sneak-attack to pick the plate back up.
			if (sp.isSneaking()) {
				entity.discard();
				sp.getInventory().offerOrDrop(new ItemStack(RitualItems.STRIKING_PLATE));
				return ActionResult.FAIL;
			}
			if (isEligibleMace(sp.getMainHandStack())) {
				handleSlam(sp, (ServerWorld) world, entity);
			}
			return ActionResult.FAIL; // the plate never takes damage
		});

		// Touching the ground breaks an in-progress run.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				if (p.isOnGround() && combos.containsKey(p.getUuid())) {
					combos.remove(p.getUuid());
				}
			}
		});

		Heirloom.LOGGER.info("[Ritual] Mace three-plate rite registered.");
	}

	private static void handleSlam(ServerPlayerEntity player, ServerWorld world, Entity plate) {
		List<UUID> combo = combos.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());
		boolean first = combo.isEmpty();
		double needed = first ? FIRST_FALL : MIN_FALL;
		if (player.fallDistance < needed) {
			player.sendMessage(Text.literal(first
				? "The first slam must fall from far above."
				: "Slam from a fall, not a standstill.").formatted(Formatting.GRAY), true);
			return;
		}

		UUID id = plate.getUuid();
		if (combo.contains(id)) {
			combo.clear(); // a repeated plate restarts the run from here
		}
		combo.add(id);
		int n = combo.size();

		world.playSound(null, plate.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0f, 0.5f);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, plate.getX(), plate.getY() + 0.5, plate.getZ(),
			18, 0.3, 0.2, 0.3, 0.05);

		if (n >= REQUIRED) {
			combos.remove(player.getUuid());
			Rituals.finish(player.getMainHandStack(), world,
				new Vec3d(player.getX(), player.getY(), player.getZ()), player);
			return; // rite complete — no launch
		}

		// Wind-burst launch toward the next plate.
		Vec3d v = player.getVelocity();
		player.setVelocity(v.x, LAUNCH, v.z);
		player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
		player.fallDistance = 0.0; // next fall measures from the apex
		player.sendMessage(Text.literal("Plate " + n + " of " + REQUIRED + " — fall and strike the next!")
			.formatted(Formatting.GOLD), true);
	}
}

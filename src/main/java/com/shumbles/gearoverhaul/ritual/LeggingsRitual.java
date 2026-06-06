package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * The leggings' Level-20 rite (freeze &amp; cold-trek). Frozen at a Freeze Machine, the leggings
 * must then carry their wearer across {@link #REQUIRED_CHUNKS} <b>distinct</b> chunks on foot.
 * Taking fall damage shatters the cold and clears the trek; the frozen state lives on the leggings,
 * so removing them merely pauses it. A mild icy overlay marks the wearer while frozen (no damage).
 */
public final class LeggingsRitual {
	public static final int REQUIRED_CHUNKS = 100;
	public static final int FROZEN_TICKS = 60; // mild icy overlay, well below the freeze-damage threshold

	private LeggingsRitual() {
	}

	public static boolean isEligible(ItemStack legs) {
		return UsageGate.kindOf(legs) == UsageGate.Kind.LEGGINGS
			&& Tempering.getLevel(legs) == Rituals.RITUAL_FROM;
	}

	public static boolean isFrozen(ItemStack legs) {
		return legs.contains(RitualComponents.FROZEN_CHUNKS);
	}

	/** Apply the frozen state to a worthy pair of leggings (called by the Freeze Machine). */
	public static void freeze(ServerPlayerEntity player, ItemStack legs) {
		legs.set(RitualComponents.FROZEN_CHUNKS, List.of());
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.PLAYERS, 1.0f, 0.7f);
		world.spawnParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0, player.getZ(),
			30, 0.4, 0.6, 0.4, 0.02);
		player.sendMessage(Text.literal("Frost grips the greaves — now cross a hundred fresh chunks.")
			.formatted(Formatting.AQUA), true);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				tickTrek(player);
			}
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, taken, blocked) -> {
			if (taken <= 0.0f || !source.isOf(DamageTypes.FALL) || !(entity instanceof ServerPlayerEntity player)) {
				return;
			}
			ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
			if (isFrozen(legs)) {
				legs.remove(RitualComponents.FROZEN_CHUNKS);
				player.setFrozenTicks(0);
				player.sendMessage(Text.literal("The cold shatters — the trek must begin anew.")
					.formatted(Formatting.GRAY), true);
			}
		});

		Heirloom.LOGGER.info("[Ritual] Leggings cold-trek registered.");
	}

	private static void tickTrek(ServerPlayerEntity player) {
		ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
		if (!isFrozen(legs) || !isEligible(legs) || player.hasVehicle()) {
			return; // not trekking, or paused while riding
		}

		// Mild icy overlay, kept topped up below the freeze-damage threshold.
		player.setFrozenTicks(FROZEN_TICKS);
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		if (world.getTime() % 6 == 0) {
			world.spawnParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0, player.getZ(),
				2, 0.3, 0.5, 0.3, 0.0);
		}

		long chunk = new ChunkPos(player.getBlockPos()).toLong();
		List<Long> visited = legs.getOrDefault(RitualComponents.FROZEN_CHUNKS, List.of());
		if (visited.contains(chunk)) {
			return;
		}
		List<Long> updated = new ArrayList<>(visited);
		updated.add(chunk);

		if (updated.size() >= REQUIRED_CHUNKS) {
			legs.remove(RitualComponents.FROZEN_CHUNKS);
			player.setFrozenTicks(0);
			Rituals.finish(legs, world, new Vec3d(player.getX(), player.getY(), player.getZ()), player);
		} else {
			legs.set(RitualComponents.FROZEN_CHUNKS, updated);
			player.sendMessage(Text.literal("Frozen trek: " + updated.size() + " / " + REQUIRED_CHUNKS)
				.formatted(Formatting.AQUA), true);
		}
	}
}

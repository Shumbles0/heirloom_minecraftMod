package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The boots' Level-20 rite (heat-bed walk). In boots tempered to 19, walk an unbroken path of
 * Heat-bed blocks — each new block of coals counts; stepping onto any other ground resets the run.
 * Cross {@link #REQUIRED} distinct heat-bed blocks to finish (19 → 20). Server-side; the run is
 * transient (it must be one continuous walk). The heat screen overlay is purely client-side.
 */
public final class BootsRitual {
	public static final int REQUIRED = 20; // distinct heat-bed blocks in one unbroken walk

	private static final Map<UUID, Set<Long>> runs = new HashMap<>();

	private BootsRitual() {
	}

	public static boolean isEligible(ItemStack boots) {
		return UsageGate.kindOf(boots) == UsageGate.Kind.BOOTS
			&& Tempering.getLevel(boots) == Rituals.RITUAL_FROM;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				tickWalk(player);
			}
		});
		Heirloom.LOGGER.info("[Ritual] Boots heat-bed walk registered.");
	}

	private static void tickWalk(ServerPlayerEntity player) {
		ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
		if (!isEligible(boots)) {
			runs.remove(player.getUuid());
			return;
		}
		ServerWorld world = (ServerWorld) player.getEntityWorld();
		BlockPos below = footBlock(player);
		boolean onHeatBed = world.getBlockState(below).isOf(RitualBlocks.HEATBED);

		if (onHeatBed && world.getTime() % 15L == 0L) {
			player.damage(world, world.getDamageSources().hotFloor(), 1.0f); // the coals bite — for effect
		}

		if (onHeatBed) {
			Set<Long> run = runs.computeIfAbsent(player.getUuid(), k -> new HashSet<>());
			if (run.add(below.asLong())) { // a new block of coals
				if (run.size() >= REQUIRED) {
					runs.remove(player.getUuid());
					Rituals.finish(boots, world,
						new Vec3d(player.getX(), player.getY(), player.getZ()), player);
				} else {
					player.sendMessage(Text.literal("Heat-walk: " + run.size() + " / " + REQUIRED)
						.formatted(Formatting.GOLD), true);
				}
			}
		} else if (player.isOnGround() && runs.remove(player.getUuid()) != null) {
			player.sendMessage(Text.literal("You step off the coals — the walk resets.")
				.formatted(Formatting.GRAY), true);
		}
	}

	private static BlockPos footBlock(ServerPlayerEntity player) {
		return BlockPos.ofFloored(player.getX(), player.getBoundingBox().minY - 0.01, player.getZ());
	}
}

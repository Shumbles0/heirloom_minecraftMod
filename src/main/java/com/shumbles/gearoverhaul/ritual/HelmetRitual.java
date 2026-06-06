package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The helmet's Level-20 rite (open-water seal). Set off a Pressure Seal placed in deep, genuinely
 * open water while wearing a helmet tempered to 19, then stay within {@link #NEAR} blocks of it,
 * submerged, for {@link #DURATION}. Depth and open water are checked once at activation (so a dug
 * pocket won't do); surfacing or straying pauses the seal, and leaving entirely breaks it.
 */
public final class HelmetRitual {
	public static final int DEEP_Y = 50;        // the seal must sit below this
	public static final int CLEAR_RADIUS = 8;   // open-water scan radius
	public static final int MAX_SOLID = 150;    // max solid blocks above/around it (else too enclosed)
	public static final double NEAR = 5.0;      // stay this close to keep the seal holding
	public static final double STRAY = 16.0;    // beyond this, the seal breaks
	public static final int DURATION = 300;     // ~15 s submerged

	private static final class Seal {
		ServerWorld world;
		BlockPos pos;
		int timer;
	}

	private static final Map<UUID, Seal> seals = new HashMap<>();

	private HelmetRitual() {
	}

	public static boolean isEligible(ItemStack helmet) {
		return UsageGate.kindOf(helmet) == UsageGate.Kind.HELMET
			&& Tempering.getLevel(helmet) == Rituals.RITUAL_FROM;
	}

	public static boolean isActive(ServerPlayerEntity player) {
		return seals.containsKey(player.getUuid());
	}

	/** Try to begin the seal at {@code sealPos}; messages and returns on any failed condition. */
	public static void activate(ServerPlayerEntity player, ServerWorld world, BlockPos sealPos) {
		if (sealPos.getY() >= DEEP_Y) {
			player.sendMessage(Text.literal("The seal must rest deep beneath the sea.").formatted(Formatting.GRAY), true);
			return;
		}
		if (!world.getFluidState(sealPos.up()).isIn(FluidTags.WATER)) {
			player.sendMessage(Text.literal("The seal must sit in open water.").formatted(Formatting.GRAY), true);
			return;
		}
		if (countSolidAround(world, sealPos) > MAX_SOLID) {
			player.sendMessage(Text.literal("Too closed-in — only truly open water will do.").formatted(Formatting.GRAY), true);
			return;
		}
		if (!player.isSubmergedInWater() || player.squaredDistanceTo(Vec3d.ofCenter(sealPos)) > NEAR * NEAR) {
			player.sendMessage(Text.literal("Stay close and beneath the surface to set it.").formatted(Formatting.GRAY), true);
			return;
		}

		Seal seal = new Seal();
		seal.world = world;
		seal.pos = sealPos;
		seal.timer = DURATION;
		seals.put(player.getUuid(), seal);
		world.playSound(null, sealPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.8f, 0.6f);
		player.sendMessage(Text.literal("The seal takes hold — endure the depths.").formatted(Formatting.AQUA), true);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<Map.Entry<UUID, Seal>> it = seals.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<UUID, Seal> e = it.next();
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
				Seal seal = e.getValue();
				if (player == null) {
					it.remove();
					continue;
				}
				double dist2 = player.squaredDistanceTo(Vec3d.ofCenter(seal.pos));
				boolean sealGone = !seal.world.getBlockState(seal.pos).isOf(RitualBlocks.PRESSURE_SEAL);
				if (player.getEntityWorld() != seal.world || dist2 > STRAY * STRAY || sealGone) {
					it.remove();
					player.sendMessage(Text.literal("The seal is broken — begin again.").formatted(Formatting.GRAY), true);
					continue;
				}

				boolean holding = player.isSubmergedInWater()
					&& dist2 <= NEAR * NEAR
					&& isEligible(player.getEquippedStack(EquipmentSlot.HEAD));
				if (holding) {
					seal.world.spawnParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 1.0, player.getZ(),
						4, 0.3, 0.4, 0.3, 0.0);
					if (--seal.timer <= 0) {
						it.remove();
						Rituals.finish(player.getEquippedStack(EquipmentSlot.HEAD), seal.world,
							Vec3d.ofCenter(seal.pos), player);
					} else if (seal.timer % 20 == 0) {
						player.sendMessage(Text.literal("Sealing… " + (seal.timer / 20 + 1) + "s").formatted(Formatting.AQUA), true);
					}
				} else if (seal.world.getTime() % 20 == 0) {
					player.sendMessage(Text.literal("Hold the seal — stay deep, close, and submerged.")
						.formatted(Formatting.GRAY), true);
				}
			}
		});
		Heirloom.LOGGER.info("[Ritual] Helmet open-water seal registered.");
	}

	/** Counts real (non-air, non-water) blocks in the upper hemisphere around the seal. */
	private static int countSolidAround(ServerWorld world, BlockPos seal) {
		int solids = 0;
		for (int dx = -CLEAR_RADIUS; dx <= CLEAR_RADIUS; dx++) {
			for (int dy = 0; dy <= CLEAR_RADIUS; dy++) {
				for (int dz = -CLEAR_RADIUS; dz <= CLEAR_RADIUS; dz++) {
					if (dx * dx + dy * dy + dz * dz > CLEAR_RADIUS * CLEAR_RADIUS) {
						continue;
					}
					BlockState s = world.getBlockState(seal.add(dx, dy, dz));
					if (!s.isAir() && !s.getFluidState().isIn(FluidTags.WATER)) {
						solids++;
					}
				}
			}
		}
		return solids;
	}
}

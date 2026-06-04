package com.shumbles.gearoverhaul.ritual;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The sword rite's quenching trough. Right-click with a freshly-pulled hot blade: if it was
 * pulled in the cherry band and quenched within the window, the rite completes (19 → 20). Any
 * other case just hisses and clears the heat — start the heat-up again.
 */
public class RitualTroughBlock extends Block {
	public RitualTroughBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		ItemStack held = player.getMainHandStack();
		if (!held.contains(RitualComponents.FORGE_HEAT)) {
			return ActionResult.PASS;
		}

		int heat = held.getOrDefault(RitualComponents.FORGE_HEAT, 0);
		long pullTick = held.getOrDefault(RitualComponents.FORGE_PULL_TICK, 0L);
		held.remove(RitualComponents.FORGE_HEAT);
		held.remove(RitualComponents.FORGE_PULL_TICK);

		ServerWorld sw = (ServerWorld) world;
		Vec3d at = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
		boolean inTime = world.getTime() - pullTick <= SwordRitual.QUENCH_WINDOW;

		if (SwordRitual.isMelting(heat)) {
			// The temper was already lost the moment it was pulled molten; this is just the hiss.
			sw.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 0.6f);
			sw.spawnParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.02);
			player.sendMessage(Text.literal("The molten blade slumps, set wrong — it has lost a temper. Retemper it.")
				.formatted(Formatting.RED), true);
		} else if (inTime && SwordRitual.inWhiteHot(heat) && Rituals.finish(held, sw, at, player)) {
			sw.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.2f);
			sw.spawnParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.02);
		} else {
			sw.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.8f, 0.7f);
			sw.spawnParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 15, 0.3, 0.3, 0.3, 0.01);
			player.sendMessage(Text.literal("The blade hisses, but the rite does not hold. Begin again.")
				.formatted(Formatting.GRAY), true);
		}
		return ActionResult.SUCCESS;
	}
}

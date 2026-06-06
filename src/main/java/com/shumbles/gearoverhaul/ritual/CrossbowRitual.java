package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The crossbow's Level-20 rite (the long shot): strike a Bullseye Target from at least
 * {@link #MIN_DISTANCE} blocks away with a crossbow tempered to 19. Only the restored bolt velocity
 * flies flat and far enough to land it. One true hit finishes the rite. Called from the arrow
 * block-hit mixin.
 */
public final class CrossbowRitual {
	public static final double MIN_DISTANCE = 80.0;

	private CrossbowRitual() {
	}

	public static boolean isEligible(ItemStack stack) {
		return UsageGate.kindOf(stack) == UsageGate.Kind.CROSSBOW
			&& Tempering.getLevel(stack) == Rituals.RITUAL_FROM;
	}

	/** A bolt has struck {@code pos}; if it's the bullseye, judge the shot. */
	public static void tryLongShot(ServerWorld world, PersistentProjectileEntity bolt, BlockPos pos) {
		if (!world.getBlockState(pos).isOf(RitualBlocks.BULLSEYE_TARGET)) {
			return;
		}
		if (!(bolt.getOwner() instanceof PlayerEntity player)) {
			return;
		}
		ItemStack crossbow = heldCrossbow(player);
		if (crossbow.isEmpty()) {
			return;
		}

		Vec3d center = Vec3d.ofCenter(pos);
		double distance = Math.sqrt(player.squaredDistanceTo(center.x, center.y, center.z));

		// Every hit on the mark gets a satisfying thunk + spark, near or far.
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(null, pos, SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.8f, 1.2f);
		world.spawnParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 18, 0.25, 0.25, 0.25, 0.15);
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 12, 0.2, 0.2, 0.2, 0.1);

		if (distance >= MIN_DISTANCE) {
			// Nailed it from afar — the full fanfare, then the rite's ceremony.
			world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.4f);
			world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.2f);
			world.playSound(null, pos, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.0f, 1.0f);
			world.spawnParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z, 80, 0.5, 0.5, 0.5, 0.3);
			world.spawnParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 40, 0.5, 0.5, 0.5, 0.2);
			world.spawnParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 20, 0.3, 0.3, 0.3, 0.05);
			Rituals.finish(crossbow, world, center, player);
		} else {
			world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.7f, 0.7f);
			world.spawnParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 10, 0.2, 0.2, 0.2, 0.01);
			player.sendMessage(Text.literal("Too close — the shot must fly from afar.").formatted(Formatting.GRAY), true);
		}
	}

	private static ItemStack heldCrossbow(PlayerEntity player) {
		if (isEligible(player.getMainHandStack())) {
			return player.getMainHandStack();
		}
		if (isEligible(player.getOffHandStack())) {
			return player.getOffHandStack();
		}
		return ItemStack.EMPTY;
	}
}

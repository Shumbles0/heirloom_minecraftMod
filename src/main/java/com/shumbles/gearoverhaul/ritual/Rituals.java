package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.temper.Tempering;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * The shared completion path for the Level-20 rituals.
 *
 * <p>Per-ritual <i>detection</i> — the forge heat-gauge, the lightning catch, the cold-trek,
 * the three-plate bounce, and so on — lives with each ritual block/condition and is built
 * separately. When a ritual's condition is finally met, that code calls {@link #finish} here:
 * the one place that actually carries a piece across the 19 → 20 gate, fires the ceremony, and
 * tells the player. Botched attempts route through {@link #softFail} — never destroy the gear,
 * only cost it.
 *
 * <p>Server-side only (it spawns particles and plays sounds on a {@link ServerWorld}).
 */
public final class Rituals {
	/** A ritual is performed on a piece sitting at this temper level... */
	public static final int RITUAL_FROM = 19;
	/** ...and carries it to this one, which re-opens the station for t21+. */
	public static final int RITUAL_TO = 20;

	private Rituals() {
	}

	/** True if {@code gear} is eligible for its ritual right now (tempered exactly to {@value #RITUAL_FROM}). */
	public static boolean canPerform(ItemStack gear) {
		return !gear.isEmpty() && Tempering.getLevel(gear) == RITUAL_FROM;
	}

	/**
	 * Completes a ritual: advances the gear {@value #RITUAL_FROM} → {@value #RITUAL_TO} (which
	 * re-bakes its stats and re-opens the station for the t21–25 climb), plays the ceremony at
	 * {@code at}, and notifies {@code player}. No-op if the piece isn't eligible.
	 *
	 * @return {@code true} if the ritual took.
	 */
	public static boolean finish(ItemStack gear, ServerWorld world, Vec3d at, PlayerEntity player) {
		if (!canPerform(gear)) {
			return false;
		}
		Tempering.setLevel(gear, RITUAL_TO);
		playFinishEffects(world, at);
		if (player != null) {
			player.sendMessage(Text.literal("The rite holds. The piece is changed.").formatted(Formatting.GOLD), true);
		}
		return true;
	}

	/**
	 * Soft failure: the rite is botched. The gear is never destroyed — it slips down a single
	 * temper level (the cost of going again). Reagent consumption is the caller's concern.
	 */
	public static void softFail(ItemStack gear, ServerWorld world, Vec3d at, PlayerEntity player) {
		int level = Tempering.getLevel(gear);
		if (level > Tempering.MIN_TEMPER) {
			Tempering.setLevel(gear, level - 1);
		}
		playFailEffects(world, at);
		if (player != null) {
			player.sendMessage(Text.literal("The rite breaks. The work slips, but the piece endures.")
				.formatted(Formatting.RED), true);
		}
	}

	private static void playFinishEffects(ServerWorld world, Vec3d at) {
		world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
		world.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_TOTEM_USE, SoundCategory.BLOCKS, 0.4f, 1.0f);
		world.spawnParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 40, 0.4, 0.6, 0.4, 0.05);
		world.spawnParticles(ParticleTypes.ENCHANT, at.x, at.y + 0.5, at.z, 60, 0.5, 0.8, 0.5, 0.2);
		world.spawnParticles(ParticleTypes.END_ROD, at.x, at.y + 0.3, at.z, 20, 0.3, 0.5, 0.3, 0.05);
	}

	private static void playFailEffects(ServerWorld world, Vec3d at) {
		world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.8f, 0.8f);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.3, at.z, 20, 0.3, 0.4, 0.3, 0.02);
	}
}

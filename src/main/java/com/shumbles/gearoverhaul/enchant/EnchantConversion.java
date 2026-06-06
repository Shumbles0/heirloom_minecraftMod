package com.shumbles.gearoverhaul.enchant;

import com.shumbles.gearoverhaul.Heirloom;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Conversion helpers for the enchanting table → {@link EnchantBlocks#ADVANCED_ENCHANTING_TABLE}. The
 * actual interaction (convert when ringed, or open direction selection) lives in
 * {@code mixin.EnchantingTableBlockMixin}, which calls {@link #ringComplete} and {@link #convert}.
 */
public final class EnchantConversion {
	/** Arcane bookshelves needed in the standard ring (the vanilla 15-shelf max). */
	private static final int REQUIRED_SHELVES = 15;

	private EnchantConversion() {
	}

	public static void register() {
		Heirloom.LOGGER.info("[Enchant] Enchanting-table interaction ready (via mixin).");
	}

	/** True if {@code pos} still has its full ring of Arcane Bookshelves (re-checked each open). */
	public static boolean ringComplete(World world, BlockPos pos) {
		return countArcaneShelves(world, pos) >= REQUIRED_SHELVES;
	}

	/** Arcane bookshelves in the standard ring: the perimeter of the 5x5 around the table, two tall. */
	private static int countArcaneShelves(World world, BlockPos table) {
		int count = 0;
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dy = 0; dy <= 1; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) {
						continue; // only the outer ring (where vanilla bookshelves sit)
					}
					p.set(table.getX() + dx, table.getY() + dy, table.getZ() + dz);
					if (world.getBlockState(p).isOf(EnchantBlocks.ARCANE_BOOKSHELF)) {
						count++;
					}
				}
			}
		}
		return count;
	}

	/** Replace the vanilla table at {@code pos} with the Advanced table, with sound + particles. */
	public static void convert(ServerWorld world, BlockPos pos) {
		if (!world.getBlockState(pos).isOf(Blocks.ENCHANTING_TABLE)) {
			return;
		}
		world.setBlockState(pos, EnchantBlocks.ADVANCED_ENCHANTING_TABLE.getDefaultState());
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 0.8f);
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.4f);
		double cx = pos.getX() + 0.5, cy = pos.getY() + 1.0, cz = pos.getZ() + 0.5;
		world.spawnParticles(ParticleTypes.ENCHANT, cx, cy, cz, 90, 0.7, 0.7, 0.7, 0.7);
		world.spawnParticles(ParticleTypes.PORTAL, cx, cy, cz, 60, 0.5, 0.6, 0.5, 0.4);
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 24, 0.4, 0.4, 0.4, 0.02);
	}
}

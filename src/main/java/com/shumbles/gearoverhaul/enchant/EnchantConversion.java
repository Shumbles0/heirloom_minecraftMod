package com.shumbles.gearoverhaul.enchant;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.screen.DirectionScreenHandler;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Converts a vanilla enchanting table into the {@link EnchantBlocks#ADVANCED_ENCHANTING_TABLE} when
 * it's ringed by enough {@link EnchantBlocks#ARCANE_BOOKSHELF}s. Right-clicking such a table awakens
 * it (with a sound + particle flourish) instead of opening the vanilla enchant screen.
 */
public final class EnchantConversion {
	/** Arcane bookshelves needed in the standard ring (the vanilla 15-shelf max). */
	private static final int REQUIRED_SHELVES = 15;

	private EnchantConversion() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			BlockPos pos = hit.getBlockPos();
			if (!world.getBlockState(pos).isOf(Blocks.ENCHANTING_TABLE)) {
				return ActionResult.PASS;
			}
			if (countArcaneShelves(world, pos) >= REQUIRED_SHELVES) {
				// Ring complete: convert (server-side) and consume the click on both sides.
				if (world instanceof ServerWorld serverWorld) {
					convert(serverWorld, pos);
				}
				return ActionResult.SUCCESS;
			}
			// Ordinary table: holding Heirloom gear opens direction selection instead of vanilla enchanting.
			ItemStack held = player.getMainHandStack();
			if (EnchantDirections.canTakeDirections(held)) {
				if (!EnchantComponents.hasFreeSlot(held)) {
					if (!world.isClient()) {
						player.sendMessage(Text.literal("This piece already holds three directions.")
							.formatted(Formatting.GRAY), true);
					}
					return ActionResult.SUCCESS;
				}
				if (player instanceof ServerPlayerEntity serverPlayer) {
					serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
						(syncId, inv, p) -> new DirectionScreenHandler(syncId, inv),
						Text.literal("Choose a Direction")));
				}
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS; // not Heirloom gear — let the vanilla enchant screen open
		});
		Heirloom.LOGGER.info("[Enchant] Advanced table conversion registered.");
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

	private static void convert(ServerWorld world, BlockPos pos) {
		world.setBlockState(pos, EnchantBlocks.ADVANCED_ENCHANTING_TABLE.getDefaultState());
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 0.8f);
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.4f);
		double cx = pos.getX() + 0.5, cy = pos.getY() + 1.0, cz = pos.getZ() + 0.5;
		world.spawnParticles(ParticleTypes.ENCHANT, cx, cy, cz, 90, 0.7, 0.7, 0.7, 0.7);
		world.spawnParticles(ParticleTypes.PORTAL, cx, cy, cz, 60, 0.5, 0.6, 0.5, 0.4);
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 24, 0.4, 0.4, 0.4, 0.02);
	}
}

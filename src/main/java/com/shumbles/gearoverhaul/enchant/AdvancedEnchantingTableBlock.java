package com.shumbles.gearoverhaul.enchant;

import com.shumbles.gearoverhaul.screen.AdvancedTableScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The Advanced Enchanting Table — the recoloured table produced when a vanilla enchanting table is
 * ringed with Arcane Bookshelves ({@link EnchantConversion}). Same 0.75-high silhouette as the
 * vanilla table. Its brewing GUI (direction → attribute → upgrades) is a later step; for now it
 * just shows a placeholder line.
 */
public class AdvancedEnchantingTableBlock extends Block {
	private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

	public AdvancedEnchantingTableBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS; // server drives the checks + opens the screen
		}
		if (!EnchantConversion.ringComplete(world, pos)) {
			// Re-checked every open: without its 15 Arcane Bookshelves it goes dormant.
			player.sendMessage(Text.literal("The Advanced Enchanting Table lies dormant — its ring of "
				+ "Arcane Bookshelves is broken.").formatted(Formatting.GRAY), true);
			return ActionResult.SUCCESS;
		}
		ItemStack gear = player.getMainHandStack();
		if (!EnchantDirections.canTakeDirections(gear)) {
			player.sendMessage(Text.literal("Hold a piece with directions to shape its attributes.")
				.formatted(Formatting.GRAY), true);
			return ActionResult.SUCCESS;
		}
		if (EnchantComponents.slotCount(gear) == 0) {
			player.sendMessage(Text.literal("This piece has no directions yet — choose some at an enchanting table.")
				.formatted(Formatting.GRAY), true);
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inv, p) -> new AdvancedTableScreenHandler(syncId, inv),
				Text.literal("Advanced Enchanting Table")));
		}
		return ActionResult.SUCCESS;
	}
}

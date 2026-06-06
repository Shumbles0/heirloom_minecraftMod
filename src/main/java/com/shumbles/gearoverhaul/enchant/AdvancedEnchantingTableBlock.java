package com.shumbles.gearoverhaul.enchant;

import com.mojang.serialization.MapCodec;
import com.shumbles.gearoverhaul.block.HeirloomBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Advanced Enchanting Table — a brewing-style station (see {@link AdvancedTableBlockEntity}).
 * Right-click opens its GUI (if the Arcane Bookshelf ring is intact); breaking it scatters its
 * contents so a piece in the slot is never lost.
 */
public class AdvancedEnchantingTableBlock extends BlockWithEntity {
	public static final MapCodec<AdvancedEnchantingTableBlock> CODEC = createCodec(AdvancedEnchantingTableBlock::new);
	private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

	public AdvancedEnchantingTableBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new AdvancedTableBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient() ? null
			: validateTicker(type, HeirloomBlockEntities.ADVANCED_TABLE, AdvancedTableBlockEntity::tick);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!EnchantConversion.ringComplete(world, pos)) {
			player.sendMessage(Text.literal("The Advanced Enchanting Table lies dormant — its ring of "
				+ "Arcane Bookshelves is broken.").formatted(Formatting.GRAY), true);
			return ActionResult.SUCCESS;
		}
		if (world.getBlockEntity(pos) instanceof NamedScreenHandlerFactory factory) {
			player.openHandledScreen(factory);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		// The Advanced table has no blockstates, so this only fires on removal — scatter its contents.
		if (world.getBlockEntity(pos) instanceof Inventory inv) {
			ItemScatterer.spawn(world, pos, inv);
		}
		super.onStateReplaced(state, world, pos, moved);
	}
}

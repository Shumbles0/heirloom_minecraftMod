package com.shumbles.gearoverhaul.ritual;

import com.mojang.serialization.MapCodec;
import com.shumbles.gearoverhaul.block.HeirloomBlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Skeet Launcher: right-click to open its loading GUI; supply redstone power to make it fire
 * clay pigeons. Hosts the {@link SkeetLauncherBlockEntity} and ticks it.
 */
public class SkeetLauncherBlock extends BlockWithEntity {
	public static final MapCodec<SkeetLauncherBlock> CODEC = createCodec(SkeetLauncherBlock::new);

	public SkeetLauncherBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new SkeetLauncherBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient() ? null
			: validateTicker(type, HeirloomBlockEntities.SKEET_LAUNCHER, SkeetLauncherBlockEntity::tick);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient() && world.getBlockEntity(pos) instanceof NamedScreenHandlerFactory factory) {
			player.openHandledScreen(factory);
		}
		return ActionResult.SUCCESS;
	}
}

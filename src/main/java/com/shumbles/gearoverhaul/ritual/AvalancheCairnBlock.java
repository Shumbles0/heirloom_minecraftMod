package com.shumbles.gearoverhaul.ritual;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Setting this off with a shovel tempered to 19 brings the earth down on the user — the start of
 * the avalanche-escape rite. The collapse + dig-out clock live in {@link ShovelRitual}.
 */
public class AvalancheCairnBlock extends Block {
	public AvalancheCairnBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayerEntity sp)) {
			return ActionResult.PASS;
		}
		if (!ShovelRitual.isEligible(sp.getMainHandStack())) {
			sp.sendMessage(Text.literal("Set this off with a shovel tempered to 19.").formatted(Formatting.GRAY), true);
			return ActionResult.SUCCESS;
		}
		if (ShovelRitual.isActive(sp)) {
			return ActionResult.SUCCESS; // already buried
		}
		ShovelRitual.trigger(sp, (ServerWorld) world);
		return ActionResult.SUCCESS;
	}
}

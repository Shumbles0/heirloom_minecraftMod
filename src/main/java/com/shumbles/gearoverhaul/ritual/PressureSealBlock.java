package com.shumbles.gearoverhaul.ritual;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
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
 * Setting this off underwater with a helmet tempered to 19 begins the open-water seal rite. The
 * depth/open-water checks and the endurance clock live in {@link HelmetRitual}.
 */
public class PressureSealBlock extends Block {
	public PressureSealBlock(Settings settings) {
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
		if (!HelmetRitual.isEligible(sp.getEquippedStack(EquipmentSlot.HEAD))) {
			sp.sendMessage(Text.literal("Wear a helmet tempered to 19 to set the seal.").formatted(Formatting.GRAY), true);
			return ActionResult.SUCCESS;
		}
		if (HelmetRitual.isActive(sp)) {
			return ActionResult.SUCCESS;
		}
		HelmetRitual.activate(sp, (ServerWorld) world, pos);
		return ActionResult.SUCCESS;
	}
}

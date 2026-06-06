package com.shumbles.gearoverhaul.ritual;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Freezes a worn pair of leggings tempered to 19 for the cold-trek rite. Right-click while wearing
 * them; the rest of the rite (the 100-chunk trek) is driven by {@link LeggingsRitual}.
 */
public class FreezeMachineBlock extends Block {
	public FreezeMachineBlock(Settings settings) {
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
		ItemStack legs = sp.getEquippedStack(EquipmentSlot.LEGS);
		if (!LeggingsRitual.isEligible(legs)) {
			sp.sendMessage(Text.literal("Wear leggings tempered to 19 to brave the freeze.")
				.formatted(Formatting.GRAY), true);
			return ActionResult.SUCCESS;
		}
		LeggingsRitual.freeze(sp, legs);
		return ActionResult.SUCCESS;
	}
}

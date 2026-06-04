package com.shumbles.gearoverhaul.ritual;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * The chestplate's Level-20 charge. Set off in hand while wearing a chestplate tempered to 19, it
 * lights a five-second fuse (quickening beeps) and then detonates — throwing you to half a heart,
 * regardless of armour, but never to death. The fuse + blast live in {@link ChargeHandler}.
 */
public class SuicideChargeItem extends Item {
	public SuicideChargeItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (world.isClient()) {
			return ActionResult.SUCCESS;
		}
		if (!(user instanceof ServerPlayerEntity player)) {
			return ActionResult.PASS;
		}
		ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
		if (!ChargeHandler.isEligible(chest)) {
			player.sendMessage(Text.literal("Only a chestplate tempered to 19 dares this charge.")
				.formatted(Formatting.GRAY), true);
			return ActionResult.FAIL;
		}

		ChargeHandler.arm(player);
		player.sendMessage(Text.literal("The charge is lit — brace yourself.").formatted(Formatting.RED), true);
		player.getStackInHand(hand).decrement(1);
		return ActionResult.SUCCESS;
	}
}

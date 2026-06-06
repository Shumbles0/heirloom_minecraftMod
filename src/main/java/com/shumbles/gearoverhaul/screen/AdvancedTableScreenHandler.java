package com.shumbles.gearoverhaul.screen;

import com.shumbles.gearoverhaul.enchant.EnchantComponents;
import com.shumbles.gearoverhaul.enchant.EnchantDirections;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

/**
 * Menu for the Advanced Enchanting Table — selecting an <b>attribute</b> for each of the held gear's
 * direction slots. No slots (the gear stays in the main hand). Button id encodes the target slot and
 * the attribute index: {@code slot * 16 + attributeIndex}. Attribute <i>effects</i> aren't applied
 * yet; this only records the choice on the gear.
 */
public class AdvancedTableScreenHandler extends ScreenHandler {
	private final PlayerInventory playerInventory;

	public AdvancedTableScreenHandler(int syncId, PlayerInventory playerInventory) {
		super(HeirloomScreenHandlers.ADVANCED_TABLE, syncId);
		this.playerInventory = playerInventory;
	}

	/** The gear being worked — the piece in the main hand. */
	public ItemStack gear() {
		return playerInventory.player.getMainHandStack();
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		ItemStack gear = gear();
		return EnchantDirections.canTakeDirections(gear) && EnchantComponents.slotCount(gear) > 0;
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		int slot = id / 16;
		int attrIndex = id % 16;
		ItemStack gear = gear();
		if (slot < 0 || slot >= EnchantComponents.slotCount(gear)) {
			return false;
		}
		List<String> attrs = EnchantDirections.attributesOf(EnchantComponents.directionAt(gear, slot));
		if (attrIndex < 0 || attrIndex >= attrs.size()) {
			return false;
		}
		if (!EnchantComponents.setAttribute(gear, slot, attrs.get(attrIndex))) {
			return false;
		}
		player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
			SoundCategory.BLOCKS, 0.7f, 1.2f);
		sendContentUpdates();
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}
}

package com.shumbles.gearoverhaul.screen;

import com.shumbles.gearoverhaul.enchant.EnchantComponents;
import com.shumbles.gearoverhaul.enchant.EnchantDirections;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

/**
 * Menu for choosing a direction at an ordinary enchanting table. No slots — the gear stays in the
 * player's main hand; three buttons offer directions, and picking one writes it to the gear and
 * spends {@link #LAPIS_COST} lapis + {@link #XP_COST} levels. Both sides derive the same offer from
 * the held stack + player, so no custom packet is needed.
 */
public class DirectionScreenHandler extends ScreenHandler {
	public static final int XP_COST = 2;
	public static final int LAPIS_COST = 1;

	private final PlayerInventory playerInventory;

	public DirectionScreenHandler(int syncId, PlayerInventory playerInventory) {
		super(HeirloomScreenHandlers.DIRECTION, syncId);
		this.playerInventory = playerInventory;
	}

	/** The gear being imbued — the piece in the main hand. */
	public ItemStack gear() {
		return playerInventory.player.getMainHandStack();
	}

	/** The up-to-three directions currently offered for the held gear. */
	public List<String> offered() {
		return EnchantDirections.offer(gear(), playerInventory.player);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return EnchantDirections.eligible(gear());
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		List<String> offered = offered();
		if (id < 0 || id >= offered.size()) {
			return false;
		}
		ItemStack gear = gear();
		if (!EnchantComponents.hasFreeSlot(gear)) {
			return false;
		}
		boolean creative = player.getAbilities().creativeMode;
		if (!creative && (player.experienceLevel < XP_COST || countLapis(player) < LAPIS_COST)) {
			return false;
		}
		if (!EnchantComponents.addDirection(gear, offered.get(id))) {
			return false;
		}
		if (!creative) {
			player.addExperienceLevels(-XP_COST);
			consumeLapis(player);
		}
		player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
			SoundCategory.BLOCKS, 0.8f, 1.0f);
		sendContentUpdates();
		return true;
	}

	private int countLapis(PlayerEntity player) {
		int n = 0;
		for (int i = 0; i < player.getInventory().size(); i++) {
			if (player.getInventory().getStack(i).isOf(Items.LAPIS_LAZULI)) {
				n += player.getInventory().getStack(i).getCount();
			}
		}
		return n;
	}

	private void consumeLapis(PlayerEntity player) {
		int remaining = LAPIS_COST;
		for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
			ItemStack s = player.getInventory().getStack(i);
			if (s.isOf(Items.LAPIS_LAZULI)) {
				int take = Math.min(remaining, s.getCount());
				s.decrement(take);
				remaining -= take;
			}
		}
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}
}

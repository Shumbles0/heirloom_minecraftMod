package com.shumbles.gearoverhaul.screen;

import com.shumbles.gearoverhaul.ritual.RitualItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/** Loading menu for the Skeet Launcher: nine slots that only accept clay pigeons. */
public class SkeetLauncherScreenHandler extends ScreenHandler {
	public static final int SIZE = 9;

	private final Inventory inventory;

	public SkeetLauncherScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(SIZE));
	}

	public SkeetLauncherScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
		super(HeirloomScreenHandlers.SKEET_LAUNCHER, syncId);
		checkSize(inventory, SIZE);
		this.inventory = inventory;
		inventory.onOpen(playerInventory.player);

		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, 8 + col * 18, 48) {
				@Override
				public boolean canInsert(ItemStack stack) {
					return stack.isOf(RitualItems.CLAY_PIGEON);
				}
			});
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return inventory.canPlayerUse(player);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		ItemStack moved = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasStack()) {
			ItemStack stack = slot.getStack();
			moved = stack.copy();
			if (index < SIZE) {
				// launcher → player inventory
				if (!this.insertItem(stack, SIZE, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.insertItem(stack, 0, SIZE, false)) {
				// player inventory → launcher (only clay pigeons are accepted by the slots)
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}
		}
		return moved;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		inventory.onClose(player);
	}
}

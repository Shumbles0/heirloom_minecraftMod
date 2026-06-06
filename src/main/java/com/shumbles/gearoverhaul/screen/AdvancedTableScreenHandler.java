package com.shumbles.gearoverhaul.screen;

import com.shumbles.gearoverhaul.enchant.AdvancedTableBlockEntity;
import com.shumbles.gearoverhaul.enchant.EnchantDirections;
import com.shumbles.gearoverhaul.enchant.EnchantItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Menu for the Advanced Enchanting Table: gear slot, Arcane Dust slot, four material slots, a
 * progress property, and the player inventory. Brewing happens in the block entity; this just shows
 * the slots + progress.
 */
public class AdvancedTableScreenHandler extends ScreenHandler {
	private final Inventory inventory;
	private final PropertyDelegate properties;

	/** Client constructor (dummy inventory + property delegate; synced from the server). */
	public AdvancedTableScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(AdvancedTableBlockEntity.SIZE), new ArrayPropertyDelegate(2));
	}

	public AdvancedTableScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
									  PropertyDelegate properties) {
		super(HeirloomScreenHandlers.ADVANCED_TABLE, syncId);
		checkSize(inventory, AdvancedTableBlockEntity.SIZE);
		this.inventory = inventory;
		this.properties = properties;
		inventory.onOpen(playerInventory.player);

		this.addSlot(new Slot(inventory, AdvancedTableBlockEntity.GEAR, 26, 47) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return EnchantDirections.canTakeDirections(stack);
			}

			@Override
			public int getMaxItemCount() {
				return 1;
			}
		});
		this.addSlot(new Slot(inventory, AdvancedTableBlockEntity.DUST, 26, 17) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isOf(EnchantItems.ARCANE_DUST);
			}
		});
		for (int i = 0; i < AdvancedTableBlockEntity.MAT_COUNT; i++) {
			this.addSlot(new Slot(inventory, AdvancedTableBlockEntity.MAT_START + i, 152, 17 + i * 18));
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}

		this.addProperties(properties);
	}

	public int getProgress() {
		return properties.get(0);
	}

	public int getMaxProgress() {
		return properties.get(1);
	}

	public ItemStack gear() {
		return inventory.getStack(AdvancedTableBlockEntity.GEAR);
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
			int beSize = AdvancedTableBlockEntity.SIZE;
			if (index < beSize) {
				if (!this.insertItem(stack, beSize, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (EnchantDirections.canTakeDirections(stack)) {
				if (!this.insertItem(stack, AdvancedTableBlockEntity.GEAR, AdvancedTableBlockEntity.GEAR + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (stack.isOf(EnchantItems.ARCANE_DUST)) {
				if (!this.insertItem(stack, AdvancedTableBlockEntity.DUST, AdvancedTableBlockEntity.DUST + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.insertItem(stack, AdvancedTableBlockEntity.MAT_START,
					AdvancedTableBlockEntity.MAT_START + AdvancedTableBlockEntity.MAT_COUNT, false)) {
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
}

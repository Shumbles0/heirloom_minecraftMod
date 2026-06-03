package com.shumbles.gearoverhaul.screen;

import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import com.shumbles.gearoverhaul.temper.data.TemperLevel;
import com.shumbles.gearoverhaul.temper.data.TemperTable;
import com.shumbles.gearoverhaul.temper.data.TemperTableLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Menu for the Tempering Station. Slot 0 is the gear; slots 1–6 hold materials.
 * Pressing the Temper button advances the gear one level if its current temper level's
 * NEXT step is defined in the tables and the required materials are present.
 *
 * <p>The station handles levels 1–19 and 21–25. Level 20 is the ritual gate, so the
 * station refuses to step from 19 → 20 (and from there the ritual is what unlocks 21+).
 */
public class TemperingStationScreenHandler extends ScreenHandler {
	public static final int TEMPER_BUTTON = 0;

	public static final int GEAR_SLOT = 0;
	private static final int FIRST_MATERIAL = 1;
	private static final int INVENTORY_SIZE = 7;
	private static final int MATERIAL_COUNT = 6;
	private static final int[] MATERIAL_X = {34, 52, 70, 88, 106, 124};
	private static final int MATERIAL_Y = 56;
	private static final int GEAR_X = 44;
	private static final int GEAR_Y = 20;

	private final Inventory inventory;
	private final ScreenHandlerContext context;

	/** Client-side constructor (dummy inventory; slots sync from the server). */
	public TemperingStationScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(INVENTORY_SIZE), ScreenHandlerContext.EMPTY);
	}

	public TemperingStationScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
										 ScreenHandlerContext context) {
		super(HeirloomScreenHandlers.TEMPERING_STATION, syncId);
		checkSize(inventory, INVENTORY_SIZE);
		this.inventory = inventory;
		this.context = context;
		inventory.onOpen(playerInventory.player);

		// Gear slot: only accepts a single piece of (damageable) gear, so shift-click
		// routes any tool/weapon/armor straight here.
		this.addSlot(new Slot(inventory, GEAR_SLOT, GEAR_X, GEAR_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.isDamageable();
			}

			@Override
			public int getMaxItemCount() {
				return 1;
			}
		});
		// Material slots: reject gear, so shift-clicked reagents land here, not on the gear.
		for (int i = 0; i < MATERIAL_COUNT; i++) {
			this.addSlot(new Slot(inventory, FIRST_MATERIAL + i, MATERIAL_X[i], MATERIAL_Y) {
				@Override
				public boolean canInsert(ItemStack stack) {
					return !stack.isDamageable();
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
	public boolean onButtonClick(PlayerEntity player, int id) {
		// onButtonClick is invoked server-side from the click packet.
		if (id != TEMPER_BUTTON) {
			return false;
		}
		return attemptTemper();
	}

	private boolean attemptTemper() {
		ItemStack gear = inventory.getStack(GEAR_SLOT);
		if (gear.isEmpty()) {
			return false;
		}

		Identifier itemId = Registries.ITEM.getId(gear.getItem());
		TemperTable table = TemperTableLoader.getTable();
		if (!table.hasItem(itemId)) {
			return false;
		}

		int current = Tempering.getLevel(gear);
		int target = nextLevel(current);
		if (target < 0) {
			return false; // maxed, or the next step is the ritual-gated level 20
		}
		if (current == 10 && !UsageGate.isComplete(gear)) {
			return false; // Level-10 milestone: the gear must be used before crossing past 10
		}

		TemperLevel entry = table.get(itemId, target);
		if (entry == null || !tryConsume(entry.materials())) {
			return false; // no recipe for that level, or materials missing
		}

		Tempering.setLevel(gear, target);
		inventory.markDirty();
		sendContentUpdates();
		return true;
	}

	/** The level the station would advance to from {@code current}, or -1 if it can't. */
	private static int nextLevel(int current) {
		if (current >= Tempering.MAX_TEMPER) {
			return -1;
		}
		int next = current + 1;
		if (next == 20) {
			return -1; // level 20 is reached by the ritual, not the station
		}
		return next;
	}

	/** Checks the material slots satisfy every cost, then consumes them. All-or-nothing. */
	private boolean tryConsume(List<TemperLevel.MaterialCost> costs) {
		for (TemperLevel.MaterialCost cost : costs) {
			int have = 0;
			for (int slot = FIRST_MATERIAL; slot < INVENTORY_SIZE; slot++) {
				ItemStack stack = inventory.getStack(slot);
				if (cost.matches(stack)) {
					have += stack.getCount();
				}
			}
			if (have < cost.count()) {
				return false;
			}
		}

		for (TemperLevel.MaterialCost cost : costs) {
			int remaining = cost.count();
			for (int slot = FIRST_MATERIAL; slot < INVENTORY_SIZE && remaining > 0; slot++) {
				ItemStack stack = inventory.getStack(slot);
				if (cost.matches(stack)) {
					int take = Math.min(remaining, stack.getCount());
					stack.decrement(take);
					remaining -= take;
				}
			}
		}
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		ItemStack moved = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasStack()) {
			ItemStack stack = slot.getStack();
			moved = stack.copy();
			if (index < INVENTORY_SIZE) {
				// station → player inventory
				if (!this.insertItem(stack, INVENTORY_SIZE, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.insertItem(stack, GEAR_SLOT, INVENTORY_SIZE, false)) {
				// player inventory → station. Slot canInsert rules route gear to the gear
				// slot and everything else to the material slots automatically.
				return ItemStack.EMPTY;
			}

			if (stack.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}
			if (stack.getCount() == moved.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTakeItem(player, stack);
		}
		return moved;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		inventory.onClose(player);
	}
}

package com.shumbles.gearoverhaul.screen;

import com.shumbles.gearoverhaul.codex.CodexComponents;
import com.shumbles.gearoverhaul.codex.CodexEntries;
import com.shumbles.gearoverhaul.codex.CodexItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Menu for the Codex. A single transient manuscript slot accepts manuscripts; the player
 * inventory is included so everything syncs.
 * <ul>
 *   <li>buttons {@code REVEAL_BASE + rarityOrdinal} — Reveal: requires 3 tempering
 *       manuscripts in the slot. Spends them and rolls up to 3 random locked recipe
 *       chapters <i>of that rarity</i> into the book's "pending" list.</li>
 *   <li>buttons 0–2 — Choose: move the chosen pending chapter into "unlocked".</li>
 * </ul>
 * Manuscripts left in the slot are returned to the player when the menu closes.
 */
public class CodexScreenHandler extends ScreenHandler {
	public static final int CHOICE_BUTTON_BASE = 0;
	public static final int REVEAL_BUTTON_BASE = 10; // + Rarity.ordinal()
	public static final int MANUSCRIPT_COST = 3;
	public static final int MANUSCRIPT_SLOT = 0;

	private static final int FIRST_PLAYER_SLOT = 1;
	private static final int HOTBAR_START = 28;

	private final PlayerInventory playerInventory;
	private final Inventory manuscriptInventory = new SimpleInventory(1);

	public CodexScreenHandler(int syncId, PlayerInventory playerInventory) {
		super(HeirloomScreenHandlers.CODEX, syncId);
		this.playerInventory = playerInventory;

		this.addSlot(new Slot(manuscriptInventory, 0, 80, 18) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return CodexItems.isManuscript(stack);
			}
		});

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 176));
		}
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		ItemStack codex = CodexItems.findCodex(player);
		if (codex.isEmpty()) {
			return false;
		}
		boolean changed;
		if (id >= CHOICE_BUTTON_BASE && id < CHOICE_BUTTON_BASE + 3) {
			changed = chooseEntry(codex, id - CHOICE_BUTTON_BASE);
		} else if (id >= REVEAL_BUTTON_BASE && id < REVEAL_BUTTON_BASE + CodexEntries.Rarity.values().length) {
			changed = tryReveal(codex, CodexEntries.Rarity.values()[id - REVEAL_BUTTON_BASE]);
		} else {
			return false;
		}
		if (changed) {
			this.sendContentUpdates();
		}
		return changed;
	}

	private boolean tryReveal(ItemStack codex, CodexEntries.Rarity rarity) {
		if (!CodexComponents.getPending(codex).isEmpty()) {
			return false; // a draw is already pending
		}
		ItemStack manuscripts = manuscriptInventory.getStack(0);
		if (!CodexItems.isTemperingManuscript(manuscripts) || manuscripts.getCount() < MANUSCRIPT_COST) {
			return false;
		}

		List<Integer> locked = CodexEntries.lockedInRarity(rarity, CodexComponents.getUnlocked(codex));
		if (locked.isEmpty()) {
			return false; // whole rarity already inscribed
		}

		Collections.shuffle(locked);
		List<Integer> pending = new ArrayList<>(locked.subList(0, Math.min(3, locked.size())));
		manuscripts.decrement(MANUSCRIPT_COST);
		codex.set(CodexComponents.PENDING, pending);
		return true;
	}

	private boolean chooseEntry(ItemStack codex, int choiceIndex) {
		List<Integer> pending = CodexComponents.getPending(codex);
		if (choiceIndex < 0 || choiceIndex >= pending.size()) {
			return false;
		}
		int chosen = pending.get(choiceIndex);
		List<Integer> unlocked = new ArrayList<>(CodexComponents.getUnlocked(codex));
		if (!unlocked.contains(chosen)) {
			unlocked.add(chosen);
		}
		codex.set(CodexComponents.UNLOCKED, unlocked);
		codex.remove(CodexComponents.PENDING);
		return true;
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		ItemStack left = manuscriptInventory.removeStack(0);
		if (!left.isEmpty()) {
			player.getInventory().offerOrDrop(left);
		}
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		ItemStack moved = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasStack()) {
			ItemStack stack = slot.getStack();
			moved = stack.copy();
			if (index == MANUSCRIPT_SLOT) {
				if (!this.insertItem(stack, FIRST_PLAYER_SLOT, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (CodexItems.isManuscript(stack)) {
				if (!this.insertItem(stack, MANUSCRIPT_SLOT, MANUSCRIPT_SLOT + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (index < HOTBAR_START) {
				if (!this.insertItem(stack, HOTBAR_START, this.slots.size(), false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.insertItem(stack, FIRST_PLAYER_SLOT, HOTBAR_START, false)) {
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

	public ItemStack getManuscriptStack() {
		return manuscriptInventory.getStack(0);
	}
}

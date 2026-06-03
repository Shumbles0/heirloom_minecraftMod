package com.shumbles.gearoverhaul.temper;

import net.minecraft.item.ItemStack;

/**
 * Core tempering constants and the read/write helpers for an item's temper level.
 *
 * <p>The level is stored in the {@link TemperComponents#TEMPER_LEVEL} data component
 * on the itemstack itself, so it travels with the piece through relogs, drops, and
 * trades — the gear's history lives on the gear. Level {@code 0} is represented by
 * the <i>absence</i> of the component, so an untempered item is byte-identical to a
 * fresh vanilla one.
 */
public final class Tempering {
	public static final int MIN_TEMPER = 0;
	public static final int MAX_TEMPER = 25;

	private Tempering() {
	}

	/** Current temper level, or {@code 0} if the item has never been tempered. */
	public static int getLevel(ItemStack stack) {
		return stack.getOrDefault(TemperComponents.TEMPER_LEVEL, MIN_TEMPER);
	}

	/** True if the item carries a temper level (i.e. has been tempered above 0). */
	public static boolean isTempered(ItemStack stack) {
		return stack.contains(TemperComponents.TEMPER_LEVEL);
	}

	/**
	 * Sets the temper level, clamped to {@code [MIN_TEMPER, MAX_TEMPER]}. Setting it
	 * to {@link #MIN_TEMPER} removes the component entirely so the item reverts to a
	 * clean untempered state.
	 */
	public static void setLevel(ItemStack stack, int level) {
		if (stack.isEmpty()) {
			return;
		}
		int clamped = Math.max(MIN_TEMPER, Math.min(MAX_TEMPER, level));
		if (clamped == MIN_TEMPER) {
			stack.remove(TemperComponents.TEMPER_LEVEL);
		} else {
			stack.set(TemperComponents.TEMPER_LEVEL, clamped);
		}
		// Re-bake the item's stat modifiers for the new level.
		TemperStats.refresh(stack);
	}
}

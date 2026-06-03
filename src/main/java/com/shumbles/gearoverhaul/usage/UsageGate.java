package com.shumbles.gearoverhaul.usage;

import com.shumbles.gearoverhaul.temper.Tempering;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import org.jetbrains.annotations.Nullable;

/**
 * The Level-10 milestone: every gear type must be <i>used</i> a certain way before it can
 * be tempered past level 10. Progress is a single int on the stack ({@link UsageComponents}).
 * This class owns the per-type thresholds, the gate check, and the block/ore classifiers;
 * {@link UsageTracking} feeds it gameplay events.
 *
 * <p>Reset rules: the sword (no-damage kill-streak) and pickaxe (qualifying-ore streak)
 * reset to 0 on failure; the chestplate (damage absorbed) resets on death. Everything else
 * simply accumulates.
 */
public final class UsageGate {
	/** Per-gear-type condition. The threshold's unit is noted per kind. */
	public enum Kind {
		SWORD(20, "kills"),         // no-damage kill-streak (resets when hit)
		AXE(200, "logs"),           // logs broken
		PICKAXE(20, "ores"),        // iron+ ore streak (resets on coal/copper ore)
		SHOVEL(200, "dug"),         // earth blocks broken
		BOW(10, "long shots"),      // kills from >10 blocks
		CROSSBOW(10, "bolt hits"),  // hits, any range
		TRIDENT(5, "throw kills"),  // thrown-trident kills
		MACE(50, "slams"),          // hits while falling
		HELMET(6000, "submerged"),  // ticks submerged (300s)
		CHESTPLATE(200, "absorbed"),// damage absorbed without dying (resets on death)
		LEGGINGS(2800, "travelled"),// blocks travelled on foot
		BOOTS(1, "hard landing");   // survive a fall leaving <= half a heart

		public final int threshold;
		public final String label;

		Kind(int threshold, String label) {
			this.threshold = threshold;
			this.label = label;
		}
	}

	private UsageGate() {
	}

	/** The condition for this item, or null if it isn't gated gear. */
	@Nullable
	public static Kind kindOf(Item item) {
		String p = Registries.ITEM.getId(item).getPath();
		if (p.endsWith("_sword")) return Kind.SWORD;
		if (p.endsWith("_pickaxe")) return Kind.PICKAXE;
		if (p.endsWith("_axe")) return Kind.AXE;
		if (p.endsWith("_shovel")) return Kind.SHOVEL;
		if (p.endsWith("_helmet")) return Kind.HELMET;
		if (p.endsWith("_chestplate")) return Kind.CHESTPLATE;
		if (p.endsWith("_leggings")) return Kind.LEGGINGS;
		if (p.endsWith("_boots")) return Kind.BOOTS;
		return switch (p) {
			case "bow" -> Kind.BOW;
			case "crossbow" -> Kind.CROSSBOW;
			case "trident" -> Kind.TRIDENT;
			case "mace" -> Kind.MACE;
			default -> null;
		};
	}

	@Nullable
	public static Kind kindOf(ItemStack stack) {
		return stack.isEmpty() ? null : kindOf(stack.getItem());
	}

	/** Only tempered gear of a gated type participates (no tracking on vanilla items). */
	public static boolean tracks(ItemStack stack, Kind kind) {
		return !stack.isEmpty() && Tempering.isTempered(stack) && kindOf(stack) == kind;
	}

	public static int progress(ItemStack stack) {
		return stack.getOrDefault(UsageComponents.USAGE, 0);
	}

	public static void setProgress(ItemStack stack, int value) {
		Kind kind = kindOf(stack);
		int max = kind == null ? value : kind.threshold;
		stack.set(UsageComponents.USAGE, Math.max(0, Math.min(value, max)));
	}

	public static void add(ItemStack stack, int amount) {
		setProgress(stack, progress(stack) + amount);
	}

	public static void reset(ItemStack stack) {
		stack.set(UsageComponents.USAGE, 0);
	}

	public static void complete(ItemStack stack) {
		Kind kind = kindOf(stack);
		if (kind != null) {
			stack.set(UsageComponents.USAGE, kind.threshold);
		}
	}

	/** Whether this gear has met its Level-10 condition. Ungated items pass trivially. */
	public static boolean isComplete(ItemStack stack) {
		Kind kind = kindOf(stack);
		return kind == null || progress(stack) >= kind.threshold;
	}

	// ---- block classifiers (pickaxe / axe / shovel) -------------------------

	/** Iron-grade-or-higher ore (counts toward the pickaxe streak). */
	public static boolean isQualifyingOre(BlockState state) {
		String p = Registries.BLOCK.getId(state.getBlock()).getPath();
		if (p.equals("ancient_debris")) {
			return true;
		}
		return p.endsWith("_ore") && !isCoalOrCopperOre(state);
	}

	/** Coal/copper ore — breaks the pickaxe streak (does not count). */
	public static boolean isCoalOrCopperOre(BlockState state) {
		String p = Registries.BLOCK.getId(state.getBlock()).getPath();
		return p.equals("coal_ore") || p.equals("deepslate_coal_ore")
			|| p.equals("copper_ore") || p.equals("deepslate_copper_ore");
	}

	public static boolean isLog(BlockState state) {
		return state.isIn(BlockTags.LOGS);
	}

	public static boolean isEarth(BlockState state) {
		return state.isIn(BlockTags.SHOVEL_MINEABLE);
	}
}

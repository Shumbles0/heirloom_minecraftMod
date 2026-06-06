package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.ritual.RitualBlocks;
import com.shumbles.gearoverhaul.ritual.RitualItems;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

/**
 * Client-side mirror of the crafting recipes for each ritual's apparatus, used to draw a
 * little item-grid in the Codex ritual entries. Kept in lock-step by hand with the recipe
 * JSONs in {@code data/heirloom/recipe/} — these are display-only and never consumed by the
 * crafting system. Rituals that need no crafted block (pickaxe, trident) return an empty list.
 */
public final class RitualRecipes {
	private RitualRecipes() {
	}

	/** One craftable apparatus: a 3x3 grid (row-major, EMPTY for blanks) and its result. */
	public record Recipe(ItemStack[] cells, ItemStack result) {
	}

	public static List<Recipe> forKind(UsageGate.Kind kind) {
		return switch (kind) {
			case SWORD -> List.of(
				shaped(new ItemStack(RitualBlocks.RITUAL_FORGE),
					Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
					Items.IRON_INGOT, Items.FURNACE, Items.IRON_INGOT,
					Items.BLACKSTONE, Items.BLACKSTONE, Items.BLACKSTONE),
				shaped(new ItemStack(RitualBlocks.QUENCHING_TROUGH),
					Items.IRON_INGOT, null, Items.IRON_INGOT,
					null, Items.CAULDRON, null,
					Items.IRON_INGOT, null, Items.IRON_INGOT));
			case AXE -> List.of(
				shaped(new ItemStack(RitualBlocks.CHOPPING_STUMP),
					null, Items.IRON_INGOT, null,
					null, Items.OAK_LOG, null,
					null, null, null));
			case SHOVEL -> List.of(
				shaped(new ItemStack(RitualBlocks.AVALANCHE_CAIRN),
					Items.GRAVEL, Items.GRAVEL, Items.GRAVEL,
					Items.GRAVEL, Items.FLINT, Items.GRAVEL,
					Items.GRAVEL, Items.GRAVEL, Items.GRAVEL));
			case BOW -> List.of(
				shaped(new ItemStack(RitualBlocks.SKEET_LAUNCHER),
					Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS,
					Items.OAK_PLANKS, Items.DISPENSER, Items.OAK_PLANKS,
					null, Items.REDSTONE, null),
				shaped(new ItemStack(RitualItems.CLAY_PIGEON),
					Items.CLAY_BALL, null, null,
					null, null, null,
					null, null, null));
			case CROSSBOW -> List.of(
				shaped(new ItemStack(RitualBlocks.BULLSEYE_TARGET),
					null, Items.RED_DYE, null,
					Items.RED_DYE, Items.QUARTZ_BLOCK, Items.RED_DYE,
					null, Items.RED_DYE, null));
			case MACE -> List.of(
				shaped(new ItemStack(RitualItems.STRIKING_PLATE),
					Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
					Items.IRON_INGOT, Items.SLIME_BLOCK, Items.IRON_INGOT,
					Items.IRON_INGOT, Items.WIND_CHARGE, Items.IRON_INGOT));
			case HELMET -> List.of(
				shaped(new ItemStack(RitualBlocks.PRESSURE_SEAL),
					Items.PRISMARINE_BRICKS, Items.PRISMARINE_BRICKS, Items.PRISMARINE_BRICKS,
					Items.PRISMARINE_BRICKS, Items.COPPER_BLOCK, Items.PRISMARINE_BRICKS,
					Items.PRISMARINE_BRICKS, Items.PRISMARINE_BRICKS, Items.PRISMARINE_BRICKS));
			case CHESTPLATE -> List.of(
				shaped(new ItemStack(RitualItems.SUICIDE_CHARGE),
					Items.LEATHER, Items.GUNPOWDER, Items.LEATHER,
					Items.GUNPOWDER, Items.TNT, Items.GUNPOWDER,
					Items.LEATHER, Items.GUNPOWDER, Items.LEATHER));
			case LEGGINGS -> List.of(
				shaped(new ItemStack(RitualBlocks.FREEZE_MACHINE),
					Items.IRON_INGOT, Items.BLUE_ICE, Items.IRON_INGOT,
					Items.BLUE_ICE, Items.REDSTONE_BLOCK, Items.BLUE_ICE,
					Items.IRON_INGOT, Items.BLUE_ICE, Items.IRON_INGOT));
			case BOOTS -> List.of(
				shaped(new ItemStack(RitualBlocks.HEATBED, 4),
					Items.MAGMA_BLOCK, Items.BLACKSTONE, null,
					null, null, null,
					null, null, null));
			case PICKAXE, TRIDENT -> List.of();
		};
	}

	/** Builds a Recipe from a result and nine item slots (null = empty cell). */
	private static Recipe shaped(ItemStack result, Item... slots) {
		ItemStack[] cells = new ItemStack[9];
		for (int i = 0; i < 9; i++) {
			cells[i] = (i < slots.length && slots[i] != null) ? new ItemStack(slots[i]) : ItemStack.EMPTY;
		}
		return new Recipe(cells, result);
	}
}

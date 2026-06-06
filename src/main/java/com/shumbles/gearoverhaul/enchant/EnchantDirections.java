package com.shumbles.gearoverhaul.enchant;

import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Which directions a piece can take (Table A of the design), and the up-to-three offered at the
 * enchanting table. The offer is stable while the chosen set is unchanged and reshuffles once you
 * pick one (the chosen set feeds the seed) — "seed-stable, rerolls on use" without touching vanilla
 * enchanting.
 */
public final class EnchantDirections {
	private EnchantDirections() {
	}

	/** Directions valid for a piece, by gear kind. */
	public static List<String> validFor(ItemStack stack) {
		UsageGate.Kind kind = UsageGate.kindOf(stack);
		if (kind == null) {
			return List.of();
		}
		return switch (kind) {
			case SWORD -> List.of("Onslaught", "Hunter", "Cruelty", "Momentum", "Persistence");
			case AXE -> List.of("Onslaught", "Hunter", "Cruelty", "Momentum", "Bite", "Persistence");
			case MACE -> List.of("Onslaught", "Hunter", "Cruelty", "Momentum", "Persistence", "Gale");
			case PICKAXE, SHOVEL -> List.of("Bite", "Bounty", "Persistence");
			case BOW, CROSSBOW -> List.of("Onslaught", "Hunter", "Cruelty", "Momentum");
			case TRIDENT -> List.of("Onslaught", "Hunter", "Cruelty", "Momentum", "Storm");
			case HELMET, CHESTPLATE, LEGGINGS, BOOTS -> List.of("Ward", "Stride", "Vigor");
		};
	}

	/** True if this is Heirloom gear that can hold directions at all. */
	public static boolean canTakeDirections(ItemStack stack) {
		return !validFor(stack).isEmpty();
	}

	/** True if directions can be added right now (gear has a free slot). */
	public static boolean eligible(ItemStack stack) {
		return canTakeDirections(stack) && EnchantComponents.hasFreeSlot(stack);
	}

	/** Up to three offered directions: valid minus already-chosen, in a per-player stable order. */
	public static List<String> offer(ItemStack stack, PlayerEntity player) {
		List<String> chosen = EnchantComponents.getDirections(stack);
		List<String> pool = new ArrayList<>(validFor(stack));
		pool.removeAll(chosen);
		long seed = player.getUuid().hashCode() * 31L + chosen.hashCode();
		Collections.shuffle(pool, new Random(seed));
		return pool.subList(0, Math.min(3, pool.size()));
	}

	/** Up to three offered directions for the gear (valid minus chosen, fixed order). No player needed. */
	public static List<String> offeredFor(ItemStack stack) {
		List<String> chosen = EnchantComponents.getDirections(stack);
		List<String> pool = new ArrayList<>(validFor(stack));
		pool.removeAll(chosen);
		return pool.subList(0, Math.min(3, pool.size()));
	}

	/** The attribute display-names belonging to a direction, in roster order. */
	public static List<String> attributesOf(String direction) {
		List<String> out = new ArrayList<>();
		for (ArcaneAttribute a : ArcaneAttribute.values()) {
			if (a.direction.equals(direction)) {
				out.add(a.displayName);
			}
		}
		return out;
	}

	// ---- selector items (the brewing reagents that pick an attribute) -------

	private static final Map<String, String> SPECIAL_SELECTORS = Map.of(
		"Bottle o' Enchanting", "experience_bottle",
		"Glistering Melon", "glistering_melon_slice");

	/** The vanilla item that selects this attribute at the Advanced table. */
	public static Item selectorItemOf(ArcaneAttribute a) {
		String path = SPECIAL_SELECTORS.getOrDefault(a.selector,
			a.selector.toLowerCase(Locale.ROOT).replace("'", "").replace(" ", "_"));
		return Registries.ITEM.getOptionalValue(Identifier.ofVanilla(path)).orElse(Items.AIR);
	}

	/** A chosen attribute for a slot (the slot index + the attribute name). */
	public record Selection(int slot, String attribute) {
	}

	/** Which unset direction slot + attribute the {@code selector} item maps to on this gear, or null. */
	public static Selection matchSelector(ItemStack gear, Item selector) {
		if (selector == Items.AIR) {
			return null;
		}
		for (int i = 0; i < EnchantComponents.slotCount(gear); i++) {
			if (EnchantComponents.attributeAt(gear, i) != null) {
				continue; // slot already has an attribute
			}
			String dir = EnchantComponents.directionAt(gear, i);
			for (ArcaneAttribute a : ArcaneAttribute.values()) {
				if (a.direction.equals(dir) && selectorItemOf(a) == selector) {
					return new Selection(i, a.displayName);
				}
			}
		}
		return null;
	}

	/** The first slot whose attribute can still be upgraded (below max level), or -1. */
	public static int firstUpgradeableSlot(ItemStack gear) {
		for (int i = 0; i < EnchantComponents.slotCount(gear); i++) {
			if (EnchantComponents.attributeAt(gear, i) != null
				&& EnchantComponents.levelAt(gear, i) < EnchantComponents.MAX_LEVEL) {
				return i;
			}
		}
		return -1;
	}

	/** True if any slot has a direction but no attribute yet (so stirring is useful). */
	public static boolean hasUnsetDirection(ItemStack gear) {
		for (int i = 0; i < EnchantComponents.slotCount(gear); i++) {
			if (EnchantComponents.attributeAt(gear, i) == null) {
				return true;
			}
		}
		return false;
	}
}

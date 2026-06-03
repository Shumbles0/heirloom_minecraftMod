package com.shumbles.gearoverhaul.codex;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The Codex's chapter model.
 *
 * <p>Index 0 is the always-unlocked <b>overview</b>. Every other chapter documents one
 * gear item's recipes for one five-level <b>band</b>, so chapter index
 * {@code 1 + gearOrdinal*BANDS + band} addresses (item, band) deterministically. The
 * gear catalogue is a fixed, hard-coded order (below), so a chapter's index is stable
 * across reloads — unlocked indices stored on the book stay valid.
 *
 * <p>Recipe <i>content</i> (the per-level materials/buffs text) is built client-side from
 * the bundled tempering tables; see {@code client.CodexContent}. This class only owns the
 * structure: which chapters exist, their indices, titles, and rarity grouping.
 */
public final class CodexEntries {
	public static final int OVERVIEW = 0;
	public static final int BANDS = 5;

	/** Short band labels, index-aligned to {@link #BAND_LEVELS}. */
	public static final String[] BAND_LABELS = {"t1-5", "t6-10", "t11-15", "t16-19", "t21-25"};
	/** The temper levels covered by each band (t20 is ritual-only and never appears). */
	public static final int[][] BAND_LEVELS = {
		{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}, {16, 17, 18, 19}, {21, 22, 23, 24, 25}
	};

	/** Rarity groups, in catalogue order. */
	public enum Rarity {
		WOOD("Wood"), STONE("Stone"), STONEWOOD("Stonewood"), COPPER("Copper"), GOLD("Gold"),
		LEATHER("Leather"), CHAINMAIL("Chainmail"), TURTLE("Turtle"), IRON("Iron"),
		DIAMOND("Diamond"), NETHERITE("Netherite"), SPECIAL("Special");

		public final String label;

		Rarity(String label) {
			this.label = label;
		}
	}

	/** One catalogued gear item: its id, rarity group, and human gear-type label. */
	public record Gear(Identifier id, Rarity rarity, String type) {
	}

	/** Fixed gear catalogue — the order here defines chapter indices. Must stay stable. */
	public static final List<Gear> ITEMS = buildCatalog();

	private CodexEntries() {
	}

	public static int chapterCount() {
		return ITEMS.size() * BANDS;
	}

	/** Total addressable entries including the overview. */
	public static int count() {
		return 1 + chapterCount();
	}

	public static boolean isOverview(int index) {
		return index == OVERVIEW;
	}

	public static int chapterIndex(int gearOrdinal, int band) {
		return 1 + gearOrdinal * BANDS + band;
	}

	public static int gearOrdinalOf(int index) {
		return (index - 1) / BANDS;
	}

	public static int bandOf(int index) {
		return (index - 1) % BANDS;
	}

	public static Gear gearOf(int index) {
		return ITEMS.get(gearOrdinalOf(index));
	}

	public static Rarity rarityOf(int index) {
		return gearOf(index).rarity();
	}

	/** Localized display name of a catalogued item (falls back to its path). */
	public static Text gearName(Gear gear) {
		Item item = Registries.ITEM.getOptionalValue(gear.id()).orElse(null);
		return item != null ? new ItemStack(item).getName() : Text.literal(gear.id().getPath());
	}

	/** Chapter title: the overview, or "<Item> (<band>)". */
	public static Text title(int index) {
		if (isOverview(index)) {
			return Text.literal("The Heirloom Codex");
		}
		Gear gear = gearOf(index);
		return gearName(gear).copy().append(Text.literal(" (" + BAND_LABELS[bandOf(index)] + ")"));
	}

	/** All chapter indices whose item is in {@code rarity}. */
	public static List<Integer> chaptersInRarity(Rarity rarity) {
		List<Integer> out = new ArrayList<>();
		for (int ordinal = 0; ordinal < ITEMS.size(); ordinal++) {
			if (ITEMS.get(ordinal).rarity() == rarity) {
				for (int band = 0; band < BANDS; band++) {
					out.add(chapterIndex(ordinal, band));
				}
			}
		}
		return out;
	}

	/** Chapter indices in {@code rarity} that are not yet in {@code unlocked}. */
	public static List<Integer> lockedInRarity(Rarity rarity, List<Integer> unlocked) {
		List<Integer> out = new ArrayList<>();
		for (int index : chaptersInRarity(rarity)) {
			if (!unlocked.contains(index)) {
				out.add(index);
			}
		}
		return out;
	}

	// ---- catalogue ----------------------------------------------------------

	private static List<Gear> buildCatalog() {
		List<Gear> list = new ArrayList<>();
		String[] toolSet = {"sword", "pickaxe", "axe", "shovel"};
		String[] armorSet = {"helmet", "chestplate", "leggings", "boots"};
		String[] fullSet = {"sword", "pickaxe", "axe", "shovel", "helmet", "chestplate", "leggings", "boots"};

		addMc(list, Rarity.WOOD, "wooden_", toolSet);
		addMc(list, Rarity.STONE, "stone_", toolSet);
		add(list, Rarity.STONEWOOD, "heirloom", "stonewood_", armorSet);
		addMc(list, Rarity.COPPER, "copper_", fullSet);
		addMc(list, Rarity.GOLD, "golden_", fullSet);
		addMc(list, Rarity.LEATHER, "leather_", armorSet);
		addMc(list, Rarity.CHAINMAIL, "chainmail_", armorSet);
		addMc(list, Rarity.TURTLE, "turtle_", new String[]{"helmet"});
		addMc(list, Rarity.IRON, "iron_", fullSet);
		addMc(list, Rarity.DIAMOND, "diamond_", fullSet);
		addMc(list, Rarity.NETHERITE, "netherite_", fullSet);
		// Special items have no shared prefix.
		list.add(new Gear(Identifier.ofVanilla("bow"), Rarity.SPECIAL, "Bow"));
		list.add(new Gear(Identifier.ofVanilla("crossbow"), Rarity.SPECIAL, "Crossbow"));
		list.add(new Gear(Identifier.ofVanilla("trident"), Rarity.SPECIAL, "Trident"));
		list.add(new Gear(Identifier.ofVanilla("mace"), Rarity.SPECIAL, "Mace"));
		return List.copyOf(list);
	}

	private static void addMc(List<Gear> list, Rarity rarity, String prefix, String[] suffixes) {
		add(list, rarity, "minecraft", prefix, suffixes);
	}

	private static void add(List<Gear> list, Rarity rarity, String ns, String prefix, String[] suffixes) {
		for (String suffix : suffixes) {
			list.add(new Gear(Identifier.of(ns, prefix + suffix), rarity, capitalize(suffix)));
		}
	}

	private static String capitalize(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}

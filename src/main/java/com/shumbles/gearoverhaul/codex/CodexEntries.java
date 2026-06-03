package com.shumbles.gearoverhaul.codex;

import com.shumbles.gearoverhaul.usage.UsageGate;
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

	/** Total addressable entries including the overview, milestones and rituals. */
	public static int count() {
		return 1 + chapterCount() + MILESTONE_KINDS.length + RITUAL_KINDS.length;
	}

	// ---- milestones ---------------------------------------------------------
	// Level-10 "use it" entries, one per gear-type. Unlike recipe chapters they are
	// rarity-agnostic (the same condition applies to every rarity of a type), so they
	// get their own index range after the recipe chapters and are seeded into every
	// rarity's reveal pool. Enum order is stable, so the indices stay valid across loads.

	/** Milestone entries, one per gear-type {@link UsageGate.Kind}, in enum order. */
	public static final UsageGate.Kind[] MILESTONE_KINDS = UsageGate.Kind.values();

	/** Index of the first milestone entry — right after the recipe chapters. */
	public static int milestoneBase() {
		return 1 + chapterCount();
	}

	public static int milestoneIndex(int kindOrdinal) {
		return milestoneBase() + kindOrdinal;
	}

	public static boolean isMilestone(int index) {
		return index >= milestoneBase() && index < milestoneBase() + MILESTONE_KINDS.length;
	}

	public static UsageGate.Kind milestoneKindOf(int index) {
		return MILESTONE_KINDS[index - milestoneBase()];
	}

	/** All milestone entry indices, in order. */
	public static List<Integer> allMilestones() {
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < MILESTONE_KINDS.length; i++) {
			out.add(milestoneIndex(i));
		}
		return out;
	}

	/** Milestone entries not yet in {@code unlocked}. */
	public static List<Integer> lockedMilestones(List<Integer> unlocked) {
		List<Integer> out = new ArrayList<>();
		for (int index : allMilestones()) {
			if (!unlocked.contains(index)) {
				out.add(index);
			}
		}
		return out;
	}

	/** The locked entries a reveal of {@code rarity} may draw: that rarity's recipes plus all milestones. */
	public static List<Integer> revealPool(Rarity rarity, List<Integer> unlocked) {
		List<Integer> out = new ArrayList<>(lockedInRarity(rarity, unlocked));
		out.addAll(lockedMilestones(unlocked));
		return out;
	}

	// ---- rituals ------------------------------------------------------------
	// Level-20 ritual entries, one per gear-type, after the milestones. Rarity-agnostic like
	// milestones, but in their own RITUAL track unlocked by Ritual Manuscripts.

	/** Ritual entries, one per gear-type {@link UsageGate.Kind}, in enum order. */
	public static final UsageGate.Kind[] RITUAL_KINDS = UsageGate.Kind.values();

	/** Index of the first ritual entry — right after the milestone entries. */
	public static int ritualBase() {
		return milestoneBase() + MILESTONE_KINDS.length;
	}

	public static int ritualIndex(int kindOrdinal) {
		return ritualBase() + kindOrdinal;
	}

	public static boolean isRitual(int index) {
		return index >= ritualBase() && index < ritualBase() + RITUAL_KINDS.length;
	}

	public static UsageGate.Kind ritualKindOf(int index) {
		return RITUAL_KINDS[index - ritualBase()];
	}

	/** All ritual entry indices, in order. */
	public static List<Integer> allRituals() {
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < RITUAL_KINDS.length; i++) {
			out.add(ritualIndex(i));
		}
		return out;
	}

	/** Ritual entries not yet in {@code unlocked}. */
	public static List<Integer> lockedRituals(List<Integer> unlocked) {
		List<Integer> out = new ArrayList<>();
		for (int index : allRituals()) {
			if (!unlocked.contains(index)) {
				out.add(index);
			}
		}
		return out;
	}

	// ---- tracks -------------------------------------------------------------
	// The Codex is split into three knowledge tracks, each fed by its own manuscript type
	// (see CodexItems.trackOf). The reading view and the inscribe pool are filtered per track.

	/** The three knowledge tracks. */
	public enum Track {
		TEMPERING("Tempering"), RITUAL("Ritual"), ARCANE("Arcane");

		public final String label;

		Track(String label) {
			this.label = label;
		}
	}

	/** Which track an entry index belongs to. (The overview is shown in every track.) */
	public static Track trackOf(int index) {
		if (isRitual(index)) {
			return Track.RITUAL;
		}
		// Overview, recipe chapters and milestones are all Tempering; Arcane has no entries yet.
		return Track.TEMPERING;
	}

	/** Total inscribable entries in a track — the denominator for the "x / y" display. */
	public static int totalInTrack(Track track) {
		return switch (track) {
			case TEMPERING -> chapterCount() + MILESTONE_KINDS.length;
			case RITUAL -> RITUAL_KINDS.length;
			case ARCANE -> 0;
		};
	}

	/** How many of a track's entries are currently unlocked. */
	public static int unlockedInTrack(Track track, List<Integer> unlocked) {
		int n = 0;
		for (int idx : unlocked) {
			if (trackOf(idx) == track) {
				n++;
			}
		}
		return n;
	}

	/** Locked entries of a non-tempering track (ritual/arcane), for a flat reveal pool. */
	public static List<Integer> lockedInTrack(Track track, List<Integer> unlocked) {
		return switch (track) {
			case RITUAL -> lockedRituals(unlocked);
			default -> List.of();
		};
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

	/** Chapter title: the overview, a milestone, or "<Item> (<band>)". */
	public static Text title(int index) {
		if (isOverview(index)) {
			return Text.literal("The Heirloom Codex");
		}
		if (isMilestone(index)) {
			return Text.literal(milestoneKindOf(index).gearName + " Milestone");
		}
		if (isRitual(index)) {
			return Text.literal(ritualKindOf(index).gearName + " Ritual");
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

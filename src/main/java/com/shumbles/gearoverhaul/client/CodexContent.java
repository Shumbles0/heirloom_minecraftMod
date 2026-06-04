package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.codex.CodexEntries;
import com.shumbles.gearoverhaul.temper.data.TemperLevel;
import com.shumbles.gearoverhaul.temper.data.TemperTable;
import com.shumbles.gearoverhaul.temper.data.TemperTableLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side text for the Codex: the overview, and the per-chapter recipe pages built
 * from the mod's bundled tempering tables. Each method returns a list of logical lines
 * (paragraphs); the reading screen word-wraps and scrolls them.
 */
public final class CodexContent {
	private static final String[] FILE_STEMS = {
		"wood", "stone", "stonewood", "copper", "gold", "leather",
		"chainmail", "turtle", "iron", "diamond", "netherite", "special"
	};

	private static volatile TemperTable table;

	private CodexContent() {
	}

	private static TemperTable table() {
		TemperTable local = table;
		if (local == null) {
			local = TemperTableLoader.loadBundled(FILE_STEMS);
			table = local;
		}
		return local;
	}

	// ---- overview (per track) ----------------------------------------------

	/** Title shown on a track's overview page. */
	public static String overviewTitle(CodexEntries.Track track) {
		return switch (track) {
			case TEMPERING -> "The Heirloom Codex";
			case RITUAL -> "The Rites";
			case ARCANE -> "The Arcane";
		};
	}

	public static List<Text> overviewLines(CodexEntries.Track track) {
		return switch (track) {
			case TEMPERING -> temperingOverview();
			case RITUAL -> ritualOverview();
			case ARCANE -> arcaneOverview();
		};
	}

	private static List<Text> temperingOverview() {
		List<Text> out = new ArrayList<>();
		out.add(Text.literal("Every blade and plate you find in the world is but an echo of what it once was. "
			+ "The old forging is lost; what you hold now barely outcuts a bare fist. This Codex is the way back."));
		out.add(Text.literal(""));
		out.add(Text.literal("TEMPERING. At a Tempering Station, gear is raised one deliberate level at a time, "
			+ "1 through 25. Each level asks for a fixed set of materials — no luck, no waste — and grants a "
			+ "fixed gain. What a level costs and gives is written here, one chapter per piece per stage."));
		out.add(Text.literal(""));
		out.add(Text.literal("THE CURVE. Gains start small and climb. Around level 10 a piece is roughly 40% of its "
			+ "former self; by level 19 it has returned to its old vanilla strength. Weapons and tools may be pushed "
			+ "a final tenth beyond that by level 25; armor is restored to its full old worth and no further."));
		out.add(Text.literal(""));
		out.add(Text.literal("REAGENTS. Five refined compounds carry the work upward: Dust, then Flux, then Alloy, "
			+ "then Core, and at the summit Quintessence. Each five-level band leans on the next compound, alongside "
			+ "materials fitting the gear itself."));
		out.add(Text.literal(""));
		out.add(Text.literal("MILESTONES. Level 10 is a threshold the gear must be put to use to cross — each kind of "
			+ "piece has its own deed to perform, recorded among these chapters."));
		out.add(Text.literal(""));
		out.add(Text.literal("Knowledge is inscribed, not given. Gather Tempering Manuscripts, choose a rarity, and "
			+ "spend them to reveal its recipes — chapter by chapter — onto these pages."));
		return out;
	}

	private static List<Text> ritualOverview() {
		List<Text> out = new ArrayList<>();
		out.add(Text.literal("Tempering carries a piece to level 19, and there it stops. The final climb — levels 21 "
			+ "through 25 — is opened not by the station but by a rite."));
		out.add(Text.literal(""));
		out.add(Text.literal("THE RITES. Level 20 is no plain temper. Each kind of gear must undergo one deliberate, "
			+ "active deed of its own — performed in the world, not at the station. Carry it out on a piece tempered "
			+ "to 19 and it crosses to 20, and the station will take it the rest of the way."));
		out.add(Text.literal(""));
		out.add(Text.literal("SOFT FAILURE. A botched rite never destroys the gear; at worst it slips a level, costing "
			+ "you time and reagents, never the piece itself. Go again."));
		out.add(Text.literal(""));
		out.add(Text.literal("Each rite, and exactly what it asks, is inscribed here from Ritual Manuscripts — one "
			+ "entry per kind of gear."));
		return out;
	}

	private static List<Text> arcaneOverview() {
		List<Text> out = new ArrayList<>();
		out.add(Text.literal("The arcane arts — the old enchanting — are lost more deeply than any forging. These "
			+ "pages wait for knowledge that has not yet been rediscovered."));
		out.add(Text.literal(""));
		out.add(Text.literal("When the work is done, Enchanting Manuscripts will inscribe its secrets here. For now, "
			+ "the section stands empty."));
		return out;
	}

	// ---- milestones ---------------------------------------------------------

	/** The exact Level-10 condition for a milestone entry. */
	public static List<Text> milestoneLines(int index) {
		return MilestoneText.codexLines(CodexEntries.milestoneKindOf(index));
	}

	/** The Level-20 rite for a ritual entry. */
	public static List<Text> ritualLines(int index) {
		return RitualText.codexLines(CodexEntries.ritualKindOf(index));
	}

	// ---- chapters -----------------------------------------------------------

	public static List<Text> chapterLines(int index) {
		List<Text> out = new ArrayList<>();
		CodexEntries.Gear gear = CodexEntries.gearOf(index);
		int band = CodexEntries.bandOf(index);
		String gearName = CodexEntries.gearName(gear).getString();

		out.add(Text.literal("Recipes for " + gearName + ", levels " + CodexEntries.BAND_LABELS[band].substring(1) + "."));
		String effect = specialEffect(gear);
		if (effect != null) {
			out.add(Text.literal(effect));
		}
		out.add(Text.literal(""));

		TemperTable t = table();
		for (int level : CodexEntries.BAND_LEVELS[band]) {
			TemperLevel entry = t.get(gear.id(), level);
			if (entry == null) {
				continue;
			}
			String mats = formatMaterials(entry);
			String buffs = formatBuffs(entry);
			String line = "Lv " + level + ":  " + mats;
			if (!buffs.isEmpty()) {
				line += "   ->   " + buffs;
			}
			out.add(Text.literal(line));
		}
		return out;
	}

	private static String specialEffect(CodexEntries.Gear gear) {
		String p = gear.id().getPath();
		return switch (p) {
			case "bow" -> "Tempering restores draw speed (3x slower at first) back to true.";
			case "crossbow" -> "Tempering restores bolt velocity (a third at first) back to full.";
			case "trident" -> "Tempering restores throw damage and melee bite.";
			case "mace" -> "Tempering restores the fall-smash bonus and melee bite.";
			default -> null;
		};
	}

	private static String formatMaterials(TemperLevel entry) {
		if (entry.materials().isEmpty()) {
			return "(no materials yet)";
		}
		List<String> parts = new ArrayList<>();
		for (TemperLevel.MaterialCost cost : entry.materials()) {
			parts.add(cost.count() + "x " + ingredientName(cost.ingredient()));
		}
		return String.join(", ", parts);
	}

	private static String ingredientName(TemperLevel.Ingredient ingredient) {
		if (ingredient instanceof TemperLevel.Ingredient.OfItem oi) {
			return new net.minecraft.item.ItemStack(oi.item()).getName().getString();
		}
		if (ingredient instanceof TemperLevel.Ingredient.OfTag ot) {
			return "any " + prettify(ot.tag().id());
		}
		return "?";
	}

	private static String formatBuffs(TemperLevel entry) {
		if (entry.buffs().isEmpty()) {
			return "";
		}
		List<String> parts = new ArrayList<>();
		for (TemperLevel.StatBuff buff : entry.buffs()) {
			String sign = buff.value() >= 0 ? "+" : "";
			parts.add(sign + String.format("%.2f", buff.value()) + " " + attributeName(buff.attributeId()));
		}
		return String.join(", ", parts);
	}

	private static String attributeName(Identifier id) {
		return switch (id.getPath()) {
			case "attack_damage" -> "Attack Damage";
			case "attack_speed" -> "Attack Speed";
			case "block_break_speed" -> "Mining Speed";
			case "armor" -> "Armor";
			case "armor_toughness" -> "Armor Toughness";
			default -> prettify(id);
		};
	}

	private static String prettify(Identifier id) {
		String[] words = id.getPath().split("_");
		StringBuilder sb = new StringBuilder();
		for (String w : words) {
			if (w.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		}
		return sb.toString();
	}
}

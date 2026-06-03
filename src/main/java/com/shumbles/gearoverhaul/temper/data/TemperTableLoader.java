package com.shumbles.gearoverhaul.temper.data;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.Tempering;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Loads the tempering tables from {@code data/<namespace>/tempering/*.txt} on every
 * datapack (re)load. Server-authoritative; reloadable with {@code /reload}.
 *
 * <p>File format (one file per rarity/material tier, e.g. {@code iron.txt}):
 * <pre>
 *   # comments start with '#'
 *   [iron_sword]
 *   t1: 2 heirloom:tempering_dust, 1 iron_ingot -> +0.2 attack_damage
 *   t10: ...                                     -> ...
 *   t25: ...                                     -> ...
 * </pre>
 * Section = full item id. Each {@code t<level>} line is {@code materials -> buffs}.
 * Materials are {@code <count> <item_id>}; buffs are {@code <value> <attribute_id>}
 * (absolute totals). Level 20 is ritual-only and must be absent; level 10 lives in
 * the file but is use-gated by the station logic.
 *
 * <p>Validation is loud but non-fatal: bad lines are reported (with file:line and a
 * reason) and skipped, valid entries still load, and a summary is logged. This keeps
 * the game runnable while you author, without letting a typo pass silently.
 */
public final class TemperTableLoader implements SynchronousResourceReloader {
	private static final Identifier RELOADER_ID = Identifier.of(Heirloom.MOD_ID, "tempering_tables");
	private static final String DIRECTORY = "tempering";

	/** Levels every gear item is expected to define: 1..19 and 21..25 (10 included, 20 excluded). */
	private static final Set<Integer> EXPECTED_LEVELS = buildExpectedLevels();

	private static volatile TemperTable current = TemperTable.EMPTY;

	public static TemperTable getTable() {
		return current;
	}

	public static void register() {
		ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(RELOADER_ID, new TemperTableLoader());
		Heirloom.LOGGER.info("[Tempering] Table loader registered (reads data/<ns>/{}/*.txt).", DIRECTORY);
	}

	@Override
	public void reload(ResourceManager manager) {
		Map<Identifier, Map<Integer, TemperLevel>> built = new HashMap<>();
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		Map<Identifier, Resource> files = manager.findResources(DIRECTORY, id -> id.getPath().endsWith(".txt"));
		for (Map.Entry<Identifier, Resource> file : files.entrySet()) {
			Identifier fileId = file.getKey();
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getValue().getInputStream(), StandardCharsets.UTF_8))) {
				parseFile(fileId, reader, built, errors);
			} catch (IOException e) {
				errors.add(fileId + ": could not read file (" + e.getMessage() + ")");
			}
		}

		int unstarted = checkCompleteness(built, warnings);
		current = freeze(built);

		warnings.forEach(w -> Heirloom.LOGGER.warn("[Tempering] {}", w));
		errors.forEach(e -> Heirloom.LOGGER.error("[Tempering] {}", e));
		if (errors.isEmpty()) {
			Heirloom.LOGGER.info("[Tempering] Loaded tables: {} items ({} not yet authored), {} level-entries{}.",
				current.itemCount(), unstarted, current.entryCount(),
				warnings.isEmpty() ? "" : ", " + warnings.size() + " warning(s)");
		} else {
			Heirloom.LOGGER.error("[Tempering] Loaded with {} ERROR(S) and {} warning(s) — {} items, {} valid entries. "
					+ "Fix the errors above; affected entries were skipped.",
				errors.size(), warnings.size(), current.itemCount(), current.entryCount());
		}
	}

	/**
	 * Builds a table directly from the mod's <i>bundled</i> tempering files on the
	 * classpath (e.g. {@code /data/heirloom/tempering/iron.txt}). Used client-side by the
	 * Codex to render recipes without a server round-trip. Parse errors are swallowed —
	 * the authoritative server load already reports them loudly. Pass the rarity file
	 * stems (no extension), e.g. {@code "iron"}, {@code "special"}.
	 */
	public static TemperTable loadBundled(String... fileStems) {
		Map<Identifier, Map<Integer, TemperLevel>> built = new HashMap<>();
		List<String> ignored = new ArrayList<>();
		for (String stem : fileStems) {
			String resource = "/data/" + Heirloom.MOD_ID + "/" + DIRECTORY + "/" + stem + ".txt";
			Identifier fileId = Identifier.of(Heirloom.MOD_ID, DIRECTORY + "/" + stem + ".txt");
			try (InputStream in = TemperTableLoader.class.getResourceAsStream(resource)) {
				if (in == null) {
					continue;
				}
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
					parseFile(fileId, reader, built, ignored);
				}
			} catch (IOException e) {
				Heirloom.LOGGER.warn("[Codex] Could not read bundled table {}: {}", stem, e.getMessage());
			}
		}
		return freeze(built);
	}

	// ---- parsing -------------------------------------------------------------

	private static void parseFile(Identifier fileId, BufferedReader reader,
								  Map<Identifier, Map<Integer, TemperLevel>> built, List<String> errors) throws IOException {
		String line;
		int lineNo = 0;
		@Nullable Identifier currentItem = null;
		boolean skipSection = false;

		while ((line = reader.readLine()) != null) {
			lineNo++;
			String text = line.trim();
			if (text.isEmpty() || text.startsWith("#")) {
				continue;
			}

			if (text.startsWith("[")) {
				if (!text.endsWith("]")) {
					errors.add(at(fileId, lineNo) + "malformed section header (missing ']'): " + text);
					currentItem = null;
					skipSection = true;
					continue;
				}
				String name = text.substring(1, text.length() - 1).trim();
				Identifier itemId = Identifier.tryParse(name);
				if (itemId == null || !Registries.ITEM.containsId(itemId)) {
					errors.add(at(fileId, lineNo) + "unknown item '" + name + "' in section header");
					currentItem = null;
					skipSection = true;
					continue;
				}
				currentItem = itemId;
				skipSection = false;
				built.computeIfAbsent(itemId, k -> new TreeMap<>());
				continue;
			}

			// Level line.
			if (skipSection) {
				continue; // already reported the bad section header
			}
			if (currentItem == null) {
				errors.add(at(fileId, lineNo) + "temper line before any valid [item] section: " + text);
				continue;
			}
			parseLevelLine(fileId, lineNo, text, currentItem, built.get(currentItem), errors);
		}
	}

	private static void parseLevelLine(Identifier fileId, int lineNo, String text, Identifier item,
									   Map<Integer, TemperLevel> levels, List<String> errors) {
		int colon = text.indexOf(':');
		if (colon < 0 || text.charAt(0) != 't') {
			errors.add(at(fileId, lineNo) + "expected 't<level>: materials -> buffs', got: " + text);
			return;
		}

		int level;
		try {
			level = Integer.parseInt(text.substring(1, colon).trim());
		} catch (NumberFormatException e) {
			errors.add(at(fileId, lineNo) + "could not read temper level from '" + text.substring(0, colon) + "'");
			return;
		}

		if (level == 20) {
			errors.add(at(fileId, lineNo) + "level 20 is ritual-only and must not appear in the file");
			return;
		}
		if (level < Tempering.MIN_TEMPER + 1 || level > Tempering.MAX_TEMPER) {
			errors.add(at(fileId, lineNo) + "level " + level + " out of range (1.." + Tempering.MAX_TEMPER + ")");
			return;
		}
		if (levels.containsKey(level)) {
			errors.add(at(fileId, lineNo) + "duplicate definition for " + item + " level " + level);
			return;
		}

		String body = text.substring(colon + 1);
		int arrow = body.indexOf("->");
		String matPart = arrow >= 0 ? body.substring(0, arrow) : body;
		String buffPart = arrow >= 0 ? body.substring(arrow + 2) : "";

		List<TemperLevel.MaterialCost> materials = new ArrayList<>();
		for (String token : splitList(matPart)) {
			TemperLevel.MaterialCost cost = parseMaterial(fileId, lineNo, token, errors);
			if (cost != null) {
				materials.add(cost);
			}
		}

		List<TemperLevel.StatBuff> buffs = new ArrayList<>();
		for (String token : splitList(buffPart)) {
			TemperLevel.StatBuff buff = parseBuff(fileId, lineNo, token, errors);
			if (buff != null) {
				buffs.add(buff);
			}
		}

		levels.put(level, new TemperLevel(List.copyOf(materials), List.copyOf(buffs)));
	}

	@Nullable
	private static TemperLevel.MaterialCost parseMaterial(Identifier fileId, int lineNo, String token, List<String> errors) {
		String[] parts = token.split("\\s+");
		int count;
		String idStr;
		if (parts.length == 1) {
			count = 1;
			idStr = parts[0];
		} else if (parts.length == 2) {
			try {
				count = Integer.parseInt(parts[0]);
			} catch (NumberFormatException e) {
				errors.add(at(fileId, lineNo) + "material count not a number in '" + token + "'");
				return null;
			}
			idStr = parts[1];
		} else {
			errors.add(at(fileId, lineNo) + "malformed material '" + token + "' (expected '<count> <item_id>')");
			return null;
		}

		if (count <= 0) {
			errors.add(at(fileId, lineNo) + "material count must be positive in '" + token + "'");
			return null;
		}

		// A leading '#' denotes an item tag ("any of"); otherwise a single item id. Tag
		// membership can't be validated here (tags aren't bound during this reload), so we
		// only check the id is well-formed — an empty/typo'd tag simply matches nothing.
		if (idStr.startsWith("#")) {
			Identifier tagId = Identifier.tryParse(idStr.substring(1));
			if (tagId == null) {
				errors.add(at(fileId, lineNo) + "invalid item tag '" + idStr + "'");
				return null;
			}
			TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, tagId);
			return new TemperLevel.MaterialCost(new TemperLevel.Ingredient.OfTag(tag), count);
		}

		Identifier id = Identifier.tryParse(idStr);
		if (id == null) {
			errors.add(at(fileId, lineNo) + "invalid item id '" + idStr + "'");
			return null;
		}
		Item item = Registries.ITEM.getOptionalValue(id).orElse(null);
		if (item == null) {
			errors.add(at(fileId, lineNo) + "unknown item '" + idStr + "'");
			return null;
		}
		return new TemperLevel.MaterialCost(new TemperLevel.Ingredient.OfItem(item), count);
	}

	@Nullable
	private static TemperLevel.StatBuff parseBuff(Identifier fileId, int lineNo, String token, List<String> errors) {
		String[] parts = token.split("\\s+");
		if (parts.length != 2) {
			errors.add(at(fileId, lineNo) + "malformed buff '" + token + "' (expected '<value> <attribute_id>')");
			return null;
		}
		double value;
		try {
			value = Double.parseDouble(parts[0]);
		} catch (NumberFormatException e) {
			errors.add(at(fileId, lineNo) + "buff value not a number in '" + token + "'");
			return null;
		}
		Identifier id = Identifier.tryParse(parts[1]);
		if (id == null) {
			errors.add(at(fileId, lineNo) + "invalid attribute id '" + parts[1] + "'");
			return null;
		}
		RegistryEntry<EntityAttribute> attribute = Registries.ATTRIBUTE.getEntry(id).orElse(null);
		if (attribute == null) {
			errors.add(at(fileId, lineNo) + "unknown attribute '" + parts[1] + "'");
			return null;
		}
		return new TemperLevel.StatBuff(attribute, id, value);
	}

	// ---- post-processing -----------------------------------------------------

	/**
	 * Warns about partially-filled gear (some levels but not all). Sections with NO
	 * entries are treated as "not yet authored" and counted, not warned, so a fresh
	 * scaffold of empty sections loads without spam. Returns the unstarted count.
	 */
	private static int checkCompleteness(Map<Identifier, Map<Integer, TemperLevel>> built, List<String> warnings) {
		int unstarted = 0;
		for (Map.Entry<Identifier, Map<Integer, TemperLevel>> entry : built.entrySet()) {
			if (entry.getValue().isEmpty()) {
				unstarted++;
				continue;
			}
			List<Integer> missing = new ArrayList<>();
			for (int level : EXPECTED_LEVELS) {
				if (!entry.getValue().containsKey(level)) {
					missing.add(level);
				}
			}
			if (!missing.isEmpty()) {
				warnings.add(entry.getKey() + " is incomplete, missing levels " + missing);
			}
		}
		return unstarted;
	}

	private static TemperTable freeze(Map<Identifier, Map<Integer, TemperLevel>> built) {
		Map<Identifier, Map<Integer, TemperLevel>> frozen = new HashMap<>();
		built.forEach((item, levels) -> frozen.put(item, Map.copyOf(levels)));
		return new TemperTable(Map.copyOf(frozen));
	}

	// ---- helpers -------------------------------------------------------------

	private static List<String> splitList(String part) {
		List<String> out = new ArrayList<>();
		for (String token : part.split(",")) {
			String t = token.trim();
			if (!t.isEmpty()) {
				out.add(t);
			}
		}
		return out;
	}

	private static String at(Identifier fileId, int lineNo) {
		return fileId + ":" + lineNo + ": ";
	}

	private static Set<Integer> buildExpectedLevels() {
		Set<Integer> levels = new LinkedHashSet<>();
		for (int l = Tempering.MIN_TEMPER + 1; l <= Tempering.MAX_TEMPER; l++) {
			if (l != 20) {
				levels.add(l);
			}
		}
		return levels;
	}
}

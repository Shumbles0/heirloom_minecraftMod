package com.shumbles.gearoverhaul.temper.data;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, loaded view of all tempering recipes: for each gear item, a map of
 * temper level → {@link TemperLevel}. Built by {@link TemperTableLoader} on each
 * datapack (re)load and swapped in atomically.
 */
public final class TemperTable {
	public static final TemperTable EMPTY = new TemperTable(Map.of());

	private final Map<Identifier, Map<Integer, TemperLevel>> byItem;

	public TemperTable(Map<Identifier, Map<Integer, TemperLevel>> byItem) {
		this.byItem = byItem;
	}

	/** The recipe for advancing {@code item} to {@code level}, or null if undefined. */
	@Nullable
	public TemperLevel get(Identifier item, int level) {
		Map<Integer, TemperLevel> levels = byItem.get(item);
		return levels == null ? null : levels.get(level);
	}

	/**
	 * The effective level data for an item at {@code level}: the entry for the highest
	 * defined level &le; {@code level}, or null if none. This lets level 20 (ritual-only,
	 * undefined) and any authoring gaps fall back to the last real level's stats.
	 */
	@Nullable
	public TemperLevel getEffective(Identifier item, int level) {
		Map<Integer, TemperLevel> levels = byItem.get(item);
		if (levels == null) {
			return null;
		}
		int best = -1;
		for (int defined : levels.keySet()) {
			if (defined <= level && defined > best) {
				best = defined;
			}
		}
		return best < 0 ? null : levels.get(best);
	}

	public boolean hasItem(Identifier item) {
		return byItem.containsKey(item);
	}

	/** Defined levels for an item (read-only), or empty if the item is unknown. */
	public Set<Integer> levelsFor(Identifier item) {
		Map<Integer, TemperLevel> levels = byItem.get(item);
		return levels == null ? Set.of() : Collections.unmodifiableSet(levels.keySet());
	}

	public Set<Identifier> items() {
		return Collections.unmodifiableSet(byItem.keySet());
	}

	public int itemCount() {
		return byItem.size();
	}

	public int entryCount() {
		return byItem.values().stream().mapToInt(Map::size).sum();
	}
}

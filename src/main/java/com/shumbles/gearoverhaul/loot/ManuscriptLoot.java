package com.shumbles.gearoverhaul.loot;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.codex.CodexItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Seeds manuscripts into vanilla chest/vault loot via {@link LootTableEvents#MODIFY} (no vanilla
 * files overwritten). Tempering Manuscripts are spread across common exploration loot; Ritual
 * Manuscripts are confined to the rare/dangerous deep structures.
 */
public final class ManuscriptLoot {
	/** Tempering manuscript sources → [min, max] count. */
	private static final Map<Identifier, int[]> TEMPERING = new HashMap<>();

	static {
		TEMPERING.put(vanilla("chests/village/village_weaponsmith"), new int[]{4, 8});
		TEMPERING.put(vanilla("chests/village/village_armorer"), new int[]{4, 8});
		TEMPERING.put(vanilla("chests/village/village_toolsmith"), new int[]{4, 8});
		TEMPERING.put(vanilla("chests/abandoned_mineshaft"), new int[]{1, 3});
		TEMPERING.put(vanilla("chests/simple_dungeon"), new int[]{1, 3});
		TEMPERING.put(vanilla("chests/pillager_outpost"), new int[]{2, 4});
		TEMPERING.put(vanilla("chests/stronghold_corridor"), new int[]{1, 3});
		TEMPERING.put(vanilla("chests/stronghold_crossing"), new int[]{1, 3});
		TEMPERING.put(vanilla("chests/desert_pyramid"), new int[]{2, 4});
		TEMPERING.put(vanilla("chests/jungle_temple"), new int[]{2, 4});
	}

	private static final Identifier ANCIENT_CITY = vanilla("chests/ancient_city");
	private static final Identifier VAULT_OMINOUS = vanilla("chests/trial_chambers/reward_ominous");
	private static final Identifier VAULT = vanilla("chests/trial_chambers/reward");

	private ManuscriptLoot() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			Identifier id = key.getValue();

			int[] range = TEMPERING.get(id);
			if (range != null) {
				builder.pool(countPool(CodexItems.TEMPERING_MANUSCRIPT, range[0], range[1]));
				return;
			}
			if (id.equals(ANCIENT_CITY)) {
				builder.pool(countPool(CodexItems.RITUAL_MANUSCRIPT, 1, 2));
			} else if (id.equals(VAULT_OMINOUS)) {
				builder.pool(chancePool(CodexItems.RITUAL_MANUSCRIPT, 3, 0.10f));
			} else if (id.equals(VAULT)) {
				builder.pool(chancePool(CodexItems.RITUAL_MANUSCRIPT, 1, 0.05f));
			}
		});
		Heirloom.LOGGER.info("[Loot] Manuscript chest/vault loot registered.");
	}

	private static LootPool.Builder countPool(Item item, int min, int max) {
		return LootPool.builder()
			.rolls(ConstantLootNumberProvider.create(1.0f))
			.with(ItemEntry.builder(item))
			.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(min, max)));
	}

	private static LootPool.Builder chancePool(Item item, int count, float chance) {
		return LootPool.builder()
			.rolls(ConstantLootNumberProvider.create(1.0f))
			.conditionally(RandomChanceLootCondition.builder(chance))
			.with(ItemEntry.builder(item))
			.apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(count)));
	}

	private static Identifier vanilla(String path) {
		return Identifier.ofVanilla(path);
	}
}

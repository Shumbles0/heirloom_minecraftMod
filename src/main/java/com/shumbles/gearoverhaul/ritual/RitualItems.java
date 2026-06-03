package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.TemperItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * The items for the Level-20 rituals — the chestplate's charge and the bow's clay targets.
 * Placeholders for now (no behaviour); see {@code docs/l20_rituals_design.md}.
 */
public final class RitualItems {
	public static final Item SUICIDE_CHARGE = register("suicide_charge");
	public static final Item CLAY_PIGEON = register("clay_pigeon");

	/** Every ritual item, for the creative tab. */
	public static final Item[] ALL = {SUICIDE_CHARGE, CLAY_PIGEON};

	private RitualItems() {
	}

	private static Item register(String name) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Heirloom.MOD_ID, name));
		return Registry.register(Registries.ITEM, key, new Item(new Item.Settings().registryKey(key)));
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(TemperItems.GROUP_KEY).register(entries -> {
			for (Item item : ALL) {
				entries.add(item);
			}
		});
		Heirloom.LOGGER.info("[Ritual] Registered {} ritual items.", ALL.length);
	}
}

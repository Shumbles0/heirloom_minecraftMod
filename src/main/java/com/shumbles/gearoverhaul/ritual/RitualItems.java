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

import java.util.function.Function;

/**
 * The items for the Level-20 rituals — the chestplate's charge and the bow's clay targets.
 * Placeholders for now (no behaviour); see {@code docs/l20_rituals_design.md}.
 */
public final class RitualItems {
	public static final Item SUICIDE_CHARGE = register("suicide_charge",
		key -> new SuicideChargeItem(new Item.Settings().registryKey(key)));
	public static final Item CLAY_PIGEON = register("clay_pigeon");
	public static final Item STRIKING_PLATE = register("striking_plate",
		key -> new StrikingPlateItem(new Item.Settings().registryKey(key)));

	/** Every ritual item, for the creative tab. */
	public static final Item[] ALL = {SUICIDE_CHARGE, CLAY_PIGEON, STRIKING_PLATE};

	private RitualItems() {
	}

	private static Item register(String name) {
		return register(name, key -> new Item(new Item.Settings().registryKey(key)));
	}

	private static Item register(String name, Function<RegistryKey<Item>, Item> factory) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Heirloom.MOD_ID, name));
		return Registry.register(Registries.ITEM, key, factory.apply(key));
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

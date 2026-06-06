package com.shumbles.gearoverhaul.enchant;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.TemperItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/** Items for the enchanting rework — currently just Arcane Dust, the base reagent for every brew. */
public final class EnchantItems {
	public static final Item ARCANE_DUST = register("arcane_dust");

	private EnchantItems() {
	}

	private static Item register(String name) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Heirloom.MOD_ID, name));
		return Registry.register(Registries.ITEM, key, new Item(new Item.Settings().registryKey(key)));
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(TemperItems.GROUP_KEY).register(entries -> entries.add(ARCANE_DUST));
		Heirloom.LOGGER.info("[Enchant] Registered enchant items.");
	}
}

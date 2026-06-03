package com.shumbles.gearoverhaul.armor;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.TemperItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * The early-game Stonewood armor set: four pieces, no tools. Its base armor values
 * mirror leather (and then get scaled to near-nothing by the gear nerf, like all armor),
 * so its real role is an early tempering substrate. Its one standout trait is a little
 * knockback resistance, which the nerf doesn't touch — so even at temper 0 it makes you
 * a bit harder to shove around.
 */
public final class StonewoodArmor {
	public static final RegistryKey<EquipmentAsset> ASSET =
		RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(Heirloom.MOD_ID, "stonewood"));

	public static final ArmorMaterial MATERIAL = new ArmorMaterial(
		5, // durability base — same as leather
		Map.of(
			EquipmentType.HELMET, 1,
			EquipmentType.CHESTPLATE, 3,
			EquipmentType.LEGGINGS, 2,
			EquipmentType.BOOTS, 1),
		15, // enchantability
		SoundEvents.ITEM_ARMOR_EQUIP_GENERIC,
		0.0f, // toughness
		0.05f, // knockback resistance per piece — the set's trait
		TagKey.of(RegistryKeys.ITEM, Identifier.of(Heirloom.MOD_ID, "stonewood_repair")),
		ASSET);

	public static final Item HELMET = register("stonewood_helmet", EquipmentType.HELMET);
	public static final Item CHESTPLATE = register("stonewood_chestplate", EquipmentType.CHESTPLATE);
	public static final Item LEGGINGS = register("stonewood_leggings", EquipmentType.LEGGINGS);
	public static final Item BOOTS = register("stonewood_boots", EquipmentType.BOOTS);

	private StonewoodArmor() {
	}

	private static Item register(String name, EquipmentType type) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Heirloom.MOD_ID, name));
		return Registry.register(Registries.ITEM, key, new Item(new Item.Settings().armor(MATERIAL, type).registryKey(key)));
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(TemperItems.GROUP_KEY).register(entries -> {
			entries.add(HELMET);
			entries.add(CHESTPLATE);
			entries.add(LEGGINGS);
			entries.add(BOOTS);
		});
		Heirloom.LOGGER.info("[Armor] Registered Stonewood armor set.");
	}
}

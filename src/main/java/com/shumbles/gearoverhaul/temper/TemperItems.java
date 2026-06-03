package com.shumbles.gearoverhaul.temper;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.block.HeirloomBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The five shared tempering reagents — the "fuel" of tempering, used by every weapon
 * and armor piece. Rarity climbs Dust → Flux → Alloy → Core → Quintessence; per-entry
 * recipes pair these with vanilla flavor materials that suit each gear piece.
 *
 * <p>Also registers a dedicated Heirloom creative tab to hold them (and future mod
 * items). Item registration must happen before registries freeze, so {@link #register()}
 * is called from the common mod initializer.
 */
public final class TemperItems {
	public static final Item TEMPERING_DUST = register("tempering_dust");
	public static final Item TEMPERING_FLUX = register("tempering_flux");
	public static final Item TEMPERING_ALLOY = register("tempering_alloy");
	public static final Item TEMPERING_CORE = register("tempering_core");
	public static final Item TEMPERING_QUINTESSENCE = register("tempering_quintessence");

	public static final RegistryKey<ItemGroup> GROUP_KEY =
		RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(Heirloom.MOD_ID, "heirloom"));

	private TemperItems() {
	}

	private static Item register(String name) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Heirloom.MOD_ID, name));
		return Registry.register(Registries.ITEM, key, new Item(new Item.Settings().registryKey(key)));
	}

	public static void register() {
		ItemGroup group = FabricItemGroup.builder()
			.icon(() -> new ItemStack(TEMPERING_CORE))
			.displayName(Text.translatable("itemGroup.heirloom"))
			.build();
		Registry.register(Registries.ITEM_GROUP, GROUP_KEY, group);

		ItemGroupEvents.modifyEntriesEvent(GROUP_KEY).register(entries -> {
			entries.add(HeirloomBlocks.TEMPERING_STATION);
			entries.add(TEMPERING_DUST);
			entries.add(TEMPERING_FLUX);
			entries.add(TEMPERING_ALLOY);
			entries.add(TEMPERING_CORE);
			entries.add(TEMPERING_QUINTESSENCE);
		});

		Heirloom.LOGGER.info("[Tempering] Registered 5 tempering reagents + creative tab.");
	}
}

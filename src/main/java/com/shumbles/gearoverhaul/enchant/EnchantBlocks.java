package com.shumbles.gearoverhaul.enchant;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.TemperItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/**
 * Blocks for the enchanting rework: the <b>Arcane Bookshelf</b> (ring an enchanting table with 15
 * of these to awaken it) and the <b>Advanced Enchanting Table</b> (the recoloured table you get
 * from that conversion — see {@link EnchantConversion}). The Advanced table's brewing GUI is a
 * later step; for now it exists, converts, and is placeable.
 */
public final class EnchantBlocks {
	public static final Block ARCANE_BOOKSHELF = register("arcane_bookshelf",
		key -> new Block(AbstractBlock.Settings.create().registryKey(key).strength(1.5f).sounds(BlockSoundGroup.WOOD)));
	public static final Block ADVANCED_ENCHANTING_TABLE = register("advanced_enchanting_table",
		key -> new AdvancedEnchantingTableBlock(AbstractBlock.Settings.create().registryKey(key)
			.strength(5.0f).sounds(BlockSoundGroup.STONE).nonOpaque()));

	/** Creative-tab listing. (The Advanced table is also obtainable by converting a vanilla table.) */
	public static final Block[] ALL = {ARCANE_BOOKSHELF, ADVANCED_ENCHANTING_TABLE};

	private EnchantBlocks() {
	}

	private static Block register(String name, Function<RegistryKey<Block>, Block> factory) {
		Identifier id = Identifier.of(Heirloom.MOD_ID, name);
		RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
		Block block = Registry.register(Registries.BLOCK, blockKey, factory.apply(blockKey));

		RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
		Registry.register(Registries.ITEM, itemKey,
			new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));
		return block;
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(TemperItems.GROUP_KEY).register(entries -> {
			for (Block block : ALL) {
				entries.add(block);
			}
		});
		Heirloom.LOGGER.info("[Enchant] Registered {} enchanting blocks.", ALL.length);
	}
}

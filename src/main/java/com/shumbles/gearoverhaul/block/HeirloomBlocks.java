package com.shumbles.gearoverhaul.block;

import com.shumbles.gearoverhaul.Heirloom;
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

/** Registers Heirloom's blocks and their block items. */
public final class HeirloomBlocks {
	public static final Block TEMPERING_STATION = register("tempering_station",
		key -> new TemperingStationBlock(AbstractBlock.Settings.create()
			.registryKey(key)
			.strength(2.5f)
			.sounds(BlockSoundGroup.WOOD)));

	private HeirloomBlocks() {
	}

	private static Block register(String name, Function<RegistryKey<Block>, Block> factory) {
		Identifier id = Identifier.of(Heirloom.MOD_ID, name);
		RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
		Block block = Registry.register(Registries.BLOCK, blockKey, factory.apply(blockKey));

		RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
		Registry.register(Registries.ITEM, itemKey, new BlockItem(block, new Item.Settings().registryKey(itemKey)));
		return block;
	}

	/** Touch this class so its static registration runs during mod init. */
	public static void register() {
		Heirloom.LOGGER.info("[Blocks] Registered Tempering Station.");
	}
}

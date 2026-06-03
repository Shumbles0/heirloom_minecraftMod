package com.shumbles.gearoverhaul.ritual;

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

/**
 * The blocks for the Level-20 rituals — registered, textured and placeable, but with <b>no
 * ritual behaviour yet</b>. Per-ritual detection (which ultimately calls {@link Rituals#finish})
 * is built separately; see {@code docs/l20_rituals_design.md}.
 */
public final class RitualBlocks {
	public static final Block RITUAL_FORGE = register("ritual_forge", BlockSoundGroup.STONE, 3.0f);
	public static final Block QUENCHING_TROUGH = register("quenching_trough", BlockSoundGroup.METAL, 3.0f);
	public static final Block CHOPPING_STUMP = register("chopping_stump", BlockSoundGroup.WOOD, 2.5f);
	public static final Block DEEPVEIN = register("deepvein", BlockSoundGroup.STONE, 3.5f);
	public static final Block AVALANCHE_CAIRN = register("avalanche_cairn", BlockSoundGroup.GRAVEL, 2.0f);
	public static final Block SKEET_LAUNCHER = register("skeet_launcher", BlockSoundGroup.WOOD, 2.5f);
	public static final Block BULLSEYE_TARGET = register("bullseye_target", BlockSoundGroup.WOOD, 2.0f);
	public static final Block STRIKING_PLATE = register("striking_plate", BlockSoundGroup.METAL, 3.5f);
	public static final Block PRESSURE_SEAL = register("pressure_seal", BlockSoundGroup.STONE, 3.0f);
	public static final Block FREEZE_MACHINE = register("freeze_machine", BlockSoundGroup.GLASS, 2.5f);
	public static final Block HEATBED = register("heatbed", BlockSoundGroup.STONE, 2.5f);

	/** Every ritual block, for the creative tab. */
	public static final Block[] ALL = {
		RITUAL_FORGE, QUENCHING_TROUGH, CHOPPING_STUMP, DEEPVEIN, AVALANCHE_CAIRN, SKEET_LAUNCHER,
		BULLSEYE_TARGET, STRIKING_PLATE, PRESSURE_SEAL, FREEZE_MACHINE, HEATBED
	};

	private RitualBlocks() {
	}

	private static Block register(String name, BlockSoundGroup sound, float strength) {
		Identifier id = Identifier.of(Heirloom.MOD_ID, name);
		RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
		Block block = Registry.register(Registries.BLOCK, blockKey,
			new Block(AbstractBlock.Settings.create().registryKey(blockKey).strength(strength).sounds(sound)));

		RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
		Registry.register(Registries.ITEM, itemKey, new BlockItem(block, new Item.Settings().registryKey(itemKey)));
		return block;
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(TemperItems.GROUP_KEY).register(entries -> {
			for (Block block : ALL) {
				entries.add(block);
			}
		});
		Heirloom.LOGGER.info("[Ritual] Registered {} ritual blocks.", ALL.length);
	}
}

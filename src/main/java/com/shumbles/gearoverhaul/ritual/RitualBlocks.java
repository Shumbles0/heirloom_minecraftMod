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

import java.util.function.Function;

/**
 * The blocks for the Level-20 rituals — registered, textured and placeable, but with <b>no
 * ritual behaviour yet</b>. Per-ritual detection (which ultimately calls {@link Rituals#finish})
 * is built separately; see {@code docs/l20_rituals_design.md}.
 */
public final class RitualBlocks {
	public static final Block RITUAL_FORGE = register("ritual_forge",
		key -> new RitualForgeBlock(AbstractBlock.Settings.create().registryKey(key).strength(3.0f).sounds(BlockSoundGroup.STONE)));
	public static final Block QUENCHING_TROUGH = register("quenching_trough",
		key -> new RitualTroughBlock(AbstractBlock.Settings.create().registryKey(key).strength(3.0f).sounds(BlockSoundGroup.METAL)));
	public static final Block CHOPPING_STUMP = register("chopping_stump", BlockSoundGroup.WOOD, 2.5f);
	public static final Block AVALANCHE_CAIRN = register("avalanche_cairn",
		key -> new AvalancheCairnBlock(AbstractBlock.Settings.create().registryKey(key).strength(2.0f).sounds(BlockSoundGroup.GRAVEL)));
	public static final Block SKEET_LAUNCHER = register("skeet_launcher",
		key -> new SkeetLauncherBlock(AbstractBlock.Settings.create().registryKey(key).strength(2.5f).sounds(BlockSoundGroup.WOOD)));
	public static final Block BULLSEYE_TARGET = register("bullseye_target", BlockSoundGroup.WOOD, 2.0f);
	// striking_plate is no longer a block — it's a spawner item (see RitualItems / StrikingPlateItem).
	public static final Block PRESSURE_SEAL = register("pressure_seal",
		key -> new PressureSealBlock(AbstractBlock.Settings.create().registryKey(key).strength(3.0f).sounds(BlockSoundGroup.STONE)));
	public static final Block FREEZE_MACHINE = register("freeze_machine",
		key -> new FreezeMachineBlock(AbstractBlock.Settings.create().registryKey(key).strength(2.5f).sounds(BlockSoundGroup.GLASS)));
	public static final Block HEATBED = register("heatbed", BlockSoundGroup.STONE, 2.5f);

	/** Every ritual block, for the creative tab. */
	public static final Block[] ALL = {
		RITUAL_FORGE, QUENCHING_TROUGH, CHOPPING_STUMP, AVALANCHE_CAIRN, SKEET_LAUNCHER,
		BULLSEYE_TARGET, PRESSURE_SEAL, FREEZE_MACHINE, HEATBED
	};

	private RitualBlocks() {
	}

	private static Block register(String name, BlockSoundGroup sound, float strength) {
		return register(name, key ->
			new Block(AbstractBlock.Settings.create().registryKey(key).strength(strength).sounds(sound)));
	}

	private static Block register(String name, Function<RegistryKey<Block>, Block> factory) {
		Identifier id = Identifier.of(Heirloom.MOD_ID, name);
		RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
		Block block = Registry.register(Registries.BLOCK, blockKey, factory.apply(blockKey));

		RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
		// useBlockPrefixedTranslationKey: a 1.21.11 BlockItem otherwise names itself from the
		// item key (item.heirloom.x); this points it at the block key (block.heirloom.x) the
		// lang file actually provides, matching vanilla blocks.
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
		Heirloom.LOGGER.info("[Ritual] Registered {} ritual blocks.", ALL.length);
	}
}

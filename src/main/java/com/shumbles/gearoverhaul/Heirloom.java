package com.shumbles.gearoverhaul;

import com.shumbles.gearoverhaul.armor.StonewoodArmor;
import com.shumbles.gearoverhaul.block.HeirloomBlockEntities;
import com.shumbles.gearoverhaul.block.HeirloomBlocks;
import com.shumbles.gearoverhaul.codex.CodexComponents;
import com.shumbles.gearoverhaul.codex.CodexItems;
import com.shumbles.gearoverhaul.enchant.EnchantBlocks;
import com.shumbles.gearoverhaul.enchant.EnchantComponents;
import com.shumbles.gearoverhaul.enchant.EnchantConversion;
import com.shumbles.gearoverhaul.enchant.EnchantItems;
import com.shumbles.gearoverhaul.foundation.GearNerf;
import com.shumbles.gearoverhaul.loot.ManuscriptDrops;
import com.shumbles.gearoverhaul.loot.ManuscriptLoot;
import com.shumbles.gearoverhaul.ritual.AxeRitual;
import com.shumbles.gearoverhaul.ritual.BootsRitual;
import com.shumbles.gearoverhaul.ritual.ChargeHandler;
import com.shumbles.gearoverhaul.ritual.HelmetRitual;
import com.shumbles.gearoverhaul.ritual.HotBladeHandler;
import com.shumbles.gearoverhaul.ritual.LeggingsRitual;
import com.shumbles.gearoverhaul.ritual.MaceRitual;
import com.shumbles.gearoverhaul.ritual.PickaxeRitual;
import com.shumbles.gearoverhaul.ritual.RitualBlocks;
import com.shumbles.gearoverhaul.ritual.RitualComponents;
import com.shumbles.gearoverhaul.ritual.RitualEntities;
import com.shumbles.gearoverhaul.ritual.RitualItems;
import com.shumbles.gearoverhaul.ritual.ShovelRitual;
import com.shumbles.gearoverhaul.screen.HeirloomScreenHandlers;
import com.shumbles.gearoverhaul.temper.TemperCommand;
import com.shumbles.gearoverhaul.temper.TemperComponents;
import com.shumbles.gearoverhaul.temper.TemperItems;
import com.shumbles.gearoverhaul.temper.data.TemperTableLoader;
import com.shumbles.gearoverhaul.usage.UsageComponents;
import com.shumbles.gearoverhaul.usage.UsageTracking;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Heirloom implements ModInitializer {
	public static final String MOD_ID = "heirloom";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Heirloom initializing.");

		// Foundation §1: nerf vanilla gear base stats. Tempering restores them.
		GearNerf.register();

		// Tempering: reagent items + creative tab.
		TemperItems.register();

		// Tempering Station: block, block entity, and menu.
		HeirloomBlocks.register();
		HeirloomBlockEntities.register();
		HeirloomScreenHandlers.register();

		// Codex: book + manuscripts + per-book unlock components.
		CodexComponents.register();
		CodexItems.register();

		// Early-game Stonewood armor set.
		StonewoodArmor.register();

		// Tempering: per-item level component + dev command (until the station exists).
		TemperComponents.register();
		TemperCommand.register();

		// Tempering: load the per-item/per-level material+buff tables from datapacks.
		TemperTableLoader.register();

		// Level-10 milestone: per-gear "use it" gate + the gameplay tracking that feeds it.
		UsageComponents.register();
		UsageTracking.register();

		// Level-20 rituals: blocks + items + per-item ritual state components.
		RitualComponents.register();
		RitualBlocks.register();
		RitualItems.register();
		RitualEntities.register();
		HotBladeHandler.register();
		MaceRitual.register();
		ChargeHandler.register();
		LeggingsRitual.register();
		BootsRitual.register();
		PickaxeRitual.register();
		AxeRitual.register();
		ShovelRitual.register();
		HelmetRitual.register();

		// Manuscript sources: rate-limited mob drops + chest/vault loot.
		ManuscriptDrops.register();
		ManuscriptLoot.register();

		// Enchanting rework (in progress): components, arcane bookshelves, the Advanced Enchanting
		// Table, and direction selection at the vanilla table.
		EnchantComponents.register();
		EnchantItems.register();
		EnchantBlocks.register();
		EnchantConversion.register();
	}
}
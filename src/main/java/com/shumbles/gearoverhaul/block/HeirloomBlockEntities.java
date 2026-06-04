package com.shumbles.gearoverhaul.block;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.ritual.RitualBlocks;
import com.shumbles.gearoverhaul.ritual.RitualForgeBlockEntity;
import com.shumbles.gearoverhaul.ritual.SkeetLauncherBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Registers Heirloom's block entity types. */
public final class HeirloomBlockEntities {
	public static final BlockEntityType<TemperingStationBlockEntity> TEMPERING_STATION =
		Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Heirloom.MOD_ID, "tempering_station"),
			FabricBlockEntityTypeBuilder.create(TemperingStationBlockEntity::new, HeirloomBlocks.TEMPERING_STATION).build());

	public static final BlockEntityType<RitualForgeBlockEntity> RITUAL_FORGE =
		Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Heirloom.MOD_ID, "ritual_forge"),
			FabricBlockEntityTypeBuilder.create(RitualForgeBlockEntity::new, RitualBlocks.RITUAL_FORGE).build());

	public static final BlockEntityType<SkeetLauncherBlockEntity> SKEET_LAUNCHER =
		Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(Heirloom.MOD_ID, "skeet_launcher"),
			FabricBlockEntityTypeBuilder.create(SkeetLauncherBlockEntity::new, RitualBlocks.SKEET_LAUNCHER).build());

	private HeirloomBlockEntities() {
	}

	public static void register() {
		Heirloom.LOGGER.info("[Blocks] Registered block entity types.");
	}
}

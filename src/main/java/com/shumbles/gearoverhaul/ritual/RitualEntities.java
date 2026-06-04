package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.Heirloom;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/** Registers the ritual entity types (the bow rite's clay pigeon). */
public final class RitualEntities {
	public static final EntityType<ClayPigeonEntity> CLAY_PIGEON = create();

	private RitualEntities() {
	}

	private static EntityType<ClayPigeonEntity> create() {
		Identifier id = Identifier.of(Heirloom.MOD_ID, "clay_pigeon");
		RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id);
		return Registry.register(Registries.ENTITY_TYPE, key,
			EntityType.Builder.<ClayPigeonEntity>create(ClayPigeonEntity::new, SpawnGroup.MISC)
				.dimensions(0.25f, 0.25f)
				.maxTrackingRange(8)
				.build(key));
	}

	public static void register() {
		Heirloom.LOGGER.info("[Ritual] Registered ritual entities.");
	}
}

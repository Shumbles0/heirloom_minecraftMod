package com.shumbles.gearoverhaul.temper;

import com.mojang.serialization.Codec;
import com.shumbles.gearoverhaul.Heirloom;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers Heirloom's custom item data components. For now just the temper level;
 * later additions (enchant loadout, ritual progress) will live here too.
 */
public final class TemperComponents {
	/**
	 * Per-item temper level. The codec range-checks {@code [0, MAX_TEMPER]} on load,
	 * so corrupt or out-of-range saved data can't push a level past the ceiling.
	 * Synced to clients via {@code VAR_INT} so the tooltip renders correctly.
	 */
	public static final ComponentType<Integer> TEMPER_LEVEL = Registry.register(
		Registries.DATA_COMPONENT_TYPE,
		Identifier.of(Heirloom.MOD_ID, "temper_level"),
		ComponentType.<Integer>builder()
			.codec(Codec.intRange(Tempering.MIN_TEMPER, Tempering.MAX_TEMPER))
			.packetCodec(PacketCodecs.VAR_INT)
			.build()
	);

	private TemperComponents() {
	}

	/** Touch this class so its static registration runs during mod init. */
	public static void register() {
		Heirloom.LOGGER.info("[Tempering] Registered temper_level component (max level {}).", Tempering.MAX_TEMPER);
	}
}

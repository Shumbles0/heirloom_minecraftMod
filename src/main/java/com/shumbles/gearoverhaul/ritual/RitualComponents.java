package com.shumbles.gearoverhaul.ritual;

import com.mojang.serialization.Codec;
import com.shumbles.gearoverhaul.Heirloom;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Transient item components used by the rituals. For the sword: when a heated blade is pulled
 * from the forge it carries the heat it was pulled at and the tick it was pulled, so the
 * quenching trough can judge whether it was pulled in the right band and quenched in time.
 * Both are cleared when the blade is quenched (success or fail).
 */
public final class RitualComponents {
	/** Heat (0..{@link SwordRitual#MAX_HEAT}) captured on a blade when pulled from the forge. */
	public static final ComponentType<Integer> FORGE_HEAT = register("forge_heat",
		ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

	/** World game-time tick the blade was pulled, for the quench window. */
	public static final ComponentType<Long> FORGE_PULL_TICK = register("forge_pull_tick",
		ComponentType.<Long>builder().codec(Codec.LONG).packetCodec(PacketCodecs.VAR_LONG).build());

	/**
	 * The distinct chunks a frozen pair of leggings has crossed during the cold-trek rite. Its
	 * presence means the leggings are frozen and trekking; its size is the progress toward 100.
	 */
	public static final ComponentType<List<Long>> FROZEN_CHUNKS = register("frozen_chunks",
		ComponentType.<List<Long>>builder()
			.codec(Codec.LONG.listOf())
			.packetCodec(PacketCodecs.VAR_LONG.collect(PacketCodecs.toList()))
			.build());

	private RitualComponents() {
	}

	private static <T> ComponentType<T> register(String name, ComponentType<T> type) {
		return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Heirloom.MOD_ID, name), type);
	}

	public static void register() {
		Heirloom.LOGGER.info("[Ritual] Registered ritual components.");
	}
}

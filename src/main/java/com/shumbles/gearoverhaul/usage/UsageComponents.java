package com.shumbles.gearoverhaul.usage;

import com.mojang.serialization.Codec;
import com.shumbles.gearoverhaul.Heirloom;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * The single integer "usage progress" stored on a gear stack, used by the Level-10
 * milestone gate. Its meaning depends on the gear type (kills, blocks, ticks, damage,
 * blocks travelled) — see {@link UsageGate}.
 */
public final class UsageComponents {
	public static final ComponentType<Integer> USAGE = Registry.register(
		Registries.DATA_COMPONENT_TYPE, Identifier.of(Heirloom.MOD_ID, "usage"),
		ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

	private UsageComponents() {
	}

	public static void register() {
		Heirloom.LOGGER.info("[Usage] Registered usage-progress component.");
	}
}

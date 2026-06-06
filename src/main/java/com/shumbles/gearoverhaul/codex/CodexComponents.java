package com.shumbles.gearoverhaul.codex;

import com.mojang.serialization.Codec;
import com.shumbles.gearoverhaul.Heirloom;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Per-Codex data: which tempering entries are unlocked, and which (if any) are pending
 * the player's choice. Both are lists of entry indices stored on the book itemstack, so
 * the Codex's knowledge travels with the book.
 */
public final class CodexComponents {
	public static final ComponentType<List<Integer>> UNLOCKED = register("codex_unlocked");
	public static final ComponentType<List<Integer>> PENDING = register("codex_pending");

	private CodexComponents() {
	}

	private static ComponentType<List<Integer>> register(String name) {
		return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Heirloom.MOD_ID, name),
			ComponentType.<List<Integer>>builder()
				.codec(Codec.INT.listOf())
				.packetCodec(PacketCodecs.VAR_INT.collect(PacketCodecs.toList()))
				.build());
	}

	public static List<Integer> getUnlocked(ItemStack codex) {
		return codex.getOrDefault(UNLOCKED, List.of());
	}

	public static List<Integer> getPending(ItemStack codex) {
		return codex.getOrDefault(PENDING, List.of());
	}

	/** The overview and the basics are always readable; everything else must be inscribed. */
	public static boolean isUnlocked(ItemStack codex, int index) {
		return index == CodexEntries.OVERVIEW || CodexEntries.isBasics(index) || getUnlocked(codex).contains(index);
	}

	/** Touch this class so its static registration runs during mod init. */
	public static void register() {
		Heirloom.LOGGER.info("[Codex] Registered codex components.");
	}
}

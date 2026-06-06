package com.shumbles.gearoverhaul.enchant;

import com.mojang.serialization.Codec;
import com.shumbles.gearoverhaul.Heirloom;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-item enchant state: up to {@link #MAX_SLOTS} slots, each a <b>direction</b> (chosen at an
 * ordinary enchanting table) and optionally an <b>attribute</b> (selected at the Advanced table).
 * Stored as a list of {@code "Direction"} or {@code "Direction|Attribute"} strings on the itemstack,
 * so it travels with the gear. Attribute <i>effects</i> aren't applied yet — this only records the
 * choice.
 */
public final class EnchantComponents {
	public static final int MAX_SLOTS = 3;
	private static final char SEP = '|';

	public static final ComponentType<List<String>> DIRECTIONS = register("enchant_directions");

	private EnchantComponents() {
	}

	private static ComponentType<List<String>> register(String name) {
		return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Heirloom.MOD_ID, name),
			ComponentType.<List<String>>builder()
				.codec(Codec.STRING.listOf())
				.packetCodec(PacketCodecs.STRING.collect(PacketCodecs.toList()))
				.build());
	}

	/** Raw encoded slots (each "Direction" or "Direction|Attribute"). */
	public static List<String> getSlots(ItemStack stack) {
		return stack.getOrDefault(DIRECTIONS, List.of());
	}

	public static int slotCount(ItemStack stack) {
		return getSlots(stack).size();
	}

	public static boolean hasFreeSlot(ItemStack stack) {
		return slotCount(stack) < MAX_SLOTS;
	}

	public static final int MAX_LEVEL = 3;

	/** Slot encoding split into [direction, attribute?, level?]. */
	private static String[] parts(ItemStack stack, int i) {
		return getSlots(stack).get(i).split("\\" + SEP);
	}

	/** The direction in slot {@code i}. */
	public static String directionAt(ItemStack stack, int i) {
		return parts(stack, i)[0];
	}

	/** The attribute in slot {@code i}, or {@code null} if none chosen yet. */
	public static String attributeAt(ItemStack stack, int i) {
		String[] p = parts(stack, i);
		return p.length >= 2 ? p[1] : null;
	}

	/** The level of slot {@code i}'s attribute (1 if set, 0 if no attribute yet). */
	public static int levelAt(ItemStack stack, int i) {
		String[] p = parts(stack, i);
		if (p.length >= 3) {
			try {
				return Integer.parseInt(p[2]);
			} catch (NumberFormatException ignored) {
				return 1;
			}
		}
		return p.length >= 2 ? 1 : 0;
	}

	/** Just the directions, slot order (for the table offer + eligibility). */
	public static List<String> getDirections(ItemStack stack) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < slotCount(stack); i++) {
			out.add(directionAt(stack, i));
		}
		return out;
	}

	/** Fills the next free slot with {@code direction}; no-op if full or already present. */
	public static boolean addDirection(ItemStack stack, String direction) {
		if (!hasFreeSlot(stack) || getDirections(stack).contains(direction)) {
			return false;
		}
		List<String> next = new ArrayList<>(getSlots(stack));
		next.add(direction);
		stack.set(DIRECTIONS, List.copyOf(next));
		return true;
	}

	/** Sets (or replaces) the attribute in slot {@code i} at level 1, keeping its direction. */
	public static boolean setAttribute(ItemStack stack, int i, String attribute) {
		List<String> slots = new ArrayList<>(getSlots(stack));
		if (i < 0 || i >= slots.size()) {
			return false;
		}
		slots.set(i, directionAt(stack, i) + SEP + attribute + SEP + 1);
		stack.set(DIRECTIONS, List.copyOf(slots));
		return true;
	}

	/** Sets the level of slot {@code i}'s attribute (no-op if the slot has no attribute). */
	public static boolean setLevel(ItemStack stack, int i, int level) {
		List<String> slots = new ArrayList<>(getSlots(stack));
		if (i < 0 || i >= slots.size()) {
			return false;
		}
		String attr = attributeAt(stack, i);
		if (attr == null) {
			return false;
		}
		slots.set(i, directionAt(stack, i) + SEP + attr + SEP + Math.max(1, Math.min(MAX_LEVEL, level)));
		stack.set(DIRECTIONS, List.copyOf(slots));
		return true;
	}

	/** Touch this class so the component registers during init. */
	public static void register() {
		Heirloom.LOGGER.info("[Enchant] Registered enchant components.");
	}
}

package com.shumbles.gearoverhaul.codex;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.temper.TemperItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/**
 * The Codex book and the three manuscript items. Only {@link #TEMPERING_MANUSCRIPT} is
 * functional for now (it feeds the tempering track); the ritual and enchanting
 * manuscripts exist as items but do nothing yet.
 */
public final class CodexItems {
	public static final Item CODEX = register("codex",
		key -> new CodexBook(new Item.Settings().registryKey(key).maxCount(1)));
	public static final Item TEMPERING_MANUSCRIPT = register("tempering_manuscript",
		key -> new Item(new Item.Settings().registryKey(key)));
	public static final Item RITUAL_MANUSCRIPT = register("ritual_manuscript",
		key -> new Item(new Item.Settings().registryKey(key)));
	public static final Item ENCHANTING_MANUSCRIPT = register("enchanting_manuscript",
		key -> new Item(new Item.Settings().registryKey(key)));

	private CodexItems() {
	}

	private static Item register(String name, Function<RegistryKey<Item>, Item> factory) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Heirloom.MOD_ID, name));
		return Registry.register(Registries.ITEM, key, factory.apply(key));
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(TemperItems.GROUP_KEY).register(entries -> {
			entries.add(CODEX);
			entries.add(TEMPERING_MANUSCRIPT);
			entries.add(RITUAL_MANUSCRIPT);
			entries.add(ENCHANTING_MANUSCRIPT);
		});
		Heirloom.LOGGER.info("[Codex] Registered codex + 3 manuscripts.");
	}

	/** Tempering manuscripts inscribe recipe chapters; the others are reserved for future systems. */
	public static boolean isTemperingManuscript(ItemStack stack) {
		return stack.isOf(TEMPERING_MANUSCRIPT);
	}

	/** Which Codex track a manuscript inscribes into. */
	public static CodexEntries.Track trackOf(ItemStack manuscript) {
		if (manuscript.isOf(RITUAL_MANUSCRIPT)) {
			return CodexEntries.Track.RITUAL;
		}
		if (manuscript.isOf(ENCHANTING_MANUSCRIPT)) {
			return CodexEntries.Track.ARCANE;
		}
		return CodexEntries.Track.TEMPERING;
	}

	public static boolean isManuscript(ItemStack stack) {
		return stack.isOf(TEMPERING_MANUSCRIPT) || stack.isOf(RITUAL_MANUSCRIPT) || stack.isOf(ENCHANTING_MANUSCRIPT);
	}

	/** Finds the player's Codex (hands first, then inventory), or EMPTY if none. */
	public static ItemStack findCodex(PlayerEntity player) {
		if (player.getMainHandStack().isOf(CODEX)) {
			return player.getMainHandStack();
		}
		if (player.getOffHandStack().isOf(CODEX)) {
			return player.getOffHandStack();
		}
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isOf(CODEX)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}
}

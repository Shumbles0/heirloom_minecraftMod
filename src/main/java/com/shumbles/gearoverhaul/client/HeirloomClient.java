package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.screen.HeirloomScreenHandlers;
import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Client-side setup. Tooltips are a client concern, so the temper-level line and the
 * item descriptions are appended here. This class is only ever loaded via the
 * {@code client} entrypoint, so its client-only references are never touched on a
 * dedicated server.
 */
public class HeirloomClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HandledScreens.register(HeirloomScreenHandlers.TEMPERING_STATION, TemperingStationScreen::new);
		HandledScreens.register(HeirloomScreenHandlers.CODEX, CodexScreen::new);

		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			// Per-item flavor description: shown for any Heirloom item that defines a
			// "tooltip.heirloom.<id>" lang key. Auto-covers current and future items.
			Identifier id = Registries.ITEM.getId(stack.getItem());
			if (id.getNamespace().equals(Heirloom.MOD_ID)) {
				String key = "tooltip." + Heirloom.MOD_ID + "." + id.getPath();
				if (I18n.hasTranslation(key)) {
					lines.add(Text.translatable(key).formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
				}
			}

			if (Tempering.isTempered(stack)) {
				int level = Tempering.getLevel(stack);
				boolean maxed = level >= Tempering.MAX_TEMPER;
				Text line = Text.literal("Temper " + level + " / " + Tempering.MAX_TEMPER);
				lines.add(maxed
					? line.copy().formatted(Formatting.GOLD, Formatting.BOLD)
					: line.copy().formatted(Formatting.AQUA));

				// Level-10 milestone progress, shown until the gear has earned it.
				UsageGate.Kind kind = UsageGate.kindOf(stack);
				if (kind != null && !UsageGate.isComplete(stack)) {
					lines.add(Text.literal("L10 " + kind.label + ": " + UsageGate.progress(stack) + " / " + kind.threshold)
						.formatted(Formatting.DARK_GRAY));
				}
			}
		});
	}
}

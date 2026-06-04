package com.shumbles.gearoverhaul.screen;

import com.shumbles.gearoverhaul.Heirloom;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

/** Registers Heirloom's screen handler types. */
public final class HeirloomScreenHandlers {
	public static final ScreenHandlerType<TemperingStationScreenHandler> TEMPERING_STATION =
		Registry.register(Registries.SCREEN_HANDLER, Identifier.of(Heirloom.MOD_ID, "tempering_station"),
			new ScreenHandlerType<>(TemperingStationScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

	public static final ScreenHandlerType<CodexScreenHandler> CODEX =
		Registry.register(Registries.SCREEN_HANDLER, Identifier.of(Heirloom.MOD_ID, "codex"),
			new ScreenHandlerType<>(CodexScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

	public static final ScreenHandlerType<SkeetLauncherScreenHandler> SKEET_LAUNCHER =
		Registry.register(Registries.SCREEN_HANDLER, Identifier.of(Heirloom.MOD_ID, "skeet_launcher"),
			new ScreenHandlerType<>(SkeetLauncherScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

	private HeirloomScreenHandlers() {
	}

	public static void register() {
		Heirloom.LOGGER.info("[Screens] Registered screen handler types.");
	}
}

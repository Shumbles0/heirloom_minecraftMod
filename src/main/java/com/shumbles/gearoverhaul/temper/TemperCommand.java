package com.shumbles.gearoverhaul.temper;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.shumbles.gearoverhaul.Heirloom;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Developer/testing command for the temper level, until the Tempering Station exists.
 *
 * <ul>
 *   <li>{@code /heirloom temper get} — print the held item's level</li>
 *   <li>{@code /heirloom temper set <0..25>} — set it</li>
 *   <li>{@code /heirloom temper add <delta>} — adjust it (clamped)</li>
 * </ul>
 *
 * Operates on the player's main-hand item. Must be run by a player. This is a dev
 * tool to be removed once the Tempering Station exists, so it is deliberately not
 * op-gated (1.21.11 reworked command permissions into a codec-based system that
 * isn't worth wiring up for throwaway tooling).
 */
public final class TemperCommand {
	private TemperCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal(Heirloom.MOD_ID)
				.requires(ServerCommandSource::isExecutedByPlayer)
				.then(literal("temper")
					.then(literal("get").executes(ctx -> report(ctx.getSource())))
					.then(literal("set")
						.then(argument("level", IntegerArgumentType.integer(Tempering.MIN_TEMPER, Tempering.MAX_TEMPER))
							.executes(ctx -> {
								ItemStack held = heldOrNull(ctx.getSource());
								if (held == null) {
									return 0;
								}
								Tempering.setLevel(held, IntegerArgumentType.getInteger(ctx, "level"));
								return report(ctx.getSource());
							})))
					.then(literal("add")
						.then(argument("delta", IntegerArgumentType.integer())
							.executes(ctx -> {
								ItemStack held = heldOrNull(ctx.getSource());
								if (held == null) {
									return 0;
								}
								Tempering.setLevel(held, Tempering.getLevel(held) + IntegerArgumentType.getInteger(ctx, "delta"));
								return report(ctx.getSource());
							}))))));
	}

	private static int report(ServerCommandSource source) {
		ItemStack held = heldOrNull(source);
		if (held == null) {
			return 0;
		}
		int level = Tempering.getLevel(held);
		source.sendFeedback(() -> Text.literal("Temper level: " + level + " / " + Tempering.MAX_TEMPER), false);
		return level;
	}

	/** The source player's main-hand stack, or null (with feedback) if unavailable. */
	private static ItemStack heldOrNull(ServerCommandSource source) {
		if (source.getEntity() instanceof PlayerEntity player) {
			ItemStack held = player.getMainHandStack();
			if (!held.isEmpty()) {
				return held;
			}
		}
		source.sendError(Text.literal("Hold an item in your main hand first."));
		return null;
	}
}

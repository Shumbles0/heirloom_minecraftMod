package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.enchant.ArcaneAttribute;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side text for the Arcane codex entries: the forced-first <b>Directions Index</b> (how
 * enchanting works, plus every direction and its attributes) and one page per {@link ArcaneAttribute}
 * (how to obtain it, its effect and drawback). Upgrade costs are added once the numbers are set.
 */
public final class ArcaneText {
	private ArcaneText() {
	}

	/** The Directions Index — the first arcane entry: how the system works + the full menu. */
	public static List<Text> directionsIndexLines() {
		List<Text> out = new ArrayList<>();
		out.add(Text.literal("Enchanting is remade. A piece no longer drinks raw power from a table — instead it takes "
			+ "up to three DIRECTIONS, and each direction is shaped into one ATTRIBUTE: a focused effect bound to a drawback."));
		out.add(Text.literal(""));
		out.add(Text.literal("HOW. Choose a direction by enchanting the piece at an ordinary enchanting table — no special "
			+ "ring needed. Then bring it to an Advanced Enchanting Table (a normal table ringed with 15 Arcane "
			+ "Bookshelves): there you shape the slot with Arcane Dust and brew a selector to set its attribute. Upgrade "
			+ "reagents soften the drawback. Three slots per piece."));
		out.add(Text.literal(""));
		out.add(Text.literal("CAUTION. Drawbacks stack straight off the top — pile two halvings of one stat and it hits zero. "
			+ "A careless loadout can cripple you; there is no safety net."));
		out.add(Text.literal(""));
		out.add(Text.literal("THE DIRECTIONS:"));
		String dir = null;
		StringBuilder line = null;
		for (ArcaneAttribute a : ArcaneAttribute.values()) {
			if (!a.direction.equals(dir)) {
				if (line != null) {
					out.add(Text.literal(line.toString()));
				}
				dir = a.direction;
				line = new StringBuilder(dir + ": " + a.displayName);
			} else {
				line.append(", ").append(a.displayName);
			}
		}
		if (line != null) {
			out.add(Text.literal(line.toString()));
		}
		return out;
	}

	/** One attribute's page: how to get it, what it does, and its drawback. */
	public static List<Text> attributeLines(ArcaneAttribute a) {
		List<Text> out = new ArrayList<>();
		out.add(Text.literal(a.displayName + "  —  " + a.direction));
		out.add(Text.literal(""));
		out.add(Text.literal("How to get it: shape a " + a.direction + " slot with Arcane Dust, then brew " + a.selector + "."));
		out.add(Text.literal(""));
		out.add(Text.literal("Effect: " + a.effect));
		out.add(Text.literal("Drawback: " + a.drawback));
		out.add(Text.literal(""));
		out.add(Text.literal("Upgrades: recorded once their costs are set."));
		return out;
	}
}

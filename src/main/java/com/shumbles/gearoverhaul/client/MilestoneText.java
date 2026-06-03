package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * All player-facing text for the Level-10 milestones, in one place.
 *
 * <p>Two registers, deliberately split:
 * <ul>
 *   <li><b>In-game</b> (tooltip + Tempering Station) — a <i>vague, lore-flavoured</i> hint
 *       plus a word-only sense of how close the gear is ({@link #hint}, {@link #stage}).
 *       No numbers: the player is nudged toward the deed without being handed the recipe.</li>
 *   <li><b>Codex</b> — the <i>exact</i> condition, numbers and reset rules and all
 *       ({@link #codexLines}), revealed only once the milestone entry is inscribed.</li>
 * </ul>
 * Client-side only; it is referenced from the tooltip, the station screen, and the Codex.
 */
public final class MilestoneText {
	private MilestoneText() {
	}

	/** Vague, lore-flavoured nudge shown in tooltips and the station while the milestone is unmet. */
	public static String hint(UsageGate.Kind kind) {
		return switch (kind) {
			case SWORD -> "This blade remembers battle, but has not yet fought enough to recall its edge.";
			case AXE -> "The axe still dreams of forests it has not yet felled.";
			case PICKAXE -> "The pick has not yet bitten deep enough to know the weight of true ore.";
			case SHOVEL -> "The shovel has not yet moved earth enough to settle into your grip.";
			case BOW -> "The bow has loosed too few arrows across too narrow a sky.";
			case CROSSBOW -> "The crossbow's bolts have not yet found their mark often enough.";
			case TRIDENT -> "The trident longs for the throw, and has not yet flown true enough.";
			case MACE -> "The mace has not yet fallen from on high with the weight it craves.";
			case HELMET -> "The helm has not yet held its breath beneath the dark for long enough.";
			case CHESTPLATE -> "The chestplate has not yet taken enough blows meant for you.";
			case LEGGINGS -> "These greaves have not yet carried you down enough long roads.";
			case BOOTS -> "The boots have not yet caught you from a fall worth surviving.";
		};
	}

	/** Word-only sense of progress (no numbers), from the raw progress and the kind's threshold. */
	public static String stage(UsageGate.Kind kind, int progress) {
		float frac = kind.threshold <= 0 ? 1.0f : (float) progress / kind.threshold;
		if (frac <= 0.0f) {
			return "untested";
		}
		if (frac < 0.34f) {
			return "stirring";
		}
		if (frac < 0.67f) {
			return "proving";
		}
		if (frac < 1.0f) {
			return "nearly proven";
		}
		return "proven";
	}

	/** Full, precise Codex entry text for a milestone — lore framing plus the exact deed. */
	public static List<Text> codexLines(UsageGate.Kind kind) {
		List<Text> out = new ArrayList<>();
		out.add(Text.literal(intro(kind)));
		out.add(Text.literal(""));
		out.add(Text.literal("The deed: " + condition(kind)));
		out.add(Text.literal(""));
		out.add(Text.literal("Only tempered gear earns this proving, and only the very piece in your hand — "
			+ "the deed belongs to the gear, not the wielder. Once proven, the tenth seal opens and tempering may "
			+ "continue."));
		return out;
	}

	/** Lore lead-in: why this kind of gear is proven the way it is. */
	private static String intro(UsageGate.Kind kind) {
		return switch (kind) {
			case SWORD -> "A sword is proven not by a single kill, but by the unbroken rhythm of a fight survived whole.";
			case AXE -> "An axe earns its temper through honest, repetitive labour — the steady fall of timber.";
			case PICKAXE -> "A pick proves itself in the deep, where only worthy stone is counted worth the swing.";
			case SHOVEL -> "A shovel is proven by the sheer weight of earth it has turned aside.";
			case BOW -> "A bow is proven by distance, not by ease — the far shot that few could make.";
			case CROSSBOW -> "A crossbow asks for nothing clever, only a steady aim repeated until it is true.";
			case TRIDENT -> "A trident yearns to leave the hand. It is proven only in flight, never in the grip.";
			case MACE -> "A mace is proven by gravity itself — the blow delivered from a falling height.";
			case HELMET -> "A helm is proven in the airless dark, where lesser plate would let you drown.";
			case CHESTPLATE -> "A chestplate earns its temper the hard way: by standing between you and harm, and holding.";
			case LEGGINGS -> "Greaves are proven by the road — the long miles walked, never ridden.";
			case BOOTS -> "Boots are proven in a single terrible moment: the fall that ought to have ended you.";
		};
	}

	/** The exact condition sentence (with numbers and reset rules) for the Codex. */
	private static String condition(UsageGate.Kind kind) {
		return switch (kind) {
			case SWORD -> "Slay 20 foes in one unbroken streak. Take any damage and the tally returns to nothing.";
			case AXE -> "Fell 200 logs.";
			case PICKAXE -> "Mine 20 iron-grade or richer ores in a row. Coal or copper ore breaks the run.";
			case SHOVEL -> "Dig 200 blocks of earth — soil, sand, gravel and the like.";
			case BOW -> "Slay 10 foes from more than 10 blocks away.";
			case CROSSBOW -> "Land 10 bolt hits, at any range.";
			case TRIDENT -> "Slay 5 foes with a thrown trident.";
			case MACE -> "Strike 50 blows while falling.";
			case HELMET -> "Remain submerged in water for 300 seconds in all.";
			case CHESTPLATE -> "Absorb 200 points of damage without dying. Should you fall, the tally falls with you.";
			case LEGGINGS -> "Travel 2800 blocks on foot.";
			case BOOTS -> "Survive a fall that leaves you at half a heart or less.";
		};
	}
}

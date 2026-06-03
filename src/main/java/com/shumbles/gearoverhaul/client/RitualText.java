package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Codex text for the Level-20 rituals — one entry per gear-type, unlocked from Ritual
 * Manuscripts. Each describes the rite the piece must undergo to cross from temper 19 into
 * the t21–25 endgame. (Mechanics are built separately; this is the player-facing account.)
 *
 * <p>Client-side only; consumed by {@link CodexContent} for the Codex reading view.
 */
public final class RitualText {
	private RitualText() {
	}

	/** Full Codex entry for a gear-type's ritual: a lore lead-in plus the rite itself. */
	public static List<Text> codexLines(UsageGate.Kind kind) {
		List<Text> out = new ArrayList<>();
		out.add(Text.literal(intro(kind)));
		out.add(Text.literal(""));
		out.add(Text.literal("The rite: " + rite(kind)));
		out.add(Text.literal(""));
		out.add(Text.literal("Performed on a piece tempered to 19, the rite carries it to 20 and opens the final "
			+ "five levels. Botch it and the work merely slips a level — the piece is never lost."));
		return out;
	}

	private static String intro(UsageGate.Kind kind) {
		return switch (kind) {
			case SWORD -> "A blade is finished the old way: brought to heat, then drunk cold in a single breath.";
			case AXE -> "An axe is sealed by labour kept in time — the swing that lands true on the beat.";
			case PICKAXE -> "A pick is set by the chase — the vein that flees deeper the moment you bite.";
			case SHOVEL -> "A shovel is proven against the earth's own weight, when it would bury you whole.";
			case BOW -> "A bow is sworn to the moving mark — what falls from the sky must not reach the ground.";
			case CROSSBOW -> "A crossbow is sworn to distance — the restored bolt flies flat where a lesser one would fall.";
			case TRIDENT -> "A trident remembers the storm, and the storm answers when it is held high enough.";
			case MACE -> "A mace is bound to the air between blows — three falls, and the ground never touched.";
			case HELMET -> "A helm is sealed in the deep open water, far from any wall, where the pressure can test it.";
			case CHESTPLATE -> "A chestplate is proven in one reckless instant: the blast you set against your own chest.";
			case LEGGINGS -> "Greaves are bound to the cold and the long road — frozen, then thawed across new country.";
			case BOOTS -> "Boots are bound to the burning path walked unbroken, sole never leaving the heat.";
		};
	}

	private static String rite(UsageGate.Kind kind) {
		return switch (kind) {
			case SWORD -> "Heat the blade in the forge to the right glow, then quench it within the narrow window. "
				+ "Too hot or too slow and you start again.";
			case AXE -> "Strike the log on the stump in time with the beat. An off-beat blow resets the run.";
			case PICKAXE -> "Mine the branching vein it exposes, clearing every block before it reseals itself.";
			case SHOVEL -> "Trigger the collapse and dig yourself free of the falling earth before it smothers you.";
			case BOW -> "Shoot the launched clay targets out of the air — lead them; do not let them land.";
			case CROSSBOW -> "Strike the distant bullseye from far across the range — only restored velocity can reach it.";
			case TRIDENT -> "In a thunderstorm, throw the trident high above a lightning rod and let the bolt take it.";
			case MACE -> "Slam three separate striking plates in a row, each launching you to the next, never landing between.";
			case HELMET -> "Seal the helm in deep open water, clear of blocks all around, and stay close as it holds for a time.";
			case CHESTPLATE -> "Set the charge, wear it, and survive the point-blank blast. Lesser plate will not save you.";
			case LEGGINGS -> "Freeze them at the machine, then cross 100 fresh chunks on foot — any fall damage undoes it.";
			case BOOTS -> "Walk the heat-bed end to end without once stepping off onto cooler ground.";
		};
	}
}

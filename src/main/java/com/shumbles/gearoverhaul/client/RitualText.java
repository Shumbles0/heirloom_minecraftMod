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
			case SWORD -> "A blade is finished the old way: brought to white-hot, then drowned cold in a single breath.";
			case AXE -> "An axe is sealed by labour kept in time — the swing that lands true on the beat.";
			case PICKAXE -> "A pick is set by the chase — the vein that flees deeper the moment you bite.";
			case SHOVEL -> "A shovel is proven against the earth's own weight, when it would bury you whole.";
			case BOW -> "A bow is sworn to the moving mark — what falls from the sky must not reach the ground.";
			case CROSSBOW -> "A crossbow is sworn to distance — the restored bolt flies flat where a lesser one would fall.";
			case TRIDENT -> "A trident remembers the storm, and the storm answers when it is held high enough.";
			case MACE -> "A mace is bound to the air between blows — three strikes, and the ground never touched.";
			case HELMET -> "A helm is sealed in the deep open water, far from any wall, where the pressure can test it.";
			case CHESTPLATE -> "A chestplate is proven in one reckless instant: the blast you set against your own chest.";
			case LEGGINGS -> "Greaves are bound to the cold and the long road — frozen, then thawed across new country.";
			case BOOTS -> "Boots are bound to the burning path walked unbroken, sole never leaving the heat.";
		};
	}

	private static String rite(UsageGate.Kind kind) {
		return switch (kind) {
			case SWORD -> "Right-click the Ritual Forge with a sword tempered to 19 to set it in the coals; the "
				+ "forge names each heat as it climbs — warming, dull red, cherry, orange, and at about thirty seconds "
				+ "white-hot. Right-click again to pull the blade, then right-click the Quenching Trough within about "
				+ "five seconds. Pull it while white-hot to finish the rite. Pull too early, or let it sear hotter past "
				+ "white-hot, and the quench holds nothing. Leave it to melting — about forty seconds — and pulling it "
				+ "costs the blade a temper level outright. A pulled blade scorches your hand until you quench it, so be quick.";
			case AXE -> "Strike the log on the stump in time with the beat. An off-beat blow resets the run.";
			case PICKAXE -> "Mine the branching vein it exposes, clearing every block before it reseals itself.";
			case SHOVEL -> "Trigger the collapse and dig yourself free of the falling earth before it smothers you.";
			case BOW -> "Load clay pigeons into a Skeet Launcher and feed it a steady redstone signal; while powered "
				+ "it flings one high into the air every couple of seconds. With a tempered bow, shoot five from the sky "
				+ "in a row. Let even one fall and shatter on the ground, and the streak begins again.";
			case CROSSBOW -> "Strike the distant bullseye from far across the range — only restored velocity can reach it.";
			case TRIDENT -> "During a thunderstorm, throw a tempered trident so it flies at least fifteen blocks "
				+ "above a lightning rod, in the rod's own column. Each such throw may draw the bolt: lightning strikes "
				+ "the trident, it hangs frozen in the air a moment, then falls. Catch the strike and the rite is done — "
				+ "keep throwing through the storm until the sky answers.";
			case MACE -> "Set out three Striking Plate targets up in the air. With a tempered mace, drop onto the "
				+ "first from high above — some fifty blocks — and strike it as you fall; the blow flings you back "
				+ "skyward. Steer to the next and strike it, then a third, hitting three different targets in a row "
				+ "without ever touching the ground. Land anywhere, or strike the same target twice, and the run resets. "
				+ "No plate softens your descent — you will always take the fall damage your plunges earn, so come ready.";
			case HELMET -> "Seal the helm in deep open water, clear of blocks all around, and stay close as it holds for a time.";
			case CHESTPLATE -> "Wear a chestplate tempered to 19 and set off a Suicide Charge in hand. A five-second "
				+ "fuse counts down in quickening beeps, then it blasts you to the very brink — half a heart, no matter "
				+ "how fine your armour — but no further. Endure it and the rite is done.";
			case LEGGINGS -> "Freeze them at the machine, then cross 100 fresh chunks on foot — any fall damage undoes it.";
			case BOOTS -> "Walk the heat-bed end to end without once stepping off onto cooler ground.";
		};
	}
}

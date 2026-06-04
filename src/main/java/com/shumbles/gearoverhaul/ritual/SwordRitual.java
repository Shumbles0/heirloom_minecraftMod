package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.item.ItemStack;

/**
 * Tuning + helpers for the sword's Level-20 rite (forge &amp; quench).
 *
 * <p>The forge heats a blade one point per tick to <b>white-hot</b> at ~30s ({@link #WHITEHOT_MIN}) —
 * the only band that finishes the rite. Held longer it merely <b>sears</b> hotter, which looks ready
 * but is no longer the right band (a trap: quenching then just fails). At ~40s it turns
 * <b>melting hot</b> ({@link #MELTING}), and a blade quenched while melting <b>loses a temper level</b>.
 * A pulled blade scorches the holder until quenched (or until it cools after {@link #COOL_TIME}).
 *
 * <p>All numbers are in ticks (20/s) and free to retune.
 */
public final class SwordRitual {
	public static final int WHITEHOT_MIN = 600; // ~30s — the target band opens
	public static final int WHITEHOT_MAX = 615; // ~30.75s — band closes (tight pull window)
	public static final int MELTING = 800;      // ~40s — the blade starts to melt
	public static final int MAX_HEAT = MELTING; // heat caps here; a melting blade just waits to be pulled

	public static final int QUENCH_WINDOW = 100; // ticks (~5s) from pull to a successful quench
	public static final int COOL_TIME = 160;     // ticks (~8s) after pull until a held blade cools
	public static final int BURN_INTERVAL = 15;  // ticks between scorch-damage ticks
	public static final float BURN_DAMAGE = 1.0f; // half a heart per burn tick

	/** Minimum heat for each named stage above "Cold" (index-aligned to {@link #STAGE_NAME}). */
	public static final int[] STAGE_HEAT = {1, 150, 300, 450, 600, 616, MELTING};
	public static final String[] STAGE_NAME =
		{"Warming", "Dull red", "Cherry red", "Orange-hot", "White-hot", "Searing", "Melting hot"};

	private SwordRitual() {
	}

	/** Only a sword tempered to exactly the ritual level may enter the forge. */
	public static boolean isEligible(ItemStack stack) {
		return UsageGate.kindOf(stack) == UsageGate.Kind.SWORD
			&& Tempering.getLevel(stack) == Rituals.RITUAL_FROM;
	}

	/** The success band: pulled while white-hot. */
	public static boolean inWhiteHot(int heat) {
		return heat >= WHITEHOT_MIN && heat <= WHITEHOT_MAX;
	}

	/** Past searing and into melting — quenching here costs the blade a temper level. */
	public static boolean isMelting(int heat) {
		return heat >= MELTING;
	}

	/** Index into {@link #STAGE_NAME} for the current heat, or {@code -1} for "Cold". */
	public static int stageIndex(int heat) {
		int idx = -1;
		for (int i = 0; i < STAGE_HEAT.length; i++) {
			if (heat >= STAGE_HEAT[i]) {
				idx = i;
			}
		}
		return idx;
	}

	public static String stageName(int heat) {
		int idx = stageIndex(heat);
		return idx < 0 ? "Cold" : STAGE_NAME[idx];
	}
}

package com.shumbles.gearoverhaul.special;

import com.shumbles.gearoverhaul.temper.Tempering;
import net.minecraft.item.ItemStack;

/**
 * The shared restore curve for special gear (bow, crossbow, trident, mace).
 *
 * <p>Unlike ordinary gear, the special items' signature effects are <i>not</i> plain
 * attribute modifiers — bow draw speed, crossbow arrow velocity, trident throw damage,
 * and the mace's fall-smash bonus are computed in code by the vanilla mechanics. The
 * mixin layer reads an item's temper level and multiplies those effects by the fraction
 * this class returns, so they climb from a deliberate floor back to vanilla.
 *
 * <p>The curve mirrors the rest of the mod: a low floor at temper 0, {@code 40%} restored
 * by temper 10, and a full {@code 100%} (vanilla) by temper 25 — two straight segments.
 * There is <b>no</b> overshoot: special gear caps at vanilla, and the level-20 ritual is a
 * pure progression gate that adds nothing here. Each effect supplies its own floor (e.g.
 * the bow's draw speed floors at 1/3 = "3× slower", the trident's throw at 20%).
 */
public final class SpecialScaling {
	/** Temper at which 40% of the effect is restored — the curve's mid knee. */
	private static final float KNEE_LEVEL = 10.0f;
	/** Restore fraction at the knee. */
	private static final float KNEE_FRACTION = 0.40f;

	/** Bow draw-speed floor: 1/3 of vanilla, i.e. drawing is 3× slower at temper 0. */
	public static final float BOW_DRAW_FLOOR = 1.0f / 3.0f;
	/** Crossbow arrow-velocity floor at temper 0 (lower velocity → less projectile damage). */
	public static final float CROSSBOW_VELOCITY_FLOOR = 0.33f;
	/** Trident thrown-damage floor at temper 0. */
	public static final float TRIDENT_THROW_FLOOR = 0.20f;
	/** Mace fall-smash bonus floor at temper 0. */
	public static final float MACE_SMASH_FLOOR = 0.20f;

	private SpecialScaling() {
	}

	/**
	 * Restore fraction for an effect at the given temper level.
	 *
	 * @param level the item's temper level (clamped to the valid range)
	 * @param floor the fraction of vanilla retained at temper 0 (e.g. 0.20 = 20%)
	 * @return floor at level 0, rising linearly to {@value #KNEE_FRACTION} at level
	 *         {@value #KNEE_LEVEL}, then to 1.0 (vanilla) at {@link Tempering#MAX_TEMPER}
	 */
	public static float restoreFraction(int level, float floor) {
		if (level <= Tempering.MIN_TEMPER) {
			return floor;
		}
		if (level >= Tempering.MAX_TEMPER) {
			return 1.0f;
		}
		if (level <= KNEE_LEVEL) {
			return floor + (KNEE_FRACTION - floor) * (level / KNEE_LEVEL);
		}
		float span = Tempering.MAX_TEMPER - KNEE_LEVEL;
		return KNEE_FRACTION + (1.0f - KNEE_FRACTION) * ((level - KNEE_LEVEL) / span);
	}

	/** Convenience: restore fraction for the temper level baked on {@code stack}. */
	public static float restoreFraction(ItemStack stack, float floor) {
		return restoreFraction(Tempering.getLevel(stack), floor);
	}
}

package com.shumbles.gearoverhaul.ritual;

import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.item.ItemStack;

/**
 * Tuning + eligibility for the bow's Level-20 rite (skeet): a Skeet Launcher fires clay pigeons
 * while powered, and the archer must shoot {@link #REQUIRED} in a row with a tempered bow. Missing
 * one (it shatters on the ground) breaks the streak.
 */
public final class BowRitual {
	public static final int REQUIRED = 5;
	public static final int FIRE_INTERVAL = 40;      // ticks between launches (~2s)
	public static final double PIGEON_LAUNCH_VY = 1.0; // upward velocity (~15 blocks)

	private BowRitual() {
	}

	public static boolean isEligibleBow(ItemStack stack) {
		return UsageGate.kindOf(stack) == UsageGate.Kind.BOW
			&& Tempering.getLevel(stack) == Rituals.RITUAL_FROM;
	}
}

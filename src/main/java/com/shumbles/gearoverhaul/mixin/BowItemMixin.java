package com.shumbles.gearoverhaul.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.shumbles.gearoverhaul.special.SpecialScaling;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Slows the bow's draw by the bow's temper level. The drawn time is mapped to pull
 * progress via {@code getPullProgress(int)}; we scale the elapsed ticks fed into it by
 * the draw-speed fraction, so a temper-0 bow needs ~3× as long
 * ({@link SpecialScaling#BOW_DRAW_FLOOR}) to reach full draw, returning to vanilla by
 * temper 25. Damage at a given pull progress is untouched — only the time to reach it.
 */
@Mixin(BowItem.class)
public class BowItemMixin {
	@ModifyArg(
		method = "onStoppedUsing",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/item/BowItem;getPullProgress(I)F"),
		index = 0)
	private int heirloom$slowDraw(int useTicks, @Local(argsOnly = true) ItemStack stack) {
		float drawSpeed = SpecialScaling.restoreFraction(stack, SpecialScaling.BOW_DRAW_FLOOR);
		return Math.round(useTicks * drawSpeed);
	}
}

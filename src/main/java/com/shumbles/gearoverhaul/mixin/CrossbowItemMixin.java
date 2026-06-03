package com.shumbles.gearoverhaul.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.shumbles.gearoverhaul.special.SpecialScaling;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Scales a fired crossbow's projectile velocity by the crossbow's temper level, flooring
 * it at {@link SpecialScaling#CROSSBOW_VELOCITY_FLOOR} and restoring to vanilla by temper
 * 25. Lower launch velocity means the arrow lands softer, reducing its damage without
 * touching the arrow item itself.
 */
@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
	@ModifyVariable(
		method = "shootAll(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;FFLnet/minecraft/entity/LivingEntity;)V",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 0)
	private float heirloom$scaleVelocity(float speed, @Local(argsOnly = true) ItemStack stack) {
		return speed * SpecialScaling.restoreFraction(stack, SpecialScaling.CROSSBOW_VELOCITY_FLOOR);
	}
}

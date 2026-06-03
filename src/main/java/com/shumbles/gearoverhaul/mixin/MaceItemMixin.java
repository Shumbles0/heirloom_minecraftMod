package com.shumbles.gearoverhaul.mixin;

import com.shumbles.gearoverhaul.special.SpecialScaling;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scales the mace's fall-smash bonus damage by the wielder's temper level. The base
 * melee damage is a plain attribute (nerfed + restored elsewhere); this only touches
 * the extra damage gained from falling, flooring it at {@link SpecialScaling#MACE_SMASH_FLOOR}
 * and restoring to vanilla by temper 25.
 */
@Mixin(MaceItem.class)
public class MaceItemMixin {
	@Inject(
		method = "getBonusAttackDamage(Lnet/minecraft/entity/Entity;FLnet/minecraft/entity/damage/DamageSource;)F",
		at = @At("RETURN"),
		cancellable = true)
	private void heirloom$scaleSmash(Entity target, float baseDamage, DamageSource source,
									 CallbackInfoReturnable<Float> cir) {
		float bonus = cir.getReturnValueF();
		if (bonus <= 0.0f) {
			return;
		}
		ItemStack weapon = source.getWeaponStack();
		if (weapon == null || weapon.isEmpty()) {
			return;
		}
		float fraction = SpecialScaling.restoreFraction(weapon, SpecialScaling.MACE_SMASH_FLOOR);
		if (fraction < 1.0f) {
			cir.setReturnValue(bonus * fraction);
		}
	}
}

package com.shumbles.gearoverhaul.mixin;

import com.shumbles.gearoverhaul.special.SpecialScaling;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Scales a thrown trident's impact damage by the trident's temper level, flooring it at
 * {@link SpecialScaling#TRIDENT_THROW_FLOOR} and restoring to vanilla by temper 25. The
 * trident's melee (held) damage is a plain attribute handled by the nerf + temper tables.
 */
@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin {
	@Shadow
	public abstract ItemStack getWeaponStack();

	@ModifyArg(
		method = "onEntityHit",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/Entity;sidedDamage(Lnet/minecraft/entity/damage/DamageSource;F)Z"),
		index = 1)
	private float heirloom$scaleThrow(float damage) {
		return damage * SpecialScaling.restoreFraction(getWeaponStack(), SpecialScaling.TRIDENT_THROW_FLOOR);
	}
}

package com.shumbles.gearoverhaul.mixin;

import com.shumbles.gearoverhaul.enchant.EnchantDirections;
import com.shumbles.gearoverhaul.enchant.EnchantEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the enchant <b>combat</b> attributes on a direct melee hit by a player whose main-hand
 * gear carries them: the damage delta (Slayer/Bane/Executioner/Piercing/Glass Edge/Concussion) and
 * the on-hit afflictions + durability costs (Withering/Crippling Blow/Concussion/Glass Edge).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float heirloom$modifyDamage(float amount, ServerWorld world, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		// Offense: the attacker's melee weapon adds/subtracts damage.
		if (source.getAttacker() instanceof PlayerEntity attacker && source.getSource() == attacker) {
			ItemStack weapon = attacker.getMainHandStack();
			if (EnchantDirections.canTakeDirections(weapon)) {
				amount += EnchantEffects.meleeDamageBonus(weapon, self, attacker);
			}
		}
		// Defense: the victim's worn armor reduces typed damage (with a melee tradeoff).
		amount = EnchantEffects.applyDefense(amount, self, source);
		return Math.max(0.0f, amount);
	}

	@Inject(method = "damage", at = @At("RETURN"))
	private void heirloom$onHit(ServerWorld world, DamageSource source, float amount,
								CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			return; // the hit didn't land
		}
		LivingEntity self = (LivingEntity) (Object) this;
		// Attacker's on-hit afflictions.
		if (source.getAttacker() instanceof PlayerEntity attacker && source.getSource() == attacker) {
			ItemStack weapon = attacker.getMainHandStack();
			if (EnchantDirections.canTakeDirections(weapon)) {
				EnchantEffects.applyMeleeOnHit(world, weapon, self, attacker);
			}
		}
		// Victim's reactive defenses: Second Wind (if low) + Adrenal Surge (speed kick).
		if (self instanceof PlayerEntity victim) {
			EnchantEffects.secondWind(victim, world);
			EnchantEffects.adrenalSurge(world, victim);
		}
	}
}

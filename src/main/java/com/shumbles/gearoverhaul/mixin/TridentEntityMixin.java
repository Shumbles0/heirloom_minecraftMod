package com.shumbles.gearoverhaul.mixin;

import com.shumbles.gearoverhaul.ritual.TridentRitual;
import com.shumbles.gearoverhaul.special.SpecialScaling;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two trident behaviours:
 * <ul>
 *   <li>Scales a thrown trident's impact damage by its temper level (floor →
 *       {@link SpecialScaling#TRIDENT_THROW_FLOOR}, vanilla by temper 25).</li>
 *   <li>The Level-20 lightning-catch rite: while a tempered trident flies high above a lightning
 *       rod in a storm, it may draw a bolt and hang frozen a moment before falling.</li>
 * </ul>
 */
@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin {
	@Shadow
	public abstract ItemStack getWeaponStack();

	@Unique
	private boolean heirloom$rolled = false;
	@Unique
	private int heirloom$floatTicks = 0;

	@ModifyArg(
		method = "onEntityHit",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/Entity;sidedDamage(Lnet/minecraft/entity/damage/DamageSource;F)Z"),
		index = 1)
	private float heirloom$scaleThrow(float damage) {
		return damage * SpecialScaling.restoreFraction(getWeaponStack(), SpecialScaling.TRIDENT_THROW_FLOOR);
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void heirloom$lightningRite(CallbackInfo ci) {
		TridentEntity self = (TridentEntity) (Object) this;
		if (!(self.getEntityWorld() instanceof ServerWorld world)) {
			return;
		}

		// Hold the trident frozen for the float after a strike, then let it fall.
		if (heirloom$floatTicks > 0) {
			self.setVelocity(0.0, 0.0, 0.0);
			self.setNoGravity(true);
			if (--heirloom$floatTicks == 0) {
				self.setNoGravity(false);
			}
			ci.cancel();
			return;
		}

		if (heirloom$rolled || !world.isThundering() || !TridentRitual.isEligible(getWeaponStack())) {
			return;
		}
		BlockPos pos = self.getBlockPos();
		int rodY = TridentRitual.rodTopBelow(world, pos);
		if (rodY == Integer.MIN_VALUE || pos.getY() - rodY < TridentRitual.MIN_HEIGHT_ABOVE_ROD) {
			return;
		}

		// Qualified — one roll per throw.
		heirloom$rolled = true;
		if (world.getRandom().nextInt(TridentRitual.STRIKE_CHANCE_IN) == 0) {
			heirloom$floatTicks = TridentRitual.FLOAT_TICKS;
			self.setVelocity(0.0, 0.0, 0.0);
			self.setNoGravity(true);
			TridentRitual.strike(world, self, getWeaponStack());
			ci.cancel();
		}
	}
}

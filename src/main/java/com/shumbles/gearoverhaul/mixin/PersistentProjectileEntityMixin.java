package com.shumbles.gearoverhaul.mixin;

import com.shumbles.gearoverhaul.ritual.CrossbowRitual;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks an arrow striking a block so the crossbow's long-shot rite can judge bolts that hit a
 * Bullseye Target. Tridents are excluded (they have their own rite).
 */
@Mixin(PersistentProjectileEntity.class)
public class PersistentProjectileEntityMixin {
	@Inject(method = "onBlockHit(Lnet/minecraft/util/hit/BlockHitResult;)V", at = @At("HEAD"))
	private void heirloom$longShot(BlockHitResult blockHitResult, CallbackInfo ci) {
		if ((Object) this instanceof TridentEntity) {
			return;
		}
		PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
		if (self.getEntityWorld() instanceof ServerWorld world) {
			CrossbowRitual.tryLongShot(world, self, blockHitResult.getBlockPos());
		}
	}
}

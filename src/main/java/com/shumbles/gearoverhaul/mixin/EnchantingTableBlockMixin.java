package com.shumbles.gearoverhaul.mixin;

import com.shumbles.gearoverhaul.enchant.EnchantConversion;
import net.minecraft.block.BlockState;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Right-clicking an enchanting table <b>empty-handed</b> while it's ringed by 15 Arcane Bookshelves
 * awakens it into the Advanced table. Otherwise the vanilla GUI opens as normal — and is rerouted to
 * direction selection by {@link EnchantmentScreenHandlerMixin} for Heirloom gear.
 */
@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin {
	@Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
	private void heirloom$convert(BlockState state, World world, BlockPos pos, PlayerEntity player,
								  BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
		if (player.getMainHandStack().isEmpty() && EnchantConversion.ringComplete(world, pos)) {
			if (world instanceof ServerWorld serverWorld) {
				EnchantConversion.convert(serverWorld, pos);
			}
			cir.setReturnValue(ActionResult.SUCCESS);
		}
		// else: let the vanilla enchant GUI open (the handler mixin reroutes it).
	}
}

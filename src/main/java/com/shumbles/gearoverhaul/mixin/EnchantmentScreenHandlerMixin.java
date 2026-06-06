package com.shumbles.gearoverhaul.mixin;

import com.shumbles.gearoverhaul.enchant.EnchantComponents;
import com.shumbles.gearoverhaul.enchant.EnchantDirections;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Locale;

import net.minecraft.screen.EnchantmentScreenHandler;

/**
 * Reroutes the vanilla enchanting table for Heirloom gear: the three option rows become the gear's
 * available DIRECTIONS. The vanilla "clue" is pointed at small no-op {@code heirloom:dir_*}
 * enchantments so the hover shows the direction name natively (no client mixin), and clicking a row
 * writes the direction to the gear instead of enchanting it.
 */
@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerMixin {
	@Shadow @Final private Inventory inventory;
	@Shadow @Final private ScreenHandlerContext context;
	@Shadow @Final public int[] enchantmentPower;
	@Shadow @Final public int[] enchantmentId;
	@Shadow @Final public int[] enchantmentLevel;

	/** After vanilla fills the options, overwrite them with directions for Heirloom gear. */
	@Inject(method = "onContentChanged", at = @At("TAIL"))
	private void heirloom$showDirections(Inventory inv, CallbackInfo ci) {
		ItemStack gear = this.inventory.getStack(0);
		if (EnchantDirections.canTakeDirections(gear)) {
			heirloom$fillDirections(gear);
		}
	}

	/** Clicking a row applies the direction (and spends lapis + levels) instead of enchanting. */
	@Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
	private void heirloom$pickDirection(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
		ItemStack gear = this.inventory.getStack(0);
		if (!EnchantDirections.canTakeDirections(gear)) {
			return; // vanilla item → vanilla enchanting
		}
		List<String> offer = EnchantDirections.offeredFor(gear);
		if (id < 0 || id >= offer.size() || !EnchantComponents.hasFreeSlot(gear)) {
			cir.setReturnValue(false);
			return;
		}
		ItemStack lapis = this.inventory.getStack(1);
		boolean creative = player.getAbilities().creativeMode;
		int cost = id + 1; // lapis + levels, vanilla-style escalating cost
		if (!creative && (lapis.getCount() < cost || player.experienceLevel < cost)) {
			cir.setReturnValue(false);
			return;
		}
		EnchantComponents.addDirection(gear, offer.get(id));
		if (!creative) {
			lapis.decrement(cost);
			player.addExperienceLevels(-cost);
		}
		player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
			SoundCategory.BLOCKS, 0.8f, 1.0f);
		heirloom$fillDirections(gear);
		cir.setReturnValue(true);
	}

	/**
	 * Point the three rows at the gear's offered directions (their no-op enchantments, for display).
	 * All writes happen inside {@code context.run} — which is server-only (the client handler's context
	 * is EMPTY) — so the client never clobbers the server-synced option arrays.
	 */
	@Unique
	private void heirloom$fillDirections(ItemStack gear) {
		List<String> offer = EnchantDirections.offeredFor(gear);
		this.context.run((world, pos) -> {
			Registry<Enchantment> registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
			for (int i = 0; i < 3; i++) {
				if (i < offer.size()) {
					Identifier id = Identifier.of("heirloom", "dir_" + offer.get(i).toLowerCase(Locale.ROOT));
					Enchantment ench = registry.getOptionalValue(id).orElse(null);
					this.enchantmentId[i] = ench == null ? -1 : registry.getRawId(ench);
					this.enchantmentLevel[i] = 1;
					this.enchantmentPower[i] = i + 1;
				} else {
					this.enchantmentId[i] = -1;
					this.enchantmentLevel[i] = 0;
					this.enchantmentPower[i] = 0;
				}
			}
		});
	}
}

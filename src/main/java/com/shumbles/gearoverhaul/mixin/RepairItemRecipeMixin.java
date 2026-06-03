package com.shumbles.gearoverhaul.mixin;

import com.shumbles.gearoverhaul.temper.Tempering;
import com.shumbles.gearoverhaul.usage.UsageComponents;
import com.shumbles.gearoverhaul.usage.UsageGate;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RepairItemRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The 2×2 crafting-grid repair builds a <i>fresh</i> result item that drops custom data
 * components, which would silently reset a tempered piece to level 0 (and wipe its baked
 * stats). The anvil and grindstone already preserve temper because they copy the input
 * stack; this brings grid-repair in line.
 *
 * <p>After the recipe forms its result, we carry over the <b>highest</b> temper level found
 * among the inputs and re-bake the result's stats from the current tables, and likewise keep
 * the best Level-10 milestone progress. So repairing a worn tempered tool on the crafting
 * grid keeps the investment instead of throwing it away.
 */
@Mixin(RepairItemRecipe.class)
public class RepairItemRecipeMixin {
	@Inject(
		method = "craft(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/item/ItemStack;",
		at = @At("RETURN"))
	private void heirloom$carryTemper(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup,
									  CallbackInfoReturnable<ItemStack> cir) {
		ItemStack result = cir.getReturnValue();
		if (result == null || result.isEmpty()) {
			return;
		}

		int bestLevel = Tempering.MIN_TEMPER;
		int bestUsage = 0;
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack in = input.getStackInSlot(slot);
			if (in.isEmpty()) {
				continue;
			}
			bestLevel = Math.max(bestLevel, Tempering.getLevel(in));
			bestUsage = Math.max(bestUsage, in.getOrDefault(UsageComponents.USAGE, 0));
		}

		if (bestLevel > Tempering.MIN_TEMPER) {
			Tempering.setLevel(result, bestLevel); // re-bakes attribute modifiers from current tables
			if (bestUsage > 0 && UsageGate.kindOf(result) != null) {
				UsageGate.setProgress(result, bestUsage);
			}
		}
	}
}

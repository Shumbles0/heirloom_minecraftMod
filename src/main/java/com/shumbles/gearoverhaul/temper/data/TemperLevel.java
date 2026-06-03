package com.shumbles.gearoverhaul.temper.data;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * The data for a single temper level of a single gear item: what it costs to reach
 * and the stats it confers.
 *
 * <p>Buffs are <b>absolute totals</b> — the {@code value} on each {@link StatBuff} is
 * the full attribute value at this level, not an increment. Applying a temper level
 * therefore just means installing this level's buffs directly; no summing across
 * levels.
 */
public record TemperLevel(List<MaterialCost> materials, List<StatBuff> buffs) {

	/**
	 * A required ingredient: {@code count} of whatever {@link Ingredient} matches. The
	 * ingredient is either a single item or an item tag, so a cost can demand a concrete
	 * item (e.g. {@code minecraft:string}) or an "any of" group (e.g. {@code #minecraft:arrows}).
	 */
	public record MaterialCost(Ingredient ingredient, int count) {
		/** Whether the given stack can pay (part of) this cost. */
		public boolean matches(ItemStack stack) {
			return ingredient.matches(stack);
		}
	}

	/** Matches the stacks that satisfy a {@link MaterialCost}: a single item, or an item tag. */
	public sealed interface Ingredient {
		boolean matches(ItemStack stack);

		/** A single concrete item. */
		record OfItem(Item item) implements Ingredient {
			@Override
			public boolean matches(ItemStack stack) {
				return stack.isOf(item);
			}
		}

		/** Any item in a tag (e.g. {@code #minecraft:wooden_buttons}). */
		record OfTag(TagKey<Item> tag) implements Ingredient {
			@Override
			public boolean matches(ItemStack stack) {
				return stack.isIn(tag);
			}
		}
	}

	/**
	 * An attribute set to {@code value} (additive modifier) at this level.
	 * {@code attributeId} is kept alongside the resolved entry so the applied
	 * modifier can be named deterministically.
	 */
	public record StatBuff(RegistryEntry<EntityAttribute> attribute, Identifier attributeId, double value) {
	}
}

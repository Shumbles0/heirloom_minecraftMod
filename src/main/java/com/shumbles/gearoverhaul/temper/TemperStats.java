package com.shumbles.gearoverhaul.temper;

import com.shumbles.gearoverhaul.Heirloom;
import com.shumbles.gearoverhaul.enchant.EnchantEffects;
import com.shumbles.gearoverhaul.temper.data.TemperLevel;
import com.shumbles.gearoverhaul.temper.data.TemperTableLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bakes an item's temper-level buffs into its {@code attribute_modifiers} component.
 *
 * <p>Called whenever the temper level changes. It always rebuilds from the item's
 * <i>default</i> (nerfed) modifiers, so it is idempotent — no modifier accumulation
 * across repeated calls. Temper buffs <b>replace</b> the base modifier for the same
 * attribute (the file's value is the absolute bonus at that level), while attributes
 * the temper level doesn't touch keep their base modifiers.
 *
 * <p>Because the result is stored on the stack, it persists with the item. The
 * tradeoff: editing the tables and {@code /reload} won't retroactively update
 * already-tempered gear — re-tempering (or re-running the dev command) re-bakes it
 * from the current tables.
 */
public final class TemperStats {
	/** Attributes whose temper buff <i>replaces</i> the base modifier (rest are additive). */
	private static final Set<RegistryEntry<EntityAttribute>> REPLACE_ATTRIBUTES = Set.of(
		EntityAttributes.ATTACK_DAMAGE, EntityAttributes.ARMOR, EntityAttributes.ARMOR_TOUGHNESS);

	private TemperStats() {
	}

	/** Recompute and install the attribute modifiers for the stack's temper level + enchant attributes. */
	public static void refresh(ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		int level = Tempering.getLevel(stack);
		Item item = stack.getItem();
		Identifier itemId = Registries.ITEM.getId(item);

		TemperLevel entry = level > Tempering.MIN_TEMPER
			? TemperTableLoader.getTable().getEffective(itemId, level)
			: null;

		boolean hasTemper = entry != null && !entry.buffs().isEmpty();
		boolean hasEnchant = EnchantEffects.hasAny(stack);
		if (!hasTemper && !hasEnchant) {
			// Untempered, unenchanted → revert to the item's default (nerfed) stats.
			stack.remove(DataComponentTypes.ATTRIBUTE_MODIFIERS);
			return;
		}

		AttributeModifiersComponent base = item.getComponents()
			.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
		AttributeModifierSlot fallback = fallbackSlot(item);
		AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

		if (hasTemper) {
			// REPLACE attributes (attack damage, armor, toughness) overwrite their base modifier —
			// the temper value IS the total, so we drop the base and reuse its id. Everything else
			// (attack speed, …) is ADDITIVE: the base stays and the temper value stacks on top.
			Set<RegistryEntry<EntityAttribute>> overridden = new HashSet<>();
			for (TemperLevel.StatBuff buff : entry.buffs()) {
				overridden.add(buff.attribute());
			}
			Map<RegistryEntry<EntityAttribute>, AttributeModifierSlot> baseSlots = new HashMap<>();
			Map<RegistryEntry<EntityAttribute>, Identifier> baseIds = new HashMap<>();
			for (AttributeModifiersComponent.Entry e : base.modifiers()) {
				if (overridden.contains(e.attribute())) {
					baseSlots.putIfAbsent(e.attribute(), e.slot());
					baseIds.putIfAbsent(e.attribute(), e.modifier().id());
					if (REPLACE_ATTRIBUTES.contains(e.attribute())) {
						continue;
					}
				}
				builder.add(e.attribute(), e.modifier(), e.slot());
			}
			for (TemperLevel.StatBuff buff : entry.buffs()) {
				AttributeModifierSlot slot = baseSlots.getOrDefault(buff.attribute(), fallback);
				Identifier heirloomId = Identifier.of(Heirloom.MOD_ID, "temper_" + buff.attributeId().getPath());
				Identifier modifierId = REPLACE_ATTRIBUTES.contains(buff.attribute())
					? baseIds.getOrDefault(buff.attribute(), heirloomId)
					: heirloomId;
				builder.add(buff.attribute(),
					new EntityAttributeModifier(modifierId, buff.value(), EntityAttributeModifier.Operation.ADD_VALUE),
					slot);
			}
		} else {
			// No temper buffs: keep the item's base modifiers as-is, then add enchant ones on top.
			for (AttributeModifiersComponent.Entry e : base.modifiers()) {
				builder.add(e.attribute(), e.modifier(), e.slot());
			}
		}

		EnchantEffects.append(stack, builder, fallback);
		stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
	}

	/** Slot for a buffed attribute not present in the base: the worn slot for armor, else main hand. */
	private static AttributeModifierSlot fallbackSlot(Item item) {
		EquippableComponent equippable = item.getComponents().get(DataComponentTypes.EQUIPPABLE);
		if (equippable != null && equippable.slot().isArmorSlot()) {
			return AttributeModifierSlot.forEquipmentSlot(equippable.slot());
		}
		return AttributeModifierSlot.MAINHAND;
	}
}

package com.shumbles.gearoverhaul.foundation;

import com.shumbles.gearoverhaul.Heirloom;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Foundation §1 (gear half): flattens vanilla gear base stats so tempering can
 * restore them. We read each item's existing default components and rewrite the
 * relevant values, so iron/diamond/netherite all start from the same low baseline
 * regardless of their vanilla numbers.
 *
 * <p><b>Attack damage</b> ({@code attribute_modifiers}):
 * <ul>
 *   <li>Swords (registry id ending in {@code _sword}) → a small fixed bonus,
 *       leaving them a touch above a bare hand. Tempering climbs from there.</li>
 *   <li>Everything else (axes, tridents, tools-as-weapons) → 0 bonus, i.e. they
 *       hit like a bare fist until tempered.</li>
 * </ul>
 *
 * <p><b>Armor + armor toughness</b> ({@code attribute_modifiers}) → scaled to
 * near-zero; armor is rebuilt entirely through tempering.
 *
 * <p><b>Mining speed</b> ({@code tool} component) → every tool's per-block mining
 * speed is dropped to barely above bare-hand (1.0). We keep each rule's
 * {@code correctForDrops} flag intact, so a pickaxe is still <i>required</i> to
 * harvest stone/ores properly — it's just slow until tempered. This preserves the
 * vanilla harvest gating while removing the speed advantage.
 */
public final class GearNerf {
	/** Flat attack-damage bonus a sword adds at temper 0 (on top of the ~1.0 hand base). */
	private static final double SWORD_BASE_DAMAGE = 1.0;
	/** Attack-damage bonus for any non-sword weapon/tool at temper 0. */
	private static final double NON_SWORD_DAMAGE = 0.0;
	/**
	 * Fraction of a special weapon's vanilla melee attack-damage retained at temper 0.
	 * The mace keeps 20% of its base modifier; tempering restores the rest along the
	 * special curve. (The trident floors at the sword baseline instead — see below.)
	 */
	private static final double SPECIAL_MELEE_FLOOR = 0.20;
	/** Fraction of vanilla armor / armor toughness retained at temper 0. Near-zero by design. */
	private static final double ARMOR_FLOOR = 0.05;
	/** Per-block mining speed for every tool at temper 0. Bare hand is 1.0, so this is a hair faster. */
	private static final float TOOL_MINING_SPEED = 1.05f;
	/**
	 * Flat reduction to a weapon's attack speed at temper 0 (attacks/sec), making gear
	 * feel a bit sluggish until restored. The attack-speed modifier is negative (it
	 * subtracts from the 4.0 base), so we make it more negative by this amount. Intended
	 * to be restored by the level-20 ritual.
	 */
	private static final double ATTACK_SPEED_PENALTY = 0.2;

	private GearNerf() {
	}

	public static void register() {
		DefaultItemComponentEvents.MODIFY.register(context ->
			context.modify(item -> true, (builder, item) -> {
				nerfAttributes(builder, item);
				nerfMiningSpeed(builder, item);
			}));

		Heirloom.LOGGER.info("[Foundation] Gear nerf registered (sword +{} dmg, non-sword +{} dmg, armor floor {}, tool speed {}, -{} atk speed).",
			SWORD_BASE_DAMAGE, NON_SWORD_DAMAGE, ARMOR_FLOOR, TOOL_MINING_SPEED, ATTACK_SPEED_PENALTY);
	}

	private static void nerfAttributes(ComponentMap.Builder builder, Item item) {
		AttributeModifiersComponent original = item.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
		if (original == null || original.modifiers().isEmpty()) {
			return;
		}

		// NOTE: this runs during registry freeze, BEFORE item tags are bound, so we
		// cannot use ItemTags.SWORDS here. The item registry itself IS populated, so
		// we identify swords by their registry id instead.
		String idPath = Registries.ITEM.getId(item).getPath();
		boolean isSword = idPath.endsWith("_sword");
		// Special melee weapons floor like swords (trident → +1, the sword baseline) or at
		// a fraction of their vanilla modifier (mace → 20%); tempering restores them along
		// the special curve. Thrown/smash damage is handled separately by the mixin layer.
		boolean isTrident = idPath.equals("trident");
		boolean isMace = idPath.equals("mace");
		boolean changed = false;
		AttributeModifiersComponent.Builder rebuilt = AttributeModifiersComponent.builder();

		for (AttributeModifiersComponent.Entry entry : original.modifiers()) {
			EntityAttributeModifier mod = entry.modifier();
			Double newValue = null;

			if (entry.attribute().equals(EntityAttributes.ATTACK_DAMAGE)) {
				if (isSword || isTrident) {
					newValue = SWORD_BASE_DAMAGE;
				} else if (isMace) {
					newValue = mod.value() * SPECIAL_MELEE_FLOOR;
				} else {
					newValue = NON_SWORD_DAMAGE;
				}
			} else if (entry.attribute().equals(EntityAttributes.ARMOR)
				|| entry.attribute().equals(EntityAttributes.ARMOR_TOUGHNESS)) {
				newValue = mod.value() * ARMOR_FLOOR;
			} else if (entry.attribute().equals(EntityAttributes.ATTACK_SPEED)) {
				newValue = mod.value() - ATTACK_SPEED_PENALTY;
			}

			if (newValue != null) {
				mod = new EntityAttributeModifier(mod.id(), newValue, mod.operation());
				changed = true;
			}
			rebuilt.add(entry.attribute(), mod, entry.slot());
		}

		if (changed) {
			builder.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, rebuilt.build());
		}
	}

	private static void nerfMiningSpeed(ComponentMap.Builder builder, Item item) {
		ToolComponent tool = item.getComponents().get(DataComponentTypes.TOOL);
		if (tool == null) {
			return;
		}

		boolean changed = false;
		List<ToolComponent.Rule> newRules = new ArrayList<>(tool.rules().size());
		for (ToolComponent.Rule rule : tool.rules()) {
			// Only rules that grant a speed bonus get clamped down; keep blocks and the
			// correctForDrops flag so harvest correctness is unchanged.
			if (rule.speed().isPresent()) {
				newRules.add(new ToolComponent.Rule(rule.blocks(), Optional.of(TOOL_MINING_SPEED), rule.correctForDrops()));
				changed = true;
			} else {
				newRules.add(rule);
			}
		}

		if (changed) {
			builder.add(DataComponentTypes.TOOL, new ToolComponent(
				newRules, tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
		}
	}
}

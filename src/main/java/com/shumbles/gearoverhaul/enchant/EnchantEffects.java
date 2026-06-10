package com.shumbles.gearoverhaul.enchant;

import com.shumbles.gearoverhaul.Heirloom;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Passive attribute-modifier effects for the enchant attributes (Batch 1). Each attribute adds a
 * fixed bonus to its main stat and a drawback to a kin stat; the drawback <b>softens by level</b>
 * (the effect is fixed, levels make the downside more livable). Contributions across the piece's
 * three slots are <b>summed and per-stat capped</b>, then baked into the gear's attribute-modifiers
 * component (see {@code TemperStats}).
 *
 * <p>First-draft numbers — tuning is a later pass. Behavioural attributes (on-hit afflictions, typed
 * protection, kill-streaks, charge attacks, Bite/Bounty, Gale/Storm, …) are <b>not</b> here; they
 * get their own handlers in later batches.
 */
public final class EnchantEffects {
	private EnchantEffects() {
	}

	private record Spec(RegistryEntry<EntityAttribute> main, double mainValue,
						RegistryEntry<EntityAttribute> drawback, double[] drawbackByLevel) {
	}

	private static final Map<ArcaneAttribute, Spec> SPECS = new HashMap<>();
	private static final Map<RegistryEntry<EntityAttribute>, double[]> CAPS = new HashMap<>();

	static {
		// attribute, +main, drawback attribute, drawback at level 1/2/3 (negative; shrinks toward 0).
		put(ArcaneAttribute.POWER, EntityAttributes.ATTACK_DAMAGE, 1.5, EntityAttributes.ATTACK_SPEED, -0.4, -0.25, -0.1);
		put(ArcaneAttribute.SWIFTNESS, EntityAttributes.ATTACK_SPEED, 0.4, EntityAttributes.ATTACK_DAMAGE, -1.5, -1.0, -0.5);
		put(ArcaneAttribute.OVERREACH, EntityAttributes.ENTITY_INTERACTION_RANGE, 0.5, EntityAttributes.ATTACK_DAMAGE, -1.0, -0.7, -0.5);
		put(ArcaneAttribute.REACH, EntityAttributes.BLOCK_INTERACTION_RANGE, 1.0, null, 0, 0, 0);
		put(ArcaneAttribute.FLEET, EntityAttributes.MOVEMENT_SPEED, 0.015, EntityAttributes.ARMOR_TOUGHNESS, -2.0, -1.5, -1.0);
		put(ArcaneAttribute.STOUTHEART, EntityAttributes.MAX_HEALTH, 4.0, EntityAttributes.MOVEMENT_SPEED, -0.02, -0.013, -0.006);
		put(ArcaneAttribute.ROOTED, EntityAttributes.KNOCKBACK_RESISTANCE, 0.3, EntityAttributes.MOVEMENT_SPEED, -0.01, -0.006, -0.003);
		put(ArcaneAttribute.BULWARK, EntityAttributes.ARMOR, 3.0, EntityAttributes.MOVEMENT_SPEED, -0.02, -0.013, -0.006);
		put(ArcaneAttribute.BERSERKERS_HIDE, EntityAttributes.ATTACK_DAMAGE, 2.0, EntityAttributes.ARMOR, -4.0, -3.0, -2.0);
		// Drawback-only passive (the effect is behavioural — see secondWind): less max health.
		put(ArcaneAttribute.SECOND_WIND, null, 0.0, EntityAttributes.MAX_HEALTH, -4.0, -3.0, -2.0);
		// Adrenal Surge: slow normally (passive), fast when hit (behavioural — see adrenalSurge).
		put(ArcaneAttribute.ADRENAL_SURGE, null, 0.0, EntityAttributes.MOVEMENT_SPEED, -0.03, -0.02, -0.01);

		// Per-stat caps on the SUMMED enchant contribution (anti-creep; lets drawbacks zero a stat).
		cap(EntityAttributes.ATTACK_DAMAGE, -6.0, 6.0);
		cap(EntityAttributes.ATTACK_SPEED, -1.0, 1.0);
		cap(EntityAttributes.MOVEMENT_SPEED, -0.1, 0.05);
		cap(EntityAttributes.MAX_HEALTH, -10.0, 10.0);
		cap(EntityAttributes.KNOCKBACK_RESISTANCE, -1.0, 1.0);
		cap(EntityAttributes.ARMOR, -12.0, 8.0);
		cap(EntityAttributes.ARMOR_TOUGHNESS, -8.0, 6.0);
		cap(EntityAttributes.BLOCK_INTERACTION_RANGE, 0.0, 1.5);
		cap(EntityAttributes.ENTITY_INTERACTION_RANGE, 0.0, 1.5);
	}

	private static void put(ArcaneAttribute a, RegistryEntry<EntityAttribute> main, double mv,
							RegistryEntry<EntityAttribute> draw, double l1, double l2, double l3) {
		SPECS.put(a, new Spec(main, mv, draw, new double[]{l1, l2, l3}));
	}

	private static void cap(RegistryEntry<EntityAttribute> a, double lo, double hi) {
		CAPS.put(a, new double[]{lo, hi});
	}

	/** Whether this piece has any attribute that contributes a passive modifier. */
	public static boolean hasAny(ItemStack stack) {
		for (int i = 0; i < EnchantComponents.slotCount(stack); i++) {
			String attr = EnchantComponents.attributeAt(stack, i);
			if (attr != null && SPECS.containsKey(ArcaneAttribute.fromDisplay(attr))) {
				return true;
			}
		}
		return false;
	}

	/** Sum + cap the piece's enchant contributions and add them to the modifier builder. */
	public static void append(ItemStack stack, AttributeModifiersComponent.Builder builder, AttributeModifierSlot slot) {
		Map<RegistryEntry<EntityAttribute>, Double> totals = new HashMap<>();
		for (int i = 0; i < EnchantComponents.slotCount(stack); i++) {
			String attrName = EnchantComponents.attributeAt(stack, i);
			if (attrName == null) {
				continue;
			}
			ArcaneAttribute a = ArcaneAttribute.fromDisplay(attrName);
			Spec spec = a == null ? null : SPECS.get(a);
			if (spec == null) {
				continue;
			}
			int level = Math.max(1, Math.min(3, EnchantComponents.levelAt(stack, i)));
			if (spec.main() != null) {
				totals.merge(spec.main(), spec.mainValue(), Double::sum);
			}
			if (spec.drawback() != null) {
				totals.merge(spec.drawback(), spec.drawbackByLevel()[level - 1], Double::sum);
			}
		}
		for (Map.Entry<RegistryEntry<EntityAttribute>, Double> e : totals.entrySet()) {
			double value = e.getValue();
			double[] cap = CAPS.get(e.getKey());
			if (cap != null) {
				value = Math.max(cap[0], Math.min(cap[1], value));
			}
			if (value == 0.0) {
				continue;
			}
			Identifier path = Registries.ATTRIBUTE.getId(e.getKey().value());
			Identifier id = Identifier.of(Heirloom.MOD_ID, "enchant_" + (path == null ? "x" : path.getPath()));
			builder.add(e.getKey(), new EntityAttributeModifier(id, value, EntityAttributeModifier.Operation.ADD_VALUE), slot);
		}
	}

	// ---- combat (Batch 2): on-hit melee effects ----------------------------
	// Sidegrades, not power boosts: a situational bonus is paid for by a penalty elsewhere, and the
	// penalty softens by level. First-draft numbers.

	public static boolean hasAttribute(ItemStack stack, ArcaneAttribute attr) {
		for (int i = 0; i < EnchantComponents.slotCount(stack); i++) {
			if (attr.displayName.equals(EnchantComponents.attributeAt(stack, i))) {
				return true;
			}
		}
		return false;
	}

	public static int levelOf(ItemStack stack, ArcaneAttribute attr) {
		for (int i = 0; i < EnchantComponents.slotCount(stack); i++) {
			if (attr.displayName.equals(EnchantComponents.attributeAt(stack, i))) {
				return EnchantComponents.levelAt(stack, i);
			}
		}
		return 0;
	}

	private static float byLevel(int level, float l1, float l2, float l3) {
		return level >= 3 ? l3 : (level == 2 ? l2 : l1);
	}

	/** Net melee damage delta from the weapon's combat attributes (situational +, paid for by −). */
	public static float meleeDamageBonus(ItemStack weapon, LivingEntity victim, PlayerEntity attacker) {
		float bonus = 0f;
		boolean slam = attacker.fallDistance > 1.5; // a mace fall-smash
		// Gale (mace): the slam trades direct hitting power for its area/control effects.
		if (slam && hasAttribute(weapon, ArcaneAttribute.WINDBURST)) {
			bonus -= 2.0f;
		}
		if (slam && hasAttribute(weapon, ArcaneAttribute.SHOCKWAVE)) {
			bonus -= 2.0f;
		}
		// Tideborn (trident): stronger in water/rain, weaker dry.
		if (hasAttribute(weapon, ArcaneAttribute.TIDEBORN)) {
			bonus += attacker.isTouchingWaterOrRain() ? 1.5f : -1.0f;
		}
		if (hasAttribute(weapon, ArcaneAttribute.SLAYER)) {
			bonus += victim.getType().isIn(EntityTypeTags.SENSITIVE_TO_SMITE)
				? 1.5f : -byLevel(levelOf(weapon, ArcaneAttribute.SLAYER), 1.0f, 0.6f, 0.3f);
		}
		if (hasAttribute(weapon, ArcaneAttribute.BANE)) {
			bonus += victim.getType().isIn(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)
				? 1.5f : -byLevel(levelOf(weapon, ArcaneAttribute.BANE), 1.0f, 0.6f, 0.3f);
		}
		if (hasAttribute(weapon, ArcaneAttribute.EXECUTIONER)) {
			boolean low = victim.getMaxHealth() > 0 && victim.getHealth() / victim.getMaxHealth() < 0.30f;
			bonus += low ? 1.0f : -byLevel(levelOf(weapon, ArcaneAttribute.EXECUTIONER), 2.0f, 1.5f, 1.0f);
		}
		if (hasAttribute(weapon, ArcaneAttribute.PIERCING)) {
			bonus += victim.getAttributeValue(EntityAttributes.ARMOR) >= 6.0 ? 1.0f : -0.3f;
		}
		if (hasAttribute(weapon, ArcaneAttribute.GLASS_EDGE)) {
			bonus += byLevel(levelOf(weapon, ArcaneAttribute.GLASS_EDGE), 0.3f, 0.5f, 0.8f);
		}
		if (hasAttribute(weapon, ArcaneAttribute.CONCUSSION)) {
			bonus -= 1.0f; // trades damage for control
		}
		// Bloodbath (Momentum): kills feed a stacking damage bonus; it sours when you stop killing.
		bonus += bloodbathBonus(attacker, weapon);
		return bonus;
	}

	/** On-hit afflictions + durability costs after a successful melee hit. */
	public static void applyMeleeOnHit(ServerWorld world, ItemStack weapon, LivingEntity victim, PlayerEntity attacker) {
		if (hasAttribute(weapon, ArcaneAttribute.WITHERING)) {
			victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0, false, true));
			int wear = level3(levelOf(weapon, ArcaneAttribute.WITHERING), 5, 3, 1); // the blade withers too
			weapon.damage(wear, attacker);
		}
		if (hasAttribute(weapon, ArcaneAttribute.CRIPPLING_BLOW)) {
			victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1, false, true));
			victim.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 60, 0, false, true));
			knockAway(victim, attacker, 0.5); // drawback: shoves them off
		}
		if (hasAttribute(weapon, ArcaneAttribute.CONCUSSION)) {
			victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 2, false, true));
			knockAway(victim, attacker, 0.6);
		}
		if (hasAttribute(weapon, ArcaneAttribute.GLASS_EDGE)) {
			weapon.damage(Math.max(1, weapon.getMaxDamage() / 100), attacker); // brittle — ~100 hits
		}

		// Gale (mace) — fall-smash effects.
		if (attacker.fallDistance > 1.5) {
			if (hasAttribute(weapon, ArcaneAttribute.WINDBURST)) {
				windBurst(world, victim, attacker);
			}
			if (hasAttribute(weapon, ArcaneAttribute.SHOCKWAVE)) {
				shockwave(world, victim, attacker);
			}
			if (hasAttribute(weapon, ArcaneAttribute.UPDRAFT) && attacker instanceof ServerPlayerEntity sp) {
				updraft(sp);
			}
		}
		// Storm (trident melee) — call lightning on the target.
		if (hasAttribute(weapon, ArcaneAttribute.STORMCALL)) {
			stormcall(world, victim.getX(), victim.getY(), victim.getZ(), attacker);
		}
	}

	// ---- Gale (mace) + Storm (trident) effects ------------------------------

	private static final Map<UUID, Long> STORMCALL_READY = new HashMap<>();
	private static final long STORMCALL_COOLDOWN = 100L; // 5 s between bolts

	/** Wind burst: radial knockback + light damage to foes around the slam point. */
	private static void windBurst(ServerWorld world, LivingEntity impact, PlayerEntity attacker) {
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, impact.getBoundingBox().expand(3.5),
				e -> e != attacker && e.isAlive())) {
			e.takeKnockback(0.9, impact.getX() - e.getX(), impact.getZ() - e.getZ());
			if (e != impact) {
				e.damage(world, world.getDamageSources().playerAttack(attacker), 2.0f);
			}
		}
		world.playSound(null, impact.getBlockPos(), SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	/** Shockwave: AoE damage around the slam, scaling with how far the attacker fell. */
	private static void shockwave(ServerWorld world, LivingEntity impact, PlayerEntity attacker) {
		float dmg = (float) Math.min(8.0, attacker.fallDistance * 0.5);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, impact.getBoundingBox().expand(3.0),
				e -> e != attacker && e != impact && e.isAlive())) {
			e.damage(world, world.getDamageSources().playerAttack(attacker), dmg);
		}
	}

	/** Updraft: fling the attacker back upward to chain another slam. */
	private static void updraft(ServerPlayerEntity attacker) {
		attacker.setVelocity(attacker.getVelocity().x, 0.9, attacker.getVelocity().z);
		attacker.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(attacker));
	}

	/** Stormcall: a real lightning bolt at the target, on a per-channeler cooldown. */
	public static void stormcall(ServerWorld world, double x, double y, double z, LivingEntity channeler) {
		if (channeler != null) {
			long now = world.getTime();
			Long ready = STORMCALL_READY.get(channeler.getUuid());
			if (ready != null && now < ready) {
				return;
			}
			STORMCALL_READY.put(channeler.getUuid(), now + STORMCALL_COOLDOWN);
		}
		LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
		bolt.refreshPositionAfterTeleport(x, y, z);
		if (channeler instanceof ServerPlayerEntity sp) {
			bolt.setChanneler(sp);
		}
		world.spawnEntity(bolt);
	}

	private static int level3(int level, int l1, int l2, int l3) {
		return level >= 3 ? l3 : (level == 2 ? l2 : l1);
	}

	private static void knockAway(LivingEntity victim, PlayerEntity attacker, double strength) {
		victim.takeKnockback(strength, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
	}

	// ---- defense (Batch 3): damage-taken effects on worn armor --------------

	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final Map<UUID, Long> SECOND_WIND_READY = new HashMap<>();
	private static final long SECOND_WIND_COOLDOWN = 18000L; // 15 minutes

	/** Typed damage reduction from worn armor, with a small physical-damage penalty as the tradeoff. */
	public static float applyDefense(float amount, LivingEntity victim, DamageSource source) {
		if (!(victim instanceof PlayerEntity player)) {
			return amount;
		}
		boolean fire = source.isIn(DamageTypeTags.IS_FIRE);
		boolean blast = source.isIn(DamageTypeTags.IS_EXPLOSION);
		boolean proj = source.isIn(DamageTypeTags.IS_PROJECTILE);
		boolean fall = source.isIn(DamageTypeTags.IS_FALL);
		boolean physical = !fire && !blast && !proj && !fall;
		boolean sneaking = player.isSneaking();

		float reduce = 0f;
		float physPenalty = 0f;
		for (EquipmentSlot s : ARMOR) {
			ItemStack p = player.getEquippedStack(s);
			if (!EnchantDirections.canTakeDirections(p)) {
				continue;
			}
			if (fire && hasAttribute(p, ArcaneAttribute.EMBERGUARD)) {
				reduce += 0.40f;
			}
			if (blast && hasAttribute(p, ArcaneAttribute.BRACING)) {
				reduce += 0.40f;
			}
			if (proj && hasAttribute(p, ArcaneAttribute.DEFLECTION)) {
				reduce += 0.40f;
			}
			if (fall && hasAttribute(p, ArcaneAttribute.CUSHIONED)) {
				reduce += 0.50f;
			}
			if (sneaking && hasAttribute(p, ArcaneAttribute.BULWARK_STANCE)) {
				reduce += 0.35f;
			}
			// Kinetic Plating: protected while moving; a standing-still floor that rises by level.
			if (hasAttribute(p, ArcaneAttribute.KINETIC_PLATING)) {
				boolean moving = player.getMovement().horizontalLengthSquared() > 0.001;
				reduce += moving ? 0.30f : byLevel(levelOf(p, ArcaneAttribute.KINETIC_PLATING), 0.0f, 0.15f, 0.30f);
			}
			// Tradeoff: focusing on a damage type leaves you a touch softer to plain melee.
			if (hasAttribute(p, ArcaneAttribute.EMBERGUARD)) {
				physPenalty += byLevel(levelOf(p, ArcaneAttribute.EMBERGUARD), 0.15f, 0.10f, 0.05f);
			}
			if (hasAttribute(p, ArcaneAttribute.BRACING)) {
				physPenalty += byLevel(levelOf(p, ArcaneAttribute.BRACING), 0.15f, 0.10f, 0.05f);
			}
			if (hasAttribute(p, ArcaneAttribute.DEFLECTION)) {
				physPenalty += byLevel(levelOf(p, ArcaneAttribute.DEFLECTION), 0.15f, 0.10f, 0.05f);
			}
		}
		float result = amount * (1f - Math.min(0.75f, reduce));
		if (physical) {
			result *= (1f + Math.min(0.5f, physPenalty));
		}
		return result;
	}

	/** Second Wind: dropping below 20% health grants a brief Resistance + Regeneration (cooldown). */
	public static void secondWind(PlayerEntity player, ServerWorld world) {
		boolean has = false;
		for (EquipmentSlot s : ARMOR) {
			ItemStack p = player.getEquippedStack(s);
			if (EnchantDirections.canTakeDirections(p) && hasAttribute(p, ArcaneAttribute.SECOND_WIND)) {
				has = true;
				break;
			}
		}
		if (!has || player.getHealth() <= 0f || player.getHealth() > player.getMaxHealth() * 0.20f) {
			return;
		}
		long now = world.getTime();
		Long ready = SECOND_WIND_READY.get(player.getUuid());
		if (ready != null && now < ready) {
			return;
		}
		SECOND_WIND_READY.put(player.getUuid(), now + SECOND_WIND_COOLDOWN);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 1, false, true));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1, false, true));
		world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.5f, 1.5f);
	}

	// ---- tick-based: kill streaks (Bloodlust/Bloodbath) + Adrenal Surge ------

	private static final Map<UUID, Integer> BLOODLUST = new HashMap<>();
	private static final Map<UUID, Integer> BLOODBATH = new HashMap<>();
	private static final Map<UUID, Long> LAST_KILL = new HashMap<>();
	private static final int STREAK_CAP = 3;
	private static final long STREAK_BUFF = 200L; // 10 s — the "fed" window
	private static final long STREAK_STALE = 400L; // 20 s — the drawback kicks in

	/** A kill with a Bloodlust/Bloodbath weapon refreshes the streak (called from the death event). */
	public static void onKill(ServerWorld world, PlayerEntity killer) {
		ItemStack weapon = killer.getMainHandStack();
		if (!EnchantDirections.canTakeDirections(weapon)) {
			return;
		}
		boolean bl = hasAttribute(weapon, ArcaneAttribute.BLOODLUST);
		boolean bb = hasAttribute(weapon, ArcaneAttribute.BLOODBATH);
		if (!bl && !bb) {
			return;
		}
		UUID id = killer.getUuid();
		LAST_KILL.put(id, world.getTime());
		if (bl) {
			int s = Math.min(STREAK_CAP, BLOODLUST.getOrDefault(id, 0) + 1);
			BLOODLUST.put(id, s);
			killer.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, Math.min(4, 1 + s), false, true));
		}
		if (bb) {
			BLOODBATH.put(id, Math.min(STREAK_CAP, BLOODBATH.getOrDefault(id, 0) + 1));
		}
	}

	/** Bloodbath's melee delta: +2 per recent kill, fading to −2 once the streak goes stale. */
	private static float bloodbathBonus(PlayerEntity attacker, ItemStack weapon) {
		if (!hasAttribute(weapon, ArcaneAttribute.BLOODBATH)) {
			return 0f;
		}
		long now = attacker.getEntityWorld().getTime();
		Long last = LAST_KILL.get(attacker.getUuid());
		if (last != null && now - last < STREAK_BUFF) {
			return 2.0f * BLOODBATH.getOrDefault(attacker.getUuid(), 0);
		}
		return (last == null || now - last > STREAK_STALE) ? -2.0f : 0f;
	}

	/** Per-tick upkeep: Bloodlust's stale-streak Slowness, and streak decay. */
	public static void tickPlayer(ServerWorld world, PlayerEntity player) {
		ItemStack weapon = player.getMainHandStack();
		if (!EnchantDirections.canTakeDirections(weapon)) {
			return;
		}
		boolean bl = hasAttribute(weapon, ArcaneAttribute.BLOODLUST);
		if (!bl && !hasAttribute(weapon, ArcaneAttribute.BLOODBATH)) {
			return;
		}
		UUID id = player.getUuid();
		Long last = LAST_KILL.get(id);
		boolean stale = last == null || world.getTime() - last > STREAK_STALE;
		if (stale) {
			BLOODLUST.remove(id);
			BLOODBATH.remove(id);
			if (bl && world.getTime() % 20 == 0) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false));
			}
		}
	}

	/** Adrenal Surge: taking a hit while wearing it grants a brief speed kick (offsets its passive slow). */
	public static void adrenalSurge(ServerWorld world, PlayerEntity player) {
		for (EquipmentSlot s : ARMOR) {
			ItemStack p = player.getEquippedStack(s);
			if (EnchantDirections.canTakeDirections(p) && hasAttribute(p, ArcaneAttribute.ADRENAL_SURGE)) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1, false, true));
				return;
			}
		}
	}
}

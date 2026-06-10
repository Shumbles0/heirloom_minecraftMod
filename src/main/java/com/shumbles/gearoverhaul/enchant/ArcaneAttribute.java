package com.shumbles.gearoverhaul.enchant;

/**
 * The full roster of enchant attributes — the single source of truth for the Arcane codex entries
 * (and, later, the brewing system). Each is one enchant: a direction, the selector item that picks
 * it, its effect, and its inherent drawback. <b>Order is stable</b> (grouped by direction); the
 * Arcane codex entry indices are derived from it, so new attributes must be <i>appended</i>, never
 * inserted, to keep already-inscribed books valid.
 */
public enum ArcaneAttribute {
	// Onslaught — raw offense
	POWER("Power", "Onslaught", "Iron Ingot", "+ melee / projectile damage", "−attack speed"),
	PIERCING("Piercing", "Onslaught", "Flint", "Ignore a very small % of the target's armor", "−knockback dealt"),
	HEAVY_SWING("Heavy Swing", "Onslaught", "Heavy Core", "Hold to charge; damage ramps up to 1.6x over 5s", "−attack speed (up to 0.4)"),
	GLASS_EDGE("Glass Edge", "Onslaught", "Glass Pane", "+ damage (0.3 / 0.5 / 0.8 by level)", "Breaks in ~100 hits"),

	// Hunter — conditional damage
	SLAYER("Slayer", "Hunter", "Bone", "+ damage vs the undead (up to +1.5)", "−damage vs everything else"),
	BANE("Bane", "Hunter", "Spider Eye", "+ damage vs arthropods (up to +1.5)", "−damage vs everything else"),
	EXECUTIONER("Executioner", "Hunter", "Wither Rose", "+1 damage vs targets under 30% health", "−2 to −1 (by level) vs targets over 30%"),
	MARKED_PREY("Marked Prey", "Hunter", "Arrow", "A melee hit marks; your next arrow on it does x1.3 / x1.6 / x2.0", "−1 melee damage"),
	RIPOSTE("Riposte", "Hunter", "Shield", "+1 damage if you strike soon after being hit", "−1 on unprovoked strikes"),

	// Cruelty — on-hit afflictions (custom-tuned)
	SUNDERING("Sundering", "Cruelty", "Iron Nugget", "Hit shaves a tiny % off the target's armor (up to ~3%, non-stacking)", "none"),
	WITHERING("Withering", "Cruelty", "Nether Wart", "A very weak wither tick on hit", "Durability cut: −80% to −50% (by level)"),
	CRIPPLING_BLOW("Crippling Blow", "Cruelty", "Cobweb", "Brief Slowness / Mining-Fatigue on the target", "+knockback dealt"),
	CONCUSSION("Concussion", "Cruelty", "Slime Ball", "Extra knockback + a brief slow", "−damage"),
	LIFEDRINK("Lifedrink", "Cruelty", "Ghast Tear", "25% of damage dealt returns as health", "No other healing while it's on you / dropped within 25 blocks (a chest lifts the lock)"),

	// Momentum — tempo & movement
	SWIFTNESS("Swiftness", "Momentum", "Sugar", "+attack speed", "−per-hit damage"),
	NOCK("Nock", "Momentum", "String", "+bow draw / crossbow reload", "−projectile damage"),
	BLOODLUST("Bloodlust", "Momentum", "Blaze Powder", "A kill grants Speed III (stacks per kill)", "Slowness when you haven't killed in 20s"),
	BLOODBATH("Bloodbath", "Momentum", "Gunpowder", "A kill grants +2 damage (stacks; stacks with Bloodlust)", "−2 damage when you haven't killed in 20s"),
	OVERREACH("Overreach", "Momentum", "Bamboo", "+0.5 blocks of reach", "−1 damage"),

	// Persistence — longevity & utility
	TOUGH("Tough", "Persistence", "Netherite Scrap", "+ effective durability", "−break speed"),
	REACH("Reach", "Persistence", "Stick", "+ block / attack reach", "−break speed"),
	MEND("Mend", "Persistence", "Bottle o' Enchanting", "Slow passive self-repair while held", "−break speed"),

	// Bite — gathering speed
	HASTE("Haste", "Bite", "Redstone", "+ break speed", "−durability"),
	SPECIALIST("Specialist", "Bite", "Diamond", "+ break speed on the tool's native material", "−speed on other materials"),
	DEEPCUT("Deepcut", "Bite", "Cobbled Deepslate", "Break speed rises the lower your Y", "Slower above sea level"),
	OVERCLOCK("Overclock", "Bite", "Blaze Powder", "Large + break speed", "Extra hunger per block"),

	// Bounty — harvest yield
	KINDLING("Kindling", "Bounty", "Coal", "Drops come pre-smelted", "−durability"),
	SCHOLAR("Scholar", "Bounty", "Bottle o' Enchanting", "+ XP from blocks & mobs", "−break speed"),
	TELEKINESIS("Telekinesis", "Bounty", "Ender Pearl", "Drops + XP go straight to your inventory", "−break speed"),
	VEIN_BREAKER("Vein Breaker", "Bounty", "TNT", "Breaks up to a few connected same-ore blocks", "Heavy durability cost"),

	// Gale — mace only
	WINDBURST("Windburst", "Gale", "Wind Charge", "A slam releases a knockback wind burst", "−direct slam damage"),
	SHOCKWAVE("Shockwave", "Gale", "Breeze Rod", "Fall-slam AoE damage (scales with fall)", "−single-target damage"),
	UPDRAFT("Updraft", "Gale", "Phantom Membrane", "A landed slam launches you back up", "You always take the fall damage"),

	// Storm — trident only
	STORMCALL("Stormcall", "Storm", "Lightning Rod", "A hit / throw calls a tuned lightning bolt (cooldown)", "Throw cooldown / −throw damage"),
	RIPTIDE("Riptide", "Storm", "Prismarine Crystals", "Enhanced riptide dash (usable in rain)", "High durability use"),
	TIDEBORN("Tideborn", "Storm", "Nautilus Shell", "+ damage / utility while wet", "− when dry"),

	// Ward — protection
	BULWARK("Bulwark", "Ward", "Iron Ingot", "+ physical protection", "−movement speed"),
	EMBERGUARD("Emberguard", "Ward", "Magma Cream", "+ fire protection + shorter burns", "−physical protection"),
	BRACING("Bracing", "Ward", "Gunpowder", "+ blast protection", "−movement speed"),
	DEFLECTION("Deflection", "Ward", "Feather", "+ projectile protection", "−movement speed"),
	BULWARK_STANCE("Bulwark Stance", "Ward", "Shield", "Large damage reduction while sneaking", "Deeper −move speed while sneaking"),

	// Stride — mobility
	FLEET("Fleet", "Stride", "Sugar", "Minor + movement speed", "Medium −armor toughness"),
	CUSHIONED("Cushioned", "Stride", "Slime Ball", "Fall-damage reduction + shorter fall-stun", "−knockback resistance"),
	CURRENT("Current", "Stride", "Prismarine Shard", "+ swim speed / depth strider", "−walking speed (mild)"),
	KINETIC_PLATING("Kinetic Plating", "Stride", "Piston", "1.6x protection while moving (floor 0 / 25 / 50% standing still, by level)", "No protection while standing still"),
	ADRENAL_SURGE("Adrenal Surge", "Stride", "Blaze Powder", "Taking a hit grants a brief speed kick", "Penalty while undamaged"),

	// Vigor — resilience
	STOUTHEART("Stoutheart", "Vigor", "Golden Carrot", "+ max health", "−movement speed"),
	ROOTED("Rooted", "Vigor", "Iron Block", "+ knockback resistance", "Minor −movement speed"),
	SECOND_WIND("Second Wind", "Vigor", "Glistering Melon", "Low health → a brief defense + regen burst (cooldown)", "−max health"),
	GRAVE_WARD("Grave Ward", "Vigor", "Totem of Undying", "Survive one lethal hit at 1 HP (long cooldown)", "−regen and −2 hearts max health"),
	BERSERKERS_HIDE("Berserker's Hide", "Vigor", "Fermented Spider Eye", "+ your own damage", "−your own defense");

	public final String displayName;
	public final String direction;
	public final String selector;
	public final String effect;
	public final String drawback;

	ArcaneAttribute(String displayName, String direction, String selector, String effect, String drawback) {
		this.displayName = displayName;
		this.direction = direction;
		this.selector = selector;
		this.effect = effect;
		this.drawback = drawback;
	}

	/** The attribute with this in-game name, or {@code null}. */
	public static ArcaneAttribute fromDisplay(String displayName) {
		for (ArcaneAttribute a : values()) {
			if (a.displayName.equals(displayName)) {
				return a;
			}
		}
		return null;
	}
}

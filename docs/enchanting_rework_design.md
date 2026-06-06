# Enchanting rework — design draft

The third progression system, after Tempering (vertical magnitude) and the L10/L20 gates.
Where tempering asks *"how strong is this piece,"* enchanting asks *"what is this piece **for**."*
Horizontal specialization, tradeoff-based. Design only; build comes after sign-off.

---

## 1. Pillars

- **Additive by default; multiplicative only when it behaves like additive.** Bonuses sum, never
  compound. No raw fixed numbers that ignore the weapon — but where flat values are clearer (e.g.
  "+0.8 damage") they're fine, they just don't multiply each other.
- **Flat, low ceiling.** Specialization, not a power ladder. Per-stat caps keep gains small.
- **Specialization, not power.** Every enchant is a *playstyle* — an effect bound to a drawback.
  Upgrading mostly **softens the drawback**; it never turns an enchant into a pure buff.
- **Feels vanilla.** Vanilla enchanting table front end, brewing-style station back end.
- **Soft, reversible.** Re-pointing a slot costs the work you invested, never the gear.
- **Determinism is a guideline here.** Direction selection rides the vanilla table's RNG.
- **Foundation done:** all 43 vanilla enchants are neutralized; we build power back on top.

---

## 2. The loop

```
   [Vanilla enchanting table]                  [Enchant Activation Station]
   offered 3 directions valid for       ──▶   selector sets the attribute (lvl 1),
   the piece, pick one per slot                upgrade reagents raise it (max lvl 3)
   (3 slots per piece, the hard max)
                       re-point a slot's direction ⟳ wipes that slot's buff
```

3 slots/piece. Pick a direction at the table (vanilla RNG, seed-stable, rerolls on enchant); build
the buff at the Station. Re-pointing a slot wipes it.

---

## 3. Directions, gear & attributes

Each attribute is one enchant: a specialized effect bound to an inherent drawback. **Damage &
Cruelty ride every weapon** (melee + ranged); **Gale** is mace-only and **Storm** is trident-only.

### Table A — which gear each direction can go on
| Direction | Gear |
|---|---|
| **Onslaught** | sword · axe · mace · bow · crossbow · trident |
| **Hunter** | sword · axe · mace · bow · crossbow · trident |
| **Cruelty** | sword · axe · mace · bow · crossbow · trident |
| **Momentum** | sword · axe · mace · bow · crossbow · trident |
| **Persistence** | sword · axe · mace · pickaxe · shovel · hoe |
| **Bite** | pickaxe · axe · shovel · hoe |
| **Bounty** | pickaxe · shovel · hoe |
| **Gale** | mace |
| **Storm** | trident |
| **Ward** | helmet · chestplate · leggings · boots |
| **Stride** | helmet · chestplate · leggings · boots |
| **Vigor** | helmet · chestplate · leggings · boots |

### Table B — attributes (effect = playstyle · drawback = inherent, tuned by level)

**Onslaught** — raw offense
| Attribute | Effect | Drawback |
|---|---|---|
| Power | +% / + damage | −attack speed |
| Piercing | ignore a *very small* % of the target's armor | −knockback dealt |
| Heavy Swing | **hold left-click to charge** up to 5 s; damage ramps **linearly to 1.6×** at full charge | −attack speed, up to −0.4 at max level (less at lower levels) |
| Glass Edge | + damage that scales by level (**L1 +0.3 · L2 +0.5 · L3 +0.8**) | brutal wear — breaks in **≤100 hits** (constant at all levels) |

**Hunter** — conditional damage
| Attribute | Effect | Drawback |
|---|---|---|
| Slayer | + damage vs undead (**max +1.5**) | −damage vs everything else |
| Bane | + damage vs arthropods (**max +1.5**) | −damage vs everything else |
| Executioner | **+1 damage** vs targets under 30% HP | **−damage vs targets over 30% HP: −2 at L1 → −1 at L3** |
| Marked Prey | a **melee** hit marks the target; your next **arrow** hit does **×1.3 / ×1.6 / ×2.0** (by level) and clears the mark | −1 melee damage on this weapon |
| Riposte | strike within ~1.5 s of being hit → **+1** damage | **−1** damage on unprovoked first strikes |

**Cruelty** — on-hit afflictions (custom-tuned, §6)
| Attribute | Effect | Drawback |
|---|---|---|
| Sundering | hit weakens the target's armor by a tiny % (**max ~3%**); **does not stack** on repeat hits | **none** (kept tiny on purpose) |
| Withering | a **very weak** wither tick (your blade withers too — thematic) | **durability cut: −80% at L1 → −50% at L3** |
| Crippling Blow | brief Slowness / Mining-Fatigue on the target | **+knockback dealt** (you shove the crippled target away) |
| Concussion | extra knockback + brief slow | −damage |
| Lifedrink | **25% of damage dealt returns to you as health** | **you cannot otherwise heal** while this sword is in your inventory — or while a sword you dropped sits unclaimed within 25 blocks (anti drop-heal-pickup). *Storing it in a chest lifts the lock.* |

**Momentum** — tempo & movement
| Attribute | Effect | Drawback |
|---|---|---|
| Swiftness | +% attack speed | −per-hit damage |
| Nock | +% bow draw / crossbow reload | −projectile damage |
| Bloodlust | a kill grants **Speed III** (stacks per kill within the window) | **Slowness while you haven't killed in the last 20 s** |
| Bloodbath | a kill grants **+2 damage** (stacks per kill) — **stacks with Bloodlust** | **−2 damage while you haven't killed in 20 s** |
| Overreach | **+0.5 blocks** of reach | **−1 damage** |

*(2 kills in 10 s with both = Speed VI + +4 damage for the window.)*

**Persistence** — longevity & utility
| Attribute | Effect | Drawback |
|---|---|---|
| Tough | +% effective durability | −break speed |
| Reach | +% block / attack reach | −break speed |
| Mend | slow passive self-repair while held | −break speed |

**Bite** — gathering speed
| Attribute | Effect | Drawback |
|---|---|---|
| Haste | +% break speed | −durability |
| Specialist | +% break speed on the tool's native material | −speed on other materials |
| Deepcut | break speed rises the lower your Y (deeper = faster) | slower above sea level |
| Overclock | large +break speed | each block drains extra **hunger** |

**Bounty** — harvest yield
| Attribute | Effect | Drawback |
|---|---|---|
| Kindling | drops come pre-smelted | −durability |
| Scholar | +% XP from blocks & mobs | −break speed |
| Telekinesis | mined drops + XP go straight to your inventory (never lost to lava/gaps) | −break speed |
| Vein Breaker | breaking an ore breaks up to a few connected same-ore blocks | heavy durability cost per use |

**Gale** — mace only (the slam & wind identity)
| Attribute | Effect | Drawback |
|---|---|---|
| Windburst | a slam releases a wind burst that knocks back & lightly damages nearby foes | −direct slam damage |
| Shockwave | fall-slam deals AoE damage to surrounding mobs (scales with fall distance) | −single-target damage |
| Updraft | a landed slam launches you back upward to chain another | you always eat the fall damage |

**Storm** — trident only (lightning & water identity)
| Attribute | Effect | Drawback |
|---|---|---|
| Stormcall | a hit/throw calls a **tuned** lightning bolt (cooldown; weaker than vanilla Channeling, no storm required) | throw cooldown / −throw damage |
| Riptide | enhanced riptide dash (longer; usable in rain, not just water) | high durability use |
| Tideborn | + damage & utility while wet / underwater / in rain | −effectiveness when dry |

**Ward** — protection
| Attribute | Effect | Drawback |
|---|---|---|
| Bulwark | +% physical protection | −movement speed |
| Emberguard | +% fire protection + shorter burns | −physical protection |
| Bracing | +% blast protection | −movement speed |
| Deflection | +% projectile protection | −movement speed |
| Bulwark Stance | large damage reduction while sneaking | deeper −move speed while sneaking |

**Stride** — mobility
| Attribute | Effect | Drawback |
|---|---|---|
| Fleet | **minor** +movement speed | **medium** −armor toughness |
| Cushioned | fall-damage reduction + shorter fall-stun | −knockback resistance |
| Current | +swim speed / depth strider | −walking speed (mild) |
| Kinetic Plating | **1.6× protection while moving**, **0% standing still** (floor rises 0 → 25 → 50% by level) | the standing-still gap itself |
| Adrenal Surge | taking a hit grants a brief speed kick | penalty while undamaged |

**Vigor** — resilience
| Attribute | Effect | Drawback |
|---|---|---|
| Stoutheart *(was Hardihood)* | +% max health | −movement speed |
| Rooted | +% knockback resistance | **minor** −movement speed |
| Second Wind | low health → brief defense + regen burst (cooldown) | −max health |
| Grave Ward | survive one lethal hit at 1 HP (long cooldown) | −regen **and −2 hearts max health** |
| Berserker's Hide | +your own damage output | −your own defense |

---

## 4. Step 2 — The Station (brewing-style)

A slot starts inert (just a direction); reagents build the enchant, stored as a **component on the
itemstack**. Reagents are direction-aware.

0. **Base — "Arcane Dust"** *(first)* — feed the primed direction a base reagent so it **"takes
   shape"** (the awkward-potion analog): a blank, attribute-ready enchant. Consumed each time.
   Recipe: **Amethyst Shard + Lapis Lazuli → 2 Arcane Dust** (shapeless).
1. **Selector** *(next)* — picks the **attribute** in the shaped direction and grants the enchant at
   **level 1** (full effect + full drawback).
2. **Upgrade reagent** — raises the level (max **3**); see §5. A **mix** of a generic upgrade
   (**Glowstone Dust** — the brewing-potency analog, works on any enchant) and some attribute-specific
   materials (often more of the selector, or a stronger themed item).
3. **Strip** — re-pointing the slot's direction at the table wipes it.

Usage is **never codex-gated** — any reagent works whether or not you've read its entry.

### Setup & conversion
- **Front end (directions):** an **ordinary enchanting table** — no bookshelf ring needed.
  Enchanting Heirloom gear there offers **directions** instead of vanilla enchants; costs XP + lapis
  and rerolls on use, exactly like vanilla.
- **Back end (attributes + upgrades):** ring a vanilla table with **custom Arcane Bookshelves**
  instead of normal ones and it **converts to the Advanced Enchanting Table** — same table,
  recoloured, with the brewing GUI. Detected from its surroundings like vanilla bookshelf power, so
  there's no block to craft-and-place — swap the shelves and it transforms.
- **Arcane Bookshelf** — built straight from raw materials (no plain bookshelf needed), shaped:
  ```
  L L L
  B B B
  L A L
  ```
  L = Lapis Lazuli · B = Book · A = Amethyst Shard (so 5 lapis + 3 books + 1 amethyst → 1 shelf).
  A full **15-ring** powers the Advanced table.
- **Two separate stations** (confirmed): an **ordinary enchanting table** for directions, and the
  Arcane 15-ring **Advanced** table for attributes + upgrades. The Advanced table re-checks its ring
  on every open and goes dormant if it's broken.

### GUI (brewing-flavored)
- A **gear slot** + a **reagent slot** (selector or upgrade) + a **lapis slot** (cost); XP shown.
- The gear's **3 enchant slots** as three panels — each lists its **direction**, current
  **attribute** (or empty), **level pips** (●●○), and the **effect / drawback** text.
- Select the active slot, drop in the matching reagent → it **selects the attribute** (level 1) or
  **upgrades** the active slot. The background carries the recoloured Advanced tint.

### Reagents — first-pass selector mapping
Mostly vanilla & thematic; **direction-aware**, so several items mean different things per direction
(e.g. *Iron* = Power on a weapon / Bulwark on armor; *Shield* = Riposte / Bulwark Stance; *Sugar* =
Swiftness / Fleet; *Blaze Powder* = Bloodlust / Overclock / Adrenal Surge). All tunable.

| Direction | Attribute → Selector |
|---|---|
| **Onslaught** | Power → Iron Ingot · Piercing → Flint · Heavy Swing → Heavy Core · Glass Edge → Glass Pane |
| **Hunter** | Slayer → Bone · Bane → Spider Eye · Executioner → Wither Rose · Marked Prey → Arrow · Riposte → Shield |
| **Cruelty** | Sundering → Iron Nugget · Withering → Nether Wart · Crippling Blow → Cobweb · Concussion → Slime Ball · Lifedrink → Ghast Tear |
| **Momentum** | Swiftness → Sugar · Nock → String · Bloodlust → Blaze Powder · Bloodbath → Gunpowder · Overreach → Bamboo |
| **Persistence** | Tough → Netherite Scrap · Reach → Stick · Mend → Bottle o' Enchanting |
| **Bite** | Haste → Redstone · Specialist → Diamond · Deepcut → Cobbled Deepslate · Overclock → Blaze Powder |
| **Bounty** | Kindling → Coal · Scholar → Bottle o' Enchanting · Telekinesis → Ender Pearl · Vein Breaker → TNT |
| **Gale** | Windburst → Wind Charge · Shockwave → Breeze Rod · Updraft → Phantom Membrane |
| **Storm** | Stormcall → Lightning Rod · Riptide → Prismarine Crystals · Tideborn → Nautilus Shell |
| **Ward** | Bulwark → Iron Ingot · Emberguard → Magma Cream · Bracing → Gunpowder · Deflection → Feather · Bulwark Stance → Shield |
| **Stride** | Fleet → Sugar · Cushioned → Slime Ball · Current → Prismarine Shard · Kinetic Plating → Piston · Adrenal Surge → Blaze Powder |
| **Vigor** | Stoutheart → Golden Carrot · Rooted → Iron Block · Second Wind → Glistering Melon · Grave Ward → Totem of Undying · Berserker's Hide → Fermented Spider Eye |

A few selectors deliberately **gate the strong/niche enchants** behind rarer items — Heavy Core,
Totem of Undying (Grave Ward), Nautilus Shell, Wither Rose, Lightning Rod.

---

## 5. Levels, stacking & failsafes

**Not buffs — playstyles.** Picking the attribute (level 1) grants the full effect at its full
drawback.

**Leveling tunes the attribute, and values are absolute.** Each level swaps in that level's tuned
numbers — usually softening the drawback, sometimes raising the effect, per the attribute.
**Numbers replace, they never sum** (exactly like the temper tables): a level-3 "+0.8 damage" is
+0.8 *total*, not +0.3 +0.5 +0.8. **3 levels**, level 1 = full drawback.

*Example — Kinetic Plating's protection while standing still:*

| Level | Standing-still protection |
|---|---|
| 1 | 0% |
| 2 | 25% |
| 3 | 50% (floor — standing still always costs something) |

**Stacking (3 per piece) — additive, clamped, never compounding.** For any one stat, all the buff
and drawback percentages across the 3 slots are **summed and applied once** off a 100% base — they
do **not** multiply. So **−50% and −50% = 0%, not 25%.** Two enchants can cancel or zero a stat.

**Hard caps & failsafes:**
- **Per-stat buff cap** — a stat's positive total is clamped to a low ceiling.
- **Drawback clamp at 0** — a stat bottoms out at a literal **0**, never negative / NaN. **No
  playability floor**: a careless stack *can* freeze your movement or stop your attacks — the
  **Basics codex entry warns about this up front** (§7).
- Clamp everything; validate on apply; no division traps.

---

## 6. Custom-tuned effects to build

Several attributes are bespoke, scaled down from anything vanilla, and need handlers/mixins (the
ritual toolkit): **Withering** (a far weaker wither tick), **Stormcall** (a cooldown lightning that
doesn't need a thunderstorm), **Sundering** (tiny non-stacking armor shred), **Crippling Blow**
(short Slowness/Fatigue), **Lifedrink** (25% lifesteal + the heal-lock incl. the dropped-within-25
check + chest exemption), **Heavy Swing** (hold-to-charge linear ramp to 1.6×), **Bloodlust /
Bloodbath** (stacking kill buffs + the 20 s no-kill penalty), **Marked Prey** (entity mark consumed
by an arrow), **Glass Edge** (extra durability per hit), **Executioner / Riposte** (conditional
damage windows), **Kinetic Plating / Adrenal Surge / Second Wind / Grave Ward** (state-driven
defense), the **Bite/Bounty** utilities, and the **Gale/Storm** slam & lightning hooks. All tuned
against the §5 caps.

---

## 7. Codex — the Arcane track

- **Overview** (always visible): enchanting **works differently now** + the **Arcane Manuscript recipe**.
- **First inscribe is forced → the Basics entry**: how brewing works, the Station recipe,
  3-directions-per-piece, the **stacking warning** (no safety net), and the **Directions Index** —
  every direction with the in-game names of the attributes in it.
- **After that, reveal-3-keep-1**, each entry **documenting one attribute** — how to obtain it (its
  selector) and its upgrade costs.
- Entries are **documentation only** — never a usage gate.

**Arcane Manuscript — recipe & sources.** Crafted shapeless from **1 Paper + 1 Lapis Lazuli**
(simple and reliable, so anyone can reach the Basics entry). Also found in:
- **Stronghold library chests** — the natural home of arcane lore.
- **Woodland mansion chests** — a rare exploration reward.
- **Librarian villager trade** (higher tier) — emeralds + lapis, low quantity / no restock; the
  arcane "knowledge vendor" now that the Master Smith villager is cut.
- **Witches & evokers** — a rare, rate-limited drop (same throttle as the tempering mob drop).

(Implementation reuses the existing `ManuscriptLoot` / `ManuscriptDrops` framework — just add the
arcane entries to those pools.)

### Entry content (draft — drops into an `ArcaneText` class at build time)

**Entry 0 — Directions Index** *(the forced Basics entry; the how-it-works text precedes this list)*
- **Onslaught** — Power · Piercing · Heavy Swing · Glass Edge
- **Hunter** — Slayer · Bane · Executioner · Marked Prey · Riposte
- **Cruelty** — Sundering · Withering · Crippling Blow · Concussion · Lifedrink
- **Momentum** — Swiftness · Nock · Bloodlust · Bloodbath · Overreach
- **Persistence** — Tough · Reach · Mend
- **Bite** — Haste · Specialist · Deepcut · Overclock
- **Bounty** — Kindling · Scholar · Telekinesis · Vein Breaker
- **Gale** *(mace)* — Windburst · Shockwave · Updraft
- **Storm** *(trident)* — Stormcall · Riptide · Tideborn
- **Ward** — Bulwark · Emberguard · Bracing · Deflection · Bulwark Stance
- **Stride** — Fleet · Cushioned · Current · Kinetic Plating · Adrenal Surge
- **Vigor** — Stoutheart · Rooted · Second Wind · Grave Ward · Berserker's Hide

**Per-attribute entries** *(one each — "how to get it" = brew the selector in a slot shaped to that
direction; `Upgrades:` filled in later)*

*Onslaught*
- **Power** — selector **Iron Ingot**. + damage. Drawback −attack speed. Upgrades: TBD.
- **Piercing** — **Flint**. Ignore a very small % of the target's armor. Drawback −knockback dealt. Upgrades: TBD.
- **Heavy Swing** — **Heavy Core**. Hold left-click to charge, up to **1.6×** over 5 s. Drawback −attack speed (≤0.4). Upgrades: TBD.
- **Glass Edge** — **Glass Pane**. + damage (L1 +0.3 · L2 +0.5 · L3 +0.8). Drawback: breaks in ≤100 hits. Upgrades: TBD.

*Hunter*
- **Slayer** — **Bone**. + damage vs undead (max +1.5). Drawback −damage vs others. Upgrades: TBD.
- **Bane** — **Spider Eye**. + damage vs arthropods (max +1.5). Drawback −damage vs others. Upgrades: TBD.
- **Executioner** — **Wither Rose**. +1 damage vs targets under 30% HP. Drawback −2→−1 vs over 30% (by level). Upgrades: TBD.
- **Marked Prey** — **Arrow**. A melee hit marks; next arrow hit does ×1.3/1.6/2.0 (by level). Drawback −1 melee damage. Upgrades: TBD.
- **Riposte** — **Shield**. +1 damage if you strike soon after being hit. Drawback −1 on unprovoked strikes. Upgrades: TBD.

*Cruelty*
- **Sundering** — **Iron Nugget**. Hit shaves a tiny % off the target's armor (≤3%, non-stacking). No drawback. Upgrades: TBD.
- **Withering** — **Nether Wart**. A very weak wither tick. Drawback durability −80%→−50% (by level). Upgrades: TBD.
- **Crippling Blow** — **Cobweb**. Brief Slowness/Mining-Fatigue on the target. Drawback +knockback dealt. Upgrades: TBD.
- **Concussion** — **Slime Ball**. Extra knockback + brief slow. Drawback −damage. Upgrades: TBD.
- **Lifedrink** — **Ghast Tear**. 25% of damage dealt returns as health. Drawback: no other healing while it's on you / dropped within 25 blocks (chest exempt). Upgrades: TBD.

*Momentum*
- **Swiftness** — **Sugar**. +attack speed. Drawback −per-hit damage. Upgrades: TBD.
- **Nock** — **String**. +bow draw / crossbow reload. Drawback −projectile damage. Upgrades: TBD.
- **Bloodlust** — **Blaze Powder**. Kill → Speed III (stacks). Drawback Slowness when no kill in 20 s. Upgrades: TBD.
- **Bloodbath** — **Gunpowder**. Kill → +2 damage (stacks; stacks with Bloodlust). Drawback −2 damage when no kill in 20 s. Upgrades: TBD.
- **Overreach** — **Bamboo**. +0.5 blocks reach. Drawback −1 damage. Upgrades: TBD.

*Persistence*
- **Tough** — **Netherite Scrap**. +% effective durability. Drawback −break speed. Upgrades: TBD.
- **Reach** — **Stick**. +% block/attack reach. Drawback −break speed. Upgrades: TBD.
- **Mend** — **Bottle o' Enchanting**. Slow passive self-repair while held. Drawback −break speed. Upgrades: TBD.

*Bite*
- **Haste** — **Redstone**. +% break speed. Drawback −durability. Upgrades: TBD.
- **Specialist** — **Diamond**. +% break speed on the tool's native material. Drawback −speed on others. Upgrades: TBD.
- **Deepcut** — **Cobbled Deepslate**. Break speed rises the lower your Y. Drawback slower above sea level. Upgrades: TBD.
- **Overclock** — **Blaze Powder**. Large +break speed. Drawback extra hunger per block. Upgrades: TBD.

*Bounty*
- **Kindling** — **Coal**. Drops come pre-smelted. Drawback −durability. Upgrades: TBD.
- **Scholar** — **Bottle o' Enchanting**. +% XP from blocks & mobs. Drawback −break speed. Upgrades: TBD.
- **Telekinesis** — **Ender Pearl**. Drops + XP go straight to your inventory. Drawback −break speed. Upgrades: TBD.
- **Vein Breaker** — **TNT**. Breaks up to a few connected same-ore blocks. Drawback heavy durability cost. Upgrades: TBD.

*Gale (mace)*
- **Windburst** — **Wind Charge**. A slam releases a knockback wind burst. Drawback −direct slam damage. Upgrades: TBD.
- **Shockwave** — **Breeze Rod**. Fall-slam AoE damage (scales with fall). Drawback −single-target damage. Upgrades: TBD.
- **Updraft** — **Phantom Membrane**. A landed slam launches you back up. Drawback you always take the fall damage. Upgrades: TBD.

*Storm (trident)*
- **Stormcall** — **Lightning Rod**. A hit/throw calls a tuned lightning bolt (cooldown). Drawback throw cooldown / −throw damage. Upgrades: TBD.
- **Riptide** — **Prismarine Crystals**. Enhanced riptide dash (usable in rain). Drawback high durability use. Upgrades: TBD.
- **Tideborn** — **Nautilus Shell**. + damage/utility while wet. Drawback − when dry. Upgrades: TBD.

*Ward*
- **Bulwark** — **Iron Ingot**. +% physical protection. Drawback −movement speed. Upgrades: TBD.
- **Emberguard** — **Magma Cream**. +% fire protection + shorter burns. Drawback −physical protection. Upgrades: TBD.
- **Bracing** — **Gunpowder**. +% blast protection. Drawback −movement speed. Upgrades: TBD.
- **Deflection** — **Feather**. +% projectile protection. Drawback −movement speed. Upgrades: TBD.
- **Bulwark Stance** — **Shield**. Large damage reduction while sneaking. Drawback deeper −move speed while sneaking. Upgrades: TBD.

*Stride*
- **Fleet** — **Sugar**. Minor +movement speed. Drawback medium −armor toughness. Upgrades: TBD.
- **Cushioned** — **Slime Ball**. Fall-damage reduction + shorter fall-stun. Drawback −knockback resistance. Upgrades: TBD.
- **Current** — **Prismarine Shard**. +swim speed / depth strider. Drawback −walking speed (mild). Upgrades: TBD.
- **Kinetic Plating** — **Piston**. 1.6× protection while moving, 0% standing still (floor 0→25→50% by level). Upgrades: TBD.
- **Adrenal Surge** — **Blaze Powder**. Taking a hit grants a brief speed kick. Drawback penalty while undamaged. Upgrades: TBD.

*Vigor*
- **Stoutheart** — **Golden Carrot**. +% max health. Drawback −movement speed. Upgrades: TBD.
- **Rooted** — **Iron Block**. +% knockback resistance. Drawback minor −movement speed. Upgrades: TBD.
- **Second Wind** — **Glistering Melon**. Low health → brief defense + regen burst (cooldown). Drawback −max health. Upgrades: TBD.
- **Grave Ward** — **Totem of Undying**. Survive one lethal hit at 1 HP (long cooldown). Drawback −regen and −2 hearts max health. Upgrades: TBD.
- **Berserker's Hide** — **Fermented Spider Eye**. +your own damage. Drawback −your own defense. Upgrades: TBD.

---

## 8. Already in place vs. to build

**Reused:** vanilla enchant neutralization; Arcane codex track; manuscript + inscribe/reveal flow;
component + tooltip patterns; station-block + screen-handler patterns (copy Tempering Station).

**To build:** the **Activation Station** block + brewing menu; the **per-slot enchant component**;
**direction offers** on the vanilla table; the **buff application layer** (additive % modifiers +
the §6 custom effects); **per-stat caps**; the **reagents** + **Arcane entries** (Basics + one per
reagent) + forced-first-entry reveal.

---

## 9. Open items remaining

- **Reagent mapping** — first pass done (§4); refine the item picks and the attribute-specific
  upgrade list.
- **Per-effect tuning** — exact magnitudes + the L1→L3 curves + per-stat caps (the big tuning pass).
- **A few specifics** — Heavy Swing's level→charge-cap curve; Lifedrink's lifesteal % by level;
  Bloodlust/Bloodbath buff durations; Gale/Storm cooldowns.
- **Station costs** — XP / lapis per direction roll and per attribute-select / upgrade.
- **Recolor** — render-tint the table vs. swap to an "Advanced" look-alike block (impl. choice).
- **Naming** — Gale / Storm / Stoutheart and the custom reagents are still placeholders.

# Level-20 Rituals — design spec

Status: **design only, not yet built.** This is the canonical reference for when we
implement the L20 ritual layer. Captures the twelve per-gear rituals, how each is
detected server-side, the custom content needed, soft-failure behaviour, and the shared
frameworks worth building once.

See also: `gear_overhaul_scope.md` (the original scope table) and `project_brief_for_ai.md`
(pillars). This doc supersedes the ritual rows in those where they differ.

---

## 1. What the ritual is, mechanically

The L20 ritual is the **active gate between temper 19 and the t21–25 endgame**. Tempering
levels run 1–19 and 21–25; **level 20 has no station recipe and exists only as the ritual**
(`TemperTableLoader` rejects any `t20:` line; `TemperingStationScreenHandler.nextLevel`
refuses the 19→20 step; `TemperTable.getEffective` falls back to t19 stats for level 20).

Each gear **type** has one ritual (it is per-type like the L10 milestone, not per-rarity).
Performing it:

1. Requires the piece at **temper 19** (plus any ritual reagents the design calls for).
2. On success, advances it to **temper 20**, which re-opens the station for 21→25.
3. Is a **once-per-piece** act (a piece already ≥20 cannot re-run it).

### What reaching 20 unlocks beyond the level number
- **t21–25 headroom** — the station can now climb to 25 (the final +0.10 weapon overshoot,
  full armour restoration, etc., per the tempering tables).
- **Weapon attack-speed restore** — `GearNerf` applies a flat **−0.2 attack speed** at temper
  0, commented as "restored by the level-20 ritual." Implementation choice for later: have
  `TemperStats` stop applying (or counter) that penalty once `level >= 20`, OR add a
  `+0.2 attack_speed` buff in the tables from t21. (The iron t25 `+0.10 attack_speed` is a
  separate, additional overshoot.)
- **Special-gear "vanilla restored" gate** — `SpecialScaling.restoreFraction` only reaches
  `1.0` at temper 25, and you cannot temper past 19 without the ritual, so the bow/crossbow/
  trident/mace only regain full vanilla power *after* the ritual. The ritual is that gate.

---

## 2. Core principles

- **Soft failure.** A botched ritual never destroys gear. Cost = consumed reagents + time;
  at worst it **nudges the piece down one level** (19→18) so you re-climb a step. Tension
  comes from the challenge, never from dread of losing the item.
- **A distinct verb per gear.** Twelve different physical acts so rituals never blur into one
  chore. (Verb audit in §4.)
- **Server-authoritative.** All conditions are validated server-side; the client only
  requests actions and shows state. Most of it rides vanilla itemstack/attribute sync.
- **State on the itemstack.** Any ritual that accumulates progress over time stores it in a
  component on the piece (mirrors `UsageComponents`), so it survives relogs. Decide
  pause/continue/reset on unequip per ritual (only the leggings really needs this).
- **Ceremony.** Hitting a ritual should feel like a deliberate event you set out to do and
  pull off — "the night I caught lightning for my trident."

---

## 3. The twelve rituals

Each entry: the act, the custom content, the server-side detection, and soft-fail behaviour.

### Sword — Forge & quench  *(verb: heat-treat)*
- **Act:** Insert the blade in a **Forge**; a heat gauge climbs cold → cherry → white-hot →
  ruined. Pull it in the right band and **quench** in the trough within a short window.
- **Custom:** Forge block (+ block entity), Quench Trough block. Possibly a quench-medium item.
- **Detection:** Block-entity ticks a heat value; player pull/quench interactions read it;
  pass if quench lands while heat is in the target band and within the window.
- **Soft fail:** Over-heat or mistimed quench → no level gain, retry (over-heat may cost
  durability, not the level).

### Axe — Timber rhythm  *(verb: timing)*
- **Act:** Place a hardened log on a **Stump** and strike it **on the beat**; off-beat strikes
  reset the run. Land the full sequence to split it.
- **Custom:** Stump block (+ block entity driving the beat: sound/particle pulses).
- **Detection:** `AttackBlockCallback` registers each strike; compare strike tick to the
  server-driven beat; count consecutive on-beat hits; reset on a miss.
- **Soft fail:** Miss the rhythm → run resets, retry freely.

### Pickaxe — Vein chase  *(verb: timed mine)*
- **Act:** A custom ore vein that **branches as you mine** — breaking one block exposes the
  next adjacent vein block. Clear the whole branching vein before it **reseals**.
- **Custom:** Vein-chase ore block (placed/seeded by a starter block at depth).
- **Detection:** `PlayerBlockBreakEvents.AFTER` on a vein block reveals neighbours and
  advances a timer; pass when the vein is fully cleared before the reseal timer expires.
- **Soft fail:** Too slow → vein reseals, retry.

### Shovel — Avalanche escape  *(verb: dig-free)*
- **Act:** Trigger a **collapsing column** of falling gravel/sand above you and **dig yourself
  free before you suffocate.** The shovel saves your life.
- **Custom:** Trigger block (or rigged spot) that spawns the column.
- **Detection:** Spawn `FallingBlockEntity` column on trigger; per-tick check the player is
  not buried/suffocating and has broken free within the window; pass on escape.
- **Soft fail:** Get buried → suffocation damage (survivable), ritual fails, retry.

### Bow — Skeet  *(verb: lead a mover)*
- **Act:** A **launcher** flings clay targets into the air; **shoot N of them out of the sky.**
  The skill is leading a moving target.
- **Custom:** Skeet-launcher block; clay-target entity (or reuse a small thrown entity).
- **Detection:** Launcher spawns targets with upward velocity; detect arrow→target collision;
  count hits within the round.
- **Soft fail:** Miss the round's quota → restart the round.

### Crossbow — The Long Shot  *(verb: distance)*
- **Act:** Hit a small **bullseye** from **~80+ blocks**. The restored bolt velocity flattens
  the arc enough to make it possible; the ritual showcases the very trait it unlocks.
- **Custom:** Bullseye target block.
- **Detection:** Record the bolt's **origin** when fired; on bolt→bullseye hit, compute
  distance from origin; pass if ≥ threshold.
- **Soft fail:** Short or missed shot → no effect, retry.

### Trident — Lightning catch  *(verb: electrify)*
- **Act:** During a **thunderstorm**, throw the trident so it passes **≥15 blocks above ground**
  in the **same X/Z column as a lightning rod**. **1-in-5 per qualifying throw**, lightning
  strikes the trident: it **freezes mid-air and floats ~3× a normal bolt's flash**, then drops.
- **Custom:** Rod-altar (or use a vanilla lightning rod) — no dedicated block strictly needed.
- **Detection:** Extend the trident tick path (cf. existing `TridentEntityMixin`): while
  thrown + storm + over a rod + height ok, roll once per throw; on hit, summon
  `LightningEntity` at the trident, zero its velocity/gravity for the extended flash, restore
  after. Pass on strike.
- **Soft fail:** None needed — you just keep throwing through the storm.

### Mace — Three-plate bounce  *(verb: aerial combo)*
- **Act:** Slam a **Striking Plate**; it launches you high enough to rebuild a full slam; steer
  to the next plate; **slam 3 distinct plates in a row without touching the ground.**
- **Custom:** Striking Plate block (craft/place 3).
- **Detection:** On a falling mace-hit on a plate (cf. the L10 mace fall tracking), apply
  upward velocity tuned to restore full slam height; track the combo of **distinct** plates;
  **reset on landing on any non-plate.** Pass at 3.
- **Soft fail:** Touch ground / repeat a plate → combo resets, retry.

### Helmet — Open-water seal  *(verb: endure depth)*
- **Act:** Place the helmet (custom placement) in **deep open water**, then **stay within 5
  blocks** of it, submerged, for a **duration**. Requires a genuinely clear area — forces a big
  dig or open ocean, not a cheesed pocket.
- **Custom:** Helmet sealing placement (minimal custom block/interaction).
- **Detection:** At activation, **once**, scan a **~18-block radius** for solid blocks (reject
  if too enclosed) and check depth (Y). Per-tick: player submerged + within 5 blocks; run a
  timer. Pass on timer complete. (Scan at start only — never per tick.)
- **Soft fail:** Leave the radius / surface early → timer resets, retry.

### Chestplate — The charge  *(verb: survive blast)*
- **Act:** Craft a **bomb item**, equip/arm it (a short dread-fuse with rising beeps), and
  **survive the point-blank self-blast** while wearing the tempered chestplate.
- **Custom:** Bomb item (gunpowder + TNT + thematic binder). *Name TBD — placeholder.*
- **Detection:** On use, schedule a fuse, then `createExplosion` centred on the player;
  `AFTER_DAMAGE` with an explosion source + survival while wearing the tempered chestplate →
  pass. Blast tuned to be lethal in anything less than a t19 chestplate.
- **Soft fail:** If the blast kills you, the **chestplate keeps its temper** (items drop
  normally, recoverable). The gamble is your inventory, never the gear.

### Leggings — Freeze & cold-trek  *(verb: distance)*
- **Act:** Use a **Freeze Machine** to apply the frozen state, then bank **100 distinct chunks**
  on foot **without taking fall damage** (fall damage resets it).
- **Custom:** Freeze Machine block.
- **Detection:** Frozen flag + a **visited-chunk set** (≤100 chunk-IDs) stored on the leggings
  component. Per-tick: add the current chunk if new; on fall damage, clear the set + frozen
  flag. Pass at 100. **Unequip = pause & resume** (state persists on the piece). The set (not a
  bare counter) is what defeats circle-walking.
- **Soft fail:** Fall damage → reset to zero, re-freeze and re-trek.

### Boots — Heat-bed walk  *(verb: continuous contact)*
- **Act:** Walk a continuous path of **heat-bed blocks** end to end **without stepping off.**
  The short/hot mirror of the leggings' long/cold trek.
- **Custom:** Heat-bed block.
- **Detection:** Per-tick check the block under the player is a heat-bed; track run length;
  reset on stepping onto a non-heat-bed block. Pass at the required length.
- **Soft fail:** Step off → run resets, retry.

---

## 4. Verb audit (no blurs)

heat-treat · timing · timed-mine · dig-free · lead-a-mover · flat-distance · electrify ·
aerial-combo · endure-depth · survive-blast · distance-trek · continuous-contact.

Deliberate mirrors: **bow (lead a moving target)** vs **crossbow (flat long distance)** stay
distinct; **boots (short/hot/acute)** vs **leggings (long/cold/sustained)** intentionally
echo each other.

---

## 5. Shared frameworks (build once)

- **`RitualGate`** — analogous to `UsageGate`: maps gear-type → ritual kind, holds the
  completion check, and gates the 19→20 advance. The station/ritual flow consults it just like
  the L10 milestone consults `UsageGate`.
- **Base ritual block + block entity** — "insert gear, track state, validate, advance level on
  success, soft-fail on abort." Reused by the Forge, Stump, Freeze Machine, helmet placement,
  pickaxe starter, etc.
- **Ritual progress component(s)** on the itemstack (mirror `UsageComponents`) for the stateful
  rituals — chiefly the leggings frozen-flag + visited-chunk set.
- **Soft-failure helper** — consume reagents and optionally nudge the level down by one.
- **Rhythm/beat helper** — for the axe (server-driven beat + on-beat strike check).
- **Reuse what exists:** `TridentEntityMixin` (trident behaviour), the mace fall-slam tracking
  and the helmet submersion sampler from `usage/`, and the `AttackBlockCallback`/break-event
  patterns already used for the L10 milestone.

---

## 6. Custom content inventory

**Blocks (~10–11, several sharing the base ritual block):** Forge, Quench Trough, Chopping
Stump, Vein-chase starter/ore, Avalanche trigger, Skeet launcher, Bullseye target, Striking
Plate (×3 placed), Helmet sealing placement, Freeze Machine, Heat-bed.

**Items:** Bomb item (chestplate), clay skeet targets (bow — may be launcher-spawned, not
inventory items), plus any ritual reagents (quench medium, fuel, binders) we decide on.

**No dedicated block / condition-led:** Trident (vanilla lightning rod suffices), Chestplate
(item only), Helmet (minimal placement). Lines up with the scope doc's "≈3 need no block."

---

## 7. Locked decisions

- Twelve rituals as in §3.
- Sword = forge heat-gauge (not lava-dunk).
- Pickaxe = vein chase (dropped the deep-dark resonance idea).
- Bow = skeet (dropped the draw/tension and the 3D draw-rig — no custom animation work).
- Axe = timber rhythm. Crossbow = long shot.
- Trident strike chance: **1-in-5 per qualifying throw**; min height **15**; float ≈ **3×**
  normal flash.
- Helmet clearance radius: **18** (checked once at activation).
- Leggings unequip mid-trek: **pause & resume**.
- Chestplate bomb item name: **placeholder, TBD.**

## 8. Open / deferred

- Final wording/lore for each ritual (player-facing), in the same vague-hint vs exact-Codex
  split as the L10 milestones.
- Exact reagent costs per ritual.
- Where the weapon attack-speed restore is applied (TemperStats at ≥20 vs a t21 table buff).
- Whether the Codex grows a "Rituals" track (and the manuscript-type tree-filter rework the
  Arcane/Ritual sections will need) — out of scope here; tracked with the Codex re-layout.

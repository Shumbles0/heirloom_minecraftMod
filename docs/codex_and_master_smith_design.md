# Heirloom — Codex & Master Smith (design + session handoff)

Design notes for the knowledge-progression book and the villager that seeds it.
Decisions locked in the session of 2026-05-30. Not yet implemented.

---

## Build state so far (what's already done & verified)

Loader/env: **Fabric, MC 1.21.11**, Yarn mappings, Java 24 (JDK at
`C:\Users\Shumbles\.jdks\temurin-24.0.2`). Mod id **`heirloom`**, package
`com.shumbles.gearoverhaul`. Build/run:
`JAVA_HOME=<jdk>; ./gradlew compileJava | runClient | runServer`.

- **Gear nerf** (`foundation/GearNerf`, via `DefaultItemComponentEvents.MODIFY`):
  swords +1.0 dmg, non-swords 0 bonus, armor ×0.05, all tools mining speed 1.05,
  weapons −0.2 attack speed (restored by the ritual later).
- **Enchant neutralization**: all 43 vanilla enchants overridden to empty effects
  (`data/minecraft/enchantment/*.json`).
- **Temper level**: `temper_level` int component (0–25), light-blue tooltip line,
  dev command `/heirloom temper get|set|add`.
- **Temper tables**: custom text format `data/heirloom/tempering/*.txt`
  (`[item]` sections, `t<level>: materials -> buffs`, buffs are absolute totals,
  level 10 present/use-gated, 20 omitted/ritual). Loader = reload listener with a
  loud non-fatal validator + completeness check. `TemperStats` bakes buffs into
  `attribute_modifiers`, reusing base modifier ids so totals render natively.
- **Reagents**: 5 shared items (Dust→Flux→Alloy→Core→Quintessence) + Heirloom
  creative tab + 10 shapeless recipes (vanilla path + "chain" path each).

Remaining core feature before tempering is playable: the **Tempering Station** block
(reads tables, consumes mats, calls `Tempering.setLevel`). Not started.

---

## The Codex system (teaches the mod, paced like the progression)

Three parts:

1. **The Codex** — custom book item, written in a master smith's voice so it teaches
   *and* carries the philosophy. Opening shows only unlocked entries. Unlock state
   lives on the itemstack via a data component.
2. **Manuscripts** — **TYPED** (decision). Multiple manuscript items, each tied to a
   topic track. Found in loot + bought from the Master Smith. Useless alone.
3. **Scribing block** ("Study Desk"): slot for the Codex; feed **3 manuscripts of a
   type** to inscribe the next entry in that type's track, consuming them.

Entry arc (the teaching curve — paces with gameplay):
1. Why your gear is weak (premise/philosophy)
2. Tempering & the station
3. Reagents & sourcing
4. Level-10 milestone (use the gear)
5. Level-20 rituals
6. Enchanting as identity, not power

### Open sub-decisions (typed manuscripts)
- **What are the types & tracks?** Proposed: `Forging` (tempering), `Rite` (rituals),
  `Arcane` (enchanting) — 3 tracks, each with a sequential set of entries. Confirm
  the set and which entries belong to each.
- **Same-type rule:** assume 3 of the *same* type → next entry in that track. Confirm.
- **Loot distribution:** which manuscript types in which chests, and relative rarity
  (e.g. Forging common/village+mineshaft, Rite mid/temples+monuments, Arcane
  rare/strongholds+ancient cities).

---

## The Master Smith villager

**Existence mechanic (decision): custom village-only job block.**
- New profession **Master Smith**, its own POI bound to a new **uncraftable** block
  ("Master's Forge").
- The block **only generates in village smith buildings** (replace the vanilla job
  block there during worldgen), so the village smith becomes a Master Smith and
  restocks trades normally. Players can't craft the block → can't farm Master Smiths.
- This is the "replace the blacksmith" the player wanted, done cleanly without the
  blast-furnace farming loop.

Trade ladder (novice → master):
- Novice: the Codex + manuscripts for emeralds (core trade; restocks normally)
- Apprentice: Tempering Dust
- Journeyman: Tempering Flux + the Study Desk block
- Expert: Tempering Alloy
- Master: Tempering Core (pricey). **Never Quintessence.**

**Custom upgrade materials = one-time leg-up, NOT a farm (decision).** Every reagent
trade (Dust through Core) sells **very low quantities** (e.g. 1 per purchase) and
**never restocks at all** — the villager is a small head-start, not a material tap.
Manuscripts (and the Codex/Study Desk) remain the normal restockable trades.

> Implementation caveat: vanilla restock resets *all* of a villager's trades' uses at
> once, so "this specific trade never restocks" isn't a native per-trade property. To
> enforce it, the reagent offers need custom handling (e.g. mark them and skip them
> when the trade list is refreshed / re-locked), or simply give them `maxUses = 1` and
> additionally prevent their use-count from resetting. Decide the mechanism at build
> time; the intent is: buy once, in small amounts, then it's gone for good on that
> villager.

### Open sub-decisions (Master Smith)
- **Fate of the vanilla Armorer:** remove it entirely (player said "noone likes
  him"), or leave player-placed blast furnaces still making Armorers and only convert
  the *village* armorer house? Recommend: keep Armorer for player blast furnaces,
  just swap the village house's job block → Master's Forge. Confirm.
- **Worldgen integration method:** structure processor that swaps the armorer house's
  blast furnace → Master's Forge (cleanest), vs jigsaw edits. Implementation detail.
- **Master's Forge function:** job site only + decorative, or also interactable?
  Default: job site + decorative.
- **Manuscript trade type:** if manuscripts are typed, does the Master Smith sell all
  types, a random subset per villager, or only `Forging`? Affects how players get the
  rarer tracks (loot vs trade).

---

## Suggested implementation order (next session)

1. **Manuscripts** (typed items) + **Codex** item (with `entries_unlocked` per-track
   component) + loot-table injection. Cheap, unblocks everything.
2. **Study Desk** block + block entity + screen: Codex slot + 3-manuscript intake →
   unlock logic. (Reuses patterns we'll also use for the Tempering Station.)
3. **Codex reading GUI** + the actual entry text (the teaching content).
4. **Master Smith**: POI + profession + trades + the Master's Forge block.
5. **Worldgen**: swap the village armorer house's job block → Master's Forge.

Note: the **Tempering Station** (separate, already-planned big feature) shares the
"custom block + block entity + screen handler + screen" skeleton with the Study Desk —
worth building one first and reusing the pattern.

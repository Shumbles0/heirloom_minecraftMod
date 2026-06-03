# Gear Overhaul — System & Asset Scope Map

A consolidation of everything designed so far. This is the **vanilla+ expanded** build — TrialShrine, Mediumcore, and the boss mods are *disabled* for this one. Scope below covers only this project.

Design throughline: two orthogonal progression axes. **Tempering** is the slow vertical magnitude axis (deterministic, 0–25). **Enchanting** is the horizontal specialization axis (tradeoff-based, no magnitude ladder). Vanilla gear and enchants are nerfed to near-nothing as the foundation; both systems are how power is earned back.

---

## 1. Foundation — vanilla nerfs (datapack / config, no assets)

| Task | Notes |
|---|---|
| Nerf vanilla gear base stats to near-nil | Tempering restores them. **Split by function:** armor may approach zero at temper 0; **weapons and tools need a functional floor (~40–60% vanilla)** or the early game softlocks (can't mine/fight to gather temper materials). |
| Nerf vanilla enchantment effects to near-nil | Enchantments are data-driven JSON as of 1.21 — override the vanilla enchantment definitions and flatten their level-based values. The activation system (§4) restores effect. |
| Custom recipes / loot table edits | For all custom items and blocks below. |

Effort: **low**, high leverage. Mostly JSON. No code or models.

---

## 2. Tempering system

Deterministic upgrade, levels **0 → 20** climbing from near-useless to roughly vanilla, **20 → 25** marginally above vanilla. Small legible steps (tooltip shows level + current bonus). Additive only — **never multiplies enchant power.** Early-accessible (stone/iron temperable turn one). Long via depth: many levels, ramping cost, diminishing-returns tail.

**Blocks**
- **Tempering Station** — the core upgrade block. *(Optional design choice: tier it so higher temper bands require an upgraded/built station — folds building into progression. Not required.)*

**Materials (proposed minimal set — not yet finalized)**
- **Tempering Dust** — common, early levels.
- **Tempering Compound** — mid levels.
- **Tempering Core** — rare, gates the 20–25 tail. Source from exploration to keep that loop relevant.

**Pure logic**
- Per-function scaling curves (armor floor vs. weapon/tool floor; the 0→20→25 shape; slightly front-loaded for early perceptibility is worth testing).

Effort: **medium.** The curve logic is the fiddly part, not the block.

---

## 3. Milestone gates (every 10 temper levels)

### Level 10 — passive "use the gear" (crossed through normal play)
No blocks. One usage-tracking framework, parameterized per gear. Failure-free.

### Level 20 — active ritual (must go for it)
Soft failure: **gear is never destroyed.** Bricking ~5× drops 1–2 levels as an easter egg; intended as a 1–2 try act. Repair is trivial where relevant (e.g. 1 iron ingot for iron leggings, ~1 gold for netherite).

| Gear | L10 (passive use) | L20 ritual (active) | Custom block |
|---|---|---|---|
| Sword | kill-streak without taking damage | thermal quench: heat in lava, quench in special ice (window = ice melting) | **Quenching Ice** (also the consumable lost on failure) |
| Axe | fell a tough mob / large tree | single-strike cleave of a hardened log | **Ironheart Log** (cleaving target) |
| Pickaxe | mine at max depth / clear a big vein | crush-harden under a press (deeper Y = better set) | **Tempering Press** |
| Shovel | total earth moved | kiln-fire packed in a clay shell | **Kiln** + *Clay Shell* (item) |
| Bow | ranged kills | release at **peak tension** on a draw-rig (telegraphed build-up) | **Drawing-Frame** + *Bowstring/Sinew* (item) |
| Crossbow | charged / multi-pierce hits | one bolt pierces a line of aligned targets | **Target Post** (place several) |
| Trident | thrown-trident kills | catch a lightning strike during a storm | **Storm Rod-Altar** |
| Mace | fall-slam hits | max-height slam | **Striking Plate** |
| Helmet | time submerged / survive a near-lethal hit | pressure-seal at ocean-floor depth for a duration | none (depth + timer) |
| Chestplate | total damage absorbed without dying | survive a rigged point-blank blast | none (vanilla explosion) |
| Leggings | distance travelled | freeze, then thaw over **100 new chunks**; **fall damage cracks them** | **Freezer / Cooling Station** |
| Boots | cumulative distance / survive a fall | walk a continuous heat-bed without stepping off | none (vanilla magma) or optional **Heat-Bed** |
| Hoe | — excluded — | — | — |
| *Shield (opt.)* | hits blocked | deflect a heavy incoming hit | TBD |
| *Elytra (opt.)* | distance flown | thread an aerial ring-run | custom rings |

**Theme check:** each ritual is a distinct verb — cool, cut, crush, fire, tension, penetrate, electrify, gravity, submerge, blast, cold-trek, contact — so 12 rituals don't blur into one chore. Boots (short/hot/acute) and leggings (long/cold/sustained) deliberately mirror each other.

Effort: **highest breadth here.** ~9 custom blocks, but 3 gears need none (condition-only), and the rest are "place gear + trigger condition" variants that can share one base ritual-block framework.

---

## 4. Enchantment system (brewing-like activation)

Enchanting table applies an **inert** enchant (easy, vanilla). A second station **activates** it; fed materials determine the buff (specialization, not magnitude).

**Tradeoff model**
- No drawback: pick a buff → **+4%**.
- With a **chosen** drawback: **+15%** buff, **−30%** to the chosen drawback stat.
- Reconfigurable for a material cost (swap loadouts for different content).

**Blocks**
- **Enchant Activation Station** (brewing-stand analog).

**Materials**
- **Payload reagents** — define the specialization. Lean on vanilla where thematic (blaze powder → fire bias, prismarine → projectile, etc.); add custom only for buffs with no vanilla analog.
- **Drawback reagents** — the choosable-penalty inputs.

**Pure logic / balancing (the hard part)**
- Additive buff math + **per-stat caps** so stacked enchants can't sum into creep.
- **Anti-dump constraint:** drawbacks must hit relevant or universal stats (health, movement, attack speed, durability) — never a worthless dump stat, or the −30% is free and the whole tradeoff collapses.

Effort: **medium-high** — code is moderate, **tuning is the real cost** (the tradeoff and anti-dump rules need playtesting).

---

## 5. Early-game armor set (stone-wood alloy)

Replaces the leather-cow tax. Craftable turn-one from gathered materials. **Role: early tempering substrate**, not early protection (all armor starts near-useless).

**Items**
- 4 armor pieces (helmet, chestplate, leggings, boots).
- **Stone-Wood Alloy** composite material (wood + stone + binder).
- Optional **binder** (custom plant fiber, or use vanilla string).

**Decisions**
- Temper ceiling: same as metal ("finish in starter gear" statement) or slightly lower (gentle pull to upgrade). Pick deliberately.
- One character trait (quieter movement / minor knockback resist) so it isn't reskinned leather.

Effort: **low.**

---

## Consolidated asset inventory

**Custom blocks (~11 core)**
1. Tempering Station (§2)
2. Enchant Activation Station (§4)
3. Quenching Ice — sword (also consumable)
4. Ironheart Log — axe
5. Tempering Press — pickaxe
6. Kiln — shovel
7. Drawing-Frame — bow
8. Target Post — crossbow (reusable)
9. Storm Rod-Altar — trident
10. Striking Plate — mace
11. Freezer / Cooling Station — leggings
- Optional: Heat-Bed (boots), Shield/Elytra ritual blocks.
- None required: helmet, chestplate, boots (condition-only).

**Custom items/materials**
- Tempering Dust / Compound / Core (3 tiers)
- Clay Shell (shovel), Bowstring/Sinew (bow)
- Stone-Wood Alloy (+ optional binder)
- Enchant payload reagents (mostly vanilla, few custom), drawback reagents

**Pure-logic systems (no assets)**
- Per-function tempering curves
- L10 usage-tracking framework
- L20 ritual conditions + soft-failure (drop-on-brick, cheap repair)
- Enchant additive math, caps, anti-dump, reconfiguration cost
- Leggings: new-chunk tracking + fall interrupt + thaw progress (+ optional warmth modifier)
- Ritual condition checks: depth/timer, blast-survival, lightning catch, peak-tension read, chunk count, contact traversal

---

## Scope read

- **Biggest drivers:** L20 ritual *breadth* (§3) and enchant *tuning* (§4).
- **Biggest reducers:** a shared ritual-block framework; the 3 condition-only rituals; leaning on vanilla payload materials; datapack-only nerfs.
- **Cheapest high-value work first:** the §1 nerfs and the §2 tempering core — that alone produces a playable, recognizably-Minecraft-but-slowed experience before any ritual or enchant work exists.
- **Open items to finalize:** tempering material set and per-level costs; L10 conditions for bow/crossbow/trident/mace (table has proposals); enchant payload list and the anchored drawback pairings; leggings temper ceiling.

# Project Brief: Vanilla+ Gear Overhaul Mod

A handoff brief for an AI implementing this. Self-contained; a detailed asset/scope map accompanies it (`gear_overhaul_scope.md`).

## Premise
A Minecraft mod that replaces fast tiered power-jumps with slow, deliberate, *earned* gear progression. Vanilla gear and enchantments are nerfed to near-nothing; the player rebuilds power back over a long time through two systems. The goal is **recognizably Minecraft, just slower and more deliberate** — not a total conversion, not an RPG.

## Non-negotiable design constraints (these define the project)
- **Deterministic, never RNG.** No random affixes, no loot-rolls, no rarities. Upgrades are chosen and predictable.
- **Additive, never multiplicative.** Tempering governs base gear stats only; it must never multiply enchant power. The two systems stay orthogonal.
- **Flat curve, no power creep.** Tempering runs levels 0–25: ~useless at 0, ~vanilla at 20, marginally-above-vanilla at 20–25. The ceiling is low; the *length* is the point. A careful player can clear the game underleveled.
- **Feels vanilla.** Vanilla-style materials, stations, and UI. No foreign stat sheets, no rarity colors, no world-tier screens. Every addition should look native.
- **Soft failure.** Gear is never destroyed. Failed rituals cost time/consumables and at worst nudge a level down. Nothing should feel punishing or unfair.

## Technical framing
- **Version:** Minecraft 1.21+ (relies on data-driven enchantments, introduced in 1.21).
- **Loader:** to be decided with the implementer — Fabric (lighter, no external mod deps here) or NeoForge both viable since this build depends on no other content mods. Recommend confirming first.
- **Language/format:** Java for the mod; JSON datapack for the nerfs and enchant definitions.
- **Architecture:** gameplay logic is **server-authoritative** (tempering, gates, ritual conditions, enchant effects); the client only requests actions and displays state. Networking is the standard request → validate → sync pattern, and most of it rides on vanilla's existing itemstack/attribute sync — no custom packets needed beyond the menu actions. Build for **multiplayer (friends server)** from the start.
- **The one thing to decide early — state persistence, not networking:** store per-item ritual progress **on the itemstack** (components/NBT) so it survives relogs. This matters only for rituals that accumulate state over time — chiefly the leggings thaw (chunk count + fall interrupt) — where you must also decide what happens if the player unequips, drops, or trades the piece mid-progress (pause / continue / reset). Instantaneous rituals (quench, slam, blast) have no such state.

## Systems

**1. Foundation (datapack).** Override vanilla gear base stats and enchantment effect values down to near-nil. Split the gear nerf by function: armor may approach zero at temper 0, but **weapons and tools need a functional floor (~40–60% vanilla)** or the early game softlocks (can't mine/fight to gather materials).

**2. Tempering.** A custom station upgrades a piece deterministically, level by level, with small legible bonuses (tooltip shows level + current value). Early-accessible (stone/iron from turn one), long via ramping material cost and a diminishing-returns tail. Materials sourced partly from exploration.

**3. Milestone gates (every 10 levels).**
- *Level 10* — passive: the gear earns it through normal use (per-gear usage condition, no blocks, failure-free).
- *Level 20* — active ritual: a deliberate per-gear "go and do it" act (quench a sword in special ice, catch lightning for a trident, release a bow at peak tension, freeze leggings and thaw them over 100 new chunks without taking fall damage, etc.). Distinct physical "verb" per gear so they don't blur. Soft failure as above.

**4. Enchanting (brewing-like).** The enchanting table applies an *inert* enchant (easy, vanilla). A second station *activates* it; fed materials determine the buff. Tradeoff model: a safe buff (+~4%) or a stronger buff (+~15%) at the cost of a **chosen** drawback (−~30% to another stat). Drawbacks must hit relevant/universal stats (never a dump stat) and buffs need per-stat caps, or the tradeoff collapses into free power. Reconfigurable for a cost. This makes enchanting *horizontal specialization*, not a power ladder.

**5. Early armor set.** A craftable "stone-wood alloy" set from turn-one gathered materials (no leather grind). Its role is an early *tempering substrate*, not early protection.

## Asset inventory (summary)
~11 custom blocks (2 system stations + ~9 ritual blocks, several sharing one base framework; 3 rituals need no block), plus a few custom materials (tempering reagents, alloy, ritual consumables, enchant payload/drawback reagents). Full table in the scope doc.

## Suggested build order
1. Foundation nerfs + tempering core — this alone yields a playable, recognizably-Minecraft-but-slowed game.
2. Level-10 usage tracking (cheap, one framework).
3. One shared ritual-block framework, then the level-20 rituals.
4. Enchant activation system (code is moderate; the **tuning** is the real work).
5. Early armor set.

## Player feeling — the experiential target
When implementation choices are ambiguous, optimize for this:

- **Early game feels vulnerable and deliberate.** Your gear is barely better than bare skin, so you respect everything — every skeleton, every ledge, every dark cave. Survival comes from attention and caution, not stats. This is Minecraft's early tension, extended instead of skipped past.
- **Power feels earned, owned, and tangible.** Every temper level is a small step *you* chose and worked for, not a windfall you looted. The player should grow attached to a specific piece because they invested in *that one* — it has a history. Progress is felt in single legible increments, never handed over in a lump.
- **The world never stops mattering.** Because the curve is flat, there's no "now I'm invincible" cliff and no point where the game becomes trivial and boring. Late-game still has teeth; the reward for progressing is doing things *more capably*, not *carelessly*.
- **Rituals are memorable moments, not chores.** Hitting a level-20 gate should feel ceremonial — you set out to do a specific deliberate thing, you pull it off, and it sticks ("the night I caught lightning for my trident"). Craftsmanship, not a checklist.
- **Enchanting feels like choosing an identity.** Not "stack more power" but "what is this piece *for*?" — bring a fire build to the Nether, a different one elsewhere. The satisfaction is in smart, situational decisions and a loadout that's yours.
- **Failure feels soft and recoverable.** A bricked ritual is "ah well, go again," never "I lost my gear." Tension comes from the challenge, not from dread.

Overall: a survival game where the *journey* is the reward, caution and craft beat speed and stats, and you finish deeply attached to gear you slowly made your own — while it still unmistakably feels like Minecraft.

**Anti-goals to avoid:** grindy tedium, punishing/unfair failure, an ARPG "slot machine" feel, foreign non-Minecraft UI, and any "I won, now I'm bored" power cliff.

## Open decisions to settle before/while building
- Loader choice (Fabric vs NeoForge).
- Tempering material set and per-level cost curve.
- Level-10 conditions for bow/crossbow/trident/mace (scope doc has proposals).
- Enchant payload list and the anchored buff↔drawback pairings.
- Early armor set's temper ceiling (same as metal, or slightly lower).

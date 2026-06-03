# Heirloom — project guide for Claude

A vanilla+ Minecraft gear-progression mod. Premise: vanilla gear/enchants are nerfed to
near-nothing, and power is rebuilt through deterministic **Tempering** (levels 0–25, per
item) plus a planned enchanting rework. Design pillars: **deterministic** (no RNG
outcomes), **additive** (never multiplicative), a **flat low curve** (≈ vanilla by t19,
slightly above at t25), **feels vanilla**, **soft failure** (gear never destroyed).

The full design intent lives in **`docs/`** (`project_brief_for_ai.md`,
`gear_overhaul_scope.md`, `codex_and_master_smith_design.md`) — read those for the "why".

## Environment & build
- **Fabric mod, Minecraft 1.21.11, Yarn mappings, JDK 24** (Temurin).
- Build: **`build.bat`** (one-click; sets JAVA_HOME for the run) → jar in `build\libs\heirloom-<version>.jar`.
  - Or `gradlew build` if `JAVA_HOME` is set. On a new machine, edit the fallback path in `build.bat` or set `JAVA_HOME`.
- Verify changes with `gradlew compileJava`, then `gradlew runServer` / `runClient`.
  - **Always kill stray java first** (`Get-Process java | Stop-Process -Force`) — a leftover server locks the world/port.
  - runServer/runClient never self-exit; boot them in the background, scan the log for `Done (` or exceptions, then kill. Exit code 255 from a kill is expected, not a failure.
  - GUIs can't be driven headlessly — compile + boot validates loading, but visual layout needs a manual in-game look.

## Conventions / gotchas (learned the hard way)
- **Indentation is TABS** in `.java`.
- **`DrawContext.drawText` colors need full alpha** (`0xFF......`). A 6-digit color (`0x3A2A18`) has alpha 0 → invisible in 1.21.11. Fills are fine.
- 1.21.11 API notes: `SwordItem` removed (all items are `Item`); `Item.use` returns `ActionResult`; BlockEntity uses `readData(ReadView)`/`writeData(WriteView)`; `Entity.fallDistance` is a **double**; `getMatrices()` is `Matrix3x2fStack` (pushMatrix/scale/popMatrix); use `new Vec3d(e.getX(),e.getY(),e.getZ())` (no `getPos()` on players here); `Identifier.ofVanilla(...)`. Mixins use MixinExtras (bundled) — `@Local`, etc.
- **Gear nerf runs at registry freeze, BEFORE tags bind** — can't use ItemTags there; detect by registry-id path.
- Temper tables are authored text: `data/heirloom/tempering/<rarity>.txt`. Format per line:
  `t<level>: <count> <item_id|#tag>, ... -> <+value> <attribute>, ...`. Levels 1–19 and 21–25; **t20 is ritual-only and must be absent**. Empty material/buff lists are allowed. Loud-but-nonfatal validator (file:line errors, skips bad lines).

## What's built (status)
- **Foundation**: `GearNerf` flattens vanilla attack/armor/mining/attack-speed; special melee floors (mace 20%, trident +1).
- **Tempering**: station block + menu, `temper_level` component, table loader, dev command. **All tiers authored** (wood, stone, stonewood, copper, gold, leather, chainmail, turtle, iron, diamond, netherite, + special bow/crossbow/trident/mace) — 65 items × ~24 levels.
  - Curve: sword floor 1.0 → 40%@t10 → 100%@t19 → 110%@t25 (+0.10 attack_speed @t25); tools (block_break_speed) scale by mining tier; armor floor 0.05·V → 40%@t10 → 90%@t19 → 100%@t25; diamond/netherite armor also restores toughness.
  - Special non-attribute effects via mixins (`special/SpecialScaling` curve): bow draw 3×→1×, crossbow velocity 33%→100%, trident throw 20%→100%, mace smash 20%→100%.
- **Codex** (`codex/`, `client/Codex*`): overview chapter (always unlocked) + ~325 per-item-per-band recipe chapters + 12 per-gear-type **milestone** entries, collapsible rarity→type→band tree (milestones in their own rarity-agnostic "Milestones" group), scrolling pages. Unlocked by inscribing **Tempering Manuscripts** (pick a rarity, reveal 3, choose 1); milestone entries are seeded into *every* rarity's reveal pool (mixed in, equal per-piece reach). Recipe text built client-side from the bundled tables (`TemperTableLoader.loadBundled`); milestone text in `client/MilestoneText`.
- **Level-10 milestone** (`usage/`): per-gear "use it" gate blocking the 10→11 temper step. Conditions: sword 20-kill streak (resets on hit), axe 200 logs, pickaxe 20 iron+ ore streak (coal/copper resets), shovel 200 earth, bow 10 kills >10 blocks, crossbow 10 hits, trident 5 thrown kills, mace 50 fall-slams, helmet 300s submerged, chestplate 200 dmg absorbed (resets on death), leggings 2800 blocks on foot, boots survive a fall to ≤½ heart. Player feedback is **vague-by-design**: tooltip + station show a lore hint + word-only progress stage (`untested`→`stirring`→`proving`→`nearly proven`), no numbers; the **exact** condition (numbers + reset rules) lives only in the gear's inscribable Codex milestone entry. Station shows "Milestone unmet - <stage>" instead of the temper arrow while blocked at t10.

## Roadmap (not yet built)
- **L20 rituals** — active, per-gear ritual blocks/conditions (deliver the t21–25 headroom + sword attack-speed + special-gear "vanilla restored" gate). Soft failure. See scope doc table.
- **Enchanting rework** — vanilla wiped; path/buff/drawback selection at an activation station; typed manuscripts.
- **Master Smith villager** — village-only job block, reagent trades (never restock, low quantities).
- **Manuscript loot/trades** — manuscripts currently only exist in the creative tab; they need loot tables / trades to be obtainable.
- **Codex** — real visual playtest; polish.

## Author preferences
- The user drives design and approves each step; implement and verify, ask when a decision is genuinely theirs. Keep things lore-flavored but mechanically clear. `iron.txt` was hand-edited by the user — don't revert it.

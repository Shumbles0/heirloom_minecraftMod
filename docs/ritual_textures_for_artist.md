# Pixel-art request — Heirloom ritual blocks & items

Thanks for helping! This is for a private Minecraft mod (a "vanilla+" gear mod). I just need
**13 small textures** for some custom blocks and items. They don't need to be perfect — just
clear and roughly matching Minecraft's look. There are placeholder versions in the game now;
you're replacing them.

## What to make

- **13 PNG images**, named **exactly** as in the tables below (lowercase, with `.png`).
- **Blocks: 16×16**, no transparency needed. Each block uses **one square image on all six
  faces** (it's a plain cube), so just one drawing per block.
- **Items: 16×16 preferred** (32×32 is fine if you want more detail). **Use transparency**
  for items — only the object should be drawn, the rest of the square see-through.
- Hand them back as plain PNG files with those names. That's it — I drop them straight in.

## Style notes (to look right next to vanilla)

- **Light comes from the top-left** (vanilla convention) — top/left edges lighter, bottom/right darker.
- **Keep a small, shared palette** across all of them so the set feels cohesive (a 16–32 color
  palette from https://lospec.com/palette-list works great).
- A little dithering/noise reads better than flat fills. Nothing fancy needed.

## Easiest tool

**Piskel** — https://piskelapp.com — free, runs in the browser, made for game sprites, exports
straight to PNG. (Aseprite / LibreSprite / Paint.NET also fine if you prefer.)

## Blocks (16×16, opaque) — `block/<name>.png`

| Filename | What it is | Suggested colors / motif |
|---|---|---|
| `ritual_forge.png` | A forge where a sword blade is heated | dark stone + glowing ember orange |
| `quenching_trough.png` | A metal trough of water to quench a hot blade | iron/grey metal + blue water |
| `chopping_stump.png` | A wooden chopping stump (top = tree rings) | brown wood, concentric rings |
| `deepvein.png` | A deep ore block whose vein "flees" the pick | dark deepslate + glowing teal ore specks |
| `avalanche_cairn.png` | A pile of loose gravel/stones that collapses | grey gravel, pebbly |
| `skeet_launcher.png` | A contraption that flings clay targets into the air | wood/green machine, a barrel/arm |
| `bullseye_target.png` | A round archery target | concentric red & white rings |
| `striking_plate.png` | A heavy metal plate a mace slams into | steel grey + corner bolts |
| `pressure_seal.png` | A deep-sea device that seals a helmet | dark teal/prismarine + rivets |
| `freeze_machine.png` | A machine that freezes armor | icy light-cyan + white frost |
| `heatbed.png` | A burning floor block you walk across | dark + glowing orange coals/grid |

## Items (16×16 or 32×32, transparent background) — `item/<name>.png`

| Filename | What it is | Suggested colors / motif |
|---|---|---|
| `suicide_charge.png` | A strapped explosive charge (worn, then detonated) | red/black danger stripes + a lit fuse on top |
| `clay_pigeon.png` | A clay skeet disc flung into the air | orange-brown disc with a darker rim |

## Notes

- Don't worry about the in-game folders or any code — just the 13 PNGs with the right names.
- Feel free to riff on the motifs above or do your own thing; they only need to be **distinct
  from each other** and read clearly at small size.
- You can peek at the current placeholders for reference (same filenames) under
  `src/main/resources/assets/heirloom/textures/block/` and `.../item/`.

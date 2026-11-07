# Farmings — Vision

## What it is
A Wildcatter add-on about **raising animals and growing crops the way a modern farm does**, with real buildings, real
processes and real genetics:

field → harvest contraption → conveyor → silo / baler → feed mill → ration → animals → milk, eggs, wool, meat →
processing (chill, separate, ferment, grade) → product

Where vanilla Minecraft gives you a fence and a pile of wheat, Farmings gives you a barn with stalls, a milking parlor, a
bulk tank, a feed mill, a hatchery and a creamery. It runs on Wildcatter's grid, so the pumps, fans, chillers and motors
are real loads on a real supply.

## Pillars (in priority order)
1. **Animals are individuals.** Each has a sex, an age, a breed composition and a genome. Breeding is a decision, and
   it pays off over generations.
2. **The farm is a process.** Feed, health, milking, storage and processing are separate steps with separate equipment.
3. **Realism.** Real equipment, real ratings and real animal science wherever it is playable. Realism constrains how
   things work; it is not the product by itself.
4. **Machines, not tractors.** Field work is done by moving contraptions and conveyors, built from distinct parts.

## Non-goals
- No blockchain, wallets, tokens or NFTs. See `docs/GENETICS.md`.
- No generic "Tier 1-5 machine" ladder, no ore-multiplication-style cheats: a cow cannot give more milk than its genes,
  feed and health allow.
- No "stack N identical blocks" buildings. A parlor or a mill is a multiblock of distinct parts.
- No tractors or vehicles for field work.
- Farmings is not a teaching tool and not veterinary or agronomy advice.

## Animals
- Cattle, sheep and chickens first. Each species has males and females, with real differences (a bull, a ram and a rooster do
  not give milk, wool or eggs the way the female does).
- Several breeds per species, with real breed differences in the traits that matter (see `docs/GENETICS.md`).
- More species later (see ROADMAP Ideas). Adding one means a species doc, a breed list and a genome, not new engine code.
- **Feed.** Cattle and sheep eat hay. Hay bales are crafted (the vanilla hay block) and can also come off the field line as
  bales. Chickens eat grain. A ration is built from what the animal needs, not from one item.

## Field and process
- Fields are worked by **contraptions** that move on rails or tracks and are built the way Tailings builds its tunnel
  boring machine.
- Crops move on **conveyors** to silos, balers, mills and processing equipment.
- Equipment that has a motor draws power from Wildcatter at a real voltage.

## Interop
- **Wildcatter is a hard dependency.** Farmings has no grid, wire or pipe of its own. Power comes through Wildcatter
  receptacles and its motors and loads, as in Tailings.
- Vanilla animals, hay blocks and crops stay valid. Farmings must not break a vanilla world.
- Optional recipe viewers (JEI and similar) only as optional dependencies.

## Art
- Block, item and entity art comes from Ludo, post-processed with shared per-material palettes, as in Wildcatter and
  Tailings. Script-drawn textures are placeholders, named `*_placeholder`, and listed in PROGRESS.md under Known gaps.

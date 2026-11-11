# Farmings — Roadmap

Exactly one phase is marked **CURRENT**. A phase is done when `./gradlew build` and the game tests pass with new coverage for
the phase, PROGRESS.md is updated, and (once it exists) the Field Manual has a page for each new system.

## Phase 0 — Set up — done
1. Docs: CLAUDE.md, VISION.md, GENETICS.md, ROADMAP.md, PROGRESS.md. **Done 2026-11-07.**
2. Skeleton: `common`, `fabric` and `neoforge` modules copied from Tailings, `mod_id=farmings`, group `dev.farmings`, CC0,
   CI that runs `./gradlew build`. Wildcatter 0.4.1 is `compileOnly`, and `runtimeOnly` on NeoForge, as in Tailings.
   `verifyPlainJava` covers `genetics`, `husbandry`, `feed` and `process`. **Done 2026-11-07.**
3. A mod that loads on both loaders next to Wildcatter and does nothing yet. **NeoForge: loads next to Wildcatter 0.4.1 in a
   dev server (2026-11-07). Fabric: builds, not yet run.**
4. Game tests (a NeoForge game-test source set and a Fabric client game test), added when the first real feature needs them.

## Phase 1 — Cattle, one full chain — done 2026-11-09
- Species doc `docs/animals/cattle.md`: sexes, ages, gestation, lactation, feed intake, with sources.
- Cattle with a sex, an age, a breed and a basic genome. A small set of breeds (open decision in GENETICS.md).
- Barn and stalls, a hay feeder and a water trough. Cattle eat hay blocks.
- Milking parlor and a bulk tank with a chiller on Wildcatter power. Spec in `docs/multiblocks/` first.
- Milk goes into the tank, is chilled, and can be taken out as a product.
- Manure collection in the barn (see `docs/waste.md`): a manure item, a dirty-stall rule, hand collection and a powered scraper on Wildcatter power.
- Animal state survives save and chunk unload.

## Phase 2 — Genetics and breeding — done for cattle 2026-11-09 (the visual coat recipe is not built)
- Port the plain-Java `genetics` package from FarmChain (see GENETICS.md), with the determinism tests.
- Mating, pregnancy, calving. Sire and dam choice. Breed composition and labels. Visual recipe for coat and markings.
- Records: a herd book showing parents, breed composition and traits.

## Phase 3 — Fields and processing — the creamery is done (2026-11-09); fields, feed mill and waste are not
- Crops and hay: a field harvest contraption on rails (spec first), a baler, a conveyor line to a silo.
- Feed mill and ration mixer: rations built from what the animal needs.
- Processing: creamery and cheese room, built from distinct real parts on Wildcatter power.
- Waste: composting and fertilizer, and a methane digester with a separator, either alone or one after the other (`docs/waste.md`).

## Phase 4 — More animals — done 2026-11-11
- Sheep (wool, meat, lambing) — **done 2026-11-10** (`docs/animals/sheep.md`, the shed, shearing shed, wool mill, sheep stalls).
- Wild animals and the taming corral — **done 2026-11-10** (`docs/taming.md`). Every species spawns wild and is tamed in a corral.
- Poultry: chickens, turkeys, quail and ducks (eggs, coop and run, duck pond, broody hens, hatchery, brooder, egg grading, meat,
  feathers and down), with sexes, breeds and genes, at 3 real days per game day; wild, tamed in the corral. **Done 2026-11-11**
  (`docs/animals/poultry.md`, `docs/multiblocks/coop.md`, `docs/multiblocks/hatchery.md`, `docs/eggs.md`).

## Ideas (not scheduled, not in code)
- More species (goats, pigs, horses, geese, guinea fowl).
- Animal health and a vet cycle; disease and quarantine.
- Seasons and pasture rotation; manure handling and fertilizer.
- A plain-text genome code (see GENETICS.md; needs a rules check).
- A Field Manual.
- **Easter egg: milk armor** (Tim, 2026-11-07). See `docs/milk-armor.md`.

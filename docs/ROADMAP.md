# Farmings — Roadmap

Exactly one phase is marked **CURRENT**. A phase is done when `./gradlew build` and the game tests pass with new coverage for
the phase, PROGRESS.md is updated, and (once it exists) the Field Manual has a page for each new system.

## Phase 0 — Set up — CURRENT
1. Docs: CLAUDE.md, VISION.md, GENETICS.md, ROADMAP.md, PROGRESS.md. **Done 2026-11-07.**
2. Skeleton: `common`, `fabric` and `neoforge` modules copied from Tailings, `mod_id=farmings`, group `dev.farmings`, CC0,
   CI that runs `./gradlew build` and the game tests. Wildcatter is `compileOnly` and `runtimeOnly` for tests, as in Tailings.
   `verifyPlainJava` covers `genetics`, `husbandry`, `feed` and `process`.
3. A mod that loads on both loaders next to Wildcatter and does nothing yet.

*Approval needed before step 2 (this is the next step).*

## Phase 1 — Cattle, one full chain
- Species doc `docs/animals/cattle.md`: sexes, ages, gestation, lactation, feed intake, with sources.
- Cattle with a sex, an age, a breed and a basic genome. A small set of breeds (open decision in GENETICS.md).
- Barn and stalls, a hay feeder and a water trough. Cattle eat hay blocks.
- Milking parlor and a bulk tank with a chiller on Wildcatter power. Spec in `docs/multiblocks/` first.
- Milk goes into the tank, is chilled, and can be taken out as a product.
- Animal state survives save and chunk unload.

## Phase 2 — Genetics and breeding
- Port the plain-Java `genetics` package from FarmChain (see GENETICS.md), with the determinism tests.
- Mating, pregnancy, calving. Sire and dam choice. Breed composition and labels. Visual recipe for coat and markings.
- Records: a herd book showing parents, breed composition and traits.

## Phase 3 — Fields and processing
- Crops and hay: a field harvest contraption on rails (spec first), a baler, a conveyor line to a silo.
- Feed mill and ration mixer: rations built from what the animal needs.
- Processing: creamery and cheese room, built from distinct real parts on Wildcatter power.

## Phase 4 — More animals
- Sheep (wool, meat, lambing) and chickens (eggs, hatchery, brooder), each with sexes, breeds and genes.

## Ideas (not scheduled, not in code)
- More species (goats, pigs, horses, ducks).
- Animal health and a vet cycle; disease and quarantine.
- Seasons and pasture rotation; manure handling and fertilizer.
- A plain-text genome code (see GENETICS.md; needs a rules check).
- Contract buyers with quality thresholds, as in FarmChain.
- A Field Manual.

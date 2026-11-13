# Progress

Phase 0 is current. The docs and the loader skeleton exist; the mod loads and does nothing yet. No art.

## Phase 0

- **2026-11-07:** CLAUDE.md, `docs/VISION.md`, `docs/GENETICS.md` and `docs/ROADMAP.md` written, with the CC0 license.
  Decided with Tim:
  - Farmings is a Wildcatter add-on with a hard dependency, built like Tailings.
  - Wildcatter-level realism.
  - Cattle, sheep and chickens first, with sexes and several breeds each; animals eat hay bales.
  - No tractors: field work uses moving contraptions and conveyors.
  - Genetics come from FarmChain's engine, **with no blockchain code**.
  - The repository is public.
- **2026-11-07: skeleton built.** Copied from Tailings: Gradle wrapper, `build-logic`, `common`/`fabric`/`neoforge` modules, CI
  (`./gradlew build`). Package `dev.farmings`; plain-Java packages `genetics`, `husbandry`, `feed`, `process` (empty, with
  `verifyPlainJava`). `Farmings.register` only logs. Wildcatter 0.4.1 is a hard dependency in `fabric.mod.json` and
  `neoforge.mods.toml`.
  - `./gradlew build` passes (needs a Java 25 JVM; there is none installed system-wide on this Mac).
  - NeoForge dev server starts, loads Wildcatter 0.4.1 and logs "Farmings loaded; requires wildcatter".
  - Fabric: jar builds; **not run in a game yet.**
- **2026-11-07: Phase 1 begun.** Decided with Tim: time scale 1 game day = 30 real days; cattle breeds Holstein, Jersey, Brown Swiss,
  Gyr, Angus and Wagyu; Wagyu drop more steaks than Angus (a written exception). `docs/animals/cattle.md` first draft written with
  cited sources; many figures are marked TO VERIFY and five items are open DECISIONS. No code yet.
- **2026-11-07: more decisions.** One milking per game day; hay block = 100 kg; UC Davis water rule; first two calves are heifers.
  Waste added (Tim's idea): manure is collected in the barn, then goes to composting/fertilizer, a methane digester, or both one
  after the other. `docs/waste.md` first draft with cited figures; two big decisions open (output per game day, Wildcatter genset
  burning biogas).
- **2026-11-07: time scale changed to 1 game day = 15 real days.** Output per game day = one real day's; machine times scaled 15:1;
  powered manure scraper in Phase 1; fertilizer acts like bone meal; biogas genset proposed in Wildcatter's Ideas; third calf a
  bull; beef cows not milked; hay block 100 kg fresh weight.
- **2026-11-07: research pass.** Heat cycle, stall and feed-bunk sizes, hay dry matter, mature weights, Gyr, Angus and Wagyu figures
  and beef cut yields added to `cattle.md` with sources. Decided with Tim: alley 3 m; first breeding at 15 months; **nothing is
  sold** (milk and beef are food and ingredients for cheese and other items); fertilizer works like bone meal on all crops and
  trees; milk armor Easter egg (`docs/milk-armor.md`). Still to research: per-breed milk peaks and totals, Gyr gestation, beef
  trait numbers.
- **2026-11-07: barn spec drafted** (`docs/multiblocks/barn.md`): tail-to-tail freestall barn, 3 m scrape alley, powered scraper at
  480 V, collection pit, hay feeders, troughs. Eight open decisions (scraper motor size and passes per day, feeder capacity,
  trough count, water source, roof, stall sizes, manure fork, manure item weight), each with a proposal. No code.
- **2026-11-07: barn spec approved.** Water from a pipe and a bucket; no roof or walls in the structure; a new manure fork tool;
  motor 2.2 kW and one pass a day stand.
- **2026-11-07: Phase 1 step 1 built** (plain Java, no Minecraft code): the genetics port and the husbandry rules, with 35 unit tests
  passing and `./gradlew build` green.
  - `genetics`: eight traits, breed composition (basis points, sums to 10,000), allele pool, heterosis, inbreeding, trait expression,
    deterministic offspring, generation-0 founders, canonical genome string and SHA-256 hash. Keyed by the world seed instead of a
    server secret. Not ported yet: visual and discrete loci, tiers, the Monte Carlo gate.
  - Parity with FarmChain: the tests compare against values produced by FarmChain's own TypeScript (RNG, breeding seed, composition
    averaging, and three whole calves with fixed parents and seeds: alleles, sex and all eight trait scores match).
  - `husbandry`: 15:1 time scale, the six breeds (weight, gestation), stages, first-breeding rule (15 months and 55% weight),
    due and lactation days, first-three-calves rule. `feed`: hay block 85 kg dry matter, intake fractions, UC Davis water rule.
  - **Not built, and why:** the lactation curve (no per-breed peak or total is sourced), intake for calves, heifers and bulls (no
    sourced figure), and Angus and Wagyu baselines (open decision). Stand-in values are marked in `CattleBreed.java`.
  - Founders draw alleles uniformly from the seven-allele pool: that is new in Farmings, not FarmChain's.
- **2026-11-07: Phase 1 step 2 built: the cattle entity.** `CattleEntity` (an `Animal`) holds breed, sex, genome, trait scores,
  birth time, weight, pregnancy and calving state, and saves all of it with the world. Age is counted in game ticks and read
  through the 15:1 scale; the vanilla baby flag is driven from our stage (vanilla's own growth timer is off). Vanilla breeding,
  bucket milking and wheat-tempting are off. Registered on both loaders (attributes, renderer, lang). A spawned animal is a
  Holstein adult that can be bred today.
  - NeoForge game-test source set added (`./gradlew :neoforge:runGameTestServer`, also run in CI). 5 scenes pass: `registers`,
    `cattle_spawns`, `cattle_state_survives_save`, `calf_grows_up`, `bull_does_not_milk`.
  - `cattle_state_survives_save` saves through the same write/read path a chunk unload uses and loads into a new entity; it does
    not unload a real chunk.
  - **Not built, and why:** growth (no sourced weight-for-age curve, so weight is set, not grown), feeding and milking (barn and
    parlor), breeding (Phase 2), Angus and Wagyu animals (no baselines yet), spawn egg and a way for players to get cattle
    (`/summon farmings:cattle` for now).
- **2026-11-07: cattle art for six breeds.** Concept art from Ludo (Holstein, Jersey, Brown Swiss, Angus approved as first drawn; Gyr
  and Wagyu redone) approved by Tim; then a coat texture per breed on the vanilla cow model (`tools/cattle_textures.py`, drawn from
  scratch, deterministic) and a Gyr model with a shoulder hump. Dairy cows that have bred show an udder; calves, heifers, bulls and
  beef cattle do not (a second `_no_udder` texture per dairy breed). Holstein, Jersey, Brown Swiss and Angus are polled (horns
  transparent); Gyr and Wagyu keep horns. The client reads breed and stage from two synced values.
  - `tools/preview_cattle.py` renders a texture on the model without the game.
  - **Fabric launched for the first time**: a Fabric client game test (`./gradlew :fabric:runClientGameTest`) builds a world, spawns
    the six breeds, calves and bulls, and takes screenshots (in `fabric/build/run/clientGameTest/screenshots`). It passes.
  - Calves use the vanilla baby model with the adult coat (no hump on a Gyr calf). Every breed is the same size; real size
    differences would need per-breed models or a scale (not done).
- **2026-11-07: Phase 1 step 3a: the barn's forming rules, as plain Java** (`husbandry/barn`: `BarnLayout`, `BarnGrid`, `BarnPart`,
  `Orientation`). It reads the world through a small interface, tries all eight ways the barn can lie (4 turns, mirrored or not),
  and either forms or names the first thing wrong ("Stall 3 north has no divider on its near side"). 12 unit tests: perfect barns
  of 2 to 12 stalls a side, each rule broken in turn, and all eight orientations.
  - **Spec error found and fixed:** the barn sketch could not be built as drawn (no cell for a divider between two beds). A stall now
    takes 2 cells, so n stalls a side take 2n+1 along the barn. Recorded in `barn.md`; Tim to confirm.
  - Not built yet: the blocks themselves (11 parts, built on Wildcatter's big-part system), the controller block entity, the scraper,
    pit and manure, and the game tests that need them.
- **2026-11-08: Phase 1 step 3b: the barn blocks.** Ten true-size parts on Wildcatter's big-part system (stall bed 1x1x3, divider 1x2x3,
  feed bunk, hay feeder, water trough 2x1x1, alley floor, scraper rail, manure scraper 1x1x3, scraper drive, collection pit 2x2x3),
  the barn controller, and its block entity that runs the forming check once a second. Right-click the controller to read it
  ("Barn: FORMED, 3 stalls a side (6 cows)" or the first thing wrong). A creative tab holds everything (no recipes yet).
  - Game tests (NeoForge, 10 scenes now pass): `barn_forms`, `barn_missing_part`, `barn_part_breaks_whole`, `barn_usual_orientation`.
  - Screenshots from a real Fabric client (`FARMINGS_ONLY=barn ./gradlew :fabric:runClientGameTest`): overview, down the alley,
    corner, and one divider missing. Per Tim's rule (2026-11-08) tests are shown with screenshots and moving things with recordings.
  - **Placeholders:** every block uses a one-box model and a flat `*_placeholder` texture (`tools/make_assets.py (was barn_assets.py)` writes them) and a
    simple collision box. Real models and textures are a separate step for Tim's approval.
  - **Not built:** the scraper moving, the pit holding manure, the drive drawing Wildcatter power, manure, the fork tool (step 3c);
    cows using the barn (stalls, feeding, drinking); crafting recipes; mining tags; the milking parlor.
- **2026-11-08: Phase 1 step 3c: the scraper, pit and manure.**
  - **Manure** item (50 kg) and the **manure fork** (right-click: gathers manure within two blocks, one wear per use).
  - **Scraper pass** (`ScraperPass`, plain Java, 9 unit tests): the blade moves a cell at a time pushing manure ahead of it, empties into
    the pit at the end, returns. One pass per game day, or on a redstone signal into the controller. It moves only while the drive is
    powered; it stops with NO POWER, PIT FULL, BLOCKED or NO SCRAPER, and the controller's readout says which.
  - **Drive** = a real Wildcatter motor (3-phase, industrial voltage, 2.2 kW), wired like Tailings' pump. **Pit** stores up to 384 manure;
    an empty hand takes a stack of 64, a click with an item just reads it.
  - Game tests (NeoForge, **16 scenes pass**): `barn_scraper_pass` (15 manure all reach the pit, blade back at rest), `barn_scraper_no_power`,
    `barn_scraper_blocked` (stops in front of a block, finishes when cleared), `barn_scraper_pit_full`, `manure_fork`, `pit_gives_manure`.
  - Evidence: Fabric client scene with a screen recording and four screenshots (`FARMINGS_ONLY=scraper ./gradlew :fabric:runClientGameTest`;
    recording in `fabric/build/scene-videos/scraper-pass.mov`).
  - **Proposals with no source:** blade speed (20 ticks a cell), fork durability 250, drive 2.2 kW (already in the spec).
  - **Not built:** the drive has not been run on a real Wildcatter grid (tests force it on; no-power is tested), no blade-moving animation
    between cells (it steps a block at a time), manure is dropped by hand until cows exist, dirty-stall health effect (needs cows).
- **2026-11-08: Phase 1 step 4: cows live in the barn.**
  - **Stalls:** the controller gives each housed cow the nearest free stall (one cow a stall; calves are not housed). She walks to it, stands on
    the bed and faces her bunk, and nothing else moves her. The stall is saved with the cow.
  - **Feeding:** a player puts hay blocks in a feeder (up to 4, 85 kg dry matter each) and a water bucket in a trough (1,000 L). Each second the
    controller tends the herd: a cow eats her day's dry matter and drinks (UC Davis rule) from the feeder at her stall and a trough in her row.
    A shortfall is recorded on the cow and never kills her. Manure is made from what she ate (2.23 kg per kg of dry matter, the Florida study in
    waste.md) and drops in the alley in front of her stall as 50 kg items, which the scraper collects.
  - **Found and fixed:** (1) the controller's once-a-second tending never ran for barns whose block position hashes negative (`%` of a negative
    number); now `Math.floorMod`. (2) Cattle planned routes straight through the thin stall dividers because the pathfinder thinks a non-full block is
    walkable; dividers, bunks, feeders, troughs, the drive and the blade now say they cannot be walked through. Both found by tests.
  - Game tests: **25 NeoForge scenes pass, 4 runs out of 4 after the fixes** (new: `cows_take_stalls`, `cow_eats_and_drinks`, `cow_makes_manure`,
    `cow_hungry_without_hay`, `feeder_takes_hay_blocks`, `trough_takes_bucket`, `cow_walks_to_stall`, `cow_walks_in_from_the_pit_end`,
    `cow_stall_survives_save`). 10 new plain unit tests (feeding step, stall layout).
  - Evidence: a screen recording and screenshots of six cows walking into a 4-stall barn (`FARMINGS_ONLY=herd ./gradlew :fabric:runClientGameTest`).
  - **Proposals with no source:** trough capacity 1,000 L (one bucket; one cell is a metre); heifers, bred heifers, retired cows, bulls and steers
    eat at the dry cow's 2.2% of body weight (no figure was found for them); a trough with no water is filled by bucket only (the Wildcatter pipe
    feed is not built yet).
  - **Not built:** milk (so no milk-dependent water or yield), health or growth effects from a shortfall (only recorded), the dirty-stall effect,
    the pipe-fed trough, lying down (cows stand on the bed), calves.
- Step 5a (2026-11-08): `husbandry/Lactation` (Wood's curve scaled to the USDA 2019 totals; 6 unit tests). Stand-ins: Brown Swiss and Gyr shapes. Not built: first-lactation reduction, genome-to-yield link, milk in the entity.
- Step 5b (2026-11-08): milk in the cow. `Lactation.yieldFactor` (milk-yield score over the breed baseline, clamped 0.5-1.5, proposed), `producedKg` (times the less of fed and watered, proposed). The cow has days in milk and an udder that refills once per game day (unmilked milk is replaced, not stacked, proposed), saved with her. 26 of 26 game tests (new: `cow_gives_milk`). Bug found: the "has she calved" check used `calvedTick >= 0`, wrong early in a world; now the flag.
  Not built: calving itself (a cow only calves through the test/display call), a right-click readout, the parlor.
- Step 6 (2026-11-08): breeding and calving, natural service (`docs/animals/breeding.md`, approved). `Breeding` (heat, waiting period, calf sex rule; 4 unit tests), cow heat/conception/calving, bull service in the barn, `CalfCounts`. 31 of 31 game tests (new: heifer_comes_into_heat, bull_serves_cow_in_heat, calf_is_born_beside_dam, first_two_calves_are_heifers, sire_survives_save). Bug found: a backdated conception gave a negative tick that read as "not in calf"; now an explicit due tick. Not built: straws, calf growth. **Evidence gap:** the Fabric client scene `FARMINGS_ONLY=calving` is written and compiles, but the client hung loading resources (no usable display), so there are no screenshots or recording of a calf being born yet; the game tests are the only proof. Re-run on 2026-11-08 with the screen awake: it worked; screenshots calving-before/after and calving.mov exist (sent to Tim).
- Step 7 (2026-11-08): calf and heifer growth (`docs/animals/growth.md`, approved). `Growth` (target curve: birth 6%, 15 months at sourced Holstein 425 / Brown Swiss 379 / Jersey 297 kg, mature at 48 months; gain = curve gain x fed fraction; 6 unit tests) and `CattleEntity.grow`. 35 of 35 game tests (new: calf_grows_to_heifer, underfed_heifer_grows_slower, grown_cow_stops_growing, weight_survives_save). Unsourced: birth 6%, 48 months, Holstein's 0.60 for other breeds. Test caveat: the game tests move time with `advanceForTest`, not a real clock. No screenshots or recording of growth yet (same display problem as calving).
- Step 8 (2026-11-08): straws, as approved in `breeding.md`. Items `empty_straw` and `semen_straw` (`StrawItem`); the genome rides in the item's custom data. 36 of 36 game tests (new: straw_inseminates). Straws never expire and the straw art is a placeholder. No screenshot or recording yet (display).
- Step 9 (2026-11-08): milking parlor and bulk tank (`docs/multiblocks/parlor.md`, approved). Plain-Java layout check and `BulkTank` (7 unit tests), blocks and entities, cows walk to a stall and are milked into the tank, chiller, faults, bucket. 45 of 45 game tests (new: parlor_forms, parlor_missing_part, parlor_milks_cow, parlor_no_power_no_milk, tank_cools_and_warms, bucket_takes_milk, parlor_state_survives_save, parlor_cow_walks_and_is_milked, parlor_milks_a_herd). Bug found by the client screenshots, not the tests: the first cow's 35 C milk made the tank WARM and stopped the other cows; warm now only blocks a new session (Tim, 2026-11-09). Evidence: client scene `FARMINGS_ONLY=parlor` (3 screenshots, 16.75 s recording), sent to Tim. The recorder was filming Tim's own open Minecraft menu; `Filming` now raises the test window first (calving re-filmed). Placeholder art.
- Step 10 (2026-11-09): the cattle finish, approved by Tim in plan mode with his popup answers. Commits 61a07a7, 510279d, 1afd552, 89fec3e and the art commit.
  - Dairy: first-lactation cut (85%), twins (5.02% / 0.68% first calf), stillbirths (12.3% / 5.4%), freemartins (90%), the heat cue (hearts), a right-click readout, castration with shears, the dirty-stall effect (all values proposed).
  - Beef: Angus and Wagyu baselines (proposed), crosses, meat by carcass weight (Penn State figures, 1 item = 10 kg), graded steaks (Select/Choice/Prime by marbling), Wagyu more steaks (Tim's rule).
  - Herd book, pipe-fed trough (Wildcatter WaterUser), milk armor (Hunger + Nausea when spoiled).
  - Creamery (`docs/multiblocks/creamery.md`): pasteurizer, separator, churn, cheese vat, press, aging shelf, bottler; milk fat tracked from each cow to the cheese (Van Slyke).
  - Recipes for every block and hand-made item (`docs/recipes.md`).
  - Art: Ludo materials and icons (`art/source/PROMPTS.md`, `tools/process_art.py`), true-size part models built with Wildcatter's model library and materials (`tools/models/`), a Ludo hair grain on the approved coats. No placeholder texture is left.
  - 112 unit tests and 67 of 67 game tests. Client evidence: scenes showroom, breeding, beef, creamery, armor, parlor, scraper (screenshots and recordings, sent to Tim).
- Step 11 (2026-11-09): the weak spots and the dairy connection, approved by Tim in plan mode with his popup answers. Commits 2138ebf to 3c62431.
  - Barn: the scrape alley is twice the stall rows, open stretches at both ends (barn.md).
  - Cow lane and holding pen link a parlor to its barn; the parlor milks only that barn's cows, who walk the lane, wait in the pen
    (15 sq ft a cow, Dairyland Initiative) and walk home (parlor.md).
  - Coat genes from FarmChain's visual genome (MC1R ED > EBR > E+ > e per UC Davis VGL, dilution, pattern, white, face, polled, hump,
    dewlap, ears), inherited and saved apart from the core genome (GENETICS.md).
  - Cattle model and coats from the genes: one model with gene-driven parts and body blends, tinted hair, white markings cut from 3D noise
    (cattle.md, "Models and coats").
  - Worn milk armor drawn from Ludo milk surfaces (milk-armor.md).
  - Creamery: whey by mass balance; hoppers take bottles, butter and wheels and feed the bottler glass (creamery.md).
  - Food spoilage: FoodKeeper fridge lives, a game day out of the cold (proposed, Tim's choice), spoiled items that sicken, freshness bar
    (food.md).
  - Walk-in fridge: insulated panel box, door, evaporator controller, condensing unit sized by a vendor load table (cold-room.md).
  - Art: Ludo icons for whey and spoiled food; insulated panel, evaporator, condensing unit and cow lane surfaces; lane and pen models.
  - Game tests now let go of their forced chunks when they end (with ~1,300 left forced, the run sometimes ran out of heap on a machine
    short of memory).
  - 141 unit tests and 81 of 81 game tests. Client evidence: scenes coats, armor, dairy (recording), fridge (recording), hoppers,
    showroom, barn.
- Step 12 (2026-11-10): sheep, approved by Tim in plan mode with his popup answers (Merino, Suffolk, Dorper, East Friesian; FarmChain's sheep
  genes plus milk yield; own model and coat genes; shed + pasture; shearing shed + mill; all four wool uses; ewes in the same parlor).
  Commits f4c5a6a to the art commit.
  - Spec with sources (`docs/animals/sheep.md`): 17-day cycle, 147-day gestation, first breeding at 7 months and 65-70% of mature weight,
    breed weights, litters, fleece, micron, staple, yield, East Friesian milk, intake, water, housing, dressing.
  - Genetics for more than one species: `Species`, sheep traits and baselines (proposed), sheep coat genes (agouti, extension, brown,
    spotting, head colour, three-allele sex-influenced horns, coat type, crimp). Cattle values are unchanged (pinned in `CattlePinTest`).
  - The sheep: stages, natural service by a ram, litters of 1-3, fleece growth, hand shears, Dorper shedding, East Friesian milk, lamb and
    mutton chops; its own model with a fleece layer sized by wool growth; coats drawn by `tools/sheep_textures.py`.
  - Sheep shed and pasture (`docs/multiblocks/sheep-shed.md`): pens, lambing pens, the barn's hay feeder and trough reused, a fenced
    pasture behind a gate; out by day, in at dusk or in rain; grazing turns grass to dirt.
  - Shearing shed (`docs/multiblocks/shearing-shed.md`): a powered stand calls in ready sheep, skirts the fleece, presses bales.
  - Wool mill (`docs/multiblocks/wool-mill.md`): scouring bowl (water, lanolin), card, spinning frame, loom, felting table; hoppers chain
    them. Wool uses (`docs/wool.md`): yarn in 16 colours, vanilla wool blocks, warm clothing (freeze-immune), felt panels, udder balm.
  - Sheep milk: sheep stalls in the parlor, one milk kind per bulk tank and per creamery, sheep milk bottles and sheep cheese.
  - Game-test harness: a scene detaches its Wildcatter circuit parts when it ends (87 scenes ran the server out of heap: the solver kept
    reloading released chunks).
  - 170 unit tests and 105 of 105 game tests. Art: 24 Ludo icons (`art/source/PROMPTS.md`), yarn tinted to 16 colours.
- Step 13 (2026-11-10): wild animals and the taming corral (`docs/taming.md`), approved by Tim in plan mode with his popup answers
  (replace vanilla cows and sheep; lure with held feed and a bait block through one-way gates; a corral where animals settle over days;
  breed by biome).
  - Natural spawns are wild and replace vanilla cows and sheep at vanilla weights and group sizes; the breed is matched to the biome.
  - Wild animals follow held wheat or hay, keep 6 blocks from empty hands, and don't breed. Barns and sheds don't take them in.
  - The taming corral: post, one-way gate, bait trough, and the barn's water trough. The bait draws wild animals within 32 blocks.
    After 7 real days fed and watered inside (FutureBeef, yard weaning, 5-10 days), the animal belongs to the corral's owner.
  - `/summon`, spawn eggs and newborns are herd animals *(proposed)*.
- Step 14 (2026-11-11): poultry. Approved by Tim in plan mode, with his popup answers:
  - chickens, turkeys, quail and ducks in one pass, on one shared bird base;
  - 3 real days per game day for birds, and one real day's eggs per game day;
  - a coop with nest boxes and a run, a hatchery with an incubator and a brooder, broody hens and a duck pond;
  - vanilla chickens replaced by wild fowl chosen by biome, tamed in the corral on grain;
  - meat, feathers and down, egg grading and cartons, and litter to compost;
  - 3-4 breeds per species.
  - **Spec:** `docs/animals/poultry.md`, `docs/multiblocks/coop.md`, `docs/multiblocks/hatchery.md` and `docs/eggs.md`. Sources cover
    incubation, laying, weights, feed, space, brooding, dressing and manure.
  - **Time scale:** `TimeScale.of(species)`. Cattle and sheep keep 15:1; birds run at 3:1.
  - **Genetics:**
    - species CHICKEN, TURKEY, QUAIL and DUCK, with the traits egg production, egg weight and broodiness;
    - 13 breeds with proposed baselines;
    - plumage, comb and shell loci, including sex-linked barring and duck brown.
  - **Rules:** `husbandry/poultry` (FowlBreed, Fowl, LayingCycle, Clutch, EggGrading) and `husbandry/coop/CoopLayout`.
  - **The bird:** one `FowlEntity` with a shared model and drawn plumage. It covers growth, mating (a Broad Breasted White tom can't
    mate), laying from the Hy-Line curve, fertile eggs that carry both parents' genes, and slow fall.
  - **Wild:** fowl replace vanilla chickens by biome, and ducks come to rivers and mangroves. The bait trough also takes grain.
  - **Buildings:**
    - the coop, with its run and pond, broody hens and mothers, and litter;
    - the incubator and the brooder;
    - the egg grading table, which packs cartons of 12.
  - **Products:** turkey, duck and quail meat, fried eggs, rotten eggs, down, a down jacket, and litter that goes in the composter.
  - **Tests:** 140 game tests and the unit tests pass. Art: 17 Ludo icons (`art/source/PROMPTS.md`).
- Step 15 (2026-11-11): Phase 3, fields, feed and waste. Approved by Tim in plan mode, with his popup answers:
  - alfalfa, silage corn, timothy and orchardgrass hay, oats and barley, on the 15:1 clock, with yields x10 and seeds from grass;
  - a gantry on rails that tills and plants in one pass, harvests, mows, rakes, bales and spreads;
  - Farmings' own belts and augers, a tower silo and a grain bin;
  - a hammer mill and a TMR mixer, with rations that change milk, growth and eggs;
  - a compost windrow and turner, a digester, a screw-press separator, and biogas into a Wildcatter gas set (Wildcatter v0.6.0 tagged and
    published; Farmings moves to it).
  - **Spec:** `docs/crops.md`, `docs/conveyors.md`, and `docs/multiblocks/` `field-gantry.md`, `silo.md`, `feed-mill.md`, `compost.md` and
    `digester.md`, each with a "Built" section.
  - **Rules (plain Java, unit-tested):** `farm/crops`, `farm/conveyor`, `farm/gantry`, `farm/silo`, `farm/compost`, `farm/digester`, and
    `feed/Feedstuff`, `Ration` and `FeedMix`. `verifyPlainJava` covers `farm`.
  - **Built:** crop blocks and seeds, belts and augers with drives, the gantry (rails, trucks, beam, cab, six tool heads, junction, headland
    stop, swaths), the silo and grain bin, the mill and mixer, ration effects on cattle, sheep and birds, the windrow and turner, the
    digester, gas holder, digestate tank and screw press.
  - **Belt into the barn:** a barn hay feeder takes feed from a belt or straight from the mixer. Belts, the silo and the gantry hand items to
    any piece of a big part (its owner).
  - **Fixed:** the gantry kept its step timer in the cab, which is re-made at every row it moves to, so it ran a row a tick. The timer now
    moves with the cab, and `gantry_runs_on_its_clock` checks that a pass is no faster than its tool's speed.
  - **Tests:** 162 game tests and the unit tests pass. Art: 36 Ludo icons (`art/source/PROMPTS.md`).
  - **Evidence:** the Fabric client scene `fields` (15 screenshots; recordings gantry-sows-corn, gantry-chops-corn-to-silo,
    gantry-mows-hay, gantry-rakes-hay, gantry-bales-hay, mixer-feeds-barn, compost-turned, digester-runs-genset).
- Next: the loose ends, then new animals (each its own plan).

- Step 16 (2026-11-11 to 11-12): the loose ends. Approved by Tim in plan mode, with his popup answers. Commits:
  - 4cd9075: the flaky walking tests;
  - 671ce28: the spec;
  - 1c1b445: poultry;
  - e659b3a: straws;
  - 371675b: sheep;
  - 5693b66: coats;
  - ee2c0b1: cold room and compost;
  - 81aed81 / 61c57b0 / 3088df6 / 9e94bbc: Phase 3 done properly;
  - fc23ad5: the sourcing sweep;
  - then the NeoForge client and evidence.
  - **Poultry:**
    - rotten eggs thrown, giving Nausea;
    - Farmings chicken that spoils;
    - hens per male enforced;
    - Coturnix and Bobwhite no longer cross;
    - foraging on the run, with the grass wearing;
    - Muscovy and Coturnix colour mutants.
  - **One straw for every species.** BBW turkeys breed by fresh straw (30 real minutes).
  - **Sheep:**
    - shed bedding and manure, mucked out by hopper;
    - stillborn lambs;
    - breeding seasons by breed;
    - the Border Collie, wild and tamed in the corral on meat, herding on whistle commands.
  - **Cattle coats:** roan, brindle and PMEL dilution. Colour points and recessive spotting were dropped because sources don't support
    them.
  - **Food:** the cold room holds a temperature, food ages by it, and spoiled food goes into compost.
  - **Phase 3 done properly:**
    - hand raking, and silo losses over time;
    - belts that climb, with the items riding them drawn;
    - the full-size digester tank and TMR mixer;
    - the gantry lifting into one entity and gliding;
    - the gas set carrying a real load (the digester's agitator, 5 kW).
  - **Bugs found on the way:**
    - Farmings crops didn't keep dry farmland, so it turned to dirt under them;
    - goals that count ticks missed steps when ticked every other tick;
    - sheep snagged on gate posts;
    - the cold room's temperature didn't reach the food's credit (caught by its own test);
    - the NeoForge client wouldn't start ("missing client-side handlers: [wildcatter:plc]"). Fixed in Wildcatter 0.6.1
      (released 2026-11-12), and Farmings is on it.
  - **Tests:** 189 game tests and 244 unit tests. Full runs passed three times in a row after the flaky-test fixes.
  - **Client scenes:** `looseends` (dog, coats, plumage, rotten egg), `belts`, and `fields` (the gantry gliding, the full-size machines,
    the gas set under load).
  - **Art:** Ludo icons for raw and cooked chicken and the shepherd's whistle.
- Step 17 (2026-11-13, under way): new animals. Tim chose goats (Saanen, Nubian, Boer) and geese (Embden, Toulouse, Chinese) and
  guinea fowl; pigs and horses wait. Approved in plan mode with his popup answers: goats share the sheep shed, dog and straws; goat milk
  through the parlor's sheep stalls into bottles and fresh chèvre; wild goats replace vanilla goats; geese graze, give down and guard;
  guinea fowl carry pearl, lavender and white genes and give an alarm call.
  - Spec: `docs/animals/goats.md` (new); geese and guinea fowl in `poultry.md`; goat and new-bird sections in `creamery.md`, `parlor.md`,
    `sheep-shed.md`, `food.md`, `eggs.md`, `breeding.md`, `taming.md`.

## Known gaps
- **Poultry:** built 2026-11-11 (loose ends): turkey straws (artificial insemination for Broad Breasted White), throwing
  rotten eggs, Farmings chicken that spoils, foraging on the run, Muscovy and quail colour mutants, hens per male, no quail cross.
- **Poultry proposals with no source:**
  - breed baselines and the biome-to-fowl mapping;
  - the feed exponent and the turkey's daily feed;
  - egg weights for Silkie and Mallard;
  - first-egg age for Pekin, Mallard and Muscovy;
  - capacities for nests, perches, grain, litter and the incubator;
  - power for the incubator and the brooder;
  - grade age thresholds;
  - the plumage dominance orders.
- **Phase 3, not built or simplified:**
  - ~~the digester tank and the TMR mixer are single blocks~~ (full size, built 2026-11-12);
  - ~~belts are level only, and items riding a belt are not drawn~~ (built 2026-11-12);
  - ~~the gantry steps as blocks; hand raking~~ (the lift and hand raking built 2026-11-12);
  - the silo takes its whole 10.5% dry-matter loss at filling;
  - the scene's gas set runs with no electrical load on it, so it idles (61 m³/h).
- **Phase 3 proposals with no source:** see the *(proposed)*, NOT FOUND and TO VERIFY marks in `docs/crops.md`, `docs/conveyors.md` and
  the five Phase 3 multiblock docs (among them the gantry span, bin and rake/spreader power, the gas holder and digestate tank sizes,
  the press motor and split, ration score shapes and floors, and fish meal and limestone analyses).
- ~~**Flaky game tests**~~ Fixed 2026-11-11: shed scenes stand 64 blocks apart (a shed takes in unhoused sheep within 48), the walking
  scenes poll instead of checking once, a sheep is home only within a block of her pen cell (two let her stop in the gate), and the fowl
  scene has room to flee (the flee goal also tries a shorter run when hemmed in). Three full runs in a row passed.
- **Sourcing pass, 2026-11-12:**
  - Marked lines (proposed, TO VERIFY, NOT FOUND) went from 387 to 337. About 50 marks were resolved with fetched sources, and the
    rest have searches noted at the end of each doc.
  - About 30 values changed in the code to match their sources, among them:
    - quail fertile window 11 days; duck litter 0.11 kg per kg a day; Mallard egg 54 g;
    - pasteurizer heater 12 kW; separator motor 1.5 kW;
    - dry-cow intake 1.95% of body weight; Gyr gestation 286 days; scraper drive 0.75 kW; tank chiller 2.6 kW;
    - Merino clean yield 70%; Suffolk ewe 104 kg; Dorper ewe 77 kg; micron grade boundaries; Dorper season medium;
      weaning 84 days; 2 days in the lambing pen; no shearing within 28 days of lambing;
    - orchardgrass 1.48 kg DM a block; barley 87 days;
    - one auger drive per 9 m; rake 10 km/h; spreader 11 km/h at 0.2 kWh/t; silo unloader 22.4 kW;
    - mixer 2.3 t and 4 minutes;
    - fish meal and limestone analyses;
    - compost moistures.
  - Tim's answers (2026-11-12): ducks hatch 50/50; Dorpers stay polled; the Silkie keeps the European standard's figures; the
    lactating cow's intake stays 2.7%.
- Proposed values with no source are marked in the docs (dirty-stall effect, beef baselines, Wagyu bonus, steak food values, creamery sizes and motor powers, press time, store sizes, the room-temperature shelf life of perishables, the cold room's COP and size limits, lane length, coat founder weights for Jersey, Brown Swiss and Gyr, body proportions).
- The Fabric client game test takes screenshots and recordings; it asserts nothing.
- The NeoForge client launches to the title screen (2026-11-12), on Wildcatter 0.6.1, which registers its PLC client handler itself
  (the fix for "missing client-side handlers: [wildcatter:plc]"). Its in-world scenes are all on Fabric's client game test.
- ~~Cattle are textures on the vanilla cow model~~ Fixed 2026-11-09: their own model, coats from the coat genes (cattle.md, "Models and coats").
- Not built: products by pipe, a plain-text genome code, lane collision rails. Built 2026-11-11 (loose ends): the cold room temperature model, spoiled food in compost.
- Sheep, not built: cloth filters. Built 2026-11-11 (loose ends): bedding that gets dirty, sheep dogs, breeding seasons, sheep straws,
  stillbirths for lambs. (Natural spawns were built with taming on 2026-11-10; this line said otherwise until now.)
- Sheep proposals with no source: breed baselines, litter odds shape, fleece-trait swings, grass block 1.5 kg DM, pen and pasture caps,
  shearing time and motor, skirting share, bale size, mill motors, lanolin recovery, carding loss, spin bonus, yarn/cloth/felt sizes,
  balm healing, clothing armor (leather's), body proportions.

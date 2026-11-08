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
  - **Placeholders:** every block uses a one-box model and a flat `*_placeholder` texture (`tools/barn_assets.py` writes them) and a
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
- Next: the milking parlor and bulk tank spec (`docs/multiblocks/parlor.md`), which also needs the lactation curve research.

## Known gaps
- Barn blocks are placeholder boxes and flat colours, with no recipes (creative tab only). Manure and fork icons are placeholders too.
- Build still uses a temporary Java 25 JDK from a scratchpad; install a permanent one (Temurin 25).
- NeoForge game tests exist (5 scenes). The Fabric client game test takes screenshots only; it asserts nothing yet.
- NeoForge has not been launched with a client; Fabric has (the cattle render).
- No icon, no creative tab. Cattle art is textures on the vanilla cow model (plus the Gyr hump), not final 3D models.

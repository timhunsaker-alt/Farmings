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
- Next: Tim approves the Phase 1 code plan (cattle entity first, then the barn); then research the TO VERIFY list, then the barn, feeder and parlor specs.

## Known gaps
- No game tests yet (Tailings has a NeoForge game-test server and a Fabric client game test; they come with the first feature).
- Fabric has not been launched.
- No icon, no creative tab, no lang strings.

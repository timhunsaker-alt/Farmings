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
- Next: Tim reviews `cattle.md`; then research the TO VERIFY list, then the barn, feeder and parlor specs.

## Known gaps
- No game tests yet (Tailings has a NeoForge game-test server and a Fabric client game test; they come with the first feature).
- Fabric has not been launched.
- No icon, no creative tab, no lang strings.

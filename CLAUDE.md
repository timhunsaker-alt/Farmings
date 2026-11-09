# Farmings — instructions for Claude Code

## Read first, every session
1. `docs/VISION.md` — what the mod is and is not
2. `docs/ROADMAP.md` — the CURRENT phase
3. `docs/GENETICS.md` — how the genetics engine comes over from FarmChain, and the no-chain rule
4. `PROGRESS.md` — what is already built

Farmings is an add-on to Wildcatter and is built the same way as Tailings. When a rule here is silent, follow
Wildcatter's `CLAUDE.md` (`../wildcatter/CLAUDE.md`). If a request conflicts with these docs, point out the
conflict and ask before doing anything.

## Scope
- Work only on the phase marked CURRENT in ROADMAP.md. New ideas go under "Ideas" in ROADMAP.md, not into code.
- Do not invent mechanics, items, ratings or numbers that are not in the docs. If one is needed, stop and propose it with
  its real-world counterpart and values. Any line marked **DECISION** is open: ask, never choose.
- Farmings follows real farming and animal science but is not a teaching tool and is not veterinary advice.
- Farming numbers (yields, feed intake, gestation, lactation) come from real sources. Cite the source in the doc.

## No blockchain, ever
- No wallet, token, NFT, signing, chain client or network call to a chain, in any module, any dependency or any config.
- Genes are deterministic and checked locally. See `docs/GENETICS.md`.
- Mojang's rules and the mod hosts' rules are the reason. Do not add a "just optional" chain feature.

## How to work
- Start every task in plan mode: files to change, multiblock specs needed, game test to add. Wait for approval before
  writing code. Present the plan with the approve-plan button (plan mode), and ask questions in the question popup, not in chat
  (Tim, 2026-11-08).
- For any piece of equipment, state its real-world counterpart and the values used.
- Power comes from Wildcatter and nowhere else. No abstract energy units. Never hardcode a voltage or frequency; read
  them from the world's electrical standard (NA or IEC), as Tailings does.
- Plain-Java packages have no Minecraft or loader imports: `genetics`, `husbandry`, `feed`, `process`. A
  `verifyPlainJava` task enforces it.
- Loader code stays in `fabric/` and `neoforge/`. `common/` reaches a loader only through a platform interface.
- Anything that keeps running while the player is away (a harvester, a conveyor, a barn) uses a chunk ticket that times
  out on its own, never a global force-load. Tailings R1 is the pattern.

## Multiblocks and contraptions
- Write `docs/multiblocks/<name>.md` before any code: layered ASCII grid (bottom to top), legend mapping characters to
  block IDs or tags, controller position, valid rotations, how it forms, what happens when a part is removed.
- Build from distinct real parts. Never tile identical blocks to make a bigger machine.
- No tractors. Field machines are moving contraptions on rails or tracks, like Tailings' tunnel boring machine: parts lift
  into one entity while running and set back down as blocks when it stops.
- Add a game test that builds the structure and checks that it forms.

## Animals
- Every animal has a sex, a breed composition and a genome. Species and breeds each have a doc in `docs/animals/`
  with real source values before any code.
- Animals eat what a real animal eats. Cattle and sheep eat hay (the vanilla hay block). Do not make animals eat
  anything unless the doc says so.
- An animal's state (age, pregnancy, lactation, health) must survive a save and a chunk unload. Add a game test.

## Art
- Final textures come from Ludo sources, processed by a tools script, as in Wildcatter and Tailings.
- Textures drawn by script are placeholders: name them `*_placeholder` and list them in PROGRESS.md under Known gaps.

## Showing tests (Tim's rule, 2026-11-08)
- Every feature test that can be seen is shown with real in-game **screenshots** (the Fabric client game test takes them).
- Anything that **moves** (conveyors, the scraper, animals, machines running) also gets a **short screen recording**. Recording
  runs under `caffeinate` so the display stays awake (`Filming.java` does this). Send the files to Tim; do not just say "passed".

## Definition of done
- `./gradlew build` and the game tests pass and cover the new feature, with screenshots (and a recording for motion) shown to Tim.
- PROGRESS.md updated. Field Manual page added or updated once the Field Manual exists.

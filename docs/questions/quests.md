# Quest chapter decisions (2026-11-14)

Farmings' chapter in Wildcatter 0.8.0's quest book (`common/src/main/resources/data/farmings/wildcatter_quests/farm.json`), from the
approved plan (`elegant-swinging-starfish.md`, "Farmings chapter"). Tim was away; each line below was **decided on recommendation; for
Tim**.

1. **No Farmings milestone of its own.** The plan's one Farmings hook is "biogas into a Wildcatter genset (milestone from Farmings' gas
   holder feeding a running set)". Wildcatter 0.8.0 already fires `wildcatter:genset_on_biogas` from the gas set's panel whenever it burns
   biogas off the steel pipe, and Farmings' gas holder is the only thing that puts biogas on that pipe. Firing a second key from the gas
   holder would duplicate it (and the holder cannot tell whether the set drawing from it is up to speed), so the quest requires Wildcatter's
   key and no Farmings machine class changed. There is no `dev.farmings.quest.Milestones` class, since there are no Farmings keys.
   **Accepted by Tim 2026-11-14** for the biogas quest; Farmings now has one key of its own for taming (decision 2), so the class exists.
2. **Cattle have no task kind.** The engine counts items, advancements and milestones, not animals, and the plan names no hook for
   taming. The "Bring in cattle" quest asks for the corral's parts (post, gate, bait trough) and 4 hay blocks, and its text tells the player
   how taming works. A `farmings:tamed` milestone from the corral would make it exact; it is in Known gaps, not built.
   **Answered by Tim 2026-11-14: add a taming milestone.** The corral post fires `farmings:tamed` when an animal settles in it; "Bring
   in cattle" asks for the milestone and keeps the 4 hay blocks (the corral parts are no longer counted). Game test
   `quest_tamed_milestone`.
3. **Chapter placement:** id `farmings:farm`, order 120 (Tailings' example in the spec is 110), `after` Getting started, since its first quest
   needs only steel. Later quests require Wildcatter quests from other chapters: `wildcatter:lamp` (hatchery, heat lamps),
   `wildcatter:motor` (parlour, scraper, gantry, mixer, digester), `wildcatter:genset` (biogas), `wildcatter:fertilizer` (spreader),
   `wildcatter:plastic` (straws). **Accepted by Tim 2026-11-14.**
4. **Hatchery and heat lamps are two quests**, as they are two different buildings (the hatchery for birds, the heat lamp over a farrowing
   crate in the pig barn). Both follow the lamp quest, because both are low-voltage loads. **Accepted by Tim 2026-11-14.**
5. **The spreader quest takes `#c:fertilizers`**, the tag Wildcatter puts its TSP, DAP and MOP bags in and the spreader reads, so any of
   the three bags counts. **Accepted by Tim 2026-11-14.** Tim also answered the bag question: **many uses by mass**. A Wildcatter
   1 t bag is 100 uses in the spreader (10 kg a use, compost's mass); the cab keeps what is left and opens the next bag only at 0.
   Game test `spreader_bag_gives_100`.
6. **Rewards** (all vanilla or with a hand recipe; checked by `QuestRules`): 8 hay blocks (barn), a manure fork (cattle), 16 wheat seeds
   (hatchery), a spare heat lamp, 2 buckets (parlour), 16 field rail (gantry), 8 glass bottles (creamery), 8 steel ingots (digester). The
   scraper, mixer, biogas, spreader and straws quests give nothing. **Accepted by Tim 2026-11-14.**
7. **Plan says "push"** for this step; the brief for this run says commit locally and never push, so it is committed and not pushed.
   **Accepted by Tim 2026-11-14.**

## Follow-ups from Tim's answers (2026-11-14)
Plan `elegant-swinging-starfish.md`, "5. Farmings" and "6. Paperwork", approved by Tim.

8. **Time scale: answered by Tim 2026-11-14: keep Farmings' own.** Farmings' waits keep their fixed speed-ups (15:1 for cattle, sheep and
   machine times, 3:1 for poultry) and do not follow Wildcatter's world time scale. Removed from Known gaps.
9. **One `farmings:tamed` key for every species.** Decided on recommendation; for Tim. The plan names one key and says "an animal"; the
   corral tames every wild species (cattle, sheep, fowl, the sheep dog and the rest) through the same `tend` call. The quest is "Bring in cattle",
   so taming a sheep there also counts. Per-species keys (`farmings:tamed/cattle`) would make it exact if Tim wants them.
   **Accepted by Tim 2026-11-19** (B6; C14 answer: keep one key, 64 blocks).
10. **Who gets the milestone.** Decided on recommendation; for Tim. Wildcatter's hook gives it to every player within 64 blocks of the
    corral post when the animal settles, not to the corral's owner wherever they are (0.8.0 has no per-player hook). The quest text
    says to be within 64 blocks. **Accepted by Tim 2026-11-19** (B6; C14 answer: keep one key, 64 blocks).
11. **What counts as a Wildcatter bag.** Decided on recommendation; for Tim. Any item in `#c:fertilizers` whose id is in the
    `wildcatter` namespace is 100 uses (today TSP, DAP and MOP bags, all 1 t). Another mod's item in the tag stays one use, since its
    mass is unknown. A part-used bag's uses are lost if the cab is broken, as the cab's bin is. **Accepted by Tim 2026-11-19** (B6).
12. **Wildcatter version.** The plan says "set `wildcatter_version=0.8.1`"; the brief for this run says stay on 0.8.0 until 0.8.1 is
    released. It stays on 0.8.0; only `QuestEvents.milestone` is used, which 0.8.0 has. **Accepted by Tim 2026-11-19** (B6). (Since raised to 0.8.1 once it was released, commit 70f846e.)
13. **No CHANGELOG.** The brief asks for the CHANGELOG under "Unreleased"; Farmings has no CHANGELOG file (PROGRESS.md is its
    history). Decided on recommendation; for Tim: PROGRESS.md carries it and no new file is made. Superseded 2026-11-19: the first public release needs one, so `CHANGELOG.md`
    now exists (`docs/questions/wave-a.md` 2).
14. **Still committed locally, not pushed** (brief).

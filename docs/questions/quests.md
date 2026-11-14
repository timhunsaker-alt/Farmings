# Quest chapter decisions (2026-11-14)

Farmings' chapter in Wildcatter 0.8.0's quest book (`common/src/main/resources/data/farmings/wildcatter_quests/farm.json`), from the
approved plan (`elegant-swinging-starfish.md`, "Farmings chapter"). Tim was away; each line below was **decided on recommendation; for
Tim**.

1. **No Farmings milestone of its own.** The plan's one Farmings hook is "biogas into a Wildcatter genset (milestone from Farmings' gas
   holder feeding a running set)". Wildcatter 0.8.0 already fires `wildcatter:genset_on_biogas` from the gas set's panel whenever it burns
   biogas off the steel pipe, and Farmings' gas holder is the only thing that puts biogas on that pipe. Firing a second key from the gas
   holder would duplicate it (and the holder cannot tell whether the set drawing from it is up to speed), so the quest requires Wildcatter's
   key and no Farmings machine class changed. There is no `dev.farmings.quest.Milestones` class, since there are no Farmings keys.
2. **Cattle have no task kind.** The engine counts items, advancements and milestones, not animals, and the plan names no hook for
   taming. The "Bring in cattle" quest asks for the corral's parts (post, gate, bait trough) and 4 hay blocks, and its text tells the player
   how taming works. A `farmings:tamed` milestone from the corral would make it exact; it is in Known gaps, not built.
3. **Chapter placement:** id `farmings:farm`, order 120 (Tailings' example in the spec is 110), `after` Getting started, since its first quest
   needs only steel. Later quests require Wildcatter quests from other chapters: `wildcatter:lamp` (hatchery, heat lamps),
   `wildcatter:motor` (parlour, scraper, gantry, mixer, digester), `wildcatter:genset` (biogas), `wildcatter:fertilizer` (spreader),
   `wildcatter:plastic` (straws).
4. **Hatchery and heat lamps are two quests**, as they are two different buildings (the hatchery for birds, the heat lamp over a farrowing
   crate in the pig barn). Both follow the lamp quest, because both are low-voltage loads.
5. **The spreader quest takes `#c:fertilizers`**, the tag Wildcatter puts its TSP, DAP and MOP bags in and the spreader reads, so any of
   the three bags counts.
6. **Rewards** (all vanilla or with a hand recipe; checked by `QuestRules`): 8 hay blocks (barn), a manure fork (cattle), 16 wheat seeds
   (hatchery), a spare heat lamp, 2 buckets (parlour), 16 field rail (gantry), 8 glass bottles (creamery), 8 steel ingots (digester). The
   scraper, mixer, biogas, spreader and straws quests give nothing.
7. **Plan says "push"** for this step; the brief for this run says commit locally and never push, so it is committed and not pushed.

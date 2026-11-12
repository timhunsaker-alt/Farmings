# Breeding and calving (APPROVED by Tim 2026-11-08; natural service built)

**Status: draft 2026-11-08. Nothing here is built.** *(proposed)* = a proposal. *(unverified)* = no good source yet. Figures already
in [cattle.md](cattle.md) are cited there and only referred to here.

**Decided with Tim, 2026-11-08:** a cow can be bred **both** by a bull (natural service) and by a straw (artificial insemination);
the calf **spawns beside the dam** at the due date and her lactation starts. Already decided 2026-11-07: first breeding at 15 real months
and at 55% of mature weight; first two calves heifers, the third a bull, then 50/50.

## Time (1 game day = 15 real days; 24,000 ticks)
| Thing | Real | Game |
|---|---|---|
| Heat cycle | 21 days (17-24) | 1.4 game days, 33,600 ticks |
| Gestation | 283 days (Angus 280, Wagyu 284) | 18.9 game days |
| Dry period | 60 days | 4 game days |
| Heat length | 18 h when watched by eye, about 7 h with continuous monitoring ([NMSU B117](https://pubs.nmsu.edu/_b/B117/): "a mean of 18 hours"; "averaged 7 hours") | **0.75 real day (18 h) = 1,200 ticks**, the visual mean |

## Heat
- A female that is a heifer past her first-breeding rules (15 months and breeding weight) or a lactating cow past the waiting period
  comes into heat every 21 real days for the heat length. Pregnant, dry-and-pregnant, calf and retired animals never come into heat.
- **Waiting period after calving: 60 real days.** "A VWP of 60 days ... is a standard throughout the dairy industry" ([Penn State Extension](https://extension.psu.edu/how-long-should-the-voluntary-waiting-period-be)).
- A cow in heat is shown (the readout, and later a visual cue such as a particle) *(proposed)*.

## Natural service *(proposed)*
- A **bull** (not a bull calf, not a steer) housed in the same barn serves any cow in heat in the barn. The cow walks to him or he to her; the first version teleports nothing: the cow stays in her stall and the bull is "in the barn" if he has a stall of his own in the same controller. Service is checked once a second with the barn tend.
- Chance of conception per heat: in real herds about 40% at first service for lactating cows and 65% for heifers ([Virginia Cooperative Extension, Nebel 2002](https://sites.ext.vt.edu/newsletter-archive/dairy/2002-05/aiconception.html): "from 60 to 40%"; "approximately 65% first service conception"). Proposal: a served cow in heat conceives, so there is no luck to lose; fertility (the genome trait) instead shifts the **waiting period** *(proposed, DECISION below)*.
- Sire = the bull that served her. One bull can serve all the cows in heat in his barn. Bull health or limits: none yet.

## Artificial insemination *(proposed)*
- A **semen straw** item stores one sire's genome (and the sire's name/id). Made by right-clicking a bull with an empty `straw` item (a collection step; a later phase can turn this into a collection station).
- Right-clicking a cow in heat with a straw inseminates her. The straw is used up.
- Straws can be carried between farms (items), not sold (nothing is sold).
- A straw never expires in this version *(proposed)*.

## Pregnancy and calving
- The sire's genome is saved on the dam from conception to calving (it survives save and chunk unload, like all state).
- At the due date a calf spawns beside the dam from `Offspring.create(dam, sire, seed, rules, forcedSex)`; the seed derives from the world key, dam and the calving tick so it is deterministic.
- Sex: the dam's calf number decides: first two heifers, third a bull, then 50/50 (already decided). "Calf number" is counted **per player**, as in FarmChain's tutorial *(DECISION: per player, or per dam? The first-calf rule only makes sense per player or per farm. Proposal: per world, simplest.)*
- The calf is a baby (stage CALF), weighs *(unverified)* at birth, and follows the existing growth rules.
- The dam becomes a lactating cow on the calving tick; days in milk count from it; `hasCalved` is set; the **wait period** and the 60-day dry period then apply as in cattle.md.
- The dam does not die in calving (no mortality in this version).
- Twins, stillbirths and difficult births: **none** in this version *(proposed)*.

## What a sire needs
- A bull must be a mature male with a genome (a "founder" or bred calf). A bull's own score does not change the calf's expressed scores except through the allele inheritance that already exists in `genetics`.
- Beef breeds (Angus, Wagyu) have no genetic baselines yet (open decision in cattle.md), so they **cannot** sire or dam a genome calf yet. Until Tim decides that, crossing them is **not built**.

## Game tests (to add with the code)
`heifer_comes_into_heat`, `bull_serves_cow_in_heat`, `straw_inseminates`, `calf_is_born_beside_dam`, `first_two_calves_are_heifers`, `dam_lactates_after_calving`, `sire_survives_save`.

## Decided with Tim, 2026-11-08
1 yes (0.75-day heat, 60-day wait). 2 yes (every service in heat conceives). 3 per player (the barn's owner is the first player to read its controller). 4 the bull has his own stall in the barn. 5 builder's choice: natural service first, straws after.

## Built (2026-11-08): natural service and calving
Heat (`husbandry/Breeding`), a housed bull serving housed cows in heat on the barn tend, conception (sire genome kept on the dam, due tick), calving (`Offspring.create`, calf beside the dam, her lactation starts), first-two-heifers rule per owner (`CalfCounts`, saved with the world), save/load. New proposals with no source: calf birth weight is 6% of the dam's mature weight (a Holstein calf about 43 kg, unverified). **Built later the same day:** calf growth ([growth.md](growth.md)) and straws (`empty_straw` used on a grown bull gives a `semen_straw` holding his genome; a semen straw used on a cow in heat inseminates her and is used up; game test `straw_inseminates`). **Not built:** twins, stillbirths, beef crosses, a heat cue you can see.

## Original decisions (kept for the record)
1. Heat length of 0.75 real day, and the 60-day waiting period. (Both sourced on 2026-11-12; see above.)
2. Conception: every service in heat conceives *(proposed)*, or add a chance? A chance needs a sourced number (now found: about 40% for cows, 65% for heifers, see above).
3. First-two-heifers counter: per world (proposed), per player, or per dam.
4. How a bull is "in the barn": his own stall in the barn, or free to roam to the cow (needs pathing and may need pens).
5. Whether the first version builds natural service **and** straws together, or natural service first (my recommendation: natural service first, straws right after, because the straw adds an item, a collection step and a right-click on cows).

## Built, 2026-11-09: twins, stillbirths, freemartins, the heat cue, beef crosses
- **Twins:** 5.02% of calvings in later lactations, from 1.3 million North American Holstein births ([Twinning rates for North American Holstein sires, PubMed 11573789](https://pubmed.ncbi.nlm.nih.gov/11573789/)). It rises with parity; a first calving twins about 0.68% of the time (1.6 million Italian calvings, same search). Both figures used.
- **Stillbirths:** 12.3% for heifers, 5.4% for cows (USDA AIPL research, [Hoard's Dairyman](https://hoards.com/E_reproduction/REP08)). A stillborn calf is not spawned; the cow lives and her lactation starts.
- **Freemartin:** a heifer born twin to a bull is infertile in more than 90% of cases ([Oklahoma State](https://news.okstate.edu/articles/agriculture/2020/stotts_zook-freking_freemartinism)); 90% is used. She never cycles, cannot conceive, and the readout says so.
- All calving rolls come from a generator seeded by the calving (world seed, dam, sire), so the same calving gives the same result.
- **Heat cue:** heart particles every 3 seconds while she is in heat (a game sign).
- **Beef crosses:** Angus and Wagyu now have baselines (proposed, in cattle.md) and breed like the dairy breeds. Their calves carry the mixed breed composition. Beef cows calve but are never milked.
- Game tests: twins_born, stillbirth_no_calf, freemartin_does_not_cycle, angus_wagyu_cross.

## Built, 2026-11-09: the herd book
A `herd_book` item. Right-click an animal to record it; use it in the air to read it as a book. One page per animal, keyed by its herd ID
(the first 8 characters of its genome hash): breed or cross, sex, birth day, dam and sire IDs (founders say "founder"), calves, freemartin,
the eight trait scores, dressing % and marbling grade. Recording again replaces the page. It holds 64 animals *(proposed)*.
Game test: herd_book_records.

## Loose ends, 2026-11-11 (spec): one straw for every species
- **Species:** the `empty_straw` and `semen_straw` now record the species they came from: cattle (a bull), sheep (a ram) or turkey (a
  tom; see poultry.md). The tooltip shows the species and the sire's herd ID.
- **Using it:** a semen straw works only on a female of its own species, in heat (a hen needs no heat). On any other animal it does
  nothing, and the actionbar says "this straw is ram semen", or whichever species it holds.
- **Sheep:** a ram's straw used on a ewe in heat inseminates her, as natural service does (`SheepEntity.conceive`). It keeps forever,
  as a bull's does.
  - Every insemination in heat conceives, as for cattle *(proposed, for parity with natural service)*.
  - Real AI conceives less often: 60-80% with frozen semen placed by laparoscope ([Sathe 2018, Frontiers](https://www.frontiersin.org/journals/veterinary-science/articles/10.3389/fvets.2018.00266/full)),
    and **40%** (was 20-40%) with thawed semen through the cervix: "The combined pregnancy rate for deep cervical and transcervical
    insemination was 40%", against 85% by laparoscope in the same trial ([Gage 1994, Utah State thesis](https://digitalcommons.usu.edu/etd/3890)). This is a known difference.
  - Collection is by artificial vagina (Steyn, [SARE](https://projects.sare.org/wp-content/uploads/993336sheep-ai-steyn-fresh.pdf)).
    The game does it with a right-click on an adult ram.
- **Turkey:** see poultry.md. A turkey straw is fresh and spoils after 30 real minutes.

### Built, 2026-11-11 (loose ends)
- **`StrawItem`:** fills from a tame grown bull, ram or turkey tom, and inseminates a female of the genome's species.
- **Turkey straws:** they record their collection tick and go stale after 30 real minutes (167 ticks). A stale straw turns back into an
  empty straw in the inventory.
- **Tooltip:** shows the species and the sire's herd ID.
- **Game tests:** ram_straw_inseminates_ewe, bbw_hen_fertile_by_straw, turkey_straw_goes_stale, straw_wrong_species_refused.

### Sourcing pass, 2026-11-12
Resolved 4 marks: heat length (18 h, matches the 0.75 day), the 60-day waiting period, sheep cervical AI (40%), and the conception rate the
decision asked for (40% cows, 65% heifers; the "always conceives" rule stays a proposal). The other marks are game rules.

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
| Heat length | sources disagree, about 7-9 h to 18-24 h *(unverified)* | **0.75 real day = 1,200 ticks** *(proposed; one real day of the cycle is the shortest the clock can show)* |

## Heat
- A female that is a heifer past her first-breeding rules (15 months and breeding weight) or a lactating cow past the waiting period
  comes into heat every 21 real days for the heat length. Pregnant, dry-and-pregnant, calf and retired animals never come into heat.
- **Waiting period after calving: 60 real days** *(proposed; the calving-interval arithmetic in cattle.md leaves about 82 days from calving to conception, so 60 fits inside it. No source found)*.
- A cow in heat is shown (the readout, and later a visual cue such as a particle) *(proposed)*.

## Natural service *(proposed)*
- A **bull** (not a bull calf, not a steer) housed in the same barn serves any cow in heat in the barn. The cow walks to him or he to her; the first version teleports nothing: the cow stays in her stall and the bull is "in the barn" if he has a stall of his own in the same controller. Service is checked once a second with the barn tend.
- Chance of conception per heat: **not sourced**. Proposal: a served cow in heat conceives, so there is no luck to lose; fertility (the genome trait) instead shifts the **waiting period** *(proposed, DECISION below)*.
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
Heat (`husbandry/Breeding`), a housed bull serving housed cows in heat on the barn tend, conception (sire genome kept on the dam, due tick), calving (`Offspring.create`, calf beside the dam, her lactation starts), first-two-heifers rule per owner (`CalfCounts`, saved with the world), save/load. New proposals with no source: calf birth weight is 6% of the dam's mature weight (a Holstein calf about 43 kg, unverified). **Not built:** straws, calf growth (a newborn stays a calf: weight is still set, not grown), twins, stillbirths, beef crosses, a heat cue you can see.

## Original decisions (kept for the record)
1. Heat length of 0.75 real day *(proposed)*, and the 60-day waiting period *(proposed)*. Both lack a good source.
2. Conception: every service in heat conceives *(proposed)*, or add a chance? A chance needs a sourced number.
3. First-two-heifers counter: per world (proposed), per player, or per dam.
4. How a bull is "in the barn": his own stall in the barn, or free to roam to the cow (needs pathing and may need pens).
5. Whether the first version builds natural service **and** straws together, or natural service first (my recommendation: natural service first, straws right after, because the straw adds an item, a collection step and a right-click on cows).

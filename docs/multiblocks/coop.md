# Coop, run and duck pond

**Status: spec, 2026-11-11 (Tim, 2026-11-10: "Coop with nest boxes + run", "Broody hens hatch eggs too", "Duck pond").** Figures are
from `docs/animals/poultry.md` unless marked *(proposed)*. The coop copies the sheep shed's shape (`sheep-shed.md`).

## Parts
| Part | Block | What it does |
|---|---|---|
| Coop controller | `coop_controller` | Sits on the floor level, touching the coop floor. Right-click reads the coop. It runs the flock: homes, feed, water, the run, nests and litter. |
| Coop floor | `coop_floor` | A litter-bedded floor cell (2/16 high). The coop is every floor cell joined to the controller. |
| Nest box | `nest_box` | Beside a floor cell. Hens lay into it, and a broody hen sits in it. It holds up to **16 eggs** *(proposed)*, and a hopper below pulls them out. |
| Perch | `perch` | A roost bar beside or above a floor cell. Chickens and turkeys roost on it at night. |
| Grain feeder | `grain_feeder` | Beside a floor cell. Holds up to **256 grain items** (25.6 kg, *proposed*), filled by hand or hopper. |
| Water trough | the barn's `water_trough` | Beside a floor cell, reused as in the shed. |
| Pop hole | `pop_hole` | A small door cell set in the run fence, touching the coop floor. The flock goes out and in through it. |

## Forming (`husbandry/coop/CoopLayout`)
From the controller, every `coop_floor` and `pop_hole` cell at the controller's height, joined side to side, is the coop. It forms
when:
1. a floor cell touches the controller;
2. the coop is at most **256 cells** *(proposed, the shed's cap)*;
3. at least one nest box, one grain feeder and one water trough touch a floor cell.

Perches are needed only by birds that roost (chickens, turkeys). Without enough of them, the readout warns and the birds sleep on the
floor; nothing is lost *(proposed)*. Fault messages name the first thing missing.

**Capacity:**
- **Floor and run space** (the poultry.md housing table): 1 floor block holds 3 chickens, 1 turkey, 16 quail or 2 ducks. A coop
  can hold mixed species, and each bird uses its share of floor.
- **Nest boxes:** 1 per 4 hens (8 for quail).
- **Perches:** 1 block per 3 chickens or 1 turkey.
- **Taking birds in:** the controller takes in tame birds within **48 blocks** that have no coop, until it is full (the shed rule).
  It never takes wild birds.
- **Not counted:** young birds still under heat age don't count towards capacity.

## The run (`husbandry/shed/PastureLayout`, reused)
- **Finding it:** from the pop hole, the run is the fenced ground reached by walking away from the coop. It holds at most
  **1,024 cells** *(proposed)*. A bigger flood means the run is not fenced in.
- **Space:** the run needs 1 cell per chicken or quail group of 8, 2 per turkey and 2 per duck. If it is smaller, only as many
  birds as fit go out; the rest stay in *(proposed)*.
- **Day and night:**
  - Birds go out from morning to dusk (game time 0-12,000), in clear weather.
  - Ducks also go out in rain *(proposed)*.
  - At dusk they come in, chickens and turkeys to the perches.
- **Feed:** the run gives no feed in this pass *(proposed: foraging is not modelled)*.

## The duck pond
- **What counts:** water source blocks in the run, or touching it.
- **How much:** ducks need **1 water block per 4 ducks** *(proposed)*. A water block is a metre deep, past the 10 cm ducks want for
  dipping their heads (RSPCA, TO VERIFY).
- **Swimming:** ducks swim in it.
- **Without a run:** water blocks touching the coop floor count instead.
- **Too little water:** each duck without its share loses **5 health points a game day** *(proposed)*. Swimming water is "not
  essential" (UW-Madison), so the cost is small, and no duck dies of it.

## Nests and eggs
- **Laying:** a hen lays her egg into the nearest nest box with room.
- **Floor eggs:** if every nest is full, she drops the egg on the floor as an item. A floor egg is a **dirty egg**, graded B at
  best (`docs/eggs.md`) *(proposed)*.
- **What an egg carries:** weight, shell shade, the tick it was laid, and, if it is fertile, the parents' genome
  (`docs/animals/poultry.md`).

## Broody hens
- **Going broody:** each game day, a tame hen whose nest box holds at least **6 eggs** of her species may go broody. Her chance is
  her broodiness score / 100 × **0.1** *(proposed)*.
  - A Silkie (high score) goes broody within days.
  - A Leghorn almost never does.
- **Sitting:**
  - A broody hen stops laying and sits in that nest box.
  - Its fertile eggs, up to **12** (a wild turkey's clutch is 10-14, [Wikipedia](https://en.wikipedia.org/wiki/Wild_turkey);
    *proposed for all*), are kept warm and count down their incubation days.
  - She leaves the nest only to eat and drink.
- **Taking her eggs:** if the player or a hopper takes every egg out, she stops sitting.
- **Hatching:**
  - Chicks hatch on the floor beside the nest.
  - The hen becomes a mother and keeps her young warm (within 2 blocks) until their heat-until age.
  - Then she lays again.
- **Odd eggs:** eggs of another species in her nest are not warmed *(proposed)*.

## Feed, water, litter
- **Each bird's need:** each game day, a bird's need is one real day's feed and water (poultry.md). It eats from the grain feeder
  and drinks from the troughs.
- **Shortfalls:** a shortfall lowers that day's eggs, growth and health (the cattle rule).
- **Litter:**
  - It builds up in the coop at the layer rate (0.05 kg per kg of bird a day).
  - The controller holds it as **poultry litter** items of **10 kg** *(proposed)*, up to 64. A hopper below the controller pulls
    them out.
  - When the store is full, the birds' health drops **2 points a game day** *(proposed, after the barn's dirty-stall rule)*.

## Not in this pass
- Foraging on the run.
- Predators.
- Mixed-species disease.
- Moving a flock between coops.

## Built, 2026-11-11
- **Code:**
  - Layout in `husbandry/coop/CoopLayout` (plain Java, unit-tested).
  - Blocks and the controller in `dev.farmings.coop`.
  - The run is found by the shed's `PastureLayout`.
- **Health "points" are a share of the health bar:** a bird's health bar is small (a chicken has 4), so the doc's "points on a 100-point
  scale" are applied as that share of each bird's bar:
  - a dry duck loses 5% a day;
  - full litter costs 2% a day;
  - a cold chick loses 10% a day.
  - Health never drops below 1.
- **Perches:** a roosting bird's home is a floor cell beside a perch. Birds don't sit on the bar itself.
- **Hoppers:**
  - A hopper under the controller pulls litter.
  - A hopper under a nest box pulls eggs.
  - Grain goes into the feeder by hand or by hopper.
- **Game tests:** coop_forms, coop_takes_tame_birds, hen_lays_in_nest, broody_hen_hatches, birds_out_by_day_in_at_night, duck_needs_pond,
  litter_builds_up, chick_cold_without_heat.

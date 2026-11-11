# Hatchery: incubator and brooder

**Status: spec, 2026-11-11 (Tim, 2026-11-10: "Hatchery: incubator + brooder").** Figures are from `docs/animals/poultry.md`
unless marked *(proposed)*. Both machines run on Wildcatter low-voltage power, as the shearing stand does.

## Incubator (`incubator`)
- **What it holds:** **24 eggs** *(proposed: a tabletop cabinet)* of any species, mixed. Eggs go in by hand or by hopper, and
  infertile eggs are refused.
- **What it keeps:** the source settings, shown on the readout:
  - 99.5-100.5 °F in forced air;
  - eggs turned 3 times a day until the species' stop-turning day;
  - then the "lockdown" humidity for the last 3 days (USU 4-H; the VT tip sheet).
  - The game shows these as the stage of each egg; it doesn't simulate air.
- **Power:** **0.1 kW** *(proposed; no rating looked up)*.
- **No power:**
  - The readout says NO POWER.
  - The eggs' clocks pause; they don't die *(proposed, poultry.md)*.
- **Hatch:** when an egg's days are up, the young bird appears on top of the incubator, or beside it if the top is blocked. It needs
  heat at once.
- **Readout:** eggs by species, the day each is on, and the next hatch.

## Brooder (`brooder`)
- **What it is:** a heat lamp hung over the floor.
- **Who it warms:** young birds within **2 blocks** of it (a 5 × 5 area at its height and one below).
- **How many:** up to **25** young birds at a time *(proposed)*.
- **Power:** **0.25 kW** *(proposed: a common heat-lamp bulb rating, not looked up)*.
- **Heat:** the source schedule (95 °F, falling 5 °F a week) is shown on the readout.
  - The game treats "under a working brooder" as warm enough for any age up to heat-until.
- **Feed and water:** young birds under a brooder eat and drink from a grain feeder and water trough within **4 blocks**
  *(proposed)*.
  - A brooder isn't part of a coop, so the coop doesn't feed them.
- **Once feathered:** at their heat-until age they become growers, and a coop within 48 blocks can take them in.
- **No power:** young birds under it are cold (poultry.md: 10 health a day, no growth).

## Recipes
In `docs/recipes.md`, made in the art and recipe part.

## Not in this pass
- Humidity and temperature as simulated quantities.
- Candling.
- Hatch rate below 100%.
- Sexing chicks.

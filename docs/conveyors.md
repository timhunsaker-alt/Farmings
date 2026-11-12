# Belts and augers

**Status: spec, 2026-11-11 (Phase 3; Tim: Farmings' own belt conveyors).** The belt follows Tailings' belt design
(`tailings/docs/multiblocks/crushing-circuit.md`); the code is copied, with no dependency (all three mods are CC0).

| Part | Block | What it does | Source |
|---|---|---|---|
| Belt conveyor | `farm_belt` | A troughed belt at **2 m/s**, laid block by block along one axis, level or up a grade of at most **1 in 3** | Tailings' 600 mm belt at 2 m/s (*proposed* there); the 1 in 3 grade is Tailings' (real belts climb about 18°) |
| Belt drive | `farm_belt_drive` | The head pulley and motor of one run: **7.5 kW at 480 V per 30 m of run**, rounded up. A run with no drive, or too little drive, does not move | Tailings *(proposed there; no farm figure found)* |
| Grain auger | `grain_auger` | A tube auger for grain, meal and seed only. It climbs at any angle up to **45°** *(proposed)*, laid as a straight run like a belt | 8 in auger, dry shelled corn, level, 600 rpm: 2,700 bu/h at 1.6 hp per 10 ft; at 45°, 2,100 bu/h (Purdue Research Bulletin 740, Table 2 in [OSU BAE-1105](https://pods.okstate.edu/fact-sheets/BAE-1105pod2017.pdf)) |
| Auger drive | `grain_auger_drive` | **3.7 kW (5 hp)** for a run of up to **9 blocks** (was 21: a 21 ft auger read as 21 blocks); longer runs take a second drive | Westfield 8 in utility auger: 3 hp at "16' – 21'", 5 hp at "26' – 31'" (maker's sheet, [AGI](https://www.aggrowth.com/globalassets/brands-section/westfield/product-images/2019_agi_westfield_utility_augers.pdf)) |

## How things move
- **Parcels:** items ride a run as parcels at the belt's speed. A 20-block run takes 10 s. This is Tailings' `BeltTravel`, plain Java.
- **Grain auger:** it moves grain at a set rate, **2,700 bu/h of corn** (was 2,750), about **19 kg/s** (Purdue's level 8 in figure at
  56 lb/bu, *derived*). It does not run faster for being longer: "The length of the auger will affect the power requirement, but not
  the capacity" ([OSU BAE-1105](https://pods.okstate.edu/fact-sheets/BAE-1105pod2017.pdf), on Purdue Bulletin 740).
- **Picking up and dropping:** a run picks up from what drops on its tail cell: a hopper, a machine's output face, or the gantry's
  headland stop. It delivers into whatever its head touches: the silo's blower, the grain bin, the mill, the mixer, the next belt, a
  hopper or a chest.
- **Blocked head:** if the head can't take an item, the run stops and keeps its load *(as Tailings)*.
- **No power:** the readout says NO POWER, and nothing moves.

## Not in this spec
- Belt wear.
- Spillage.
- Belt scales.

## Built, 2026-11-11
- **Code:**
  - `farm/conveyor/Travel` is plain Java and unit-tested.
  - `conveyor/` holds the cell blocks, the drive (a Wildcatter 480 V load, rated by run length) and `ItemSink`, through which Farmings
    machines take from a run.
- **Belts are level only in this pass.** Augers run in any direction.
- **Items riding a belt are not drawn yet.** The belt's surface is animated, and the drive's readout lists its parcels.
- **Recipes:**
  - belt: 3 leather (for rubber) over 3 iron, makes 6;
  - auger: iron, stick, iron, makes 4;
  - drives from iron, copper and redstone.
- **Game tests:** belt_carries_to_chest, belt_blocked_head_holds, auger_lifts_grain.

## Loose ends, 2026-11-11 (spec)
- **Slopes:** a belt run can climb or fall one block for every 3 along, at most (the 1 in 3 above). A `farm_belt` placed one block up
  from the cell behind it becomes a sloped belt facing up. A run's length is counted along the slope: a sloped cell rises a block over one, so it is
  √2 = 1.41 m of belt *(derived)*.
- **Items drawn:** the drive's parcel list is sent to the client. Each parcel is drawn as its item, lying on the belt at its place
  along the run. A sloped cell lifts it.

### Built, 2026-11-12 (loose ends)
- **Slopes:**
  - Belt cells have a `slope` state: flat, up or down. A belt placed a block above the cell behind it makes that cell rise, and one
    placed a block below falls into it.
  - The run is traced up and down its slopes.
  - Two slopes closer than 3 cells read TOO STEEP and stop the run.
  - Augers stay straight.
- **Drawn items:** the drive sends its load to clients twice a second. `client/BeltItemsRenderer` lays each parcel on the belt at its
  place along the run and moves it on smoothly between updates.
- **Game test:** belt_climbs_slope. **Client scene:** `belts`.

### Sourcing pass, 2026-11-12
- Resolved: auger capacity and "length doesn't change capacity" (Purdue 740 via OSU), the auger drive's run (9 blocks, was 21).
- Still unsourced: the belt drive's 7.5 kW per 30 m (searched Purdue, OSU, NDSU for belt power; found only calculators) and the 45°
  limit. Real augers climb to 90°, losing capacity (Purdue 740); the game keeps the level rate.

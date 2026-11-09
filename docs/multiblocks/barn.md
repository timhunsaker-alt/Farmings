# The freestall barn (APPROVED by Tim 2026-11-07)

Where cattle live, eat, drink and lie down, and where their manure is collected. A **freestall barn** is a row of individual
stalls the cows come and go from, a feed bunk with hay, water, and an alley that a **powered scraper** pushes clean into a
**collection pit**. The cattle are in [../animals/cattle.md](../animals/cattle.md), the manure's next steps in [../waste.md](../waste.md),
and the parlor is a separate spec (`parlor.md`, not written).

**Decided with Tim, 2026-11-07:** scraper motor 2.2 kW and one pass per game day; water comes from **both** a Wildcatter pipe and a
bucket; a roof and walls are **not** part of the structure for now; the manure fork is a **new tool**. The other proposals
(feeder holds 4 hay blocks a cell, one trough per row, same stalls for every breed, 50 kg per manure item, 64 manure items a pit
cell) stand as approved unless Tim changes them.

A figure marked *(proposed)* is a proposal or a scaled value. A figure marked *(unverified)* has no good source yet. Nothing marked
*(proposed)* or *(unverified)* is built until Tim approves it.

**Real counterparts:**
- A **tail-to-tail freestall barn**: two rows of free stalls with a scrape alley between them and a feed bunk on each outer side.
- An **alley scraper** (a cable- or hydraulic-driven blade that pushes manure along a flat alley to a cross-channel or pit).
- A **manure pit** or reception pit at the end of the alley.
- A **water trough**.

## Size (1 cell = 1 m)
| Thing | Real | Game |
|---|---|---|
| Stall width (Holstein, 1,000 lb cow) | 107 cm centre to centre; 145 cm for a 2,000 lb cow | 1 cell (rounded) |
| Stall length | 3.1 m, plus 0.9-1.2 m lunge space | 3 cells; the lunge space is the open side beyond the stall |
| Feed bunk space | about 75 cm per head (heifer table, *unverified* for cows) | 1 cell per cow, in line with its stall |
| **Scrape alley** | **3 m** (Tim, 2026-11-07) | 3 cells wide |
| **Alley length** | runs past the stalls into cross-alleys at both ends | **twice the stall row** (Tim, 2026-11-09): 2(2n+1) cells |

Sources: [Dairyland Initiative](https://thedairylandinitiative.vetmed.wisc.edu/adult-cow-housing/freestall-design-and-dimensions/),
[Lactanet](https://lactanet.ca/en/housing-newly-weaned-heifers-2/). A stall is one cell wide whatever the breed (rounding).
Small breeds (Jersey, 470 kg) still use a whole stall in this version. *(proposed)*

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `barn_controller` | 1 x 1 x 1 | The control and starter box for the scraper (like a motor starter), at the pit end. It is the multiblock's controller |
| `stall_bed` | 1 x 1 x 3 | A freestall bed (the cow's lying surface) with a curb. Holds **one** cow |
| `stall_divider` | 1 x 2 x 3 | The steel loops that divide stalls. One between each pair of beds, and one at each row end |
| `feed_bunk` | 1 x 1 x 1 | The concrete feed bunk. Cows reach it from their stall |
| `hay_feeder` | 1 x 1 x 1 | A hay rack above a bunk cell. Holds hay blocks (the vanilla hay block, **100 kg**); cows eat from it |
| `water_trough` | 2 x 1 x 1 | A float-valve trough. Fed by a Wildcatter pipe or filled by bucket *(proposed, see decisions)* |
| `alley_floor` | 1 x 1 x 1 | Grooved concrete alley floor. It collects manure and is the surface the scraper rides over |
| `scraper_rail` | 1 x 1 x 1 | The guide channel the scraper blade runs in, along the alley's centre |
| `manure_scraper` | 3 x 1 x 1 | The blade and carriage: spans the 3 m alley, pushes manure toward the pit |
| `scraper_drive` | 1 x 1 x 1 | The drive unit at the far end: a gearmotor and cable drum. **Motor: 2.2 kW (3 hp), 480 V three-phase** *(proposed, unverified; real alley scrapers use small motors of roughly 1 to 3 hp)* |
| `collection_pit` | 3 x 2 x 2 | A reception pit. Holds manure items until the player or a later machine takes them |

Power: only the `scraper_drive` (and a later trough heater) draws power. It takes Wildcatter's three-phase at 480 V (NA) or 400 V
(IEC), read from the world's standard, through the usual motor box. Nothing else in the barn needs power.

## Correction (2026-11-07, found while writing the forming code)
The sketch below drew the beds shoulder to shoulder (`B B B B`, 4 cells for 4 stalls) but also put a divider between every pair of beds
(`D . D . D . D . D`, 9 positions). Both cannot be true: a divider needs its own cell. **A stall takes two cells along the barn: one for
the bed and one for the divider after it, plus a divider at the start. So n stalls a side take 2n+1 cells** (4 stalls: 9 cells,
8 cows). The alley, rail and floor are the same 2n+1 long. Everything else in the sketch stands. The forming code and its tests
(`husbandry/barn/BarnLayout`) use this. Tell Tim if he would rather have stalls one cell wide with the dividers drawn on the beds.

## Layout (a sketch, tail-to-tail, facing north)
One bay, 4 stalls long per side, so 8 cows. A barn is as many stalls long as the player builds (2 to 12 per side *(proposed)*).

Plan, y 0 (floor), north at the top. x runs along the barn, z across it:
```
 x ->  0   1   2   3   4   5
 z 0   F   F   F   F   .   .     F feed_bunk (north bunk row)
   1   B   B   B   B   .   .     B stall_bed (3 cells long, rows z1-z3, north side)
   2   B   B   B   B   .   .
   3   B   B   B   B   .   .
   4   A   A   A   A   P   P     A alley_floor (3 wide, z4-z6); the scraper_rail runs along z5
   5   R   R   R   R   P   P     R scraper_rail      P collection_pit (3 x 2 x 2, set into the floor at the east end)
   6   A   A   A   A   P   P
   7   B   B   B   B   .   .     B stall_bed (south side)
   8   B   B   B   B   .   .
   9   B   B   B   B   .   .
  10   F   F   F   F   .   .     F feed_bunk (south bunk row)
```
Layer y 1 (one above the floor; dividers are 2 high and run into y 2):
```
 x ->  0   1   2   3   4   5
 z 0   H   H   H   H   .   .     H hay_feeder above each feed_bunk
 z 1-3 D . D . D . D . D          D stall_divider between beds (x 0,1,2,3,4 boundaries; drawn as the wall positions)
 z 4   .   .   .   .   .   .
 z 5   S   .   .   .   C   .     S scraper_drive (west end), C barn_controller (east end, beside the pit)
       (the manure_scraper, 3 cells across z4-z6, rides at y 1 on the rail)
 z 7-9 D . D . D . D . D
 z 10  H   H   H   H   .   .
```
Water: a `water_trough` (2 x 1 x 1) sits at the east end of each bed row, against the last stall (x 4-5, z 1 and z 9). Capacity:
one trough serves the row *(proposed; real practice is one trough space per roughly 10 to 15 cows, unverified)*.

**Controller position:** the `barn_controller`, at (4, 1, 5) in the sketch, beside the pit and in line with the scraper rail.
**Valid rotations:** the 4 horizontal rotations, and a mirror (the pit on either end). The scraper_drive must be at the end opposite
the pit.

## Forming
- The player places the parts as the sketch shows and right-clicks the `barn_controller`.
- It scans the region from the pit to the drive, along the alley.
- It forms when all of these are true:
  - at least 2 stalls per side, both rows the same length;
  - every stall has a bed, and a divider on each side;
  - every stall has a bunk cell and a `hay_feeder` in line with it;
  - the alley is a continuous run of `alley_floor` 3 wide, with a `scraper_rail` along the centre, a `manure_scraper` on it, a
    `scraper_drive` at the end opposite the pit, and the `collection_pit` at the pit end;
  - at least one `water_trough` in each bed row.
- If it fails, the controller names the missing part ("Stall 3 north has no divider"), as Tailings' cab does.

## How it runs
- **Cattle:** a cow in the barn picks a free stall to lie in, eats from the nearest `hay_feeder` with hay, and drinks from the
  trough. Stall count is the herd limit: one cow per stall.
- **Feeding:** each cow takes the dry matter her stage needs (cattle.md). A 100 kg hay block holds 85 kg of dry matter, so one lactating
  cow eats about a quarter of a block a day. A feeder holds up to 4 hay blocks per cell *(proposed, so 8 cows have 16 blocks, about 8 game days)*.
- **Water:** from the UC Davis rule (cattle.md): 6 L per kg of dry matter eaten plus 1 L per litre of milk.
- **Manure:** each cow adds manure to the alley or her bed over the day. A cow makes about 45-50 kg a day (waste.md). One manure item
  stands for **50 kg**, about one cow-day *(proposed)*.
- **Dirty stall rule:** uncollected manure in a cow's stall or in front of it lowers her health and milk quality. She never dies
  from it. The size of the effect is *(unverified)* (waste.md).
- **Hand collection:** a player can clear manure from a stall or the alley with a manure fork *(a tool item; proposed)*.
- **The scraper:** when its drive has power it makes one pass per game day *(proposed; real scrapers run several times a day)*,
  pushing every manure item in the alley into the pit, then returns. A pass starts on a redstone signal into the controller too.
- **The pit:** holds up to 64 manure items per cell *(proposed)*, 384 in all. When it is full the scraper stops (PIT FULL) and
  the alley fills.
- **What the controller reads when the scraper stops:** NO POWER, PIT FULL, BLOCKED (something on the rail), NO SCRAPER.

## When a part is removed
- **Stall bed, divider, bunk or hay feeder:** that stall goes out of use, and the cow in it is released (she stays in the world and
  walks away). The rest keeps working. The controller names the missing part.
- **Water trough:** if a row has none, its cows get no water; the dehydration rule is the underfeeding rule (less milk, less
  health, never death).
- **Alley floor, rail, scraper or drive:** the scraper stops with a fault. Cows stay housed. Manure collects until fixed.
- **Collection pit:** manure pushed to the end stays on the floor. The scraper stops with NO PIT.
- **Barn controller:** the barn stops as a unit. Cows stay in their stalls but are no longer fed or counted. Re-placing the
  controller re-forms it.
- Any part's contents (hay, manure) drop when it is broken (the rule from Tailings R1).
- A cow's state (age, pregnancy, lactation, health, genome) lives on the cow and survives all of the above.

## Game tests (to add with the code)
- `barn_forms`: builds the 4-stall bay and checks it forms.
- `barn_missing_part`: removes a divider and checks the right stall is named and the rest still runs.
- `barn_scraper_pass`: puts manure in the alley, powers the drive, and checks it all ends in the pit.
- `barn_scraper_blocked_and_full`: a block on the rail, and a full pit.
- `cow_state_survives_save`: a cow in a barn through a save and a chunk unload.

## Built (2026-11-08)
The scraper pass, drive motor, pit store, manure item and fork are built as above. Proposals added with no source: the blade crosses a cell
in 20 ticks, and the fork has 250 uses. The scraper rests in the first alley cell beside the drive.

## Built, step 4 (2026-11-08)
Cows take stalls, eat from the feeder and drink from a trough, and make manure from what they ate. Proposals added with no source: a trough
holds 1,000 L (a bucket); stages with no sourced intake eat at the dry cow's 2.2% of body weight. The pipe-fed trough, lying down, the
dirty-stall effect and milk are not built.

## Built, 2026-11-09: the dirty-stall effect (all values proposed; Tim chose "build, marked proposed")
Manure lying in the alley cell in front of her stall makes her stall dirty. One item (one day's manure, cleared by the daily scraper pass) is
normal. Each further item adds a quarter, so five items is fully dirty. Fully dirty costs 10% of her milk. At half dirty or worse she loses 1
health (of 10) per game day, never below 2, so she never dies. Game test: dirty_stall_less_milk.

## Built, 2026-11-09: the pipe-fed trough
The trough is a Wildcatter water user: a water pump whose pipe touches any cell of it tops it up to 1,000 L, as a float valve would. A
bucket still works. Game test: trough_filled_by_pipe.

## Changed 2026-11-09: the alley is twice as long as the stall rows (Tim)
Tim asked for the scrape alley to be double the length of the stall rows, with the extra split evenly between both ends. A row of n stalls is
2n+1 cells, so the alley (rail and both floor rows, from the drive to the cell before the pit) is **2(2n+1)** cells:
```
 drive | n open cells | divider bed divider ... divider (2n+1) | n+1 open cells | pit
```
The total open length, 2n+1, is odd, so the pit end gets the extra cell *(proposed)*. Nothing is required beside the open stretches;
they are where the troughs can stand and where the cow lane to the parlor leaves the alley (parlor.md). Real barns have cross-alleys at
the row ends for the same reasons. The scraper sweeps the whole alley, and rests beside the drive as before. With 12 stalls a side the
alley is 50 cells. A barn built before this change no longer forms; the controller names the length it needs ("The alley is 9 cells long;
it must be twice a stall row, 2 x (2n+1): 18 for 4 stalls a side"). Tests: BarnLayoutTest (both open stretches, the old short alley) and
every barn game test, rebuilt to the longer alley.

## Open decisions
None. All eight were settled on 2026-11-07 (see the top of this file). The *(unverified)* tags remain: the scraper motor size has no
source, the trough ratio has no source, and the dirty-stall effect size has no source. Each is revisited when a source is found.

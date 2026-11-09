# The creamery (APPROVED in scope by Tim 2026-11-09: cream and skim, butter, cheese, pasteurized bottled milk)

The farm's dairy processing room: milk in from the parlor's bulk tank (piped) or by bucket, then pasteurized, and made into bottled milk,
cream and skim, butter, or aged cheddar. Everything it makes is food. Nothing is sold.

A figure marked *(proposed)* has no source and is a game value. Process times are real times scaled 15:1, like the animals.

## Parts (all on Wildcatter's big-part system, one item each)
| Part | Cells (W x H x D) | Real counterpart | Power |
|---|---|---|---|
| `creamery_controller` | 1 x 1 x 1 | the control panel | none |
| `receiving_tank` | 2 x 2 x 2 | a raw-milk receiving tank, 1,000 L *(proposed, the bulk tank's size)* | none |
| `batch_pasteurizer` | 1 x 2 x 1 | a batch (vat) pasteurizer, 200 L a batch *(proposed)* | heater 9 kW *(proposed)* |
| `cream_separator` | 1 x 1 x 1 | a centrifugal cream separator | 2.2 kW *(proposed)* |
| `butter_churn` | 1 x 1 x 1 | a butter churn | 1.5 kW *(proposed)* |
| `cheese_vat` | 2 x 1 x 1 | a cheese vat with agitator | 0.75 kW *(proposed)* |
| `cheese_press` | 1 x 1 x 1 | a cheese press | none |
| `aging_shelf` | 1 x 2 x 1 | a cheese aging rack | none |
| `bottler` | 1 x 1 x 1 | a hand bottle filler | none |

## Layout *(proposed)*
The controller stands at one end. All the other parts stand in one straight row on the same level, starting in the cell beside the
controller, in any order, with no gaps, each part exactly once (the tank and vat take two cells along the row). Any of the four
horizontal directions works. The controller names a missing part.

## Milk in
- **By bucket:** a milk bucket on the receiving tank pours 10 L (the same bucket size as the bulk tank).
- **By pipe:** a run of `milk_line` blocks from any cell of the receiving tank to any cell of a parlor `bulk_tank` (up to 64 blocks)
  moves 50 L a second *(proposed)* from the bulk tank into the receiving tank while there is room.
- **Milk fat** is tracked by volume through both tanks. Each cow's milk carries her breed's fat (Holstein 3.72%, Jersey 4.76%, Brown
  Swiss 4.02%, USDA AIPL, cattle.md; Gyr 4.55%, IJAnS review) times her butterfat score over her breed's baseline (kept between
  0.8 and 1.2 *(proposed)*). A vanilla milk bucket counts as Holstein milk (3.72%).
- **Protein** is taken as the breed table's figure for the milk's mix (Holstein 3.06% when unknown); it only matters for cheese.

## Processing (the controller runs once a second; a mode picks the product)
Sneak-right-click the controller to change the mode: **BOTTLE**, **CREAM**, **BUTTER**, **CHEESE**. Right-click it to read it.
1. **Pasteurize** (every mode): the pasteurizer takes a batch of up to 200 L from the receiving tank, heats it and holds it at 63 C for
   30 minutes (the US batch pasteurization rule, FDA Grade "A" PMO), then cools it. Heat-up and cool-down take 30 minutes each
   *(proposed)*. 90 real minutes scaled 15:1 is 100 ticks. The heater must have power the whole time, or the batch waits.
2. Then, by mode:
   - **BOTTLE:** the batch goes to the bottler as pasteurized whole milk.
   - **CREAM:** the separator splits it by fat balance into cream at 36% fat and skim at 0.1% fat (cream 35-40%, Wisconsin Center
     for Dairy Research; skim 0.1%, Dairysafe Victoria). Cream litres = milk x (milk fat - 0.1) / (36 - 0.1).
   - **BUTTER:** separated as above, then the churn turns the cream into butter at 80% fat (the US legal minimum, 80-82% in practice,
     CDR) and buttermilk. About 1.1% of the cream's fat is lost to the buttermilk (Iowa State, lowest churning loss 1.0-1.2%). The skim
     goes to the bottler.
   - **CHEESE:** the vat makes cheddar curd by the Van Slyke formula: kg of cheese per 100 kg of milk = (0.93 x fat% + 0.78 x
     protein% - 0.1) x 1.09 / (1 - 0.38) (University of Guelph cheesemaking text; USDA AMS uses 38% moisture). Holstein milk gives
     about 10 kg per 100 kg. Whey is not kept *(proposed)*. The press makes 10 kg wheels *(proposed size)* after 12 hours real
     *(proposed)*, and each wheel ages on the shelf for 60 days real (the US minimum age for raw-milk cheese, used here as a cheddar
     age), which is 4 game days.
3. A machine without power stops its step; the batch waits there. NO POWER names the part.

## What comes out (all food)
| Item | Stands for | Taken from | Food *(proposed)* |
|---|---|---|---|
| `bottled_milk` | 1 L pasteurized milk | bottler, with an empty glass bottle | 4 hunger, clears effects like a milk bucket |
| `cream_bottle` | 1 L cream | bottler | 2 hunger |
| `skim_milk_bottle` | 1 L skim | bottler | 2 hunger |
| `buttermilk_bottle` | 1 L buttermilk | bottler | 2 hunger |
| `butter` | 0.5 kg | churn, empty hand | 3 hunger |
| `cheddar_wheel` | 10 kg aged cheddar | aging shelf, empty hand | crafts into 8 `cheddar` slices, 3 hunger each |

Glass bottles go into the bottler from the player's hand: each right-click fills one, from the first product that has a litre (milk,
then cream, skim, buttermilk), and the empty bottle is used up.

## Game tests
creamery_forms, creamery_missing_part, pasteurizer_bottles, separator_splits_fat, churn_makes_butter, cheese_yield_and_aging,
creamery_no_power, milk_line_from_parlor, creamery_state_survives_save.

## Built (2026-11-09)
All of the above, in `process/creamery` (plain Java: `Dairy`, `Creamery`, `CreameryLayout`) and `dev.farmings.creamery` (blocks and
controller). The parlor's bulk tank now tracks fat and protein by volume, and the parlor adds each cow's milk with her own fat
(`husbandry/MilkComponents`). Results: 100 L of Jersey milk gives about 13 L of cream and 87 L of skim. 200 L of Holstein milk gives about
9.1 kg of butter, or about 20.5 kg of cheddar (two 10 kg wheels and some curd left over). 9 game tests, all passing.

## Not built
Pipes for the products, other cheeses, yogurt, ice cream.

## Whey (built 2026-11-09)
- **How much:** a cheese make leaves whey: the milk's mass less the cheese's. This is the mass balance used in cheddar yield work
  ([SDSU](https://openprairie.sdstate.edu/etd/1291)), at the milk's 1.03 kg a litre *(proposed: whey is a little lighter)*. 200 L of
  Holstein milk gives about 20.5 kg of cheddar and **about 180 L of whey**, about 9 kg of whey per kg of cheese at a 10% yield.
- **Where it goes:** whey goes to the bottler's store like the other liquids (store 1,000 L; a full store stops the vat with BOTTLER FULL). It
  bottles after buttermilk, as `whey_bottle`: 1 hunger *(proposed)*. It is kept as a future pig feed.
- **Old saves:** the creamery's save marks the layout with whey; older saves still load, with no whey.

## Hoppers (built 2026-11-09)
A vanilla hopper works on three parts. It must be against the part's main cell, the bottom cell where it was placed:

| Part | Hopper below takes | Hopper on top or a side puts in |
|---|---|---|
| `bottler` | filled bottles, in the hand order (milk, cream, skim, buttermilk, whey), one per glass bottle | empty glass bottles, kept in the bottler (up to 64; they drop if it is broken) |
| `butter_churn` | butter, 0.5 kg blocks | nothing |
| `aging_shelf` | aged cheddar wheels | nothing |

The products are read from the creamery each time, so nothing is held twice. A hopper that takes an item it cannot hold puts it back.
Taking by hand still works. Game tests: hopper_takes_butter_and_wheels, hopper_bottles_with_glass, whey_from_cheese.

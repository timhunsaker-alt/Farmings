# The creamery (APPROVED in scope by Tim 2026-11-09: cream and skim, butter, cheese, pasteurized bottled milk)

The farm's dairy processing room: milk in from the parlor's bulk tank (piped) or by bucket, then pasteurized, and made into bottled milk,
cream and skim, butter, or aged cheddar. Everything it makes is food. Nothing is sold.

A figure marked *(proposed)* has no source and is a game value. Process times are real times scaled 15:1, like the animals.

## Parts (all on Wildcatter's big-part system, one item each)
| Part | Cells (W x H x D) | Real counterpart | Power |
|---|---|---|---|
| `creamery_controller` | 1 x 1 x 1 | the control panel | none |
| `receiving_tank` | 2 x 2 x 2 | a raw-milk receiving tank, 1,000 L *(proposed, the bulk tank's size)* | none |
| `batch_pasteurizer` | 1 x 2 x 1 | a batch (vat) pasteurizer, 200 L a batch ([Foodtechprocess CHV 200](https://foodtechprocess.com/en/pasteurizers/101-cheese-vat-batch-pasteurizer-chv-200-300-500.html): "Nominal volume, L: 200") | heater 12 kW (was 9; same sheet: "Heating electric, kW: 12") |
| `cream_separator` | 1 x 1 x 1 | a centrifugal cream separator, 500 L/h | 1.5 kW (was 2.2; [NDPAC](https://www.ndpac.com/product/cream-centrifugal-separation-skimming-for-dairy-factories/): "Capacity 500 L/h", "Power (KW) 1.5 KW"; a sister 500 L/h model there has a "3KW motor") |
| `butter_churn` | 1 x 1 x 1 | a butter churn, up to 50 L of cream | 1.5 kW ([Milky FJ 100](https://serowar.eu/en_GB/p/Electric-butter-churn-Milky-FJ-100/1437): "Power of motor: 1,5 kW", "Max. Capacity: 50 L", "Time of butter making: 20 - 35 min") |
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
  ([SDSU](https://openprairie.sdstate.edu/etd/1291)), at the milk's 1.03 kg a litre *(proposed: whey is a little lighter)*. A paneer-whey study gives about
  1,015 kg/m³ at 20 °C ([AGRIS](https://agris.fao.org/search/en/records/65df41a163b8185d9cabc400), confirmed 2026-11-14; acid whey, not cheddar whey). 200 L of
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

## Sheep milk (built 2026-11-10)
- **One milk kind at a time:** the creamery works cow milk or sheep milk, never both. It changes kind only when it is empty of
  everything: raw milk, the batch, stores, butter, curd and wheels. Milk of the other kind is refused at the receiving tank, and the
  milk line leaves it in the parlor's tank.
- **Products of sheep milk:**
  - **sheep milk** bottles and **sheep cheese** wheels (cut into sheep cheese);
  - cream, skim, buttermilk, whey and butter, which are the shared items.
- **Yield:** the Van Slyke cheese yield and the butter yield read the milk's own fat and protein. East Friesian milk (5.68% fat,
  4.92% protein) gives more cheese per litre than Holstein milk.

### Sourcing pass, 2026-11-12
- Resolved: pasteurizer batch 200 L and heater (now 12 kW), separator (now 1.5 kW at 500 L/h), churn 1.5 kW. Whey density is a TO VERIFY.
- Still unsourced: vat motor 0.75 kW (only 0.18-0.25 kW for 50-150 L vats found, in snippets), heat-up and cool-down 30 min (CFIA, Florida
  FDACS give the 63 C / 30 min hold only), press 12 h (Guelph text not reachable). Tank size, wheel size, flow, fat clamp and hunger are game values.

## Goat milk and chèvre, 2026-11-13 (spec; Tim: "Bottled milk + chèvre")
- **Milk kind:** the tank and the creamery hold **cow, sheep or goat** milk, one kind at a time, with the sheep milk rules above. Faults
  read `TANK HOLDS GOAT MILK` and so on. Goat milk comes only by milk line, as sheep milk.
- **Goat milk in each mode:**
  - **BOTTLE:** goat milk bottles.
  - **CREAM / BUTTER:** as cow milk, into the shared cream, skim, buttermilk and butter. Goat milk does make butter: "The following
    recipe is for cow or goat's milk" ([FAO](https://www.fao.org/4/t1265e/t1265e13.htm)). Its small fat globules ("naturally
    homogenized", Park, after Fahmi et al. 1956) cream slowly by gravity, which a separator does not need.
  - **CHEESE:** refused for goat milk (no aged goat wheel this pass; Tim). The readout says to use CHÈVRE.
  - **CHÈVRE** (new, goat milk only): below.

### The chèvre mode
| Step | Value | Source |
|---|---|---|
| Pasteurize | 63 C for 30 min, as every mode | 21 CFR 1240.61 |
| Cool to set | **28 C** | "the milk was cooled to 28 °C" ([PMC10572428](https://pmc.ncbi.nlm.nih.gov/articles/PMC10572428/)); Cornell: "80-90 degrees" ([Cheesemaking Simplified](http://goatdocs.ansci.cornell.edu/Resources/GoatArticles/GoatProducts/CheesemakingSimplified.pdf)) |
| Culture and rennet | 0.20% starter, 0.02% rennet; not separate items *(proposed: as cheddar, the vat supplies them)* | PMC10572428: "0.20% (w/w) of starter culture … 0.02% (v/w) of rennet" |
| Set | **18 hours** (12-24) | "until there is a firm curd with a thin layer of clear whey on top, usually 12-24 hours" (Cornell) |
| Drain | **16 hours** (8-24) in the cheese vat's cloths | "hang to drain for 8-24 hours depending on how firm you want the cheese to be" (Cornell) |
| Yield | **17 kg of chèvre per 100 kg of milk** (14-20) at 60% moisture | Guo et al. 2004, Small Rumin. Res. 52:103: yield "ranged from 14% in June … to 20% in December and January" ([AGRIS](https://agris.fao.org/search/en/records/65df50124c5aef494fe1a1e5)). The game uses the middle; it does not move with fat and protein *(proposed)* |
| The rest | whey, to the whey store (milk kg - chèvre kg) | as cheese |

- **Machine time:** setting and draining run in the vat at the creamery's 15:1 machine scale (waste.md): 34 real hours is 113 s *(derived: 90 real minutes is 100 ticks, as the pasteurizer)*.
  The vat needs power to hold 28 C *(proposed)*; with no power the batch waits.
- **Item:** `chevre`, **0.25 kg** a piece *(proposed: a small log)*, 2 hunger *(proposed)*. It is not aged; it is taken from the aging
  shelf (by hand or a hopper below), the creamery's one cheese outlet, after any aged wheels.
- **Shelf life:** see food.md (goat products).
- **Not modelled:** salt (1 tsp per pound, [Baetje Farms via SARE](https://projects.sare.org/wp-content/uploads/Baetje-Farms-CHEVRE-Cheese-recipe.pdf)),
  molds, herbs or ash coatings.

### Built, 2026-11-13: goat milk and chèvre
- **Code:** `husbandry/parlor/MilkKind` (cow, sheep, goat) in the tank and creamery; `Creamery.Mode.CHEVRE` with the vat's setting and
  draining, saved as version 3 (older saves load). Items `goat_milk_bottle` and `chevre` (Ludo icons).
- **Faults:** CHEVRE NEEDS GOAT MILK; GOAT MILK: USE CHEVRE (in the CHEESE mode); VAT BUSY while a batch drains.
- **Tests:** `ChevreTest`; game tests chevre_from_goat_milk, cow_milk_refused_for_chevre.

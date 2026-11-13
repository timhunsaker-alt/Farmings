# Food spoilage (APPROVED by Tim 2026-11-09)

Farmings food goes off. **Tim's choices:**
- Farmings food only. Vanilla beef and milk buckets do not spoil.
- Spoiled food turns into a spoiled item.
- Shelf lives are real days played as **game days** (1 real day = 1 game day = 24,000 ticks).
- Perishables keep **1 game day** out of the cold, marked *(proposed)*. The only sourced room figure is the USDA two-hour safety rule,
  which would be about 100 seconds.
- The parlor bulk tank and the creamery count as cold. Food starts its clock when it leaves them.

The walk-in fridge that keeps food cold is [multiblocks/cold-room.md](multiblocks/cold-room.md).

## Shelf lives (`process/food/Spoilage`)
| Food | In a powered fridge | Out of the cold | Turns into |
|---|---|---|---|
| bottled milk, skim milk | 7 days | 1 day *(proposed)* | spoiled bottle |
| cream | 3.5 days (half and half 3-4) | 1 day *(proposed)* | spoiled bottle |
| buttermilk | 10.5 days (1-2 weeks) | 1 day *(proposed)* | spoiled bottle |
| whey | 7 days *(proposed: no FoodKeeper entry; milk's)* | 1 day *(proposed)* | spoiled bottle |
| butter | 45 days (1-2 months) | 1.5 days (1-2 days on the counter) | rancid butter |
| cheddar wheel | 180 days (hard cheese, 6 months) | 14 days *(proposed)*. **TO VERIFY**: FSIS mail-order table, hard cheese "safe at room temperature, but refrigeration prolongs quality" ([archived FSIS](https://webarchive.library.unt.edu/web/20090115174930/http:/www.fsis.usda.gov/PDF/Mail_Order_Food_Safety.pdf), snippet only; gives no days) | moldy cheese |
| cheddar (cut) | 24.5 days (3-4 weeks after opening) | 1 day *(proposed)* | moldy cheese |
| steak, raw (any grade) | 4 days (3-5) | 1 day *(proposed)* | spoiled steak |
| steak, cooked | 3.5 days (cooked beef 3-4) | 1 day *(proposed)* | spoiled steak |

Each figure is the middle of the source's range. Sources:
- USDA FoodKeeper, as relayed by:
  - [Wyoming Dept. of Health](https://health.wyo.gov/wp-content/uploads/2018/06/FOOD-STORAGE-TIMES-.pdf): milk, buttermilk;
  - [American Dairy Association](https://www.americandairy.com/health-wellness/how-to-store-dairy-products/): half and half, hard cheese;
  - [Safety+Health](https://safetyandhealthmagazine.com/articles/23534-is-butter-better-and-safer-in-the-fridge) and others: butter.
- [USDA FSIS, "Beef from farm to table"](https://www.fsis.usda.gov/food-safety/safe-food-handling-and-preparation/meat-catfish/beef-farm-table)
  and [FoodSafety.gov](https://www.foodsafety.gov/food-safety-charts/cold-food-storage-charts): steaks and cooked beef.

## How it works
- **The clock:** each food stack carries the tick it spoils at room temperature (custom data `SpoilsAt`). It starts when the food leaves
  the cold:
  - bottled or taken by hand at the creamery;
  - taken by a hopper;
  - dropped by a slaughtered animal;
  - for anything else (a crafted cheddar slice, a cooked steak), the first time it is carried.
- **Stacking:** spoil ticks are rounded up to a minute (1,200 ticks) *(proposed)*, so food made together carries the same tick and stacks.
- **Carried food** turns into its spoiled item when the tick comes, keeping its count.
- **In a powered walk-in fridge,** once a minute every food stack's tick moves later by the time the fridge saved: a minute x (1 - room
  life / fridge life). Milk kept cold therefore lasts its 7 days. Without power the fridge saves nothing.
- **The bar** under the icon shows the room-temperature life left: green when fresh, through yellow, to red.
- **Spoiled food** can be eaten (1 hunger *(proposed)*) but gives Hunger (30 s) and Nausea (15 s), like spoiled milk armor. It is meant for
  compost later.
- **Food in a chest outside a fridge** is not looked at until it is taken. Then it turns at once if its tick has passed.
- The milk armor keeps its own clock (1 game day, milk-armor.md).

**Tests:**
- `SpoilageTest`: a day out of the cold; 7 days for cold milk; the FoodKeeper figures.
- Game tests: milk_spoils_at_room (bottles stamped at the creamery stack, then turn) and spoiled_food_sickens. fridge_slows_spoilage is
  with the walk-in fridge.

## Sheep products (2026-11-10)
| Item | Fridge | Room | Source |
|---|---|---|---|
| Sheep milk | 7 days | 1 day | cow milk's FoodKeeper figure, used for sheep milk *(proposed: no sheep-milk entry found)* |
| Sheep cheese wheel / cut | as cheddar (180 / 24.5 days) | as cheddar | hard cheese, FoodKeeper *(proposed for sheep cheese)* |
| Lamb and mutton chops, raw | 4 days (3-5) | 1 day | USDA FoodKeeper, lamb chops |
| Lamb and mutton chops, cooked | 3.5 days (3-4) | 1 day | USDA FoodKeeper, cooked meat |

Spoiled chops become **spoiled mutton** (Hunger and Nausea, as the other spoiled foods).

## Loose ends, 2026-11-11
- **Food ages by the room's temperature:** in a cold room, food ages by the room's temperature. It is log-linear between each food's
  fridge and room lives (multiblocks/cold-room.md, "the room's temperature").
- **Farmings chicken:** raw and cooked Farmings chicken join the perishables (eggs.md).

### Sourcing pass, 2026-11-12
- One TO VERIFY added (hard cheese at room temperature). No value changed. The 1-day room lives are Tim's game choice over the 2-hour rule.
- Still unsourced: whey and sheep-milk fridge lives (no FoodKeeper or extension entry found; only blogs), cheddar wheel room days.
  Rounding and spoiled-food hunger are game values.

## Goat and new poultry products, 2026-11-13 (spec)
| Item | Fridge | Room | Turns into | Source |
|---|---|---|---|---|
| Goat milk | 7 days | 1 day | spoiled bottle | cow milk's figure *(proposed: no FSIS or extension figure for goat milk; searched, only blogs)* |
| Chèvre | **14 days** (1-3 weeks) | 1 day | moldy cheese | soft cheeses "will keep from one to three weeks after opening" ([Clemson HGIC](https://hgic.clemson.edu/factsheet/handling-of-cheese-for-safety-quality/)). A sealed commercial log keeps 45 days (**TO VERIFY**, snippet: Masotti et al. 2012, J. Dairy Sci.); farm chèvre is not sealed |
| Kid and chevon chops, raw | **4 days** (3-5) | 1 day | spoiled mutton | "use within 3 to 5 days" (FSIS *Goat from Farm to Table*, as reproduced by [ABGA](https://www.abga.org/goat-from-farm-to-table)) |
| Kid and chevon chops, cooked | 3.5 days (3-4) | 1 day | spoiled mutton | "Cooked meat or poultry \| 3 to 4 days" ([FoodSafety.gov chart](https://www.foodsafety.gov/food-safety-charts/cold-food-storage-charts)) |
| Goose, raw | **1.5 days** (1-2) | 1 day | spoiled poultry | "Fresh Duck or Goose 1 to 2 days" ([FSIS, Duck and goose](https://www.fsis.usda.gov/food-safety/safe-food-handling-and-preparation/poultry/duck-and-goose-farm-table)) |
| Goose, cooked | 3.5 days (3-4) | 1 day | spoiled poultry | "Cooked Duck or Goose … 3 to 4 days" (FSIS) |
| Guinea fowl, raw | 1.5 days (1-2) | 1 day | spoiled poultry | chicken's "1 to 2 days" (FoodSafety.gov) *(proxy: no guinea figure found)* |
| Guinea fowl, cooked | 3.5 days | 1 day | spoiled poultry | "Cooked meat or poultry \| 3 to 4 days" |

The room column is Tim's 1-day game rule, as for every other food. Eggs are in eggs.md.

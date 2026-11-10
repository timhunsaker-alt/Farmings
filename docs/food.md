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
| cheddar wheel | 180 days (hard cheese, 6 months) | 14 days *(proposed)* | moldy cheese |
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

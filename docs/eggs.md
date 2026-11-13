# Eggs, poultry meat, feathers and down

**Status: spec, 2026-11-11 (Tim, 2026-11-10: "Meat and processing", "Feathers and down", "Egg grading and cartons", "Manure to
compost").** Birds and figures are in `docs/animals/poultry.md`. Nothing is sold.

## Egg items
- **Items:** one item each, `chicken_egg`, `turkey_egg`, `quail_egg` and `duck_egg`.
- **What each egg carries:**
  - weight in grams;
  - shell shade, which sets the icon tint: white, cream, brown, speckled or mottled;
  - the tick it was laid;
  - fertile or not;
  - for a fertile egg, the parents' genome.
- **Stacking:** only identical eggs stack. Graded eggs stack well (by class); ungraded ones stack poorly. Cartons fix this.
- **Use:**
  - Any Farmings egg works where a vanilla recipe takes an egg (cake, pumpkin pie), through a `farmings:eggs` tag.
  - A **fried egg** is cooked on a furnace, smoker or campfire. Its food value is 3 hunger, 3.6 saturation *(proposed)*.
  - A thrown Farmings egg breaks; it doesn't hatch anything *(proposed: only an incubator or a broody hen hatches)*.

## Egg grading table (`egg_grader`)
A hand-worked table that takes chicken eggs by hand or hopper and puts out cartons. It has no power.

**Size (USDA weight classes):** the minimum net weight per dozen, from *How to Buy Eggs* (USDA AMS Home and Garden Bulletin 264,
[archived PDF](https://webharvest.gov/peth04/20041015232259/http:/www.ams.usda.gov/howtobuy/eggs2.pdf)). Per egg, the game
divides by 12:

| Size | oz per dozen | g per egg (minimum) |
|---|---|---|
| Jumbo | 30 | 70.9 |
| Extra Large | 27 | 63.8 |
| Large | 24 | 56.7 |
| Medium | 21 | 49.6 |
| Small | 18 | 42.5 |
| Peewee | 15 | 35.4 |
| Under Peewee | | ungraded *(proposed)* |

**Grade:** USDA grades eggs by their interior. AA has whites "thick and firm" and yolks "high, round"; A has whites "reasonably
firm"; B has whites "thinner" and yolks "wider and flatter" (same bulletin). The interior falls with age, so the game grades by age
since laying *(proposed thresholds)*:

| Grade | Rule |
|---|---|
| AA | laid less than 7 days ago, and clean |
| A | less than 21 days |
| B | older, or a floor (dirty) egg |

- **Cartons:**
  - The table packs **12 eggs of one size and grade** into a carton: `egg_carton`, labelled with size and grade.
  - Turkey, duck and quail eggs are packed 12 to a carton by species, without a size class *(proposed: the USDA classes are for
    hen eggs)*.
  - A carton opens back into its eggs.
- **Readout:** eggs waiting, cartons packed, and the share of each size.

## Keeping eggs
- **Cold:** "in the refrigerator (40 °F). For best quality, use within 5 weeks" (USDA HG 264). So in a powered cold room an egg keeps
  **35 days**.
- **Out of the cold:** **7 days** *(proposed; the FSIS room-temperature guidance was not reachable)*.
- **Spoiling:** a spoiled egg becomes a **rotten egg**, which can be thrown and gives Nausea; it is not food. The spoilage clock is
  `process/food/Spoilage`, as for milk.
- **Fertile eggs:** they stop being hatchable after 7 days cold-stored or not started (poultry.md).

## Meat
- **What a bird gives when killed:**
  - meat items: its live weight × the dressing percentage (poultry.md), at **one item per 0.5 kg** *(proposed)*;
  - feathers;
  - and, for a duck, down.
- **Meat items:**

  | Species | Raw | Cooked |
  |---|---|---|
  | Chicken | vanilla `chicken` | vanilla `cooked_chicken` |
  | Turkey | `turkey` | `cooked_turkey` |
  | Duck | `duck` | `cooked_duck` |
  | Quail | `quail` | `cooked_quail` |

  - Chicken keeps the vanilla items, so vanilla cooking and recipes work.
  - Food values for the new items copy vanilla chicken's: raw 2 / 1.2, cooked 6 / 7.2 *(proposed)*.
  - A quail at 0.14 kg × 60% gives no whole item, so it drops one `quail` per bird *(proposed: at least one)*.
- **Examples:**
  - Plymouth Rock hen, 3.4 kg × 68%: 4 chicken items.
  - Bronze tom, 16.6 kg × 80.5%: 26 turkey items.
- **Keeping meat:** raw poultry keeps **1.5 days** in the cold room. Cooked poultry keeps **3.5 days**.
  - These are USDA FSIS figures: "Chicken or turkey, pieces 1 to 2 days" raw, and cooked poultry "Pieces, plain 3 to 4 days"
    ([FSIS 2004 fact sheet](https://webharvest.gov/peth04/20041015215325/http:/www.fsis.usda.gov/OA/pubs/facts_basics.pdf);
    FoodSafety.gov's chart refused the fetch).
  - Out of the cold, the steak rule applies: **1 day** *(proposed)*.
  - Spoiled poultry becomes **spoiled poultry** (Hunger and Nausea).

## Feathers and down
- **Feathers:** 1-2 vanilla feathers per chicken, turkey or duck (the vanilla chicken drop) *(proposed)*. Quail drop 0-1.
- **Down:** a duck gives **1 down item** (about 30 g of down a bird, Farrell review, TO VERIFY).
- **Down jacket:** a chest piece made from 8 down and 1 felt panel *(proposed recipe)*.
  - It is in the freeze-immune wearables tag, like the wool clothing (`docs/wool.md`).
  - Its armor is leather's, as for the wool pieces *(proposed)*.
- **Down quilt:** a second recipe for the vanilla white bed, with 3 down in place of the 3 wool *(proposed)*.

## Poultry litter
- **What it is:** the coop's litter (`coop.md`) comes out as **poultry litter** items (10 kg each, *proposed*).
- **Where it goes:**
  - The Phase 3 compost line (`docs/waste.md`) is not built yet. Until it is, poultry litter can go into the vanilla composter with
    a **0.85** chance per item to add a layer (vanilla's chance for bread) *(proposed)*.
  - The barn's manure item is unchanged.
- **Later:** when the compost line is built, litter is one of its inputs.

## Not in this pass
- Pasteurised or liquid eggs.
- Pickled eggs.
- Smoking.
- Feather meal.
- Plucking machines: a bird is processed as cattle and sheep are, by killing it.

## Built, 2026-11-11
- **Code:**
  - Egg grading rules in `husbandry/poultry/EggGrading` (unit-tested).
  - The table is `dev.farmings.eggs.EggGraderEntity`, with cartons in `item/EggCartonItem`.
  - Spoilage entries EGG, RAW_POULTRY and COOKED_POULTRY in `Spoilage`.
- **Vanilla recipes:** a cake and a pumpkin pie made with any `#farmings:eggs`, and a white bed from down, sit beside vanilla's own recipes.
- **Not built as written:**
  - A rotten egg can't be thrown; it is a plain item.
  - Vanilla chicken meat from Farmings chickens does not spoil; it's the vanilla item.
  - Eggs in a carton keep their own spoil clocks but aren't checked until the carton is opened.
- **Game tests:** grading_packs_cartons, table_eggs_spoil_fertile_dont, poultry_meat_and_down.

## Loose ends, 2026-11-11 (spec)
- **Throwing a rotten egg:** a `rotten_egg` is thrown like a vanilla egg (use it in the air; it flies as a snowball does, 16 to a
  stack). It hatches nothing. A mob or player it hits gets **Nausea for 5 seconds** *(proposed)*, and it breaks with an egg's
  particles. It is still not food.
- **Farmings chicken:** a chicken from a Farmings bird drops `farmings:chicken`, and cooking gives `farmings:cooked_chicken`, in place
  of vanilla chicken. Both spoil as the other fowl meat does:
  - raw: 1-2 days in the fridge, so 1.5 *(USDA FSIS: "Chicken or turkey, pieces 1 to 2 days";
    [2004 fact sheet](https://webharvest.gov/peth04/20041015215325/http:/www.fsis.usda.gov/OA/pubs/facts_basics.pdf), the live page
    refused the fetch)*;
  - cooked: 3-4 days in the fridge, so 3.5 (same sheet: "Use cooked leftovers within 4 days");
  - room temperature: 1 day *(proposed; the only sourced figure is the 2-hour safety rule, same sheet)*.
  - Their food values are vanilla chicken's.
  - Furnace, smoker and campfire recipes cook them, and they carry the `c:foods/raw_meat` and `c:foods/cooked_meat` tags, so other
    mods' recipes take them.
  - Vanilla chickens and their meat are untouched.

### Built, 2026-11-11 (loose ends)
- **Rotten eggs:** `RottenEggItem` throws a `ThrownRottenEgg`, drawn as the item, which gives 5 s of Nausea.
- **Chicken:** Farmings birds drop `farmings:chicken`, which cooks to `farmings:cooked_chicken`. Both spoil, and both carry the
  `c:foods` meat tags.
- **Game tests:** rotten_egg_thrown_nauseates, farm_chicken_spoils.

### Sourcing pass, 2026-11-12
- Resolved 1 mark: the FSIS raw and cooked poultry fridge times.
- Still unsourced: shell eggs out of the cold (the FSIS egg page refused the fetch) and the duck down yield (Farrell review only).

## Geese and guinea fowl, 2026-11-13 (spec)
- **Egg items:** `goose_egg` (Embden and Toulouse 170 g, Chinese 120 g) and `guinea_egg` (40 g); weights in poultry.md. Graded by
  species only, as turkey and duck eggs. A goose egg is "1 egg" = 144.0 g in USDA FoodData Central
  ([FDC 172190](https://fdc.nal.usda.gov/food-details/172190)), inside the breed range.
- **Keeping:** as hen eggs (5 weeks cold). Goose and guinea egg figures are NOT FOUND in FSIS or FoodSafety.gov; the hen figure ("Raw
  eggs in shell | 3 to 5 weeks") stands in. Guinea eggs have a "longer shelf life, in comparison to chicken eggs"
  ([SciELO review](https://www.scielo.br/j/rbca/a/KwspKmHJXdMGhypFbRX4dpC/)); not used *(no number)*.
- **Fried egg:** both fry, as the other eggs.
- **Meat:** `goose` / `cooked_goose`, `guinea_fowl` / `cooked_guinea_fowl` (food.md for keeping). Food values copy chicken's *(proposed)*.
- **Down:** a goose drops 1 down item at slaughter (poultry.md).
- **Feathers:** 1-2 vanilla feathers per goose or guinea fowl *(proposed: as the others)*.

# Feed mill, TMR mixer and rations

**Status: spec, 2026-11-11 (Phase 3; Tim: a mill and mixer, and a balanced ration changes milk, growth and eggs).** Marks are as in
`docs/crops.md`. Feed values are in `docs/crops.md`, "Feed values".

## Machines
| Part | Block | What it does | Figures |
|---|---|---|---|
| Hammer mill | `hammer_mill` | Grinds what goes in its top into meal at its output face. Inputs and outputs: corn, oat and barley grain to `corn_meal`, `oat_meal` and `barley_meal`; alfalfa hay or bales to `alfalfa_meal`; vanilla calcite to `ground_limestone`; vanilla cod or salmon to `fish_meal`. 1 kg in, 1 kg out | **4.8 kWh/t at 1.9 t/h** for corn to 900 µm ([Mississippi State](https://extension.msstate.edu/agriculture/livestock/swine/particle-size-important-for-swine-diets)), so a **9.1 kW** motor and 0.53 kg/s *(derived)* |
| TMR mixer | `tmr_mixer` (2 x 2 x 2) | A vertical mixer. Ingredients come in by belt, auger or hopper. Four minutes after the last ingredient, the batch is mixed into ration items at its output face | **7.1 m³ needs 75 hp (56 kW)** (**TO VERIFY**: the 75 hp is from [Alltech](https://www.alltech.com/taxonomy/term/986?page=0), a trade source; searched again 2026-11-14, the page did not load). Fill to 75-95% of struck capacity ([Ag Proud 2023](https://www.agproud.com/articles/57757-considerations-for-sizing-a-tmr-mixer)); the game's 80% fits. Mix **4 minutes** after the last ingredient (was 1): "Typically, three to five minutes is sufficient time starting from when the last ingredient is added" ([Saskatchewan Agriculture](https://publications.saskatchewan.ca/api/v1/products/129055/formats/154881/download)), which is 16 s of game time at 15:1 (waste.md: machine times scale 15:1) |

- **Mixer capacity:** 7.1 m³ x 80% at an as-fed density of **400 kg/m³** (was 350): 25 lb/ft³, the low end of "ration density
  assumptions ranged from 25 to 32 pounds per cubic foot" (a trade article, confirmed 2026-11-14,
  [Ag Proud 2023](https://www.agproud.com/articles/57757-considerations-for-sizing-a-tmr-mixer)), about **2.3 t a batch** (was 2). Sizing
  check: "A typical dairy cow will consume 5 to 7 cubic feet of a TMR ration per day" (trade, confirmed 2026-11-14, [Ag Proud](https://www.agproud.com/articles/24953-doug-williams-the-nuts-and-bolts-of-tmr-mixers)),
  so one batch feeds about 40 cows for a day *(derived)*.
- **Power:** both machines are Wildcatter 480 V loads, like the creamery's.

## Ingredients and the ration item
- **What a ration is:** a ration item, `tmr` (**25 kg as-fed**, *proposed*), carries its own analysis: dry matter %, and CP, NDF and
  TDN % of DM, worked out from what went in by dry-matter weight. It also records whether the batch held premix and limestone.
- **Ingredients:** every crop product in `docs/crops.md`, the meals, and these:

| Ingredient | DM % | CP % | NDF % | TDN % | Other | Source |
|---|---|---|---|---|---|---|
| `alfalfa_meal` | 89 | 17 | 47 | 58 | | alfalfa hay (Auburn table) |
| `fish_meal` | 90 | 66 | 12 | 74 | | "Fish meal" ([Auburn table](https://ssl.acesag.auburn.edu/agriculture/livestock-poultry/beef/documents/2014FeedcompositionTables.pdf)) (was 92, 62, 0, 75) |
| `ground_limestone` | 98 | 0 | 0 | 0 | Ca 34% | "Limestone ground", Ca 34.00 (Auburn table) (was DM 100, Ca 38%: that is calcium carbonate's 38.5) |
| `mineral_premix` | 100 | 0 | 0 | 0 | fed at **0.265 kg a cow a day** (was 0.3) | a product label, "typically at 265 grams/head/day when fed as the sole source of supplemental minerals and vitamins" ([Masterfeeds](https://www.masterfeeds.com/nutrition/total-balance-2-dairy-premix-15156/)) |

- **Premix recipe:** bone meal + ground limestone, shapeless, makes 4 *(proposed)*.

## What a ration does
Each animal's feeder (barn hay feeder, sheep rack, coop grain feeder) keeps the dry-matter-weighted average analysis of what is in it.
The animal's daily intake carries that analysis. A ration score of 0 to 1 sets the effect below. The **shape** of each score is
*(proposed)*; its targets and end points are sourced.

| Animal | Targets for score 1 | Effect | Source |
|---|---|---|---|
| Dairy cow, in milk | CP 15-18%, NDF 25-30%, forage at least 40% of DM, premix fed | milk **x (0.67 + 0.33 x score)**: a balanced TMR is the breed's own milk record; poor forage alone gives 67% of it | CP: "crude protein requirements range from 15 percent for 50 pounds of milk to 18 percent for cows producing 110 pounds of milk" ([UNL G1358](https://extensionpubs.unl.edu/publication/g1358/na/html/view)). NDF 25-30%: the MSD table gives a 25-33% NDF minimum ([MSD Veterinary Manual, feeding guidelines for large-breed dairy cattle](https://www.msdvetmanual.com/multimedia/table/feeding-guidelines-for-large-breed-dairy-cattle)); UNL says NDF "should not fall below 26–28 percent". Forage 40%: "Generally, 40–50 percent forage dry matter is the minimum amount to avoid low milk fat test" (UNL G1358). Kolver & Muller 1998, J. Dairy Sci. 81:1403: "milk production (29.6 vs. 44.1 kg/d)" on pasture and TMR, so 29.6 / 44.1 = 0.67 ([AGRIS](https://agris.fao.org/search/en/records/65df57e77c7033e84bed7e99); pasture stands in for poor hay) |
| Growing cattle | CP 12% or more and TDN 65% or more *(proposed targets)* | growth **x (0.29 + 0.71 x score)** (was 0.6 + 0.4 x score) | Penn State backgrounding: "Expected daily gain (pounds) 0.5 0.5 0.75 1.0 1.0 1.0 1.25 1.75" ([Penn State](https://extension.psu.edu/beef-background-production)). The worst ration gains 0.5 lb a day on grass hay; the best, 1.75 lb, uses corn silage and a protein supplement (ration 7 or 8). The floor is 0.5 / 1.75 = 0.29 *(derived)* (was 0.6, proposed) |
| Lambs | TDN 66% or more, CP 15% or more | growth **x (0.8 + 0.2 x score)** | grain-fed or creep-fed lambs gain 20-30% more ([UMN](https://conservancy.umn.edu/server/api/core/bitstreams/8c42e89a-437a-43c1-a82b-f9c63e85b151/content)), so 1 / 1.25 = 0.8 |
| Ewes and does, in milk | CP 11.6% or more, the low end of 11.6-15.5% in early lactation (NRC 2007); the score falls to 0 at CP 8% *(proposed shape)* | milk **x (0.83 + 0.17 x score)** | [Penn State](https://extension.psu.edu/nutrition-throughout-pregnancy-for-sheep-flocks). A low-protein diet gave "a decrease (P< 0·01) in daily milk yield of 0·53 kg (17%)" ([Robinson et al. 1979, Anim. Prod.](https://www.cambridge.org/core/journals/animal-science/article/further-studies-on-the-response-of-lactating-ewes-to-dietary-protein/4B2BBE8C3B04B31243DEC30731070984)), so 0.83 at worst *(derived)*. Grass hay or pasture (CP 10.5%) gives x0.95 |
| Laying hens | CP 16% or more, Ca 3-4% | eggs **x (0.75 + 0.25 x score)**. Plain grain (9-13% CP, under 1% Ca) scores near 0 | [Alabama ANR-2913](https://www.aces.edu/wp-content/uploads/2022/09/ANR-2913_FeedingtheLayingHen_080822L-G.pdf): CP 16%, Ca 3-4%, and on grain alone "egg production is likely to suffer". Laying falls at 13.5% and 12% CP ([WUR](https://edepot.wur.nl/557184)). The 0.75 floor is *(proposed)* |

- **Hay and grain on their own score by their own analysis.** Good alfalfa hay alone gives a cow CP 17%, NDF 47%, all forage: a middling
  score.
- **Changing existing results:** milk from cows on grass hay alone drops to about 70% of what it is now. This is the point of the
  ration (Tim). The cattle and sheep game tests that pin milk figures are updated to feed a TMR.

## Not in this spec
- Particle size and roller mills (a roller mill uses about half a hammer mill's energy, Mississippi State).
- Starch, sugar and fat as separate nutrients.
- Bloat.
- Feed bunk management.

## Built, 2026-11-11
- **Code:**
  - `feed/Feedstuff`, `feed/Ration` and `feed/FeedMix` are plain Java with unit tests.
  - The mill and mixer are one block entity, `feedmill/FeedMachineEntity`.
  - The items are `feedmill/FeedItems`; the ration item is `feedmill/RationItem`, whose tooltip shows its analysis and its factors.
- **Running average:** each animal keeps a running average of what it eats. It starts balanced, so animals that no feeder has fed are
  unchanged.
  - Cattle: from the barn's hay feeders.
  - Sheep: from the racks, and pasture while grazing.
  - Birds: from the coop's grain feeder.
- **Effects:**
  - milk x `milkFactor`; ewe and doe milk x `eweMilkFactor` (2026-11-14);
  - housed cattle's growth x `growthFactor`;
  - lambs' growth x `lambFactor`;
  - eggs x `eggFactor`.
- **What the feeders take:**
  - A barn hay feeder takes any feed: hay blocks, loose hay, silage, haylage, straw, grain, meal and rations. A round bale goes in only
    when the feeder is empty.
  - The coop's grain feeder takes grain, seed, meal and rations by their mass as fed.
- **The mill and mixer are single blocks** in this pass, not the spec's 2 x 2 x 2 mixer.
- **Into the barn:** a barn hay feeder takes rations (and any feed) from a belt or auger at its side, or straight from a mixer beside it,
  one item at a time while there is room.
- **Game tests:** mill_grinds, mixer_makes_ration, feeder_ration_feeds_cow, belt_feeds_barn_feeder.

## Loose ends, 2026-11-11 (spec): the full-size mixer
- The TMR mixer becomes the spec's **2 x 2 x 2** big part, placed from one item. It takes ingredients on any face and puts out rations
  at its output face. Its figures are unchanged.
- A mixer placed as a single block before this change still works.

### Built, 2026-11-12 (loose ends): the full-size mixer
- **Code:** `FeedMixerBlock`, a 2 x 2 x 2 big part. A click on any cell loads the batch.
- **Output:** rations are handed on from every outside face (`Towers.handOn` now works from all of a big part's cells).
- **Game test:** mixer_multiblock_forms.

### Sourcing pass, 2026-11-12
- Resolved: mixing time (4 min, was 1), fish meal and limestone (Auburn table), the dairy CP and forage targets (UNL G1358),
  Kolver & Muller's milk figures. Changed but still weak: TMR density (400 kg/m³) and the premix rate (265 g).
- Still unsourced: Alltech's mixer power (page would not load), Manitoba's NDF (page gone), Penn State's backgrounding table (garbled),
  the ewe milk response (not searched further). Score shapes and floors are game choices.

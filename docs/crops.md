# Crops: forage and small grains

**Status: spec, 2026-11-11 (Phase 3; Tim's answers of the same day).** Real figures carry a source. *(proposed)* marks a game
choice with no source. **TO VERIFY** marks a weak source (a trade page, a search snippet, a site far from typical farms). **NOT
FOUND** means no source was found, so the game value is proposed.

Tim's choices:
- **Crops:** alfalfa, silage and grain corn, grass hay (timothy and orchardgrass), oats and barley.
- **Growth:** on the **15:1** cattle clock, with no random ticks.
- **Field work:** a gantry on rails (`multiblocks/field-gantry.md`) tills, plants, harvests, mows, rakes, bales and spreads.
- **Yields are scaled x10:** each block gives ten times its real square metre. One cow then needs about 32 x 32 blocks of alfalfa
  instead of 100 x 100. The proportions between crops stay real. The sheep pasture is scaled for the same reason (sheep.md).
- **Seeds:** they drop from grass in matching biomes.
- Vanilla crops are unchanged.

## Time and space
- **Time:** 1 game day = 15 real days for crops, as for cattle (`TimeScale.LIVESTOCK`). For example, alfalfa's 32.5 days between
  cuttings is 2.2 game days.
- **Space:** 1 block = 1 m² = 1/10,000 ha. At x10, **1 t/ha gives 1 kg per block**.
- **No seasons:** crops grow all year *(proposed, as sheep breed all year)*. A perennial is cut on its regrowth interval all year round.

## The crops
| Crop | Kind | Seed to first harvest | Regrowth / cuttings | Stand life | Real yield | Game yield per block per harvest |
|---|---|---|---|---|---|---|
| Alfalfa | perennial legume | 65 days (60-70 after emergence) | 32.5 days (30-35); up to 5 cuts a year (4 used for the yield split) | 5.5 years (3-8) | 3-5 t hay/ac a year (4: 8.96 t/ha, 89% DM, so 7.97 t DM/ha) | 2.0 kg DM (2.24 kg hay) |
| Timothy | perennial grass | 60 days *(proposed; NOT FOUND)* | 42 days *(proposed; NOT FOUND; a secondary report says about 6 weeks)*; 1-2 cuts a year (2 used) | 5 years *(proposed; NOT FOUND; the day counts were searched again 2026-11-14, the pages did not load)* | 2 t hay/ac a year in 2 cuts (4.48 t/ha, 88% DM; confirmed, UMaine 2265e) | 1.97 kg DM |
| Orchardgrass | perennial grass | 60 days *(proposed)* | 35 days (4-6 weeks); 4 cuts (3-4, WSU) | 5 years *(proposed)* | 2-4 t hay/ac a year (3: 6.72 t/ha, 88% DM, so 5.92 t DM/ha) (was 3.5 t DM/ac, 7.84 t DM/ha, from a "per harvest" figure) | 1.48 kg DM (was 1.96) |
| Corn, silage | annual, 2 blocks tall | **125 days** (was 100, proposed) *(derived: slightly before R6)*. Silking "69-75 days after emergence (1250 GDU)"; R6 "130 days after emergence or 50-60 days after silking"; "Silage harvest would be slightly earlier than R6" ([UW corn, critical stages](https://corn.aae.wisc.edu/Management/pdfs/CriticalStages.pdf)) | none | one harvest | 20-25 t as-fed/ac at 35% DM (22.5: 7.9 t DM/ac, 17.6 t DM/ha) | 17.6 kg DM (50.3 kg chopped as-fed) |
| Corn, grain | annual, 2 blocks tall | **130 days**, R6 (UW corn, above; was 114, Kentucky's relative maturity at 2,700 GDD, which fell before the new silage day) | none | one harvest | 179.3 bu/ac, US 2024 (11.25 t/ha) | 11.25 kg grain |
| Oats | annual | 100 days (confirmed, WSU EM4918E; a seed catalogue gives 100-120; milk stage is 55-65 days; South Dakota State, a midseason oat "requires about 90 days from planting until maturity", [core.ac.uk](https://core.ac.uk/download/215574937.pdf)) | none | one harvest | 61 bu/ac (2.19 t/ha); straw 2.03 t/ac (4.55 t/ha) | 2.19 kg grain + 4.55 kg straw |
| Barley | annual | 87 days (was 85; Alaska gives 80-100) | none | one harvest | 76.7 bu/ac, US 2024 (4.13 t/ha); straw 1.01 t/ac (2.26 t/ha) | 4.13 kg grain + 2.26 kg straw |

**Sources:**
- **Alfalfa:**
  - first cut and intervals: Ohio State, [seeding-year harvest](https://agcrops.osu.edu/specialization-areas/forages/seeding-year-harvest-management);
  - cuts, stand life, yield and seeding rate: UMaine, [Bulletin 2245](https://extension.umaine.edu/publications/resource/bulletin-2245-maine-forage-facts-alfalfa).
- **Timothy:** UMaine [2265](https://extension.umaine.edu/publications/2265e/).
- **Orchardgrass:**
  - interval: UMaine [2266](https://extension.umaine.edu/publications/2266e/). Its yield, "2 to 5 tons of dry matter per acre per
    harvest", is not used;
  - yield: Arkansas [FSA3154](https://www.uaex.uada.edu/publications/pdf/FSA3154.pdf): "Hay yield: 2-4 tons/acre per year";
  - cuts: WSU [EB2004](https://wpcdn.web.wsu.edu/wp-extension/uploads/sites/2071/2024/04/EB2004-Orchardgrass-Hay-Production-Guide-for-Col-Basin-WA-Copy.pdf):
    "Columbia Basin growers take from 3 to 4 cuttings", "at 32 to 40 d intervals".
- **Corn:**
  - silage yield: Kentucky [ID-275](https://publications.ca.uky.edu/files/ID275.pdf);
  - relative maturity: Kentucky, [planting dates](https://graincrops.ca.uky.edu/files/corn_planting_dates_blacklayer.pdf);
  - grain yield: USDA NASS Crop Production 2024, as quoted by [Iowa Agriculture](https://iowaagriculture.gov/news/strong-prod-strong-market-demand):
    "The average yield in the United States was estimated at a record high 179.3 bushels per acre".
- **Barley:**
  - yield: USDA NASS, [Crop Production 2024](https://esmis.nal.usda.gov/sites/default/release-files/tm70mv177/bz60fp33t/fj237w71q/crop0924.txt);
  - days: Montana State, [Havre 2020 spring barley trial](https://agresearch.montana.edu/narc/Programs_and_projects/varietytesting/varietytestingreports/reports/2020/2020OnStationBarley.html):
    seeded "April 27, 2020", trial mean maturity 205.3 days from January 1 ("176 = June 24"), so 87 days *(derived)*. Alaska
    [MP 2014-06](https://uaf.edu/afes/publications/database/miscellaneous-publications/files/pdfs/MP_2014-06.pdf) gives 80-100;
  - straw: [Ohio State](https://agcrops.osu.edu/newsletter/corn-newsletter/2018-30/northwest-ohio-field-scale-barley-yield-results).
- **Oats:**
  - yield: [Wisconsin oat and barley trials 2023](https://alseed.com/wp-content/uploads/2024/02/2023-Oats-and-Barley-Wisconsin-Performance-Tests.pdf);
  - straw: [Iowa State](https://iastatedigitalpress.com/farmreports/article/16209/galley/14842/view);
  - milk stage: [Penn State](https://extension.psu.edu/spring-oats-seeding-date-seed-spring-oats-in-early-march).
- **Bushel weights:** corn 56, oats 32 and barley 48 lb ([Ohio State AGF-503](https://ohioline.osu.edu/factsheet/agf-503)).

**Rules:**
- **Perennials:**
  - A cut perennial drops back to its regrowth stage and grows to harvest again in its regrowth days.
  - It dies after its stand life, counted in cuts at 4 a year for alfalfa (22 cuts), 2 for timothy (10) and 4 for orchardgrass (20)
    *(proposed: real stands thin, they don't end on a date)*.
- **Grain or silage:** a corn plant is "silage" if it is cut by the forage chopper from 125 days, and "grain" if the combine takes it
  at 130 or later.
- **Combining corn too early:** a combine on corn under 130 days takes nothing and leaves the plant *(proposed)*.

## Growth stages and the clock
- **Stages:** each crop has 8 visible stages (0 to 7, as vanilla wheat), spread evenly over its days to harvest *(proposed)*.
- **The clock:**
  - A crop block schedules its next stage on the game clock when it is planted (a scheduled block tick, so it carries on after a
    chunk reload).
  - Random ticks don't move it.
  - Light at night and water are not modelled *(proposed: the gantry and the field are the game)*.
- **Fertilizer** (compost, liquid digestate, vanilla bone meal) works exactly as vanilla bone meal (waste.md, decided 2026-11-07): one
  use moves a crop on by bone meal's random 2-5 stages.

## Products
| Item | What | Mass and use |
|---|---|---|
| `alfalfa_hay`, `grass_hay` | hay from the baler or by hand | baled into **round bales** of **454 kg** (a 5 x 5 ft round bale, about 1,000 lb, [UF/IFAS AN326](https://edis.ifas.ufl.edu/publication/AN326)) at **82% DM** (hay is baled at 80-82% DM, [Ohio State](https://agcrops.osu.edu/newsletter/corn-newsletter/22-2021/steps-speed-field-curing-hay-crops)) |
| `round_bale` | alfalfa or grass hay | 454 kg; the barn's hay feeder and the sheep's rack take it as 372 kg of dry matter. One bale opens into 4 vanilla hay blocks (100 kg each) and 54 kg of loose hay as a remainder item *(proposed)* |
| `chopped_corn`, `chopped_alfalfa` | forage chopper output, into the silo | 25 kg as-fed an item *(proposed)* |
| `corn_silage`, `haylage` | out of the silo | 25 kg as-fed an item *(proposed)*; corn silage 34% DM, haylage 37.5% DM |
| `corn_grain`, `oat_grain`, `barley_grain` | combine output | **1 kg an item** *(proposed)*. Birds take it in the grain feeder by its mass, so 1 item = 10 seed items |
| `oat_straw`, `barley_straw` | the swath the combine leaves; raked and baled | round bales of straw, 454 kg *(the hay bale's size, proposed)* |
| seeds | 6 kinds | 1 seed item sows 1 block |

**Hay drying:**
- Cut forage is 20-25% DM (75-80% moisture) and is baled at 80-82% DM.
- Raking is "12 to 24 hours after mowing" ([UW-Madison](https://dairy.extension.wisc.edu/articles/field-drying-forage-for-hay-and-haylage)).
- Drying from 80% to 20% moisture took about 38 h in poor conditions ([Purdue](https://extension.entm.purdue.edu/newsletters/pestandcrop/article/environmental-conditions-affect-time-to-safe-hay-baling-3)).
- Days in the swath: orchardgrass in the Columbia Basin, "Curing takes between 3 to 7 d during the growing season"
  ([WSU EB2004](https://wpcdn.web.wsu.edu/wp-extension/uploads/sites/2071/2024/04/EB2004-Orchardgrass-Hay-Production-Guide-for-Col-Basin-WA-Copy.pdf)).
  The game is faster, nearer the Purdue figure above. Game rule: a mown swath is ready to rake after **1 real day** and ready to bale **1.5
  real days** after mowing. That is about 0.1 game days, and rain pauses it *(proposed)*.
- Haylage is chopped wilted, the same day, at 35-40% DM (Penn State).

**Feed values** (DM basis, [2014 feed composition table, Auburn](https://ssl.acesag.auburn.edu/agriculture/livestock-poultry/beef/documents/2014FeedcompositionTables.pdf)):

| Feed | DM % | CP % | NDF % | TDN % |
|---|---|---|---|---|
| Alfalfa hay (midbloom) | 89 | 17 | 47 | 58 |
| Timothy hay (early bloom) | 88 | 11 | 63 | 59 |
| Orchardgrass hay | 88 | 10 | 67 | 59 |
| Corn silage | 34 | 8 | 46 | 72 |
| Corn grain | 88 | 9 | 10 | 88 |
| Oat grain | 89 | 13 | 28 | 76 |
| Barley grain | 89 | 12 | 20 | 84 |
| Oat straw | 91 | 4 | 73 | 48 |
| Barley straw | 90 | 4 | 78 | 44 |

- **Haylage:** alfalfa's CP, NDF and TDN at 37.5% DM *(proposed: the same plant, wetter)*.
- **The vanilla hay block** (`feed/Hay`) counts as grass hay.

## Seeds
- **Where they drop:** breaking grass (short and tall) drops a new seed at vanilla's wheat-seed odds, chosen by biome *(proposed)*:
  - plains and sunflower plains: corn, alfalfa, oats;
  - savanna: corn, alfalfa;
  - meadow, forest and birch forest: timothy, orchardgrass;
  - taiga: oats, barley;
  - all other biomes: none.
  - Vanilla's own wheat-seed drop is unchanged.
- **From harvest:** a harvested block gives back 1-2 seeds of its crop *(proposed, like vanilla wheat)*. The gantry keeps them in its
  seed hopper.
- **Straw:** bedding, compost carbon and a fibre feed.

## Not in this spec
- Seasons and frost.
- Soil fertility and crop rotation.
- Weeds.
- Irrigation.
- Crop disease.

## Built, 2026-11-11 (crops part)
- **Code:**
  - Rules in `farm/crops/FieldCrop`, which is plain Java; `verifyPlainJava` now covers the `farm` package.
  - Blocks in `crops/` (FieldCropBlock, PerennialCropBlock with a `cuts` count, CornCropBlock with `ripe`) and items in `crops/FieldCrops`.
  - `crops/GrassSeeds` gives seeds from grass.
- **Seeds come only from grass a player breaks** (both loaders' break events). Grass broken by water or explosions gives none.
- **Loose hay and straw are 10 kg items.** By hand, a ripe block drops one with the chance of its yield (alfalfa 2.25 kg of hay: 22.5%).
- **Round bales:**
  - A hay bale crafts into 4 vanilla hay blocks. The bale's other 54 kg is lost to rounding *(proposed)*.
  - A straw bale crafts into 45 loose straw.
- **Game tests:** crop_grows_on_clock, alfalfa_regrows_and_dies, corn_ripens_for_grain, ripe_oats_by_hand.

### Sourcing pass, 2026-11-12
- Resolved: orchardgrass cuts and yield (yield per block changed), barley days (changed), the 2024 corn yield, days in the swath.
- Still unsourced: timothy's first harvest, regrowth and stand life, and orchardgrass's (searched UMaine 2265/2266, MSU, WVU, Arkansas);
  corn days to silage (searched GDD to silking); oat days (only a snippet). Game choices are left as marked.

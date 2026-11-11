# Poultry: chickens, turkeys, quail and ducks

**Status: built 2026-11-11 (Phase 4, Tim's answers of 2026-11-10).** Real figures carry a source. A figure marked *(proposed)*
has no source and is a game choice. **TO VERIFY** marks a figure from a secondary source (a breed society, a review quoting
another paper, a search summary), used until a better one is found. **NOT FOUND** names a figure no source gave; its game value
is then proposed.

Tim's choices (2026-11-10):
- **Species:** all four in one pass, on one shared bird base.
- **Time:** fowl run at **3 real days per game day**; cattle and sheep stay at 15.
- **Eggs:** one real day's lay per game day, the same rule as milk.
- **Housing:**
  - a coop with nest boxes and a run;
  - a hatchery with an incubator and a brooder;
  - broody hens;
  - a duck pond.
- **Wild:** vanilla chickens are replaced by wild Farmings fowl, with species and breed by biome. They are tamed in the existing
  corral, with grain in the bait trough.
- **Products:**
  - meat and processing;
  - feathers and down;
  - egg grading and cartons;
  - manure to compost.
- **Breeds:** 3-4 classic breeds per species.

Buildings: `docs/multiblocks/coop.md` (coop, run, pond) and `docs/multiblocks/hatchery.md` (incubator, brooder). Products are in
`docs/eggs.md`.

## Time scale
**1 game day = 3 real days for every bird** (`TimeScale.of(species)`). Output per game day is one real day's output, as for cattle
and sheep. A laying hen therefore gives about one real day's eggs each game day, while she ages three days.

| Stage | Real | Game days at 1:3 |
|---|---|---|
| Chicken incubation | 21 days | 7 |
| Chicken, hatch to first egg (Leghorn, 50% production) | 143 days | 47.7 |
| Turkey incubation | 28 days | 9.3 |
| Coturnix quail, hatch to first egg | 7 weeks | 16.3 |
| Duck incubation (Muscovy) | 28 (35) days | 9.3 (11.7) |
| Settling in the taming corral | 7 days | 2.3 |

Machines that only process eggs and meat (grading, cold room) keep their own times; only animal life runs on the bird scale.

## Sexes and stages
| Species | Female | Male | Young |
|---|---|---|---|
| Chicken | pullet (before lay), hen | cockerel (under a year), rooster | chick |
| Turkey | hen | tom (jake under a year) | poult |
| Quail | hen | cock | chick |
| Duck | duck | drake | duckling |

- **Stages:** young (needs heat until feathered, below) → grower → adult (lays or breeds) → retired (no more eggs; the year-3 point
  below).
- **Sex ratio:** hatchlings are 50/50 male and female *(proposed; no source gave a hatch sex ratio)*.
- **Eggs:** only females lay. A male does not lay.
- **Capons:** not in this pass.

## Breeds
| Species | Breed | Code | Origin | Use | Male / female kg | Eggs a year | Egg g | Shell | Broody |
|---|---|---|---|---|---|---|---|---|---|
| Chicken | Leghorn | LEG | Italy (Livorno) | eggs | 2.7 / 2.0 | 150-320 (235) | 58.5 | white | rarely |
| Chicken | Rhode Island Red | RIR | Rhode Island, USA | dual | 3.9 / 2.9 | 200-300 (250) | 60 | brown | mostly |
| Chicken | Plymouth Rock | PLR | Massachusetts, USA | dual | 4.3 / 3.4 | about 200 | 60 | brown | normally |
| Chicken | Silkie | SLK | China | pet, sitter | 1.55 / 1.25 | about 100 | 40 | cream or tinted | very |
| Turkey | Broad Breasted White | BBW | USA, commercial | meat | 14.5 / 8.2 | 100-120 (110) | 75 | cream to brown, speckled | (not used) |
| Turkey | Bronze (standard) | BRZ | USA | meat | 16.6 / 9.1 | about 75 | 75 | cream to brown, speckled | good mothers |
| Turkey | Bourbon Red | BRD | Kentucky, USA | meat | 14.5 / 8.2 | 40-50 (45) | 75 | cream to brown, speckled | broody |
| Quail | Coturnix (Japanese) | COT | East Asia | eggs, meat | 0.12 / 0.14 | 200-300 (250) | 10 | mottled brown | rarely |
| Quail | Northern Bobwhite | BOB | North America | meat, game | 0.17 / 0.17 | 50-200 (125) | 9.5 | white | broody |
| Duck | Pekin | PEK | China (Beijing) | meat | 4.3 / 3.9 | 100-180 (140) | 78 | white | rarely |
| Duck | Khaki Campbell | KHC | England | eggs | 1.9 / 1.7 | 200-300 (250) | 78 | off-white | seldom |
| Duck | Mallard | MAL | wild, temperate northern hemisphere | wild, game | 1.1 / 0.94 | 20-50 (35) | 55 | creamy white to greenish-buff | broody |
| Duck | Muscovy | MUS | Central and South America | meat | 5.0 / 2.95 | 60-120 (90) | 78 | white | broody |

Values in brackets are what the game uses (the middle of the range, the sheep rule).

**Chickens:**
- **Weights:** UW-Madison, *Chicken breeds and varieties*
  ([A2880](https://livestock.extension.wisc.edu/articles/chicken-breeds-varieties-a2880)): Leghorn "Cock—6 pounds, hen—4½",
  Rhode Island Red 8½ / 6½, Plymouth Rock 9½ / 7½. Silkie: the European standard 1.4-1.7 / 1.1-1.4 kg
  ([Wikipedia](https://en.wikipedia.org/wiki/Silkie_(chicken_breed)), **TO VERIFY**; the article also gives a smaller US
  figure).
- **Eggs a year:**
  - Leghorn, Rhode Island Red and Plymouth Rock from the Livestock Conservancy breed pages
    ([Leghorn](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/leghorn-chicken/),
    [Rhode Island Red](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/rhode-island-red-chicken/),
    [Plymouth Rock](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/plymouth-rock-chicken/)).
  - Silkie: "A Silkie hen can produce 100 eggs in an ideal year" (Wikipedia, **TO VERIFY**).
- **Egg weight:**
  - Leghorn: 58.5 g at 32 weeks, from the Hy-Line W-36 standard
    ([PDF](https://www.hyline.com/filesimages/Hy-Line-Products/Hy-Line-Product-PDFs/W-36/36%20STD%20ENG.pdf); W-36 is a
    commercial Leghorn strain).
  - Rhode Island Red and Plymouth Rock are "Large" (Livestock Conservancy). USDA Large is 24-27 oz a dozen, about 56.7-63.8 g an
    egg. The game uses 60 g *(proposed, inside the class)*.
  - Silkie: NOT FOUND, so 40 g *(proposed: a small egg)*.
- **Broodiness:**
  - UW-Madison A2880: Leghorns "rarely go broody"; most Reds "show broodiness"; Plymouth Rocks "normally will show broodiness".
  - Silkies are "champions at going broody" ([Oregon State PNW-565](https://extension.oregonstate.edu/catalog/pnw-565-why-did-my-chickens-stop-laying)).

**Turkeys:**
- **Weights:**
  - Broad Breasted White: 32 / 18 lb adult ([UAF Extension](https://www.uaf.edu/ces/publications/database/food/holiday-turkey.php)).
  - Bronze: mature toms 35-38 lb and hens 18-22 lb ([Livestock Conservancy](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/bronze-turkey/));
    the game uses the middles, 36.5 / 20 lb.
  - Bourbon Red: mature 32 / 18 lb ([Livestock Conservancy](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/bourbon-red-turkey/)).
- **Eggs a year:**
  - Broad Breasted White: 100-120 eggs per commercial breeder hen
    ([The Poultry Site, turkey production cycle](https://www.thepoultrysite.com/articles/production-cycle-of-turkeys/p4)).
  - Bronze: "about 75" a year.
  - Bourbon Red: the heritage-turkey figure of 40-50 a year when eggs are pulled
    ([Livestock Conservancy turkey chart](https://livestockconservancy.org/wp-content/uploads/2021/06/TurkeyChart.pdf)).
- **Egg:** 70-80 g, pale cream to medium brown with spotting (same chart); the game uses 75.
- **Broad Breasted White mating:** broad-breasted selection "nearly eliminated their ability to mate naturally" (Livestock
  Conservancy, Bronze page). So a BBW tom does not mate in the game. BBW hens lay fertile eggs only from a standard tom
  *(proposed; artificial insemination is not in this pass)*.
- **Bourbon Red broodiness:** NOT FOUND, so the game gives it the heritage Bronze's "good mothers" *(proposed)*.

**Quail:**
- **Coturnix:**
  - Weights: 100-140 g male, 120-160 g female.
  - Egg about 10 g, "mottled brown".
  - Source: [Extension, raising Japanese quail](https://poultry.extension.org/articles/poultry-management/raising-japanese-quail-in-small-and-backyard-flocks/).
  - Eggs a year: about 200 in the first year ([NSW DPI via The Poultry Site](https://www.thepoultrysite.com/articles/raising-japanese-quail-1)),
    up to 300 a year for domestic lines (Wikipedia). The game uses 250.
  - Hens "rarely go broody" (NSW DPI).
- **Bobwhite:**
  - Eggs: "50-200+ eggs a year" ([USDA APHIS via The Poultry Site](https://www.thepoultrysite.com/articles/bobwhite-quail-production)).
  - Egg weight: 9.3-9.8 g ([Wilson Bulletin 74(1)](https://sora.unm.edu/sites/default/files/journals/wilson/v074n01/p0005-p0027.pdf));
    eggs are white ([Texas A&M](https://wildlife.tamu.edu/quail/northern-bobwhite-kids-reverse-quail-decline-initiative)).
  - Weight: 170-173 g for northern birds ([Wikipedia](https://en.wikipedia.org/wiki/Northern_bobwhite), **TO VERIFY**).
  - Broodiness: NOT FOUND for a farmed line. The game calls it broody *(proposed: a wild-type bird)*.

**Ducks:**
- **Weights and eggs a year:** University of Kentucky, *Raising ducks*
  ([ASC-198](https://publications.mgcafe.uky.edu/sites/publications.ca.uky.edu/files/ASC198_0.pdf)), Table 1. The game uses the
  middle of each range:
  - Pekin 9-10 / 8-9 lb, 100-180 eggs;
  - Khaki Campbell 4.0-4.5 / 3.5-4.0 lb, 200-300 eggs;
  - Mallard 2.25-2.50 / 1.88-2.25 lb, 20-50 eggs;
  - Muscovy 10-12 / 6-7 lb, 60-120 eggs.
- **Egg weight:** duck eggs are "32-34 ounces per dozen" ([UW-Madison](https://livestock.extension.wisc.edu/?p=958)), about
  75.6-80.3 g, so the game uses 78 g for the farmed ducks. Mallard: NOT FOUND, so 55 g *(proposed)*.
- **Shells:**
  - Pekin white, Khaki Campbell "off-white" (ASC-198).
  - Mallard "creamy white to greenish-buff" ([Wikipedia](https://en.wikipedia.org/wiki/Mallard)).
  - Muscovy white ([Wikipedia](https://en.wikipedia.org/wiki/Muscovy_duck)).
- **Broodiness:**
  - Khaki Campbells "seldom become broody" (ASC-198).
  - Pekin "rarely" is *(proposed)*.
  - Mallard and Muscovy broodiness: NOT FOUND from an extension source. The game calls them broody *(proposed)*.

**Baselines:** trait scores (0-100) per breed are FarmChain-style game targets *(proposed)*, as for cattle and sheep. They are set
in `Breeds`, pinned by a test.

## Laying
| Quantity | Value | Source |
|---|---|---|
| Chicken, first egg | 18-20 weeks; 143 days to 50% production (Leghorn) | [Oregon State PNW-565](https://extension.oregonstate.edu/catalog/pnw-565-why-did-my-chickens-stop-laying); Hy-Line W-36 |
| Chicken, peak | above 90% hen-day at about 35 weeks, lasting about 10 weeks, then a slow decline | PNW-565 |
| Leghorn curve | 95-97% peak; 93% at week 40, 89% at 50, 86% at 60, 82% at 70, 77% at 80, 72% at 90 | Hy-Line W-36 |
| Egg weight with age | 54.7 g at 26 weeks, 58.5 g at 32, 63.3 g at 70 | Hy-Line W-36 |
| Turkey, first egg | 31-32 weeks; laying lasts about 28 weeks; peak about 75% | [The Poultry Site](https://www.thepoultrysite.com/articles/production-cycle-of-turkeys/p4) |
| Coturnix, first egg | 7 weeks; full lay by 50 days | Extension; NSW DPI |
| Bobwhite, first egg | about 22 weeks | APHIS |
| Khaki Campbell, first egg | about 6 months; "an egg a day for ten months" | ASC-198 |
| Pekin | lays "continuously for 7-12 months" | [Cornell duck research lab](https://www.vet.cornell.edu/index%2Ephp/animal-health-diagnostic-center/programs/duck-research-laboratory/duck-housing-and-management) |

**Game rule (`LayingCycle`):**
- **First egg:**
  - chickens: 20 weeks (140 days), except Leghorn at 143 days;
  - turkeys: 31.5 weeks;
  - Coturnix: 7 weeks; Bobwhite: 22 weeks;
  - ducks: 26 weeks (6 months). Pekin, Mallard and Muscovy are NOT FOUND, so they take the Khaki Campbell figure *(proposed)*.
- **Curve shape:** the Hy-Line Leghorn curve, measured in weeks from the first egg, is the shape for every bird *(proposed)*. It is
  scaled so one laying year gives the breed's eggs a year.
- **Turkeys:** they lay for **28 weeks**, then rest until a year from their first egg *(proposed: real turkeys lay in spring;
  Minecraft has no seasons)*.
- **Daily egg:**
  - Each game day a hen adds one real day's rate to an egg counter, and lays an egg when it reaches 1. So a 90% hen lays about
    nine eggs in ten game days.
  - The egg_production trait moves the rate by up to ±20% *(proposed, as wool_yield)*.
- **Egg weight:**
  - It is the breed's figure at the curve's peak and follows the Hy-Line rise with age: about -6% at the first egg, +8% late in lay.
  - The egg_weight trait moves it by up to ±10% *(proposed)*.
- **Retirement:** after 3 laying years *(proposed)* a hen lays no more and is "retired".
- **Underfed or thirsty hens:** an underfed or thirsty day lowers that day's rate by the share not met (the cattle milk rule). A
  hen that is too cold (a chick out of heat) or sick does not lay.

## Mating and fertile eggs
| Quantity | Value | Source |
|---|---|---|
| Hens per rooster | 10-12 ([MSU](https://extension.msstate.edu/publications/choosing-the-right-breed-for-your-backyard-flock)); 6-8 ([Kentucky](https://afs.mgcafe.uky.edu/extension/poultry/breeding)) | |
| Hens per tom | 8-12 | Kentucky |
| Quail | 1 male to 3 females | NSW DPI; APHIS (bobwhite, "maximum 1 cock bird per 3 hens") |
| Ducks | 1 drake to 4-5 ducks (NCSU); 1 to 4-7 (Mallard) | The Poultry Site (NCSU); [Mallard production](https://www.thepoultrysite.com/articles/mallard-duck-production) |
| Fertile eggs after the male is gone | up to 4 weeks (chicken); 3 weeks or longer (turkey) | [MSU](https://extension.msstate.edu/agriculture/livestock/poultry/time-hens-continue-produce-fertile-eggs); The Poultry Site |

**Game rule (`Clutch`):**
- **Who a male serves:** an adult male serves up to **10** hens (chicken, turkey), **3** (quail) or **5** (duck) in his coop or
  pen, the middle of the sources.
- **How long a mating lasts:** a hen mated within the last **28 real days** (chicken) or **21** (turkey, quail, duck; quail and duck
  NOT FOUND, *proposed*) lays fertile eggs.
- **What a fertile egg carries:** the parents' genome, so the chick's genes are set at laying, as a calf's are set at conception.
- **Crosses:** they work only within a species. Muscovy × other ducks gives a sterile "mule" in reality; that cross is **not**
  allowed in this pass *(proposed)*.

## Incubation and hatching
| Species | Days | Temp °F | Stop turning after day | Source |
|---|---|---|---|---|
| Chicken | 21 | 100 (99.5-100.5 forced air) | 18 | [USU 4-H](https://extension.usu.edu/utah4h/research/hatching-and-caring-for-chicks); [VT tip sheet](https://fauquier.ext.vt.edu/content/dam/fauquier_ext_vt_edu/files/embryology/tip-sheet.pdf) |
| Turkey | 28 | 99 | 25 | USU 4-H |
| Duck | 28 | 100 | 25 | [MSU, incubation factors](https://extension.msstate.edu/agriculture/livestock/poultry/important-incubation-factors) |
| Muscovy | 35-37 (35) | 100 | 31 | MSU; "35 days compared to 28 days for other ducks" (ASC-198) |
| Coturnix | 17 | 100 | 15 | MSU |
| Bobwhite | 23-24 (23.5) | 100 | 20 | MSU |

The humidity is raised for the last 3 days (USU: wet bulb 90 °F). Incubators are in `hatchery.md`, broody hens in `coop.md`.

**Game:**
- **The clock:** a fertile egg develops only while it is kept warm by an incubator or a broody hen. It hatches after its species'
  days.
- **Chilling:** an egg that sits cold for over **7 real days** before it starts no longer hatches *(proposed; storage limits for
  hatching eggs were not looked up)*.
- **Pause, no loss:** once started, the clock pauses when the warmth stops and nothing is lost *(proposed simplification, as the
  corral's short day)*.

## Brooding (young birds need heat)
| Species | Start temp | Drop | Heat until | Source |
|---|---|---|---|---|
| Chick | 95 °F | 5 °F a week | about 6 weeks, fully feathered | [NDSU AS2045](https://www.ndsu.edu/agriculture/sites/default/files/2022-04/as2045.pdf) |
| Poult | 95-98 °F for 2 weeks | 5 °F a week to 70 °F | 6-8 weeks (7) | [UMaine 2187](https://extension.umaine.edu/publications/2187e/) |
| Quail (Coturnix) | 35 °C | about 3.5 °C a week | 3-4 weeks (3.5) | NSW DPI |
| Duckling | 90-95 °F | 5 °F a week | 2-3 weeks in summer, 5-6 in cold (4) | [UW-Madison, care of ducks](https://livestock.extension.wisc.edu/articles/care-of-ducks); Extension, meat ducks |

**Game:**
- **Keeping warm:** a young bird is warm if it is in a working brooder (`hatchery.md`) or within **2 blocks** of a broody or
  brooding mother of its species *(proposed)*.
- **A cold day:** it costs **10 health points** *(proposed; the cattle health scale)* and no growth that day.
- **When warmth stops mattering:** after the heat-until age, the bird is a grower and needs no heat.

## Feed and water
| Quantity | Value | Source |
|---|---|---|
| Laying hen | 100-150 g feed a day; Leghorn W-36 99.6 g | [Alabama ACES](https://www.aces.edu/blog/topics/farming/backyard-small-poultry-flock-management-series-feeding-the-laying-hen/); Hy-Line |
| Coturnix | 14-18 g a day | NSW DPI |
| Breeder duck | 48 lb per 100 ducks a day, about 218 g | [The Poultry Site, breeder ducks](https://www.thepoultrysite.com/articles/management-programme-for-raising-breeder-duck-flocks) |
| Adult turkey | NOT FOUND (only totals to 26 weeks) | |
| Water, chicken and turkey | about 2× the feed weight (UAF gives 2.5× for turkeys) | ACES; [The Poultry Site](https://www.thepoultrysite.com/articles/turkey-feeding-and-nutrition/p2); UAF |
| Water, ducks | 3.3:1 summer, 2.7:1 winter | [The Poultry Site, Farrell review](https://thepoultrysite.com/articles/recent-advances-in-the-production-management-and-nutrition-of-intensivelyfarmed-domestic-ducks) |

**Game:**
- **Feed** is fed as-is (not as dry matter).
- **Each bird's daily feed** scales from its species anchor by body weight to the power 0.75 *(proposed: the usual metabolic
  scaling; the exponent is a game choice)*:
  - chicken: 100 g at 2.0 kg (the Leghorn);
  - quail: 16 g at 0.14 kg;
  - duck: 218 g at 3.9 kg (Pekin);
  - turkey: the chicken anchor *(proposed)*, which gives about 290 g for an 8.2 kg hen.
- **Young birds** eat by their weight on the same rule.
- **Water:** 2× the feed weight (chicken, turkey, quail; the quail figure is NOT FOUND, so it is *proposed*), and 3× for ducks
  (the middle of 2.7-3.3).
- **Grain:** one wheat or seed item is **100 g of feed** *(proposed)*. So a hen eats about one item each game day.
  - Wheat seeds, beetroot seeds, melon seeds, pumpkin seeds, torchflower seeds, pitcher pods and wheat all count. Wheat is the vanilla
    chicken lure, and the seeds are its food.
  - Real layers need a balanced ration; that is not in this pass.
- **Underfeeding:** less growth and fewer eggs, lower health. No bird dies of a missed day (cattle rule).

## Housing
| Quantity | Value | Source |
|---|---|---|
| Hen, indoors | 3 sq ft (0.28 m²); 1 nest opening per 4 hens; 1 ft of roost per bird | [UNH, producing your own eggs](https://extension.unh.edu/resource/producing-your-own-eggs) |
| Hen, run | 8 sq ft (0.74 m²) laying hens, 10 large chickens | [Penn State, small-scale poultry housing](https://extension.psu.edu/small-scale-poultry-housing) (**TO VERIFY**: read from a search summary) |
| Turkey | 6 sq ft indoors (0.56 m²), 20 sq ft yard (1.86 m²) after 2 months | [UNH, raising turkeys](https://extension.unh.edu/resource/raising-turkeys) |
| Coturnix | 145 cm² floor per bird | NSW DPI |
| Bobwhite | 1 sq ft floor (breeders), 2 sq ft flight pen | [Penn State, bobwhite](https://extension.psu.edu/bobwhite-quail-production) |
| Duck | 5 sq ft indoors (0.46 m²); run: over 15 sq ft (1.4 m²) | [Extension, space allowances](https://poultry.extension.org/articles/getting-started-with-small-and-backyard-poultry/housing-for-small-and-backyard-poultry-flocks/space-allowances-in-housing-for-small-and-backyard-poultry-flocks/); [VT 2902-1092](https://pubs.ext.vt.edu/2902/2902-1092.html) (**TO VERIFY**) |
| Duck water | "swimming water … is not essential"; water at least 10 cm deep matters to ducks | UW-Madison, care of ducks; RSPCA duck report (**TO VERIFY**: search summary) |

Game capacities, one block = 1 m², all rounded down so a full coop is never over the source:

| Species | Coop floor blocks per bird | Run blocks per bird | Birds per nest box | Birds per perch block |
|---|---|---|---|---|
| Chicken | 1 block holds 3 | 1 | 4 | 3 |
| Turkey | 1 holds 1 | 2 | 4 *(proposed)* | 1 *(proposed)* |
| Quail | 1 holds 16 *(proposed cap; the source allows 68)* | 1 holds 8 | 8 *(proposed)* | none (ground birds) |
| Duck | 1 holds 2 | 2 | 4 *(proposed)* | none (ducks don't perch) |

## Wild fowl (docs/taming.md)
**Vanilla:** every vanilla chicken spawn entry in the 26.3 biome data is weight 10, groups of 4, in 25 biomes. Each is replaced by
a wild-fowl entry with the same weight and group size. The species and breed come from the biome group:

| Biome group | Vanilla biomes | Wild fowl | Habitat source |
|---|---|---|---|
| Jungle | jungle, sparse_jungle, bamboo_jungle | chicken: Silkie | red junglefowl, the chicken's wild ancestor, lives from India to SE Asia and "prefer[s] disturbed habitats and edges" ([Wikipedia](https://en.wikipedia.org/wiki/Red_junglefowl)) |
| Forest | forest, flower_forest, birch_forest, old_growth_birch_forest, dark_forest, dappled_forest | turkey: Bronze, Bourbon Red | wild turkey: "hardwood and mixed conifer-hardwood forests with scattered openings" ([Wikipedia](https://en.wikipedia.org/wiki/Wild_turkey)) |
| Taiga | taiga, snowy_taiga, old_growth_pine_taiga, old_growth_spruce_taiga | turkey: Bronze | same (mixed conifer forests) |
| Plains | plains, sunflower_plains | quail: Bobwhite; chicken: Rhode Island Red, Plymouth Rock | bobwhite: "agricultural fields, grassland" ([Wikipedia](https://en.wikipedia.org/wiki/Northern_bobwhite)) |
| Savanna | savanna, savanna_plateau, windswept_savanna | quail: Coturnix | Japanese quail: steppes and meadows ([Wikipedia](https://en.wikipedia.org/wiki/Japanese_quail)) |
| Dry | badlands, eroded_badlands, wooded_badlands | chicken: Leghorn | Leghorn is Mediterranean |
| Hills | windswept_hills, windswept_gravelly_hills, windswept_forest | chicken: Plymouth Rock; turkey: Bourbon Red | |
| Swamp | swamp | duck: Mallard, Muscovy | Muscovy: "forested swamps, lakes and streams" ([Wikipedia](https://en.wikipedia.org/wiki/Muscovy_duck)) |

**Added entries (no vanilla chicken there before):** river: Mallard, Pekin and Khaki Campbell; mangrove_swamp: Muscovy. Each is
weight 10, groups of 4, like the chicken entry.

**Mapping rules:**
- The biome-to-fowl mapping is a game choice *(proposed)*; the habitats above come from the cited pages.
- Within a group, each listed species and breed is equally likely.
- A spawn group shares one breed, and has one male.

**Taming and spawning:**
- **Breed table:** the jungle chicken is a Silkie because it is the only Asian chicken breed in the list. Broad Breasted White and
  Coturnix stay farm-only, except that Coturnix spawns wild in savanna. Broad Breasted White never spawns wild: it can't mate, so it
  comes only from a hatchery *(proposed: a starter BBW pair is a summon or spawn egg, as for creative)*.
- **Spawn rule:**
  - chickens, turkeys and quail: the vanilla animal rule (grass block, light 9 or more);
  - ducks: grass or sand within 4 blocks of water *(proposed)*, so river banks work.
- **Wild behaviour:**
  - Wild fowl follow a player holding grain (the items above).
  - They keep **6 blocks** from empty hands, like cattle and sheep.
  - They don't breed or lay fertile eggs in the wild *(proposed: wild hens lay no eggs at all, so wild numbers stay what the spawn
    cycle makes)*.
- **Taming:** the corral's bait trough takes grain as well as hay. Each wild animal eats only its own feed.
  - A bird inside a formed corral that is fed and watered settles for **7 real days** *(proposed for birds: the cattle figure)*.
    That is about 2.3 game days at 1:3.
- **Vanilla leftovers:** vanilla chickens already in a world stay. A thrown vanilla egg still hatches a vanilla chicken (vanilla
  eggs only come from old chickens and loot).

## Movement and the model
- **Movement:**
  - Chickens, quail and turkeys flap to fall slowly, as vanilla chickens do. None of them can clear a fence.
  - Ducks swim and float, and paddle in water (they are the pond birds).
- **Model:** one bird model, with proportions per species:
  - chicken: comb and wattles;
  - turkey: the tom's fan tail, snood and beard;
  - quail: a small round body;
  - duck: bill, flat feet and an upright Pekin stance for Pekin;
  - the Silkie's fluffy crest and the Muscovy's red face caruncles.
- **Babies** are down-covered, with a coat per species.

## Plumage and shell genes (`PoultryLocus`, drafted in the genetics part)
Every breed has a fixed founder look. The loci below give crosses a believable mix.

| Species | Locus | Alleles (dominance) |
|---|---|---|
| Chicken | Dominant white (PMEL17) | I > i (Leghorn is I/I) |
| Chicken | Extension (MC1R) | E (black) > e^Wh (wheaten) > e+ (wild type) |
| Chicken | Barring | B > b, sex-linked (Z): Plymouth Rock is barred |
| Chicken | Silkie feathering | h recessive (h/h is silky) |
| Chicken | Recessive white | C > c (Silkie white is c/c) |
| Chicken | Comb | rose R and pea P dominant over single |
| Turkey | Black / bronze | B (black) > b+ (bronze) |
| Turkey | Red | r recessive (Bourbon Red is r/r with buff) |
| Turkey | Recessive white | C > c (Broad Breasted White is c/c) |
| Quail | Wild-type and pattern | Coturnix and Bobwhite wild type; colour mutants not in this pass |
| Duck | Extended black / restricted (Mallard) pattern | M^R > M+ > m^d |
| Duck | Recessive white | C > c (Pekin is c/c) |
| Duck | Brown (dusky) | sex-linked d (Khaki Campbell) |
| Duck | Muscovy | its own species: black with white wing patch; colour loci not in this pass |
| All | Shell colour | brown is polygenic: a 0-100 shade score per breed, inherited as the mid-parent value plus noise *(proposed)* |

**Proposed until sourced:** these dominance orders are the usual poultry-genetics loci, but no primary source is cited yet, so
the code (`genetics/PoultryLocus`) marks them *(proposed)*. They are simplified:
- one "red" extension allele stands for the Rhode Island Red's set;
- Muscovy and quail colour mutants are left out.

Sex-linked loci (barring, duck brown) follow the bird rule: a hen has one Z, from her sire.

## Meat, feathers, down
| Quantity | Value | Source |
|---|---|---|
| Heritage heavy chicken | 68% dressing | [UW-Madison, bird breakdown](https://livestock.extension.wisc.edu/articles/bird-breakdown-exploring-yields-and-cuts-of-poultry/) |
| Turkey | 80.5% | same |
| Pekin | 66-69% (7 weeks) | [UMaine 2223](https://extension.umaine.edu/publications/2223e/) |
| Muscovy | 69-75% | same |
| Quail | 58-62% | same |
| Ducks, general | about 65% | UW-Madison |
| Duck feathers and down | about 100 g of feathers a bird, about 30 g of it down | Farrell review (**TO VERIFY**: the review cites no source) |

**Game:** whole birds, cuts and feathers are in `docs/eggs.md` (products). Meat items are **one item per 0.5 kg** of dressed bird
*(proposed; sheep use 2.5 kg)*.

## Manure
As excreted (MWPS-18, ±30%, [PDF](https://dcr.am.virginia.gov/soil-and-water/document/manure-characteristics-mwps-18-1.pdf)):

| Bird | lb a day | kg a day |
|---|---|---|
| Layer, 3 lb | 0.15 | 0.068 |
| Broiler, 2 lb | 0.19 | 0.086 |
| Turkey hen, 10 lb | 0.47 | 0.21 |
| Turkey tom, 20 lb | 0.74 | 0.34 |

**Game:**
- **How much:** each bird adds litter to its coop at the layer rate per kg of body weight (0.068 kg / 1.36 kg = 0.05 kg per kg a
  day). Quail and ducks have no figure (NOT FOUND), so they use the same rate *(proposed)*.
- **What happens to it:** litter goes into the existing compost chain (`docs/waste.md`).

## Economy
Nothing is sold (cattle rule, 2026-11-07).

## Open points
- Seasons, if ever added: turkeys and wild birds lay in spring.
- Better sources for the TO VERIFY rows; the turkey's daily feed; egg weights for Silkie and Mallard.
- Poultry artificial insemination, for Broad Breasted White.
- Geese and guinea fowl.

## Built, 2026-11-11
- **Rules and genes:**
  - `husbandry/poultry` (FowlBreed, Fowl, LayingCycle, Clutch, EggGrading) and `husbandry/TimeScale` (`of(species)`).
  - Genetics in `Species`, `Trait`, `Breeds`, `PoultryLocus`, `PoultryGenes` and `PoultryLook`.
- **The bird:** `entity/FowlEntity` (one class for all four species), `entity/FowlEggs`, and its model and renderer in `client/Fowl*`.
- **Buildings:** the coop (`coop.md`), the hatchery (`hatchery.md`) and the products (`docs/eggs.md`).
- **Where the code differs from this spec:**
  - Mating is with the nearest male within 8 blocks. The hens-per-male figures are not enforced yet.
  - Crossing the two quail species is allowed; in reality Coturnix and Bobwhite don't cross.
  - A hen's body weight grows in a straight line; no growth curve was found.

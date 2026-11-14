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
- **Sex ratio:**
  - Chickens, turkeys and quail hatch 50/50 male and female. In commercial Leghorns the "sex ratio was balanced"
    ([Klein & Grossmann 2008](https://europepmc.org/article/MED/18030680)).
  - Ducks hatch 50/50 too (Tim, 2026-11-12). One study finds "an average bias of 51%-52%" toward males
    ([Narushin 2026](https://kar.kent.ac.uk/115666/), abstract), which the game does not use.
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
| Duck | Mallard | MAL | wild, temperate northern hemisphere | wild, game | 1.1 / 0.94 | 20-50 (35) | 54 | creamy white to greenish-buff | broody |
| Duck | Muscovy | MUS | Central and South America | meat | 5.0 / 2.95 | 60-120 (90) | 78 | white | broody |

Values in brackets are what the game uses (the middle of the range, the sheep rule).

**Chickens:**
- **Weights:** UW-Madison, *Chicken breeds and varieties*
  ([A2880](https://livestock.extension.wisc.edu/articles/chicken-breeds-varieties-a2880)): Leghorn "Cock—6 pounds, hen—4½",
  Rhode Island Red 8½ / 6½, Plymouth Rock 9½ / 7½. Silkie: the European standard 1.4-1.7 / 1.1-1.4 kg
  ([Wikipedia](https://en.wikipedia.org/wiki/Silkie_(chicken_breed)), citing the BDRG standard; the article also gives a smaller US
  figure). A measured flock: "1.6 kg for Sultan and 1.4 kg for Silkie" hens (Tyc et al. 2023, Pol J Vet Sci 26(3),
  [PDF](https://www.czasopisma.pan.pl//Content/128544/PDF/11%20_%20Tyc.pdf)). Poultry Hub gives cock "1.8 Kg" and hen "1.4 Kg", with no standard named
  ([Poultry Hub, Silkie](https://www.poultryhub.org/all-about-poultry/species/fancy-chicken-breeds/silkie)). The game uses a
  **1.55 kg** cock (the European middle) and a **1.4 kg** hen (was 1.25; Tyc et al.), and 100 eggs a year.
- **Eggs a year:**
  - Leghorn, Rhode Island Red and Plymouth Rock from the Livestock Conservancy breed pages
    ([Leghorn](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/leghorn-chicken/),
    [Rhode Island Red](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/rhode-island-red-chicken/),
    [Plymouth Rock](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/plymouth-rock-chicken/)).
  - Silkie: "A Silkie hen can produce 100 eggs in an ideal year" (Wikipedia, **TO VERIFY**; searched for a primary laying figure
    again 2026-11-14, the pages did not load). Poultry Hub says "approximately
    120 eggs per year" (same page as above, unsourced).
- **Egg weight:**
  - Leghorn: 58.5 g at 32 weeks, from the Hy-Line W-36 standard
    ([PDF](https://www.hyline.com/filesimages/Hy-Line-Products/Hy-Line-Product-PDFs/W-36/36%20STD%20ENG.pdf); W-36 is a
    commercial Leghorn strain).
  - Rhode Island Red and Plymouth Rock are "Large" (Livestock Conservancy). USDA Large is 24-27 oz a dozen, about 56.7-63.8 g an
    egg. The game uses 60 g *(proposed, inside the class)*.
  - Silkie: **41 g** (was 40, proposed): "Egg weight [g] … Silkie … 41.15 ±0.97" (Tyc et al. 2023, Table 2, above).
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
- **Bourbon Red broodiness:** NOT FOUND (searched again 2026-11-14; the pages did not load), so the game gives it the heritage Bronze's "good mothers" *(proposed)*.

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
  - Weight: 170-173 g for northern birds ([Wikipedia](https://en.wikipedia.org/wiki/Northern_bobwhite), citing Nelson & Martin 1953).
  - Broodiness: NOT FOUND for a farmed line (searched again 2026-11-14; the pages did not load). The game calls it broody *(proposed: a wild-type bird)*.

**Ducks:**
- **Weights and eggs a year:** University of Kentucky, *Raising ducks*
  ([ASC-198](https://publications.mgcafe.uky.edu/sites/publications.ca.uky.edu/files/ASC198_0.pdf)), Table 1. The game uses the
  middle of each range:
  - Pekin 9-10 / 8-9 lb, 100-180 eggs;
  - Khaki Campbell 4.0-4.5 / 3.5-4.0 lb, 200-300 eggs;
  - Mallard 2.25-2.50 / 1.88-2.25 lb, 20-50 eggs;
  - Muscovy 10-12 / 6-7 lb, 60-120 eggs.
- **Egg weight:** duck eggs are "32-34 ounces per dozen" ([UW-Madison](https://livestock.extension.wisc.edu/?p=958)), about
  75.6-80.3 g, so the game uses 78 g for the farmed ducks. Mallard: 54 g (was 55), egg mass "54g"
  ([BTO BirdFacts, Mallard](https://www.bto.org/understanding-birds/birdfacts/mallard)).
- **Shells:**
  - Pekin white, Khaki Campbell "off-white" (ASC-198).
  - Mallard "creamy white to greenish-buff" ([Wikipedia](https://en.wikipedia.org/wiki/Mallard)).
  - Muscovy white ([Wikipedia](https://en.wikipedia.org/wiki/Muscovy_duck)).
- **Broodiness:**
  - Khaki Campbells "seldom become broody" (ASC-198).
  - Pekins "are poor setters and seldom raise a brood"; Muscovies "make good setters" (Minnesota Extension, *Raising ducks*
    WW-01189, [archived](https://webarchive.library.unt.edu/eot2008/20090115161252/http://www.extension.umn.edu/distribution/livestocksystems/DI1189.html)).
  - Mallard broodiness: NOT FOUND from an extension source (searched again 2026-11-14; the pages did not load). The game calls it broody *(proposed)*.

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
  - ducks: 26 weeks (6 months). Pekin, Mallard and Muscovy are NOT FOUND (searched again 2026-11-14; the pages did not load), so they take the Khaki Campbell figure *(proposed)*.
    Pekin: **TO VERIFY**: snippet, "Pekins start laying eggs when they are about 26–28 weeks of age"
    ([NSW DPI, egg production](https://www.dpi.nsw.gov.au/animals-and-livestock/poultry-and-birds/species/duck-raising/egg-production); the page refused the fetch, again on 2026-11-14).
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
- **How long a mating lasts:** a hen mated within the last **28 real days** (chicken), **21** (turkey, duck) or **11** (quail) lays
  fertile eggs.
  - Quail: 11 days (was 21). Japanese quail "can store sperm for up to 11 days"
    ([Adkins-Regan 2015, PLoS ONE](https://pmc.ncbi.nlm.nih.gov/articles/PMC4519265/)).
  - Duck: 21 days is *proposed*. **TO VERIFY**: snippet, isolated Mallard hens' "fertility persisted for 6 to 14 days" (Elder &
    Weller 1954, [J. Wildl. Manage. 18:495](https://doi.org/10.2307/3797084); the abstract refused the fetch, 403 again on 2026-11-14;
    other snippets give "fertility lasted 6-14 days"). The game keeps 21.
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
- **Chilling:** an egg that sits cold for over **7 real days** before it starts no longer hatches. Hatchability "remains high" for
  eggs stored "fewer than 7–10 days", and "After 3 weeks of storage, the hatchability is near 0 percent"
  ([MSU P1182](https://extension.msstate.edu/publications/hatching-quality-chicks)). The game uses the low end, 7.
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
| Adult turkey | NOT FOUND (only totals to 26 weeks). **TO VERIFY**: snippet, breeder hens ate "252 grams" a day (Broad Breasted Bronze) and "137 grams" (Beltsville Small White) | [MSU thesis, 1963](https://d.lib.msu.edu/etd/41098) (the repository refused the fetch, again on 2026-11-14) |
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
- **Water:** 2× the feed weight (chicken, turkey), **2.3×** for quail (was 2×, proposed), and 3× for ducks (the middle of 2.7-3.3).
  - Quail: water "59.67 ± 7.17 ml/bird/day" against feed "26.32 ± 0.05 g/bird/day", so about 2.3× (Arch Razi Inst 2024,
    [PMC11345484](https://pmc.ncbi.nlm.nih.gov/articles/PMC11345484/)).
  - Quail, a second figure: **TO VERIFY**: snippet, laying Japanese quail drank "73.67 g" to "129.45 g" a bird a day (Penkov 2010,
    [Agricultural Sciences, Plovdiv](https://agrarninauki.au-plovdiv.bg/2010/8-investigation-on-the-water-consumption-by-laying-japanese-quails-coturnix-coturnix-japonica/);
    the page gave a certificate error on 2026-11-14).
    That is far above 2× 16 g.
- **Grain:** one wheat or seed item is **100 g of feed** *(proposed)*. So a hen eats about one item each game day.
  - Wheat seeds, beetroot seeds, melon seeds, pumpkin seeds, torchflower seeds, pitcher pods and wheat all count. Wheat is the vanilla
    chicken lure, and the seeds are its food.
  - Real layers need a balanced ration; that is not in this pass.
- **Underfeeding:** less growth and fewer eggs, lower health. No bird dies of a missed day (cattle rule).

## Housing
| Quantity | Value | Source |
|---|---|---|
| Hen, indoors | 3 sq ft (0.28 m²); 1 nest opening per 4 hens; 1 ft of roost per bird | [UNH, producing your own eggs](https://extension.unh.edu/resource/producing-your-own-eggs) |
| Hen, run | 8 sq ft (0.74 m²) laying hens, 10 large chickens | [Penn State, small-scale poultry housing](https://extension.psu.edu/small-scale-poultry-housing), Table 1 "Sq ft/bird outside runs" |
| Turkey | 6 sq ft indoors (0.56 m²), 20 sq ft yard (1.86 m²) after 2 months | [UNH, raising turkeys](https://extension.unh.edu/resource/raising-turkeys) |
| Coturnix | 145 cm² floor per bird | NSW DPI |
| Bobwhite | 1 sq ft floor (breeders), 2 sq ft flight pen | [Penn State, bobwhite](https://extension.psu.edu/bobwhite-quail-production) |
| Duck | 5 sq ft indoors (0.46 m²); run: over 15 sq ft (1.4 m²) | [Extension, space allowances](https://poultry.extension.org/articles/getting-started-with-small-and-backyard-poultry/housing-for-small-and-backyard-poultry-flocks/space-allowances-in-housing-for-small-and-backyard-poultry-flocks/); [VT 2902-1092](https://pubs.ext.vt.edu/2902/2902-1092/2902-1092.html), Table 1: ducks "5" inside, "&gt;15" outside |
| Duck water | "swimming water … is not essential"; water at least 10 cm deep matters to ducks | UW-Madison, care of ducks; RSPCA Australia: "shallow water (10-20 cm) where they can dabble and submerge their heads" ([RSPCA KB](https://kb.rspca.org.au/knowledge-base/how-should-i-care-for-my-pet-ducks/)) |
| Nests | "at least one nest for every 4-5 females" | VT 2902-1092 |

Game capacities, one block = 1 m², all rounded down so a full coop is never over the source:

| Species | Coop floor blocks per bird | Run blocks per bird | Birds per nest box | Birds per perch block |
|---|---|---|---|---|
| Chicken | 1 block holds 3 | 1 | 4 | 3 |
| Turkey | 1 holds 1 | 2 | 4 (VT nests) | 1 *(proposed)* |
| Quail | 1 holds 16 *(proposed cap; the source allows 68)* | 1 holds 8 | 8 *(proposed)* | none (ground birds) |
| Duck | 1 holds 2 | 2 | 4 (VT nests) | none (ducks don't perch) |

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
| Duck feathers and down | about 100 g of feathers a bird, about 30 g of it down | Farrell review (confirmed 2026-11-14; still secondary: the review cites no source) |

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
| Duck, 4 lb | 0.44 | 0.20 |

**Game:**
- **How much:** each bird adds litter to its coop at the layer rate per kg of body weight (0.068 kg / 1.36 kg = 0.05 kg per kg a
  day).
  - Ducks use their own row: 0.20 kg / 1.81 kg = **0.11 kg per kg a day** (was the layer rate, 0.05). MWPS-18 Table 6 lists
    "Duck 4 0.44" (lb, lb a day).
  - Quail have no figure (NOT FOUND; searched again 2026-11-14; the pages did not load), so they use the layer rate *(proposed)*.
- **What happens to it:** litter goes into the existing compost chain (`docs/waste.md`).

## Economy
Nothing is sold (cattle rule, 2026-11-07).

## Open points
- Seasons, if ever added: turkeys and wild birds lay in spring.
- Better sources for the TO VERIFY rows; the turkey's daily feed.
- Poultry artificial insemination, for Broad Breasted White.
- Geese and guinea fowl: specced 2026-11-13 (below).

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

## Loose ends, 2026-11-11 (spec)
### Hens per male (now enforced)
A male fertilises at most this many hens at once. A hen counts against him while her eggs are fertile from his service (`Clutch`); she
frees his place when her fertile window ends.

| Species | Hens per male | Source |
|---|---|---|
| Chicken | 10 | "a ratio of about 1:10, m:f, if breeding" ([UC ANR](https://ucanr.edu/site/small-acreage-landowners/poultry)) |
| Quail | 3 | "one male to three females is sufficient" (NSW DPI, [The Poultry Site](https://www.thepoultrysite.com/articles/raising-japanese-quail-1)) |
| Duck | 5 | 1 male to 5-8 females for meat-type ducks ([Narushin 2026](https://kar.kent.ac.uk/115666/), quoting "Poultry 2024"; still secondary) |
| Turkey | 10 | the middle of "varies from 8-12" ([Kentucky](https://afs.mgcafe.uky.edu/extension/poultry/breeding)); UC ANR adds that most domestic breeds "can no longer breed naturally" |

### The two quail don't cross
Coturnix (Old World quail, Phasianidae) and Bobwhite (New World quail, Odontophoridae) are in different families. No record of a
hybrid was found (NOT FOUND: searched Coturnix x Colinus hybrid, again 2026-11-14). Even within *Colinus*, a crested x northern bobwhite cross was
"nearly 70%" infertile (**TO VERIFY**: snippet, [Auk 92](https://sora.unm.edu/sites/default/files/journals/auk/v092n04/p0803-p0805.pdf); the PDF did not load on 2026-11-14).
- **Game rule:** a quail male serves only hens of his own genus, judged by the top breed of each bird. A mixed bird counts as the genus
  of its larger share.

### Artificial insemination for turkeys
- **Collecting:** an empty straw used on any adult tom collects semen ("abdominal massage", [TNAU](https://agritech.tnau.ac.in/animal_husbandry/ani_chik_ai.html)).
  A Broad Breasted White tom can't mate, but semen can be collected from him; that is how the breed is kept.
- **Fresh only:** fresh turkey semen is used "within 30 minutes after collection" (TNAU; also Brillard 2003, cited in
  [AAJBB](https://banglajol.info/index.php/AAJBB/article/download/59384/40978)).
  - Game: a poultry straw spoils **30 real minutes** after collection, about **8 seconds** of play at 3:1. It turns back into an empty
    straw.
- **Inseminating:** a filled turkey straw used on a turkey hen inseminates her, as if a tom of that genome had served her. Her eggs are
  fertile for the same window as after mating (`Fowl.fertileDays`).
  - Turkey hens can store sperm for 10-15 weeks ([PMC3944358](https://pmc.ncbi.nlm.nih.gov/articles/PMC3944358/)), but farms
    inseminate weekly to keep fertility up ([Frontiers 2023](https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2023.1275922/full)).
    The game keeps the shorter fertile window for both.
- **Who it works on:** the straw is for turkeys only. Chicken, duck and quail AI are not in this pass.

### Foraging on the run
- **Share:** a bird outdoors on a grass run gets **20%** of its daily feed from forage and insects.
  - "pastured poultry eat 5-20% (of their diet) from pasture" (Mattocks 2002, in [NCAT/ATTRA IP453](https://www.ncat.org/wp-content/uploads/2025/12/IP453-pasturedpoultrynutrition.pdf)).
  - Layers reach "20% of the diet", about "25g dry matter per hen and day" ([Agrarforschung Schweiz 2024](https://www.agrarforschungschweiz.ch/en/2024/05/roughage-components-in-nutrition-of-layers-does-it-make-sense-for-dual-purpose-poultry/)).
  - Game: the upper end, 20%, for every species *(proposed: broilers get only about 3% of their energy from forage, ATTRA, and the
    game has no broiler line)*.
- **Ration:** that 20% counts toward the bird's ration average as pasture (`Feedstuff.PASTURE`). The grain feeder supplies the other
  80%.
- **Grass wears slowly:**
  - Each grass block on the run holds 1.5 kg DM of forage, as for sheep.
  - It regrows 0.31 g DM a day. That is what lets a run carry 50 hens an acre for good: 50 x 25 g / 4,047 m² *(derived)*.
    The 50 is *(derived: half Dryden's 100, for a run grazed all year without rest)*. Dryden 1916, p. 192, gives "1. Maximum number of
    fowls per acre : 100 laying hens", but only if night droppings go on other land, the ground is split into two yards cropped in
    turn, and it rests at least 6 months a year ([Dryden, *Poultry Breeding and Management*](https://distantreader.org/stacks/trust/uc2/uc2_ark+=13960=t4dn41478.txt)).
  - Birds eat from the run's blocks evenly. A block drawn down to nothing turns to dirt, and vanilla grass can spread back onto it.
  - Example: 10 hens on an 8 x 6 run draw 235 g a day more than it regrows, so the run starts to go bare after about 300 game days.

### Colour mutants (Muscovy and Coturnix)
The loci follow breeders' genetics guides and OMIA. The dominance orders are **TO VERIFY** except where a peer-reviewed or OMIA source
is given.

| Species | Locus | Alleles and what shows | Source |
|---|---|---|---|
| Muscovy | blue | + / Bl, incomplete: one Bl is blue, two are silver (splash) | **TO VERIFY**: breeder snippet ([waterfowl.org.uk](https://www.waterfowl.org.uk/domestic-waterfowl/muscovy-ducks/); searched again 2026-11-14; the pages did not load) |
| Muscovy | lavender | + / lav, autosomal recessive | [OMIA 002249](https://omia.org/OMIA002249/8855/) (Hollander & Walther 1962) |
| Muscovy | chocolate | + / choc, sex-linked recessive | **TO VERIFY**: breeder snippet (searched again 2026-11-14; the pages did not load) |
| Coturnix | fawn (Y, ASIP) | + / Y, incomplete: one is Italian, two are Golden | [Southwest Gamebirds](https://southwestgamebirds.com/pages/genetics); order Y*F2 &gt; Y*Y &gt; Y*N: "the dominance relationship was Y*F2 &gt; Y*Y &gt; Y*N &gt; Y*RB" (Hiragaki et al. 2008, Genetics, [PMC2248368](https://pmc.ncbi.nlm.nih.gov/articles/PMC2248368)) |
| Coturnix | extended brown (E, MC1R) | + / E, incomplete: one is Rosetta, two are Tibetan | Southwest Gamebirds |
| Coturnix | dotted white (S, EDNRB2) | + / s, recessive: s/s is English White; carriers can show Tuxedo *(not drawn)* | Southwest Gamebirds; EDNRB2 recessive (Miwa et al. 2007, [PubMed 17313575](https://pubmed.ncbi.nlm.nih.gov/17313575/); [OMIA](https://omia.org/OMIA000375/93934/): "Autosomal recessive") |

- **Founders:** wild founders carry the wild type. Herd founders carry each mutant at **5%** *(proposed)*, so mutants turn up in
  breeding.
- **Drawing:** a bird with a mutant colour gets that colour's texture.

### Built, 2026-11-11 (loose ends)
- **Hens per male:** enforced. A male's hens are counted within 32 blocks *(proposed: his coop and run)*.
- **Quail:** the genus check is in `mate` and in natural service.
- **Foraging:** on the run, with the grass wearing down.
- **Colour mutants:** six new loci, appended after the shell locus so older birds load with the wild type. There are nine new
  textures.
- **Game tests:** male_serves_limited_hens, quail_species_dont_cross, hens_forage_on_run, muscovy_colour_mutant_inherits.
- **Unit tests:** PoultryMutantTest, and the run sums in FowlTest.
- **Turkey straws:** come with the straws part.

### Sourcing pass, 2026-11-12
- Resolved 13 marks: hatch sex ratio, Mallard egg, Pekin and Muscovy broodiness, quail fertile days, hatching-egg storage, Penn State
  and VT housing, duck water depth, turkey and duck nest boxes, duck manure, hens per tom. Four now carry a TO VERIFY snippet: Pekin
  first egg, duck fertile days, turkey feed, quail water.
- Still unsourced: Silkie egg weight and weights (Poultry Hub differs from Wikipedia), Bourbon Red and Mallard broodiness, Bobwhite
  weight, quail manure, Muscovy first egg. Searched extension, BTO, Livestock Conservancy and Cornell pages; several refused the fetch.

## Geese and guinea fowl, 2026-11-13 (spec; Tim's answers of the same day)
Tim's choices: geese **Embden, Toulouse, Chinese**; geese live **mostly on grass** on a grassy run, with grain to top up; geese give
**down** and are **guard geese**; guinea fowl in **pearl, lavender and white** by genes, with an **alarm call**. Both run on the poultry
scale (3:1). Not in this pass: guinea fowl eating ticks, roosting in trees, goose fat.

Sources used throughout:
- **FAO** *Goose Production* (Buckland & Guy, 2002): [ch. 1 breeds](https://www.fao.org/4/y4359e/y4359e03.htm),
  [ch. 2 behaviour](https://www.fao.org/4/y4359e/y4359e04.htm), [ch. 6 breeders](https://www.fao.org/4/y4359e/y4359e08.htm),
  [ch. 8 incubation](https://www.fao.org/4/y4359e/y4359e0a.htm), [ch. 9 meat](https://www.fao.org/4/y4359e/y4359e0b.htm),
  [ch. 10 down](https://www.fao.org/4/y4359e/y4359e0c.htm), [ch. 13 killing](https://www.fao.org/4/y4359e/y4359e0f.htm).
- **MSU E-1050** ([archived 1977 bulletin](https://archive.lib.msu.edu/DMC/extension_publications/e1050/e1050_77.pdf); it is marked
  "do not use for current recommendations"; its goose weights are confirmed from the scan, 2026-11-14).
- **TLC**: Livestock Conservancy pages for [Toulouse](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/toulouse-goose/) and
  [Chinese](https://livestockconservancy.org/heritage-breeds/heritage-breeds-list/chinese-goose/).
- **UKY ASC-209** [Raising guinea fowl](https://publications.mgcafe.uky.edu/sites/publications.ca.uky.edu/files/ASC209.pdf); **EXT-GF**
  [extension.org, raising guinea fowl](https://poultry.extension.org/articles/poultry-management/raising-guinea-fowl/).

### Breeds
| Species | Breed | Code | Use | Male / female kg | Eggs a year | Egg g | Shell | Broody |
|---|---|---|---|---|---|---|---|---|
| Goose | Embden | EMB | meat | 11.8 / 9.1 | 40 | 170 | white | yes *(proposed)* |
| Goose | Toulouse | TOU | meat | 11.8 / 9.1 | 20-35 (27.5) | 170 *(proposed)* | white | yes *(proposed)* |
| Goose | Chinese | CHI | eggs, guard | 5.4 / 4.5 | 50-60 (55) | 120 | white | yes *(proposed)* |
| Guinea fowl | Helmeted | GUI | meat, eggs, alarm | 1.5 / 1.5 | 100 | 40 | brown, speckled *(proposed)* | late in the season |

- **Weights:** MSU's table, "Toulouse 26 20 … Embden 26 20 … Chinese 12 10" lb (MSU E-1050, 1977); TLC agrees for Toulouse
  ("Males – 20-26 lbs. / Females – 16-20 lbs.") and Chinese ("Mature ganders average 12 lbs. while mature geese average 10 lbs.").
  FAO gives lighter Embden ("up to 10.0 kg … up to 9.0 kg") and Chinese ("5.0 kg and females 4.0 kg"). Guinea fowl: "Both male and
  female adult guineas weigh 3.0 to 3.5 lb (~1.5 kg)" (UKY).
- **Eggs:** Embden "40 eggs per year", "egg size of 170 g" (FAO); Toulouse "moderate egg-layers of 20-35 eggs annually" (TLC); Chinese
  "50-60 eggs is the average", "Egg weight, at 120 g" (FAO). The Toulouse egg weight is NOT FOUND (TLC says only "extra large"; searched again 2026-11-14; the pages did not load), so it
  takes the Embden's. Guinea: "Hens can lay more than 100 eggs per year"; "about 1.4 oz (40 g)" (UKY).
- **Broody:** the geese's broodiness is NOT FOUND in these sources (searched again 2026-11-14; the pages did not load); all three sit *(proposed)*. Guinea hens "do not go broody until the
  nest has around thirty eggs" (UKY): a guinea hen turns broody only with 30 eggs in her nest *(the game rule)*.

### Laying season and first egg
Geese and guinea fowl lay in a season. The poultry year is the sheep calendar at the poultry scale: a real year is 365 / 3 =
**121.7 game days**, and day 0 of a world is 1 October (sheep.md) *(proposed: one calendar for every species)*.

| Species | First egg | Season | Rate in season | Source |
|---|---|---|---|---|
| Goose (Embden, Toulouse) | **1 year** old | **15 Feb - 15 Jun** | the year's eggs spread over the season | "geese begin to lay about mid-February and continue until early or mid-June" (FAO ch. 6); "Young females (approximately one year old) will lay eggs" (MSU) |
| Goose (Chinese) | 1 year | **1 Feb - 30 Jun** | as above | "a breeding season of approximately five months (February to the end of June)" (FAO ch. 1) |
| Guinea fowl | **28 weeks** (was 30) | **1 Apr - 31 Oct** | up to one a day, to the year's total | "start laying in March or April and may continue to lay until October" (EXT-GF); "an egg a day is common" (UKY); "Sexual maturity guinea hens appears to occur around 27-28 weeks of age with the onset of egg production" ([eXtension](https://ask.extension.org/kb/faq.php?id=753900)); Farm Radio gives 28-32 weeks ([Farm Radio](https://scripts.farmradio.fm/radio-script/13-backgrounder-raising-guinea-fowl-for-food-and-income/), secondary) |

- A new world starts on 1 October, so geese first lay about 46 game days in *(derived)* and guinea fowl about 61. Out of season they
  lay nothing. The coop readout shows the date and IN LAY or OUT OF SEASON.
- Toulouse geese "do not reproduce consistently until two or three years of age" (TLC); the game uses one year for all *(proposed)*.

### Mating and incubation
| Quantity | Goose | Guinea fowl | Source |
|---|---|---|---|
| Females per male | **4** | **5** (4-5) | "select one male for every four females" (FAO ch. 6); "one male for each four to five females" (UKY) |
| Fertile days after mating | **14** *(proposed)* | 21 *(proposed: the default)* | NOT FOUND for geese (searched FAO, extension, AI literature; only forum figures of 10-14 days; searched again 2026-11-14; the pages did not load) |
| Incubation | **30 days** | **27 days** (26-28) | "The incubation period for goose eggs is 30 days" (FAO ch. 8); "26 to 28 days" (UKY, EXT-GF) |
| Stop turning after day | **27** | **25** | goose: the hatcher takes the eggs for "days 27-30" and they are no longer turned (FAO ch. 8, *derived*); guinea: "Stop turning on day 25" (**TO VERIFY**: NC State 4-H egg chart, snippet; searched again 2026-11-14; the pages did not load) |
| Setter | 37.7 C, 50-55% RH, turned every 2 h | 37.7 C, 55% RH | FAO ch. 8; guinea "at a temperature of 37.7°C, relative humidity of 55%" (Kouame, Voemesse, Lin, Onagbesan & Tona 2021, Poultry Sci 100:101428, [PMC8531856](https://pmc.ncbi.nlm.nih.gov/articles/PMC8531856)) |

- Goose pairs: "In flocks larger than twenty this bond does not exist" (FAO ch. 2); the game uses the ratio only.
- Embden goslings can be sexed by down colour until 2-3 weeks (FAO ch. 1); not modelled.

### Grazing (geese)
- **Share from grass:** on a grassy run a goose gets **two-thirds** of its feed from grass *(derived)*: geese on pasture are held to
  "between 0.5-1.0 kg of grain per goose per week" (FAO ch. 9), about 107 g a day at the middle, against a full ration of "320 g per
  day bird" (FAO ch. 6). FAO also measured that geese with pasture "consumed 34 percent less of the grain-based ration", and "48
  percent less" at 12-14 weeks.
- **Grass worn:** the goose's grass share comes off the run's grass blocks at the hen rule (1.5 kg DM a block, regrowth 0.31 g a day),
  so a goose wears a run about ten times as fast as a hen (3.3 times the share of 3 times the feed) *(derived)*. "Stocking densities for geese on pasture are around
  150 geese per hectare" (FAO ch. 9): a run that would last is about 67 m² a goose *(derived)*. A bare run gives no forage; the grain
  feeder covers the full ration.
- Insects and snails "can provide about 10 percent of their total protein intake" (FAO ch. 9); not modelled.

### Feed, water and housing
| Quantity | Goose | Guinea fowl | Source |
|---|---|---|---|
| Full ration | **320 g a day** | **85 g a day** | FAO ch. 6; guinea "(84.5g/fowl/day)" (Konlan et al. 2011, J. World's Poult. Res. 1(1):1-3, a field study in Ghana) |
| Water | 3x feed *(proposed: the duck rule; NOT FOUND for adult geese)* | 2x feed *(proposed: NOT FOUND)* | searched again 2026-11-14; the pages did not load |
| House floor | **1 block holds 1** (1.0 m² on deep litter) | **1 block holds 4** (2-3 sq ft, 0.19-0.28 m²) | "each bird be given approximately 1.0 m 2 of floor space" (FAO ch. 6); "2 to 3 square feet per guinea" (UKY) |
| Run | **1 block a bird** (at least 1.0 m²) | 1 block a bird *(proposed: as hens)* | "a minimum of 1.0 m 2 per bird in a yard or run" (FAO ch. 6) |
| Nests | 4 birds a nest *(proposed: as ducks)* | 4 *(proposed)* | |
| Perch | none | **3 a perch block** *(proposed: as hens; guinea fowl roost high)* | "guineas roost in the trees at night" (UKY) |
| Forage share on the run | two-thirds (above) | **20%** (the hen rule) | guinea: "able to meet most of their nutrition requirements on their own" (UKY), no share found |

- Geese need no pond *(proposed: no source found that requires swimming water)*.

### Meat, down and guarding
- **Dressing:** goose **70%** ("approximately 70 percent with giblets", FAO ch. 13); guinea fowl **70%** ("70-71%" at 16 weeks,
  [UMaine 2223](https://extension.umaine.edu/publications/2223e/)).
- **Items:** `goose` / `cooked_goose` and `guinea_fowl` / `cooked_guinea_fowl`, at one item per 0.5 kg (eggs.md). An Embden goose
  gives 12 goose items *(derived)*.
- **Down:** a goose plucked live gives "between 100-120 g" of feathers and down, of which "between 15-20 percent" is down (FAO ch. 10).
  At slaughter a goose drops **1 down item** (about 30 g, the duck's item) *(proposed: no slaughter figure found; one plucking's down
  is 15-24 g)*. Live plucking is not modelled.
- **Guard geese:** "it does not forget people, animals or situations easily which is what makes it such a good watch animal against
  intruders whether human or animal" (FAO ch. 2); "The Chinese goose is reported to be very effective as a guard animal" (FAO ch. 1);
  guard geese "will almost charge you, honk, and use intimidation tactics", but "only … if there's an intruder that goes into their
  special area" ([National Geographic, 2022](https://www.nationalgeographic.com/animals/article/why-china-is-using-guard-geese-to-uphold-its-zero-covid-policy)).
  - **Game:** a tame goose on its coop's run (or in its coop) honks and charges a hostile mob, or a player who is not the coop's
    owner, inside the run or within **4 blocks** of it *(proposed)*. It bites for **1 damage** a second while touching *(proposed)*
    and goes back when the intruder leaves.
  - Chinese geese guard at twice the range *(proposed: the "very effective" breed)*. Hissing has no authoritative source; the goose
    honks.

### Alarm call (guinea fowl)
- "Guinea fowl will sound an alarm whenever anything unusual occurs on the farm"; "The warning call of guineas notifies others when
  raptors are in the area, giving them time to dash under shelter" (UKY).
- **Game:** a tame guinea that sees a hostile mob within **16 blocks** *(proposed)* calls (a loud call, at most once every 10 s per
  bird *(proposed)*). Every bird of that coop that is out on the run goes inside, and the coop readout shows `ALARM: <mob>` for the
  next minute. Wild guinea fowl call too, and flee.

### Plumage genes (new loci, appended after the Coturnix mutants)
| Locus | Species | Alleles | Source |
|---|---|---|---|
| `GOOSE_DILUTION` | goose | sex-linked dilution Sd > sd+ (Z-linked); two copies pale the bird to white, one copy to pale grey | "an allele for sex-linked dilution and white spotting, together producing white plumage"; Sd "was confirmed to cause sex-linked dilution, and thus autosexing" (Ollila et al. 2026, [bioRxiv preprint](https://www.biorxiv.org/content/10.64898/2026.01.19.700295v1.full), confirmed 2026-11-14; still a preprint, not peer reviewed). The one-copy and two-copy looks follow the Pilgrim, "the males being white and the females grey" (FAO ch. 1) |
| `GOOSE_SPOTTING` | goose | white spotting S, recessive to plain *(proposed mode)*; with dilution it makes the white Embden, alone the saddleback | "Saddleback pattern is caused by white spotting" (Ollila et al.) |
| `GOOSE_WHITE` | goose (Chinese) | Chinese white, recessive | "the white plumage phenotype in Chinese geese should be recessive" ([Xi et al. 2020, BMC Genomics](https://pmc.ncbi.nlm.nih.gov/articles/PMC7027040)); OMIA lists it as co-dominant (Yang 2024, Wugang Copper geese). Both readings are confirmed (2026-11-14): recessive in Gang geese (Xi 2020), co-dominant in Wugang Copper geese. The game keeps recessive |
| `GUINEA_LAVENDER` | guinea | lavender, recessive | "recessive to pearl grey" (**TO VERIFY**, breeder source; searched again 2026-11-14; the pages did not load); "all of these loci are autosomal with three being recessive and the fourth incompletely dominant" ([Soara et al. 2022, PeerJ](https://pmc.ncbi.nlm.nih.gov/articles/PMC8784013)) |
| `GUINEA_WHITE` | guinea | white, recessive *(proposed: one of the three recessive loci; which one is NOT FOUND; searched again 2026-11-14; the pages did not load)*. White keets hatch white | "the only solid white bird that hatches solid white and not yellow" (UKY) |

- **Looks:** goose grey (Toulouse, wild type), pale grey, white, saddleback, Chinese brown (the swan goose's wild type) and Chinese
  white; guinea pearl, lavender, white (white over lavender). The Chinese goose has the bill knob on the model.
- **Founders:** Embden Sd/Sd (gander) or Sd (goose) with spotting; Toulouse wild type; Chinese with the white allele at 77%, so about
  60% white *(proposed)*; guinea fowl pearl, with lavender and white carriers at 10% each *(proposed)*.

### Wild geese and guinea fowl
| Biome group | Added | Source |
|---|---|---|
| River, swamp | goose: Toulouse, Embden | the greylag, ancestor of European geese, breeds in wetlands *(proposed mapping)* |
| Plains | goose: Chinese | the swan goose, the Chinese goose's ancestor, grazes grassland by water *(proposed mapping)* |
| Savanna | guinea fowl | helmeted guinea fowl are African savanna birds *(proposed mapping)* |

Groups of 4 with one male, weight 10, as the other fowl. Wild geese and guinea fowl tame in the corral on grain.

### Keets
- Brooding: "Hovers should be started at 95°F (35°C) for the first two weeks, and then lowered by 5°F (2.75°C) per week" (UKY). The
  brooder's warmth rule covers keets and goslings as the other young.
- Keets are "very susceptible to dampness during their first two weeks"; not modelled (no death from care).

### Open points
- A goose fertile-days figure, adult goose and guinea water, the Toulouse egg weight, gene names for guinea colours.

### Built, 2026-11-13: geese
- **Code:** `Species.GOOSE`; `FowlBreed` EMBDEN, TOULOUSE, CHINESE with laying seasons (`inLay`, `seasonDays`); `LayingCycle` lays a
  seasonal bird's year evenly over its season; loci GOOSE_DILUTION (Z), GOOSE_SPOTTING, GOOSE_WHITE; `PoultryLook` six goose plumages,
  packed in 6 bits now; `Fowl.forageShare`; the guard goal in `FowlEntity`; the calendar on every bird (`dayOfYear`).
- **Wild geese:** their own spawn entries (river, swamp, plains), so the chicken weights are unchanged.
- **The model:** the goose has the duck's bill and webbed feet, a long neck, and the Chinese its knob.
- **Sound:** geese use the bird call, low and loud for the guard's honk (no goose sound in the game).
- **Tests:** `GooseGeneticsTest`; game tests goose_founders, goose_lays_in_season, goose_grazes_run, goose_drops_down,
  guard_goose_chases_zombie, goose_egg_hatches. Client scene `newbirds`.

### Built, 2026-11-13: guinea fowl
- **Code:** `Species.GUINEA`, `FowlBreed.HELMETED` (laying 1 Apr - 31 Oct); loci GUINEA_LAVENDER and GUINEA_WHITE; plumages PEARL,
  LAVENDER, GUINEA_WHITE; the casque and wattles on the bird model; the alarm in `FowlEntity.watch` and `CoopControllerEntity.alarm`.
- **The alarm:** a guinea fowl that sees a hostile mob within 16 blocks calls (at most every 10 s); its coop brings every bird in from
  the run for a minute and the readout shows `ALARM: <mob>`. The call is the bird call, loud and shrill.
- **Wild guinea fowl:** their own spawn entry in the savannas.
- **Tests:** game tests guinea_colours_inherit, guinea_lays_in_season, guinea_alarm_on_zombie; client scene `newbirds`.

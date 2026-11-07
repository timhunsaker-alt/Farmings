# Cattle

**Status: first draft, 2026-11-07. Nothing here is built.** Real figures carry a source. A line marked **TO VERIFY** has a
figure from memory or from a source that is not the primary one, and must be checked before code uses it. A line marked
**DECISION** is open: ask Tim, do not choose.

## Time scale (decided with Tim, 2026-11-07)
**1 game day = 30 real days.** A Minecraft day is 20 minutes, so a cow's whole year takes about 12 game days. The ratio is one
config value (`TIME_SCALE_DAYS = 30`); every duration below is stored in real days and converted. Rounded game days
are for reading only.

| Stage | Real | Game days at 1:30 |
|---|---|---|
| Heat cycle | ~21 days | 0.7 |
| Gestation | ~283 days | 9.4 |
| Lactation | 305 days | 10.2 |
| Dry period | 60 days | 2 |
| Calving interval (target) | 365 days | 12.2 |
| First calving | ~24 months (~730 days) | 24.3 |

## Sexes and stages
- **Female:** calf, heifer, bred heifer, lactating cow, dry cow, retired. A heifer becomes a cow only by calving.
- **Male:** bull calf, bull, steer (a castrated male raised for beef).
- Calf sex is 50/50. FarmChain's tutorial guarantees (first two calves heifers) are **not** used here. **DECISION** if wanted.
- A bull does not give milk. A steer does not breed.

## Reproduction
| Quantity | Value | Source |
|---|---|---|
| Gestation | about 283 days (one study of 1,109 cows); a more recent analysis found about 276 | [Purdue dry-period study, via Bovine Practitioner](https://bovine-ojs-tamu.tdl.org/bovine/article/view/1517/1510); [Dairy Herd: dry period cues](https://www.dairyherd.com/news/dairy-production/dry-period-cues-better-lactation-performance). Game uses 283. |
| Dry period | 60 days is the conventional standard; 40-50 days shows little harm when gestation is normal | [Dairy Herd](https://www.dairyherd.com/news/dairy-production/dry-period-cues-better-lactation-performance); [Farmtario](https://farmtario.com/dairy/shorter-dry-periods-make-sense-with-caveats). Game uses 60. |
| Lactation | 305 days (standard record length) | [Feedipedia, Cattle husbandry: milking cows](https://www.feedipedia.org/sites/default/files/public/BH_018_milking%20cows.pdf). **TO VERIFY** the 305 figure in that file. |
| Calving interval | 365 days is the usual target. 305 + 60 = 365 leaves about 82 days from calving to conception (283-day gestation) | Arithmetic from the figures above. |
| First calving | about 24 months; 23-26 months is the usual range | [Wisconsin Extension](https://dairy.extension.wisc.edu/articles/weight-or-wait-how-defining-breeding-eligibility-of-heifers-impacts-first-lactation-milk-production); [Wynnstay](https://www.wynnstay.co.uk/blog/heifers-must-be-the-correct-body-size-as-well-as-age-at-first-calving/) |
| First breeding | at 55-60% of mature weight, about 13-14 months; breeding weight Holstein 390 kg, Brown Swiss 390 kg, Jersey 260 kg | [Wynnstay](https://www.wynnstay.co.uk/blog/heifers-must-be-the-correct-body-size-as-well-as-age-at-first-calving/); [Engormix: dairy heifer development](https://en.engormix.com/dairy-cattle/articles/dairy-heifer-development-t35401.htm) |
| Puberty | tied to weight (about 40% of mature weight), not age | [KALRO: rear your own heifers](https://kalro.org/navcdp/docs/livestock/Rear%20your%20own%20heifers.pdf) |
| Heat cycle | about 21 days | **TO VERIFY** (not found in the first search; needs a source). |

Game rule from this: **a heifer can be bred when she reaches the breeding weight for her breed, which depends on her growth.
Well-fed heifers get there sooner.** Growth comes from the feed rules below.

## Milk
- Yield follows a lactation curve (rises to a peak at 6-8 weeks, then declines). The curve shape and each breed's peak and
  total are **TO VERIFY** against the [USDA AIPL national lactation averages](https://aipl.arsusda.gov/publish/dhi/dhi13/lasum.html).
  The first search could not extract clean per-lactation milk totals for Holstein and Jersey.
- Components by breed (USDA AIPL standardized lactation averages; recent columns, treat as indicative, **TO VERIFY** against
  the current tables):

| Breed | Fat % | Protein % |
|---|---|---|
| Holstein | 3.72 | 3.06 |
| Jersey | 4.76 | 3.63 |
| Brown Swiss | 4.02 | 3.32 |

  Source: [AIPL Tables 8A-8F](https://aipl.arsusda.gov/publish/dhi/dhi13/lasum.html). Brown Swiss milk is about 22,700 lb (about 10,300 kg)
  per standardized lactation for 2015 calvings, from the same tables.
- Gyr, Angus and Wagyu figures: not found yet (**TO VERIFY**; see Breeds).
- Milking is twice a day on a real farm; the game milks once per game day through the parlor and counts both milkings. **DECISION**
  if per-milking cycles are wanted.

## Feed and water
- **Hay:** a cow eats hay blocks (the vanilla hay block) from a feeder. Cattle eat nothing else in Phase 1.
- **Intake:** forage-based planning figures are about 2.7% of body weight per day as dry matter for a lactating cow and about
  2.2% for a dry cow (a regional extension source). **TO VERIFY** against NRC dairy requirements; this is a forage-only
  assumption, not a TMR figure. How many kg of dry matter one hay block holds is a **DECISION** (a real bale is
  15-25 kg; a vanilla block is one block). It sets how fast a herd empties the feeder.
- **Water:** the NRC (2001) equation: `35.25 + 1.58 x DMI(lb/day) + 0.90 x milk(lb/day) + 0.11 x sodium(g/day)` pounds
  of water per day, with a temperature term in some versions. A UC Davis fact sheet gives `6 L per kg DMI + 1 L per L milk`.
  Heat raises need a lot (120-200% in one source). Sources: [NMSU D107](https://pubs.nmsu.edu/_d/D107.pdf),
  [UC Davis](https://epakag.ucdavis.edu/livestock/factsheets/fs-live-AIP12-CowBuffaloWaterNeeds.pdf),
  [NDSU](https://www.ag.ndsu.edu/publications/livestock/water-needs-and-quality-guidelines-for-dairy-cattle/water-needs-and-quality-guidelines-for-dairy.pdf/@@download/file/Water%20Needs%20and%20Quality%20Guidelines%20for%20Dairy%20Cattle.pdf).
  Phase 1 uses the NRC equation without the sodium term. The two sources disagree by a wide margin for the same cow, so the
  water numbers are **TO VERIFY** and Tim approves the chosen equation.
- **Underfeeding:** a cow that eats less than she needs gives less milk, grows slower and loses health. **She never dies from a
  missed day.** (The same rule FarmChain uses.)

## Housing
- Space per animal (stall width, feeder length, trough length, pasture area) is **TO VERIFY**; not yet researched. It sets the
  barn's capacity and is needed before the barn spec.

## Breeds (decided with Tim, 2026-11-07)
Six cattle breeds to start. The first four are dairy and come from FarmChain's 30-breed registry
(`BREED-SPEC.md` section 6.1); their 0-100 game-balance scores come across as they are.

| Code | Breed | Use |
|---|---|---|
| HOL | Holstein-Friesian | dairy, high volume |
| JER | Jersey | dairy, high butterfat, small |
| BSW | Brown Swiss | dairy, long-lived, balanced |
| GYR | Gyr | dairy, heat tolerant |
| ANG | Angus | **beef** |
| WAG | Wagyu | **beef** |

**Angus and Wagyu are not in FarmChain's registry.** FarmChain is dairy-only, so they have no baseline scores. They need new
beef traits, which are all **DECISIONS** to approve:
- **Beef traits:** growth rate, carcass weight (dressing percentage), marbling, and the same fertility, health, longevity and
  temperament traits the dairy breeds use.
- **Real differences to build on (from the first search):** Wagyu-sired carcasses grade clearly higher in marbling (one study:
  marbling score 771 vs 597; 65% graded Prime vs 21%). Angus steers generally reach heavier final weights, and Angus carcasses
  tended to carry more backfat. A Wagyu x Angus study gave a 55.6% dressing percentage, and a Czech source gives about 64% for
  Wagyu, so dressing percentage is **TO VERIFY**.
  Sources: [Production characteristics and carcass quality of Angus and Wagyu](https://twinwoodcattle.com/sites/default/files/publications/2019-06/Production%20Characteristics_and_Carcass_Quality_of_Angus_and_Wagyu.pdf);
  [Growth and carcass characteristics of Angus and American Wagyu steers](https://twinwoodcattle.com/sites/default/files/publications/2021-01/TW455_Growth%20and%20carcass%20characteristics%20of%20Angus%20and%20American%20Wagyu%20steers.pdf);
  [Acta Univ. Agric. Silvic. Mendelianae Brun. 2018](https://acta.mendelu.cz/artkey/acu-201804-0007_analysis-of-growth-intensity-and-carcass-characteristics-of-wagyu-aberdeen-angus-crossbred-steers.php).
- **Written exception (Tim's request, 2026-11-07): Wagyu drop more steaks than Angus.** In the real data the Wagyu advantage
  is marbling and grade, not carcass weight, and Angus grow faster. So this is a game rule, not a realism claim. Proposal,
  not decided: steak count comes from carcass weight and dressing percentage; marbling raises steak quality. Either way,
  Wagyu's steak total is set higher by the breed baseline. **DECISION:** the exact steak counts per breed and the steak grades.
- Beef breeds also give a cow and a calf, so a beef cow has a milk trait for raising her calf only. Whether beef cows can be
  milked at all is a **DECISION**.
- Mature weights, gestation for Angus and Wagyu, and their breeding weights: not found yet. **TO VERIFY**.

## Open decisions
1. Per-milking cycles or one milking per game day.
2. Kilograms of dry matter in one hay block.
3. Which water equation, and whether heat adds to it.
4. Beef traits and baseline scores for Angus and Wagyu; steak counts and grades per breed; whether beef cows can be milked.
5. Whether to use FarmChain's "first two calves are heifers" tutorial guarantee.

## To research before the barn and parlor specs
Heat cycle length, space per animal, hay bale mass, Gyr figures, mature weights for all six breeds, peak and total milk yield
by breed, and the primary NRC source for intake.

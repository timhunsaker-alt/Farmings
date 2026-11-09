# Cattle

**Status: first draft, 2026-11-07. Nothing here is built.** Real figures carry a source. A line marked **TO VERIFY** has a
figure from memory or from a source that is not the primary one, and must be checked before code uses it. A line marked
**DECISION** is open: ask Tim, do not choose.

## Time scale (decided with Tim, 2026-11-07; changed from 30:1 to 15:1 the same day)
**1 game day = 15 real days.** A Minecraft day is 20 minutes, so a cow's whole year takes about 24 game days (8 real hours of
play). The ratio is one config value (`TIME_SCALE_DAYS = 15`); every duration below is stored in real days and converted. Rounded
game days are for reading only. Output per game day (milk, manure) is **one real day's output** (decided with Tim), not 15 days'.

| Stage | Real | Game days at 1:15 |
|---|---|---|
| Heat cycle | ~21 days | 1.4 |
| Gestation | ~283 days | 18.9 |
| Lactation | 305 days | 20.3 |
| Dry period | 60 days | 4 |
| Calving interval (target) | 365 days | 24.3 |
| First calving | ~24 months (~730 days) | 48.7 |

## Sexes and stages
- **Female:** calf, heifer, bred heifer, lactating cow, dry cow, retired. A heifer becomes a cow only by calving.
- **Male:** bull calf, bull, steer (a castrated male raised for beef).
- Calf sex is 50/50, except FarmChain's tutorial guarantee carries over: **a player's first two calves are heifers** (decided
  with Tim, 2026-11-07). The **third calf is a bull**, as in FarmChain (recommended, 2026-11-07); after that, 50/50.
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
| Heat cycle | about 21 days (range 17-24); heats are short, a few to about 18 hours | [LSU AgCenter: the bovine estrous cycle](https://www.lsuagcenter.com/articles/page1678995149005); [Acta Univ. Agric. Silvic. Mendelianae Brun. 2018](https://actavia.mendelu.cz/pdfs/acu/2018/02/27.pdf). Heat duration is **TO VERIFY**: sources disagree (about 7-9 h in one study, 18-24 h in a slide deck). |

**First breeding at 15 months (decided with Tim, 2026-11-07; read as the age a heifer is first bred).** For all six breeds a heifer
can first be bred at 15 real months (30.4 game days) and calves about 9 months later, at roughly 24 months (about 49 game days).
She must also have reached the breeding weight for her breed (55% of mature weight). An underfed heifer is delayed until she
does; a well-fed one is never bred earlier than 15 months. Growth comes from the feed rules below.

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
- **Gyr** (the dairy-strain Gir): 1,225-2,268 kg per lactation in India, 240-380 days, fat 4.5-4.6% ([Indian Journal of Animal Sciences review](https://epubs.icar.org.in/index.php/IJAnS/article/download/119832/46085/337986)). A Tamil Nadu herd in hot, humid conditions gave 1,507 kg and 5.05% fat with a 447-day calving interval. Brazilian averages are quoted higher (about 3,500 kg) but only by a popular article, **TO VERIFY**.
- **Lactation curve:** yield peaks about 4-8 weeks after calving. One Colombian Holstein herd averaged 5,830 kg per 305 days with a peak near 27.7 kg/day at 4.7 weeks ([Fitting lactation curves in a Colombian Holstein herd](https://agris.fao.org/search/en/records/6759909bc7a957febdfc71f9)); Wood's model is the practical curve ([Revista Facultad Nacional de Agronomia Medellin](https://revistas.unal.edu.co/index.php/refame/article/download/67424/65883/381784)). Per-breed peaks are **TO VERIFY**.
- **Wood's curve research, 2026-11-08 (all *(unverified)*, from a conference abstract):** yield on day t is `a * t^b * e^(-c*t)` kg/day.
  [ADSA 2019 abstract M128](https://m.adsa.org/2019/abs/t/78869), Wisconsin test-day data. First lactation: Holstein a 16.13, b 0.235, c 0.0019;
  Jersey a 14.07, b 0.186, c 0.0021. Later lactations: Holstein a about 23.7, b 0.23-0.24, c 0.0032-0.0036; Jersey a about 19.2, b 0.17-0.19,
  c 0.0028-0.0032. Worked out by us (not in the source): Holstein later lactations peak near day 69 at about 51 kg and total about 13,200 kg in 305 days;
  the USDA national average is 12,558 kg (see below), so these parameters are only about 5% high. (An earlier note here said 10,900 kg; that was wrong.) Jersey later:
  peak day 60, about 33 kg, about 8,750 kg. **No Brown Swiss or Gyr parameters found.** The USDA AIPL links and the Brown Swiss papers
  (Turkey 2024, Slovenia 2013) could not be opened. Do not build milk yields until Tim picks a source.
- **Decided with Tim, 2026-11-08: scale to the USDA average.** National standardized 305-day averages, 2019 calvings
  ([CDCB tables 7B-7F](https://queries.uscdcb.com/publish/dhi/current/lasum.html)): Holstein 27,686 lb milk (12,558 kg), fat 3.90%, protein 3.15%;
  Jersey 20,740 lb (9,408 kg), 4.82%, 3.68%; Brown Swiss 22,737 lb (10,313 kg), 4.07%, 3.36%. Herd averages for 2021 are close
  ([DHI herd averages 2021](https://queries.uscdcb.com/publish/dhi/current/haall.html)). These replace the older component table above.
  Built in `husbandry/Lactation`: Wood's shape from the ADSA midpoints (Holstein b 0.235 c 0.0034; Jersey b 0.18 c 0.0030), scale solved so
  the 305-day sum equals the USDA total. That gives Holstein a peak near day 69 at about 48 kg, Jersey near day 60 at about 36 kg.
  **Stand-ins (proposed):** Brown Swiss uses Holstein's shape; Gyr uses Jersey's shape with the middle of the Indian range (1,746.5 kg).
  The first-lactation reduction (Wisconsin fits suggest about 15% less) is **not built**. The link from the genome's milk-yield trait to a yield
  factor is **not built** (needs a decision); the function takes a factor, 1.0 = breed average.
- Milking is twice a day on a real farm; the game milks **once per game day** through the parlor and counts both milkings
  (decided with Tim, 2026-11-07). How much a milking holds is tied to the output-per-game-day decision in `docs/waste.md`.

## Feed and water
- **Hay:** a cow eats hay blocks (the vanilla hay block) from a feeder. Cattle eat nothing else in Phase 1.
- **Intake:** forage-based planning figures are about 2.7% of body weight per day as dry matter for a lactating cow and about
  2.2% for a dry cow (a regional extension source). **TO VERIFY** against NRC dairy requirements; this is a forage-only
  assumption, not a TMR figure. **One hay block = 100 kg** (decided with Tim, 2026-11-07; a real square bale is 15-25 kg and a
  round bale about 500 kg, so 100 kg is a game size). Recommended: **100 kg is fresh weight**, and the dry matter in it is the hay's dry-matter
  share (about 85-90% for dry hay, **TO VERIFY** against a feed table). It sets how fast a herd empties the feeder.
- **Water: use the UC Davis rule** (decided with Tim, 2026-11-07): `6 L per kg DMI + 1 L per L milk`, plus extra in heat. For
  reference, the NRC (2001) equation: `35.25 + 1.58 x DMI(lb/day) + 0.90 x milk(lb/day) + 0.11 x sodium(g/day)` pounds
  of water per day, with a temperature term in some versions. A UC Davis fact sheet gives `6 L per kg DMI + 1 L per L milk`.
  Heat raises need a lot (120-200% in one source). Sources: [NMSU D107](https://pubs.nmsu.edu/_d/D107.pdf),
  [UC Davis](https://epakag.ucdavis.edu/livestock/factsheets/fs-live-AIP12-CowBuffaloWaterNeeds.pdf),
  [NDSU](https://www.ag.ndsu.edu/publications/livestock/water-needs-and-quality-guidelines-for-dairy-cattle/water-needs-and-quality-guidelines-for-dairy.pdf/@@download/file/Water%20Needs%20and%20Quality%20Guidelines%20for%20Dairy%20Cattle.pdf).
  The two sources disagree by a wide margin for the same cow; the UC Davis rule is chosen. The heat term is **TO VERIFY**.
- **Hay dry matter:** the Victorian government's feed table assumes **85% dry matter** for hay bales ([Victoria: weight of hay bales](https://feedinglivestock.vic.gov.au/?p=197)),
  so one 100 kg hay block holds **85 kg of dry matter**. A cow at about 20 kg of dry matter a day eats about a quarter of a hay block
  each game day. (Hay alone is a forage ration; see the intake caveat above.)
- **Underfeeding:** a cow that eats less than she needs gives less milk, grows slower and loses health. **She never dies from a
  missed day.** (The same rule FarmChain uses.)

## Housing
- **Stall (Holstein, 1,000 lb cow):** 42 in (107 cm) wide centre-to-centre and 10 ft (3.1 m) long, plus 3-4 ft (91-122 cm) of lunge space.
  A 2,000 lb cow wants 57 in (145 cm). Source: [Dairyland Initiative, UW-Madison](https://thedairylandinitiative.vetmed.wisc.edu/adult-cow-housing/freestall-design-and-dimensions/).
- **Feed bunk:** about 75 cm per head for a 700 kg Holstein ([Lactanet](https://lactanet.ca/en/housing-newly-weaned-heifers-2/); a heifer-housing table, **TO VERIFY** for lactating cows).
- **Alley: 3 m** (decided with Tim, 2026-11-07). The powered scraper runs along it. The scraper's own width is set in its spec.
- Water trough length and pasture area per animal: **TO VERIFY**.

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
  Wagyu's steak total is set higher by the breed baseline. The exact steak counts per breed and the steak grades are set in
  the beef-traits section once the beef traits are researched (still a **DECISION**, not yet numbers).
- Beef cows are **not milked** (recommended, 2026-11-07): their milk trait only raises their own calf.
- **Beef cows are not milked and their meat is food, not stock.** (No selling; see Economy.)
- **Dressing percentage:** fed beef is usually 62-64% of live weight. The four main steak cuts (strip loin, tenderloin, sirloin, ribeye)
  are about 8-12% of carcass weight; individual steak types are each under 3% ([Penn State: beef carcass yields](https://extension.psu.edu/understanding-beef-carcass-yields-and-losses-during-processing);
  [South Dakota State](https://extension.sdstate.edu/how-much-meat-can-you-expect-fed-steer)). A 760 kg Wagyu steer (slaughter at about 29 months) at 62-64% gives roughly a 470-485 kg carcass and about 40-55 kg of the main steaks.
- **Gestation:** Angus about 280 days (10,000+ records); Wagyu 283.8 days in a Brazilian herd and 286 days for Wagyu-sired calves
  ([USDA ARS Lincoln](https://digitalcommons.unl.edu/hruskareports/378); [The sire effect on gestational length in Wagyu cattle](https://agris.fao.org/search/en/records/687a93589d6e0577321f4de6)).
  Gyr: not found, **TO VERIFY**; 283 is used as a stand-in until then.

### Mature cow weight (kg)
| Breed | Weight | Source |
|---|---|---|
| Holstein | about 710 | Duplessis et al. 2014, via [Better Farming](https://betterfarming.com/flippingbook/betterfarming/2024/june-july/50) |
| Jersey | about 470 | same |
| Brown Swiss | about 600-612 | breed lists; secondary, **TO VERIFY** |
| Gyr | about 313 (study) to 385 (breed profile) | [Animal Genetic Resources](https://www.cambridge.org/core/journals/animal-genetic-resources-resources-genetiques-animales-recursos-geneticos-animales/article/gir-cattle-breed-of-india-characteristics-and-present-status/A689843E8FCF994B31C3AF552CA0F356); [Oklahoma State](https://breeds.okstate.edu/cattle/gir-cattle). Indian figures; Brazilian dairy strain **TO VERIFY**. Game: use 385 until verified. |
| Angus | about 550 | breed lists; secondary, **TO VERIFY** |
| Wagyu (Japanese Black) | about 487 | Motoyama et al. 2014, via [Australian Wagyu Association](https://www.wagyu.org.au/wp-content/uploads/2021/03/Australian-Wagyu-Update-Ten-year-trends-PART-2.pdf) |

Breeding weights follow as 55% of these (Holstein about 390 kg, Jersey about 260 kg; the Gyr, Angus, Wagyu and Brown Swiss ones are computed, not sourced).

## Economy: nothing is sold (decided with Tim, 2026-11-07)
Milk and beef are **not sold**. The player eats them, and the farm turns them into other items (cheese and other dairy and meat
products, set out in the processing specs). There are no buyers, no contracts and no currency for animal products. FarmChain's
buyer contracts are not ported. A breed's value is in what it makes and how well, not in a price.

## Waste
Cattle make manure that must be collected. See `docs/waste.md` (collection in the barn, then composting and a methane digester).

## Open decisions
1. Beef traits and baseline scores for Angus and Wagyu, and steak counts and grades per breed. The research above (dressing 62-64%, steaks
   8-12% of carcass, Wagyu marbling) is the basis; the numbers come next, with Tim's rule that Wagyu give more steaks.
2. Whether a heat term is added to the UC Davis water rule, and its size (no — dropped, 2026-11-07, "no need").

Decided 2026-11-07: time scale 1 game day = 15 real days; one milking per game day, one real day's output; hay block = 100 kg
(fresh weight); UC Davis water rule; first two calves heifers, third a bull; beef cows are not milked.

## Still to research
Per-breed peak and total milk yield, heat duration, a primary source for brown swiss and Angus weights, water trough and pasture
space, Gyr gestation and Brazilian Gyr weight, and the primary NRC source for intake.

## Built, 2026-11-09: first lactation, readout, castration, beef
- **First lactation** gives 15% less (the Wisconsin fits above). The cow counts her lactations; it is saved.
- **Readout:** right-click with an empty hand shows breed (or the cross, largest share first), sex, age, weight, stage, heat, due date, days in milk and lactation number, milk in the udder, fed and watered, stall dirt and health.
- **Castration** *(proposed)*: shears on a bull calf make him a steer.
- **Angus and Wagyu baselines** on FarmChain's 0-100 scale, all *(proposed, no source; FarmChain has no beef breeds)*: Angus milk 20, fat 50, protein 50, feed efficiency 75, health 80, fertility 80, longevity 75, temperament 70. Wagyu 15, 55, 50, 55, 75, 70, 85, 85. They point the way the real differences go: little milk, Angus more efficient and fertile, Wagyu longer-lived and calmer.
- **Beef traits** (dressing %, marbling; growth is the breed's own weight curve) come from the animal's breed composition, weighted by share. They are not genome loci, so the FarmChain-checked genome is unchanged.
- **Meat** (husbandry/Carcass, 1 item = 10 kg, Tim 2026-11-09):
  - carcass = live weight x dressing %: 63% for beef breeds, 58% for dairy ([Penn State](https://extension.psu.edu/understanding-beef-carcass-yields-and-losses-during-processing));
  - boneless retail meat = 50% of the carcass (Penn State, yield grade 3);
  - steaks = 10% of the carcass (the four steak cuts are 8-12%), taken out of the retail meat; the rest drops as vanilla beef;
  - Wagyu: steak share x 1.5 for a purebred, scaled by Wagyu share *(proposed; Tim's game rule, not a realism claim)*;
  - marbling: Angus 597, Wagyu 771 (Twin Wood study above); dairy 450 *(proposed)*. Grade: under 400 Select, 400-699 Choice, 700 up Prime;
  - steak items by grade, raw and cooked. Raw is vanilla raw beef. Cooked Select is vanilla steak (8 hunger); Choice 9, Prime 10 *(proposed)*;
  - 1-3 leather; an animal that dies burning drops cooked meat.
  - Results: a 550 kg Angus gives 13 beef and 3 Choice steaks. A 487 kg Wagyu gives 10 beef and 4 Prime steaks.
- Game tests: first_lactation_lower, cow_readout_and_castration, beef_drops_by_weight, wagyu_more_steaks.

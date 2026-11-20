# Animal health: disease, the vet cycle and quarantine

**Status: APPROVED by Tim 2026-11-19 (Wave B, build 1): all eight questions answered with the recommended option. Built on branch
`animal-health-spec`; see "Built" at the end.** Marks as in `docs/crops.md`:
real figures carry a source. *(proposed)* marks a game choice with no source. **TO VERIFY** marks a weak source (a trade page, a
search snippet, a secondary summary). **NOT FOUND** means no source was found, so the game value is proposed. The open design
choices are at the end and in `docs/questions/animal-health.md`. Everything else is decided on recommendation, and says so.

Farmings follows real animal science but is not veterinary advice. There is no gore: no wounds, blood, carcasses or culling on screen.

## What it is
Until now an animal's health has been its vanilla hit points (10 for cattle, 22.5 for horses), lowered by hunger, thirst, cold and
dirty stalls and never below a floor. The genome's HEALTH trait is scored but does nothing. This build adds:

1. **Diseases** with real counterparts: what causes them, which animals get them, how they spread inside a barn, shed, coop or stable,
   what the player sees, and what they cost (milk, growth, eggs, condition).
2. **The vet cycle**, the routine of a real herd health programme: look over the herd, examine the animals that look off, treat or
   isolate, write it down, hold milk and meat for the withdrawal time, recheck, vaccinate on schedule.
3. **Quarantine and a sick pen:** an isolation pen where new animals wait out their quarantine and sick ones are kept apart.
4. **Notifiable diseases** that a farm must report, with a standstill when it does (an opt-in world rule; question 3).

## Real counterpart
A dairy or mixed livestock farm's herd health routine:
- **Daily:** stockpeople watch for animals that eat less, stand apart, cough, limp or give abnormal milk. Milkers strip each quarter
  and dip every teat after milking.
- **Exam:** a rectal temperature, a look at the signs, a California Mastitis Test (CMT) on a cow with a suspect quarter.
- **Treatment:** by the farm's vet or under their written protocol, and every treatment is recorded, with the date the animal's milk and
  meat may go into the food chain again (the **withdrawal time** on the drug's label).
- **Prevention:** vaccines on a calendar (before calving or lambing, at weaning, at hatch), footbaths, clean bedding, colostrum.
- **Biosecurity:** new or returning animals are isolated, "a minimum of 30 days in a separate pen with no over-the-fence contact"
  ([Idaho State Dept. of Agriculture, cattle biosecurity](https://agri.idaho.gov/wp-content/uploads/animal-industries/Biosecurity-Cattle.pdf));
  NC State gives "21-30 days" depending on the diseases of concern ([NC State Extension](https://stokes.ces.ncsu.edu/news/biosecurity-for-livestock/)).
- **Reporting:** foreign animal diseases such as foot-and-mouth disease, African swine fever and highly pathogenic avian influenza
  are reportable, immediately, to the state animal health official ([CFSPH, Iowa State](https://www.cfsph.iastate.edu/thelivestockproject/wp-content/uploads/sites/5/2022/03/reportable-disease-fad-infographic.pdf)).

## Time
Every duration below is in **real days**, converted by each species' own clock (`TimeScale.of(species)`): 15 real days a game day for
cattle, sheep, goats, pigs and horses, 3 for poultry (Tim kept Farmings' own clocks, 2026-11-14). Annual incidences are turned into a
hazard per real day, `h = -ln(1 - annual share) / 365`, and a game day of a 15:1 animal runs 15 real days of it. So:

| Thing | Real | Game (15:1) |
|---|---|---|
| Clinical mastitis, 24.8% of cows a year | 0.00078 a cow a real day | about 1 case per 4 cows per cow-year, a cow-year being 24 game days (8 h of play) |
| Milk withdrawal, intramammary tube | 96 h | 0.27 game day (6,400 ticks) |
| Meat withdrawal, long-acting injection | 28 days | 1.87 game days |
| Quarantine | 30 days | 2 game days (40 minutes) |
| BRD risk window after arrival | 45 days | 3 game days |

**Decided on recommendation:** rates come out at the real annual figures on each species' clock (question 7 asks about the pace).

## The diseases
Twelve, chosen so every species has one or two that matter and each one ties to something the farm already has (stalls, pasture,
the parlor, the hatchery, the corral). Nine are endemic; three are notifiable (opt-in, question 2). Question 6 asks about the scope.

| # | Disease | Animals | Real cause | Where it comes from in the game |
|---|---|---|---|---|
| 1 | **Mastitis** | cows, dairy ewes and does in milk | bacteria in the udder: environmental (coliforms, *Strep. uberis*) or contagious at milking (*Staph. aureus*) | dirty stalls; another infected cow on the same milking unit |
| 2 | **Bovine respiratory disease (BRD, shipping fever)** | cattle | viruses (IBR, BVD, PI3, BRSV) then *Mannheimia haemolytica* | stress: taming, a move to new housing, weaning; then spread by air within the barn |
| 3 | **Neonatal scours** | calves, lambs, kids, piglets under 30 days | *E. coli*, rotavirus, coronavirus, *Cryptosporidium* | dirty pen, cold (the piglet and chick cold rule), a dam not vaccinated |
| 4 | **Foot rot** | cattle, sheep, goats | *Fusobacterium necrophorum* (cattle); *Dichelobacter nodosus* (sheep, goats; contagious) | wet, dirty floors; wet pasture in mild weather; an infected sheep on the same pasture |
| 5 | **Clostridial enterotoxaemia (overeating disease)** | sheep, goats | *Clostridium perfringens* type D toxin | a sudden rise in grain in the ration; prevented by the CDT vaccine |
| 6 | **Swine erysipelas** | pigs | *Erysipelothrix rhusiopathiae* | sporadic; spread within a pen run; prevented by vaccine |
| 7 | **Coccidiosis** | poultry under 8 weeks *(proposed age)* | *Eimeria* protozoa in the gut | wet, dirty litter; spread within the coop |
| 8 | **Marek's disease** | chickens | a herpesvirus; nerve and tumour disease | in the dust of any coop; prevented only by vaccinating at hatch |
| 9 | **Strangles** | horses | *Streptococcus equi* | a new horse; spread within the stable; some recovered horses carry it for months |
| 10 | **Foot-and-mouth disease** *(notifiable)* | cattle, sheep, goats, pigs | a picornavirus | very rarely with wild cloven-hoofed animals brought in through the corral |
| 11 | **Highly pathogenic avian influenza** *(notifiable)* | all poultry | influenza A (H5) | wild waterfowl: wild ducks and geese near the coop or the duck pond |
| 12 | **African swine fever** *(notifiable)* | pigs | an asfivirus | wild pigs brought in through the corral, or pigs on a pasture wild pigs roam |

### 1. Mastitis
- **Incidence:** clinical mastitis "in about one-fourth of all cows (24.8 percent)" during a year
  ([NAHMS Dairy 2014, Milk Quality, Milking Procedures, and Mastitis](https://www.aphis.usda.gov/sites/default/files/dairy14_dr_mastitis.pdf)).
  The same report: about three-quarters of affected cows recovered and stayed, and under 5% died.
- **Subclinical mastitis:** the milk looks normal but the somatic cell count (SCC) is "above 200,000 cells per mL"
  ([University of Kentucky, using SCC to evaluate subclinical mastitis](https://afs.ca.uky.edu/files/usingscc_to_evaluate_subclinical_mastitis_cows.pdf)).
  "If more than 15% of cows exceed 200,000 cells/mL" on a test day, the herd has a problem (same).
- **Each cow carries an SCC** (cells/mL). Healthy: 50,000-150,000 *(proposed)*. Subclinical: 200,000-1,200,000. Clinical: over 1,200,000.
- **CMT score** read from her SCC, by the Kentucky table (**TO VERIFY**: the bands differ between sources; the Afimilk table puts trace at
  200,000-300,000): N under 200,000, T (trace) 200,000-400,000, 1 at 400,000-1,200,000, 2 at 1,200,000-5,000,000, 3 over 5,000,000.
- **Bulk tank SCC:** the tank keeps the volume-weighted SCC, as it keeps fat and protein. The Grade "A" limit for individual producer
  milk is **750,000/mL**, and **1,500,000/mL** for goat milk ([PMO 2013, Section 7](https://www.extension.purdue.edu/4-H/_docs/2013-pmo-final-section-7-grade-a-standards.pdf)).
  Over it the tank reads `HIGH SCC` (a warning, not a fault; nothing is sold, so there is no penalty beyond the warning) *(proposed)*.
- **Risk:** the background hazard (above) times the risk factors (below). The dirty-stall fraction counts double here (environmental
  mastitis). A cow in her first 30 days in milk has twice the risk *(proposed; real incidence is highest in early lactation, **TO VERIFY**)*.
- **Contagious at milking:** a cow milked on a stall's unit after a cow with clinical or chronic mastitis that day is exposed, with a
  5% chance of infection *(proposed)*. **Post-milking teat dip** cuts new infections: the National Mastitis Council puts it at "70
  percent or more" when every teat of every cow is dipped after every milking ([NMC fact sheet, via Feedstuffs](https://www.feedstuffs.com/nutrition-health/nmc-updates-teat-disinfection-fact-sheet)).
  The game takes 70% off both the contagious chance and the environmental hazard for cows dipped that day.
- **Course:** subclinical first; 1 in 3 goes clinical within 3 real days *(proposed)*. Clinical: clots in the milk, hard quarter,
  fever in some. Untreated, a clinical case cures itself in 10 real days half the time *(proposed)*, otherwise it becomes **chronic**:
  SCC stays over 400,000 and she flares again. Treated with intramammary tubes: cured in 3 real days *(proposed)*.
- **Cost:** while clinical her milk is **abnormal milk**, which may not go into the tank (the PMO's abnormal milk rule; Section 7 above).
  The parlor diverts it (below). Subclinical and chronic cows give 5% less milk *(proposed)*.

### 2. Bovine respiratory disease
- **Incidence:** "16.2 percent" of cattle placed in feedlots ([NAHMS Feedlot 2011, BRD info sheet](https://www.aphis.usda.gov/sites/default/files/feed11_is_respdis.pdf)),
  and "about 75% of BRD cases are reported to occur within the first 40 to 55 days after arrival" (review in
  [Smith et al. 2023, Vet. Sci.](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC10052932/), **TO VERIFY**: snippet).
- **Game:** a **stress window of 45 real days** opens when an animal is tamed, moved to new housing, or weaned *(the trigger list is
  proposed)*. In it the hazard is the one that gives 16.2% over 45 days (0.0039 a real day). Outside it, calves and heifers keep a
  tenth of that *(proposed)*; adult cows are not at risk *(proposed)*.
- **Contagious:** each sick animal in the same housing adds to everyone's hazard (the spread rule, below).
- **Signs:** cough, head low, eats less, fever over 39.3 C (dairy) or 39.1 C (beef) (MSD table, below).
- **Course:** 2-5 real days incubation *(proposed)*; sick 7 real days untreated *(proposed)*, 1 in 4 of those goes chronic (poor
  growth for life) *(proposed)*. Treated with the injection: recovered in 3 real days *(proposed)*.
- **Cost:** intake and growth x 0.75 while sick *(proposed)*; health falls 1 point a game day to the floor.
- **Vaccine:** the respiratory vaccine (a modified-live IBR/BVD/PI3/BRSV vaccine) given at least 14 real days before the stress
  halves the risk *(proposed; real efficacy studies disagree)*.

### 3. Neonatal scours
- **Real weight:** in dairy heifer calves, 7.8% died before weaning and "scours, diarrhea and other digestive problems accounted for
  56.5%" of those deaths ([NAHMS Dairy 2007, heifer calf health](https://www.aphis.usda.gov/sites/default/files/dairy07_ir_calfhealth.pdf),
  via a trade summary, **TO VERIFY**). The share of calves that get it is **NOT FOUND** in what loaded; the game uses 20% of calves
  in the first 30 days *(proposed)*.
- **Risk:** dirty pen (double), cold (the existing cold rule for piglets and chicks, double), and halved if the dam was vaccinated
  (CDT for ewes and does, the respiratory and scours vaccine for cows) in the 6 weeks before birth *(proposed)*.
- **Colostrum:** calves that fail passive transfer "had a 40.3% chance of becoming ill" (NAHMS data quoted by a breeding company,
  **TO VERIFY**). Calves in Farmings suckle their dam, so colostrum is assumed; an orphan (dam gone) has double the risk *(proposed)*.
- **Treatment:** oral electrolytes, no withdrawal. Untreated it lasts 7 real days and costs growth; treated, 3 *(proposed)*.

### 4. Foot rot
- **Cattle (lameness):** lameness was reported in 16.8% of cows in a year, second to mastitis
  ([NAHMS Dairy 2014, Part III, via the same mastitis report](https://www.aphis.usda.gov/sites/default/files/dairy14_dr_partiii.pdf), **TO VERIFY**).
  Game: foot rot uses that rate, with the dirty-stall factor doubled. Not contagious in cattle *(proposed simplification)*.
- **Sheep and goats (contagious foot rot):** "Moisture is essential for transmission"; "A soil temperature of 50–70 F is ideal, while at
  below 40 F very little transmission occurs" ([MSD Vet Manual, contagious footrot in sheep](https://www.msdvetmanual.com/musculoskeletal-system/lameness-in-sheep/contagious-footrot-in-sheep)).
  The bacterium lives off the foot "a maximum of 7 days" ([Agriculture Victoria](https://agriculture.vic.gov.au/biosecurity/animal-diseases/sheep-diseases/footrot-in-sheep/about-footrot-in-sheep), **TO VERIFY**: snippet).
  - **Game:** a pasture where an infected sheep grazed is infective for 7 real days. Sheep on it pick it up when it is wet (raining, or
    rained in the last real day) and the biome is between 10 and 21 C *(Minecraft biome temperature mapped to degrees: proposed)*.
  - So **resting a pasture for 7 real days** (half a game day) cleans it, as on a real farm.
- **Footbath:** "10% wt/vol zinc sulfate", at least 6 cm deep, sheep standing in it at least 10 minutes, "once every 14 days during the wet
  season" (MSD, above). Game: a pass through a charged footbath cures a case in 3 real days and protects for 14 real days *(protection
  length proposed from the 14-day interval)*. Cattle use the same bath (real cattle baths often use copper sulfate or formalin instead;
  one solution keeps it simple, decided on recommendation).
- **Signs:** a limp (walk speed x 0.7, a drawn limp), eats less. **Cost:** milk and growth x 0.9 *(proposed)*. Chronic if untreated
  for 21 real days *(proposed)*.

### 5. Clostridial enterotoxaemia (sheep, goats)
- **Real:** "overeating disease", from a sudden change to a rich, grain-heavy diet; the CDT toxoid protects. Ewes and does get a booster
  "four weeks before lambing or kidding" so lambs get antibodies in colostrum; lambs get a first dose at 6-8 weeks and a booster 4 weeks
  later; vaccinating lambs "before 6 weeks of age is completely ineffective"
  ([MSU Extension, CDT vaccinations](https://www.canr.msu.edu/news/cdt_vaccinations_for_sheep_and_goats);
  [MSU, overeating disease](https://www.canr.msu.edu/news/overeating-disease-in-sheep-making-sense-of-vaccination-schedules)). Adults: once a year.
- **Game trigger:** the feeder's grain share (the feed mill's analysis) rising by more than 20 points within 3 real days *(proposed)*.
  Unvaccinated sheep on it fall ill 1 in 10 *(proposed)*; vaccinated ones 1 in 100 *(proposed)*.
- **Real course:** often sudden death. **In the game it is a severe illness**: down and not eating for 2 real days, health to the floor,
  then slow recovery (question 1 asks whether disease may kill).

### 6. Swine erysipelas
- **Real:** incubation "24 to 48 hours"; raised "diamond shaped areas" on the skin in the acute form, but not always; viral disease such
  as PRRS can set it off ([3tres3 pathology atlas](https://www.3tres3.com/en/pathology-atlas/diamond-shaped-skin-lesions_84); [The Pig Site](https://www.thepigsite.com/disease-guide/erysipelas-diamond-lesion), **TO VERIFY**).
  Vaccine: "2 doses 3-5 weeks apart with the second dose administered 2-4 weeks prior to breeding" (label quoted by a vet site, **TO VERIFY**).
- **Game:** 5% of unvaccinated pigs a year *(proposed)*, contagious within a pen run; sick pigs show pink diamond marks on the coat (a
  texture overlay, no lesions drawn beyond that), fever, off feed. Treated with the injection, recovered in 3 real days. Two vaccine doses
  cut the risk by 80% *(proposed)*.

### 7. Coccidiosis (poultry)
- **Real:** *Eimeria* protozoa in the small intestine; called "the most common cause of death for young chicks" ([Myers Poultry](https://www.myerspoultry.com/mareks-disease-coccidiosis-in-chickens), **TO VERIFY**).
- **Game:** birds under 8 weeks *(proposed)* in a coop whose litter is dirty (the coop's litter rule) get it at 2% a real day per dirty
  share *(proposed)*; contagious within the coop. Signs: hunched, ruffled, off feed. Cost: no growth while sick. Treatment: the oral
  coccidiostat (a dose in the coop's water trough treats every bird drinking from it for 5 real days) *(proposed)*.

### 8. Marek's disease (chickens)
- **Real:** vaccinated at the hatchery "either in-ovo at day 18 of the chicken embryo incubation or at day of age via subcutaneous (SC)
  injection"; "vaccinating older birds is not an effective means to prevent Marek's disease"; vaccination "does not stop infection but
  decreases tumour formation" ([UTIA D185](https://utia.tennessee.edu/publications/wp-content/uploads/sites/269/2023/10/D185.pdf);
  [Farmers Weekly](https://www.fwi.co.uk/livestock/poultry/vaccination-keeps-marek-s-disease-under-control), **TO VERIFY**).
- **Game:** a hatchery with Marek's vaccine in its slot vaccinates each chick as it hatches (one dose a chick, a vial is 100 doses
  *(proposed)*). A broody hen's chicks are not vaccinated. Unvaccinated chickens get it between 6 and 30 weeks old at 10% *(proposed)*;
  vaccinated 1% *(proposed)*. Signs: a bird that sits with one leg forward and the other back (the classic sign; drawn as a sitting pose),
  stops laying. It does not recover (chronic). Not contagious between birds in the game (the virus is everywhere in dust, so the
  hatchery dose is what matters) *(proposed simplification)*.

### 9. Strangles (horses)
- **Real:** incubation 3-14 days; nasal shedding for 2-3 weeks; some recovered horses carry it, on average for 9.2 months; new horses
  isolated about three weeks; a horse is cleared after three negative tests over two to three weeks
  ([EquiManagement](https://equimanagement.com/articles/equine-strangles-management-and-prevention/); [dvm360](https://dvm360.com/view/equine-strangles-diagnosis-treatment-and-options-prevention-proceedings), **TO VERIFY**).
- **Game:** a horse tamed in the corral arrives carrying it 1 time in 20 *(proposed)*. Contagious within the stable and paddock.
  Signs: cough, nasal discharge (a readout line, nothing drawn), fever over 38.2 C, off feed, no racing. Course 3 real weeks; 1 in 10
  recovered horses becomes a carrier for 270 real days (the 9.2 months), showing nothing but spreading it. The vet kit's swab finds a
  carrier; three clean swabs a week apart clear it. Vaccine halves the risk *(proposed)*. Antibiotics are not given (real advice is mixed;
  decided on recommendation: rest, isolation and time).

### 10-12. Notifiable diseases (opt-in world rule; questions 2 and 3)
| Disease | Real | Game source | Signs |
|---|---|---|---|
| Foot-and-mouth | reportable foreign animal disease (CFSPH, above); incubation 2-14 days **TO VERIFY** | 1 in 2,000 wild cattle, sheep, goats and pigs tamed *(proposed)* | fever, drooling, limping, milk drops; no blisters drawn |
| HPAI | reportable; the control area reaches "at least 10 km" past the infected premises, an infected zone of 3 km and a buffer of 7 km, movement "by permit only" ([USDA APHIS, HPAI zones](https://www.aphis.usda.gov/sites/default/files/hpai_zones.pdf); [Indiana BOAH](https://www.in.gov/boah/species-information/avianbirds/highly-pathogenic-avian-influenza/response-zones)) | wild ducks or geese within 16 blocks of a coop or pond, 1 chance in 5,000 a real day each *(proposed)* | birds stop laying, sit hunched, many fall ill at once |
| African swine fever | reportable (CFSPH) | wild pigs tamed, or wild pigs on the farm's pig pasture, 1 in 2,000 *(proposed)* | fever, off feed, red skin |

**When the vet kit finds one**, the animal's readout says `SUSPECT NOTIFIABLE DISEASE: report it` and the kit offers **Report**
(sneak + use). What reporting does, and what not reporting does, is question 3. Recommended:
- **Reported:** a **standstill** on every Farmings housing within **192 blocks** *(proposed: 3 km cannot fit a Minecraft farm; 12 chunks
  is a farm's reach)*. No animal may enter or leave housing in the zone, the corral stops taming, no eggs leave the hatchery, no straws are
  filled. Sick animals recover over 14 real days *(proposed)*; there is **no culling**. The standstill lifts 28 real days after the last
  case recovers *(proposed)*. The zone shows on the controllers' readouts and in the herd book.
- **Not reported:** it spreads to every housing within 192 blocks at 10x the normal contagion and lasts until it burns out *(proposed)*.

## How disease spreads
- **Groups:** animals share air and floor with their **housing unit**: a barn, a sheep shed (and its pasture), a coop (and its run), a
  pig barn's pen run, a stable (and its paddock), an isolation pen. Animals out of any housing share with those within 8 blocks
  *(proposed)*. Nothing passes between two housing units except: mastitis through a shared parlor's units, foot rot through a shared
  pasture, and the notifiable diseases.
- **Each game-tick batch** (the once-a-second tend the controllers already run) each susceptible animal is infected with chance
  `1 - exp(-(h0 + beta x I / N) x R x d)`, where `h0` is the background hazard, `I / N` the share of its group that is sick or carrying,
  `beta` the disease's contagion *(proposed per disease: BRD 0.10, foot rot in sheep 0.05, erysipelas 0.05, coccidiosis 0.10, strangles
  0.15, notifiable 0.30, a real day)*, `R` the risk factors, and `d` the real days since the last batch.
- **Deterministic:** every roll is drawn from a generator seeded by the world key, the animal's herd ID, the disease and the batch tick,
  as breeding seeds are, so a game test with the same world gives the same cases.
- **Immunity:** a recovered animal is immune to that disease for 180 real days *(proposed)*, except mastitis and foot rot (no immunity,
  as in real herds) and Marek's (chronic).

### Risk factors (`R`, multiplied)
| Factor | Value | Ties to |
|---|---|---|
| **Genetics: HEALTH trait** (0-100) | `2^((50 - score) / 50)`: score 50 is x1, 100 is x0.5, 0 is x2 *(proposed)* | the genome (GENETICS.md); finally gives HEALTH a job |
| Genetics: HARDINESS (sheep, goats, pigs, horses) | the same curve, for foot rot only *(proposed)* | the genome |
| Dirty stall, pen, bedding or litter | `1 + dirty` (x2 fully dirty); doubled again for mastitis, foot rot, scours and coccidiosis | `Hygiene`, the shed and stable bedding, coop litter |
| Underfed or underwatered (fed share under 0.8) | x1.5 *(proposed)* | the feed and water rules |
| Ration score under 0.5 | x1.25 *(proposed)* | feed mill rations |
| Cold (piglets, chicks, the existing cold rule) | x2 for scours and coccidiosis | heat lamps, brooders |
| Vaccinated, in date | x(1 - efficacy) for that disease | the vaccines below |
| Teat dipped this milking | x0.3 for mastitis | the parlor |
| Footbath in the last 14 real days | x0.3 for foot rot *(proposed)* | the footbath |

**Decided on recommendation:** the HEALTH trait is a strong lever. Real heritability of disease resistance is low (clinical mastitis
is commonly put at a few percent, **TO VERIFY**), so on a real farm management matters more than breeding; the game lets a breeder see
the effect within a few generations, and the doc and the Field Manual say it is a game rule.

## What the player sees
- **Signs, without a kit:** a sick animal walks slowly with its head low, lies down more, and eats less (its feeder share falls). BRD,
  strangles and the notifiable diseases add a **cough** (sound). Foot rot adds a **limp**. Marek's adds the sitting pose. Erysipelas adds
  the coat marks. Nothing else is drawn (no gore).
- **The readout** (empty hand) lists the signs it can see: `coughing`, `lame`, `off feed`, `clots in milk` (at milking).
- **Vet kit** (examine): rectal temperature against the species' normal range, the signs, and a **diagnosis** (the disease, its stage,
  days sick), its vaccinations and the end of any withdrawal. Normal rectal temperatures from the MSD table
  ([MSD Vet Manual](https://www.msdvetmanual.com/multimedia/table/normal-rectal-temperature-ranges)):

  | Animal | Normal, C |
  |---|---|
  | Dairy cow | 38.0-39.3 |
  | Beef cow | 36.7-39.1 |
  | Sheep | 38.3-39.9 |
  | Goat | 38.5-39.7 |
  | Pig | 38.7-39.8 |
  | Horse | 37.2-38.2 |
  | Chicken | 40.6-43.0 |

  Ducks, turkeys, quail, geese and guinea fowl use the chicken range *(proposed; not in the table)*. A fever is 1.0-2.0 C over the top of
  the range by severity *(proposed)*.
- **CMT paddle:** used on a cow, ewe or doe in milk: the score (N, T, 1, 2, 3) and her SCC band.
- **Red leg band** drawn on any animal under a milk or meat withdrawal, put on by the treatment itself.
- **Controllers:** each housing's readout adds a line: `2 sick, 1 in withdrawal, 3 vaccines due`.
- **Herd book:** a **Health** section on each animal's page: cases (date, disease, outcome), treatments (drug, date, withdrawal end),
  vaccinations (vaccine, doses, booster due), last SCC and CMT, quarantine dates. The page is refreshed when the animal is recorded, as
  now. Animal state carries the record (the last 16 events, *proposed*), so nothing is lost between recordings.

## The vet cycle
The real herd health visit, done by the player *(question 4 asks whether a vet character visits instead)*:
1. **Herd check:** the vet kit used on a housing controller writes a **herd check** page into the herd book in the player's other hand
   (or chat, without one): every animal in that housing that is sick, under withdrawal (with the time it ends), due a vaccine or booster,
   due a recheck (3 real days after a treatment), or in quarantine (with its release time).
2. **Examine** each listed animal with the kit; CMT the cows with clots.
3. **Treat** (one item, one course), **isolate** (sneak + use with the kit sends the animal to the nearest isolation pen in hospital
   mode with room), or **leave** it (some cases cure themselves).
4. **Record:** automatic: every exam, treatment and vaccination goes on the animal.
5. **Withdrawal:** milk and meat are held until the times on the band (below).
6. **Recheck** and **vaccinate** when the herd check says so.

A full cycle on an 8-cow barn takes a minute or two each game day or two; it is the farm's routine, not a chore every few seconds.

## Treatment and withdrawal
Drug classes, never brand names. Withdrawal times are the label's, run on the animal's clock.

| Item | Real counterpart | For | Milk withdrawal | Meat withdrawal | Source |
|---|---|---|---|---|---|
| `mastitis_tube` | intramammary cephapirin sodium, one tube per affected quarter, repeated in 12 h | mastitis | **96 h** | **4 days** | label via [Minnesota Dept. of Agriculture](https://www.mda.state.mn.us/sites/default/files/inline-files/ToDAY-Lactating-Dairy-Cows-2.21.pdf) |
| `antibiotic_injection` | long-acting oxytetracycline, 200 mg/mL | BRD, foot rot, erysipelas, scours with fever | **10 days** | **28 days** | an Irish-authorised 200 mg/mL L.A. label: "28 days for meat and offal but 10 days for milk" (**TO VERIFY**, EU product database snippet); US labels: 28 days meat and not for lactating dairy cattle ([FDA FOI](https://animaldrugsatfda.fda.gov/adafda/app/search/public/document/downloadFoi/1980)) |
| `electrolytes` | oral rehydration salts | scours | none | none | |
| `coccidiostat` | oral anticoccidial in water | coccidiosis | none (eggs: 0, *proposed*) | 5 days *(proposed)* | NOT FOUND (labels vary) |
| `zinc_sulfate` (footbath charge) | 10% zinc sulfate | foot rot | none | none | MSD, above |
| `teat_dip` | post-milking disinfectant (chlorine-based) | mastitis prevention | none | none | NMC, above |

- **One item is one course** (the 12-hour second tube is part of the item) *(decided on recommendation: dosing by weight adds clicks,
  not choices)*.
- **Decided on recommendation:** the injection uses the EU milk withdrawal so it can be given to cows in milk; the US rule (not for
  lactating dairy cattle) is noted in the Field Manual.
- **Milk during withdrawal or clinical mastitis** never reaches the tank: the parlor milks her last and **diverts her milk to the waste
  milk can** (a new parlor part). This is question 5 (recommended: automatic). Waste milk is poured away from the can.
- **Meat during withdrawal:** an animal killed before its meat withdrawal ends drops no meat ("condemned: drug withdrawal" in the action
  bar). Wool, leather, feathers and down still drop. *(Decided on recommendation: condemnation is what an inspector does.)*
- **Eggs during withdrawal:** eggs laid under a withdrawal are dropped as `withdrawn_egg`, which can't be eaten or hatched *(proposed;
  the coccidiostat is the only poultry drug and has none, so this is for completeness)*.

## Vaccines
| Item | Real counterpart | Animals | Schedule | Efficacy (risk x) |
|---|---|---|---|---|
| `vaccine_cdt` | clostridial C and D toxoid with tetanus | sheep, goats | dam 4 weeks before birth; lambs and kids 6-8 weeks + 4 weeks; adults yearly (MSU, above) | enterotoxaemia x0.1, neonatal scours in her young x0.5 *(proposed)* |
| `vaccine_respiratory` | modified-live IBR/BVD/PI3/BRSV | cattle | calves at weaning (or on arrival) + yearly *(proposed)*; cows 6 weeks before calving for the scours effect *(proposed)* | BRD x0.5, scours in her calf x0.5 *(proposed)* |
| `vaccine_erysipelas` | erysipelas bacterin | pigs | 2 doses 3-5 weeks apart, 2nd 2-4 weeks before breeding; yearly | x0.2 *(proposed)* |
| `vaccine_mareks` | turkey herpesvirus (HVT) vaccine | chickens | at hatch, in the hatchery only | x0.1 *(proposed)* |
| `vaccine_strangles` | intranasal live strangles vaccine | horses | 2 doses 3 weeks apart; yearly *(proposed)* | x0.5 *(proposed)* |

- A dose not in date (booster missed) gives nothing. The herd check lists every booster due.
- **Cold chain:** vaccines carry the food spoilage clock (`docs/food.md`) and keep only in the cold room; outside it they are useless after
  1 game day *(proposed; labels say store refrigerated, **TO VERIFY**)*. That makes the cold room (on Wildcatter power) part of the vet
  cycle. *(Decided on recommendation.)*
- No vaccines for the notifiable diseases (real vaccination for these is a government decision, not a farm one).

## Blocks, items and multiblocks
**Items:** `vet_kit` (a tool; 256 uses *(proposed)*), `cmt_paddle` (reusable) and `cmt_reagent` (4 tests a bottle *(proposed)*),
`mastitis_tube`, `antibiotic_injection`, `electrolytes`, `coccidiostat`, `teat_dip`, `zinc_sulfate`, the five vaccines, `withdrawn_egg`.

**Blocks:**
| Block | Real counterpart | What it does |
|---|---|---|
| `footbath` | a concrete footbath set in a race or lane | 1 x 1, set in a cow lane, a pasture gate path, a stable door. Filled with water (bucket, or a Wildcatter water pipe tops it up as the trough does) and charged with a `zinc_sulfate` bag (100 passes a bag, *proposed*). Every animal that walks through is treated and protected. Its readout: water, charge left. |
| `waste_milk_can` | a dump bucket or dump line at the parlor | a parlor part next to the milk line. Holds 200 L *(proposed)*. Full: banded cows are not called (fault `WASTE CAN FULL`). Right-click empties it. |
| `teat dip` in the parlor | the dip cup at each stall | not a block: `teat_dip` goes in the parlor controller's slot, one bottle dips 50 cows *(proposed)*. Empty: the readout says `NO TEAT DIP`. |
| **Isolation pen** (`isolation_controller`, `isolation_pen`) | a quarantine or hospital pen, separate from the herd | a multiblock: `docs/multiblocks/isolation-pen.md`. |

**Ties:** power only where real equipment draws it: the parlor's divert (the parlor's own supply) and the cold room for vaccines. The
footbath takes a Wildcatter water pipe. **No PLC points** in this build: Farmings has none yet, and the first one deserves its own step
(Left out).

## Recipes (all *(proposed)*; grids go in `docs/recipes.md` at build time)
| Item | From | Why |
|---|---|---|
| `vet_kit` | leather, glass (thermometer), iron ingot, string | a bag with a thermometer and stethoscope |
| `cmt_paddle` | Wildcatter plastic sheet | a four-cup plastic paddle |
| `cmt_reagent` (4) | glass bottle, water bottle, Wildcatter sodium sulfate bag | the real reagent is a detergent (sodium lauryl sulfate) **TO VERIFY**; the nearest Wildcatter chemical stands in |
| `antibiotic` (intermediate, 4) | whey (Farmings), brown mushroom, sugar | antibiotics are fermentation products; lactose was a carbon source for early penicillin fermentation **TO VERIFY** |
| `mastitis_tube` (4) | antibiotic, Wildcatter plastic sheet | |
| `antibiotic_injection` (2) | antibiotic, glass bottle, iron nugget | |
| `electrolytes` (4) | sugar, water bottle, Wildcatter salt (or vanilla kelp if Wildcatter has no salt item; checked at build) | glucose and sodium |
| `coccidiostat` (4) | antibiotic, water bottle | |
| `teat_dip` (4) | glass bottle, water bottle, Wildcatter chlorine (64 bottles from one chlorine ton container) | hypochlorite dips |
| `zinc_sulfate` (4) | Wildcatter zinc ingot, Wildcatter sulfuric acid | zinc + sulfuric acid is how it is made |
| vaccines (4 each) | glass bottle, egg, Wildcatter plastic sheet, and a marker: wool (CDT), leather (respiratory), porkchop (erysipelas), feather (Marek's), hay (strangles) | many vaccines are grown in eggs |
| `footbath` | gray concrete, iron ingot, bucket | |
| `waste_milk_can` | iron ingot, bucket | |
| `isolation_controller`, `isolation_pen` | as the holding pen: iron bars, gray concrete; controller adds a redstone and steel | |

## Progression and the quest book
After "Bring in cattle" (`cattle`), in Farmings' chapter:
- **Herd health** (`vet_kit`): craft a vet kit and examine an animal; milestone `farmings:examined`. Reward: 2 electrolytes.
- **Isolation pen**: form one; milestone `farmings:isolation_pen`. Requires Herd health.
- **Vaccinate**: give any vaccine; milestone `farmings:vaccinated`. Requires Herd health and `wildcatter:plastic` (vaccines take plastic).
- **Milk quality**: CMT a cow and dip a herd (a teat dip bottle in a parlor); requires `parlor`.
- **Footbath**: charge one with zinc sulfate; requires Herd health and the Wildcatter quest that first makes sulfuric acid (id checked
  at build, as `wildcatter:fertilizer` was for the spreader).

The **Field Manual** gets an "Animal health" chapter: diseases (one page each, with the signs table), the vet cycle, treatments and
withdrawal, vaccines, the isolation pen, the footbath, notifiable diseases; each figure checked by `checkManual` against this doc.

## World rules (on Wildcatter's World Rules screen, as Tailings' rules are)
| Rule | Default | What it does |
|---|---|---|
| `farmingsDisease` | **on** in new worlds; **off** in worlds saved before this build *(as Wildcatter does for new rules)* | the endemic diseases |
| `farmingsNotifiableDisease` | **off** | the three notifiable diseases and standstills |
| `farmingsDiseaseDeaths` | **off** | question 1 |

## Code shape (for the build plan, not a commitment)
- Plain Java `husbandry/health`: `Disease` (the table above as data), `Course` (stages: EXPOSED, SICK, CHRONIC, CARRIER, RECOVERED with
  times), `Spread` (the batch roll), `RiskFactors`, `Withdrawal`, `VaccineSchedule`, `Scc`. Under `verifyPlainJava`. Unit tests on every
  formula, the time conversion and determinism.
- On each animal entity, saved with it (CLAUDE.md: health survives save and chunk unload): disease states, SCC, vaccinations, withdrawal
  end ticks, the stress window, the last 16 events.
- The housing controllers' existing tend runs the spread for their group; the parlor reads withdrawal and SCC; the hatchery vaccinates;
  the corral starts the stress window and the arrival carriers.

## Sounds and art
- **Sounds:** a cough for cattle, sheep and goats, pigs and horses (four, from Ludo); a footbath splash; the CMT paddle swirl; the vet
  kit's case opening.
- **Art (Ludo, through the tools scripts):** the items above; the footbath, waste milk can, isolation pen floor and controller blocks
  (true-size models from the parts library where they are parlor-like); a red leg band overlay; the erysipelas coat marks; a head-low
  sick pose and a limp for each species' model; the Marek's sitting pose.

## Game tests and client scenes
**Game tests (NeoForge):**
- `health_state_survives_save` (a sick cow under withdrawal, vaccinated, through save and chunk unload)
- `cmt_reads_scc`, `bulk_tank_scc_warning`
- `withdrawal_milk_diverted`, `withdrawal_ends_on_time`, `withdrawal_meat_condemned`
- `teat_dip_cuts_contagion` (seeded)
- `brd_window_after_taming`, `contagion_stays_in_group` (two barns, one sick: the other stays clean)
- `isolation_pen_forms`, `quarantine_releases_after_30_days`, `kit_sends_sick_to_hospital_pen`
- `vaccine_lowers_risk`, `booster_out_of_date_gives_nothing`, `vaccine_spoils_out_of_cold_room`
- `footbath_treats_and_protects`, `rested_pasture_clears_foot_rot`
- `hatchery_vaccinates_chicks`, `strangles_carrier_found_by_swab`
- `disease_rule_off_no_cases`; with the notifiable rule on: `notifiable_report_standstill`, `standstill_lifts`
- unit: `RiskFactorsTest` (HEALTH curve), `SpreadTest` (determinism), `WithdrawalTest` (clocks), `SccTest`.

**Client scenes (Fabric client game test; screenshots, and recordings for motion, sent to Tim):**
- `health`: a coughing, head-low cow beside a well one; the vet kit readout; a CMT result; the red leg band; the herd book health page and
  a herd check page.
- `footbath` (recording): sheep walking through a charged footbath from the shed to the pasture.
- `parlor_divert` (recording): a banded cow milked last into the waste milk can.
- `isolation` : a tamed heifer walking from the corral into the isolation pen; the release readout.
- `limp` and `marek` (short recordings) for the poses.

## Left out
- **Deaths from disease** unless question 1 says otherwise; **culling, euthanasia, post-mortems** and anything gory, always.
- **Internal parasites** (worms, FAMACHA scoring), **metabolic diseases** (milk fever, ketosis), **heat stress**: each a build of its own.
- **Johne's, bovine TB testing, BVD persistently infected calves, bluetongue and vectors, scrapie**: chronic or vector diseases with
  incubations of months to years. **Genetic resistance loci** (sheep PRNP for scrapie, the chicken B21 haplotype for Marek's) go to
  Ideas with them.
- **Diseases that pass to players** (zoonoses). Players never get sick.
- **A handling chute or headgate** to restrain animals for treatment; a later build may add one with hoof trimming.
- **PLC points** (a milk meter's conductivity, a herd check over the network).
- **Dosing by weight, antibiotic resistance, a milk residue test kit** (the parlor knows; question 5).
- **Waste milk as a digester feedstock** (real co-digestion; needs a sourced biogas yield).
- **SCC's effect on cheese yield** in the creamery.
- **An NPC vet** unless question 4 says otherwise.

## Questions for Tim
(Also in `docs/questions/animal-health.md`, recommended option first.)
1. **Can disease kill an animal?** No: health stops at the floor and production falls; a `farmingsDiseaseDeaths` world rule (off)
   turns real case fatality on / always on at real rates / never, no rule.
2. **Which diseases are on by default?** Endemic on, notifiable off (opt-in) / all on / all off (opt-in).
3. **What a reported notifiable disease does:** a standstill on housing within 192 blocks, no culling / leave notifiable diseases out /
   offscreen culling of the affected group with a restock wait.
4. **Who is the vet?** The player, with a vet kit / an NPC vet who visits on a schedule / both (the kit plus a visit that does a herd check).
5. **Milk during withdrawal:** the parlor diverts it automatically to a waste can / the player must band the cow, and a missed one spoils
   the whole tank / milked by hand into a bucket, parlor refuses her.
6. **Scope:** the twelve diseases above / cattle first (mastitis, BRD, scours, foot rot), the rest later / twelve plus parasites.
7. **Pace:** real annual incidences on each species' clock / half the real rates / double.
8. **Quarantine for wild-caught animals:** optional, but tamed animals can arrive carrying disease that only quarantine catches / required:
   housing refuses a tamed animal until it has done 30 real days in an isolation pen / no quarantine, a hospital pen only.

## Built, 2026-11-19 (branch `animal-health-spec`; Tim answered all eight questions with the recommended option)
- **Engine** (plain Java `husbandry/health`): `Disease`, `Vaccine`, `Drug`, `Risk`, `Scc`, `HealthRecord`, `HealthModel`,
  `IsolationLayout`; unit tests `HealthModelTest` (the 24.8% and 16.2% hazards, the HEALTH curve, CMT bands, saving, withdrawal clocks,
  determinism, the BRD window's rate over 2,000 calves, vaccines, temperatures).
- **In the world** (`dev.farmings.health`): every species (cattle, sheep, goats, pigs, horses, all poultry) is a `HealthHost`; its record
  is saved on the animal as one string; its once-a-second bookkeeping runs the step. Sick animals walk 20% slower (lame 30%), cough
  (cattle, sheep and goats, pigs, horses; Ludo sounds), lose 1 health a game day to a floor of a fifth of their maximum, and give less milk,
  growth and eggs. The readout shows the signs, the red leg band (withdrawal) and quarantine.
- **World rules** `disease` (on in new worlds), `notifiable_disease` (off), `disease_deaths` (off), saved with the world and shown on
  Wildcatter's World Rules screen. A world with Farmings data saved before this build starts with all three off.
- **Items, blocks, recipes, quests and the Field Manual chapter "Animal health"** as specified above; icons, block faces and sounds from Ludo
  (`tools/process_art.py`, `tools/process_sounds.py`).

### Where the build differs from the spec (decided on recommendation; for Tim, `docs/questions/animal-health.md` "Build decisions")
- **The waste milk can is a block placed within 4 blocks of the parlor controller**, not a part of the parlor's layout, so existing parlors
  keep forming. Withheld cows are milked when their turn comes, not last.
- **Chronic mastitis sits at 1,000,000 cells/mL** (was 700,000 in the first draft of `Scc`), so a herd's chronic cows can push a tank over
  the 750,000 limit *(proposed)*.
- **Dirt** counts for cattle (the stall) and sheep (the bedding); goats, pigs, horses and birds have no per-animal dirt figure yet, so the
  factor is 1 for them. **Cold** (piglets, chicks) and **a dam's vaccination** (the scours factor) are not wired: the factors exist in the
  engine, the inputs are not read yet. **Weaning** does not open the BRD window; taming and a move to new housing do.
- **Not drawn:** the head-low pose, the limp animation, the red leg band and the erysipelas marks. The slower walk, the cough and the
  readout carry those signs for now (Known gaps).
- **No `/farmings rule` command:** Farmings has no commands; the rules are set on the World Rules screen at world creation.
- **The footbath** is a Wildcatter water user (a pump's pipe fills it), as the trough is.

### Sourcing, 2026-11-19
Searched again for the TO VERIFY lines (Agriculture Victoria's footrot page: refused the fetch, 403; NAHMS Dairy 2014 Part III: timed out;
the Myers Poultry page: no response). The marks stay as they are: the 7-day pasture survival, lameness 16.8%, the coccidiosis line, the
NAHMS 2007 calf figures, the CMT bands, the strangles figures, the EU oxytetracycline milk withdrawal. The figures that rest on a primary
source are unchanged: NAHMS 24.8% and 16.2%, PMO 750,000 and 1,500,000, MSD temperatures and footbath, MSU CDT schedule, NMC 70%, the
cephapirin label, the Idaho 30 days.

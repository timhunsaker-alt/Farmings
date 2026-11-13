# Horses

**Status: spec, 2026-11-13 (Step 18; Tim's answers of the same day).** Marks as in `docs/animals/sheep.md`: a real figure carries a source;
*(proposed)* is a game choice; **TO VERIFY** is a secondary source or a quote from a search snippet.

Tim (2026-11-13): "since horses are not used for farming they can still be ridden but we should also breed for racing." His choices:
**Thoroughbred, Quarter Horse, Arabian**, replacing vanilla horses; ridden as vanilla horses are; **real genes plus traits** for speed
(the MSTN "speed gene" plus heritable speed and stamina, conditioning and age); **training and fitness**; a **stable**
(`multiblocks/stable.md`); **coat colour genes**; a **stud book**; races on **a track you build** (`multiblocks/race-track.md`) with
no betting or prizes (nothing is sold).

## Time scale
Horses live on the livestock scale, **1 game day = 15 real days** (growth, pregnancy, conditioning). **Races and riding are real time:**
a horse that runs 402 m in 20 s does so in 20 seconds of play.

| Stage | Real | Game days at 1:15 |
|---|---|---|
| Oestrous cycle | 21 days | 1.4 |
| Gestation | 340 days | 22.7 |
| Weaning | 150 days | 10 |
| Peak racing age | 4.45 years | 108 |

## Breeds
| Breed | Code | Height | Mare / stallion kg | MSTN founders | Coat founders |
|---|---|---|---|---|---|
| Thoroughbred | TB | 15.2-17 hh (16.1, 163 cm) | 522 | C at 45% *(proposed)* | bay and brown mostly, some chestnut, a few grey |
| Quarter Horse | QH | 14.3-16 hh (15.1, 155 cm) | 454 | C/C 83%, C/T 14%, T/T 3% | chestnut (sorrel) common, bay, some dun and palomino |
| Arabian | ARA | 14.1-15.1 hh (14.3, 150 cm) | 408 | T/T 90%, C/T 10% *(the other 10% proposed)* | bay, grey common, chestnut, black |

- **Sizes:** Thoroughbred "15.2 to 17 hands", "1,000 to 1,300 pounds" (**TO VERIFY**, snippet: [Mad Barn](https://madbarn.com/thoroughbred-horse-breed-profile));
  Quarter Horse "14.3 to 16 hands", "Approximately 1000 pounds" (**TO VERIFY**, snippet: [Horse Illustrated](https://www.horseillustrated.com/horse-breeds-horse-height-explained));
  Arabian "between 14.2 and 15.2 hands", "between 800 and 1,000 pounds" (**TO VERIFY**, snippet: [Oklahoma State](https://breeds.okstate.edu/horses/arabian-horses.html)).
  The game uses the middles: 1,150 lb (522 kg), 1,000 lb (454 kg), 900 lb (408 kg), for both sexes *(proposed)*.
- **MSTN founders:** Quarter Horses "an excess of C alleles (0.90; C/C, 0.83; C/T, 0.14; T/T, 0.03)"; Egyptian Arabians "an excess of T/T
  (0.90) genotypes" ([Hill et al. 2010, PLoS ONE](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0008645)). The
  Thoroughbred population frequency is NOT FOUND in the paper's quoted results; 45% C is *(proposed)*, between its sprinters and stayers.
- **Coat founders:** shares are *(proposed)*. "Nearly 90 percent of all Thoroughbreds registered with The Jockey Club are a variation of
  brown" (**TO VERIFY**, a fan site); Arabian grey and Quarter Horse dun are NOT FOUND as frequencies.

## Reproduction
| Quantity | Value | Source |
|---|---|---|
| Oestrous cycle | **21 days** | "a mare will have a 21-day estrous cycle" ([extension.org, mare reproduction](https://horses.extension.org/mare-reproduction/)) |
| Heat | **5.5 days** (4-7) | "usually 4 to 7 days" (same) |
| Breeding season | **1 Apr - 31 Oct** (long-day breeders) | "breeding season lasting from April–October in the Northern Hemisphere" (**TO VERIFY**, snippet: [Reprod. Biol. Endocrinol.](https://rbej.biomedcentral.com/track/pdf/10.1186/1477-7827-9-130)). The sheep calendar (day 0 = 1 October) |
| Gestation | **340 days** (338-343) | "average length of pregnancy in the mare is 338 to 343 days" (**TO VERIFY**, snippet: [AAEP](https://aaep.org/horsehealth/expectant-mare-assuring-health-and-well-being-pregnant-mare)) |
| Conception | **60%** a service | "mean per-cycle pregnancy rate of 59.9%" (**TO VERIFY**, snippet: Newmarket Thoroughbreds via [Mad Barn](https://madbarn.com/research/reproductive-efficiency-of-intensively-managed-thoroughbred-mares-in-newmarket)) |
| Foal heat | **8 days** after foaling (7-9) | "usually occurs seven to nine days after foaling" (**TO VERIFY**, snippet: [Univ. of Missouri G2790](https://extension.missouri.edu/g2790)) |
| Puberty | 18 months | "mares reach puberty at about 18 months of age" (**TO VERIFY**, snippet: MSD Vet Manual) |
| First breeding | **3 years** *(proposed: NOT FOUND as a sourced age)* | |
| Foal | **one**; twins not modelled (born early and rarely survive: AAEP) | |
| Birth weight | **10%** of the dam's weight | "about 10% of the mare's weight" (**TO VERIFY**, [Horse Canada](https://horse-canada.com/breeds/foal/)) |
| Weaning | **150 days** (4-6 months) | "foals typically wean between four-to-six months of age" (**TO VERIFY**, snippet: [Michigan State](https://cvm.msu.edu/hospital/services/equine-services/for-owners/general-conditions-and-seeing-your-vet/breeding/caring-for-a-new-foal/weaning)) |
| Growth | 43% of mature at 6 months, 64% at 12, 77% at 18, 86% at 24, mature at 5 years *(the last proposed)* | MSD Vet Manual growth table ([nutritional requirements of horses](https://www.msdvetmanual.com/management-and-nutrition/nutrition-horses/nutritional-requirements-of-horses-and-other-equids)), percentages *derived* |

- **Stages:** foal (to weaning), filly / colt, mare (has foaled), stallion (colt of 3 years), gelding (castrated with shears as a colt, as
  rams and bucks).
- **Service:** a stallion serves a mare in heat in the same stable or paddock; each service conceives 60% of the time. **Stallion straws**
  work as bull straws (`breeding.md`). Thoroughbred racing rules forbid AI; the game doesn't model racing rules.
- **Lethal white overo** (EDNRB) is not in the game: none of the three breeds carries the overo pattern.

## Feed and water
- **Dry matter:** **2.25%** of body weight a day: "The average maximum daily DM intake by equine animals is usually 2–2.5%– body weight"
  (MSD, above).
- **Water:** **25 L** a day for a 500 kg horse at rest, "21–29 L of water per day"; doubled on a day of racing or training *(proposed:
  MSD says much more working in heat)*. Game: 0.05 L per kg of body weight.
- **Manure:** **5.1% of body weight** a day, fresh: "A 1,000-pound horse produces about 31 pounds of feces and 2.4 gallons of urine daily",
  "around 51 pounds of total raw waste per day"; stall waste C:N "20:1 to 30:1" ([Penn State, horse stable manure management](https://extension.psu.edu/horse-stable-manure-management)).

## Speed: genes, traits, age and fitness
**What a horse can run.** A horse's pace over a distance is its breed's record pace scaled by everything below. Anchors:

| Breed | Sourced pace | Source |
|---|---|---|
| Thoroughbred | 402 m at 70.76 km/h (19.66 m/s); 2,012 m in 1:59.40 (16.85 m/s); 2,414 m in 2:24 (16.76 m/s) | Winning Brew, "fastest speed for a race horse" (**TO VERIFY**, snippet: [Guinness](https://www.guinnessworldrecords.com/world-records/fastest-speed-for-a-race-horse)); Secretariat's Derby and Belmont records ([America's Best Racing](https://www.americasbestracing.net/the-sport/2023-the-numbers-1973-triple-crown-winner-secretariat)), paces *derived* |
| Quarter Horse | 402 m in 20.27 s (19.84 m/s) | First Moonflash, 2009 (**TO VERIFY**: [horseracingsense](https://horseracingsense.com/quarter-horse-racing-distance-history-records/), not AQHA); AQHA's 2005 record :20.686 cited via [eBarrelRacing](https://www.ebarrelracing.com/articles/industry/a-long-goodbye-sets-440-world-record-in-mbna-new-mexico-challenge) |
| Arabian | **92%** of the Thoroughbred pace *(proposed)*; Quarter Horses and Thoroughbreds "were substantially faster than Arabians" ([Nielsen et al. 2006, PMID 17402407](https://pubmed.ncbi.nlm.nih.gov/17402407/), abstract; no speeds given). Endurance: 160 km in 15 h 37 m (Tevis Cup, [teviscup.org](https://teviscup.org/70th-tevis-cup-100-mile-ride-anniversary-celebrates-successful-event/)) |

- **The distance curve** *(proposed shape through the sourced points)*: a breed's record pace at distance d falls with distance: for the
  Thoroughbred 19.66 m/s to 402 m, 16.85 at 2,012 m and 16.76 at 2,414 m, joined by straight lines in log distance, then 1% less per
  doubling beyond. The Quarter Horse curve starts at 19.84 and falls 25% faster *(proposed: a sprinter)*; the Arabian curve is 92% of
  the Thoroughbred's and falls 40% slower *(proposed: a stayer)*, down to the Tevis pace (2.85 m/s) at 160 km.
- **MSTN, the speed gene:** each genotype has a best distance, "C/C mean = 6.2±0.8 f; C/T mean = 9.1±2.4 f; and T/T mean = 10.5±2.7 f"
  (Hill et al. 2010): **1,250 m, 1,830 m and 2,110 m**. Away from its best distance a horse runs **3% slower per natural-log unit** of
  distance ratio *(proposed)*, so a C/C horse beats a T/T horse at 400 m and loses at 2,400 m, as Hill found ("C/C horses are suited to
  fast, short-distance races"; "T/T horses have greater stamina").
- **Traits:** SPEED moves pace by up to ±3% and STAMINA flattens or steepens the distance fall by up to ±20% *(proposed swings)*. Racing
  speed is weakly heritable: "sprint (h2 = 0.124), middle-distance (h2 = 0.122) and long-distance races (h2 = 0.074)" ([Sharman & Wilson
  2023, Heredity](https://www.nature.com/articles/s41437-023-00623-8.pdf)); the founders' trait spread is set so a sire's score passes
  on weakly *(proposed: the genome's own inheritance)*. Best race distance is highly heritable (0.61, Velie et al. 2015, **TO VERIFY**),
  which the MSTN gene carries.
- **Age:** **peak at 4.45 years**: "peak racing age is 4.45 years"; "At age 2 a horse is predicted to be 26 points slower than at peak
  age"; "A horse beaten by two lengths in a 6 furlong sprint will receive a speed figure 5 points lower"; after the peak "the typical
  decline is 6 lengths for sprints and 9 1/2 lengths for routes" over five years ([Gramm & Marksteiner 2010, J. Equine Sci.](https://www.chrb.ca.gov/rule_law/Scientific_Material/Effect_of_Age_TR_Perf_Journal_of_Equine_Science_RM.pdf)).
  At 2.4 m a length *(proposed)*, 26 points is 10.4 lengths over 1,207 m: **2.1% slower at 2 years** *(derived)*, straight to 0 at 4.45,
  then **0.25% slower a year** *(derived from 6 lengths in 5 years over 6 f)*. Foals and yearlings can't be ridden (under 2 years).
- **Fitness (conditioning), 0 to 1:** an untrained horse runs **5% slower** than a fit one *(proposed: NOT FOUND as a percentage)*.
  - Work builds it: each kilometre run at over half its pace adds 0.02, at most 0.06 a real day *(proposed)*.
  - Rest keeps it 6 weeks, then it falls to nothing over the next 6: "no significant reduction in VO2max until after 6 wk detraining"
    (Standardbreds, **TO VERIFY**, snippet: J. Appl. Physiol. 1996); Thoroughbred gains "almost disappeared after detraining for 6 week"
    (**TO VERIFY**, snippet). The game holds 42 real days, then loses it linearly over 42 *(proposed shape)*.
  - An underfed horse (stable fed share under 1) gains no fitness *(proposed)*.
- **Within a run:** a horse runs at its pace for the distance it has galloped since it last rested *(proposed)*: fresh, it sprints;
  after 1,600 m without a breather it is at its mile pace. Walking for 30 s resets it.
- **Rider weight** is not modelled (the "pound per length" rule and jockey weights are NOT FOUND).

**In Minecraft:** 1 block = 1 m. A ridden or AI-run horse's movement is set so it covers its pace in blocks a second *(calibrated by a game
test)*. Elite founders run near the records; an ordinary horse runs at 90-97% of them *(proposed founder spread)*. Jump strength stays
vanilla's middle *(proposed)*.

## Coat colour genes
| Locus | Alleles | Source |
|---|---|---|
| Extension (MC1R) | E dominant, e/e chestnut | "The allele responsible for this pattern is designated as E, the capital letter indicating it is dominant" ([Univ. of Kentucky Gluck, colour testing](https://gluck.mgcafe.uky.edu/color-testing)) |
| Agouti (ASIP) | A: bay with E; a/a with E: black | "A horse that has at least one dominant "A" allele will be bay if it also possesses at least one "E" allele" (Gluck) |
| Cream (SLC45A2) | Cr dominant dilution: one copy palomino (chestnut) or buckskin (bay); two cremello or perlino | "The cream gene, designated Cr, is a dominant dilution gene" (Gluck) |
| Dun (TBX3) | D dominant, no dosage effect | "There is no dosage effect" (Gluck) |
| Grey (STX17) | G dominant; born the base colour, greys with age | "Gray is caused by a dominant mutation"; "Grey horses are born with their base colour" ([PLoS Genet. 2013](https://journals.plos.org/plosgenetics/article?id=10.1371/journal.pgen.1003248)). Grey by 6 years *(proposed pace)*; melanoma not modelled |
| Roan (KIT) | Rn dominant | "It is a dominant color pattern" (Gluck) |
| Sabino-1 (KIT) | SB1: white markings, more with two copies | "Homozygous sabinos may have more extensive white than heterozygous horses" (Gluck) |

Plus a face and leg marking seed *(drawn only)*. Founders by breed are *(proposed)*: TB E/A bay mostly with 15% e and 5% G; QH 40% e/e
and a few Cr and D; Arabian 30% G, no Cr or D.

## Wild horses
- **Spawns:** Farmings horses take vanilla horses' spawn entries (plains, savanna) with their group sizes; one stallion a group.
- **Breeds by biome** *(proposed)*: plains Quarter Horse and Thoroughbred; savanna Arabian.
- **Taming:** in the corral on hay, 7 real days (taming.md). Vanilla taming by riding is off: a wild horse throws its rider. Once tame it
  takes a saddle and is ridden as a vanilla horse.

## Meat
Horses are not killed for meat in Farmings *(proposed: Tim's "not used for farming")*; a horse that dies drops its saddle and nothing else.

## The stud book
A **stud book** item, like the herd book (`item/HerdBookItem`), records up to 64 horses: name or herd id, breed, sex, MSTN genotype,
colour, sire and dam, the grandparents, fitness, and race records (best time at each distance, races run, wins).

## Open points
- Breed-standard heights and weights from AQHA, the Jockey Club and the Arabian Horse Association; Arabian flat-race times.
- A percentage for fitness, the weight-carried rule, colour frequencies by breed, Thoroughbred MSTN frequency.

## Built, 2026-11-13: the animal
- **Code:** `entity/HorseEntity` extends vanilla `AbstractHorse`, so saddles, horse armour, riding and jumping are vanilla's; rules in
  `husbandry/Equine` (reproduction, growth, feed) and `husbandry/Gallop` (speed and fitness), `HorseBreed`, `HorseStage`; genes
  `HorseLocus`/`HorseGenes`/`HorseLook`; `SPEED` and `STAMINA` appended to the core traits (`Species.HORSE`); renderer `client/HorseRenderer`
  with textures from `tools/horse_textures.py`; stallion straws.
- **How speed works in play:** `Gallop.Runner.pace(d)` is the best *average* pace over d metres (the record curve by breed share, times
  the MSTN, SPEED, age and fitness factors). A horse x metres into a run goes at `speedAt(x)`, one metre over the time that metre takes
  on the `time(d) = d / pace(d)` curve, so a race of any length takes exactly its `time` *(derived: this is what makes the MSTN rule hold
  over a whole race; a horse simply running each metre at `pace(x)` let C/C horses win at 2,414 m too)*. The server counts metres
  galloped (moving faster than half the horse's 402 m pace) and resets them after 30 s slower; it syncs `speedAt` to the rider's side,
  where the ridden speed is that over 43.17 blocks a second per point of movement speed (the player's 0.1 walks 4.317 m/s,
  **TO VERIFY** in a timed ridden lap, Step 18 part 8).
- **Differences from the spec above:**
  - **SPEED** is 1.0 at a score of 1000, 0.97 at 500 and 0.94 at 0 (the ±3% swing around the middle), so only an elite horse at its
    best distance runs the record; **STAMINA** flattens the fall with distance by 20% at 1000 and steepens it 20% at 0.
  - **MSTN founders** draw each allele by the breed's C share (Hardy-Weinberg, *derived*): Quarter Horse C 0.90, Thoroughbred 0.45,
    Arabian 0.05. Coat founders by allele share *(proposed, derived from the phenotype shares above)*: TB e 0.387, A 0.70, G 0.025; QH
    e 0.632, A 0.60, Cr 0.05, D 0.05, Rn 0.03, SB1 0.03; ARA e 0.45, A 0.65, G 0.163, SB1 0.05. Roan and sabino are in the QH and Arabian
    founders at these small shares *(proposed)* so the loci show up.
  - Beyond 2,414 m the curve keeps falling 1% a doubling; the Tevis pace is not used (galloped distance resets with each rest).
  - **Model:** vanilla's horse model with Farmings textures (18 base coats; roan, sabino, star, blaze, socks and three grey stages
    drawn over), not a new model, so vanilla saddles and armour fit. Foals are the adult model scaled by the cube root of their share of
    mature weight *(proposed)*. The model's four legs share one texture, so socks are on all four or none.
  - Spawned horses are 3 to 10 years old *(proposed)*. Vanilla's random speed, jump and health rolls are off: walking speed 0.225, jump
    0.7 and health 22.5, vanilla's middles.
  - A horse fed vanilla horse food (wheat, apples, hay, sugar, golden carrots) is healed as vanilla's; golden carrots don't breed it.
- **Tests:** unit `GallopTest`, `HorseGeneticsTest`; game tests horse_spawns_and_survives_save, horse_lifecycle, mare_foals,
  wild_horse_replaces_vanilla, horse_gallop_builds_fitness, stallion_straw_inseminates_mare, sprinter_and_stayer. Client scene `horses`.
- **Not yet:** the stable and training (next part); the race track and stud book (part 7).

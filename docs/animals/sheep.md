# Sheep

**Status: spec, 2026-11-10 (Phase 4, Tim's answers of the same day).** Real figures carry a source. A figure marked
*(proposed)* has no source and is a game choice. **TO VERIFY** marks a figure from a secondary source (breeder or association
listing) that is used until a better one is found.

Tim's choices (2026-11-10): breeds **Merino, Suffolk, Dorper, East Friesian**; FarmChain's sheep gene list plus milk yield; an
own sheep model with coat genes; a sheep shed with pasture; a shearing shed and a wool mill; wool for vanilla blocks, clothing,
farm parts and yarn; East Friesian ewes milked in the same parlor through sheep stalls. Chickens are a later pass.

## Time scale
The cattle rule holds: **1 game day = 15 real days** (`TimeScale`, decided 2026-11-07). Output per game day is one real day's
output.

| Stage | Real | Game days at 1:15 |
|---|---|---|
| Oestrous cycle | 17 days | 1.1 |
| Gestation | 147 days | 9.8 |
| East Friesian lactation | 221 days | 14.7 |
| Shearing interval | 365 days | 24.3 |
| First breeding (ewe lamb) | 7-10 months, by weight | about 14-20 |

## Sexes and stages
- **Female:** lamb, ewe lamb (weaned, not yet bred), ewe (has lambed), retired ewe.
- **Male:** ram lamb, ram, wether (a castrated male raised for meat or wool). Castration uses the cattle tool and rules.
- Lamb sex is 50/50. The cattle tutorial guarantee (first two heifers) is **not** copied *(proposed)*: twins make a flock grow
  fast enough.
- A ram does not give milk; a wether does not breed.

## Reproduction
| Quantity | Value | Source |
|---|---|---|
| Oestrous cycle | about 17 days | [WikiVet: oestrous cycle, sheep](https://en.wikivet.net/Oestrous_Cycle_-_Sheep); [UMD sheep reproduction guide](https://sites.google.com/terpmail.umd.edu/sheepreproductionguide/female-puberty-cyclicity) |
| Gestation | 147 days on average (142-152) | same sources. Game uses 147 for every breed (no per-breed figure from a primary source). |
| First breeding weight | about 65% of mature weight (70% for traditional fine-wool breeds such as Rambouillet) | [Montana State sheep handbook](https://animalrangeextension.montana.edu/sheep/reproduction.html); SDSU gives 65-75% ([breeding ewe lambs](https://extension.sdstate.edu/breeding-ewe-lambs)) |
| First breeding age | 7-10 months; weight matters more than age | [SDSU](https://extension.sdstate.edu/breeding-ewe-lambs) |

**Built rule:** a ewe lamb can first be bred at **7 months** and **65% of mature weight** (70% for Merino), whichever comes later.
Puberty for the readout is the same point.

**Breeding season.** Real sheep are short-day breeders. Minecraft has no seasons, so ewes cycle all year *(proposed; open point
for Tim if seasons are ever added)*.

**Service.** A ram breeds a ewe in heat in the same pen or pasture (natural service). No straws for sheep in this pass.

## Breeds
| Breed | Code | Use | Mature ewe kg | Lambs per ewe lambing | Greasy fleece kg | Micron | Staple cm/yr | Clean yield | Coat |
|---|---|---|---|---|---|---|---|---|---|
| Merino | MER | fine wool | 50 | 1.25 | 4-6 (5) | under 24 (21) | 6-10 (8) | 65-75% (70%) | white, rams horned |
| Suffolk | SUF | meat | 104 | 1.8 | 2.2-2.7 (2.45) | 28-33 (30) | 8-10 (9) | 50-62% (56%) | white, black head and legs, polled |
| Dorper | DOR | meat, hair | 77 | 1.5 | 0.5-1, shed | hair | short | n/a | white, black head and neck, polled (Tim, 2026-11-12) |
| East Friesian | EFR | dairy | 77 | 2.2 | 4-5.5 (4.75) | 29-33 (31) | 10-15 (12.5) | 55-65% (60%) | white, polled, bare "rat tail" |

Values in brackets are what the game uses (the middle of the range).

- **Merino:** ewe weight 50 kg from fine-woolled and control Merino ewes at Grootfontein (50.45 and 51.01 kg,
  [S. Afr. J. Anim. Sci. 37(1)](https://scielo.org.za/pdf/sajas/v37n1/05.pdf)), close to the CSIRO Temora trial (S.A. Merino
  49.0 kg, [CSIRO EA9800288](https://publish.csiro.au/AN/EA9800288)). Fleece, micron, staple and lambing from the
  [Canadian Co-operative Wool Growers breed page](https://www.wool.ca/breeders-directory/sheep/merino) (confirmed 2026-11-14; its ewe
  weight, 90-115 kg, is far above the research figures and is not used). Micron 20-22 also from
  [Wool measurement](https://en.wikipedia.org/wiki/Wool_measurement) listings. Yield 65-75% (was 45-55%, a Rambouillet range):
  [NMSU Guide B-409](https://pubs.nmsu.edu/_b/B409/), Table 4, "Merino (Medium)", 21-22 µm, clean wool yield "65-75". The
  same table gives 60-70% for finer Merino.
  Rams horned, ewes polled: [Duijvesteijn et al. 2018, Genet. Sel. Evol.](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5964914/).
  Skin folds are drawn on the model only.
- **Suffolk:** fleece, micron, staple, lambing 180% and colour from the
  [Canadian Co-operative Wool Growers](https://www.wool.ca/breeders-directory/sheep/suffolk). Ewe weight 104 kg (was 90, from
  Farmers Weekly's South African 80-100 kg): [OMAFRA, choosing breeds](https://www.ontario.ca/page/choosing-breeds-producing-profitable-market-lambs),
  "104.4 kg (230 lb) — Suffolk", an estimate of average mature ewe weight for U.S. breeds. The wool growers list 100-115. Yield 50-62% from
  [Kentucky ASC-127](https://publications.mgcafe.uky.edu/sites/publications.ca.uky.edu/files/asc127.htm) and breed listings.
- **Dorper:** lambing 150%, hair coat that sheds with 0.5-1 kg of wool, black head and neck, from the
  [Canadian Co-operative Wool Growers](https://www.wool.ca/breeders-directory/sheep/dorper). Ewe weight 77 kg (was 85, the
  middle of the wool growers' 70-100): [OMAFRA](https://www.ontario.ca/page/choosing-breeds-producing-profitable-market-lambs),
  "77.1 kg (170 lb) — Border Leicester, Corriedale, Dorper, East Friesian, ...". The breed society lists "95.0 kg" for ewes of
  6 teeth and over in good condition ([Dorper Sheep Breeders' Society of SA](https://dorpersa.co.za/breed-standards/)).
  The coat is partly hair and partly wool, grown in winter and shed in spring
  ([Cornell Small Farms](https://smallfarms.cornell.edu/2011/01/dorper-sheep-truths-and-myths)). Horns: polled in the game (Tim,
  2026-11-12: no horns). The breed standard allows them: "A developed horn base or small horns are the ideal" and "Heavy horns
  are undesirable but permissible" ([Dorper SA breed standards](https://dorpersa.co.za/breed-standards/)).
- **East Friesian:** ewes 160-180 lb (73-82 kg), grease fleece 9-12 lb (4-5.5 kg), 29-33 micron, yield 55-65%, staple 4-6 in
  (10-15 cm), polled, open white face: American Sheep Industry Association material quoted at
  [apparelsearch.com](https://www.apparelsearch.com/education_research_sheep_east_friesian.htm). Litter size 2.2 lambs per
  lambing: [UW-Extension Guide to Raising Dairy Sheep](https://sheboygan.extension.wisc.edu/files/2010/08/Guide-to-Raising-Dairy-Sheep.pdf).
  The bare tail is the breed's known mark *(drawn only)*.

**Litter size in the game.** The breed figure is the mean number of lambs. Each lambing rolls a single, twins or triplets with the
odds below, which give that mean *(the odds are proposed; the means are sourced)*:

| Mean | Single | Twins | Triplets |
|---|---|---|---|
| 1.25 (Merino) | 75% | 25% | 0% |
| 1.5 (Dorper) | 50% | 50% | 0% |
| 1.8 (Suffolk) | 30% | 60% | 10% |
| 2.2 (East Friesian) | 15% | 50% | 35% |

Crosses take the breed-weighted mean, and the fertility trait moves it by up to ±0.2 *(proposed)*.

## Wool
- **Fleece growth.** A full fleece is the breed's greasy fleece weight after 365 days of growth, and staple length grows at the
  breed's staple cm per year (values above). Growth is linear in the game *(proposed simplification)*. The wool_yield trait moves
  weight by up to ±20% and fleece_quality moves micron by up to ±2 *(proposed)*.
- **Shearing:** once a year is the normal interval (the fleece weights above are annual). A sheep can be shorn any time; a part
  fleece weighs its share of the year. A fleece is **ready** at 300 days or more of growth *(proposed)*.
- **Dorper:** sheds. Shedding sheep have no shearable fleece; the coat drops as a little loose wool (0.5-1 kg a year, as above)
  once a year *(proposed timing)*. Crosses carry the hair/wool gene below.
- **Clean yield** (scouring): greasy weight × the breed's yield; the wool mill uses this. Yield ranges 35-60% in general and rise
  with fibre diameter ([Kentucky ASC-127](https://publications.mgcafe.uky.edu/sites/publications.ca.uky.edu/files/asc127.htm)).
- **Grade** by micron, in the US blood-grade style: fine under 22.05, half blood 22.05-24.94, three-eighths 24.95-27.84,
  quarter 27.85-30.99, low quarter and coarser 31.00 and over (was 22.0 / 25.0 / 28.0 / 31.0). Source:
  [SDSU Wool Evaluation 101](https://extension.sdstate.edu/sites/default/files/2022-12/P-00227-V2.pdf), Table 1, "Requirements
  for wool grades": 64's "20.60-22.04" (fine), ½ blood "22.05-23.49" to "23.50-24.94", ⅜ blood "24.95-26.39" to "26.40-27.84",
  ¼ blood "27.85-29.29" to "29.30-30.99", low ¼ blood from "31.00". Skirtings are one grade lower.

## Milk (East Friesian)
| Quantity | Value | Source |
|---|---|---|
| Lactation | 221 days, 365 kg milk | [Croatian East Friesian study (via SDU)](https://opact.sdu.edu.tr/vufind/SummonRecord/FETCH-doaj_primary_oai_doaj_org_article_7450502e9f794b478f775022cd194fe93) |
| Fat | 5.68% | same |
| Protein | 4.92% | same |
| Lactose | 4.59% | same |

- Daily yield follows the cattle Wood's curve shape scaled so 221 days sum to 365 kg *(proposed shape; the total is sourced)*.
  Only ewes with East Friesian blood give a milkable yield; others feed their lambs. Yield scales by the breed's EFR share and by
  the milk_yield trait (±20%, *proposed*). Crosses with 0% EFR give no parlor milk.
- A ewe nursing lambs still gives parlor milk in the game (real dairy flocks wean early) *(proposed)*.

## Feed and water
- **Dry matter intake:** 2-3% of body weight for sheep generally, 3.5% for a ewe nursing a single and 4.5-5% for twins
  ([Oregon State ANS 312](https://courses.ecampus.oregonstate.edu/ans312/nine/sheep_11_trans.htm)). Game: 2.5% at maintenance,
  3.5% lactating with one lamb, 4.5% with two or more.
- **Hay:** the cattle hay block (100 kg fresh, 85% dry matter) is reused.
- **Water:** 1.0-1.5 gallons for every 4 lb of dry matter eaten
  ([SDSU: sheep water requirements](https://extension.sdstate.edu/sheep-water-requirements-and-quality-testing)), so
  2.1-3.1 L per kg DM. Game uses 2.6 L per kg DM (the middle).
- **Grazing:** improved pasture carries about 9.9-17.3 ewes per hectare (CSIRO trial,
  [EA9730013](https://publish.csiro.au/EA/EA9730013)). At 13 ewes per hectare a ewe has about 770 m² for a year, so a real
  square metre yields roughly 0.6 kg of dry matter a year (worked out from the 50 kg Merino at 2.5%). A Minecraft pasture that
  honest would be enormous, so grazing is scaled: **one grass block grazed bare gives 1.5 kg of dry matter**, and it grows back as
  vanilla grass spreads *(proposed)*. A 50-90 kg ewe needs 1.25-2.25 kg DM a day, so about one or two grass blocks per game day.
- **Underfeeding:** less growth, less wool and milk, lower health; never death from a missed day (cattle rule).

## Housing
| Quantity | Value | Source |
|---|---|---|
| Dry ewe | 1.0-1.2 m² | [FAO](https://www.fao.org/3/X6542E/X6542E04.htm); Ontario gives 0.93 m² minimum ([OMAFRA](https://www.ontario.ca/page/evaluating-farm-resources-and-sheep-production-systems)) |
| Pregnant or lactating ewe | 1.4-1.5 m² | [OMAFRA](https://www.ontario.ca/page/evaluating-farm-resources-and-sheep-production-systems) |
| Ewe with lambs (to 6 weeks) | 2.0-2.2 m² | UK guidance quoted by [Agriland](https://www.agriland.co.uk/farming-news/sheep-housing-what-you-should-and-shouldnt-do/) |
| Lambing jug | 4 × 4 ft (1.5 m², min) or 5 × 5 ft for large ewes | [Penn State G5](https://webarchive.library.unt.edu/web/20090116053823/http://server.age.psu.edu/extension/factsheets/g/G5.pdf) |
| Jugs needed | about 10% of a 100-ewe flock | same |
| Feeder space | 9-20 in (23-51 cm) per head | same |

**Game:** one pen floor block (1 m²) is a bit under the space a pregnant ewe needs, so **3 pen blocks hold 2 sheep** (1.5 m² each)
and a lamb under weaning age counts as nothing. One lambing pen (1 block, a game size for a 1.5 m² jug) holds one ewe and her lambs.
One hay rack block serves 2 sheep (50 cm each, the top of the Penn State range rounded to a block).

## Meat
- **Dressing percentage:** about 50% of live weight for planning (54% shorn, 52% unshorn)
  ([Montana State: understanding dressing percentage](https://montana.edu/extension/sanders/understanding%20the%20dressing%20percenage.pdf)).
- **Retail cuts:** bone-in cuts are 65-75% of the carcass ([UW-Madison livestock extension](https://livestock.extension.wisc.edu/articles/how-much-meat-should-a-lamb-yield)).
- **Items:** lamb chops from animals under a year, mutton from older sheep. One item per 2.5 kg of retail meat *(proposed;
  beef uses 10 kg, so a 90 kg Suffolk gives 12 items and a 40 kg lamb 5)*.

## Coat genes (sheep loci)
| Locus | Alleles (dominance) | Source |
|---|---|---|
| Agouti (ASIP) | white/tan A^Wt > ... > non-agouti a (black or brown) | [OMIA 000201-9940](https://omia.org/2555); Sponenberg, [sheep color genetics (Cornell)](https://bpb-us-e1.wpmucdn.com/blogs.cornell.edu/dist/5/6103/files/2020/02/sheep-color-genetics.pdf) |
| Extension (MC1R) | dominant black E^D > wild type E+ | same; E^D masks agouti |
| Brown (TYRP1) | black B+ > brown b (moorit, recessive) | Sponenberg notes |
| Spotting | spotted S > plain s (usually dominant) | Sponenberg notes; amount of white varies *(proposed range)* |
| Head and legs | black head/legs pattern (Suffolk, Dorper): recessive to plain white face *(proposed mode)* | breed descriptions above; mode not found |
| Horns (RXFP2) | three alleles: polled H^P, sex-limited H^L (rams horned, ewes polled: the Merino), horned H+. Polled is dominant; a polled-carrier ram grows scurs; H^L over H+ is *(proposed)* | the three-allele model as reviewed in [Duijvesteijn et al. 2018](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5964914/); [OMIA 000483-9940](https://omia.org/830) |
| Coat type | wool W vs hair/shedding h, incomplete dominance (W/h sheds partly) *(proposed; Dorper shedding is polygenic)* | [Cornell Small Farms](https://smallfarms.cornell.edu/2011/01/dorper-sheep-truths-and-myths) |
| Pattern seed | spot shapes only | as cattle |

Black-based colours fade to grey in many sheep (Sponenberg); the renderer greys black wool a little with age *(proposed)*.
Founders: all four breeds are white (A^Wt). Suffolk and Dorper carry the black head pattern, Merino carries sex-limited horns (H^L/H^L), the others are polled. The Dorper's standard calls for a horn base or small horns
(see Breeds), so its polled founders are an open point.
A small share of founders carry recessive non-agouti and brown *(proposed: 5% each)*, so coloured lambs appear now and then, as on
real farms.

## Economy
Nothing is sold (cattle rule, 2026-11-07).

## Open points
- Seasons, if ever added (sheep breed in short days).
- Per-breed gestation (147 used for all).
- Better primary sources for the breed weights marked TO VERIFY.

## Built, 2026-11-10
Everything above is in the game:
- **Rules and genes:** `husbandry/Flock`, `SheepBreed` and `SheepStage`; genetics in `Species`, `SheepLocus`, `SheepGenes` and
  `SheepLook`.
- **The animal:** `entity/SheepEntity`, with its model and renderer in `client/Sheep*`.
- **Buildings and machines:** the shed and pasture (`docs/multiblocks/sheep-shed.md`), the shearing shed (`shearing-shed.md`) and
  the mill (`wool-mill.md`).
- **Products:** wool uses in `docs/wool.md`; sheep milk in `parlor.md` and `creamery.md`.
- **Where the model differs from this spec:**
  - Spotting shows only on coloured sheep.
  - The grey of an old black fleece shows on the shorn fleece, not on the living sheep.
  - Horns follow the three-allele model.

## Loose ends, 2026-11-11 (spec)
### Stillborn lambs
Each lamb at a lambing is rolled on its own; a stillborn lamb isn't spawned.

| Ewe | Chance a lamb is stillborn | Source |
|---|---|---|
| First lambing | **9%** *(derived)* | Lambs of ewe hoggets: 22% died, and 41.1% of the deaths were stillbirths ([PMC8833723](https://pmc.ncbi.nlm.nih.gov/articles/PMC8833723)); 0.22 x 0.411 |
| Later lambings | **1%** *(derived)* | Mature ewes in the same study: 273 of 1,979 lambs died, and "only 7% of deaths were attributed to stillborn"; 0.138 x 0.07 |

- **By litter:** a lamb in a single birth has **0.8x** that chance, and a twin or triplet **1.2x**. This is *(proposed)*: its shape
  follows the study's 19.2% mortality for singles against 28.7% for multiples, a ratio of 0.67.
- **The ewe:** she still lactates for her live lambs. Her intake counts only live lambs. If every lamb is stillborn, she isn't milked as
  a nursing ewe; the East Friesian is still milked in the parlour.
- **Readout:** the lambing record lists each lamb, with stillborn ones marked.

### Breeding season
Sheep are short-day breeders. Ewes "will typically begin cycling when daylight hours become less than 14 after June 21"
([Univ. of Kentucky ASC237](https://publications.mgcafe.uky.edu/sites/publications.ca.uky.edu/files/ASC237.pdf)).
- **The game year:** sheep run at 15:1, so a real year is 365 / 15 = **24.3 game days**.
  - Day 0 of a world is **1 October**, the middle of the season *(proposed)*, so a new flock can breed at once.
  - The shed readout gives the date.
- **Season by breed** (ASC237 Table 1; the Ontario factsheet agrees):

| Breed | Season (ASC237 class) | Game rule |
|---|---|---|
| Suffolk | short (under 4 months) | cycles 1 Sep - 1 Jan ("usually begin cycling in September and continue until January 1") |
| East Friesian | medium (4-6 months) | 15 Aug - 15 Jan (the expanded season ASC237 gives) |
| Merino | long (6-8+ months) | 15 Jul - 15 Mar, 8 months *(proposed: the long class's upper end)* |
| Dorper | not in ASC237 | medium, as East Friesian: 15 Aug - 15 Jan (was long, as Merino). "Dorpers are regarded as intermediate with regard to seasonal expression of oestrus activity (Cloete et al., 2000)" ([Flock & Herd, 2014](https://www.flockandherd.net.au/sheep/reader/reproductive-performance-dorper.html)) |

- **Crossbreds:** a crossbred ewe takes her top breed's season.
- **Outside the season:** a ewe is ANOESTROUS. She doesn't cycle and can't take a straw. A ewe already in lamb carries on.
- **After lambing:** "All ewes are in anestrus for at least 17 to 21 days after lambing" (ASC237). The existing wait after lambing
  stays.

### Sheep dog (Border Collie)
- **The animal:** a working Border Collie, `farmings:sheep_dog`.
  - Height 53 cm at the withers for males, females slightly less ([Kennel Club standard](https://www.kusa.co.za/images/HERDING%20GROUP%202025/Border%20Collie%20-%20KC%20%5B02-2017%5D.pdf)).
  - Weight 14-25 kg for males and 12-19 kg for females (**TO VERIFY**: Wikipedia only; searched for a breed-standard weight again 2026-11-14, the pages did not load). The model is drawn to that size.
- **Wild and taming (taming.md):**
  - Dogs spawn wild in pairs in plains, meadows and windswept hills, the sheep biomes, at weight 2 *(proposed)*.
  - A wild dog keeps 6 blocks from a player with empty hands, and follows raw meat (beef, mutton, chicken, Farmings meats).
  - It settles in a corral on **raw meat** in the bait trough: 0.5 kg a day, one meat item a day. "As a guide, dogs should
    consume 2-3% of their body weight in raw food each day"
    ([Royal Kennel Club](https://www.royalkennelclub.com/health-and-dog-care/health/health-and-care/a-z-of-health-and-care-issues/is-raw-food-good-for-dogs/));
    2.5% of a 20 kg dog is 0.5 kg.
  - 7 real days settled tames it, as for the others.
  - Dogs don't breed in this pass. They don't age past adult and don't die of age.
- **The whistle:** a `shepherds_whistle` item, made from 2 iron nuggets and 1 string.
  - Sneak-use on a block sets the **target**:
    - a sheep shed controller sends the flock to its pen;
    - a pasture gate sends it to the pasture;
    - any other block sends it to that spot (a pen or a field).
  - Use in the air blows the current command to your tamed dogs within 64 blocks. Commands are the trial commands
    ([SARE handout](https://projects.sare.org/media/pdf/E/d/i/Edited-swswhandout-for-SARE-final-notes.pdf)); sneak-use in the air cycles them:
    - **Come bye** (gather): the dog runs out clockwise around every tame sheep within 32 blocks and brings them to you.
    - **Walk up** (drive): it drives them to the target.
    - **Lie down:** it stops where it is and the sheep stand.
    - **That'll do:** it stops working and comes back to heel.
- **How the dog herds:**
  - The dog moves to the far side of the flock from the target, 6 blocks behind it *(proposed)*.
  - Each sheep within 8 blocks of it walks away from it, toward the target. In code, the dog gives each sheep a `sendTo` step toward
    the target.
  - A sheep within 2 blocks of the target is released, and the drive ends when every sheep is in.
  - A shed's members driven to its pen go back under the shed's day rules once there.
- **Range:** open trials work the dog 300-500 yards out (was 200-600, from a snippet). [USBCHA](https://usbcha.com/resources/sheepdog-trials-explained/):
  "working 350 or 500 yards apart", and a dog that "lies down or flanks right when 300 yards away". The game uses 32 blocks for the gather and 64 for hearing the whistle *(proposed: shrunk to Minecraft's
  scale)*.
- **Speed:** a vanilla wolf's speed (NOT FOUND for a working collie; searched for working Border Collie running speed again 2026-11-14, the pages did not load).
- **Not in this pass:** dogs breeding, guarding against wolves, and herding cattle or fowl.

### Built, 2026-11-11 (loose ends)
- **Stillbirths:** each lamb is rolled on its own. A litter fixed by a test is born alive unless the test also forces stillbirths.
- **Seasons:** `Flock.season`, `Flock.inSeason` and `Flock.dayOfYear`. A ewe out of season doesn't cycle and reads ANOESTROUS, and the
  shed readout gives the date.
- **The dog:**
  - The dog is `SheepDogEntity`, on the vanilla wolf frame. Its own black-and-white coat is painted by `tools/dog_texture.py`.
  - The `shepherds_whistle` is made from 2 iron nuggets and a string.
  - The corral's bait trough now holds raw meat too (16 items of 0.5 kg).
  - **Known difference:** only tame sheep are herded, and a herded sheep goes straight for the target by pathfinding. It doesn't flee
    the dog the way a real flock does.
- **Game tests:** lamb_stillborn_not_spawned, ewe_anoestrous_out_of_season, dog_gathers_flock, dog_drives_flock_to_shed,
  dog_commands_and_owners, dog_tamed_in_corral.

### Sourcing pass, 2026-11-12
- Resolved 8 marks: Merino clean yield, Suffolk and Dorper ewe weights, Dorper horns, grade boundaries, Dorper season, the dog's
  daily meat, and the trial range. Values changed: Merino yield 50% to 70%, Suffolk 90 to 104 kg, Dorper 85 to 77 kg, grade
  boundaries, Dorper season long to medium, Dorper not polled.
- Still open: the wool.ca Merino weight note (not used), the dog's weight (AKC and Royal Kennel Club standards give no weight),
  and a working collie's speed (searched kennel club standards and USBCHA). The coat-gene modes were not searched in this pass.
  The other marks are game choices.

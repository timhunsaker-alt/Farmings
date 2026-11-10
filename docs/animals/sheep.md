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
| Merino | MER | fine wool | 50 | 1.25 | 4-6 (5) | under 24 (21) | 6-10 (8) | 45-55% (50%) | white, rams horned |
| Suffolk | SUF | meat | 90 | 1.8 | 2.2-2.7 (2.45) | 28-33 (30) | 8-10 (9) | 50-62% (56%) | white, black head and legs, polled |
| Dorper | DOR | meat, hair | 85 | 1.5 | 0.5-1, shed | hair | short | n/a | white, black head and neck, polled |
| East Friesian | EFR | dairy | 77 | 2.2 | 4-5.5 (4.75) | 29-33 (31) | 10-15 (12.5) | 55-65% (60%) | white, polled, bare "rat tail" |

Values in brackets are what the game uses (the middle of the range).

- **Merino:** ewe weight 50 kg from fine-woolled and control Merino ewes at Grootfontein (50.45 and 51.01 kg,
  [S. Afr. J. Anim. Sci. 37(1)](https://scielo.org.za/pdf/sajas/v37n1/05.pdf)), close to the CSIRO Temora trial (S.A. Merino
  49.0 kg, [CSIRO EA9800288](https://publish.csiro.au/AN/EA9800288)). Fleece, micron, staple and lambing from the
  [Canadian Co-operative Wool Growers breed page](https://www.wool.ca/breeders-directory/sheep/merino) (**TO VERIFY**: its ewe
  weight, 90-115 kg, is far above the research figures and is not used). Micron 20-22 also from
  [Wool measurement](https://en.wikipedia.org/wiki/Wool_measurement) listings. Yield from the Rambouillet range in
  [SDSU Wool Evaluation 101](https://extension.sdstate.edu/sites/default/files/2022-12/P-00227-V2.pdf) (**TO VERIFY** for Merino).
  Rams horned, ewes polled: [Duijvesteijn et al. 2018, Genet. Sel. Evol.](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5964914/).
  Skin folds are drawn on the model only.
- **Suffolk:** fleece, micron, staple, lambing 180% and colour from the
  [Canadian Co-operative Wool Growers](https://www.wool.ca/breeders-directory/sheep/suffolk). Ewe weight 80-100 kg from
  [Farmers Weekly](https://www.farmersweekly.co.za/farming-tips/how-to-livestock/the-basics-of-the-suffolk-breed/), midpoint 90
  (**TO VERIFY**; the wool growers list 100-115). Yield 50-62% from
  [Kentucky ASC-127](https://publications.mgcafe.uky.edu/sites/publications.ca.uky.edu/files/asc127.htm) and breed listings.
- **Dorper:** weight 70-100 kg, lambing 150%, hair coat that sheds with 0.5-1 kg of wool, black head and neck, from the
  [Canadian Co-operative Wool Growers](https://www.wool.ca/breeders-directory/sheep/dorper) (**TO VERIFY**: a South African
  study reports about 51 kg mature ewes). The coat is partly hair and partly wool, grown in winter and shed in spring
  ([Cornell Small Farms](https://smallfarms.cornell.edu/2011/01/dorper-sheep-truths-and-myths)). Polled *(proposed; the breed
  standard was not opened)*.
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
- **Grade** by micron, in the US blood-grade style: fine under 22.0, half blood 22.0-24.9, three-eighths 25.0-27.9,
  quarter 28.0-30.9, low quarter and coarser 31.0 and over. **TO VERIFY** the boundaries against the ASI
  grade table (SDSU Wool Evaluation 101 lists the system). Skirtings are one grade lower.

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
| Horns (RXFP2) | polled P > horned H, sex-influenced: a P/H ram may grow scurs or small horns, a P/H ewe is polled *(rams-horned, ewes-polled pattern of Merino)* | [Duijvesteijn et al. 2018](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5964914/); [OMIA 000483-9940](https://omia.org/830) |
| Coat type | wool W vs hair/shedding h, incomplete dominance (W/h sheds partly) *(proposed; Dorper shedding is polygenic)* | [Cornell Small Farms](https://smallfarms.cornell.edu/2011/01/dorper-sheep-truths-and-myths) |
| Pattern seed | spot shapes only | as cattle |

Black-based colours fade to grey in many sheep (Sponenberg); the renderer greys black wool a little with age *(proposed)*.
Founders: all four breeds are white (A^Wt). Suffolk and Dorper carry the black head pattern, Merino is horned (H/H), the others polled.
A small share of founders carry recessive non-agouti and brown *(proposed: 5% each)*, so coloured lambs appear now and then, as on
real farms.

## Economy
Nothing is sold (cattle rule, 2026-11-07).

## Open points
- Seasons, if ever added (sheep breed in short days).
- Per-breed gestation (147 used for all).
- Better primary sources for the breed weights marked TO VERIFY.

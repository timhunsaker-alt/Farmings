# Goats

**Status: spec, 2026-11-13 (new animals pass; Tim's answers of the same day).** Marks are as in `docs/animals/sheep.md`: a real figure
carries a source; *(proposed)* is a game choice with no source; **TO VERIFY** marks a secondary source, or a quote taken from a search
snippet because the page would not load.

Tim's choices (2026-11-13): breeds **Saanen, Nubian, Boer** (no Angora); goats **share the sheep shed** (their own pens, never mixed
with sheep in one pen), the pasture, the sheep dog and straws; dairy does are milked through the parlor's sheep stalls into a goat
tank; the creamery bottles goat milk and makes **fresh chèvre** (`creamery.md`); wild goats **replace vanilla goats** in the mountains
and are tamed in the corral. Pigs and horses wait.

## Time scale
Goats run on the livestock scale: **1 game day = 15 real days** (`TimeScale`, as cattle and sheep). Output per game day is one real
day's output.

| Stage | Real | Game days at 1:15 |
|---|---|---|
| Oestrous cycle | 21 days | 1.4 |
| Heat | 36 hours | 0.1 |
| Gestation | 150 days | 10 |
| Lactation | 284 days | 18.9 |
| First breeding | 7 months and 60% of mature weight | about 14 |

## Sexes and stages
- **Female:** kid, doeling (weaned, not yet bred), doe (has kidded), retired doe.
- **Male:** buckling, buck, wether (castrated, with the cattle tool, as sheep).
- Kid sex is 50/50.
- A buck gives no milk; a wether does not breed.

## Reproduction
| Quantity | Value | Source |
|---|---|---|
| Oestrous cycle | **21 days** (18-22) | "The estrous cycle in goats is 21 days, on average" ([Merck Vet Manual: puberty and estrus in goats](https://www.merckvetmanual.com/management-and-nutrition/management-of-reproduction-goats/puberty-and-estrus-in-goats)); "approximately every 18 to 22 days" ([NC State, heat detection in meat goats](https://www.ccfruitandnuts.ucanr.edu/sites/default/files/2020-12/341763.pdf)) |
| Heat | **36 hours** | "The mean duration of standing estrus has been reported to be 36 hours" (Merck) |
| Gestation | **150 days** | "Gestation length in the goat is approximately 150 days" ([Langston University](https://goats.langston.edu/?p=732)). One figure for every breed |
| Puberty | does 6-8 months, bucks 4-6 months | Merck: "6–8 months old in does and 4–6 months old in bucks" |
| First breeding | **60% of mature weight**, and not before **7 months** | Merck: "delayed until the animal has reached at least 60% of its mature body weight"; NC State gives 60-75%. The 7 months is the middle of Merck's puberty range *(derived)* |
| After kidding | the doe waits one cycle (21 days) before she cycles again *(proposed: as sheep)* | |

### Breeding season
Goats are short-day breeders: "considered to be short-day breeders because they begin to cycle during the shorter days of fall"
([Langston University](https://goats.langston.edu/?p=732)). The game year is the sheep one (24.3 game days;
day 0 is 1 October; `Flock.dayOfYear`).

| Breed | Source | Game rule |
|---|---|---|
| Saanen | "Dairy breeds such as the Saanen and Alpine are temperate climate Swiss dairy breeds and the prototype of seasonal breeders"; "the breeding season generally extend from September to February" (NC State) | cycles **1 Sep - 28 Feb** |
| Nubian | "The Nubian breed, originally derived from African stock, is probably the least seasonal dairy type goat" (NC State) | **1 Aug - 31 Mar** *(proposed: wider than Saanen; no dates found)* |
| Boer | "The same appears to be true for the Boer breed" (NC State); Boer in Croatia kid every 265 days, "1.38 kiddings/doe/year" ([Đuričić et al. 2012, Vet. Arhiv 82:351](https://hrcak.srce.hr/file/125113)) | **all year** *(proposed: a 265-day interval means kidding out of season)* |

- A crossbred doe takes her top breed's season. Outside it she is ANOESTROUS and can't take a straw; a doe in kid carries on.

### Litter size
| Breed | Mean kids per kidding | Odds (single / twins / triplets) | Source |
|---|---|---|---|
| Saanen | **1.37** first kidding, **1.56** later | first 65 / 33 / 2, later 50 / 44 / 6 *(proposed odds giving the means)* | "Litter size averaged 1.37 for primiparous against 1.50 and 1.62 for multiparous" (**TO VERIFY**, snippet: [researcherslinks, Togo](https://researcherslinks.com/current-issues/Reproduction-and-Milk-Production-Performance/33/1/7423/html); searched again 2026-11-14, the page gave 403/503); 1.56 is the middle of 1.50-1.62 |
| Nubian | **1.39** (was 1.56, as Saanen) | read off the table, about 63 / 34 / 2; FAO gives 66.1 / 30.4 / 3.5 | "1.40 in Sudan; 1.38 in Egypt"; "66.1 per cent single, 30.4 per cent twin, 3.5 per cent triplet" ([FAO, Nubian goat](https://www.fao.org/4/t0376e/T0376E06.htm)); 1.39 is the middle |
| Boer | **1.64** | **43 / 50 / 7** | "single 43 per cent, twin 50 per cent, triplet 7 per cent" ([FAO, Boer goat](https://www.fao.org/4/t0376e/T0376E06.htm)) |

- **First kidding:** every breed's mean is multiplied by 1.37 / 1.56, the Saanen first-to-later ratio *(derived)*; the FAO Boer odds
  are for all does. Odds for a mean are read off the table rows (1.37, 1.56, 1.64) by straight lines, up to 2.0 *(proposed ceiling)*.
- Crosses take the breed-weighted odds; the fertility trait moves the mean by up to ±0.2 *(proposed; as sheep)*.

### Stillborn kids
- **Chance a kid is stillborn: 6%.** German dairy goat farms: "a mean stillbirth rate of 6.4%" (median 5.8%) (Balasopoulou et
  al. 2022, [vetline](https://www.vetline.de/management-and-goat-kid-mortality-on-southern-german-dairy-goat-farms)). The game rounds to 6.
- Litter size scales it as for sheep (single 0.8x, twin or triplet 1.2x) *(proposed)*. A first kidding has no extra risk: the sheep
  study's first-lambing figure is for ewe hoggets and has no goat counterpart *(proposed)*.
- "On average, nearly 14 percent of kids in the U.S. die before weaning" ([Univ. of Arkansas FSA9615](https://www.uaex.uada.edu/publications/pdf/FSA-9615.pdf)).
  Only the stillbirth is modelled; kids are never lost after birth (cattle rule: no death from care).

### Natural service and straws
- A buck serves a doe in heat in the same pen or pasture.
- **Buck straws:** one straw item for every species (breeding.md). A grown buck fills an empty straw; a doe in heat takes it. Goat
  straws keep, as bull and ram straws. Fresh liquid semen gives 59.8% kidding and frozen 43.9%
  ([Ahanger et al.](https://arccjournals.com/journal/bhartiya-krishi-anusandhan-patrika/BKAP672)); the game uses its straw rule as for
  sheep (a straw on a doe in heat takes) *(proposed: no conception roll, as the other species)*.

## Breeds
| Breed | Code | Use | Doe kg | Buck kg | Birth kg | Milk kg / lactation | Fat % | Protein % | Coat |
|---|---|---|---|---|---|---|---|---|---|
| Saanen | SAA | dairy | 61 | 77 | 2.7 | 1,154 | 3.2 | 2.8 | white, erect ears |
| Nubian | NUB | dairy | 61 | 77 | 3.25 | 832 | 4.6 | 3.7 | any colour, long lop ears, Roman nose |
| Boer | BOE | meat | 80 | 130 | 3.5 | none in the parlor | | | white body, red head and neck, lop ears |

- **Saanen:** "Does weigh at least 135 pounds, while bucks weigh at least 170 pounds" ([Mississippi State P4120](https://extension.msstate.edu/sites/default/files/2025-08/P4120_web.pdf));
  135 and 170 lb are 61 and 77 kg (the breed minimums; the game uses them as mature weights). Milk: "ADGA averages for 2010 lactations:
  2545 lbs milk, 3.2% fat, and 2.8% protein" ([Purdue, Saanen](https://www.ansc.purdue.edu/goat/factsheet/Saanen.htm)); 2,545 lb is
  1,154 kg. Birth weight 2.81 kg male, 2.66 female (**TO VERIFY**, snippet: [KnE Life Sciences](https://knepublishing.com/index.php/KnE-Life/article/view/11813);
  searched again 2026-11-14, the page gave 403/503),
  so 2.7.
- **Nubian:** "Does weigh at least 135 pounds and bucks at least 170 pounds" (MSU P4120). Milk: "1835 lbs milk, 4.6% fat, and 3.7%
  protein" ([Purdue, Nubian](https://www.ansc.purdue.edu/goat/factsheet/Nubian.htm)); 1,835 lb is 832 kg. Birth weight "3.25 kg"
  ([AGRIS, Trinidad](https://agris.fao.org/search/en/records/6471d43f77fd37171a703db7)).
- **Boer:** "Weight: male 120-140 kg; female 70-90 kg" (FAO); the game uses the middles, 130 and 80. Birth weight "3.0-5.0 kg; singles
  4.5, twins 3.9, triplets 3.5 kg" (FAO); Croatia's mean "3.48 ± 0.04 kg" (Đuričić 2012), so 3.5. A twin is born at 92% and a triplet
  at 87% of a single's weight ([Mellado et al., Anim. Prod. Sci. 51:144](https://connectsci.au/an/article/51/2/144/16821/Relationship-between-litter-birthweight-and-litter)).
  Boer does nurse their kids and are not milked in the parlor (as Suffolk ewes).

## Milk
- **Lactation:** **284 days**: "Dairy goats' lactation period averages 284 days, with an average range of 275 to 305 days" (MSU P4120).
- **Curve:** Wood's curve y = a·t^b·e^(−ct) with **b = 0.20 and c = 0.005** (t in days), from Brazilian Alpine, Saanen and crosses:
  "The means were 2.08 ± 0.71, 0.20 ± 0.14 and 0.005 ± 0.01 for a, b and c" ([Siqueira et al., LRRD 29(7)](https://lrrd.org/lrrd29/7/rrmo29142.html)).
  The peak is at b/c = **40 days**, inside "Goats peak at 5 – 5.5 l / day at 6 to 8 weeks"
  ([Shur-Gain dairy goat guidelines](https://epa-dccs.ornl.gov/documents/Goat_Guidelines.pdf); a feed company).
  **a** is set per breed so 284 days sum to the breed's total *(derived; the Brazilian herds gave far less milk than ADGA records)*.
- Yield scales by the dairy breeds' share (Saanen and Nubian blood) and the milk_yield trait (±20%, *proposed*; as sheep).
  First lactation x0.85 *(proposed: the cattle parity factor)*.
- A doe nursing kids still gives parlor milk *(proposed; as sheep)*.
- **Fat and protein** follow the breed shares (table above); crosses take the weighted mean.
- Goat milk goes to the parlor's **goat tank** (one milk kind per tank: cow, sheep or goat).

## Feed and water
- **Dry matter intake:**
  - lactating: **5% of body weight**: "Average dry matter intake of lactating dairy goats is 5% of body weight" (Shur-Gain, a feed company);
    Merck gives a ceiling: "Goats can consume up to 6.5% of their body weight as dry matter"
    ([Merck: nutritional requirements of goats](https://www.merckvetmanual.com/management-and-nutrition/nutrition-goats/nutritional-requirements-of-goats)).
  - not lactating: **2.75%** (the middle of 2.5-3.0% for market goats;
    [Penn State, feeding market goats](https://extension.psu.edu/feeding-market-goats-for-show)).
  - a first-lactation doe eats less (Shur-Gain: older does "as much as 40% higher"); not modelled *(proposed)*.
- **Water:** "daily water intake should be 2–3 times dry matter intake"; "Lactation greatly increases the requirement to 4–5 times dry
  matter intake" (Merck). Game: **2.5 L per kg DM**, **4.5 L** lactating.
- **Hay, rations and racks:** the sheep shed's racks (barn hay feeders) feed goats too; the ration score uses the lamb rule for kids and
  the ewe rule for does: milk x (0.83 + 0.17 x score), so a low-protein ration gives 17% less milk at worst *(derived)*. A low-protein
  diet gave ewes "a decrease (P< 0·01) in daily milk yield of 0·53 kg (17%)" ([Robinson et al. 1979](https://www.cambridge.org/core/journals/animal-science/article/further-studies-on-the-response-of-lactating-ewes-to-dietary-protein/4B2BBE8C3B04B31243DEC30731070984));
  the ewe figure stands for goats too *(proposed)* (feed-mill.md).
- **Grazing and browse:** goats eat "20–30% grass, 10–30% weeds and legumes, 40–60% browse" (Peischel, Tennessee State,
  [Nutrition management for healthy goats](https://ucanr.edu/sites/default/files/2014-08/197594.pdf)).
  - On the shed's pasture a goat grazes grass blocks as a sheep does (1.5 kg DM a block, `Flock.GRASS_BLOCK_KG_DM`).
  - **Browse:** a goat on pasture beside leaves (any vanilla leaves block within 1 block of its spot) also browses them: up to **half its
    daily dry matter** comes from leaves *(proposed; the browse share above is 40-60%)*, so a wooded pasture wears its grass half as
    fast. Leaves are not removed *(proposed: trees would vanish)*.
- **Underfeeding:** as sheep and cattle: less growth and milk, lower health, never death.

## Housing (the sheep shed)
| Quantity | Value | Source |
|---|---|---|
| Enclosed area | 20-25 sq ft (1.9-2.3 m²) per goat | "20-25 sq. feet of enclosed housing area per goat" ([Purdue, goat housing](https://www.ansc.purdue.edu/goat/factsheet/housing.htm)) |
| Bedded area | 10-15 sq ft (0.9-1.4 m²) | "Ten to fifteen square feet of bedded area is adequate for each adult goat" ([NMSU D-703](https://pubs.nmsu.edu/_d/D703/)) |
| Kidding pen | 4 × 5 ft (1.9 m²) | "minimum of 4 by 5 feet of bedded area" (NMSU D-703) |
| Fence | 4 ft (1.2 m) woven wire with a board on top | "Four-foot-high woven wire with a board or barbed wire on top should be adequate" (NMSU D-703) |

**Game:**
- **Pens:** a pen holds one species. The first animal housed sets it; it is free again when empty. **One goat to 2 pen blocks** (2 m², the
  middle of Purdue's 1.9-2.3 rounded to blocks); a kid under weaning age counts as nothing (as lambs).
- **Kidding pens:** the lambing pens. One pen block holds one doe and her kids (a game size for the 1.9 m² pen, as the lambing jug).
- **Fences:** vanilla fences (1.5 blocks) hold goats; goats don't jump fences *(proposed: Minecraft's fence already stands above the
  4 ft rule)*. Wild goats jump as vanilla goats do.
- **Racks:** one rack block serves 2 goats *(proposed: as sheep)*.
- **Bedding:** goats use the shed's bedding store as sheep (sheep-shed.md). Manure: **13 lb dry matter per 1,000 lb live weight a day**
  ("Manure and Nutrients as Excreted Per 1,000 Pound Live Weight/Day", row "Goats … 0.65 13", adapted from the NRCS Agricultural Waste
  Management Field Handbook: [Univ. of Kentucky BAE](https://engr.uky.edu/sites/default/files/BAE/Solids_Worksheet_1%201.pdf)), so
  1.3% of body weight in dry matter, 0.78 kg for a 60 kg goat *(derived)*. Fresh goat manure is **64% moisture, C:N 31**
  ([Utah State Extension, Table 1](https://extension.usu.edu/yardandgarden/research/sustainable-manure-and-compost-application)).
  It comes out as the shared manure item.

## Growth and weaning
- **Weaning:** dam-reared kids at **90 days**: "Weaning weights are typically adjusted to 90 days of age"
  ([Penn State, selecting meat goats](https://extension.psu.edu/selecting-meat-goats-using-performance-data)). Dairy kids reared away
  from the dam are weaned at 42-56 days ([Vickery et al. 2023](https://www.frontiersin.org/journals/veterinary-science/articles/10.3389/fvets.2023.1200849/full));
  the game keeps kids with the dam (90 days) *(proposed)*.
- **Growth:** the sheep growth curve (`Flock.targetKg`) from birth weight to mature weight *(proposed shape)*, checked against "35 to 50
  pounds or more by 90 days of age" for meat kids (16-23 kg; Penn State). A Boer kid reaches about 22 kg at 90 days on this line *(derived)*, inside the range. Twins are born at 92% and triplets at 87% of a
  single's weight (Mellado et al., above).
- **Ration:** kids' growth takes the lamb ration factor (feed-mill.md).

## Meat
- **Dressing percentage:** Boer **48% at 8-10 months, 58% mature** ("48 at 8-10 months, 56-60 at mature ages", FAO); other goats
  **47.5%** ("Goats will average between 45 and 50 percent", [Univ. of Kentucky ASC-182](https://publications.mgcafe.uky.edu/sites/publications.ca.uky.edu/files/asc182.pdf)).
- **Kid and chevon:** **kid** meat from goats under **9 months**, **chevon** from older goats ("kid under 9 months",
  [Texas A&M](https://texaslocalproduce.tamu.edu/files/2019/11/Caprine-Meat-from-farm-to-table-PDF.pdf); 4-H classes differ).
- **Retail share and items:** as sheep: 70% of the carcass in retail cuts and one item per 2.5 kg *(proposed; as sheep)*. Items
  `kid_chop` / `chevon_chop` and their cooked forms; spoiled meat is the spoiled mutton item (food.md).

## Coat genes (goat loci)
| Locus | Alleles (dominance) | Source |
|---|---|---|
| Agouti (ASIP) | white/tan A^Wt > (bezoar A^bz, badgerface A^b, swiss markings A^sm, codominant) > nonagouti a | "The postulated alleles are white or tan (A^Wt), black mask … nonagouti (a)" ([OMIA 000201-9925](https://omia.org/OMIA000201/9925/)); Adalsteinsson et al. 1994. Saanen white is A^Wt: "In Girgentana and Saanen breeds, this CNV might cause the A^Wt allele" (OMIA). The game keeps four of the ten alleles *(proposed)* |
| Brown (TYRP1) | black B+ > brown b | as sheep *(proposed for goats; not searched further)* |
| Red head (EDNRA copy number) | three copy-number alleles, incomplete dominance: 2/2 is the Boer white body with a red head and neck, 1/1 fully pigmented | "a 1 Mb copy number variant (CNV) harboring 5 genes including EDNRA"; "Autosomal incomplete dominant"; "non-pigmented white body and a pigmented red head and neck … 2/2 genotype" ([OMIA 002164-9925](https://omia.org/OMIA002164/9925/)). The game uses two alleles, pigmented P and white-body W: W/W is the Boer pattern, P/W shows a white belt and patches *(proposed look)* |
| Red (Boer head colour) | the head's colour is red where the agouti allele is A^Wt or the red tan *(proposed)* | MC1R was "suggested" for the red head ([Animal Research 2006](https://animres.edpsciences.org/10.1051/animres:2006020); [PMC2748843](https://pmc.ncbi.nlm.nih.gov/articles/PMC2748843/)) |
| Polled (PIS deletion) | polled P dominant; **a P/P doe is intersex and can't breed**; some P/P bucks are sterile | "polledness in goats is a single-locus autosomal dominant trait"; "Most XX goats that are homozygous for the polled allele, show signs of intersexuality" ([OMIA 829](https://omia.org/829)). Game: every P/P doe is intersex (can't conceive, gives no milk), a P/P buck is sterile in **half** of cases *(proposed share)* |
| Wattles | wattles W dominant | "Autosomal dominant" ([OMIA 001061](https://omia.org/OMIA001061/9925/)) |
| Ears | erect vs lop, drawn by the Nubian and Boer blood share | NOT FOUND as a single gene (searched goat ear carriage; MSRB3 is a candidate in sheep and pigs). Drawn from breed shares like body shape *(proposed)* |
| Beard | every adult buck, and does at 50% *(proposed)* | No mode known: OMIA 000104 lists inheritance as "unknown" (checked 2026-11-14) |

**Founders:**
- Saanen: A^Wt/A^Wt, no red head, horned or polled 30% carriers *(proposed)*, wattles 50% *(proposed)*.
- Nubian: agouti alleles at random (A^bz, A^b, A^sm, a) and brown 20% *(proposed)*, so Nubians come in many colours, as the breed does.
- Boer: W/W with a red head, horned.
- Polled founders never start P/P *(proposed: so no founder is intersex)*.

## Wild goats
- **Spawns:** Farmings goats take the vanilla goat's spawns (snowy slopes, jagged peaks, frozen peaks; vanilla's biome tags) in groups
  of 2-4, one buck a group *(proposed: vanilla's group size)*.
- **Breeds by biome** *(proposed)*: Saanen 60%, Nubian 20%, Boer 20% (Saanen is the alpine breed).
- **Behaviour:** a wild goat rams a player (not in creative or spectator) or a hostile mob within 6 blocks: a straight charge, the vanilla
  goat's 2 damage and half its knock-back, then 30-300 s before the next (vanilla's ram cooldown) *(proposed)*. Tame goats don't ram.
- **Taming:** in the corral, as sheep: hay or wheat in the bait trough, 7 real days settled (taming.md).
- **Climbing:** goats "use elevated surfaces to climb and rest" ([MSD: behavior of goats](https://www.msdvetmanual.com/behavior/behavior-of-production-animals/behavior-of-goats));
  the game gives every goat a step height of 1 block, so it climbs a block without jumping and a 1.5-block fence still holds it
  *(proposed)*.

## Economy
Nothing is sold (cattle rule, 2026-11-07).

## Open points
- First-kidding odds for all three breeds, and ADGA's current DHIR breed averages (2010 used).
- Lop ears and beards as genes.
- A fridge life for goat milk from FSIS or extension (searched; only blogs).
- Angora and mohair (Tim: not this pass).

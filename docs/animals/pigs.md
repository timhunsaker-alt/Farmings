# Pigs

**Status: spec, 2026-11-13 (Step 18; Tim's answers of the same day).** Marks as in `docs/animals/sheep.md`.

Tim's choices: **Large White (Yorkshire), Duroc, Berkshire**; a **new pig barn** (`multiblocks/pig-barn.md`); **rooting only outdoors on
pasture**; pork plus **cured bacon and ham, lard and sausage** (`multiblocks/curing-room.md`).

Sources used throughout:
- **[PIG24]** PigCHAMP 2024 North America benchmarks ([PDF](https://www.pigchamp.com/Portals/0/Documents/Benchmarking%20Summaries/2024-benchmark-summaries-north-america.pdf)).
- **[KH13]** Knauer & Hostetler 2013, J. Swine Health Prod. 21(5):248 ([AASV](https://www.aasv.org/shap/issues/v21n5/v21n5p248.html)).
- **[MSDB]** MSD Vet Manual, [breeding management of pigs](https://www.msdvetmanual.com/management-and-nutrition/management-of-reproduction-pigs/breeding-management-of-pigs).
- **[PIH-G]** Pork Industry Handbook, *Development of gilts and boars* (1997, [MSU archive](https://sanweb.lib.msu.edu/DMC/extension_publications/e1534/e1534-1997-rev1.pdf)).
- **[KSU]** K-State MF2302, [breeding herd recommendations](https://bookstore.ksre.ksu.edu/pubs/MF2302.pdf); K-State MF3396, pork processing.
- **[WI]** UW Extension A3858-03, [Guide to raising healthy pigs](https://4h.extension.wisc.edu/files/2026/02/Guide-to-Raising-Healthy-Pigs.pdf) (tables from PIH).
- **[MWPS]** MWPS-18 S1 Table 6 ([Virginia DCR copy](https://dcr.am.virginia.gov/soil-and-water/document/manure-characteristics-mwps-18-1.pdf)).
- **[NOW20]** Nowak et al. 2020, Animals ([PMC7401630](https://pmc.ncbi.nlm.nih.gov/articles/PMC7401630)); **[KLI20]** Klimas et al. 2020, S. Afr. J. Anim. Sci. 50(3).

## Time scale
Pigs live on the livestock scale, **1 game day = 15 real days**.

| Stage | Real | Game days |
|---|---|---|
| Oestrous cycle | 21 days | 1.4 |
| Gestation | 114 days | 7.6 |
| Weaning | 21 days | 1.4 |
| Market weight (120 kg) | 186 days | 12.4 |

## Breeds
| Breed | Code | Role | Sow / boar kg | Litter, total born | Stillborn | Coat, ears |
|---|---|---|---|---|---|---|
| Large White | LWH | maternal | 280 / 365 | 11.6 first, 13.7 later | 8.8% | white, erect ears |
| Duroc | DUR | terminal sire | 350 / 400 | 10.4 | 8.9% | red, drooping ears |
| Berkshire | BRK | sire, prized pork | 220 / 280 | 9.2 | 15.1% | black with six white points, erect ears |

- **Weights:** Large White "Sows weight around 260-300kg and boars, 350-380kg"; Berkshire "sows weigh around 220kg and boars 280kg"
  (**TO VERIFY**: [Rare Breeds Trust of Australia](https://rarebreedstrust.com.au/public/pages/sp-large-white)); Duroc "The weight of a
  mature boar is about 882 pounds, and the sow is about 772 pounds" (**TO VERIFY**: Wikipedia citing Briggs, *Modern Breeds of
  Livestock*).
- **Litters:** Large White by parity, "An increase in litter size was observed until parity 5" ([KLI20], Table 2: 11.6 total born at the
  first, 13.7 at the third); Duroc 10.39 and Berkshire 9.16 total born, stillborn 8.92% and 15.09%, Polish Large White 8.80% ([NOW20],
  Table 3; small purebred herds). Duroc and Berkshire first litters take the Large White's first-to-later ratio, 0.85 *(derived)*.
  The roll is a normal spread around the mean, sd 2.5, 1 to 20 *(proposed)*.
- **Looks:** Yorkshire "Erect ears"; Duroc "Droopy, non-erect ears"; Berkshire "black with six white points (face, four socks, tip of
  tail)" ([Purdue breed ID](https://extension.purdue.edu/county/vigo/_docs/swine-breed-id.pdf)).

## Reproduction
| Quantity | Value | Source |
|---|---|---|
| Oestrous cycle | **21 days** | "normally 21 days but can range from 18 to 24 days" ([WI]) |
| Heat | **2 days** | "36–48 hours (1–2 days) in gilts and ≥ 48–72 hours (2–3 days) in sows" ([MSDB]) |
| Gestation | **114 days** | "114 days (3 months, 3 weeks, and 3 days)" ([WI]) |
| First breeding | **210 days and 136 kg** | "have a 300 lb gilt at 210 days of age" ([KSU]) |
| After weaning | in heat **5 days** after weaning (4-6) | "Most sows will return to their estrous cycle within 4–6 days" ([WI]) |
| Conception | **84%** a service | "Farrowing rate 83.81" ([PIG24]) |
| Boars | one boar to 17 sows | "a boar-to-sow ratio of 1:15–1:25 (average 1:17 or 1:18)" ([MSDB]) |
| Boar straws | fresh semen keeps **6 days** | "survive in a cooled state for 5–7 days" ([MSDB]) |
| Birth weight | **1.5 kg** | "weighing from 3 to 3½ pounds" ([WI]) |
| Pre-weaning loss | 14% in industry, **not modelled** (no death from care) | "Pre-weaning mortality 14.17" ([PIG24]) |

## Growth, feed and water
- **Growth** (a straight line between points): birth 1.5 kg; **5.7 kg at 21 days** ("Weaning weight (kg/pig) … 5.7", [KH13]); 24 kg at
  66 days and **120 kg at 186 days** (finishing "ADG (kg) … 0.80"; the exit weight printed "264" reads as lb, 120 kg, **TO VERIFY** the
  unit); then to the breed's mature weight at 2 years *(proposed)*.
- **Feed** (as fed, kg a day): finishers **2.2 at 80 kg** (0.80 ADG / 0.36 G:F, [KH13], *derived*), scaled by body weight to the 0.75
  power *(proposed)*; gestating sows **2.34** ("Gestation-feed intake (kg/day) … 2.34"); lactating sows **6.5** ([KH13]).
- **Water** (L a day): finishers **7** (1.5-2 gal, **TO VERIFY**, snippet: extension.org); gestating sows **17** (3-6 gal, **TO VERIFY**,
  snippet: MSU); lactating sows **28** (5-10 gal, **TO VERIFY**, snippet: Alabama).
- **Rations:** the feed mill's rations feed pigs (`feed-mill.md`); growth takes the lamb ration factor *(proposed)*.

## Pasture and rooting (Tim: only outdoors)
- On the barn's pasture a pig takes a share of its feed from the ground: **15%** for growing pigs, "Finishing pigs Forage + concentrate
  10-20%" ([NC State CEFS](https://ccfruitandnuts.ucanr.edu/sites/default/files/2020-10/338311.pdf)); **50%** for sows, "performed best
  when 50% of the energy requirements were supplied by supplements" ([Utah State, pasture pig basics](https://extension.usu.edu/smallfarms/research/pasture-pig-basics.pdf)).
- **Rooting:** what a pig takes off pasture comes off grass blocks at 1.5 kg DM a block (the sheep rule) and the block turns to dirt.
  Indoors pigs never change blocks.
- Wallows and nose rings are not modelled.

## Manure
As excreted ([MWPS], "can vary ± 30% or more"), per pig a day: nursery (18 kg) **1.4 kg**; finishing (100 kg) **4.9 kg**, 89% water,
VS 0.96 lb of 10.9; gestating sow **4.1 kg**; lactating sow **10.6 kg**; boar **2.8 kg**. The game interpolates by weight and class
*(proposed)*. It falls through the slatted floor into the pit (`pig-barn.md`).

## Meat
- **Dressing 70%:** "250 lb x 70% = 175 lb" ([UW](https://livestock.extension.wisc.edu/files/2020/10/how-much-meat-should-a-hog-yield.pdf)).
- **Cuts** as % of the hot carcass ([KSU MF3396](https://bookstore.ksre.ksu.edu/download/pork-processing-options-for-consumers_MF3396)):
  ham 15.8, loin 14.6, belly 12.6, Boston butt 8.6, picnic 8.0, spareribs 2.9, trimmings 7.8; "Collar, Fat Back, and Clear Plate - 15%".
- **Items** *(item sizes proposed)*: `fresh_ham` (the two hams, each half of 15.8%), `pork_chop` 0.5 kg from the loin, `pork_belly` 2 kg
  slabs, `pork_shoulder` 2 kg (butt, picnic and trimmings, 24.4%), `pork_fat` 1 kg (the 15% fat back). A 120 kg pig gives 2 hams of
  6.6 kg, 24 chops, 5 bellies, 10 shoulders and 12 fat *(derived)*. Piglets under 30 kg give chops only.

## Coat genes
| Locus | Alleles | Source |
|---|---|---|
| KIT | dominant white **I** over coloured i | "The dominant white phenotype is caused by two mutations, a gene duplication and a splice mutation" ([Pielberg et al. 2002](https://pmc.ncbi.nlm.nih.gov/articles/PMC1461930)); [OMIA 000209-9823](https://omia.org/OMIA000209/9823/) |
| MC1R | **E^D2** dominant black > **E^P** black spotting > E+ wild > **e** red | "dominant black in European pigs (E^D2)"; "black spotting on red or white background (E^P)"; "red (e)" ([OMIA 001199-9823](https://omia.org/OMIA001199/9823/)) |
| Berkshire points | E^P/E^P shows black with six white points *(proposed: "SWP in the Berkshire is derived from the MC1R D124N allele through a 2-bp insertion" and KITLG "may also influence the coat", [Lü et al. 2016](https://pmc.ncbi.nlm.nih.gov/articles/PMC4897638); the points' own gene is not settled)* | |
| Ears | erect or drooping by the Duroc share, drawn | NOT FOUND as a single gene |

**Founders:** Large White I/I (its MC1R hidden, E+ *proposed*); Duroc i/i e/e; Berkshire i/i E^P/E^P.

## Wild pigs
- Farmings pigs take vanilla pigs' spawn entries; groups as vanilla's, one boar a group. Feral pigs "often travel in groups known as
  'sounders'" ([Clemson](https://www.clemson.edu/extension/wildlife/wild-hogs/info.html)).
- Breeds by biome *(proposed)*: plains Large White and Duroc, forests Berkshire.
- Tamed in the corral on grain (taming.md).

## Open points
- Registry figures for breed weights and litters; a creep temperature schedule by week; finisher intake from an extension table.

## Built, 2026-11-13: the animal
- **Code:** `entity/PigEntity`, rules in `husbandry/Swine`, `PigBreed`, `PigStage`; genes `PigLocus`/`PigGenes`/`PigLook`; model and
  renderer `client/Pig*`, textures by `tools/pig_textures.py`; pork items with Ludo icons; boar straws keep 6 days.
- **Wild pigs** take vanilla pigs' spawns.
- **Tests:** `SwineTest`, `PigGeneticsTest`; game tests pig_spawns_and_survives_save, pig_lifecycle, sow_farrows_litter, pig_cuts_drop,
  wild_pig_replaces_vanilla, boar_straw_goes_stale. Client scene `pigs`.
- **Not yet:** the pig barn (next part); until it is built, pigs are fed as loose animals (fed share 1).

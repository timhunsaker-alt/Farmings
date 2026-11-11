# Compost windrow and turner

**Status: spec, 2026-11-11 (Phase 3; Tim: a compost windrow and turner).** Builds on `docs/waste.md`, Route A. Marks are as in
`docs/crops.md`.

## Parts
| Part | Block | What it does |
|---|---|---|
| Compost pad | `compost_pad` | Concrete floor cells in a straight row: the windrow lies on them. **4-64 cells** *(proposed)* |
| Windrow controller | `compost_controller` | At one end of the pad. Takes inputs by belt, hopper or hand, reads the pile, and puts out finished compost at its output face. |
| Windrow turner | `windrow_turner` | A powered straddle turner on the pad's end. It runs down the windrow when the pile cools. **41 kW** *(the middle of 50-60 hp three-point turners, [BioCycle](https://www.biocycle.net/windrow-turner-equipment-review/), **TO VERIFY**)* |

## The pile
| Input | Item mass | C:N | Moisture | Source |
|---|---|---|---|---|
| Manure | 50 kg | 20 | 87% *(**TO VERIFY**: as excreted; separated solids are 65-80%)* | C:N: [NDSU NM1478](https://www.ndsu.edu/agriculture/extension/publications/composting-animal-manures-guide-process-and-management-animal-manure-compost), Table 1 |
| Poultry litter | 10 kg | 10.5 (raw litter 9-12) | 30% *(**TO VERIFY**)* | [Univ. of Arkansas](https://poultry-science.uark.edu/_resources/pdf/November2018_AvianAdvice.pdf) |
| Separated digestate solids | 10 kg | 20 *(proposed: manure's)* | 72.5% (20-35% DM) | Cornell PRO-DAIRY (digester.md) |
| Straw (oat or barley) | 454 kg bale, or loose 10 kg | 80 *(**TO VERIFY**: no source looked up)* | 10% (90% DM, Auburn table) | |
| Water | 1 bucket = 1,000 kg *(as the trough's 1,000 L)* | | | |

**Targets:**
- C:N 25:1 to 30:1 (20-40 works);
- moisture 50-60% (40-65% works);
- about **8 weeks** with at least **5 turnings**;
- turn when the pile drops below 110 °F
  ([Oregon State EM 8825](https://extension.oregonstate.edu/es/catalog/em-8825-composting-alternative-livestock-manure-management-disposal-dead-animals);
  [UW-Madison](https://dairy.extension.wisc.edu/articles/composting-animal-manure/)).

**Game rules:**
- **Starting:** a pile starts composting when its C:N is 20-40 and its moisture 40-65%. Otherwise the readout names the fault: TOO
  WET, TOO DRY, NEEDS CARBON or NEEDS NITROGEN.
- **Turning:** the turner turns the windrow once every **11.2 real days** (8 weeks / 5), about 0.75 game days. Each turn needs power.
  A pile that misses a turn does not age until it is turned *(proposed: it stands for "cooled below 110 °F")*.
- **Ready:** after **56 real days** (3.7 game days) and 5 turns, the pile is finished compost.
- **Mass:** finished compost weighs **42.5%** of the wet mass that went in. Composting cuts volume and weight by about 50-65% (NDSU),
  and the game uses the middle, 57.5%.
- **Output:** **10 kg `compost` items** *(proposed)*. One compost item is **one fertilizer use**: vanilla bone meal's effect on a crop,
  sapling or grass (waste.md). Compost is also spread by the field gantry.
- **Nutrients:** dairy compost is N 1.3-2.3%, P 0.2-0.6% and K 0.7-1.7% on a dry basis
  ([NCDA&CS](https://www.ncagr.gov/divisions/agronomic-services/waste-analysis/nutrient-concentrations-compost)). This is shown on the
  item; it doesn't change the bone-meal effect.
- **The vanilla composter** still takes poultry litter (eggs.md).

## Not in this spec
- Temperature as a simulated quantity.
- Odour.
- Runoff.
- Pathogen kill.
- Curing piles.

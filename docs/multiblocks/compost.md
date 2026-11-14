# Compost windrow and turner

**Status: spec, 2026-11-11 (Phase 3; Tim: a compost windrow and turner).** Builds on `docs/waste.md`, Route A. Marks are as in
`docs/crops.md`.

## Parts
| Part | Block | What it does |
|---|---|---|
| Compost pad | `compost_pad` | Concrete floor cells in a straight row: the windrow lies on them. **4-64 cells** *(proposed)* |
| Windrow controller | `compost_controller` | At one end of the pad. Takes inputs by belt, hopper or hand, reads the pile, and puts out finished compost at its output face. |
| Windrow turner | `windrow_turner` | A powered straddle turner on the pad's end. It runs down the windrow when the pile cools. **41 kW**: a three-point turner needs 50-60 hp (Australian Eggs fact sheet, confirmed 2026-11-14); the middle, 55 hp, is 41 kW. The [BioCycle](https://www.biocycle.net/windrow-turner-equipment-review/) review gives no figure for tractor turners; a pull-type Vermeer CT612 needs "Min horsepower: 80 hp (59.7 kW)" ([listing](https://www.agriculture-xprt.com/products/vermeer-compost-turner-107674)) |

## The pile
| Input | Item mass | C:N | Moisture | Source |
|---|---|---|---|---|
| Manure | 50 kg | 20 | 88% (was 87%), as excreted; separated solids are 65-80% | C:N: [NDSU NM1478](https://www.ndsu.edu/agriculture/extension/publications/composting-animal-manures-guide-process-and-management-animal-manure-compost), Table 1. Moisture: lactating cow, water 88% ([MWPS-18](https://dcr.am.virginia.gov/soil-and-water/document/manure-characteristics-mwps-18-1.pdf), Table 6) |
| Poultry litter | 10 kg | 10.5 (raw litter 9-12) | 27% (was 30%) | C:N: [Univ. of Arkansas](https://poultry-science.uark.edu/_resources/pdf/November2018_AvianAdvice.pdf). Moisture: "ranged from 23.85 percent to 30.26 percent with an average of 26.86 percent" (210 farms, [Mississippi State](https://extension.msstate.edu/publications/nutrient-content-mississippi-broiler-litter)) |
| Separated digestate solids | 10 kg | 20 *(proposed: manure's)* | 72.5% (20-35% DM) | Cornell PRO-DAIRY (digester.md) |
| Straw (oat or barley) | 454 kg bale, or loose 10 kg | oat 60 (was 80); barley 80 | 10% (90% DM, Auburn table) | "Straw, oat 60:1" and "Straw, general 80:1" (On-Farm Composting Handbook, NRAES-54, in the [SARE quick guide](https://projects.sare.org/media/pdf/M/a/n/Manure-Composting-Quick-Guide.pdf)). Barley straw is not listed, so it takes straw in general |
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
- **A working mix** (worked out from the figures above): manure as excreted is too wet, and enough straw to dry it brings too much carbon
  (500 kg of manure with 200 kg of barley straw is C:N 46, was 44). Poultry litter or separated digestate solids bring both into range.
  For example, 500 kg of manure, 200 kg of barley straw and 100 kg of litter is C:N 26 at 61% moisture. With oat straw it is C:N 24.

## Not in this spec
- Temperature as a simulated quantity.
- Odour.
- Runoff.
- Pathogen kill.
- Curing piles.

## Built, 2026-11-11
- **Code:**
  - `farm/compost/Windrow` is plain Java and unit-tested.
  - The pad, controller and turner are in `compost/`.
- **Blending:** inputs are blended assuming every input's dry matter holds the same share of carbon *(proposed)*.
- **Starting:** a pile starts when the player sneak-right-clicks the controller. Inputs are refused once it has started.
- **Turning:** the pile ages only up to its next due turn, and waits there until the turner has power.
- **Compost:** it works as bone meal by hand, and the gantry's spreader uses it.
- **Game tests:** compost_matures, compost_waits_for_turns.

## Loose ends, 2026-11-11 (spec): spoiled food into the windrow
Spoiled Farmings food goes into the windrow as a nitrogen-rich input.

| Input | Item mass | C:N | Moisture | Source |
|---|---|---|---|---|
| Spoiled steak, mutton or poultry | 0.25 kg *(proposed)* | 5 | 70% *(proposed)* | "Animal Carcass … 5:1" ([Cornell CWMI 2014](https://cwmi.css.cornell.edu/Composting_Animal_Mortalities.pdf), Table 6-2) |
| Spoiled milk (a bottle) | 1 kg *(proposed)* | 15 *(proposed: food waste's; dairy NOT FOUND, searched again 2026-11-14, the pages did not load)* | 87% *(proposed)* | food waste 15:1 ([Cornell](https://www.compost.css.cornell.edu/Factsheets/FS2.html)) |
| Rancid butter, mouldy cheese | 0.25 kg *(proposed)* | 15 *(as food waste)* | 40% *(proposed)* | as above |
| Rotten egg | 0.06 kg *(the egg's)* | 15 *(as food waste)* | 75% *(proposed)* | eggshells "will break down in the compost pile" ([Nebraska](https://lancaster.unl.edu/6-common-composting-questions)) |

- **Meat:** meat is fine in a managed farm pile. "butcher/slaughter house waste can be composted without any pre-treatment" (Cornell
  CWMI), though home composting guides say not to.

### Built, 2026-11-11 (loose ends)
- **Inputs:** spoiled steak, mutton and poultry, spoiled milk, rancid butter, mouldy cheese and rotten eggs go on the pile. Spoiled
  milk poured in by hand gives the glass bottle back.
- **Game test:** spoiled_food_composts.

### Sourcing pass, 2026-11-12
- Resolved: manure moisture (88%, MWPS-18), litter moisture (27%, Mississippi State), straw C:N (oat 60, barley 80, NRAES-54).
- The turner's old source does not hold; only a dealer snippet was found (80 hp minimum for a pull-type turner).
- Still unsourced: digestate solids' C:N (searched Idaho, Ontario, AgProud: no figure for pressed solids); the spoiled food moistures
  (not searched this pass). Pad size, output items, blending and the missed-turn rule are game choices.

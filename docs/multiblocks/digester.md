# Methane digester and separator

**Status: spec, 2026-11-11 (Phase 3; Tim: a digester, a separator, and biogas into a Wildcatter genset).** Builds on `docs/waste.md`,
Route B. Wildcatter 0.6.0 is the other side: a mod offers `Product.BIOGAS` on steel pipe through `FluidNode` (Wildcatter
`docs/biogas.md`). Marks are as in `docs/crops.md`.

## Parts
| Part | Block | What it does |
|---|---|---|
| Feed pit | `digester_feed_pit` | Takes manure items by belt, hopper or hand, or straight from a barn collection pit it touches |
| Digester tank | `digester_tank` (3 x 3 x 3 ring with a roof) | A sealed, heated, stirred complete-mix tank. Its mixer is a **6.5 kW** submersible agitator (a trade example, confirmed 2026-11-14: [Process Engineering](https://processengineering.co.uk/article/2002256/biogas-in-the-mixbio), "two low-speed submersible agitators", each "with a power rating of 6.5kW"; the tank size is not given); it needs heat (below) |
| Gas holder | `gas_holder` | A membrane dome on the tank: **200 m³** *(proposed)*. It offers biogas on any face with a steel pipe |
| Digestate tank | `digestate_tank` | Holds what leaves the digester: **50 t** *(proposed)* |
| Screw-press separator | `screw_press` | Splits digestate into separated solids and liquid. **5.5 kW** (a dealer's page, confirmed 2026-11-14, for a press screw separator (PSS 3.2-780): "5.5 kW geared motor", "Cattle manure throughput is 15–35 m³/h", [Zorg Biogas](https://zorg-biogas.com/equipment/separators-en/separator-pss-32-780)) |

## The process
| Quantity | Value | Source |
|---|---|---|
| Temperature | mesophilic, 35-40.5 °C (95-105 °F) | [MSU E3080](https://archive.lib.msu.edu/DMC/extension_publications/e3080/E3080.pdf); NC State gives 30-38 °C |
| Retention time | **20 days** | Clover Hill Dairy: "approximately 100 °F... HRT of 20 days" ([EPA AgSTAR](https://19january2025snapshot.epa.gov/agstar/project-profile-clover-hill-dairy)); complete-mix systems run 17-30 days |
| Biogas | **23 m³ per wet tonne of dairy manure** | Ontario OMAFA, Table 1: "Dairy manure 23 48 62" (m³/t, kWh/t electricity, kWh/t heat) (waste.md, and the Wildcatter genset's calibration). MSU E3080 gives 1.87 m³ a cow a day (about 34 m³/t, *derived*): the game keeps 23 so the genset's 48 kWh/t holds |
| Methane | 60% | MSU E3080; EPA AgSTAR |
| Heating | **half the biogas made** heats the tank, when there is no engine heat to use | "At a conservative estimate, 50 percent of the produced biogas is used to meet heating requirements" (MU Extension [EM702](https://mospace.umsystem.edu/server/api/core/bitstreams/16c40052-69b5-4515-9597-348f80097b1a/content)) |

**Game rules:**
- **Gas from manure:**
  - A manure item (50 kg) gives **1.15 m³** of biogas.
  - The gas comes off evenly over the **20 real days** after feeding (1.3 game days at 15:1).
  - The tank burns **half** of it to stay warm; the other half goes to the gas holder.
  - Poultry litter (10 kg) gives the same per tonne *(proposed; no poultry figure looked up)*.
- **Power:** the mixer needs Wildcatter 480 V. With no power the tank is unstirred and makes no gas until power returns *(proposed)*.
- **Digestate:** what goes in comes out after 20 days as digestate, less the gas's mass (about 1.2 kg/m³, so about 3% of the
  manure: *derived*). It goes to the digestate tank.
- **Separator** (Cornell PRO-DAIRY,
  [manure separation, 2020](https://ecommons.cornell.edu/server/api/core/bitstreams/1286ac74-6d5e-4a87-afd2-5f268326bddf/content)):
  - "Separated solids" are 20-35% DM; "separated liquid" is 1-8% DM; "the majority of the mass flow is always with the liquid".
  - Game:
    - **10%** of the digestate's mass comes out as separated solids *(proposed, inside the 5-15% volume figure)*, as 10 kg items
      for compost or bedding;
    - **90%** comes out as liquid fertilizer, as **100 L `liquid_digestate` items** *(proposed)*.
  - A press needs digestate of at least 3% total solids (Cornell) *(always true in the game)*.
- **Liquid fertilizer:**
  - It holds 27 lb N, 9 lb P₂O₅ and 27 lb K₂O per 1,000 gal (MSU E3080), shown on the item.
  - One 100 L item is **one fertilizer use** (bone meal's effect) *(proposed)*. It goes out by the gantry's spreader or by hand.
- **To the genset:** the gas holder implements Wildcatter's `FluidNode` and offers `Product.BIOGAS` (m³) on its faces. A Wildcatter gas
  set on that pipe burns it at 850 kW maximum, 407 m³/h at full load (Wildcatter biogas.md). Run it in bursts with the set's remote
  start.
- **Size check:**
  - About 100 cows make 5 t of manure a day: 115 m³ of biogas, of which 57.5 m³ reaches the holder.
  - At one real day per game day, that is 57.5 m³ every 20 minutes of play: 8.5 minutes of a 1 MW set at full load each game day.
  - This matches waste.md's warning: it runs a few lights and fans, not a grid.

## Not in this spec
- Hydrogen sulfide scrubbing (Wildcatter assumes conditioned gas).
- Thermophilic digesters.
- Plug-flow tanks.
- Feeding food waste.
- Combined heat and power: the engine's heat warming the tank.

## Built, 2026-11-11
- **Code:**
  - `farm/digester/Digester` is plain Java and unit-tested.
  - The parts are in `digester/`.
- **Single blocks in this pass:** the tank is one block, not the spec's 3 x 3 x 3 ring.
  - The gas holder sits on top of the tank.
  - The feed pit and the digestate tank sit beside it, and the screw press beside the digestate tank.
- **The gas holder** is a Wildcatter `FluidNode` offering `Product.BIOGAS` on its top and sides.
- **Feeding:**
  - The feed pit takes manure and poultry litter by hand, belt or hopper.
  - It also pulls up to 4 manure items a second from a barn collection pit it touches.
- **Liquid digestate:** it works as bone meal by hand, and the gantry's spreader uses it.
- **Game tests:** digester_fills_holder, separator_splits, and genset_burns_farm_biogas. That last one is a real Wildcatter 0.6.0 gas set
  started on the digester's gas line, which runs on the farm's biogas.

## Loose ends, 2026-11-11 (spec): the full-size tank
- The digester tank becomes the spec's **3 x 3 x 3** big part (Wildcatter `BigParts`).
  - It is placed from one `digester_tank` item, like the barn's big parts; the footprint must be clear.
  - The gas holder sits on its roof centre, and the feed pit and digestate tank against its sides.
- Its figures are unchanged. A tank placed as a single block before this change still works.

### Built, 2026-11-12 (loose ends): the full-size tank
- **Code:** `DigesterTankBlock`, a Wildcatter big part (3 x 3 x 3, main cell at the bottom centre).
- **Neighbours:**
  - The feed pit, gas holder and screw press find the tank through any of its cells.
  - The tank looks for its holder above, and its digestate tank beside, any of its cells.
- **Older worlds:** a single-block tank grows to full size where there is room.
- **Game test:** digester_multiblock_forms. The digester tests use the full tank.

### Sourcing pass, 2026-11-12
- Resolved: heating takes half the biogas (MU EM702). The screw press's 5.5 kW now has a dealer figure, still weak. The mixer quote was checked.
- Still unsourced: poultry litter's gas per tonne (OMAFA and MU tables have no poultry row), gas holder volume (no sizing rule found).
  Digestate tank size, the 10/90 split, 100 L items and the no-power rule are game choices.

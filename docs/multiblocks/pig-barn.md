# The pig barn

**Status: spec, 2026-11-13 (Step 18; Tim: "New pig barn").** Marks as in `docs/crops.md`. Figures in `docs/animals/pigs.md`.

## Parts
| Part | Block | What it does |
|---|---|---|
| Controller | `pig_barn_controller` | Checks the layout, houses tame pigs within 48 blocks, reads out. |
| Pen floor | `slatted_floor` | Pen floor over the pit: manure falls through. Each joined run is a pen (`PenRuns`). |
| Farrowing crate | `farrowing_crate` (1 x 2) | One sow and her litter. 5 x 7 ft (1.5 x 2.1 m) with "an 18in. creep area on both sides of a 24in. sow stall" (snippet: extension.org, which would not load 2026-11-14; confirmed in substance by NCSU: "Overall stall dimensions were 5 × 7 ft., with a 2 × 7 ft. sow crate", [NCSU swine](https://swine.ces.ncsu.edu/2021/08/effect-of-stall-size-number-of-heat-lamps-during-farrowing-part-1)). |
| Heat lamp | `heat_lamp` | Over a crate's creep: **250 W** on Wildcatter power ("250 watt heat lamps are placed over the creep areas", **TO VERIFY**, snippet: Iowa State, behind a bot wall 2026-11-14; NCSU's trial used "175 W HL"). Piglets under it are warm. |
| Feeder | `pig_feeder` | Takes grain, meal and rations; **5 pigs** a feeder ("120–280 5", [WI] citing PIH #32). |
| Drinker | `nipple_drinker` | Water from a trough block behind it; **12 pigs** a drinker ("1 drinker/10-12 pigs", PIC; "100–finish 12–15", [WI]). |
| Slurry pump | `slurry_pump` | Beside the pit: on Wildcatter power (**1.5 kW**, *proposed*) it moves slurry along `slurry_pipe` blocks to a digester feed pit or a `slurry_store`. |
| Slurry store | `slurry_store` | A tank of **100 m³** *(proposed)*; the gantry spreader takes slurry from it as it takes digestate. |
| Pasture gate | `pasture_gate` (the shed's) | Optional: a fenced pasture where pigs go by day and root (`pigs.md`). |

## Space
- **Pens:** one block holds **one pig of 45 kg or more**, two under 45 kg: 150 lb to market "8 sq ft" (0.74 m²), 60-100 lb "5"
  ([UMN Hog Producers Planning Guide](https://conservancy.umn.edu/server/api/core/bitstreams/6c4ffe6c-bb61-4a8e-a90b-f9e38ca7f4e1/content),
  citing PIH-55). Sows and boars take **2 blocks**: a gestating sow on slats "16" sq ft (1.49 m²). Each pen run holds one group: sows,
  growers, or a boar *(proposed)*.
- **Crates:** a sow due within 5 days goes to a free crate and stays until weaning *(proposed: as the lambing pens)*.

## The day
- **Feed and water** from feeders and drinkers by class (`pigs.md`); a pig with no feeder room or water goes short.
- **Warmth:** newborns need "32–35 °C" ([Smith et al. 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC6912423)). A piglet in a crate with
  a powered heat lamp is warm; without one it loses health as a cold chick does, until weaning *(proposed: the poultry rule)*.
- **Manure:** each pig's manure goes into the pit, **0.5 m³ of pit per pen block** *(proposed)*. A full pit makes the barn dirty
  (the barn's dirty-stall rule: health and growth). The pump empties it.
- **Pasture:** by day in dry weather pigs go out through the gate and root (`pigs.md`); at dusk they come in.

## Digester
- Slurry is a digester input: 89% water, VS 8.8% of fresh weight (0.96 / 10.9 lb, [MWPS]); "The average biogas production was 8.9 ft3/lb
  VS loaded" (0.56 m³ a kg VS) at "63.7 percent methane" ([NC State, swine digesters](https://www.agmrc.org/media/cms/swine_farms_in_north_carolina_DF18C4F9A0594.pdf)).

## Not in this pass
- Ventilation and room temperature, sorting, all-in-all-out, lagoons as earthworks (the slurry store stands in).

## Built, 2026-11-13
- **Code:** `piggery/` (`PigBarnBlocks`, `PigBarnControllerEntity`, `HeatLampEntity`, `SlurryPumpEntity`, `SlurryStoreEntity`). The layout is
  the sheep shed's check (`ShedLayout`) with the barn's cells.
- **Differences from the spec above:**
  - The **feeder and drinker are the barn's hay feeder and water trough**, not new blocks: the feeder already takes grain, meal and
    rations. The readout gives each one's room (5 pigs a feeder, 12 a drinker).
  - A **farrowing crate is one block**, not 1 x 2 *(proposed: a block's floor stands for the crate and its creep)*.
  - The **slurry store** only stores (a lagoon stand-in); the gantry spreader does not draw from it in this pass.
  - **Slurry into a digester:** a kg of slurry feeds the digester as 2.14 kg of dairy manure (49 / 23 m³ a tonne, *derived*; the
    digester counts one kind of manure).
  - Piglets need a powered heat lamp within 3 blocks above their crate or pen cell until weaning; without one they lose health as cold
    chicks do.
- **Tests:** game tests pig_barn_forms, sow_goes_to_crate, piglet_cold_without_lamp, slurry_pumped_to_store, pigs_root_pasture.

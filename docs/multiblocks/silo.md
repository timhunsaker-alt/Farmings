# Tower silo and grain bin

**Status: spec, 2026-11-11 (Phase 3; Tim: a tower silo and a grain bin).** Marks are as in `docs/crops.md`.

## Tower silo
| Part | Block | What it does |
|---|---|---|
| Silo stave | `silo_stave` | Concrete stave wall, laid as a ring of 3 x 3 or 5 x 5 with a hollow middle, up to **24 blocks** high *(proposed)* |
| Silo roof | `silo_roof` | Caps the ring |
| Forage blower | `forage_blower` | At the foot. Takes chopped forage from a belt or hopper and blows it up the fill pipe. **1.3 kWh per tonne of corn silage, 1.7 for haycrop** (Purdue, [Buckmaster](https://engineering.purdue.edu/ABE/People/Papers/dennis.buckmaster.1/equipment_matching.pdf)); its motor is **15 kW** *(proposed, inside the paper's matching range)* |
| Silo unloader | `silo_unloader` | At the foot. Unloads silage to a belt, hopper or chest. **18.6 kW (25 hp)**, up to **400 lb/min (181 kg/min)** of haylage (Harvestore XL bottom unloader, **TO VERIFY**: maker's sheet, [CST](https://www.cstindustries.com/wp-content/uploads/Harvestore-XL-Unloader.pdf)) |

- **Capacity:**
  - the hollow volume x **240 kg DM per m³**, the packing target of 15 lb DM/ft³ ([UW-Madison](https://fyi.extension.wisc.edu/forage/files/2014/01/BunkDens3.pdf));
  - tower silage runs from 106 to 434 kg DM/m³ by depth ([JOE 2013](https://archives.joe.org/joe/2013october/rb9.php)).
  - Example: a 3 x 3 ring has 1 m² inside, so a 20-block silo holds 4.8 t DM, about 14 t of corn silage.
- **One crop at a time:** corn silage or haylage. Adding the other kind is refused.
- **Fermenting:**
  - Silage is stable "in about 21 days" ([Penn State](https://extension.psu.edu/from-harvest-to-feed-understanding-silage-management)).
    In a tower, fermentation takes 7-21 days and full stabilisation 30-40 days ([UW-Madison, Managing tower silos](https://fyi.extension.wisc.edu/forage/files/2014/01/ManagingTowerSilos.pdf)).
  - Game: the silo can be unloaded **21 real days after the last fill** (1.4 game days). Before that the readout says FERMENTING.
- **Dry-matter loss:** 10.5% of the dry matter put in is lost. That is the middle of each of R.E. Muck's phases: filling 1-2%,
  fermentation 1-4%, effluent 0-2%, storage 1-10% ([Muck, UC Davis](https://alfalfa.sf.ucdavis.edu/sites/g/files/dgvnsk12586/files/media/documents/the_art_and_science_of_making_silage_by_richard_e_muck.pdf)).
- **Output:** `corn_silage` or `haylage` items, 25 kg as-fed each.

## Grain bin
| Part | Block | What it does |
|---|---|---|
| Bin wall | `grain_bin_wall` | Corrugated steel ring, 3 x 3 or 5 x 5, up to 16 high *(proposed)* |
| Bin roof | `grain_bin_roof` | Cone roof with a fill hole: an auger or belt delivers into it |
| Bin hopper | `grain_bin_hopper` | At the foot. Unloads grain to a belt, auger, hopper or chest by gravity, with no power |

- **Capacity:** the hollow volume x bulk density: corn **721 kg/m³**, oats **412**, barley **618** (56, 32 and 48 lb per bushel of
  1.2445 ft³; [Ohio State AGF-503](https://ohioline.osu.edu/factsheet/agf-503), *derived*).
- **One grain at a time.**
- **No spoilage:** grain in a bin doesn't spoil *(proposed: drying and aeration are not modelled)*.

## Not in this spec
- Silo gas.
- Bunker silos.
- Bagged silage.
- Grain drying.
- Moisture shrink.

## Built, 2026-11-11
- **Code:**
  - `farm/silo` (`SiloLayout`, `Silage`) is plain Java and unit-tested.
  - The parts are in `silo/`: the unloader is the silo's controller, the hopper is the bin's, the blower feeds the silo and the roof's
    fill hole feeds the bin.
- **Dry-matter loss:** the whole 10.5% is taken when forage goes in *(proposed simplification)*.
- **What the bin takes:** vanilla wheat goes in at oats' density *(proposed)*.
- **Hand-off:** the unloader and the bin hopper pass items to a belt, an auger, a Farmings machine or a container beside them.
- **Game tests:** silo_ferments_and_unloads, grain_bin_fills_and_empties.

## Loose ends, 2026-11-11 (spec): losses over time
The 10.5% taken at filling is replaced by Muck's phases, each at its own time:
- **Filling:** 1.5% (Muck, 1-2%), taken as forage goes in.
- **Effluent:** 1% (Muck, 0-2%), taken at filling.
- **Fermentation:** 2.5% (Muck, 1-4%), spread evenly over the 21 real days of fermenting. "primarily the CO2 produced by lactic acid
  bacteria".
- **Storage:** 0.18% a month from then on, *derived* from about 4.3% over 2 years in an oxygen-limiting tower (Muck, ARS; **TO
  VERIFY**: snippet). Muck's range is 1-10% for all storage.
- **Feed-out:** loss when unloading is not modelled.
- **Readout:** "lost so far: X kg DM".

### Built, 2026-11-11 (loose ends): losses over time
- **Losses:** 2.5% is taken at fill. Fermenting and storage losses are taken once a second, and the readout shows "lost so far".
- **Game test:** silo_loss_over_storage.

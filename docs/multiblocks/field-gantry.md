# Field gantry

**Status: spec, 2026-11-11 (Phase 3; Tim: a gantry on rails over the field, built the way Tailings builds its tunnel boring machine;
it tills and plants, harvests, mows, rakes, bales and spreads).** Marks are as in `docs/crops.md`.

A wide-span gantry, or controlled-traffic vehicle, is a real machine: a frame on two tracks that carries its tools across the
strip between them. For example:
- research gantries are 7.6 m and 10 m wide;
- the ASA-Lift WS 9600 spans 9.6 m, on tracks "at least 8-10 m" apart
  ([Res. Agr. Eng. 2018](https://rae.agriculturejournals.cz/pdfs/rae/2018/01/01.pdf)).

A linear-move irrigator rides its field the same way, on electric drive motors of 0.5-1.5 hp per tower
([NC State](https://content.ces.ncsu.edu/selection-and-management-of-efficient-center-pivot-and-linear-move-irrigation-systems)). No
vehicles (VISION).

## Parts
| Part | Block | What it does |
|---|---|---|
| Field rail | `field_rail` | Laid in two straight, parallel lines along a field edge, any length up to **256 blocks** *(proposed)*, at the field's height (one step up or down allowed). |
| End truck | `gantry_truck` (1 x 1 x 2) | One on each rail: wheels and a drive motor, **0.75 kW** each (the middle of the irrigator's 0.5-1.5 hp, rounded). |
| Bridge beam | `gantry_beam` | Spans from truck to truck across the field: **4-12 blocks** between the rails *(proposed: up to the 10 m research gantries)*. |
| Cab | `gantry_cab` | On the beam: the controller. Right-click reads it; holds the program and the gantry's bin. |
| Tool head | one of six blocks under the beam | The tool across the whole span (below). One at a time; swapped while the gantry is parked. |
| Cable junction | `field_junction` | A fixed block at the rail end, wired to Wildcatter **480 V** (industrial) power; the gantry trails its cable to it, as the TBM does. |
| Headland stop | `headland_stop` | Ends a rail line. The gantry unloads its bin into whatever touches the stop: a belt, an auger, a hopper or a chest. |

## Tool heads
Speeds are the typical field speeds in ASAE D497.5, Table 3 (2006,
[copy](https://faculty.uobasrah.edu.iq/uploads/teaching/1722757166.pdf)). Power uses the same standard's rotary-power parameters, with
the span as the working width.

| Tool | Block | Work on each strip it passes | Speed | Power |
|---|---|---|---|---|
| Seed drill | `tool_seed_drill` | turns grass and dirt into farmland and sows one seed a block from its seed hopper | 8.0 km/h | draft 300 N a row *(2.4-3.7 m drills)*, rows at 0.19 m *(proposed: a 7.5 in drill)*, so about 3.5 kW per m of span at 8 km/h *(derived)* |
| Combine / forage chopper | `tool_harvester` | grain crops: grain to the bin, straw left as a swath. Corn before 114 days and wilted alfalfa: chopped forage to the bin | 5.0 km/h | 6.0 kW + 3.3 kWh/t corn silage, 4.0 wilted alfalfa (forage harvester) |
| Mower | `tool_mower` | cuts mature alfalfa or grass into a swath on the ground; the plant drops to its regrowth stage | 11 km/h | 8.0 kW per m (disk mower-conditioner) |
| Rake | `tool_rake` | rakes a dried swath (1 real day after mowing) into a windrow | 8 km/h *(proposed; not in the table)* | 2 kW per m *(proposed)* |
| Round baler | `tool_baler` | takes a windrow that has dried (1.5 real days) into round bales of 454 kg | 8 km/h | 4.0 kW + 1.1 kWh/t (variable chamber) |
| Spreader | `tool_spreader` | puts one fertilizer use (bone meal's effect) on each crop block it passes, from its hopper of compost or liquid digestate | 8 km/h *(proposed)* | 2 kW per m *(proposed)* |

- **Motion:** the gantry moves one block along the rails at a time. At its tool's speed one block takes 1 m / speed: 0.45 s at
  8 km/h, 0.72 s at 5 km/h.
- **Work:** each step it works the row of blocks under the beam.
- **Bin:** **4,000 kg** *(proposed)*. When it is full, the gantry runs to the headland stop, unloads, and goes back to where it was.

## Running it
- **Starting:** right-click the cab to read it; sneak-right-click to start or stop the program.
- **The program:** work every row from the cab end to the far stop, unload at the stop, and come back parked.
- **Lifting and setting down (the TBM pattern):** while moving, the whole gantry is one entity carrying its parts; when it stops it
  sets its blocks back down. It saves its state either way.
- **Power:** the trucks' and tool's power is drawn from the junction through the trailing cable. With no power it stops where it is
  and reads NO POWER.
- **What it needs:**
  - the seed drill needs seed;
  - the spreader needs fertilizer;
  - a full bin with no room at the stop reads STOP FULL.
- **Swapping tools:** while the gantry is parked by its cab end, sneak-right-click the cab with a tool head item; the old head drops
  as an item.

## Swath and windrow
- Mown forage and combine straw lie on the ground as a `swath` block: a thin layer, walkable, with its crop and the tick it was cut.
- The rake turns a swath into a `windrow` block once 1 real day has passed. The baler takes the windrow once 1.5 real days have
  passed since cutting.
- Rain pauses drying *(proposed)*.
- Swaths and windrows are also raked and baled by hand: right-click with a hoe to rake, and break a dry windrow for its hay as loose hay
  items *(proposed)*.

## Not in this spec
- Irrigation.
- Turning on a pivot.
- Two gantries on one pair of rails.
- Autosteer.
- Field mapping.

---
id: power
title: Power from Wildcatter
chapter: getting_started
order: 40
icon: farmings:condensing_unit
docs: docs/VISION.md, docs/multiblocks/barn.md, docs/multiblocks/parlor.md, docs/multiblocks/creamery.md, docs/multiblocks/cold-room.md, docs/multiblocks/digester.md, docs/conveyors.md
---
Farmings has no grid, wire or pipe of its own. **Wildcatter** is required, and every machine with a motor or heater is a real load on
your Wildcatter grid.

## Hooking up
- Motors take Wildcatter's three-phase supply: **480** V in a North American world, **400** V in an IEC world.
- Power reaches a machine through Wildcatter's usual motor box and receptacles.
- Water troughs can be fed by a Wildcatter water pump and pipe, as well as by bucket.

## Some loads
- [Barn](barn) scraper drive: **0.75** kW.
- [Parlour](parlour) vacuum pump: **4** kW; bulk tank chiller: **2.6** kW.
- [Creamery](creamery) pasteurizer heater: **12** kW.
- [Cold room](cold-room): sized to the room. A **3** x **2** x **3** room draws about **0.75** kW.
- [Digester](digester) mixer: **6.5** kW. [Conveyor](conveyors) drives are rated by run length.

## No power
A machine without power stops its step and its readout says **NO POWER**. Work waits and picks up again when power returns. Animals are
never harmed by it.

## Making power
The [digester](digester) turns manure into biogas. Its gas holder offers the gas on Wildcatter steel pipe, and a Wildcatter gas set burns
it, at up to **850** kW.

---
id: parlour
title: The milking parlour
chapter: buildings
order: 30
icon: farmings:milking_unit
docs: docs/multiblocks/parlor.md
---
Cows walk from the [barn](barn) to the parlour once a game day, are milked into a cooled bulk tank, and walk back to their stalls.

## Build it
- **Floor:** a vacuum pump at the far end, then a milk line, one block per stall, running under the parlour controller.
- **Above the line:** a milking unit at each stall, and the controller at the tank end.
- **Stalls:** a parlour stall (**2** blocks long) under each unit, from **2** to **12** stalls. A sheep stall (one block) milks dairy ewes
  and does instead.
- **The bulk tank** (**2** x **2** x **2**) beside the controller.
- **Holding pen and cow lane:** holding-pen floor touching the stalls, with room for every stall (**3** stalls need **5** blocks), and a
  cow lane of up to **64** blocks to the end of the barn's alley.

```grid
layer 1
VLLLLTT
.PPP.TT
.PPP...
.HHHHH.
.N.....
.N.....
layer 2
.UUUCTT
.....TT
key V farmings:vacuum_pump
key L farmings:milk_line
key T farmings:bulk_tank
key P farmings:parlor_stall
key H farmings:holding_pen
key N farmings:cow_lane
key U farmings:milking_unit
key C farmings:parlor_controller
```

## Running it
- The vacuum pump draws **4** kW and the tank's chiller **2.6** kW.
- The tank holds **1,000** L and cools milk to **4** C. Above **10** C it is WARM and no new milking starts.
- Faults: NO POWER, TANK FULL, TANK WARM, or NO LANE TO BARN.
- An empty bucket on the tank takes **10** L as a milk bucket. A milk line from the tank to a [creamery](creamery) pipes the milk across.
- A tank holds one kind of milk: cow, sheep or goat.
- Ewes and does reach the sheep stalls from the [sheep shed](sheep-shed) pen behind them.

{item:farmings:bulk_tank} {item:farmings:milking_unit} {item:minecraft:milk_bucket}

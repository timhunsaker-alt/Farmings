---
id: field-gantry
title: The field gantry
chapter: fields
order: 20
icon: farmings:gantry_cab
docs: docs/multiblocks/field-gantry.md
---
No tractors here: a gantry on two rails carries a tool across the field.

## Build it
- Two straight, parallel lines of **field rail**, up to **256** blocks long.
- A **gantry truck** on each rail, and **gantry beam** from truck to truck, with **4-12** blocks of field between the trucks.
- The **gantry cab** on the beam, above one truck. It is the controller.
- A **field junction** beside the home end of a rail, wired to Wildcatter **480 V** power.
- A **headland stop** at the far end of a rail. Put a belt, auger, hopper or chest against it.

```grid
layer 1
J.....
R....R
R....R
R....R
R....R
H.....
layer 2
......
T....T
layer 3
......
BBBBBB
layer 4
......
C.....
key R farmings:field_rail
key J farmings:field_junction
key H farmings:headland_stop
key T farmings:gantry_truck
key B farmings:gantry_beam
key C farmings:gantry_cab
```

## Tools
Fit one tool head at a time: park the gantry at the cab end and sneak-right-click the cab with the head.
- **Seed drill** (**8.0 km/h**): tills and sows one seed a block.
- **Harvester** (**5.0 km/h**): grain to the bin and straw left in a swath, or chopped forage.
- **Mower** (**11 km/h**): cuts hay into a swath.
- **Rake** (**10 km/h**): rakes a swath once it has dried **1** real day.
- **Baler** (**8 km/h**): bales hay **1.5** real days after mowing.
- **Spreader** (**11 km/h**): spreads compost, liquid digestate or bone meal, one item a spread. A **1 t** bag of TSP, DAP or MOP from
  Wildcatter is **100** spreads of **10 kg**: the cab opens a bag, keeps what is left and opens the next only when it runs out.

{item:farmings:tool_seed_drill} {item:farmings:tool_harvester} {item:farmings:tool_mower} {item:farmings:tool_rake} {item:farmings:tool_baler} {item:farmings:tool_spreader}

## Running it
Sneak-right-click the cab to start or stop. It works every row to the far stop and comes back. Its bin holds **4,000 kg**; when full it
unloads at the stop as **454 kg** round bales, **1 kg** grain items and **25 kg** chopped forage. A hoe rakes a dry swath by hand. See
[crops](crops).

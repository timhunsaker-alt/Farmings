---
id: digester
title: Digester and biogas
chapter: waste
order: 30
icon: farmings:gas_holder
docs: docs/multiblocks/digester.md, docs/multiblocks/pig-barn.md
---
A digester turns manure into biogas for a Wildcatter gas set, plus fertilizer.

## Build it
- A **digester tank**, a **3 x 3 x 3** tank placed from one item on clear ground.
- A **gas holder** on its roof centre (**200 m³**).
- A **digester feed pit** against one side, and a **digestate tank** (**50 t**) against another.
- A **screw press** beside the digestate tank.

```grid
layer 1
.KKK..
FKKKDS
.KKK..
layer 2
.KKK
.KKK
.KKK
layer 3
.KKK
.KKK
.KKK
layer 4
....
..G.
key K farmings:digester_tank
key F farmings:digester_feed_pit
key D farmings:digestate_tank
key S farmings:screw_press
key G farmings:gas_holder
```

## Running it
- Feed manure or poultry litter into the pit by hand, belt or hopper. A pit touching a barn's collection pit pulls up to **4** manure items
  a second. Pig slurry counts as **2.14 kg** of dairy manure per kg.
- The tank's mixer (**6.5 kW**) needs Wildcatter **480 V**. With no power it makes no gas.
- Each **50 kg** of manure gives **1.15 m³** of biogas over **20** real days (**1.3** game days). The tank burns half to stay warm; the
  other half goes to the holder. The gas is **60%** methane.
- Run a steel pipe from the holder to a Wildcatter gas set. It burns biogas at up to **850 kW**.

{item:farmings:manure} {item:farmings:poultry_litter} {item:farmings:separated_solids} {item:farmings:liquid_digestate}

## Fertilizer
The screw press (**5.5 kW**) splits digestate: **10%** comes out as **10 kg** separated solids for [compost](compost), **90%** as **100 L**
liquid digestate. One liquid digestate is one use of bone meal, by hand or with the [field gantry](field-gantry)'s spreader.

## How much power?
About **100** cows give **57.5 m³** to the holder each game day: **8.5** minutes of a **1 MW** set. Enough for lights and fans, not a grid.

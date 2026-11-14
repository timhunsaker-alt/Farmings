---
id: pig-barn
title: The pig barn
chapter: buildings
order: 80
icon: farmings:pig_barn_controller
docs: docs/multiblocks/pig-barn.md, docs/animals/pigs.md
---
Slatted pens for your [pigs](pigs), with farrowing crates, heat lamps for the piglets and a manure pit under the floor.

## Build it
- A **pig barn controller** touching the pen floor.
- **Slatted floor:** at least **3** blocks. Each joined run of it is one pen, for one group: sows, growers or a boar.
- **Farrowing crates**, one block each, for sows and their litters, part of the pen.
- A **hay feeder** (the pig feeder) and a **water trough** (the drinker) touching the pen.
- A **heat lamp** within **3** blocks above each crate, on Wildcatter power (**250** W).
- Optional: a **pasture gate** onto fenced grass, where pigs root by day.

```grid
layer 1
.H..W.
CSSSSX
.SSSSX
..G...
layer 3
.....L
.....L
key C farmings:pig_barn_controller
key S farmings:slatted_floor
key X farmings:farrowing_crate
key H farmings:hay_feeder
key W farmings:water_trough
key G farmings:pasture_gate
key L farmings:heat_lamp
```

## Running it
- The controller takes in tame pigs within **48** blocks.
- One block holds one pig of **45** kg or more, or two smaller ones. Sows and boars take **2** blocks.
- A feeder serves **5** pigs and a drinker **12**; the readout shows the room.
- A sow due within **5** days moves to a free crate and stays until weaning. Piglets without a powered lamp get cold and lose health.
- Manure falls through the slats into the pit: **0.5** m³ of pit per pen block. A full pit makes the barn dirty.

## Slurry
A **slurry pump** (**1.5** kW) moves the slurry along **slurry pipe** to a [digester](digester) feed pit or a **slurry store** of **100** m³.

{item:farmings:slurry_pump} {item:farmings:slurry_pipe} {item:farmings:slurry_store}

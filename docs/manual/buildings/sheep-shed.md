---
id: sheep-shed
title: The sheep shed
chapter: buildings
order: 40
icon: farmings:shed_controller
docs: docs/multiblocks/sheep-shed.md
---
Home for your [sheep](sheep) and [goats](goats): a bedded pen, hay, water, and a fenced pasture to graze by day.

## Build it
- A **shed controller** on the floor, touching the pen.
- **Sheep pen** floor joined to it: at least **3** blocks, at most **256**. Add **lambing pens** for ewes about to lamb.
- A **hay feeder** and a **water trough** touching the pen.
- Optional: a **pasture gate** touching the pen, opening onto fenced grass of up to **4,096** blocks.
- Walls and fences are up to you; the shed doesn't check them.

```grid
layer 1
.H..W.
CPPPPL
.PPPPL
..G...
key C farmings:shed_controller
key P farmings:sheep_pen
key L farmings:lambing_pen
key G farmings:pasture_gate
key H farmings:hay_feeder
key W farmings:water_trough
```

## The day
- **3** pen blocks hold **2** sheep; a goat takes **2** blocks. Each pen run holds one species.
- The controller takes in tame sheep within **48** blocks until it is full.
- In clear weather the flock goes out by day. A grazed grass block gives **1.5** kg of dry matter and turns to dirt; grass spreads back.
- At dusk or in rain they come home to hay (**85** kg of dry matter a block) and water.
- A ewe due within **3** real days goes to a free lambing pen and stays **2** real days after lambing.

## Bedding
- Put straw in by hand or hopper: loose straw or a straw bale, up to **1,000** kg. Each housed sheep uses **1** kg a day.
- A hopper under the controller pulls out manure items of **50** kg. The store holds **64** items.
- A full store or no clean straw makes the bedding dirty, which costs health.

{item:farmings:straw} {item:farmings:straw_bale} {item:farmings:manure}

Add a [shearing shed](shearing-shed) and a [sheep dog](sheep-dog).

---
id: shearing-shed
title: The shearing shed
chapter: buildings
order: 50
icon: farmings:shearing_stand
docs: docs/multiblocks/shearing-shed.md, docs/animals/sheep.md
---
A powered shearing stand built onto your [sheep shed](sheep-shed). It calls in each sheep whose fleece is ready and shears it.

## Build it
- A **shearing stand**: the controller, on Wildcatter power (**0.5** kW).
- A **catching pen** touching the stand and touching the sheep shed's pen. At most **16** blocks.
- A **wool table** touching the stand. It keeps **9** stacks; a hopper below takes them.
- Optional: a **wool press** beside the table.

```grid
layer 1
CPPPW
HPPP.
..K..
XTS..
key C farmings:shed_controller
key P farmings:sheep_pen
key W farmings:water_trough
key H farmings:hay_feeder
key K farmings:catching_pen
key S farmings:shearing_stand
key T farmings:wool_table
key X farmings:wool_press
```

## Shearing
- The stand calls one sheep at a time: one with **300** days or more of fleece, not nursing, and not within **28** real days of lambing.
- Shearing takes **5** seconds.
- The fleece is split into the body fleece and skirtings (**12%** of the weight, one grade lower).
- The press bales every **8** fleeces of one grade and colour into a wool bale.
- Hand shears still work anywhere, but give one grade lower and no skirtings.
- With no power the stand reads NO POWER and lets the sheep go.

{item:farmings:fleece} {item:farmings:skirtings} {item:farmings:wool_bale}

Take the wool to the [wool mill](wool-mill).

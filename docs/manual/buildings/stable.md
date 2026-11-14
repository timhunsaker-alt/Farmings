---
id: stable
title: The stable
chapter: buildings
order: 90
icon: farmings:stable_controller
docs: docs/multiblocks/stable.md, docs/animals/horses.md
---
Stalls, hay, water and bedding for your [horses](horses), with an optional paddock to graze by day.

## Build it
- A **stable controller** touching the stall floor.
- **Horse stall** floor: every **12** blocks of it is one stall, so **24** blocks make **2** stalls. Fences between stalls are up to you.
- A **hay feeder** and a **water trough** touching the floor.
- Optional: a **pasture gate** onto a fenced paddock.

```grid
layer 1
.H....W
CSSSSSS
.SSSSSS
.SSSSSS
.SSSSSS
...G...
key C farmings:stable_controller
key S farmings:horse_stall
key H farmings:hay_feeder
key W farmings:water_trough
key G farmings:pasture_gate
```

## The day
- The controller takes in tame horses within **48** blocks. Each gets the middle block of its stall as home.
- By night horses go to their stalls; by day out to the paddock, if there is one.
- Each eats **2.25%** of its body weight in dry matter a day and drinks **0.05** L per kg. Foals nurse until weaning.
- A horse being ridden is out working and is skipped.

## Bedding and fitness
- Straw goes in by hand or hopper. Manure comes out of a hopper under the controller, at **5.1%** of body weight a day.
- Dirty bedding costs health.
- Training is riding. A horse fed under **95%** of its need gains no fitness. The readout lists every horse's fitness.

{item:farmings:straw_bale} {item:farmings:hay_feeder} {item:minecraft:saddle}

Race them at [the race track](race-track).

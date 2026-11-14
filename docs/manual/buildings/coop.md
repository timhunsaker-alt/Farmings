---
id: coop
title: Coop and run
chapter: buildings
order: 60
icon: farmings:nest_box
docs: docs/multiblocks/coop.md, docs/animals/poultry.md
---
The coop houses your tame birds: [chickens](chickens), [turkeys](turkeys), [ducks](ducks), [quail](quail), [geese](geese) and
[guinea fowl](guinea-fowl). It feeds and waters them, collects eggs and litter, and lets them out on a fenced run by day.

## Build it
- **Coop floor** cells, joined side to side, at most **256** cells.
- A **coop controller** on the floor level, touching the floor. Right-click it to read the coop.
- At least one **nest box**, one **grain feeder** and one **water trough** touching a floor cell.
- **Perches** for chickens, turkeys and guinea fowl to roost on.
- A **pop hole** in the run fence, touching the floor. The fenced run beyond it may be up to **1,024** cells.

```grid
layer 1
FFFFFF
F....F
F....F
FFHFFF
NCCCCK
NCCCCW
.PGP..
key F minecraft:oak_fence
key H farmings:pop_hole
key C farmings:coop_floor
key K farmings:coop_controller
key N farmings:nest_box
key W farmings:water_trough
key G farmings:grain_feeder
key P farmings:perch
```

## Using it
- The controller takes in tame birds within **48 blocks** until the coop is full. It never takes wild birds.
- A nest box holds **16** eggs; a hopper under it pulls them out. If every nest is full, hens lay dirty floor eggs.
- The grain feeder holds **256** grain items, by hand or hopper.
- Birds go out on the run from morning to dusk in clear weather, and come in at night.
- Litter piles up as **poultry litter** items of **10 kg**, up to **64**. Pull them with a hopper under the controller; a full
  store costs the birds **2%** health a day. Litter goes to [compost](compost).

## Broody hens
A hen whose nest holds at least **6** eggs may go broody. She sits on up to **12** fertile eggs until they hatch, then keeps her
young warm. Or use the [hatchery](hatchery).

{item:farmings:chicken_egg} {item:farmings:poultry_litter}

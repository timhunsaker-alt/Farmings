---
id: corral
title: The taming corral
chapter: buildings
order: 10
icon: farmings:corral_post
docs: docs/taming.md
---
Wild animals become herd animals in a corral.

## Build it
- A closed fence around a floor of up to **1,024** cells.
- A **corral post** on the floor: right-click it to read it. The first player to read it owns it.
- At least one **corral gate** in the fence. Animals pass in, never out.
- A **bait trough** with hay and a **water trough** inside.

```grid
layer 1
FFFFFFF
F.....F
G..P..F
F.B.W.F
FFFFFFF
key F minecraft:oak_fence
key G farmings:corral_gate
key P farmings:corral_post
key B farmings:bait_trough
key W farmings:water_trough
```

## Settling
A filled bait trough draws wild animals in through the gate. After **7** real days settled on its hay and water, each animal is tamed
and the corral's owner becomes its owner.

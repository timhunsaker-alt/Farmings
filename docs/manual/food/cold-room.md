---
id: cold-room
title: The cold room
chapter: food
order: 40
icon: farmings:evaporator
docs: docs/multiblocks/cold-room.md, docs/food.md, docs/recipes.md
---
A walk-in fridge you build to any size. Chests and barrels inside keep food cold, so it lasts its fridge life. See
[spoilage](spoilage).

## Build it
- **Cold room panels** for the walls, floor and ceiling. **3** iron, **3** wool (or felt) and **3** iron make **4**.
- At least one **cold room door**, **2** high.
- An **evaporator** set in a wall, facing in. It is the controller: right-click to read it.
- A **condensing unit** outside, touching a wall. It runs on Wildcatter three-phase power.
- The inside must be a box, from **1** x **1** x **1** to **10** x **4** x **10**. Edges and corners don't need panels.

```grid
layer 1
......
.PPP..
.PPP..
.PPP..
......
layer 2
.PPP..
P...PU
D.c.P.
P...P.
.PPP..
layer 3
.PEP..
P...P.
D...P.
P...P.
.PPP..
layer 4
......
.PPP..
.PPP..
.PPP..
......
key P farmings:cold_room_panel
key D farmings:cold_room_door
key E farmings:evaporator
key U farmings:condensing_unit
key c minecraft:chest
```

## Running it
- The room is held at **2** °C. A new room starts at **20** °C and pulls down within a few minutes. It reads COLD at **4** °C or below.
- A **3** x **2** x **3** room draws about **0.75** kW; the largest about **15** kW.
- Opening a door lets warm air in. Without power the room warms, from **2** to **5** °C in **4** hours, and then food ages faster.
- Only food in containers is kept cold, not items lying on the floor.
- Bottled milk keeps **7** days inside, against **1** day out of the cold.

{item:farmings:cold_room_panel} {item:farmings:cold_room_door} {item:farmings:evaporator} {item:farmings:condensing_unit}

The [curing room](curing-room) rack works inside a cold room.

---
id: race-track
title: The race track
chapter: buildings
order: 100
icon: farmings:start_gate
docs: docs/multiblocks/race-track.md
---
Build a track and race your [horses](horses). There is no betting and no prize: the records are the prize.

## Build it
- Lay **race track** blocks in a flat ring around an infield: an oval or a rounded rectangle.
- Set a **start gate** beside the track. The track must be at least **3** wide there; each block across is one stall.
- Put a **finish post** beside the track. You read results there.
- Rails are optional; the track forms without them.

```grid
layer 1
.....S.....
TTTTTTTTTTT
TTTTTTTTTTT
TTTTTTTTTTT
TTT.....TTT
TTT.....TTT
TTT.....TTT
TTTTTTTTTTT
TTTTTTTTTTT
TTTTTTTTTTT
........F..
key T farmings:race_track
key S farmings:start_gate
key F farmings:finish_post
```

## Racing
- **Sneak-use** the gate to set the distance: **402** m, **1,207** m, **1,609** m (a mile), **2,012** m or **2,414** m.
- Ride a horse into a stall, or lead a tame one in: an AI jockey rides it.
- **Use** the gate: a three-second count, then it opens. Races run counter-clockwise.
- Each horse is timed to **0.05** s when it has covered the distance, wherever that falls on the loop.
- A horse off the track for **2** s is out.
- A win counts when two or more horses ran. A solo run is a timed run.
- Each horse keeps its best time at each distance, races run and wins, and a race counts as training.

Record them in the **stud book**: see [the herd book](herd-book).

{item:farmings:stud_book} {item:farmings:track_rail}

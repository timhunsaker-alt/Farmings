---
id: barn
title: The barn
chapter: buildings
order: 20
icon: farmings:barn_controller
docs: docs/multiblocks/barn.md
---
A freestall barn houses your [cattle](cattle): stalls to lie in, hay to eat, water, and a powered scraper that pushes manure into a pit.

## Build it
- Two rows of stalls facing a **3**-wide alley, from **2** to **12** stalls a side. A stall is a bed (**3** blocks long) with a divider
  (**2** high) on each side, so beds and dividers alternate.
- A feed bunk with a hay feeder above it in line with every stall.
- The alley: alley floor with a scraper rail down the middle, a manure scraper on the rail, the scraper drive at the far end, and the
  collection pit dug in at the near end, under and beside the barn controller.
- The alley is twice as long as a stall row: **18** for **4** stalls a side. The extra length goes at both ends.
- At least one water trough in each bed row. Right-click the controller to form it; it names any missing part.

```grid
layer 1
..........PP
..........PP
..........PP
layer 2
...F.F......
..DBDBD.....
..DBDBD.....
..DBDBD.WW..
AAAAAAAAAAPP
RRRRRRRRRRPP
AAAAAAAAAAPP
..DBDBD.WW..
..DBDBD.....
..DBDBD.....
...F.F......
layer 3
...H.H......
..D.D.D.....
..D.D.D.....
..D.D.D.....
.M..........
SM........C.
.M..........
..D.D.D.....
..D.D.D.....
..D.D.D.....
...H.H......
key P farmings:collection_pit
key F farmings:feed_bunk
key D farmings:stall_divider
key B farmings:stall_bed
key W farmings:water_trough
key A farmings:alley_floor
key R farmings:scraper_rail
key H farmings:hay_feeder
key M farmings:manure_scraper
key S farmings:scraper_drive
key C farmings:barn_controller
```

## Running it
- One cow per stall. A feeder holds up to **4** hay blocks; a milking cow eats about a quarter of a **100** kg hay block a day.
- A trough holds **1,000** L, filled by bucket or by a Wildcatter pump's pipe.
- The scraper (**0.75** kW) makes one pass a game day, or on a redstone signal. The pit holds **384** manure items of **50** kg.
- Manure in front of a stall makes it dirty: five items is fully dirty and costs **10%** of her milk. Clear it by hand with a
  manure fork.

{item:farmings:manure} {item:farmings:manure_fork}

Next: [the parlour](parlour), [manure](manure).

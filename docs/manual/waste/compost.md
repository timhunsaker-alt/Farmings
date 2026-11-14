---
id: compost
title: Compost windrow
chapter: waste
order: 20
icon: farmings:compost
docs: docs/multiblocks/compost.md
---
A compost windrow turns manure, litter and straw into fertilizer.

## Build it
- A straight row of **compost pad**, **4-64** cells long.
- A **compost controller** at one end. It takes inputs by belt, hopper or hand.
- A **windrow turner** at the other end. It runs on Wildcatter power (**41 kW**).

```grid
layer 1
CPPPPPPT
key C farmings:compost_controller
key P farmings:compost_pad
key T farmings:windrow_turner
```

## The pile
Put in manure (**50 kg** items), poultry litter, separated digestate solids, straw, water (**1** bucket is **1,000 kg**) and spoiled
food. The pile needs a carbon-to-nitrogen ratio of **20-40** and **40-65%** moisture. Manure alone is too wet; straw adds carbon, litter
brings both into range.

A working mix: **500 kg** of manure, **200 kg** of barley straw and **100 kg** of litter is C:N **26** at **61%** moisture.

Sneak-right-click the controller to start. The readout names any fault: TOO WET, TOO DRY, NEEDS CARBON or NEEDS NITROGEN.

## Turning and finishing
- The turner turns the pile every **11.2** real days, about **0.75** game days. Without power the pile waits.
- After **56** real days (**3.7** game days) and **5** turns, it is compost.
- Finished compost weighs **42.5%** of what went in, as **10 kg** items.

{item:farmings:compost}

One compost item is one use of bone meal on a crop, sapling or grass. Use it by hand or load it into the gantry's spreader. See
[the field gantry](field-gantry) and [manure](manure).

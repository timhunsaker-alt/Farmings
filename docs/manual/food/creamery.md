---
id: creamery
title: The creamery
chapter: food
order: 10
icon: farmings:creamery_controller
docs: docs/multiblocks/creamery.md
---
The creamery pasteurizes milk and turns it into bottled milk, cream, butter, cheddar or chèvre.

## Build it
The **creamery controller** at one end, then every other part once, in one straight row on the same level, in any order, with no gaps:
receiving tank, batch pasteurizer, cream separator, butter churn, cheese vat, cheese press, aging shelf and bottler.

```grid
layer 1
CRRBSUVVPAO
.RR........
layer 2
.RRB.....A.
.RR........
key C farmings:creamery_controller
key R farmings:receiving_tank
key B farmings:batch_pasteurizer
key S farmings:cream_separator
key U farmings:butter_churn
key V farmings:cheese_vat
key P farmings:cheese_press
key A farmings:aging_shelf
key O farmings:bottler
```

## Milk in
- A milk bucket pours **10** L into the receiving tank (**1,000** L).
- Or run **milk line** from a [parlour](parlour) bulk tank, up to **64** blocks.
- The creamery works one kind of milk at a time: cow, sheep or goat.

## Modes
Sneak-right-click the controller to pick a mode; right-click to read it. Every batch of up to **200** L is first held at **63** C for
**30** minutes.
- **BOTTLE:** pasteurized milk.
- **CREAM:** cream at **36%** fat and skim.
- **BUTTER:** butter at **80%** fat, in **0.5** kg blocks, plus buttermilk.
- **CHEESE:** cheddar pressed into **10** kg wheels, aged **4** game days. A wheel crafts into **8** slices.
- **CHEVRE:** goat milk only, **17** kg of chèvre per **100** kg of milk.

**200** L of Holstein milk gives about **9.1** kg of butter, or about **20.5** kg of cheddar. Cheese leaves whey, which is bottled too.

## Taking products
Right-click the bottler with glass bottles. Butter comes off the churn, wheels and chèvre off the aging shelf, by hand or by hopper.

{item:farmings:bottled_milk} {item:farmings:cream_bottle} {item:farmings:butter} {item:farmings:cheddar_wheel} {item:farmings:chevre} {item:farmings:whey_bottle}

The parts draw power: the heater **12** kW, separator and churn **1.5** kW each. Keep products in a [cold room](cold-room).

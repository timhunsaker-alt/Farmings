---
id: feed-mill
title: Feed mill and TMR mixer
chapter: fields
order: 50
icon: farmings:ration
docs: docs/multiblocks/feed-mill.md
---
Grind grain into meal, then mix a balanced ration. A good ration means more milk, faster growth and more eggs.

## Hammer mill
Put grain, alfalfa hay, calcite or fish into its top; meal comes out of its output face, **1 kg** in, **1 kg** out. Its motor is **9.1 kW**
on Wildcatter **480 V**.

{item:farmings:hammer_mill} {item:farmings:corn_meal} {item:farmings:oat_meal} {item:farmings:barley_meal} {item:farmings:alfalfa_meal} {item:farmings:fish_meal} {item:farmings:ground_limestone}

**Mineral premix:** bone meal + ground limestone, shapeless, makes **4**.

## TMR mixer
- A **2 x 2 x 2** machine placed from one item, also on **480 V**.
- Feed it ingredients by belt, auger, hopper or hand.
- **4 minutes** after the last ingredient, it mixes the batch into **25 kg** ration items. A batch is about **2.3 t**, enough for about **40**
  cows for a day.
- A barn hay feeder beside the mixer, or at the end of a belt, takes rations straight in.

{item:farmings:tmr_mixer} {item:farmings:ration} {item:farmings:mineral_premix}

## What a ration does
A ration's tooltip shows its analysis. Each animal scores what it eats from **0** to **1**:
- **Dairy cows:** CP **15-18%**, NDF **25-30%**, at least **40%** forage, and premix. Milk is x (**0.67** + **0.33** x score), so poor
  hay alone gives about two thirds of the milk.
- **Growing cattle:** growth x (**0.29** + **0.71** x score), so a poor ration grows them far slower.
- **Ewes and does in milk:** CP **11.6%** or more. Milk x (**0.83** + **0.17** x score).
- **Lambs:** growth x (**0.8** + **0.2** x score).
- **Laying hens:** CP **16%** and Ca **3-4%**. Eggs x (**0.75** + **0.25** x score).

See [crops](crops), [the silo](silo) and [the barn](barn).

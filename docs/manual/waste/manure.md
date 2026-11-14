---
id: manure
title: Manure
chapter: waste
order: 10
icon: farmings:manure
docs: docs/waste.md, docs/multiblocks/barn.md, docs/multiblocks/pig-barn.md, docs/multiblocks/stable.md
---
Every animal makes manure, and you have to collect it. Left lying, it makes stalls dirty: the animals lose health and milk, but never die
from it.

{item:farmings:manure} {item:farmings:manure_fork} {item:farmings:poultry_litter}

## In the barn
- A milking cow makes about **67 kg** of manure a day, a dry cow **39 kg**, a heifer **24 kg**. One manure item is **50 kg**.
- Clear stalls and the alley by hand with a **manure fork** (**250** uses).
- Or let the **powered scraper** do it: on **0.75 kW** Wildcatter power it makes one pass per game day, or on a redstone signal, and pushes
  the alley's manure into the **collection pit**.
- The pit holds **64** items per cell, **384** in all. When it is full the scraper stops and reads PIT FULL.
- One item in front of a stall is normal. Each extra item makes it dirtier; **5** items is fully dirty and costs **10%** of her milk.

See [the barn](barn).

## Other animals
- **Pigs:** manure falls through the slatted floor into the pit. A **slurry pump** moves it along slurry pipe to a digester feed pit or a
  slurry store. See [the pig barn](pig-barn).
- **Horses:** about **5.1%** of body weight a day goes into each stall's straw bedding. A hopper under the controller takes the manure.
  See [the stable](stable).
- **Poultry:** the coop collects litter in **10 kg** items. See [the coop](coop).

## Where it goes
- [Compost](compost): manure and straw become fertilizer.
- [The digester](digester): manure becomes biogas for power, then fertilizer.

---
id: hatchery
title: Hatchery
chapter: buildings
order: 70
icon: farmings:incubator
docs: docs/multiblocks/hatchery.md, docs/animals/poultry.md
---
Two single blocks that hatch and raise young birds without a broody hen. Both run on Wildcatter low-voltage [power](power).

{block:farmings:incubator} {block:farmings:brooder}

## Incubator
- Holds **24** fertile eggs of any species, mixed, put in by hand or hopper. Infertile eggs are refused.
- It turns the eggs and keeps them warm; the readout shows each egg's day and the next hatch.
- Draws **0.025 kW**. With no power the eggs' clocks pause, and nothing is lost.
- The young bird appears on top of the incubator and needs heat at once.
- Days to hatch: chicken **21**, turkey and duck **28**, Muscovy **35**, Coturnix **17**, goose **30**, guinea fowl **27**.
- A fertile egg left cold for over **7 real days** before it starts won't hatch.

## Brooder
- A heat lamp hung over the floor. It warms up to **25** young birds within **2 blocks**.
- Draws **0.25 kW**. With no power, young birds under it are cold: they lose health and don't grow.
- Put a grain feeder and water trough within **4 blocks**; the brooder isn't part of a coop.
- Once feathered, young birds become growers, and a [coop](coop) within **48 blocks** takes them in.

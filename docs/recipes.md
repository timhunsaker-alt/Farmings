# Farmings recipes

Written by `tools/make_assets.py` (2026-11-09). All recipes are *(proposed)*: there is no real-world source for a crafting grid.
They use vanilla materials and Wildcatter's steel ingot (Wildcatter is a hard dependency). Powered parts take copper and redstone (the motor or heater);
tanks take a bucket. Animal and machine products have no recipe: manure, filled straws, raw steaks (from beef animals), bottled dairy, butter
and cheddar wheels (from the creamery). The game test `recipes_cover_all_items` checks that every other item has one.

| Result | Grid | Key | Makes |
|---|---|---|---|
| aging_shelf | `WWW / T.T / WWW` | T stick, W planks | 1 |
| alley_floor | `KK / KK` | K gray concrete | 4 |
| barn_controller | `SRS / CIC / SSS` | C copper ingot, I iron ingot, R redstone, S steel ingot (Wildcatter) | 1 |
| batch_pasteurizer | `SBS / SRS / SCS` | B bucket, C copper ingot, R redstone, S steel ingot (Wildcatter) | 1 |
| bottler | `IGI / IRI / III` | G glass, I iron ingot, R redstone | 1 |
| bulk_tank | `SSS / SBS / SCS` | B bucket, C copper ingot, S steel ingot (Wildcatter) | 1 |
| cow_lane | `N N / KKK` | K gray concrete, N iron bars | 6 |
| holding_pen | `NNN / KKK` | K gray concrete, N iron bars | 6 |
| butter_churn | `WSW / WCW / WRW` | C copper ingot, R redstone, S steel ingot (Wildcatter), W planks | 1 |
| cheddar | shapeless | farmings:cheddar_wheel | 8 |
| cheese_press | `WIW / W.W / WWW` | I iron ingot, W planks | 1 |
| cheese_vat | `S.S / SBS / SCS` | B bucket, C copper ingot, S steel ingot (Wildcatter) | 1 |
| collection_pit | `K.K / K.K / KKK` | K gray concrete | 1 |
| cream_separator | `SGS / CRC / SIS` | C copper ingot, G glass, I iron ingot, R redstone, S steel ingot (Wildcatter) | 1 |
| creamery_controller | `SRS / GCG / SSS` | C copper ingot, G glass, R redstone, S steel ingot (Wildcatter) | 1 |
| empty_straw | shapeless | wildcatter:plastic_sheet | 8 |
| feed_bunk | `K.K / KKK` | K gray concrete | 1 |
| hay_feeder | `NNN / N.N / SSS` | N iron bars, S steel ingot (Wildcatter) | 1 |
| herd_book | shapeless | minecraft:writable_book + minecraft:wheat | 1 |
| manure_fork | `I.I / .T. / .T.` | I iron ingot, T stick | 1 |
| manure_scraper | `SSS / III` | I iron ingot, S steel ingot (Wildcatter) | 1 |
| milk_boots | `M.M / M.M` | M milk bucket | 1 |
| milk_chestplate | `M.M / MMM / MMM` | M milk bucket | 1 |
| milk_helmet | `MMM / M.M` | M milk bucket | 1 |
| milk_leggings | `MMM / M.M / M.M` | M milk bucket | 1 |
| milk_line | `SGS` | G glass, S steel ingot (Wildcatter) | 6 |
| milking_unit | `IGI / SBS` | B bucket, G glass, I iron ingot, S steel ingot (Wildcatter) | 1 |
| parlor_controller | `SRS / CGC / SSS` | C copper ingot, G glass, R redstone, S steel ingot (Wildcatter) | 1 |
| parlor_stall | `S.S / S.S / KKK` | K gray concrete, S steel ingot (Wildcatter) | 1 |
| receiving_tank | `SSS / SBS / SSS` | B bucket, S steel ingot (Wildcatter) | 1 |
| scraper_drive | `SCS / CRC / SIS` | C copper ingot, I iron ingot, R redstone, S steel ingot (Wildcatter) | 1 |
| scraper_rail | `S.S / SRS / S.S` | R redstone, S steel ingot (Wildcatter) | 8 |
| stall_bed | `LLL / KKK` | K gray concrete, L wool | 1 |
| stall_divider | `SSS / S.S / S.S` | S steel ingot (Wildcatter) | 1 |
| vacuum_pump | `SCS / CRC / SSS` | C copper ingot, R redstone, S steel ingot (Wildcatter) | 1 |
| water_trough | `SBS / SSS` | B bucket, S steel ingot (Wildcatter) | 1 |

Cooking: each raw steak cooks to its cooked grade in a furnace (200 ticks), smoker (100) or campfire (600).
Milk armor: the buckets come back empty (vanilla's milk-bucket remainder).

## The walk-in fridge (2026-11-09)
| Block | Pattern | Key | Makes |
|---|---|---|---|
| cold_room_panel | `III / LLL / III` | I iron ingot, L wool | 4 |
| cold_room_door | `II / LL / II` | I iron ingot, L wool | 1 |
| evaporator | `SNS / CRC / SSS` | S steel ingot, N iron bars, C copper ingot, R redstone | 1 |
| condensing_unit | `SNS / CRC / SIS` | S steel ingot, N iron bars, C copper ingot, R redstone, I iron ingot | 1 |

## Sheep (2026-11-10)
| Block or item | Pattern | Key | Makes |
|---|---|---|---|
| shed_controller | `WRW / WHW / WWW` | W planks, R redstone, H hay block | 1 |
| sheep_pen | `HHH / WWW` | H hay block, W planks | 6 |
| lambing_pen | `F.F / HHH / WWW` | F wooden fence, H hay block, W planks | 2 |
| pasture_gate | `FWF / FWF` | F wooden fence, W planks | 1 |
| shearing_stand | `SRS / CIC / WWW` | S steel ingot, R redstone, C copper ingot, I iron ingot, W planks | 1 |
| catching_pen | `F.F / WWW` | F wooden fence, W planks | 4 |
| wool_table | `WWW / T.T` | W planks, T stick | 1 |
| wool_press | `III / W.W / WWW` | I iron ingot, W planks | 1 |
| sheep_stall | `SNS / KKK` | S steel ingot, N iron bars, K gray concrete | 2 |
| scouring_bowl | `SBS / S.S / SCS` | S steel ingot, B bucket, C copper ingot | 1 |
| carding_machine | `NNN / CRC / SSS` | N iron bars, C copper ingot, R redstone, S steel ingot | 1 |
| spinning_frame | `TTT / CRC / SSS` | T stick, C copper ingot, R redstone, S steel ingot | 1 |
| loom_frame | `WWW / TRT / SSS` | W planks, T stick, R redstone, S steel ingot | 1 |
| felting_table | `WBW / SRS` | W planks, B bucket, S steel ingot, R redstone | 1 |
| wool_cap / wool_sweater / wool_trousers / wool_socks | the armour shapes | X wool cloth | 1 |
| udder_balm | shapeless | 2 lanolin | 1 |
| sheep_cheese | shapeless | 1 sheep cheese wheel | 8 |
| (colour)_wool, vanilla | `YY / YY` | 4 yarn of that colour | 1 |
| yarn_(colour) | shapeless | white yarn + that dye | 1 |
| cold_room_panel (felt) | `III / EEE / III` | I iron ingot, E felt | 4 |

Cooking: lamb and mutton chops cook in a furnace, smoker or campfire. Fleece, skirtings, bales, clean wool, sliver, lanolin, white yarn,
cloth, felt, sheep milk and sheep cheese wheels come from animals and machines, not recipes.

## Taming corral (2026-11-10)
| Block or item | Pattern | Key | Makes |
|---|---|---|---|
| corral_post | `T / W / W` | T stick, W planks | 1 |
| corral_gate | `TFT / TWT` | T stick, F wooden fence, W planks | 1 |
| bait_trough | `WHW / WWW` | W planks, H hay block | 1 |

The corral's water trough is the barn's `water_trough`.

## Poultry (2026-11-11)
| Block or item | Pattern | Key | Makes |
|---|---|---|---|
| coop_controller | `WDW / WTW / WWW` | W planks, D wheat seeds, T stick | 1 |
| coop_floor | `DHD / WWW` | D wheat seeds, H hay block, W planks | 6 |
| pop_hole | `TWT / TWT` | T stick, W planks | 1 |
| nest_box | `W W / WHW` | W planks, H hay block | 2 |
| perch | `TTT` | T stick | 3 |
| grain_feeder | `I I / IDI / III` | I iron ingot, D wheat seeds | 1 |
| incubator | `IGI / IRI / WWW` | I iron ingot, G glass, R redstone, W planks | 1 |
| brooder | ` N / IRI / G ` | N iron bars, I iron ingot, R redstone, G glass | 1 |
| egg_grader | `WGW / T T` | W planks, G glass, T stick | 1 |
| down_jacket | `QPQ / QQQ / QQQ` | Q down, P felt | 1 |
| white_bed, vanilla | `QQQ / WWW` | Q down, W planks | 1 |
| cake, vanilla | `MMM / SES / WWW` | as vanilla, E any Farmings egg (`#farmings:eggs`) | 1 |
| pumpkin_pie, vanilla | shapeless | pumpkin, sugar, any Farmings egg | 1 |

- **Cooking:**
  - Turkey, duck and quail cook in a furnace, smoker or campfire.
  - Any Farmings egg fries into a fried egg.
- **Not made by recipe:** eggs, meat, down, litter and egg cartons come from birds and machines.

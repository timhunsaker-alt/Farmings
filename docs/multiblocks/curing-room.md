# Curing, smoking, lard and sausage

**Status: spec, 2026-11-13 (Step 18; Tim: "Cured bacon and ham", "Lard", "Sausage").** Marks as in `docs/crops.md`. Machine times run at
the livestock scale, 15:1 (`waste.md`), so a 60-day cure takes 4 game days.

Sources used throughout:
- **VT**: Virginia Tech 458-223, [Virginia ham](https://pubs.ext.vt.edu/458/458-223/458-223.html).
- **UKY**: Univ. of Kentucky ASC-213, [How to make a country ham](https://publications.mgcafe.uky.edu/files/ASC213.pdf).
- **MSU**: Mississippi State P2648, [Curing pork products at home](https://extension.msstate.edu/sites/default/files/publications/P2648_web.pdf).
- **USDA78**: USDA, *Pork slaughtering, cutting, preserving, and cooking on the farm* (1978, rev. 1981; [Project Gutenberg](https://www.gutenberg.org/ebooks/62859)).
- **FSIS-Ham**: FSIS, [Ham and food safety](https://extension.usu.edu/fscreate/files/2020-22-staff/FFU_Ham_and_Food_Safety.pdf).
- **CFSC**: FoodSafety.gov cold food storage chart (2021), [VDH copy](https://www.vdh.virginia.gov/content/uploads/sites/6/2023/07/Cold_Food_Storage_Chart.pdf).
- **OSU**: Oregon State PNW 784, [Making cured bacon at home](https://extension.oregonstate.edu/catalog/pnw-784-making-cured-bacon-home).

## Salt
- Curing needs **salt**. A water bucket smelted in a furnace or smoker gives **1 salt** and the empty bucket *(proposed: Minecraft has no
  salt; this stands for evaporating brine)*.
- **Cure mix**: 4 salt + 1 sugar make 5 **cure** *(proposed recipe)*, after the 8-2-2 cure: "For 100 pounds of meat, use an 8-2-2 mix"
  (8 lb salt, 2 lb sugar, 2 oz nitrate; USDA78). One cure item cures 1 kg of meat: "10 pounds of mixture will cure 100 pounds of ham"
  (UKY) *(one item stands for the 0.1 kg it takes, proposed)*.

## The curing rack
A block that holds up to **4** pieces of fresh ham or pork belly, each with its cure. It **must stand in a cold room at 4 °C or below**
for the cure: "keep hams at a temperature of 36 to 40° F" (VT); "constant temperature between 36° F and 42° F" (USDA78). Above 10 °C
the cure stops and the meat ages as fresh meat (it can spoil) *(proposed: the cold room's temperature, `cold-room.md`)*.

| Product | Steps | Source |
|---|---|---|
| **Country ham** (from a fresh ham) | cure **2 days per lb** (a 9 kg ham: 40 days), equalize **14 days**, cold smoke **2 days** in the smokehouse, then age **90 days** on the rack (no cold needed) | "two days per pound of ham or another rule of thumb is 60 days" (UKY); "stored in a 50 to 60° F environment for approximately 14 days" (VT); "may take one to three days" below 90 °F (VT); "Age hams for 45 to 180 days" (VT), 90 *(proposed, inside the range)* |
| **Bacon** (from pork belly) | cure **7 days**, then hot smoke **3 hours** in the smokehouse; no aging | "Cure in refrigerator for five to seven days" (OSU); "approximately three hours" at 225-250 °F (OSU); "Bacon does not require aging" (MSU) |

- **Weight:** a country ham ends at **77.5%** of its fresh weight: "reduces ham weight by at least 18% — but typically 20 to 25%"
  (FSIS-Ham). Bacon yield is NOT FOUND (searched OSU, MSU, USDA78); **90%** *(proposed)*.
- **Items:** a whole `country_ham` (its weight carried in the item), sliced at a crafting table into 0.5 kg `ham_slice`s *(proposed)*;
  bacon as 0.5 kg `bacon` *(proposed)*.

## The smokehouse
A block with a fuel slot (logs, charcoal, coal) and 4 meat slots. Cold smoke for hams, hot smoke for bacon, by what is in it *(proposed:
the smokehouse sets its own heat)*. One log burns 2 game hours of smoking *(proposed)*.

## Lard
- A **lard kettle** over a lit campfire or fire takes pork fat and renders it: "should remain about 212° F... Do not allow it to go above
  255° F"; done "When boiling (evaporation of water) ceases" (USDA78). **3 real hours** a batch *(proposed)*.
- **Yield:** 0.75 kg of lard a kg of fat *(proposed: no authoritative figure; a homestead blog gives about a pint a pound, **TO VERIFY**)*.
  Out comes `lard`, 1 kg an item, as butter's blocks *(proposed)*.
- Lard is a cooking fat: it stands in for butter in recipes that take Farmings butter *(proposed)*.

## Sausage
- A hand-cranked **meat grinder**: use it with pork shoulder in hand and fat in the offhand (or in the grinder), and it makes fresh
  sausage at **25% fat** ("Fresh sausage should contain about 20 to 30 percent fat", USDA78) and **2% salt** ("2 pounds of salt" per 100 lb
  trimmings, USDA78): 3 kg shoulder + 1 kg fat + salt gives 16 **sausage** links of 0.25 kg *(proposed item size)*. No casing item:
  "Stuffing sausage requires skill and proper equipment" (USDA78); the links are drawn cased.
- Cooked on a furnace, smoker or campfire.

## Shelf lives (food.md)
| Item | Fridge | Room | Source |
|---|---|---|---|
| Pork chop, raw | **4 days** (3-5) | 1 day | "Chops 3 to 5 days" (CFSC) |
| Pork chop, cooked | 3.5 days (3-4) | 1 day | "Cooked leftovers" 3-4 days (CFSC) |
| Fresh ham, belly, shoulder, fat | 4 days (3-5) | 1 day | fresh ham "3–5 days" (FSIS-Ham) |
| Country ham, whole | **365 days** | **365 days** | "A whole, uncut country ham can be stored safely at room temperature for up to 1 year" (FSIS-Ham) |
| Ham slice (cut, uncooked) | 75 days (2-3 months) | 1 day *(proposed)* | FSIS-Ham chart |
| Bacon | **7 days** | 1 day | "Bacon 1 week" (CFSC) |
| Sausage, raw | **1.5 days** (1-2) | 1 day | CFSC |
| Sausage, cooked | 7 days | 1 day | "Fully cooked sausage 1 week" (CFSC) |
| Lard | 365 days *(proposed)* | 150 days *(proposed)* | NOT FOUND authoritatively; blogs give 4-6 months cool and 6-12 months refrigerated (**TO VERIFY**) |

The room column's 1 day is Tim's game rule for fresh food, as elsewhere.

## Not in this pass
- Nitrate and nitrite as separate items; country-ham mould; prosciutto and other styles; liver sausage, scrapple, chorizo.

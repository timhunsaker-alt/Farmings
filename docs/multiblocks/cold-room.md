# The walk-in fridge (cold room) (APPROVED in scope by Tim 2026-11-09)

Tim: "Refrigerator to slow spoilage, essentially a walk-in style fridge with chests, built from blocks so the player decides size with the
inside and outside components". He chose an insulated panel box with an evaporator inside and a condensing unit outside on Wildcatter power,
sized by volume. Food spoilage itself is [../food.md](../food.md).

**Real counterpart:** a walk-in cooler:
- insulated wall, floor and ceiling panels;
- an insulated door;
- an evaporator (fan coil) mounted inside;
- a condensing unit (compressor and condenser) outside.

## Parts (ordinary one-cell blocks, not big parts)
| Block | Real counterpart | Recipe |
|---|---|---|
| `cold_room_panel` | an insulated panel (wall, floor, ceiling) | iron, wool (the insulation), iron: 4 |
| `cold_room_door` | an insulated walk-in door; opens by hand (2 high) | iron and wool |
| `evaporator` | the fan coil; set in a wall, facing in; **the controller** (right-click to read) | steel, iron bars, copper, redstone |
| `condensing_unit` | compressor and condenser, outside against a wall; Wildcatter three-phase power | steel, iron bars, copper, redstone, iron |

## Forming *(proposed rules)*
- **The inside:** the evaporator is part of a wall and faces in. The inside is everything reached from the cell in front of it that is not
  a panel, door or evaporator: air, chests, barrels, anything. If it spreads past the largest room, a wall has a gap. The controller says
  where it ran out.
- **Size:** the inside must be a box, 1 x 1 x 1 to **10 x 4 x 10** cells. The outside is 3 x 3 x 3 to 12 x 6 x 12.
- **Walls:** all six walls around it are panels, a door or the evaporator. Edges and corners are not needed, because they do not touch
  the inside.
- **Door:** at least one door.
- **Condensing unit:** one touches a wall from outside.

## Power (sourced table, proposed efficiency)
- **Cooling load:** taken from a vendor's walk-in cooler load table at 35 F, typical use
  ([Atlantic Series BTU calculator](https://www.atlanticse.com/blogs/refrigeration-display/walk-in-cooler-freezer-btu-calculator)):
  6 x 6 x 8 ft 4,986 BTU/h, 8 x 8 x 8 ft 6,258, 10 x 10 x 8 ft 8,230.
  - Between those points the load is interpolated by inside volume; outside them it is extended along the nearest two.
  - Load per cubic foot falls as boxes grow, because wall area grows more slowly than volume. A heavy-use box needs about 30% more; that
    is not modelled.
- **Electric draw:** the cooling over a coefficient of performance of 2 *(proposed, no source)*. A 3 x 2 x 3 room (18 m3) needs about
  1.5 kW of cooling and draws about 0.75 kW. The largest (400 m3) needs about 29 kW and draws about 15 kW.
- **The motor:** the condensing unit's compressor is sized to its room. When the room is rebuilt to another size, the motor is replaced
  and the grid is solved again.

## How it runs
- **Checks:** the evaporator checks the room every 5 seconds.
- **Keeping cold:** once a minute, while the condensing unit has power, every food stack in every container inside (chests, barrels,
  anything that holds items) has its spoil clock held back by the time the cold saved: a minute x (1 - room life / fridge life)
  (food.md). Milk kept inside lasts its FoodKeeper 7 days.
- **Readout:**
  - formed: "FORMED, w x h x l inside (V m3), load X kW cooling, condensing unit Y kW, COLD";
  - otherwise: NO POWER, or the forming problem.
- **No power:** the room saves nothing, and food ages as at room temperature.

## Game tests
cold_room_forms, cold_room_gap_named, fridge_slows_spoilage, cold_room_no_power_warms. Unit tests: ColdRoomLayoutTest (box sizes, a gap,
too big, door and unit, chests inside, the load table).

## Not built
A temperature that rises and falls, door-opening losses, a freezer, heavy-use load, items lying loose on the floor inside (only
containers are kept cold).

## Loose ends, 2026-11-11 (spec): the room's temperature
The cold room now holds a temperature, and food ages by it.
- **Set point:** **2 °C** *(proposed)*. **TO VERIFY**: a vendor guide puts walk-ins at "34°F and 38°F (1°C – 3°C)"
  ([Operandio](https://operandio.com/walk-in-cooler-temperature/), snippet only; searched again 2026-11-14; the page did not load). A fridge should be at most 40 °F, 4 °C (USDA, via
  [Virginia Tech FST-518](https://accomack.ext.vt.edu/content/dam/pubs_ext_vt_edu/FST/fst-518/FST-518.pdf)).
  - The room starts at **20 °C**, the room temperature *(proposed)*, and that is the outside air too.
- **Heat in through the walls:**
  - U = **0.23 W/m²K** over the room's inside surface area. That is R-25 (US units), the legal minimum for walk-in cooler panels
    ([10 CFR 431.306](https://www.law.cornell.edu/cfr/text/10/431.306)), converted *(derived)*.
  - The gain is U x area x (20 - T).
- **Heat in through the door:**
  - Each time a cold-room door is opened, one room's volume of air is swapped for outside air: 1.2 kJ/m³K x volume x (20 - T)
    *(proposed: door infiltration was NOT FOUND as a figure; searched again 2026-11-14, the pages did not load)*.
- **Thermal mass:** the room's thermal mass C is set so that, with no power and the door shut, it warms from 2 °C to 5 °C in
  **4 hours**.
  - FST-518 says a home fridge "lasts ~4 hours" with the power off. That stands in for a walk-in, *derived* from that figure.
- **Cooling:** with power, the condensing unit removes up to the load table's cooling (as now) until the room is at the set point. COP
  stays 2 *(proposed)*. The law's minimum annual efficiency for these units, AWEF 5.61-7.60 Btu/Wh, is about COP 1.6-2.2 *(derived)*,
  so 2 sits inside it.
- **Time:** temperature is stepped once a minute, as the cold credit is. A real hour is a game hour, because food runs at 1 real day
  per game day.
- **Spoilage by temperature (`Spoilage.lifeAt`):**
  - Each food's life at 4 °C or colder is its fridge life, and at 20 °C its room life (both already in food.md).
  - Between them, the life falls by the same factor for every degree, a log-linear Q10 model. That means each food has its own Q10,
    from its two points: Q10 = (fridge / room)^(10/16) *(derived)*.
    - Milk comes out at 3.4. NC State's rule, "For each 18oF increase in temperature, the spoilage rate of milk doubles"
      ([NC State](https://units.cals.ncsu.edu/foodsafety/wp-content/uploads/sites/13/2019/06/dairy_milk_safety.pdf)), would give 2.
      The game keeps the FoodKeeper points.
  - Above 20 °C, room life applies.
- **The credit:** the cold credit each minute becomes a minute x (1 - room life / life at T). It is zero at 20 °C and the full fridge
  credit at 4 °C.
- **Readout:** "... T °C (set 2 °C), COLD" while at or below 4 °C, else "WARMING" (no power) or "COOLING".

### Built, 2026-11-11 (loose ends)
- **Code:** `process/coldroom/ColdRoomTemp` and `Spoilage.lifeAt` are plain Java and unit-tested. The room saves its temperature.
- **Steps:** the walls are stepped exactly, as an exponential, so a step of a minute or of hours gives the same answer.
- **A new room:** starts at 20 °C and pulls down to 2 °C within a few minutes of play.
- **Doors:** opening a door warms the room by about half a degree in a 3 x 2 x 3 room.
- **Game tests:** cold_room_warms_without_power, warm_room_spoils_faster. The older tests start from a cold room.

### Sourcing pass, 2026-11-12
- One TO VERIFY added (set point). No value changed.
- Still unsourced: COP 2 (DOE gives AWEF only, not COP), door air swap per opening (IIR and Kettering walk-in door studies give rates
  and correction factors, not a volume per opening). Forming rules and the 20 °C room are game values.

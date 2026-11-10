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

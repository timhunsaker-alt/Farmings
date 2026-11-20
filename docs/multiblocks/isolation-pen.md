# The isolation pen (quarantine and hospital pen)

**Status: APPROVED by Tim 2026-11-19 with the animal health spec (`docs/animal-health.md`).** Marks as in `docs/crops.md`.

## Real counterpart
- A **quarantine pen** for new or returning animals: "a minimum of 30 days in a separate pen with no over-the-fence contact with
  resident animals" ([Idaho State Dept. of Agriculture](https://agri.idaho.gov/wp-content/uploads/animal-industries/Biosecurity-Cattle.pdf));
  "ideally ... a separate barn" ([OMAFRA, sheep flock biosecurity](https://www.ontario.ca/page/biosecurity-sheep-flock)).
- A **hospital (sick) pen** where sick animals are kept apart, fed and watered and treated.
- Real farms keep the two apart. The game uses one block set with a **mode** on the controller (decided on recommendation: one
  recipe and one behaviour; two pens are built by building two).

## Parts
| Part | Block | What it does |
|---|---|---|
| Controller | `isolation_controller` | On the floor level, touching the pen. Right-click reads it; sneak + right-click switches QUARANTINE / HOSPITAL (only while empty). |
| Pen floor | `isolation_pen` | A bedded floor cell (2/16 high, walked on). The pen is every pen cell joined to the controller. |
| Feeder | the barn's `hay_feeder` (cattle, sheep, goats, horses, pigs) or the coop's `grain_feeder` (birds) | Beside a pen cell. |
| Water | the barn's `water_trough` | Beside a pen cell. |

Any species. One species at a time *(proposed: mixing a sick cow with a quarantined ewe makes no sense)*.

## Layout (example, 3 x 4 pen, north at the top)
Layer y 0 (floor):
```
 x ->  0   1   2   3   4
 z 0   H   P   P   P   .      H hay_feeder (or grain_feeder) beside a pen cell
   1   .   P   P   P   W      P isolation_pen
   2   C   P   P   P   W      W water_trough (2 x 1)
   3   .   P   P   P   .      C isolation_controller
```
Layer y 1: nothing required. Walls and fences are the player's; the pen does not check that it is closed in (as the sheep shed).

**Controller position:** any cell touching a pen cell at floor height. **Rotations:** any; the check floods from the controller, so
the pen may be any shape.

## Forming (`husbandry/health/IsolationLayout`, reusing `ShedLayout`'s flood)
It forms when:
1. a pen cell touches the controller;
2. the pen is **2 to 64 cells** *(proposed)*;
3. at least one feeder and one trough touch a pen cell;
4. **no cell of any other Farmings housing** (barn, shed, coop, pig barn, stable, corral, another isolation pen) is within **8 blocks**
   of any pen cell *(proposed: the game's "no over-the-fence contact"; NC State asks for distance beyond an open fenceline)*.

Faults name the first thing missing: `NO FEEDER`, `NO WATER`, `TOO CLOSE TO SHEEP SHED AT x y z (8 blocks needed)`.

**Capacity:** the species' own space: 3 cells a cow or a horse (a stall's floor), 1.5 m² a sheep or goat (the shed's 3 cells for 2),
the pig barn's rule for pigs, the coop's for birds.

## How it runs
- **QUARANTINE:** a tamed animal leaving a corral goes to the nearest quarantine pen with room within 64 blocks *(proposed)* before any
  other housing takes it, and stays **30 real days** (2 game days at 15:1; 10 at 3:1 for birds). Animals that enter together form a
  batch; a new arrival restarts the batch's clock (all-in, all-out) *(proposed)*. On release they go to housing as now. A case found in
  quarantine restarts the clock when it recovers. Whether quarantine is required is question 8 (`docs/questions/animal-health.md`).
- **HOSPITAL:** the vet kit's isolate action sends a sick animal here; it goes home when the vet kit finds it recovered (or by hand).
  A cow in milk in the hospital pen is still called to the parlor and milked last, into the waste can if she is in withdrawal
  *(proposed)*.
- **Spread:** the pen is its own group (`docs/animal-health.md`, "How disease spreads"): nothing passes between it and the herd.
- **Feeding, water, manure, bedding** as the sheep shed (`husbandry/shed/ShedBedding`); dirty bedding counts as a dirty stall.

## When a part is removed
- **Feeder or trough:** the pen stops feeding (fault named); animals stay, with the underfeeding rule.
- **Pen cells:** capacity drops; animals over capacity are released to the nearest free pen cell or stand beside it.
- **Controller:** the pen stops as a unit; animals stay in place, their quarantine clocks keep running on the animal (saved on the
  animal, not the controller).
- Feeder contents drop when broken (Tailings R1 rule).

## Game tests
`isolation_pen_forms`, `isolation_pen_too_close` (a shed 5 blocks away), `quarantine_releases_after_30_days`,
`kit_sends_sick_to_hospital_pen`, `contagion_stays_in_group`.

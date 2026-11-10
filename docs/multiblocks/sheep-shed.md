# Sheep shed and pasture

**Status: spec and build, 2026-11-10 (Tim: "Sheep shed + pasture").** Figures are from `docs/animals/sheep.md` unless marked
*(proposed)*.

## Parts
| Part | Block | What it does |
|---|---|---|
| Shed controller | `shed_controller` | Sits on the floor level, touching the pen. Right-click reads the shed. Runs the flock: homes, feeding, water, pasture. |
| Sheep pen | `sheep_pen` | A straw-bedded floor cell (2/16 high, walked on). The pen is every pen cell joined to the controller. |
| Lambing pen | `lambing_pen` | A floor cell with hurdles drawn on it: a jug for one ewe and her lambs. Part of the pen. |
| Pasture gate | `pasture_gate` | A floor cell set in the pasture fence, touching the pen. The flock goes out and in through it. |
| Hay rack | the barn's `hay_feeder` | Beside a pen cell. Holds hay blocks; filled by hand. |
| Water trough | the barn's `water_trough` | Beside a pen cell. Filled by bucket or pipe, as in the barn. |

The hay rack and trough are the barn's own parts, reused: one part, one recipe, one behaviour.

## Forming (`husbandry/shed/ShedLayout`)
From the controller, every `sheep_pen`, `lambing_pen` and `pasture_gate` cell at the controller's height joined side to side is
the pen. It forms when:
1. a pen cell touches the controller;
2. the pen is at most **256 cells** *(proposed)*;
3. it has at least **3 sheep-pen cells** (room for 2 sheep);
4. at least one hay rack and one water trough touch a pen cell.

Fault messages name the first thing missing. Walls and fences are the player's: the shed does not check that the pen is closed in.

**Capacity:** 1.5 m² per pregnant or lactating ewe (OMAFRA), so **3 sheep-pen cells hold 2 sheep** (`Flock.penCapacity`). A lambing
pen holds one ewe and her lambs. Lambs under weaning age (120 days, proposed) do not count. The controller takes in sheep within
**48 blocks** *(proposed)* that have no shed and are not wild (docs/taming.md), until the shed is full.

**Lambing pens:** about 10% of a 100-ewe flock (Penn State G5). The readout warns when there are fewer. A ewe due within **3 real
days** *(proposed)* is sent to a free lambing pen and stays there for **14 real days** after lambing *(proposed)*. If no jug is free,
she lambs in the pen.

## Pasture (`husbandry/shed/PastureLayout`)
- **Finding it:** from the gate, the pasture is the ground reached by walking away from the pen. That ground is grass, dirt or open
  walkable cells, stepping up or down at most one block, and it ends at fences, walls, water or anything else a sheep cannot walk on.
- **Limits:** the pasture holds at most **4,096 cells** *(proposed)*. A bigger flood means the pasture is not fenced in, and the
  readout says so.
- **Grass:** the grass blocks in it are what the flock can graze.

## The day
- **Morning to dusk:** if the shed has a pasture and the weather is clear, the flock goes out (game time 0-12,000 of each day;
  *proposed*: any rain brings them in).
  - Each sheep is sent to a grass cell and picks another every 30 seconds, so the flock spreads and grazes.
  - Ewes in lambing pens and lambs younger than 14 real days stay in.
- **At dusk, in rain, or when the pasture is lost:** the flock comes home to its pen cells.

## Feeding and water
Each sheep's need per game day is one real day's intake (the cattle rule): `Flock.intakeKgDm`, and water at 2.6 L per kg of dry
matter.
- **On pasture:** a sheep grazes the grass block it stands on, or the nearest one. The block turns to dirt and gives **1.5 kg of dry
  matter** *(proposed, sheep.md)*. Vanilla grass spreads back over the dirt in time.
- **In the shed:** it eats from the hay racks: a hay block is 85 kg of dry matter, as for cattle.
- **Water:** from the troughs.
- **Shortfalls:** how much of the need was met sets the sheep's fed and watered shares. Underfed sheep grow slower, grow less wool
  and give less milk, as the cattle rules have it. No sheep dies of a missed day.

## Not in this pass
- Bedding getting dirty (the barn's manure rule).
- Sheep dogs.
- Moving between several pastures.
- Seasons.

# Wool mill

**Status: built 2026-11-10 (Tim: "Shearing shed + mill").** Values without a source are marked *(proposed)*.

The mill is a line of machines, each on Wildcatter industrial power. Wool goes through them by hand or by hopper: each machine takes
its input from the top or sides and gives its output from below, so hoppers chain the line. There is no controller and no layout
check. A real mill is a row of separate machines too, and here each machine reads its own state (right-click with an empty hand
takes its output, or reads it when there is none).

## Machines
| Machine | Block | Takes | Gives | Motor *(proposed)* |
|---|---|---|---|---|
| Scouring bowl | `scouring_bowl` | fleece, skirtings, wool bale, loose wool; and water | clean wool, lanolin | 3 kW (mostly the water heater) |
| Carding machine | `carding_machine` | clean wool | sliver | 1.5 kW |
| Spinning frame | `spinning_frame` | sliver | yarn in the wool's natural colour | 1 kW |
| Power loom | `loom_frame` | 4 yarn of one colour | wool cloth | 0.75 kW |
| Felting table | `felting_table` | clean wool | felt | 0.5 kW |

A batch takes 5 seconds *(proposed)*. No motor ratings were found, so all five are proposals.

## The numbers (`process/wool/WoolProcess`)
- **Water:** scouring uses **15 L per kg of greasy wool**, the middle of 10-20 L
  ([US patent 4168143](https://patents.google.com/patent/US4168143A/oth)). The bowl holds 1,000 L, filled by bucket or Wildcatter
  pipe.
- **Clean wool:** greasy weight × the fleece's clean yield (sheep.md). Loose wool from a hair sheep is taken at 50% *(proposed)*.
- **Lanolin:**
  - Wool grease is about **15%** of greasy wool. One Australian scour reports 150 kg of lanolin per tonne of greasy wool
    ([P2 InfoHouse](https://p2infohouse.org/ref/10/09338.htm)); other sources give 6-20%.
  - The bowl's centrifuge recovers **30%** of the grease *(proposed; no figure for water scouring was found)*.
  - One lanolin item is 0.1 kg *(proposed)*.
- **Carding:** loses **5%** as short fibre *(proposed)*.
- **Spinning:**
  - One yarn item is a **100 g** skein *(proposed)*.
  - Finer wool spins a finer, longer yarn from the same weight, so it gives more skeins *(proposed shares)*: fine +25%, half blood
    +15%, three-eighths +10%, quarter +5%, coarse and hair +0.
  - Part skeins carry over to the next batch.
- **Weaving:** 4 yarn make 1 cloth *(proposed)*.
- **Felting:** 0.25 kg of clean wool makes 1 felt *(proposed)*.

## Colour
Yarn keeps the fleece's natural colour from the coat genes. White wool spins white yarn, black spins black, brown spins brown, and
grey (a spotted coloured sheep, or a black one past two years) spins light grey. Dyeing is a crafting recipe: white yarn plus any
vanilla dye gives yarn of that colour.

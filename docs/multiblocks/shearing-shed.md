# Shearing shed

**Status: built 2026-11-10 (Tim: "Shearing shed + mill").** Values without a source are marked *(proposed)*.

## Parts
| Part | Block | What it does |
|---|---|---|
| Shearing stand | `shearing_stand` | The shearing machine (an overhead motor and handpiece) and the controller. Runs on Wildcatter low-voltage power, **0.5 kW** *(proposed)*. **TO VERIFY** against a maker's sheet: a retailer lists the Lister Nexus overhead motor at "Motor power 250 W (1/3 HP)" ([Fane Valley Stores](https://www.fanevalleystores.com/product/127006/lister-nexus-motor-flexi-drive)); search snippets give 300 W for the Heiniger ONE and 700 W for the Lister Nexus Elite. Right-click reads it. |
| Catching pen | `catching_pen` | Slatted floor beside the stand that opens onto a sheep shed's pen. Sheep wait here to be caught. |
| Wool table | `wool_table` | Beside the stand. Fleeces are skirted on it and kept (9 stacks). A hopper below takes them. |
| Wool press | `wool_press` | Beside the table (optional). Bales every **8 fleeces** of one grade and colour *(proposed)*. A hopper below takes the bales. |

## Forming (`husbandry/shearing/ShearingLayout`)
1. A catching pen cell touches the stand. The catching pen is every catching-pen cell joined to it, at most **16** *(proposed)*.
2. The catching pen touches a sheep shed's pen cell (sheep pen or lambing pen). The stand serves the shed whose controller that pen
   reaches.
3. A wool table touches the stand.

The parlor links to its barn through a cow lane. A shearing shed is built onto the sheep shed's pen instead, as on real farms where
the catching pen is filled from the yards behind it. The fault messages name what is missing.

## Shearing
- **Choosing a sheep:** while powered, the stand calls in one sheep at a time from its shed. She must be one whose fleece is ready
  (300 days or more of growth, `Flock.FLEECE_READY_DAYS`), not nursing *(proposed)*, and not within **28 real days** of lambing
  (was: not in lamb at all). "4-6 weeks before lambing is an ideal time to shear sheep" and "Shearing sheep later in pregnancy can
  be more stressful on ewes, so don't delay too long" ([Perennia, Preparing for lambing](https://www.perennia.ca/wp-content/uploads/2018/03/Preparing-for-lambing-Factsheet-UPDATED.pdf)).
- **Walking in:** she walks into the catching pen. Within two blocks of the stand the shearer catches her and pulls her onto the
  board, as shearers do.
- **Time:** shearing takes **5 seconds** *(proposed game size; a professional takes 2-3 minutes:
  [Wikipedia, sheep shearer](https://en.wikipedia.org/wiki/Sheep_shearer))*.
- **What it gives:**
  - The fleece is split at the table into the **body fleece** and **skirtings**, the belly and edge wool, which make up **12%** of the
    weight *(proposed; no figure found)* and are one grade lower.
  - Both keep the fleece's micron, staple, clean yield and colour.
  - Grade comes from the micron (sheep.md, "Wool"): a Merino fleece is fine, a Suffolk quarter blood, an East Friesian coarse.
- **Afterwards:** she goes home, and the shed takes her back.
- **No power:** the stand reads NO POWER, and the called sheep is let go.

## Hand shears
Shears still work on any sheep with wool. They give one fleece, one grade lower than the stand *(proposed)*, and no skirtings.

## Bales
Eight body fleeces of one grade and colour on the table are pressed into one **wool bale**. The bale holds their combined greasy
weight, and its micron, staple and yield are averaged by weight. The wool mill takes fleeces, skirtings, bales and loose wool.

### Sourcing pass, 2026-11-12
- Shearing in pregnancy is now sourced (Perennia): blocked only in the last 28 real days. The motor power is a **TO VERIFY**: retail
  listings give 250-700 W, and no maker's sheet was fetched.
- Still proposed: the skirtings' 12% share (searched AWI, Woolwise and Texas AgriLife skirting reports; no share found) and the
  game sizes. A real AWEX bale is 120-204 kg (search snippet only), about 25-40 Merino fleeces, against the game's 8.

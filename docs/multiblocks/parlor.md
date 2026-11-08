# The milking parlor and bulk tank (DRAFT, not approved)

**Status: draft 2026-11-08. Nothing here is built.** *(proposed)* = a proposal. *(unverified)* = no good source yet.
Milk is food and an ingredient; it is never sold (VISION).

## Real counterparts
- A **herringbone or parallel milking parlor**: cows stand in a row of stalls on a raised platform, the milker works from a pit.
- A **milking unit** (cluster, claw, pulsator) on a **vacuum pump**; a **milk line** to a **receiver jar and milk pump**.
- A **plate cooler** and a **bulk tank** (refrigerated, stirred) holding milk at about 4 C.

## Proposed parts *(proposed)*
| Part | Cells | Job |
|---|---|---|
| `parlor_stall` | 1 x 1 x 2 | A cow stands here to be milked. One per cow at a time |
| `milking_unit` | 1 x 1 x 1 | Takes the cow's milk. Needs vacuum |
| `vacuum_pump` | 1 x 1 x 1 | Motor on Wildcatter power, size *(unverified)* |
| `milk_line` | 1 x 1 x 1 | Carries milk to the tank |
| `bulk_tank` | 2 x 2 x 2 | Cooled and stirred; holds milk. Needs a chiller on Wildcatter power |
| `parlor_controller` | 1 x 1 x 1 | Multiblock controller; names missing parts as the barn's does |

## How it would run *(proposed)*
- Once per game day each lactating cow in the barn is led to a free parlor stall, milked, and returned. Output is one real day's milk (decided).
- Milk goes by milk line to the bulk tank. If the tank is full or warm, milking stops (a fault, shown on the controller); the cow is not harmed.
- No vacuum or no power: no milking, cows stay housed. Underfed or underwatered cows give less (rule in cattle.md).

## DECISIONS for Tim
1. **Which lactation source?** The Wisconsin Wood's parameters above give Holstein about 13,200 kg per lactation, above the national average. Options: (a) use them as is, (b) scale to the USDA average, (c) find a better source first. No Brown Swiss or Gyr numbers exist yet.
2. Whether to build milk as a plain-Java curve (breed + days in milk + genome milk-yield trait) before any parlor blocks.
3. Vacuum pump, chiller and tank sizes: all *(unverified)*; need sources before any wattage is set.

# The stable

**Status: spec, 2026-11-13 (Step 18; Tim: a stable multiblock).** Marks as in `docs/crops.md`.

## Parts
| Part | Block | What it does |
|---|---|---|
| Controller | `stable_controller` | Checks the layout, houses tame horses within 48 blocks (as the sheep shed), reads out. |
| Stall | `horse_stall` floor | One horse a stall. A stall is a **3 x 4 block** run of stall floor (12 m²) inside a fence or wall: "A 12-foot x 12-foot stall is the standard recommendation for a 1,000-pound horse" (13.4 m²; **TO VERIFY**, snippet: [Penn State, horse stall design](https://extension.psu.edu/horse-stall-design)); 12 m² is the nearest whole-block size under it *(proposed)*. |
| Hay rack | the barn's hay feeder | Feeds the horses of the stalls it touches: hay, haylage, grain, rations (`feed-mill.md`). |
| Water trough | the barn's trough | Water. |
| Paddock gate | `paddock_gate` | Optional: a fenced paddock behind it, where horses go out by day and graze, as the sheep pasture (`sheep-shed.md`). |

## The day
- **By night** each horse goes to its stall; **by day** out to the paddock if there is one, else it stays in.
- **Feed and water:** 2.25% of body weight in dry matter and 0.05 L of water per kg a day (`horses.md`), from the rack and trough, or
  grazed in the paddock (1.5 kg DM a grass block, as sheep).
- **Bedding and manure:** each stall keeps a straw store, as the sheep shed's (`husbandry/shed/ShedBedding`): manure at 5.1% of body
  weight a day fresh (Penn State, `horses.md`); straw "traditionally the most widely used bedding source" (**TO VERIFY**, snippet:
  [Rutgers](https://esc.rutgers.edu/fact_sheet/horse-manure-management-bedding-use/)). A hopper under the controller takes manure items.
  Dirty bedding costs health as in the barn and shed.
- **Fitness:** a horse not worked keeps its fitness 42 real days, then loses it (horses.md). The readout lists each horse's fitness.

## Not in this pass
- Tack rooms, farriers, turnout rugs.

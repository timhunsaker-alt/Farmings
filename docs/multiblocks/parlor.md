# The milking parlor and bulk tank (APPROVED by Tim 2026-11-08, built)

**Status: approved 2026-11-08 (sizes and whole spec), built the same day.** *(proposed)* = a proposal. *(unverified)* = no good source yet.
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

## Size research, 2026-11-08 (secondary sources, all *(unverified)*)
- **Vacuum pump:** [UW Extension](https://fyi.extension.wisc.edu/energy/files/2016/03/sizing-vacuum-pumps-Reinemann_Mein.pdf) suggests 3 cfm per milking unit plus a 35 cfm base for up to 32 units. A 6-unit parlor is about 53 cfm. A slide deck gives about 10 cfm per hp, so about 5 hp (our arithmetic, not a source). Pumps are sized by flow and vacuum, so a supplier figure is better.
- **Bulk tank compressor:** no hp-per-volume figure found. A manufacturer spec for a 2,000 L direct-expansion tank cools 35 C to 4 C in 3.0 h for a first milking and 10 C to 4 C in 1.5 h for a second. Cool to 0-4 C, within 2 h at most ([Lactanet](https://lactanet.ca/?p=26631)).
- So the tank's chiller wattage stays open; ask a supplier datasheet or pick a proposal for Tim.

## More size research, 2026-11-08 (secondary, all *(unverified)*)
- **Parlor stall spacing:** manufacturers use 80 to 115 cm between cows, centre to centre: [DeLaval HB30](https://delaval.com/globalassets/inriverresources/pdfs/h/hb30-hb50-new-4page-can.pdf) 45 in (115 cm) and HB50 31.5 in (80 cm), [BouMatic HD2](https://boumatic.com/products/hd2/) 115 cm, [GEA EuroClass 1200](https://www.gea.com/en/products/milking-farming-barn/dairyparlor-milking-parlor/herringbone-parlors/herringbone-parlor-euroclass-1200re.jsp) 120 cm. At 1 cell = 1 m that is **1 cell per cow** *(proposed, same rounding as the barn's stalls)*.
- **Vacuum pump motors:** a milking-equipment dealer lists pump sets at [3 kW and 4 kW](https://res.cloudinary.com/milking-equipment-solutions/image/upload/v1692797180/Milking%20Equipment%20Solutions%20Images/Exploded%20diagrams/Technical_Information_-_Vaccum_Pump_Sets_n74i0p.pdf) (flow columns not readable). That agrees with our own arithmetic of about 5 hp (3.7 kW) for 6 units, so a **4 kW three-phase pump motor** is a reasonable *(proposed)* figure.
- **Bulk tank refrigeration:** no hp-per-size table found. One data point: a small [Paul Mueller open tank](https://genemco.com/cdn/shop/files/SPPP260paulmuellerprocessor.pdf) needs a condensing unit of about 1.97 hp (66,000 BTU/h). The [R-1000](https://www.paulmueller.com/milk-cooling-and-storage/milk-cooling-tanks/mueller-model-r-open-tank) uses a three-phase 400 V reciprocating compressor. A real figure needs a manufacturer datasheet for a stated tank size; until then pick one *(proposed)*, e.g. a 2.2 kW chiller for the 2 x 2 x 2 tank.

## Layout *(proposed; the approved spec listed parts but no geometry)*
Drawn with the controller at the origin and the tank to the east; it may lie any of the barn's eight ways (the controller finds the one that forms).
- Floor: the vacuum pump at the far end, then a milk line one cell per stall running to the controller.
- Level above: a milking unit over the line at each stall; the controller at the tank end.
- Under each unit a parlor stall (1 wide, 2 long, a low platform) on the south side: one cow each. 2 to 12 stalls.
- The bulk tank (2 x 2 x 2) east of the controller.

## Built (2026-11-08)
Plain Java `husbandry/parlor` (layout check, `BulkTank`, tests); blocks and entities in `dev.farmings.parlor`. Once a second the controller
calls each lactating cow with milk (within 24 blocks *(proposed)*) to a free stall, she walks there (`CattleEntity.MilkingGoal`), is milked
into the tank (1.03 kg per litre, 35 C), and goes home. Pump 4 kW, chiller 2.2 kW, tank 1,000 L (all Tim's, 2026-11-08). The tank cools
35 C to 4 C in 14,400 ticks (the manufacturer's 3 h, scaled 15:1); above 10 C the tank is WARM *(proposed)*; warm only blocks a **new milking session** (Tim chose this 2026-11-09 after the first client screenshots showed one cow's 35 C milk stopping the rest of the herd): milk keeps going in while the last of it went in under 6,000 ticks ago *(proposed)*. Faults: NO POWER,
TANK FULL, TANK WARM. An empty bucket on the tank takes 10 L as a milk bucket *(proposed)*. Not built: spoilage, processing, models.

## DECISIONS for Tim
1. ~~Which lactation source?~~ Decided 2026-11-08: scale to the USDA average; built in `husbandry/Lactation`. (Was: The Wisconsin Wood's parameters above give Holstein about 13,200 kg per lactation, above the national average. Options: (a) use them as is, (b) scale to the USDA average, (c) find a better source first. No Brown Swiss or Gyr shapes exist yet.)
2. ~~Whether to build milk as a plain-Java curve~~ Decided yes, built ( (breed + days in milk + genome milk-yield trait) before any parlor blocks).
3. Sizes: 1 cell per cow in the parlor; vacuum pump 4 kW; bulk-tank chiller 2.2 kW; tank capacity (a 2 x 2 x 2 tank holding 1,000 L is my suggestion, not a source). Approve, change, or ask me to look for datasheets first.
4. Approve the whole parlor spec (parts, how it runs) so building can start.

## Built later (2026-11-09)
The bulk tank tracks the milk's fat and protein by volume. Each cow's milk goes in with her own fat (her breed's, moved by her butterfat
score) for the creamery. A run of milk_line from the bulk tank to a creamery's receiving tank pipes the milk across (creamery.md).

## The cow lane and holding pen (Tim, 2026-11-09: "can the barn and parlor be connected"; he chose a lane plus a holding pen)
**Real counterpart:** cows walk from the barn along a return/travel lane to a **holding area** (the collecting yard) beside the parlor,
wait there, enter the parlor stalls, and walk back along the lane after milking.

**Parts** (two new parlor parts, one cell each, walked on, collision 2/16 high):
| Part | Real counterpart |
|---|---|
| `cow_lane` | a concrete travel lane with side rails |
| `holding_pen` | the holding area floor |

**How they link** (`husbandry/lane/LanePath`, plain Java, tested on drawn grids):
- The **pen** is every holding-pen cell joined (side by side) to one that touches a parlor stall. It must give each parlor stall
  **15 sq ft (1.39 m2)**, the low end of the Dairyland Initiative's 15-20 sq ft per cow
  ([Hoard's Dairyman](https://hoards.com/article-36289-Fit-your-facilities-to-your-herd.html)), so one parlor turn can wait in it.
  2 stalls need 3 cells, 3 need 5, 12 need 17. The pen holds floor(cells / 1.39) cows.
- The **lane** is the shortest run of `cow_lane` cells from the pen to a cell beside a barn's scrape alley, up to **64 cells**
  *(proposed)*. It may step up or down one block between cells. It leaves the alley from one of the open stretches at the alley's ends
  (barn.md), the only alley edge not taken by stalls.
- The **barn** is the barn whose controller stands at the end of that alley.

**How it runs:**
- The parlor milks **only the cows housed in its linked barn** (the 24-block calling radius is gone).
- Once a second, while it can milk, it calls lactating cows with milk along the lane to the pen, as many as the pen holds. Each cow
  walks the lane waypoint by waypoint (alley exit, every lane cell, the pen cell), starting from the waypoint nearest her.
- A free stall takes the nearest cow that has reached the pen; the others wait in the pen.
- After milking she walks the lane back to the alley, then to her own stall. Her stall stays hers while she is away.
- No pen, lane or barn: the fault is `NO LANE TO BARN: <what is missing>` and no cow is called. No power or a full or warm tank: the
  cows waiting in the pen go home.

**Recipes:** cow lane: iron bars, gray concrete (6); holding pen: iron bars, gray concrete (6). See recipes.md.

**Game tests:** lane_links_barn_and_parlor, no_lane_no_milking, cows_wait_in_pen, cow_walks_lane_and_return (a real barn, lane, pen and
parlor; the cow walks it with her own AI, is milked and returns to her stall). The older parlor tests build a pen, a short lane and a
stub barn alley.

**Not built:** collision rails on the lane (cows follow it by waypoints, so the rails are drawn but low), crowd gates, a return lane
separate from the entry lane.

# What wool is for

**Status: built 2026-11-10 (Tim chose all four: vanilla blocks, warm clothing, farm use, dyed yarn).**

## Vanilla wool, carpets and beds
- **Wool blocks:** 4 yarn of one colour (2 × 2) make 1 vanilla wool block of that colour, in all 16 colours. Carpets and beds then
  come from vanilla's own recipes.
- **Grade:** a better grade gives more blocks per fleece through the spinning bonus (`WoolProcess.spinBonus`). A fine Merino fleece
  makes about a quarter more yarn than a coarse one of the same clean weight *(proposed)*.

## Warm clothing
- **The set:** a knitted **cap, sweater, trousers and socks**, made from the mill's wool cloth in the armour shapes.
- **Protection:** leather's, read from vanilla's leather material *(proposed: close to leather)*. The pieces are repaired with wool
  cloth.
- **Cold:**
  - Every piece is in vanilla's freeze-immune wearables tag, so **any one piece keeps powder snow's freezing off**, as leather does.
  - Walking on powder snow stays leather boots' alone.
  - If cold-biome damage is ever added, the full set is the place to answer it.
- **Look:** the worn layers are a cream knit with ribbed cuffs, a cable down the sweater's front, and an open face in the cap, drawn
  by `tools/wool_textures.py`.

## Farm use
- **Felt insulation:** 3 iron, 3 felt and 3 iron make 4 insulated cold-room panels, the same as with vanilla wool. Felt is the
  mill's own insulation.
- **Udder balm:** 2 lanolin make 1 udder balm. Rubbed on a hurt cow or sheep it heals 4 health *(proposed)*. Lanolin is the base of
  real udder and teat salves; the amount it heals is a game value.
- **Not built yet:** cloth filters for the cheese press and the mill (the plan's other farm use).

## Dyed yarn
White yarn plus a vanilla dye gives dyed yarn. Natural-coloured yarn (black, brown, light grey) comes from coloured sheep.

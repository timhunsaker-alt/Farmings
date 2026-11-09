# Ludo prompts (Farmings)

The prompts behind the Ludo pictures in this folder, so a picture can be remade or matched. `tools/process_art.py` turns them into the
mod's textures; `tools/models/parts.py` draws the part models with them and with Wildcatter's materials (stainless, galvanized, concrete,
paint, rubber, wood, decals), referenced from Wildcatter so both mods look alike. Made 2026-11-09, Ludo `createImage`, model klimt.

## Materials (image_type `texture`, art_style `Photorealistic 3D`, 2 variations, the better kept)
All prompts start "Seamless flat texture, ... orthographic view, even lighting:".

| File | Prompt (after the common start) |
|---|---|
| mat_grooved_concrete | grey poured concrete dairy barn alley floor with parallel shallow anti-slip grooves cut across it every 10 cm, slightly worn and damp, no text, no logos (top-down) |
| mat_rubber_mat | black rubber cow stall mattress surface with a fine pebbled anti-slip pattern, matte, a little straw dust, no text, no logos (top-down) |
| mat_hay | loose dry grass hay, golden yellow-green stems and leaves tangled together, cattle feed, no text |
| mat_manure | wet dairy cow manure slurry mixed with bits of straw, dark olive brown, glossy in places, no text (top-down) |
| mat_milk | surface of fresh whole cow milk in a tank, creamy off-white, very faint ripples, no text (top-down) |
| mat_cheese_rind | the waxed cloth-bound rind of an aged cheddar cheese wheel, deep orange-tan, fine cloth weave texture, no text |
| mat_paint_yellow | steel plate painted safety yellow farm machinery paint, slightly scuffed, plain, no seams, no text |
| mat_water | clear still drinking water in a galvanized livestock trough, blue-grey with soft light ripples, no text (top-down) |
| mat_cow_hair | close-up of short smooth light grey cattle hide hair, fine hairs lying in one direction with subtle sheen, neutral grey, no spots, no text. Used as a light-and-shade grain over the approved coat colours (tools/cattle_textures.py) |

## Item icons (image_type `item-icon`, art_style `Photorealistic 3D`, 1 variation)
Every prompt ends "game item icon, plain white background, no text" (the armor: "plain grey background").

| File | Prompt |
|---|---|
| manure | A single small heap of cow manure with a few bits of straw, dark brown |
| manure_fork | A single barn manure fork (pitchfork) with a long wooden handle and a D-grip and a wide steel head with many curved tines, lying diagonally |
| empty_straw | A single empty clear plastic 0.5 ml artificial insemination semen straw, a thin transparent tube with a white cotton plug at one end, lying diagonally |
| semen_straw | A single filled 0.5 ml bull semen straw, a thin blue-tinted plastic tube filled with pale liquid, white cotton plug, sealed blue end, frosted from liquid nitrogen, lying diagonally |
| select_steak / choice_steak / prime_steak | A single raw beef strip steak, lean, very little marbling (Select); a raw ribeye, moderate fine marbling (Choice); a raw Wagyu steak densely laced with abundant marbling (Prime) |
| cooked_* | The same three grilled: lean strip with grill marks; ribeye with crisscross grill marks and rendered fat; seared Wagyu, sliced to show a pink centre |
| herd_book | A single closed green clothbound farm herd record book with a leather spine and a small embossed cow silhouette on the cover |
| milk_helmet / _chestplate / _leggings / _boots | A single knight helmet (chestplate, leggings, boots) made entirely of glossy white milk, liquid milk surface with drips, blocky medieval shape. The spoiled looks are made from these in process_art.py (soured colour, curdled spots), not drawn again |
| bottled_milk / cream_bottle / skim_milk_bottle / buttermilk_bottle | A single old-fashioned 1 litre clear glass bottle full of white milk (cardboard cap) / heavy cream (gold foil cap) / skim milk (light blue cap) / buttermilk with butter flecks (green cap), upright, no label |
| butter | A single rectangular half-kilo block of fresh pale yellow farm butter on folded white parchment paper |
| cheddar_wheel | A single whole aged cheddar cheese wheel with an orange-tan cloth-bound waxed rind, three-quarter view |
| cheddar | A single wedge slice of aged orange cheddar cheese with a thin rind edge |

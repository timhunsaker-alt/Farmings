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

## Worn milk armor (2026-11-09, Ludo createImage, texture, Photorealistic 3D)
- `mat_milk_armor.webp`: "Seamless tileable texture of fresh white milk surface poured over a curved form: glossy creamy white with soft sheen highlights, gentle ripples and a few thick drips, faint blue-white tint, no objects, no text, even lighting, top-down"
- `mat_milk_spoiled.webp`: "Seamless tileable texture of spoiled curdled sour milk: lumpy cottage-cheese curds, yellowish off-white with sickly green-yellow tinge, separated watery whey in places, a few mould spots, matte, no objects, no text, even lighting, top-down"

## 2026-11-09: whey, spoiled food, the walk-in fridge, the cow lane (Ludo createImage)
Item icons (`item-icon`, Photorealistic 3D):
| file | prompt |
|---|---|
| whey_bottle | A single old-fashioned 1 litre clear glass bottle full of thin translucent yellowish-green whey liquid, white cardboard cap, upright, no label, plain background |
| spoiled_bottle | A single old-fashioned 1 litre clear glass bottle of spoiled sour milk: lumpy curdled yellowish chunks separated from greenish watery liquid, a bulging grey cap, a few mould spots, upright, no label, plain background |
| rancid_butter | A half-kilo block of rancid butter on torn wax paper: dark yellow, translucent oily edges, a darker oxidised crust, a few small mould spots, plain background |
| moldy_cheese | A wedge of mouldy cheddar cheese: orange-yellow cheese covered in fuzzy blue-green and white mould patches, cracked dry rind, plain background |
| spoiled_steak | A single spoiled raw beef steak: dull grey-brown and greenish discoloured meat, slimy sheen, dark edges, a couple of mould spots, plain background |

Surfaces (`texture`, Photorealistic 3D):
| file | prompt |
|---|---|
| mat_insulated_panel | Seamless tileable texture of a white walk-in cooler insulated wall panel: smooth glossy white food-grade FRP surface with faint vertical stucco embossing, a thin tongue-and-groove seam, clean, even lighting, flat front view, no text, no logos |
| face_evaporator | Front face of a commercial walk-in cooler evaporator unit: brushed aluminium casing with a round black fan guard grille in the centre and fine horizontal aluminium cooling fins around it, flat front view, even lighting, no text, no logos |
| face_condensing_unit | Side of an outdoor refrigeration condensing unit: grey painted steel casing with a large round black wire fan guard over a fan, copper condenser coil fins visible behind, flat front view, even lighting, no text, no logos |
| mat_cow_lane | Seamless tileable texture of a brushed concrete livestock walkway with shallow parallel anti-slip grooves and a little straw and dust, top-down, even lighting, no text |

## 2026-11-10: sheep (Ludo createImage, `item-icon`, Photorealistic 3D, 1 variation)
Every prompt ends "game item icon, plain white background, no text" (clothing: "plain grey background").
| file | prompt |
|---|---|
| fleece | A single freshly shorn whole sheep fleece rolled up into a neat bundle, creamy white greasy crimped wool locks |
| skirtings | A small loose pile of sheep wool skirtings: short dirty belly wool and edge pieces, greyish off-white with bits of straw |
| wool_bale | A single pressed rectangular wool bale wrapped in white woven nylon pack with steel straps, cream wool showing at the top |
| loose_wool | A small tuft of shed sheep hair and wool, coarse white and tan fibres loosely tangled |
| clean_wool | A single fluffy armful of clean scoured wool, bright white soft fibres, washed and dried |
| sliver | A single coil of carded wool sliver, a soft continuous rope of white combed wool fibres wound in a spiral |
| lanolin | A single small round tin of pure lanolin wool grease, open lid, thick pale yellow waxy cream inside |
| wool_cloth | A single folded bolt of woven natural cream wool cloth, visible twill weave texture |
| felt | A small neat stack of three thick square sheets of dark charcoal grey wool felt, matted fibres, slightly fuzzy edges, three-quarter view (a first try in light grey lost its edges to the background) |
| udder_balm | A single green square farm tin of udder balm salve with a cream label showing no text, lid tilted open, yellow ointment inside |
| wool_cap / wool_sweater / wool_trousers / wool_socks | A hand-knitted cream wool beanie with a ribbed brim / a cream fisherman sweater with a cable down the front / thick oatmeal wool trousers laid flat / thick hand-knitted cream wool socks with ribbed tops |
| lamb_chop / cooked_lamb_chop | A single raw lamb loin chop, pink meat, white fat rim, small T-bone / the same grilled with crisscross marks |
| mutton_chop / cooked_mutton_chop | A single large raw mutton chop, deep dark red meat, thick creamy fat rim, bone / the same roasted, dark crust |
| spoiled_mutton | A single spoiled raw mutton chop: dull grey-green discoloured meat, slimy sheen, yellowed fat, a couple of mould spots |
| sheep_milk_bottle | A single old-fashioned 1 litre clear glass bottle full of rich ivory-white sheep milk, a cardboard cap with a small sheep silhouette |
| sheep_cheese_wheel / sheep_cheese | A whole aged sheep milk cheese wheel like pecorino, pale straw basket-weave rind / a wedge of it, ivory crumbly interior |
| yarn | A single twisted skein of pure white wool yarn, soft plied strands. `process_art.py` tints it to the sixteen dye colours |

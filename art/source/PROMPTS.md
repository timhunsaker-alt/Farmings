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

## 2026-11-11: poultry (Ludo createImage, `item-icon`, Photorealistic 3D, 1 variation)
Every prompt ends "game item icon, plain white background, no text" (the down jacket: "plain grey background").

| File | Prompt |
|---|---|
| chicken_egg | A single brown hen's egg, smooth matte shell |
| turkey_egg | A single large turkey egg, pale cream shell covered in small brown speckles |
| quail_egg | A single small coturnix quail egg, cream shell mottled with dark brown and black blotches |
| duck_egg | A single large duck egg, smooth chalky white shell with a faint blue-green tint |
| fried_egg | A single fried egg sunny side up, bright yellow yolk, crisp white edges, seen from above at a slight angle |
| rotten_egg | A single rotten egg, dirty grey-green stained cracked shell with dark ooze leaking from the crack |
| egg_carton | A closed grey molded pulp cardboard egg carton for a dozen eggs, lid shut, seen at an angle |
| turkey / cooked_turkey | A single raw whole turkey drumstick and thigh, pale pink skin with goose bumps / A single roast turkey drumstick, golden brown crispy glazed skin |
| duck / cooked_duck | A single raw duck breast fillet with a thick layer of pale fat and skin on top, deep red meat / A single roast duck breast, crisp mahogany brown skin, sliced to show pink meat inside |
| quail / cooked_quail | A single small whole raw plucked quail, pale pink skin, legs tucked / A single small whole roast quail, golden brown glossy skin |
| spoiled_poultry | A single spoiled rotten raw poultry drumstick, grey-green slimy discoloured skin |
| down | A small fluffy cluster of white duck down feathers, soft wispy plumules |
| down_jacket | A single quilted puffy down jacket, navy blue with horizontal baffles, front view laid flat |
| poultry_litter | A small heap of used chicken coop litter, brown wood shavings mixed with dark droppings and a few small feathers |

Bird plumages are drawn by `tools/poultry_textures.py`. The coop, hatchery and grader faces are drawn by `tools/make_assets.py`, as for the
sheep shed.

## 2026-11-11: Phase 3, fields, feed and waste (Ludo createImage, `item-icon`, Photorealistic 3D, 1 variation)
Every prompt ends "game item icon, plain white background, no text".

| File | Prompt |
|---|---|
| alfalfa_seeds | A small pile of tiny kidney-shaped yellow-olive alfalfa seeds |
| timothy_seeds | A small pile of tiny round silvery-tan timothy grass seeds |
| orchardgrass_seeds | A small pile of slender pale straw-coloured orchardgrass seeds with tiny husks |
| corn_seeds | A loose handful of treated seed corn kernels, coated pink |
| oats_seeds / barley_seeds | A small pile of pale husked seed oats / a small pile of golden seed barley with short awns |
| alfalfa_hay / grass_hay / straw | A loose armful of green leafy alfalfa hay with a few purple flowers / of pale green-gold timothy grass hay / of bright golden dry cereal straw |
| alfalfa_bale / grass_bale / straw_bale | A single large round bale of green alfalfa hay / of pale green grass hay / of golden straw, net-wrapped, three-quarter view |
| corn_grain / oat_grain / barley_grain | A small pile of shelled yellow dent corn kernels / of harvested oat grain / of harvested golden barley grain |
| chopped_corn / chopped_alfalfa | A small heap of freshly chopped whole-plant corn forage with kernel bits / of chopped wilted green alfalfa |
| corn_silage / haylage | A small heap of fermented corn silage, moist olive-tan with yellow kernel pieces / of fermented alfalfa haylage, moist olive green |
| corn_meal / oat_meal / barley_meal | A small mound of coarsely ground yellow corn meal / pale oat meal feed / tan barley meal feed |
| alfalfa_meal / fish_meal / ground_limestone | A small mound of ground green alfalfa meal powder / of brown fish meal feed powder / of fine white ground limestone powder |
| mineral_premix | A small paper sack of livestock mineral and vitamin premix with a scoop of speckled pale powder |
| ration | A heap of total mixed ration cattle feed: chopped silage, hay pieces and ground grain blended together |
| compost / separated_solids | A small heap of dark crumbly finished farm compost / of separated digestate solids, damp fibrous brown manure fibre from a screw press |
| liquid_digestate | A small plastic jerrycan of dark brown liquid digestate fertilizer |
| tool_seed_drill | A green steel grain seed drill implement head with a seed hopper and a row of disc openers, isolated machine part |
| tool_harvester | A red combine harvester header with a reel and cutterbar, isolated machine part |
| tool_mower | A yellow disc mower-conditioner cutter bar with spinning discs, isolated machine part |
| tool_rake | A blue hay rake wheel head with spring tines, isolated machine part |
| tool_baler | A teal round baler body with a pickup and a rear tailgate, isolated machine part |
| tool_spreader | A brown farm manure and fertilizer spreader box with rear beaters, isolated machine part |

Crop stages, swaths, belts, the gantry, silo, mill, compost and digester faces are drawn by `tools/make_assets.py`.

## 2026-11-12: Loose ends
Ludo createImage, item-icon, Photorealistic 3D, n 1.
- `chicken`: "A raw whole chicken leg and thigh piece, pale pink raw poultry meat with skin, game item icon, centered, plain transparent background"
- `cooked_chicken`: "A roasted golden-brown cooked chicken leg and thigh, crispy skin, game item icon, centered, plain transparent background"
- `shepherds_whistle`: "A shepherd's sheepdog whistle: a small curved crescent-shaped metal whistle on a short leather lanyard, brushed steel, game item icon, centered, plain transparent background"

## 2026-11-13: goats (Ludo createImage, `item-icon`, Photorealistic 3D, 1 variation)
Every prompt ends "game item icon, plain white background, no text".
| file | prompt |
|---|---|
| kid_chop / cooked_kid_chop | A single raw goat kid loin chop, pale pink lean meat, thin white fat rim, small bone / a single grilled goat kid loin chop, browned with crisscross grill marks, small bone |
| chevon_chop / cooked_chevon_chop | A single large raw chevon goat chop, deep red lean meat, very thin fat edge, bone / a single roasted chevon goat chop, dark brown crust, bone |
| goat_milk_bottle | A single old-fashioned 1 litre clear glass bottle full of bright pure white goat milk, a cardboard cap with a small goat silhouette |
| chevre | A single small log of fresh soft white goat cheese chevre, bright white, slightly crumbly creamy surface, on nothing |

## 2026-11-13: geese (Ludo createImage, `item-icon`, Photorealistic 3D, 1 variation)
| file | prompt |
|---|---|
| goose_egg | A single large white goose egg, smooth matte chalky shell, much bigger than a hen egg |
| goose / cooked_goose | A single raw goose leg and thigh portion, dark red meat with a thick layer of pale yellow fat and skin / a single roast goose leg and thigh portion, crisp glossy deep golden brown skin |

## 2026-11-13: guinea fowl (Ludo createImage, `item-icon`, Photorealistic 3D, 1 variation)
| file | prompt |
|---|---|
| guinea_egg | A single small guinea fowl egg, light brown speckled shell, pointed at one end, smaller than a hen egg |
| guinea_fowl / cooked_guinea_fowl | A single raw guinea fowl drumstick, lean dark pinkish red game meat with thin pale skin / a single roasted guinea fowl drumstick, golden brown crisp skin, lean |

## 2026-11-13: pigs (Ludo createImage, `item-icon`, Photorealistic 3D, 1 variation)
| file | prompt |
|---|---|
| pork_chop / cooked_pork_chop | A single raw bone-in pork loin chop, pale pink meat with a white fat edge / a single pan-seared bone-in pork chop, golden brown seared surface |
| pork_belly | A single raw slab of fresh pork belly with skin on, visible layers of pink meat and white fat on the cut side |
| pork_shoulder | A single raw pork shoulder roast (Boston butt), marbled deep pink meat |
| pork_fat | A single thick slab of raw white pork back fat, creamy white |
| fresh_ham | A single whole raw fresh pork ham leg, pink meat at the cut end, pale skin, shank bone |
| spoiled_pork | A single spoiled raw pork chop: grey-green discoloured meat, slimy sheen, yellowed fat, a few mould spots |

## 2026-11-13: pork processing (Ludo createImage, `item-icon`, Photorealistic 3D, 1 variation; each prompt ends "game item icon, plain white background, no text")
| file | prompt |
|---|---|
| salt | A small heap of coarse white salt crystals |
| cure | A small cloth sack of curing mix: coarse salt mixed with brown sugar, open top |
| country_ham | A single whole dry-cured aged country ham, dark mahogany brown smoked rind, hanging string |
| ham_slice | A single thin slice of dry-cured country ham, deep rosy red meat with a white fat edge |
| bacon / cooked_bacon | Three raw smoked bacon rashers side by side, pink meat and white fat stripes / Three crispy fried bacon rashers, deep reddish brown and wavy |
| sausage / cooked_sausage | A short chain of three raw fresh pork sausage links in natural casing, pale pink with flecks of herbs / Three grilled pork sausages, browned and glistening with grill marks |
| lard / rancid_lard | A small glass jar of pure white rendered pork lard, smooth creamy surface, no lid / A small glass jar of rancid lard, yellowed and grey-streaked, dull, with a few dark spots |

## 2026-11-13: horses (Ludo createImage, `item-icon`, Photorealistic 3D, 1 variation)
| file | prompt |
|---|---|
| stud_book | A single closed leather-bound stud book, dark brown leather cover with a small gold embossed horseshoe, game item icon, plain white background, no text |

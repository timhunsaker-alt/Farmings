# Cheese armor decisions (2026-11-25)

Brief: Tim's idea, "turning milk armor into cheese armor its weaker but lasts longer and is edible". Tim was away. Each line is
**decided on recommendation; for Tim**, and Tim can overturn any of them. The spec is [../cheese-armor.md](../cheese-armor.md).

1. **Made in the vat, then the press.** A fresh milk piece is right-clicked onto the `cheese_vat`. The vat sets it on its next working
   second, with power, as it sets a cheddar batch. The piece then presses in the `cheese_press` for the press's own time (800 ticks,
   40 s), and is taken from the press by hand. A press-only path would skip setting the curd, which no cheese does, and the vat-then-press
   flow is the creamery's own. It works in any mode, one piece at a time. The press holds one load: wheels and the piece take turns.
   Hoppers do not move it. *Alternative:* press only, or require CHEESE mode.
2. **Spoiled milk armor is refused** with "Spoiled milk does not make cheese".
3. **No aging.** The piece does not go to the aging shelf (60 real days, 4 game days). It is a pressed young cheese. *Alternative:* age it
   on the shelf like a wheel.
4. **What carries over:** the cheese piece keeps the milk piece's share of durability and its enchantments. Its name and other data do not
   carry over.
5. **Weaker = iron.** Durability, defence, toughness, knockback resistance, enchantability and the equip sound are read at load from
   vanilla `ArmorMaterials.IRON`. Nothing is hardcoded. Game test `cheese_armor_iron_stats` compares each piece with vanilla iron.
6. **Repaired with cut cheese:** `cheddar` and `sheep_cheese` (tag `farmings:repairs_cheese_armor`). Chèvre is a soft fresh cheese and is
   left out. Wheels are left out because a wheel is 10 kg.
7. **Lasts longer = a cheddar wheel's room life.** food.md gives a whole cheddar wheel 14 days out of the cold *(proposed there)*. food.md
   plays shelf-life days as game days at 24,000 ticks, so the clock is **336,000 ticks**, 14 times milk armor's 24,000. It is the food
   clock (`SpoilsAt`), rounded up to a minute as all food is.
8. **The cold room applies.** The cold room already credits any food stack in a chest by its fridge and room lives. Cheese armor
   reports itself as cheddar-wheel food (`Perishables.foodOf`), so a powered fridge holds a fresh piece back toward the wheel's 180
   fridge days. This needed one line and no change to the cold room. Worn pieces are not chilled; moldy pieces are left alone. Nothing
   else pauses the clock. *Alternative:* no chilling, as milk armor.
9. **Moldy:** "Moldy Cheese …" names, moldy icons and worn look. Hunger and Nausea while any moldy piece is worn, refreshed each second,
   wearing off about 5 s after it comes off: the milk armor rule. A moldy piece keeps its durability.
10. **Eating:** sneak-use with the piece in hand eats it. It uses the vanilla food animation and sound, 1.6 s, the same as a slice.
    Plain use equips it. A worn piece is never eaten, which the eating code also checks.
11. **Hunger per piece.** Each material unit is a milk bucket of 10 L (the creamery's bucket). At Holstein's 3.72% fat and 3.06% protein
    (a vanilla bucket's milk, creamery.md), Van Slyke gives **1.04 kg of cheddar per unit**. Cheddar is 2.4 hunger per kg: a 10 kg wheel
    is 8 slices of 3 hunger. So:

    | Piece | Units | Cheddar | Hunger |
    |---|---|---|---|
    | Helmet | 5 | 5.2 kg | 12 |
    | Chestplate | 8 | 8.3 kg | 20 |
    | Leggings | 7 | 7.3 kg | 17 |
    | Boots | 4 | 4.2 kg | 10 |

    Saturation is cheddar's 0.7. The numbers are computed in code from the creamery's constants, not written in.
    *Alternative:* 0.5 kg a unit (the butter block's size), which halves them.
12. **Eating a moldy piece** gives moldy cheese's food and sickness: 1 hunger, Hunger 30 s, Nausea 15 s.
13. **No Field Manual page.** Milk armor has none; it is an easter egg.
14. **Art from Ludo.** Two surfaces and four icons. The moldy icons are made by script (`process_art.py` `mould()`), the way the spoiled
    milk icons are. Prompts are in `art/source/PROMPTS.md`.
15. **Colour pass (Tim, 2026-11-25: "reads brown, like cork").**
    - The worn layers are now a bright cheddar yellow-orange with a subtle rind and a few large round, lighter-rimmed Swiss-style eyes.
    - The moldy set is the same base with blue-green mould on top.
    - The icons were redrawn by Ludo in the same style.

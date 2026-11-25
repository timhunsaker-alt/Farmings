# Cheese armor

**Status: built 2026-11-25.** Tim's idea: "turning milk armor into cheese armor its weaker but lasts longer and is edible". Each choice
below was made on a recommendation while Tim was away and is logged in [questions/cheese-armor.md](questions/cheese-armor.md), where Tim
can overturn it.

## Why it fits
Milk armor ([milk-armor.md](milk-armor.md)) is the joke that milk spoils. Cheese is how farms keep milk: pressed curd keeps for weeks
where milk keeps for a day. So the milk armor goes through the creamery the way milk does, and comes out weaker, longer-keeping and
edible.

## Making it: the vat, then the press
1. Right-click the creamery's `cheese_vat` with a **fresh** milk armor piece. The creamery must be formed. The piece goes into the vat,
   one piece at a time.
2. The vat sets it as it sets a cheddar batch: at once, on its next working second, if the vat has power (the creamery's controller runs
   once a second). With no power the readout shows `NO POWER: cheese vat` and the piece waits.
3. Then the piece goes to the `cheese_press` and is pressed for the press's own time, `PRESS_TICKS` (12 real hours at the creamery's
   15:1 scale, 800 ticks or 40 s; creamery.md). The press holds one load: the piece waits in the vat while wheels are pressing
   (`PRESS BUSY`), and wheels wait while the piece is pressing.
4. Right-click the press with an empty hand to take the matching cheese piece: helmet to helmet, chestplate to chestplate, and so on.
   It keeps the same share of its durability and its enchantments. Its clock starts when it leaves the press, as creamery food does.

- **Refused:**
  - a spoiled milk piece: "Spoiled milk does not make cheese" (sour milk does not make cheddar);
  - a second piece while one is in the vat or press;
  - any piece while a chèvre batch drains in the vat.
- **Not aged:** the piece does not go to the aging shelf. It is a pressed young cheese.
- **Not cooled on the way:** the creamery counts as cold (food.md), so the milk piece's own clock does not matter once it is in the vat.
- **Saving:** the piece and its step (vat, press, ready) are saved with the controller (creamery save version 4; older saves load).
- **Breaking the controller:** a piece in the vat or press drops as the milk piece it was, or as the cheese piece if it was ready.
- **Hoppers:** they do not move armor in or out.

## Weaker: iron's stats
The material is copied at load from vanilla iron armor, never written here, so it follows the game. It takes iron's durability,
defence, toughness, knockback resistance, enchantability and equip sound. Milk armor copies netherite the same way. It is repaired with
cut cheese: `cheddar` or `sheep_cheese` (tag `farmings:repairs_cheese_armor`).

## Lasts longer: a cheddar wheel's shelf life
- **Clock:** a whole cheddar wheel's life out of the cold (food.md): **14 days**, played as game days. At 24,000 ticks a day that is
  **336,000 ticks**, 14 times milk armor's 24,000. The clock is the food clock (custom data `SpoilsAt`, rounded up to a minute as all
  food is).
- **Moldy:** after that the piece goes moldy, and keeps its durability.
  - Its name becomes "Moldy Cheese …".
  - Its icon and worn look change to the moldy texture.
  - While any moldy piece is worn, the wearer has Hunger and Nausea, as with spoiled milk armor. They are refreshed each second and wear
    off about 5 s after it comes off.
- **Cold room:** a fresh piece kept in a chest in a powered walk-in fridge is held back as a cheddar wheel is: 180 days at 4 °C, by the
  room's temperature between. This uses the cold room's existing food rule with no new code there. Nothing else pauses the clock.

## Edible
- **Eating:** hold a piece and **sneak-use** it. It plays the vanilla eating animation and sound (1.6 s), and the piece is used up.
  Plain use still puts it on.
- **Food:** each piece holds the cheddar of the milk it was made from. Each material unit was a milk bucket, which at the creamery is
  10 L. 10 L of Holstein milk (3.72% fat, 3.06% protein, as a vanilla bucket) makes **1.04 kg** of cheddar by the creamery's Van Slyke
  yield. A cheddar wheel (10 kg) cuts into 8 slices of 3 hunger, so cheddar is **2.4 hunger per kg**. The saturation modifier is
  cheddar's, 0.7.

| Piece | Units | Cheddar | Hunger |
|---|---|---|---|
| Helmet | 5 | 5.2 kg | 12 |
| Chestplate | 8 | 8.3 kg | 20 |
| Leggings | 7 | 7.3 kg | 17 |
| Boots | 4 | 4.2 kg | 10 |

- **Moldy pieces** eat as moldy cheese: 1 hunger, then Hunger (30 s) and Nausea (15 s).
- **Worn pieces:** a piece being worn is never eaten. You can eat a piece from your hand while wearing another one.

## Art
- **Surfaces:** two Ludo surfaces (`art/source/mat_cheese_armor.webp`, young cheddar with a few holes; `mat_cheese_moldy.webp`,
  with blue-green mould spots). Prompts are in `art/source/PROMPTS.md`.
- **Worn look (redone 2026-11-25; Tim: the first pass read brown, like cork):** `tools/armor_textures.py` (`paint_cheese`) draws each
  face of the vanilla armor layout:
  - a bright, warm cheddar yellow-orange (RGB 255, 202, 84), with the fine grain of the fresh surface;
  - a subtle deeper-orange rind along the edges;
  - a few large round eyes, drawn as shallow hollows with a lighter lower rim. Swiss-style eyes read better than cheddar's at this
    scale.
  
  The moldy set is the same cheese, a little duller, with blue-green mould spots in a pale fuzzy ring. The mould colours are taken from
  the moldy surface. The change reads as cheese gone bad.
- **Icons:** four Ludo icons of bright cheese with large eyes, warmed a touch by `tools/process_art.py` (`warm()`). The moldy icons are
  the same icons with mould added (`mould()`).

## Code and tests
- **Code:**
  - `item/CheeseArmorItem` (clock, mould, eating);
  - `Creamery.ArmorStep` and the controller's held piece;
  - the vat and press clicks in `CreameryPartBlock`;
  - `Perishables.foodOf` (the cold room).
- **Game tests:**
  - `cheese_armor_iron_stats`
  - `cheese_armor_from_milk_armor`
  - `cheese_armor_spoils_later_and_sickens`
  - `cheese_armor_edible`
- **Client scene:** `FARMINGS_ONLY=armor` shows milk armor fresh and spoiled, then cheese armor fresh and moldy.

# Easter egg: milk armor

**Status: built 2026-11-09** (Tim moved it into the cattle finish). Decisions on 2026-11-09: Hunger + Nausea, nothing pauses the spoiling,
a spoiled piece keeps its durability.

## What Tim asked for
- Armor whose crafting recipes use **buckets of milk**.
- While fresh it has **netherite durability** (the same protection and durability as netherite armor).
- After **one day cycle** it goes bad, **makes the player sick until it is taken off**.

## Why it fits
Real milk spoils without refrigeration. The joke is that you wore the milk too long.

## Rules as understood
- Recipe: buckets of milk plus the ingredients of the matching armor piece. Which ingredients is a **DECISION**.
- Freshness is per piece and starts when it is crafted. A day cycle is one Minecraft day (20 minutes; the same unit as a game day).
- Stats: copied from vanilla netherite armor, never hardcoded here, so they follow the game's version.
- Once spoiled: the wearer gets a sickness status effect for as long as any spoiled piece is worn. Taking the piece off ends it.

## Decisions (open)
1. Which effect. **Recommended: Hunger and Nausea.** Not poison or wither, because the effect must not kill.
2. Does a spoiled piece keep its durability? **Recommended: yes**, so removing it is the only fix.
3. Can it be made fresh again? **Idea, not decided:** chilling it in a Wildcatter-powered cold room would pause the clock, which
   ties the joke to the real thing (refrigeration), and ties in the bulk tank's chiller.
4. Does it count as food or armor for crafting and recipe tabs? Armor.
5. Does the spoiled sickness also stop eating or the armor from showing off-colour? A spoiled texture is placeholder art until
   Ludo art exists.

## Built (2026-11-09)
- Four pieces (`milk_helmet`, `milk_chestplate`, `milk_leggings`, `milk_boots`). Material copied at load from vanilla netherite (durability,
  defence, toughness, knockback resistance, enchantability, sound). Repaired with milk buckets (tag `farmings:repairs_milk_armor`).
- The clock starts when it is crafted (or the first tick it exists). After 24,000 ticks it spoils: the name becomes "Spoiled ...", the
  item and worn textures change. While any spoiled piece is worn: Hunger and Nausea, refreshed each second; they wear off about 5 seconds
  after it comes off.
- Recipe: the vanilla armor shapes with milk buckets as the material; the buckets come back empty (docs/recipes.md).
- Game test: milk_armor_spoils_and_sickens.

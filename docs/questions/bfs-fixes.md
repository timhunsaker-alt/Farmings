# Built From Scratch quest fixes: judgement calls (2026-11-26, branch `bfs-fixes`)

From the quest check (`videos/longform/built-from-scratch/QUEST_CHECK.md`), approved by Tim. Wildcatter's side is in its own
`docs/questions/bfs-fixes.md`.

### 1. Teat dip: recipe count 16, or stack size 64?
- **Chosen:** the recipe makes 16 (it made 64 of an item that stacks to 16, so the server refused it and nothing crafted). The stack stays 16.
- **Why:** a teat dip is a bottle of liquid, one bottle dipping 50 cows (docs/animal-health.md). Every other bottled health item (drugs,
  vaccines, electrolytes) stacks to 16, and liquid bottles in the game stack small. 16 bottles still dip 800 cows from one chlorine ton
  container.
- **Alternatives:** raise `stacksTo` to 64 (the only bottled health item that would stack to 64).

### 2. footbath: what it requires now
- **Chosen:** `herd_health` only (was `herd_health` and `wildcatter:fertilizer`).
- **Why:** zinc sulfate is a zinc ingot (smelted sphalerite), sulfur (the salt-dome caprock ore's drop) and water; nothing from the
  fertilizer plant is used. The text names the zinc and sulfur sources.

### 3. milk_quality
- Requires `wildcatter:chlor_alkali` (the recipe's chlorine ton container; its closure brings plastic for the CMT paddle). Its text names
  the chlorine.

### 4. isolation_pen: fired while formed
- **Chosen:** the controller fires the milestone on each structure check while the pen is formed (every 100 ticks), as Wildcatter's
  `Multiblock.formed` does; the saved `announced` flag is gone (an old save's flag is ignored).
- **Why:** fired once ever, a pen formed with nobody within 64 blocks never fired again. The milestone call itself counts once every 5 s
  for a position and key.

### 5. The check's machine table
- `QuestReach` is the same class as Tailings', with its machine table updated for Wildcatter 0.10.2 (see Tailings'
  `docs/questions/bfs-fixes.md`).
- Until Wildcatter 0.10.2 is released, build with
  `-Pwildcatter_repo=file:/Volumes/KINGSTON/home/minecraft-mods/wildcatter-local-releases`.

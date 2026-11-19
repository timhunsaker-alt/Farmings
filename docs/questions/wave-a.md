# Wave A decisions (2026-11-19)

Brief: record Tim's 2026-11-19 answers, fix the digester's power bug from the farm short, any art placeholders, release prep. Tim was away;
each line is **decided on recommendation; for Tim**.

1. **Digester power on any cell, by an internal bus.** The mixer is one Wildcatter `Motor` whose terminals can sit on one cell only. Its
   terminals now face up from the main cell into the tank, and a three-phase bus (two contact joints, 0.002 ohm each) carries them to a
   terminal on every outside face of all 27 cells. The motor itself (inrush, stall, lead swap) is unchanged. Wildcatter's machines use
   one terminal box; "any cell" is what the farm short and Tim's bug report expect, so any cell it is. A one-block tank from an old world
   (not grown) keeps terminals on its own faces.
2. **A CHANGELOG after all.** `quests.md` 13 kept the history in PROGRESS.md only; a public release needs release notes, so `CHANGELOG.md`
   now exists, starting at 0.1.0.
3. **Version 0.1.0** for the first public release (gradle.properties was 0.0.1, never released). Wildcatter requirement stays 0.8.1.
4. **Sheep stall art without Ludo.** The only `*_placeholder` texture left was the sheep stall's. It is now a true-size model from the
   parts library (`tools/models/parts.py`) on the Ludo materials already in the repo, as every other parlor part is, so no Ludo credits
   were spent. Its counterpart: a sheep and goat milking stall with a headlock yoke and a feed trough; collision unchanged (0.19 m).

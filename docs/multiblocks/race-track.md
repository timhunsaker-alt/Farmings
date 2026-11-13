# The race track

**Status: spec, 2026-11-13 (Step 18; Tim: "A race track you build").** Marks as in `docs/crops.md`. No betting, no prize money: nothing is
sold (VISION.md).

## Parts
| Part | Block | What it does |
|---|---|---|
| Track | `race_track` | A dirt racing surface laid in a closed loop, 3 or more blocks wide *(proposed)*. |
| Rail | `track_rail` | Inner and outer rails along the loop (any fence also counts). |
| Start gate | `start_gate` | Placed across the track: one stall a track block of width, so a 4-wide track starts 4 horses *(proposed: a real gate's stall width is NOT FOUND)*. Sneak-use sets the distance; use starts the race. |
| Finish post | `finish_post` | Beside the track at the finish line. Times every horse that crosses after running the distance. |

## Forming
- The track forms when its surface makes one closed loop and the gate and post stand on it. The **lap length** is measured along the
  loop's centre line, in blocks (1 block = 1 m).
- **Distances** *(the common ones)*: 402 m (2 f, the Quarter Horse's 440 yd), 1,207 m (6 f), 1,609 m (8 f, a mile), 2,012 m (10 f, the
  Derby), 2,414 m (12 f, the Belmont). A race is as many laps and part-laps as its distance; the finish post sits where the distance
  ends *(proposed: the post is moved for each distance, or the race starts from the gate and finishes the first time a horse passes the
  post after covering the distance)*.

## A race
- **Entrants:** a horse stands in a gate stall: ridden by a player, or led in tame and unridden, when an **AI jockey** rides it (no
  jockey entity; the horse runs its own line) *(proposed)*.
- **Start:** use the gate: a three-second count, then the gate opens.
- **Running:** each horse runs at its pace for the distance it has covered (horses.md, "Speed"). An AI horse follows the track's centre
  line, keeping to its lane until the last 400 m *(proposed)*. A player rides as they like.
- **Finish:** the post records each horse's time to 0.05 s (one tick) and its place. A horse that leaves the track is out.
- **Records:** each horse keeps its best time at each distance, races run and wins; the stud book copies them. The readout of the post
  lists the last race's result.
- **Fitness:** a race counts as work (horses.md, "Fitness").

## A timed run
- Ride through the start gate and past the finish post: the post times a solo run at the gate's set distance, recorded the same way.

## Not in this pass
- Turf versus dirt, going (track condition), handicap weights, steeplechase jumps, harness racing.

## Built, 2026-11-13
- **Code:** shape in `husbandry/racing/TrackShape` (plain Java, `TrackShapeTest`); blocks in `racing/` (`RaceBlocks`, `StartGateBlock`,
  `StartGateEntity`, `FinishPostBlock`); records on `HorseEntity`; `item/StudBookItem`.
- **Forming:** the gate stands beside the track (at track level or one above it). From the track block next to it the gate finds every
  joined `race_track` block at that level (a flat track *(proposed)*). The track must be a **ring around an infield**, an oval or rounded
  rectangle: its middle is not track, and it is cut into 72 directions out from the middle (5° each), every one of which must cross
  track. The **centre line** joins the middle of the track in each direction; the lap is its length (an oval's lap comes within 3% of
  the true perimeter, `TrackShapeTest`). Races run **counter-clockwise seen from above**, as in American racing *(proposed)*.
- **Stalls** are the track blocks straight across the track from the gate; the track must be at least 3 wide there. A **finish post**
  must stand beside the track.
- **Differences from the spec above:**
  - **The finish is the distance, not the post:** each horse's progress is measured along the centre line from where it started, and
    it is timed (to the tick, 0.05 s) when it has covered the race distance, wherever on the loop that falls. The post is where you
    read the result; where it stands doesn't change the timing *(proposed: exact distances without moving the post)*.
  - **AI jockeys** run a led-in horse along its own lane (its start offset from the centre line) the whole way, at `Gallop.speedAt` for
    the distance covered; they move the horse directly, a tick at a time, with the horse's own AI held off and handed back after.
    No move to the rail in the last 400 m. A horse ridden by a player runs as its rider rides it (`horses.md`).
  - An AI lane is kept 1.3 m inside the track's edge so the horse clears the rails (found by the `race` client scene, where horses in
    the outside and inside stalls ran into the rails). An AI-run horse that gains less than a metre in 5 s is out.
  - Distance counts from where each horse is as the gate opens, so a rider who sets off in the countdown gains no ground (there is no
    false-start rule).
  - A horse off the track for 2 s is out. A race ends when every horse has finished or is out, or after the distance at 4 m/s.
  - The race in hand isn't saved: after a save a horse held by a jockey gets its AI back. The last result is saved with the gate.
  - **Rails** (`track_rail`, a fence) are for the builder; the track forms without them.
  - A win counts when two or more horses ran; a solo run is a timed run (recorded, no win).
- **Stud book:** a page a horse (up to 64): its herd ID and name, breed and stage, colour, MSTN, age, SPEED and STAMINA, fitness, races,
  wins and best times; its sire and dam, each with their own sire and dam when the book has a page for them (grandparents from the
  book's own records *(proposed)*). Crafted from a book and quill and leather.
- **Tests:** unit `TrackShapeTest`; game tests track_forms, race_two_horses (a C/C Quarter Horse beats a T/T Arabian over 402 m, each
  within 5% of the speed model's time), race_ridden_run_timed (a ridden horse carried at 15 m/s is timed at 26.8 s), stud_book_pedigree.

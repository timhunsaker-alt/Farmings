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

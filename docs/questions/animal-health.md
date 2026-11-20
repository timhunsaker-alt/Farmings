# Animal health (Wave B, build 1): questions for Tim (2026-11-19)

The spec is `docs/animal-health.md`, with the isolation pen in `docs/multiblocks/isolation-pen.md`. Branch `animal-health-spec`.
Format as in Wildcatter's `docs/questions/`.

**Answered by Tim 2026-11-19: all eight, each with the recommended option** (the "Chosen" line of every entry below). The spec is
approved and built on this branch. Only real design choices are here; every other open point in the spec was **decided on
recommendation; for Tim** and says so where it stands. The recommended option is listed first and is what the spec assumes.

### 1. Can disease kill an animal?
- **Chosen:** no, by default. Disease takes health down to the existing floor (2 of 10 for cattle) and costs milk, growth, eggs and
  condition; an animal that stays sick is a poor producer the player decides about. A `farmingsDiseaseDeaths` world rule (off) turns on
  deaths at real case fatality (under 5% of clinical mastitis cases, NAHMS Dairy 2014; overeating disease often fatal), with the animal
  simply lying down and not getting up (no gore).
- **Why:** every rule in Farmings so far says "never dies" (hunger, thirst, dirt, cold). Deaths change the game's tone and punish a player
  who was away. The rule keeps it for players who want the stakes.
- **Alternatives:** always on at real rates (most realistic, harshest); never, with no rule (simplest).
- **Answered by Tim 2026-11-19:** the chosen option.

### 2. Which diseases are on by default?
- **Chosen:** the nine endemic diseases on in new worlds (off in worlds saved before, as Wildcatter does for new rules); the three
  notifiable diseases off, opt-in through `farmingsNotifiableDisease`.
- **Why:** the endemic ones are the vet cycle; the notifiable ones can stop a whole farm for an hour of play, which a player should choose.
- **Alternatives:** all on; all off (the whole system opt-in).
- **Answered by Tim 2026-11-19:** the chosen option.

### 3. What does reporting a notifiable disease do?
- **Chosen:** a standstill on every Farmings housing within 192 blocks: no animal in or out, no taming, no hatching out, no straws; sick
  animals recover over 14 real days; it lifts 28 real days after the last case. No culling. Not reporting lets it spread at 10x.
- **Why:** a standstill is the real first response and can be played without gore. Real control of foot-and-mouth and HPAI is mass
  culling, which Farmings will not show.
- **Alternatives:** leave notifiable diseases out entirely; offscreen culling of the affected group (it vanishes, with a restock wait).
- **Answered by Tim 2026-11-19:** the chosen option.

### 4. Who is the vet?
- **Chosen:** the player, with a vet kit: herd check on a controller, exam, CMT, treatment, records.
- **Why:** Farmings has no NPCs; the routine is the gameplay. A visiting vet would do the interesting part for the player.
- **Alternatives:** an NPC vet who visits every few game days and does the herd check and exams (the player treats); both (kit plus
  an optional scheduled visit that only writes the herd check).
- **Answered by Tim 2026-11-19:** the chosen option.

### 5. Milk during a withdrawal or clinical mastitis
- **Chosen:** the parlor knows the cow's state, milks her last and diverts her milk to a waste milk can automatically.
- **Why:** a real herd management system does flag treated cows; the cost (lost milk, a can to empty) stays, and one forgotten click
  can't spoil a 1,000 L tank.
- **Alternatives:** the player must band her (the kit bands her on treatment, but a band can be taken off), and a missed one puts residue
  in the tank, which must be dumped; or the parlor refuses her and she is milked by hand into a bucket.
- **Answered by Tim 2026-11-19:** the chosen option.

### 6. Scope of the first build
- **Chosen:** all twelve diseases in the spec (mastitis, BRD, scours, foot rot, overeating disease, erysipelas, coccidiosis, Marek's,
  strangles, and three notifiable).
- **Why:** every species gets something that ties into a building it already has (parlor, corral, hatchery, shed pasture, stable).
- **Alternatives:** cattle first (mastitis, BRD, scours, foot rot) and the rest in a second pass; the twelve plus internal parasites
  (worms and dosing), which is a big system of its own.
- **Answered by Tim 2026-11-19:** the chosen option.

### 7. Pace
- **Chosen:** real annual incidences on each species' clock: about one mastitis case per four cows per cow-year (24 game days), BRD in
  about one in six newly tamed cattle without a vaccine.
- **Why:** the same rule as every other Farmings figure; at 15:1 an 8-cow barn sees a case every game day or two, enough for a routine.
- **Alternatives:** half the real rates (gentler); double (busier vet cycle).
- **Answered by Tim 2026-11-19:** the chosen option.

### 8. Quarantine for wild-caught animals
- **Chosen:** optional. If an isolation pen in QUARANTINE mode is within 64 blocks of the corral, tamed animals go there first for 30 real
  days; otherwise they go straight to housing. Some arrive carrying disease (BRD stress, strangles carriers, rarely a notifiable one) that
  quarantine keeps away from the herd.
- **Why:** it rewards doing it right without blocking a new player who hasn't built one.
- **Alternatives:** required: housing refuses a tamed animal until it has done its 30 days; no quarantine at all, a hospital pen only.
- **Answered by Tim 2026-11-19:** the chosen option.

## Build decisions (2026-11-19, decided on recommendation; for Tim)
Logged while building the answered spec. Format as above.

### 9. The waste milk can stands beside the parlor rather than in its layout
- **Chosen:** a `waste_milk_can` block anywhere within 4 blocks of the parlor controller; withheld milk goes there. With none (or full), a
  withheld cow is not called and keeps her milk.
- **Why:** a layout part would make every existing parlor fail to form until rebuilt.
- **Alternatives:** a parlor part on the milk line (a real dump line); milking withheld cows last by reordering the call.

### 10. Signs shown by walk, sound and readout before the drawn poses
- **Chosen:** a sick animal walks slower (lame 30%), coughs (four Ludo coughs) and the readout lists its signs and the red leg band; the
  head-low pose, limp, leg band and erysipelas marks are not drawn yet.
- **Why:** each is a change to six species' models; the behaviour is complete without them.
- **Alternatives:** draw them now, species by species.

### 11. Chronic mastitis at 1,000,000 cells/mL
- **Chosen:** 1,000,000 (proposed), above the 750,000 tank limit, so chronic cows are what push a tank to HIGH SCC.
- **Why:** with 700,000 no herd could ever reach the limit (clinical milk never goes in the tank).
- **Alternatives:** 700,000 and a lower warning threshold.

## Finish-up decisions (2026-11-20, branch `finish-0.2`, decided on recommendation; for Tim)
Closing the Known gaps of decision 10 and the risk factors left unwired. Format as above.

### 12. How the signs are drawn
- **Chosen:** poses in each model after its own walk (head low 26 degrees and 1.5 px; the limp on the right fore: a 35% stride, the
  sound fore 125%, the foot held up at rest, a head nod as the lame foot lands), and a layer that adds small cubes sized from the leg's
  and body's own cube: a 2 px red band round **both** hind legs under a withdrawal, five raised diamond patches a flank on a pig with
  erysipelas. Textures from two Ludo swatches (a red plastic band, red-purple skin), tiled by `tools/process_art.py`. The server syncs one
  flags value per animal (`HealthSigns`).
- **Why:** one layer fits all species and body shapes (beef, zebu, lambs, foals) with no per-model UV work; both hind legs so the band
  shows from either side, as many dairies band them.
- **Alternatives:** one band on one leg (truer to some farms, hidden half the time); painting marks into each coat texture (six texture
  sets to redo).
- Birds show the head low only; Marek's sitting pose is still not drawn.

### 13. Dirt for goats, pigs, horses and birds
- **Chosen:** the housing's own dirt state as a share: shed and stable bedding (soiled share, 1 when DIRTY), the pig barn's pit (how full,
  1 when FULL), the coop's litter (share of the full store); 0 on pasture or paddock.
- **Why:** each housing already has a dirt rule with its own health cost; this reads it rather than inventing a second figure.
- **Alternatives:** binary (1 only when the housing reads DIRTY); a per-animal figure like the cattle stall.
- Sheep read 0 until this build (their dirt field was never set); they now read the shed.

### 14. Dam vaccination window and which vaccine
- **Chosen:** any dose of the dam's vaccine (respiratory for cows, CDT for ewes and does) in the 42 real days before the birth halves the
  young's scours risk, as the spec says. Sows have no such vaccine in this build. Orphans are not doubled (the spec's orphan rule needs
  a dam link the young don't keep).
- **Alternatives:** require a full, in-date course as well.

### 15. Weaning ages for calves
- **Chosen:** Farmings calves do not suckle, so weaning is by age: dairy breeds 56 real days (8 weeks), beef breeds 205 (the age beef
  weaning weights are adjusted to). Crossing it opens the 45-day BRD window. Both **TO VERIFY**.
- **Alternatives:** one age for all (56); a weaning act by the player (a new mechanic).

### 16. Footbath drag-out
- **Chosen:** 1 L carried out per dosed animal *(proposed, no source found)*; the bath needs 60 L to work, so a full bath serves 40
  animals before a top-up. The block shows its level in eighths.
- **Why:** the level visibly falls and the water pump has a job; 40 head between top-ups is a plausible working day.
- **Alternatives:** 0.5 L (as built in 0.2.0, invisible: the readout rounded it away); dosing by concentration.

### 17. The old footbath film
- **Found:** the 0.2.0 health scene walked the lame cow and the well cow 12 blocks east, which left them standing at the end of the
  footbath lane; they were what looked like cows in the footbath film (the three in the lane were Suffolk ewes). The new films stand
  each set 20+ blocks apart and the footbath film shows only the three ewes.

# Animal health (Wave B, build 1): questions for Tim (2026-11-19)

The spec is `docs/animal-health.md`, with the isolation pen in `docs/multiblocks/isolation-pen.md`. Branch `animal-health-spec`.
Format as in Wildcatter's `docs/questions/`. Only real design choices are here; every other open point in the spec was **decided on
recommendation; for Tim** and says so where it stands. The recommended option is listed first and is what the spec assumes.

### 1. Can disease kill an animal?
- **Chosen:** no, by default. Disease takes health down to the existing floor (2 of 10 for cattle) and costs milk, growth, eggs and
  condition; an animal that stays sick is a poor producer the player decides about. A `farmingsDiseaseDeaths` world rule (off) turns on
  deaths at real case fatality (under 5% of clinical mastitis cases, NAHMS Dairy 2014; overeating disease often fatal), with the animal
  simply lying down and not getting up (no gore).
- **Why:** every rule in Farmings so far says "never dies" (hunger, thirst, dirt, cold). Deaths change the game's tone and punish a player
  who was away. The rule keeps it for players who want the stakes.
- **Alternatives:** always on at real rates (most realistic, harshest); never, with no rule (simplest).

### 2. Which diseases are on by default?
- **Chosen:** the nine endemic diseases on in new worlds (off in worlds saved before, as Wildcatter does for new rules); the three
  notifiable diseases off, opt-in through `farmingsNotifiableDisease`.
- **Why:** the endemic ones are the vet cycle; the notifiable ones can stop a whole farm for an hour of play, which a player should choose.
- **Alternatives:** all on; all off (the whole system opt-in).

### 3. What does reporting a notifiable disease do?
- **Chosen:** a standstill on every Farmings housing within 192 blocks: no animal in or out, no taming, no hatching out, no straws; sick
  animals recover over 14 real days; it lifts 28 real days after the last case. No culling. Not reporting lets it spread at 10x.
- **Why:** a standstill is the real first response and can be played without gore. Real control of foot-and-mouth and HPAI is mass
  culling, which Farmings will not show.
- **Alternatives:** leave notifiable diseases out entirely; offscreen culling of the affected group (it vanishes, with a restock wait).

### 4. Who is the vet?
- **Chosen:** the player, with a vet kit: herd check on a controller, exam, CMT, treatment, records.
- **Why:** Farmings has no NPCs; the routine is the gameplay. A visiting vet would do the interesting part for the player.
- **Alternatives:** an NPC vet who visits every few game days and does the herd check and exams (the player treats); both (kit plus
  an optional scheduled visit that only writes the herd check).

### 5. Milk during a withdrawal or clinical mastitis
- **Chosen:** the parlor knows the cow's state, milks her last and diverts her milk to a waste milk can automatically.
- **Why:** a real herd management system does flag treated cows; the cost (lost milk, a can to empty) stays, and one forgotten click
  can't spoil a 1,000 L tank.
- **Alternatives:** the player must band her (the kit bands her on treatment, but a band can be taken off), and a missed one puts residue
  in the tank, which must be dumped; or the parlor refuses her and she is milked by hand into a bucket.

### 6. Scope of the first build
- **Chosen:** all twelve diseases in the spec (mastitis, BRD, scours, foot rot, overeating disease, erysipelas, coccidiosis, Marek's,
  strangles, and three notifiable).
- **Why:** every species gets something that ties into a building it already has (parlor, corral, hatchery, shed pasture, stable).
- **Alternatives:** cattle first (mastitis, BRD, scours, foot rot) and the rest in a second pass; the twelve plus internal parasites
  (worms and dosing), which is a big system of its own.

### 7. Pace
- **Chosen:** real annual incidences on each species' clock: about one mastitis case per four cows per cow-year (24 game days), BRD in
  about one in six newly tamed cattle without a vaccine.
- **Why:** the same rule as every other Farmings figure; at 15:1 an 8-cow barn sees a case every game day or two, enough for a routine.
- **Alternatives:** half the real rates (gentler); double (busier vet cycle).

### 8. Quarantine for wild-caught animals
- **Chosen:** optional. If an isolation pen in QUARANTINE mode is within 64 blocks of the corral, tamed animals go there first for 30 real
  days; otherwise they go straight to housing. Some arrive carrying disease (BRD stress, strangles carriers, rarely a notifiable one) that
  quarantine keeps away from the herd.
- **Why:** it rewards doing it right without blocking a new player who hasn't built one.
- **Alternatives:** required: housing refuses a tamed animal until it has done its 30 days; no quarantine at all, a hospital pen only.

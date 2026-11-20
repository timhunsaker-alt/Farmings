# Changelog

## v0.2.0 (unreleased): animal health

- **Disease:** twelve real diseases (mastitis, shipping fever, scours, foot rot, overeating disease, swine erysipelas, coccidiosis,
  Marek's disease, strangles, and three notifiable ones off by default). They spread inside a barn, shed, coop, pig barn or stable; dirty
  stalls, poor feed and the HEALTH gene raise or lower the risk. Sick animals cough, limp, walk slower and give less; they never die of it
  unless the `disease_deaths` world rule is on.
- **The vet cycle:** a vet kit (temperature, diagnosis, herd check, sick pen, report), the CMT paddle, drugs with real withdrawal times
  (withheld milk goes to a waste milk can, condemned meat drops nothing), five vaccines that keep only in the cold room, teat dip, a
  footbath, bulk-tank somatic cell counts, and health pages in the herd book.
- **Quarantine and the sick pen:** the isolation pen; tamed animals near one wait out 30 real days in it.
- **World rules** `disease`, `notifiable_disease` and `disease_deaths` on Wildcatter's World Rules screen; worlds saved before start with
  disease off.
- Five quests, a Field Manual chapter, new icons and sounds.

## v0.1.0 (unreleased): the first public release

Farmings is a Wildcatter add-on (Minecraft 26.3, Fabric and NeoForge) for raising animals and growing crops the way a modern farm does.
It needs Wildcatter 0.8.1 or later. The full build history is in `PROGRESS.md`.

### What is in it
- **Animals with a sex, a breed and a genome:** cattle (Holstein, Jersey, Brown Swiss, Angus, Wagyu, Gyr), sheep (Merino, Suffolk, Dorper,
  East Friesian), goats, pigs, horses, and poultry (chickens, ducks, turkeys, quail, geese, guinea fowl). Coats, fleece and plumage come
  from their genes; breeding, pregnancy, growth and lactation run on real figures (sources in `docs/animals/`). Straws for artificial
  insemination, herd and stud books.
- **Wild animals and the taming corral:** wild herds replace vanilla spawns; bait them through a one-way gate and they settle into the herd.
- **Buildings on Wildcatter's grid:** a freestall barn with a manure scraper, a milking parlor and bulk tank (cows, ewes and does), a
  creamery (pasteurizer, separator, churn, cheese), a cold room, a sheep shed and shearing shed, a wool mill, a coop and hatchery, a pig
  barn with heat lamps and a slurry store, a stable and a race track.
- **Fields:** a field gantry on rails that sows, mows, rakes, bales and spreads; corn, alfalfa, oats and more; conveyors and augers; a
  tower silo and grain bin; a feed mill and TMR mixer.
- **Waste to power:** compost windrows and a 3 x 3 x 3 digester whose biogas holder feeds a Wildcatter gas set.
- **Quest book chapter** in Wildcatter's quest book (milestone `farmings:tamed`) and an in-game Field Manual.

### Since the last build (2026-11-19)
- **Fixed:** a 480 V cable touching any outside face of the digester tank now powers its mixer (before, only a cable under the tank's
  bottom centre did, so a cable on a side cell left it unpowered). Game test `genset_powers_digester_side_cell`.
- **Art:** the sheep stall has a real model (a headlock yoke and feed trough on a matted standing); no placeholder texture is left.
- Tim's answers recorded: the quest decisions (taming milestone one key for every species, players within 64 blocks; Wildcatter fertilizer
  bags 100 uses) are accepted.

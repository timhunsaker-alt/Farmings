# Growth: how calves and heifers gain weight (APPROVED by Tim 2026-11-08, built)

**Status: approved and built 2026-11-08** (curve points as proposed, feeding option 1, slower growth the only effect of underfeeding). *(proposed)* = a proposal. *(unverified)* = no good source yet.

## Why
Weight is still set by hand, so a calf born in the game never grows up (cattle.md stages and the breeding rules read weight:
puberty at 40% of mature weight, first breeding at 15 months and 55%). This adds growth so the herd renews itself.

## What the sources give
- **Weight at about 15 months** (Duplessis and colleagues, as quoted by [Dairy Producer](https://www.dairyproducer.com/tracking-heifer-growth-building-a-strong-foundation-for-productive-cows/)):
  Holstein 425 kg, Brown Swiss 379 kg, Jersey 297 kg. Against our mature weights that is 59.9%, 62.5% and 63.2% of mature weight.
- **Birth weight:** older compilations give Holstein about 89 lb (40 kg), Jersey about 55 lb (25 kg), Brown Swiss 80-100 lb (36-45 kg); a
  secondary source. Our 6% of mature weight (Holstein 43 kg, Jersey 28 kg, Brown Swiss 36 kg) is close to sourced figures:
  Holstein "43.3. kg" ([Heins et al. 2010, J. Dairy Sci.](https://experts.umn.edu/en/publications/birth-traits-of-pure-holstein-calves-versus-montbeliarde-sired-cr/)),
  Jersey "60 lbs" (27 kg) ([UMaine Extension Bulletin 1078](https://extension.umaine.edu/publications/1078e/)),
  Brown Swiss "39.3 +/- 0.09 kg" in a Turkish herd ([Turkish Brown Swiss study](https://avesis.omu.edu.tr/yayin/b1428901-1d12-4f87-b20a-643a8483da2b/estimates-of-phenotypic-and-genetic-parameters-for-birth-weight-of-brown-swiss-calves-in-turkey-using-an-animal-model)).
  That is 5.8-6.5% of mature weight; Brown Swiss at 6% is about 3 kg light.
- **Daily gain targets:** 0.7-0.8 kg/day is the usual advice for calving at 24 months ([Wynnstay](https://www.wynnstay.co.uk/blog/heifers-must-be-the-correct-body-size-as-well-as-age-at-first-calving/)); Holsteins
  2.0-2.2 lb/day (about 0.9-1.0 kg) before puberty and Jerseys 1.4 lb/day (0.64 kg) (Washington State table, via search result, secondary). Gain above 2 lb/day (0.9 kg)
  before 12 months can harm udder development.
- **Breeding at 55% of mature weight** and puberty at 45% (Wisconsin Extension, Dairy Calf & Heifer Association) — we use 40% for puberty from KALRO already (cattle.md).

## Proposed model (all one curve)
Target weight by age, straight lines between points (fractions of the breed's mature weight):

| Age | Fraction |
|---|---|
| birth | 0.06 (sourced birth weights above are 5.8-6.5%) |
| 15 months (456 real days) | Holstein 0.599, Brown Swiss 0.625, Jersey 0.632 (sourced, above); other breeds use Holstein's 0.60 *(stand-in)* |
| 48 months (1,460 real days) | 1.00: the third calving (24 months plus two 12-month intervals); "3rd calving, 100% mature weight" ([USDA APHIS BAMN, heifer growth](https://www.aphis.usda.gov/sites/default/files/bamn07_heifergrowth.pdf)). The same table gives "1st calving, 82% mature weight" |

- This gives a Holstein about 0.84 kg a day to 15 months, Jersey 0.59, Brown Swiss 0.74: close to the 0.7-0.8 advice. After 15 months it slows.
- **Actual gain per real day = target slope at her age x her fed fraction** (the less fed shortfall the better she grows; an underfed heifer is
  delayed to the breeding weight, as cattle.md already says). A cow past 48 months does not grow. A bull keeps growing the same way. Castration changes nothing yet.
- Weight is updated in the once-a-second bookkeeping from the real days that passed (`Growth.gainKg(breed, ageDays, days, fed)`, plain Java).
- **Pregnancy, lactation, condition:** no weight swings in this version.

## Feeding calves *(DECISION)*
Calves eat no hay yet (`Hay.dryMatterIntakeOrStandIn`). Options:
1. **Calves grow on the target curve with no feed need** (simplest; real calves drink milk and starter, which is not built). Heifers and bulls housed in a barn are
   fed like cows at the stand-in 2.2% intake and grow by their fed fraction; animals outside the barn grow at 100% (pasture assumed) *(proposed)*.
2. Outside the barn nothing grows until they are housed (forces the player to build the barn).
3. A calf milk and starter feed system first (a bigger step, needs more research).

## Game tests (to add with the code)
`calf_grows_to_heifer`, `underfed_heifer_grows_slower`, `grown_cow_stops_growing`, `weight_survives_save`; plain-Java `GrowthTest`
(Holstein at 456 days = 425 kg, Jersey 297, Brown Swiss 379; mature at 1,460 days).

## Built
`husbandry/Growth` (plain Java, `GrowthTest`), `CattleEntity.grow` in the once-a-second bookkeeping. A calf needs no feed; housed older animals grow by
their last fed fraction; animals outside the barn grow at 100%. Gain over a step is the curve's gain across it, so long gaps are right. Saved as
GrowthMarked/GrowthTick; an older saved cow starts counting from the load. Game tests: `calf_grows_to_heifer`, `underfed_heifer_grows_slower`,
`grown_cow_stops_growing`, `weight_survives_save`. Still unsourced: birth 6%, maturity at 48 months, Holstein's 0.60 for other breeds.

## Decisions
All three settled 2026-11-08: curve points as proposed; option 1; slower growth is the only effect.

### Sourcing pass, 2026-11-12
Resolved 3 marks: birth weight (6% fits Holstein and Jersey; Brown Swiss is about 3 kg light) and maturity at 48 months (third calving).
Note: the straight line gives about 71% of mature weight at 24 months, below the source's 82% at first calving. Pasture growth is a game rule.

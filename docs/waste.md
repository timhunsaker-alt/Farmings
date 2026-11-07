# Waste, biogas and fertilizer

**Status: first draft, 2026-11-07. Nothing here is built.** Same marks as `docs/animals/cattle.md`: **TO VERIFY** is a figure
without a good primary source, **DECISION** is open.

## Idea (Tim, 2026-11-07)
Animals make waste, and the player has to collect it. Collected manure can go to a **methane digester**, to **fertilizer**
production, or **both one after the other**: digest first, then turn what is left into fertilizer.

```
stall / alley -> scraper or gutter -> collection pit -> either:
    A. composting  -> compost / fertilizer
    B. digester   -> biogas -> Wildcatter genset -> grid
                  -> digestate -> separator -> solids -> compost / fertilizer
                                            -> liquid  -> fertilizer (liquid)
```

Route B then A is the "both, one after the other" path: the energy comes out first, and the digested solids and liquid still
become fertilizer. This follows how real farms use a digester and a solids separator.

## Collection (Phase 1: part of the barn)
- Each animal adds manure to its stall or alley over time. Real amount: a lactating Holstein of 567 kg eating about 20 kg of dry
  matter a day made about **44.6 kg of raw manure per day** in a Florida study ([Florida lactating cow manure study](https://agris.fao.org/search/en/records/65dedb077c7033e84bec0a68)).
  The MWPS-18 planning table gives roughly **111 lb (about 50 kg) per day** for a 1,000 lb lactating cow
  ([MWPS-18 manure characteristics](https://dcr.am.virginia.gov/soil-and-water/document/manure-characteristics-mwps-18-1.pdf)).
  Its own note says real values vary by 30% or more. Milk yield predicts manure output better than body weight does for lactating
  cows, and dry cows, heifers and bulls need their own values ([Nennich et al., ASAE 2003](https://core.ac.uk/download/188063935.pdf)).
  Game design values should come from the ASAE D384 table. **TO VERIFY.**
- Collection is by hand or by machine, not magic. A scraper (a real barn alley scraper) pushes it to a pit. Both are in Phase 1:
  hand-shoveling and a powered scraper on Wildcatter power (decided with Tim, 2026-11-07).
- **If it is not collected**, the stall gets dirty. Dirty stalls lower the cow's health and milk quality, as on a real farm.
  She never dies from it. The size of the effect is **TO VERIFY** (needs a source on hygiene and mastitis).

## Route A: composting
- Manure solids can be composted without extra material when they come from a mechanical separator; presses and centrifuges
  give drier solids than screens ([Oregon State](https://extension.oregonstate.edu/animals-livestock/beef/disposal-animal-mortality-byproducts)).
- Targets: carbon-to-nitrogen ratio 25:1 to 30:1; moisture 50-60% (40-65% acceptable); about **eight weeks** in a windrow turned at
  least five times; turn when the pile drops below 110 F. Raw dairy manure is about 3-4% nitrogen, so it needs a high-carbon
  bulking material (straw, in the game) to keep the nitrogen. Same source.
- Game: a windrow or a bay with a turning machine, a compost product, and a fertilizer item made from it. **DECISION:** what the
  fertilizer does in the game (faster crops, higher hay yield, anything else); no invented numbers until then.

## Route B: methane digester
- A digester is a sealed, heated, stirred tank where bacteria turn manure into **biogas** (mostly methane and carbon dioxide). What
  is left is **digestate**.
- Figures found (all **TO VERIFY**; the first search could not find the operating temperature, methane share or typical retention
  time from a good source):
  - dairy manure is about **23 m3 biogas per wet tonne**, worth about **48 kWh of electricity per tonne** ([Ontario OMAFA energy yields table](https://www.ontario.ca/files/2025-09/omafa-energy-yields-farm-based-anaerobic-digestion-system-25-015-en-2025-06-04.pdf));
  - methane yield 165-191 L per kg of volatile solids in a 21-day on-farm cycle ([Tropical Animal Science Journal 2020](https://doaj.org/article/5e6b6618f469415aa0b09149d1b5d9cd));
  - retention time 5 to 28 days, depending on the system ([Newfoundland biodigestion fact sheet](https://www.gov.nl.ca/ffa/files/1-Biodigestion-Fact-sheet.pdf)).
- **A check on the numbers:** 50 kg of manure a day at 48 kWh/tonne is about 2.4 kWh of electricity per cow per day. That is small
  next to what a real parlor needs, so one barn's digester feeds a few lights and fans, not a grid. Do not inflate it.
- **Biogas into Wildcatter:** biogas burns in a gas engine-generator. Wildcatter already has gas gensets at 480 V three-phase. Whether a
  Wildcatter genset can burn a Farmings fuel, or Wildcatter needs a small change (as Tailings needed one for slurry on the pipe),
  is **proposed under Wildcatter's ROADMAP Ideas** (2026-11-07). No Wildcatter code changes without Tim's go-ahead.
- Digester parts as a multiblock of real parts (spec first): feed pit and pump, digester tank with heating and mixer, gas holder,
  gas conditioning, engine-generator. The spec file is `docs/multiblocks/digester.md` and does not exist yet.

## Separator
A mechanical separator (a screw press or a centrifuge) splits digestate into solids and liquid. Solids go on to composting
(route A); liquid is a liquid fertilizer. Digestate's own nitrogen content is **TO VERIFY**: the search found nothing specific.

## Time and quantity (decided with Tim, 2026-11-07)
- **Output per game day is one real day's production.** A cow makes about 45-50 kg of manure and one real day's milk per game day.
  Machines are sized to real flows.
- **Machine process times are scaled 15:1**, like the animals (`docs/animals/cattle.md`): a real 8-week compost takes about
  3.7 game days; a real 21-day digester cycle takes 1.4 game days. The ratio is the same config value.
- **Powered scraper is in Phase 1** with the barn, on Wildcatter power (spec first, with its real counterpart and voltage).
- **Fertilizer works like bone meal:** using it on a crop advances its growth, as bone meal does in vanilla Minecraft. Amount per use
  and what it works on (hay crops, wheat, grass) are **DECISIONS**; no invented numbers.
- **Biogas fuel:** proposed in Wildcatter's ROADMAP Ideas (a genset that can burn biogas). No Wildcatter code change.

## Phasing
- **Phase 1:** collection in the barn (manure item, dirty-stall rule, hand collection and a powered scraper), because it is part of the barn. No
  digester or composter yet.
- **Phase 3:** digester, separator, composting and fertilizer, alongside the field and crop processing, since fertilizer feeds back
  into the field.

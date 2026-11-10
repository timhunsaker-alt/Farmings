# Wild animals and the taming corral

**Status: spec 2026-11-10.** Tim: "Animals should still spawn on their own and you have to lure them into a taming corral to make
them part of your herd. This goes for all animals."

Tim's choices:
- **Vanilla:** replace vanilla cows and sheep.
- **Lure:** hold feed and they follow, plus a bait block in the corral that draws nearby wild animals in through one-way gates.
- **Corral:** a multiblock where the animal takes days to settle.
- **Breeds:** matched to biome.

Values without a source are marked *(proposed)*.

This applies to every Farmings species: cattle and sheep now, chickens and later ones as they are built.

## Wild and herd animals
| | Wild | Herd |
|---|---|---|
| Comes from | a natural spawn (world generation or the vanilla spawn cycle) | a corral, `/summon`, a spawn egg, or birth to a herd mother |
| Owner | none | the player who tamed it (corral owner) or bred it |
| Barn, sheep shed, parlor, shearing stand, straws | **refuse it** | use it as before |
| Follows a player holding its feed | yes | yes (vanilla tempt) |
| Moves away from a player who holds no feed | within **6 blocks** *(proposed)* | no |
| Breeds | no *(proposed: wild numbers stay what the spawn cycle makes)* | yes |
| Readout | `WILD`, plus days settled in a corral | as before |

`/summon` and spawn eggs give herd animals *(proposed)*, so creative building and the game tests keep working. A calf or lamb takes
its mother's state.

## Spawns
The vanilla `minecraft:cow` and `minecraft:sheep` spawn entries are **removed** from every biome and replaced by `farmings:cattle`
and `farmings:sheep`. The weights and group sizes are vanilla's (26.3 biome data):
- cattle: weight 8, groups of 4;
- sheep: weight 12, groups of 4;
- meadow and cherry grove: sheep only, weight 2, groups of 2-4.

Vanilla cows and sheep already in a world are left alone. Spawn placement is the vanilla animal rule: on a grass block, in light 9 or
more.

### Breed by biome
The breed is chosen from the biome where the animal spawns, matched to the climate the breed was developed in. One group shares one
breed; a group has one bull or ram, and the rest are females *(proposed)*.

**Breed origins:**

| Breed | Developed in | Source |
|---|---|---|
| Holstein | Netherlands lowlands (North Holland, Friesland) | [Wikipedia, Holstein Friesian](https://en.wikipedia.org/wiki/Holstein_Friesian_cattle) |
| Jersey | Island of Jersey (Channel Islands) | [Wikipedia, Jersey cattle](https://en.wikipedia.org/wiki/Jersey_cattle) |
| Brown Swiss | Swiss Alps | [MSU Extension](https://www.canr.msu.edu/news/history_of_dairy_cow_breeds_brown_swiss); [Oklahoma State](https://breeds.okstate.edu/cattle/brown-swiss-cattle) |
| Gyr | Gujarat, India, a hot climate | [Animal Genetic Resources](https://www.cambridge.org/core/journals/animal-genetic-resources-resources-genetiques-animales-recursos-geneticos-animales/article/gir-cattle-breed-of-india-characteristics-and-present-status/A689843E8FCF994B31C3AF552CA0F356) (cited in cattle.md) |
| Angus | Scotland (Aberdeenshire, Angus) | [Wikipedia, Aberdeen Angus](https://en.wikipedia.org/wiki/Aberdeen_Angus) |
| Wagyu | Japan | [Wikipedia, Wagyu](https://en.wikipedia.org/wiki/Wagyu) |
| Merino | Spain; today mainly Australia's dry rangelands | [Wikipedia, Merino](https://en.wikipedia.org/wiki/Merino) |
| Suffolk | England (Suffolk) | [Wikipedia, Suffolk sheep](https://en.wikipedia.org/wiki/Suffolk_sheep) |
| Dorper | South Africa, for arid and semi-arid rangeland | [Wikipedia, Dorper](https://en.wikipedia.org/wiki/Dorper); [FAO](https://fao.org/docrep/pdf/009/t0376e/t0376e16.pdf) |
| East Friesian | East Friesland, the North Sea coast of Germany | [Wikipedia, East Friesian sheep](https://en.wikipedia.org/wiki/East_Friesian_sheep) |

**Biome groups and the breeds that spawn there** *(proposed: the mapping is a game choice; the climates come from the table above)*:

| Biome group | Vanilla biomes | Cattle | Sheep |
|---|---|---|---|
| Hot and dry | savanna, savanna_plateau, windswept_savanna, badlands, eroded_badlands, wooded_badlands | Gyr | Dorper, Merino |
| Hot and wet | jungle, sparse_jungle, bamboo_jungle | Gyr | Dorper |
| Mountain | windswept_hills, windswept_gravelly_hills, windswept_forest, meadow | Brown Swiss | Merino |
| Cold | taiga, snowy_taiga, old_growth_pine_taiga, old_growth_spruce_taiga | Angus, Brown Swiss | Suffolk |
| Temperate | plains, sunflower_plains, forest, flower_forest, birch_forest, old_growth_birch_forest, dark_forest, dappled_forest | Holstein, Jersey, Angus | Suffolk, East Friesian |
| Wet lowland | swamp | Holstein | East Friesian |
| Cherry grove | cherry_grove | Wagyu (cattle added, weight 2, groups of 2-4, as the sheep there) | Merino |

Meadow has sheep only in vanilla. Farmings keeps that. Within a group, each listed breed is equally likely *(proposed)*.

Wild animals are founders: their genes come from `Founders`, the same as a summoned animal. They are adults at first-breeding age.

## Luring
1. **Hand:** a player holding the species' feed is followed by wild animals within **10 blocks** (vanilla tempt range for cows).
   - For cattle and sheep, the feed is wheat or a hay block.
   - A lead also works.
2. **Bait trough:** a filled bait trough inside a formed corral draws the wild animals of every species it can feed within
   **32 blocks** *(proposed)*.
   - They walk to the corral's gate, through it, and on to the trough.
   - The corral fills on its own over time.

## The taming corral
### Parts
| Part | Block | What it does |
|---|---|---|
| Corral post | `corral_post` | The controller. Stands on the corral floor. Right-click reads it; the first player to read it owns the corral (as the barn). |
| Corral gate | `corral_gate` | A one-way gate set in the fence line. Animals pass from outside to inside; from inside it is a closed fence. A player can open it by hand (right-click), and it closes again when right-clicked. |
| Bait trough | `bait_trough` | Inside the corral. Takes hay blocks (up to 4) by hand or hopper. Its hay is both the lure and the corral's feed. |
| Water trough | the barn's `water_trough` | Inside the corral, filled by bucket or pipe as in the barn. |

### Forming (`husbandry/corral/CorralLayout`)
From the post, the corral floor is every walkable cell reached side to side at the post's height (±1 step). It stops at fences,
walls and other blocks an animal can't cross. A corral gate also stops the flood, and counts as part of the edge.
- At most **1,024 cells** *(proposed)*. A bigger flood means the corral isn't closed, and it doesn't form.
- At least one corral gate in its edge.
- At least one bait trough and one water trough inside.

Fault messages name the first thing missing: `NOT CLOSED IN`, `NO GATE`, `NO BAIT TROUGH`, `NO WATER`.

### Settling
- **Daily feed and water:** each game day, every wild animal inside a formed corral eats its daily intake from the bait trough's hay
  (85 kg dry matter per hay block, as the barn) and drinks from the trough.
  - Cattle intake comes from the cattle rules; sheep intake from `Flock.intakeKgDm` and 2.6 L of water per kg of dry matter.
- **Clock:** an animal that was fully fed and watered for the day gains a settled day.
- **Taming:** after **7 real days** settled it is tamed. The `Wild` flag is cleared and the corral owner becomes its owner. From then
  on a barn or sheep shed takes it in as usual.
  - Source: yard weaning, which settles cattle in yards on hay, takes 5 to 10 days ("Cattle weaned in the yards onto hay or silage
    for 5 to 10 days adapted quickly": [FutureBeef, Yard weaning and
    education](https://futurebeef.com.au/resources/yard-weaning-and-education/)). Farmings uses 7, inside that range. The same figure
    is used for sheep *(proposed: no sheep figure found)*.
- **A short day:** a day without enough feed or water doesn't count. Nothing is lost.
- **Leaving early:** a wild animal let out of the corral before it is tamed keeps its settled days.

## Not in this pass
- Wild animals breeding in the wild.
- Predators.
- Vanilla cows and sheep becoming Farmings animals.
- Chickens (they follow these rules in their own pass).

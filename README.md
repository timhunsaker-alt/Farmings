# Farmings

Farmings is a planned Minecraft **26.3** add-on (Java **25**) for [Wildcatter](https://github.com/timhunsaker-alt/Wildcatter), on
Fabric and NeoForge. It is about raising animals and growing crops the way a modern farm does: individual animals with a sex, a
breed and a genome; barns, parlors, mills and tanks on Wildcatter's grid; field work by moving contraptions and conveyors,
not tractors.

Status: **playable.** Cattle, sheep, poultry, goats, geese, guinea fowl, pigs and horses with sexes, breeds and genes; barns, a parlor and
bulk tank, a creamery, a wool mill, a hatchery, a pig barn, a stable and a race track; a field gantry, conveyors, a silo and a feed mill;
compost and a digester whose biogas runs a Wildcatter gas set; an in-game Field Manual. See [docs/ROADMAP.md](docs/ROADMAP.md) and
[PROGRESS.md](PROGRESS.md).

| Doc | What it is |
|---|---|
| [docs/VISION.md](docs/VISION.md) | What the mod is and is not |
| [docs/GENETICS.md](docs/GENETICS.md) | How the genetics work, and why there is no blockchain |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Phases |
| [PROGRESS.md](PROGRESS.md) | Short status notes |

License: [CC0](LICENSE).

## Building

Needs a Java 25 JVM for Gradle (NeoForge's build tools also fetch a JDK 21). Requires Wildcatter 0.7.0, downloaded from its GitHub
release as in Tailings.

```bash
./gradlew build
./gradlew :neoforge:runServer   # dev server
```

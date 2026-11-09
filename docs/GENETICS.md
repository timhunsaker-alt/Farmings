# Farmings — Genetics

## Source
The genetics come from FarmChain, Tim's own dairy simulation (`~/farmGame`, backend `packages/genetics`, specs
`003-deterministic-animal-genetics`, `019-breed-genome-alignment`, `021-quantitative-genetics-breeding`,
`022-visual-genome-render-recipe`, and `BREED-SPEC.md`). FarmChain is a deterministic engine: the same parents and the same
seed give the same calf, on any machine.

## The rule: no chain
FarmChain certifies some animals on Solana as NFTs. **None of that comes to Farmings.** Mojang's Minecraft Usage
Guidelines do not allow blockchain integration in the game or NFTs tied to Minecraft content, and mod hosts follow that. So:

- No wallet, mint, token, signing, chain client or chain network call, in code, dependencies or config.
- The genome is plain data stored on the animal (and in the world save). Nothing is uploaded.
- A Farmings animal cannot be minted, sold as a token or tied to a token.
- **DECISION (open, not started):** a plain-text genome code that players could copy between games. It is not built and
  needs a rules check first. Do not start it without asking Tim.

What stays from FarmChain is the *science*: quantitative traits, inheritance, breed composition and a visual recipe.

## What gets ported (plain Java, package `genetics`, no Minecraft imports)
From FarmChain, with the source value copied and cited, not invented:
- **Trait genome.** FarmChain's core eight: milk yield, butterfat, protein, feed efficiency, health, fertility, longevity and
  temperament, as in BREED-SPEC section 8.1. Farmings adds per-species traits as each species doc needs them (wool
  fineness for sheep, egg production for chickens). Each added trait is a **DECISION** until approved.
- **Breed composition** in basis points that sum to 10,000, with child = average of the parents (BREED-SPEC 7.1), and the
  label rules for purebred, influenced, cross and composite (7.5).
- **Inheritance modes** for visual loci: dominant/recessive, incomplete dominance, codominant, polygenic blend and a
  deterministic seed that picks patterns without changing the inherited phenotype (7.2-7.4).
- **Quantitative breeding** (spec 021) and the **visual recipe** (spec 022): the server resolves the genome, and the renderer
  only draws what the genome allows.
- **Determinism tests.** The same input gives the same output. These tests come across with the code.

## Status of the port (2026-11-07)
Built in `common/.../genetics` with unit tests: the eight core traits, breed composition, Mendelian transmission, heterosis,
inbreeding, trait expression, deterministic offspring and the canonical genome hash. The tests check the results against FarmChain's
own TypeScript on fixed inputs. **Not built yet:** discrete loci (CSN2/CSN3/defects; POLLED is now a coat-gene locus, below),
tier bands, the large statistical gates. The breeding seed uses a world-derived key where FarmChain uses a server secret.

## What changes for Minecraft
- **Sex.** Every animal is male or female. FarmChain's dairy MVP was females; Farmings adds males, mating and sire choice.
  The sex-linked parts of the model are a **DECISION** to settle in the cattle doc.
- **Species and breeds.** BREED-SPEC lists 30 dairy breeds. Farmings starts with a small set of cattle breeds from that list
  (the choice is a **DECISION**, in `docs/animals/cattle.md`) and adds sheep and chickens with their own real breeds.
  Breed baseline scores are FarmChain's game-balance targets (0-100), not real-world breed averages, and must be
  simulation-tested. Say that in the Field Manual.
- **Time.** FarmChain days are an abstraction. Farmings ties lifecycle (gestation, lactation, age) to Minecraft days with a
  stated ratio in each species doc.
- **Storage.** The genome lives on the animal entity and survives save and chunk unload. A game test proves it.

## License note
Farmings is CC0 (public domain). Porting the engine releases that code and its breeding math into the public domain. FarmChain
itself is unchanged and keeps its own license. Tim confirmed the repository is public on 2026-11-07.

## Open decisions
1. ~~The breed list for the cattle start.~~ Decided 2026-11-07: Holstein, Jersey, Brown Swiss, Gyr (dairy, from FarmChain) plus Angus and Wagyu (beef, no FarmChain baseline; beef traits are open, see `docs/animals/cattle.md`).
2. Sex-linked traits and what a male contributes beyond his genes.
3. Extra per-species traits.
4. Whether a plain-text genome code is ever built (see above).

## Coat genes (built 2026-11-09; Tim chose "new models + coat genes")
The visual loci come from FarmChain's visual genome (BREED-SPEC §7.2-7.4, spec 022). Only the loci the renderer can show are kept. They live
in `genetics/VisualGenome` beside the core genome, **not inside it**. The core genome's canonical string, hash and breeding seed are
unchanged, so every older breeding result holds. A calf takes one allele per locus from each parent, from a generator seeded by the same
breeding seed XORed with a fixed salt (FarmChain salts its visual seed the same way, spec 022 D3).

| Locus | Alleles | Inheritance | Source |
|---|---|---|---|
| `pigment_extension` (MC1R) | ED, EBR, E+, e | ED > EBR > E+ > e | [UC Davis VGL, MC1R](https://vgl.ucdavis.edu/test/mc1r-cattle); EBR is the Holstein Black/Red (Telstar) allele ([VGL](https://vgl.vetmed.ucdavis.edu/test/holstein-mc1r-telstar)) |
| `dilution` | none, light, medium, strong | semi-dominant (mean) | FarmChain §7.2 |
| `pattern_family` | solid, colour-sided, speckled, patch | two patterns layer (FarmChain §7.2); solid over a pattern is solid *(proposed; real piebald spotting is recessive)* | FarmChain §7.2-7.3 |
| `white_coverage` | 0-1000 | polygenic blend (mean) | FarmChain §7.2 |
| `face_white` | none, star, blaze, white face | the whiter shows *(proposed)* | FarmChain §7.3 |
| `horn_status` | polled, horned | polled is dominant | FarmChain §7.2; UC Davis VGL Polled test |
| `zebu_hump`, `dewlap_loose_skin`, `ear_length_shape` | 0-3 | incomplete dominance (mean) | FarmChain §7.2 |

The phenotype follows FarmChain's cross-rendering order (§7.4):
- **Body family** comes from the breed shares: dairy (Holstein, Jersey, Brown Swiss), beef (Angus, Wagyu) or zebu (Gyr). A second family
  is blended when it gives at least 25%.
- **Then the coat:** pigment, dilution, pattern, white, face, horns, and the hump, dewlap and ears.
- **Exact marking shapes** come from a seed taken from the genome hash. They pick among the shapes the phenotype allows, never changing it.

**Founder alleles per breed** (`genetics/FounderAlleles`):

| Breed | Founder alleles | Status |
|---|---|---|
| Holstein | ED mostly, with EBR and e present (Red and White Holsteins exist) | weights *(proposed)* |
| | patch pattern, white 450-900, face none to white face, mostly horned | FarmChain's Holstein entry (BREED-SPEC Appendix A); the 95% horned is *(proposed)* |
| Angus | black and polled | breed standard; 3% e *(proposed)* |
| Wagyu (Japanese Black) | black, horned | breed standard |
| Jersey | E+ with light to medium dilution (fawn), occasional white patches | *(unverified)*; no MC1R survey found |
| Brown Swiss | drawn as ED with medium dilution (grey-brown) | *(unverified)* |
| Gyr | red to speckled with white 300-800, horned, hump 3, dewlap 3, ears 3 | from the breed description; alleles *(proposed)* |

**Coat colours** (`VisualPhenotype.coatRgb`, *proposed tints*): black, red, or wild type (reddish brown), lightened a fifth of the way to
cream per dilution step.

**Animals with no coat genes:** an animal saved before this, or a straw filled before it, gets coat genes drawn from its own breed mix,
seeded by its genome hash, so they are always the same. New straws carry the bull's coat genes.

**Where it shows:** the readout, the herd book page, and the renderer (cattle.md, "Models and coats").

**Tests:**
- `VisualGenomeTest`:
  - Ed x e gives all black, and e x e gives red;
  - carrier x carrier gives a quarter red, and polled x horned gives all polled;
  - the dominance order, blends and layers;
  - breed founders, the F1 body blend, packing, saving and the fallback.
- Game test `coat_inherited`.

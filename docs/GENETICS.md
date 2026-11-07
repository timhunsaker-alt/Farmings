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
1. The breed list for the cattle start.
2. Sex-linked traits and what a male contributes beyond his genes.
3. Extra per-species traits.
4. Whether a plain-text genome code is ever built (see above).

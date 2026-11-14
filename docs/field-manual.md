# The Field Manual

**Status: spec, 2026-11-14 (Step 19).** Tim's popup answers (2026-11-14):
- a custom guide screen with chapters, a contents page, item icons, links between pages and a search box;
- pages say how to play plus the key numbers; sources stay in the docs;
- every player is given one on first join, and it can be crafted.

The manual has no real-world counterpart beyond a farm handbook, and it adds no mechanic, item rating or number of its own. Every
figure on a page is copied from a doc and checked against it (see "The checks").

## The item
| | |
|---|---|
| ID | `farmings:field_manual`, stack size 1 |
| Recipe | shapeless: `minecraft:book` + `minecraft:wheat`. The herd book is a *writable* book + wheat, so the two recipes don't clash. |
| Given | once, on a player's first join to a world, into the inventory (or dropped at their feet if it is full). The player gets the tag `farmings.manual_given`, which survives death and relog, so it is not given again. |
| Use | right-click in the air opens the manual screen. Nothing happens on the server. |
| Icon | a Ludo item icon, processed by `tools/process_art.py` |

## The screen
A two-page spread: two parchment panels with a spine and border, drawn as filled shapes in code, with no texture file and no vanilla
book art.

```
┌──────────────── Field Manual ────────────────┐
│ [search_____________]  │ Pigs        [icon]  │
│                        │                     │
│ Getting started        │ Large White, Duroc, │
│ Animals                │ Berkshire. Gestation│
│   Cattle               │ 114 days, ...       │
│   Sheep                │                     │
│ > Pigs                 │ See → Pig barn      │
│   Horses               │                     │
│ Buildings              │                     │
│ ...                    │  ‹   2 / 4   ›      │
│                  ‹ ›   │ [Back]              │
└──────────────────────────────────────────────┘
```

- **Left page:** the contents. Chapters fold open on a click; the page being read is marked. If the list is longer than the page it
  turns with its own ‹ ›. Above the list is a **search box**: typing filters the list to pages whose title or text contains every
  word typed, ignoring case; an empty box shows the contents again.
- **Right page:** the page being read, wrapped to the page width. A page longer than one sheet splits into sheets, turned with ‹ ›
  and shown as "2 / 4".
- **Links** are underlined and open the page they name; **Back** goes back through the pages followed (up to 32).
- The screen reopens on the last page read during the game session (not saved to disk).
- Esc or the inventory key closes it, as any screen.

### Chapters, in order
1. **Getting started:** what Farmings is, wild animals and the taming corral, the herd book, time scales, power from Wildcatter,
   spoilage and keeping food.
2. **Animals:** cattle, sheep, the sheep dog, goats, pigs, horses, chickens, turkeys, ducks, quail, geese and guinea fowl.
3. **Genes and breeding:** genes and traits, breeding and straws, growth.
4. **Buildings:** the corral, barn, parlour, sheep shed, shearing shed, coop and run, hatchery, pig barn, stable and race track.
5. **Fields and feed:** crops, the field gantry, the silo, conveyors, the feed mill.
6. **Food and processing:** the creamery, wool mill and clothing, the curing room, the cold room, eggs, food.
7. **Waste and power:** manure, compost, the digester and biogas.

That is about 45 pages.

## Page source
One markdown file per page: `docs/manual/<chapter>/<page>.md`. It is written as a short summary of its doc and kept in the repo, so
it is reviewed like any doc.

```
---
id: pigs
title: Pigs
chapter: animals
order: 40
icon: farmings:pork_belly
docs: docs/animals/pigs.md, docs/multiblocks/pig-barn.md
---
Large White, Duroc and Berkshire. A sow farrows **10 to 14** piglets after **114 days**. See [the pig barn](pig-barn).

{item:farmings:ham} {item:farmings:bacon} {item:farmings:lard}
```

Markup:
- **Front matter** (required): `id`, `title`, `chapter` and `order`. Optional: `icon` (an item ID), and `docs`, the docs its figures
  come from.
- **Text:** plain paragraphs, `**bold**`, `*italic*`, and lists that start with `- `.
- `[text](page-id)` is a link to another page.
- `{item:<id>}` or `{block:<id>}` draws a 16 px icon, with the item's name as a tooltip. A line holding only icons is drawn as an icon
  row.
- **Grid blocks** draw a multiblock's layers, bottom first, as the multiblock docs do:

  ````
  ```grid
  layer 1
  WWW
  WCW
  layer 2
  W.W
  key W farmings:horse_stall
  key C farmings:stable_controller
  ```
  ````

  Each character is one block, drawn as its item icon. `.` is anything, or air. A character without a `key` fails the build.

## Build
`tools/make_manual.py` turns each page into `assets/farmings/manual/en_us/<id>.json`, holding title, chapter, order, icon, and the
page as a list of blocks: paragraph, list, icon row and grid, with styled runs and links inside. `tools/make_assets.py` runs it, as it
does the lang and recipe files. The client reads these files through the resource manager when the screen opens, so a resource pack
can replace pages.

## The checks
`make_manual.py --check` runs in `./gradlew build`. It fails if:
- a link names a page that doesn't exist, or an item or block ID isn't one of Farmings' or vanilla's (from the generated lang and
  vanilla's item list);
- two pages share an ID, or a chapter isn't one of the seven;
- **a number on the page doesn't appear in one of its `docs`.** Numbers are compared as written, with thousands commas taken out,
  and grids and IDs are skipped. The manual can't drift from the sourced docs this way.

A game test (`manual_pages_resolve`) checks again in the game that every item ID on every page is registered.

## Rule for later work
CLAUDE.md's definition of done already says "Field Manual page added or updated once the Field Manual exists". From now on every
new system adds or updates its page under `docs/manual/`.

## Not in this pass
- Languages other than en_us.
- Pages that read live world data (a herd list, a barn's state); the readouts and herd book do that.
- Page-turn sounds and animation.
- A manual for Wildcatter.

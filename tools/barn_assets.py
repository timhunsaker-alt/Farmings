#!/usr/bin/env python3
"""Writes the barn's placeholder assets: blockstates, block models, item definitions, loot tables, lang names and 16x16 placeholder
textures (named *_placeholder). Placeholders only; the real models and textures replace them (PROGRESS.md, Known gaps).
    python3 tools/barn_assets.py
"""
import json
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main")
ASSETS = os.path.join(ROOT, "resources", "assets", "farmings")
DATA = os.path.join(ROOT, "resources", "data", "farmings")

# id: (display name, colour, footprint (w, h, d, mainX, mainZ), [model box per layer], layers sharing the last box)
PARTS = {
    "stall_bed": ("Stall Bed", (196, 170, 110), (1, 1, 3, 0, 0), [(0, 0, 0, 16, 4, 16)]),
    "stall_divider": ("Stall Divider", (150, 158, 164), (1, 2, 3, 0, 0), [(7, 0, 0, 9, 16, 16)]),
    "feed_bunk": ("Feed Bunk", (150, 150, 146), (1, 1, 1, 0, 0), [(0, 0, 0, 16, 10, 16)]),
    "hay_feeder": ("Hay Feeder", (140, 100, 60), (1, 1, 1, 0, 0), [(0, 0, 2, 16, 14, 14)]),
    "water_trough": ("Water Trough", (110, 130, 150), (2, 1, 1, 0, 0), [(0, 0, 2, 16, 8, 14)]),
    "alley_floor": ("Alley Floor", (96, 96, 92), (1, 1, 1, 0, 0), [(0, 0, 0, 16, 2, 16)]),
    "scraper_rail": ("Scraper Rail", (70, 72, 74), (1, 1, 1, 0, 0), [(0, 0, 0, 16, 2, 16)]),
    "manure_scraper": ("Manure Scraper", (212, 170, 40), (1, 1, 3, 0, 1), [(4, 0, 0, 12, 6, 16)]),
    "scraper_drive": ("Scraper Drive", (60, 120, 80), (1, 1, 1, 0, 0), [(1, 0, 1, 15, 16, 15)]),
    "collection_pit": ("Collection Pit", (64, 60, 56), (2, 2, 3, 0, 1), [(0, 0, 0, 16, 16, 16), (0, 0, 0, 16, 2, 16)]),
}
CONTROLLER = ("Barn Controller", (200, 110, 40))


def pieces(fp):
    """The layer (dy) of every piece, in Wildcatter's order: main first, then bottom layer first, each layer north to south, west to east."""
    w, h, d, mx, mz = fp
    out = [0]
    for y in range(h):
        for z in range(d):
            for x in range(w):
                if not (y == 0 and x == mx and z == mz):
                    out.append(y)
    return out


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        if isinstance(obj, str):
            f.write(obj)
        else:
            json.dump(obj, f, indent=1)
            f.write("\n")


def texture(name, colour, centre_stripe=None):
    img = Image.new("RGB", (16, 16), colour)
    d = ImageDraw.Draw(img)
    darker = tuple(max(0, c - 28) for c in colour)
    lighter = tuple(min(255, c + 24) for c in colour)
    for i in range(0, 16, 4):
        d.line([(i, 0), (i, 15)], fill=darker)
    d.rectangle([0, 0, 15, 15], outline=lighter)
    if centre_stripe:
        d.rectangle([0, 7, 15, 8], fill=centre_stripe)
    path = os.path.join(ASSETS, "textures", "block", name + "_placeholder.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def block_model(name, box):
    x1, y1, z1, x2, y2, z2 = box
    return {
        "textures": {"all": "farmings:block/%s_placeholder" % name, "particle": "farmings:block/%s_placeholder" % name},
        "elements": [{"from": [x1, y1, z1], "to": [x2, y2, z2],
                      "faces": {f: {"texture": "#all", "cullface": f} if False else {"texture": "#all"} for f in ("north", "south", "east", "west", "up", "down")}}],
    }


def main():
    lang = {"itemGroup.farmings": "Farmings", "entity.farmings.cattle": "Cattle", "block.farmings.barn_controller": CONTROLLER[0]}
    for name, (title, colour, fp, boxes) in PARTS.items():
        lang["block.farmings." + name] = title
        texture(name, colour, (255, 214, 64) if name == "scraper_rail" else None)
        layers = pieces(fp)
        for layer, box in enumerate(boxes):
            write(os.path.join(ASSETS, "models", "block", "%s_%d.json" % (name, layer)), block_model(name, box))
        variants = {}
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            for piece, layer in enumerate(layers):
                variant = {"model": "farmings:block/%s_%d" % (name, min(layer, len(boxes) - 1))}
                if y:
                    variant["y"] = y
                variants["facing=%s,piece=%d" % (facing, piece)] = variant
            if len(layers) == 1:   # a one-cell part's piece property still has two values
                variants["facing=%s,piece=1" % facing] = dict(variants["facing=%s,piece=0" % facing])
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": variants})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/%s_0" % name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
    # controller
    texture("barn_controller", CONTROLLER[1], (40, 40, 40))
    write(os.path.join(ASSETS, "models", "block", "barn_controller.json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/barn_controller_placeholder"}})
    write(os.path.join(ASSETS, "blockstates", "barn_controller.json"), {"variants": {"": {"model": "farmings:block/barn_controller"}}})
    write(os.path.join(ASSETS, "items", "barn_controller.json"), {"model": {"type": "minecraft:model", "model": "farmings:block/barn_controller"}})
    write(os.path.join(DATA, "loot_table", "blocks", "barn_controller.json"), loot("barn_controller"))
    write(os.path.join(ASSETS, "lang", "en_us.json"), {k: lang[k] for k in sorted(lang)})
    print("wrote", len(PARTS) + 1, "blocks")


def loot(name):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": "farmings:" + name}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": "farmings:blocks/" + name}


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Writes the barn's placeholder assets: blockstates, block models, item definitions, loot tables, lang names and 16x16 placeholder
textures (named *_placeholder). Placeholders only; the real models and textures replace them (PROGRESS.md, Known gaps).
    python3 tools/make_assets.py
"""
import json
import os
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "models"))
import parts as real_models   # parts with a real model (tools/models/build.py writes their blockstate, models and item)
MODELED = set(real_models.PARTS) | set(real_models.CONTROLLERS)

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
    # the milking parlor (docs/multiblocks/parlor.md)
    "parlor_stall": ("Parlor Stall", (150, 150, 156), (1, 1, 2, 0, 0), [(0, 0, 0, 16, 4, 16)]),
    "milking_unit": ("Milking Unit", (180, 180, 186), (1, 1, 1, 0, 0), [(4, 0, 4, 12, 12, 12)]),
    "vacuum_pump": ("Vacuum Pump", (70, 100, 150), (1, 1, 1, 0, 0), [(1, 0, 1, 15, 14, 15)]),
    "milk_line": ("Milk Line", (200, 200, 205), (1, 1, 1, 0, 0), [(0, 0, 5, 16, 5, 11)]),
    "bulk_tank": ("Bulk Tank", (205, 208, 214), (2, 2, 2, 0, 0), [(0, 0, 0, 16, 16, 16)]),
    "cow_lane": ("Cow Lane", (120, 120, 116), (1, 1, 1, 0, 0), [(0, 0, 0, 16, 2, 16)]),
    "holding_pen": ("Holding Pen", (110, 112, 108), (1, 1, 1, 0, 0), [(0, 0, 0, 16, 2, 16)]),
    # the creamery (docs/multiblocks/creamery.md)
    "receiving_tank": ("Receiving Tank", (200, 204, 210), (2, 2, 2, 0, 0), [(0, 0, 0, 16, 16, 16)]),
    "batch_pasteurizer": ("Batch Pasteurizer", (190, 196, 204), (1, 2, 1, 0, 0), [(1, 0, 1, 15, 16, 15)]),
    "cream_separator": ("Cream Separator", (176, 180, 190), (1, 1, 1, 0, 0), [(3, 0, 3, 13, 15, 13)]),
    "butter_churn": ("Butter Churn", (150, 120, 80), (1, 1, 1, 0, 0), [(2, 0, 2, 14, 14, 14)]),
    "cheese_vat": ("Cheese Vat", (196, 200, 206), (2, 1, 1, 0, 0), [(0, 0, 1, 16, 12, 15)]),
    "cheese_press": ("Cheese Press", (120, 100, 70), (1, 1, 1, 0, 0), [(2, 0, 2, 14, 16, 14)]),
    "aging_shelf": ("Aging Shelf", (140, 110, 70), (1, 2, 1, 0, 0), [(0, 0, 2, 16, 16, 14)]),
    "bottler": ("Bottler", (170, 190, 200), (1, 1, 1, 0, 0), [(2, 0, 2, 14, 14, 14)]),
}
# items that are not blocks: (id, name, placeholder colour, kind: "blob", "fork", "straw", "steak")
ITEMS = [
    ("manure", "Manure", (92, 64, 38), "blob"),
    ("manure_fork", "Manure Fork", (138, 120, 96), "fork"),
    ("empty_straw", "Empty Straw", (170, 170, 176), "straw"),
    ("semen_straw", "Semen Straw", (80, 130, 200), "straw"),
    ("select_steak", "Select Steak", (190, 60, 60), "steak"),
    ("choice_steak", "Choice Steak", (196, 70, 70), "steak"),
    ("prime_steak", "Prime Steak", (204, 84, 84), "steak"),
    ("cooked_select_steak", "Cooked Select Steak", (120, 70, 40), "steak"),
    ("cooked_choice_steak", "Cooked Choice Steak", (128, 76, 44), "steak"),
    ("cooked_prime_steak", "Cooked Prime Steak", (136, 82, 48), "steak"),
    ("herd_book", "Herd Book", (60, 110, 60), "book"),
    ("milk_helmet", "Milk Helmet", (246, 246, 240), "armor"),
    ("milk_chestplate", "Milk Chestplate", (246, 246, 240), "armor"),
    ("milk_leggings", "Milk Leggings", (246, 246, 240), "armor"),
    ("milk_boots", "Milk Boots", (246, 246, 240), "armor"),
    ("bottled_milk", "Bottled Milk", (248, 248, 244), "bottle"),
    ("cream_bottle", "Cream", (250, 244, 220), "bottle"),
    ("skim_milk_bottle", "Skim Milk", (232, 238, 246), "bottle"),
    ("buttermilk_bottle", "Buttermilk", (244, 240, 214), "bottle"),
    ("whey_bottle", "Whey", (226, 228, 170), "bottle"),
    ("spoiled_bottle", "Spoiled Bottle", (196, 200, 120), "bottle"),
    ("rancid_butter", "Rancid Butter", (200, 180, 90), "blob"),
    ("moldy_cheese", "Moldy Cheese", (170, 160, 90), "blob"),
    ("spoiled_steak", "Spoiled Steak", (110, 90, 70), "steak"),
    ("butter", "Butter", (246, 220, 110), "blob"),
    ("cheddar_wheel", "Cheddar Wheel", (232, 168, 60), "blob"),
    ("cheddar", "Cheddar", (240, 180, 70), "blob"),
]
SPOILED_COLOUR = (196, 200, 150)
EXTRA_LANG = {
    "message.farmings.straw.not_a_bull": "Only a grown bull can fill a straw.",
    "message.farmings.straw.not_in_heat": "She is not in heat, or the straw is empty.",
    "message.farmings.castrated": "Castrated: he will grow as a steer.",
    "message.farmings.herd_book.recorded": "Recorded %s in the herd book.",
    "message.farmings.herd_book.full": "The herd book is full (64 animals).",
}
CONTROLLER = ("Barn Controller", (200, 110, 40))
PARLOR_CONTROLLER = ("Parlor Controller", (60, 110, 190))
CREAMERY_CONTROLLER = ("Creamery Controller", (230, 230, 220))


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


def item_texture(name, colour, fork):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if fork == "armor":
        piece = name.split("_")[1]
        shapes = {"helmet": [(3, 4, 12, 9)], "chestplate": [(2, 3, 13, 13)], "leggings": [(4, 3, 7, 14), (8, 3, 11, 14)], "boots": [(2, 9, 6, 14), (9, 9, 13, 14)]}
        for box in shapes[piece]:
            d.rectangle(box, fill=colour, outline=tuple(max(0, c - 60) for c in colour))
    elif fork == "bottle":
        d.rectangle([6, 1, 9, 3], fill=(150, 110, 70))                     # cork
        d.rectangle([5, 4, 10, 14], fill=colour, outline=(170, 190, 200))  # glass with the product
    elif fork == "book":
        d.rectangle([3, 2, 12, 14], fill=colour, outline=(40, 60, 40))
        d.rectangle([5, 4, 10, 6], fill=(230, 220, 190))
    elif fork == "steak":
        d.ellipse([2, 4, 14, 12], fill=colour)
        d.arc([2, 4, 14, 12], 0, 360, fill=(240, 230, 220))                 # fat rim
        marbling = {"select": 2, "choice": 5, "prime": 9}.get(name.replace("cooked_", "").split("_")[0], 0)
        for i in range(marbling):
            d.point((4 + (i * 5) % 9, 6 + (i * 3) % 5), fill=(245, 235, 230))
    elif fork == "straw":
        d.line([(3, 13), (13, 3)], fill=(232, 224, 200), width=3)         # a thin tube
        d.line([(3, 13), (6, 10)], fill=colour, width=3)                  # the plug end
    elif fork:
        d.line([(3, 13), (11, 5)], fill=(120, 84, 48), width=2)          # the handle
        for x in (9, 11, 13):                                              # three tines
            d.line([(x, 1), (x, 6)], fill=(150, 150, 156))
        d.line([(9, 6), (13, 6)], fill=(150, 150, 156))
    else:
        d.ellipse([3, 6, 13, 13], fill=colour)
        d.ellipse([5, 4, 11, 9], fill=tuple(min(255, c + 18) for c in colour))
        d.ellipse([6, 8, 9, 10], fill=tuple(max(0, c - 22) for c in colour))
    path = os.path.join(ASSETS, "textures", "item", name + "_placeholder.png")
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
    lang.update(EXTRA_LANG)
    for name, (title, colour, fp, boxes) in PARTS.items():
        lang["block.farmings." + name] = title
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
        if name in MODELED:
            continue
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
    # controller
    if "barn_controller" not in MODELED:
        texture("barn_controller", CONTROLLER[1], (40, 40, 40))
    if "barn_controller" not in MODELED:
        write(os.path.join(ASSETS, "models", "block", "barn_controller.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/barn_controller_placeholder"}})
    if "barn_controller" not in MODELED:
        write(os.path.join(ASSETS, "blockstates", "barn_controller.json"), {"variants": {"": {"model": "farmings:block/barn_controller"}}})
    if "barn_controller" not in MODELED:
        write(os.path.join(ASSETS, "items", "barn_controller.json"), {"model": {"type": "minecraft:model", "model": "farmings:block/barn_controller"}})
    write(os.path.join(DATA, "loot_table", "blocks", "barn_controller.json"), loot("barn_controller"))
    if "parlor_controller" not in MODELED:
        texture("parlor_controller", PARLOR_CONTROLLER[1], (40, 40, 40))
    lang["block.farmings.parlor_controller"] = PARLOR_CONTROLLER[0]
    if "parlor_controller" not in MODELED:
        write(os.path.join(ASSETS, "models", "block", "parlor_controller.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/parlor_controller_placeholder"}})
    if "parlor_controller" not in MODELED:
        write(os.path.join(ASSETS, "blockstates", "parlor_controller.json"), {"variants": {"": {"model": "farmings:block/parlor_controller"}}})
    if "parlor_controller" not in MODELED:
        write(os.path.join(ASSETS, "items", "parlor_controller.json"), {"model": {"type": "minecraft:model", "model": "farmings:block/parlor_controller"}})
    write(os.path.join(DATA, "loot_table", "blocks", "parlor_controller.json"), loot("parlor_controller"))
    if "creamery_controller" not in MODELED:
        texture("creamery_controller", CREAMERY_CONTROLLER[1], (40, 40, 40))
    lang["block.farmings.creamery_controller"] = CREAMERY_CONTROLLER[0]
    if "creamery_controller" not in MODELED:
        write(os.path.join(ASSETS, "models", "block", "creamery_controller.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/creamery_controller_placeholder"}})
    if "creamery_controller" not in MODELED:
        write(os.path.join(ASSETS, "blockstates", "creamery_controller.json"), {"variants": {"": {"model": "farmings:block/creamery_controller"}}})
    if "creamery_controller" not in MODELED:
        write(os.path.join(ASSETS, "items", "creamery_controller.json"), {"model": {"type": "minecraft:model", "model": "farmings:block/creamery_controller"}})
    write(os.path.join(DATA, "loot_table", "blocks", "creamery_controller.json"), loot("creamery_controller"))
    # items: manure and the fork (placeholders)
    for name, title, colour, kind in ITEMS:
        parent = "minecraft:item/handheld" if kind == "fork" else "minecraft:item/generated"
        lang["item.farmings." + name] = title
        real = os.path.exists(os.path.join(ASSETS, "textures", "item", name + ".png"))
        if not real:
            item_texture(name, colour, kind if kind in ("straw", "steak", "armor", "book", "bottle") else kind == "fork")
        tex = "farmings:item/" + name + ("" if real else "_placeholder")
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": parent, "textures": {"layer0": tex}})
        if kind == "armor":
            lang["item.farmings." + name + ".spoiled"] = "Spoiled " + title
            if not real:
                item_texture(name + "_spoiled", SPOILED_COLOUR, "armor")
            write(os.path.join(ASSETS, "models", "item", name + "_spoiled.json"), {"parent": parent, "textures": {"layer0": tex.replace(name, name + "_spoiled")}})
            write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:condition", "property": "minecraft:custom_model_data", "index": 0,
                  "on_true": {"type": "minecraft:model", "model": "farmings:item/%s_spoiled" % name},
                  "on_false": {"type": "minecraft:model", "model": "farmings:item/" + name}}})
        else:
            write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:item/" + name}})
    # the worn milk armor: equipment assets and their layer textures (placeholders)
    for asset, colour in (("milk", (246, 246, 240)), ("milk_spoiled", SPOILED_COLOUR)):
        write(os.path.join(ASSETS, "equipment", asset + ".json"),
              {"layers": {"humanoid": [{"texture": "farmings:" + asset}], "humanoid_leggings": [{"texture": "farmings:" + asset}]}})
        for layer in ("humanoid", "humanoid_leggings"):
            if os.path.exists(os.path.join(ASSETS, "textures", "entity", "equipment", layer, asset + ".png")):
                continue
            img = Image.new("RGBA", (64, 32), colour + (255,))
            ImageDraw.Draw(img).rectangle([0, 0, 63, 31], outline=tuple(max(0, c - 50) for c in colour))
            path = os.path.join(ASSETS, "textures", "entity", "equipment", layer, asset + ".png")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            img.save(path)
    write(os.path.join(DATA, "tags", "item", "repairs_milk_armor.json"), {"values": ["minecraft:milk_bucket"]})
    write(os.path.join(ASSETS, "lang", "en_us.json"), {k: lang[k] for k in sorted(lang)})
    recipes()
    print("wrote", len(PARTS) + 1, "blocks")


# Crafting recipes (docs/recipes.md). S Wildcatter steel ingot, I iron ingot, C copper ingot, R redstone, G glass, B bucket,
# K gray concrete, W planks, N iron bars, L wool, M milk bucket.
KEYS = {"S": "wildcatter:steel_ingot", "I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone",
        "G": "minecraft:glass", "B": "minecraft:bucket", "K": "minecraft:gray_concrete", "W": "#minecraft:planks",
        "N": "minecraft:iron_bars", "L": "#minecraft:wool", "M": "minecraft:milk_bucket", "T": "minecraft:stick"}
SHAPED = {
    # barn
    "barn_controller": (["SRS", "CIC", "SSS"], 1),
    "stall_bed": (["LLL", "KKK"], 1),
    "stall_divider": (["SSS", "S S", "S S"], 1),
    "feed_bunk": (["K K", "KKK"], 1),
    "hay_feeder": (["NNN", "N N", "SSS"], 1),
    "water_trough": (["SBS", "SSS"], 1),
    "alley_floor": (["KK", "KK"], 4),
    "scraper_rail": (["S S", "SRS", "S S"], 8),
    "manure_scraper": (["SSS", "III"], 1),
    "scraper_drive": (["SCS", "CRC", "SIS"], 1),
    "collection_pit": (["K K", "K K", "KKK"], 1),
    # parlor
    "parlor_controller": (["SRS", "CGC", "SSS"], 1),
    "parlor_stall": (["S S", "S S", "KKK"], 1),
    "milking_unit": (["IGI", "SBS"], 1),
    "vacuum_pump": (["SCS", "CRC", "SSS"], 1),
    "milk_line": (["SGS"], 6),
    "bulk_tank": (["SSS", "SBS", "SCS"], 1),
    "cow_lane": (["N N", "KKK"], 6),
    "holding_pen": (["NNN", "KKK"], 6),
    # creamery
    "creamery_controller": (["SRS", "GCG", "SSS"], 1),
    "receiving_tank": (["SSS", "SBS", "SSS"], 1),
    "batch_pasteurizer": (["SBS", "SRS", "SCS"], 1),
    "cream_separator": (["SGS", "CRC", "SIS"], 1),
    "butter_churn": (["WSW", "WCW", "WRW"], 1),
    "cheese_vat": (["S S", "SBS", "SCS"], 1),
    "cheese_press": (["WIW", "W W", "WWW"], 1),
    "aging_shelf": (["WWW", "T T", "WWW"], 1),
    "bottler": (["IGI", "IRI", "III"], 1),
    # tools and the easter egg
    "manure_fork": (["I I", " T ", " T "], 1),
    "milk_helmet": (["MMM", "M M"], 1),
    "milk_chestplate": (["M M", "MMM", "MMM"], 1),
    "milk_leggings": (["MMM", "M M", "M M"], 1),
    "milk_boots": (["M M", "M M"], 1),
}
SHAPELESS = {
    "empty_straw": (["wildcatter:plastic_sheet"], 8),
    "herd_book": (["minecraft:writable_book", "minecraft:wheat"], 1),
    "cheddar": (["farmings:cheddar_wheel"], 8),
}
COOKING = {"select_steak": "cooked_select_steak", "choice_steak": "cooked_choice_steak", "prime_steak": "cooked_prime_steak"}


def ingredient(ref):
    return ref   # 26.x recipes take an item id or "#tag" string


def recipes():
    out = os.path.join(DATA, "recipe")
    for name, (pattern, count) in SHAPED.items():
        used = sorted({c for row in pattern for c in row if c != " "})
        write(os.path.join(out, name + ".json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
              "key": {c: ingredient(KEYS[c]) for c in used}, "result": {"id": "farmings:" + name, "count": count}})
    for name, (items, count) in SHAPELESS.items():
        write(os.path.join(out, name + ".json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
              "ingredients": [ingredient(i) for i in items], "result": {"id": "farmings:" + name, "count": count}})
    for raw, cooked in COOKING.items():
        for kind, time in (("smelting", 200), ("smoking", 100), ("campfire_cooking", 600)):
            write(os.path.join(out, "%s_from_%s.json" % (cooked, kind)), {"type": "minecraft:" + kind, "category": "food",
                  "ingredient": "farmings:" + raw, "result": {"id": "farmings:" + cooked}, "experience": 0.35, "cookingtime": time})
    # no two shaped recipes may share a pattern and key
    seen = {}
    for name, (pattern, _) in SHAPED.items():
        sig = json.dumps([[KEYS.get(c, " ") for c in row] for row in pattern])
        assert sig not in seen, "%s and %s have the same recipe" % (name, seen[sig])
        seen[sig] = name


def loot(name):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": "farmings:" + name}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": "farmings:blocks/" + name}


if __name__ == "__main__":
    main()

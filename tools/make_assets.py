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
ROOT_DATA = os.path.join(ROOT, "resources", "data")

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
    "sheep_stall": ("Sheep Stall", (160, 160, 166), (1, 1, 1, 0, 0), [(0, 0, 0, 16, 3, 16)]),
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
    ("sheep_milk_bottle", "Sheep Milk", (250, 248, 236), "bottle"),
    ("sheep_cheese_wheel", "Sheep Cheese Wheel", (236, 218, 160), "blob"),
    ("sheep_cheese", "Sheep Cheese", (240, 224, 170), "blob"),
    # sheep (docs/animals/sheep.md)
    ("fleece", "Fleece", (236, 228, 206), "blob"),
    ("loose_wool", "Loose Wool", (214, 206, 186), "blob"),
    ("skirtings", "Skirtings", (200, 190, 160), "blob"),
    ("wool_bale", "Wool Bale", (226, 218, 196), "blob"),
    # the wool mill and wool uses (docs/multiblocks/wool-mill.md, docs/wool.md)
    ("clean_wool", "Clean Wool", (244, 240, 230), "blob"),
    ("sliver", "Sliver", (240, 236, 226), "blob"),
    ("lanolin", "Lanolin", (232, 206, 120), "blob"),
    ("wool_cloth", "Wool Cloth", (226, 220, 204), "blob"),
    ("felt", "Felt", (176, 170, 158), "blob"),
    ("udder_balm", "Udder Balm", (238, 222, 160), "blob"),
    ("wool_cap", "Wool Cap", (226, 218, 196), "clothing"),
    ("wool_sweater", "Wool Sweater", (226, 218, 196), "clothing"),
    ("wool_trousers", "Wool Trousers", (206, 198, 176), "clothing"),
    ("wool_socks", "Wool Socks", (226, 218, 196), "clothing"),
    ("yarn_white", "White Yarn", (234, 236, 236), "yarn"),
    ("yarn_orange", "Orange Yarn", (240, 118, 19), "yarn"),
    ("yarn_magenta", "Magenta Yarn", (189, 68, 179), "yarn"),
    ("yarn_light_blue", "Light Blue Yarn", (58, 175, 217), "yarn"),
    ("yarn_yellow", "Yellow Yarn", (248, 197, 39), "yarn"),
    ("yarn_lime", "Lime Yarn", (112, 185, 25), "yarn"),
    ("yarn_pink", "Pink Yarn", (237, 141, 172), "yarn"),
    ("yarn_gray", "Gray Yarn", (62, 68, 71), "yarn"),
    ("yarn_light_gray", "Light Gray Yarn", (142, 142, 134), "yarn"),
    ("yarn_cyan", "Cyan Yarn", (21, 137, 145), "yarn"),
    ("yarn_purple", "Purple Yarn", (121, 42, 172), "yarn"),
    ("yarn_blue", "Blue Yarn", (53, 57, 157), "yarn"),
    ("yarn_brown", "Brown Yarn", (114, 71, 40), "yarn"),
    ("yarn_green", "Green Yarn", (84, 109, 27), "yarn"),
    ("yarn_red", "Red Yarn", (160, 39, 34), "yarn"),
    ("yarn_black", "Black Yarn", (26, 26, 30), "yarn"),
    ("lamb_chop", "Lamb Chop", (214, 120, 120), "steak"),
    ("cooked_lamb_chop", "Cooked Lamb Chop", (150, 96, 60), "steak"),
    ("mutton_chop", "Mutton Chop", (176, 70, 66), "steak"),
    ("cooked_mutton_chop", "Cooked Mutton Chop", (124, 76, 46), "steak"),
    ("spoiled_mutton", "Spoiled Mutton", (110, 96, 72), "steak"),
    # eggs (docs/eggs.md); placeholders until the Ludo icons
    ("poultry_litter", "Poultry Litter", (120, 100, 70), "blob"),
    ("shepherds_whistle", "Shepherd's Whistle", (200, 200, 190), "blob"),
    ("chicken", "Raw Chicken", (236, 170, 150), "steak"),
    ("cooked_chicken", "Cooked Chicken", (190, 130, 70), "steak"),
    ("turkey", "Raw Turkey", (226, 150, 140), "steak"),
    ("cooked_turkey", "Roast Turkey", (184, 120, 64), "steak"),
    ("duck", "Raw Duck", (214, 120, 110), "steak"),
    ("cooked_duck", "Roast Duck", (166, 96, 50), "steak"),
    ("quail", "Raw Quail", (220, 140, 130), "steak"),
    ("cooked_quail", "Roast Quail", (176, 110, 60), "steak"),
    ("spoiled_poultry", "Spoiled Poultry", (130, 120, 80), "steak"),
    ("fried_egg", "Fried Egg", (250, 240, 200), "blob"),
    ("rotten_egg", "Rotten Egg", (150, 150, 100), "blob"),
    ("down", "Down", (244, 242, 236), "blob"),
    ("down_jacket", "Down Jacket", (90, 110, 150), "clothing"),
    ("egg_carton", "Egg Carton", (200, 190, 170), "blob"),
    ("chicken_egg", "Chicken Egg", (226, 196, 150), "blob"),
    ("turkey_egg", "Turkey Egg", (214, 190, 150), "blob"),
    ("quail_egg", "Quail Egg", (170, 140, 100), "blob"),
    ("duck_egg", "Duck Egg", (236, 234, 222), "blob"),
]
SPOILED_COLOUR = (196, 200, 150)
EXTRA_LANG = {
    "message.farmings.straw.not_a_bull": "Only a grown bull can fill a straw.",
    "message.farmings.straw.not_a_sire": "Only a grown, tame bull, ram or turkey tom can fill a straw.",
    "message.farmings.straw.wrong_species": "This straw holds %s semen.",
    "message.farmings.straw.stale": "This turkey semen is stale: use it within 30 minutes of collecting.",
    "tooltip.farmings.straw": "%s semen, sire %s",
    "tooltip.farmings.straw.fresh": "Fresh: use within 30 real minutes",
    "species.farmings.cattle": "Bull",
    "species.farmings.sheep": "Ram",
    "species.farmings.turkey": "Turkey",
    "species.farmings.chicken": "Chicken",
    "species.farmings.duck": "Duck",
    "species.farmings.quail": "Quail",
    "message.farmings.straw.not_in_heat": "She is not in heat, or the straw is empty.",
    "message.farmings.castrated": "Castrated: he will grow as a steer.",
    "message.farmings.wether": "Castrated: he will grow as a wether.",
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
        piece = {"cap": "helmet", "sweater": "chestplate", "jacket": "chestplate", "trousers": "leggings", "socks": "boots"}.get(piece, piece)
        for box in shapes[piece]:
            d.rectangle(box, fill=colour, outline=tuple(max(0, c - 60) for c in colour))
    elif fork == "yarn":
        d.ellipse([3, 4, 12, 13], fill=colour, outline=tuple(max(0, c - 50) for c in colour))     # a skein
        for x in (5, 7, 9):
            d.arc([x - 2, 4, x + 3, 13], 250, 110, fill=tuple(min(255, c + 30) for c in colour))
        d.line([(12, 9), (15, 6)], fill=colour)                                                     # the loose end
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
    lang = {"itemGroup.farmings": "Farmings", "entity.farmings.cattle": "Cattle", "entity.farmings.sheep": "Sheep", "entity.farmings.chicken": "Chicken", "entity.farmings.turkey": "Turkey",
            "entity.farmings.quail": "Quail", "entity.farmings.duck": "Duck", "block.farmings.barn_controller": CONTROLLER[0]}
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
            item_texture(name, colour, "armor" if kind == "clothing" else kind if kind in ("straw", "steak", "armor", "book", "bottle", "yarn") else kind == "fork")
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
    # wool clothing: its worn look (tools/wool_textures.py draws the layers), repairs and the freeze-immune tag (docs/wool.md)
    write(os.path.join(ASSETS, "equipment", "wool.json"),
          {"layers": {"humanoid": [{"texture": "farmings:wool"}], "humanoid_leggings": [{"texture": "farmings:wool"}]}})
    write(os.path.join(DATA, "tags", "item", "repairs_wool_clothing.json"), {"values": ["farmings:wool_cloth"]})
    write(os.path.join(ROOT_DATA, "minecraft", "tags", "item", "freeze_immune_wearables.json"),
          {"replace": False, "values": ["farmings:wool_cap", "farmings:wool_sweater", "farmings:wool_trousers", "farmings:wool_socks",
                                      "farmings:down_jacket"]})
    # poultry products (docs/eggs.md): every Farmings egg counts where a recipe takes an egg
    write(os.path.join(DATA, "tags", "item", "eggs.json"), {"values": ["farmings:chicken_egg", "farmings:turkey_egg", "farmings:quail_egg",
                                                                       "farmings:duck_egg"]})
    # Farmings meat in the common meat tags, so other mods' recipes take it (docs/eggs.md, "Loose ends")
    write(os.path.join(ROOT_DATA, "c", "tags", "item", "foods", "raw_meat.json"), {"replace": False, "values": [
        "farmings:chicken", "farmings:turkey", "farmings:duck", "farmings:quail", "farmings:lamb_chop", "farmings:mutton_chop",
        "farmings:select_steak", "farmings:choice_steak", "farmings:prime_steak"]})
    write(os.path.join(ROOT_DATA, "c", "tags", "item", "foods", "cooked_meat.json"), {"replace": False, "values": [
        "farmings:cooked_chicken", "farmings:cooked_turkey", "farmings:cooked_duck", "farmings:cooked_quail", "farmings:cooked_lamb_chop",
        "farmings:cooked_mutton_chop", "farmings:cooked_select_steak", "farmings:cooked_choice_steak", "farmings:cooked_prime_steak"]})
    lang["entity.farmings.rotten_egg"] = "Rotten Egg"
    lang["entity.farmings.sheep_dog"] = "Border Collie"
    lang["entity.farmings.field_gantry"] = "Field Gantry"
    # Farmings crops keep the farmland under them, as vanilla crops do; without this, dry farmland turns back to dirt on a random tick
    # and the crop pops off (found 2026-11-11 by an intermittent gantry test)
    write(os.path.join(ROOT_DATA, "minecraft", "tags", "block", "maintains_farmland.json"), {"replace": False, "values": [
        "farmings:" + c for c in CROPS]})
    wool_mill_assets(lang)
    cold_room_assets(lang)
    shed_assets(lang)
    corral_assets(lang)
    coop_assets(lang)
    hatchery_assets(lang)
    egg_assets(lang)
    crop_assets(lang)
    conveyor_assets(lang)
    gantry_assets(lang)
    silo_assets(lang)
    feedmill_assets(lang)
    compost_assets(lang)
    digester_assets(lang)
    write(os.path.join(ASSETS, "lang", "en_us.json"), {k: lang[k] for k in sorted(lang)})
    recipes()
    print("wrote", len(PARTS) + 1, "blocks")


# Crafting recipes (docs/recipes.md). S Wildcatter steel ingot, I iron ingot, C copper ingot, R redstone, G glass, B bucket,
# K gray concrete, W planks, N iron bars, L wool, M milk bucket.
KEYS = {"S": "wildcatter:steel_ingot", "I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "R": "minecraft:redstone",
        "G": "minecraft:glass", "B": "minecraft:bucket", "K": "minecraft:gray_concrete", "W": "#minecraft:planks",
        "N": "minecraft:iron_bars", "L": "#minecraft:wool", "M": "minecraft:milk_bucket", "T": "minecraft:stick",
        "H": "minecraft:hay_block", "F": "#minecraft:wooden_fences", "X": "farmings:wool_cloth",
        "D": "minecraft:wheat_seeds", "Q": "farmings:down", "P": "farmings:felt", "E": "minecraft:leather"}
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
    "sheep_stall": (["SNS", "KKK"], 2),
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
    # the walk-in fridge (docs/multiblocks/cold-room.md): wool stands for the panels' insulation
    "cold_room_panel": (["III", "LLL", "III"], 4),
    "cold_room_door": (["II", "LL", "II"], 1),
    "evaporator": (["SNS", "CRC", "SSS"], 1),
    "condensing_unit": (["SNS", "CRC", "SIS"], 1),
    # the sheep shed (docs/multiblocks/sheep-shed.md): straw over boards, hurdles, a field gate
    "shed_controller": (["WRW", "WHW", "WWW"], 1),
    "sheep_pen": (["HHH", "WWW"], 6),
    "lambing_pen": (["F F", "HHH", "WWW"], 2),
    "pasture_gate": (["FWF", "FWF"], 1),
    # the taming corral (docs/taming.md): a log post, a fence-and-plank gate, a plank trough with hay
    "corral_post": (["T", "W", "W"], 1),
    "corral_gate": (["TFT", "TWT"], 1),
    "bait_trough": (["WHW", "WWW"], 1),
    # the coop (docs/multiblocks/coop.md): planks and seed, litter over boards, a small door, a straw-lined box, a roost bar, a tin feeder
    "coop_controller": (["WDW", "WTW", "WWW"], 1),
    "coop_floor": (["DHD", "WWW"], 6),
    "pop_hole": (["TWT", "TWT"], 1),
    "nest_box": (["W W", "WHW"], 2),
    "perch": (["TTT"], 3),
    "grain_feeder": (["I I", "IDI", "III"], 1),
    # the hatchery (docs/multiblocks/hatchery.md): a glass-fronted cabinet with a heater, a heat lamp on a bracket
    "incubator": (["IGI", "IRI", "WWW"], 1),
    "brooder": ([" N ", "IRI", " G "], 1),
    # egg grading and down (docs/eggs.md): a plank table with a candling lamp; a down jacket on a felt lining
    "egg_grader": (["WGW", "T T"], 1),
    "down_jacket": (["QPQ", "QQQ", "QQQ"], 1),
    # belts and augers (docs/conveyors.md): rubber is leather here
    "farm_belt": (["EEE", "III"], 6),
    "farm_belt_drive": (["ICI", "IRI", "III"], 1),
    "grain_auger": (["I", "T", "I"], 4),
    "grain_auger_drive": (["ICI", "RTR"], 1),
    # the field gantry (docs/multiblocks/field-gantry.md): steel, a redstone controller, tool heads of iron
    "field_rail": (["I I", "SSS", "I I"], 16),
    "gantry_truck": (["SSS", "SCS", "IRI"], 1),
    "gantry_beam": (["SSS", " S "], 4),
    "headland_stop": (["SKS"], 1),
    "gantry_cab": (["SGS", "SRS", "SCS"], 1),
    "field_junction": (["SCS", "CRC", "SCS"], 1),
    "tool_seed_drill": (["DDD", "III", "T T"], 1),
    "tool_harvester": (["NNN", "ICI", "T T"], 1),
    "tool_mower": (["III", "NCN", "T T"], 1),
    "tool_rake": (["TTT", "III", "T T"], 1),
    "tool_baler": (["III", "IRI", "T T"], 1),
    "tool_spreader": (["I I", "IBI", "T T"], 1),
    # the tower silo and grain bin (docs/multiblocks/silo.md)
    "silo_stave": (["KIK", "KIK"], 8),
    "silo_roof": (["SSS", "S S"], 1),
    "forage_blower": (["SNS", "IRI", "SCS"], 1),
    "silo_unloader": (["SCS", "IRI", "SNS"], 1),
    "grain_bin_wall": (["I I", "SIS", "I I"], 8),
    "grain_bin_roof": (["  I", " I ", "IBI"], 1),
    "grain_bin_hopper": (["I I", "IBI", " I "], 1),
    # the feed mill (docs/multiblocks/feed-mill.md)
    "hammer_mill": (["SNS", "SRS", "SCS"], 1),
    "tmr_mixer": (["S S", "SRS", "SCS"], 1),
    # compost (docs/multiblocks/compost.md)
    "compost_pad": (["KKK"], 3),
    "compost_controller": (["WRW", "WTW"], 1),
    "windrow_turner": (["SSS", "NCN", "S S"], 1),
    # the digester (docs/multiblocks/digester.md)
    "digester_tank": (["SGS", "BRB", "SCS"], 1),
    "digester_feed_pit": (["K K", "KBK", "KKK"], 1),
    "gas_holder": (["LLL", "L L", "SSS"], 1),
    "digestate_tank": (["S S", "SBS", "SSS"], 1),
    "screw_press": (["SNS", "ICI", "SRS"], 1),
    # the shearing shed (docs/multiblocks/shearing-shed.md)
    "shearing_stand": (["SRS", "CIC", "WWW"], 1),
    "catching_pen": (["F F", "WWW"], 4),
    "wool_table": (["WWW", "T T"], 1),
    "wool_press": (["III", "W W", "WWW"], 1),
    # the wool mill (docs/multiblocks/wool-mill.md)
    "scouring_bowl": (["SBS", "S S", "SCS"], 1),
    "carding_machine": (["NNN", "CRC", "SSS"], 1),
    "spinning_frame": (["TTT", "CRC", "SSS"], 1),
    "loom_frame": (["WWW", "TRT", "SSS"], 1),
    "felting_table": (["WBW", "SRS"], 1),
    # wool clothing from the mill's cloth (docs/wool.md)
    "wool_cap": (["XXX", "X X"], 1),
    "wool_sweater": (["X X", "XXX", "XXX"], 1),
    "wool_trousers": (["XXX", "X X", "X X"], 1),
    "wool_socks": (["X X", "X X"], 1),
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
    "udder_balm": (["farmings:lanolin", "farmings:lanolin"], 1),
    "mineral_premix": (["minecraft:bone_meal", "farmings:ground_limestone"], 4),
    "sheep_cheese": (["farmings:sheep_cheese_wheel"], 8),
    "shepherds_whistle": (["minecraft:iron_nugget", "minecraft:iron_nugget", "minecraft:string"], 1),
}
COOKING = {"select_steak": "cooked_select_steak", "choice_steak": "cooked_choice_steak", "prime_steak": "cooked_prime_steak",
           "lamb_chop": "cooked_lamb_chop", "mutton_chop": "cooked_mutton_chop",
           "chicken": "cooked_chicken", "turkey": "cooked_turkey", "duck": "cooked_duck", "quail": "cooked_quail"}


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
    # poultry recipes with vanilla results or several inputs (docs/eggs.md)
    for egg in ("chicken_egg", "turkey_egg", "quail_egg", "duck_egg"):
        for kind, time in (("smelting", 200), ("smoking", 100), ("campfire_cooking", 600)):
            fname = "fried_egg" if (egg, kind) == ("chicken_egg", "smelting") else "fried_egg_from_%s_%s" % (egg, kind)
            write(os.path.join(out, fname + ".json"), {"type": "minecraft:" + kind, "category": "food",
                  "ingredient": "farmings:" + egg, "result": {"id": "farmings:fried_egg"}, "experience": 0.35, "cookingtime": time})
    write(os.path.join(out, "cake_with_farmings_eggs.json"), {"type": "minecraft:crafting_shaped", "category": "misc",
          "pattern": ["MMM", "SES", "WWW"], "key": {"M": "minecraft:milk_bucket", "S": "minecraft:sugar", "E": "#farmings:eggs", "W": "minecraft:wheat"},
          "result": {"id": "minecraft:cake", "count": 1}})
    write(os.path.join(out, "pumpkin_pie_with_farmings_eggs.json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
          "ingredients": ["minecraft:pumpkin", "minecraft:sugar", "#farmings:eggs"], "result": {"id": "minecraft:pumpkin_pie", "count": 1}})
    for bale in ("alfalfa_bale", "grass_bale"):
        write(os.path.join(out, "hay_block_from_%s.json" % bale), {"type": "minecraft:crafting_shapeless", "category": "misc",
              "ingredients": ["farmings:" + bale], "result": {"id": "minecraft:hay_block", "count": 4}})
    write(os.path.join(out, "straw.json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
          "ingredients": ["farmings:straw_bale"], "result": {"id": "farmings:straw", "count": 45}})
    write(os.path.join(out, "white_bed_from_down.json"), {"type": "minecraft:crafting_shaped", "category": "misc",
          "pattern": ["QQQ", "WWW"], "key": {"Q": "farmings:down", "W": "#minecraft:planks"}, "result": {"id": "minecraft:white_bed", "count": 1}})
    # no two shaped recipes may share a pattern and key
    seen = {}
    for name, (pattern, _) in SHAPED.items():
        sig = json.dumps([[KEYS.get(c, " ") for c in row] for row in pattern])
        assert sig not in seen, "%s and %s have the same recipe" % (name, seen[sig])
        seen[sig] = name


COLD_ROOM = {"cold_room_panel": "Insulated Panel", "cold_room_door": "Cold-Room Door", "evaporator": "Evaporator",
             "condensing_unit": "Condensing Unit"}


def cold_room_textures():
    """Drawn stand-ins (tools/process_art.py replaces them when Ludo art is in art/final): white insulated panel with seams, a
    finned evaporator face, a condensing unit with a fan grille, and the two door halves."""
    from PIL import Image, ImageDraw
    tex = os.path.join(ASSETS, "textures", "block")
    def panel():
        im = Image.new("RGBA", (16, 16), (232, 234, 236, 255))
        d = ImageDraw.Draw(im)
        for y in (0, 15):
            d.line([(0, y), (15, y)], fill=(196, 200, 204, 255))
        for x in (0, 15):
            d.line([(x, 0), (x, 15)], fill=(196, 200, 204, 255))
        for y in range(3, 15, 4):
            d.line([(1, y), (14, y)], fill=(220, 222, 226, 255))
        return im
    out = {}     # the panel itself is the Ludo insulated panel (textures/block/mat/mat_insulated_panel)
    front = Image.new("RGBA", (16, 16), (210, 214, 218, 255))
    d = ImageDraw.Draw(front)
    for x in range(1, 15, 2):
        d.line([(x, 2), (x, 13)], fill=(150, 156, 162, 255))
    d.ellipse([4, 4, 11, 11], outline=(90, 96, 104, 255))
    out["evaporator_front"] = front
    cu = Image.new("RGBA", (16, 16), (120, 128, 136, 255))
    d = ImageDraw.Draw(cu)
    d.rectangle([0, 0, 15, 15], outline=(80, 86, 92, 255))
    d.ellipse([2, 2, 13, 13], fill=(60, 64, 70, 255), outline=(170, 176, 184, 255))
    for i in range(3, 13, 2):
        d.line([(3, i), (12, i)], fill=(150, 156, 162, 255))
    out["condensing_unit_side"] = cu
    top = Image.new("RGBA", (16, 16), (110, 118, 126, 255))
    ImageDraw.Draw(top).rectangle([0, 0, 15, 15], outline=(80, 86, 92, 255))
    out["condensing_unit_top"] = top
    for half in ("bottom", "top"):
        im = panel()
        dd = ImageDraw.Draw(im)
        dd.rectangle([0, 0, 15, 15], outline=(150, 156, 162, 255))
        if half == "bottom":
            dd.rectangle([12, 2, 13, 8], fill=(90, 96, 104, 255))     # the handle
        else:
            dd.rectangle([3, 3, 12, 9], fill=(170, 200, 220, 255), outline=(150, 156, 162, 255))   # a small window
        out["cold_room_door_" + half] = im
    for name, im in out.items():
        if not os.path.exists(os.path.join(os.path.dirname(__file__), "..", "art", "final", name + ".png")):
            im.save(os.path.join(tex, name + ".png"))
    if not os.path.exists(os.path.join(os.path.dirname(__file__), "..", "art", "final", "cold_room_door_bottom.png")):
        out["cold_room_door_bottom"].save(os.path.join(ASSETS, "textures", "item", "cold_room_door.png"))


def cold_room_assets(lang):
    m = os.path.join(ASSETS, "models", "block")
    for name, title in COLD_ROOM.items():
        lang["block.farmings." + name] = title
    write(os.path.join(m, "cold_room_panel.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/mat/mat_insulated_panel"}})
    write(os.path.join(ASSETS, "blockstates", "cold_room_panel.json"), {"variants": {"": {"model": "farmings:block/cold_room_panel"}}})
    write(os.path.join(m, "condensing_unit.json"), {"parent": "minecraft:block/cube_column",
          "textures": {"side": "farmings:block/condensing_unit_side", "end": "farmings:block/condensing_unit_top"}})
    write(os.path.join(ASSETS, "blockstates", "condensing_unit.json"), {"variants": {"": {"model": "farmings:block/condensing_unit"}}})
    write(os.path.join(m, "evaporator.json"), {"parent": "minecraft:block/orientable", "textures": {
          "front": "farmings:block/evaporator_front", "side": "farmings:block/mat/mat_insulated_panel", "top": "farmings:block/mat/mat_insulated_panel"}})
    write(os.path.join(ASSETS, "blockstates", "evaporator.json"), {"variants": {
          "facing=%s" % f: {"model": "farmings:block/evaporator", **({"y": y} if y else {})}
          for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    # the door: the vanilla door's eight models and its rotation rule
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            for op in ("", "_open"):
                write(os.path.join(m, "cold_room_door_%s_%s%s.json" % (half, hinge, op)), {"parent": "minecraft:block/door_%s_%s%s" % (half, hinge, op),
                      "textures": {"bottom": "farmings:block/cold_room_door_bottom", "top": "farmings:block/cold_room_door_top"}})
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, y0 in base.items():
        for half, hname in (("lower", "bottom"), ("upper", "top")):
            for hinge in ("left", "right"):
                for op in ("false", "true"):
                    y = y0 if op == "false" else (y0 + (90 if hinge == "left" else 270)) % 360
                    model = "farmings:block/cold_room_door_%s_%s%s" % (hname, hinge, "_open" if op == "true" else "")
                    variants["facing=%s,half=%s,hinge=%s,open=%s" % (facing, half, hinge, op)] = {"model": model, **({"y": y} if y else {})}
    write(os.path.join(ASSETS, "blockstates", "cold_room_door.json"), {"variants": variants})
    write(os.path.join(ASSETS, "models", "item", "cold_room_door.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "farmings:item/cold_room_door"}})
    for name in COLD_ROOM:
        model = "farmings:item/cold_room_door" if name == "cold_room_door" else "farmings:block/" + name
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": model}})
        drop = loot(name)
        if name == "cold_room_door":
            drop["pools"][0]["entries"][0]["conditions"] = [{"condition": "minecraft:block_state_property", "block": "farmings:cold_room_door",
                                                             "properties": {"half": "lower"}}]
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), drop)
    cold_room_textures()


WOOL_DYES = {'white': (234, 236, 236), 'orange': (240, 118, 19), 'magenta': (189, 68, 179), 'light_blue': (58, 175, 217), 'yellow': (248, 197, 39), 'lime': (112, 185, 25), 'pink': (237, 141, 172), 'gray': (62, 68, 71), 'light_gray': (142, 142, 134), 'cyan': (21, 137, 145), 'purple': (121, 42, 172), 'blue': (53, 57, 157), 'brown': (114, 71, 40), 'green': (84, 109, 27), 'red': (160, 39, 34), 'black': (26, 26, 30)}
MILL = {"scouring_bowl": "Scouring Bowl", "carding_machine": "Carding Machine", "spinning_frame": "Spinning Frame", "loom_frame": "Power Loom",
        "felting_table": "Felting Table"}


def wool_mill_assets(lang):
    """The mill machines (drawn fronts on a steel body), their loot, and the mill's recipes that make vanilla blocks or dye yarn."""
    from PIL import Image, ImageDraw
    tex = os.path.join(ASSETS, "textures", "block")
    m = os.path.join(ASSETS, "models", "block")
    body = Image.new("RGBA", (16, 16), (150, 156, 162, 255))
    ImageDraw.Draw(body).rectangle([0, 0, 15, 15], outline=(100, 106, 112, 255))
    fronts = {}
    f = body.copy(); d = ImageDraw.Draw(f); d.rectangle([2, 5, 13, 13], fill=(90, 130, 170, 255)); d.rectangle([3, 6, 12, 8], fill=(236, 232, 218, 255))
    fronts["scouring_bowl"] = f        # a bowl of water with wool on it
    f = body.copy(); d = ImageDraw.Draw(f)
    for y in (4, 8, 12):
        d.ellipse([2, y - 2, 13, y + 2], fill=(120, 110, 96, 255), outline=(70, 70, 76, 255))
    fronts["carding_machine"] = f      # the card's toothed rollers
    f = body.copy(); d = ImageDraw.Draw(f)
    for x in (3, 7, 11):
        d.rectangle([x, 3, x + 2, 13], fill=(236, 232, 218, 255)); d.line([(x + 1, 1), (x + 1, 3)], fill=(60, 60, 66, 255))
    fronts["spinning_frame"] = f       # bobbins
    f = body.copy(); d = ImageDraw.Draw(f)
    for x in range(2, 14, 2):
        d.line([(x, 2), (x, 13)], fill=(226, 220, 204, 255))
    for y in range(3, 13, 3):
        d.line([(2, y), (13, y)], fill=(196, 188, 168, 255))
    fronts["loom_frame"] = f           # warp and weft
    f = body.copy(); d = ImageDraw.Draw(f); d.rectangle([2, 6, 13, 10], fill=(176, 170, 158, 255))
    fronts["felting_table"] = f        # a felt sheet
    for name, title in MILL.items():
        lang["block.farmings." + name] = title
        if not os.path.exists(os.path.join(os.path.dirname(__file__), "..", "art", "final", name + "_front.png")):
            fronts[name].save(os.path.join(tex, name + "_front.png"))
        write(os.path.join(m, name + ".json"), {"parent": "minecraft:block/cube", "textures": {
              "north": "farmings:block/%s_front" % name, "south": "farmings:block/wool_machine_side", "east": "farmings:block/wool_machine_side",
              "west": "farmings:block/wool_machine_side", "up": "farmings:block/wool_machine_side", "down": "farmings:block/wool_machine_side",
              "particle": "farmings:block/wool_machine_side"}})
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
    body.save(os.path.join(tex, "wool_machine_side.png"))
    out = os.path.join(DATA, "recipe")
    for colour in WOOL_DYES:
        # four skeins of yarn make one vanilla wool block of their colour; carpets and beds then come from vanilla's own recipes
        write(os.path.join(out, "%s_wool_from_yarn.json" % colour), {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["YY", "YY"],
              "key": {"Y": "farmings:yarn_" + colour}, "result": {"id": "minecraft:%s_wool" % colour, "count": 1}})
        if colour != "white":
            write(os.path.join(out, "yarn_%s.json" % colour), {"type": "minecraft:crafting_shapeless", "category": "misc",
                  "ingredients": ["farmings:yarn_white", "minecraft:%s_dye" % colour], "result": {"id": "farmings:yarn_" + colour, "count": 1}})
    # felt insulates a cold-room panel as well as wool does (docs/wool.md)
    write(os.path.join(out, "cold_room_panel_from_felt.json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "EEE", "III"],
          "key": {"I": "minecraft:iron_ingot", "E": "farmings:felt"}, "result": {"id": "farmings:cold_room_panel", "count": 4}})


SHED = {"shed_controller": "Sheep Shed Controller", "sheep_pen": "Sheep Pen", "lambing_pen": "Lambing Pen", "pasture_gate": "Pasture Gate",
        "shearing_stand": "Shearing Stand", "catching_pen": "Catching Pen", "wool_table": "Wool Table", "wool_press": "Wool Press"}
SHED_FLOORS = ("sheep_pen", "lambing_pen", "pasture_gate", "catching_pen")


def shed_textures():
    """Drawn stand-ins (tools/process_art.py replaces them when Ludo art is in art/final): straw bedding, straw with hurdles round the
    edge, gate slats, and the controller's board with a tally sheet."""
    from PIL import Image, ImageDraw
    import random
    tex = os.path.join(ASSETS, "textures", "block")
    rnd = random.Random(11)
    def straw():
        im = Image.new("RGBA", (16, 16), (196, 168, 92, 255))
        d = ImageDraw.Draw(im)
        for _ in range(46):
            x, y = rnd.randrange(16), rnd.randrange(16)
            dx = rnd.choice((-3, -2, 2, 3))
            c = rnd.choice(((226, 200, 120), (170, 140, 70), (210, 186, 104)))
            d.line([(x, y), (x + dx, y + rnd.choice((-1, 0, 1)))], fill=c + (255,))
        return im
    out = {"sheep_pen_top": straw()}
    lp = straw()
    d = ImageDraw.Draw(lp)
    d.rectangle([0, 0, 15, 15], outline=(120, 84, 48, 255))
    d.rectangle([1, 1, 14, 14], outline=(150, 108, 64, 255))
    out["lambing_pen_top"] = lp
    gate = Image.new("RGBA", (16, 16), (140, 104, 60, 255))
    d = ImageDraw.Draw(gate)
    for y in range(1, 16, 4):
        d.rectangle([0, y, 15, y + 1], fill=(170, 128, 76, 255))
    d.line([(0, 15), (15, 0)], fill=(110, 80, 46, 255), width=2)
    out["pasture_gate_top"] = gate
    side = Image.new("RGBA", (16, 16), (150, 112, 66, 255))
    ImageDraw.Draw(side).line([(0, 14), (15, 14)], fill=(110, 80, 46, 255))
    out["shed_floor_side"] = side
    board = Image.new("RGBA", (16, 16), (150, 108, 64, 255))
    d = ImageDraw.Draw(board)
    for y in (4, 8, 12):
        d.line([(0, y), (15, y)], fill=(120, 84, 48, 255))
    d.rectangle([4, 2, 11, 13], fill=(236, 232, 214, 255), outline=(90, 70, 50, 255))
    for y in range(4, 12, 2):
        d.line([(6, y), (9, y)], fill=(70, 70, 80, 255))
    out["shed_controller"] = board
    catching = Image.new("RGBA", (16, 16), (128, 96, 58, 255))
    d = ImageDraw.Draw(catching)
    for x in range(0, 16, 3):
        d.line([(x, 0), (x, 15)], fill=(100, 74, 44, 255))         # slatted boards
    out["catching_pen_top"] = catching
    stand = Image.new("RGBA", (16, 16), (138, 100, 60, 255))
    d = ImageDraw.Draw(stand)
    d.rectangle([5, 0, 10, 15], fill=(110, 110, 116, 255))           # the down tube
    d.rectangle([3, 9, 12, 13], fill=(60, 60, 66, 255))             # the handpiece
    d.line([(3, 13), (12, 13)], fill=(200, 200, 206, 255))          # the comb
    out["shearing_stand"] = stand
    table = Image.new("RGBA", (16, 16), (176, 136, 84, 255))
    d = ImageDraw.Draw(table)
    for x in range(1, 16, 2):
        d.line([(x, 0), (x, 15)], fill=(150, 112, 66, 255))         # the slatted top lets dust and short bits fall through
    out["wool_table_top"] = table
    press = Image.new("RGBA", (16, 16), (126, 92, 54, 255))
    d = ImageDraw.Draw(press)
    d.rectangle([1, 1, 14, 14], outline=(80, 80, 86, 255))
    d.rectangle([3, 3, 12, 12], fill=(226, 218, 196, 255))          # a bale showing in the box
    for y in (5, 8, 11):
        d.line([(3, y), (12, y)], fill=(196, 186, 160, 255))
    out["wool_press"] = press
    for name, im in out.items():
        if not os.path.exists(os.path.join(os.path.dirname(__file__), "..", "art", "final", name + ".png")):
            im.save(os.path.join(tex, name + ".png"))


def shed_assets(lang):
    m = os.path.join(ASSETS, "models", "block")
    for name, title in SHED.items():
        lang["block.farmings." + name] = title
    for cube in ("shed_controller", "shearing_stand", "wool_press"):
        write(os.path.join(m, cube + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/" + cube}})
    leg = lambda x, z: {"from": [x, 0, z], "to": [x + 2, 10, z + 2], "faces": {f: {"texture": "#side"} for f in ("north", "south", "east", "west", "up", "down")}}
    write(os.path.join(m, "wool_table.json"), {
        "textures": {"top": "farmings:block/wool_table_top", "side": "farmings:block/shed_floor_side", "particle": "farmings:block/wool_table_top"},
        "elements": [{"from": [0, 10, 0], "to": [16, 13, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#top"},
                      **{f: {"texture": "#side", "uv": [0, 13, 16, 16]} for f in ("north", "south", "east", "west")}}},
                     leg(1, 1), leg(13, 1), leg(1, 13), leg(13, 13)]})
    for name in SHED_FLOORS:
        write(os.path.join(m, name + ".json"), {
            "textures": {"top": "farmings:block/%s_top" % name, "side": "farmings:block/shed_floor_side", "particle": "farmings:block/%s_top" % name},
            "elements": [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
                "up": {"texture": "#top"}, "down": {"texture": "#side", "cullface": "down"},
                **{f: {"texture": "#side", "uv": [0, 14, 16, 16]} for f in ("north", "south", "east", "west")}}}]})
    for name in SHED:
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
    shed_textures()


CORRAL = {"corral_post": "Corral Post", "corral_gate": "Corral Gate (one-way)", "bait_trough": "Bait Trough"}


def corral_textures():
    """Drawn faces: a weathered post with a tally board, gate rails, and a plank trough holding hay."""
    from PIL import Image, ImageDraw
    import random
    tex = os.path.join(ASSETS, "textures", "block")
    rnd = random.Random(23)
    out = {}
    post = Image.new("RGBA", (16, 16), (112, 82, 50, 255))
    d = ImageDraw.Draw(post)
    for x in range(0, 16, 3):
        d.line([(x, 0), (x, 15)], fill=(96, 70, 42, 255))           # grain
    d.rectangle([4, 3, 11, 9], fill=(232, 226, 204, 255), outline=(70, 52, 34, 255))   # the tally card
    for y in (5, 7):
        d.line([(6, y), (9, y)], fill=(70, 70, 80, 255))
    out["corral_post"] = post
    rail = Image.new("RGBA", (16, 16), (150, 112, 66, 255))
    d = ImageDraw.Draw(rail)
    for y in (3, 8, 13):
        d.line([(0, y), (15, y)], fill=(118, 86, 50, 255))
    out["corral_gate"] = rail
    top = Image.new("RGBA", (16, 16), (130, 96, 58, 255))
    d = ImageDraw.Draw(top)
    d.rectangle([2, 2, 13, 13], fill=(196, 168, 92, 255))           # hay in the trough
    for _ in range(30):
        x, y = rnd.randrange(2, 14), rnd.randrange(2, 14)
        d.line([(x, y), (min(13, x + rnd.choice((1, 2))), y)], fill=rnd.choice(((226, 200, 120), (170, 140, 70))) + (255,))
    out["bait_trough_top"] = top
    for name, im in out.items():
        if not os.path.exists(os.path.join(os.path.dirname(__file__), "..", "art", "final", name + ".png")):
            im.save(os.path.join(tex, name + ".png"))


def corral_assets(lang):
    m = os.path.join(ASSETS, "models", "block")
    for name, title in CORRAL.items():
        lang["block.farmings." + name] = title
    faces = lambda tex: {f: {"texture": tex} for f in ("north", "south", "east", "west", "up", "down")}
    write(os.path.join(m, "corral_post.json"), {"textures": {"wood": "farmings:block/corral_post", "particle": "farmings:block/corral_post"},
          "elements": [{"from": [5, 0, 5], "to": [11, 16, 11], "faces": faces("#wood")}]})
    # the gate runs along x and faces north (into the corral) in the model; the blockstate turns it
    posts = [{"from": [0, 0, 6], "to": [2, 16, 10], "faces": faces("#rail")}, {"from": [14, 0, 6], "to": [16, 16, 10], "faces": faces("#rail")}]
    rails = [{"from": [2, y, 7], "to": [14, y + 2, 9], "faces": faces("#rail")} for y in (4, 8, 12)]
    rails.append({"from": [2, 4, 7.5], "to": [14, 14, 8.5], "rotation": {"angle": 45, "axis": "z", "origin": [8, 9, 8]}, "faces": faces("#rail")})
    write(os.path.join(m, "corral_gate.json"), {"textures": {"rail": "farmings:block/corral_gate", "particle": "farmings:block/corral_gate"},
          "elements": posts + rails[:3] + [{"from": [6, 4, 7.5], "to": [10, 14, 8.5], "faces": faces("#rail")}]})
    write(os.path.join(m, "corral_gate_open.json"), {"textures": {"rail": "farmings:block/corral_gate", "particle": "farmings:block/corral_gate"},
          "elements": posts + [{"from": [0, y, 9], "to": [2, y + 2, 16], "faces": faces("#rail")} for y in (4, 8, 12)]})
    write(os.path.join(m, "bait_trough.json"), {"textures": {"top": "farmings:block/bait_trough_top", "side": "farmings:block/corral_gate",
                                                              "particle": "farmings:block/bait_trough_top"},
          "elements": [{"from": [0, 0, 0], "to": [16, 10, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#side"},
                        **{f: {"texture": "#side"} for f in ("north", "south", "east", "west")}}}]})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    write(os.path.join(ASSETS, "blockstates", "corral_gate.json"), {"variants": {
        "facing=%s,open=%s" % (f, o): {"model": "farmings:block/corral_gate" + ("_open" if o == "true" else ""), "y": r}
        for f, r in rot.items() for o in ("false", "true")}})
    for name in ("corral_post", "bait_trough"):
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
    for name in CORRAL:
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
    corral_textures()


COOP = {"coop_controller": "Coop Controller", "coop_floor": "Coop Floor", "pop_hole": "Pop Hole", "nest_box": "Nest Box", "perch": "Perch",
        "grain_feeder": "Grain Feeder"}


def coop_textures():
    """Drawn faces: a plank coop wall with a little door, wood-shaving litter, a straw-lined nest, a roost bar, a galvanised feeder."""
    from PIL import Image, ImageDraw
    import random
    tex = os.path.join(ASSETS, "textures", "block")
    rnd = random.Random(31)
    out = {}
    wall = Image.new("RGBA", (16, 16), (150, 108, 64, 255))
    d = ImageDraw.Draw(wall)
    for y in (0, 4, 8, 12):
        d.line([(0, y), (15, y)], fill=(118, 84, 48, 255))
    d.rectangle([5, 7, 10, 15], fill=(70, 50, 30, 255))             # the little door
    d.line([(5, 7), (10, 7)], fill=(196, 196, 186, 255))             # a white trim
    out["coop_controller"] = wall
    litter = Image.new("RGBA", (16, 16), (196, 170, 118, 255))
    d = ImageDraw.Draw(litter)
    for _ in range(60):
        x, y = rnd.randrange(16), rnd.randrange(16)
        d.point((x, y), fill=rnd.choice(((222, 198, 140), (170, 142, 92), (150, 120, 76))) + (255,))
    out["coop_floor"] = litter
    hole = litter.copy()
    d = ImageDraw.Draw(hole)
    d.rectangle([0, 0, 15, 2], fill=(118, 84, 48, 255))
    d.rectangle([0, 13, 15, 15], fill=(118, 84, 48, 255))
    out["pop_hole"] = hole
    nest = Image.new("RGBA", (16, 16), (150, 108, 64, 255))
    d = ImageDraw.Draw(nest)
    d.rectangle([2, 2, 13, 13], fill=(206, 180, 100, 255))           # straw
    for _ in range(24):
        x, y = rnd.randrange(2, 14), rnd.randrange(2, 14)
        d.line([(x, y), (min(13, x + 2), y)], fill=rnd.choice(((232, 210, 130), (178, 150, 72))) + (255,))
    out["nest_box_top"] = nest
    bar = Image.new("RGBA", (16, 16), (128, 92, 54, 255))
    d = ImageDraw.Draw(bar)
    for x in range(0, 16, 4):
        d.line([(x, 0), (x, 15)], fill=(108, 76, 44, 255))
    out["perch"] = bar
    tin = Image.new("RGBA", (16, 16), (170, 176, 180, 255))
    d = ImageDraw.Draw(tin)
    for y in range(0, 16, 3):
        d.line([(0, y), (15, y)], fill=(150, 156, 160, 255))
    d.rectangle([0, 12, 15, 15], fill=(200, 172, 96, 255))           # grain in the pan
    out["grain_feeder"] = tin
    for name, im in out.items():
        if not os.path.exists(os.path.join(os.path.dirname(__file__), "..", "art", "final", name + ".png")):
            im.save(os.path.join(tex, name + ".png"))


def coop_assets(lang):
    m = os.path.join(ASSETS, "models", "block")
    for name, title in COOP.items():
        lang["block.farmings." + name] = title
    faces = lambda tex: {f: {"texture": tex} for f in ("north", "south", "east", "west", "up", "down")}
    write(os.path.join(m, "coop_controller.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/coop_controller"}})
    for name in ("coop_floor", "pop_hole"):
        write(os.path.join(m, name + ".json"), {"textures": {"t": "farmings:block/" + name, "particle": "farmings:block/" + name},
              "elements": [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": faces("#t")}]})
    write(os.path.join(m, "nest_box.json"), {"textures": {"top": "farmings:block/nest_box_top", "side": "farmings:block/coop_controller",
                                                           "particle": "farmings:block/nest_box_top"},
          "elements": [{"from": [0, 0, 0], "to": [16, 12, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#side"},
                        **{f: {"texture": "#side"} for f in ("north", "south", "east", "west")}}}]})
    write(os.path.join(m, "perch.json"), {"textures": {"t": "farmings:block/perch", "particle": "farmings:block/perch"},
          "elements": [{"from": [0, 6, 6], "to": [16, 9, 10], "faces": faces("#t")},
                       {"from": [1, 0, 7], "to": [3, 6, 9], "faces": faces("#t")}, {"from": [13, 0, 7], "to": [15, 6, 9], "faces": faces("#t")}]})
    write(os.path.join(m, "grain_feeder.json"), {"textures": {"t": "farmings:block/grain_feeder", "particle": "farmings:block/grain_feeder"},
          "elements": [{"from": [4, 2, 4], "to": [12, 14, 12], "faces": faces("#t")}, {"from": [2, 0, 2], "to": [14, 2, 14], "faces": faces("#t")}]})
    for name in COOP:
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
    coop_textures()


HATCHERY = {"incubator": "Incubator", "brooder": "Brooder (heat lamp)"}


def hatchery_assets(lang):
    """A white cabinet with a window onto the egg trays; a red heat lamp in a steel shade."""
    from PIL import Image, ImageDraw
    tex = os.path.join(ASSETS, "textures", "block")
    m = os.path.join(ASSETS, "models", "block")
    cab = Image.new("RGBA", (16, 16), (228, 228, 222, 255))
    d = ImageDraw.Draw(cab)
    d.rectangle([2, 3, 13, 10], fill=(150, 190, 200, 255), outline=(120, 120, 120, 255))      # the window
    for x in range(3, 13, 2):
        d.ellipse([x, 6, x + 1, 8], fill=(236, 214, 170, 255))                               # eggs on the tray
    d.rectangle([10, 12, 13, 13], fill=(200, 60, 40, 255))                                    # the thermostat light
    lamp = Image.new("RGBA", (16, 16), (150, 156, 160, 255))
    d = ImageDraw.Draw(lamp)
    d.rectangle([3, 3, 12, 12], fill=(220, 70, 40, 255))
    d.rectangle([5, 5, 10, 10], fill=(255, 150, 80, 255))
    for name, im in (("incubator", cab), ("brooder", lamp)):
        if not os.path.exists(os.path.join(os.path.dirname(__file__), "..", "art", "final", name + ".png")):
            im.save(os.path.join(tex, name + ".png"))
    faces = lambda t: {f: {"texture": t} for f in ("north", "south", "east", "west", "up", "down")}
    write(os.path.join(m, "incubator.json"), {"textures": {"t": "farmings:block/incubator", "particle": "farmings:block/incubator"},
          "elements": [{"from": [1, 0, 1], "to": [15, 12, 15], "faces": faces("#t")}]})
    write(os.path.join(m, "brooder.json"), {"textures": {"t": "farmings:block/brooder", "particle": "farmings:block/brooder"},
          "elements": [{"from": [5, 8, 5], "to": [11, 12, 11], "faces": faces("#t")}, {"from": [7.5, 12, 7.5], "to": [8.5, 16, 8.5], "faces": faces("#t")}]})
    for name, title in HATCHERY.items():
        lang["block.farmings." + name] = title
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))


def egg_assets(lang):
    """The egg grading table: a plank top with a lit candling hole and egg trays."""
    from PIL import Image, ImageDraw
    tex = os.path.join(ASSETS, "textures", "block")
    top = Image.new("RGBA", (16, 16), (170, 128, 80, 255))
    d = ImageDraw.Draw(top)
    for x in range(1, 15, 3):
        for y in (2, 5, 8):
            d.ellipse([x, y, x + 1, y + 1], fill=(236, 214, 170, 255))
    d.ellipse([10, 11, 13, 14], fill=(255, 230, 140, 255))         # the candling light
    if not os.path.exists(os.path.join(os.path.dirname(__file__), "..", "art", "final", "egg_grader.png")):
        top.save(os.path.join(tex, "egg_grader.png"))
    write(os.path.join(ASSETS, "models", "block", "egg_grader.json"), {"textures": {"top": "farmings:block/egg_grader", "side": "farmings:block/coop_controller",
                                                                       "particle": "farmings:block/egg_grader"},
          "elements": [{"from": [0, 0, 0], "to": [16, 14, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#side"},
                        **{f: {"texture": "#side"} for f in ("north", "south", "east", "west")}}}]})
    lang["block.farmings.egg_grader"] = "Egg Grading Table"
    write(os.path.join(ASSETS, "blockstates", "egg_grader.json"), {"variants": {"": {"model": "farmings:block/egg_grader"}}})
    write(os.path.join(ASSETS, "items", "egg_grader.json"), {"model": {"type": "minecraft:model", "model": "farmings:block/egg_grader"}})
    write(os.path.join(DATA, "loot_table", "blocks", "egg_grader.json"), loot("egg_grader"))


CROPS = {"alfalfa": ("Alfalfa", (96, 150, 60), (150, 110, 200)), "timothy": ("Timothy", (110, 160, 70), (150, 150, 110)),
         "orchardgrass": ("Orchardgrass", (90, 140, 70), (120, 130, 90)), "corn": ("Corn", (80, 150, 50), (230, 200, 90)),
         "oats": ("Oats", (120, 170, 80), (220, 205, 140)), "barley": ("Barley", (120, 165, 70), (225, 195, 110))}
CROP_PRODUCTS = {"alfalfa_hay": ("Alfalfa Hay (loose, 10 kg)", (150, 170, 90)), "grass_hay": ("Grass Hay (loose, 10 kg)", (180, 175, 110)),
                 "straw": ("Straw (loose, 10 kg)", (220, 200, 120)), "alfalfa_bale": ("Round Bale of Alfalfa Hay", (140, 160, 80)),
                 "grass_bale": ("Round Bale of Grass Hay", (175, 170, 100)), "straw_bale": ("Round Bale of Straw", (220, 200, 120)),
                 "corn_grain": ("Corn (grain, 1 kg)", (235, 190, 60)), "oat_grain": ("Oats (grain, 1 kg)", (215, 195, 140)),
                 "barley_grain": ("Barley (grain, 1 kg)", (215, 185, 110)), "chopped_corn": ("Chopped Corn (25 kg)", (140, 160, 60)),
                 "chopped_alfalfa": ("Chopped Alfalfa (25 kg)", (100, 140, 60)), "corn_silage": ("Corn Silage (25 kg)", (160, 150, 70)),
                 "haylage": ("Haylage (25 kg)", (120, 130, 60))}


def crop_textures():
    """Drawn crop stages: stems that grow with the stage, then each crop's head colour at the last stage."""
    import random
    tex = os.path.join(ASSETS, "textures", "block")
    for name, (_, green, head) in CROPS.items():
        rnd = random.Random(name)
        stems = [rnd.randrange(1, 15) for _ in range(9)]
        for stage in range(8):
            img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            d = ImageDraw.Draw(img)
            h = 2 + int(13 * (stage + 1) / 8)
            for i, x in enumerate(stems):
                top = 15 - h + (i % 3)
                c = tuple(max(0, min(255, v + rnd.randrange(-15, 15))) for v in green)
                d.line([(x, 15), (x, max(0, top))], fill=c + (255,))
                if stage >= 6:
                    d.rectangle([x - (1 if name in ("alfalfa", "corn") else 0), max(0, top), x, max(0, top) + 2], fill=head + (255,))
            img.save(os.path.join(tex, "%s_stage%d.png" % (name, stage)))
        if name == "corn":
            img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            d = ImageDraw.Draw(img)
            for x in stems:
                d.line([(x, 15), (x, 1)], fill=(190, 170, 100, 255))
                d.rectangle([x, 6, x + 1, 9], fill=(235, 195, 70, 255))
            img.save(os.path.join(tex, "corn_ripe.png"))


def crop_assets(lang):
    m = os.path.join(ASSETS, "models", "block")
    for name, (title, _, _) in CROPS.items():
        lang["block.farmings." + name] = title
        lang["item.farmings.%s_seeds" % name] = ("Seed Corn" if name == "corn" else title + " Seeds")
        stages = ["%s_stage%d" % (name, i) for i in range(8)] + (["corn_ripe"] if name == "corn" else [])
        for st in stages:
            if name == "corn":
                height = 16 if st.endswith(("0", "1", "2")) else 32 if st in ("corn_stage6", "corn_stage7", "corn_ripe") else 24
                planes = []
                for x in (4, 12):
                    planes.append({"from": [x, 0, 0], "to": [x, height, 16], "shade": False,
                                   "faces": {"east": {"uv": [0, 0, 16, 16], "texture": "#crop"}, "west": {"uv": [0, 0, 16, 16], "texture": "#crop"}}})
                    planes.append({"from": [0, 0, x], "to": [16, height, x], "shade": False,
                                   "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#crop"}, "south": {"uv": [0, 0, 16, 16], "texture": "#crop"}}})
                write(os.path.join(m, st + ".json"), {"ambientocclusion": False, "textures": {"crop": "farmings:block/" + st, "particle": "farmings:block/" + st},
                                                       "elements": planes, "render_type": "minecraft:cutout"})
            else:
                write(os.path.join(m, st + ".json"), {"parent": "minecraft:block/crop", "textures": {"crop": "farmings:block/" + st}, "render_type": "minecraft:cutout"})
        if name == "corn":
            variants = {"age=%d,ripe=false" % i: {"model": "farmings:block/corn_stage%d" % i} for i in range(8)}
            variants.update({"age=%d,ripe=true" % i: {"model": "farmings:block/corn_ripe"} for i in range(8)})
            write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": variants})
        elif name in ("alfalfa", "timothy", "orchardgrass"):
            parts = [{"when": {"age": str(i)}, "apply": {"model": "farmings:block/%s_stage%d" % (name, i)}} for i in range(8)]
            parts.append({"when": {"swath": "1"}, "apply": {"model": "farmings:block/swath_flat_hay"}})
            parts.append({"when": {"swath": "2"}, "apply": {"model": "farmings:block/swath_row_hay"}})
            write(os.path.join(ASSETS, "blockstates", name + ".json"), {"multipart": parts})
        else:
            variants = {"age=%d" % i: {"model": "farmings:block/%s_stage%d" % (name, i)} for i in range(8)}
            write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": variants})
        seed = name + "_seeds"
        real = os.path.exists(os.path.join(ASSETS, "textures", "item", seed + ".png"))
        if not real:
            item_texture(seed, CROPS[name][2], False)
        write(os.path.join(ASSETS, "models", "item", seed + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "farmings:item/" + seed + ("" if real else "_placeholder")}})
        write(os.path.join(ASSETS, "items", seed + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:item/" + seed}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), crop_loot(name))
    for name, (title, colour) in CROP_PRODUCTS.items():
        lang["item.farmings." + name] = title
        real = os.path.exists(os.path.join(ASSETS, "textures", "item", name + ".png"))
        if not real:
            item_texture(name, colour, False)
        tex = "farmings:item/" + name + ("" if real else "_placeholder")
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:item/" + name}})
    crop_textures()


def crop_loot(name):
    """By hand (docs/crops.md): the ripe crop's product at its block yield (10 kg loose hay or straw as a chance, 1 kg grain as a count),
    and seeds back. The gantry harvests in code."""
    ripe = {"type": "minecraft:match_block", "blocks": "farmings:" + name, "state": {"age": "7"}}
    if name == "corn":
        ripe = {"type": "minecraft:match_block", "blocks": "farmings:corn", "state": {"ripe": "true"}}
    seed = "farmings:%s_seeds" % name
    pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": seed}]},
             {"rolls": 1, "condition": ripe, "entries": [{"type": "minecraft:item", "name": seed,
                                                           "modifier": {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}}]}]
    def chance(item, p):
        return {"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [ripe, {"type": "minecraft:random_chance", "chance": p}]},
                "entries": [{"type": "minecraft:item", "name": item}]}
    def count(item, n):
        return {"rolls": 1, "condition": ripe, "entries": [{"type": "minecraft:item", "name": item, "modifier": {"type": "minecraft:set_count", "count": n}}]}
    if name == "alfalfa":
        pools.append(chance("farmings:alfalfa_hay", 0.225))
    elif name in ("timothy", "orchardgrass"):
        pools.append(chance("farmings:grass_hay", 0.223))
    elif name == "oats":
        pools += [count("farmings:oat_grain", 2), chance("farmings:straw", 0.455)]
    elif name == "barley":
        pools += [count("farmings:barley_grain", 4), chance("farmings:straw", 0.226)]
    elif name == "corn":
        pools.append(count("farmings:corn_grain", 11))
    return {"type": "minecraft:block", "modifier": {"type": "minecraft:explosion_decay"}, "pools": pools, "random_sequence": "farmings:blocks/" + name}


CONVEYORS = {"farm_belt": "Belt Conveyor", "farm_belt_drive": "Belt Drive (head pulley)", "grain_auger": "Grain Auger",
             "grain_auger_drive": "Grain Auger Drive"}


def conveyor_assets(lang):
    """A black belt on a steel frame, an animated belt surface, a motor box, a galvanised auger tube. Every model faces north (the load
    moves to -z); the blockstates turn them."""
    tex = os.path.join(ASSETS, "textures", "block")
    m = os.path.join(ASSETS, "models", "block")
    frames = 4
    belt = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 255))
    d = ImageDraw.Draw(belt)
    for f in range(frames):
        d.rectangle([0, 16 * f, 15, 16 * f + 15], fill=(36, 36, 38, 255))
        for y in range(0, 16, 4):
            yy = 16 * f + (y + f) % 16
            d.line([(1, yy), (14, yy)], fill=(60, 60, 64, 255))
        d.line([(0, 16 * f), (0, 16 * f + 15)], fill=(150, 150, 150, 255))
        d.line([(15, 16 * f), (15, 16 * f + 15)], fill=(150, 150, 150, 255))
    belt.save(os.path.join(tex, "farm_belt.png"))
    with open(os.path.join(tex, "farm_belt.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2}}, fh)
    frame = Image.new("RGBA", (16, 16), (150, 152, 156, 255))
    d = ImageDraw.Draw(frame)
    d.rectangle([0, 6, 15, 9], fill=(110, 112, 116, 255))
    frame.save(os.path.join(tex, "farm_belt_frame.png"))
    motor = Image.new("RGBA", (16, 16), (60, 100, 150, 255))
    d = ImageDraw.Draw(motor)
    for y in range(2, 14, 3):
        d.line([(2, y), (13, y)], fill=(40, 70, 110, 255))
    d.rectangle([5, 5, 10, 10], fill=(150, 152, 156, 255))
    motor.save(os.path.join(tex, "conveyor_motor.png"))
    tube = Image.new("RGBA", (16, 16), (176, 180, 182, 255))
    d = ImageDraw.Draw(tube)
    for x in range(0, 16, 4):
        d.line([(x, 0), (x, 15)], fill=(150, 154, 156, 255))
    tube.save(os.path.join(tex, "grain_auger.png"))
    faces = lambda t: {f: {"texture": t} for f in ("north", "south", "east", "west", "up", "down")}
    write(os.path.join(m, "farm_belt.json"), {"textures": {"belt": "farmings:block/farm_belt", "frame": "farmings:block/farm_belt_frame",
                                                            "particle": "farmings:block/farm_belt_frame"},
          "elements": [{"from": [0, 0, 0], "to": [16, 3, 16], "faces": {**faces("#frame"), "up": {"texture": "#belt", "uv": [1, 0, 15, 16]}}},
                       {"from": [0, 3, 0], "to": [1, 5, 16], "faces": faces("#frame")}, {"from": [15, 3, 0], "to": [16, 5, 16], "faces": faces("#frame")}]})
    write(os.path.join(m, "grain_auger.json"), {"textures": {"t": "farmings:block/grain_auger", "particle": "farmings:block/grain_auger"},
          "elements": [{"from": [4, 4, 0], "to": [12, 12, 16], "faces": faces("#t")}]})
    for drive in ("farm_belt_drive", "grain_auger_drive"):
        write(os.path.join(m, drive + ".json"), {"textures": {"t": "farmings:block/conveyor_motor", "f": "farmings:block/farm_belt_frame",
                                                               "particle": "farmings:block/conveyor_motor"},
              "elements": [{"from": [0, 0, 0], "to": [16, 10, 16], "faces": faces("#f")}, {"from": [3, 10, 3], "to": [13, 16, 13], "faces": faces("#t")}]})
    # a sloped belt cell: the belt as a ramp rising one block toward the way the load moves (north here); 45 degrees, a block up over one
    write(os.path.join(m, "farm_belt_slope.json"), {"textures": {"belt": "farmings:block/farm_belt", "frame": "farmings:block/farm_belt_frame",
                                                                  "particle": "farmings:block/farm_belt_frame"},
          "elements": [{"from": [0, 7, -3.31], "to": [16, 10, 19.31], "rotation": {"origin": [8, 8, 8], "axis": "x", "angle": 45},
                        "faces": {**faces("#frame"), "up": {"texture": "#belt", "uv": [1, 0, 15, 16]}}},
                       {"from": [0, 0, 6], "to": [16, 8, 10], "faces": faces("#frame")}]})
    rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    flip = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}
    for name, title in CONVEYORS.items():
        lang["block.farmings." + name] = title
        if name in ("farm_belt", "grain_auger"):
            v = {}
            for f, r in rot.items():
                for slope in ("flat", "up", "down"):
                    if name == "farm_belt" and slope == "up" and f not in ("up", "down"):
                        v["facing=%s,slope=%s" % (f, slope)] = {"model": "farmings:block/farm_belt_slope", **r}
                    elif name == "farm_belt" and slope == "down" and f not in ("up", "down"):
                        v["facing=%s,slope=%s" % (f, slope)] = {"model": "farmings:block/farm_belt_slope", **rot[flip[f]]}
                    else:
                        v["facing=%s,slope=%s" % (f, slope)] = {"model": "farmings:block/" + name, **r}
            write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": v})
        else:
            write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"facing=" + f: {"model": "farmings:block/" + name, **r} for f, r in rot.items()}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))


GANTRY = {"field_rail": "Field Rail", "gantry_truck": "Gantry End Truck", "gantry_beam": "Gantry Bridge Beam", "headland_stop": "Headland Stop",
          "gantry_cab": "Gantry Cab (controller)", "field_junction": "Field Cable Junction"}
GANTRY_TOOLS = {"seed_drill": ("Seed Drill Head", (60, 120, 60)), "harvester": ("Combine / Forage Chopper Head", (200, 60, 40)),
                "mower": ("Mower Head", (220, 170, 40)), "rake": ("Rake Head", (90, 90, 160)), "baler": ("Round Baler Head", (40, 120, 160)),
                "spreader": ("Spreader Head", (130, 90, 50))}


def gantry_assets(lang):
    """Galvanised rail on sleepers, a yellow-painted frame, a cab with a window, tool heads hanging under the beam, straw and hay swaths."""
    tex = os.path.join(ASSETS, "textures", "block")
    m = os.path.join(ASSETS, "models", "block")
    def fill(name, base, lines=None, box=None):
        img = Image.new("RGBA", (16, 16), base + (255,))
        d = ImageDraw.Draw(img)
        for l in (lines or []):
            d.line(l[0], fill=l[1] + (255,))
        if box:
            d.rectangle(box[0], fill=box[1] + (255,))
        img.save(os.path.join(tex, name + ".png"))
    fill("gantry_steel", (214, 176, 40), [([(0, 0), (15, 0)], (160, 130, 30)), ([(0, 15), (15, 15)], (160, 130, 30)), ([(0, 0), (15, 15)], (180, 150, 34))])
    fill("field_rail", (110, 84, 56), [([(0, 3), (15, 3)], (180, 182, 186)), ([(0, 12), (15, 12)], (180, 182, 186))])
    fill("gantry_cab", (214, 176, 40), None, ([3, 3, 12, 9], (120, 170, 200)))
    fill("field_junction", (120, 124, 128), None, ([5, 3, 10, 12], (230, 200, 40)))
    fill("headland_stop", (230, 200, 40), [([(0, y), (15, y)], (30, 30, 30)) for y in (2, 6, 10, 14)])
    for kind, col in (("hay", (176, 170, 90)), ("oat_straw", (220, 200, 120)), ("barley_straw", (214, 190, 110))):
        fill("swath_" + kind, col, [([(x, 0), (x, 15)], tuple(max(0, c - 30) for c in col)) for x in range(1, 16, 3)])
    tools = {"seed_drill": (60, 120, 60), "harvester": (200, 60, 40), "mower": (220, 170, 40), "rake": (90, 90, 160), "baler": (40, 120, 160),
             "spreader": (130, 90, 50)}
    for t, col in tools.items():
        fill("tool_" + t, col, [([(0, 8), (15, 8)], (40, 40, 40))])
    faces = lambda t: {f: {"texture": t} for f in ("north", "south", "east", "west", "up", "down")}
    write(os.path.join(m, "field_rail.json"), {"textures": {"t": "farmings:block/field_rail", "particle": "farmings:block/field_rail"},
          "elements": [{"from": [0, 0, 0], "to": [16, 3, 16], "faces": faces("#t")}]})
    write(os.path.join(m, "gantry_truck.json"), {"textures": {"t": "farmings:block/gantry_steel", "particle": "farmings:block/gantry_steel"},
          "elements": [{"from": [2, 0, 2], "to": [14, 16, 14], "faces": faces("#t")}]})
    beam = {"from": [0, 8, 4], "to": [16, 16, 12], "faces": faces("#t")}
    write(os.path.join(m, "gantry_beam.json"), {"textures": {"t": "farmings:block/gantry_steel", "particle": "farmings:block/gantry_steel"}, "elements": [beam]})
    for t in tools:
        write(os.path.join(m, "gantry_beam_" + t + ".json"), {"textures": {"t": "farmings:block/gantry_steel", "h": "farmings:block/tool_" + t,
                                                                            "particle": "farmings:block/gantry_steel"},
              "elements": [beam, {"from": [0, -12, 5], "to": [16, 8, 11], "faces": faces("#h")}]})
    write(os.path.join(m, "gantry_cab.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/gantry_cab"}})
    write(os.path.join(m, "field_junction.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/field_junction"}})
    write(os.path.join(m, "headland_stop.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/headland_stop"}})
    for kind in ("hay", "oat_straw", "barley_straw"):
        write(os.path.join(m, "swath_flat_" + kind + ".json"), {"textures": {"t": "farmings:block/swath_" + kind, "particle": "farmings:block/swath_" + kind},
              "elements": [{"from": [0, 0, 0], "to": [16, 1, 16], "faces": faces("#t")}]})
        write(os.path.join(m, "swath_row_" + kind + ".json"), {"textures": {"t": "farmings:block/swath_" + kind, "particle": "farmings:block/swath_" + kind},
              "elements": [{"from": [4, 0, 0], "to": [12, 5, 16], "faces": faces("#t")}]})
    tool_names = ["none"] + list(tools)
    def axis_rot(axis):
        return {"y": 90} if axis == "z" else {}
    for name in ("field_rail", "gantry_truck", "gantry_beam", "headland_stop"):
        variants = {}
        for axis in ("x", "z"):
            for t in tool_names:
                model = "farmings:block/" + name
                if name == "gantry_beam" and t != "none":
                    model = "farmings:block/gantry_beam_" + t
                variants["axis=%s,tool=%s" % (axis, t)] = {"model": model, **axis_rot(axis)}
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": variants})
    for name in ("gantry_cab", "field_junction"):
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
    write(os.path.join(ASSETS, "blockstates", "swath.json"), {"variants": {
        "kind=%s,raked=%s" % (k, r): {"model": "farmings:block/swath_%s_%s" % ("row" if r == "true" else "flat", k)}
        for k in ("oat_straw", "barley_straw") for r in ("false", "true")}})
    lang["block.farmings.swath"] = "Swath"
    for name, title in GANTRY.items():
        lang["block.farmings." + name] = title
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
    for t, (title, colour) in GANTRY_TOOLS.items():
        n = "tool_" + t
        lang["item.farmings." + n] = title
        real = os.path.exists(os.path.join(ASSETS, "textures", "item", n + ".png"))
        if not real:
            item_texture(n, colour, False)
        write(os.path.join(ASSETS, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "farmings:item/" + n + ("" if real else "_placeholder")}})
        write(os.path.join(ASSETS, "items", n + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:item/" + n}})


SILO = {"silo_stave": ("Silo Stave", (190, 188, 180)), "silo_roof": ("Silo Roof", (120, 60, 50)), "forage_blower": ("Forage Blower", (190, 50, 40)),
        "silo_unloader": ("Silo Unloader", (190, 50, 40)), "grain_bin_wall": ("Grain Bin Wall", (190, 194, 196)),
        "grain_bin_roof": ("Grain Bin Roof (fill hole)", (170, 174, 176)), "grain_bin_hopper": ("Grain Bin Hopper", (150, 154, 156))}


def silo_assets(lang):
    """Concrete staves with hoops, a dome roof, red machines, corrugated bin steel."""
    tex = os.path.join(ASSETS, "textures", "block")
    for name, (title, col) in SILO.items():
        img = Image.new("RGBA", (16, 16), col + (255,))
        d = ImageDraw.Draw(img)
        dark = tuple(max(0, c - 30) for c in col)
        if name == "silo_stave":
            for y in (0, 5, 10, 15):
                d.line([(0, y), (15, y)], fill=dark + (255,))
            d.line([(0, 7), (15, 7)], fill=(90, 90, 90, 255))
        elif name.startswith("grain_bin"):
            for x in range(0, 16, 2):
                d.line([(x, 0), (x, 15)], fill=dark + (255,))
        else:
            d.rectangle([3, 3, 12, 12], fill=dark + (255,))
        img.save(os.path.join(tex, name + ".png"))
        lang["block.farmings." + name] = title
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/" + name}})
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))


FEED_ITEMS = {"corn_meal": ("Corn Meal (1 kg)", (230, 196, 90)), "oat_meal": ("Oat Meal (1 kg)", (215, 200, 160)),
              "barley_meal": ("Barley Meal (1 kg)", (215, 190, 130)), "alfalfa_meal": ("Alfalfa Meal (1 kg)", (110, 150, 70)),
              "fish_meal": ("Fish Meal (1 kg)", (160, 130, 90)), "ground_limestone": ("Ground Limestone (1 kg)", (230, 230, 225)),
              "mineral_premix": ("Mineral Premix (1 kg)", (200, 180, 200)), "ration": ("Mixed Ration (25 kg)", (170, 160, 90))}


def feedmill_assets(lang):
    """A green hammer mill and a red vertical mixer; meal and ration placeholders until the Ludo icons."""
    tex = os.path.join(ASSETS, "textures", "block")
    for name, col in (("hammer_mill", (60, 120, 70)), ("tmr_mixer", (180, 50, 40))):
        img = Image.new("RGBA", (16, 16), col + (255,))
        d = ImageDraw.Draw(img)
        d.rectangle([2, 2, 13, 13], outline=(40, 40, 40, 255))
        d.ellipse([5, 5, 10, 10], fill=(170, 172, 176, 255))
        img.save(os.path.join(tex, name + ".png"))
        lang["block.farmings." + name] = "Hammer Mill" if name == "hammer_mill" else "TMR Mixer"
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/" + name}})
        if name == "tmr_mixer":
            big_cube_states(name, 8)     # a 2 x 2 x 2 big part (feed-mill.md, "the full-size mixer")
        else:
            write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
    for name, (title, colour) in FEED_ITEMS.items():
        lang["item.farmings." + name] = title
        real = os.path.exists(os.path.join(ASSETS, "textures", "item", name + ".png"))
        if not real:
            item_texture(name, colour, False)
        tex = "farmings:item/" + name + ("" if real else "_placeholder")
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:item/" + name}})


def compost_assets(lang):
    """A concrete pad, a plank controller board, an orange straddle turner; compost and solids placeholders until the Ludo icons."""
    tex = os.path.join(ASSETS, "textures", "block")
    blocks = {"compost_pad": ("Compost Pad", (150, 148, 140)), "compost_controller": ("Compost Windrow Controller", (140, 100, 60)),
              "windrow_turner": ("Windrow Turner", (220, 120, 30))}
    for name, (title, col) in blocks.items():
        img = Image.new("RGBA", (16, 16), col + (255,))
        d = ImageDraw.Draw(img)
        for x in range(0, 16, 5):
            d.line([(x, 0), (x, 15)], fill=tuple(max(0, c - 25) for c in col) + (255,))
        img.save(os.path.join(tex, name + ".png"))
        lang["block.farmings." + name] = title
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/" + name}})
        write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
    for name, title, colour in (("compost", "Compost (10 kg)", (70, 50, 30)), ("separated_solids", "Separated Digestate Solids (10 kg)", (110, 90, 60))):
        lang["item.farmings." + name] = title
        real = os.path.exists(os.path.join(ASSETS, "textures", "item", name + ".png"))
        if not real:
            item_texture(name, colour, False)
        tex = "farmings:item/" + name + ("" if real else "_placeholder")
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:item/" + name}})


def big_cube_states(name, pieces):
    """Every piece of a big part drawn as the same full cube, for each facing."""
    v = {}
    for facing in ("north", "east", "south", "west"):
        for piece in range(pieces):
            v["facing=%s,piece=%d" % (facing, piece)] = {"model": "farmings:block/" + name}
    write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": v})


def digester_assets(lang):
    """A green glass-lined tank, a concrete feed pit, a grey membrane gas holder, a digestate tank and a steel screw press."""
    tex = os.path.join(ASSETS, "textures", "block")
    blocks = {"digester_tank": ("Digester Tank", (70, 110, 70)), "digester_feed_pit": ("Digester Feed Pit", (140, 138, 130)),
              "gas_holder": ("Gas Holder (biogas)", (200, 202, 196)), "digestate_tank": ("Digestate Tank", (90, 120, 80)),
              "screw_press": ("Screw-Press Separator", (150, 154, 158))}
    for name, (title, col) in blocks.items():
        img = Image.new("RGBA", (16, 16), col + (255,))
        d = ImageDraw.Draw(img)
        for y in (3, 8, 13):
            d.line([(0, y), (15, y)], fill=tuple(max(0, c - 25) for c in col) + (255,))
        img.save(os.path.join(tex, name + ".png"))
        lang["block.farmings." + name] = title
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": "farmings:block/" + name}})
        if name == "digester_tank":
            big_cube_states(name, 27)     # a 3 x 3 x 3 big part (digester.md, "the full-size tank")
        else:
            write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "farmings:block/" + name}}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:block/" + name}})
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name))
    name = "liquid_digestate"
    lang["item.farmings." + name] = "Liquid Digestate (100 L)"
    real = os.path.exists(os.path.join(ASSETS, "textures", "item", name + ".png"))
    if not real:
        item_texture(name, (90, 80, 50), False)
    tex = "farmings:item/" + name + ("" if real else "_placeholder")
    write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
    write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": "farmings:item/" + name}})


def loot(name):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": "farmings:" + name}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": "farmings:blocks/" + name}


if __name__ == "__main__":
    main()

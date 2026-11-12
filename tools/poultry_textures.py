#!/usr/bin/env python3
"""Draws the bird textures for the shared poultry model (common/.../client/FowlModels.java; docs/animals/poultry.md, "Plumage and shell
genes"). Everything is drawn here from scratch (no Mojang pixels are copied; the repo is CC0). Deterministic: same output every run.

    python3 tools/poultry_textures.py

Output, in common/src/main/resources/assets/farmings/textures/entity/fowl/:
    <species>_<plumage>.png        one per plumage a species can show (FowlRenderer.textureName)
    duck_<plumage>_male.png        drake heads for the mallard, restricted and khaki patterns
    <species>_young.png            down for chicks, poults, quail chicks and ducklings
Colours are drawn, not measured.
"""
import os
from PIL import Image

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "fowl")

# name, (u, v), (w, h, d): the boxes of FowlModels, in the standard cube net
BOXES = [
    ("head", (0, 0), (4, 6, 3)), ("beak", (14, 0), (4, 2, 2)), ("wattle", (14, 4), (2, 2, 2)), ("snood", (22, 4), (1, 3, 1)),
    ("comb", (26, 0), (1, 3, 4)), ("crest", (36, 0), (5, 3, 5)), ("beard", (56, 0), (1, 3, 1)), ("body", (0, 9), (6, 8, 6)),
    ("wing", (24, 13), (1, 4, 6)), ("leg", (38, 13), (3, 5, 3)), ("tail", (50, 8), (4, 4, 2)), ("fan", (0, 24), (12, 10, 1)),
    ("bill", (26, 24), (4, 1, 3)), ("caruncle", (40, 24), (4, 2, 1)), ("foot", (52, 24), (3, 1, 3)), ("duckleg", (0, 36), (2, 4, 2)),
]


def net(u, v, w, h, d):
    """Face name -> (u0, v0, width, height), the vanilla cube layout."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "west": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "east": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def grain(x, y, salt):
    """A small deterministic jitter, -1 .. 1."""
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 32767.5 - 1.0


def mix(c, k):
    return tuple(max(0, min(255, int(round(v + k)))) for v in c)


def hexc(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


RED = hexc(0xC8202A)
EYE = hexc(0x15110E)
WHITE = hexc(0xF2F0EA)
BLACK = hexc(0x1C1C22)

# plumage palettes: head, neck/chest, body, back, wing, tail, plus a pattern
PLUMAGE = {
    "chicken_white": dict(all=WHITE),
    "chicken_red": dict(all=hexc(0x8E2A14), tail=BLACK, wingedge=BLACK, head=hexc(0x9A3018)),
    "chicken_black": dict(all=BLACK, sheen=hexc(0x2A4A3A)),
    "chicken_barred": dict(all=hexc(0xE4E4E4), bars=hexc(0x3A3A42)),
    "chicken_wild": dict(all=hexc(0x8B5A2B), head=hexc(0xD07A20), chest=hexc(0x2A1C14), tail=hexc(0x1A2A1A)),
    "turkey_white": dict(all=WHITE, head=hexc(0xC8505A)),
    "turkey_bronze": dict(all=hexc(0x3A2E22), sheen=hexc(0x7A5A30), head=hexc(0xC8505A), fanband=hexc(0x15110E), fantip=hexc(0xE0D6C2)),
    "turkey_black": dict(all=BLACK, sheen=hexc(0x2A3A34), head=hexc(0xC8505A)),
    "turkey_bourbon": dict(all=hexc(0x7A3420), wing=WHITE, tail=WHITE, fanband=hexc(0x7A3420), fantip=WHITE, head=hexc(0xC8505A)),
    "quail_coturnix": dict(all=hexc(0x8A6A48), speckle=hexc(0x3A2A1A), streak=hexc(0xD8C8A0), head=hexc(0x7A5A3A), brow=hexc(0xD8C8A0)),
    "quail_bobwhite": dict(all=hexc(0x8A5A3A), speckle=hexc(0x2A1A10), streak=hexc(0xC8B090), head=hexc(0x5A3A2A), brow=WHITE),
    "duck_white": dict(all=hexc(0xF4F2EA)),
    "duck_mallard": dict(all=hexc(0x8A6A4A), speckle=hexc(0x3A2A1A), speculum=hexc(0x3050A0)),
    "duck_mallard_male": dict(all=hexc(0xB8B8B0), head=hexc(0x1E5A32), chest=hexc(0x6A3A28), tail=BLACK, speculum=hexc(0x3050A0), ring=WHITE),
    "duck_restricted": dict(all=hexc(0x6A5038), speckle=hexc(0x2A1E14), speculum=hexc(0x3050A0)),
    "duck_restricted_male": dict(all=hexc(0x8A8A84), head=hexc(0x163E24), chest=hexc(0x4A2A1E), tail=BLACK, speculum=hexc(0x3050A0)),
    "duck_dusky": dict(all=hexc(0x5A4632), speckle=hexc(0x3A2A1C)),
    "duck_khaki": dict(all=hexc(0xA08A60), speckle=hexc(0x8A7450)),
    "duck_khaki_male": dict(all=hexc(0x8E7A54), head=hexc(0x4A5A3A), tail=hexc(0x5A4A30)),
    "duck_muscovy": dict(all=BLACK, sheen=hexc(0x2A4A3A), wingpatch=WHITE),
    # colour mutants (docs/animals/poultry.md, "Colour mutants")
    "duck_muscovy_blue": dict(all=hexc(0x5A6878), sheen=hexc(0x4A5868), wingpatch=WHITE),
    "duck_muscovy_silver": dict(all=hexc(0xB4B8BE), sheen=hexc(0x9EA4AC), wingpatch=WHITE),
    "duck_muscovy_lavender": dict(all=hexc(0x9C94A2), sheen=hexc(0x8A8292), wingpatch=WHITE),
    "duck_muscovy_chocolate": dict(all=hexc(0x5A3A24), sheen=hexc(0x6A4A30), wingpatch=WHITE),
    "quail_italian": dict(all=hexc(0xC89A5A), speckle=hexc(0x8A6438), streak=hexc(0xE8D8B0), head=hexc(0xB08048), brow=hexc(0xE8D8B0)),
    "quail_golden": dict(all=hexc(0xDCB474), speckle=hexc(0xB8905A), streak=hexc(0xF0E4C4), head=hexc(0xC89C60), brow=hexc(0xF0E4C4)),
    "quail_rosetta": dict(all=hexc(0x7A4A32), speckle=hexc(0x3A2218), streak=hexc(0xC8A080), head=hexc(0x6A3A26), brow=hexc(0xC8A080)),
    "quail_tibetan": dict(all=hexc(0x3A2A20), speckle=hexc(0x24180F), streak=hexc(0x6A5240), head=hexc(0x2E2018), brow=hexc(0x6A5240)),
    "quail_english_white": dict(all=hexc(0xF0EEE6), speckle=hexc(0xD8D4C8), head=hexc(0xF0EEE6)),
    "chicken_young": dict(all=hexc(0xF0D860), down=True),
    "turkey_young": dict(all=hexc(0xC8A878), down=True, stripe=hexc(0x8A6A40)),
    "quail_young": dict(all=hexc(0xB08A50), down=True, stripe=hexc(0x4A3420)),
    "duck_young": dict(all=hexc(0xF0D860), down=True, stripe=hexc(0x8A7A40)),
}

BEAK = {"chicken": hexc(0xE8C060), "turkey": hexc(0xC8B090), "quail": hexc(0x6B5B4B), "duck": hexc(0xE8B040)}
LEGS = {"chicken": hexc(0xE0B040), "turkey": hexc(0xC89080), "quail": hexc(0xD8A090), "duck": hexc(0xE89030)}
BILL = {"duck_mallard_male": hexc(0xC8C040), "duck_mallard": hexc(0xB07030), "duck_restricted": hexc(0xB07030),
        "duck_restricted_male": hexc(0xC8C040), "duck_khaki_male": hexc(0x5A6A30), "duck_khaki": hexc(0x4A4A30),
        "duck_dusky": hexc(0x4A4A30), "duck_muscovy": hexc(0xD8A0A0), "duck_muscovy_blue": hexc(0xD8A0A0),
        "duck_muscovy_silver": hexc(0xD8A0A0), "duck_muscovy_lavender": hexc(0xD8A0A0), "duck_muscovy_chocolate": hexc(0xD8A0A0)}


def plumage_colour(p, part, face, i, j, fw, fh, salt):
    base = p["all"]
    if p.get("down"):
        c = base
        if p.get("stripe") and part == "body" and face == "back" and (i in (1, fw - 2)):
            c = p["stripe"]
        return mix(c, 10 * grain(i + 7 * fw, j, salt))
    c = p.get(part if part in ("head", "tail", "wing") else "none", base)
    if part in ("fan",):
        c = p.get("tail", base)
        if "fanband" in p and fh - 3 <= (fh - 1 - j) <= fh - 2:
            c = p["fanband"]
        if "fantip" in p and j <= 1:
            c = p["fantip"]
    if part == "body" and face == "top" and "chest" in p:
        c = p["chest"]
    if part == "head" and "ring" in p and j == fh - 1:
        c = p["ring"]
    if part == "head" and "brow" in p and face in ("west", "east") and j == 1:
        c = p["brow"]
    if part == "wing" and "speculum" in p and face in ("west", "east") and j == 2:
        c = p["speculum"]
    if part == "wing" and "wingpatch" in p and j <= 1:
        c = p["wingpatch"]
    if part == "wing" and "wingedge" in p and j == fh - 1:
        c = p["wingedge"]
    if "bars" in p and part in ("body", "wing", "tail", "head") and (j + (i % 2 if part == "body" else 0)) % 2 == 1:
        c = p["bars"]
    if "speckle" in p and part in ("body", "wing", "head") and grain(i, j, salt + 11) > 0.55:
        c = p["speckle"]
    if "streak" in p and part in ("body", "wing") and face in ("back", "west", "east") and i % 3 == 1 and grain(i, j, salt + 5) > 0.2:
        c = p["streak"]
    if "sheen" in p and grain(i, j, salt + 3) > 0.6:
        c = p["sheen"]
    return mix(c, 9 * grain(i + 13 * fw, j + 17 * fh, salt))


def draw(name):
    species = name.split("_")[0]
    p = PLUMAGE[name]
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    salt = sum(ord(ch) for ch in name)
    for part, (u, v), (w, h, d) in BOXES:
        for face, (fu, fv, fw, fh) in net(u, v, w, h, d).items():
            for j in range(fh):
                for i in range(fw):
                    if part in ("comb", "wattle", "snood", "caruncle"):
                        c = mix(RED, 12 * grain(i, j, salt))
                    elif part == "beak":
                        c = BEAK[species]
                    elif part == "bill":
                        c = BILL.get(name, BEAK["duck"])
                    elif part in ("leg", "foot", "duckleg"):
                        c = hexc(0x5A5A6A) if name == "chicken_white_silkie" else LEGS[species]
                    elif part == "beard":
                        c = hexc(0x15110E)
                    else:
                        c = plumage_colour(p, part, face, i, j, fw, fh, salt)
                    px[fu + i, fv + j] = c + (255,)
    # eyes on the head's sides, near the front, a third of the way down
    hu, hv = 0, 0
    w, h, d = 4, 6, 3
    faces = net(hu, hv, w, h, d)
    fu, fv, fw, fh = faces["west"]
    px[fu, fv + 2] = EYE + (255,)
    fu, fv, fw, fh = faces["east"]
    px[fu + fw - 1, fv + 2] = EYE + (255,)
    img.save(os.path.join(OUT, name + ".png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    for name in PLUMAGE:
        draw(name)
    print("wrote %d bird textures to %s" % (len(PLUMAGE), os.path.normpath(OUT)))


if __name__ == "__main__":
    main()

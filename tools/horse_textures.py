#!/usr/bin/env python3
"""Draws the horse textures for the gene-driven horse (common/.../client/HorseRenderer.java; docs/animals/horses.md, "Coat colour genes").
Drawn from scratch on the UV layout of vanilla's horse model (its boxes are listed below; no Mojang pixels are copied; CC0). Deterministic.

    python3 tools/horse_textures.py

Output, in common/src/main/resources/assets/farmings/textures/entity/horse/:
    <base>_<cream>[_dun].png   the coat: base chestnut, bay or black; 0, 1 or 2 cream copies; dun or not (18 textures)
    roan.png                    white hairs through the body and neck (Rn)
    sabino_1.png, sabino_2.png  sabino white, one and two copies of SB1
    face_1.png, face_2.png      a star; a star and blaze (drawn from a seed, not genes)
    socks.png                   white socks on all four legs (the model's legs share one texture)
    grey_1.png ... grey_3.png   greying with age: dapples, mostly grey, white
Overlays are cut out: each texel is either drawn or clear.
"""
import math
import os
import random
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from cattle_textures import Noise, hair_grain, shade  # noqa: E402
from sheep_textures import texels, blank  # noqa: E402

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "horse")
O = (0.0, 0.0, 0.0)
# vanilla AbstractEquineModel.createBodyMesh: name, texOffs, box min, size (read from the 26.3 model's code), pivot, rotated, coat
BOXES = [
    ("body", (0, 32), (-5, -8, -17), (10, 10, 22), (0, 11, 5), False, True),
    ("neck", (0, 35), (-2.05, -6, -2), (4, 12, 7), (0, 4, -12), False, True),
    ("head", (0, 13), (-3, -11, -2), (6, 5, 7), (0, 4, -12), False, True),
    ("mane", (56, 36), (-1, -11, 5.01), (2, 16, 2), (0, 4, -12), False, True),
    ("mouth", (0, 25), (-2, -11, -7), (4, 5, 5), (0, 4, -12), False, True),
    ("leg", (48, 21), (-3, -1.01, -1), (4, 11, 4), O, False, True),
    ("tail", (42, 36), (-1.5, 0, 0), (3, 14, 4), (0, -5, 2), False, True),
    ("ear", (19, 16), (0.55, -13, 4), (2, 3, 1), (0, 4, -12), False, True),
]
WHITE = (236, 233, 226)
HOOF = (58, 50, 46)
LIGHT_HOOF = (156, 140, 118)


def coat(base, cream, dun):
    """Body, points (mane, tail, lower legs) and muzzle colours for a coat."""
    table = {
        ("chestnut", 0): ((150, 72, 34), (136, 62, 28), False),
        ("bay", 0): ((124, 64, 32), (28, 22, 20), True),
        ("black", 0): ((32, 28, 27), (24, 20, 19), True),
        ("chestnut", 1): ((214, 170, 92), (238, 226, 198), False),    # palomino, flaxen mane
        ("bay", 1): ((206, 164, 100), (30, 24, 22), True),             # buckskin
        ("black", 1): ((54, 44, 38), (34, 28, 26), True),              # smoky black
        ("chestnut", 2): ((236, 224, 200), (244, 238, 222), False),   # cremello
        ("bay", 2): ((234, 220, 194), (206, 160, 120), True),          # perlino
        ("black", 2): ((230, 216, 190), (196, 172, 150), True),        # smoky cream
    }
    body, points, dark_legs = table[(base, cream)]
    stripe = None
    if dun:
        if base == "black" and cream == 0:
            body = (126, 116, 106)                                     # grullo
        else:
            body = tuple(int(b * 0.55 + t * 0.45) for b, t in zip(body, (204, 172, 122)))
        stripe = points if dark_legs else tuple(int(c * 0.7) for c in body)
        if not dark_legs:
            points = stripe if cream == 0 else points
            dark_legs = cream == 0
    muzzle = (196, 150, 140) if cream == 2 else (52, 46, 46)
    hoof = LIGHT_HOOF if cream == 2 else HOOF
    return body, points, dark_legs, stripe, muzzle, hoof


def hoof_texel(box, face, st):
    return box == "leg" and (face == "bottom" or face not in ("top",) and st[1] > 0.88)


def lower_leg(box, face, st, from_t):
    return box == "leg" and face not in ("top", "bottom") and st[1] > from_t


def eye(box, face, st):
    return box == "head" and face in ("west", "east") and 0.15 < (st[0] if face == "west" else 1 - st[0]) < 0.35 and 0.3 < st[1] < 0.6


def draw_coat(tex, grain, base, cream, dun):
    body, points, dark_legs, stripe, muzzle, hoof = coat(base, cream, dun)
    img = blank()
    px = img.load()
    for (x, y), (box, face, p, _, st) in tex.items():
        c = body
        if box in ("mane", "tail"):
            c = points if base != "chestnut" or cream > 0 or dun else tuple(int(v * 0.9) for v in body)
        elif hoof_texel(box, face, st):
            c = hoof
        elif dark_legs and lower_leg(box, face, st, 0.45):
            c = points
        elif box == "mouth" and face in ("front", "bottom") or box == "mouth" and st[1] > 0.6 and face not in ("top",):
            c = muzzle
        elif box == "ear" and dark_legs:
            c = points
        elif stripe is not None and box == "body" and face == "top" and abs(st[0] - 0.5) < 0.12:
            c = stripe
        if eye(box, face, st):
            c = (18, 14, 14)
        g = 0.86 + 0.14 * grain(x, y)
        sh = shade(face)
        px[x, y] = (min(255, int(c[0] * g * sh)), min(255, int(c[1] * g * sh)), min(255, int(c[2] * g * sh)), 255)
    return img


def overlay(tex, grain, where, colour=WHITE):
    img = blank()
    px = img.load()
    for (x, y), (box, face, p, _, st) in tex.items():
        if hoof_texel(box, face, st) or eye(box, face, st):
            continue
        c = where(x, y, box, face, p, st)
        if c:
            col = c if isinstance(c, tuple) else colour
            g = 0.9 + 0.1 * grain(x, y)
            sh = shade(face)
            px[x, y] = (min(255, int(col[0] * g * sh)), min(255, int(col[1] * g * sh)), min(255, int(col[2] * g * sh)), 255)
    return img


def hashed(x, y, salt):
    return random.Random(x * 7919 + y * 104729 + salt).random()


def star(box, face, st):
    return box == "head" and face == "top" and 0.3 < st[0] < 0.7 and st[1] < 0.35


def blaze(box, face, st):
    return star(box, face, st) or box == "mouth" and face in ("top", "front") and 0.3 < st[0] < 0.7


def main():
    os.makedirs(OUT, exist_ok=True)
    grain = hair_grain()
    tex = texels(BOXES)
    n = 0
    for base in ("chestnut", "bay", "black"):
        for cream in (0, 1, 2):
            for dun in (False, True):
                name = "%s_%d%s.png" % (base, cream, "_dun" if dun else "")
                draw_coat(tex, grain, base, cream, dun).save(os.path.join(OUT, name))
                n += 1
    spots = Noise(5150)
    body_or_neck = ("body", "neck")
    overlays = {
        "roan": lambda x, y, b, f, p, st: b in body_or_neck and hashed(x, y, 1) < 0.45,
        "sabino_1": lambda x, y, b, f, p, st: lower_leg(b, f, st, 0.35) or blaze(b, f, st) or b == "mouth" and f == "bottom"
        or b == "body" and f == "bottom" and spots.at(p[0] / 4, p[1] / 4, p[2] / 4) > 0.45,
        "sabino_2": lambda x, y, b, f, p, st: b in ("head", "mouth", "leg", "ear") or b in body_or_neck and spots.at(p[0] / 5, p[1] / 5, p[2] / 5) < 0.72,
        "face_1": lambda x, y, b, f, p, st: star(b, f, st),
        "face_2": lambda x, y, b, f, p, st: blaze(b, f, st),
        "socks": lambda x, y, b, f, p, st: lower_leg(b, f, st, 0.7),
        "grey_1": lambda x, y, b, f, p, st: (b in body_or_neck and abs(spots.at(p[0] / 2.5, p[1] / 2.5, p[2] / 2.5) - 0.5) > 0.18
                                             or b in ("head", "mouth") and hashed(x, y, 2) < 0.3) and (196, 196, 194),
        "grey_2": lambda x, y, b, f, p, st: (b not in ("mane", "tail") and hashed(x, y, 3) < 0.7 or hashed(x, y, 4) < 0.3) and (212, 212, 208),
        "grey_3": lambda x, y, b, f, p, st: (b in ("mane", "tail") and (200, 200, 196)) or (b == "mouth" and f in ("front", "bottom") and (90, 86, 86))
        or (226, 226, 222),
    }
    for name, where in overlays.items():
        overlay(tex, grain, where).save(os.path.join(OUT, name + ".png"))
        n += 1
    print("wrote", n, "horse textures")


if __name__ == "__main__":
    main()

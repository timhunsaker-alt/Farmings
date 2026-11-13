#!/usr/bin/env python3
"""Draws the goat textures for the gene-driven goat model (common/.../client/GoatModels.java; docs/animals/goats.md, "Coat genes").
Everything is drawn here from scratch (no Mojang pixels are copied; the repo is CC0). Deterministic: same output every run.

    python3 tools/goat_textures.py

Output, in common/src/main/resources/assets/farmings/textures/entity/goat/:
    skin.png                short hair, near white so the renderer's tint (the agouti colour) shows true
    pattern_bezoar.png      black dorsal stripe, shoulder stripe, belly, legs and face stripes over the bezoar tan
    pattern_badgerface.png  black belly, legs and face stripes (no dorsal stripe) over the badgerface tan
    pattern_swiss.png       white face stripes, white lower legs and rump over the swiss brown (the Toggenburg look)
    redhead_2.png           the Boer: white body, red head and neck (EDNRA 2/2)
    redhead_1.png           a cross carrying one copy: a white belt and white patches with the red head
    detail.png              eyes, nose, hooves, horns, udder: drawn over everything and never tinted
Areas are cut from each texel's place on the model, with sheep_textures.faces and the cattle hair grain.
"""
import os
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from cattle_textures import Noise, hair_grain, shade  # noqa: E402
from sheep_textures import texels, blank  # noqa: E402

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "goat")

HEAD = (0.0, 5.0, -8.0)
BODY = (0.0, 5.0, 2.0)
ORIGIN = (0.0, 0.0, 0.0)
# name, (u, v), box min (x, y, z) in the part's frame, size, part pivot, rotated, coat?
SKIN = [
    ("head", (0, 0), (-2.5, -4, -6), (5, 5, 7), HEAD, False, True),
    ("ear", (24, 0), (-3.5, -6, -1), (1, 3, 2), HEAD, False, True),
    ("lop", (30, 0), (-3.3, -3, -2), (1, 4, 2), HEAD, False, True),
    ("horn", (38, 0), (-2.2, -5, -1.5), (2, 2, 5), HEAD, False, False),
    ("beard", (54, 0), (-1, 1, -5), (2, 4, 1), HEAD, False, True),
    ("wattle", (60, 0), (-2, 1, -1.5), (1, 3, 1), HEAD, False, True),
    ("leg", (0, 16), (-4, 12, 5.5), (3, 12, 3), ORIGIN, False, True),
    ("body", (28, 8), (-4, -10, -7), (8, 16, 6), BODY, True, True),
    ("udder", (0, 34), (-2, 1, -9), (4, 3, 3), BODY, True, False),
    ("tail", (16, 34), (-1, 0, 7.5), (2, 4, 1), ORIGIN, False, True),
]

RED = (138, 64, 34)
BLACK = (34, 30, 28)
WHITE = (238, 233, 224)


def skin(tex, grain):
    img = blank()
    px = img.load()
    for (x, y), (box, face, p, coat, st) in tex.items():
        if coat:
            c = int(round(255 * (0.93 + 0.07 * grain(x, y)) * (0.9 + 0.1 * shade(face))))
            px[x, y] = (c, c, c, 255)
    return img


def paint(tex, grain, where, rgb):
    img = blank()
    px = img.load()
    for (x, y), (box, face, p, coat, st) in tex.items():
        if coat and where(box, face, p, st):
            g = 0.88 + 0.12 * grain(x, y)
            px[x, y] = (int(rgb[0] * g), int(rgb[1] * g), int(rgb[2] * g), 255)
    return img


def head_part(box):
    return box in ("head", "ear", "lop", "beard", "wattle")


def face_stripe(box, face, p, st):
    """A narrow stripe down each side of the face, from the eye forward to the muzzle, and the matching edges of the front face."""
    if box != "head" or not 0.2 < st[1] < 0.9:
        return False
    if face == "front":
        return st[0] < 0.2 or st[0] > 0.8
    if face in ("west", "east"):
        s = st[0] if face == "west" else 1 - st[0]
        return s < 0.45 and abs(st[1] - (0.25 + s)) < 0.18
    return False


def belly(box, p):
    return box == "body" and p[1] > 10.5


def lower_leg(box, p):
    return box == "leg" and p[1] > 17


def dorsal(box, p):
    return box == "body" and p[1] < 6.6 and abs(p[0]) < 1.2


def shoulder(box, p):
    return box == "body" and -7.5 < p[2] < -5.5 and p[1] < 9.5


def bezoar(box, face, p, st):
    return face_stripe(box, face, p, st) or belly(box, p) or lower_leg(box, p) or dorsal(box, p) or shoulder(box, p) or box == "beard"


def badgerface(box, face, p, st):
    return face_stripe(box, face, p, st) or belly(box, p) or lower_leg(box, p) or box == "beard"


def swiss(box, face, p, st):
    return face_stripe(box, face, p, st) or lower_leg(box, p) or (box == "body" and p[2] > 8.5) or box == "tail" or (box in ("ear", "lop") and face == "front")


def boer(tex, grain, full):
    img = blank()
    px = img.load()
    n = Noise(5150)
    for (x, y), (box, face, p, coat, st) in tex.items():
        if not coat:
            continue
        neck = box == "body" and p[2] < -6.8
        red = head_part(box) or neck
        if full:
            white = not red
        else:
            belt = box == "body" and -1.5 < p[2] < 3.0
            patch = box in ("body", "leg") and n.at(p[0] / 3.0, p[1] / 3.0, p[2] / 3.0) > 0.62
            white = not red and (belt or patch)
        rgb = RED if red else WHITE if white else None
        if rgb:
            g = 0.88 + 0.12 * grain(x, y)
            px[x, y] = (int(rgb[0] * g), int(rgb[1] * g), int(rgb[2] * g), 255)
    return img


def detail(tex):
    img = blank()
    px = img.load()
    for (x, y), (box, face, p, coat, st) in tex.items():
        if box == "horn":
            ridge = (x + y) % 2 == 0
            px[x, y] = (150, 140, 124, 255) if ridge else (124, 114, 100, 255)
        elif box == "udder":
            px[x, y] = (222, 170, 160, 255)
        elif box == "leg" and face != "top" and p[1] >= 22.5:
            px[x, y] = (46, 40, 36, 255)
    # the head's front face is u 7..11, v 7..11: eyes at its top corners, the nose low in the middle
    for ex in (7, 11):
        px[ex, 8] = (20, 16, 14, 255)
    for nx in (8, 9, 10):
        px[nx, 10] = (96, 72, 68, 255)
        px[nx, 11] = (64, 48, 46, 255)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    grain = hair_grain()
    tex = texels(SKIN)
    skin(tex, grain).save(os.path.join(OUT, "skin.png"))
    paint(tex, grain, bezoar, BLACK).save(os.path.join(OUT, "pattern_bezoar.png"))
    paint(tex, grain, badgerface, BLACK).save(os.path.join(OUT, "pattern_badgerface.png"))
    paint(tex, grain, swiss, WHITE).save(os.path.join(OUT, "pattern_swiss.png"))
    boer(tex, grain, True).save(os.path.join(OUT, "redhead_2.png"))
    boer(tex, grain, False).save(os.path.join(OUT, "redhead_1.png"))
    detail(tex).save(os.path.join(OUT, "detail.png"))
    print("wrote", len(os.listdir(OUT)), "goat textures")


if __name__ == "__main__":
    main()

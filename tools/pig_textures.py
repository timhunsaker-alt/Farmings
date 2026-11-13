#!/usr/bin/env python3
"""Draws the pig textures for the gene-driven pig model (common/.../client/PigModels.java; docs/animals/pigs.md, "Coat genes").
Drawn from scratch (no Mojang pixels; CC0); deterministic.

    python3 tools/pig_textures.py

Output, in common/src/main/resources/assets/farmings/textures/entity/pig/: one texture a PigLook colour: white (Large White),
red (Duroc), black, berkshire (black with six white points: face, four socks, tail tip), spotted (black spots on red), wild (brown).
"""
import os
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from cattle_textures import Noise, hair_grain, shade  # noqa: E402
from sheep_textures import texels, blank  # noqa: E402

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "pig")
HEAD = (0.0, 13.0, -7.0)
BODY = (0.0, 11.0, 2.0)
ORIGIN = (0.0, 0.0, 0.0)
SKIN = [
    ("head", (0, 0), (-4, -4, -6), (8, 7, 6), HEAD, False, True),
    ("snout", (28, 0), (-2, -0.5, -8), (4, 3, 2), HEAD, False, False),
    ("ear", (40, 0), (-3.5, -7, -2), (2, 3, 1), HEAD, False, True),
    ("droop", (48, 0), (-4, -4, -4), (3, 4, 1), HEAD, False, True),
    ("tail", (58, 0), (-0.5, 6, 8.5), (1, 3, 1), ORIGIN, False, True),
    ("leg", (0, 16), (-5, 18, 5), (4, 6, 4), ORIGIN, False, True),
    ("body", (24, 14), (-5, -10, -7), (10, 16, 8), BODY, True, True),
]
COLOURS = {
    "white": (238, 214, 206),
    "red": (156, 74, 40),
    "black": (36, 32, 32),
    "wild": (104, 78, 56),
    "berkshire": (36, 32, 32),
    "spotted": (180, 110, 70),
}
PINK = (228, 160, 160)
HOOF = (60, 50, 46)


def draw(name, tex, grain):
    img = blank()
    px = img.load()
    base = COLOURS[name]
    spots = Noise(812)
    for (x, y), (box, face, p, coat, st) in tex.items():
        c = base
        if box == "snout":
            c = PINK if name in ("white",) else (90, 70, 66) if name in ("black", "berkshire", "spotted") else (170, 110, 90)
        elif box == "leg" and p[1] >= 23:
            c = HOOF
        elif name == "berkshire" and (box == "tail" and p[1] > 7 or box == "leg" and p[1] > 20 or box == "head" and face == "front" and 0.3 < st[0] < 0.7):
            c = (236, 232, 226)
        elif name == "spotted" and box in ("body", "head") and spots.at(p[0] / 3, p[1] / 3, p[2] / 3) > 0.62:
            c = (36, 32, 32)
        g = 0.88 + 0.12 * grain(x, y)
        sh = 0.9 + 0.1 * shade(face)
        px[x, y] = (int(c[0] * g * sh), int(c[1] * g * sh), int(c[2] * g * sh), 255)
    # eyes on the head's front face (u 6..13, v 6..12)
    for ex in (7, 12):
        px[ex, 8] = (16, 12, 12, 255)
    img.save(os.path.join(OUT, name + ".png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    grain = hair_grain()
    tex = texels(SKIN)
    for name in COLOURS:
        draw(name, tex, grain)
    print("wrote", len(COLOURS), "pig textures")


if __name__ == "__main__":
    main()

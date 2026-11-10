#!/usr/bin/env python3
"""Draws the sheep textures for the gene-driven sheep models (common/.../client/SheepModels.java; docs/animals/sheep.md, "Coat genes").
Everything is drawn here from scratch (no Mojang pixels are copied; the repo is CC0). Deterministic: same output every run.

    python3 tools/sheep_textures.py

Output, in common/src/main/resources/assets/farmings/textures/entity/sheep/:
    skin.png              short hair and skin, near white so the renderer's tint (the colour from the genes) shows true
    head_black.png        the black head and legs of a Suffolk or Dorper, drawn over a white sheep
    head_mottled.png      the mottled head of a cross between a black-headed and a white-faced breed
    spots_skin_<0-3>      white spots on a spotted coloured sheep (four shapes)
    detail.png            eyes, nose, hooves, horns, udder: drawn over everything and never tinted
    fleece_<0-3>          the fleece by crimp (0 open and coarse ... 3 fine and dense, Merino-like), near white for the wool tint
    spots_fleece_<0-3>    the same spots on the fleece, cut from the same noise so they line up with the skin's
The spots are cut from 3D noise sampled at each texel's place on the model, as tools/cattle_textures.py does for cattle.
"""
import math
import os
import sys
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from cattle_textures import Noise, hair_grain, shade  # noqa: E402  (shared helpers)

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "sheep")
SPOT_SHAPES = 4

HEAD = (0.0, 6.0, -8.0)
BODY = (0.0, 5.0, 2.0)
ORIGIN = (0.0, 0.0, 0.0)
# name, (u, v), box min (x, y, z) in the part's frame, size, part pivot, rotated (the body lies on its side), coat?
SKIN = [
    ("head", (0, 0), (-3, -4, -6), (6, 6, 8), HEAD, False, True),
    ("ear", (28, 0), (-6, -3, -3), (3, 1, 2), HEAD, False, True),
    ("horn", (40, 0), (-4.5, -4.5, -2), (2, 2, 5), HEAD, False, False),
    ("curl", (54, 0), (-4.5, -3, 1.5), (2, 4, 2), HEAD, False, False),
    ("leg", (0, 16), (-5, 12, 5), (4, 12, 4), ORIGIN, False, True),
    ("body", (28, 8), (-4, -10, -7), (8, 16, 6), BODY, True, True),
    ("udder", (0, 34), (-2, 1, -9), (4, 3, 3), BODY, True, False),
    ("tail", (16, 34), (-0.5, 6.5, 7.5), (1, 8, 1), ORIGIN, False, True),
]
FLEECE = [
    ("cap", (0, 0), (-3, -4, -4), (6, 3, 6), HEAD, False, True),
    ("body", (28, 8), (-4, -10, -7), (8, 16, 6), BODY, True, True),
    ("leg", (0, 16), (-5, 12, 5), (4, 6, 4), ORIGIN, False, True),
]


def faces(box):
    """Each face of a box as (name, u0, v0, fw, fh, point(s, t)); point is in model space (same rules as cattle_textures.faces)."""
    _, (u, v), (x0, y0, z0), (w, h, d), pivot, rotated, _ = box
    w, h, d = int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d))
    fs = [
        ("top", u + d, v, w, d, lambda s, t: (x0 + s * w, y0, z0 + t * d)),
        ("bottom", u + d + w, v, w, d, lambda s, t: (x0 + s * w, y0 + h, z0 + t * d)),
        ("west", u, v + d, d, h, lambda s, t: (x0, y0 + t * h, z0 + s * d)),
        ("front", u + d, v + d, w, h, lambda s, t: (x0 + s * w, y0 + t * h, z0)),
        ("east", u + d + w, v + d, d, h, lambda s, t: (x0 + w, y0 + t * h, z0 + (1 - s) * d)),
        ("back", u + 2 * d + w, v + d, w, h, lambda s, t: (x0 + (1 - s) * w, y0 + t * h, z0 + d)),
    ]
    out = []
    for name, fu, fv, fw, fh, fn in fs:
        def point(s, t, fn=fn):
            x, y, z = fn(s, t)
            if rotated:
                x, y, z = x, -z, y
            return (x + pivot[0], y + pivot[1], z + pivot[2])
        out.append((name, fu, fv, fw, fh, point))
    return out


def texels(boxes):
    """(x, y) -> (box, face, model-space point, coat?, (s, t) on the face)."""
    seen = {}
    for box in boxes:
        for fname, fu, fv, fw, fh, point in faces(box):
            for j in range(fh):
                for i in range(fw):
                    x, y = fu + i, fv + j
                    if 0 <= x < 64 and 0 <= y < 64 and (x, y) not in seen:
                        s, t = (i + 0.5) / fw, (j + 0.5) / fh
                        seen[(x, y)] = (box[0], fname, point(s, t), box[6], (s, t))
    return seen


def blank():
    return Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def skin(tex, grain):
    img = blank()
    px = img.load()
    for (x, y), (box, face, p, coat, st) in tex.items():
        if coat:
            c = int(round(255 * (0.84 + 0.16 * grain(x, y)) * shade(face)))
            px[x, y] = (c, c, c, 255)
    return img


def dark_area(box, p):
    """Where a black head and legs show: the head and ears, and the legs below the body."""
    if box in ("head", "ear"):
        return True
    return box == "leg" and p[1] >= 15.0


def head_colour(tex, grain, mottled):
    img = blank()
    px = img.load()
    n = Noise(4242)
    for (x, y), (box, face, p, coat, st) in tex.items():
        if not coat or not dark_area(box, p):
            continue
        if mottled and n.at(p[0] / 1.6, p[1] / 1.6, p[2] / 1.6) < 0.5:
            continue
        c = 0.13 + 0.07 * grain(x, y)
        px[x, y] = (int(255 * c * 1.05), int(255 * c), int(255 * c * 0.95), 255)
    return img


def spot_field(seed):
    n1, n2 = Noise(seed), Noise(seed + 900)
    return lambda p: 0.75 * n1.at(p[0] / 7.0, p[1] / 7.0, p[2] / 7.0) + 0.25 * n2.at(p[0] / 3.0, p[1] / 3.0, p[2] / 3.0)


def spots(tex, grain, field, cover, base):
    vals = sorted(field(p) for (b, f, p, coat, st) in tex.values() if coat)
    cut = vals[min(len(vals) - 1, int(round((1 - cover) * len(vals))))]
    img = blank()
    px = img.load()
    for (x, y), (box, face, p, coat, st) in tex.items():
        if coat and field(p) >= cut:
            c = int(round(255 * (base + (1 - base) * grain(x, y)) * (0.94 + 0.06 * shade(face))))
            px[x, y] = (c, c, c, 255)
    return img


def detail(tex):
    img = blank()
    px = img.load()
    for (x, y), (box, face, p, coat, st) in tex.items():
        if box in ("horn", "curl"):
            ridge = (x + y) % 2 == 0
            px[x, y] = (196, 178, 140, 255) if ridge else (172, 152, 116, 255)
        elif box == "udder":
            px[x, y] = (226, 176, 166, 255)
        elif box == "leg" and face != "top" and p[1] >= 22.5:
            px[x, y] = (52, 44, 38, 255)
    # the head's front face is u 8..13, v 8..13: eyes at its side columns, the nose low in the middle
    for ex in (8, 13):
        px[ex, 10] = (22, 18, 16, 255)
    for nx in (10, 11):
        px[nx, 12] = (110, 82, 78, 255)
        px[nx, 13] = (70, 52, 50, 255)
    return img


def fleece(tex, grain, crimp):
    """Wool: soft locks from noise, with crimp waves along the staple; finer and denser crimp for a higher gene value."""
    img = blank()
    px = img.load()
    locks = Noise(700 + crimp)
    fine = Noise(800 + crimp)
    freq = [0.0, 1.2, 2.0, 3.1][crimp]
    size = [2.6, 2.0, 1.5, 1.1][crimp]
    for (x, y), (box, face, p, coat, st) in tex.items():
        lock = locks.at(p[0] / size, p[1] / size, p[2] / size)
        wave = 0.5 + 0.5 * math.sin((p[0] + p[2]) * freq * 2.2 + 3.0 * lock) if freq > 0 else 0.5
        v = 0.60 + 0.24 * lock + 0.08 * fine.at(p[0] * 1.3, p[1] * 1.3, p[2] * 1.3) + 0.08 * wave
        v *= 0.92 + 0.08 * shade(face)
        c = int(round(255 * min(1.0, v)))
        px[x, y] = (c, c, c, 255)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    grain = hair_grain()
    tex = texels(SKIN)
    skin(tex, grain).save(os.path.join(OUT, "skin.png"))
    head_colour(tex, grain, False).save(os.path.join(OUT, "head_black.png"))
    head_colour(tex, grain, True).save(os.path.join(OUT, "head_mottled.png"))
    detail(tex).save(os.path.join(OUT, "detail.png"))
    ftex = texels(FLEECE)
    for shape in range(SPOT_SHAPES):
        field = spot_field(300 + shape)
        spots(tex, grain, field, 0.35, 0.88).save(os.path.join(OUT, f"spots_skin_{shape}.png"))
        spots(ftex, grain, field, 0.35, 0.86).save(os.path.join(OUT, f"spots_fleece_{shape}.png"))
    for crimp in range(4):
        fleece(ftex, grain, crimp).save(os.path.join(OUT, f"fleece_{crimp}.png"))
    print("wrote", len(os.listdir(OUT)), "sheep textures")


if __name__ == "__main__":
    main()

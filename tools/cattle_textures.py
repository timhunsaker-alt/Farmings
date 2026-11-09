#!/usr/bin/env python3
"""Draws the cattle textures for the gene-driven cattle model (common/.../client/CattleModels.java; docs/animals/cattle.md, "Models and
coats"). Everything is drawn here from scratch (no Mojang pixels are copied; the repo is CC0). Deterministic: same output every run.

    python3 tools/cattle_textures.py

Output, in common/src/main/resources/assets/farmings/textures/entity/cattle/:
    coat.png                       the hair, near white so the renderer's tint (the coat colour from the genes) shows true
    detail.png                     eyes, muzzle, horns, hooves, udder: drawn over everything and never tinted
    white/patch_<level>_<shape>    piebald patches, 4 white levels x 8 shapes (Holstein)
    white/speckle_<level>_<shape>  speckles and mottling, 4 levels x 4 shapes (Gyr)
    white/sided_<level>            colour-sided: white along the back line and belly, 4 levels
    white/face_<1-3>               star, blaze, white face
A white level is the white_coverage gene / 250 (1-4). The patches are cut from 3D noise sampled at each texel's place on the model, so a
patch runs on across the edge of a box face instead of stopping at it. Needs Pillow. The hair grain is the Ludo cow-hair swatch
(art/final/hair.png, from tools/process_art.py).
"""
import math
import os
import random
from PIL import Image

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "cattle")
HAIR = os.path.join(HERE, "..", "art", "final", "hair.png")
PATCH_SHAPES, SPECKLE_SHAPES = 8, 4
COVER = {1: 0.25, 2: 0.50, 3: 0.72, 4: 0.92}

# The boxes of CattleModels.createLayer: name, (u, v), (x0, y0, z0), (w, h, d), part pivot, rotated (the body lies on its side), coat?
HEAD = (0.0, 4.0, -8.0)
BODY = (0.0, 5.0, 2.0)
BOXES = [
    ("head", (0, 0), (-4, -4, -6), (8, 8, 6), HEAD, False, True),
    ("muzzle", (28, 0), (-3, 1, -7), (6, 3, 1), HEAD, False, False),
    ("horn_r", (44, 0), (-5, -6, -5), (1, 3, 1), HEAD, False, False),
    ("horn_l", (48, 0), (4, -6, -5), (1, 3, 1), HEAD, False, False),
    ("ear", (52, 0), (-7, -3, -3), (3, 1, 2), HEAD, False, True),
    ("long_ear", (52, 4), (-5, -2, -4), (1, 5, 2), HEAD, False, True),
    ("leg", (0, 16), (-6, 12, -7), (4, 12, 4), (0, 0, 0), False, True),
    ("body", (18, 14), (-6, -10, -7), (12, 18, 10), BODY, True, True),
    ("udder", (0, 42), (-2, 2, -8), (4, 6, 1), BODY, True, False),
    ("crest", (10, 42), (-3, -10, 3), (6, 8, 2), BODY, True, True),
    ("dewlap", (48, 42), (-0.5, -10, -11), (1, 6, 4), BODY, True, True),
    ("hump_l", (0, 54), (-3, -9, 3), (6, 7, 3), BODY, True, True),
    ("hump_m", (18, 54), (-3, -8, 3), (6, 5, 2), BODY, True, True),
    ("hump_s", (34, 54), (-2.5, -7, 3), (5, 3, 2), BODY, True, True),
]


def faces(box):
    """Each face of a box as (name, u0, v0, fw, fh, point(s, t)) with s, t in 0..1 across the face; point is in model space."""
    _, (u, v), (x0, y0, z0), (w, h, d), pivot, rotated, _ = box
    w, h, d = int(math.ceil(w)), int(h), int(d)
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
            if rotated:     # a quarter turn about x: local +y runs to the rump (world +z), local +z is up (world -y)
                x, y, z = x, -z, y
            return (x + pivot[0], y + pivot[1], z + pivot[2])
        out.append((name, fu, fv, fw, fh, point))
    return out


def texels():
    """Every painted texel: (x, y) -> (box name, face name, model-space point, coat?)."""
    seen = {}
    for box in BOXES:
        for fname, fu, fv, fw, fh, point in faces(box):
            for j in range(fh):
                for i in range(fw):
                    x, y = fu + i, fv + j
                    if 0 <= x < 64 and 0 <= y < 64 and (x, y) not in seen:
                        seen[(x, y)] = (box[0], fname, point((i + 0.5) / fw, (j + 0.5) / fh), box[6])
    return seen


class Noise:
    """Smooth 3D value noise on a seeded lattice."""

    def __init__(self, seed):
        self.rnd = random.Random(seed)
        self.table = {}

    def lattice(self, i, j, k):
        key = (i, j, k)
        if key not in self.table:
            self.table[key] = self.rnd.random()
        return self.table[key]

    def at(self, x, y, z):
        i, j, k = math.floor(x), math.floor(y), math.floor(z)
        fx, fy, fz = x - i, y - j, z - k
        sx, sy, sz = (f * f * (3 - 2 * f) for f in (fx, fy, fz))
        total = 0.0
        for di in (0, 1):
            for dj in (0, 1):
                for dk in (0, 1):
                    w = (sx if di else 1 - sx) * (sy if dj else 1 - sy) * (sz if dk else 1 - sz)
                    total += w * self.lattice(i + di, j + dj, k + dk)
        return total


def hair_grain():
    hair = Image.open(HAIR).convert("L")
    px = hair.load()
    vals = [px[i, j] for j in range(hair.height) for i in range(hair.width)]
    lo, hi = min(vals), max(vals)
    return lambda x, y: (px[x % hair.width, y % hair.height] - lo) / max(1, hi - lo)


def shade(face):
    return {"top": 1.0, "bottom": 0.84, "front": 0.95, "back": 0.95, "west": 0.92, "east": 0.92}[face]


def coat(tex, grain):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    for (x, y), (box, face, _, is_coat) in tex.items():
        c = int(round(255 * (0.80 + 0.20 * grain(x, y)) * shade(face)))
        px[x, y] = (c, c, c, 255)
    return img


def white_mask(tex, grain, field, cover):
    """White hair where field(point) is in the top `cover` share of the coat texels."""
    vals = sorted(field(p) for (box, face, p, is_coat) in tex.values() if is_coat)
    cut = vals[min(len(vals) - 1, max(0, int(round((1 - cover) * len(vals)))))]
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    for (x, y), (box, face, p, is_coat) in tex.items():
        if is_coat and field(p) >= cut:
            c = int(round(255 * (0.90 + 0.10 * grain(x, y)) * (0.94 + 0.06 * shade(face))))
            px[x, y] = (c, c, c, 255)
    return img


def patch_field(seed):
    n1, n2 = Noise(seed), Noise(seed + 1000)

    def f(p):
        x, y, z = p
        # big soft patches, a finer edge, and more white low down (belly and legs), as on most Holsteins
        return 0.75 * n1.at(x / 7.0, y / 7.0, z / 7.0) + 0.25 * n2.at(x / 3.0, y / 3.0, z / 3.0) + 0.012 * y
    return f


def speckle_field(seed):
    n1, n2 = Noise(seed), Noise(seed + 2000)

    def f(p):
        x, y, z = p
        # fine speckle over a broad mottle: the Gyr's red-and-white look
        return 0.55 * n1.at(x / 2.0, y / 2.0, z / 2.0) + 0.45 * n2.at(x / 6.0, y / 6.0, z / 6.0)
    return f


def sided_field():
    n = Noise(77)

    def f(p):
        x, y, z = p
        # white along the top line and the belly, darkest on the flanks
        top = max(0.0, -y + 2) / 12.0
        belly = max(0.0, y - 8) / 6.0
        return max(top * (1.0 - abs(x) / 7.0), belly) + 0.15 * n.at(x / 2.0, y / 2.0, z / 2.0)
    return f


def face_mask(tex, grain, kind):
    """Face white on the head's front (and the muzzle for a white face). kind 1 star, 2 blaze, 3 white face."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    for (x, y), (box, face, p, is_coat) in tex.items():
        if box not in ("head", "muzzle"):
            continue
        hx, hy, hz = p[0] - HEAD[0], p[1] - HEAD[1], p[2] - HEAD[2]   # back in the head's own frame
        white = False
        if kind == 1:
            white = face == "front" and box == "head" and abs(hx) <= 1.2 and -3.2 <= hy <= -1.0
        elif kind == 2:
            white = face == "front" and abs(hx) <= 1.3 and hy >= -3.5
        elif kind == 3:
            white = (face == "front" or box == "muzzle" or (face == "top" and hz < -3)) and not (box == "head" and face == "front" and abs(hx) > 3.2 and hy < -0.5)
        if white:
            c = int(round(255 * (0.90 + 0.10 * grain(x, y))))
            px[x, y] = (c, c, c, 255)
    return img


def detail(tex):
    """Untinted parts: eyes, muzzle, horns, hooves, udder skin."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    for (x, y), (box, face, p, is_coat) in tex.items():
        if box == "muzzle":
            px[x, y] = (88, 70, 68, 255)
        elif box.startswith("horn"):
            tip = p[1] - HEAD[1] < -5.3
            px[x, y] = (70, 62, 52, 255) if tip else (222, 210, 184, 255)
        elif box == "udder":
            px[x, y] = (222, 168, 160, 255)
        elif box == "leg" and face != "top" and p[1] >= 22.0:
            px[x, y] = (46, 40, 36, 255)
    # nostrils on the muzzle's front face (u 29..34, v 1..3), and the eyes on the head's front face (u 6..13, v 6..13)
    for nx in (30, 33):
        px[nx, 2] = (40, 30, 30, 255)
    for ex in (6, 13):
        px[ex, 8] = (24, 20, 20, 255)
        px[ex, 9] = (24, 20, 20, 255)
    # teats along the udder's bottom face
    for tx in (5, 6, 7, 8):
        if (tx, 42) in tex:
            px[tx, 42] = (196, 128, 124, 255)
    return img


def main():
    os.makedirs(os.path.join(OUT, "white"), exist_ok=True)
    tex = texels()
    grain = hair_grain()
    coat(tex, grain).save(os.path.join(OUT, "coat.png"))
    detail(tex).save(os.path.join(OUT, "detail.png"))
    for level, cover in COVER.items():
        for shape in range(PATCH_SHAPES):
            white_mask(tex, grain, patch_field(100 + shape), cover).save(os.path.join(OUT, "white", f"patch_{level}_{shape}.png"))
        for shape in range(SPECKLE_SHAPES):
            white_mask(tex, grain, speckle_field(500 + shape), cover * 0.8).save(os.path.join(OUT, "white", f"speckle_{level}_{shape}.png"))
        white_mask(tex, grain, sided_field(), cover * 0.6).save(os.path.join(OUT, "white", f"sided_{level}.png"))
    for kind in (1, 2, 3):
        face_mask(tex, grain, kind).save(os.path.join(OUT, "white", f"face_{kind}.png"))
    print("wrote coat, detail and", len(os.listdir(os.path.join(OUT, "white"))), "white masks")


if __name__ == "__main__":
    main()

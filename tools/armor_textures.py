#!/usr/bin/env python3
"""Draws the worn milk armor (docs/milk-armor.md) and cheese armor (docs/cheese-armor.md) on the vanilla armor layer layout (64x32): the Ludo milk surfaces in art/source
(mat_milk_armor.webp fresh, mat_milk_spoiled.webp spoiled; prompts in art/source/PROMPTS.md) laid face by face on each box, with edge
shading, a highlight along the top, drips at the bottom edges, and an opening in the helmet for the face. Deterministic.

    python3 tools/armor_textures.py
Writes textures/entity/equipment/humanoid/milk(_spoiled).png and humanoid_leggings/milk(_spoiled).png.

Cheese armor (paint_cheese) must read as cheese from a distance (Tim, 2026-11-25: the first pass read brown, like cork). So it is a
bright, warm cheddar yellow-orange whose fine grain only is taken from mat_cheese_armor.webp, a subtle deeper rind along each face's
edge, and a few large round eyes drawn as shallow hollows with a lighter rim (Swiss-style eyes read better than cheddar's at 16 px a
block). The moldy set is the same cheese, a little duller, with blue-green mould spots (their colours sampled from mat_cheese_moldy.webp)
and a pale fuzzy edge, so the change reads as cheese gone bad. Writes cheese(_moldy).png in both layers. Needs Pillow.
"""
import os
import random
from PIL import Image

HERE = os.path.dirname(__file__)
SRC = os.path.join(HERE, "..", "art", "source")
EQ = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "equipment")

# vanilla armor boxes: (u, v), (w, h, d)
HEAD = ((0, 0), (8, 8, 8))
BODY = ((16, 16), (8, 12, 4))
ARM = ((40, 16), (4, 12, 4))
LEG = ((0, 16), (4, 12, 4))


def faces(box):
    (u, v), (w, h, d) = box
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "west": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "east": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def clamp(c):
    return max(0, min(255, int(round(c))))


def paint(img, src, box, rnd, spoiled, helmet=False):
    px, sp = img.load(), src.load()
    sw, sh = src.size
    for name, (fu, fv, fw, fh) in faces(box).items():
        ox, oy = rnd.randrange(sw), rnd.randrange(sh)
        drips = {c: rnd.randint(1, 3) for c in range(fw) if rnd.random() < 0.35} if name not in ("top", "bottom") else {}
        for j in range(fh):
            for i in range(fw):
                r, g, b = sp[(ox + i * 3) % sw, (oy + j * 3) % sh][:3]
                k = 1.0
                if i == 0 or i == fw - 1:
                    k *= 0.88                                  # the face's side edges fall away
                if name not in ("top", "bottom") and j == 0:
                    k *= 1.08                                  # light catches the top edge
                if name not in ("top", "bottom") and j >= fh - drips.get(i, 0):
                    k *= 0.93 if spoiled else 1.05             # a drip running off the bottom edge
                if name == "bottom":
                    k *= 0.85
                if spoiled and rnd.random() < 0.05:
                    r, g, b = r * 0.75, g * 0.82, b * 0.45     # a mould spot
                px[fu + i, fv + j] = (clamp(r * k), clamp(g * k), clamp(b * k), 255)
        if helmet and name == "front":
            for j in range(3, 7):                              # the face opening: eyes, nose, mouth
                for i in range(1, fw - 1):
                    px[fu + i, fv + j] = (0, 0, 0, 0)


CHEDDAR = (255, 202, 84)          # bright, warm cheddar yellow-orange
RIND = (238, 160, 52)             # a subtle deeper orange along the edges
MOULD_EDGE = (176, 206, 186)      # the pale fuzz round a mould spot


def pixels(img):
    px = img.load()
    return [px[x, y] for y in range(img.height) for x in range(img.width)]


def grain(src):
    """The cheese surface's fine light and shade, around 1.0, with its own holes and specks evened out."""
    lum = [sum(p[:3]) / 3 for p in pixels(src)]
    mean = sum(lum) / len(lum)
    return [max(0.93, min(1.07, 1 + (v / mean - 1) * 0.35)) for v in lum], src.size


def mould_colours(src):
    """The blue-green of the Ludo mouldy cheese, picked from its bluish pixels."""
    out = [p[:3] for p in pixels(src) if p[2] > p[0] + 10 and p[1] > p[0]]
    return out or [(70, 135, 125)]


def paint_cheese(img, gr, box, rnd, moldy, colours, helmet=False):
    px = img.load()
    vals, (sw, sh) = gr
    base = tuple(c * 0.94 for c in CHEDDAR) if moldy else CHEDDAR
    for name, (fu, fv, fw, fh) in faces(box).items():
        ox, oy = rnd.randrange(sw), rnd.randrange(sh)
        cell = {}
        for j in range(fh):
            for i in range(fw):
                k = vals[((oy + j * 3) % sh) * sw + (ox + i * 3) % sw]
                # the rind: a subtle deeper orange along the bottom edge and, half as strong, the side edges
                c = base
                if name not in ("top", "bottom") and (j == fh - 1 or i in (0, fw - 1)):
                    w = 1.0 if j == fh - 1 else 0.4
                    c = tuple(b + (r - b) * w for b, r in zip(base, RIND))
                if name not in ("top", "bottom") and j == 0:
                    k *= 1.05                                  # light along the top edge
                if name == "bottom":
                    k *= 0.86
                cell[i, j] = [c[0] * k, c[1] * k, c[2] * k]
        # eyes: a few large round shallow hollows, darker inside at the top (in shadow), lighter on the lower rim. A wide face gets a round
        # 3 px eye (a 3 x 3 without its corners), a narrow one a 2 px eye.
        if fw >= 4 and fh >= 4:
            big = fw >= 6 and fh >= 6
            for _ in range(max(1, fw * fh // 40) if rnd.random() < 0.8 else 0):
                ex, ey = rnd.randrange(1, fw - (3 if big else 2)), rnd.randrange(1, fh - (3 if big else 2))
                if helmet and name == "front" and 1 <= ey <= 7:
                    continue
                if big:
                    hollow = (((1, 0), 0.74), ((0, 1), 0.80), ((1, 1), 0.82), ((2, 1), 0.84), ((1, 2), 0.90))
                    rim = ((0, 2), (2, 2), (1, 3))
                else:
                    hollow = (((0, 0), 0.74), ((1, 0), 0.80), ((0, 1), 0.86), ((1, 1), 0.90))
                    rim = ((0, 2), (1, 2), (2, 1))
                for (dx, dy), k in hollow:
                    cell[ex + dx, ey + dy] = [v * k for v in base]
                for dx, dy in rim:
                    if (ex + dx, ey + dy) in cell:
                        cell[ex + dx, ey + dy] = [min(255, v * 1.08) for v in base]
        if moldy:
            for _ in range(max(1, fw * fh // 14)):
                mx, my = rnd.randrange(fw), rnd.randrange(fh)
                cell[mx, my] = list(rnd.choice(colours))
                for dx, dy in rnd.sample([(1, 0), (-1, 0), (0, 1), (0, -1)], 2):
                    if (mx + dx, my + dy) in cell and rnd.random() < 0.7:
                        cell[mx + dx, my + dy] = list(MOULD_EDGE)
        for (i, j), (r, g, b) in cell.items():
            px[fu + i, fv + j] = (clamp(r), clamp(g), clamp(b), 255)
        if helmet and name == "front":
            for j in range(3, 7):                              # the face opening
                for i in range(1, fw - 1):
                    px[fu + i, fv + j] = (0, 0, 0, 0)


def source(name):
    img = Image.open(os.path.join(SRC, name)).convert("RGB")
    return img.resize((96, 96), Image.LANCZOS)


def main():
    for spoiled in (False, True):
        src = source("mat_milk_spoiled.webp" if spoiled else "mat_milk_armor.webp")
        suffix = "_spoiled" if spoiled else ""
        rnd = random.Random(7 if spoiled else 3)
        top = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        paint(top, src, HEAD, rnd, spoiled, helmet=True)
        paint(top, src, BODY, rnd, spoiled)
        paint(top, src, ARM, rnd, spoiled)
        paint(top, src, LEG, rnd, spoiled)       # boots use the leg box of the outer layer
        top.save(os.path.join(EQ, "humanoid", "milk" + suffix + ".png"))
        legs = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        paint(legs, src, BODY, rnd, spoiled)
        paint(legs, src, LEG, rnd, spoiled)
        legs.save(os.path.join(EQ, "humanoid_leggings", "milk" + suffix + ".png"))
    gr = grain(source("mat_cheese_armor.webp"))
    colours = mould_colours(source("mat_cheese_moldy.webp"))
    for moldy in (False, True):
        suffix = "_moldy" if moldy else ""
        rnd = random.Random(5)                 # the same eyes in both sets: the moldy set is the same cheese gone bad
        top = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        paint_cheese(top, gr, HEAD, rnd, moldy, colours, helmet=True)
        paint_cheese(top, gr, BODY, rnd, moldy, colours)
        paint_cheese(top, gr, ARM, rnd, moldy, colours)
        paint_cheese(top, gr, LEG, rnd, moldy, colours)
        top.save(os.path.join(EQ, "humanoid", "cheese" + suffix + ".png"))
        legs = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        paint_cheese(legs, gr, BODY, rnd, moldy, colours)
        paint_cheese(legs, gr, LEG, rnd, moldy, colours)
        legs.save(os.path.join(EQ, "humanoid_leggings", "cheese" + suffix + ".png"))
    print("wrote the milk and cheese armor layers")


if __name__ == "__main__":
    main()

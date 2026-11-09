#!/usr/bin/env python3
"""Draws the worn milk armor (docs/milk-armor.md) on the vanilla armor layer layout (64x32): the Ludo milk surfaces in art/source
(mat_milk_armor.webp fresh, mat_milk_spoiled.webp spoiled; prompts in art/source/PROMPTS.md) laid face by face on each box, with edge
shading, a highlight along the top, drips at the bottom edges, and an opening in the helmet for the face. Deterministic.

    python3 tools/armor_textures.py
Writes textures/entity/equipment/humanoid/milk(_spoiled).png and humanoid_leggings/milk(_spoiled).png. Needs Pillow.
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
    print("wrote the milk armor layers")


if __name__ == "__main__":
    main()

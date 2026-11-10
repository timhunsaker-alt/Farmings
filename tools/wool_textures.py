#!/usr/bin/env python3
"""Draws the worn wool clothing (docs/wool.md) on the vanilla armor layer layout (64x32): a cream knit with V stitches in rows, ribbed
cuffs at the bottom edges, a cable down the front of the sweater, and the face left open in the cap. Drawn from scratch; deterministic.

    python3 tools/wool_textures.py
Writes textures/entity/equipment/humanoid/wool.png and humanoid_leggings/wool.png. Needs Pillow.
"""
import os
import random
from PIL import Image

from armor_textures import HEAD, BODY, ARM, LEG, faces, clamp   # the same box layout as the milk armor

HERE = os.path.dirname(__file__)
EQ = os.path.join(HERE, "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "equipment")
BASE = (226, 216, 190)


def knit(img, box, rnd, cap=False, cable=False):
    px = img.load()
    for name, (fu, fv, fw, fh) in faces(box).items():
        for j in range(fh):
            for i in range(fw):
                x, y = fu + i, fv + j
                # a V stitch: two texels wide, darker on its outer edges
                v = 1.0 - (0.10 if (i + (j // 2)) % 2 == 0 else 0.0) - 0.04 * ((j % 2) == 1)
                if name not in ("top", "bottom") and j >= fh - 2:
                    v = 0.86 if i % 2 == 0 else 0.98        # ribbed cuff
                if cable and name == "front" and abs(i - fw / 2 + 0.5) < 1.5:
                    v = 1.05 if (j // 2) % 2 == 0 else 0.84  # the cable
                v *= 0.96 + 0.08 * rnd.random()
                if cap and name == "front" and j >= 2:
                    continue                                 # the face stays open
                px[x, y] = tuple(clamp(c * v) for c in BASE) + (255,)


def main():
    rnd = random.Random(5)
    top = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    knit(top, HEAD, rnd, cap=True)
    knit(top, BODY, rnd, cable=True)
    knit(top, ARM, rnd)
    knit(top, LEG, rnd)                 # the socks (boots use the outer layer's leg boxes)
    legs = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    knit(legs, BODY, rnd)
    knit(legs, LEG, rnd)
    for layer, img in (("humanoid", top), ("humanoid_leggings", legs)):
        os.makedirs(os.path.join(EQ, layer), exist_ok=True)
        img.save(os.path.join(EQ, layer, "wool.png"))
    print("wrote the wool clothing layers")


if __name__ == "__main__":
    main()

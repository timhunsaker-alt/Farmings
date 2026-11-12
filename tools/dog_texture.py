#!/usr/bin/env python3
"""Paints the Border Collie's coat (docs/animals/sheep.md, "Sheep dog") from scratch on the wolf model's 64 x 32 texture layout:
black with a white blaze, muzzle, collar and chest, white lower legs and a white tail tip. No vanilla art is copied; only the box layout
(where each face sits on the sheet) is shared, so the vanilla wolf model can wear it."""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity")
BLACK = (28, 26, 30)
BLACK2 = (40, 37, 42)
WHITE = (238, 236, 230)
NOSE = (20, 18, 20)
EYE = (60, 40, 20)


def faces(u, v, w, h, d):
    """The six faces of a box's net: name -> (x, y, width, height)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def fill(px, rect, colour_at):
    x0, y0, w, h = rect
    for j in range(h):
        for i in range(w):
            px[x0 + i, y0 + j] = colour_at(i, j, w, h) + (255,)


def coat(i, j, w, h):
    rnd = random.Random(i * 131 + j * 17 + w * 7 + h)
    return BLACK2 if rnd.random() < 0.18 else BLACK


def main():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    # head 6 x 6 x 4 at (0, 0): black with a white blaze down the middle of the face
    for name, rect in faces(0, 0, 6, 6, 4).items():
        fill(px, rect, coat)
    fx, fy, fw, fh = faces(0, 0, 6, 6, 4)["front"]
    for j in range(fh):
        for i in range(fw):
            if 2 <= i <= 3 or (j >= 4 and 1 <= i <= 4):
                px[fx + i, fy + j] = WHITE + (255,)
    px[fx + 1, fy + 2] = EYE + (255,)
    px[fx + 4, fy + 2] = EYE + (255,)
    # ears 2 x 2 x 1 at (16, 14): black
    for rect in faces(16, 14, 2, 2, 1).values():
        fill(px, rect, coat)
    # muzzle 3 x 3 x 4 at (0, 10): white, black nose at the tip
    for rect in faces(0, 10, 3, 3, 4).values():
        fill(px, rect, lambda i, j, w, h: WHITE)
    nx, ny, nw, nh = faces(0, 10, 3, 3, 4)["front"]
    for i in range(nw):
        px[nx + i, ny] = NOSE + (255,)
    # body 6 x 9 x 6 at (18, 14): black
    for rect in faces(18, 14, 6, 9, 6).values():
        fill(px, rect, coat)
    # mane / shoulders 8 x 6 x 7 at (21, 0): white collar and chest in front and below, black on the back
    for name, rect in faces(21, 0, 8, 6, 7).items():
        if name in ("front", "bottom"):
            fill(px, rect, lambda i, j, w, h: WHITE)
        elif name in ("right", "left"):
            fill(px, rect, lambda i, j, w, h: WHITE if i < 3 else coat(i, j, w, h))
        else:
            fill(px, rect, coat)
    # legs 2 x 8 x 2 at (0, 18): black above, white socks below
    for name, rect in faces(0, 18, 2, 8, 2).items():
        fill(px, rect, lambda i, j, w, h: WHITE if (name == "bottom" or (name not in ("top",) and j >= 4)) else coat(i, j, w, h))
    # tail 2 x 8 x 2 at (9, 18): black with a white tip
    for name, rect in faces(9, 18, 2, 8, 2).items():
        fill(px, rect, lambda i, j, w, h: WHITE if (name == "bottom" or (name not in ("top",) and j >= 6)) else coat(i, j, w, h))
    os.makedirs(ROOT, exist_ok=True)
    out = os.path.join(ROOT, "sheep_dog.png")
    img.save(out)
    print("wrote", os.path.normpath(out))


if __name__ == "__main__":
    main()

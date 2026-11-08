#!/usr/bin/env python3
"""Renders a cattle texture on the vanilla cow model's boxes (CowModel.createBaseCowModel) so the art can be judged without
launching the game. A flat software renderer: good enough to check the coat, the markings and the Gyr hump, not a screenshot.

    python3 tools/preview_cattle.py out.png holstein jersey ...
Texture-face rules follow Minecraft's cube UV mapping (u runs to the viewer's right looking at a face from outside, up = -y).
"""
import math
import os
import sys
from PIL import Image, ImageDraw

TEX = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "cattle")

# (u, v, x0, y0, z0, w, h, d, part) boxes; y is down, the cow faces -z
def rotx(p, a):
    x, y, z = p
    return (x, y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a))


def cows(hump):
    boxes = []
    boxes.append(((0, 0, -4, -4, -6, 8, 8, 6), (0, 4, -8), 0))      # head
    boxes.append(((1, 33, -3, 1, -7, 6, 3, 1), (0, 4, -8), 0))      # muzzle
    boxes.append(((22, 0, -5, -5, -5, 1, 3, 1), (0, 4, -8), 0))     # horns
    boxes.append(((22, 0, 4, -5, -5, 1, 3, 1), (0, 4, -8), 0))
    boxes.append(((18, 4, -6, -10, -7, 12, 18, 10), (0, 5, 2), math.pi / 2))   # body
    boxes.append(((52, 0, -2, 2, -8, 4, 6, 1), (0, 5, 2), math.pi / 2))        # udder
    if hump:
        boxes.append(((0, 40, -3, -9, 3, 6, 7, 3), (0, 5, 2), math.pi / 2))
    for (x, z) in ((-4, 7), (4, 7), (-4, -5), (4, -5)):
        boxes.append(((0, 16, -2, 0, -2, 4, 12, 4), (x, 12, z), 0))
    return boxes


def faces(box, offset, rot):
    (u, v, x0, y0, z0, w, h, d) = box
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    # (texture rect, origin corner, u axis vector, v axis vector, size in texels)
    spec = [
        ((u, v + d, d, h), (x0, y0, z1), (0, 0, -1), (0, 1, 0), d, h),        # west  (-x): u toward -z
        ((u + d + w, v + d, d, h), (x1, y0, z0), (0, 0, 1), (0, 1, 0), d, h),  # east  (+x): u toward +z
        ((u + d, v + d, w, h), (x0, y0, z0), (1, 0, 0), (0, 1, 0), w, h),      # north (-z): u toward +x
        ((u + d + w + d, v + d, w, h), (x1, y0, z1), (-1, 0, 0), (0, 1, 0), w, h),  # south (+z): u toward -x
        ((u + d, v, w, d), (x1, y0, z0), (-1, 0, 0), (0, 0, 1), w, d),         # top   (-y)
        ((u + d + w, v, w, d), (x0, y1, z0), (1, 0, 0), (0, 0, 1), w, d),      # bottom(+y)
    ]
    out = []
    for rect, o, ua, va, nu, nv in spec:
        n = (-(ua[1] * va[2] - ua[2] * va[1]), -(ua[2] * va[0] - ua[0] * va[2]), -(ua[0] * va[1] - ua[1] * va[0]))   # outward
        out.append((rect, o, ua, va, nu, nv, n))
    return out


def render(tex, hump, yaw, pitch, scale=13, size=(520, 360)):
    img = Image.new("RGBA", size, (226, 230, 232, 255))
    dr = ImageDraw.Draw(img)
    polys = []
    cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)

    def tf(p, off, rot):
        q = rotx(p, rot) if rot else p
        q = (q[0] + off[0], q[1] + off[1], q[2] + off[2])
        x, y, z = q
        x, z = x * cy + z * sy, -x * sy + z * cy           # yaw about the vertical axis
        y, z = y * cp - z * sp, y * sp + z * cp            # pitch
        return x, y, z

    for box, off, rot in cows(hump):
        for rect, o, ua, va, nu, nv, n in faces(box, off, rot):
            nn = rotx(n, rot) if rot else n
            x, y, z = nn
            x, z = x * cy + z * sy, -x * sy + z * cy
            y, z = y * cp - z * sp, y * sp + z * cp
            if z >= 0:                                      # facing away from the camera (camera looks toward +z)
                continue
            rx, ry, _, _ = rect
            for i in range(nu):
                for j in range(nv):
                    col = tex.getpixel((rx + i, ry + j))
                    if col[3] < 40:
                        continue
                    c = [tuple(o[k] + ua[k] * (i + a) + va[k] * (j + b) for k in range(3)) for a, b in ((0, 0), (1, 0), (1, 1), (0, 1))]
                    pts = [tf(pp, off, rot) for pp in c]
                    depth = sum(p[2] for p in pts) / 4
                    polys.append((depth, [(size[0] / 2 + (p[0] - 1) * scale, size[1] / 2 + (p[1] - 10) * scale) for p in pts], col))
    polys.sort(key=lambda t: -t[0])
    for depth, pts, col in polys:
        shade = 1.0
        dr.polygon(pts, fill=(int(col[0] * shade), int(col[1] * shade), int(col[2] * shade)), outline=(int(col[0] * shade), int(col[1] * shade), int(col[2] * shade)))
    return img


def main():
    out = sys.argv[1]
    names = sys.argv[2:]
    tiles = []
    for n in names:
        tex = Image.open(os.path.join(TEX, n + ".png")).convert("RGBA")
        hump = n.startswith("gyr")
        side = render(tex, hump, math.radians(90), math.radians(0))
        three = render(tex, hump, math.radians(-40), math.radians(-14))
        front = render(tex, hump, math.radians(0), math.radians(-6), scale=13)
        tiles.append((n, side, three, front))
    w, h = tiles[0][1].size
    sheet = Image.new("RGB", (w * 3, h * len(tiles)), "white")
    d = ImageDraw.Draw(sheet)
    for i, (n, a, b, c) in enumerate(tiles):
        sheet.paste(a.convert("RGB"), (0, i * h))
        sheet.paste(b.convert("RGB"), (w, i * h))
        sheet.paste(c.convert("RGB"), (2 * w, i * h))
        d.text((8, i * h + 6), n, fill="black")
    sheet.save(out)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Draws the six cattle coat textures for the vanilla cow model's 64x64 UV layout. Everything is drawn here from scratch (no Mojang
pixels are copied; the repo is CC0). Colors were chosen from the Ludo concept art approved by Tim. Deterministic: same output every run.

    python3 tools/cattle_textures.py            # writes the PNGs
Needs Pillow. Output: common/src/main/resources/assets/farmings/textures/entity/cattle/
"""
import os
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "farmings", "textures", "entity", "cattle")

# UV layout of the vanilla cow model (CowModel.createBaseCowModel): box (w,h,d) at (u,v) has
#   top (u+d, v) w*d | bottom (u+d+w, v) w*d | west (u, v+d) d*h | front (u+d, v+d) w*h | east (u+d+w, v+d) d*h | back (u+2d+w, v+d) w*h
HEAD_TOP, HEAD_BOTTOM, HEAD_WEST, HEAD_FRONT, HEAD_EAST, HEAD_BACK = (6, 0, 8, 6), (14, 0, 8, 6), (0, 6, 6, 8), (6, 6, 8, 8), (14, 6, 6, 8), (20, 6, 8, 8)
MUZZLE_FRONT, MUZZLE_TOP, MUZZLE_BOTTOM, MUZZLE_SIDE_W, MUZZLE_SIDE_E, MUZZLE_BACK = (2, 34, 6, 3), (2, 33, 6, 1), (8, 33, 6, 1), (1, 34, 1, 3), (8, 34, 1, 3), (9, 34, 6, 3)
HORN = (22, 0, 4, 4)       # 1x3x1 box: faces fit in x22..26, y0..4
BODY_NORTH_BELLY, BODY_SOUTH_BACK = (28, 14, 12, 18), (50, 14, 12, 18)
BODY_WEST, BODY_EAST = (18, 14, 10, 18), (40, 14, 10, 18)
BODY_CHEST, BODY_RUMP = (28, 4, 12, 10), (40, 4, 12, 10)
UDDER = (52, 0, 8, 7)      # 4x6x1 box
LEG = (0, 16, 16, 16)      # 4x12x4 box: x0..16, y16..32


def clamp(v):
    return max(0, min(255, int(round(v))))


class Painter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)

    def fill(self, rect, color, noise=0):
        x, y, w, h = rect
        for j in range(y, y + h):
            for i in range(x, x + w):
                self.px[i, j] = self.shade(color, noise)

    def shade(self, color, noise):
        if noise == 0:
            return (*color[:3], 255)
        n = self.rnd.uniform(-noise, noise)
        return (clamp(color[0] + n), clamp(color[1] + n), clamp(color[2] + n), 255)

    def set(self, x, y, color):
        self.px[x, y] = (*color[:3], 255)

    def clear(self, rect):
        x, y, w, h = rect
        for j in range(y, y + h):
            for i in range(x, x + w):
                self.px[i, j] = (0, 0, 0, 0)


def blobs(rnd, n, width, height, rmin, rmax):
    """Random round blobs on a canvas that wraps sideways, as a list of (cx, cy, rx, ry)."""
    return [(rnd.uniform(0, width), rnd.uniform(0, height), rnd.uniform(rmin, rmax), rnd.uniform(rmin * 0.7, rmax * 1.1)) for _ in range(n)]


def field(blob_list, x, y, width):
    best = 0.0
    for cx, cy, rx, ry in blob_list:
        dx = min(abs(x - cx), width - abs(x - cx))
        d = (dx / rx) ** 2 + ((y - cy) / ry) ** 2
        best = max(best, 1.0 - d)
    return best


def paint_body(p, pixel_fn):
    """pixel_fn(cx, cy) -> color, on the unrolled body: x 0..44 = back(12) west flank(10) belly(12) east flank(10), y 0..18 = head to tail."""
    for u in range(12):
        for v in range(18):
            p.set(BODY_SOUTH_BACK[0] + u, BODY_SOUTH_BACK[1] + v, pixel_fn(u, v))
    for u in range(10):
        for v in range(18):
            p.set(BODY_WEST[0] + u, BODY_WEST[1] + v, pixel_fn(12 + u, v))
    for u in range(12):
        for v in range(18):
            p.set(BODY_NORTH_BELLY[0] + u, BODY_NORTH_BELLY[1] + v, pixel_fn(22 + u, v))
    for u in range(10):
        for v in range(18):
            p.set(BODY_EAST[0] + u, BODY_EAST[1] + v, pixel_fn(34 + u, v))
    # chest and rump: the colour of the body's dorsal/front edge
    for u in range(12):
        for v in range(10):
            p.set(BODY_CHEST[0] + u, BODY_CHEST[1] + v, pixel_fn(u, 0))
            p.set(BODY_RUMP[0] + u, BODY_RUMP[1] + v, pixel_fn(u, 17))


def paint_legs(p, upper, lower, hoof, sock_rows):
    x, y, w, h = LEG
    for face_x in (4, 0, 8, 12):          # front, west, east, back faces of a 4x12x4 leg
        for i in range(4):
            for j in range(12):
                color = upper
                if j >= 12 - sock_rows:
                    color = lower
                if j >= 10:
                    color = hoof
                p.set(face_x + i, 20 + j, p.shade(color, 5)[:3])
    p.fill((4, 16, 4, 4), upper, 4)
    p.fill((8, 16, 4, 4), hoof, 2)


def paint_head(p, head_color, face_color, ear_color, muzzle_color, nostril, eye_white=(240, 236, 228), blaze=None, forehead=None):
    for r in (HEAD_TOP, HEAD_BOTTOM, HEAD_WEST, HEAD_EAST, HEAD_BACK):
        p.fill(r, head_color, 5)
    p.fill(HEAD_FRONT, face_color, 5)
    if blaze:                                           # a pale stripe down the face
        x, y, w, h = HEAD_FRONT
        for j in range(h):
            for i in (3, 4):
                p.set(x + i, y + j, blaze)
        for j in range(HEAD_TOP[3]):
            for i in (3, 4):
                p.set(HEAD_TOP[0] + i, HEAD_TOP[1] + j, blaze)
    if forehead:
        x, y, w, h = HEAD_FRONT
        for j in range(0, 2):
            for i in range(w):
                p.set(x + i, y + j, forehead)
    # eyes: pale outer pixel, dark inner pixel, same row on both sides
    x, y, _, _ = HEAD_FRONT
    for row in (3, 4):
        p.set(x + 1, y + row, eye_white)
        p.set(x + 2, y + row, (20, 14, 12))
        p.set(x + 5, y + row, (20, 14, 12))
        p.set(x + 6, y + row, eye_white)
    # ears: painted on the sides of the head, towards the back
    for fx, fy, w, h in (HEAD_WEST, HEAD_EAST):
        for i in range(3):
            for j in range(2):
                p.set(fx + 3 + i - (1 if fx == 0 else 0) + (0 if fx == 0 else 0), fy + 1 + j, ear_color)
    # muzzle block
    for r in (MUZZLE_TOP, MUZZLE_BOTTOM, MUZZLE_SIDE_W, MUZZLE_SIDE_E, MUZZLE_BACK):
        p.fill(r, muzzle_color, 3)
    p.fill(MUZZLE_FRONT, muzzle_color, 3)
    mx, my, _, _ = MUZZLE_FRONT
    p.set(mx + 1, my + 1, nostril)
    p.set(mx + 4, my + 1, nostril)


def paint_horns(p, color, tip):
    x, y, _, _ = HORN
    p.fill((x, y, 4, 4), color, 3)
    p.set(x + 1, y + 1, tip)


def paint_hump(p, coat, dark):
    """The Gyr's shoulder hump: a 6x7x3 box at (0,40). Its faces are painted in the coat, a little darker on top."""
    p.fill((0, 40, 18, 10), coat, 6)
    p.fill((3, 40, 6, 3), dark, 5)


def paint_udder(p, color, teat):
    x, y, _, _ = UDDER
    p.fill((x, y, 8, 7), color, 4)
    for dx in (1, 3):
        p.set(x + 1 + dx, y + 5, teat)     # front face (53..56, 1..7): teats at the bottom
    p.fill((x + 1, y + 1, 4, 6), color, 4)
    p.set(x + 2, y + 6, teat)
    p.set(x + 4, y + 6, teat)


# --- the breeds ---

def black_share(patches, x0, x1):
    n = sum(1 for cx in range(x0, x1) for cy in range(18) if field(patches, cx, cy, 44) > 0.0)
    return n / ((x1 - x0) * 18)


def holstein_patches():
    """The first seed whose two flanks are each between 40 and 55 percent black, and whose back is not all one colour."""
    for seed in range(1, 500):
        patches = blobs(random.Random(seed), 12, 44, 18, 3.0, 6.5)
        west, east, back = black_share(patches, 12, 22), black_share(patches, 34, 44), black_share(patches, 0, 12)
        if 0.40 <= west <= 0.55 and 0.40 <= east <= 0.55 and 0.25 <= back <= 0.7:
            return patches
    raise RuntimeError("no Holstein pattern found")


def holstein(udder):
    p = Painter(101)
    white, black, grey, pink = (244, 242, 238), (24, 24, 26), (176, 172, 176), (232, 160, 168)
    patches = holstein_patches()

    def fn(cx, cy):
        f = field(patches, cx, cy, 44)
        if f > 0.0:
            n = p.rnd.uniform(-5, 5)
            return (clamp(black[0] + n), clamp(black[1] + n), clamp(black[2] + n))
        if 20 <= cx < 36 and cy > 9:                    # grey shading on the underside
            return p.shade(grey, 4)[:3]
        return p.shade(white, 3)[:3]
    paint_body(p, fn)
    paint_legs(p, white, white, (92, 80, 74), 0)
    paint_head(p, black, black, pink, (150, 110, 112), (60, 40, 40), blaze=white)
    p.fill(MUZZLE_FRONT, (232, 190, 188), 3)
    mx, my, _, _ = MUZZLE_FRONT
    p.set(mx + 1, my + 1, (90, 50, 54))
    p.set(mx + 4, my + 1, (90, 50, 54))
    p.clear(HORN)
    if udder:
        paint_udder(p, pink, (214, 130, 140))
    return p.img


def jersey(udder):
    p = Painter(202)
    fawn, belly, dark = (188, 138, 92), (214, 172, 130), (150, 104, 66)

    def fn(cx, cy):
        if 20 <= cx < 36:
            return p.shade(belly, 4)[:3]
        if cx < 12 or cx >= 40:                          # back: a touch darker
            return p.shade(dark if p.rnd.random() < 0.05 else fawn, 5)[:3]
        return p.shade(fawn, 6)[:3]
    paint_body(p, fn)
    paint_legs(p, fawn, (206, 160, 116), (58, 42, 36), 4)
    paint_head(p, (170, 122, 82), (176, 128, 88), (214, 150, 140), (58, 40, 36), (24, 16, 14))
    x, y, w, h = HEAD_FRONT                              # pale ring round the muzzle
    for i in range(w):
        p.set(x + i, y + h - 1, (226, 206, 184))
    p.clear(HORN)
    if udder:
        paint_udder(p, (214, 150, 140), (176, 110, 110))
    return p.img


def brown_swiss(udder):
    p = Painter(303)
    coat, light, dark = (112, 98, 84), (140, 126, 110), (92, 80, 68)

    def fn(cx, cy):
        if 20 <= cx < 36:
            return p.shade(light, 4)[:3]
        return p.shade(dark if p.rnd.random() < 0.05 else coat, 5)[:3]
    paint_body(p, fn)
    paint_legs(p, coat, (96, 84, 72), (44, 38, 36), 3)
    paint_head(p, (118, 104, 90), (124, 110, 94), (186, 150, 140), (226, 214, 200), (50, 42, 40), forehead=(150, 136, 120))
    x, y, w, h = HEAD_FRONT
    for i in range(w):
        p.set(x + i, y + h - 1, (226, 214, 200))
    mx, my, _, _ = MUZZLE_FRONT
    p.fill(MUZZLE_FRONT, (60, 52, 48), 3)
    p.set(mx + 1, my + 1, (20, 16, 16))
    p.set(mx + 4, my + 1, (20, 16, 16))
    p.clear(HORN)
    if udder:
        paint_udder(p, (196, 150, 144), (160, 104, 104))
    return p.img


def gyr(udder):
    p = Painter(404)
    coat, dark, speck, belly = (168, 66, 34), (122, 44, 24), (224, 200, 180), (152, 98, 78)

    def fn(cx, cy):
        r = p.rnd.random()
        if 20 <= cx < 36:
            return p.shade(belly, 5)[:3]
        if r < 0.10:
            return p.shade(speck, 6)[:3]
        if r < 0.22:
            return p.shade(dark, 5)[:3]
        return p.shade(coat, 7)[:3]
    paint_body(p, fn)
    paint_legs(p, (150, 56, 30), (176, 92, 62), (54, 36, 30), 4)
    paint_head(p, (140, 52, 28), (146, 56, 30), (176, 96, 80), (156, 150, 142), (50, 38, 34), forehead=(166, 66, 36))
    paint_horns(p, (214, 200, 172), (70, 62, 52))
    paint_hump(p, coat, dark)
    if udder:
        paint_udder(p, (170, 90, 100), (130, 66, 76))
    return p.img


def angus(udder):
    p = Painter(505)
    coat, hi = (32, 32, 36), (46, 46, 52)

    def fn(cx, cy):
        return p.shade(hi if p.rnd.random() < 0.14 else coat, 3)[:3]
    paint_body(p, fn)
    paint_legs(p, coat, (28, 28, 32), (14, 14, 16), 0)
    paint_head(p, (30, 30, 34), (34, 34, 38), (70, 52, 52), (46, 44, 48), (14, 14, 16), eye_white=(220, 216, 208))
    p.clear(HORN)
    p.clear(UDDER)
    return p.img


def wagyu(udder):
    p = Painter(606)
    coat, sheen = (22, 17, 14), (40, 26, 18)
    sh = blobs(random.Random(11), 3, 44, 18, 4.0, 6.0)
    flank = [(16.5, 9, 6.5, 5.2), (39.0, 9, 6.5, 5.2)]       # one warm patch on each flank (x 12..22 west, 34..44 east)

    def fn(cx, cy):
        f = field(flank, cx, cy, 44)
        if f > 0.15:
            return p.shade(sheen, 5)[:3]
        if f > 0.0:
            return p.shade((30, 21, 16), 3)[:3]
        return p.shade(coat, 3)[:3]
    paint_body(p, fn)
    paint_legs(p, coat, (26, 20, 16), (60, 54, 52), 0)
    paint_head(p, (24, 19, 16), (26, 20, 17), (76, 54, 50), (44, 38, 36), (12, 10, 10), eye_white=(226, 220, 210))
    paint_horns(p, (216, 204, 178), (70, 62, 52))
    p.clear(UDDER)
    return p.img


BREEDS = {"holstein": (holstein, True), "jersey": (jersey, True), "brown_swiss": (brown_swiss, True), "gyr": (gyr, True),
          "angus": (angus, False), "wagyu": (wagyu, False)}


HAIR = os.path.join(os.path.dirname(__file__), "..", "art", "final", "hair.png")


def with_hair(img):
    """Lays the Ludo cow-hair swatch (art/final/hair.png, from tools/process_art.py) over the coat as a light-and-shade grain, so the
    approved colours and patterns stay and the hide reads as hair. Horns and the transparent parts are left alone."""
    if not os.path.exists(HAIR):
        return img
    hair = Image.open(HAIR).convert("L").resize(img.size)
    mean = sum(hair.getdata()) / (hair.width * hair.height)
    px, hp = img.load(), hair.load()
    for x in range(img.width):
        for y in range(img.height):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            k = 1 + 0.35 * (hp[x, y] - mean) / 128
            px[x, y] = (clamp(r * k), clamp(g * k), clamp(b * k), a)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, (fn, dairy) in BREEDS.items():
        with_hair(fn(True)).save(os.path.join(OUT, name + ".png"))
        if dairy:
            with_hair(fn(False)).save(os.path.join(OUT, name + "_no_udder.png"))
    print("wrote", sorted(os.listdir(OUT)))


if __name__ == "__main__":
    main()

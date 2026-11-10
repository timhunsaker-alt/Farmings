#!/usr/bin/env python3
"""Turns the Ludo pictures in art/source into the mod's textures (the Wildcatter way: cleanest() and tileable() are copied from
Wildcatter's tools/process_materials.py).

  art/source/mat_*.webp   -> textures/block/mat/<name>.png, 32 px, the cleanest window made to tile
  art/source/item/*.webp  -> textures/item/<name>.png, 32 px icons (a plain background is made transparent)
  the spoiled milk armor  -> the fresh icons, soured: yellow-green, darker, with curdled spots
  the worn milk armor     -> textures/entity/equipment/humanoid(_leggings)/milk(_spoiled).png, from the milk swatch
  the cow-hair swatch     -> art/final/hair.png, read by tools/cattle_textures.py
Run from the repo root: python3 tools/process_art.py. Prompts: art/source/PROMPTS.md.
"""
import os, random
from PIL import Image, ImageEnhance, ImageDraw

SRC, SIZE = "art/source", 32
ASSETS = "common/src/main/resources/assets/farmings/textures"
WINDOW = 0.34
# a fixed window where the cleanest window drops the pattern: (crop, brightness, colour)
PATTERN = {"mat_grooved_concrete": ((0.0, 0.0, 0.34, 0.34), 1.05, 1.0), "mat_hay": ((0.0, 0.0, 0.3, 0.3), 1.1, 1.0)}


def cleanest(img):
	"""The window of the picture with the least edge energy: no grout lines, no tile borders."""
	from PIL import ImageFilter
	small = img.resize((96, 96), Image.BOX).convert("L").filter(ImageFilter.FIND_EDGES)
	px = small.load()
	w = int(96 * WINDOW)
	best, at = None, (0, 0)
	for y in range(2, 96 - w - 2, 2):
		for x in range(2, 96 - w - 2, 2):
			e = sum(px[i, j] for i in range(x, x + w, 2) for j in range(y, y + w, 2))
			if best is None or e < best:
				best, at = e, (x, y)
	k = img.width / 96
	return img.crop((int(at[0] * k), int(at[1] * k), int((at[0] + w) * k), int((at[1] + w) * k)))


def tileable(img, band=4):
	"""Cross-fades each edge with the opposite one so the swatch repeats without a seam."""
	px = img.load()
	w, h = img.size
	out = img.copy()
	o = out.load()
	for y in range(h):
		for i in range(band):
			k = 0.5 * (1 - i / band)
			a, b = px[i, y], px[w - 1 - i, y]
			o[i, y] = tuple(int(x * (1 - k) + z * k) for x, z in zip(a, b))
			o[w - 1 - i, y] = tuple(int(z * (1 - k) + x * k) for x, z in zip(a, b))
	img = out.copy()
	px = img.load()
	for x in range(w):
		for i in range(band):
			k = 0.5 * (1 - i / band)
			a, b = px[x, i], px[x, h - 1 - i]
			o[x, i] = tuple(int(p * (1 - k) + q * k) for p, q in zip(a, b))
			o[x, h - 1 - i] = tuple(int(q * (1 - k) + p * k) for p, q in zip(a, b))
	return out



def material(name):
	img = Image.open(f"{SRC}/{name}.webp").convert("RGB")
	if name in PATTERN:
		(l, t, r, b), bright, colour = PATTERN[name]
		w, h = img.size
		img = img.crop((int(l * w), int(t * h), int(r * w), int(b * h)))
		img = ImageEnhance.Color(ImageEnhance.Brightness(img).enhance(bright)).enhance(colour)
	else:
		img = cleanest(img)
	img = img.resize((SIZE, SIZE), Image.BOX)
	img = ImageEnhance.Contrast(img).enhance(1.1)
	return tileable(img)


def icon(path):
	img = Image.open(path).convert("RGBA")
	if img.getpixel((0, 0))[3] == 255:   # drawn on a plain background: make that transparent
		for corner in ((0, 0), (img.width - 1, 0), (0, img.height - 1), (img.width - 1, img.height - 1)):
			ImageDraw.floodfill(img, corner, (0, 0, 0, 0), thresh=40)
	img = img.crop(img.getbbox())
	side = max(img.size)
	square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
	square.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
	return square.resize((SIZE, SIZE), Image.BOX)


def sour(img, seed):
	"""Spoiled milk: yellow-green, a little darker, with curdled spots. Keeps the alpha."""
	rnd = random.Random(seed)
	out = img.copy()
	px = out.load()
	for x in range(out.width):
		for y in range(out.height):
			r, g, b, a = px[x, y]
			if a:
				px[x, y] = (int(r * 0.86), int(g * 0.88), int(b * 0.55), a)
	for _ in range(out.width * out.height // 40):
		x, y = rnd.randrange(out.width), rnd.randrange(out.height)
		r, g, b, a = px[x, y]
		if a:
			px[x, y] = (int(r * 0.7), int(g * 0.72), int(b * 0.5), a)
	return out


def face(name, box=(0.0, 0.0, 0.5, 0.5)):
	"""A block face from a Ludo picture: one window of it (a single fan), 32 px."""
	img = Image.open(f"{SRC}/{name}.webp").convert("RGB")
	w, h = img.size
	return img.crop((int(box[0] * w), int(box[1] * h), int(box[2] * w), int(box[3] * h))).resize((SIZE, SIZE), Image.LANCZOS).convert("RGBA")


def cold_room():
	"""The walk-in fridge's faces (docs/multiblocks/cold-room.md): the evaporator's fan, the condensing unit's fan grille, a grey top, and
	the door halves drawn on the Ludo insulated panel (a handle, a window, a steel kick plate). Copies go to art/final so make_assets.py
	keeps them."""
	panel = Image.open(f"{ASSETS}/block/mat/mat_insulated_panel.png").convert("RGBA")
	out = {"evaporator_front": face("face_evaporator"), "condensing_unit_side": face("face_condensing_unit")}
	out["condensing_unit_top"] = ImageEnhance.Brightness(ImageEnhance.Color(panel).enhance(0.2)).enhance(0.62)
	for half in ("bottom", "top"):
		im = panel.copy()
		d = ImageDraw.Draw(im)
		d.rectangle([0, 0, SIZE - 1, SIZE - 1], outline=(150, 156, 162, 255))
		if half == "bottom":
			d.rectangle([24, 4, 26, 18], fill=(90, 96, 104, 255))                 # the handle
			d.rectangle([1, 24, SIZE - 2, SIZE - 2], fill=(176, 182, 188, 255))   # the kick plate
		else:
			d.rectangle([7, 7, 24, 20], fill=(168, 196, 214, 255), outline=(120, 126, 132, 255))   # the window
		out["cold_room_door_" + half] = im
	for name, im in out.items():
		im.save(f"{ASSETS}/block/{name}.png")
		im.save(f"art/final/{name}.png")
	out["cold_room_door_bottom"].save(f"{ASSETS}/item/cold_room_door.png")


def main():
	os.makedirs(f"{ASSETS}/block/mat", exist_ok=True)
	os.makedirs(f"{ASSETS}/item", exist_ok=True)
	os.makedirs("art/final", exist_ok=True)
	mats = sorted(f[:-5] for f in os.listdir(SRC) if f.startswith("mat_") and f.endswith(".webp") and f != "mat_cow_hair.webp")
	for name in mats:
		material(name).convert("RGBA").save(f"{ASSETS}/block/mat/{name}.png")
	items = sorted(f[:-5] for f in os.listdir(f"{SRC}/item") if f.endswith(".webp"))
	for name in items:
		img = icon(f"{SRC}/item/{name}.webp")
		img.save(f"{ASSETS}/item/{name}.png")
		if name.startswith("milk_"):
			sour(img, name).save(f"{ASSETS}/item/{name}_spoiled.png")
	# the worn milk armor is drawn by tools/armor_textures.py (face by face, with drips), not from a flat swatch
	import subprocess, sys
	subprocess.run([sys.executable, os.path.join(os.path.dirname(__file__), "armor_textures.py")], check=True)
	cold_room()
	hair = cleanest(Image.open(f"{SRC}/mat_cow_hair.webp").convert("RGB")).convert("L").resize((64, 64), Image.BOX)
	hair.save("art/final/hair.png")
	print("art:", len(mats), "materials,", len(items), "icons")


if __name__ == "__main__":
	main()

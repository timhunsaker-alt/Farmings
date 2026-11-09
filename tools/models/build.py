#!/usr/bin/env python3
"""Builds every Farmings part model (tools/models/parts.py) and a preview of each in build/model-previews/.

Run from the repo root after tools/make_assets.py: python3 tools/models/build.py [part ...]. The models replace the placeholder
blockstates, block models and item models make_assets.py writes for the same parts.
"""
import json, os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mlib
import parts

WILDCATTER_TEXTURES = os.path.join(mlib.ROOT, "..", "wildcatter", "common/src/main/resources/assets/wildcatter/textures")


def colours(model):
	out = {}
	for short, path in model.textures.items():
		if ":" in path:
			ns, p = path.split(":")
			png = os.path.join(WILDCATTER_TEXTURES, p + ".png") if ns == "wildcatter" else None
		else:
			png = os.path.join(mlib.ASSETS, "textures", path + ".png")
		if png and os.path.exists(png):
			out[short] = Image.open(png).convert("RGB").resize((1, 1), Image.BOX).getpixel((0, 0))
	return out


def main(names):
	previews = os.path.join(mlib.ROOT, "build/model-previews")
	os.makedirs(previews, exist_ok=True)
	for name, fn in parts.PARTS.items():
		if names and name not in names:
			continue
		M = fn()
		M.export()
		if len(M.cells) == 1:   # a one-cell part's piece property still has two values (Wildcatter's BigPartBlock)
			path = os.path.join(mlib.ASSETS, "blockstates", name + ".json")
			states = json.load(open(path))
			for facing in ("north", "east", "south", "west"):
				states["variants"]["facing=%s,piece=1" % facing] = dict(states["variants"]["facing=%s,piece=0" % facing])
			with open(path, "w") as f:
				json.dump(states, f, indent=1)
				f.write("\n")
		M.preview(os.path.join(previews, name + ".png"), colours(M))
	for name, paint in parts.CONTROLLERS.items():
		if names and name not in names:
			continue
		M = parts.controller(name, paint)
		M.export(blockstate=False)
		with open(os.path.join(mlib.ASSETS, "blockstates", name + ".json"), "w") as f:
			json.dump({"variants": {"": {"model": "farmings:block/%s/p0" % name}}}, f, indent=1)
			f.write("\n")
		M.preview(os.path.join(previews, name + ".png"), colours(M))


if __name__ == "__main__":
	main(sys.argv[1:])

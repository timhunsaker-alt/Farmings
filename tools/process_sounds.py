#!/usr/bin/env python3
"""Turns the Ludo sources in art/sound_source into the mod's mono .ogg files and sounds.json (docs/animal-health.md, "Sounds and art"),
the way Tailings' tools/process_sounds.py does. Needs numpy and soundfile. Run from the repo root."""
import json
import os
import sys

import numpy as np
import soundfile as sf

SRC = "art/sound_source"
OUT = "common/src/main/resources/assets/farmings/sounds"
# sound event -> source file (one-shots: trimmed to the sound, normalised, faded at both ends)
EVENTS = {"entity.cattle.cough": "cattle_cough", "entity.sheep.cough": "sheep_cough", "entity.pig.cough": "pig_cough",
          "entity.horse.cough": "horse_cough", "block.footbath.splash": "footbath", "item.cmt_paddle.swirl": "cmt", "item.vet_kit.open": "vet_kit"}

os.makedirs(OUT, exist_ok=True)
for event, name in EVENTS.items():
	found = [f for f in os.listdir(SRC) if f.startswith(name + ".")]
	if not found:
		sys.exit(f"no source for {name}")
	data, rate = sf.read(os.path.join(SRC, found[0]), always_2d=True)
	mono = data.mean(axis=1)
	loud = np.abs(mono) > 0.01
	first, last = np.argmax(loud), len(loud) - np.argmax(loud[::-1])
	mono = mono[max(0, first - 200):last + 200]
	mono = mono / np.abs(mono).max() * 0.9
	fade = min(len(mono) // 20, int(rate * 0.02))
	mono[:fade] *= np.linspace(0, 1, fade)
	mono[-fade:] *= np.linspace(1, 0, fade)
	sf.write(f"{OUT}/{name}.ogg", mono, rate, format="OGG", subtype="VORBIS")
	print(f"{name:14} {len(mono) / rate:5.2f} s")
with open("common/src/main/resources/assets/farmings/sounds.json", "w") as f:
	json.dump({e: {"sounds": [f"farmings:{n}"], "subtitle": f"subtitles.farmings.{e}"} for e, n in EVENTS.items()}, f, indent=2)
	f.write("\n")

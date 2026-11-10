"""Every Farmings part at true size (1 cell = 1 m), drawn facing north (front toward -z), main cell at [0, 1].

Real counterparts and sizes are in docs/multiblocks/barn.md, parlor.md and creamery.md. Each function returns a Model; build.py exports them.
"""
from farmlib import model, legs

PARTS = {}


def part(fn):
	PARTS[fn.__name__] = fn
	return fn


# ---- the barn -------------------------------------------------------------------------------------------------------------

@part
def stall_bed():
	"""A freestall bed: a concrete base, a rubber mattress, a brisket board at the head and a curb at the alley end."""
	M = model("stall_bed", 1, 1, 3, particle="mat")
	M.box((0, 0, 0), (1, 0.2, 3), "concrete")
	M.box((0.02, 0.2, 0.25), (0.98, 0.25, 2.85), "mat")
	M.box((0.05, 0.2, 0.05), (0.95, 0.36, 0.17), "wood")       # brisket board
	M.box((0, 0.2, 2.85), (1, 0.3, 3), "concrete")             # rear curb
	M.collide((0, 0, 0), (1, 0.25, 3))
	return M


@part
def stall_divider():
	"""Galvanized steel stall loops: a post at the head and one behind, a top rail sweeping down to the rear and a lower rail."""
	M = model("stall_divider", 1, 2, 3, particle="galv")
	x = 0.5
	M.pipe([(x, 0.0, 0.35), (x, 1.15, 0.35)], 3, "galv")
	M.pipe([(x, 0.0, 2.2), (x, 0.75, 2.2)], 3, "galv")
	M.pipe([(x, 1.15, 0.35), (x, 1.15, 1.7)], 2, "galv")
	M.pipe([(x, 1.15, 1.7), (x, 0.75, 1.7)], 2, "galv")
	M.pipe([(x, 0.75, 1.7), (x, 0.75, 2.6)], 2, "galv")
	M.pipe([(x, 0.45, 0.35), (x, 0.45, 1.45)], 2, "galv")
	M.box((x - 0.12, 0.0, 0.25), (x + 0.12, 0.02, 0.45), "galv")   # base plates
	M.box((x - 0.12, 0.0, 2.1), (x + 0.12, 0.02, 2.3), "galv")
	M.collide((0.44, 0, 0), (0.56, 1.2, 3))
	return M


@part
def feed_bunk():
	"""A concrete feed bunk with raised lips, a little hay on its floor."""
	M = model("feed_bunk", 1, 1, 1, particle="concrete")
	M.box((0, 0, 0), (1, 0.4, 1), "concrete")
	M.box((0, 0.4, 0), (1, 0.6, 0.1), "concrete")
	M.box((0, 0.4, 0.9), (1, 0.55, 1), "concrete")
	M.box((0.05, 0.4, 0.15), (0.95, 0.46, 0.85), "hay")
	M.collide((0, 0, 0), (1, 0.6, 1))
	return M


@part
def hay_feeder():
	"""A galvanized hay rack over the bunk: a frame, vertical bars the cows eat between, and the hay in it."""
	M = model("hay_feeder", 1, 1, 1, particle="hay")
	for x in (0.04, 0.96):
		for z in (0.12, 0.92):
			M.box((x - 0.03, 0, z - 0.03), (x + 0.03, 0.9, z + 0.03), "galv")
	M.box((0, 0.87, 0.09), (1, 0.93, 0.95), "galv")
	for k in range(9):
		x = 0.1 + k * 0.1
		M.box((x - 0.012, 0.05, 0.06), (x + 0.012, 0.87, 0.09), "galv")
	M.box((0.08, 0.05, 0.16), (0.92, 0.78, 0.88), "hay")
	M.collide((0, 0, 0.1), (1, 0.9, 0.95))
	return M


@part
def water_trough():
	"""A 2 m galvanized float-valve trough on a concrete plinth, full of water, the float box at one end."""
	M = model("water_trough", 2, 1, 1, particle="galv")
	M.box((0.05, 0, 0.15), (1.95, 0.15, 0.85), "concrete")
	M.box((0.08, 0.15, 0.18), (1.92, 0.22, 0.82), "galv")
	M.box((0.08, 0.22, 0.18), (1.92, 0.7, 0.22), "galv")
	M.box((0.08, 0.22, 0.78), (1.92, 0.7, 0.82), "galv")
	M.box((0.08, 0.22, 0.22), (0.12, 0.7, 0.78), "galv")
	M.box((1.88, 0.22, 0.22), (1.92, 0.7, 0.78), "galv")
	M.box((0.12, 0.22, 0.22), (1.88, 0.62, 0.78), "water", faces=("up",))
	M.box((1.6, 0.62, 0.24), (1.86, 0.74, 0.76), "galv")          # the float valve cover
	M.pipe([(1.75, 0.0, 0.88), (1.75, 0.65, 0.88)], 1, "galv")   # supply riser
	M.collide((0.05, 0, 0.15), (1.95, 0.7, 0.85))
	return M


@part
def alley_floor():
	"""Grooved concrete, 2 cm proud of the barn floor."""
	M = model("alley_floor", 1, 1, 1, particle="groove")
	M.box((0, 0, 0), (1, 0.125, 1), {"up": "groove", "north": "concrete", "south": "concrete", "east": "concrete", "west": "concrete", "down": "concrete"})
	M.collide((0, 0, 0), (1, 0.125, 1))
	return M


@part
def cow_lane():
	"""A cell of the travel lane between barn and parlor: brushed concrete with anti-slip grooves, and a low galvanised rail and post
	along each side (drawn only: cows follow the lane by waypoints, so the rails do not block, parlor.md)."""
	M = model("cow_lane", 1, 1, 1, particle="lane")
	M.box((0, 0, 0), (1, 0.125, 1), {"up": "lane", "north": "concrete", "south": "concrete", "east": "concrete", "west": "concrete", "down": "concrete"})
	for x0 in (0.0, 0.94):
		M.box((x0, 0.125, 0.44), (x0 + 0.06, 0.95, 0.56), "galv")          # a post mid-cell
		M.box((x0, 0.85, 0), (x0 + 0.06, 0.92, 1), "galv")                # the top rail
		M.box((x0, 0.45, 0), (x0 + 0.06, 0.52, 1), "galv")                # the mid rail
	M.collide((0, 0, 0), (1, 0.125, 1))
	return M


@part
def holding_pen():
	"""A cell of the holding pen: grooved concrete with a yellow painted border where cows wait for a stall."""
	M = model("holding_pen", 1, 1, 1, particle="groove")
	M.box((0, 0, 0), (1, 0.125, 1), {"up": "groove", "north": "concrete", "south": "concrete", "east": "concrete", "west": "concrete", "down": "concrete"})
	M.box((0, 0.125, 0), (1, 0.135, 0.06), "yellow")
	M.box((0, 0.125, 0.94), (1, 0.135, 1), "yellow")
	M.collide((0, 0, 0), (1, 0.125, 1))
	return M


@part
def scraper_rail():
	"""The alley's centre cell: grooved concrete with the scraper's steel guide channel set into it."""
	M = model("scraper_rail", 1, 1, 1, particle="groove")
	M.box((0, 0, 0), (1, 0.1, 1), {"up": "groove", "north": "concrete", "south": "concrete", "east": "concrete", "west": "concrete", "down": "concrete"})
	M.box((0, 0.1, 0.4), (1, 0.125, 0.45), "galv")
	M.box((0, 0.1, 0.55), (1, 0.125, 0.6), "galv")
	M.box((0, 0.06, 0.45), (1, 0.1, 0.55), "dark")
	M.collide((0, 0, 0), (1, 0.125, 1))
	return M


@part
def manure_scraper():
	"""The 3 m alley scraper: a yellow V-less straight blade with a rubber wiper, braced back to a carriage on the guide channel."""
	M = model("manure_scraper", 1, 1, 3, 0, 1, particle="yellow")
	M.box((0.42, 0.02, -0.97), (0.5, 0.38, 1.97), "yellow")
	M.box((0.38, 0.0, -0.97), (0.42, 0.12, 1.97), "rubber")
	for z in (-0.6, 0.2, 0.8, 1.6):
		M.box((0.5, 0.05, z - 0.03), (0.75, 0.3, z + 0.03), "yellow")
	M.box((0.5, 0.0, 0.3), (0.9, 0.22, 0.7), "yellow")
	M.box((0.85, 0.05, 0.42), (0.98, 0.12, 0.58), "galv")     # the cable eye
	M.collide((0.25, 0, -1), (0.75, 0.38, 2))
	return M


@part
def scraper_drive():
	"""The drive unit: a 2.2 kW gearmotor turning a cable drum, on a galvanized base, with its nameplate."""
	M = model("scraper_drive", 1, 1, 1, particle="blue")
	M.box((0.05, 0, 0.1), (0.95, 0.1, 0.9), "galv")
	M.cylinder("x", (0.45, 0.5), 0.26, 0.12, 0.5, "galv", 12)            # cable drum
	M.cylinder("x", (0.45, 0.5), 0.3, 0.1, 0.13, "galv", 12)
	M.cylinder("x", (0.45, 0.5), 0.3, 0.49, 0.52, "galv", 12)
	M.box((0.52, 0.12, 0.3), (0.68, 0.62, 0.7), "blue")                  # gearbox
	M.cylinder("x", (0.45, 0.5), 0.17, 0.68, 0.93, "blue", 12)          # motor
	M.fins((0.7, 0.3, 0.32), (0.92, 0.62, 0.34), "x", 5, 0.01, "blue")
	M.box((0.75, 0.62, 0.42), (0.88, 0.72, 0.58), "blue")                # terminal box
	M.decal((0.55, 0.35, 0.29), (0.65, 0.45, 0.3), "nameplate", "north", "blue")
	M.collide((0.06, 0, 0.06), (0.94, 1.0, 0.94))
	return M


@part
def collection_pit():
	"""A reception pit, 2 x 3 m and a block deep, concrete walls, manure in it, a guard rail on the far side."""
	M = model("collection_pit", 2, 2, 3, 0, 1, particle="concrete")
	M.box((0, 0, -1), (2, 0.2, 2), "concrete")
	M.box((0, 0.2, -1), (2, 1.0, -0.85), "concrete")
	M.box((0, 0.2, 1.85), (2, 1.0, 2), "concrete")
	M.box((1.85, 0.2, -0.85), (2, 1.0, 1.85), "concrete")
	M.box((0, 0.2, -0.85), (0.1, 0.35, 1.85), "concrete")             # the low lip the scraper pushes over
	M.box((0.1, 0.2, -0.85), (1.85, 0.6, 1.85), "manure", faces=("up",))
	for z in (-0.9, 0.5, 1.9):
		M.pipe([(1.92, 1.0, z), (1.92, 1.9, z)], 1.5, "yellow")
	M.pipe([(1.92, 1.9, -0.9), (1.92, 1.9, 1.9)], 1.5, "yellow")
	M.pipe([(1.92, 1.45, -0.9), (1.92, 1.45, 1.9)], 1.5, "yellow")
	M.collide((0, 0, -1), (2, 1.0, 2))
	return M


# ---- the parlor ----------------------------------------------------------------------------------------------------------

@part
def parlor_stall():
	"""A parlor stall: a raised concrete standing with a rubber mat, galvanized side rails."""
	M = model("parlor_stall", 1, 1, 2, particle="mat")
	M.box((0, 0, 0), (1, 0.2, 2), "concrete")
	M.box((0.03, 0.2, 0.03), (0.97, 0.25, 1.97), "mat")
	for x in (0.04, 0.96):
		for z in (0.1, 1.9):
			M.pipe([(x, 0.25, z), (x, 0.95, z)], 1.5, "galv")
		M.pipe([(x, 0.95, 0.1), (x, 0.95, 1.9)], 1.5, "galv")
		M.pipe([(x, 0.6, 0.1), (x, 0.6, 1.9)], 1.5, "galv")
	M.collide((0, 0, 0), (1, 0.25, 2))
	return M


@part
def milking_unit():
	"""A milking point: a stainless detacher box on the rail, the long milk tube, the claw and its four teat cups."""
	M = model("milking_unit", 1, 1, 1, particle="steel")
	M.box((0.3, 0.55, 0.85), (0.7, 0.95, 1.0), "steel")
	M.decal((0.38, 0.75, 0.84), (0.62, 0.9, 0.85), "buttons", "north", "steel")
	M.cylinder("y", (0.25, 0.92), 0.05, 0.1, 0.55, "steel", 8)     # glass-and-steel milk meter
	M.pipe([(0.5, 0.55, 0.9), (0.5, 0.35, 0.9)], 0.75, "rubber")
	M.pipe([(0.5, 0.35, 0.9), (0.5, 0.35, 0.5)], 0.75, "rubber")
	M.cylinder("y", (0.5, 0.5), 0.07, 0.22, 0.32, "steel", 12)     # the claw
	for dx, dz in ((-0.08, -0.06), (0.08, -0.06), (-0.08, 0.06), (0.08, 0.06)):
		M.cylinder("y", (0.5 + dx, 0.5 + dz), 0.025, 0.3, 0.45, "steel", 8)
		M.cylinder("y", (0.5 + dx, 0.5 + dz), 0.027, 0.42, 0.45, "rubber", 8)
	M.collide((0.25, 0.1, 0.4), (0.75, 0.95, 1.0))
	return M


@part
def vacuum_pump():
	"""The 4 kW vacuum pump set: motor and pump on a base, a belt guard, and the vacuum receiver tank."""
	M = model("vacuum_pump", 1, 1, 1, particle="blue")
	M.box((0.05, 0, 0.05), (0.95, 0.08, 0.95), "galv")
	M.cylinder("x", (0.3, 0.3), 0.17, 0.1, 0.5, "blue", 12)          # motor
	M.fins((0.12, 0.13, 0.14), (0.48, 0.47, 0.16), "x", 6, 0.01, "blue")
	M.cylinder("x", (0.3, 0.3), 0.15, 0.55, 0.85, "iron", 12)        # pump
	M.box((0.5, 0.1, 0.1), (0.56, 0.5, 0.5), "yellow")                 # belt guard
	M.cylinder("y", (0.5, 0.75), 0.16, 0.08, 0.75, "steel", 12)      # receiver
	M.pipe([(0.7, 0.3, 0.3), (0.7, 0.3, 0.75)], 2, "steel")
	M.decal((0.6, 0.45, 0.586), (0.75, 0.6, 0.59), "gauge", "north", "steel")
	M.collide((0.06, 0, 0.06), (0.94, 0.88, 0.94))
	return M


@part
def milk_line():
	"""The stainless milk line on its floor brackets."""
	M = model("milk_line", 1, 1, 1, particle="steel")
	M.pipe([(0.0, 0.2, 0.5), (1.0, 0.2, 0.5)], 3, "steel")
	M.box((0.45, 0, 0.42), (0.55, 0.15, 0.58), "galv")
	M.box((0.4, 0, 0.38), (0.6, 0.02, 0.62), "galv")
	M.collide((0, 0, 0.35), (1, 0.3, 0.65))
	return M


@part
def bulk_tank():
	"""The 1,000 L bulk tank: a stainless horizontal tank on legs, a manway and agitator motor on top, outlet valve at the front."""
	M = model("bulk_tank", 2, 2, 2, particle="steel")
	M.cylinder("x", (1.05, 1.0), 0.78, 0.08, 1.92, "steel", 16)
	M.flange("x", (1.05, 1.0), 0.8, 0.08, 0.03, "steel", bolts=0)
	M.flange("x", (1.05, 1.0), 0.8, 1.89, 0.03, "steel", bolts=0)
	for x in (0.3, 1.7):
		for z in (0.45, 1.55):
			M.cylinder("y", (x, z), 0.04, 0.0, 0.5, "steel", 8)
	M.cylinder("y", (1.0, 1.0), 0.22, 1.8, 1.9, "steel", 12)        # manway
	M.cylinder("y", (0.5, 1.0), 0.09, 1.8, 1.98, "grey", 8)          # agitator motor
	M.pipe([(1.0, 0.45, 0.25), (1.0, 0.45, 0.05)], 2, "steel")       # outlet
	M.box((0.93, 0.38, 0.04), (1.07, 0.52, 0.08), "steel")
	M.decal((1.3, 0.9, 0.205), (1.6, 1.15, 0.21), "nameplate", "north", "steel")
	M.decal((0.4, 0.9, 0.205), (0.6, 1.1, 0.21), "gauge", "north", "steel")
	M.collide((0, 0, 0.15), (2, 1.85, 1.85))
	return M


# ---- the creamery --------------------------------------------------------------------------------------------------------

@part
def receiving_tank():
	"""A vertical stainless raw-milk receiving tank on four legs, a top manway, the outlet valve, a level gauge."""
	M = model("receiving_tank", 2, 2, 2, particle="steel")
	M.cylinder("y", (1.0, 1.0), 0.85, 0.35, 1.85, "steel", 16)
	M.cylinder("y", (1.0, 1.0), 0.6, 1.85, 1.95, "steel", 16)
	M.cylinder("y", (1.0, 1.0), 0.2, 1.95, 2.0, "steel", 12)
	legs(M, 0.15, 1.85, 0.15, 1.85, 0.4, r=0.05, inset=0.15)
	M.pipe([(1.0, 0.25, 0.3), (1.0, 0.25, 0.05)], 2, "steel")
	M.box((0.93, 0.18, 0.02), (1.07, 0.32, 0.07), "steel")
	M.box((0.26, 0.6, 0.38), (0.3, 1.6, 0.42), "steel")              # level sight tube
	M.decal((0.75, 1.1, 0.135), (1.25, 1.4, 0.15), "nameplate", "north", "steel")
	M.collide((0.1, 0, 0.1), (1.9, 2.0, 1.9))
	return M


@part
def batch_pasteurizer():
	"""A 200 L jacketed batch pasteurizer: the vat on legs, lid, agitator motor, thermometer and chart recorder."""
	M = model("batch_pasteurizer", 1, 2, 1, particle="steel")
	M.cylinder("y", (0.5, 0.5), 0.42, 0.35, 1.25, "steel", 16)
	M.cylinder("y", (0.5, 0.5), 0.44, 1.25, 1.3, "steel", 16)
	legs(M, 0.1, 0.9, 0.1, 0.9, 0.4, r=0.03)
	M.cylinder("y", (0.5, 0.5), 0.09, 1.3, 1.6, "grey", 8)
	M.box((0.44, 1.6, 0.44), (0.56, 1.7, 0.56), "grey")
	M.decal((0.3, 0.75, 0.065), (0.5, 0.95, 0.08), "gauge", "north", "steel")      # the 63 C thermometer
	M.decal((0.55, 0.7, 0.065), (0.75, 1.0, 0.08), "controller", "north", "steel")
	M.pipe([(0.5, 0.2, 0.5), (0.5, 0.2, 0.0)], 1.5, "steel")
	M.collide((0.06, 0, 0.06), (0.94, 1.7, 0.94))
	return M


@part
def cream_separator():
	"""A centrifugal cream separator: cast-iron pedestal and motor, the stainless bowl, the cream and skim spouts."""
	M = model("cream_separator", 1, 1, 1, particle="steel")
	M.box((0.2, 0, 0.2), (0.8, 0.08, 0.8), "iron")
	M.cylinder("y", (0.5, 0.5), 0.16, 0.08, 0.5, "iron", 12)
	M.cylinder("x", (0.5, 0.5), 0.1, 0.66, 0.9, "blue", 12)
	M.cylinder("y", (0.5, 0.5), 0.26, 0.5, 0.82, "steel", 16)
	M.cylinder("y", (0.5, 0.5), 0.12, 0.82, 0.95, "steel", 12)
	M.pipe([(0.5, 0.7, 0.3), (0.5, 0.7, 0.08)], 1, "steel")
	M.pipe([(0.42, 0.62, 0.3), (0.42, 0.62, 0.05)], 1, "steel")
	M.collide((0.2, 0, 0.2), (0.8, 0.95, 0.8))
	return M


@part
def butter_churn():
	"""A barrel churn: a wooden drum with steel hoops on its axle, a stand, the motor and its belt guard."""
	M = model("butter_churn", 1, 1, 1, particle="wood")
	M.cylinder("x", (0.55, 0.5), 0.32, 0.18, 0.82, "wood", 16)
	for x in (0.25, 0.5, 0.75):
		M.ring("x", (0.55, 0.5), 0.33, x - 0.015, x + 0.015, 0.02, "steel", 16)
	M.pipe([(0.06, 0.55, 0.5), (0.94, 0.55, 0.5)], 1.5, "steel")
	for x in (0.08, 0.92):
		M.box((x - 0.04, 0, 0.3), (x + 0.04, 0.6, 0.7), "galv")
	M.cylinder("y", (0.85, 0.15), 0.08, 0.0, 0.25, "blue", 8)
	M.collide((0.04, 0, 0.15), (0.96, 0.9, 0.85))
	return M


@part
def cheese_vat():
	"""A 2 m open double-wall cheese vat on legs, milk and curd in it, the cutting/stirring bridge across."""
	M = model("cheese_vat", 2, 1, 1, particle="steel")
	M.box((0.05, 0.3, 0.1), (1.95, 0.35, 0.9), "steel")
	M.box((0.05, 0.35, 0.1), (1.95, 0.85, 0.15), "steel")
	M.box((0.05, 0.35, 0.85), (1.95, 0.85, 0.9), "steel")
	M.box((0.05, 0.35, 0.15), (0.1, 0.85, 0.85), "steel")
	M.box((1.9, 0.35, 0.15), (1.95, 0.85, 0.85), "steel")
	M.box((0.1, 0.35, 0.15), (1.9, 0.75, 0.85), "milk", faces=("up",))
	legs(M, 0.05, 1.95, 0.1, 0.9, 0.3, r=0.03, inset=0.1)
	M.box((0.95, 0.85, 0.08), (1.05, 0.95, 0.92), "steel")              # the bridge
	M.box((0.97, 0.45, 0.4), (1.03, 0.85, 0.6), "steel")                # the knives
	M.cylinder("y", (1.0, 0.5), 0.07, 0.95, 1.0, "grey", 8)
	M.collide((0.05, 0, 0.1), (1.95, 0.85, 0.9))
	return M


@part
def cheese_press():
	"""A screw cheese press: a wooden base, steel uprights and crossbar, the screw and follower, a hooped wheel in its mould."""
	M = model("cheese_press", 1, 1, 1, particle="wood")
	M.box((0.1, 0, 0.15), (0.9, 0.12, 0.85), "wood")
	for x in (0.15, 0.85):
		M.box((x - 0.03, 0.12, 0.47), (x + 0.03, 0.95, 0.53), "steel")
	M.box((0.1, 0.9, 0.45), (0.9, 0.97, 0.55), "steel")
	M.cylinder("y", (0.5, 0.5), 0.02, 0.55, 1.0, "steel", 8)
	M.box((0.38, 0.97, 0.49), (0.62, 1.0, 0.51), "steel")              # the handle
	M.cylinder("y", (0.5, 0.5), 0.2, 0.5, 0.55, "wood", 12)             # follower
	M.cylinder("y", (0.5, 0.5), 0.2, 0.12, 0.5, "rind", 12)             # the wheel in its mould
	M.ring("y", (0.5, 0.5), 0.205, 0.2, 0.24, 0.01, "steel", 12)
	M.ring("y", (0.5, 0.5), 0.205, 0.38, 0.42, 0.01, "steel", 12)
	M.collide((0.1, 0, 0.15), (0.9, 1.0, 0.85))
	return M


@part
def aging_shelf():
	"""A wooden cheese aging rack, four shelves of cheddar wheels."""
	M = model("aging_shelf", 1, 2, 1, particle="wood")
	for x in (0.03, 0.97):
		for z in (0.15, 0.85):
			M.box((x - 0.03, 0, z - 0.03), (x + 0.03, 1.95, z + 0.03), "wood")
	for y in (0.1, 0.55, 1.0, 1.45, 1.9):
		M.box((0, y, 0.12), (1, y + 0.04, 0.88), "wood")
	for y in (0.14, 0.59, 1.04, 1.49):
		for x in (0.27, 0.73):
			M.cylinder("y", (x, 0.5), 0.2, y, y + 0.22, "rind", 12)
	M.collide((0, 0, 0.12), (1, 1.95, 0.88))
	return M


@part
def bottler():
	"""A hand bottle filler: a stainless table, a small header tank with two filling valves, bottles waiting."""
	M = model("bottler", 1, 1, 1, particle="steel")
	M.box((0.05, 0.55, 0.15), (0.95, 0.6, 0.85), "steel")
	legs(M, 0.05, 0.95, 0.15, 0.85, 0.55, r=0.025, inset=0.05)
	M.box((0.2, 0.85, 0.45), (0.8, 1.0, 0.8), "steel")
	for x in (0.35, 0.65):
		M.cylinder("y", (x, 0.5), 0.025, 0.72, 0.85, "steel", 8)
	for x in (0.2, 0.35, 0.5, 0.65, 0.8):
		M.cylinder("y", (x, 0.3), 0.04, 0.6, 0.78, "milk", 8)
		M.cylinder("y", (x, 0.3), 0.02, 0.78, 0.82, "milk", 8)
	M.collide((0.05, 0, 0.15), (0.95, 1.0, 0.85))
	return M


# ---- the controllers (plain blocks, not big parts) -----------------------------------------------------------------------

def controller(name, paint):
	M = model(name, 1, 1, 1, particle=paint)
	M.box((0.1, 0, 0.1), (0.9, 0.08, 0.9), "concrete")
	M.box((0.15, 0.08, 0.45), (0.85, 1.0, 0.85), paint)
	M.decal((0.22, 0.3, 0.44), (0.78, 0.92, 0.45), "controller", "north", paint)
	M.decal((0.3, 0.12, 0.44), (0.7, 0.28, 0.45), "buttons", "north", paint)
	M.pipe([(0.5, 0.08, 0.7), (0.5, 0.0, 0.7)], 1.5, "galv")
	return M


CONTROLLERS = {"barn_controller": "grey", "parlor_controller": "steel", "creamery_controller": "steel"}

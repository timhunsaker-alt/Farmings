"""Shared by every Farmings part model: the materials. Wildcatter's are used by reference (it is a hard dependency), so steel,
concrete and paint look the same as in Wildcatter; Farmings' own (hay, manure, milk, rubber mat...) are under farmings:block/mat/.
Prompts in art/source/PROMPTS.md.
"""
import mlib

W = "wildcatter:block/mat/"
F = "block/mat/"
TEX = {
	"steel": W + "mat_stainless", "galv": W + "mat_galvanized", "iron": W + "mat_cast_iron", "concrete": W + "mat_concrete",
	"rubber": W + "mat_rubber", "grey": W + "mat_paint_ansi61", "blue": W + "mat_paint_ansi70", "engine": W + "mat_paint_engine",
	"copper": W + "mat_copper", "brass": W + "mat_brass", "wood": W + "mat_wood_fir", "dark": W + "mat_oil_dark", "plastic": W + "mat_polymer_grey",
	"checker": W + "mat_checker_plate", "grating": W + "mat_grating",
	"nameplate": W + "decal_nameplate", "gauge": W + "decal_gauge", "controller": W + "decal_controller", "buttons": W + "decal_pushbuttons",
	"groove": F + "mat_grooved_concrete", "mat": F + "mat_rubber_mat", "hay": F + "mat_hay", "manure": F + "mat_manure", "milk": F + "mat_milk",
	"lane": F + "mat_cow_lane", "rind": F + "mat_cheese_rind", "yellow": F + "mat_paint_yellow", "water": F + "mat_water",
}


def model(name, w, h, d, mx=0, mz=0, particle="steel"):
	return mlib.Model(name, mlib.footprint_box(w, h, d, mx, mz), dict(TEX), TEX[particle])


def legs(M, x0, x1, z0, z1, top, tex="steel", r=0.03, inset=0.08):
	"""Four round legs with feet, from the floor up to `top`."""
	for x in (x0 + inset, x1 - inset):
		for z in (z0 + inset, z1 - inset):
			M.cylinder("y", (x, z), r, 0.0, top, tex, 8)
			M.cylinder("y", (x, z), r * 1.8, 0.0, 0.02, tex, 8)

package dev.farmings.barn;

import dev.farmings.husbandry.barn.BarnPart;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;

/**
 * The barn's blocks as the game builds them (docs/multiblocks/barn.md). Each is one item that fills its real size when placed, built
 * on Wildcatter's big parts. Footprints are drawn facing NORTH (x is west to east, z north to south), the way the barn is drawn: beds,
 * dividers and the scraper run north-south, so a barn whose alley runs east-west places them facing north.
 *
 * <p>Collision boxes are in sixteenths, per cell, facing north. They are placeholders for the real models (listed in PROGRESS.md).
 */
public enum BarnParts {
	STALL_BED("stall_bed", BarnPart.STALL_BED, Footprint.box(1, 1, 3, 0, 0), List.of(box(0, 0, 0, 16, 4, 16))),
	STALL_DIVIDER("stall_divider", BarnPart.STALL_DIVIDER, Footprint.box(1, 2, 3, 0, 0), List.of(box(7, 0, 0, 9, 16, 16))),
	FEED_BUNK("feed_bunk", BarnPart.FEED_BUNK, Footprint.box(1, 1, 1, 0, 0), List.of(box(0, 0, 0, 16, 10, 16))),
	HAY_FEEDER("hay_feeder", BarnPart.HAY_FEEDER, Footprint.box(1, 1, 1, 0, 0), List.of(box(0, 0, 2, 16, 14, 14))),
	WATER_TROUGH("water_trough", BarnPart.WATER_TROUGH, Footprint.box(2, 1, 1, 0, 0), List.of(box(0, 0, 2, 16, 8, 14))),
	ALLEY_FLOOR("alley_floor", BarnPart.ALLEY_FLOOR, Footprint.box(1, 1, 1, 0, 0), List.of(box(0, 0, 0, 16, 2, 16))),
	SCRAPER_RAIL("scraper_rail", BarnPart.SCRAPER_RAIL, Footprint.box(1, 1, 1, 0, 0), List.of(box(0, 0, 0, 16, 2, 16))),
	MANURE_SCRAPER("manure_scraper", BarnPart.MANURE_SCRAPER, Footprint.box(1, 1, 3, 0, 1), List.of(box(4, 0, 0, 12, 6, 16))),
	SCRAPER_DRIVE("scraper_drive", BarnPart.SCRAPER_DRIVE, Footprint.box(1, 1, 1, 0, 0), List.of(box(1, 0, 1, 15, 16, 15))),
	/** Its lower layer is solid ground and its top layer is a slab: the pit is set into the floor. The shape picks by layer. */
	COLLECTION_PIT("collection_pit", BarnPart.COLLECTION_PIT, Footprint.box(2, 2, 3, 0, 1), List.of(box(0, 0, 0, 16, 16, 16), box(0, 0, 0, 16, 2, 16)));

	public final String id;
	public final BarnPart part;
	public final Footprint footprint;
	/** Shapes by layer: the first for the bottom layer, the last for any higher one (one entry means the same for every layer). */
	private final List<double[]> shapes;

	BarnParts(String id, BarnPart part, Footprint footprint, List<double[]> shapes) {
		this.id = id;
		this.part = part;
		this.footprint = footprint;
		this.shapes = shapes;
	}

	private static double[] box(double x1, double y1, double z1, double x2, double y2, double z2) {
		return new double[] {x1, y1, z1, x2, y2, z2};
	}

	/** The box for a cell, by its layer in the part. */
	public double[] shapeFor(int layer) {
		return shapes.get(Math.min(layer, shapes.size() - 1));
	}

	public static BarnParts of(BarnPart part) {
		for (BarnParts p : values()) {
			if (p.part == part) {
				return p;
			}
		}
		throw new IllegalArgumentException("the controller is not a big part");
	}
}

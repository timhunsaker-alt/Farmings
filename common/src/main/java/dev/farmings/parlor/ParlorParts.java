package dev.farmings.parlor;

import dev.farmings.husbandry.parlor.ParlorPart;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;

/**
 * The parlor's blocks as the game builds them (docs/multiblocks/parlor.md). Each is one item that fills its real size when placed, on
 * Wildcatter's big parts. Footprints face NORTH: the stall runs north to south, so a parlor whose row runs east-west places stalls facing
 * north. Collision boxes are in sixteenths and are placeholders for real models.
 */
public enum ParlorParts {
	PARLOR_STALL("parlor_stall", ParlorPart.PARLOR_STALL, Footprint.box(1, 1, 2, 0, 0), List.of(box(0, 0, 0, 16, 4, 16))),
	MILKING_UNIT("milking_unit", ParlorPart.MILKING_UNIT, Footprint.box(1, 1, 1, 0, 0), List.of(box(4, 0, 4, 12, 12, 12))),
	VACUUM_PUMP("vacuum_pump", ParlorPart.VACUUM_PUMP, Footprint.box(1, 1, 1, 0, 0), List.of(box(1, 0, 1, 15, 14, 15))),
	MILK_LINE("milk_line", ParlorPart.MILK_LINE, Footprint.box(1, 1, 1, 0, 0), List.of(box(0, 0, 5, 16, 5, 11))),
	BULK_TANK("bulk_tank", ParlorPart.BULK_TANK, Footprint.box(2, 2, 2, 0, 0), List.of(box(0, 0, 0, 16, 16, 16)));

	public final String id;
	public final ParlorPart part;
	public final Footprint footprint;
	private final List<double[]> shapes;

	ParlorParts(String id, ParlorPart part, Footprint footprint, List<double[]> shapes) {
		this.id = id;
		this.part = part;
		this.footprint = footprint;
		this.shapes = shapes;
	}

	private static double[] box(double x1, double y1, double z1, double x2, double y2, double z2) {
		return new double[] {x1, y1, z1, x2, y2, z2};
	}

	public double[] shapeFor(int layer) {
		return shapes.get(Math.min(layer, shapes.size() - 1));
	}
}

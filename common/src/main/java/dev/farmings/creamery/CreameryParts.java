package dev.farmings.creamery;

import dev.farmings.process.creamery.CreameryPart;
import dev.wildcatter.multiblock.Footprint;
import java.util.List;

/**
 * The creamery's blocks (docs/multiblocks/creamery.md), on Wildcatter's big parts. Footprints face NORTH; a two-cell part runs east-west
 * when it faces north, which is along a row that runs east from the controller. Collision boxes in sixteenths.
 */
public enum CreameryParts {
	RECEIVING_TANK("receiving_tank", CreameryPart.RECEIVING_TANK, Footprint.box(2, 2, 2, 0, 0), List.of(box(0, 0, 0, 16, 16, 16)), 0),
	BATCH_PASTEURIZER("batch_pasteurizer", CreameryPart.BATCH_PASTEURIZER, Footprint.box(1, 2, 1, 0, 0), List.of(box(1, 0, 1, 15, 16, 15)), 12_000),
	CREAM_SEPARATOR("cream_separator", CreameryPart.CREAM_SEPARATOR, Footprint.box(1, 1, 1, 0, 0), List.of(box(3, 0, 3, 13, 15, 13)), 1_500),
	BUTTER_CHURN("butter_churn", CreameryPart.BUTTER_CHURN, Footprint.box(1, 1, 1, 0, 0), List.of(box(2, 0, 2, 14, 14, 14)), 1_500),
	CHEESE_VAT("cheese_vat", CreameryPart.CHEESE_VAT, Footprint.box(2, 1, 1, 0, 0), List.of(box(0, 0, 1, 16, 12, 15)), 750),
	CHEESE_PRESS("cheese_press", CreameryPart.CHEESE_PRESS, Footprint.box(1, 1, 1, 0, 0), List.of(box(2, 0, 2, 14, 16, 14)), 0),
	AGING_SHELF("aging_shelf", CreameryPart.AGING_SHELF, Footprint.box(1, 2, 1, 0, 0), List.of(box(0, 0, 2, 16, 16, 14)), 0),
	BOTTLER("bottler", CreameryPart.BOTTLER, Footprint.box(1, 1, 1, 0, 0), List.of(box(2, 0, 2, 14, 14, 14)), 0);

	public final String id;
	public final CreameryPart part;
	public final Footprint footprint;
	private final List<double[]> shapes;
	/** Motor or heater size, watts; 0 for an unpowered part. All proposed (creamery.md). */
	public final int watts;

	CreameryParts(String id, CreameryPart part, Footprint footprint, List<double[]> shapes, int watts) {
		this.id = id;
		this.part = part;
		this.footprint = footprint;
		this.shapes = shapes;
		this.watts = watts;
	}

	private static double[] box(double x1, double y1, double z1, double x2, double y2, double z2) {
		return new double[] {x1, y1, z1, x2, y2, z2};
	}

	public double[] shapeFor(int layer) {
		return shapes.get(Math.min(layer, shapes.size() - 1));
	}

	public static CreameryParts of(CreameryPart part) {
		for (CreameryParts p : values()) {
			if (p.part == part) {
				return p;
			}
		}
		return null;
	}
}

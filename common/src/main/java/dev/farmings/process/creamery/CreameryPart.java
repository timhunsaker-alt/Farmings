package dev.farmings.process.creamery;

/** The creamery's parts (docs/multiblocks/creamery.md). */
public enum CreameryPart {
	CONTROLLER, RECEIVING_TANK, BATCH_PASTEURIZER, CREAM_SEPARATOR, BUTTER_CHURN, CHEESE_VAT, CHEESE_PRESS, AGING_SHELF, BOTTLER;

	/** Cells the part takes along the row. */
	public int length() {
		return this == RECEIVING_TANK || this == CHEESE_VAT ? 2 : 1;
	}
}

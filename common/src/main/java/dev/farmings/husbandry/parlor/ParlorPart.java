package dev.farmings.husbandry.parlor;

/** The parts of the milking parlor (docs/multiblocks/parlor.md). A part that fills several cells shows as the same value in each. */
public enum ParlorPart {
	CONTROLLER("parlor controller"),
	PARLOR_STALL("parlor stall"),
	MILKING_UNIT("milking unit"),
	VACUUM_PUMP("vacuum pump"),
	MILK_LINE("milk line"),
	BULK_TANK("bulk tank"),
	COW_LANE("cow lane"),
	HOLDING_PEN("holding pen"),
	/** A one-cell raised stall for a dairy ewe (docs/multiblocks/parlor.md, "Sheep stalls"). */
	SHEEP_STALL("sheep stall");

	public final String label;

	ParlorPart(String label) {
		this.label = label;
	}
}

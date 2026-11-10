package dev.farmings.woolmill;

/** The wool mill's machines (docs/multiblocks/wool-mill.md). Motor ratings are *(proposed)*: no ratings were found. */
public enum WoolMachine {
	/** Hot water and detergent bowls with a grease centrifuge: greasy wool to clean wool and lanolin; the heater is most of the load. */
	SCOURING_BOWL("scouring_bowl", "Scouring Bowl", 3000),
	/** Opens and lines the fibres into a continuous sliver. */
	CARDING_MACHINE("carding_machine", "Carding Machine", 1500),
	/** Draws and twists sliver into yarn. */
	SPINNING_FRAME("spinning_frame", "Spinning Frame", 1000),
	/** Weaves yarn into cloth. */
	LOOM("loom_frame", "Power Loom", 750),
	/** Wets and works clean wool until the fibres mat into felt. */
	FELTING_TABLE("felting_table", "Felting Table", 500);

	public final String id;
	public final String title;
	public final double watts;

	WoolMachine(String id, String title, double watts) {
		this.id = id;
		this.title = title;
		this.watts = watts;
	}
}

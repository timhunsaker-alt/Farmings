package dev.farmings.husbandry.barn;

/** The parts of a freestall barn (docs/multiblocks/barn.md). A part that fills several cells shows as the same value in each of them. */
public enum BarnPart {
	CONTROLLER("barn controller"),
	STALL_BED("stall bed"),
	STALL_DIVIDER("stall divider"),
	FEED_BUNK("feed bunk"),
	HAY_FEEDER("hay feeder"),
	WATER_TROUGH("water trough"),
	ALLEY_FLOOR("alley floor"),
	SCRAPER_RAIL("scraper rail"),
	MANURE_SCRAPER("manure scraper"),
	SCRAPER_DRIVE("scraper drive"),
	COLLECTION_PIT("collection pit");

	public final String label;

	BarnPart(String label) {
		this.label = label;
	}
}

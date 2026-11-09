package dev.farmings.husbandry.parlor;

/** A view of the world for the parlor check: which parlor part, if any, is in a cell. x east, y up, z south. */
@FunctionalInterface
public interface ParlorGrid {
	/** The part in this cell, or null for anything else. */
	ParlorPart at(int x, int y, int z);
}

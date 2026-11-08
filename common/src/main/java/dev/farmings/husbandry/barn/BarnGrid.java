package dev.farmings.husbandry.barn;

/** A view of the world for the barn check: which barn part, if any, is in a cell. x east, y up, z south. */
@FunctionalInterface
public interface BarnGrid {
	/** The part in this cell, or null for anything else. */
	BarnPart at(int x, int y, int z);
}

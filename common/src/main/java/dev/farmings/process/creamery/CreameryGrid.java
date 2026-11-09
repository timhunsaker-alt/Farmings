package dev.farmings.process.creamery;

/** What part stands in a world cell, or null. */
@FunctionalInterface
public interface CreameryGrid {
	CreameryPart at(int x, int y, int z);
}

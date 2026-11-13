package dev.farmings.husbandry.parlor;

import java.util.Locale;

/**
 * Which animal's milk a bulk tank or the creamery holds (docs/multiblocks/parlor.md, creamery.md). A tank or creamery holds one kind at a
 * time and changes kind only when empty. Plain Java.
 */
public enum MilkKind {
	COW, SHEEP, GOAT;

	/** "cow", "sheep", "goat": for readouts and faults. */
	public String label() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The fault a parlor shows when the tank holds this kind and another is waiting: "TANK HOLDS GOAT MILK". */
	public String holdsFault() {
		return "TANK HOLDS " + name() + " MILK";
	}

	/** The kind saved by name, or the old saves' sheep flag when there is no name. */
	public static MilkKind load(String name, boolean oldSheepFlag) {
		for (MilkKind k : values()) {
			if (k.name().equals(name)) {
				return k;
			}
		}
		return oldSheepFlag ? SHEEP : COW;
	}
}

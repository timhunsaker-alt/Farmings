package dev.farmings.husbandry.poultry;

/**
 * Egg grading (docs/eggs.md). Size: the USDA weight classes, minimum net weight per dozen from "How to Buy Eggs" (USDA AMS Home and Garden
 * Bulletin 264), divided by 12 for one egg. Grade: USDA grades eggs by their interior, which falls with age; the game grades by days since
 * laying (proposed thresholds), and a floor egg is B at best.
 */
public final class EggGrading {
	private EggGrading() {
	}

	private static final double OZ = 28.349523125;

	public enum Size {
		JUMBO("Jumbo", 30), EXTRA_LARGE("Extra Large", 27), LARGE("Large", 24), MEDIUM("Medium", 21), SMALL("Small", 18), PEEWEE("Peewee", 15),
		UNDERGRADE("Undergrade", 0);

		public final String label;
		/** Minimum ounces per dozen. */
		public final int ozPerDozen;

		Size(String label, int ozPerDozen) {
			this.label = label;
			this.ozPerDozen = ozPerDozen;
		}

		/** Minimum grams for one egg of this size. */
		public double minGrams() {
			return ozPerDozen * OZ / 12.0;
		}
	}

	public enum Grade {
		AA, A, B
	}

	/** Grade AA under this many days since laying, A under {@link #A_DAYS} (proposed). */
	public static final double AA_DAYS = 7;
	public static final double A_DAYS = 21;
	public static final int CARTON = 12;

	public static Size size(double grams) {
		for (Size s : Size.values()) {
			if (s != Size.UNDERGRADE && grams >= s.minGrams() - 1e-9) {
				return s;
			}
		}
		return Size.UNDERGRADE;
	}

	public static Grade grade(double ageDays, boolean floorEgg) {
		Grade g = ageDays < AA_DAYS ? Grade.AA : ageDays < A_DAYS ? Grade.A : Grade.B;
		return floorEgg && g != Grade.B ? Grade.B : g;
	}
}

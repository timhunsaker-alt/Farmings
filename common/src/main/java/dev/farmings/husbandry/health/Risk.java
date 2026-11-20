package dev.farmings.husbandry.health;

/**
 * The risk factors of docs/animal-health.md, "Risk factors (R, multiplied)". All shapes are proposed; the HEALTH curve is a game rule (the
 * doc says so).
 */
public final class Risk {
	/** Fed share under this counts as underfed. */
	public static final double UNDERFED = 0.8;
	public static final double UNDERFED_FACTOR = 1.5;
	public static final double POOR_RATION = 0.5;
	public static final double POOR_RATION_FACTOR = 1.25;
	public static final double COLD_FACTOR = 2.0;
	/** Teat dip: "70 percent or more" fewer new infections (NMC 2020). */
	public static final double TEAT_DIP_FACTOR = 0.3;
	public static final double FOOTBATH_FACTOR = 0.3;
	/** A fresh cow (under 30 days in milk) has twice the mastitis risk (proposed). */
	public static final double FRESH_DAYS = 30;
	public static final double FRESH_FACTOR = 2.0;

	private Risk() {
	}

	/** The constant hazard per real day that gives {@code share} of animals a case over {@code days}: -ln(1 - share) / days. */
	public static double hazard(double share, double days) {
		return -Math.log(1 - share) / days;
	}

	/** The chance of at least one event at hazard {@code perDay} over {@code days}. */
	public static double chance(double perDay, double days) {
		return perDay <= 0 || days <= 0 ? 0 : 1 - Math.exp(-perDay * days);
	}

	/** The genome's HEALTH (or HARDINESS) score, 0-100, as a multiplier: 50 is 1, 100 is 0.5, 0 is 2. */
	public static double genetic(int score) {
		return Math.pow(2, (50 - Math.max(0, Math.min(100, score))) / 50.0);
	}

	/** Dirt (0 clean to 1 fully dirty): 1 + dirt, or 1 + 2 x dirt for the dirt-borne diseases. */
	public static double dirt(double dirt, boolean dirtBorne) {
		double d = Math.max(0, Math.min(1, dirt));
		return 1 + (dirtBorne ? 2 : 1) * d;
	}

	/** Feed: underfed and a poor ration each raise the risk. A ration score below 0 means none is known. */
	public static double feed(double fed, double rationScore) {
		double f = fed < UNDERFED ? UNDERFED_FACTOR : 1;
		if (rationScore >= 0 && rationScore < POOR_RATION) {
			f *= POOR_RATION_FACTOR;
		}
		return f;
	}

	/** What one animal's situation is this second: the inputs to the model, read off the entity. */
	public record Inputs(int healthScore, int hardiness, double dirt, double fed, double rationScore, boolean cold, double ageDays,
			boolean inMilk, double daysInMilk, boolean wet, boolean pastureInfective, boolean grainJump, int wildFowlNear, boolean outbreakNear) {
		public static Inputs typical(double ageDays) {
			return new Inputs(50, 50, 0, 1, -1, false, ageDays, false, 0, false, false, false, 0, false);
		}

		public Inputs withDirt(double d) {
			return new Inputs(healthScore, hardiness, d, fed, rationScore, cold, ageDays, inMilk, daysInMilk, wet, pastureInfective, grainJump, wildFowlNear, outbreakNear);
		}

		public Inputs withMilk(double dim) {
			return new Inputs(healthScore, hardiness, dirt, fed, rationScore, cold, ageDays, true, dim, wet, pastureInfective, grainJump, wildFowlNear, outbreakNear);
		}

		public Inputs withHealth(int score) {
			return new Inputs(score, hardiness, dirt, fed, rationScore, cold, ageDays, inMilk, daysInMilk, wet, pastureInfective, grainJump, wildFowlNear, outbreakNear);
		}

		public Inputs withWet(boolean w, boolean infective) {
			return new Inputs(healthScore, hardiness, dirt, fed, rationScore, cold, ageDays, inMilk, daysInMilk, w, infective, grainJump, wildFowlNear, outbreakNear);
		}

		public Inputs withGrainJump(boolean j) {
			return new Inputs(healthScore, hardiness, dirt, fed, rationScore, cold, ageDays, inMilk, daysInMilk, wet, pastureInfective, j, wildFowlNear, outbreakNear);
		}

		public Inputs withCold(boolean c) {
			return new Inputs(healthScore, hardiness, dirt, fed, rationScore, c, ageDays, inMilk, daysInMilk, wet, pastureInfective, grainJump, wildFowlNear, outbreakNear);
		}

		public Inputs withWildFowl(int n, boolean outbreak) {
			return new Inputs(healthScore, hardiness, dirt, fed, rationScore, cold, ageDays, inMilk, daysInMilk, wet, pastureInfective, grainJump, n, outbreak);
		}
	}
}

package dev.farmings.process.curing;

/**
 * Curing hams and bellies on the curing rack (docs/multiblocks/curing-room.md). Plain Java: a piece's phases and their lengths in real days,
 * and what comes out. The rack runs them at the livestock (machine) scale, 15:1.
 */
public final class Curing {
	public enum Kind { HAM, BACON }

	public enum Phase { CURE, EQUALIZE, SMOKE, AGE, DONE, SPOILED }

	/** "two days per pound of ham" (Univ. of Kentucky ASC-213). */
	public static final double HAM_CURE_DAYS_PER_KG = 2 * 2.20462;
	/** "stored in a 50 to 60° F environment for approximately 14 days" (Virginia Tech 458-223). */
	public static final double HAM_EQUALIZE_DAYS = 14;
	/** Cold smoke "one to three days" (VT): 2. */
	public static final double HAM_SMOKE_DAYS = 2;
	/** "Age hams for 45 to 180 days" (VT): 90 *(proposed, inside the range)*. */
	public static final double HAM_AGE_DAYS = 90;
	/** A country ham ends at 77.5% of its fresh weight: "at least 18% — but typically 20 to 25%" lost (FSIS). */
	public static final double HAM_YIELD = 0.775;
	/** "Cure in refrigerator for five to seven days" (Oregon State PNW 784): 7. */
	public static final double BACON_CURE_DAYS = 7;
	/** Hot smoke "approximately three hours" (OSU). */
	public static final double BACON_SMOKE_DAYS = 3.0 / 24;
	/** Bacon yield NOT FOUND: 90% *(proposed)*. */
	public static final double BACON_YIELD = 0.90;
	/** A bacon item is 0.5 kg *(proposed)*. */
	public static final double BACON_ITEM_KG = 0.5;
	/** The cure stops above this room temperature: "36 to 40° F" (VT) is 2-4 °C; the rack allows the cold room's 4 °C set point. */
	public static final double CURE_MAX_C = 4.5;
	/** Fresh meat not cold this long spoils on the rack: fresh pork's room life, 1 real day (food.md). */
	public static final double WARM_SPOIL_DAYS = 1;
	/** One cure item cures 1 kg of meat (curing-room.md, "Salt"). */
	public static final double CURE_PER_KG = 1;

	private Curing() {
	}

	/** Real days a phase takes for a piece of {@code kg}. */
	public static double days(Kind k, Phase p, double kg) {
		return switch (p) {
			case CURE -> k == Kind.HAM ? HAM_CURE_DAYS_PER_KG * kg : BACON_CURE_DAYS;
			case EQUALIZE -> k == Kind.HAM ? HAM_EQUALIZE_DAYS : 0;
			case SMOKE -> k == Kind.HAM ? HAM_SMOKE_DAYS : BACON_SMOKE_DAYS;
			case AGE -> k == Kind.HAM ? HAM_AGE_DAYS : 0;
			default -> 0;
		};
	}

	/** The phase after this one (skipping any of zero length). */
	public static Phase next(Kind k, Phase p) {
		Phase n = switch (p) {
			case CURE -> Phase.EQUALIZE;
			case EQUALIZE -> Phase.SMOKE;
			case SMOKE -> Phase.AGE;
			default -> Phase.DONE;
		};
		return n != Phase.DONE && days(k, n, 1) <= 0 ? next(k, n) : n;
	}

	/** One piece on the rack, as plain numbers: its kind, fresh kg, phase, real days into the phase, and real days spent warm while fresh. */
	public static final class Piece {
		public final Kind kind;
		public final double kg;
		public Phase phase = Phase.CURE;
		public double progress;
		public double warmDays;

		public Piece(Kind kind, double kg) {
			this.kind = kind;
			this.kg = kg;
		}

		/**
		 * Moves the piece {@code days} on: the cure only while {@code cold}, smoking only while {@code smoked}; the other phases run anyway.
		 * Fresh meat (in its cure) that is not cold for a real day spoils.
		 */
		public void step(double days, boolean cold, boolean smoked) {
			if (phase == Phase.DONE || phase == Phase.SPOILED) {
				return;
			}
			if (phase == Phase.CURE && !cold) {
				warmDays += days;
				if (warmDays > WARM_SPOIL_DAYS) {
					phase = Phase.SPOILED;
				}
				return;
			}
			if (phase == Phase.SMOKE && !smoked) {
				return;
			}
			progress += days;
			double need = days(kind, phase, kg);
			if (progress >= need) {
				double over = progress - need;
				phase = next(kind, phase);
				progress = 0;
				if (over > 0 && phase != Phase.DONE) {
					step(over, cold, smoked);
				}
			}
		}

		/** What it gives when done: a country ham's kg, or bacon items. */
		public double outKg() {
			return kind == Kind.HAM ? kg * HAM_YIELD : kg * BACON_YIELD;
		}

		public int baconItems() {
			return (int) Math.floor(outKg() / BACON_ITEM_KG + 1e-9);
		}
	}

	// ---- lard and sausage ----

	/** Lard a kg of fat: 0.75 *(proposed; a blog's pint a pound, TO VERIFY)*. */
	public static final double LARD_YIELD = 0.75;
	/** A rendering batch, 3 real hours *(proposed)*. */
	public static final double RENDER_DAYS = 3.0 / 24;
	/** Sausage: 3 shoulders (6 kg) + 2 fat (2 kg) = 25% fat ("20 to 30 percent", USDA 1978) + 1 salt, into 32 links of 0.25 kg *(proposed sizes)*. */
	public static final int SAUSAGE_SHOULDERS = 3;
	public static final int SAUSAGE_FAT = 2;
	public static final int SAUSAGE_LINKS = 32;

	public static double sausageFatShare() {
		return SAUSAGE_FAT * 1.0 / (SAUSAGE_SHOULDERS * 2.0 + SAUSAGE_FAT * 1.0);
	}
}

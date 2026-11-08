package dev.farmings.husbandry.barn;

/**
 * One pass of the manure scraper (docs/multiblocks/barn.md, "How it runs"): the blade starts beside the drive, moves along the alley
 * one cell at a time pushing manure ahead of it, empties into the pit at the end, and returns. This is the decision logic only; the
 * game moves the blocks and the items. Plain Java, so it is tested without Minecraft.
 *
 * <p>Each tick the barn tells it what it sees ({@link Look}) and it answers with what to do ({@link Action}). While a pass runs
 * the scraper only moves when the drive is powered; with no power it holds where it is. The controller shows {@link #fault()}.
 */
public final class ScraperPass {
	/** How long the blade takes to cross one cell, in ticks. *(proposed, unverified: real alley scrapers are slow, but no figure was found)* */
	public static final int TICKS_PER_CELL = 20;

	public enum Phase { IDLE, PUSHING, RETURNING }

	/** Why the scraper is not moving, as the controller reads it (docs/multiblocks/barn.md). */
	public enum Fault {
		NONE("RUNNING"),
		NO_POWER("NO POWER"),
		PIT_FULL("PIT FULL"),
		BLOCKED("BLOCKED"),
		NO_SCRAPER("NO SCRAPER");

		public final String label;

		Fault(String label) {
			this.label = label;
		}
	}

	public enum Action { NONE, MOVE_TO_PIT, MOVE_TO_DRIVE, DUMP }

	/**
	 * What the barn sees this tick.
	 *
	 * @param scraperPresent    the blade is on the rail
	 * @param atPitEnd          the blade is in the last alley cell, next to the pit
	 * @param atRest            the blade is in the first cell beside the drive
	 * @param powered           the drive's motor is running
	 * @param pitwardClear      the cell ahead (towards the pit) is free of anything that would stop the blade
	 * @param drivewardClear    the cell behind (towards the drive) is free
	 * @param pitHasRoom        the pit can take at least one more manure item
	 */
	public record Look(boolean scraperPresent, boolean atPitEnd, boolean atRest, boolean powered, boolean pitwardClear, boolean drivewardClear,
			boolean pitHasRoom) {
	}

	private Phase phase = Phase.IDLE;
	private Fault fault = Fault.NONE;
	private int timer;

	public Phase phase() {
		return phase;
	}

	public Fault fault() {
		return fault;
	}

	public boolean running() {
		return phase != Phase.IDLE;
	}

	/** Whether a pass is wanted: asks to start one. It does not start when the pit is already full or there is no blade. */
	public boolean start(Look look) {
		if (phase != Phase.IDLE) {
			return false;
		}
		if (!look.scraperPresent()) {
			fault = Fault.NO_SCRAPER;
			return false;
		}
		if (!look.pitHasRoom()) {
			fault = Fault.PIT_FULL;
			return false;
		}
		phase = Phase.PUSHING;
		timer = 0;
		fault = Fault.NONE;
		return true;
	}

	public Action tick(Look look) {
		if (phase == Phase.IDLE) {
			if (!look.scraperPresent()) {
				fault = Fault.NO_SCRAPER;
			} else if (fault == Fault.NO_SCRAPER) {
				fault = Fault.NONE;
			}
			return Action.NONE;
		}
		if (!look.scraperPresent()) {
			fault = Fault.NO_SCRAPER;
			return Action.NONE;
		}
		if (!look.powered()) {
			fault = Fault.NO_POWER;
			return Action.NONE;
		}
		fault = Fault.NONE;
		if (phase == Phase.PUSHING) {
			if (look.atPitEnd()) {
				if (!look.pitHasRoom()) {
					fault = Fault.PIT_FULL;
					return Action.NONE;
				}
				phase = Phase.RETURNING;
				timer = 0;
				return Action.DUMP;
			}
			if (timer < TICKS_PER_CELL) {
				timer++;
				return Action.NONE;
			}
			if (!look.pitwardClear()) {
				fault = Fault.BLOCKED;
				return Action.NONE;
			}
			timer = 0;
			return Action.MOVE_TO_PIT;
		}
		// RETURNING
		if (look.atRest()) {
			phase = Phase.IDLE;
			timer = 0;
			return Action.NONE;
		}
		if (timer < TICKS_PER_CELL) {
			timer++;
			return Action.NONE;
		}
		if (!look.drivewardClear()) {
			fault = Fault.BLOCKED;
			return Action.NONE;
		}
		timer = 0;
		return Action.MOVE_TO_DRIVE;
	}

	/** Saved with the controller: phase, timer, and the last fault. */
	public int[] save() {
		return new int[] {phase.ordinal(), timer, fault.ordinal()};
	}

	public void load(int[] saved) {
		if (saved != null && saved.length == 3) {
			phase = Phase.values()[Math.floorMod(saved[0], Phase.values().length)];
			timer = Math.max(0, saved[1]);
			fault = Fault.values()[Math.floorMod(saved[2], Fault.values().length)];
		}
	}
}

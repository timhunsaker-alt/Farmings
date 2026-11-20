package dev.farmings.husbandry.health;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One animal's health (docs/animal-health.md): its cases, somatic cell count, vaccinations, withdrawals, stress window, quarantine and
 * the last 16 events. Every time is an absolute game tick. It is saved on the animal as one string ({@link #encode}), so it survives a
 * save and a chunk unload with the rest of the animal's state.
 */
public final class HealthRecord {
	/** How far a case has gone. */
	public enum Stage {
		/** Infected, no signs yet (mastitis: subclinical). */
		INCUBATING,
		/** Mastitis only: high cells, normal-looking milk. */
		SUBCLINICAL,
		SICK,
		CHRONIC,
		/** Strangles only: recovered, shows nothing, still spreads it. */
		CARRIER,
		IMMUNE;

		public boolean active() {
			return this != IMMUNE;
		}

		/** Spreads to the rest of its group. */
		public boolean contagious() {
			return this == SICK || this == CARRIER || this == CHRONIC;
		}
	}

	/** One disease's course on this animal. {@code until} is when the stage ends (Long.MAX_VALUE: never on its own). */
	public static final class Case {
		public final Disease disease;
		public Stage stage;
		public long since;
		public long until;
		public boolean treated;

		public Case(Disease disease, Stage stage, long since, long until) {
			this.disease = disease;
			this.stage = stage;
			this.since = since;
			this.until = until;
		}
	}

	/** One vaccine's doses: how many in the current course and when the last was given. */
	public record Doses(int doses, long last) {
	}

	public static final int EVENTS = 16;

	private final List<Case> cases = new ArrayList<>();
	private final Map<Vaccine, Doses> vaccines = new EnumMap<>(Vaccine.class);
	private final Deque<String> events = new ArrayDeque<>();
	private int scc = Scc.HEALTHY;
	private long milkUntil = -1;
	private long meatUntil = -1;
	private long stressUntil = -1;
	private long footbathUntil = -1;
	private long quarantineUntil = -1;
	private long teatDipped = -1;
	private long lastTick = -1;
	private long lastExam = -1;
	private boolean reported;
	/** The housing it was last in (a packed position; Long.MIN_VALUE for none): a change opens the stress window. */
	private long home = Long.MIN_VALUE;
	/** The isolation pen holding it (a packed position; Long.MIN_VALUE for none). */
	private long pen = Long.MIN_VALUE;

	public List<Case> cases() {
		return cases;
	}

	public Case caseOf(Disease d) {
		for (Case c : cases) {
			if (c.disease == d) {
				return c;
			}
		}
		return null;
	}

	/** An active case (not immunity) of this disease. */
	public boolean has(Disease d) {
		Case c = caseOf(d);
		return c != null && c.stage.active();
	}

	public boolean has(Disease d, Stage s) {
		Case c = caseOf(d);
		return c != null && c.stage == s;
	}

	/** Any case showing signs now. */
	public boolean sick() {
		for (Case c : cases) {
			if (c.stage == Stage.SICK || c.stage == Stage.CHRONIC && c.disease != Disease.MASTITIS) {
				return true;
			}
		}
		return false;
	}

	public boolean coughing() {
		for (Case c : cases) {
			if (c.stage == Stage.SICK && c.disease.coughs()) {
				return true;
			}
		}
		return false;
	}

	public boolean lame() {
		for (Case c : cases) {
			if ((c.stage == Stage.SICK || c.stage == Stage.CHRONIC) && c.disease.lames()) {
				return true;
			}
		}
		return false;
	}

	/** A notifiable disease this animal has, active, or null. */
	public Disease notifiable() {
		for (Case c : cases) {
			if (c.disease.notifiable && c.stage.active()) {
				return c.disease;
			}
		}
		return null;
	}

	public void infect(Disease d, Stage stage, long now, long until) {
		cases.removeIf(c -> c.disease == d);
		cases.add(new Case(d, stage, now, until));
	}

	public void remove(Disease d) {
		cases.removeIf(c -> c.disease == d);
	}

	// ---- production effects (docs/animal-health.md, each disease's "Cost"; all proposed) ----

	/** Clinical mastitis: abnormal milk, which may not go in the tank (PMO, abnormal milk). */
	public boolean abnormalMilk() {
		return has(Disease.MASTITIS, Stage.SICK);
	}

	/** Milk is held from the tank: abnormal, or under a milk withdrawal. */
	public boolean milkHeld(long now) {
		return abnormalMilk() || now < milkUntil;
	}

	public boolean meatHeld(long now) {
		return now < meatUntil;
	}

	public double milkFactor() {
		double f = 1;
		for (Case c : cases) {
			if (c.disease == Disease.MASTITIS && (c.stage == Stage.SUBCLINICAL || c.stage == Stage.CHRONIC)) {
				f *= 0.95;
			} else if (c.disease == Disease.FOOT_ROT && (c.stage == Stage.SICK || c.stage == Stage.CHRONIC)) {
				f *= 0.9;
			} else if (c.stage == Stage.SICK && c.disease != Disease.MASTITIS) {
				f *= c.disease == Disease.ENTEROTOXAEMIA ? 0 : 0.75;
			}
		}
		return f;
	}

	public double growthFactor() {
		double f = 1;
		for (Case c : cases) {
			if (c.disease == Disease.MASTITIS) {
				continue;
			}
			if (c.disease == Disease.FOOT_ROT && (c.stage == Stage.SICK || c.stage == Stage.CHRONIC)) {
				f *= 0.9;
			} else if (c.stage == Stage.SICK) {
				f *= c.disease == Disease.COCCIDIOSIS || c.disease == Disease.ENTEROTOXAEMIA ? 0 : 0.75;
			} else if (c.stage == Stage.CHRONIC) {
				f *= c.disease == Disease.MAREKS ? 0.5 : 0.75;
			}
		}
		return f;
	}

	/** A sick bird does not lay; a bird with Marek's stops for good. */
	public boolean laying() {
		for (Case c : cases) {
			if (c.stage == Stage.SICK || c.disease == Disease.MAREKS && c.stage == Stage.CHRONIC) {
				return false;
			}
		}
		return true;
	}

	/** Health points lost per game day while sick: 1, as the dirty-stall rule (proposed); the floor is the caller's. */
	public double healthLossPerGameDay() {
		for (Case c : cases) {
			if (c.stage == Stage.SICK && c.disease == Disease.ENTEROTOXAEMIA) {
				return 100;
			}
		}
		return sick() ? 1 : 0;
	}

	// ---- treatment and vaccination ----

	/**
	 * Gives a course of {@code drug}: every active case it treats starts to recover ({@code treatedDays} from now), and the withdrawals are
	 * set from the label. Returns the cases it treated (empty: given anyway, withdrawals still apply).
	 */
	public List<Disease> treat(Drug drug, long now, long milkTicks, long meatTicks, java.util.function.DoubleFunction<Long> realDaysToTicks) {
		List<Disease> treated = new ArrayList<>();
		for (Case c : cases) {
			if (drug.treats.contains(c.disease) && (c.stage == Stage.SICK || c.stage == Stage.CHRONIC || c.stage == Stage.SUBCLINICAL
					|| c.stage == Stage.INCUBATING)) {
				c.treated = true;
				c.stage = Stage.SICK;
				c.until = now + realDaysToTicks.apply(c.disease.treatedDays);
				treated.add(c.disease);
			}
		}
		if (milkTicks > 0) {
			milkUntil = Math.max(milkUntil, now + milkTicks);
		}
		if (meatTicks > 0) {
			meatUntil = Math.max(meatUntil, now + meatTicks);
		}
		event(now, drug.label + (treated.isEmpty() ? "" : " for " + String.join(", ", treated.stream().map(d -> d.label).toList())));
		return treated;
	}

	/**
	 * Gives a dose of {@code v}. Returns the new dose count in the course, or 0 when the dose counted for nothing (too young). A dose after the
	 * primary gap has run out twice over restarts the course; a dose within a year of a finished course is a booster.
	 */
	public int vaccinate(Vaccine v, long now, double ageDays, java.util.function.DoubleFunction<Long> realDaysToTicks) {
		if (ageDays < v.minAgeDays) {
			event(now, v.label + " vaccine given too young: no effect");
			return 0;
		}
		Doses d = vaccines.get(v);
		int doses = 1;
		if (d != null) {
			long gap = now - d.last();
			boolean complete = d.doses() >= v.primaryDoses;
			if (complete && gap <= realDaysToTicks.apply(Vaccine.BOOSTER_DAYS + 30)) {
				doses = d.doses() + 1;
			} else if (!complete && gap <= realDaysToTicks.apply(Math.max(1, v.primaryGapDays * 2))) {
				doses = d.doses() + 1;
			}
		}
		vaccines.put(v, new Doses(doses, now));
		event(now, v.label + " vaccine, dose " + doses);
		return doses;
	}

	/** Protected by {@code v} now: the primary course done and the last dose within a year. */
	public boolean protectedBy(Vaccine v, long now, java.util.function.DoubleFunction<Long> realDaysToTicks) {
		Doses d = vaccines.get(v);
		return d != null && d.doses() >= v.primaryDoses && now - d.last() <= realDaysToTicks.apply(Vaccine.BOOSTER_DAYS);
	}

	/** A dose is due: the course unfinished, or a booster a year on. */
	public boolean doseDue(Vaccine v, long now, java.util.function.DoubleFunction<Long> realDaysToTicks) {
		Doses d = vaccines.get(v);
		if (d == null) {
			return false;
		}
		if (d.doses() < v.primaryDoses) {
			return now - d.last() >= realDaysToTicks.apply(v.primaryGapDays);
		}
		return now - d.last() >= realDaysToTicks.apply(Vaccine.BOOSTER_DAYS);
	}

	public Map<Vaccine, Doses> vaccines() {
		return vaccines;
	}

	// ---- simple state ----

	public int scc() {
		return scc;
	}

	public void setScc(int scc) {
		this.scc = scc;
	}

	public long milkUntil() {
		return milkUntil;
	}

	public long meatUntil() {
		return meatUntil;
	}

	public void setWithdrawalsForTest(long milk, long meat) {
		this.milkUntil = milk;
		this.meatUntil = meat;
	}

	public boolean stressed(long now) {
		return now < stressUntil;
	}

	public void stress(long until) {
		stressUntil = Math.max(stressUntil, until);
	}

	public boolean footbathed(long now) {
		return now < footbathUntil;
	}

	public void footbath(long until) {
		footbathUntil = until;
	}

	public long quarantineUntil() {
		return quarantineUntil;
	}

	public void setQuarantineUntil(long until) {
		quarantineUntil = until;
	}

	public boolean inQuarantine(long now) {
		return quarantineUntil >= 0 && now < quarantineUntil;
	}

	public long teatDipped() {
		return teatDipped;
	}

	public void dipTeats(long now) {
		teatDipped = now;
	}

	public long lastTick() {
		return lastTick;
	}

	public void setLastTick(long t) {
		lastTick = t;
	}

	public long lastExam() {
		return lastExam;
	}

	public void examined(long now) {
		lastExam = now;
	}

	public boolean reported() {
		return reported;
	}

	public void setReported(boolean r) {
		reported = r;
	}

	public long home() {
		return home;
	}

	public void setHome(long h) {
		home = h;
	}

	public long pen() {
		return pen;
	}

	public void setPen(long p) {
		pen = p;
	}

	public void event(long now, String text) {
		events.addLast(now + "|" + text.replace(';', ',').replace('|', '/'));
		while (events.size() > EVENTS) {
			events.removeFirst();
		}
	}

	/** The last events, oldest first, as "tick|text". */
	public List<String> events() {
		return new ArrayList<>(events);
	}

	// ---- saving: one line of ;-separated fields ----

	public String encode() {
		StringBuilder b = new StringBuilder("v1");
		b.append(";scc=").append(scc).append(";milk=").append(milkUntil).append(";meat=").append(meatUntil).append(";stress=").append(stressUntil)
				.append(";bath=").append(footbathUntil).append(";quar=").append(quarantineUntil).append(";dip=").append(teatDipped)
				.append(";last=").append(lastTick).append(";exam=").append(lastExam).append(";rep=").append(reported).append(";home=").append(home).append(";pen=").append(pen);
		for (Case c : cases) {
			b.append(";case=").append(c.disease.name()).append(',').append(c.stage.name()).append(',').append(c.since).append(',').append(c.until)
					.append(',').append(c.treated);
		}
		vaccines.forEach((v, d) -> b.append(";vac=").append(v.name()).append(',').append(d.doses()).append(',').append(d.last()));
		for (String e : events) {
			b.append(";ev=").append(e);
		}
		return b.toString();
	}

	public static HealthRecord decode(String s) {
		HealthRecord r = new HealthRecord();
		if (s == null || !s.startsWith("v1")) {
			return r;
		}
		for (String part : s.split(";")) {
			int eq = part.indexOf('=');
			if (eq < 0) {
				continue;
			}
			String k = part.substring(0, eq);
			String v = part.substring(eq + 1);
			try {
				switch (k) {
					case "scc" -> r.scc = Integer.parseInt(v);
					case "milk" -> r.milkUntil = Long.parseLong(v);
					case "meat" -> r.meatUntil = Long.parseLong(v);
					case "stress" -> r.stressUntil = Long.parseLong(v);
					case "bath" -> r.footbathUntil = Long.parseLong(v);
					case "quar" -> r.quarantineUntil = Long.parseLong(v);
					case "dip" -> r.teatDipped = Long.parseLong(v);
					case "last" -> r.lastTick = Long.parseLong(v);
					case "exam" -> r.lastExam = Long.parseLong(v);
					case "rep" -> r.reported = Boolean.parseBoolean(v);
					case "home" -> r.home = Long.parseLong(v);
					case "pen" -> r.pen = Long.parseLong(v);
					case "case" -> {
						String[] f = v.split(",");
						Disease d = Disease.byName(f[0]);
						if (d != null) {
							Case c = new Case(d, Stage.valueOf(f[1]), Long.parseLong(f[2]), Long.parseLong(f[3]));
							c.treated = Boolean.parseBoolean(f[4]);
							r.cases.add(c);
						}
					}
					case "vac" -> {
						String[] f = v.split(",");
						Vaccine vac = Vaccine.byName(f[0]);
						if (vac != null) {
							r.vaccines.put(vac, new Doses(Integer.parseInt(f[1]), Long.parseLong(f[2])));
						}
					}
					case "ev" -> r.events.addLast(v);
					default -> {
					}
				}
			} catch (RuntimeException e) {
				// a damaged field is skipped; the rest of the record stands
			}
		}
		return r;
	}
}

package dev.farmings.husbandry.health;

import dev.farmings.genetics.Species;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.health.HealthRecord.Case;
import dev.farmings.husbandry.health.HealthRecord.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Moves one animal's health forward (docs/animal-health.md, "How disease spreads"): each case along its course, then new infections at
 * {@code 1 - exp(-(h0 + beta x I/N) x R x d)}. Deterministic: every roll comes from {@link Roll}, which the caller seeds from the world,
 * the animal and the tick.
 */
public final class HealthModel {
	/** Mastitis: a subclinical case goes clinical 1 time in 3 (proposed). */
	public static final double MASTITIS_CLINICAL = 1.0 / 3;
	/** Subclinical mastitis lasts 10 real days, then half cure and half go chronic (proposed). */
	public static final double MASTITIS_SUBCLINICAL_DAYS = 10;
	/** A chronic mastitis cow flares into a clinical case at 2% a real day (proposed). */
	public static final double MASTITIS_FLARE = 0.02;
	/** BRD outside the stress window: calves and heifers under a year keep a tenth of the hazard (proposed). */
	public static final double BRD_YOUNG_SHARE = 0.1;
	/** The stress window after taming, a move or weaning (75% of cases in the first 40-55 days: Smith et al. 2023). */
	public static final double BRD_WINDOW_DAYS = 45;
	public static final double SCOURS_AGE_DAYS = 30;
	/** Weaning opens the BRD stress window: dairy calves at 8 weeks (proposed, from the common 6-8 weeks; TO VERIFY), beef calves at 205 days
	 * (the age beef weaning weights are adjusted to; TO VERIFY). Farmings calves have no suckling to stop, so the age is the weaning. */
	public static final double DAIRY_WEANING_DAYS = 56;
	public static final double BEEF_WEANING_DAYS = 205;
	public static final double COCCIDIOSIS_AGE_DAYS = 56;
	public static final double MAREKS_FROM_DAYS = 42;
	public static final double MAREKS_TO_DAYS = 210;
	/** Contagious foot rot picked up off an infective, wet pasture, a real day (proposed). */
	public static final double PASTURE_FOOT_ROT = 0.05;
	/** Overeating disease after a grain jump: 1 in 10 (proposed). */
	public static final double ENTEROTOXAEMIA_CHANCE = 0.1;
	/** Strangles: 1 in 10 recovered horses carries it for 9.2 months (EquiManagement). */
	public static final double STRANGLES_CARRIER = 0.1;
	public static final double STRANGLES_CARRIER_DAYS = 270;
	/** HPAI from each wild duck or goose within 16 blocks: 1 in 5,000 a real day (proposed). */
	public static final double HPAI_PER_WILD_BIRD = 1.0 / 5000;
	/** An unreported notifiable outbreak within 192 blocks: a tenth of its contagion, a real day (proposed). */
	public static final double OUTBREAK_SHARE = 0.1;

	/** A roll in [0, 1); {@code salt} keeps the rolls of one tick apart. */
	@FunctionalInterface
	public interface Roll {
		double next(int salt);
	}

	/** The share of the animal's group (itself left out) that is spreading each disease. */
	public record Group(Map<Disease, Double> spreading) {
		public static final Group NONE = new Group(Map.of());

		public double share(Disease d) {
			return spreading.getOrDefault(d, 0.0);
		}
	}

	/** What the world allows: the endemic diseases, the notifiable ones, deaths. */
	public record Rules(boolean disease, boolean notifiable, boolean deaths) {
		public static final Rules ALL = new Rules(true, true, false);
	}

	/** What happened this step, for messages and the readout. */
	public record Outcome(List<String> events, boolean died) {
	}

	private HealthModel() {
	}

	public static TimeScale.Scale scale(Species s) {
		return TimeScale.of(s);
	}

	public static Outcome step(HealthRecord r, Species sp, Risk.Inputs in, Group g, long now, Roll roll, Rules rules) {
		List<String> events = new ArrayList<>();
		TimeScale.Scale sc = scale(sp);
		if (r.lastTick() < 0 || now <= r.lastTick()) {
			r.setLastTick(Math.max(now, r.lastTick()));
			return new Outcome(events, false);
		}
		double days = sc.ticksToRealDays(now - r.lastTick());
		r.setLastTick(now);
		boolean died = progress(r, sp, now, days, roll, rules, events);
		if (rules.disease()) {
			infect(r, sp, in, g, now, days, roll, rules, events);
		}
		r.setScc(scc(r, in.healthScore()));
		for (String e : events) {
			r.event(now, e);
		}
		return new Outcome(events, died);
	}

	private static boolean progress(HealthRecord r, Species sp, long now, double days, Roll roll, Rules rules, List<String> events) {
		TimeScale.Scale sc = scale(sp);
		boolean died = false;
		int salt = 100;
		for (Case c : new ArrayList<>(r.cases())) {
			salt++;
			Disease d = c.disease;
			if (c.stage == Stage.CHRONIC && d == Disease.MASTITIS) {
				// a chronic cow flares now and then
				if (now > c.since && roll.next(salt) < Risk.chance(MASTITIS_FLARE, days)) {
					c.stage = Stage.SICK;
					c.since = now;
					c.until = now + sc.realDaysToTicks(d.sickDays);
					c.treated = false;
					events.add("mastitis flared up");
				}
				continue;
			}
			if (now < c.until) {
				continue;
			}
			switch (c.stage) {
				case INCUBATING -> {
					if (d == Disease.MASTITIS) {
						if (roll.next(salt) < MASTITIS_CLINICAL) {
							toSick(c, now, sc);
						} else {
							c.stage = Stage.SUBCLINICAL;
							c.until = now + sc.realDaysToTicks(MASTITIS_SUBCLINICAL_DAYS - d.incubationDays);
						}
					} else if (d == Disease.MAREKS) {
						c.stage = Stage.CHRONIC;
						c.until = Long.MAX_VALUE;
					} else {
						toSick(c, now, sc);
					}
					if (c.stage == Stage.SICK || c.stage == Stage.CHRONIC) {
						events.add("fell ill: " + d.label);
					}
				}
				case SUBCLINICAL -> {
					if (roll.next(salt) < 0.5) {
						recover(r, c, now, sc);
					} else {
						c.stage = Stage.CHRONIC;
						c.since = now;
						c.until = Long.MAX_VALUE;
					}
				}
				case SICK -> {
					if (!c.treated && rules.deaths() && d.fatality > 0 && roll.next(salt + 50) < d.fatality) {
						died = true;
						events.add("died of " + d.label);
						continue;
					}
					if (c.treated) {
						recover(r, c, now, sc);
						events.add("recovered: " + d.label);
					} else if (d == Disease.STRANGLES && roll.next(salt) < STRANGLES_CARRIER) {
						c.stage = Stage.CARRIER;
						c.since = now;
						c.until = now + sc.realDaysToTicks(STRANGLES_CARRIER_DAYS);
						events.add("recovered, but carries strangles");
					} else if (d.chronicShare > 0 && roll.next(salt) < d.chronicShare) {
						c.stage = Stage.CHRONIC;
						c.since = now;
						c.until = Long.MAX_VALUE;
						events.add("chronic " + d.label);
					} else {
						recover(r, c, now, sc);
						events.add("recovered: " + d.label);
					}
				}
				case CARRIER, IMMUNE -> r.remove(d);
				case CHRONIC -> {
				}
			}
		}
		return died;
	}

	private static void toSick(Case c, long now, TimeScale.Scale sc) {
		c.stage = Stage.SICK;
		c.since = now;
		c.until = now + sc.realDaysToTicks(c.disease.sickDays);
	}

	private static void recover(HealthRecord r, Case c, long now, TimeScale.Scale sc) {
		if (c.disease.immuneDays > 0) {
			c.stage = Stage.IMMUNE;
			c.since = now;
			c.until = now + sc.realDaysToTicks(c.disease.immuneDays);
			c.treated = false;
		} else {
			r.remove(c.disease);
		}
	}

	private static void infect(HealthRecord r, Species sp, Risk.Inputs in, Group g, long now, double days, Roll roll, Rules rules, List<String> events) {
		TimeScale.Scale sc = scale(sp);
		int salt = 0;
		for (Disease d : Disease.values()) {
			salt++;
			if (!d.affects(sp) || r.caseOf(d) != null || d.notifiable && !rules.notifiable()) {
				continue;
			}
			double lambda = hazard(d, r, sp, in, g, now, sc);
			if (d == Disease.ENTEROTOXAEMIA) {
				double p = in.grainJump() ? ENTEROTOXAEMIA_CHANCE * vaccineFactor(r, d, now, sc) : 0;
				if (p > 0 && roll.next(salt) < p) {
					r.infect(d, Stage.INCUBATING, now, now + sc.realDaysToTicks(d.incubationDays));
				}
				continue;
			}
			if (lambda > 0 && roll.next(salt) < Risk.chance(lambda, days)) {
				r.infect(d, Stage.INCUBATING, now, now + sc.realDaysToTicks(d.incubationDays));
			}
		}
	}

	/** The hazard per real day for one disease on this animal now, with every risk factor. Public for the tests and the readout. */
	public static double hazard(Disease d, HealthRecord r, Species sp, Risk.Inputs in, Group g, long now, TimeScale.Scale sc) {
		double base;
		boolean dirtBorne = false;
		switch (d) {
			case MASTITIS -> {
				if (!in.inMilk()) {
					return 0;
				}
				base = d.hazard * (in.daysInMilk() < Risk.FRESH_DAYS ? Risk.FRESH_FACTOR : 1);
				if (r.teatDipped() >= 0 && now - r.teatDipped() < sc.realDaysToTicks(1.5)) {
					base *= Risk.TEAT_DIP_FACTOR;
				}
				dirtBorne = true;
			}
			case BRD -> base = r.stressed(now) ? d.hazard : in.ageDays() < 365 ? d.hazard * BRD_YOUNG_SHARE : 0;
			case SCOURS -> {
				if (in.ageDays() >= SCOURS_AGE_DAYS) {
					return 0;
				}
				base = d.hazard * (in.cold() ? Risk.COLD_FACTOR : 1) * (r.damVaccinated() ? Risk.DAM_VACCINE_FACTOR : 1);
				dirtBorne = true;
			}
			case FOOT_ROT -> {
				if (sp == Species.CATTLE) {
					base = d.hazard;
				} else {
					base = (in.wet() ? d.hazard : 0) + (in.wet() && in.pastureInfective() ? PASTURE_FOOT_ROT : 0);
				}
				base *= Risk.genetic(in.hardiness()) * (r.footbathed(now) ? Risk.FOOTBATH_FACTOR : 1);
				dirtBorne = true;
			}
			case COCCIDIOSIS -> {
				if (in.ageDays() >= COCCIDIOSIS_AGE_DAYS) {
					return 0;
				}
				base = d.hazard * in.dirt() * (in.cold() ? Risk.COLD_FACTOR : 1);
				dirtBorne = true;
			}
			case MAREKS -> base = in.ageDays() >= MAREKS_FROM_DAYS && in.ageDays() <= MAREKS_TO_DAYS ? d.hazard : 0;
			case HPAI -> base = in.wildFowlNear() * HPAI_PER_WILD_BIRD;
			default -> base = d.hazard;
		}
		if (d.notifiable && in.outbreakNear()) {
			base += d.beta * OUTBREAK_SHARE;
		}
		double contagion = d.beta * g.share(d);
		if (d == Disease.FOOT_ROT && sp == Species.CATTLE) {
			contagion = 0;     // not contagious in cattle (a simplification)
		}
		double lambda = (base * (d == Disease.COCCIDIOSIS ? 1 : Risk.dirt(in.dirt(), dirtBorne)) + contagion);
		return lambda * Risk.genetic(in.healthScore()) * Risk.feed(in.fed(), in.rationScore()) * vaccineFactor(r, d, now, sc);
	}

	private static double vaccineFactor(HealthRecord r, Disease d, long now, TimeScale.Scale sc) {
		double f = 1;
		for (Vaccine v : Vaccine.values()) {
			Double e = v.efficacy.get(d);
			if (e != null && r.protectedBy(v, now, sc::realDaysToTicks)) {
				f *= e;
			}
		}
		return f;
	}

	/** The somatic cell count the udder settles at for its mastitis state; a healthier genome sits lower (proposed). */
	public static int scc(HealthRecord r, int healthScore) {
		Case c = r.caseOf(Disease.MASTITIS);
		int base = (int) Math.round(Scc.HEALTHY * Risk.genetic(healthScore));
		if (c == null || c.stage == Stage.IMMUNE) {
			return base;
		}
		return switch (c.stage) {
			case INCUBATING, SUBCLINICAL -> Scc.SUBCLINICAL_LEVEL;
			case SICK -> c.treated ? Scc.SUBCLINICAL_LEVEL : Scc.CLINICAL_LEVEL;
			case CHRONIC -> Scc.CHRONIC_LEVEL;
			default -> base;
		};
	}

	/**
	 * Contagious mastitis at milking: a cow milked on a unit after a cow with clinical or chronic mastitis is exposed, 5% (proposed), x0.3 if
	 * the parlor dips teats. Returns true if she was infected.
	 */
	public static boolean parlorExposure(HealthRecord r, long now, boolean dipped, double roll, Rules rules, Species sp) {
		if (!rules.disease() || r.caseOf(Disease.MASTITIS) != null) {
			return false;
		}
		double p = 0.05 * (dipped ? Risk.TEAT_DIP_FACTOR : 1);
		if (roll < p) {
			r.infect(Disease.MASTITIS, Stage.INCUBATING, now, now + scale(sp).realDaysToTicks(Disease.MASTITIS.incubationDays));
			r.event(now, "picked up mastitis at milking");
			return true;
		}
		return false;
	}

	/** A rectal temperature in C: the middle of the normal range, a fever of 1-2 C when sick (proposed). */
	public static double temperature(Species sp, HealthRecord r, double roll) {
		double[] range = normalRange(sp);
		double t = (range[0] + range[1]) / 2 + (roll - 0.5) * (range[1] - range[0]) * 0.5;
		for (Case c : r.cases()) {
			if (c.stage == Stage.SICK && c.disease != Disease.FOOT_ROT && c.disease != Disease.MAREKS) {
				t = range[1] + 1.0 + roll;
				break;
			}
		}
		return t;
	}

	/** Normal rectal temperature, C (MSD Veterinary Manual table). Birds other than chickens use the chicken range (proposed). */
	public static double[] normalRange(Species sp) {
		return switch (sp) {
			case CATTLE -> new double[] {38.0, 39.3};
			case SHEEP -> new double[] {38.3, 39.9};
			case GOAT -> new double[] {38.5, 39.7};
			case PIG -> new double[] {38.7, 39.8};
			case HORSE -> new double[] {37.2, 38.2};
			default -> new double[] {40.6, 43.0};
		};
	}
}

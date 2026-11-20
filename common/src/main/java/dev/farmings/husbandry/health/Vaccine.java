package dev.farmings.husbandry.health;

import dev.farmings.genetics.Species;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The five vaccines (docs/animal-health.md, "Vaccines"). Primary doses a set number of real days apart, then a yearly booster; a dose given
 * too young does nothing (CDT before 6 weeks: MSU). Efficacy is a risk multiplier per disease (all proposed).
 */
public enum Vaccine {
	/** Clostridial C and D toxoid with tetanus: 2 doses 4 weeks apart, yearly (MSU Extension). */
	CDT("CDT", EnumSet.of(Species.SHEEP, Species.GOAT), 2, 28, 42, Map.of(Disease.ENTEROTOXAEMIA, 0.1)),
	/** Modified-live IBR/BVD/PI3/BRSV, one dose then yearly (proposed). */
	RESPIRATORY("respiratory", EnumSet.of(Species.CATTLE), 1, 0, 0, Map.of(Disease.BRD, 0.5)),
	/** Erysipelas bacterin: 2 doses 3-5 weeks apart (label), yearly. */
	ERYSIPELAS("erysipelas", EnumSet.of(Species.PIG), 2, 28, 0, Map.of(Disease.ERYSIPELAS, 0.2)),
	/** HVT Marek's vaccine, at hatch only (UTIA D185: older birds are not protected). */
	MAREKS("Marek's", EnumSet.of(Species.CHICKEN), 1, 0, 0, Map.of(Disease.MAREKS, 0.1)),
	/** Intranasal live strangles vaccine: 2 doses 3 weeks apart, yearly (proposed). */
	STRANGLES("strangles", EnumSet.of(Species.HORSE), 2, 21, 0, Map.of(Disease.STRANGLES, 0.5));

	/** A booster is due a year after the last dose. */
	public static final double BOOSTER_DAYS = 365;

	public final String label;
	public final Set<Species> species;
	public final int primaryDoses;
	/** Real days between the primary doses (at least this many; at most twice it, or the course restarts). */
	public final double primaryGapDays;
	/** Youngest age in real days a dose counts. */
	public final double minAgeDays;
	public final Map<Disease, Double> efficacy;

	Vaccine(String label, Set<Species> species, int primaryDoses, double primaryGapDays, double minAgeDays, Map<Disease, Double> efficacy) {
		this.label = label;
		this.species = species;
		this.primaryDoses = primaryDoses;
		this.primaryGapDays = primaryGapDays;
		this.minAgeDays = minAgeDays;
		this.efficacy = efficacy;
	}

	public static Vaccine byName(String n) {
		for (Vaccine v : values()) {
			if (v.name().equalsIgnoreCase(n)) {
				return v;
			}
		}
		return null;
	}
}

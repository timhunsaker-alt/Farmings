package dev.farmings.husbandry.health;

import dev.farmings.genetics.Species;
import java.util.EnumSet;
import java.util.Set;

/**
 * The twelve diseases of docs/animal-health.md, as data. Every duration is in real days on the animal's own clock; every rate is a hazard
 * per real day. Values with a source are cited in the doc; the rest are marked *(proposed)* there.
 */
public enum Disease {
	/** NAHMS Dairy 2014: 24.8% of cows a year. */
	MASTITIS("mastitis", set(Species.CATTLE, Species.SHEEP, Species.GOAT), Risk.hazard(0.248, 365), 0, 3, 10, 3, 0.5, 0, 0.03, false),
	/** NAHMS Feedlot 2011: 16.2% of arrivals, in a 45-day window. */
	BRD("bovine respiratory disease", set(Species.CATTLE), Risk.hazard(0.162, 45), 0.10, 3, 7, 3, 0.25, 180, 0.05, false),
	/** 20% of young in their first 30 days (proposed; NOT FOUND). */
	SCOURS("neonatal scours", set(Species.CATTLE, Species.SHEEP, Species.GOAT, Species.PIG), Risk.hazard(0.20, 30), 0, 1, 7, 3, 0, 180, 0.05, false),
	/** NAHMS Dairy 2014 Part III: lameness in 16.8% of cows a year. */
	FOOT_ROT("foot rot", set(Species.CATTLE, Species.SHEEP, Species.GOAT), Risk.hazard(0.168, 365), 0.05, 5, 21, 3, 1.0, 0, 0, false),
	/** Triggered by a grain jump, 1 in 10 unvaccinated (proposed). */
	ENTEROTOXAEMIA("overeating disease", set(Species.SHEEP, Species.GOAT), 0, 0, 0.5, 2, 2, 0, 0, 0.5, false),
	/** 5% of unvaccinated pigs a year (proposed); incubation 24-48 h. */
	ERYSIPELAS("swine erysipelas", set(Species.PIG), Risk.hazard(0.05, 365), 0.05, 1.5, 10, 3, 0.25, 180, 0.05, false),
	/** 2% a real day per dirty share, birds under 8 weeks (proposed). */
	COCCIDIOSIS("coccidiosis", fowl(), 0.02, 0.10, 2, 7, 5, 0, 180, 0.05, false),
	/** 10% of unvaccinated chickens between 6 and 30 weeks (proposed). Chronic once sick. */
	MAREKS("Marek's disease", set(Species.CHICKEN), Risk.hazard(0.10, 168), 0, 14, 0, 0, 1.0, 0, 0, false),
	/** Incubation 3-14 days, 3 weeks sick, 1 in 10 a carrier for 9.2 months (EquiManagement). */
	STRANGLES("strangles", set(Species.HORSE), 0, 0.15, 7, 21, 21, 0, 180, 0.01, false),
	FOOT_AND_MOUTH("foot-and-mouth disease", set(Species.CATTLE, Species.SHEEP, Species.GOAT, Species.PIG), 0, 0.30, 5, 14, 14, 0, 180, 0.02, true),
	HPAI("highly pathogenic avian influenza", fowl(), 0, 0.30, 2, 14, 14, 0, 180, 0.5, true),
	AFRICAN_SWINE_FEVER("African swine fever", set(Species.PIG), 0, 0.30, 5, 14, 14, 0, 180, 0.5, true);

	public final String label;
	public final Set<Species> species;
	/** Background hazard per real day (before the risk factors). */
	public final double hazard;
	/** Contagion per real day, times the share of the group sick (0 = not contagious within a group). */
	public final double beta;
	/** Real days from infection to signs. */
	public final double incubationDays;
	/** Real days sick when untreated. */
	public final double sickDays;
	/** Real days to recover once treated. */
	public final double treatedDays;
	/** The share of untreated cases that become chronic at the end of the course. */
	public final double chronicShare;
	/** Real days immune after recovering (0 = none). */
	public final double immuneDays;
	/** The share that die at the end of a sick course when the deaths rule is on (proposed; mastitis: NAHMS "less than 5 percent"). */
	public final double fatality;
	public final boolean notifiable;

	Disease(String label, Set<Species> species, double hazard, double beta, double incubationDays, double sickDays, double treatedDays,
			double chronicShare, double immuneDays, double fatality, boolean notifiable) {
		this.label = label;
		this.species = species;
		this.hazard = hazard;
		this.beta = beta;
		this.incubationDays = incubationDays;
		this.sickDays = sickDays;
		this.treatedDays = treatedDays;
		this.chronicShare = chronicShare;
		this.immuneDays = immuneDays;
		this.fatality = fatality;
		this.notifiable = notifiable;
	}

	private static Set<Species> set(Species first, Species... rest) {
		return java.util.Collections.unmodifiableSet(EnumSet.of(first, rest));
	}

	private static Set<Species> fowl() {
		EnumSet<Species> s = EnumSet.noneOf(Species.class);
		for (Species sp : Species.values()) {
			if (sp.fowl()) {
				s.add(sp);
			}
		}
		return java.util.Collections.unmodifiableSet(s);
	}

	public boolean affects(Species s) {
		return species.contains(s);
	}

	/** Signs a player can see without a kit, for the readout. */
	public String signs() {
		return switch (this) {
			case MASTITIS -> "clots in milk";
			case BRD, STRANGLES -> "coughing, off feed";
			case SCOURS -> "loose manure, off feed";
			case FOOT_ROT -> "lame";
			case ENTEROTOXAEMIA -> "down, not eating";
			case ERYSIPELAS -> "red diamond marks, off feed";
			case COCCIDIOSIS -> "hunched, ruffled";
			case MAREKS -> "sits with one leg forward";
			case FOOT_AND_MOUTH -> "drooling, lame, off feed";
			case HPAI -> "hunched, stopped laying";
			case AFRICAN_SWINE_FEVER -> "red skin, off feed";
		};
	}

	/** Whether a sick animal coughs (the sound). */
	public boolean coughs() {
		return this == BRD || this == STRANGLES || this == FOOT_AND_MOUTH || this == HPAI;
	}

	/** Whether a sick animal limps (slower walk). */
	public boolean lames() {
		return this == FOOT_ROT || this == FOOT_AND_MOUTH;
	}

	public static Disease byName(String name) {
		for (Disease d : values()) {
			if (d.name().equalsIgnoreCase(name)) {
				return d;
			}
		}
		return null;
	}
}

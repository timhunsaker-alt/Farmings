package dev.farmings.husbandry.poultry;

import dev.farmings.genetics.Species;
import java.util.Locale;

/**
 * The thirteen poultry breeds (docs/animals/poultry.md, "Breeds", "Laying", "Incubation", "Brooding", "Meat"). Every figure is the
 * "game uses" value there, with its source in the doc; ranges are taken at their middle. Weights are in kg (converted from lb).
 */
public enum FowlBreed {
	// UW-Madison A2880 weights; Livestock Conservancy eggs; Hy-Line W-36 egg weight and 143 days to 50% production. USU 4-H incubation,
	// NDSU heat to 6 weeks, UW-Madison heritage dressing 68%.
	LEGHORN("LEG", Species.CHICKEN, 2.72, 2.04, 235, 58.5, 143, 21, 18, 42, 0.68),
	RHODE_ISLAND_RED("RIR", Species.CHICKEN, 3.86, 2.95, 250, 60, 140, 21, 18, 42, 0.68),
	PLYMOUTH_ROCK("PLR", Species.CHICKEN, 4.31, 3.40, 200, 60, 140, 21, 18, 42, 0.68),
	// Silkie: hen "1.4 kg for Silkie" and egg 41 g ("Egg weight [g] … Silkie … 41.15 ±0.97"; Tyc et al. 2023, Pol J Vet Sci 26(3)).
	// Cock 1.55 kg, the middle of the European standard's 1.4-1.7 (Wikipedia citing BDRG).
	SILKIE("SLK", Species.CHICKEN, 1.55, 1.4, 100, 41, 140, 21, 18, 42, 0.68),
	// UAF / Livestock Conservancy weights; first egg at 31.5 weeks (The Poultry Site); USU incubation 28 days, stop turning day 25;
	// UMaine heat 6-8 weeks (7); UW-Madison turkey dressing 80.5%.
	BROAD_BREASTED_WHITE("BBW", Species.TURKEY, 14.5, 8.16, 110, 75, 220.5, 28, 25, 49, 0.805),
	BRONZE("BRZ", Species.TURKEY, 16.56, 9.07, 75, 75, 220.5, 28, 25, 49, 0.805),
	BOURBON_RED("BRD", Species.TURKEY, 14.5, 8.16, 45, 75, 220.5, 28, 25, 49, 0.805),
	// Extension / NSW DPI; MSU incubation 17 days, stop day 15; heat 3-4 weeks (3.5); UMaine quail dressing 58-62% (60%).
	COTURNIX("COT", Species.QUAIL, 0.12, 0.14, 250, 10, 49, 17, 15, 24.5, 0.60),
	// APHIS eggs and first egg at 22 weeks; Wilson Bulletin egg weight; MSU 23-24 days, stop day 20. Heat: the Coturnix figure (proposed).
	BOBWHITE("BOB", Species.QUAIL, 0.17, 0.17, 125, 9.5, 154, 23.5, 20, 24.5, 0.60),
	// Kentucky ASC-198 weights and eggs; first egg about 6 months (the Khaki Campbell figure, used for all ducks: proposed for the
	// others); duck eggs 32-34 oz a dozen (78 g); MSU 28 days, stop day 25 (Muscovy 35, day 31); heat 4 weeks; UMaine and UW-Madison
	// dressing (Pekin 67.5%, Muscovy 72%, others 65%). Mallard egg 54 g, egg mass "54g" (BTO BirdFacts).
	PEKIN("PEK", Species.DUCK, 4.31, 3.86, 140, 78, 182, 28, 25, 28, 0.675),
	KHAKI_CAMPBELL("KHC", Species.DUCK, 1.93, 1.70, 250, 78, 182, 28, 25, 28, 0.65),
	MALLARD("MAL", Species.DUCK, 1.08, 0.94, 35, 54, 182, 28, 25, 28, 0.65),
	MUSCOVY("MUS", Species.DUCK, 4.99, 2.95, 90, 78, 182, 35, 31, 28, 0.72),
	// Geese (poultry.md, "Geese and guinea fowl"), appended so saved ordinals hold. MSU E-1050 weights (1977: "Toulouse 26 20 … Embden 26 20 …
	// Chinese 12 10" lb); FAO eggs and egg weights (Toulouse: TLC's 20-35, egg weight NOT FOUND, the Embden's); first egg at a year (MSU);
	// FAO 30-day incubation, the hatcher from day 27; goslings kept warm 4 weeks (the duck figure, proposed); FAO dressing 70%. Laying seasons FAO: 15 Feb - 15 Jun, Chinese 1 Feb - 30 Jun.
	EMBDEN("EMB", Species.GOOSE, 11.8, 9.1, 40, 170, 365, 30, 27, 28, 0.70, 45, 165),
	TOULOUSE("TOU", Species.GOOSE, 11.8, 9.1, 27.5, 170, 365, 30, 27, 28, 0.70, 45, 165),
	CHINESE("CHI", Species.GOOSE, 5.4, 4.5, 55, 120, 365, 30, 27, 28, 0.70, 31, 180),
	// Helmeted guinea fowl (poultry.md): UKY ASC-209 weights (1.5 kg both sexes), "more than 100 eggs", 40 g eggs, incubation 26-28 days
	// (27), stop turning day 25 (TO VERIFY: NC State 4-H chart); first egg at 28 weeks ("Sexual maturity guinea hens appears to occur
	// around 27-28 weeks of age with the onset of egg production", eXtension); keets kept warm 6 weeks (the chicken figure, proposed);
	// UMaine dressing 70%; laying season 1 Apr - 31 Oct (extension.org: March or April to October).
	HELMETED("GUI", Species.GUINEA, 1.5, 1.5, 100, 40, 196, 27, 25, 42, 0.70, 90, 303);

	public final String code;
	public final Species species;
	public final double maleKg;
	public final double femaleKg;
	public final double eggsPerYear;
	public final double eggGrams;
	/** Age at the first egg, in real days. */
	public final double firstEggDays;
	public final double incubationDays;
	/** The day of incubation after which eggs are no longer turned ("lockdown"). */
	public final double stopTurningDay;
	/** Age until which a young bird needs heat, in real days. */
	public final double heatUntilDays;
	public final double dressing;
	/** The laying season as day-of-year {first, last}, wrapping past New Year; -1 for a bird that lays all year. */
	public final int seasonStart;
	public final int seasonEnd;

	FowlBreed(String code, Species species, double maleKg, double femaleKg, double eggsPerYear, double eggGrams, double firstEggDays,
			double incubationDays, double stopTurningDay, double heatUntilDays, double dressing) {
		this(code, species, maleKg, femaleKg, eggsPerYear, eggGrams, firstEggDays, incubationDays, stopTurningDay, heatUntilDays, dressing, -1, -1);
	}

	FowlBreed(String code, Species species, double maleKg, double femaleKg, double eggsPerYear, double eggGrams, double firstEggDays,
			double incubationDays, double stopTurningDay, double heatUntilDays, double dressing, int seasonStart, int seasonEnd) {
		this.seasonStart = seasonStart;
		this.seasonEnd = seasonEnd;
		this.code = code;
		this.species = species;
		this.maleKg = maleKg;
		this.femaleKg = femaleKg;
		this.eggsPerYear = eggsPerYear;
		this.eggGrams = eggGrams;
		this.firstEggDays = firstEggDays;
		this.incubationDays = incubationDays;
		this.stopTurningDay = stopTurningDay;
		this.heatUntilDays = heatUntilDays;
		this.dressing = dressing;
	}

	public static FowlBreed byCode(String code) {
		for (FowlBreed b : values()) {
			if (b.code.equals(code)) {
				return b;
			}
		}
		return null;
	}

	public boolean seasonal() {
		return seasonStart >= 0;
	}

	/** Lays on this day of the year (always, for a bird with no season). */
	public boolean inLay(int dayOfYear) {
		if (seasonStart < 0) {
			return true;
		}
		return seasonStart <= seasonEnd ? dayOfYear >= seasonStart && dayOfYear <= seasonEnd : dayOfYear >= seasonStart || dayOfYear <= seasonEnd;
	}

	/** Days in the laying season (365 for a bird with no season). */
	public int seasonDays() {
		if (seasonStart < 0) {
			return 365;
		}
		return seasonStart <= seasonEnd ? seasonEnd - seasonStart + 1 : 365 - seasonStart + seasonEnd + 1;
	}

	public String displayName() {
		String n = name().toLowerCase(Locale.ROOT).replace('_', ' ');
		StringBuilder b = new StringBuilder();
		for (String w : n.split(" ")) {
			b.append(b.isEmpty() ? "" : " ").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		}
		return b.toString();
	}
}

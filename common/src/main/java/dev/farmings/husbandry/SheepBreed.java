package dev.farmings.husbandry;

import java.util.Locale;

/**
 * The four starting sheep breeds (docs/animals/sheep.md, decided with Tim 2026-11-10). Every figure is the "game uses" value of the
 * breed table there, with its source; ranges are taken at their middle.
 */
public enum SheepBreed {
	// Ewe 50 kg (Grootfontein, S. Afr. J. Anim. Sci. 37); 1.25 lambs, 4-6 kg greasy, under 24 micron, 6-10 cm (Canadian Co-operative
	// Wool Growers); yield 65-75%, "Merino (Medium) 21-22 … 65-75" (NMSU B409, Table 4). Ewe lambs bred at 70% for fine-wool breeds (Montana State).
	MERINO("MER", Use.FINE_WOOL, 50, 1.25, 5.0, 21, 8, 0.70, 0.70),
	// Ewe 104 kg, "104.4 kg (230 lb) — Suffolk" (OMAFRA, choosing breeds); 1.8 lambs, 2.2-2.7 kg, 28-33 micron, 8-10 cm (Canadian Co-operative Wool Growers);
	// yield 50-62% (Kentucky ASC-127).
	SUFFOLK("SUF", Use.MEAT, 104, 1.8, 2.45, 30, 9, 0.56, 0.65),
	// Ewe 77 kg, "77.1 kg (170 lb) — … Dorper" (OMAFRA); the wool growers give "70 - 100 kg- Ewes" and "150 %- Lamb/Ewe Percentage",
	// with 0.5-1 kg of shed wool (Canadian Co-operative Wool Growers). A hair sheep: no micron or yield.
	DORPER("DOR", Use.HAIR, 77, 1.5, 0.75, 0, 0, 0, 0.65),
	// Ewe 73-82 kg, 4-5.5 kg, 29-33 micron, 10-15 cm, 55-65% (ASI via apparelsearch); 2.2 lambs (UW-Extension dairy sheep guide).
	EAST_FRIESIAN("EFR", Use.DAIRY, 77, 2.2, 4.75, 31, 12.5, 0.60, 0.65);

	public enum Use {
		FINE_WOOL,
		MEAT,
		HAIR,
		DAIRY
	}

	public final String code;
	public final Use use;
	public final double matureWeightKg;
	/** Mean lambs per ewe lambing. */
	public final double litterMean;
	/** Greasy fleece weight after a year of growth, kg. */
	public final double fleeceKg;
	/** Mean fibre diameter, microns; 0 for a hair breed. */
	public final double micron;
	/** Staple length grown in a year, cm; 0 for a hair breed. */
	public final double stapleCmPerYear;
	/** Clean (scoured) weight as a share of greasy weight; 0 for a hair breed. */
	public final double cleanYield;
	/** Share of mature weight a ewe lamb needs before she is first bred. */
	public final double firstBreedingFraction;

	SheepBreed(String code, Use use, double matureWeightKg, double litterMean, double fleeceKg, double micron, double stapleCmPerYear,
			double cleanYield, double firstBreedingFraction) {
		this.code = code;
		this.use = use;
		this.matureWeightKg = matureWeightKg;
		this.litterMean = litterMean;
		this.fleeceKg = fleeceKg;
		this.micron = micron;
		this.stapleCmPerYear = stapleCmPerYear;
		this.cleanYield = cleanYield;
		this.firstBreedingFraction = firstBreedingFraction;
	}

	public boolean hair() {
		return use == Use.HAIR;
	}

	public String assetName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String label() {
		String n = name().replace('_', ' ').toLowerCase(Locale.ROOT);
		StringBuilder b = new StringBuilder();
		for (String w : n.split(" ")) {
			b.append(b.length() == 0 ? "" : " ").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		}
		return b.toString();
	}

	public static SheepBreed byCode(String code) {
		for (SheepBreed b : values()) {
			if (b.code.equals(code)) {
				return b;
			}
		}
		throw new IllegalArgumentException("unknown sheep breed " + code);
	}

	public static boolean isSheep(String code) {
		for (SheepBreed b : values()) {
			if (b.code.equals(code)) {
				return true;
			}
		}
		return false;
	}
}

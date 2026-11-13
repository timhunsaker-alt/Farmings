package dev.farmings.husbandry;

import java.util.Locale;

/**
 * The three goat breeds (docs/animals/goats.md, Tim 2026-11-13). Every figure is the "game uses" value of the breed table there, with its
 * source in that doc.
 */
public enum GoatBreed {
	// Doe 61, buck 77 kg: the breed minimums, 135 and 170 lb (MSU P4120). Birth 2.7 kg (KnE Life Sciences, TO VERIFY). ADGA 2010:
	// 2,545 lb (1,154 kg), 3.2% fat, 2.8% protein (Purdue). Strongly seasonal, Sep-Feb (NC State).
	SAANEN("SAA", Use.DAIRY, 61, 77, 2.7, 1.56, 1154, 3.2, 2.8, 0.475, 0.475, 243, 58),
	// Doe 61, buck 77 kg (MSU P4120). Birth 3.25 kg (AGRIS, TO VERIFY). ADGA 2010: 1,835 lb (832 kg), 4.6% fat, 3.7% protein (Purdue).
	// The least seasonal dairy goat (NC State): 1 Aug - 31 Mar (proposed dates). Litter as Saanen (Nubian NOT FOUND).
	NUBIAN("NUB", Use.DAIRY, 61, 77, 3.25, 1.56, 832, 4.6, 3.7, 0.475, 0.475, 212, 89),
	// Doe 80, buck 130 kg: the middles of FAO's 70-90 and 120-140. Birth 3.5 kg (FAO; Đuričić 2012). Litter 1.64 (FAO: 43/50/7).
	// Dressing 48% at 8-10 months, 58% mature (FAO, 56-60). Breeds all year (proposed: a 265-day kidding interval, Đuričić 2012).
	BOER("BOE", Use.MEAT, 80, 130, 3.5, 1.64, 0, 0, 0, 0.48, 0.58, -1, -1);

	public enum Use {
		DAIRY,
		MEAT
	}

	public final String code;
	public final Use use;
	public final double doeKg;
	public final double buckKg;
	public final double birthKg;
	/** Mean kids per kidding after the first. */
	public final double litterMean;
	/** Milk a lactation, kg; 0 for a breed the parlor doesn't milk. */
	public final double lactationKg;
	public final double fatPct;
	public final double proteinPct;
	/** Dressing share of live weight for a kid (under 9 months) and an older goat. */
	public final double dressingKid;
	public final double dressingMature;
	/** Breeding season as day-of-year {first, last}, wrapping past New Year; -1 for all year. */
	public final int seasonStart;
	public final int seasonEnd;

	GoatBreed(String code, Use use, double doeKg, double buckKg, double birthKg, double litterMean, double lactationKg, double fatPct,
			double proteinPct, double dressingKid, double dressingMature, int seasonStart, int seasonEnd) {
		this.code = code;
		this.use = use;
		this.doeKg = doeKg;
		this.buckKg = buckKg;
		this.birthKg = birthKg;
		this.litterMean = litterMean;
		this.lactationKg = lactationKg;
		this.fatPct = fatPct;
		this.proteinPct = proteinPct;
		this.dressingKid = dressingKid;
		this.dressingMature = dressingMature;
		this.seasonStart = seasonStart;
		this.seasonEnd = seasonEnd;
	}

	public boolean dairy() {
		return use == Use.DAIRY;
	}

	public String assetName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String label() {
		String n = name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	public static GoatBreed byCode(String code) {
		for (GoatBreed b : values()) {
			if (b.code.equals(code)) {
				return b;
			}
		}
		throw new IllegalArgumentException("unknown goat breed " + code);
	}

	public static boolean isGoat(String code) {
		for (GoatBreed b : values()) {
			if (b.code.equals(code)) {
				return true;
			}
		}
		return false;
	}
}

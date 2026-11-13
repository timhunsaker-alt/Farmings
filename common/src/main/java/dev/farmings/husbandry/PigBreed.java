package dev.farmings.husbandry;

import java.util.Locale;

/** The three pig breeds (docs/animals/pigs.md, Tim 2026-11-13); every figure is the "game uses" value there, with its source. */
public enum PigBreed {
	// Sow 260-300, boar 350-380 kg (Rare Breeds Trust of Australia, TO VERIFY). Total born 11.6 first, 13.7 third parity (Klimas 2020);
	// stillborn 8.80% (Polish Large White, Nowak 2020).
	LARGE_WHITE("LWH", 280, 365, 11.6, 13.7, 0.088),
	// Sow 772 lb, boar 882 lb (Wikipedia citing Briggs, TO VERIFY). Total born 10.39, stillborn 8.92% (Nowak 2020); first litter x0.85.
	DUROC("DUR", 350, 400, 10.39 * 0.85, 10.39, 0.0892),
	// Sow 220, boar 280 kg (Rare Breeds Trust of Australia, TO VERIFY). Total born 9.16, stillborn 15.09% (Nowak 2020); first litter x0.85.
	BERKSHIRE("BRK", 220, 280, 9.16 * 0.85, 9.16, 0.1509);

	public final String code;
	public final double sowKg;
	public final double boarKg;
	public final double litterFirst;
	public final double litterLater;
	public final double stillbornRate;

	PigBreed(String code, double sowKg, double boarKg, double litterFirst, double litterLater, double stillbornRate) {
		this.code = code;
		this.sowKg = sowKg;
		this.boarKg = boarKg;
		this.litterFirst = litterFirst;
		this.litterLater = litterLater;
		this.stillbornRate = stillbornRate;
	}

	public String label() {
		String n = name().replace('_', ' ').toLowerCase(Locale.ROOT);
		StringBuilder b = new StringBuilder();
		for (String w : n.split(" ")) {
			b.append(b.length() == 0 ? "" : " ").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		}
		return b.toString();
	}

	public static PigBreed byCode(String code) {
		for (PigBreed b : values()) {
			if (b.code.equals(code)) {
				return b;
			}
		}
		throw new IllegalArgumentException("unknown pig breed " + code);
	}

	public static boolean isPig(String code) {
		for (PigBreed b : values()) {
			if (b.code.equals(code)) {
				return true;
			}
		}
		return false;
	}
}

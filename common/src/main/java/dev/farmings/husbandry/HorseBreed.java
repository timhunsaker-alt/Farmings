package dev.farmings.husbandry;

/** The three horse breeds (docs/animals/horses.md, Tim 2026-11-13); every figure is the "game uses" value there, with its source. */
public enum HorseBreed {
	// "stands a little over 16 hands" and "weighs approximately 1,000 pounds" (America's Best Racing, The Jockey Club): 454 kg, 163 cm.
	// MSTN C at 0.56: 1 minus the elite Thoroughbreds' T frequency, 0.443 (Hill et al. 2010, Table 1; derived).
	THOROUGHBRED("TB", "Thoroughbred", 454, 163, 0.56),
	// "14.3–16.2 hands" (Horse Illustrated): 159 cm; weight "Approximately 1000 pounds" NOT FOUND in a primary source (TO VERIFY). "C/C, 0.83; C/T, 0.14; T/T, 0.03" (Hill et al. 2010):
	// C at 0.90.
	QUARTER_HORSE("QH", "Quarter Horse", 454, 159, 0.90),
	// "averages 14.1 to 15.1 hands" (Arabian Horse Association): 150 cm; "between 800 and 1,000 pounds" (Oklahoma State, TO VERIFY). Egyptian Arabians "T/T (0.90)" (Hill et al.
	// 2010); the rest C/T *(proposed)*: C at 0.05.
	ARABIAN("ARA", "Arabian", 408, 150, 0.05);

	public final String code;
	private final String label;
	/** Mature weight, both sexes *(proposed: the middle of the quoted range)*. */
	public final double matureKg;
	public final int heightCm;
	/** The founders' MSTN C allele frequency; founders draw two alleles by it. */
	public final double mstnC;

	HorseBreed(String code, String label, double matureKg, int heightCm, double mstnC) {
		this.code = code;
		this.label = label;
		this.matureKg = matureKg;
		this.heightCm = heightCm;
		this.mstnC = mstnC;
	}

	public String label() {
		return label;
	}

	public static HorseBreed byCode(String code) {
		for (HorseBreed b : values()) {
			if (b.code.equals(code)) {
				return b;
			}
		}
		throw new IllegalArgumentException("unknown horse breed " + code);
	}

	public static boolean isHorse(String code) {
		for (HorseBreed b : values()) {
			if (b.code.equals(code)) {
				return true;
			}
		}
		return false;
	}
}

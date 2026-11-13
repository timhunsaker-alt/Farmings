package dev.farmings.husbandry;

/** The three horse breeds (docs/animals/horses.md, Tim 2026-11-13); every figure is the "game uses" value there, with its source. */
public enum HorseBreed {
	// "15.2 to 17 hands", "1,000 to 1,300 pounds" (Mad Barn, TO VERIFY): 1,150 lb. MSTN C at 45% (proposed: not in Hill et al. 2010).
	THOROUGHBRED("TB", "Thoroughbred", 522, 163, 0.45),
	// "14.3 to 16 hands", "Approximately 1000 pounds" (Horse Illustrated, TO VERIFY). "C/C, 0.83; C/T, 0.14; T/T, 0.03" (Hill et al. 2010):
	// C at 0.90.
	QUARTER_HORSE("QH", "Quarter Horse", 454, 155, 0.90),
	// "between 14.2 and 15.2 hands", "between 800 and 1,000 pounds" (Oklahoma State, TO VERIFY). Egyptian Arabians "T/T (0.90)" (Hill et al.
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

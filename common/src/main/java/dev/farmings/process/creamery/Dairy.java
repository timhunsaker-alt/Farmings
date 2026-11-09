package dev.farmings.process.creamery;

/**
 * Dairy-processing arithmetic (docs/multiblocks/creamery.md), each figure with its source there. Percentages are mass percent; litres and
 * kilograms are treated alike for cream, skim and buttermilk (their densities are within a few percent of 1; milk itself is 1.03 kg/L).
 */
public final class Dairy {
	/** Cream 35-40% fat (Wisconsin Center for Dairy Research); 36 used. */
	public static final double CREAM_FAT = 36;
	/** Skim from a separator, 0.1% fat (Dairysafe Victoria). */
	public static final double SKIM_FAT = 0.1;
	/** Butter at least 80% fat (US standard), 80-82% in practice (CDR); 80 used. */
	public static final double BUTTER_FAT = 80;
	/** Churning loses 1.0-1.2% of the fat to the buttermilk (Iowa State); 1.1 used. */
	public static final double CHURN_FAT_LOSS = 0.011;
	/** Van Slyke: fat recovery 0.93, casein = 0.78 x protein, solids factor 1.09, cheddar moisture 38% (Guelph; USDA AMS). */
	public static final double VS_FAT_RECOVERY = 0.93;
	public static final double VS_CASEIN_OF_PROTEIN = 0.78;
	public static final double VS_SOLIDS = 1.09;
	public static final double VS_MOISTURE = 0.38;
	public static final double MILK_KG_PER_L = 1.03;

	private Dairy() {
	}

	public record Split(double creamL, double skimL) {
	}

	/** Separates milk by fat balance into 36% cream and 0.1% skim. */
	public static Split separate(double milkL, double milkFat) {
		if (milkL <= 0 || milkFat <= SKIM_FAT) {
			return new Split(0, Math.max(0, milkL));
		}
		double cream = milkL * (milkFat - SKIM_FAT) / (CREAM_FAT - SKIM_FAT);
		return new Split(cream, milkL - cream);
	}

	public record Churned(double butterKg, double buttermilkL) {
	}

	/** Churns cream at {@code creamFat}% into 80% butter; 1.1% of the fat stays in the buttermilk. */
	public static Churned churn(double creamKg, double creamFat) {
		double fat = creamKg * creamFat / 100;
		double butter = fat * (1 - CHURN_FAT_LOSS) / (BUTTER_FAT / 100);
		return new Churned(butter, Math.max(0, creamKg - butter));
	}

	/** Van Slyke cheddar yield, kg of cheese per 100 kg of milk. */
	public static double vanSlykePer100(double fat, double protein) {
		return Math.max(0, (VS_FAT_RECOVERY * fat + VS_CASEIN_OF_PROTEIN * protein - 0.1) * VS_SOLIDS / (1 - VS_MOISTURE));
	}

	public static double cheeseKg(double milkL, double fat, double protein) {
		return milkL * MILK_KG_PER_L * vanSlykePer100(fat, protein) / 100;
	}

	/**
	 * Whey left by a cheese make, in litres: the milk's mass less the cheese's (the mass balance used in cheddar yield work, e.g. SDSU,
	 * "Evaluation of yield and quality of Cheddar cheese"), at the milk's 1.03 kg per litre *(proposed: whey is a little lighter, about
	 * 1.02-1.03)*. About 9 kg of whey per kg of cheddar at a 10% yield.
	 */
	public static double wheyL(double milkL, double cheeseKg) {
		return Math.max(0, milkL * MILK_KG_PER_L - cheeseKg) / MILK_KG_PER_L;
	}
}

package dev.farmings.husbandry.health;

import java.util.EnumSet;
import java.util.Set;

/**
 * Treatments, by drug class (never a brand), with the label withdrawal times (docs/animal-health.md, "Treatment and withdrawal"). One item
 * is one course.
 */
public enum Drug {
	/** Intramammary cephapirin sodium: milk 96 h, meat 4 days (label, via the Minnesota Dept. of Agriculture). */
	MASTITIS_TUBE("intramammary tube", 4, 4, EnumSet.of(Disease.MASTITIS)),
	/** Long-acting oxytetracycline 200 mg/mL: meat 28 days, milk 10 days (an EU-authorised label). */
	INJECTION("long-acting antibiotic", 10, 28, EnumSet.of(Disease.BRD, Disease.FOOT_ROT, Disease.ERYSIPELAS, Disease.SCOURS)),
	/** Oral rehydration salts: no withdrawal. */
	ELECTROLYTES("electrolytes", 0, 0, EnumSet.of(Disease.SCOURS)),
	/** Oral anticoccidial in the water: meat 5 days (proposed; labels vary). */
	COCCIDIOSTAT("coccidiostat", 0, 5, EnumSet.of(Disease.COCCIDIOSIS)),
	/** 10% zinc sulfate footbath (MSD): no withdrawal. */
	FOOTBATH("zinc sulfate footbath", 0, 0, EnumSet.of(Disease.FOOT_ROT));

	public final String label;
	public final double milkWithdrawalDays;
	public final double meatWithdrawalDays;
	public final Set<Disease> treats;

	Drug(String label, double milkDays, double meatDays, Set<Disease> treats) {
		this.label = label;
		this.milkWithdrawalDays = milkDays;
		this.meatWithdrawalDays = meatDays;
		this.treats = treats;
	}
}

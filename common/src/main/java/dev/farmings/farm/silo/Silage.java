package dev.farmings.farm.silo;

import dev.farmings.husbandry.TimeScale;

/** Silo and bin rules (docs/multiblocks/silo.md). Plain Java. */
public final class Silage {
	private Silage() {
	}

	/** Packing target, kg DM per m3 (15 lb DM/ft3, UW-Madison). */
	public static final double DM_PER_M3 = 240;
	/** Dry matter lost from filling to feeding: the middles of R.E. Muck's phases, 1.5 + 2.5 + 1 + 5.5 % (the first build took it all at fill). */
	public static final double DM_LOSS = 0.105;
	/** Taken as forage goes in (silo.md, "losses over time"): filling 1.5% and effluent 1% (Muck: 1-2% and 0-2%). */
	public static final double FILL_LOSS = 0.015 + 0.01;
	/** Spread evenly over the 21 days of fermenting: 2.5% (Muck: 1-4%). */
	public static final double FERMENT_LOSS = 0.025;
	/** After fermenting, a day: about 4.3% over 2 years in an oxygen-limiting tower (Muck, ARS, as quoted by a news article), *derived*. */
	public static final double STORAGE_LOSS_PER_DAY = 0.043 / 730;

	/** Share of the dry matter now in the silo lost between {@code fromDays} and {@code toDays} after the last fill. */
	public static double lossBetween(double fromDays, double toDays) {
		if (toDays <= fromDays) {
			return 0;
		}
		double fermentDays = Math.max(0, Math.min(toDays, FERMENT_DAYS) - Math.max(fromDays, 0));
		double storeDays = Math.max(0, toDays - Math.max(fromDays, FERMENT_DAYS));
		double keep = (1 - FERMENT_LOSS * fermentDays / FERMENT_DAYS) * Math.pow(1 - STORAGE_LOSS_PER_DAY, storeDays);
		return 1 - keep;
	}
	/** Days after the last fill before the silo opens (Penn State: stable in about 21 days). */
	public static final double FERMENT_DAYS = 21;
	/** Chopped forage and silage items, kg as-fed (proposed). */
	public static final double ITEM_KG = 25;
	/** Dry matter shares: chopped corn 35%, corn silage 34% (Auburn table), chopped alfalfa and haylage 37.5% (Penn State 35-40%). */
	public static final double CHOPPED_CORN_DM = 0.35;
	public static final double CORN_SILAGE_DM = 0.34;
	public static final double HAYLAGE_DM = 0.375;
	/** Unloader rate: 400 lb/min of haylage (Harvestore XL), kg as-fed a second. */
	public static final double UNLOAD_KG_PER_S = 400 * 0.45359237 / 60;
	/** Bulk density of grain, kg/m3: 56, 32 and 48 lb per 1.2445 ft3 bushel (Ohio State AGF-503). */
	public static final double CORN_DENSITY = 56 * 0.45359237 / (1.2445 * 0.0283168);
	public static final double OAT_DENSITY = 32 * 0.45359237 / (1.2445 * 0.0283168);
	public static final double BARLEY_DENSITY = 48 * 0.45359237 / (1.2445 * 0.0283168);

	public static double capacityKgDm(int volumeM3) {
		return volumeM3 * DM_PER_M3;
	}

	/** Dry matter left in the silo at filling out of {@code dmIn} blown in; fermenting and storage take more later. */
	public static double kept(double dmIn) {
		return dmIn * (1 - FILL_LOSS);
	}

	public static long fermentTicks() {
		return TimeScale.realDaysToTicks(FERMENT_DAYS);
	}

	/** Ticks between unloaded items (one 25 kg item at the unloader's rate). */
	public static int unloadTicks() {
		return (int) Math.round(ITEM_KG / UNLOAD_KG_PER_S * 20);
	}
}

package dev.farmings.farm.digester;

import java.util.ArrayList;
import java.util.List;

/**
 * A complete-mix manure digester (docs/multiblocks/digester.md). Each feeding gives its biogas evenly over the retention time; half heats
 * the tank, half goes to the gas holder; after the retention time it leaves as digestate, less the gas's mass. The digester's own clock
 * runs only while its mixer has power. Plain Java.
 */
public final class Digester {
	/** Clover Hill Dairy: an HRT of 20 days (EPA AgSTAR). */
	public static final double RETENTION_DAYS = 20;
	/** 23 m3 of biogas a wet tonne of dairy manure (Ontario OMAFA; Wildcatter's genset is calibrated on it). */
	public static final double M3_PER_KG = 0.023;
	/** Half the biogas heats the tank when there is no engine heat (MU Extension: 50%). */
	public static final double HEATING_SHARE = 0.5;
	/** Biogas, 60% CH4, about 1.2 kg/m3 (derived). */
	public static final double KG_PER_M3 = 1.2;
	/** Gas holder, m3; digestate tank, kg (both proposed). */
	public static final double HOLDER_M3 = 200;
	public static final double DIGESTATE_KG = 50_000;
	/** Separated solids take 10% of the digestate's mass (proposed, inside Cornell's 5-15% by volume). */
	public static final double SOLIDS_SHARE = 0.10;

	private record Batch(double kg, double fedDay) {
	}

	private final List<Batch> batches = new ArrayList<>();
	private double day;
	private double gasM3;
	private double digestateKg;
	private double heatedM3;

	public double day() {
		return day;
	}

	public double gasM3() {
		return gasM3;
	}

	public double digestateKg() {
		return digestateKg;
	}

	/** Manure (wet kg) in the tank now. */
	public double inTankKg() {
		double kg = 0;
		for (Batch b : batches) {
			kg += b.kg();
		}
		return kg;
	}

	public void feed(double kg) {
		if (kg > 0) {
			batches.add(new Batch(kg, day));
		}
	}

	/** {@code days} of digestion while stirred and warm; nothing happens unpowered. */
	public void step(double days, boolean powered) {
		if (!powered || days <= 0) {
			return;
		}
		double from = day;
		double to = day + days;
		for (int i = 0; i < batches.size(); i++) {
			Batch b = batches.get(i);
			double end = b.fedDay() + RETENTION_DAYS;
			double overlap = Math.max(0, Math.min(to, end) - Math.max(from, b.fedDay()));
			double gas = b.kg() * M3_PER_KG * overlap / RETENTION_DAYS;
			heatedM3 += gas * HEATING_SHARE;
			gasM3 = Math.min(HOLDER_M3, gasM3 + gas * (1 - HEATING_SHARE));
			if (to >= end - 1e-9) {
				digestateKg = Math.min(DIGESTATE_KG, digestateKg + b.kg() * (1 - M3_PER_KG * KG_PER_M3));
				batches.remove(i--);
			}
		}
		day = to;
	}

	public double takeGas(double m3) {
		double t = Math.max(0, Math.min(m3, gasM3));
		gasM3 -= t;
		return t;
	}

	public double takeDigestate(double kg) {
		double t = Math.max(0, Math.min(kg, digestateKg));
		digestateKg -= t;
		return t;
	}

	public double[] save() {
		double[] v = new double[4 + batches.size() * 2];
		v[0] = day;
		v[1] = gasM3;
		v[2] = digestateKg;
		v[3] = heatedM3;
		for (int i = 0; i < batches.size(); i++) {
			v[4 + 2 * i] = batches.get(i).kg();
			v[5 + 2 * i] = batches.get(i).fedDay();
		}
		return v;
	}

	public void load(double[] v) {
		batches.clear();
		if (v == null || v.length < 4) {
			return;
		}
		day = v[0];
		gasM3 = v[1];
		digestateKg = v[2];
		heatedM3 = v[3];
		for (int i = 4; i + 1 < v.length; i += 2) {
			batches.add(new Batch(v[i], v[i + 1]));
		}
	}
}

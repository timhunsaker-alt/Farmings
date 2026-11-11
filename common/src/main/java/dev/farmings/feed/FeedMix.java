package dev.farmings.feed;

/**
 * What a feeder holds: kilograms and the dry-matter-weighted analysis of the mixture. Taking from it takes the mixture as it is. Plain
 * Java; saved by its owner as six numbers.
 */
public final class FeedMix {
	private double kg;
	private Ration.Analysis analysis = Ration.Analysis.BALANCED;

	public double kg() {
		return kg;
	}

	public Ration.Analysis analysis() {
		return analysis;
	}

	public void add(double addKg, Ration.Analysis a) {
		if (addKg <= 0) {
			return;
		}
		analysis = kg <= 1e-9 ? a : analysis.mix(kg, a, addKg);
		kg += addKg;
	}

	/** Takes up to {@code want} kg; returns what it took. */
	public double take(double want) {
		double t = Math.max(0, Math.min(want, kg));
		kg -= t;
		return t;
	}

	public void set(double kg, Ration.Analysis a) {
		this.kg = Math.max(0, kg);
		this.analysis = a;
	}
}

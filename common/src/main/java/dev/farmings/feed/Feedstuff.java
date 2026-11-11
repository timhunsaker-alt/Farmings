package dev.farmings.feed;

/**
 * What a feed is made of, per kg of dry matter (docs/crops.md, "Feed values": the 2014 feed composition table hosted by Auburn; and
 * docs/multiblocks/feed-mill.md for the mill's ingredients). Plain Java.
 */
public enum Feedstuff {
	// DM %, CP %, NDF %, TDN %, Ca %, forage?
	ALFALFA_HAY(89, 17, 47, 58, 0, true),
	/** Timothy (11 CP, 63 NDF) and orchardgrass (10, 67) hay averaged; the vanilla hay block counts as this. */
	GRASS_HAY(88, 10.5, 65, 59, 0, true),
	CORN_SILAGE(34, 8, 46, 72, 0, true),
	/** Alfalfa's analysis at 37.5% DM (proposed: the same plant, wetter). */
	HAYLAGE(37.5, 17, 47, 58, 0, true),
	/** Oat and barley straw averaged. */
	STRAW(90.5, 4, 75.5, 46, 0, true),
	/** Grazed pasture: grass hay's analysis at the 24% DM of fresh forage (proposed; no fresh-grass row in the table). */
	PASTURE(24, 10.5, 65, 59, 0, true),
	CORN_GRAIN(88, 9, 10, 88, 0, false),
	OAT_GRAIN(89, 13, 28, 76, 0, false),
	BARLEY_GRAIN(89, 12, 20, 84, 0, false),
	/** Vanilla seeds and wheat as a grain (proposed: barley's analysis). */
	MIXED_GRAIN(89, 12, 20, 84, 0, false),
	ALFALFA_MEAL(89, 17, 47, 58, 0, true),
	/** TO VERIFY: textbook-typical, no source looked up. */
	FISH_MEAL(92, 62, 0, 75, 0, false),
	/** TO VERIFY: Ca 38%. */
	LIMESTONE(100, 0, 0, 0, 38, false),
	/** Fed at 0.3 kg a cow a day (TO VERIFY, a product label). */
	PREMIX(100, 0, 0, 0, 0, false);

	public final double dm;
	public final double cp;
	public final double ndf;
	public final double tdn;
	public final double ca;
	public final boolean forage;

	Feedstuff(double dmPct, double cp, double ndf, double tdn, double ca, boolean forage) {
		this.dm = dmPct / 100.0;
		this.cp = cp;
		this.ndf = ndf;
		this.tdn = tdn;
		this.ca = ca;
		this.forage = forage;
	}

	public Ration.Analysis analysis() {
		return new Ration.Analysis(cp, ndf, tdn, forage ? 1 : 0, ca, this == PREMIX ? 1 : 0);
	}
}

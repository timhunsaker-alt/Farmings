package dev.farmings.husbandry.shed;

/**
 * The sheep shed's bedding store (docs/multiblocks/sheep-shed.md, "bedding and manure"). Plain Java.
 * <ul>
 *   <li>Manure: 5 L a day from 60 kg, 3.3 L under 60 kg (UK SI 2021/77, Schedule 1), 1.8 L for lambs (the same table's "6 months to first
 *       lambing", used for all lambs, proposed); 1 L as 1 kg *(proposed)*.</li>
 *   <li>Straw: 1 kg a housed sheep a day ("lowland ewes on average require 7kg straw bedding each week", Irish Farmers Journal), and the
 *       used straw joins the soiled store.</li>
 *   <li>The store holds 64 manure items of 50 kg *(proposed, as the coop's litter)*; full, or with no clean straw, the bedding is dirty.</li>
 * </ul>
 */
public final class ShedBedding {
	public static final double MANURE_ITEM_KG = 50;
	public static final int MAX_ITEMS = 64;
	public static final double MAX_SOILED_KG = MANURE_ITEM_KG * MAX_ITEMS;
	public static final double MAX_STRAW_KG = 1000;
	public static final double STRAW_KG_PER_DAY = 1.0;
	public static final double LOOSE_STRAW_KG = 10;
	public static final double STRAW_BALE_KG = 454;

	private double strawKg;
	private double soiledKg;

	/** Manure a sheep makes in a real day, kg. */
	public static double manureKgPerDay(double weightKg, boolean lamb) {
		return lamb ? 1.8 : weightKg >= 60 ? 5.0 : 3.3;
	}

	/** One step of {@code dayShare} of a day for one housed sheep. */
	public void housed(double weightKg, boolean lamb, double dayShare) {
		double used = Math.min(strawKg, STRAW_KG_PER_DAY * dayShare);
		strawKg -= used;
		soiledKg = Math.min(MAX_SOILED_KG, soiledKg + manureKgPerDay(weightKg, lamb) * dayShare + used);
	}

	/** Adds straw; returns how much of {@code kg} went in (the rest doesn't fit). */
	public double addStraw(double kg) {
		double room = Math.max(0, MAX_STRAW_KG - strawKg);
		double in = Math.min(room, kg);
		strawKg += in;
		return in;
	}

	public boolean hasRoomFor(double kg) {
		return strawKg + kg <= MAX_STRAW_KG + 1e-9;
	}

	/** Dirty: the store is full, or there are sheep housed and no clean straw. */
	public boolean dirty(boolean anyHoused) {
		return soiledKg >= MAX_SOILED_KG - 1e-9 || (anyHoused && strawKg <= 1e-9);
	}

	public int manureItems() {
		return (int) Math.min(MAX_ITEMS, Math.floor(soiledKg / MANURE_ITEM_KG + 1e-9));
	}

	/** Takes up to {@code n} manure items out; returns how many. */
	public int takeManure(int n) {
		int took = Math.min(n, manureItems());
		soiledKg -= took * MANURE_ITEM_KG;
		return took;
	}

	/** Keeps the store in step after a hopper put back part of what it took: {@code items} whole items, plus the part-item left over. */
	public void setManureItems(int items) {
		soiledKg = Math.min(MAX_SOILED_KG, soiledKg % MANURE_ITEM_KG + items * MANURE_ITEM_KG);
	}

	public double strawKg() {
		return strawKg;
	}

	public double soiledKg() {
		return soiledKg;
	}

	public void load(double straw, double soiled) {
		this.strawKg = Math.max(0, Math.min(MAX_STRAW_KG, straw));
		this.soiledKg = Math.max(0, Math.min(MAX_SOILED_KG, soiled));
	}
}

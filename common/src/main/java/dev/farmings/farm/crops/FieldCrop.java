package dev.farmings.farm.crops;

import dev.farmings.husbandry.TimeScale;

/**
 * The Phase 3 crops (docs/crops.md): days to harvest, regrowth and stand life, and the yield one block gives at one harvest, scaled x10
 * from the real square metre (Tim, 2026-11-11: 1 t/ha gives 1 kg a block). Days are real days on the 15:1 clock.
 */
public enum FieldCrop {
	// UMaine 2245 / Ohio State: first cut 65 days (60-70), 32.5-day regrowth (30-35), 5.5-year stand (3-8) at 4 cuts a year;
	// 4 t hay/ac a year at 89% DM = 7.97 t DM/ha, split over 4 cuts.
	ALFALFA("alfalfa", Kind.PERENNIAL, 65, 32.5, 22, 2.0, 0.89),
	// UMaine 2265: 2 t hay/ac a year in 2 cuts at 88% DM. First cut, regrowth and stand life NOT FOUND (proposed).
	TIMOTHY("timothy", Kind.PERENNIAL, 60, 42, 10, 1.97, 0.88),
	// UMaine 2266: 3.5 t DM/ac (2-5, TO VERIFY) in 4 cuts, 35-day regrowth (4-6 weeks). First cut and stand life proposed.
	ORCHARDGRASS("orchardgrass", Kind.PERENNIAL, 60, 35, 20, 1.48, 0.88),
	// Kentucky ID-275: 22.5 t as-fed/ac at 35% DM = 17.6 t DM/ha. Silage at 100 days (proposed); grain at 114 (relative maturity);
	// grain 179.3 bu/ac x 56 lb = 11.25 t/ha (USDA NASS 2024, TO VERIFY).
	CORN("corn", Kind.CORN, 100, 0, 1, 17.6, 0.35),
	// Wisconsin trials: 61 bu/ac x 32 lb = 2.19 t/ha; straw 2.03 t/ac (Iowa State) = 4.55 t/ha. 100 days TO VERIFY.
	OATS("oats", Kind.GRAIN, 100, 0, 1, 2.19, 0.89),
	// USDA NASS 2024: 76.7 bu/ac x 48 lb = 4.13 t/ha; straw 1.01 t/ac (Ohio State) = 2.26 t/ha; 85 days (Alaska, TO VERIFY).
	BARLEY("barley", Kind.GRAIN, 87, 0, 1, 4.13, 0.89);

	public enum Kind {
		/** Cut and regrows (alfalfa, grasses). */
		PERENNIAL,
		/** Combined for grain and straw (oats, barley). */
		GRAIN,
		/** Chopped for silage at 100 days, or combined for grain at 114. */
		CORN
	}

	/** Visible stages 0 to 7, as vanilla wheat. */
	public static final int MAX_STAGE = 7;
	/** A cut perennial drops back to this stage (proposed). */
	public static final int REGROWTH_STAGE = 3;
	/** Corn is ripe for grain at this many days (Kentucky: 114-day relative maturity). */
	public static final double CORN_GRAIN_DAYS = 114;
	/** Corn grain a block, kg (179.3 bu/ac x 56 lb = 11.25 t/ha, x10). */
	public static final double CORN_GRAIN_KG = 11.25;
	/** Straw a block, kg, for the small grains (oats 4.55, barley 2.26 t/ha, x10). */
	public static final double OAT_STRAW_KG = 4.55;
	public static final double BARLEY_STRAW_KG = 2.26;

	public final String id;
	public final Kind kind;
	/** Seed to first harvest, real days. */
	public final double harvestDays;
	/** A perennial's days from cut to cut. */
	public final double regrowthDays;
	/** Cuts a perennial stand gives before it dies (stand years x cuts a year). */
	public final int standCuts;
	/** Per block per harvest: kg of dry matter for forages and corn silage, kg of grain for oats and barley. */
	public final double yieldKg;
	/** Dry matter share at harvest (hay at baling; corn at chopping). */
	public final double dryMatter;

	FieldCrop(String id, Kind kind, double harvestDays, double regrowthDays, int standCuts, double yieldKg, double dryMatter) {
		this.id = id;
		this.kind = kind;
		this.harvestDays = harvestDays;
		this.regrowthDays = regrowthDays;
		this.standCuts = standCuts;
		this.yieldKg = yieldKg;
		this.dryMatter = dryMatter;
	}

	/** Ticks from one stage to the next while growing from seed. */
	public long stageTicks() {
		return Math.max(1, TimeScale.realDaysToTicks(harvestDays) / MAX_STAGE);
	}

	/** Ticks from one stage to the next while a cut perennial regrows from {@link #REGROWTH_STAGE}. */
	public long regrowthStageTicks() {
		return Math.max(1, TimeScale.realDaysToTicks(regrowthDays) / (MAX_STAGE - REGROWTH_STAGE));
	}

	/** Hay mass as baled from one block's cut (dry matter over the hay's DM share). */
	public double hayKg() {
		return yieldKg / dryMatter;
	}

	/** Corn chopped as-fed from one block (dry matter over 35% DM). */
	public double choppedKg() {
		return yieldKg / dryMatter;
	}

	public double strawKg() {
		return this == OATS ? OAT_STRAW_KG : this == BARLEY ? BARLEY_STRAW_KG : 0;
	}

	/** Ticks after reaching stage 7 until corn is ripe for grain. */
	public static long cornRipeningTicks() {
		return TimeScale.realDaysToTicks(CORN_GRAIN_DAYS - CORN.harvestDays);
	}
}

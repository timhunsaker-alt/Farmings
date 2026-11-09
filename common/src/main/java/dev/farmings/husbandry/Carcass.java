package dev.farmings.husbandry;

import java.util.Map;

/**
 * What an animal gives when it is killed (docs/animals/cattle.md, Beef). Plain Java, no Minecraft.
 *
 * <ul>
 *   <li>Carcass = live weight x dressing %. Penn State Extension ("Understanding beef carcass yields and losses during processing"):
 *       most cattle about 63%, dairy breeds 56-60% (58% used).</li>
 *   <li>Boneless retail meat = 50% of the carcass (Penn State, yield grade 3).</li>
 *   <li>Steaks: the four main steak cuts are 8-12% of the carcass (Penn State); 10% is used. They come out of the retail meat, so the
 *       rest of the retail meat (40%) drops as beef.</li>
 *   <li>Wagyu drop more steaks than Angus: Tim's written exception (2026-11-07), a game rule, not a realism claim. A purebred Wagyu's
 *       steak share is 1.5 times the base (proposed), scaled by its Wagyu share.</li>
 *   <li>Marbling score: Angus 597, Wagyu 771 (Twin Wood Cattle: "Production characteristics and carcass quality of Angus and Wagyu").
 *       Dairy breeds 450 (proposed, no source). USDA grade by marbling: below 400 Select, 400-699 Choice, 700 and up Prime (the score
 *       scale used by that study: 400 Small, 500 Modest, 600 Moderate, 700 Slightly Abundant).</li>
 *   <li>One meat item stands for 10 kg (Tim, 2026-11-09).</li>
 * </ul>
 * The three beef traits (growth, dressing %, marbling) are read from the animal's breed composition, weighted by its share of each
 * breed. They are not genome loci, so the FarmChain-checked genome is unchanged.
 */
public final class Carcass {
	public static final double KG_PER_ITEM = 10;
	public static final double BEEF_DRESSING = 0.63;
	public static final double DAIRY_DRESSING = 0.58;
	public static final double RETAIL_YIELD = 0.50;
	public static final double STEAK_SHARE = 0.10;
	public static final double WAGYU_STEAK_BONUS = 1.5;

	public enum Grade {
		SELECT, CHOICE, PRIME
	}

	public record Yield(double carcassKg, int beefItems, int steakItems, int marbling, Grade grade) {
	}

	private Carcass() {
	}

	private static double share(Map<String, Integer> compositionBp, String code) {
		return compositionBp.getOrDefault(code, 0) / 10_000.0;
	}

	public static double dressing(Map<String, Integer> compositionBp) {
		double d = 0;
		double total = 0;
		for (Map.Entry<String, Integer> e : compositionBp.entrySet()) {
			double w = e.getValue() / 10_000.0;
			CattleBreed b = CattleBreed.byCode(e.getKey());
			d += w * (b.use == CattleBreed.Use.BEEF ? BEEF_DRESSING : DAIRY_DRESSING);
			total += w;
		}
		return total <= 0 ? DAIRY_DRESSING : d / total;
	}

	public static int marblingOf(CattleBreed breed) {
		return switch (breed) {
			case ANGUS -> 597;
			case WAGYU -> 771;
			default -> 450;
		};
	}

	public static int marbling(Map<String, Integer> compositionBp) {
		double m = 0;
		double total = 0;
		for (Map.Entry<String, Integer> e : compositionBp.entrySet()) {
			double w = e.getValue() / 10_000.0;
			m += w * marblingOf(CattleBreed.byCode(e.getKey()));
			total += w;
		}
		return (int) Math.round(total <= 0 ? 450 : m / total);
	}

	public static Grade grade(int marbling) {
		if (marbling >= 700) {
			return Grade.PRIME;
		}
		return marbling >= 400 ? Grade.CHOICE : Grade.SELECT;
	}

	public static Yield of(double liveKg, Map<String, Integer> compositionBp) {
		double carcass = liveKg * dressing(compositionBp);
		double steakShare = STEAK_SHARE * (1 + (WAGYU_STEAK_BONUS - 1) * share(compositionBp, "WAG"));
		double steakKg = carcass * steakShare;
		double beefKg = Math.max(0, carcass * RETAIL_YIELD - steakKg);
		int m = marbling(compositionBp);
		return new Yield(carcass, (int) Math.floor(beefKg / KG_PER_ITEM), (int) Math.floor(steakKg / KG_PER_ITEM), m, grade(m));
	}
}

package dev.farmings.feed;

import dev.farmings.husbandry.Stage;
import java.util.OptionalDouble;

/**
 * Hay and water rules for cattle (docs/animals/cattle.md, Feed and water). Hay is the only feed in Phase 1.
 */
public final class Hay {
	/** One hay block is 100 kg of fresh hay (decided with Tim, 2026-11-07; a game size). */
	public static final double BLOCK_FRESH_KG = 100;
	/** Hay bales are 85% dry matter (Victoria feed table), so a block holds 85 kg of dry matter. */
	public static final double DRY_MATTER_FRACTION = 0.85;
	/** Forage-based planning figure: dry matter eaten per day as a share of body weight, lactating cow (TO VERIFY against NRC). */
	public static final double LACTATING_INTAKE_FRACTION = 0.027;
	/** Same, dry cow (TO VERIFY against NRC). */
	public static final double DRY_INTAKE_FRACTION = 0.022;

	private Hay() {
	}

	public static double blockDryMatterKg() {
		return BLOCK_FRESH_KG * DRY_MATTER_FRACTION;
	}

	/**
	 * Dry-matter intake in kg a day. Only lactating and dry cows have a sourced figure. Other stages return empty until
	 * Tim approves numbers for them; nothing is invented.
	 */
	public static OptionalDouble dryMatterIntakeKg(Stage stage, double weightKg) {
		return switch (stage) {
			case LACTATING_COW -> OptionalDouble.of(weightKg * LACTATING_INTAKE_FRACTION);
			case DRY_COW -> OptionalDouble.of(weightKg * DRY_INTAKE_FRACTION);
			default -> OptionalDouble.empty();
		};
	}

	/** Hay blocks a cow eats in one day, from the dry matter she needs. */
	public static double blocksPerDay(double dryMatterKg) {
		return dryMatterKg / blockDryMatterKg();
	}

	/** UC Davis rule (decided with Tim): 6 L of water per kg of dry matter eaten plus 1 L per litre of milk. No heat term. */
	public static double waterLitresPerDay(double dryMatterKg, double milkLitres) {
		return 6.0 * dryMatterKg + milkLitres;
	}
}

package dev.farmings.health;

import dev.farmings.genetics.Species;
import dev.farmings.husbandry.health.HealthRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import org.jspecify.annotations.Nullable;

/**
 * What animal health needs from an animal of any Farmings species (docs/animal-health.md). Each species keeps its own entity class; this
 * is the shared face, as {@link dev.farmings.entity.SmallRuminant} is for the shed.
 */
public interface HealthHost extends dev.farmings.entity.Wild.Animal {
	Animal animal();

	Species species();

	HealthRecord health();

	String herdId();

	/** The housing it belongs to (barn, shed, coop, pig barn or stable controller), or null. */
	@Nullable BlockPos housing();

	/** Leaves its housing (sent to a sick pen or quarantine); its housing may take it back later. */
	void leaveHousing();

	/** The genome's HEALTH score, 0-100 (50 with no genome). */
	int healthScore();

	/** The genome's HARDINESS score, 0-100 (50 where the species has none). */
	int hardinessScore();

	double ageRealDays();

	/** How dirty its stall, bedding or litter is, 0-1. */
	default double dirtShare() {
		return 0;
	}

	/** The share of its feed it got (1 when it is not housed). */
	default double fedShare() {
		return 1;
	}

	/** Its ration's score, 0-1, or -1 when none is known. */
	default double rationScore() {
		return -1;
	}

	/** The forage share of its running ration, 0-1, or -1 (for the grain jump that sets off overeating disease). */
	default double forageShare() {
		return -1;
	}

	/** A young animal out of heat (the cold rule for piglets and chicks). */
	default boolean cold() {
		return false;
	}

	/** In milk, and for how many days. */
	default boolean inMilk() {
		return false;
	}

	default double milkDays() {
		return 0;
	}

	/** Sends the signs of sickness to the client ({@link HealthSigns} flags). */
	void syncSigns(int signs);

	/** The signs of sickness as last synced (client side, for drawing). */
	int syncedSigns();

}

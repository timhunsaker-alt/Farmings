package dev.farmings.entity;

import dev.farmings.genetics.Species;
import dev.farmings.husbandry.parlor.MilkKind;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * What the sheep shed, the pasture, the parlor's small stalls and the sheep dog need from a sheep or a goat (docs/multiblocks/sheep-shed.md,
 * "Goats"). Each species keeps its own entity class and rules (Flock, Herd); this is only the shared face.
 */
public interface SmallRuminant {
	/** The animal itself. */
	Animal animal();

	Species species();

	boolean isWild();

	/** A lamb or kid not yet weaned: it counts as nothing in a pen and follows its dam. */
	boolean young();

	/** Real days to birth, or -1 when not pregnant. */
	double daysToBirth();

	/** Real days since the last birth (large when none). */
	double daysSinceBirth();

	/** Young she is nursing. */
	int youngNursing();

	String herdId();

	String damId();

	double ageRealDays();

	double weightKg();

	double dryMatterIntakeKgPerDay();

	void recordRation(dev.farmings.feed.Ration.Analysis a);

	void recordTending(double fed, double watered);

	double lastFed();

	double lastWatered();

	/** Pen floor blocks an adult takes (sheep 1.5, goats 2). */
	double penBlocks();

	/** Fresh manure a day, kg, into the shed's bedding. */
	double manureKgPerDay();

	/** Litres of water a day per kg of dry matter eaten. */
	double waterLPerKgDm();

	/** The share of its day's dry matter it can browse from leaves beside its pasture spot (goats; 0 for sheep). */
	default double browseShare() {
		return 0;
	}

	// ---- the parlor's small stalls ----

	/** A female with milk for the parlor (an East Friesian ewe, a Saanen or Nubian doe). */
	boolean dairy();

	double milkKg();

	double takeMilk();

	double milkFatPct();

	double milkProteinPct();

	MilkKind milkKind();

	// ---- placement ----

	Placement place();

	default @Nullable BlockPos shedController() {
		return place().shedController();
	}

	default @Nullable BlockPos home() {
		return place().home();
	}

	default void assignShed(@Nullable BlockPos controller, @Nullable BlockPos home) {
		place().assignShed(controller, home);
	}

	default void sendTo(@Nullable BlockPos target) {
		place().sendTo(target);
	}

	default @Nullable BlockPos sentTo() {
		return place().sentTo();
	}

	default @Nullable BlockPos reservedBy() {
		return place().reservedBy();
	}

	default void reserve(@Nullable BlockPos stand) {
		place().reserve(stand);
	}

	/** Every sheep or goat in the box that passes the test. */
	static List<SmallRuminant> in(Level level, AABB box, Predicate<SmallRuminant> test) {
		List<SmallRuminant> out = new ArrayList<>();
		for (Animal a : level.getEntitiesOfClass(Animal.class, box, a -> a instanceof SmallRuminant r && test.test(r))) {
			out.add((SmallRuminant) a);
		}
		return out;
	}
}

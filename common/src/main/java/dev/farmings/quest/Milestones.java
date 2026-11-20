package dev.farmings.quest;

import dev.farmings.Farmings;
import net.minecraft.resources.Identifier;

/**
 * The milestone keys Farmings' machines fire through Wildcatter's {@code QuestEvents.milestone} (Wildcatter docs/quests.md,
 * "Milestones"). Every player within the hook's radius of the machine reaches the milestone.
 */
public final class Milestones {
	/** A wild animal, of any species, has settled into the herd in a taming corral (docs/taming.md); fired at the corral post. */
	public static final Identifier TAMED = Farmings.id("tamed");
	/** An animal examined with the vet kit (docs/animal-health.md, "Progression"). */
	public static final Identifier EXAMINED = Farmings.id("examined");
	/** A vaccine dose that counted. */
	public static final Identifier VACCINATED = Farmings.id("vaccinated");
	/** An isolation pen formed. */
	public static final Identifier ISOLATION_PEN = Farmings.id("isolation_pen");
	/** A parlor dipping teats (a teat dip bottle loaded). */
	public static final Identifier TEAT_DIP = Farmings.id("teat_dip");
	/** A footbath charged with zinc sulfate. */
	public static final Identifier FOOTBATH = Farmings.id("footbath");

	private Milestones() {
	}
}

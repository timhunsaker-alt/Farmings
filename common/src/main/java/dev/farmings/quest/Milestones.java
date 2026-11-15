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

	private Milestones() {
	}
}

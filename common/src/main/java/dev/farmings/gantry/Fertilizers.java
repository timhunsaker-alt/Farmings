package dev.farmings.gantry;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** What counts as one fertilizer use (waste.md: bone meal's effect): bone meal now; compost and liquid digestate join with their parts. */
public final class Fertilizers {
	private Fertilizers() {
	}

	public static java.util.function.Predicate<ItemStack> more = s -> false;

	public static boolean is(ItemStack s) {
		return s.is(Items.BONE_MEAL) || more.test(s);
	}
}

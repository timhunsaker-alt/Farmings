package dev.farmings.gantry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What counts as one fertilizer use (waste.md: bone meal's effect): bone meal, compost and liquid digestate (added by their parts), and
 * anything in the convention tag {@code c:fertilizers}, which is where Wildcatter puts its TSP, DAP and MOP bags (its fertilizer.md).
 */
public final class Fertilizers {
	/** The common fertilizer tag other mods fill. */
	public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "fertilizers"));

	private Fertilizers() {
	}

	public static java.util.function.Predicate<ItemStack> more = s -> false;

	public static boolean is(ItemStack s) {
		return s.is(Items.BONE_MEAL) || s.is(TAG) || more.test(s);
	}
}

package dev.farmings.gantry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What counts as fertilizer in the gantry's spreader (waste.md: one use is bone meal's effect): bone meal, compost and liquid digestate
 * (added by their parts), and anything in the convention tag {@code c:fertilizers}, which is where Wildcatter puts its TSP, DAP and MOP
 * bags. Uses go by mass, as compost does: one use is a 10 kg compost item (multiblocks/compost.md), so one of Wildcatter's 1 t bags is
 * 100 uses (Tim, 2026-11-14). Every other item is one use.
 */
public final class Fertilizers {
	/** The common fertilizer tag other mods fill. */
	public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "fertilizers"));
	/** The mass of one fertilizer use: one 10 kg compost item (multiblocks/compost.md). */
	public static final double USE_KG = 10;
	/** One of Wildcatter's bulk bags of TSP, DAP or MOP: 1 t (Wildcatter's README, "in 1 t bags"). */
	public static final double WILDCATTER_BAG_KG = 1000;

	private Fertilizers() {
	}

	public static java.util.function.Predicate<ItemStack> more = s -> false;

	public static boolean is(ItemStack s) {
		return s.is(Items.BONE_MEAL) || s.is(TAG) || more.test(s);
	}

	/** How many spreads one of this item gives: 100 for a Wildcatter bag in {@code c:fertilizers}, 1 for any other fertilizer, else 0. */
	public static int uses(ItemStack s) {
		if (!is(s)) {
			return 0;
		}
		if (s.is(TAG) && "wildcatter".equals(BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace())) {
			return (int) Math.round(WILDCATTER_BAG_KG / USE_KG);
		}
		return 1;
	}
}

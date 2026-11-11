package dev.farmings.feedmill;

import dev.farmings.Farmings;
import dev.farmings.crops.FieldCrops;
import dev.farmings.feed.Feedstuff;
import dev.farmings.feed.Ration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

/**
 * The mill's and mixer's items, and what every feed item is (docs/multiblocks/feed-mill.md): its feedstuff and its mass as fed. A ration
 * item carries its own analysis.
 */
public final class FeedItems {
	private FeedItems() {
	}

	public static final Map<String, Item> ALL = new LinkedHashMap<>();
	/** Meals, 1 kg an item (as the grain). */
	public static final Item CORN_MEAL = item("corn_meal", Item::new);
	public static final Item OAT_MEAL = item("oat_meal", Item::new);
	public static final Item BARLEY_MEAL = item("barley_meal", Item::new);
	public static final Item ALFALFA_MEAL = item("alfalfa_meal", Item::new);
	public static final Item FISH_MEAL = item("fish_meal", Item::new);
	public static final Item GROUND_LIMESTONE = item("ground_limestone", Item::new);
	/** 1 kg an item (proposed). */
	public static final Item MINERAL_PREMIX = item("mineral_premix", Item::new);
	/** A mixed ration, 25 kg as fed (proposed), with its analysis. */
	public static final Item RATION = item("ration", p -> new RationItem(p.stacksTo(16)));

	private static Item item(String name, Function<Item.Properties, Item> f) {
		Item i = f.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Farmings.id(name))));
		ALL.put(name, i);
		return i;
	}

	public static void register() {
		ALL.forEach((n, i) -> Registry.register(BuiltInRegistries.ITEM, Farmings.id(n), i));
		dev.farmings.conveyor.ConveyorBlocks.meal = s -> s.is(CORN_MEAL) || s.is(OAT_MEAL) || s.is(BARLEY_MEAL) || s.is(ALFALFA_MEAL) || s.is(FISH_MEAL)
				|| s.is(GROUND_LIMESTONE) || s.is(MINERAL_PREMIX);
	}

	/** One feed item's mass as fed and analysis, or null if it is not feed. */
	public record Feed(double kgAsFed, double dm, Ration.Analysis analysis) {
		public double kgDm() {
			return kgAsFed * dm;
		}
	}

	private static Feed of(double kg, Feedstuff f) {
		return new Feed(kg, f.dm, f.analysis());
	}

	public static @Nullable Feed feed(ItemStack s) {
		if (s.is(Items.HAY_BLOCK)) {
			return new Feed(100, 0.85, Feedstuff.GRASS_HAY.analysis());       // the barn's hay block: 100 kg fresh, 85% DM (feed/Hay)
		}
		if (s.is(FieldCrops.ALFALFA_BALE)) {
			return new Feed(454, 0.82, Feedstuff.ALFALFA_HAY.analysis());
		}
		if (s.is(FieldCrops.GRASS_BALE)) {
			return new Feed(454, 0.82, Feedstuff.GRASS_HAY.analysis());
		}
		if (s.is(FieldCrops.STRAW_BALE)) {
			return of(454, Feedstuff.STRAW);
		}
		if (s.is(FieldCrops.ALFALFA_HAY)) {
			return new Feed(10, 0.82, Feedstuff.ALFALFA_HAY.analysis());
		}
		if (s.is(FieldCrops.GRASS_HAY)) {
			return new Feed(10, 0.82, Feedstuff.GRASS_HAY.analysis());
		}
		if (s.is(FieldCrops.STRAW)) {
			return of(10, Feedstuff.STRAW);
		}
		if (s.is(FieldCrops.CORN_SILAGE)) {
			return of(25, Feedstuff.CORN_SILAGE);
		}
		if (s.is(FieldCrops.HAYLAGE)) {
			return of(25, Feedstuff.HAYLAGE);
		}
		if (s.is(FieldCrops.CORN_GRAIN) || s.is(CORN_MEAL)) {
			return of(1, Feedstuff.CORN_GRAIN);
		}
		if (s.is(FieldCrops.OAT_GRAIN) || s.is(OAT_MEAL)) {
			return of(1, Feedstuff.OAT_GRAIN);
		}
		if (s.is(FieldCrops.BARLEY_GRAIN) || s.is(BARLEY_MEAL)) {
			return of(1, Feedstuff.BARLEY_GRAIN);
		}
		if (s.is(ALFALFA_MEAL)) {
			return of(1, Feedstuff.ALFALFA_MEAL);
		}
		if (s.is(FISH_MEAL)) {
			return of(1, Feedstuff.FISH_MEAL);
		}
		if (s.is(GROUND_LIMESTONE)) {
			return of(1, Feedstuff.LIMESTONE);
		}
		if (s.is(MINERAL_PREMIX)) {
			return of(1, Feedstuff.PREMIX);
		}
		if (s.is(RATION)) {
			return RationItem.feed(s);
		}
		if (dev.farmings.entity.Wild.grain(s)) {
			return of(dev.farmings.husbandry.poultry.Fowl.GRAIN_ITEM_KG, Feedstuff.MIXED_GRAIN);
		}
		return null;
	}

	/** Grain-sized feeds birds can take from a grain feeder: grain, seed, meal and rations. */
	public static boolean birdFeed(ItemStack s) {
		Feed f = feed(s);
		return f != null && (s.is(RATION) || f.kgAsFed() <= 1.0 + 1e-9);
	}

	static void putAnalysis(ItemStack stack, Ration.Analysis a, double dm) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
			t.putDouble("Cp", a.cp());
			t.putDouble("Ndf", a.ndf());
			t.putDouble("Tdn", a.tdn());
			t.putDouble("Forage", a.forage());
			t.putDouble("Ca", a.ca());
			t.putDouble("Premix", a.premix());
			t.putDouble("Dm", dm);
		});
	}

	static @Nullable Feed readRation(ItemStack s) {
		CustomData d = s.get(DataComponents.CUSTOM_DATA);
		if (d == null) {
			return null;
		}
		CompoundTag t = d.copyTag();
		return new Feed(RationItem.KG, t.getDoubleOr("Dm", 0.5), new Ration.Analysis(t.getDoubleOr("Cp", 0), t.getDoubleOr("Ndf", 0),
				t.getDoubleOr("Tdn", 0), t.getDoubleOr("Forage", 0), t.getDoubleOr("Ca", 0), t.getDoubleOr("Premix", 0)));
	}
}

package dev.farmings;

import dev.farmings.item.ManureForkItem;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Items that are not blocks: manure and the fork that collects it. */
public final class FarmingsItems {
	/** One manure item stands for 50 kg, about one cow-day (docs/multiblocks/barn.md; approved by Tim 2026-11-07). */
	public static final Item MANURE = item("manure", Item::new);
	/**
	 * A tool for clearing manure by hand (a new tool, decided with Tim 2026-11-07). Its durability is the ordinary 250 of an iron tool
	 * *(proposed, no source)*.
	 */
	public static final Item MANURE_FORK = item("manure_fork", props -> new ManureForkItem(props.durability(250).stacksTo(1)));

	/** An empty straw, collected into by right-clicking a grown bull (docs/animals/breeding.md). */
	public static final Item EMPTY_STRAW = item("empty_straw", props -> new dev.farmings.item.StrawItem(props.stacksTo(64), false));
	/** A straw holding one sire's genome; right-click a cow in heat to inseminate her. */
	public static final Item SEMEN_STRAW = item("semen_straw", props -> new dev.farmings.item.StrawItem(props.stacksTo(16), true));

	// Steaks by USDA grade (husbandry.Carcass). Raw: vanilla raw beef's food. Cooked: Select is vanilla steak (8 hunger); Choice 9 and
	// Prime 10 (proposed: better marbling, a little more food).
	public static final Item SELECT_STEAK = item("select_steak", p -> new Item(p.food(net.minecraft.world.food.Foods.BEEF)));
	public static final Item CHOICE_STEAK = item("choice_steak", p -> new Item(p.food(net.minecraft.world.food.Foods.BEEF)));
	public static final Item PRIME_STEAK = item("prime_steak", p -> new Item(p.food(net.minecraft.world.food.Foods.BEEF)));
	public static final Item COOKED_SELECT_STEAK = item("cooked_select_steak", p -> new Item(p.food(net.minecraft.world.food.Foods.COOKED_BEEF)));
	public static final Item COOKED_CHOICE_STEAK = item("cooked_choice_steak",
			p -> new Item(p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(9).saturationModifier(0.8F).build())));
	public static final Item COOKED_PRIME_STEAK = item("cooked_prime_steak",
			p -> new Item(p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(10).saturationModifier(0.9F).build())));

	public static Item steak(dev.farmings.husbandry.Carcass.Grade grade, boolean cooked) {
		return switch (grade) {
			case SELECT -> cooked ? COOKED_SELECT_STEAK : SELECT_STEAK;
			case CHOICE -> cooked ? COOKED_CHOICE_STEAK : CHOICE_STEAK;
			case PRIME -> cooked ? COOKED_PRIME_STEAK : PRIME_STEAK;
		};
	}

	/** Every item this class registers, by name, for registration and the asset/recipe checks. */
	public static final java.util.Map<String, Item> ALL = new java.util.LinkedHashMap<>();

	static {
		ALL.put("manure", MANURE);
		ALL.put("manure_fork", MANURE_FORK);
		ALL.put("empty_straw", EMPTY_STRAW);
		ALL.put("semen_straw", SEMEN_STRAW);
		ALL.put("select_steak", SELECT_STEAK);
		ALL.put("choice_steak", CHOICE_STEAK);
		ALL.put("prime_steak", PRIME_STEAK);
		ALL.put("cooked_select_steak", COOKED_SELECT_STEAK);
		ALL.put("cooked_choice_steak", COOKED_CHOICE_STEAK);
		ALL.put("cooked_prime_steak", COOKED_PRIME_STEAK);
	}

	private FarmingsItems() {
	}

	private static Item item(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Farmings.id(name));
		return factory.apply(new Item.Properties().setId(key));
	}

	public static void register() {
		ALL.forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, Farmings.id(name), item));
	}
}

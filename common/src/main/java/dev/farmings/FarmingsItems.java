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
	public static final Item SELECT_STEAK = item("select_steak", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.RAW_STEAK, p.food(net.minecraft.world.food.Foods.BEEF)));
	public static final Item CHOICE_STEAK = item("choice_steak", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.RAW_STEAK, p.food(net.minecraft.world.food.Foods.BEEF)));
	public static final Item PRIME_STEAK = item("prime_steak", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.RAW_STEAK, p.food(net.minecraft.world.food.Foods.BEEF)));
	public static final Item COOKED_SELECT_STEAK = item("cooked_select_steak", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.COOKED_STEAK, p.food(net.minecraft.world.food.Foods.COOKED_BEEF)));
	public static final Item COOKED_CHOICE_STEAK = item("cooked_choice_steak",
			p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.COOKED_STEAK, p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(9).saturationModifier(0.8F).build())));
	public static final Item COOKED_PRIME_STEAK = item("cooked_prime_steak",
			p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.COOKED_STEAK, p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(10).saturationModifier(0.9F).build())));

	public static Item steak(dev.farmings.husbandry.Carcass.Grade grade, boolean cooked) {
		return switch (grade) {
			case SELECT -> cooked ? COOKED_SELECT_STEAK : SELECT_STEAK;
			case CHOICE -> cooked ? COOKED_CHOICE_STEAK : CHOICE_STEAK;
			case PRIME -> cooked ? COOKED_PRIME_STEAK : PRIME_STEAK;
		};
	}

	/** The herd book (records each animal's parents and traits). */
	public static final Item HERD_BOOK = item("herd_book", p -> new dev.farmings.item.HerdBookItem(p.stacksTo(1)));

	// Milk armor, the easter egg (docs/milk-armor.md).
	public static final Item MILK_HELMET = item("milk_helmet", p -> new dev.farmings.item.MilkArmorItem(p, net.minecraft.world.item.equipment.ArmorType.HELMET));
	public static final Item MILK_CHESTPLATE = item("milk_chestplate", p -> new dev.farmings.item.MilkArmorItem(p, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
	public static final Item MILK_LEGGINGS = item("milk_leggings", p -> new dev.farmings.item.MilkArmorItem(p, net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
	public static final Item MILK_BOOTS = item("milk_boots", p -> new dev.farmings.item.MilkArmorItem(p, net.minecraft.world.item.equipment.ArmorType.BOOTS));

	// Creamery products (docs/multiblocks/creamery.md). Food values proposed. Bottles give back the glass bottle.
	private static net.minecraft.world.food.FoodProperties food(int nutrition, float saturation) {
		return new net.minecraft.world.food.FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build();
	}

	public static final Item BOTTLED_MILK = item("bottled_milk", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.MILK, p.stacksTo(16).usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
			.food(food(4, 0.3F), net.minecraft.world.item.component.Consumables.defaultDrink()
					.onConsume(net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect.INSTANCE).build())));
	public static final Item CREAM_BOTTLE = item("cream_bottle", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.CREAM, p.stacksTo(16).usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
			.food(food(2, 0.4F), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)));
	public static final Item SKIM_MILK_BOTTLE = item("skim_milk_bottle", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.SKIM, p.stacksTo(16).usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
			.food(food(2, 0.2F), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)));
	public static final Item BUTTERMILK_BOTTLE = item("buttermilk_bottle", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.BUTTERMILK, p.stacksTo(16).usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
			.food(food(2, 0.3F), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)));
	/** Whey from the cheese vat (creamery.md): a weak drink *(proposed food value)*; pigs will eat it when pigs come. */
	public static final Item WHEY_BOTTLE = item("whey_bottle", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.WHEY, p.stacksTo(16).usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
			.food(food(1, 0.1F), net.minecraft.world.item.component.Consumables.DEFAULT_DRINK)));
	public static final Item BUTTER = item("butter", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.BUTTER, p.food(food(3, 0.6F))));
	public static final Item CHEDDAR_WHEEL = item("cheddar_wheel", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.CHEDDAR_WHEEL, p.stacksTo(16)));
	public static final Item CHEDDAR = item("cheddar", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.CHEDDAR, p.food(food(3, 0.7F))));

	// Spoiled food (docs/food.md): eating it gives Hunger and Nausea, like spoiled milk armor; food values proposed.
	private static net.minecraft.world.item.component.Consumable sickening(boolean drink) {
		return (drink ? net.minecraft.world.item.component.Consumables.defaultDrink() : net.minecraft.world.item.component.Consumables.defaultFood())
				.onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(java.util.List.of(
						new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 600, 0),
						new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NAUSEA, 300, 0)))).build();
	}

	public static final Item SPOILED_BOTTLE = item("spoiled_bottle", p -> new Item(p.stacksTo(16).usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE)
			.food(food(1, 0.0F), sickening(true))));
	public static final Item RANCID_BUTTER = item("rancid_butter", p -> new Item(p.food(food(1, 0.0F), sickening(false))));
	public static final Item MOLDY_CHEESE = item("moldy_cheese", p -> new Item(p.food(food(1, 0.0F), sickening(false))));
	public static final Item SPOILED_STEAK = item("spoiled_steak", p -> new Item(p.food(food(1, 0.0F), sickening(false))));

	// Sheep (docs/animals/sheep.md). Lamb chops from sheep under a year, mutton chops from older ones; food values are vanilla mutton's.
	public static final Item FLEECE = item("fleece", p -> new dev.farmings.item.FleeceItem(p.stacksTo(16)));
	/** Belly and edge wool taken off a fleece at the wool table: one grade lower (docs/multiblocks/shearing-shed.md). */
	public static final Item SKIRTINGS = item("skirtings", p -> new dev.farmings.item.FleeceItem(p.stacksTo(16)));
	/** Pressed fleeces of one grade and colour. */
	public static final Item WOOL_BALE = item("wool_bale", p -> new dev.farmings.item.FleeceItem(p.stacksTo(1)));
	/** The little wool a hair sheep sheds each year (0.5-1 kg, sheep.md): the lowest grade *(proposed)*. */
	public static final Item LOOSE_WOOL = item("loose_wool", Item::new);
	public static final Item LAMB_CHOP = item("lamb_chop", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.RAW_LAMB, p.food(net.minecraft.world.food.Foods.MUTTON)));
	public static final Item COOKED_LAMB_CHOP = item("cooked_lamb_chop", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.COOKED_LAMB, p.food(net.minecraft.world.food.Foods.COOKED_MUTTON)));
	public static final Item MUTTON_CHOP = item("mutton_chop", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.RAW_LAMB, p.food(net.minecraft.world.food.Foods.MUTTON)));
	public static final Item COOKED_MUTTON_CHOP = item("cooked_mutton_chop", p -> new dev.farmings.item.PerishableItem(dev.farmings.process.food.Spoilage.Food.COOKED_LAMB, p.food(net.minecraft.world.food.Foods.COOKED_MUTTON)));
	public static final Item SPOILED_MUTTON = item("spoiled_mutton", p -> new Item(p.food(food(1, 0.0F), sickening(false))));

	public static Item sheepMeat(boolean lamb, boolean cooked) {
		return lamb ? (cooked ? COOKED_LAMB_CHOP : LAMB_CHOP) : (cooked ? COOKED_MUTTON_CHOP : MUTTON_CHOP);
	}

	/** A fleece shorn with hand shears (a grade lower than the shearing stand, proposed). */
	public static net.minecraft.world.item.ItemStack handShorn(dev.farmings.entity.SheepEntity.Fleece f, long now) {
		return dev.farmings.item.FleeceItem.of(f, true);
	}

	/** What a hair sheep drops when it sheds. */
	public static net.minecraft.world.item.ItemStack shedWool() {
		return new net.minecraft.world.item.ItemStack(LOOSE_WOOL);
	}

	public static Item bottle(dev.farmings.process.creamery.Creamery.Bottle b) {
		return switch (b) {
			case MILK -> BOTTLED_MILK;
			case CREAM -> CREAM_BOTTLE;
			case SKIM -> SKIM_MILK_BOTTLE;
			case BUTTERMILK -> BUTTERMILK_BOTTLE;
			case WHEY -> WHEY_BOTTLE;
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
		ALL.put("herd_book", HERD_BOOK);
		ALL.put("milk_helmet", MILK_HELMET);
		ALL.put("milk_chestplate", MILK_CHESTPLATE);
		ALL.put("milk_leggings", MILK_LEGGINGS);
		ALL.put("milk_boots", MILK_BOOTS);
		ALL.put("bottled_milk", BOTTLED_MILK);
		ALL.put("cream_bottle", CREAM_BOTTLE);
		ALL.put("skim_milk_bottle", SKIM_MILK_BOTTLE);
		ALL.put("buttermilk_bottle", BUTTERMILK_BOTTLE);
		ALL.put("whey_bottle", WHEY_BOTTLE);
		ALL.put("spoiled_bottle", SPOILED_BOTTLE);
		ALL.put("rancid_butter", RANCID_BUTTER);
		ALL.put("moldy_cheese", MOLDY_CHEESE);
		ALL.put("spoiled_steak", SPOILED_STEAK);
		ALL.put("butter", BUTTER);
		ALL.put("cheddar_wheel", CHEDDAR_WHEEL);
		ALL.put("cheddar", CHEDDAR);
		ALL.put("fleece", FLEECE);
		ALL.put("loose_wool", LOOSE_WOOL);
		ALL.put("skirtings", SKIRTINGS);
		ALL.put("wool_bale", WOOL_BALE);
		ALL.put("lamb_chop", LAMB_CHOP);
		ALL.put("cooked_lamb_chop", COOKED_LAMB_CHOP);
		ALL.put("mutton_chop", MUTTON_CHOP);
		ALL.put("cooked_mutton_chop", COOKED_MUTTON_CHOP);
		ALL.put("spoiled_mutton", SPOILED_MUTTON);
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

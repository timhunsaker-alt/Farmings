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

	private FarmingsItems() {
	}

	private static Item item(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Farmings.id(name));
		return factory.apply(new Item.Properties().setId(key));
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, Farmings.id("manure"), MANURE);
		Registry.register(BuiltInRegistries.ITEM, Farmings.id("manure_fork"), MANURE_FORK);
	}
}

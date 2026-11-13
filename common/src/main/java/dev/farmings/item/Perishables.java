package dev.farmings.item;

import dev.farmings.FarmingsItems;
import dev.farmings.process.food.Spoilage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

/**
 * The spoil clock on a food stack (docs/food.md): the tick it spoils at room temperature, in its custom data. Plain helpers for the
 * creamery, the cattle's drops, the inventory and the walk-in fridge.
 */
public final class Perishables {
	private static final String KEY = "SpoilsAt";

	private Perishables() {
	}

	public static Spoilage.@Nullable Food foodOf(ItemStack stack) {
		return stack.getItem() instanceof PerishableItem p ? p.food() : null;
	}

	/** The spoil tick, or {@link Long#MIN_VALUE} if its clock has not started. */
	public static long spoilsAt(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d == null ? Long.MIN_VALUE : d.copyTag().getLongOr(KEY, Long.MIN_VALUE);
	}

	public static void setSpoilsAt(ItemStack stack, long tick) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong(KEY, tick));
	}

	/** Starts the clock if it has not started: the food has just left the cold (or was made in the open). Returns the stack. */
	public static ItemStack stamp(ItemStack stack, long now) {
		Spoilage.Food f = foodOf(stack);
		if (f != null && spoilsAt(stack) == Long.MIN_VALUE) {
			setSpoilsAt(stack, Spoilage.spoilsAt(f, now));
		}
		return stack;
	}

	/** A fridge kept it cold for {@code ticks}: its spoil tick moves later (Spoilage.coldCredit). Starts the clock first if need be. */
	public static void keepCold(ItemStack stack, long now, long ticks) {
		keepCold(stack, now, ticks, dev.farmings.process.coldroom.ColdRoomTemp.FRIDGE_C);
	}

	/** Kept at {@code tempC} for {@code ticks}: the credit follows the temperature (Spoilage.coldCredit). */
	public static void keepCold(ItemStack stack, long now, long ticks, double tempC) {
		Spoilage.Food f = foodOf(stack);
		if (f == null) {
			return;
		}
		stamp(stack, now);
		setSpoilsAt(stack, spoilsAt(stack) + Spoilage.coldCredit(f, ticks, tempC));
	}

	/** The spoiled stack this one has become by {@code now}, the same count, or null if it is still good. */
	public static @Nullable ItemStack spoiledVersion(ItemStack stack, long now) {
		Spoilage.Food f = foodOf(stack);
		long at = spoilsAt(stack);
		if (f == null || at == Long.MIN_VALUE || !Spoilage.spoiled(at, now)) {
			return null;
		}
		return new ItemStack(spoiledItem(f.spoiled), stack.getCount());
	}

	public static Item spoiledItem(Spoilage.Spoiled s) {
		return switch (s) {
			case BOTTLE -> FarmingsItems.SPOILED_BOTTLE;
			case BUTTER -> FarmingsItems.RANCID_BUTTER;
			case CHEESE -> FarmingsItems.MOLDY_CHEESE;
			case STEAK -> FarmingsItems.SPOILED_STEAK;
			case MUTTON -> FarmingsItems.SPOILED_MUTTON;
			case EGG -> FarmingsItems.ROTTEN_EGG;
			case POULTRY -> FarmingsItems.SPOILED_POULTRY;
			case PORK -> FarmingsItems.SPOILED_PORK;
		};
	}
}

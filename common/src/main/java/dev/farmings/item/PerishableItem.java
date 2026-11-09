package dev.farmings.item;

import dev.farmings.Farmings;
import dev.farmings.process.food.Spoilage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Food that spoils (docs/food.md). Its clock starts when it first exists out of the cold; carried, it turns into its spoiled form when the
 * clock runs out; in a powered walk-in fridge the clock is held back. The bar under the icon shows how much room-temperature life is left.
 */
public class PerishableItem extends Item {
	private final Spoilage.Food food;

	public PerishableItem(Spoilage.Food food, Properties properties) {
		super(properties);
		this.food = food;
	}

	public Spoilage.Food food() {
		return food;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		long now = level.getGameTime();
		Perishables.stamp(stack, now);
		ItemStack spoiled = Perishables.spoiledVersion(stack, now);
		if (spoiled != null && entity instanceof Player player) {
			var inv = player.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				if (inv.getItem(i) == stack) {
					inv.setItem(i, spoiled);
					return;
				}
			}
		}
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return Perishables.spoilsAt(stack) != Long.MIN_VALUE && Farmings.clientTime != null;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return (int) Math.round(13 * left(stack));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		float h = (float) (left(stack) / 3.0);      // green when fresh, through yellow, to red
		return net.minecraft.util.Mth.hsvToRgb(h, 1.0F, 1.0F);
	}

	private double left(ItemStack stack) {
		long now = Farmings.clientTime == null ? 0 : Farmings.clientTime.getAsLong();
		return Spoilage.left(food, Perishables.spoilsAt(stack), now);
	}
}

package dev.farmings.item;

import dev.farmings.entity.FowlEggs;
import dev.farmings.genetics.PoultryLook;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * An egg of one species (docs/eggs.md): weight, shell colour and whether it is fertile, shown in the tooltip. Table eggs spoil (35 days
 * cold, 7 out); a fertile egg does not rot in a pocket, it only stops being hatchable after 7 days unwarmed (poultry.md).
 */
public class FowlEggItem extends PerishableItem {
	public FowlEggItem(Properties props) {
		super(dev.farmings.process.food.Spoilage.Food.EGG, props);
	}

	@Override
	public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity entity,
			net.minecraft.world.entity.@org.jspecify.annotations.Nullable EquipmentSlot slot) {
		if (!FowlEggs.fertile(stack)) {
			super.inventoryTick(stack, level, entity, slot);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		double g = FowlEggs.grams(stack);
		if (g <= 0) {
			return;
		}
		out.accept(Component.literal(String.format(Locale.ROOT, "%.1f g, %s shell%s%s", g, PoultryLook.shellColour(FowlEggs.shade(stack)),
				FowlEggs.fertile(stack) ? ", fertile" : "", FowlEggs.floor(stack) ? ", floor egg" : "")));
		double warm = FowlEggs.warmDays(stack);
		if (warm > 0) {
			out.accept(Component.literal(String.format(Locale.ROOT, "incubated %.1f days", warm)));
		}
	}
}

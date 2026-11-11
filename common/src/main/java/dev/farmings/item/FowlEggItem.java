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

/** An egg of one species (docs/eggs.md): weight, shell colour and whether it is fertile, shown in the tooltip. */
public class FowlEggItem extends Item {
	public FowlEggItem(Properties props) {
		super(props);
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

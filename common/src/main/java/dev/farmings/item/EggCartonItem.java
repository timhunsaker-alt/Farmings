package dev.farmings.item;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A carton of 12 eggs from the grading table (docs/eggs.md): its label (size and grade for hen eggs, the species for the others) and the
 * eggs themselves, which keep their own clocks. Use it to open it.
 */
public class EggCartonItem extends Item {
	public static final String LABEL = "Label";

	public EggCartonItem(Properties props) {
		super(props);
	}

	public static ItemStack of(List<ItemStack> eggs, String label) {
		ItemStack c = new ItemStack(dev.farmings.FarmingsItems.EGG_CARTON);
		c.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(eggs));
		CustomData.update(DataComponents.CUSTOM_DATA, c, t -> t.putString(LABEL, label));
		return c;
	}

	public static String label(ItemStack c) {
		CustomData d = c.get(DataComponents.CUSTOM_DATA);
		return d == null ? "" : d.copyTag().getStringOr(LABEL, "");
	}

	public static List<ItemStack> eggs(ItemStack c) {
		ItemContainerContents cont = c.get(DataComponents.CONTAINER);
		return cont == null ? List.of() : cont.nonEmptyItemCopyStream().toList();
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack carton = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			for (ItemStack e : eggs(carton)) {
				if (!player.addItem(e)) {
					net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), e);
				}
			}
			carton.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		String l = label(stack);
		if (!l.isEmpty()) {
			out.accept(Component.literal(String.format(Locale.ROOT, "%s, %d eggs", l, eggs(stack).size())));
		}
	}
}

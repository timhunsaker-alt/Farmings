package dev.farmings.item;

import dev.farmings.FarmingsItems;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.SheepLook;
import dev.farmings.husbandry.Flock;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * A shorn fleece (docs/animals/sheep.md, "Wool"): its greasy weight, micron, staple, clean yield, grade and natural colour, kept in its custom
 * data. Fleeces from hand shears are a grade lower than from the shearing stand *(proposed)*.
 */
public class FleeceItem extends Item {
	private static final String KG = "GreasyKg";
	private static final String MICRON = "Micron";
	private static final String STAPLE = "StapleCm";
	private static final String YIELD = "CleanYield";
	private static final String GRADE = "Grade";
	private static final String COLOUR = "Colour";

	public FleeceItem(Properties props) {
		super(props);
	}

	/** One fleece item for a shorn fleece; {@code byHand} lowers the grade one step. */
	public static ItemStack of(SheepEntity.Fleece f, boolean byHand) {
		return make(FarmingsItems.FLEECE, f.greasyKg(), f.micron(), f.stapleCm(), f.cleanYield(), byHand ? f.grade().lower() : f.grade(), f.colour());
	}

	/** A wool item (fleece, skirtings, bale, clean wool, sliver) carrying its weight and quality. */
	public static ItemStack make(Item item, double kg, double micron, double stapleCm, double cleanYield, Flock.Grade grade, SheepLook.Wool colour) {
		ItemStack stack = new ItemStack(item);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
			t.putDouble(KG, Math.round(kg * 100) / 100.0);
			t.putDouble(MICRON, Math.round(micron * 10) / 10.0);
			t.putDouble(STAPLE, Math.round(stapleCm * 10) / 10.0);
			t.putDouble(YIELD, cleanYield);
			t.putString(GRADE, grade.name());
			t.putString(COLOUR, colour.name());
		});
		return stack;
	}

	public static double stapleCm(ItemStack stack) {
		return tag(stack).getDoubleOr(STAPLE, 0);
	}

	/** True for any item carrying wool data. */
	public static boolean isWool(ItemStack stack) {
		return stack.getItem() instanceof FleeceItem && grade(stack) != null;
	}

	private static CompoundTag tag(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d == null ? new CompoundTag() : d.copyTag();
	}

	public static double greasyKg(ItemStack stack) {
		return tag(stack).getDoubleOr(KG, 0);
	}

	public static double micron(ItemStack stack) {
		return tag(stack).getDoubleOr(MICRON, 0);
	}

	public static double cleanYield(ItemStack stack) {
		return tag(stack).getDoubleOr(YIELD, 0);
	}

	public static Flock.@Nullable Grade grade(ItemStack stack) {
		String g = tag(stack).getStringOr(GRADE, "");
		try {
			return g.isEmpty() ? null : Flock.Grade.valueOf(g);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static SheepLook.Wool colour(ItemStack stack) {
		try {
			return SheepLook.Wool.valueOf(tag(stack).getStringOr(COLOUR, "WHITE"));
		} catch (IllegalArgumentException e) {
			return SheepLook.Wool.WHITE;
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		Flock.Grade g = grade(stack);
		if (g == null) {
			return;
		}
		out.accept(Component.literal(String.format(Locale.ROOT, "%s, %s", g.label, colour(stack).name().toLowerCase(Locale.ROOT))));
		boolean greasy = stack.is(FarmingsItems.FLEECE) || stack.is(FarmingsItems.SKIRTINGS) || stack.is(FarmingsItems.WOOL_BALE);
		out.accept(Component.literal(String.format(Locale.ROOT, "%.2f kg%s, %.1f micron, %.1f cm staple", greasyKg(stack), greasy ? " greasy" : "",
				micron(stack), tag(stack).getDoubleOr(STAPLE, 0))));
	}
}

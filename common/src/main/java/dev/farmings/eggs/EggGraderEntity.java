package dev.farmings.eggs;

import dev.farmings.entity.FowlEggs;
import dev.farmings.genetics.Species;
import dev.farmings.husbandry.poultry.EggGrading;
import dev.farmings.item.EggCartonItem;
import dev.farmings.process.food.Spoilage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The egg grading table (docs/eggs.md): sorts chicken eggs by USDA size class and by grade (age, floor eggs B), and the other species' eggs
 * by species, and packs each bin into a carton of 12. Eggs go in by hand or by hopper from the top or sides; cartons come out of the bottom.
 * Fertile eggs are not graded: they are hatching eggs.
 */
public class EggGraderEntity extends BlockEntity implements WorldlyContainer {
	private static final int[] IN = {0};
	private static final int[] OUT = {1};
	private final Map<String, List<ItemStack>> bins = new LinkedHashMap<>();
	private final List<ItemStack> cartons = new ArrayList<>();
	private int graded;
	private final Map<String, Integer> counts = new LinkedHashMap<>();

	public EggGraderEntity(BlockPos pos, BlockState state) {
		super(EggBlocks.EGG_GRADER_ENTITY, pos, state);
	}

	/** The bin an egg goes in: "Large, Grade AA" for a hen egg, "Duck eggs" and so on for the others. */
	public static String label(ItemStack egg, long now) {
		Species s = FowlEggs.species(egg);
		if (s != Species.CHICKEN) {
			return s == null ? "" : s.name().charAt(0) + s.name().substring(1).toLowerCase(Locale.ROOT) + " eggs";
		}
		EggGrading.Size size = EggGrading.size(FowlEggs.grams(egg));
		double age = (double) (now - FowlEggs.laid(egg)) / Spoilage.TICKS_PER_DAY;
		EggGrading.Grade grade = EggGrading.grade(age, FowlEggs.floor(egg));
		return size.label + ", Grade " + grade.name();
	}

	public static boolean accepts(ItemStack egg) {
		return FowlEggs.species(egg) != null && !FowlEggs.fertile(egg);
	}

	/** Grades one egg; a full bin becomes a carton. */
	public boolean grade(ItemStack egg) {
		if (!accepts(egg) || level == null) {
			return false;
		}
		String l = label(egg, level.getGameTime());
		List<ItemStack> bin = bins.computeIfAbsent(l, k -> new ArrayList<>());
		bin.add(egg.copyWithCount(1));
		graded++;
		counts.merge(l, 1, Integer::sum);
		if (bin.size() >= EggGrading.CARTON) {
			cartons.add(EggCartonItem.of(new ArrayList<>(bin), l));
			bin.clear();
		}
		setChanged();
		return true;
	}

	public List<ItemStack> cartons() {
		return cartons;
	}

	public List<ItemStack> takeCartons() {
		List<ItemStack> l = new ArrayList<>(cartons);
		cartons.clear();
		setChanged();
		return l;
	}

	public String readout() {
		StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "Egg grader: %d graded, %d carton%s ready", graded, cartons.size(), cartons.size() == 1 ? "" : "s"));
		for (Map.Entry<String, List<ItemStack>> e : bins.entrySet()) {
			if (!e.getValue().isEmpty()) {
				b.append("; ").append(e.getKey()).append(": ").append(e.getValue().size()).append("/12");
			}
		}
		if (graded > 0) {
			b.append("; sizes so far:");
			counts.forEach((k, v) -> b.append(String.format(Locale.ROOT, " %s %.0f%%", k, 100.0 * v / graded)));
		}
		return b.toString();
	}

	// ---- hoppers: eggs in (top, sides), cartons out (bottom) ----

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? OUT : IN;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == 0 && accepts(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == 1 && side == Direction.DOWN;
	}

	@Override
	public int getContainerSize() {
		return 2;
	}

	@Override
	public boolean isEmpty() {
		return cartons.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == 1 && !cartons.isEmpty() ? cartons.get(0) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		if (slot != 1 || cartons.isEmpty()) {
			return ItemStack.EMPTY;
		}
		setChanged();
		return cartons.remove(0);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return removeItem(slot, 1);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == 0) {
			for (int i = 0; i < stack.getCount(); i++) {
				grade(stack);
			}
		} else if (slot == 1 && !stack.isEmpty()) {
			cartons.add(0, stack);     // a hopper that could not take it hands it back
		}
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		cartons.clear();
		bins.clear();
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putInt("Graded", graded);
		var binsOut = out.childrenList("Bins");
		for (Map.Entry<String, List<ItemStack>> e : bins.entrySet()) {
			var o = binsOut.addChild();
			o.putString("Label", e.getKey());
			var items = o.list("Eggs", ItemStack.CODEC);
			e.getValue().forEach(items::add);
		}
		var c = out.list("Cartons", ItemStack.CODEC);
		cartons.forEach(c::add);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		graded = in.getIntOr("Graded", 0);
		bins.clear();
		for (ValueInput o : in.childrenListOrEmpty("Bins")) {
			List<ItemStack> l = new ArrayList<>();
			o.listOrEmpty("Eggs", ItemStack.CODEC).forEach(l::add);
			bins.put(o.getStringOr("Label", ""), l);
		}
		cartons.clear();
		in.listOrEmpty("Cartons", ItemStack.CODEC).forEach(cartons::add);
	}
}

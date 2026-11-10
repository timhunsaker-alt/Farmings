package dev.farmings.woolmill;

import dev.farmings.FarmingsItems;
import dev.farmings.genetics.SheepLook;
import dev.farmings.husbandry.Flock;
import dev.farmings.item.FleeceItem;
import dev.farmings.process.wool.WoolProcess;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * One wool-mill machine on Wildcatter industrial power. Slot 0 is the input, put in from the top or sides (by hopper or by hand); slots
 * 1-4 are the output, taken from below. A batch takes {@link WoolProcess#BATCH_TICKS}. The scouring bowl also holds water, filled by
 * bucket or pipe.
 */
public class WoolMachineEntity extends BlockEntity implements WorldlyContainer, CircuitPart, Inspectable, WaterUser {
	public static final double WATER_CAPACITY_L = 1000;
	private static final int[] IN = {0};
	private static final int[] OUT = {1, 2, 3, 4};

	private final WoolMachine kind;
	private final Motor motor;
	private final NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
	private int progress;
	private double water;
	private double lanolinKg;
	private final Map<SheepLook.Wool, Double> yarnLeft = new EnumMap<>(SheepLook.Wool.class);
	private double feltLeft;
	private boolean forced;
	private String fault = "";
	private int batches;

	public WoolMachineEntity(BlockPos pos, BlockState state) {
		super(WoolMillBlocks.MACHINE_ENTITY, pos, state);
		this.kind = state.getBlock() instanceof WoolMachineBlock b ? b.kind() : WoolMachine.CARDING_MACHINE;
		this.motor = new Motor(true, Voltage.INDUSTRIAL, kind.watts);
	}

	public WoolMachine kind() {
		return kind;
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public double water() {
		return water;
	}

	public int batches() {
		return batches;
	}

	public NonNullList<ItemStack> items() {
		return items;
	}

	/** True for an item this machine takes. */
	public boolean accepts(ItemStack s) {
		return switch (kind) {
			case SCOURING_BOWL -> s.is(FarmingsItems.FLEECE) || s.is(FarmingsItems.SKIRTINGS) || s.is(FarmingsItems.WOOL_BALE) || s.is(FarmingsItems.LOOSE_WOOL);
			case CARDING_MACHINE, FELTING_TABLE -> s.is(FarmingsItems.CLEAN_WOOL);
			case SPINNING_FRAME -> s.is(FarmingsItems.SLIVER);
			case LOOM -> FarmingsItems.YARN.containsValue(s.getItem());
		};
	}

	private boolean ready(ItemStack in) {
		if (in.isEmpty() || !accepts(in)) {
			return false;
		}
		if (kind == WoolMachine.LOOM && in.getCount() < WoolProcess.YARN_PER_CLOTH) {
			return false;
		}
		if (kind == WoolMachine.SCOURING_BOWL) {
			double need = WoolProcess.scour(greasyKg(in), 1).waterL();
			if (water < need) {
				fault = "NO WATER (" + (int) Math.ceil(need) + " L needed)";
				return false;
			}
		}
		return true;
	}

	private static double greasyKg(ItemStack s) {
		return s.is(FarmingsItems.LOOSE_WOOL) ? WoolProcess.LOOSE_WOOL_KG : FleeceItem.greasyKg(s);
	}

	public void serverTick(Level level) {
		ItemStack in = items.get(0);
		if (!powered()) {
			fault = in.isEmpty() ? "" : "NO POWER";
			return;
		}
		fault = "";
		if (!ready(in)) {
			progress = 0;
			return;
		}
		progress++;
		if (progress >= WoolProcess.BATCH_TICKS) {
			progress = 0;
			if (run(in)) {
				batches++;
			}
			setChanged();
		}
	}

	/** One batch; false (and nothing used) when the outputs have no room. */
	private boolean run(ItemStack in) {
		switch (kind) {
			case SCOURING_BOWL -> {
				boolean loose = in.is(FarmingsItems.LOOSE_WOOL);
				double kg = greasyKg(in);
				double yield = loose ? WoolProcess.LOOSE_WOOL_YIELD : FleeceItem.cleanYield(in);
				WoolProcess.Scoured s = WoolProcess.scour(kg, yield);
				Flock.Grade g = loose ? Flock.Grade.HAIR : FleeceItem.grade(in);
				SheepLook.Wool c = loose ? SheepLook.Wool.WHITE : FleeceItem.colour(in);
				ItemStack clean = FleeceItem.make(FarmingsItems.CLEAN_WOOL, s.cleanKg(), loose ? 0 : FleeceItem.micron(in), loose ? 0 : FleeceItem.stapleCm(in), 1.0, g, c);
				if (!fits(clean)) {
					return false;
				}
				water -= s.waterL();
				lanolinKg += s.lanolinKg();
				put(clean);
				int lan = (int) Math.floor(lanolinKg / WoolProcess.LANOLIN_ITEM_KG + 1e-9);
				if (lan > 0 && fits(new ItemStack(FarmingsItems.LANOLIN, lan))) {
					put(new ItemStack(FarmingsItems.LANOLIN, lan));
					lanolinKg -= lan * WoolProcess.LANOLIN_ITEM_KG;
				}
				in.shrink(1);
			}
			case CARDING_MACHINE -> {
				ItemStack sliver = FleeceItem.make(FarmingsItems.SLIVER, WoolProcess.card(FleeceItem.greasyKg(in)), FleeceItem.micron(in), FleeceItem.stapleCm(in), 1.0,
						FleeceItem.grade(in), FleeceItem.colour(in));
				if (!fits(sliver)) {
					return false;
				}
				put(sliver);
				in.shrink(1);
			}
			case SPINNING_FRAME -> {
				SheepLook.Wool c = FleeceItem.colour(in);
				Flock.Grade g = FleeceItem.grade(in);
				double total = yarnLeft.getOrDefault(c, 0.0) + WoolProcess.yarnItems(FleeceItem.greasyKg(in), g == null ? Flock.Grade.COARSE : g);
				int n = (int) Math.floor(total + 1e-9);
				Item yarn = FarmingsItems.naturalYarn(c);
				if (n > 0 && !fits(new ItemStack(yarn, n))) {
					return false;
				}
				if (n > 0) {
					put(new ItemStack(yarn, n));
				}
				yarnLeft.put(c, total - n);
				in.shrink(1);
			}
			case LOOM -> {
				ItemStack cloth = new ItemStack(FarmingsItems.WOOL_CLOTH);
				if (!fits(cloth)) {
					return false;
				}
				put(cloth);
				in.shrink(WoolProcess.YARN_PER_CLOTH);
			}
			case FELTING_TABLE -> {
				double total = feltLeft + FleeceItem.greasyKg(in) / WoolProcess.FELT_ITEM_KG;
				int n = (int) Math.floor(total + 1e-9);
				if (n > 0 && !fits(new ItemStack(FarmingsItems.FELT, n))) {
					return false;
				}
				if (n > 0) {
					put(new ItemStack(FarmingsItems.FELT, n));
				}
				feltLeft = total - n;
				in.shrink(1);
			}
		}
		return true;
	}

	private boolean fits(ItemStack s) {
		int room = 0;
		for (int i : OUT) {
			ItemStack here = items.get(i);
			if (here.isEmpty()) {
				room += s.getMaxStackSize();
			} else if (ItemStack.isSameItemSameComponents(here, s)) {
				room += here.getMaxStackSize() - here.getCount();
			}
		}
		return room >= s.getCount();
	}

	private void put(ItemStack s) {
		for (int i : OUT) {
			ItemStack here = items.get(i);
			if (!here.isEmpty() && ItemStack.isSameItemSameComponents(here, s)) {
				int move = Math.min(s.getCount(), here.getMaxStackSize() - here.getCount());
				here.grow(move);
				s.shrink(move);
			}
		}
		for (int i : OUT) {
			if (s.isEmpty()) {
				return;
			}
			if (items.get(i).isEmpty()) {
				items.set(i, s.copy());
				s.setCount(0);
			}
		}
	}

	/** By hand: puts the held input in; returns false if this machine does not take it. */
	public boolean insertByHand(ItemStack held) {
		if (!accepts(held)) {
			return false;
		}
		ItemStack in = items.get(0);
		if (in.isEmpty()) {
			items.set(0, held.split(held.getMaxStackSize()));
		} else if (ItemStack.isSameItemSameComponents(in, held)) {
			int move = Math.min(held.getCount(), in.getMaxStackSize() - in.getCount());
			in.grow(move);
			held.shrink(move);
		} else {
			return false;
		}
		setChanged();
		return true;
	}

	/** By hand: takes the first output stack. */
	public ItemStack takeOutput() {
		for (int i : OUT) {
			if (!items.get(i).isEmpty()) {
				ItemStack s = items.get(i);
				items.set(i, ItemStack.EMPTY);
				setChanged();
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	public boolean pourBucket() {
		if (kind != WoolMachine.SCOURING_BOWL || WATER_CAPACITY_L - water < 1000) {
			return false;
		}
		water += 1000;
		setChanged();
		return true;
	}

	public String readout() {
		StringBuilder b = new StringBuilder(kind.title).append(": ");
		b.append(fault.isEmpty() ? (powered() ? (items.get(0).isEmpty() ? "idle" : String.format(Locale.ROOT, "working %d%%", progress * 100 / WoolProcess.BATCH_TICKS)) : "NO POWER") : fault);
		if (kind == WoolMachine.SCOURING_BOWL) {
			b.append(String.format(Locale.ROOT, ", water %.0f L", water));
		}
		b.append(", ").append(batches).append(" done");
		return b.toString();
	}

	// ---- water (the scouring bowl) ----

	@Override
	public double waterLevel() {
		return kind == WoolMachine.SCOURING_BOWL ? water / WATER_CAPACITY_L : 1.0;
	}

	@Override
	public int addWater(int litres) {
		if (kind != WoolMachine.SCOURING_BOWL) {
			return 0;
		}
		int fits = (int) Math.max(0, Math.min(litres, Math.floor(WATER_CAPACITY_L - water)));
		water += fits;
		if (fits > 0) {
			setChanged();
		}
		return fits;
	}

	// ---- hoppers ----

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? OUT : IN;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == 0 && side != Direction.DOWN && accepts(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot != 0 && side == Direction.DOWN;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 && accepts(stack);
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack r = ContainerHelper.removeItem(items, slot, count);
		setChanged();
		return r;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			net.minecraft.world.Containers.dropContents(level, pos, items);
		}
		super.preRemoveSideEffects(pos, state);
	}

	// ---- Wildcatter ----

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && role != Conductor.N && role != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, !items.get(0).isEmpty());
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format(Locale.ROOT, "%s (%.2f kW): %s", kind.title, kind.watts / 1000.0, motor.describe()));
	}

	// ---- saving ----

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		ContainerHelper.saveAllItems(out, items);
		out.putInt("Progress", progress);
		out.putDouble("Water", water);
		out.putDouble("LanolinKg", lanolinKg);
		out.putDouble("FeltLeft", feltLeft);
		out.putInt("Batches", batches);
		for (SheepLook.Wool w : SheepLook.Wool.values()) {
			out.putDouble("Yarn" + w.name(), yarnLeft.getOrDefault(w, 0.0));
		}
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		items.clear();
		ContainerHelper.loadAllItems(in, items);
		progress = in.getIntOr("Progress", 0);
		water = in.getDoubleOr("Water", 0);
		lanolinKg = in.getDoubleOr("LanolinKg", 0);
		feltLeft = in.getDoubleOr("FeltLeft", 0);
		batches = in.getIntOr("Batches", 0);
		for (SheepLook.Wool w : SheepLook.Wool.values()) {
			yarnLeft.put(w, in.getDoubleOr("Yarn" + w.name(), 0.0));
		}
	}
}

package dev.farmings.compost;

import dev.farmings.FarmingsItems;
import dev.farmings.conveyor.ItemSink;
import dev.farmings.crops.FieldCrops;
import dev.farmings.farm.compost.Windrow;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.silo.Towers;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The windrow controller (docs/multiblocks/compost.md): at one end of a straight row of compost pad, it takes the pile's inputs (by belt,
 * hopper or hand), reads the blend, starts the pile on the player's word, has the turner turn it on time, and puts finished compost out as
 * 10 kg items. The pile ages on the 15:1 clock (waste.md).
 */
public class CompostControllerEntity extends BlockEntity implements ItemSink, WorldlyContainer {
	public static final double COMPOST_ITEM_KG = 10;
	private static final int[] SLOTS = {0};
	private final Windrow pile = new Windrow();
	private double compostKg;
	private int made;

	public CompostControllerEntity(BlockPos pos, BlockState state) {
		super(CompostBlocks.CONTROLLER_ENTITY, pos, state);
	}

	public Windrow pile() {
		return pile;
	}

	public int made() {
		return made;
	}

	/** The pad: compost pad cells in a straight line from the controller. */
	public int padCells() {
		if (level == null) {
			return 0;
		}
		int best = 0;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			int n = 0;
			BlockPos p = worldPosition.relative(d);
			while (n < 64 && level.getBlockState(p).getBlock() == CompostBlocks.COMPOST_PAD) {
				n++;
				p = p.relative(d);
			}
			best = Math.max(best, n);
		}
		return best;
	}

	private @Nullable TurnerEntity turner() {
		if (level == null) {
			return null;
		}
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos p = worldPosition.relative(d);
			int n = 0;
			while (n < 66) {
				if (level.getBlockEntity(p) instanceof TurnerEntity t) {
					return t;
				}
				if (level.getBlockState(p).getBlock() != CompostBlocks.COMPOST_PAD) {
					break;
				}
				p = p.relative(d);
				n++;
			}
		}
		return null;
	}

	/** An input's wet kg, C:N and moisture (compost.md, "The pile"); null if it is not a compost input. */
	public static double @Nullable [] input(ItemStack s) {
		if (s.is(FarmingsItems.MANURE)) {
			return new double[] {50, 20, 0.87};
		}
		if (s.is(FarmingsItems.POULTRY_LITTER)) {
			return new double[] {10, 10.5, 0.30};
		}
		if (s.is(FieldCrops.STRAW)) {
			return new double[] {10, 80, 0.10};
		}
		if (s.is(FieldCrops.STRAW_BALE)) {
			return new double[] {454, 80, 0.10};
		}
		if (s.is(CompostBlocks.SEPARATED_SOLIDS)) {
			return new double[] {10, 20, 0.725};
		}
		if (s.is(Items.WATER_BUCKET)) {
			return new double[] {1000, 0, 1.0};
		}
		return null;
	}

	@Override
	public ItemStack accept(ItemStack stack) {
		double[] in = input(stack);
		if (in == null || pile.started()) {
			return stack;
		}
		double cap = padCells() * Windrow.KG_PER_CELL;
		int n = (int) Math.min(stack.getCount(), Math.floor((cap - pile.wetKg()) / in[0] + 1e-9));
		if (n <= 0) {
			return stack;
		}
		pile.add(n * in[0], in[1], in[2]);
		setChanged();
		return stack.copyWithCount(stack.getCount() - n);
	}

	public boolean start() {
		boolean ok = padCells() >= 4 && pile.start();
		setChanged();
		return ok;
	}

	public void serverTick(ServerLevel level) {
		if (level.getGameTime() % 20 != 0) {
			return;
		}
		step(level, TimeScale.ticksToRealDays(20));
	}

	/** {@code days} of composting: age, turn when due and the turner has power, and put finished compost out. */
	public void step(ServerLevel level, double days) {
		pile.age(days);
		TurnerEntity t = turner();
		if (pile.turnDue() && t != null && t.powered()) {
			pile.turn();
			t.turned();
		}
		if (pile.done()) {
			compostKg += pile.takeCompost();
		}
		while (compostKg >= COMPOST_ITEM_KG - 1e-9) {
			ItemStack rest = Towers.handOn(level, worldPosition, new ItemStack(CompostBlocks.COMPOST), p -> level.getBlockState(p).getBlock() == CompostBlocks.COMPOST_PAD);
			if (!rest.isEmpty()) {
				break;
			}
			compostKg -= COMPOST_ITEM_KG;
			made++;
		}
		setChanged();
	}

	/** Takes the finished compost out by hand. */
	public int takeCompostItems() {
		int n = (int) Math.floor(compostKg / COMPOST_ITEM_KG + 1e-9);
		compostKg -= n * COMPOST_ITEM_KG;
		made += n;
		setChanged();
		return n;
	}

	public String readout() {
		int cells = padCells();
		StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "Compost windrow: %d pad cells (%.0f kg), pile %.0f kg", cells, cells * Windrow.KG_PER_CELL, pile.wetKg()));
		if (pile.wetKg() > 0) {
			b.append(String.format(Locale.ROOT, ", C:N %.0f, moisture %.0f%%", pile.cn(), pile.moisture() * 100));
		}
		if (pile.started()) {
			b.append(String.format(Locale.ROOT, ", COMPOSTING day %.1f of 56, %d of 5 turns", pile.ageDays(), pile.turns()));
			if (pile.turnDue()) {
				TurnerEntity t = turner();
				b.append(t == null ? ", NEEDS A TURNER" : t.powered() ? ", turning" : ", TURN DUE: turner has NO POWER");
			}
		} else if (pile.wetKg() > 0) {
			String p = pile.problem();
			b.append(p.isEmpty() ? (cells < 4 ? ", the pad needs 4 cells" : ", ready: sneak-right-click to start") : ", " + p);
		}
		if (compostKg >= COMPOST_ITEM_KG) {
			b.append(String.format(Locale.ROOT, ", %.0f kg of compost to take", compostKg));
		}
		return b.append(", ").append(made).append(" made").toString();
	}

	// ---- hoppers ----

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		double[] in = input(stack);
		return in != null && !pile.started() && pile.wetKg() + in[0] <= padCells() * Windrow.KG_PER_CELL + 1e-9;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		accept(stack);
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		double[] v = pile.save();
		for (int i = 0; i < v.length; i++) {
			out.putDouble("Pile" + i, v[i]);
		}
		out.putDouble("CompostKg", compostKg);
		out.putInt("Made", made);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		double[] v = new double[7];
		for (int i = 0; i < v.length; i++) {
			v[i] = in.getDoubleOr("Pile" + i, 0);
		}
		pile.load(v);
		compostKg = in.getDoubleOr("CompostKg", 0);
		made = in.getIntOr("Made", 0);
	}
}

package dev.farmings.curing;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The smokehouse (curing-room.md): logs, charcoal or coal; one burns 2 game hours of smoking *(proposed)*, only while a rack beside it smokes. */
public class SmokehouseEntity extends BlockEntity implements CuringMachine {
	public static final long BURN_TICKS = 2000;
	public static final int MAX_FUEL = 64;
	private int fuel;
	private long burnLeft;

	public SmokehouseEntity(BlockPos pos, BlockState state) {
		super(CuringBlocks.SMOKEHOUSE_ENTITY, pos, state);
	}

	public int fuel() {
		return fuel;
	}

	/** Smokes for {@code ticks}: burns fuel as it goes; false when there is none. */
	public boolean smoke(long ticks) {
		if (burnLeft <= 0) {
			if (fuel <= 0) {
				return false;
			}
			fuel--;
			burnLeft = BURN_TICKS;
		}
		burnLeft -= ticks;
		setChanged();
		return true;
	}

	@Override
	public boolean insert(ItemStack stack, ServerLevel level) {
		if (!(stack.is(ItemTags.LOGS) || stack.is(Items.CHARCOAL) || stack.is(Items.COAL)) || fuel >= MAX_FUEL) {
			return false;
		}
		int n = Math.min(stack.getCount(), MAX_FUEL - fuel);
		fuel += n;
		stack.shrink(n);
		setChanged();
		return true;
	}

	@Override
	public ItemStack take(ServerLevel level) {
		return ItemStack.EMPTY;
	}

	@Override
	public void serverTick(ServerLevel level) {
	}

	@Override
	public String readout() {
		return String.format(Locale.ROOT, "Smokehouse: %d fuel%s", fuel, burnLeft > 0 ? ", smoking" : "");
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putInt("Fuel", fuel);
		out.putLong("Burn", burnLeft);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		fuel = in.getIntOr("Fuel", 0);
		burnLeft = in.getLongOr("Burn", 0);
	}
}

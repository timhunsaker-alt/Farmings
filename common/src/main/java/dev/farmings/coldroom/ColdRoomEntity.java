package dev.farmings.coldroom;

import dev.farmings.item.Perishables;
import dev.farmings.process.coldroom.ColdRoomCheck;
import dev.farmings.process.coldroom.ColdRoomLayout;
import dev.farmings.process.coldroom.ColdRoomLoad;
import dev.farmings.process.food.Spoilage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The walk-in fridge's controller, in the evaporator (docs/multiblocks/cold-room.md): checks the room every few seconds, sizes the
 * condensing unit for it, and once a minute, while the unit has power, holds back the spoil clock of every food stack in every container
 * inside (chests, barrels, anything that is a container), by the time the cold saved (docs/food.md).
 */
public class ColdRoomEntity extends BlockEntity {
	public static final int CHECK_TICKS = 100;
	private ColdRoomCheck check;
	private long lastCold = Long.MIN_VALUE;
	private String fault = "";
	/** The room's air, °C (cold-room.md, "the room's temperature"); a new room starts at the outside 20 °C. */
	private double tempC = dev.farmings.process.coldroom.ColdRoomTemp.OUTSIDE_C;
	/** A game tick is 3.6 real seconds for food: 1 real day = 1 game day = 24,000 ticks. */
	public static final double SECONDS_PER_TICK = 86_400.0 / 24_000.0;

	public ColdRoomEntity(BlockPos pos, BlockState state) {
		super(ColdRoomBlocks.COLD_ROOM_ENTITY, pos, state);
	}

	public void refresh(Level level) {
		Direction f = getBlockState().getValue(EvaporatorBlock.FACING);
		check = ColdRoomLayout.check(ColdRoomWorld.grid(level), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), f.getStepX(), f.getStepZ());
	}

	public ColdRoomCheck check() {
		return check;
	}

	public boolean formed() {
		return check != null && check.formed();
	}

	public CondenserEntity condenser(Level level) {
		if (!formed()) {
			return null;
		}
		int[] c = check.condenser();
		return level.getBlockEntity(new BlockPos(c[0], c[1], c[2])) instanceof CondenserEntity u ? u : null;
	}

	public void serverTick(Level level) {
		long now = level.getGameTime();
		long t = now + worldPosition.hashCode();
		if (check == null || Math.floorMod(t, (long) CHECK_TICKS) == 0) {
			refresh(level);
		}
		if (Math.floorMod(now, Spoilage.STEP) == 0) {
			cool(level, now, Spoilage.STEP);
		}
	}

	public double tempC() {
		return tempC;
	}

	/** Game tests: set the room's temperature. */
	public void setTempForTest(double c) {
		this.tempC = c;
		setChanged();
	}

	/** A door of the room was opened: one room of outside air comes in. */
	public void doorOpened() {
		if (formed()) {
			int[] s = check.size();
			tempC = dev.farmings.process.coldroom.ColdRoomTemp.doorOpened(tempC, s[0], s[1], s[2]);
			setChanged();
		}
	}

	/**
	 * One step of {@code ticks}: the room's temperature moves (warming through the walls, cooled while the unit has power), and every food
	 * stack inside ages by that temperature. Run once a minute; tests call it.
	 */
	public void cool(Level level, long now, long ticks) {
		CondenserEntity unit = condenser(level);
		if (!formed() || unit == null) {
			fault = formed() ? "NO CONDENSING UNIT" : "";
			return;
		}
		unit.size(ColdRoomLoad.electricWatts(check.volumeM3()));
		unit.setDemand(true);
		boolean powered = unit.powered();
		int[] s = check.size();
		double before = tempC;
		tempC = dev.farmings.process.coldroom.ColdRoomTemp.step(tempC, ticks * SECONDS_PER_TICK, powered,
				ColdRoomLoad.coolingWatts(check.volumeM3()), s[0], s[1], s[2]);
		double mean = (before + tempC) / 2;
		fault = powered ? "" : "NO POWER";
		setChanged();
		if (mean >= dev.farmings.process.coldroom.ColdRoomTemp.OUTSIDE_C - 1e-6) {
			return;     // no cooler than the outside: no credit
		}
		for (int[] c : check.inside()) {
			if (level.getBlockEntity(new BlockPos(c[0], c[1], c[2])) instanceof Container box) {
				boolean changed = false;
				for (int i = 0; i < box.getContainerSize(); i++) {
					var stack = box.getItem(i);
					if (Perishables.foodOf(stack) != null) {
						Perishables.keepCold(stack, now, ticks, mean);
						changed = true;
					}
				}
				if (changed) {
					box.setChanged();
				}
			}
		}
		lastCold = now;
		setChanged();
	}

	public String fault() {
		return fault;
	}

	public String readout(Level level) {
		if (check == null) {
			return "Cold room: not checked yet";
		}
		if (!check.formed()) {
			return "Cold room: " + check.status();
		}
		double kw = ColdRoomLoad.coolingWatts(check.volumeM3()) / 1000;
		CondenserEntity unit = condenser(level);
		String state = unit == null ? "no condensing unit"
				: tempC <= dev.farmings.process.coldroom.ColdRoomTemp.FRIDGE_C + 1e-6 && unit.powered() ? "COLD" : unit.powered() ? "COOLING" : "NO POWER, WARMING";
		return String.format(java.util.Locale.ROOT, "Cold room: %s, load %.1f kW cooling, condensing unit %.1f kW, %.1f °C (set %.0f °C), %s", check.status(), kw,
				ColdRoomLoad.electricWatts(check.volumeM3()) / 1000, tempC, dev.farmings.process.coldroom.ColdRoomTemp.SET_POINT_C, state);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("LastCold", lastCold);
		output.putDouble("TempC", tempC);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lastCold = input.getLongOr("LastCold", Long.MIN_VALUE);
		tempC = input.getDoubleOr("TempC", dev.farmings.process.coldroom.ColdRoomTemp.OUTSIDE_C);
	}
}

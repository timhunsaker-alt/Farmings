package dev.farmings.coldroom;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The condensing unit's compressor motor. Its size follows the room it serves (ColdRoomLoad.electricWatts): when the room is rebuilt to
 * another size, the motor is replaced and the grid solved again.
 */
public class CondenserEntity extends BlockEntity implements CircuitPart, Inspectable {
	/** Before it serves a room: the smallest room's draw. */
	private static final double IDLE_WATTS = 750;
	private Motor motor = new Motor(true, Voltage.INDUSTRIAL, IDLE_WATTS);
	private double ratedWatts = IDLE_WATTS;
	private boolean demand;
	private boolean forced;

	public CondenserEntity(BlockPos pos, BlockState state) {
		super(ColdRoomBlocks.CONDENSER_ENTITY, pos, state);
	}

	/** Sizes the compressor for the room; a change of more than 5% replaces the motor. */
	public void size(double watts) {
		if (Math.abs(watts - ratedWatts) > 0.05 * ratedWatts) {
			ratedWatts = watts;
			motor = new Motor(true, Voltage.INDUSTRIAL, Math.max(1, watts));
			if (level != null) {
				CircuitManager.markDirty(level);
			}
			setChanged();
		}
	}

	public double ratedWatts() {
		return ratedWatts;
	}

	public void setDemand(boolean demand) {
		this.demand = demand;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	/** For tests: run as if fed, without building a grid. */
	public void forceRunning(boolean on) {
		forced = on;
	}

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
		motor.apply(circuit, ticks, demand);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format("Condensing unit (%.2f kW): %s", ratedWatts / 1000.0, motor.describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("RatedWatts", ratedWatts);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ratedWatts = input.getDoubleOr("RatedWatts", IDLE_WATTS);
		motor = new Motor(true, Voltage.INDUSTRIAL, Math.max(1, ratedWatts));
	}
}

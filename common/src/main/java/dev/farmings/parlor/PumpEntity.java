package dev.farmings.parlor;

import dev.farmings.FarmingsBlocks;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The vacuum pump's motor: 4 kW, three-phase, on Wildcatter's industrial voltage (approved by Tim 2026-11-08; a dealer lists 3 and 4 kW
 * pump sets). Milking only happens while it runs.
 */
public class PumpEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	public static final int WATTS = 4_000;

	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, WATTS);
	private boolean demand;
	private boolean forced;
	private boolean grown;

	public PumpEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.PUMP_ENTITY, pos, state);
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public void setDemand(boolean demand) {
		this.demand = demand;
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
		return Component.literal("Vacuum pump (4 kW): " + motor.describe());
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
	}
}

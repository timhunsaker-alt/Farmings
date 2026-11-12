package dev.farmings.barn;

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
 * The scraper drive's motor: 2.2 kW, three-phase, on Wildcatter's industrial voltage (480 V in North America, 400 V under the IEC
 * standard, read from the world), wired like Tailings' dewatering pump. The wattage is a proposal with no source (docs/multiblocks/barn.md).
 * The scraper only moves while this motor is running.
 */
public class DriveEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	public static final int WATTS = 750;     // 0.75 kW (Prinzing scraper drive; sourcing pass 2026-11-12; Tim decided 2.2 kW on 2026-11-08 before a source)

	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, WATTS);
	private boolean demand;
	private boolean forced;
	private boolean grown;

	public DriveEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.DRIVE_ENTITY, pos, state);
	}

	/** Whether the motor is turning (or a test has forced it on). */
	public boolean powered() {
		return forced || motor.output() > 0;
	}

	/** The controller asks the motor to run while a pass is under way. */
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
		return Component.literal("Scraper drive (2.2 kW): " + motor.describe());
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

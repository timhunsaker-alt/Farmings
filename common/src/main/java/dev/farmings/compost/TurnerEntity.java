package dev.farmings.compost;

import dev.wildcatter.circuit.Circuit;
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

/** The windrow turner (compost.md): 41 kW on Wildcatter 480 V (the middle of 50-60 hp three-point turners; Australian Eggs fact sheet). */
public class TurnerEntity extends BlockEntity implements CircuitPart, Inspectable {
	public static final double WATTS = 41_000;
	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, WATTS);
	private boolean forced;
	private int turns;

	public TurnerEntity(BlockPos pos, BlockState state) {
		super(CompostBlocks.TURNER_ENTITY, pos, state);
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	void turned() {
		turns++;
		setChanged();
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, true);
	}

	@Override
	public Component inspect() {
		return Component.literal("Windrow turner (41 kW): " + turns + " turns; " + motor.describe());
	}
}

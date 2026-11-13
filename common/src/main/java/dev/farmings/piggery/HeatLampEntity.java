package dev.farmings.piggery;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A creep heat lamp: "250 watt heat lamps are placed over the creep areas" (TO VERIFY, Iowa State; pig-barn.md). */
public class HeatLampEntity extends BlockEntity implements CircuitPart, Inspectable {
	public static final double WATTS = 250;
	private final Motor lamp = new Motor(false, Voltage.LOW, WATTS);
	private boolean forced;

	public HeatLampEntity(BlockPos pos, BlockState state) {
		super(PigBarnBlocks.HEAT_LAMP_ENTITY, pos, state);
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || lamp.output() > 0;
	}

	public void serverTick(ServerLevel level) {
	}

	public String readout() {
		return String.format(Locale.ROOT, "Heat lamp (%.0f W): %s", WATTS, powered() ? "on" : "NO POWER");
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.LOW;
	}

	@Override
	public void build(Circuit circuit) {
		lamp.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		lamp.apply(circuit, ticks, true);
	}

	@Override
	public Component inspect() {
		return Component.literal(readout() + "; " + lamp.describe());
	}
}

package dev.farmings.silo;

import dev.farmings.conveyor.ItemSink;
import dev.farmings.farm.silo.SiloLayout;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The forage blower at the silo's foot (silo.md): takes chopped forage from a belt or hopper and blows it up into the silo. 15 kW
 * (proposed, inside Purdue's matching range; 1.3 kWh per tonne of corn silage).
 */
public class BlowerEntity extends BlockEntity implements ItemSink, CircuitPart, Inspectable, net.minecraft.world.WorldlyContainer {
	public static final double WATTS = 15_000;
	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, WATTS);
	private boolean forced;

	public BlowerEntity(BlockPos pos, BlockState state) {
		super(SiloBlocks.BLOWER_ENTITY, pos, state);
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	private SiloEntity silo() {
		if (level == null) {
			return null;
		}
		SiloLayout.Tower t = Towers.fromBase(level, worldPosition, SiloBlocks.SILO_STAVE, SiloBlocks.SILO_ROOF, Towers.SILO_MAX);
		if (t == null || !t.formed()) {
			return null;
		}
		BlockPos u = Towers.partBeside(level, t, SiloBlocks.SILO_UNLOADER);
		return u != null && level.getBlockEntity(u) instanceof SiloEntity s ? s : null;
	}

	@Override
	public ItemStack accept(ItemStack stack) {
		SiloEntity s = silo();
		if (s == null || !powered()) {
			return stack;
		}
		return s.fill(stack);
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
		motor.apply(circuit, ticks, silo() != null);
	}

	@Override
	public Component inspect() {
		SiloEntity s = silo();
		return Component.literal("Forage blower (15 kW): " + (s == null ? "no silo with an unloader beside it" : s.readout()) + "; " + motor.describe());
	}

	// a hopper puts chopped forage in
	private static final int[] SLOTS = {0};

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return accept(stack.copyWithCount(1)).isEmpty();
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
	public boolean stillValid(net.minecraft.world.entity.player.Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}
}

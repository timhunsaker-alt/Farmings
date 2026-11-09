package dev.farmings.creamery;

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

/** A powered creamery part (pasteurizer heater, separator, churn, vat agitator): one three-phase load on Wildcatter's industrial voltage. */
public class CreameryMachineEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity, net.minecraft.world.WorldlyContainer {
	private final CreameryParts kind;
	private final Motor motor;
	private boolean demand;
	private boolean forced;
	private final CreameryOutlet outlet;
	private boolean grown;

	public CreameryMachineEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.CREAMERY_MACHINE_ENTITY, pos, state);
		this.kind = state.getBlock() instanceof CreameryPartBlock b ? b.kind() : CreameryParts.CREAM_SEPARATOR;
		this.motor = new Motor(true, Voltage.INDUSTRIAL, Math.max(1, kind.watts));
		this.outlet = new CreameryOutlet(kind);
	}

	public CreameryParts kind() {
		return kind;
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
		return Component.literal(String.format("%s (%.2f kW): %s", kind.id.replace('_', ' '), kind.watts / 1000.0, motor.describe()));
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
		output.putInt("GlassBottles", outlet.glassBottles());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		outlet.setGlassBottles(input.getIntOr("GlassBottles", 0));
	}

	// ---- a hopper's view (CreameryOutlet) ----

	@Override
	public int[] getSlotsForFace(net.minecraft.core.Direction side) {
		return outlet.slots(side);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.@org.jspecify.annotations.Nullable Direction side) {
		return outlet.canPlace(slot, stack, side);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) {
		return outlet.canTake(slot, side);
	}

	@Override
	public int getContainerSize() {
		return 2;
	}

	@Override
	public boolean isEmpty() {
		return getItem(0).isEmpty() && getItem(1).isEmpty();
	}

	@Override
	public net.minecraft.world.item.ItemStack getItem(int slot) {
		return outlet.peek(level, worldPosition, slot);
	}

	@Override
	public net.minecraft.world.item.ItemStack removeItem(int slot, int count) {
		net.minecraft.world.item.ItemStack out = outlet.take(level, worldPosition, slot, count);
		setChanged();
		return out;
	}

	@Override
	public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
		return removeItem(slot, 64);
	}

	@Override
	public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
		outlet.put(level, worldPosition, slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(net.minecraft.world.entity.player.Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		outlet.setGlassBottles(0);
	}

	/** Its stock of empty glass bottles drops when the part is broken (the barn spec's rule for a part's contents). */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && outlet.glassBottles() > 0) {
			net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, outlet.glassBottles()));
			outlet.setGlassBottles(0);
		}
	}

	public CreameryOutlet outlet() {
		return outlet;
	}
}

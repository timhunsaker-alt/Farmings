package dev.farmings.creamery;

import dev.farmings.FarmingsBlocks;
import dev.wildcatter.multiblock.BigPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The main cell of an unpowered creamery part: it remembers that it has grown into its cells, and is what a hopper sees (CreameryOutlet). */
public class CreameryPartEntity extends BlockEntity implements BigPartEntity, net.minecraft.world.WorldlyContainer {
	private final CreameryOutlet outlet;
	private boolean grown;

	public CreameryPartEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.CREAMERY_PART_ENTITY, pos, state);
		this.outlet = new CreameryOutlet(state.getBlock() instanceof CreameryPartBlock b ? b.kind() : CreameryParts.BOTTLER);
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

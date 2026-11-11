package dev.farmings.coop;

import dev.farmings.entity.Wild;
import dev.farmings.husbandry.poultry.Fowl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A grain feeder (docs/multiblocks/coop.md): up to 256 grain items, kept as kilograms of feed (100 g an item). Hand or hopper fills it. */
public class GrainFeederEntity extends BlockEntity implements WorldlyContainer {
	public static final int MAX_ITEMS = 256;
	public static final double CAPACITY_KG = MAX_ITEMS * Fowl.GRAIN_ITEM_KG;
	private static final int[] SLOTS = {0};
	private double kg;

	public GrainFeederEntity(BlockPos pos, BlockState state) {
		super(CoopBlocks.GRAIN_FEEDER_ENTITY, pos, state);
	}

	public double kg() {
		return kg;
	}

	public boolean addItem() {
		if (kg + Fowl.GRAIN_ITEM_KG > CAPACITY_KG + 1e-9) {
			return false;
		}
		kg += Fowl.GRAIN_ITEM_KG;
		setChanged();
		return true;
	}

	public double take(double want) {
		double t = Math.max(0, Math.min(want, kg));
		if (t > 0) {
			kg -= t;
			setChanged();
		}
		return t;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return Wild.grain(stack) && kg + Fowl.GRAIN_ITEM_KG <= CAPACITY_KG + 1e-9;
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
		for (int i = 0; i < stack.getCount() && Wild.grain(stack); i++) {
			addItem();
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putDouble("grain_kg", kg);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		kg = Math.max(0, Math.min(CAPACITY_KG, in.getDoubleOr("grain_kg", 0)));
	}
}

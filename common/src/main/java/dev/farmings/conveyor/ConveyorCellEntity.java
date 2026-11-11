package dev.farmings.conveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** A run cell's hopper face: what a hopper puts in is handed to the run's drive, at this cell's place on the run. */
public class ConveyorCellEntity extends BlockEntity implements WorldlyContainer {
	private static final int[] SLOTS = {0};

	public ConveyorCellEntity(BlockPos pos, BlockState state) {
		super(ConveyorBlocks.CELL_ENTITY, pos, state);
	}

	public @Nullable ConveyorDriveEntity drive() {
		return level == null ? null : ConveyorDriveEntity.driveOf(level, worldPosition);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		ConveyorDriveEntity d = drive();
		return d != null && d.kind().carries(stack) && d.hasRoom();
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
		ConveyorDriveEntity d = drive();
		if (d != null && !stack.isEmpty()) {
			d.putOn(stack.copy(), worldPosition);
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}
}

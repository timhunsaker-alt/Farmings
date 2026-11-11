package dev.farmings.coop;

import dev.farmings.entity.FowlEggs;
import dev.farmings.genetics.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A nest box (docs/multiblocks/coop.md): holds up to 16 eggs, one to a slot. Hens lay into it, a player can put eggs in by hand (to set a
 * broody hen) and take them out, and a hopper below pulls them out.
 */
public class NestBoxEntity extends BlockEntity implements WorldlyContainer {
	public static final int SIZE = 16;
	private final NonNullList<ItemStack> eggs = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private static final int[] SLOTS = java.util.stream.IntStream.range(0, SIZE).toArray();

	public NestBoxEntity(BlockPos pos, BlockState state) {
		super(CoopBlocks.NEST_BOX_ENTITY, pos, state);
	}

	public NonNullList<ItemStack> eggs() {
		return eggs;
	}

	/** Puts one egg in; false when full or not an egg. */
	public boolean add(ItemStack egg) {
		if (FowlEggs.species(egg) == null) {
			return false;
		}
		for (int i = 0; i < SIZE; i++) {
			if (eggs.get(i).isEmpty()) {
				eggs.set(i, egg.copyWithCount(1));
				setChanged();
				return true;
			}
		}
		return false;
	}

	public int count() {
		int n = 0;
		for (ItemStack e : eggs) {
			if (!e.isEmpty()) {
				n++;
			}
		}
		return n;
	}

	public int count(Species s) {
		int n = 0;
		for (ItemStack e : eggs) {
			if (!e.isEmpty() && FowlEggs.species(e) == s) {
				n++;
			}
		}
		return n;
	}

	public boolean full() {
		return count() >= SIZE;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		return count() == 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		return eggs.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack r = ContainerHelper.removeItem(eggs, slot, count);
		setChanged();
		return r;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(eggs, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		eggs.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		eggs.clear();
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		ContainerHelper.saveAllItems(out, eggs);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		eggs.clear();
		ContainerHelper.loadAllItems(in, eggs);
	}
}

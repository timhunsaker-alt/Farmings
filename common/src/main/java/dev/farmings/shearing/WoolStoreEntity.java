package dev.farmings.shearing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A wool table or wool press: a small store the shearing stand fills. A hopper below takes from it; nothing is put in from outside
 * (docs/multiblocks/shearing-shed.md). Right-click with an empty hand takes the first stack.
 */
public class WoolStoreEntity extends BlockEntity implements WorldlyContainer {
	private final NonNullList<ItemStack> items;
	private final int[] all;

	public WoolStoreEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
		super(type, pos, state);
		this.items = NonNullList.withSize(size, ItemStack.EMPTY);
		this.all = new int[size];
		for (int i = 0; i < size; i++) {
			all[i] = i;
		}
	}

	/** Adds a stack; returns what did not fit. */
	public ItemStack insert(ItemStack stack) {
		for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
			ItemStack here = items.get(i);
			if (here.isEmpty()) {
				items.set(i, stack.copy());
				stack = ItemStack.EMPTY;
			} else if (ItemStack.isSameItemSameComponents(here, stack) && here.getCount() < here.getMaxStackSize()) {
				int move = Math.min(stack.getCount(), here.getMaxStackSize() - here.getCount());
				here.grow(move);
				stack = stack.copyWithCount(stack.getCount() - move);
			}
		}
		setChanged();
		return stack;
	}

	public NonNullList<ItemStack> items() {
		return items;
	}

	public ItemStack takeFirst() {
		for (int i = 0; i < items.size(); i++) {
			if (!items.get(i).isEmpty()) {
				ItemStack s = items.get(i);
				items.set(i, ItemStack.EMPTY);
				setChanged();
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	public int count(java.util.function.Predicate<ItemStack> match) {
		int n = 0;
		for (ItemStack s : items) {
			if (match.test(s)) {
				n += s.getCount();
			}
		}
		return n;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? all : new int[0];
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
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack r = ContainerHelper.removeItem(items, slot, count);
		setChanged();
		return r;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		ContainerHelper.saveAllItems(out, items);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		items.clear();
		ContainerHelper.loadAllItems(in, items);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			net.minecraft.world.Containers.dropContents(level, pos, items);
		}
		super.preRemoveSideEffects(pos, state);
	}
}

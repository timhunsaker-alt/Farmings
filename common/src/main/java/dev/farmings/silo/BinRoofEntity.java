package dev.farmings.silo;

import dev.farmings.conveyor.ItemSink;
import dev.farmings.farm.silo.SiloLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The grain bin's cone roof: its fill hole takes grain from an auger, belt or hopper and drops it into the bin below. */
public class BinRoofEntity extends BlockEntity implements ItemSink, WorldlyContainer {
	private static final int[] SLOTS = {0};

	public BinRoofEntity(BlockPos pos, BlockState state) {
		super(SiloBlocks.ROOF_ENTITY, pos, state);
	}

	private @Nullable GrainBinEntity bin() {
		if (level == null) {
			return null;
		}
		BlockPos p = worldPosition.below();
		int n = 0;
		while (n < Towers.BIN_MAX + 1 && level.getBlockState(p.below()).isAir()) {
			p = p.below();
			n++;
		}
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos w = p;
			int k = 0;
			while (k < 3 && level.getBlockState(w).getBlock() != SiloBlocks.BIN_WALL) {
				w = w.relative(d);
				k++;
			}
			if (level.getBlockState(w).getBlock() == SiloBlocks.BIN_WALL) {
				SiloLayout.Tower t = SiloLayout.find(Towers.grid(level, SiloBlocks.BIN_WALL, SiloBlocks.BIN_ROOF), w.getX(), w.getY(), w.getZ(), Towers.BIN_MAX);
				if (t.formed()) {
					BlockPos h = Towers.partBeside(level, t, SiloBlocks.GRAIN_BIN_HOPPER);
					return h != null && level.getBlockEntity(h) instanceof GrainBinEntity b ? b : null;
				}
			}
		}
		return null;
	}

	@Override
	public ItemStack accept(ItemStack stack) {
		GrainBinEntity b = bin();
		return b == null ? stack : b.fill(stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
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
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void clearContent() {
	}
}

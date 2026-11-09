package dev.farmings.creamery;

import dev.farmings.FarmingsBlocks;
import dev.farmings.process.creamery.CreameryCheck;
import dev.farmings.process.creamery.CreameryGrid;
import dev.farmings.process.creamery.CreameryLayout;
import dev.farmings.process.creamery.CreameryPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Reads the world as a {@link CreameryGrid} for the plain-Java forming rule, and finds the controller a part belongs to. */
public final class CreameryWorld {
	private CreameryWorld() {
	}

	public static CreameryPart partOf(BlockState state) {
		if (state.getBlock() instanceof CreameryPartBlock block) {
			return block.kind().part;
		}
		return state.is(FarmingsBlocks.CREAMERY_CONTROLLER) ? CreameryPart.CONTROLLER : null;
	}

	public static CreameryCheck check(Level level, BlockPos pos) {
		CreameryGrid grid = (x, y, z) -> {
			BlockPos at = new BlockPos(x, y, z);
			return level.isLoaded(at) ? partOf(level.getBlockState(at)) : null;
		};
		return CreameryLayout.find(grid, pos.getX(), pos.getY(), pos.getZ());
	}

	/** The formed controller whose row holds this cell, searching the row's reach (16 blocks) on the cell's level and the one below. */
	public static CreameryControllerEntity controllerOf(Level level, BlockPos cell) {
		for (int dy = 0; dy >= -1; dy--) {
			for (int d = 0; d <= 16; d++) {
				for (BlockPos p : new BlockPos[] {cell.offset(d, dy, 0), cell.offset(-d, dy, 0), cell.offset(0, dy, d), cell.offset(0, dy, -d)}) {
					if (level.getBlockEntity(p) instanceof CreameryControllerEntity c) {
						if (c.check() == null) {
							c.refresh(level);
						}
						if (c.formed() && c.owns(cell)) {
							return c;
						}
					}
				}
			}
		}
		return null;
	}
}

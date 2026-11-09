package dev.farmings.parlor;

import dev.farmings.FarmingsBlocks;
import dev.farmings.husbandry.parlor.ParlorCheck;
import dev.farmings.husbandry.parlor.ParlorGrid;
import dev.farmings.husbandry.parlor.ParlorLayout;
import dev.farmings.husbandry.parlor.ParlorPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Reads the world as a {@link ParlorGrid}, so the plain-Java forming rules can look at real blocks. */
public final class ParlorWorld {
	private ParlorWorld() {
	}

	public static ParlorPart partOf(BlockState state) {
		if (state.getBlock() instanceof ParlorPartBlock block) {
			return block.kind().part;
		}
		return state.is(FarmingsBlocks.PARLOR_CONTROLLER) ? ParlorPart.CONTROLLER : null;
	}

	public static ParlorGrid grid(Level level) {
		return (x, y, z) -> {
			BlockPos at = new BlockPos(x, y, z);
			return level.isLoaded(at) ? partOf(level.getBlockState(at)) : null;
		};
	}

	/** What a cell is to the cow lane's path finder: lane, pen, parlor stall, barn alley or barn controller. */
	public static dev.farmings.husbandry.lane.LanePath.Grid laneGrid(Level level) {
		return (x, y, z) -> {
			BlockPos at = new BlockPos(x, y, z);
			if (!level.isLoaded(at)) {
				return dev.farmings.husbandry.lane.LanePath.Cell.OTHER;
			}
			BlockState state = level.getBlockState(at);
			ParlorPart p = partOf(state);
			if (p == ParlorPart.COW_LANE) {
				return dev.farmings.husbandry.lane.LanePath.Cell.LANE;
			}
			if (p == ParlorPart.HOLDING_PEN) {
				return dev.farmings.husbandry.lane.LanePath.Cell.PEN;
			}
			if (p == ParlorPart.PARLOR_STALL) {
				return dev.farmings.husbandry.lane.LanePath.Cell.STALL;
			}
			dev.farmings.husbandry.barn.BarnPart b = dev.farmings.barn.BarnWorld.partOf(state);
			if (b == dev.farmings.husbandry.barn.BarnPart.ALLEY_FLOOR || b == dev.farmings.husbandry.barn.BarnPart.SCRAPER_RAIL) {
				return dev.farmings.husbandry.lane.LanePath.Cell.ALLEY;
			}
			return b == dev.farmings.husbandry.barn.BarnPart.CONTROLLER ? dev.farmings.husbandry.lane.LanePath.Cell.BARN_CONTROLLER
					: dev.farmings.husbandry.lane.LanePath.Cell.OTHER;
		};
	}

	public static ParlorCheck check(Level level, BlockPos pos) {
		return ParlorLayout.find(grid(level), pos.getX(), pos.getY(), pos.getZ());
	}
}

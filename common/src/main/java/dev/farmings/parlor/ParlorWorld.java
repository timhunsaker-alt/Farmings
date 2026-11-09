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

	public static ParlorCheck check(Level level, BlockPos pos) {
		return ParlorLayout.find(grid(level), pos.getX(), pos.getY(), pos.getZ());
	}
}

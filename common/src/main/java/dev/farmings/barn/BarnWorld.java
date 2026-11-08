package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
import dev.farmings.husbandry.barn.BarnCheck;
import dev.farmings.husbandry.barn.BarnGrid;
import dev.farmings.husbandry.barn.BarnLayout;
import dev.farmings.husbandry.barn.BarnPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Reads the world as a {@link BarnGrid}, so the plain-Java forming rules can look at real blocks. */
public final class BarnWorld {
	private BarnWorld() {
	}

	public static BarnPart partOf(BlockState state) {
		if (state.getBlock() instanceof BarnPartBlock block) {
			return block.kind().part;
		}
		return state.is(FarmingsBlocks.BARN_CONTROLLER) ? BarnPart.CONTROLLER : null;
	}

	public static BarnGrid grid(Level level) {
		return (x, y, z) -> {
			BlockPos at = new BlockPos(x, y, z);
			return level.isLoaded(at) ? partOf(level.getBlockState(at)) : null;
		};
	}

	/** The barn the controller at {@code pos} stands at the head of, formed or not. */
	public static BarnCheck check(Level level, BlockPos pos) {
		return BarnLayout.find(grid(level), pos.getX(), pos.getY(), pos.getZ());
	}
}

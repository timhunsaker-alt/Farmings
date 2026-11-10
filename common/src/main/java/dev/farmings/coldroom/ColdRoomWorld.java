package dev.farmings.coldroom;

import dev.farmings.process.coldroom.ColdRoomLayout;
import dev.farmings.process.coldroom.ColdRoomPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Reads the world for the plain-Java forming rule. */
public final class ColdRoomWorld {
	private ColdRoomWorld() {
	}

	public static ColdRoomPart partOf(BlockState state) {
		if (state.is(ColdRoomBlocks.PANEL)) {
			return ColdRoomPart.PANEL;
		}
		if (state.is(ColdRoomBlocks.DOOR)) {
			return ColdRoomPart.DOOR;
		}
		if (state.is(ColdRoomBlocks.EVAPORATOR)) {
			return ColdRoomPart.EVAPORATOR;
		}
		return state.is(ColdRoomBlocks.CONDENSING_UNIT) ? ColdRoomPart.CONDENSER : ColdRoomPart.OTHER;
	}

	public static ColdRoomLayout.Grid grid(Level level) {
		return (x, y, z) -> {
			BlockPos at = new BlockPos(x, y, z);
			// an unloaded cell counts as open, so a room on a chunk edge does not form half-seen
			return level.isLoaded(at) ? partOf(level.getBlockState(at)) : ColdRoomPart.OTHER;
		};
	}
}

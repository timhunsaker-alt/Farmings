package dev.farmings.shed;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/** Finding a sheep shed from the world: used by the shearing stand and the parlor's sheep stalls. */
public final class ShedWorld {
	private ShedWorld() {
	}

	/** True for a sheep-pen or lambing-pen cell. */
	public static boolean penFloor(Level level, BlockPos p) {
		Block b = level.getBlockState(p).getBlock();
		return b == ShedBlocks.SHEEP_PEN || b == ShedBlocks.LAMBING_PEN;
	}

	/** The controller of the sheep shed whose pen holds {@code floor}: the pen is walked until a cell touches a shed controller. */
	public static @Nullable BlockPos findShed(Level level, BlockPos floor) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(floor);
		seen.add(floor);
		while (!queue.isEmpty() && seen.size() <= dev.farmings.husbandry.shed.ShedLayout.MAX_CELLS) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos n = p.relative(d);
				if (level.getBlockState(n).getBlock() == ShedBlocks.SHED_CONTROLLER) {
					return n;
				}
				if (penFloor(level, n) && seen.add(n)) {
					queue.add(n);
				}
			}
		}
		return null;
	}
}

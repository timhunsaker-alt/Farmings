package dev.farmings.silo;

import dev.farmings.conveyor.ConveyorDriveEntity;
import dev.farmings.conveyor.ConveyorPieceBlock;
import dev.farmings.conveyor.ItemSink;
import dev.farmings.farm.silo.SiloLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jspecify.annotations.Nullable;

/** Finding a silo or bin from its parts, and handing items on (docs/multiblocks/silo.md). */
public final class Towers {
	private Towers() {
	}

	public static final int SILO_MAX = 24;
	public static final int BIN_MAX = 16;

	public static SiloLayout.Grid grid(Level level, Block wall, Block roof) {
		return (x, y, z) -> {
			var s = level.getBlockState(new BlockPos(x, y, z));
			if (s.getBlock() == wall) {
				return SiloLayout.Cell.WALL;
			}
			if (s.getBlock() == roof) {
				return SiloLayout.Cell.ROOF;
			}
			return s.isAir() ? SiloLayout.Cell.EMPTY : SiloLayout.Cell.OTHER;
		};
	}

	/** The tower a base part stands beside: the first wall cell next to it that is in a bottom ring. */
	public static SiloLayout.@Nullable Tower fromBase(Level level, BlockPos part, Block wall, Block roof, int max) {
		SiloLayout.Tower last = null;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos w = part.relative(d);
			if (level.getBlockState(w).getBlock() == wall) {
				SiloLayout.Tower t = SiloLayout.find(grid(level, wall, roof), w.getX(), w.getY(), w.getZ(), max);
				if (t.formed()) {
					return t;
				}
				last = t;
			}
		}
		return last;
	}

	/** A part of the given block beside the tower's bottom ring, or null. */
	public static @Nullable BlockPos partBeside(Level level, SiloLayout.Tower t, Block part) {
		int r = t.half() + 1;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
					continue;
				}
				BlockPos p = new BlockPos(t.cx() + dx, t.y0(), t.cz() + dz);
				if (level.getBlockState(p).getBlock() == part) {
					return p;
				}
			}
		}
		return null;
	}

	/** Hands a stack to a belt, a Farmings machine or a container beside {@code from} (not the tower's own parts); returns the rest. */
	public static ItemStack handOn(Level level, BlockPos from, ItemStack stack, java.util.function.Predicate<BlockPos> skip) {
		ItemStack rest = stack;
		// a big part (the 2 x 2 x 2 mixer, the 3 x 3 x 3 digester) hands on from every cell's outside face
		java.util.List<BlockPos> own = dev.wildcatter.multiblock.BigParts.cellsAt(level, from);
		java.util.Set<BlockPos> mine = new java.util.HashSet<>(own);
		for (BlockPos cell : own) {
			for (Direction d : Direction.values()) {
				if (rest.isEmpty()) {
					return rest;
				}
				BlockPos at = cell.relative(d);
				if (mine.contains(at) || skip.test(at)) {
					continue;
				}
				if (level.getBlockState(at).getBlock() instanceof ConveyorPieceBlock) {
					ConveyorDriveEntity drive = ConveyorDriveEntity.driveOf(level, at);
					if (drive != null && drive.kind().carries(rest) && drive.hasRoom()) {
						drive.putOn(rest.copy(), at);
						rest = ItemStack.EMPTY;
					}
					continue;
				}
				BlockEntity be = dev.wildcatter.multiblock.BigParts.entity(level, at);
				if (be instanceof ItemSink sink) {
					rest = sink.accept(rest);
				} else if (be instanceof Container c) {
					rest = HopperBlockEntity.addItem(null, c, rest, d.getOpposite());
				}
			}
		}
		return rest;
	}
}

package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
import dev.farmings.husbandry.barn.BarnCheck;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Looks at the barn every second and remembers what it found: formed, how many stalls, or the first thing wrong. */
public class BarnControllerEntity extends BlockEntity {
	/** How often the structure is checked, in ticks (Wildcatter's STRUCTURE_CHECK_TICKS). */
	public static final int CHECK_TICKS = 20;

	private BarnCheck check;

	public BarnControllerEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.CONTROLLER_ENTITY, pos, state);
	}

	public void serverTick(Level level) {
		if (check == null || (level.getGameTime() + worldPosition.hashCode()) % CHECK_TICKS == 0) {
			refresh(level);
		}
	}

	public void refresh(Level level) {
		check = BarnWorld.check(level, worldPosition);
	}

	/** The last check, or null before the first one. */
	public BarnCheck check() {
		return check;
	}

	public boolean formed() {
		return check != null && check.formed();
	}

	/** What the controller shows when read. */
	public String readout() {
		if (check == null) {
			return "Barn: not checked yet";
		}
		return check.formed() ? "Barn: FORMED, " + check.stallsPerSide() + " stalls a side (" + check.stalls() + " cows)" : "Barn: " + check.status();
	}
}

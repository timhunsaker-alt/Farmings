package dev.farmings.coldroom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The cold-room door: an insulated door that opens by hand (copper's door behaviour and metal sounds). Opening it lets a room of outside
 * air into the cold room it belongs to (docs/multiblocks/cold-room.md, "the room's temperature").
 */
public class ColdRoomDoorBlock extends DoorBlock {
	public ColdRoomDoorBlock(Properties properties) {
		super(BlockSetType.COPPER, properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		boolean wasOpen = state.getValue(OPEN);
		InteractionResult r = super.useWithoutItem(state, level, pos, player, hit);
		if (!level.isClientSide() && !wasOpen) {
			opened(level, pos);
		}
		return r;
	}

	/** Tells the cold room this door is part of that it was opened. */
	public static void opened(Level level, BlockPos door) {
		for (BlockPos p : BlockPos.betweenClosed(door.offset(-12, -6, -12), door.offset(12, 6, 12))) {
			if (level.getBlockEntity(p) instanceof ColdRoomEntity room && room.formed()) {
				int[] min = room.check().min();
				int[] max = room.check().max();
				// a door in the room's walls sits one cell outside its inside box
				if (door.getX() >= min[0] - 1 && door.getX() <= max[0] + 1 && door.getY() >= min[1] - 1 && door.getY() <= max[1] + 1
						&& door.getZ() >= min[2] - 1 && door.getZ() <= max[2] + 1) {
					room.doorOpened();
				}
			}
		}
	}
}

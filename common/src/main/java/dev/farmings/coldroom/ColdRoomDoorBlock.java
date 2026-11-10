package dev.farmings.coldroom;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** The cold-room door: an insulated door that opens by hand (copper's door behaviour and metal sounds). */
public class ColdRoomDoorBlock extends DoorBlock {
	public ColdRoomDoorBlock(Properties properties) {
		super(BlockSetType.COPPER, properties);
	}
}

package dev.farmings.racing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The finish post: beside the track. Use it to read the last race's result from the track's start gate. */
public class FinishPostBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

	public FinishPostBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			StartGateEntity gate = gateFor((ServerLevel) level, pos);
			player.sendSystemMessage(Component.literal(gate == null ? "Finish post: no start gate on this track" : gate.results()));
		}
		return InteractionResult.SUCCESS;
	}

	/** The start gate whose track this post stands beside: the gates in the chunks within 128 blocks. */
	public static StartGateEntity gateFor(ServerLevel level, BlockPos post) {
		int cx = post.getX() >> 4;
		int cz = post.getZ() >> 4;
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				if (!level.hasChunk(cx + dx, cz + dz)) {
					continue;
				}
				for (BlockEntity be : level.getChunk(cx + dx, cz + dz).getBlockEntities().values()) {
					if (be instanceof StartGateEntity gate) {
						gate.refresh(level);
						if (gate.posts().contains(post)) {
							return gate;
						}
					}
				}
			}
		}
		return null;
	}
}

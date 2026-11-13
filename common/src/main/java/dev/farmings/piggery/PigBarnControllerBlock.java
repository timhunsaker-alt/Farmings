package dev.farmings.piggery;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The pig barn's controller: on the floor level touching the pens; right-click reads the barn. */
public class PigBarnControllerBlock extends BaseEntityBlock {
	public PigBarnControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PigBarnControllerEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != PigBarnBlocks.PIG_BARN_CONTROLLER_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((PigBarnControllerEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PigBarnControllerEntity barn) {
			barn.refresh((net.minecraft.server.level.ServerLevel) level);
			player.sendSystemMessage(Component.literal(barn.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}

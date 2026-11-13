package dev.farmings.racing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

/** The start gate: beside the track. Use it to start a race with the horses in its stalls; sneak-use to set the distance. */
public class StartGateBlock extends BaseEntityBlock {
	public StartGateBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StartGateEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != RaceBlocks.START_GATE_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((StartGateEntity) e).serverTick((ServerLevel) l);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StartGateEntity gate) {
			ServerLevel sl = (ServerLevel) level;
			gate.refresh(sl);
			String said = player.isSecondaryUseActive() ? gate.nextDistance() : gate.start(sl);
			player.sendSystemMessage(Component.literal(said));
		}
		return InteractionResult.SUCCESS;
	}
}

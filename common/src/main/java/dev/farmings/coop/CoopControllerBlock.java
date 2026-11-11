package dev.farmings.coop;

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

/** The coop's controller: sits on the floor level touching the coop floor; right-click reads the coop. */
public class CoopControllerBlock extends BaseEntityBlock {
	public CoopControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CoopControllerEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != CoopBlocks.COOP_CONTROLLER_ENTITY) {
			return null;
		}
		return (l, p, s, entity) -> ((CoopControllerEntity) entity).serverTick((net.minecraft.server.level.ServerLevel) l);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CoopControllerEntity coop) {
			coop.refresh((net.minecraft.server.level.ServerLevel) level);
			player.sendSystemMessage(Component.literal(coop.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}

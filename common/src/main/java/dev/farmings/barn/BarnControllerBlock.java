package dev.farmings.barn;

import dev.farmings.FarmingsBlocks;
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

/** The barn's controller: a 1x1x1 box at the pit end. Right-click it to read the barn. */
public class BarnControllerBlock extends BaseEntityBlock {
	public BarnControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BarnControllerEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != FarmingsBlocks.CONTROLLER_ENTITY) {
			return null;
		}
		return (l, p, s, entity) -> ((BarnControllerEntity) entity).serverTick(l);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BarnControllerEntity controller) {
			controller.refresh(level);
			player.sendOverlayMessage(Component.literal(controller.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}

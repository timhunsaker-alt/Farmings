package dev.farmings.creamery;

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

/** The creamery's control panel. Right-click to read it; sneak-right-click to change the product (bottle, cream, butter, cheese). */
public class CreameryControllerBlock extends BaseEntityBlock {
	public CreameryControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CreameryControllerEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != FarmingsBlocks.CREAMERY_CONTROLLER_ENTITY) {
			return null;
		}
		return (l, p, s, entity) -> ((CreameryControllerEntity) entity).serverTick(l);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CreameryControllerEntity controller) {
			controller.refresh(level);
			if (player.isShiftKeyDown()) {
				controller.creamery().setMode(controller.creamery().mode().next());
				controller.setChanged();
			}
			player.sendOverlayMessage(Component.literal(controller.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}

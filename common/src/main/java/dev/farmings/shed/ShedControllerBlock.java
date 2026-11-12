package dev.farmings.shed;

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

/** The sheep shed's controller: sits on the floor level touching the pen; right-click reads the shed. */
public class ShedControllerBlock extends BaseEntityBlock {
	public ShedControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShedControllerEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != ShedBlocks.SHED_CONTROLLER_ENTITY) {
			return null;
		}
		return (l, p, s, entity) -> ((ShedControllerEntity) entity).serverTick((net.minecraft.server.level.ServerLevel) l);
	}

	/** Straw in hand beds the shed (sheep-shed.md, "bedding and manure"). */
	@Override
	protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		if (ShedControllerEntity.strawKg(stack) <= 0) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShedControllerEntity shed) {
			if (shed.addStraw(stack)) {
				stack.consume(1, player);
			} else {
				player.sendOverlayMessage(Component.literal("The shed's straw store is full"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShedControllerEntity shed) {
			shed.refresh((net.minecraft.server.level.ServerLevel) level);
			player.sendSystemMessage(Component.literal(shed.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}

package dev.farmings.gantry;

import dev.farmings.farm.gantry.GantryTool;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The gantry's cab, on the beam at one end. Right-click reads it; sneak-right-click empty-handed starts or stops it; sneak-right-click with
 * a tool head fits the head (parked only); right-click with seed or fertilizer loads it.
 */
public class GantryCabBlock extends BaseEntityBlock {
	public GantryCabBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GantryCabEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != GantryBlocks.CAB_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((GantryCabEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof GantryCabEntity cab)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		GantryTool t = GantryBlocks.toolOf(stack);
		if (t != null && player.isShiftKeyDown()) {
			if (!level.isClientSide()) {
				if (cab.phase() != GantryCabEntity.Phase.PARKED) {
					player.sendOverlayMessage(Component.literal("Park the gantry before swapping its tool"));
				} else {
					ItemStack old = cab.fitTool(t);
					stack.consume(1, player);
					if (!old.isEmpty() && !player.addItem(old)) {
						net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), old);
					}
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (GantryCabEntity.acceptsInput(stack)) {
			if (!level.isClientSide()) {
				ItemStack rest = cab.addInput(stack);
				stack.setCount(rest.getCount());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GantryCabEntity cab) {
			if (player.isShiftKeyDown()) {
				cab.toggle();
			}
			player.sendSystemMessage(Component.literal(cab.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}

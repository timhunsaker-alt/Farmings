package dev.farmings.hatchery;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The incubator: right-click with fertile eggs to set them (the whole handful), empty-handed to read it. */
public class IncubatorBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

	public IncubatorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new IncubatorEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != HatcheryBlocks.INCUBATOR_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((IncubatorEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (dev.farmings.entity.FowlEggs.species(stack) == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IncubatorEntity inc) {
			if (!IncubatorEntity.accepts(stack)) {
				player.sendOverlayMessage(Component.literal("Only fertile eggs can be set"));
			} else {
				int n = 0;
				while (n < stack.getCount() && inc.add(stack)) {
					n++;
				}
				stack.consume(n, player);
				inc.setOwner(player.getUUID());
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IncubatorEntity inc) {
			player.sendSystemMessage(Component.literal(inc.readout()));
		}
		return InteractionResult.SUCCESS;
	}
}

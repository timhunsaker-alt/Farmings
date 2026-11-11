package dev.farmings.coop;

import dev.farmings.entity.Wild;
import java.util.Locale;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A grain feeder: right-click with grain to pour in the whole handful, empty-handed to read it. */
public class GrainFeederBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

	public GrainFeederBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GrainFeederEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!dev.farmings.feedmill.FeedItems.birdFeed(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GrainFeederEntity f) {
			int n = 0;
			while (n < stack.getCount() && f.addItem(stack)) {
				n++;
			}
			stack.consume(n, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GrainFeederEntity f) {
			player.sendSystemMessage(Component.literal(String.format(Locale.ROOT, "Grain feeder: %.1f kg of %.1f", f.kg(), GrainFeederEntity.CAPACITY_KG)));
		}
		return InteractionResult.SUCCESS;
	}
}

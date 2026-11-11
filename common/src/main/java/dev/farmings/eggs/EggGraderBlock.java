package dev.farmings.eggs;

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

/** The egg grading table: right-click with eggs to grade the handful, empty-handed to take the cartons (sneak to read it). */
public class EggGraderBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

	public EggGraderBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EggGraderEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!EggGraderEntity.accepts(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EggGraderEntity g) {
			int n = stack.getCount();
			for (int i = 0; i < n; i++) {
				g.grade(stack);
			}
			stack.consume(n, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EggGraderEntity g) {
			if (player.isShiftKeyDown()) {
				player.sendSystemMessage(Component.literal(g.readout()));
			} else {
				for (ItemStack c : g.takeCartons()) {
					if (!player.addItem(c)) {
						net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), c);
					}
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}

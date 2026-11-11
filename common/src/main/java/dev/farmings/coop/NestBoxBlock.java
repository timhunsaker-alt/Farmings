package dev.farmings.coop;

import dev.farmings.entity.FowlEggs;
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

/** A nest box: right-click with an egg to put it in, empty-handed to collect the eggs (sneak to read it). */
public class NestBoxBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

	public NestBoxBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new NestBoxEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (FowlEggs.species(stack) == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NestBoxEntity n && n.add(stack)) {
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NestBoxEntity n) {
			if (player.isShiftKeyDown()) {
				int fertile = 0;
				for (ItemStack e : n.eggs()) {
					if (!e.isEmpty() && FowlEggs.fertile(e)) {
						fertile++;
					}
				}
				player.sendSystemMessage(Component.literal(String.format(Locale.ROOT, "Nest box: %d of %d eggs, %d fertile", n.count(), NestBoxEntity.SIZE, fertile)));
			} else {
				for (int i = 0; i < NestBoxEntity.SIZE; i++) {
					ItemStack e = n.removeItemNoUpdate(i);
					if (!e.isEmpty() && !player.addItem(e)) {
						net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), e);
					}
				}
				n.setChanged();
			}
		}
		return InteractionResult.SUCCESS;
	}
}

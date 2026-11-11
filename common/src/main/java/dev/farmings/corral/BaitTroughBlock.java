package dev.farmings.corral;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

/** The corral's bait trough: right-click with a hay block to fill it, empty-handed to read it. */
public class BaitTroughBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 10, 16);

	public BaitTroughBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BaitTroughEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		boolean grain = dev.farmings.entity.Wild.grain(stack);
		if (!stack.is(Items.HAY_BLOCK) && !grain) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BaitTroughEntity t) {
			// grain goes in a handful at a time: the whole stack in hand, as far as there is room
			int n = 0;
			while (n < stack.getCount() && (grain ? t.addGrain() : t.addBlock())) {
				n++;
				if (!grain) {
					break;
				}
			}
			stack.consume(n, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BaitTroughEntity t) {
			player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT, "Bait trough: %.0f kg of hay (dry matter), %.1f kg of grain",
					t.dryMatterKg(), t.grainKg())));
		}
		return InteractionResult.SUCCESS;
	}
}

package dev.farmings.creamery;

import dev.farmings.FarmingsItems;
import dev.farmings.process.creamery.Creamery;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One creamery part at true size. A milk bucket on the receiving tank pours 10 L in; a glass bottle on the bottler fills one bottle; an empty
 * hand takes butter from the churn and aged wheels from the aging shelf, and reads any other part.
 */
public class CreameryPartBlock extends BigPartBlock {
	public static final double BUCKET_L = 10;
	private final CreameryParts kind;
	private final VoxelShape[][] shapes;

	public CreameryPartBlock(Properties properties, CreameryParts kind) {
		super(properties, kind.footprint, "farmings_" + kind.id);
		this.kind = kind;
		int pieces = kind.footprint.pieces();
		this.shapes = new VoxelShape[4][pieces];
		for (int turns = 0; turns < 4; turns++) {
			for (int piece = 0; piece < pieces; piece++) {
				double[] b = dev.farmings.parlor.ParlorPartBlock.turnBox(kind.shapeFor(kind.footprint.cell(piece).dy()), turns);
				shapes[turns][piece] = Shapes.box(b[0] / 16, b[1] / 16, b[2] / 16, b[3] / 16, b[4] / 16, b[5] / 16);
			}
		}
	}

	public CreameryParts kind() {
		return kind;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return kind.watts > 0 ? new CreameryMachineEntity(pos, state) : new CreameryPartEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		boolean bucket = kind == CreameryParts.RECEIVING_TANK && stack.is(Items.MILK_BUCKET);
		boolean bottle = kind == CreameryParts.BOTTLER && stack.is(Items.GLASS_BOTTLE);
		if (!bucket && !bottle) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		CreameryControllerEntity c = CreameryWorld.controllerOf(level, pos);
		if (c == null) {
			player.sendOverlayMessage(Component.literal("This part is not in a formed creamery"));
			return InteractionResult.FAIL;
		}
		Creamery creamery = c.creamery();
		if (bucket) {
			if (creamery.room() < BUCKET_L) {
				player.sendOverlayMessage(Component.literal("The receiving tank is full"));
				return InteractionResult.FAIL;
			}
			creamery.addMilk(BUCKET_L, Creamery.DEFAULT_FAT, Creamery.DEFAULT_PROTEIN);
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
		} else {
			Creamery.Bottle b = creamery.takeBottle();
			if (b == null) {
				player.sendOverlayMessage(Component.literal("Nothing to bottle yet"));
				return InteractionResult.FAIL;
			}
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.bottle(b, creamery.kind())), level.getGameTime())));
		}
		c.setChanged();
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		CreameryControllerEntity c = CreameryWorld.controllerOf(level, pos);
		if (c != null && kind == CreameryParts.BUTTER_CHURN) {
			give(player, dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.BUTTER, c.creamery().takeButter(64)), level.getGameTime()));
			c.setChanged();
			return InteractionResult.SUCCESS;
		}
		if (c != null && kind == CreameryParts.AGING_SHELF) {
			give(player, dev.farmings.item.Perishables.stamp(new ItemStack(FarmingsItems.cheeseWheel(c.creamery().kind()), c.creamery().takeWheels(16)), level.getGameTime()));
			c.setChanged();
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(BigParts.main(pos, state)) instanceof Inspectable machine) {
			player.sendOverlayMessage(machine.inspect());
		} else if (c != null) {
			player.sendOverlayMessage(Component.literal(c.readout()));
		}
		return InteractionResult.SUCCESS;
	}

	private static void give(Player player, ItemStack stack) {
		if (stack.isEmpty()) {
			player.sendOverlayMessage(Component.literal("Nothing ready yet"));
			return;
		}
		if (!player.getInventory().add(stack)) {
			player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack));
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int piece = Math.min(state.getValue(piece()), shapes[0].length - 1);
		return shapes[Math.floorMod(BigParts.turns(state), 4)][piece];
	}

	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return false;
	}
}

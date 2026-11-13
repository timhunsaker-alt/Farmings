package dev.farmings.parlor;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigParts;
import dev.farmings.husbandry.parlor.BulkTank;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One parlor part at true size: fills its footprint, breaks whole, drops one item, placeholder collision shape. */
public class ParlorPartBlock extends BigPartBlock {
	private final ParlorParts kind;
	private final VoxelShape[][] shapes;

	public ParlorPartBlock(Properties properties, ParlorParts kind) {
		super(properties, kind.footprint, "farmings_" + kind.id);
		this.kind = kind;
		int pieces = kind.footprint.pieces();
		this.shapes = new VoxelShape[4][pieces];
		for (int turns = 0; turns < 4; turns++) {
			for (int piece = 0; piece < pieces; piece++) {
				double[] b = turnBox(kind.shapeFor(kind.footprint.cell(piece).dy()), turns);
				shapes[turns][piece] = Shapes.box(b[0] / 16, b[1] / 16, b[2] / 16, b[3] / 16, b[4] / 16, b[5] / 16);
			}
		}
	}

	public ParlorParts kind() {
		return kind;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (kind) {
			case VACUUM_PUMP -> new PumpEntity(pos, state);
			case BULK_TANK -> new TankEntity(pos, state);
			default -> new ParlorPartEntity(pos, state);
		};
	}

	/** An empty bucket on the tank takes {@link BulkTank#BUCKET_L} litres out as a vanilla milk bucket. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
			BlockHitResult hit) {
		if (level.getBlockEntity(BigParts.main(pos, state)) instanceof TankEntity tank && stack.is(Items.BUCKET)) {
			if (!level.isClientSide()) {
				if (tank.tank().kind() != dev.farmings.husbandry.parlor.MilkKind.COW) {
					player.sendOverlayMessage(Component.literal("The tank holds " + tank.tank().kind().label() + " milk: it goes to the creamery, not into a bucket"));
				} else if (tank.tank().litres() >= BulkTank.BUCKET_L) {
					tank.tank().take(BulkTank.BUCKET_L);
					tank.setChanged();
					player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, new ItemStack(Items.MILK_BUCKET)));
				} else {
					player.sendOverlayMessage(Component.literal("The tank has less than one bucket (" + (int) BulkTank.BUCKET_L + " L)"));
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(BigParts.main(pos, state)) instanceof Inspectable machine) {
			player.sendOverlayMessage(machine.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int piece = Math.min(state.getValue(piece()), shapes[0].length - 1);
		return shapes[Math.floorMod(BigParts.turns(state), 4)][piece];
	}

	/** The parlor stall, cow lane and holding pen are walked on; everything else stops a cow. */
	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return (kind == ParlorParts.PARLOR_STALL || kind == ParlorParts.COW_LANE || kind == ParlorParts.HOLDING_PEN || kind == ParlorParts.SHEEP_STALL) && super.isPathfindable(state, type);
	}

	public static double[] turnBox(double[] b, int turns) {
		double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
		for (int i = 0; i < turns; i++) {
			double nx1 = 16 - z2, nx2 = 16 - z1, nz1 = x1, nz2 = x2;
			x1 = nx1;
			x2 = nx2;
			z1 = nz1;
			z2 = nz2;
		}
		return new double[] {x1, b[1], z1, x2, b[4], z2};
	}
}

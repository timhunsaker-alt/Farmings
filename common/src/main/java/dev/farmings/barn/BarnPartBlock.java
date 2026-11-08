package dev.farmings.barn;

import dev.farmings.FarmingsItems;
import dev.wildcatter.energy.Inspectable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One barn part at true size: it fills its footprint, breaks whole, drops one item, and has a placeholder collision shape. */
public class BarnPartBlock extends BigPartBlock {
	private final BarnParts kind;
	private final VoxelShape[][] shapes;     // [turns][piece]

	public BarnPartBlock(Properties properties, BarnParts kind) {
		super(properties, kind.footprint, "farmings_" + kind.id);
		this.kind = kind;
		int pieces = kind.footprint.pieces();
		this.shapes = new VoxelShape[4][pieces];
		for (int turns = 0; turns < 4; turns++) {
			for (int piece = 0; piece < pieces; piece++) {
				double[] b = turn(kind.shapeFor(kind.footprint.cell(piece).dy()), turns);
				shapes[turns][piece] = Shapes.box(b[0] / 16, b[1] / 16, b[2] / 16, b[3] / 16, b[4] / 16, b[5] / 16);
			}
		}
	}

	public BarnParts kind() {
		return kind;
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return switch (kind) {
			case COLLECTION_PIT -> new PitEntity(pos, state);
			case SCRAPER_DRIVE -> new DriveEntity(pos, state);
			case HAY_FEEDER -> new FeederEntity(pos, state);
			case WATER_TROUGH -> new TroughEntity(pos, state);
			default -> new BarnPartEntity(pos, state);
		};
	}

	/** A hay block into the feeder, a water bucket into the trough. Anything else falls through to a plain click. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
			BlockHitResult hit) {
		BlockEntity be = level.getBlockEntity(BigParts.main(pos, state));
		if (be instanceof FeederEntity feeder && stack.is(net.minecraft.world.item.Items.HAY_BLOCK)) {
			if (!level.isClientSide()) {
				if (feeder.addBlock()) {
					if (!player.hasInfiniteMaterials()) {
						stack.shrink(1);
					}
				} else {
					player.sendOverlayMessage(Component.literal("The hay feeder holds " + FeederEntity.MAX_BLOCKS + " blocks and is full"));
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (be instanceof TroughEntity trough && stack.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
			if (!level.isClientSide()) {
				if (trough.pourBucket()) {
					player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, new ItemStack(net.minecraft.world.item.Items.BUCKET)));
				} else {
					player.sendOverlayMessage(Component.literal("The trough is full"));
				}
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	/**
	 * A click on any cell goes to the part's main cell. The pit gives its manure to an empty hand (up to a stack) and otherwise says
	 * how full it is; the drive says what its motor is doing.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity be = level.getBlockEntity(BigParts.main(pos, state));
		if (be instanceof PitEntity pit) {
			if (player.getMainHandItem().isEmpty() && pit.manure() > 0) {
				int taken = pit.take(64);
				ItemStack stack = new ItemStack(FarmingsItems.MANURE, taken);
				if (!player.getInventory().add(stack)) {
					level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack));   // a full inventory: it lands at the player's feet
				}
			}
			player.sendOverlayMessage(Component.literal("Collection pit: " + pit.manure() + " / " + PitEntity.CAPACITY + " manure"));
		} else if (be instanceof FeederEntity feeder) {
			player.sendOverlayMessage(Component.literal("Hay feeder: " + Math.round(feeder.dryMatterKg()) + " kg dry matter (" + String.format("%.1f", feeder.blocks()) + " of "
					+ FeederEntity.MAX_BLOCKS + " blocks)"));
		} else if (be instanceof TroughEntity trough) {
			player.sendOverlayMessage(Component.literal("Water trough: " + Math.round(trough.litres()) + " / " + Math.round(TroughEntity.CAPACITY_LITRES) + " L"));
		} else if (be instanceof Inspectable machine) {
			player.sendOverlayMessage(machine.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int piece = Math.min(state.getValue(piece()), shapes[0].length - 1);
		return shapes[Math.floorMod(BigParts.turns(state), 4)][piece];
	}

	/**
	 * Cattle must not plan a route through a part they cannot pass. A thin divider is not a full block, so the pathfinder would call its cell
	 * walkable and the cow would press against the panel for ever (the way vanilla fences say no). Walkable are the surfaces: the stall bed,
	 * the alley floor, the rail and the pit's top. Everything else stops a cow.
	 */
	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return switch (kind) {
			case STALL_BED, ALLEY_FLOOR, SCRAPER_RAIL, COLLECTION_PIT -> super.isPathfindable(state, type);
			default -> false;
		};
	}

	/** A box in sixteenths turned quarter turns clockwise (seen from above) about the cell's centre, as Wildcatter turns its own. */
	static double[] turn(double[] b, int turns) {
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

	static boolean isPart(BlockState state) {
		return state.getBlock() instanceof BarnPartBlock;
	}
}

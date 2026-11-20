package dev.farmings.health;

import dev.farmings.Farmings;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Animal health's blocks (docs/animal-health.md, "Blocks"): the footbath, the waste milk can and the isolation pen. */
public final class HealthBlocks {
	public static final Block FOOTBATH = block("footbath", p -> new FootbathBlock(p.mapColor(MapColor.STONE).strength(1.5F, 6.0F).sound(SoundType.STONE).noOcclusion()));
	public static final Block WASTE_MILK_CAN = block("waste_milk_can", p -> new WasteCanBlock(p.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));
	public static final Block ISOLATION_CONTROLLER = block("isolation_controller",
			p -> new IsolationController.ControllerBlock(p.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL)));
	public static final Block ISOLATION_PEN = block("isolation_pen", p -> new PenFloorBlock(p.mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.GRASS).noOcclusion()));
	public static final BlockEntityType<FootbathEntity> FOOTBATH_ENTITY = new BlockEntityType<>(FootbathEntity::new, Set.of(FOOTBATH));
	public static final BlockEntityType<WasteCanEntity> WASTE_CAN_ENTITY = new BlockEntityType<>(WasteCanEntity::new, Set.of(WASTE_MILK_CAN));
	public static final BlockEntityType<IsolationController> ISOLATION_ENTITY = new BlockEntityType<>(IsolationController::new, Set.of(ISOLATION_CONTROLLER));
	public static final Block[] ALL = {FOOTBATH, WASTE_MILK_CAN, ISOLATION_CONTROLLER, ISOLATION_PEN};
	public static final String[] NAMES = {"footbath", "waste_milk_can", "isolation_controller", "isolation_pen"};

	private HealthBlocks() {
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Farmings.id(name));
		return factory.apply(BlockBehaviour.Properties.of().setId(key));
	}

	public static void register() {
		for (int i = 0; i < ALL.length; i++) {
			Registry.register(BuiltInRegistries.BLOCK, Farmings.id(NAMES[i]), ALL[i]);
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Farmings.id(NAMES[i]));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(ALL[i], new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		}
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("footbath"), FOOTBATH_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("waste_milk_can"), WASTE_CAN_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("isolation_controller"), ISOLATION_ENTITY);
	}

	/** The isolation pen's bedded floor: 2/16 high, walked on. */
	public static class PenFloorBlock extends Block {
		private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

		public PenFloorBlock(Properties p) {
			super(p);
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
			return SHAPE;
		}

		@Override
		protected boolean isPathfindable(BlockState state, PathComputationType type) {
			return true;
		}
	}

	// ---- the footbath ----

	/**
	 * A concrete footbath set in a lane or a gate path: 10% zinc sulfate, at least 6 cm deep (MSD). Water from a bucket or a Wildcatter
	 * water pump's pipe; a {@code zinc_sulfate} bag charges it for 100 passes (proposed). Every animal that walks through is treated and
	 * protected 14 real days.
	 */
	public static class FootbathBlock extends BaseEntityBlock {
		private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3, 16);

		public FootbathBlock(Properties p) {
			super(p);
		}

		@Override
		public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new FootbathEntity(pos, state);
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
			return SHAPE;
		}

		@Override
		protected boolean isPathfindable(BlockState state, PathComputationType type) {
			return true;
		}

		@Override
		public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
			if (level instanceof ServerLevel server && entity instanceof HealthHost h && level.getBlockEntity(pos) instanceof FootbathEntity bath) {
				bath.pass(server, h);
			}
			super.stepOn(level, pos, state, entity);
		}

		@Override
		protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
			if (!(level.getBlockEntity(pos) instanceof FootbathEntity bath)) {
				return InteractionResult.PASS;
			}
			if (stack.is(Items.WATER_BUCKET)) {
				if (!level.isClientSide()) {
					bath.addWater(FootbathEntity.CAPACITY_L);
					if (!player.getAbilities().instabuild) {
						player.setItemInHand(hand, new ItemStack(Items.BUCKET));
					}
					player.sendOverlayMessage(Component.literal(bath.readout()));
				}
				return InteractionResult.SUCCESS;
			}
			if (stack.is(HealthItems.ZINC_SULFATE)) {
				if (!level.isClientSide()) {
					bath.charge(FootbathEntity.PASSES_PER_BAG);
					stack.consume(1, player);
					dev.wildcatter.quest.QuestEvents.milestone(level, pos, dev.farmings.quest.Milestones.FOOTBATH);
					player.sendOverlayMessage(Component.literal(bath.readout()));
				}
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FootbathEntity bath) {
				player.sendSystemMessage(Component.literal(bath.readout()));
			}
			return InteractionResult.SUCCESS;
		}
	}

	public static class FootbathEntity extends BlockEntity implements dev.wildcatter.block.WaterUser {
		/** A 1 m x 1 m bath 10 cm deep holds 100 L (proposed: over MSD's 6 cm). */
		public static final double CAPACITY_L = 100;
		public static final int PASSES_PER_BAG = 100;
		private double litres;
		private int passes;

		public FootbathEntity(BlockPos pos, BlockState state) {
			super(FOOTBATH_ENTITY, pos, state);
		}

		public boolean charged() {
			return passes > 0 && litres >= CAPACITY_L * 0.6;
		}

		public int passes() {
			return passes;
		}

		public void charge(int n) {
			passes += n;
			setChanged();
		}

		public void addWater(double l) {
			litres = Math.min(CAPACITY_L, litres + l);
			setChanged();
		}

		/** An animal walks through: a dose if charged and it has not had one in the last real day. */
		public void pass(ServerLevel level, HealthHost h) {
			if (!charged()) {
				return;
			}
			long now = level.getGameTime();
			boolean protectedLong = h.health().footbathed(now + Health.scale(h).realDaysToTicks(Health.FOOTBATH_DAYS - 1));
			if (protectedLong) {
				return;
			}
			Health.footbath(level, h);
			passes--;
			litres = Math.max(0, litres - 0.5);
			setChanged();
			level.playSound(null, worldPosition, HealthSounds.FOOTBATH, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);
		}

		public String readout() {
			return String.format(Locale.ROOT, "Footbath: %.0f of %.0f L, %s", litres, CAPACITY_L,
					passes > 0 ? passes + " passes of 10% zinc sulfate left" : "NOT CHARGED (zinc sulfate)") + (litres < CAPACITY_L * 0.6 ? ", NEEDS WATER" : "");
		}

		@Override
		public double waterLevel() {
			return litres / CAPACITY_L;
		}

		@Override
		public int addWater(int l) {
			int fits = (int) Math.max(0, Math.min(l, Math.floor(CAPACITY_L - litres)));
			if (fits > 0) {
				litres += fits;
				setChanged();
			}
			return fits;
		}

		@Override
		protected void saveAdditional(ValueOutput out) {
			super.saveAdditional(out);
			out.putDouble("litres", litres);
			out.putInt("passes", passes);
		}

		@Override
		protected void loadAdditional(ValueInput in) {
			super.loadAdditional(in);
			litres = in.getDoubleOr("litres", 0);
			passes = in.getIntOr("passes", 0);
		}
	}

	// ---- the waste milk can ----

	/** A dump bucket at the parlor: withheld and abnormal milk goes here, never to the tank. Right-click empties it. */
	public static class WasteCanBlock extends BaseEntityBlock {
		private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

		public WasteCanBlock(Properties p) {
			super(p);
		}

		@Override
		public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new WasteCanEntity(pos, state);
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
			return SHAPE;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WasteCanEntity can) {
				if (player.isShiftKeyDown() && can.litres() > 0) {
					player.sendSystemMessage(Component.literal(String.format(Locale.ROOT, "Poured away %.0f L of waste milk", can.litres())));
					can.empty();
				} else {
					player.sendSystemMessage(Component.literal(can.readout() + (can.litres() > 0 ? " (sneak + use to pour it away)" : "")));
				}
			}
			return InteractionResult.SUCCESS;
		}
	}

	public static class WasteCanEntity extends BlockEntity {
		public static final double CAPACITY_L = 200;
		private double litres;

		public WasteCanEntity(BlockPos pos, BlockState state) {
			super(WASTE_CAN_ENTITY, pos, state);
		}

		public double litres() {
			return litres;
		}

		public double room() {
			return CAPACITY_L - litres;
		}

		public void add(double l) {
			litres = Math.min(CAPACITY_L, litres + l);
			setChanged();
		}

		public void empty() {
			litres = 0;
			setChanged();
		}

		public String readout() {
			return String.format(Locale.ROOT, "Waste milk can: %.0f of %.0f L%s", litres, CAPACITY_L, room() <= 0 ? ", FULL" : "");
		}

		@Override
		protected void saveAdditional(ValueOutput out) {
			super.saveAdditional(out);
			out.putDouble("litres", litres);
		}

		@Override
		protected void loadAdditional(ValueInput in) {
			super.loadAdditional(in);
			litres = in.getDoubleOr("litres", 0);
		}
	}

	/** The isolation controller's ticker, shared shape for {@link IsolationController.ControllerBlock}. */
	static <T extends BlockEntity> @Nullable BlockEntityTicker<T> isolationTicker(Level level, BlockEntityType<T> type) {
		if (level.isClientSide() || type != ISOLATION_ENTITY) {
			return null;
		}
		return (l, p, s, e) -> ((IsolationController) e).serverTick((ServerLevel) l);
	}
}

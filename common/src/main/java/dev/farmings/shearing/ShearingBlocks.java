package dev.farmings.shearing;

import dev.farmings.Farmings;
import dev.farmings.shed.ShedFloorBlock;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The shearing shed's blocks (docs/multiblocks/shearing-shed.md). */
public final class ShearingBlocks {
	public static final Block SHEARING_STAND = block("shearing_stand", p -> new StandBlock(p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)));
	public static final Block CATCHING_PEN = block("catching_pen", p -> new ShedFloorBlock(p.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
	public static final Block WOOL_TABLE = block("wool_table", p -> new StoreBlock(p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion(), true));
	public static final Block WOOL_PRESS = block("wool_press", p -> new StoreBlock(p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD), false));
	public static final BlockEntityType<ShearingStandEntity> STAND_ENTITY = new BlockEntityType<>(ShearingStandEntity::new, Set.of(SHEARING_STAND));
	public static final BlockEntityType<WoolStoreEntity> TABLE_ENTITY = new BlockEntityType<>((pos, state) -> new WoolStoreEntity(ShearingBlocks.TABLE_ENTITY, pos, state, 9), Set.of(WOOL_TABLE));
	public static final BlockEntityType<WoolStoreEntity> PRESS_ENTITY = new BlockEntityType<>((pos, state) -> new WoolStoreEntity(ShearingBlocks.PRESS_ENTITY, pos, state, 4), Set.of(WOOL_PRESS));
	public static final Block[] ALL = {SHEARING_STAND, CATCHING_PEN, WOOL_TABLE, WOOL_PRESS};
	public static final String[] NAMES = {"shearing_stand", "catching_pen", "wool_table", "wool_press"};

	private ShearingBlocks() {
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
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("shearing_stand"), STAND_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("wool_table"), TABLE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("wool_press"), PRESS_ENTITY);
	}

	/** The shearing stand: right-click reads it. */
	static final class StandBlock extends BaseEntityBlock {
		StandBlock(Properties p) {
			super(p);
		}

		@Override
		public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new ShearingStandEntity(pos, state);
		}

		@Override
		public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
			if (level.isClientSide() || type != STAND_ENTITY) {
				return null;
			}
			return (l, p, s, e) -> ((ShearingStandEntity) e).serverTick((net.minecraft.server.level.ServerLevel) l);
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShearingStandEntity stand) {
				stand.refresh((net.minecraft.server.level.ServerLevel) level);
				player.sendSystemMessage(Component.literal(stand.readout()));
			}
			return InteractionResult.SUCCESS;
		}
	}

	/** The wool table or press: right-click with an empty hand takes the first stack out. */
	static final class StoreBlock extends BaseEntityBlock {
		private final boolean table;

		StoreBlock(Properties p, boolean table) {
			super(p);
			this.table = table;
		}

		@Override
		public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return table ? new WoolStoreEntity(TABLE_ENTITY, pos, state, 9) : new WoolStoreEntity(PRESS_ENTITY, pos, state, 4);
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WoolStoreEntity store) {
				ItemStack s = store.takeFirst();
				if (s.isEmpty()) {
					player.sendOverlayMessage(Component.literal(table ? "The wool table is empty" : "The wool press is empty"));
				} else if (!player.getInventory().add(s)) {
					net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, s);
				}
			}
			return InteractionResult.SUCCESS;
		}
	}
}

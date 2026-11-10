package dev.farmings;

import dev.farmings.barn.BarnControllerBlock;
import dev.farmings.barn.BarnControllerEntity;
import dev.farmings.barn.BarnPartBlock;
import dev.farmings.barn.BarnPartEntity;
import dev.farmings.barn.DriveEntity;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.barn.PitEntity;
import dev.farmings.barn.BarnParts;
import dev.farmings.parlor.ParlorControllerBlock;
import dev.farmings.parlor.ParlorControllerEntity;
import dev.farmings.parlor.ParlorPartBlock;
import dev.farmings.parlor.ParlorPartEntity;
import dev.farmings.parlor.ParlorParts;
import dev.farmings.parlor.PumpEntity;
import dev.farmings.parlor.TankEntity;
import dev.wildcatter.item.BigPartItem;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The barn's blocks, items and block entities. Registered with the other content while the registries are open. */
public final class FarmingsBlocks {
	public static final Block BARN_CONTROLLER = block("barn_controller", props -> new BarnControllerBlock(props.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL)));
	public static final Block PARLOR_CONTROLLER = block("parlor_controller", props -> new ParlorControllerBlock(props.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL)));
	public static final Block CREAMERY_CONTROLLER = block("creamery_controller", props -> new dev.farmings.creamery.CreameryControllerBlock(props.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL)));
	private static final Block[] CREAMERY_PARTS = new Block[dev.farmings.creamery.CreameryParts.values().length];
	private static final Block[] PARLOR_PARTS = new Block[ParlorParts.values().length];
	private static final Block[] PARTS = new Block[BarnParts.values().length];

	static {
		for (BarnParts kind : BarnParts.values()) {
			PARTS[kind.ordinal()] = block(kind.id, props -> new BarnPartBlock(props.mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE), kind));
		}
	}

	static {
		for (ParlorParts kind : ParlorParts.values()) {
			PARLOR_PARTS[kind.ordinal()] = block(kind.id, props -> new ParlorPartBlock(props.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL), kind));
		}
	}

	static {
		for (dev.farmings.creamery.CreameryParts kind : dev.farmings.creamery.CreameryParts.values()) {
			CREAMERY_PARTS[kind.ordinal()] = block(kind.id, props -> new dev.farmings.creamery.CreameryPartBlock(props.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL), kind));
		}
	}

	public static final BlockEntityType<dev.farmings.creamery.CreameryPartEntity> CREAMERY_PART_ENTITY = new BlockEntityType<>(dev.farmings.creamery.CreameryPartEntity::new, creameryParts(false));
	public static final BlockEntityType<dev.farmings.creamery.CreameryMachineEntity> CREAMERY_MACHINE_ENTITY = new BlockEntityType<>(dev.farmings.creamery.CreameryMachineEntity::new, creameryParts(true));
	public static final BlockEntityType<dev.farmings.creamery.CreameryControllerEntity> CREAMERY_CONTROLLER_ENTITY = new BlockEntityType<>(dev.farmings.creamery.CreameryControllerEntity::new, Set.of(CREAMERY_CONTROLLER));
	public static final BlockEntityType<ParlorPartEntity> PARLOR_PART_ENTITY = new BlockEntityType<>(ParlorPartEntity::new, parlorOthers());
	public static final BlockEntityType<PumpEntity> PUMP_ENTITY = new BlockEntityType<>(PumpEntity::new, Set.of(parlorPart(ParlorParts.VACUUM_PUMP)));
	public static final BlockEntityType<TankEntity> TANK_ENTITY = new BlockEntityType<>(TankEntity::new, Set.of(parlorPart(ParlorParts.BULK_TANK)));
	public static final BlockEntityType<ParlorControllerEntity> PARLOR_CONTROLLER_ENTITY = new BlockEntityType<>(ParlorControllerEntity::new, Set.of(PARLOR_CONTROLLER));
	public static final BlockEntityType<BarnPartEntity> PART_ENTITY = new BlockEntityType<>(BarnPartEntity::new, others());
	public static final BlockEntityType<FeederEntity> FEEDER_ENTITY = new BlockEntityType<>(FeederEntity::new, Set.of(part(BarnParts.HAY_FEEDER)));
	public static final BlockEntityType<TroughEntity> TROUGH_ENTITY = new BlockEntityType<>(TroughEntity::new, Set.of(part(BarnParts.WATER_TROUGH)));
	public static final BlockEntityType<PitEntity> PIT_ENTITY = new BlockEntityType<>(PitEntity::new, Set.of(part(BarnParts.COLLECTION_PIT)));
	public static final BlockEntityType<DriveEntity> DRIVE_ENTITY = new BlockEntityType<>(DriveEntity::new, Set.of(part(BarnParts.SCRAPER_DRIVE)));
	public static final BlockEntityType<BarnControllerEntity> CONTROLLER_ENTITY = new BlockEntityType<>(BarnControllerEntity::new, Set.of(BARN_CONTROLLER));

	private FarmingsBlocks() {
	}

	/** Every part whose main cell only remembers it has grown: all but the pit and the drive, which have their own entities. */
	private static Set<Block> others() {
		Set<Block> out = new HashSet<>();
		for (BarnParts kind : BarnParts.values()) {
			if (kind != BarnParts.COLLECTION_PIT && kind != BarnParts.SCRAPER_DRIVE && kind != BarnParts.HAY_FEEDER && kind != BarnParts.WATER_TROUGH) {
				out.add(PARTS[kind.ordinal()]);
			}
		}
		return out;
	}

	private static Set<Block> parlorOthers() {
		Set<Block> out = new HashSet<>();
		for (ParlorParts kind : ParlorParts.values()) {
			if (kind != ParlorParts.VACUUM_PUMP && kind != ParlorParts.BULK_TANK) {
				out.add(PARLOR_PARTS[kind.ordinal()]);
			}
		}
		return out;
	}

	private static Set<Block> creameryParts(boolean powered) {
		Set<Block> out = new HashSet<>();
		for (dev.farmings.creamery.CreameryParts kind : dev.farmings.creamery.CreameryParts.values()) {
			if ((kind.watts > 0) == powered) {
				out.add(CREAMERY_PARTS[kind.ordinal()]);
			}
		}
		return out;
	}

	public static Block creameryPart(dev.farmings.creamery.CreameryParts kind) {
		return CREAMERY_PARTS[kind.ordinal()];
	}

	public static Block parlorPart(ParlorParts kind) {
		return PARLOR_PARTS[kind.ordinal()];
	}

	public static Block part(BarnParts kind) {
		return PARTS[kind.ordinal()];
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Farmings.id(name));
		return factory.apply(BlockBehaviour.Properties.of().setId(key));
	}

	private static void add(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Farmings.id(name), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Farmings.id(name));
		Item.Properties properties = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
		Registry.register(BuiltInRegistries.ITEM, itemKey, block instanceof BarnPartBlock || block instanceof ParlorPartBlock || block instanceof dev.farmings.creamery.CreameryPartBlock ? new BigPartItem(block, properties) : new BlockItem(block, properties));
	}

	public static Component tabTitle() {
		return Component.translatable("itemGroup.farmings");
	}

	public static ItemStack tabIcon() {
		return new ItemStack(BARN_CONTROLLER);
	}

	/** Every item the creative tab shows. The tab itself is built in each loader, which has the builder that takes no argument. */
	public static void creativeTab(java.util.function.Consumer<net.minecraft.world.level.ItemLike> output) {
		output.accept(BARN_CONTROLLER);
		FarmingsItems.ALL.values().forEach(output);
		for (BarnParts kind : BarnParts.values()) {
			output.accept(part(kind));
		}
		output.accept(PARLOR_CONTROLLER);
		for (ParlorParts kind : ParlorParts.values()) {
			output.accept(parlorPart(kind));
		}
		output.accept(CREAMERY_CONTROLLER);
		for (dev.farmings.creamery.CreameryParts kind : dev.farmings.creamery.CreameryParts.values()) {
			output.accept(creameryPart(kind));
		}
		for (Block b : dev.farmings.coldroom.ColdRoomBlocks.ALL) {
			output.accept(b);
		}
		for (Block b : dev.farmings.shed.ShedBlocks.ALL) {
			output.accept(b);
		}
		for (Block b : dev.farmings.shearing.ShearingBlocks.ALL) {
			output.accept(b);
		}
		for (Block b : dev.farmings.woolmill.WoolMillBlocks.MACHINES.values()) {
			output.accept(b);
		}
	}

	public static void register() {
		add("barn_controller", BARN_CONTROLLER);
		for (BarnParts kind : BarnParts.values()) {
			add(kind.id, part(kind));
		}
		add("parlor_controller", PARLOR_CONTROLLER);
		for (ParlorParts kind : ParlorParts.values()) {
			add(kind.id, parlorPart(kind));
		}
		add("creamery_controller", CREAMERY_CONTROLLER);
		for (dev.farmings.creamery.CreameryParts kind : dev.farmings.creamery.CreameryParts.values()) {
			add(kind.id, creameryPart(kind));
		}
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("creamery_part"), CREAMERY_PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("creamery_machine"), CREAMERY_MACHINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("creamery_controller"), CREAMERY_CONTROLLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("parlor_part"), PARLOR_PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("parlor_controller"), PARLOR_CONTROLLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("vacuum_pump"), PUMP_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("bulk_tank"), TANK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("barn_part"), PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("barn_controller"), CONTROLLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("pit"), PIT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("scraper_drive"), DRIVE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("hay_feeder"), FEEDER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("water_trough"), TROUGH_ENTITY);
		dev.farmings.coldroom.ColdRoomBlocks.register();
		dev.farmings.shed.ShedBlocks.register();
		dev.farmings.shearing.ShearingBlocks.register();
		dev.farmings.woolmill.WoolMillBlocks.register();
		FarmingsItems.register();
	}
}

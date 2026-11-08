package dev.farmings;

import dev.farmings.barn.BarnControllerBlock;
import dev.farmings.barn.BarnControllerEntity;
import dev.farmings.barn.BarnPartBlock;
import dev.farmings.barn.BarnPartEntity;
import dev.farmings.barn.DriveEntity;
import dev.farmings.barn.PitEntity;
import dev.farmings.barn.BarnParts;
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
	private static final Block[] PARTS = new Block[BarnParts.values().length];

	static {
		for (BarnParts kind : BarnParts.values()) {
			PARTS[kind.ordinal()] = block(kind.id, props -> new BarnPartBlock(props.mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE), kind));
		}
	}

	public static final BlockEntityType<BarnPartEntity> PART_ENTITY = new BlockEntityType<>(BarnPartEntity::new, others());
	public static final BlockEntityType<PitEntity> PIT_ENTITY = new BlockEntityType<>(PitEntity::new, Set.of(part(BarnParts.COLLECTION_PIT)));
	public static final BlockEntityType<DriveEntity> DRIVE_ENTITY = new BlockEntityType<>(DriveEntity::new, Set.of(part(BarnParts.SCRAPER_DRIVE)));
	public static final BlockEntityType<BarnControllerEntity> CONTROLLER_ENTITY = new BlockEntityType<>(BarnControllerEntity::new, Set.of(BARN_CONTROLLER));

	private FarmingsBlocks() {
	}

	/** Every part whose main cell only remembers it has grown: all but the pit and the drive, which have their own entities. */
	private static Set<Block> others() {
		Set<Block> out = new HashSet<>();
		for (BarnParts kind : BarnParts.values()) {
			if (kind != BarnParts.COLLECTION_PIT && kind != BarnParts.SCRAPER_DRIVE) {
				out.add(PARTS[kind.ordinal()]);
			}
		}
		return out;
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
		Registry.register(BuiltInRegistries.ITEM, itemKey, block instanceof BarnPartBlock ? new BigPartItem(block, properties) : new BlockItem(block, properties));
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
		output.accept(FarmingsItems.MANURE_FORK);
		output.accept(FarmingsItems.MANURE);
		for (BarnParts kind : BarnParts.values()) {
			output.accept(part(kind));
		}
	}

	public static void register() {
		add("barn_controller", BARN_CONTROLLER);
		for (BarnParts kind : BarnParts.values()) {
			add(kind.id, part(kind));
		}
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("barn_part"), PART_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("barn_controller"), CONTROLLER_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("pit"), PIT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("scraper_drive"), DRIVE_ENTITY);
		FarmingsItems.register();
	}
}

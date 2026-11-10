package dev.farmings.woolmill;

import dev.farmings.Farmings;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The wool mill's machines (docs/multiblocks/wool-mill.md). */
public final class WoolMillBlocks {
	public static final Map<WoolMachine, Block> MACHINES = new EnumMap<>(WoolMachine.class);

	static {
		for (WoolMachine k : WoolMachine.values()) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Farmings.id(k.id));
			MACHINES.put(k, new WoolMachineBlock(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL), k));
		}
	}

	public static final BlockEntityType<WoolMachineEntity> MACHINE_ENTITY = new BlockEntityType<>(WoolMachineEntity::new, new HashSet<>(MACHINES.values()));

	private WoolMillBlocks() {
	}

	public static Block machine(WoolMachine k) {
		return MACHINES.get(k);
	}

	public static void register() {
		for (WoolMachine k : WoolMachine.values()) {
			Registry.register(BuiltInRegistries.BLOCK, Farmings.id(k.id), MACHINES.get(k));
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Farmings.id(k.id));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(MACHINES.get(k), new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		}
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Farmings.id("wool_machine"), MACHINE_ENTITY);
	}
}

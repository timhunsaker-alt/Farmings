package dev.farmings.item;

import dev.farmings.Farmings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Wool clothing (docs/wool.md): a knitted cap, sweater, trousers and socks woven from the mill's cloth. Protection is leather's, read from
 * vanilla's leather material *(proposed: close to leather)*. The pieces are in the freeze-immune wearables tag, so any one of them keeps
 * the cold of powder snow off, as leather does.
 */
public class WoolClothingItem extends Item {
	public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Farmings.id("wool"));
	public static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, Farmings.id("repairs_wool_clothing"));
	public static final ArmorMaterial MATERIAL = new ArmorMaterial(ArmorMaterials.LEATHER.durability(), ArmorMaterials.LEATHER.defense(),
			ArmorMaterials.LEATHER.enchantmentValue(), ArmorMaterials.LEATHER.equipSound(), ArmorMaterials.LEATHER.toughness(),
			ArmorMaterials.LEATHER.knockbackResistance(), REPAIRS, ASSET);

	public WoolClothingItem(Properties properties, ArmorType type) {
		super(properties.humanoidArmor(MATERIAL, type));
	}
}

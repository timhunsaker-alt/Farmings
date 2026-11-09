package dev.farmings.item;

import dev.farmings.Farmings;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

/**
 * The milk armor easter egg (docs/milk-armor.md, Tim 2026-11-07 and 2026-11-09). Made from milk buckets, it has netherite's protection,
 * toughness and durability, read from vanilla's netherite material so it follows the game. One game day (24,000 ticks) after it is made it
 * spoils: the name and look change, and while any spoiled piece is worn the wearer has Hunger and Nausea. Taking it off ends it. Nothing
 * stops the spoiling, and a spoiled piece keeps its durability (Tim's choices).
 */
public class MilkArmorItem extends Item {
	public static final long SPOIL_TICKS = 24_000;
	public static final ResourceKey<EquipmentAsset> FRESH = ResourceKey.create(EquipmentAssets.ROOT_ID, Farmings.id("milk"));
	public static final ResourceKey<EquipmentAsset> SPOILED = ResourceKey.create(EquipmentAssets.ROOT_ID, Farmings.id("milk_spoiled"));
	public static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, Farmings.id("repairs_milk_armor"));
	public static final ArmorMaterial MATERIAL = new ArmorMaterial(ArmorMaterials.NETHERITE.durability(), ArmorMaterials.NETHERITE.defense(),
			ArmorMaterials.NETHERITE.enchantmentValue(), ArmorMaterials.NETHERITE.equipSound(), ArmorMaterials.NETHERITE.toughness(),
			ArmorMaterials.NETHERITE.knockbackResistance(), REPAIRS, FRESH);
	private static final String MADE = "MilkMade";
	private static final String SPOILED_KEY = "MilkSpoiled";

	private final ArmorType type;

	public MilkArmorItem(Properties properties, ArmorType type) {
		super(properties.humanoidArmor(MATERIAL, type));
		this.type = type;
	}

	public ArmorType armorType() {
		return type;
	}

	public static boolean spoiled(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d != null && d.copyTag().getBooleanOr(SPOILED_KEY, false);
	}

	/** The game tick it was made, or {@link Long#MIN_VALUE} if the clock has not started (a made tick may be negative in tests). */
	public static long made(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d == null ? Long.MIN_VALUE : d.copyTag().getLongOr(MADE, Long.MIN_VALUE);
	}

	/** Starts the clock (the first tick it exists in a world) or moves it, for game tests. */
	public static void setMade(ItemStack stack, long tick) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong(MADE, tick));
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		long now = level.getGameTime();
		if (made(stack) == Long.MIN_VALUE) {
			setMade(stack, now);
		}
		if (!spoiled(stack) && now - made(stack) >= SPOIL_TICKS) {
			spoil(stack);
		}
		if (spoiled(stack) && slot != null && slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && entity instanceof LivingEntity wearer
				&& wearer.getItemBySlot(slot) == stack && (level.getGameTime() % 20 == 0 || !wearer.hasEffect(MobEffects.NAUSEA))) {
			sicken(wearer);
		}
	}

	/** Hunger and Nausea, refreshed each second while it is worn (they wear off a few seconds after it comes off). */
	public static void sicken(LivingEntity wearer) {
		wearer.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0, false, true));
		wearer.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, false, true));
	}

	/** Spoils it: a flag, the spoiled look (worn and in the hand) and the spoiled name. Durability is kept. */
	public static void spoil(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean(SPOILED_KEY, true));
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(true), List.of(), List.of()));
		Equippable fresh = stack.get(DataComponents.EQUIPPABLE);
		if (fresh != null) {
			stack.set(DataComponents.EQUIPPABLE, Equippable.builder(fresh.slot()).setEquipSound(fresh.equipSound()).setAsset(SPOILED).build());
		}
	}

	@Override
	public Component getName(ItemStack stack) {
		return spoiled(stack) ? Component.translatable(getDescriptionId() + ".spoiled") : super.getName(stack);
	}

	@Override
	public void onCraftedBy(ItemStack stack, Player player) {
		super.onCraftedBy(stack, player);
		if (made(stack) == Long.MIN_VALUE) {
			setMade(stack, player.level().getGameTime());
		}
	}
}

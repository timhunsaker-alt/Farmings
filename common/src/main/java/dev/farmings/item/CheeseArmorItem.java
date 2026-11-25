package dev.farmings.item;

import dev.farmings.Farmings;
import dev.farmings.FarmingsItems;
import dev.farmings.process.creamery.Creamery;
import dev.farmings.process.creamery.Dairy;
import dev.farmings.process.food.Spoilage;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Cheese armor (docs/cheese-armor.md, Tim 2026-11-25: "turning milk armor into cheese armor its weaker but lasts longer and is edible").
 * A fresh milk armor piece set in the creamery's cheese vat and pressed in its cheese press comes out as the matching cheese piece.
 * <ul>
 *   <li>Weaker: iron's protection, toughness, durability and enchantability, read from vanilla's iron material so it follows the game.</li>
 *   <li>Lasts longer: it keeps as a cheddar wheel does out of the cold (food.md, 14 days, 336,000 ticks) and then goes moldy: the name,
 *       look and food change, and while a moldy piece is worn the wearer has Hunger and Nausea, as with spoiled milk armor. Its clock is the
 *       food clock ({@link Perishables}), so a cold room holds it back as it does a wheel.</li>
 *   <li>Edible: sneak-use it in the hand to eat it, for the hunger of the cheddar it holds. A moldy piece eats as moldy cheese.</li>
 * </ul>
 */
public class CheeseArmorItem extends Item {
	/** The cheese it keeps as: a whole cheddar wheel (food.md). */
	public static final Spoilage.Food FOOD = Spoilage.Food.CHEDDAR_WHEEL;
	/** The room-temperature life: 14 game days, 336,000 ticks, 14 times milk armor's one day. */
	public static final long SPOIL_TICKS = FOOD.roomTicks();
	public static final ResourceKey<EquipmentAsset> FRESH = ResourceKey.create(EquipmentAssets.ROOT_ID, Farmings.id("cheese"));
	public static final ResourceKey<EquipmentAsset> MOLDY = ResourceKey.create(EquipmentAssets.ROOT_ID, Farmings.id("cheese_moldy"));
	public static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, Farmings.id("repairs_cheese_armor"));
	public static final ArmorMaterial MATERIAL = new ArmorMaterial(ArmorMaterials.IRON.durability(), ArmorMaterials.IRON.defense(),
			ArmorMaterials.IRON.enchantmentValue(), ArmorMaterials.IRON.equipSound(), ArmorMaterials.IRON.toughness(),
			ArmorMaterials.IRON.knockbackResistance(), REPAIRS, FRESH);

	/** A cheddar slice's hunger (creamery.md: a wheel crafts into 8 slices, 3 hunger each). */
	public static final int CHEDDAR_SLICE_HUNGER = 3;
	public static final int SLICES_PER_WHEEL = 8;
	/** Cheddar's saturation modifier, as the slice. */
	public static final float CHEDDAR_SATURATION = 0.7F;
	/** Hunger per kg of cheddar: 8 slices x 3 hunger from a 10 kg wheel, 2.4. */
	public static final double HUNGER_PER_KG = SLICES_PER_WHEEL * CHEDDAR_SLICE_HUNGER / Creamery.WHEEL_KG;
	/**
	 * The cheddar in one unit of the piece: each unit was a milk bucket, 10 L at the creamery, made into cheddar by the Van Slyke yield at
	 * Holstein's fat and protein (a vanilla bucket counts as Holstein milk, creamery.md). About 1.04 kg.
	 */
	public static final double KG_PER_UNIT = Dairy.cheeseKg(10, Creamery.DEFAULT_FAT, Creamery.DEFAULT_PROTEIN);

	private static final String MOLDY_KEY = "CheeseMoldy";

	private final ArmorType type;

	public CheeseArmorItem(Properties properties, ArmorType type) {
		super(properties.humanoidArmor(MATERIAL, type).food(food(type), Consumables.defaultFood().build()));
		this.type = type;
	}

	public ArmorType armorType() {
		return type;
	}

	/** Material units in the piece, as the vanilla armor recipes: helmet 5, chestplate 8, leggings 7, boots 4. */
	public static int units(ArmorType type) {
		return switch (type) {
			case HELMET -> 5;
			case CHESTPLATE -> 8;
			case LEGGINGS -> 7;
			case BOOTS -> 4;
			default -> 0;
		};
	}

	public static double cheeseKg(ArmorType type) {
		return units(type) * KG_PER_UNIT;
	}

	/** Hunger for eating the piece fresh: its cheddar's kg x 2.4, rounded. Helmet 12, chestplate 20, leggings 17, boots 10. */
	public static int hunger(ArmorType type) {
		return (int) Math.round(cheeseKg(type) * HUNGER_PER_KG);
	}

	private static FoodProperties food(ArmorType type) {
		return new FoodProperties.Builder().nutrition(hunger(type)).saturationModifier(CHEDDAR_SATURATION).build();
	}

	/** The cheese piece a milk piece presses into. */
	public static Item from(MilkArmorItem milk) {
		return switch (milk.armorType()) {
			case HELMET -> FarmingsItems.CHEESE_HELMET;
			case CHESTPLATE -> FarmingsItems.CHEESE_CHESTPLATE;
			case LEGGINGS -> FarmingsItems.CHEESE_LEGGINGS;
			default -> FarmingsItems.CHEESE_BOOTS;
		};
	}

	/**
	 * The cheese piece pressed from {@code milk} at {@code now}: the same share of durability left, and its enchantments. Its clock starts
	 * now, as food leaving the creamery does (food.md).
	 */
	public static ItemStack pressed(ItemStack milk, long now) {
		ItemStack out = new ItemStack(from((MilkArmorItem) milk.getItem()));
		if (milk.isDamaged() && milk.getMaxDamage() > 0) {
			out.setDamageValue((int) Math.round((double) milk.getDamageValue() / milk.getMaxDamage() * out.getMaxDamage()));
		}
		var ench = milk.get(DataComponents.ENCHANTMENTS);
		if (ench != null) {
			out.set(DataComponents.ENCHANTMENTS, ench);
		}
		Perishables.stamp(out, now);
		return out;
	}

	// ---- the clock ----

	public static boolean moldy(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d != null && d.copyTag().getBooleanOr(MOLDY_KEY, false);
	}

	/** Sets the clock as if made at {@code tick} (game tests and client scenes). */
	public static void setMade(ItemStack stack, long tick) {
		Perishables.setSpoilsAt(stack, tick + SPOIL_TICKS);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		long now = level.getGameTime();
		Perishables.stamp(stack, now);
		if (!moldy(stack) && Spoilage.spoiled(Perishables.spoilsAt(stack), now)) {
			mold(stack);
		}
		if (moldy(stack) && slot != null && slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && entity instanceof LivingEntity wearer
				&& wearer.getItemBySlot(slot) == stack && (now % 20 == 0 || !wearer.hasEffect(MobEffects.NAUSEA))) {
			MilkArmorItem.sicken(wearer);
		}
	}

	/** Goes moldy: a flag, the moldy look (worn and in the hand), the moldy name, and moldy cheese's food. Durability is kept. */
	public static void mold(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean(MOLDY_KEY, true));
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(true), List.of(), List.of()));
		Equippable fresh = stack.get(DataComponents.EQUIPPABLE);
		if (fresh != null) {
			stack.set(DataComponents.EQUIPPABLE, Equippable.builder(fresh.slot()).setEquipSound(fresh.equipSound()).setAsset(MOLDY).build());
		}
		ItemStack mould = new ItemStack(FarmingsItems.MOLDY_CHEESE);
		stack.set(DataComponents.FOOD, mould.get(DataComponents.FOOD));
		stack.set(DataComponents.CONSUMABLE, mould.get(DataComponents.CONSUMABLE));
	}

	// ---- eating ----

	/** True if this very stack is in one of the entity's armor slots: a worn piece is never eaten. */
	public static boolean worn(LivingEntity entity, ItemStack stack) {
		for (EquipmentSlot s : EquipmentSlot.values()) {
			if (s.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && entity.getItemBySlot(s) == stack) {
				return true;
			}
		}
		return false;
	}

	/** Sneak-use eats it (the vanilla eating animation and sound); plain use puts it on, as any armor. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSecondaryUseActive()) {
			Consumable eat = stack.get(DataComponents.CONSUMABLE);
			if (eat == null || worn(player, stack)) {
				return InteractionResult.FAIL;
			}
			return eat.startConsuming(player, stack, hand);
		}
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null && equippable.swappable() ? equippable.swapWithEquipmentSlot(stack, player) : InteractionResult.PASS;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		return worn(entity, stack) ? stack : super.finishUsingItem(stack, level, entity);
	}

	@Override
	public Component getName(ItemStack stack) {
		return moldy(stack) ? Component.translatable(getDescriptionId() + ".moldy") : super.getName(stack);
	}
}

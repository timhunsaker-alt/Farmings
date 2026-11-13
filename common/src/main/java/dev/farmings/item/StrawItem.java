package dev.farmings.item;

import dev.farmings.FarmingsItems;
import dev.farmings.entity.CattleEntity;
import dev.farmings.entity.FowlEntity;
import dev.farmings.entity.GoatEntity;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.husbandry.SheepStage;
import dev.farmings.husbandry.Stage;
import dev.farmings.husbandry.TimeScale;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * Artificial insemination, one straw for every species (docs/animals/breeding.md, "one straw for every species"). An empty straw used
 * on a grown bull, ram, buck or tom becomes a semen straw holding his genome; the genome says the species. A semen straw used on a
 * female of that species in heat (a turkey hen needs no heat) inseminates her and is used up. Bull, ram and buck straws keep forever
 * *(proposed)*;
 * turkey semen is fresh and spoils 30 real minutes after collection (TNAU; poultry.md), turning back into an empty straw.
 */
public class StrawItem extends Item {
	private static final String KEY_GENOME = "Genome";
	/** The sire's coat or plumage genes (VisualGenome, SheepGenes or PoultryGenes, saved); a straw filled before these existed has none. */
	private static final String KEY_COAT = "Coat";
	private static final String KEY_SIRE = "Sire";
	private static final String KEY_HERD = "Herd";
	/** The tick a turkey straw was filled. */
	private static final String KEY_TAKEN = "Taken";
	/** Fresh turkey semen is used "within 30 minutes after collection" (TNAU): 30 real minutes at the poultry scale. */
	public static final long TURKEY_FRESH_TICKS = TimeScale.FOWL.realDaysToTicks(30.0 / (24 * 60));
	private final boolean filled;

	public StrawItem(Properties properties, boolean filled) {
		super(properties);
		this.filled = filled;
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (!(target instanceof CattleEntity || target instanceof SheepEntity || target instanceof GoatEntity || target instanceof FowlEntity)) {
			return InteractionResult.PASS;
		}
		if (player.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		return filled ? inseminate(stack, player, target) : collect(stack, player, target);
	}

	private InteractionResult collect(ItemStack stack, Player player, LivingEntity male) {
		Genome g;
		int[] coat;
		String herd;
		if (male instanceof CattleEntity bull && bull.sex() == Sex.MALE && bull.stage() == Stage.BULL && !bull.isWild() && bull.genome() != null) {
			g = bull.genome();
			coat = bull.visualGenome().save();
			herd = CattleEntity.herdId(g);
		} else if (male instanceof SheepEntity ram && ram.sex() == Sex.MALE && ram.stage() == SheepStage.RAM && !ram.isWild() && ram.genome() != null) {
			g = ram.genome();
			coat = ram.genes().save();
			herd = ram.herdId();
		} else if (male instanceof GoatEntity buck && buck.sex() == Sex.MALE && buck.stage() == dev.farmings.husbandry.GoatStage.BUCK && !buck.isWild()
				&& buck.genome() != null) {
			g = buck.genome();
			coat = buck.genes().save();
			herd = buck.herdId();
		} else if (male instanceof FowlEntity tom && tom.species() == Species.TURKEY && tom.sex() == Sex.MALE && tom.adult() && !tom.isWild()
				&& tom.genome() != null) {
			g = tom.genome();
			coat = tom.genes().save();
			herd = tom.herdId();
		} else {
			player.sendOverlayMessage(Component.translatable("message.farmings.straw.not_a_sire"));
			return InteractionResult.FAIL;
		}
		long now = player.level().getGameTime();
		ItemStack full = new ItemStack(FarmingsItems.SEMEN_STRAW);
		CustomData.update(DataComponents.CUSTOM_DATA, full, tag -> {
			tag.putString(KEY_GENOME, g.encode());
			tag.putIntArray(KEY_COAT, coat);
			tag.putString(KEY_SIRE, male.getUUID().toString());
			tag.putString(KEY_HERD, herd);
			if (g.species() == Species.TURKEY) {
				tag.putLong(KEY_TAKEN, now);
			}
		});
		stack.consume(1, player);
		if (!player.getInventory().add(full)) {
			player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), full));
		}
		return InteractionResult.SUCCESS;
	}

	private InteractionResult inseminate(ItemStack stack, Player player, LivingEntity female) {
		Genome sire = genomeOf(stack);
		if (sire == null) {
			player.sendOverlayMessage(Component.translatable("message.farmings.straw.not_in_heat"));
			return InteractionResult.FAIL;
		}
		Species want = switch (female) {
			case CattleEntity c -> Species.CATTLE;
			case SheepEntity s -> Species.SHEEP;
			case GoatEntity g -> Species.GOAT;
			case FowlEntity f -> f.species();
			default -> null;
		};
		if (want == null) {
			return InteractionResult.PASS;
		}
		if (sire.species() != want) {
			player.sendOverlayMessage(Component.translatable("message.farmings.straw.wrong_species", speciesName(sire.species())));
			return InteractionResult.FAIL;
		}
		long now = player.level().getGameTime();
		CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
		int[] coat = tag.getIntArray(KEY_COAT).orElse(null);
		boolean ok = switch (female) {
			case CattleEntity cow -> {
				if (cow.sex() != Sex.FEMALE || !cow.canConceive()) {
					yield false;
				}
				dev.farmings.genetics.VisualGenome vg = coat == null ? null : dev.farmings.genetics.VisualGenome.load(coat);
				yield vg == null ? cow.conceive(sire, now) : cow.conceive(sire, vg, now);
			}
			case SheepEntity ewe -> {
				dev.farmings.genetics.SheepGenes sg = coat == null ? null : dev.farmings.genetics.SheepGenes.load(coat);
				yield ewe.sex() == Sex.FEMALE && ewe.canConceive() && sg != null && ewe.conceive(sire, sg, now);
			}
			case GoatEntity doe -> {
				dev.farmings.genetics.GoatGenes gg = coat == null ? null : dev.farmings.genetics.GoatGenes.load(coat);
				yield doe.sex() == Sex.FEMALE && doe.canConceive() && gg != null && doe.conceive(sire, gg, now);
			}
			case FowlEntity hen -> {
				if (stale(stack, now)) {
					player.sendOverlayMessage(Component.translatable("message.farmings.straw.stale"));
					yield false;
				}
				dev.farmings.genetics.PoultryGenes pg = coat == null ? null : dev.farmings.genetics.PoultryGenes.load(coat);
				yield pg != null && hen.inseminate(sire, pg, tag.getStringOr(KEY_HERD, ""));
			}
			default -> false;
		};
		if (!ok) {
			if (!(female instanceof FowlEntity)) {
				player.sendOverlayMessage(Component.translatable("message.farmings.straw.not_in_heat"));
			}
			return InteractionResult.FAIL;
		}
		if (female instanceof CattleEntity cow && cow.owner() == null) {
			cow.setOwner(player.getUUID());
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	/** A turkey straw past its 30 minutes. Bull and ram straws never go stale. */
	public static boolean stale(ItemStack stack, long now) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return false;
		}
		long taken = data.copyTag().getLongOr(KEY_TAKEN, Long.MIN_VALUE);
		return taken != Long.MIN_VALUE && now - taken > TURKEY_FRESH_TICKS;
	}

	/** Stale turkey semen in an inventory turns back into an empty straw. */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		if (!filled || !stale(stack, level.getGameTime()) || !(entity instanceof Player player)) {
			return;
		}
		var inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i) == stack) {
				inv.setItem(i, new ItemStack(FarmingsItems.EMPTY_STRAW, stack.getCount()));
				return;
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		Genome g = genomeOf(stack);
		if (g == null) {
			return;
		}
		String herd = stack.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr(KEY_HERD, "");
		out.accept(Component.translatable("tooltip.farmings.straw", speciesName(g.species()), herd.isEmpty() ? "?" : herd));
		if (g.species() == Species.TURKEY) {
			out.accept(Component.translatable("tooltip.farmings.straw.fresh"));
		}
	}

	private static Component speciesName(Species s) {
		return Component.translatable("species.farmings." + s.name().toLowerCase(Locale.ROOT));
	}

	/** The sire's genome stored in a semen straw, or null for an empty or damaged one. */
	public static Genome genomeOf(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		String text = data.copyTag().getStringOr(KEY_GENOME, "");
		if (text.isEmpty()) {
			return null;
		}
		try {
			return Genome.decode(text);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}

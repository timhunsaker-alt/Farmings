package dev.farmings.health;

import dev.farmings.Farmings;
import dev.farmings.husbandry.health.Drug;
import dev.farmings.husbandry.health.Scc;
import dev.farmings.husbandry.health.Vaccine;
import dev.farmings.process.food.Spoilage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Animal health's items (docs/animal-health.md, "Blocks, items and multiblocks"). */
public final class HealthItems {
	public static final Map<String, Item> ALL = new LinkedHashMap<>();

	public static final Item VET_KIT = item("vet_kit", p -> new VetKitItem(p.durability(256)));
	public static final Item CMT_PADDLE = item("cmt_paddle", p -> new CmtPaddleItem(p.stacksTo(1)));
	public static final Item CMT_REAGENT = item("cmt_reagent", Item::new);
	public static final Item ANTIBIOTIC = item("antibiotic", Item::new);
	public static final Item MASTITIS_TUBE = item("mastitis_tube", p -> new DrugItem(p.stacksTo(16), Drug.MASTITIS_TUBE));
	public static final Item ANTIBIOTIC_INJECTION = item("antibiotic_injection", p -> new DrugItem(p.stacksTo(16), Drug.INJECTION));
	public static final Item ELECTROLYTES = item("electrolytes", p -> new DrugItem(p.stacksTo(16), Drug.ELECTROLYTES));
	public static final Item COCCIDIOSTAT = item("coccidiostat", p -> new DrugItem(p.stacksTo(16), Drug.COCCIDIOSTAT));
	public static final Item TEAT_DIP = item("teat_dip", p -> new TeatDipItem(p.stacksTo(16)));
	public static final Item ZINC_SULFATE = item("zinc_sulfate", p -> new Item(p.stacksTo(16)));
	public static final Item VACCINE_CDT = item("vaccine_cdt", p -> new VaccineItem(p.stacksTo(16), Vaccine.CDT));
	public static final Item VACCINE_RESPIRATORY = item("vaccine_respiratory", p -> new VaccineItem(p.stacksTo(16), Vaccine.RESPIRATORY));
	public static final Item VACCINE_ERYSIPELAS = item("vaccine_erysipelas", p -> new VaccineItem(p.stacksTo(16), Vaccine.ERYSIPELAS));
	public static final Item VACCINE_MAREKS = item("vaccine_mareks", p -> new VaccineItem(p.stacksTo(16), Vaccine.MAREKS));
	public static final Item VACCINE_STRANGLES = item("vaccine_strangles", p -> new VaccineItem(p.stacksTo(16), Vaccine.STRANGLES));
	public static final Item SPOILED_VACCINE = item("spoiled_vaccine", Item::new);

	/** One teat dip bottle dips 50 cows (proposed). */
	public static final int DIPS_PER_BOTTLE = 50;
	/** A hatchery's vial is 100 Marek's doses (proposed). */
	public static final int MAREKS_DOSES = 100;

	private HealthItems() {
	}

	private static Item item(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Farmings.id(name));
		Item i = factory.apply(new Item.Properties().setId(key));
		ALL.put(name, i);
		return i;
	}

	public static void register() {
		ALL.forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, Farmings.id(name), item));
	}

	public static Item vaccine(Vaccine v) {
		return switch (v) {
			case CDT -> VACCINE_CDT;
			case RESPIRATORY -> VACCINE_RESPIRATORY;
			case ERYSIPELAS -> VACCINE_ERYSIPELAS;
			case MAREKS -> VACCINE_MAREKS;
			case STRANGLES -> VACCINE_STRANGLES;
		};
	}

	/**
	 * The vet kit: on an animal, an exam (sneak: report a notifiable disease it has, or send it to a hospital pen); on a housing or isolation
	 * controller, a herd check of its members.
	 */
	public static class VetKitItem extends Item {
		public VetKitItem(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
			if (!(target instanceof HealthHost h)) {
				return InteractionResult.PASS;
			}
			if (player.level() instanceof ServerLevel level) {
				String msg;
				if (player.isShiftKeyDown()) {
					msg = h.health().notifiable() != null ? Health.report(level, h) : IsolationHold.sendToHospital(level, h);
				} else {
					msg = Health.exam(level, h);
					level.playSound(null, target.blockPosition(), HealthSounds.VET_KIT, net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.0F);
				}
				player.sendSystemMessage(Component.literal(msg));
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult useOn(UseOnContext ctx) {
			if (!(ctx.getLevel() instanceof ServerLevel level) || ctx.getPlayer() == null) {
				return InteractionResult.PASS;
			}
			var be = level.getBlockEntity(ctx.getClickedPos());
			if (be == null || !housing(be)) {
				return InteractionResult.PASS;
			}
			List<String> lines = Health.herdCheck(level, ctx.getClickedPos());
			Player player = ctx.getPlayer();
			player.sendSystemMessage(Component.literal("Herd check at " + ctx.getClickedPos().toShortString() + ":"));
			for (String l : lines) {
				player.sendSystemMessage(Component.literal(" - " + l));
			}
			ItemStack other = player.getItemInHand(ctx.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
			if (other.is(dev.farmings.FarmingsItems.HERD_BOOK)) {
				dev.farmings.item.HerdBookItem.writeHerdCheck(other, ctx.getClickedPos(), level.getGameTime(), lines);
				player.sendOverlayMessage(Component.literal("Herd check written into the herd book"));
			}
			return InteractionResult.SUCCESS;
		}

		private static boolean housing(net.minecraft.world.level.block.entity.BlockEntity be) {
			return be instanceof dev.farmings.barn.BarnControllerEntity || be instanceof dev.farmings.shed.ShedControllerEntity
					|| be instanceof dev.farmings.coop.CoopControllerEntity || be instanceof dev.farmings.piggery.PigBarnControllerEntity
					|| be instanceof dev.farmings.stable.StableControllerEntity || be instanceof IsolationController;
		}
	}

	/** The California Mastitis Test paddle: on an animal in milk, with a reagent bottle in the inventory, the CMT score of her milk. */
	public static class CmtPaddleItem extends Item {
		public CmtPaddleItem(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
			if (!(target instanceof HealthHost h)) {
				return InteractionResult.PASS;
			}
			if (player.level() instanceof ServerLevel level) {
				if (!h.inMilk()) {
					player.sendSystemMessage(Component.literal("Not in milk: nothing to test"));
					return InteractionResult.SUCCESS;
				}
				int slot = reagentSlot(player);
				if (slot < 0 && !player.getAbilities().instabuild) {
					player.sendSystemMessage(Component.literal("No CMT reagent in your inventory"));
					return InteractionResult.SUCCESS;
				}
				if (slot >= 0 && !player.getAbilities().instabuild) {
					player.getInventory().getItem(slot).shrink(1);
				}
				String cmt = Scc.cmt(h.health().scc());
				h.health().event(level.getGameTime(), "CMT " + cmt);
				level.playSound(null, target.blockPosition(), HealthSounds.CMT, net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.0F);
				player.sendSystemMessage(Component.literal("CMT " + h.herdId() + ": " + cmt + " (" + Scc.band(cmt) + ")"
						+ (cmt.equals("N") ? "" : cmt.equals("T") ? ", trace: watch her" : ", mastitis likely")));
			}
			return InteractionResult.SUCCESS;
		}

		private static int reagentSlot(Player player) {
			var inv = player.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				if (inv.getItem(i).is(CMT_REAGENT)) {
					return i;
				}
			}
			return -1;
		}
	}

	/** A course of a drug, given to one animal (the coccidiostat also on a coop controller: every bird drinking there). */
	public static class DrugItem extends Item {
		public final Drug drug;

		public DrugItem(Properties p, Drug drug) {
			super(p);
			this.drug = drug;
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
			if (!(target instanceof HealthHost h)) {
				return InteractionResult.PASS;
			}
			if (player.level() instanceof ServerLevel level) {
				player.sendSystemMessage(Component.literal(Health.treat(level, h, drug)));
				stack.consume(1, player);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult useOn(UseOnContext ctx) {
			if (drug != Drug.COCCIDIOSTAT || !(ctx.getLevel() instanceof ServerLevel level)
					|| !(level.getBlockEntity(ctx.getClickedPos()) instanceof dev.farmings.coop.CoopControllerEntity coop)) {
				return InteractionResult.PASS;
			}
			int n = 0;
			for (var f : coop.members(level)) {
				Health.treat(level, f, drug);
				n++;
			}
			if (ctx.getPlayer() != null) {
				ctx.getPlayer().sendSystemMessage(Component.literal("Coccidiostat in the coop's water: " + n + " birds dosed; meat withheld 5 real days"));
				ctx.getItemInHand().consume(1, ctx.getPlayer());
			}
			return InteractionResult.SUCCESS;
		}
	}

	/** A vaccine dose (perishable: it keeps only in the cold room). Marek's goes into a hatchery's incubator instead. */
	public static class VaccineItem extends dev.farmings.item.PerishableItem {
		public final Vaccine vaccine;

		public VaccineItem(Properties p, Vaccine v) {
			super(Spoilage.Food.VACCINE, p);
			this.vaccine = v;
		}

		@Override
		public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
			if (!(target instanceof HealthHost h)) {
				return InteractionResult.PASS;
			}
			if (player.level() instanceof ServerLevel level) {
				if (!vaccine.species.contains(h.species())) {
					player.sendSystemMessage(Component.literal("The " + vaccine.label + " vaccine is not for this animal"));
					return InteractionResult.SUCCESS;
				}
				if (vaccine == Vaccine.MAREKS) {
					player.sendSystemMessage(Component.literal("Marek's vaccine works only at hatch: put it in the hatchery's incubator"));
					return InteractionResult.SUCCESS;
				}
				player.sendSystemMessage(Component.literal(Health.vaccinate(level, h, vaccine)));
				stack.consume(1, player);
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		public InteractionResult useOn(UseOnContext ctx) {
			if (vaccine != Vaccine.MAREKS || !(ctx.getLevel() instanceof ServerLevel level)
					|| !(level.getBlockEntity(ctx.getClickedPos()) instanceof dev.farmings.hatchery.IncubatorEntity inc)) {
				return InteractionResult.PASS;
			}
			inc.addMareksDoses(MAREKS_DOSES);
			if (ctx.getPlayer() != null) {
				ctx.getPlayer().sendSystemMessage(Component.literal("Marek's vaccine loaded: " + inc.mareksDoses() + " doses; every chick that hatches here is vaccinated"));
				ctx.getItemInHand().consume(1, ctx.getPlayer());
			}
			return InteractionResult.SUCCESS;
		}
	}

	/** A bottle of post-milking teat dip: into a parlor controller, 50 dips. */
	public static class TeatDipItem extends Item {
		public TeatDipItem(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult useOn(UseOnContext ctx) {
			if (!(ctx.getLevel() instanceof ServerLevel level)
					|| !(level.getBlockEntity(ctx.getClickedPos()) instanceof dev.farmings.parlor.ParlorControllerEntity parlor)) {
				return InteractionResult.PASS;
			}
			parlor.addTeatDip(DIPS_PER_BOTTLE);
			dev.wildcatter.quest.QuestEvents.milestone(level, ctx.getClickedPos(), dev.farmings.quest.Milestones.TEAT_DIP);
			if (ctx.getPlayer() != null) {
				ctx.getPlayer().sendSystemMessage(Component.literal("Teat dip in the parlor: " + parlor.teatDips() + " cows"));
				ctx.getItemInHand().consume(1, ctx.getPlayer());
			}
			return InteractionResult.SUCCESS;
		}
	}
}

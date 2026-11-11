package dev.farmings.entity;

import dev.farmings.FarmingsEntities;
import dev.farmings.FarmingsItems;
import dev.farmings.genetics.BreedingSeed;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.Offspring;
import dev.farmings.genetics.PoultryGenes;
import dev.farmings.genetics.Ruleset;
import dev.farmings.genetics.Species;
import dev.farmings.husbandry.poultry.Clutch;
import dev.farmings.husbandry.poultry.Fowl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

/**
 * Eggs (docs/eggs.md): one item per species, carrying weight, shell shade, the tick it was laid, and, for a fertile egg, both parents'
 * genomes and plumage genes, so the chick's genes are settled at laying. Also the carcass a bird leaves.
 */
public final class FowlEggs {
	private FowlEggs() {
	}

	public static final String GRAMS = "Grams";
	public static final String SHADE = "Shade";
	public static final String LAID = "Laid";
	public static final String FERTILE = "Fertile";
	public static final String FLOOR = "Floor";
	public static final String DAM = "Dam";
	public static final String DAM_PLUMAGE = "DamPlumage";
	public static final String SIRE = "Sire";
	public static final String SIRE_PLUMAGE = "SirePlumage";
	public static final String DAM_ID = "DamId";
	public static final String SIRE_ID = "SireId";
	/** Real days of warmth an egg has had (an incubator or a broody hen). */
	public static final String WARM_DAYS = "WarmDays";

	public static Item item(Species s) {
		return switch (s) {
			case TURKEY -> FarmingsItems.TURKEY_EGG;
			case QUAIL -> FarmingsItems.QUAIL_EGG;
			case DUCK -> FarmingsItems.DUCK_EGG;
			default -> FarmingsItems.CHICKEN_EGG;
		};
	}

	public static @Nullable Species species(ItemStack stack) {
		if (stack.is(FarmingsItems.CHICKEN_EGG)) {
			return Species.CHICKEN;
		}
		if (stack.is(FarmingsItems.TURKEY_EGG)) {
			return Species.TURKEY;
		}
		if (stack.is(FarmingsItems.QUAIL_EGG)) {
			return Species.QUAIL;
		}
		return stack.is(FarmingsItems.DUCK_EGG) ? Species.DUCK : null;
	}

	public static ItemStack egg(Species s, double grams, int shade, long laidTick, @Nullable Genome dam, @Nullable PoultryGenes damPlumage, String sire,
			int[] sirePlumage, String damId, String sireId) {
		ItemStack stack = new ItemStack(item(s));
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
			t.putDouble(GRAMS, Math.round(grams * 10) / 10.0);
			t.putInt(SHADE, shade);
			t.putLong(LAID, laidTick);
			boolean fertile = dam != null && damPlumage != null && !sire.isEmpty();
			t.putBoolean(FERTILE, fertile);
			if (fertile) {
				t.putString(DAM, dam.encode());
				t.putIntArray(DAM_PLUMAGE, damPlumage.save());
				t.putString(SIRE, sire);
				t.putIntArray(SIRE_PLUMAGE, sirePlumage);
				t.putString(DAM_ID, damId);
				t.putString(SIRE_ID, sireId);
			}
		});
		return stack;
	}

	public static CompoundTag tag(ItemStack stack) {
		CustomData d = stack.get(DataComponents.CUSTOM_DATA);
		return d == null ? new CompoundTag() : d.copyTag();
	}

	public static double grams(ItemStack stack) {
		return tag(stack).getDoubleOr(GRAMS, 0);
	}

	public static int shade(ItemStack stack) {
		return tag(stack).getIntOr(SHADE, 0);
	}

	public static long laid(ItemStack stack) {
		return tag(stack).getLongOr(LAID, 0L);
	}

	public static boolean fertile(ItemStack stack) {
		return tag(stack).getBooleanOr(FERTILE, false);
	}

	public static boolean floor(ItemStack stack) {
		return tag(stack).getBooleanOr(FLOOR, false);
	}

	public static double warmDays(ItemStack stack) {
		return tag(stack).getDoubleOr(WARM_DAYS, 0);
	}

	public static void setWarmDays(ItemStack stack, double days) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putDouble(WARM_DAYS, days));
	}

	/** An egg laid outside a nest is a floor (dirty) egg, graded B at best (docs/multiblocks/coop.md). */
	public static ItemStack markFloor(ItemStack egg) {
		CustomData.update(DataComponents.CUSTOM_DATA, egg, t -> t.putBoolean(FLOOR, true));
		return egg;
	}

	/** Can this egg still hatch? Fertile, and either started or not left unwarmed past the limit (poultry.md). */
	public static boolean hatchable(ItemStack stack, long now) {
		if (!fertile(stack)) {
			return false;
		}
		if (warmDays(stack) > 0) {
			return true;
		}
		return Clutch.hatchable(dev.farmings.husbandry.TimeScale.FOWL.ticksToRealDays(now - laid(stack)));
	}

	/**
	 * Hatches a fertile egg into a bird at {@code pos}: an Offspring of the two genomes, plumage genes inherited from both, the sex drawn by
	 * the Offspring roll. Returns null for an infertile egg.
	 */
	public static @Nullable FowlEntity hatch(ServerLevel level, ItemStack egg, BlockPos pos, java.util.@Nullable UUID owner) {
		Species s = species(egg);
		CompoundTag t = tag(egg);
		if (s == null || !t.getBooleanOr(FERTILE, false)) {
			return null;
		}
		Genome dam = Genome.decode(t.getStringOr(DAM, ""));
		Genome sire = Genome.decode(t.getStringOr(SIRE, ""));
		long now = level.getGameTime();
		String world = Long.toString(level.getSeed());
		String eventId = "hatch:" + dam.hash() + ":" + t.getLongOr(LAID, 0L) + ":" + t.getDoubleOr(GRAMS, 0);
		Offspring.Result r = Offspring.create(dam, sire, eventId, world, Ruleset.defaults(), null);
		FowlEntity chick = FarmingsEntities.fowl(s).create(level, EntitySpawnReason.BREEDING);
		if (chick == null) {
			return null;
		}
		PoultryGenes damP = PoultryGenes.load(t.getIntArray(DAM_PLUMAGE).orElse(null));
		PoultryGenes sireP = PoultryGenes.load(t.getIntArray(SIRE_PLUMAGE).orElse(null));
		String damCode = FowlEntity.topBreed(dam.breedCompositionBp(), s).code;
		String sireCode = FowlEntity.topBreed(sire.breedCompositionBp(), s).code;
		PoultryGenes plumage = PoultryGenes.inherit(damP != null ? damP : PoultryGenes.founder(damCode, dev.farmings.genetics.Sex.FEMALE, 0),
				sireP != null ? sireP : PoultryGenes.founder(sireCode, dev.farmings.genetics.Sex.MALE, 0), r.sex(),
				BreedingSeed.seed(world, eventId, dam.hash(), sire.hash(), Ruleset.defaults().version()));
		chick.initHatchling(r, plumage, now);
		chick.setOwner(owner);
		chick.setDamId(t.getStringOr(DAM_ID, ""));
		chick.setSireId(t.getStringOr(SIRE_ID, ""));
		chick.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0);
		level.addFreshEntity(chick);
		return chick;
	}

	/** What a killed bird leaves: meat by dressed weight (chicken: the vanilla items), feathers, and down from a duck. */
	static void dropCarcass(ServerLevel level, FowlEntity bird, boolean cooked) {
		if (bird.young()) {
			return;
		}
		int items = Fowl.meatItems(bird.composition(), bird.weightKg());
		Item meat = FarmingsItems.fowlMeat(bird.species(), cooked);
		if (meat != null) {
			while (items > 0) {
				int n = Math.min(64, items);
				bird.spawnAtLocation(level, dev.farmings.item.Perishables.stamp(new ItemStack(meat, n), level.getGameTime()));
				items -= n;
			}
		}
		int feathers = bird.species() == Species.QUAIL ? level.getRandom().nextInt(2) : 1 + level.getRandom().nextInt(2);
		if (feathers > 0) {
			bird.spawnAtLocation(level, new ItemStack(Items.FEATHER, feathers));
		}
		if (bird.species() == Species.DUCK) {
			bird.spawnAtLocation(level, new ItemStack(FarmingsItems.DOWN));     // about 30 g of down a duck (docs/eggs.md)
		}
	}
}

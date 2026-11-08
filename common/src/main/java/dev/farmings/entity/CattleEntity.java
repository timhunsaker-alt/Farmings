package dev.farmings.entity;

import dev.farmings.Farmings;
import dev.farmings.genetics.Founders;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.Ruleset;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.CattleBreed;
import dev.farmings.husbandry.Lifecycle;
import dev.farmings.husbandry.Stage;
import dev.farmings.husbandry.TimeScale;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.CowSoundVariant;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A head of cattle: an individual with a breed, a sex, a genome, an age and a weight (docs/animals/cattle.md). Age, pregnancy and
 * lactation are counted in game ticks and read through {@link TimeScale} (1 game day = 15 real days). All of it is saved with the
 * animal and survives a chunk unload.
 *
 * <p>This is the animal itself. It does not breed, milk or eat yet: vanilla breeding, bucket milking and wheat-tempting are left
 * out on purpose, because those come with the barn, the parlor and the genetics step. Its look is a vanilla cow until the Ludo art.
 */
public class CattleEntity extends Animal {
	/** Breed and stage, sent to the client so the renderer can pick the right coat, model and udder. */
	private static final EntityDataAccessor<Integer> DATA_BREED = SynchedEntityData.defineId(CattleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(CattleEntity.class, EntityDataSerializers.INT);

	private CattleBreed breed = CattleBreed.HOLSTEIN;
	private Sex sex = Sex.FEMALE;
	private @Nullable Genome genome;
	private int[] scores = new int[Trait.values().length];
	/** The game time she was born, in ticks. Negative is fine: an adult spawned today was born "before" the world began. */
	private long birthTick;
	private double weightKg;
	private boolean castrated;
	/** Game time she was bred, or -1 when she is not in calf. */
	private long bredTick = -1;
	private boolean hasCalved;
	private long calvedTick = -1;
	/** Milk in the udder, kg. One game day's production; unmilked milk is replaced, not stacked (proposed). */
	private double milkKg;
	/** The game tick the current or next heat window opens; -1 when she does not cycle. */
	private long nextHeatTick = -1;
	/** The sire's encoded genome while she is in calf; empty if none (an art/test cow in calf has no sire and never calves). */
	private String sireGenome = "";
	/** The game tick she calves. */
	private long dueTick = -1;
	/** The player whose barn houses her; calf counts for the tutorial sex rule are kept per player. */
	private @Nullable UUID ownerId;
	/** Game-day index since calving at which the udder was last refilled. */
	private long lastMilkDay = -1;
	private boolean retired;
	/** Her stall in a barn: where she stands, the bunk she faces, and the controller of her barn. All null when she is not housed. */
	private @Nullable BlockPos stallStand;
	private @Nullable BlockPos stallBunk;
	private @Nullable BlockPos stallController;
	/** Manure made but not yet dropped, and what she has eaten and drunk in all, and how well the last tending went (0 to 1). */
	private double manureKg;
	private double hayEatenKg;
	private double waterDrunkL;
	private double lastFed = 1;
	private double lastWatered = 1;

	public CattleEntity(EntityType<? extends CattleEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BREED, CattleBreed.HOLSTEIN.ordinal());
		builder.define(DATA_STAGE, Stage.HEIFER.ordinal());
	}

	/** The breed as the client knows it. */
	public CattleBreed syncedBreed() {
		CattleBreed[] all = CattleBreed.values();
		int i = entityData.get(DATA_BREED);
		return all[Math.max(0, Math.min(all.length - 1, i))];
	}

	/** The stage as the client knows it. */
	public Stage syncedStage() {
		Stage[] all = Stage.values();
		int i = entityData.get(DATA_STAGE);
		return all[Math.max(0, Math.min(all.length - 1, i))];
	}

	public static AttributeSupplier.Builder createAttributes() {
		// Vanilla cow values until real ones are decided; there is no source for either.
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.2F);
	}

	// ---- the barn ----

	public @Nullable BlockPos stallStand() {
		return stallStand;
	}

	public @Nullable BlockPos stallBunk() {
		return stallBunk;
	}

	public @Nullable BlockPos stallController() {
		return stallController;
	}

	public void assignStall(BlockPos controller, BlockPos stand, BlockPos bunk) {
		this.stallController = controller;
		this.stallStand = stand;
		this.stallBunk = bunk;
	}

	public void clearStall() {
		this.stallController = null;
		this.stallStand = null;
		this.stallBunk = null;
	}

	/** Records one tending: what she ate and drank, and how well it met her need. Her manure goes through {@link #collectManureItems}. */
	public void recordTending(double eatenKg, double drunkL, double fed, double watered) {
		hayEatenKg += eatenKg;
		waterDrunkL += drunkL;
		lastFed = fed;
		lastWatered = watered;
	}

	/** Adds manure and returns how many whole 50 kg items are now ready to drop; the rest stays. */
	public int collectManureItems(double addKg) {
		manureKg += addKg;
		int items = (int) Math.floor(manureKg / dev.farmings.feed.Intake.MANURE_ITEM_KG);
		manureKg -= items * dev.farmings.feed.Intake.MANURE_ITEM_KG;
		return items;
	}

	public double hayEatenKg() {
		return hayEatenKg;
	}

	public double waterDrunkL() {
		return waterDrunkL;
	}

	public double lastFed() {
		return lastFed;
	}

	public double lastWatered() {
		return lastWatered;
	}

	/** Dry matter she should eat in a day at her stage and weight (0 for a calf). */
	public double dryMatterIntakeKgPerDay() {
		return dev.farmings.feed.Hay.dryMatterIntakeOrStandIn(stage(), weightKg).orElse(0);
	}

	/** Walks to her stall and stays there, facing her bunk. While she has a stall nothing else moves her (it outranks strolling). */
	private static final class StallGoal extends net.minecraft.world.entity.ai.goal.Goal {
		private final CattleEntity cow;

		StallGoal(CattleEntity cow) {
			this.cow = cow;
			setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return cow.stallStand != null && !cow.isBaby();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void tick() {
			BlockPos stand = cow.stallStand;
			if (stand == null) {
				return;
			}
			double x = stand.getX() + 0.5;
			double z = stand.getZ() + 0.5;
			double y = stand.getY() + 0.25;       // the bed is a low slab
			if (cow.distanceToSqr(x, y, z) > 0.25) {
				cow.getNavigation().moveTo(x, y, z, 1.0);
			} else {
				cow.getNavigation().stop();
				if (cow.stallBunk != null) {
					cow.getLookControl().setLookAt(cow.stallBunk.getX() + 0.5, cow.getEyeY(), cow.stallBunk.getZ() + 0.5);
				}
			}
		}
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new StallGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	// --- the animal's facts ---

	/**
	 * Gives this animal a founder's genes and a starting state. Only breeds with genetic baselines can be made (Holstein, Jersey,
	 * Brown Swiss, Gyr); Angus and Wagyu wait for their beef traits.
	 */
	public void initFounder(CattleBreed breed, Sex sex, long seed, double ageRealDays, double weightKg, long nowTick) {
		Founders.Result f = Founders.create(breed.code, sex, seed, Ruleset.defaults());
		this.breed = breed;
		this.sex = sex;
		this.genome = f.genome();
		for (Trait t : Trait.values()) {
			this.scores[t.ordinal()] = f.phenotype().score(t);
		}
		this.birthTick = nowTick - TimeScale.realDaysToTicks(ageRealDays);
		this.weightKg = weightKg;
		this.castrated = false;
		this.bredTick = -1;
		this.hasCalved = false;
		this.calvedTick = -1;
		this.retired = false;
		syncAge(nowTick);
	}

	/**
	 * Shows this animal as another breed without giving it genes. For art checks and game tests only: Angus and Wagyu have no genetic
	 * baselines yet, so they cannot be made with {@link #initFounder}.
	 */
	public void setBreedForDisplay(CattleBreed breed, Sex sex, double weightKg, boolean inCalf) {
		this.breed = breed;
		this.sex = sex;
		this.weightKg = weightKg;
		this.bredTick = inCalf ? level().getGameTime() : -1;
		syncAge(level().getGameTime());
	}

	/** Marks her as having calved this many real days ago. For art checks and game tests: calving itself is not built yet. */
	public void setCalvedForDisplay(double realDaysAgo) {
		long now = level().getGameTime();
		this.hasCalved = true;
		this.bredTick = -1;
		this.calvedTick = now - TimeScale.realDaysToTicks(realDaysAgo);
		this.lastMilkDay = -1;
		syncAge(now);
		updateMilk(now);
	}

	/** Real days since calving, or 0 if she has not calved. */
	public double realDaysSinceCalving() {
		return hasCalved ? TimeScale.ticksToRealDays(level().getGameTime() - calvedTick) : 0;
	}

	public int daysInMilk() {
		return Lifecycle.givesMilk(stage()) ? dev.farmings.husbandry.Lactation.daysInMilk(realDaysSinceCalving()) : 0;
	}

	public double milkKg() {
		return milkKg;
	}

	/** The milk a milking machine takes: all of it. */
	public double takeMilk() {
		double taken = milkKg;
		milkKg = 0;
		return taken;
	}

	/** Refills the udder once per game day while she is lactating, from the curve, her genome and her last tending. */
	private void updateMilk(long nowTick) {
		if (sex != Sex.FEMALE || stageAt(nowTick) != Stage.LACTATING_COW) {
			milkKg = 0;
			return;
		}
		long day = (nowTick - calvedTick) / TimeScale.TICKS_PER_GAME_DAY;
		if (day != lastMilkDay) {
			lastMilkDay = day;
			double realDays = TimeScale.ticksToRealDays(nowTick - calvedTick);
			milkKg = dev.farmings.husbandry.Lactation.producedKg(breed, dev.farmings.husbandry.Lactation.daysInMilk(realDays),
					score(Trait.MILK_YIELD), lastFed, lastWatered);
		}
	}

	// --- breeding and calving (docs/animals/breeding.md) ---

	public @Nullable UUID owner() {
		return ownerId;
	}

	public void setOwner(@Nullable UUID owner) {
		this.ownerId = owner;
	}

	public boolean inHeat() {
		return sex == Sex.FEMALE && dev.farmings.husbandry.Breeding.inHeat(level().getGameTime(), nextHeatTick);
	}

	/** For game tests and art: opens a heat window now. */
	public void startHeatForDisplay() {
		this.nextHeatTick = level().getGameTime();
	}

	public boolean canConceive() {
		return genome != null && inHeat() && bredTick < 0;
	}

	/** A service in heat conceives (decided 2026-11-08). The sire's genome is kept until the calf is born. */
	public boolean conceive(Genome sire, long nowTick) {
		if (!canConceive()) {
			return false;
		}
		this.bredTick = nowTick;
		this.sireGenome = sire.encode();
		this.dueTick = nowTick + TimeScale.realDaysToTicks(breed.gestationDays);
		this.nextHeatTick = -1;
		syncAge(nowTick);
		return true;
	}

	public boolean hasSire() {
		return !sireGenome.isEmpty();
	}

	private void updateBreeding(ServerLevel level, long now) {
		if (sex != Sex.FEMALE || genome == null) {
			return;
		}
		if (bredTick >= 0) {
			if (hasSire() && dueTick >= 0 && now >= dueTick) {
				calve(level, now);
			}
			return;
		}
		double sinceCalving = hasCalved ? TimeScale.ticksToRealDays(now - calvedTick) : 0;
		if (!dev.farmings.husbandry.Breeding.cycles(breed, stageAt(now), false, ageRealDays(), weightKg, sinceCalving)) {
			nextHeatTick = -1;
			return;
		}
		if (nextHeatTick < 0) {
			long offset = Math.floorMod(genome.hash().hashCode(), dev.farmings.husbandry.Breeding.cycleTicks());
			nextHeatTick = now + offset;
		} else {
			nextHeatTick = dev.farmings.husbandry.Breeding.nextHeatStart(now, nextHeatTick);
		}
	}

	/** The calf is born beside her and her lactation starts. */
	private void calve(ServerLevel level, long now) {
		Genome sire = Genome.decode(sireGenome);
		Sex forced = null;
		CalfCounts counts = CalfCounts.get(level.getServer());
		if (ownerId != null) {
			forced = dev.farmings.husbandry.Breeding.forcedSex(counts.count(ownerId));
		}
		String eventId = "calving:" + getUUID() + ":" + now;
		dev.farmings.genetics.Offspring.Result r = dev.farmings.genetics.Offspring.create(genome, sire, eventId,
				Long.toString(level.getSeed()), Ruleset.defaults(), forced);
		CattleEntity calf = dev.farmings.FarmingsEntities.CATTLE.create(level, EntitySpawnReason.BREEDING);
		if (calf != null) {
			calf.initCalf(r, now, breed.matureWeightKg);
			calf.ownerId = ownerId;
			calf.setPos(getX() + 0.8, getY(), getZ());
			level.addFreshEntity(calf);
		}
		if (ownerId != null) {
			counts.add(ownerId);
		}
		hasCalved = true;
		calvedTick = now;
		bredTick = -1;
		sireGenome = "";
		dueTick = -1;
		nextHeatTick = -1;
		lastMilkDay = -1;
		syncAge(now);
		updateMilk(now);
	}

	/** The once-a-second bookkeeping: age and stage, the udder, heat and calving. Game tests call it to skip waiting. */
	public void settle(ServerLevel level) {
		long now = level.getGameTime();
		syncAge(now);
		updateMilk(now);
		updateBreeding(level, now);
	}

	/** For game tests: she is due now, so calving can be reached without waiting 18.9 game days. */
	public void makeDueNow() {
		this.dueTick = level().getGameTime();
	}

	/** A newborn from {@link dev.farmings.genetics.Offspring}: its breed is the largest share of its composition. */
	public void initCalf(dev.farmings.genetics.Offspring.Result r, long nowTick, double damMatureWeightKg) {
		String top = null;
		int best = -1;
		for (java.util.Map.Entry<String, Integer> e : r.genome().breedCompositionBp().entrySet()) {
			if (e.getValue() > best) {
				best = e.getValue();
				top = e.getKey();
			}
		}
		this.breed = CattleBreed.byCode(top);
		this.sex = r.sex();
		this.genome = r.genome();
		for (Trait t : Trait.values()) {
			this.scores[t.ordinal()] = r.phenotype().score(t);
		}
		this.birthTick = nowTick;
		this.weightKg = damMatureWeightKg * dev.farmings.husbandry.Breeding.BIRTH_WEIGHT_FRACTION;
		this.castrated = false;
		this.bredTick = -1;
		this.hasCalved = false;
		this.calvedTick = -1;
		this.retired = false;
		syncAge(nowTick);
	}

	public CattleBreed breed() {
		return breed;
	}

	public Sex sex() {
		return sex;
	}

	public @Nullable Genome genome() {
		return genome;
	}

	public int score(Trait trait) {
		return scores[trait.ordinal()];
	}

	public double weightKg() {
		return weightKg;
	}

	public void setWeightKg(double weightKg) {
		this.weightKg = weightKg;
		syncAge(level().getGameTime());
	}

	/** Marks her in calf from this game time. Breeding itself (heat, choosing a sire) comes in Phase 2. */
	public void markBred(long nowTick) {
		this.bredTick = nowTick;
		syncAge(nowTick);
	}

	public boolean isInCalf() {
		return bredTick >= 0;
	}

	/** Age in real days, at the current game time. */
	public double ageRealDays() {
		return TimeScale.ticksToRealDays(level().getGameTime() - birthTick);
	}

	public Stage stage() {
		return stageAt(level().getGameTime());
	}

	private Stage stageAt(long nowTick) {
		if (sex == Sex.MALE) {
			return Lifecycle.maleStage(breed, weightKg, castrated);
		}
		boolean pregnant = bredTick >= 0;
		double sinceCalving = hasCalved ? TimeScale.ticksToRealDays(nowTick - calvedTick) : 0;
		OptionalDouble toDue = pregnant ? OptionalDouble.of(breed.gestationDays - TimeScale.ticksToRealDays(nowTick - bredTick)) : OptionalDouble.empty();
		return Lifecycle.femaleStage(breed, weightKg, hasCalved, pregnant, sinceCalving, toDue, retired);
	}

	private boolean isCalfStage(Stage stage) {
		return stage == Stage.CALF || stage == Stage.BULL_CALF;
	}

	/**
	 * Keeps vanilla's baby flag equal to our stage, because the renderer and the hitbox read it. Vanilla's own growth timer is off
	 * ({@link #canAgeUp()}), so this is the only thing that moves it.
	 */
	private void syncAge(long nowTick) {
		Stage stage = stageAt(nowTick);
		entityData.set(DATA_BREED, breed.ordinal());
		entityData.set(DATA_STAGE, stage.ordinal());
		boolean calf = isCalfStage(stage);
		if (calf != isBaby()) {
			setAge(calf ? -1 : 0);
		}
	}

	// --- vanilla hooks we switch off for now ---

	@Override
	public boolean canAgeUp() {
		return false;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (tickCount % 20 == 0) {
			settle(level);
		}
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (genome == null) {
			// An adult that can be bred today: old enough, and at the breeding weight.
			Sex rolled = level.getRandom().nextBoolean() ? Sex.FEMALE : Sex.MALE;
			initFounder(CattleBreed.HOLSTEIN, rolled, level.getRandom().nextLong(), Lifecycle.FIRST_BREEDING_AGE_DAYS,
					CattleBreed.HOLSTEIN.breedingWeightKg(), level.getLevel().getGameTime());
		}
		return data;
	}

	// --- saving ---

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Breed", breed.code);
		output.putString("Sex", sex.name());
		if (genome != null) {
			output.putString("Genome", genome.encode());
		}
		output.putIntArray("Scores", scores.clone());
		output.putLong("BirthTick", birthTick);
		output.putDouble("WeightKg", weightKg);
		output.putBoolean("Castrated", castrated);
		output.putLong("BredTick", bredTick);
		output.putBoolean("HasCalved", hasCalved);
		output.putLong("CalvedTick", calvedTick);
		output.putDouble("MilkKg", milkKg);
		output.putLong("NextHeat", nextHeatTick);
		output.putString("Sire", sireGenome);
		output.putLong("Due", dueTick);
		if (ownerId != null) {
			output.putString("Owner", ownerId.toString());
		}
		output.putLong("LastMilkDay", lastMilkDay);
		output.putBoolean("Retired", retired);
		output.putLong("StallStand", stallStand == null ? Long.MIN_VALUE : stallStand.asLong());
		output.putLong("StallBunk", stallBunk == null ? Long.MIN_VALUE : stallBunk.asLong());
		output.putLong("StallController", stallController == null ? Long.MIN_VALUE : stallController.asLong());
		output.putDouble("ManureKg", manureKg);
		output.putDouble("HayEatenKg", hayEatenKg);
		output.putDouble("WaterDrunkL", waterDrunkL);
		output.putDouble("LastFed", lastFed);
		output.putDouble("LastWatered", lastWatered);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		try {
			breed = CattleBreed.byCode(input.getStringOr("Breed", CattleBreed.HOLSTEIN.code));
			sex = Sex.valueOf(input.getStringOr("Sex", Sex.FEMALE.name()));
			genome = input.getString("Genome").map(Genome::decode).orElse(null);
		} catch (IllegalArgumentException e) {
			Farmings.LOGGER.warn("A cattle record could not be read; keeping defaults: {}", e.getMessage());
		}
		input.getIntArray("Scores").ifPresent(saved -> {
			if (saved.length == scores.length) {
				scores = saved.clone();
			}
		});
		birthTick = input.getLongOr("BirthTick", 0L);
		weightKg = input.getDoubleOr("WeightKg", 0.0);
		castrated = input.getBooleanOr("Castrated", false);
		bredTick = input.getLongOr("BredTick", -1L);
		hasCalved = input.getBooleanOr("HasCalved", false);
		calvedTick = input.getLongOr("CalvedTick", -1L);
		milkKg = input.getDoubleOr("MilkKg", 0);
		nextHeatTick = input.getLongOr("NextHeat", -1L);
		sireGenome = input.getStringOr("Sire", "");
		dueTick = input.getLongOr("Due", -1L);
		String own = input.getStringOr("Owner", "");
		ownerId = own.isEmpty() ? null : java.util.UUID.fromString(own);
		lastMilkDay = input.getLongOr("LastMilkDay", -1L);
		retired = input.getBooleanOr("Retired", false);
		long stand = input.getLongOr("StallStand", Long.MIN_VALUE);
		long bunk = input.getLongOr("StallBunk", Long.MIN_VALUE);
		long controller = input.getLongOr("StallController", Long.MIN_VALUE);
		stallStand = stand == Long.MIN_VALUE ? null : BlockPos.of(stand);
		stallBunk = bunk == Long.MIN_VALUE ? null : BlockPos.of(bunk);
		stallController = controller == Long.MIN_VALUE ? null : BlockPos.of(controller);
		manureKg = input.getDoubleOr("ManureKg", 0);
		hayEatenKg = input.getDoubleOr("HayEatenKg", 0);
		waterDrunkL = input.getDoubleOr("WaterDrunkL", 0);
		lastFed = input.getDoubleOr("LastFed", 1);
		lastWatered = input.getDoubleOr("LastWatered", 1);
		entityData.set(DATA_BREED, breed.ordinal());
		entityData.set(DATA_STAGE, stageAt(level().getGameTime()).ordinal());
	}

	// --- sounds (the vanilla cow's, until there are our own) ---

	private static CowSoundVariant sounds() {
		return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return sounds().ambientSound().value();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return sounds().hurtSound().value();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return sounds().deathSound().value();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(sounds().stepSound().value(), 0.15F, 1.0F);
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}
}

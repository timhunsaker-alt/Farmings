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
	private boolean retired;

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

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
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
		double sinceCalving = calvedTick >= 0 ? TimeScale.ticksToRealDays(nowTick - calvedTick) : 0;
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
			syncAge(level.getGameTime());
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
		output.putBoolean("Retired", retired);
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
		retired = input.getBooleanOr("Retired", false);
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

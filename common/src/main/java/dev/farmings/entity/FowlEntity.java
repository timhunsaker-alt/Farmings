package dev.farmings.entity;

import dev.farmings.Farmings;
import dev.farmings.FarmingsEntities;
import dev.farmings.genetics.Founders;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.Offspring;
import dev.farmings.genetics.PoultryGenes;
import dev.farmings.genetics.PoultryLook;
import dev.farmings.genetics.Ruleset;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.poultry.Clutch;
import dev.farmings.husbandry.poultry.Fowl;
import dev.farmings.husbandry.poultry.FowlBreed;
import dev.farmings.husbandry.poultry.LayingCycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariant;
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A chicken, turkey, quail or duck (docs/animals/poultry.md): an individual with a breed mix, a sex, a poultry genome, plumage genes, an
 * age and a weight. One class serves all four species; the entity type says which. Ages run on the birds' own 3:1 time scale
 * ({@link TimeScale#FOWL}), while feed, water and eggs per game day are one real day's (the cattle rule).
 *
 * <p>Rules live in plain Java ({@link Fowl}, {@link LayingCycle}, {@link Clutch}); this class keeps the state and does the Minecraft side.
 */
public class FowlEntity extends Animal implements Wild.Animal {
	private static final EntityDataAccessor<Integer> DATA_BREED = SynchedEntityData.defineId(FowlEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> DATA_LOOK = SynchedEntityData.defineId(FowlEntity.class, EntityDataSerializers.LONG);
	/** Bit 0 male, bit 1 young (needs heat). */
	private static final EntityDataAccessor<Integer> DATA_FLAGS = SynchedEntityData.defineId(FowlEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(FowlEntity.class, EntityDataSerializers.FLOAT);

	/** A hen mates with an intact adult male of her species within this reach (the sheep's service range). */
	public static final double SERVICE_RANGE = 8.0;
	private static final TimeScale.Scale SCALE = TimeScale.FOWL;

	private final Species species;
	private FowlBreed breed;
	private Sex sex = Sex.FEMALE;
	private @Nullable Genome genome;
	private int[] scores = new int[Trait.values().length];
	private @Nullable PoultryGenes genes;
	private long birthTick;
	private double weightKg;
	private long growthTick;
	private boolean growthMarked;
	private long matedTick = -1;
	private String sireGenome = "";
	private int[] sireGenes = new int[0];
	private double eggCounter;
	private long lastLayDay = -1;
	private int eggsLaid;
	private @Nullable UUID ownerId;
	private boolean wild;
	private double settledDays;
	private @Nullable BlockPos lure;
	private String damId = "";
	private String sireId = "";
	private double lastFed = 1;
	private double lastWatered = 1;
	private boolean broody;
	private @Nullable BlockPos nest;
	private @Nullable BlockPos coop;
	private @Nullable BlockPos home;
	private @Nullable BlockPos target;
	/** Whether the bird was warm on its last check (a brooder, or its mother near); set by the brooder and the coop. */
	private long warmUntilTick = Long.MIN_VALUE;
	/** Until when this hen mothers her hatched young (warms them); after a broody hatch. */
	private long motherUntilTick = -1;
	private long lastColdDay = -1;
	private final List<ItemStack> laidForTest = new ArrayList<>();

	public FowlEntity(EntityType<? extends FowlEntity> type, Level level) {
		super(type, level);
		this.species = FarmingsEntities.speciesOf(type);
		this.breed = defaultBreed(species);
	}

	static FowlBreed defaultBreed(Species s) {
		for (FowlBreed b : FowlBreed.values()) {
			if (b.species == s) {
				return b;
			}
		}
		return FowlBreed.LEGHORN;
	}

	/** The vanilla chicken's values (4 health, 0.25 speed) scaled for the bigger birds (proposed). */
	public static AttributeSupplier.Builder createAttributes(Species s) {
		double health = switch (s) {
			case TURKEY -> 10.0;
			case DUCK -> 6.0;
			case QUAIL -> 2.0;
			default -> 4.0;
		};
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, 0.25F)
				.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BREED, 0);
		builder.define(DATA_LOOK, -1L);
		builder.define(DATA_FLAGS, 0);
		builder.define(DATA_SIZE, 1.0F);
	}

	// ---- what the client knows ----

	public Species species() {
		return species;
	}

	public @Nullable PoultryLook syncedLook() {
		long v = entityData.get(DATA_LOOK);
		return v < 0 ? null : PoultryLook.unpack(v);
	}

	public FowlBreed syncedBreed() {
		FowlBreed[] all = FowlBreed.values();
		return all[Math.max(0, Math.min(all.length - 1, entityData.get(DATA_BREED)))];
	}

	public boolean syncedMale() {
		return (entityData.get(DATA_FLAGS) & 1) != 0;
	}

	public boolean syncedYoung() {
		return (entityData.get(DATA_FLAGS) & 2) != 0;
	}

	public boolean syncedBroody() {
		return (entityData.get(DATA_FLAGS) & 4) != 0;
	}

	/** Body weight in kg, for the renderer's scale. */
	public float syncedSize() {
		return entityData.get(DATA_SIZE);
	}

	private void sync(long now) {
		entityData.set(DATA_BREED, breed.ordinal());
		if (genome != null) {
			entityData.set(DATA_LOOK, look().pack());
		}
		boolean young = young();
		entityData.set(DATA_FLAGS, (sex == Sex.MALE ? 1 : 0) | (young ? 2 : 0));
		entityData.set(DATA_SIZE, (float) Math.max(0.01, weightKg));
		entityData.set(DATA_FLAGS, entityData.get(DATA_FLAGS) | (broody ? 4 : 0));
		boolean baby = !Fowl.adult(composition(), ageRealDays(now)) && young;
		if (baby != isBaby()) {
			setAge(baby ? -1 : 0);
		}
	}

	// ---- the animal's facts ----

	/** A founder of one breed: its genes, plumage genes and a starting age. Weight follows from the age. */
	public void initFounder(FowlBreed breed, Sex sex, long seed, double ageRealDays, long now) {
		if (breed.species != species) {
			throw new IllegalArgumentException(breed + " is not a " + species);
		}
		Founders.Result f = Founders.create(breed.code, sex, seed, Ruleset.defaults());
		this.breed = breed;
		this.sex = sex;
		this.genome = f.genome();
		this.genes = PoultryGenes.founder(breed.code, sex, seed);
		for (Trait t : species.traits()) {
			scores[t.ordinal()] = f.phenotype().score(t);
		}
		this.birthTick = now - SCALE.realDaysToTicks(ageRealDays);
		this.weightKg = Fowl.weightKg(composition(), sex, ageRealDays);
		this.growthMarked = false;
		sync(now);
	}

	/** A hatchling from a fertile egg ({@link FowlEggs#hatch}). */
	public void initHatchling(Offspring.Result r, PoultryGenes plumage, long now) {
		this.breed = topBreed(r.genome().breedCompositionBp(), species);
		this.sex = r.sex();
		this.genome = r.genome();
		this.genes = plumage;
		for (Trait t : species.traits()) {
			scores[t.ordinal()] = r.phenotype().score(t);
		}
		this.birthTick = now;
		this.weightKg = Fowl.weightKg(composition(), sex, 0);
		this.growthMarked = false;
		sync(now);
	}

	public static FowlBreed topBreed(Map<String, Integer> composition, Species s) {
		FowlBreed top = null;
		int best = -1;
		for (Map.Entry<String, Integer> e : composition.entrySet()) {
			FowlBreed b = FowlBreed.byCode(e.getKey());
			if (b != null && b.species == s && e.getValue() > best) {
				best = e.getValue();
				top = b;
			}
		}
		return top == null ? defaultBreed(s) : top;
	}

	public FowlBreed breed() {
		return breed;
	}

	public Sex sex() {
		return sex;
	}

	public @Nullable Genome genome() {
		return genome;
	}

	public Map<String, Integer> composition() {
		return genome == null ? Map.of(breed.code, 10_000) : genome.breedCompositionBp();
	}

	public int score(Trait t) {
		int s = scores[t.ordinal()];
		return s == 0 ? 500 : s;
	}

	public PoultryGenes genes() {
		if (genes == null) {
			genes = PoultryGenes.founder(breed.code, sex, getUUID().getLeastSignificantBits());
		}
		return genes;
	}

	public PoultryLook look() {
		return PoultryLook.of(species, breed.code, genes(), sex);
	}

	public double weightKg() {
		return weightKg;
	}

	public double ageRealDays() {
		return ageRealDays(level().getGameTime());
	}

	private double ageRealDays(long now) {
		return SCALE.ticksToRealDays(now - birthTick);
	}

	/** Still under heat age: needs a brooder or its mother (docs/animals/poultry.md, "Brooding"). */
	public boolean young() {
		return Fowl.needsHeat(composition(), ageRealDays());
	}

	public boolean adult() {
		return Fowl.adult(composition(), ageRealDays());
	}

	public String herdId() {
		return genome == null ? "" : CattleEntity.herdId(genome);
	}

	/** A bird that lays eggs now: an adult hen, tame, not retired, not sitting. */
	public boolean laying() {
		return sex == Sex.FEMALE && !wild && !broody && genome != null && LayingCycle.eggsPerDay(species, composition(), ageRealDays(), score(Trait.EGG_PRODUCTION)) > 0;
	}

	/** A Broad Breasted White tom cannot mate (broad-breasted selection, Livestock Conservancy). */
	public boolean canServe() {
		return sex == Sex.MALE && !wild && adult() && genome != null && !(species == Species.TURKEY && breed == FowlBreed.BROAD_BREASTED_WHITE
				&& composition().getOrDefault("BBW", 0) >= 5000);
	}

	/** Lays fertile eggs: mated within the species' fertile days. */
	public boolean fertile() {
		return matedTick >= 0 && !sireGenome.isEmpty() && Clutch.fertile(species, SCALE.ticksToRealDays(level().getGameTime() - matedTick));
	}

	public void mate(FowlEntity male) {
		if (male.genome == null || male.species != species || sex != Sex.FEMALE) {
			return;
		}
		matedTick = level().getGameTime();
		sireGenome = male.genome.encode();
		sireGenes = male.genes().save();
		sireId = male.herdId();
	}

	public int eggsLaid() {
		return eggsLaid;
	}

	public boolean broody() {
		return broody;
	}

	public void setBroody(boolean broody, @Nullable BlockPos nest) {
		this.broody = broody;
		this.nest = broody ? nest : null;
	}

	public @Nullable BlockPos nest() {
		return nest;
	}

	public @Nullable BlockPos coop() {
		return coop;
	}

	public @Nullable BlockPos home() {
		return home;
	}

	public void assignCoop(@Nullable BlockPos controller, @Nullable BlockPos home) {
		this.coop = controller;
		this.home = home;
	}

	public void sendTo(@Nullable BlockPos target) {
		this.target = target;
	}

	public @Nullable BlockPos sentTo() {
		return target;
	}

	/** Called by a brooder or a brooding mother each time they warm this bird. */
	public void keepWarm(long untilTick) {
		warmUntilTick = Math.max(warmUntilTick, untilTick);
	}

	public boolean warm() {
		return level().getGameTime() <= warmUntilTick;
	}

	public void becomeMother(long untilTick) {
		this.motherUntilTick = untilTick;
	}

	public boolean mothering() {
		return level().getGameTime() < motherUntilTick;
	}

	public void recordTending(double fed, double watered) {
		this.lastFed = fed;
		this.lastWatered = watered;
	}

	public double lastFed() {
		return lastFed;
	}

	public double lastWatered() {
		return lastWatered;
	}

	// ---- Wild.Animal ----

	@Override
	public boolean isWild() {
		return wild;
	}

	@Override
	public void setWild(boolean wild) {
		this.wild = wild;
	}

	@Override
	public double settledDays() {
		return settledDays;
	}

	@Override
	public void setSettledDays(double days) {
		this.settledDays = days;
	}

	@Override
	public @Nullable UUID owner() {
		return ownerId;
	}

	@Override
	public void setOwner(@Nullable UUID owner) {
		this.ownerId = owner;
	}

	/** Birds eat grain as fed; the corral counts it in the same kilograms (docs/eggs.md, docs/taming.md). */
	@Override
	public double dailyDryMatterKg() {
		return Fowl.feedKgPerDay(species, weightKg);
	}

	@Override
	public double dailyWaterL() {
		return Fowl.waterLPerDay(species, weightKg);
	}

	@Override
	public @Nullable BlockPos lure() {
		return lure;
	}

	@Override
	public void setLure(@Nullable BlockPos to) {
		this.lure = to;
	}

	@Override
	public TimeScale.Scale timeScale() {
		return SCALE;
	}

	@Override
	public boolean eatsGrain() {
		return true;
	}

	@Override
	public boolean isFeed(ItemStack stack) {
		return Wild.grain(stack);
	}

	// ---- goals ----

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
		this.goalSelector.addGoal(2, new PlaceGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		Wild.addGoals(this, this.goalSelector, this::isWild, Wild::grain);
	}

	/** Goes to the spot its coop picks (a nest, the run, home), as the sheep's place goal does. A sitting hen stays on her nest. */
	private static final class PlaceGoal extends net.minecraft.world.entity.ai.goal.Goal {
		private final FowlEntity bird;

		PlaceGoal(FowlEntity bird) {
			this.bird = bird;
			setFlags(java.util.EnumSet.of(Flag.MOVE));
		}

		private @Nullable BlockPos spot() {
			if (bird.broody && bird.nest != null) {
				return bird.nest;
			}
			return bird.target != null ? bird.target : bird.home;
		}

		@Override
		public boolean canUse() {
			return spot() != null;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void tick() {
			BlockPos p = spot();
			if (p == null) {
				return;
			}
			double x = p.getX() + 0.5;
			double z = p.getZ() + 0.5;
			double dx = bird.getX() - x;
			double dz = bird.getZ() - z;
			double reach = bird.broody || bird.target != null ? 0.36 : 4.0;
			if (dx * dx + dz * dz > reach) {
				bird.getNavigation().moveTo(x, p.getY() + 0.1, z, 1.0);
			} else if (bird.broody || bird.target != null) {
				bird.getNavigation().stop();
			}
		}
	}

	// ---- the once-a-second bookkeeping ----

	public void settle(ServerLevel level) {
		long now = level.getGameTime();
		grow(now);
		cold(now);
		if (sex == Sex.FEMALE && !wild) {
			if (!fertile()) {
				serveFromNearbyMale(level);
			}
			lay(level, now);
		}
		sync(now);
	}

	/** For game tests: the once-a-day cold check, now. */
	public void coldDayForTest() {
		if (young() && !warm()) {
			setHealth((float) Math.max(1.0, getHealth() - Fowl.COLD_HEALTH_LOSS / 100.0 * getMaxHealth()));
		}
	}

	/** Once a game day, a young bird out of the warmth loses health (poultry.md, "Brooding": 10 points on 100, proposed). */
	private void cold(long now) {
		long day = now / TimeScale.TICKS_PER_GAME_DAY;
		if (lastColdDay < 0) {
			lastColdDay = day;
			return;
		}
		if (day <= lastColdDay) {
			return;
		}
		lastColdDay = day;
		if (young() && !warm()) {
			setHealth((float) Math.max(1.0, getHealth() - Fowl.COLD_HEALTH_LOSS / 100.0 * getMaxHealth()));
		}
	}

	private void grow(long now) {
		if (!growthMarked) {
			growthMarked = true;
			growthTick = now;
			return;
		}
		double span = SCALE.ticksToRealDays(now - growthTick);
		growthTick = now;
		if (span <= 0) {
			return;
		}
		double age = ageRealDays(now);
		double fed = coop != null ? lastFed : 1;
		if (young() && !warm()) {
			fed = 0;     // a cold young bird does not grow that day
		}
		double gain = (Fowl.weightKg(composition(), sex, age) - Fowl.weightKg(composition(), sex, age - span)) * fed;
		double mature = Fowl.matureKg(composition(), sex);
		if (gain > 0 && weightKg < mature) {
			weightKg = Math.min(mature, weightKg + gain);
		}
	}

	/** Natural mating: the nearest male that can serve, within reach. */
	private void serveFromNearbyMale(ServerLevel level) {
		if (!adult() || genome == null) {
			return;
		}
		FowlEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (FowlEntity f : level.getEntitiesOfClass(FowlEntity.class, getBoundingBox().inflate(SERVICE_RANGE))) {
			if (f != this && f.species == species && f.canServe()) {
				double d = f.distanceToSqr(this);
				if (d < bestD) {
					bestD = d;
					best = f;
				}
			}
		}
		if (best != null) {
			mate(best);
		}
	}

	/** Each game day she adds one real day's lay to her counter; an egg comes out each time it reaches one (poultry.md, "Laying"). */
	private void lay(ServerLevel level, long now) {
		long day = now / TimeScale.TICKS_PER_GAME_DAY;
		if (lastLayDay < 0) {
			lastLayDay = day;
			return;
		}
		if (day <= lastLayDay) {
			return;
		}
		long days = Math.min(day - lastLayDay, 30);
		lastLayDay = day;
		if (!laying()) {
			return;
		}
		double rate = LayingCycle.eggsPerDay(species, composition(), ageRealDays(now), score(Trait.EGG_PRODUCTION));
		eggCounter += rate * Math.min(lastFed, lastWatered) * days;
		while (eggCounter >= 1) {
			eggCounter -= 1;
			deliver(level, makeEgg(now));
		}
	}

	/** One egg from her now: weight from her age and egg_weight, shell from her genes, the sire's genes if she is fertile. */
	public ItemStack makeEgg(long now) {
		double grams = LayingCycle.eggGrams(composition(), ageRealDays(now), score(Trait.EGG_WEIGHT));
		boolean fertile = fertile();
		eggsLaid++;
		return FowlEggs.egg(species, grams, genes().expressed(dev.farmings.genetics.PoultryLocus.SHELL, sex), now, fertile ? genome : null,
				fertile ? genes() : null, fertile ? sireGenome : "", fertile ? sireGenes : new int[0], herdId(), sireId);
	}

	/** Where a laid egg goes: the coop's nest (set by the coop), else the ground (a floor egg). */
	protected void deliver(ServerLevel level, ItemStack egg) {
		if (eggSink != null && eggSink.take(level, this, egg)) {
			return;
		}
		laidForTest.add(egg.copy());
		spawnAtLocation(level, FowlEggs.markFloor(egg));
	}

	/** Coops take eggs into nest boxes; the coop package installs one ({@link #eggSink}). */
	public interface EggSink {
		boolean take(ServerLevel level, FowlEntity hen, ItemStack egg);
	}

	public static @Nullable EggSink eggSink;

	/** For game tests: lay one egg now, through the usual route. */
	public void layNowForTest(ServerLevel level) {
		deliver(level, makeEgg(level.getGameTime()));
	}

	public List<ItemStack> laidForTest() {
		return laidForTest;
	}

	/** For game tests: {@code realDays} older. */
	public void advanceForTest(double realDays) {
		long ticks = SCALE.realDaysToTicks(realDays);
		birthTick -= ticks;
		growthTick -= ticks;
		weightKg = Math.max(weightKg, Fowl.weightKg(composition(), sex, ageRealDays()));
		sync(level().getGameTime());
	}

	// ---- vanilla hooks ----

	@Override
	public void aiStep() {
		super.aiStep();
		// Chickens, turkeys and quail flap to fall slowly, as vanilla chickens do; ducks float (FloatGoal).
		Vec3 v = getDeltaMovement();
		if (!onGround() && v.y < 0.0 && !isInWater()) {
			setDeltaMovement(v.multiply(1.0, 0.6, 1.0));
		}
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

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
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) {
			if (!level().isClientSide()) {
				player.sendSystemMessage(Component.literal(readout()));
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	public String breedLabel() {
		List<Map.Entry<String, Integer>> parts = new ArrayList<>(composition().entrySet());
		parts.sort((x, y) -> y.getValue() - x.getValue());
		if (parts.size() == 1) {
			FowlBreed b = FowlBreed.byCode(parts.get(0).getKey());
			return b == null ? parts.get(0).getKey() : b.displayName();
		}
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, Integer> e : parts) {
			FowlBreed fb = FowlBreed.byCode(e.getKey());
			b.append(b.length() > 0 ? " " : "").append(fb == null ? e.getKey() : fb.displayName())
					.append(String.format(Locale.ROOT, " %.0f%%", e.getValue() / 100.0));
		}
		return b.toString();
	}

	/** hen, rooster, tom, drake ... by species and sex; chick, poult, duckling while young. */
	public String kind() {
		if (young()) {
			return switch (species) {
				case TURKEY -> "poult";
				case DUCK -> "duckling";
				default -> "chick";
			};
		}
		boolean male = sex == Sex.MALE;
		return switch (species) {
			case CHICKEN -> male ? "rooster" : adult() ? "hen" : "pullet";
			case TURKEY -> male ? "tom" : "hen";
			case QUAIL -> male ? "cock" : "hen";
			case DUCK -> male ? "drake" : "duck";
			default -> "bird";
		};
	}

	public String readout() {
		StringBuilder b = new StringBuilder();
		if (wild) {
			b.append(String.format(Locale.ROOT, "WILD (settled %.1f of %.0f days), ", settledDays, dev.farmings.husbandry.WildBreeds.SETTLE_DAYS));
		}
		double age = ageRealDays();
		b.append(breedLabel()).append(' ').append(kind());
		b.append(age < 120 ? String.format(Locale.ROOT, ", %.0f days", age) : String.format(Locale.ROOT, ", %.0f weeks", age / 7));
		b.append(String.format(Locale.ROOT, weightKg < 1 ? ", %.2f kg" : ", %.1f kg", weightKg));
		if (genome != null) {
			b.append(", ").append(look().describe());
		}
		if (young()) {
			b.append(warm() ? ", warm" : ", NEEDS HEAT");
		}
		if (sex == Sex.FEMALE && !wild) {
			if (broody) {
				b.append(", BROODY (sitting)");
			} else if (mothering()) {
				b.append(", mothering her young");
			} else if (LayingCycle.retired(composition(), age)) {
				b.append(", retired");
			} else if (laying()) {
				double rate = LayingCycle.eggsPerDay(species, composition(), age, score(Trait.EGG_PRODUCTION));
				b.append(String.format(Locale.ROOT, ", laying %.0f%%, %s eggs", rate * 100,
						PoultryLook.shellColour(genes().expressed(dev.farmings.genetics.PoultryLocus.SHELL, sex))));
				b.append(fertile() ? ", fertile" : ", not fertile");
			} else if (!adult()) {
				b.append(String.format(Locale.ROOT, ", lays in %.1f game days",
						SCALE.realDaysToGameDays(Fowl.mean(composition(), fb -> fb.firstEggDays) - age)));
			}
		}
		if (coop != null) {
			b.append(String.format(Locale.ROOT, ", fed %.0f%%, watered %.0f%%", lastFed * 100, lastWatered * 100));
		}
		b.append(String.format(Locale.ROOT, ", health %.0f/%.0f", getHealth(), getMaxHealth()));
		return b.toString();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		FowlEggs.dropCarcass(level, this, isOnFire());
	}

	/** Natural spawns follow the vanilla animal rule (grass, light 9 or more); ducks may also stand on sand by water (poultry.md). */
	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
		if (Wild.wildReason(reason)) {
			BlockPos p = blockPosition();
			if (species == Species.DUCK && level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.SAND) && nearWater(level, p)) {
				return level.getRawBrightness(p, 0) > 8;
			}
			@SuppressWarnings("unchecked")
			EntityType<FowlEntity> t = (EntityType<FowlEntity>) getType();
			return Animal.checkAnimalSpawnRules(t, level, reason, p, getRandom());
		}
		return true;     // a summon, spawn egg or hatch goes where it is put (docs/taming.md)
	}

	static boolean nearWater(net.minecraft.world.level.LevelAccessor level, BlockPos p) {
		for (BlockPos q : BlockPos.betweenClosed(p.offset(-4, -2, -4), p.offset(4, 1, 4))) {
			if (level.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (genome == null) {
			List<FowlBreed> mine = new ArrayList<>();
			for (FowlBreed b : FowlBreed.values()) {
				if (b.species == species) {
					mine.add(b);
				}
			}
			FowlBreed b = mine.get(level.getRandom().nextInt(mine.size()));
			Sex s = level.getRandom().nextBoolean() ? Sex.FEMALE : Sex.MALE;
			if (Wild.wildReason(reason)) {
				// A wild founder: the breed comes from the biome, one male per spawn group (poultry.md, "Wild fowl").
				Wild.Group g = Wild.group(groupData, level);
				data = g;
				FowlBreed wb = dev.farmings.husbandry.WildBreeds.fowl(species, Wild.biome(level, blockPosition()), g.roll);
				if (wb != null) {
					b = wb;
				}
				s = g.maleGiven ? Sex.FEMALE : Sex.MALE;
				g.maleGiven = true;
				wild = true;
			}
			initFounder(b, s, level.getRandom().nextLong(), Fowl.mean(Map.of(b.code, 10_000), fb -> fb.firstEggDays) + 14, level.getLevel().getGameTime());
		}
		return data;
	}

	// ---- saving ----

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putString("Breed", breed.code);
		out.putString("Sex", sex.name());
		if (genome != null) {
			out.putString("Genome", genome.encode());
		}
		out.putIntArray("Scores", scores.clone());
		if (genes != null) {
			out.putIntArray("Plumage", genes.save());
		}
		out.putLong("BirthTick", birthTick);
		out.putDouble("WeightKg", weightKg);
		out.putBoolean("GrowthMarked", growthMarked);
		out.putLong("GrowthTick", growthTick);
		out.putLong("MatedTick", matedTick);
		out.putString("Sire", sireGenome);
		out.putIntArray("SirePlumage", sireGenes);
		out.putDouble("EggCounter", eggCounter);
		out.putLong("LastLayDay", lastLayDay);
		out.putInt("EggsLaid", eggsLaid);
		if (ownerId != null) {
			out.putString("Owner", ownerId.toString());
		}
		out.putBoolean("Wild", wild);
		out.putDouble("SettledDays", settledDays);
		out.putString("Dam", damId);
		out.putString("SireId", sireId);
		out.putDouble("LastFed", lastFed);
		out.putDouble("LastWatered", lastWatered);
		out.putBoolean("Broody", broody);
		out.putLong("MotherUntil", motherUntilTick);
		out.putLong("Nest", nest == null ? Long.MIN_VALUE : nest.asLong());
		out.putLong("Coop", coop == null ? Long.MIN_VALUE : coop.asLong());
		out.putLong("Home", home == null ? Long.MIN_VALUE : home.asLong());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		try {
			FowlBreed b = FowlBreed.byCode(in.getStringOr("Breed", breed.code));
			breed = b != null && b.species == species ? b : defaultBreed(species);
			sex = Sex.valueOf(in.getStringOr("Sex", Sex.FEMALE.name()));
			genome = in.getString("Genome").map(Genome::decode).orElse(null);
		} catch (IllegalArgumentException e) {
			Farmings.LOGGER.warn("A bird record could not be read; keeping defaults: {}", e.getMessage());
		}
		in.getIntArray("Scores").ifPresent(saved -> {
			if (saved.length <= scores.length) {
				System.arraycopy(saved, 0, scores, 0, saved.length);
			}
		});
		genes = in.getIntArray("Plumage").map(PoultryGenes::load).orElse(null);
		birthTick = in.getLongOr("BirthTick", 0L);
		weightKg = in.getDoubleOr("WeightKg", 0.0);
		growthMarked = in.getBooleanOr("GrowthMarked", false);
		growthTick = in.getLongOr("GrowthTick", 0L);
		matedTick = in.getLongOr("MatedTick", -1L);
		sireGenome = in.getStringOr("Sire", "");
		sireGenes = in.getIntArray("SirePlumage").orElse(new int[0]);
		eggCounter = in.getDoubleOr("EggCounter", 0);
		lastLayDay = in.getLongOr("LastLayDay", -1L);
		eggsLaid = in.getIntOr("EggsLaid", 0);
		String own = in.getStringOr("Owner", "");
		ownerId = own.isEmpty() ? null : UUID.fromString(own);
		wild = in.getBooleanOr("Wild", false);
		settledDays = in.getDoubleOr("SettledDays", 0);
		damId = in.getStringOr("Dam", "");
		sireId = in.getStringOr("SireId", "");
		lastFed = in.getDoubleOr("LastFed", 1);
		lastWatered = in.getDoubleOr("LastWatered", 1);
		broody = in.getBooleanOr("Broody", false);
		motherUntilTick = in.getLongOr("MotherUntil", -1L);
		nest = pos(in.getLongOr("Nest", Long.MIN_VALUE));
		coop = pos(in.getLongOr("Coop", Long.MIN_VALUE));
		home = pos(in.getLongOr("Home", Long.MIN_VALUE));
		sync(level().getGameTime());
	}

	private static @Nullable BlockPos pos(long v) {
		return v == Long.MIN_VALUE ? null : BlockPos.of(v);
	}

	public void setDamId(String id) {
		this.damId = id;
	}

	public void setSireId(String id) {
		this.sireId = id;
	}

	// ---- sounds: the vanilla chicken's, pitched by species (proposed; vanilla has no turkey, quail or duck sounds) ----

	private ChickenSoundVariant.ChickenSoundSet sounds() {
		return SoundEvents.CHICKEN_SOUNDS.get(ChickenSoundVariants.SoundSet.CLASSIC).adultSounds();
	}

	@Override
	public float getVoicePitch() {
		float base = switch (species) {
			case TURKEY -> 0.6F;
			case DUCK -> 0.75F;
			case QUAIL -> 1.6F;
			default -> 1.0F;
		};
		return base * super.getVoicePitch();
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
}

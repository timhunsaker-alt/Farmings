package dev.farmings.entity;

import dev.farmings.Farmings;
import dev.farmings.genetics.BreedingSeed;
import dev.farmings.genetics.Founders;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.HorseGenes;
import dev.farmings.genetics.HorseLocus;
import dev.farmings.genetics.HorseLook;
import dev.farmings.genetics.Offspring;
import dev.farmings.genetics.Rng;
import dev.farmings.genetics.Ruleset;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.Equine;
import dev.farmings.husbandry.Flock;
import dev.farmings.husbandry.Gallop;
import dev.farmings.husbandry.HorseBreed;
import dev.farmings.husbandry.HorseStage;
import dev.farmings.husbandry.TimeScale;
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
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Farmings horse (docs/animals/horses.md): Thoroughbred, Quarter Horse or Arabian, with its genome (SPEED and STAMINA among the traits),
 * the MSTN speed gene and coat genes, foaling one foal a season, and fitness that builds with galloping. It extends vanilla's
 * {@link AbstractHorse}, so saddles, riding and jumping work as vanilla; vanilla taming by riding is off (a wild horse throws you; it is
 * tamed in the corral). Ridden, it runs at its pace from {@link Gallop}: the server works out the speed for the distance galloped since
 * the horse last rested and syncs it, and {@link #getRiddenSpeed} turns it into movement on the rider's side.
 */
public class HorseEntity extends AbstractHorse implements Wild.Animal {
	private static final EntityDataAccessor<Integer> DATA_BREED = SynchedEntityData.defineId(HorseEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(HorseEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> DATA_LOOK = SynchedEntityData.defineId(HorseEntity.class, EntityDataSerializers.LONG);
	/** Body weight in kg, for the model's size. */
	private static final EntityDataAccessor<Integer> DATA_KG = SynchedEntityData.defineId(HorseEntity.class, EntityDataSerializers.INT);
	/** The speed the horse runs at now when ridden, m/s; 0 when not ridden. */
	private static final EntityDataAccessor<Float> DATA_PACE = SynchedEntityData.defineId(HorseEntity.class, EntityDataSerializers.FLOAT);

	/**
	 * Blocks a second per point of movement speed on flat ground: the player's 0.1 walks 4.317 m/s. Checked in the race client scene: a
	 * ridden horse set to 18.39 m/s moved 18.38 m/s on the straights (horses.md).
	 */
	public static final double BLOCKS_PER_SECOND_PER_SPEED = 43.17;
	/** Vanilla's middle horse: speed 0.225 (0.1125-0.3375), jump 0.7 (0.4-1.0), health 22.5 (15-30) *(proposed: jump stays vanilla's middle)*. */
	private static final double WALK_SPEED = 0.225;
	private static final double JUMP = 0.7;
	private static final double HEALTH = 22.5;
	/** An underfed horse gains no fitness (horses.md): fed under 95% of its need in the stable *(proposed tolerance)*. */
	public static final double FED_FOR_FITNESS = 0.95;

	private HorseBreed breed = HorseBreed.THOROUGHBRED;
	private Sex sex = Sex.FEMALE;
	private @Nullable Genome genome;
	private final int[] scores = new int[Trait.values().length];
	private @Nullable HorseGenes genes;
	private long birthTick;
	private double weightKg;
	private long growthTick;
	private boolean growthMarked;
	private boolean castrated;
	private long bredTick = -1;
	private long dueTick = -1;
	private String sireGenome = "";
	private int[] sireGenes = new int[0];
	private boolean hasFoaled;
	private long foaledTick = -1;
	private int foalings;
	private long nextHeatTick = -1;
	private @Nullable UUID ownerId;
	private boolean wild;
	private double settledDays;
	private String damId = "";
	private String sireId = "";
	private double lastFed = 1;
	private double lastWatered = 1;
	private @Nullable Placement place;
	private @Nullable BlockPos lure;
	private boolean sureConceptionForTest;
	private int forcedDayOfYear = -1;
	// fitness and the run in hand
	private double fitness;
	private long lastWorkTick = Long.MIN_VALUE;
	private long gainDay = -1;
	private double gainedToday;
	private double gallopM;
	private double workM;
	private int restTicks;
	private double lastX = Double.NaN;
	private double lastZ = Double.NaN;
	private double halfPace = 8;
	// race records (race-track.md, "Records"): best time in ticks at each distance, races run, wins
	private final java.util.TreeMap<Integer, Integer> bestTicks = new java.util.TreeMap<>();
	private int races;
	private int wins;
	/** Held still by a start gate's AI jockey (its own AI off) and what its AI was before. Saved, so a race cut short by a save lets go. */
	private boolean raceHold;
	private boolean noAiBeforeRace;

	public HorseEntity(EntityType<? extends HorseEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return AbstractHorse.createBaseHorseAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
				.add(Attributes.JUMP_STRENGTH, JUMP).add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BREED, 0);
		builder.define(DATA_STAGE, HorseStage.FILLY.ordinal());
		builder.define(DATA_LOOK, -1L);
		builder.define(DATA_KG, 0);
		builder.define(DATA_PACE, 0F);
	}

	/** Vanilla rolls random speed, jump and health on spawn; a Farmings horse's speed comes from its genes. */
	@Override
	protected void randomizeAttributes(RandomSource random) {
	}

	// ---- what the client knows ----

	public @Nullable HorseLook syncedLook() {
		long v = entityData.get(DATA_LOOK);
		return v < 0 ? null : HorseLook.unpack(v);
	}

	public HorseStage syncedStage() {
		HorseStage[] all = HorseStage.values();
		return all[Math.max(0, Math.min(all.length - 1, entityData.get(DATA_STAGE)))];
	}

	public float syncedKg() {
		return entityData.get(DATA_KG);
	}

	/** The breed's mature weight, for the model's size. */
	public float syncedMatureKg() {
		HorseBreed[] all = HorseBreed.values();
		return (float) all[Math.max(0, Math.min(all.length - 1, entityData.get(DATA_BREED)))].matureKg;
	}

	/** The speed the horse runs at now when ridden, m/s. */
	public float syncedPace() {
		return entityData.get(DATA_PACE);
	}

	private void sync(long now) {
		entityData.set(DATA_BREED, breed.ordinal());
		HorseStage stage = stageAt(now);
		entityData.set(DATA_STAGE, stage.ordinal());
		if (genome != null) {
			entityData.set(DATA_LOOK, look().pack());
		}
		entityData.set(DATA_KG, (int) Math.min(2000, Math.round(weightKg)));
		boolean young = stage.foal();
		if (young != isBaby()) {
			setAge(young ? -1 : 0);
		}
		if (isTamed() == wild) {
			setTamed(!wild);     // a herd horse takes a saddle; a wild one doesn't
		}
	}

	// ---- the animal's facts ----

	public void initFounder(HorseBreed breed, Sex sex, long seed, double ageRealDays, long now) {
		Founders.Result f = Founders.create(breed.code, sex, seed, Ruleset.defaults());
		this.breed = breed;
		this.sex = sex;
		this.genome = f.genome();
		this.genes = HorseGenes.founder(breed.code, seed);
		for (Trait t : Species.HORSE.traits()) {
			scores[t.ordinal()] = f.phenotype().score(t);
		}
		this.birthTick = now - TimeScale.realDaysToTicks(ageRealDays);
		this.weightKg = Equine.targetKg(Map.of(breed.code, 10_000), ageRealDays);
		this.growthMarked = false;
		sync(now);
	}

	public void initFoal(Offspring.Result r, HorseGenes coat, double birthKg, long now) {
		this.breed = Equine.topBreed(r.genome().breedCompositionBp());
		this.sex = r.sex();
		this.genome = r.genome();
		this.genes = coat;
		for (Trait t : Species.HORSE.traits()) {
			scores[t.ordinal()] = r.phenotype().score(t);
		}
		this.birthTick = now;
		this.weightKg = birthKg;
		sync(now);
	}

	public HorseBreed breed() {
		return breed;
	}

	public Sex sex() {
		return sex;
	}

	public @Nullable Genome genome() {
		return genome;
	}

	public Map<String, Integer> composition() {
		return genome != null ? genome.breedCompositionBp() : Map.of(breed.code, 10_000);
	}

	public int score(Trait t) {
		return scores[t.ordinal()];
	}

	public void setScoreForTest(Trait t, int score) {
		scores[t.ordinal()] = score;
	}

	public HorseGenes genes() {
		if (genes == null) {
			long seed = genome == null ? getUUID().getLeastSignificantBits() : Long.parseUnsignedLong(genome.hash().substring(0, 15), 16);
			genes = HorseGenes.founder(breed.code, seed);
		}
		return genes;
	}

	public void setGenesForTest(HorseGenes g) {
		this.genes = g;
		sync(level().getGameTime());
	}

	public HorseLook look() {
		return HorseLook.of(genes(), ageYears());
	}

	public double weightKg() {
		return weightKg;
	}

	public double ageRealDays() {
		return TimeScale.ticksToRealDays(level().getGameTime() - birthTick);
	}

	public double ageYears() {
		return ageRealDays() / 365.0;
	}

	public void setAgeForTest(double realDays) {
		long now = level().getGameTime();
		birthTick = now - TimeScale.realDaysToTicks(realDays);
		weightKg = Equine.targetKg(composition(), realDays);
		growthMarked = false;
		sync(now);
	}

	public HorseStage stage() {
		return stageAt(level().getGameTime());
	}

	private HorseStage stageAt(long now) {
		double age = TimeScale.ticksToRealDays(now - birthTick);
		return sex == Sex.MALE ? HorseStage.male(age, castrated) : HorseStage.female(age, hasFoaled);
	}

	public String damId() {
		return damId;
	}

	public String sireId() {
		return sireId;
	}

	public String herdId() {
		return CattleEntity.herdId(genome);
	}

	public int foalings() {
		return foalings;
	}

	@Override
	public @Nullable UUID owner() {
		return ownerId;
	}

	@Override
	public void setOwner(@Nullable UUID owner) {
		this.ownerId = owner;
	}

	public Placement place() {
		if (place == null) {
			place = new Placement();
		}
		return place;
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
	public boolean isWild() {
		return wild;
	}

	@Override
	public void setWild(boolean wild) {
		this.wild = wild;
		sync(level().getGameTime());
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
	public double dailyDryMatterKg() {
		return Equine.dryMatterKg(weightKg);
	}

	@Override
	public double dailyWaterL() {
		return Equine.waterL(weightKg, false);
	}

	public double manureKgPerDay() {
		return Equine.manureKg(weightKg);
	}

	// ---- speed and fitness ----

	/** What this horse is, for {@link Gallop}, at its age and fitness now. */
	public Gallop.Runner runner() {
		return new Gallop.Runner(composition(), genes().count(HorseLocus.MSTN), score(Trait.SPEED), score(Trait.STAMINA), ageYears(), fitness());
	}

	/** Fitness 0-1 now: kept 42 real days after the last work, then lost over 42. */
	public double fitness() {
		if (lastWorkTick == Long.MIN_VALUE) {
			return fitness;
		}
		return Gallop.rested(fitness, TimeScale.ticksToRealDays(level().getGameTime() - lastWorkTick));
	}

	public void setFitnessForTest(double f) {
		this.fitness = f;
		this.lastWorkTick = level().getGameTime();
	}

	/** Metres galloped since the horse last rested. */
	public double gallopedM() {
		return gallopM;
	}

	/** Work done: {@code metres} galloped at over half the horse's pace. Builds fitness unless the horse is underfed. */
	public void work(double metres) {
		long now = level().getGameTime();
		long day = (long) Math.floor(TimeScale.ticksToRealDays(now));
		if (day != gainDay) {
			gainDay = day;
			gainedToday = 0;
		}
		double f = fitness();
		double g = lastFed < FED_FOR_FITNESS ? 0 : Gallop.gain(metres / 1000.0, gainedToday);
		fitness = Math.min(1, f + g);
		gainedToday += g;
		lastWorkTick = now;
	}

	/** Server side, every tick: counts the distance galloped and the rests, and syncs the ridden speed. */
	private void trackGallop() {
		double x = getX();
		double z = getZ();
		if (Double.isNaN(lastX)) {
			lastX = x;
			lastZ = z;
		}
		double d = Math.hypot(x - lastX, z - lastZ);
		lastX = x;
		lastZ = z;
		if (d > 3) {
			d = 0;     // a teleport, not a stride
		}
		if (tickCount % 20 == 0) {
			halfPace = 0.5 * runner().pace(Gallop.SHORTEST_M);
		}
		if (d * 20 > halfPace) {
			gallopM += d;
			workM += d;
			restTicks = 0;
		} else if (++restTicks >= Gallop.REST_SECONDS * 20) {
			gallopM = 0;
		}
		if (workM >= 100) {
			work(workM);
			workM = 0;
		}
		if (tickCount % 5 == 0) {
			entityData.set(DATA_PACE, isVehicle() && !isBaby() ? (float) runner().speedAt(gallopM) : 0F);
		}
	}

	/** Ridden, the horse runs at the pace its genes, age and fitness give for the distance it has galloped since it rested. */
	@Override
	protected float getRiddenSpeed(Player rider) {
		float pace = syncedPace();
		return pace > 0 ? (float) (pace / BLOCKS_PER_SECOND_PER_SPEED) : super.getRiddenSpeed(rider);
	}

	// ---- race records ----

	/** A finished race or timed run: {@code ticks} over {@code metres}; a win if it came first among two or more. */
	public void recordRace(int metres, int ticks, boolean won) {
		races++;
		if (won) {
			wins++;
		}
		Integer best = bestTicks.get(metres);
		if (best == null || ticks < best) {
			bestTicks.put(metres, ticks);
		}
	}

	/** A start gate's AI jockey takes the horse: its own AI is off until {@link #releaseFromRace}. */
	public void holdForRace() {
		if (!raceHold) {
			noAiBeforeRace = isNoAi();
			raceHold = true;
			setNoAi(true);
		}
	}

	public void releaseFromRace() {
		if (raceHold) {
			raceHold = false;
			setNoAi(noAiBeforeRace);
			setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		}
	}

	/** Best time at a distance, in ticks, or -1. */
	public int bestTicks(int metres) {
		return bestTicks.getOrDefault(metres, -1);
	}

	public java.util.SortedMap<Integer, Integer> bests() {
		return java.util.Collections.unmodifiableSortedMap(bestTicks);
	}

	public int races() {
		return races;
	}

	public int wins() {
		return wins;
	}

	/** The name a race card shows: the horse's own name, else its herd id and breed. */
	public String raceName() {
		return hasCustomName() ? getCustomName().getString() : herdId() + " (" + breedLabel() + ")";
	}

	// ---- breeding and foaling ----

	private int dayOfYear(long now) {
		return forcedDayOfYear >= 0 ? forcedDayOfYear : Flock.dayOfYear(TimeScale.ticksToRealDays(now));
	}

	public void forceDayOfYearForTest(int day) {
		this.forcedDayOfYear = day;
	}

	public boolean inHeat() {
		long now = level().getGameTime();
		return sex == Sex.FEMALE && nextHeatTick >= 0 && now >= nextHeatTick && now < nextHeatTick + TimeScale.realDaysToTicks(Equine.HEAT_DAYS);
	}

	public void startHeatForDisplay() {
		this.nextHeatTick = level().getGameTime();
	}

	public boolean pregnant() {
		return bredTick >= 0;
	}

	public void sureConceptionForTest() {
		this.sureConceptionForTest = true;
	}

	public boolean canConceive() {
		return genome != null && !wild && inHeat() && bredTick < 0;
	}

	/** A service: conceives 60% of the time, by a roll from the service. */
	public boolean conceive(Genome sire, HorseGenes sireCoat, long now) {
		if (!canConceive() || sire.species() != Species.HORSE) {
			return false;
		}
		Rng rng = new Rng(BreedingSeed.seed(Long.toString(now), "service:" + getUUID(), genome.hash(), sire.hash(), Ruleset.defaults().version()));
		if (!sureConceptionForTest && rng.next() >= Equine.CONCEPTION) {
			nextHeatTick = -1;     // missed: she cycles again
			return false;
		}
		this.bredTick = now;
		this.sireGenome = sire.encode();
		this.sireGenes = sireCoat.save();
		this.dueTick = now + TimeScale.realDaysToTicks(Equine.GESTATION_DAYS);
		this.nextHeatTick = -1;
		sync(now);
		return true;
	}

	public void makeDueNow() {
		this.dueTick = level().getGameTime();
	}

	public double daysToFoaling() {
		return bredTick < 0 || dueTick < 0 ? -1 : TimeScale.ticksToRealDays(dueTick - level().getGameTime());
	}

	private boolean cycles(long now) {
		if (sex != Sex.FEMALE || genome == null || bredTick >= 0 || !Equine.inSeason(dayOfYear(now))) {
			return false;
		}
		double age = TimeScale.ticksToRealDays(now - birthTick);
		if (!hasFoaled) {
			return age >= Equine.FIRST_BREEDING_DAYS;
		}
		return TimeScale.ticksToRealDays(now - foaledTick) >= Equine.FOAL_HEAT_DAYS;
	}

	private void updateBreeding(ServerLevel level, long now) {
		if (sex != Sex.FEMALE || genome == null) {
			return;
		}
		if (bredTick >= 0) {
			if (!sireGenome.isEmpty() && dueTick >= 0 && now >= dueTick) {
				foal(level, now);
			}
			return;
		}
		if (!cycles(now)) {
			nextHeatTick = -1;
			return;
		}
		long cycle = TimeScale.realDaysToTicks(Equine.CYCLE_DAYS);
		if (nextHeatTick < 0) {
			// the foal heat comes 8 days after foaling; otherwise somewhere in her cycle
			boolean foalHeat = hasFoaled && TimeScale.ticksToRealDays(now - foaledTick) < Equine.FOAL_HEAT_DAYS + Equine.HEAT_DAYS;
			nextHeatTick = foalHeat ? foaledTick + TimeScale.realDaysToTicks(Equine.FOAL_HEAT_DAYS) : now + Math.floorMod(genome.hash().hashCode(), cycle);
		} else if (now >= nextHeatTick + TimeScale.realDaysToTicks(Equine.HEAT_DAYS)) {
			nextHeatTick += cycle * ((now - nextHeatTick) / cycle + 1);
		}
		if (inHeat()) {
			serveFromNearbyStallion(level, now);
		}
	}

	private void serveFromNearbyStallion(ServerLevel level, long now) {
		HorseEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (HorseEntity h : level.getEntitiesOfClass(HorseEntity.class, getBoundingBox().inflate(Equine.SERVICE_RANGE))) {
			if (h != this && h.sex == Sex.MALE && h.stage() == HorseStage.STALLION && h.genome != null && !h.wild) {
				double d = h.distanceToSqr(this);
				if (d < bestD) {
					bestD = d;
					best = h;
				}
			}
		}
		if (best != null) {
			conceive(best.genome, best.genes(), now);
		}
	}

	/** She foals: one foal of 10% of her weight (twins aren't modelled). */
	private void foal(ServerLevel level, long now) {
		Genome sire = Genome.decode(sireGenome);
		String world = Long.toString(level.getSeed());
		String eventId = "foaling:" + getUUID() + ":" + now;
		Offspring.Result r = Offspring.create(genome, sire, eventId, world, Ruleset.defaults(), null);
		HorseEntity foal = dev.farmings.FarmingsEntities.HORSE.create(level, EntitySpawnReason.BREEDING);
		if (foal != null) {
			HorseGenes sireCoat = HorseGenes.load(sireGenes);
			HorseGenes coat = HorseGenes.inherit(genes(), sireCoat != null ? sireCoat : HorseGenes.founder(Equine.topBreed(sire.breedCompositionBp()).code, 0),
					BreedingSeed.seed(world, eventId, genome.hash(), sire.hash(), Ruleset.defaults().version()));
			foal.initFoal(r, coat, Equine.BIRTH_SHARE * weightKg, now);
			foal.ownerId = ownerId;
			foal.damId = herdId();
			foal.sireId = CattleEntity.herdId(sire);
			foal.place().copyHomeFrom(place());
			foal.setPos(getX() + 0.6, getY(), getZ());
			level.addFreshEntity(foal);
		}
		hasFoaled = true;
		foalings++;
		foaledTick = now;
		bredTick = -1;
		dueTick = -1;
		sireGenome = "";
		sireGenes = new int[0];
		nextHeatTick = -1;
		sync(now);
	}

	public boolean nursing() {
		return hasFoaled && TimeScale.ticksToRealDays(level().getGameTime() - foaledTick) < Equine.WEANING_DAYS;
	}

	// ---- feed and growth ----

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

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.2));
		this.goalSelector.addGoal(3, new NotRidden(place().goal(this, this::isBaby)));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		Wild.addGoals(this, this.goalSelector, this::isWild);
	}

	public void settle(ServerLevel level) {
		long now = level.getGameTime();
		grow(now);
		updateBreeding(level, now);
		sync(now);
	}

	private void grow(long now) {
		if (!growthMarked) {
			growthMarked = true;
			growthTick = now;
			return;
		}
		double span = TimeScale.ticksToRealDays(now - growthTick);
		growthTick = now;
		if (span <= 0) {
			return;
		}
		double age = TimeScale.ticksToRealDays(now - birthTick);
		double fed = place().shedController() != null && !stageAt(now).foal() ? lastFed : 1;
		double mature = Equine.matureKg(composition());
		double gain = Equine.gainKg(composition(), age - span, span, fed);
		if (gain > 0 && weightKg < mature) {
			weightKg = Math.min(mature, weightKg + gain);
		}
	}

	public void advanceForTest(double realDays) {
		long ticks = TimeScale.realDaysToTicks(realDays);
		birthTick -= ticks;
		growthTick -= ticks;
	}

	/** The stable's walking goal, held off while someone rides the horse. */
	private final class NotRidden extends net.minecraft.world.entity.ai.goal.Goal {
		private final net.minecraft.world.entity.ai.goal.Goal inner;

		NotRidden(net.minecraft.world.entity.ai.goal.Goal inner) {
			this.inner = inner;
			setFlags(inner.getFlags());
		}

		@Override
		public boolean canUse() {
			return !isVehicle() && inner.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !isVehicle() && inner.canContinueToUse();
		}

		@Override
		public void start() {
			inner.start();
		}

		@Override
		public void stop() {
			inner.stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return inner.requiresUpdateEveryTick();
		}

		@Override
		public void tick() {
			inner.tick();
		}
	}

	// ---- vanilla hooks ----

	@Override
	public boolean canAgeUp() {
		return false;
	}

	/** Foals come from service and straws, not vanilla's golden carrots. */
	@Override
	public boolean canMate(Animal other) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide()) {
			trackGallop();
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (tickCount % 20 == 0) {
			settle(level);
		}
		if (tickCount % 60 == 0 && inHeat()) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 2, 0.3, 0.2, 0.3, 0.0);
		}
	}

	/**
	 * Sneak with an empty hand: the readout (and vanilla's saddle screen for a tame horse). Shears on a colt make him a gelding. A wild horse
	 * won't be ridden or saddled: it rears and throws you off (horses.md, "Wild horses"). Under 2 years a horse can't be ridden.
	 */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty() && player.isSecondaryUseActive()) {
			if (!level().isClientSide()) {
				player.sendSystemMessage(Component.literal(readout()));
			}
			if (isTamed() && !isBaby()) {
				openCustomInventoryScreen(player);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.SHEARS) && sex == Sex.MALE && !castrated && stage() == HorseStage.COLT) {
			if (!level().isClientSide()) {
				castrated = true;
				sync(level().getGameTime());
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				player.sendOverlayMessage(Component.translatable("message.farmings.gelding"));
			}
			return InteractionResult.SUCCESS;
		}
		if (isFood(stack)) {
			return fedFood(player, stack);
		}
		if (wild && !isVehicle()) {
			// vanilla would mount you whatever you hold once the item does nothing; a wild horse rears instead
			if (!level().isClientSide()) {
				makeMad();
				player.sendOverlayMessage(Component.translatable("message.farmings.horse.wild"));
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.isEmpty() && !isVehicle()) {
			if (!isBaby() && ageRealDays() < Equine.RIDING_AGE_DAYS) {
				if (!level().isClientSide()) {
					player.sendOverlayMessage(Component.translatable("message.farmings.horse.too_young"));
				}
				return InteractionResult.SUCCESS;
			}
		}
		return super.mobInteract(player, hand);
	}

	public String breedLabel() {
		List<Map.Entry<String, Integer>> parts = new ArrayList<>(composition().entrySet());
		parts.sort((x, y) -> y.getValue() - x.getValue());
		if (parts.size() == 1) {
			return HorseBreed.byCode(parts.get(0).getKey()).label();
		}
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, Integer> e : parts) {
			b.append(b.length() > 0 ? " " : "").append(HorseBreed.byCode(e.getKey()).label()).append(String.format(Locale.ROOT, " %.0f%%", e.getValue() / 100.0));
		}
		return b.toString();
	}

	public String readout() {
		StringBuilder b = new StringBuilder();
		if (wild) {
			b.append(String.format(Locale.ROOT, "WILD (settled %.1f of %.0f days), ", settledDays, dev.farmings.husbandry.WildBreeds.SETTLE_DAYS));
		}
		b.append(breedLabel()).append(' ').append(stage().label());
		b.append(String.format(Locale.ROOT, ", %.1f years, %.0f kg", ageYears(), weightKg));
		if (genome != null) {
			b.append(", ").append(look().describe());
			b.append(", MSTN ").append(genes().mstn());
			b.append(String.format(Locale.ROOT, ", speed %d, stamina %d", score(Trait.SPEED), score(Trait.STAMINA)));
			if (ageRealDays() >= Equine.RIDING_AGE_DAYS) {
				Gallop.Runner r = runner();
				b.append(String.format(Locale.ROOT, ", 402 m in %.2f s, 1,609 m in %.2f s", r.time(402), r.time(1609)));
			}
		}
		b.append(String.format(Locale.ROOT, ", fitness %.0f%%", fitness() * 100));
		if (races > 0) {
			b.append(String.format(Locale.ROOT, ", %d race%s, %d win%s", races, races == 1 ? "" : "s", wins, wins == 1 ? "" : "s"));
			for (Map.Entry<Integer, Integer> e : bestTicks.entrySet()) {
				b.append(String.format(Locale.ROOT, ", best %,d m %.2f s", e.getKey(), e.getValue() / 20.0));
			}
		}
		if (inHeat()) {
			b.append(", IN HEAT");
		}
		if (pregnant()) {
			b.append(String.format(Locale.ROOT, ", in foal, due in %.1f game days", daysToFoaling() / TimeScale.REAL_DAYS_PER_GAME_DAY));
		}
		if (nursing()) {
			b.append(", with foal at foot");
		}
		b.append(String.format(Locale.ROOT, ", health %.0f/%.0f", getHealth(), getMaxHealth()));
		return b.toString();
	}

	/** Horses are not killed for meat in Farmings (horses.md, "Meat"): only the saddle and armour drop, by vanilla's equipment drop. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
	}

	/** Natural spawns follow the vanilla animal rule (grass, light). */
	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
		if (Wild.wildReason(reason)) {
			return Animal.checkAnimalSpawnRules(dev.farmings.FarmingsEntities.HORSE, level, reason, blockPosition(), getRandom());
		}
		return true;
	}

	/** Wild and spawned horses are adults of 3 to 10 years *(proposed)*. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (genome == null) {
			HorseBreed b = HorseBreed.values()[level.getRandom().nextInt(HorseBreed.values().length)];
			Sex s = level.getRandom().nextBoolean() ? Sex.FEMALE : Sex.MALE;
			if (Wild.wildReason(reason)) {
				Wild.Group g = Wild.group(groupData, level);
				data = g;
				b = dev.farmings.husbandry.WildBreeds.horse(Wild.biome(level, blockPosition()), g.roll);
				s = g.maleGiven ? Sex.FEMALE : Sex.MALE;
				g.maleGiven = true;
				wild = true;
			}
			double age = 365 * (3 + 7 * level.getRandom().nextDouble());
			initFounder(b, s, level.getRandom().nextLong(), age, level.getLevel().getGameTime());
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
			out.putIntArray("Coat", genes.save());
		}
		out.putLong("BirthTick", birthTick);
		out.putDouble("WeightKg", weightKg);
		out.putBoolean("GrowthMarked", growthMarked);
		out.putLong("GrowthTick", growthTick);
		out.putBoolean("Castrated", castrated);
		out.putLong("BredTick", bredTick);
		out.putLong("Due", dueTick);
		out.putString("Sire", sireGenome);
		out.putIntArray("SireCoat", sireGenes);
		out.putBoolean("HasFoaled", hasFoaled);
		out.putLong("FoaledTick", foaledTick);
		out.putInt("Foalings", foalings);
		out.putLong("NextHeat", nextHeatTick);
		if (ownerId != null) {
			out.putString("HerdOwner", ownerId.toString());
		}
		out.putBoolean("Wild", wild);
		out.putDouble("SettledDays", settledDays);
		out.putString("Dam", damId);
		out.putString("SireId", sireId);
		out.putDouble("LastFed", lastFed);
		out.putDouble("LastWatered", lastWatered);
		out.putDouble("Fitness", fitness);
		out.putLong("LastWork", lastWorkTick);
		out.putLong("GainDay", gainDay);
		out.putIntArray("BestDist", bestTicks.keySet().stream().mapToInt(Integer::intValue).toArray());
		out.putIntArray("BestTicks", bestTicks.values().stream().mapToInt(Integer::intValue).toArray());
		out.putInt("Races", races);
		out.putBoolean("RaceHold", raceHold);
		out.putBoolean("NoAiBeforeRace", noAiBeforeRace);
		out.putInt("Wins", wins);
		out.putDouble("GainedToday", gainedToday);
		place().save(out);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		try {
			breed = HorseBreed.byCode(in.getStringOr("Breed", HorseBreed.THOROUGHBRED.code));
			sex = Sex.valueOf(in.getStringOr("Sex", Sex.FEMALE.name()));
			genome = in.getString("Genome").map(Genome::decode).orElse(null);
		} catch (IllegalArgumentException e) {
			Farmings.LOGGER.warn("A horse record could not be read; keeping defaults: {}", e.getMessage());
		}
		in.getIntArray("Scores").ifPresent(saved -> {
			if (saved.length <= scores.length) {
				System.arraycopy(saved, 0, scores, 0, saved.length);
			}
		});
		genes = in.getIntArray("Coat").map(HorseGenes::load).orElse(null);
		birthTick = in.getLongOr("BirthTick", 0L);
		weightKg = in.getDoubleOr("WeightKg", 0.0);
		growthMarked = in.getBooleanOr("GrowthMarked", false);
		growthTick = in.getLongOr("GrowthTick", 0L);
		castrated = in.getBooleanOr("Castrated", false);
		bredTick = in.getLongOr("BredTick", -1L);
		dueTick = in.getLongOr("Due", -1L);
		sireGenome = in.getStringOr("Sire", "");
		sireGenes = in.getIntArray("SireCoat").orElse(new int[0]);
		hasFoaled = in.getBooleanOr("HasFoaled", false);
		foaledTick = in.getLongOr("FoaledTick", -1L);
		foalings = in.getIntOr("Foalings", 0);
		nextHeatTick = in.getLongOr("NextHeat", -1L);
		String own = in.getStringOr("HerdOwner", "");
		ownerId = own.isEmpty() ? null : UUID.fromString(own);
		wild = in.getBooleanOr("Wild", false);
		settledDays = in.getDoubleOr("SettledDays", 0);
		damId = in.getStringOr("Dam", "");
		sireId = in.getStringOr("SireId", "");
		lastFed = in.getDoubleOr("LastFed", 1);
		lastWatered = in.getDoubleOr("LastWatered", 1);
		fitness = in.getDoubleOr("Fitness", 0);
		lastWorkTick = in.getLongOr("LastWork", Long.MIN_VALUE);
		gainDay = in.getLongOr("GainDay", -1L);
		gainedToday = in.getDoubleOr("GainedToday", 0);
		int[] dist = in.getIntArray("BestDist").orElse(new int[0]);
		int[] ticks = in.getIntArray("BestTicks").orElse(new int[0]);
		bestTicks.clear();
		for (int i = 0; i < Math.min(dist.length, ticks.length); i++) {
			bestTicks.put(dist[i], ticks[i]);
		}
		races = in.getIntOr("Races", 0);
		wins = in.getIntOr("Wins", 0);
		if (in.getBooleanOr("RaceHold", false)) {
			setNoAi(in.getBooleanOr("NoAiBeforeRace", false));     // the race isn't saved: it ended with the save
		}
		raceHold = false;
		place().load(in);
		sync(level().getGameTime());
	}

	// ---- sounds: vanilla's horse ----

	@Override
	protected SoundEvent getAmbientSound() {
		return isBaby() ? SoundEvents.HORSE_AMBIENT_BABY : SoundEvents.HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return isBaby() ? SoundEvents.HORSE_HURT_BABY : SoundEvents.HORSE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return isBaby() ? SoundEvents.HORSE_DEATH_BABY : SoundEvents.HORSE_DEATH;
	}

	@Override
	protected @Nullable SoundEvent getEatingSound() {
		return isBaby() ? SoundEvents.HORSE_EAT_BABY : SoundEvents.HORSE_EAT;
	}

	@Override
	protected SoundEvent getAngrySound() {
		return isBaby() ? SoundEvents.HORSE_ANGRY_BABY : SoundEvents.HORSE_ANGRY;
	}
}

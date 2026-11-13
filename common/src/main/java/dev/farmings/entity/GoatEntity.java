package dev.farmings.entity;

import dev.farmings.Farmings;
import dev.farmings.genetics.BreedingSeed;
import dev.farmings.genetics.Founders;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.GoatGenes;
import dev.farmings.genetics.GoatLook;
import dev.farmings.genetics.Offspring;
import dev.farmings.genetics.Rng;
import dev.farmings.genetics.Ruleset;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.Flock;
import dev.farmings.husbandry.GoatBreed;
import dev.farmings.husbandry.GoatStage;
import dev.farmings.husbandry.Herd;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.parlor.MilkKind;
import java.util.ArrayList;
import java.util.EnumSet;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Farmings goat (docs/animals/goats.md): Saanen, Nubian or Boer, with its own genome, coat genes, breeding season, kidding, milk for
 * the parlor's small stalls, and meat. It lives in the sheep shed beside sheep (its own pens) through {@link SmallRuminant}, is herded by
 * the sheep dog, and is tamed in the corral. Rules live in plain Java ({@link Herd}, {@link GoatStage}); this class keeps the state, as
 * {@link SheepEntity} does for sheep, and is written apart from it for the same reason.
 */
public class GoatEntity extends Animal implements Wild.Animal, SmallRuminant {
	private static final EntityDataAccessor<Integer> DATA_BREED = SynchedEntityData.defineId(GoatEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(GoatEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> DATA_LOOK = SynchedEntityData.defineId(GoatEntity.class, EntityDataSerializers.LONG);
	/** Shares of the dairy (Saanen, Nubian), meat (Boer) and lop-eared (Nubian, Boer) breeds, 0-100, for the body and ears. */
	private static final EntityDataAccessor<Integer> DATA_BODY = SynchedEntityData.defineId(GoatEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_UDDER = SynchedEntityData.defineId(GoatEntity.class, EntityDataSerializers.BOOLEAN);

	/** How far a buck reaches a doe in heat: the same pen or paddock *(proposed: as sheep, 8 blocks)*. */
	public static final double SERVICE_RANGE = 8.0;

	private GoatBreed breed = GoatBreed.SAANEN;
	private Sex sex = Sex.FEMALE;
	private @Nullable Genome genome;
	private int[] scores = new int[Trait.values().length];
	private @Nullable GoatGenes genes;
	private long birthTick;
	private double weightKg;
	private long growthTick;
	private boolean growthMarked;
	private boolean castrated;
	private long bredTick = -1;
	private long dueTick = -1;
	private String sireGenome = "";
	private int[] sireGenes = new int[0];
	private boolean hasKidded;
	private long kiddedTick = -1;
	private int kiddings;
	/** Kids born alive at the last kidding; they nurse until weaning. */
	private int lastLitter;
	private long nextHeatTick = -1;
	private @Nullable UUID ownerId;
	private boolean wild;
	private double settledDays;
	private String damId = "";
	private String sireId = "";
	/** Parlor milk in the udder, kg; only Saanen and Nubian blood gives any. Refilled once per game day. */
	private double milkKg;
	private long lastMilkDay = -1;
	private double lastFed = 1;
	private double lastWatered = 1;
	/** Made on first use: goals register in the super constructor. */
	private @Nullable Placement place;
	private int forcedKids;
	private boolean forcedStillborn;
	private int forcedDayOfYear = -1;
	private final List<String> lastKidding = new ArrayList<>();
	private dev.farmings.feed.Ration.Analysis ration = dev.farmings.feed.Ration.Analysis.BALANCED;
	private @Nullable BlockPos lure;

	public GoatEntity(EntityType<? extends GoatEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		// The vanilla goat's 10 health and 0.2 speed; step height 1 so goats climb a block without jumping (goats.md, climbing; proposed).
		// Follow range 64 as sheep, so a goat can path across the shed's pasture.
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.2F).add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BREED, GoatBreed.SAANEN.ordinal());
		builder.define(DATA_STAGE, GoatStage.DOELING.ordinal());
		builder.define(DATA_LOOK, -1L);
		builder.define(DATA_BODY, 0);
		builder.define(DATA_UDDER, false);
	}

	// ---- what the client knows ----

	public @Nullable GoatLook syncedLook() {
		long v = entityData.get(DATA_LOOK);
		return v < 0 ? null : GoatLook.unpack(v);
	}

	public GoatBreed syncedBreed() {
		GoatBreed[] all = GoatBreed.values();
		return all[Math.max(0, Math.min(all.length - 1, entityData.get(DATA_BREED)))];
	}

	public GoatStage syncedStage() {
		GoatStage[] all = GoatStage.values();
		return all[Math.max(0, Math.min(all.length - 1, entityData.get(DATA_STAGE)))];
	}

	/** Dairy share 0-1. */
	public float syncedDairy() {
		return (entityData.get(DATA_BODY) & 0xFF) / 100.0F;
	}

	/** Meat (Boer) share 0-1. */
	public float syncedMeat() {
		return (entityData.get(DATA_BODY) >> 8 & 0xFF) / 100.0F;
	}

	/** Lop-eared (Nubian and Boer) share 0-1. */
	public float syncedLop() {
		return (entityData.get(DATA_BODY) >> 16 & 0xFF) / 100.0F;
	}

	public boolean syncedUdder() {
		return entityData.get(DATA_UDDER);
	}

	private void sync(long now) {
		entityData.set(DATA_BREED, breed.ordinal());
		GoatStage stage = stageAt(now);
		entityData.set(DATA_STAGE, stage.ordinal());
		if (genome != null) {
			entityData.set(DATA_LOOK, look().pack());
			Map<String, Integer> c = genome.breedCompositionBp();
			int dairy = (int) Math.round(Herd.dairyShare(c) * 100);
			int meat = c.getOrDefault("BOE", 0) / 100;
			int lop = (c.getOrDefault("NUB", 0) + c.getOrDefault("BOE", 0)) / 100;
			entityData.set(DATA_BODY, dairy | meat << 8 | lop << 16);
		}
		entityData.set(DATA_UDDER, sex == Sex.FEMALE && (hasKidded || bredTick >= 0) && !stage.kid());
		boolean kid = stage.kid();
		if (kid != isBaby()) {
			setAge(kid ? -1 : 0);
		}
	}

	// ---- the animal's facts ----

	/** A founder of one breed: its genes, coat genes and a starting age and weight. */
	public void initFounder(GoatBreed breed, Sex sex, long seed, double ageRealDays, double weightKg, long now) {
		Founders.Result f = Founders.create(breed.code, sex, seed, Ruleset.defaults());
		this.breed = breed;
		this.sex = sex;
		this.genome = f.genome();
		this.genes = GoatGenes.founder(breed.code, seed);
		for (Trait t : Species.GOAT.traits()) {
			scores[t.ordinal()] = f.phenotype().score(t);
		}
		this.birthTick = now - TimeScale.realDaysToTicks(ageRealDays);
		this.weightKg = weightKg;
		this.castrated = false;
		this.bredTick = -1;
		this.hasKidded = false;
		this.kiddedTick = -1;
		sync(now);
	}

	/** A newborn kid from {@link Offspring}, at its share of the litter's birth weight. */
	public void initKid(Offspring.Result r, GoatGenes coat, int litter, long now) {
		this.breed = Herd.topBreed(r.genome().breedCompositionBp());
		this.sex = r.sex();
		this.genome = r.genome();
		this.genes = coat;
		for (Trait t : Species.GOAT.traits()) {
			scores[t.ordinal()] = r.phenotype().score(t);
		}
		this.birthTick = now;
		this.weightKg = Herd.birthKg(r.genome().breedCompositionBp(), litter);
		this.bredTick = -1;
		this.hasKidded = false;
		sync(now);
	}

	public GoatBreed breed() {
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

	/** Coat genes; a goat with none recorded gets founder genes of its breed from its genome hash, once. */
	public GoatGenes genes() {
		if (genes == null) {
			long seed = genome == null ? getUUID().getLeastSignificantBits() : Long.parseUnsignedLong(genome.hash().substring(0, 15), 16);
			genes = GoatGenes.founder(breed.code, seed);
		}
		return genes;
	}

	public void setGenesForTest(GoatGenes g) {
		this.genes = g;
		sync(level().getGameTime());
	}

	public GoatLook look() {
		return GoatLook.of(genes(), sex);
	}

	@Override
	public double weightKg() {
		return weightKg;
	}

	public void setWeightKg(double kg) {
		this.weightKg = kg;
		sync(level().getGameTime());
	}

	@Override
	public double ageRealDays() {
		return TimeScale.ticksToRealDays(level().getGameTime() - birthTick);
	}

	public GoatStage stage() {
		return stageAt(level().getGameTime());
	}

	private GoatStage stageAt(long now) {
		double age = TimeScale.ticksToRealDays(now - birthTick);
		return sex == Sex.MALE ? GoatStage.male(composition(), age, weightKg, castrated) : GoatStage.female(age, hasKidded, false);
	}

	public boolean castrated() {
		return castrated;
	}

	@Override
	public String damId() {
		return damId;
	}

	public String sireId() {
		return sireId;
	}

	@Override
	public String herdId() {
		return CattleEntity.herdId(genome);
	}

	public int kiddings() {
		return kiddings;
	}

	@Override
	public @Nullable UUID owner() {
		return ownerId;
	}

	@Override
	public void setOwner(@Nullable UUID owner) {
		this.ownerId = owner;
	}

	/** A P/P doe: intersex, she can't conceive and gives no milk (goats.md, "Polled"). */
	public boolean intersex() {
		return sex == Sex.FEMALE && genes().polledHomozygous();
	}

	// ---- breeding and kidding ----

	public void setDayOfYearForTest(int day) {
		this.forcedDayOfYear = day;
	}

	/** The day of the year by the world clock at the livestock scale (the sheep calendar). */
	public int dayOfYear(long now) {
		return forcedDayOfYear >= 0 ? forcedDayOfYear : Flock.dayOfYear(TimeScale.ticksToRealDays(now));
	}

	public boolean inSeason(long now) {
		return Herd.inSeason(breed, dayOfYear(now));
	}

	public boolean inHeat() {
		return sex == Sex.FEMALE && nextHeatTick >= 0 && level().getGameTime() >= nextHeatTick
				&& level().getGameTime() < nextHeatTick + TimeScale.realDaysToTicks(Herd.HEAT_DAYS);
	}

	public void startHeatForDisplay() {
		this.nextHeatTick = level().getGameTime();
	}

	public boolean pregnant() {
		return bredTick >= 0;
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
		return Herd.intakeKgDm(weightKg, lactating());
	}

	@Override
	public double dailyWaterL() {
		return dailyDryMatterKg() * waterLPerKgDm();
	}

	public boolean canConceive() {
		return genome != null && !wild && inHeat() && bredTick < 0 && !intersex();
	}

	/** Conceives to a goat sire; a sterile polled/polled buck sires nothing (goats.md). */
	public boolean conceive(Genome sire, GoatGenes sireCoat, long now) {
		if (!canConceive() || sire.species() != Species.GOAT || GoatLook.sterileBuck(sireCoat)) {
			return false;
		}
		this.bredTick = now;
		this.sireGenome = sire.encode();
		this.sireGenes = sireCoat.save();
		this.dueTick = now + TimeScale.realDaysToTicks(Herd.GESTATION_DAYS);
		this.nextHeatTick = -1;
		sync(now);
		return true;
	}

	public void setDueInForTest(double realDays) {
		this.dueTick = level().getGameTime() + TimeScale.realDaysToTicks(realDays);
	}

	public void makeDueNow() {
		this.dueTick = level().getGameTime();
	}

	/** Real days to kidding, or -1 if she is not in kid. */
	public double daysToKidding() {
		return bredTick < 0 || dueTick < 0 ? -1 : TimeScale.ticksToRealDays(dueTick - level().getGameTime());
	}

	public double daysSinceKidding() {
		return hasKidded ? TimeScale.ticksToRealDays(level().getGameTime() - kiddedTick) : Double.MAX_VALUE;
	}

	public void forceKidsForTest(int n) {
		this.forcedKids = n;
	}

	public void forceStillbornForTest() {
		this.forcedStillborn = true;
	}

	public List<String> lastKidding() {
		return lastKidding;
	}

	private boolean cycles(long now) {
		if (sex != Sex.FEMALE || genome == null || bredTick >= 0 || intersex() || !inSeason(now)) {
			return false;
		}
		double age = TimeScale.ticksToRealDays(now - birthTick);
		if (!hasKidded) {
			return Herd.canFirstBreed(composition(), false, age, weightKg);
		}
		return TimeScale.ticksToRealDays(now - kiddedTick) >= Herd.WAIT_AFTER_KIDDING_DAYS;
	}

	private void updateBreeding(ServerLevel level, long now) {
		if (sex != Sex.FEMALE || genome == null) {
			return;
		}
		if (bredTick >= 0) {
			if (!sireGenome.isEmpty() && dueTick >= 0 && now >= dueTick) {
				kid(level, now);
			}
			return;
		}
		if (!cycles(now)) {
			nextHeatTick = -1;
			return;
		}
		long cycle = TimeScale.realDaysToTicks(Herd.CYCLE_DAYS);
		if (nextHeatTick < 0) {
			nextHeatTick = now + Math.floorMod(genome.hash().hashCode(), cycle);
		} else if (now >= nextHeatTick + TimeScale.realDaysToTicks(Herd.HEAT_DAYS)) {
			nextHeatTick += cycle * ((now - nextHeatTick) / cycle + 1);
		}
		if (inHeat()) {
			serveFromNearbyBuck(level, now);
		}
	}

	/** Natural service: the nearest intact buck within reach serves a doe in heat. */
	private void serveFromNearbyBuck(ServerLevel level, long now) {
		GoatEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (GoatEntity g : level.getEntitiesOfClass(GoatEntity.class, getBoundingBox().inflate(SERVICE_RANGE))) {
			if (g != this && g.sex == Sex.MALE && g.stage() == GoatStage.BUCK && g.genome != null && !g.wild) {
				double d = g.distanceToSqr(this);
				if (d < bestD) {
					bestD = d;
					best = g;
				}
			}
		}
		if (best != null) {
			conceive(best.genome, best.genes(), now);
		}
	}

	/** She kids: one to three kids by her litter odds; a stillborn kid isn't spawned (goats.md, "Stillborn kids"). */
	private void kid(ServerLevel level, long now) {
		Genome sire = Genome.decode(sireGenome);
		String world = Long.toString(level.getSeed());
		String eventId = "kidding:" + getUUID() + ":" + now;
		Rng rng = new Rng(BreedingSeed.seed(world, eventId + ":litter", genome.hash(), sire.hash(), Ruleset.defaults().version()));
		boolean forced = forcedKids > 0;
		int n = forced ? forcedKids : Herd.litter(composition(), !hasKidded, score(Trait.FERTILITY)).roll(rng.next());
		forcedKids = 0;
		lastKidding.clear();
		GoatGenes sireCoat = GoatGenes.load(sireGenes);
		int live = 0;
		for (int i = 0; i < n; i++) {
			String id = eventId + (i == 0 ? "" : ":kid" + i);
			double roll = rng.next();
			if (forcedStillborn || (!forced && roll < Herd.stillbirthRate(n))) {
				lastKidding.add("stillborn");
				continue;
			}
			Offspring.Result r = Offspring.create(genome, sire, id, world, Ruleset.defaults(), null);
			GoatEntity kid = dev.farmings.FarmingsEntities.GOAT.create(level, EntitySpawnReason.BREEDING);
			if (kid == null) {
				continue;
			}
			GoatGenes coat = GoatGenes.inherit(genes(), sireCoat != null ? sireCoat : GoatGenes.founder(Herd.topBreed(sire.breedCompositionBp()).code, 0),
					BreedingSeed.seed(world, id, genome.hash(), sire.hash(), Ruleset.defaults().version()));
			kid.initKid(r, coat, n, now);
			kid.ownerId = ownerId;
			kid.damId = herdId();
			kid.sireId = CattleEntity.herdId(sire);
			kid.place().copyHomeFrom(place());
			kid.setPos(getX() + 0.6 * (i - (n - 1) / 2.0), getY(), getZ() + 0.6);
			level.addFreshEntity(kid);
			lastKidding.add(r.sex().name().toLowerCase(Locale.ROOT));
			live++;
		}
		forcedStillborn = false;
		boolean first = !hasKidded;
		hasKidded = true;
		kiddings++;
		lastLitter = live;
		kiddedTick = now;
		bredTick = -1;
		dueTick = -1;
		sireGenome = "";
		sireGenes = new int[0];
		nextHeatTick = -1;
		lastMilkDay = -1;
		firstLactation = first;
		sync(now);
		updateMilk(now);
	}

	// ---- milk ----

	private boolean firstLactation;

	/** Days since kidding while she is in milk (Herd.LACTATION_DAYS), else 0. */
	public int daysInMilk() {
		if (sex != Sex.FEMALE || !hasKidded) {
			return 0;
		}
		int d = (int) Math.floor(TimeScale.ticksToRealDays(level().getGameTime() - kiddedTick)) + 1;
		return d <= Herd.LACTATION_DAYS ? d : 0;
	}

	/** Nursing kids, or a dairy doe in milk: the lactating intake and water (goats.md). */
	public boolean lactating() {
		return kidsNursing() > 0 || (Herd.dairyShare(composition()) > 0 && daysInMilk() > 0);
	}

	@Override
	public double milkKg() {
		return milkKg;
	}

	@Override
	public double takeMilk() {
		double m = milkKg;
		milkKg = 0;
		return m;
	}

	/** A doe that gives parlor milk now: Saanen or Nubian blood, in milk, not intersex. */
	@Override
	public boolean dairy() {
		return Herd.dairyShare(composition()) > 0 && daysInMilk() > 0 && !intersex();
	}

	private void updateMilk(long now) {
		if (sex != Sex.FEMALE || !hasKidded || intersex()) {
			milkKg = 0;
			return;
		}
		long day = (now - kiddedTick) / TimeScale.TICKS_PER_GAME_DAY;
		if (day != lastMilkDay) {
			lastMilkDay = day;
			int dim = (int) Math.floor(TimeScale.ticksToRealDays(now - kiddedTick)) + 1;
			milkKg = Herd.milkKg(composition(), dim, score(Trait.MILK_YIELD), firstLactation) * Math.min(lastFed, lastWatered);
		}
	}

	/** For game tests and art: she kidded this many real days ago. */
	public void setKiddedForTest(double realDaysAgo, int litter) {
		long now = level().getGameTime();
		hasKidded = true;
		kiddings = Math.max(1, kiddings);
		lastLitter = litter;
		kiddedTick = now - TimeScale.realDaysToTicks(realDaysAgo);
		lastMilkDay = -1;
		firstLactation = false;
		sync(now);
		updateMilk(now);
	}

	@Override
	public double milkFatPct() {
		return Herd.fatPct(composition());
	}

	@Override
	public double milkProteinPct() {
		return Herd.proteinPct(composition());
	}

	@Override
	public MilkKind milkKind() {
		return MilkKind.GOAT;
	}

	// ---- feed, water and housing ----

	public int kidsNursing() {
		if (!hasKidded || TimeScale.ticksToRealDays(level().getGameTime() - kiddedTick) >= GoatStage.WEANING_DAYS) {
			return 0;
		}
		return lastLitter;
	}

	@Override
	public double dryMatterIntakeKgPerDay() {
		return stage().kid() ? 0 : Herd.intakeKgDm(weightKg, lactating());
	}

	public dev.farmings.feed.Ration.Analysis ration() {
		return ration;
	}

	@Override
	public void recordRation(dev.farmings.feed.Ration.Analysis a) {
		ration = ration.toward(a, dev.farmings.feed.Ration.SMOOTHING);
	}

	@Override
	public void recordTending(double fed, double watered) {
		this.lastFed = fed;
		this.lastWatered = watered;
	}

	@Override
	public double lastFed() {
		return lastFed;
	}

	@Override
	public double lastWatered() {
		return lastWatered;
	}

	@Override
	public Placement place() {
		if (place == null) {
			place = new Placement();
		}
		return place;
	}

	// ---- the shared face (SmallRuminant) ----

	@Override
	public Animal animal() {
		return this;
	}

	@Override
	public Species species() {
		return Species.GOAT;
	}

	@Override
	public boolean young() {
		return stage().kid();
	}

	@Override
	public double daysToBirth() {
		return daysToKidding();
	}

	@Override
	public double daysSinceBirth() {
		return daysSinceKidding();
	}

	@Override
	public int youngNursing() {
		return kidsNursing();
	}

	@Override
	public double penBlocks() {
		return Herd.PEN_M2_PER_GOAT;
	}

	@Override
	public double manureKgPerDay() {
		return Herd.manureKgPerDay(weightKg);
	}

	@Override
	public double waterLPerKgDm() {
		return Herd.waterLPerKgDm(lactating());
	}

	@Override
	public double browseShare() {
		return Herd.BROWSE_SHARE;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
		this.goalSelector.addGoal(2, new RamGoal(this));
		this.goalSelector.addGoal(3, place().goal(this, this::isBaby));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		Wild.addGoals(this, this.goalSelector, this::isWild);
	}

	/**
	 * A wild goat rams a player (not in creative or spectator) or a hostile mob that comes close (goats.md, "Wild goats"): it charges, and on
	 * contact deals the vanilla goat's 2 damage *(the vanilla value)* and half its knock-back *(proposed)*. It waits 30-300 s between rams, vanilla's goat ram cooldown. Tame goats don't.
	 */
	static final class RamGoal extends Goal {
		static final double REACH = 6.0;
		private final GoatEntity goat;
		private net.minecraft.world.entity.@Nullable LivingEntity target;
		private int charging;

		RamGoal(GoatEntity goat) {
			this.goat = goat;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!goat.isWild() || goat.isBaby()) {
				return false;
			}
			if (goat.level().getGameTime() < goat.ramReadyAt) {
				return false;
			}
			Player p = goat.level().getNearestPlayer(goat, REACH);
			if (p != null && !p.isCreative() && !p.isSpectator()) {
				target = p;
				return true;
			}
			List<net.minecraft.world.entity.monster.Monster> mobs = goat.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
					goat.getBoundingBox().inflate(REACH), m -> m.isAlive() && m.distanceTo(goat) <= REACH);
			target = mobs.isEmpty() ? null : mobs.get(0);
			return target != null;
		}

		@Override
		public boolean canContinueToUse() {
			return target != null && target.isAlive() && charging < 60 && goat.isWild();
		}

		@Override
		public void start() {
			charging = 0;
		}

		@Override
		public void stop() {
			target = null;
			goat.ramReadyAt = goat.level().getGameTime() + 600 + goat.getRandom().nextInt(5400);
		}

		@Override
		public void tick() {
			if (target == null) {
				return;
			}
			goat.getLookControl().setLookAt(target);
			goat.getNavigation().stop();
			goat.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.6);     // a straight charge, not a path
			charging++;
			if (goat.distanceToSqr(target) < 4.0 && goat.level() instanceof ServerLevel server) {     // within 2 blocks: the head reaches
				target.hurtServer(server, goat.damageSources().mobAttack(goat), 2.0F);
				double dx = target.getX() - goat.getX();
				double dz = target.getZ() - goat.getZ();
				double d = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
				target.push(dx / d * 1.25, 0.25, dz / d * 1.25);
				target.needsSync = true;
				goat.playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0F, 1.0F);
				goat.rams++;
				charging = 60;
			}
		}
	}

	/** Rams landed since it was loaded (not saved); game tests read it, as a peaceful world zeroes the damage. */
	private int rams;

	public int rams() {
		return rams;
	}

	/** Game time before which it won't ram again (not saved: a loaded goat may ram at once). */
	private long ramReadyAt;

	/** Game tests: a wild goat rams at the next chance. */
	public void readyToRamForTest() {
		ramReadyAt = 0;
	}

	// ---- the once-a-second bookkeeping ----

	public void settle(ServerLevel level) {
		long now = level.getGameTime();
		grow(now);
		updateMilk(now);
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
		boolean male = sex == Sex.MALE;
		double fed = place().shedController() != null && !stageAt(now).kid() ? lastFed : 1;
		double mature = Herd.matureWeightKg(composition(), male);
		double gain = Herd.gainKg(composition(), male, age - span, span, fed) * (stageAt(now).kid() ? dev.farmings.feed.Ration.lambFactor(ration) : 1);
		if (gain > 0 && weightKg < mature) {
			weightKg = Math.min(mature, weightKg + gain);
		}
	}

	/** For game tests: {@code realDays} older, growing all that time. */
	public void advanceForTest(double realDays) {
		long ticks = TimeScale.realDaysToTicks(realDays);
		birthTick -= ticks;
		growthTick -= ticks;
	}

	// ---- vanilla hooks ----

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
		if (tickCount % 60 == 0 && inHeat()) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 2, 0.3, 0.2, 0.3, 0.0);
		}
	}

	/** Empty hand reads the goat. Shears on a buckling kid make him a wether (as rams and bulls are castrated). */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) {
			if (!level().isClientSide()) {
				player.sendSystemMessage(Component.literal(readout()));
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.SHEARS) && sex == Sex.MALE && !castrated && stage() == GoatStage.KID) {
			if (!level().isClientSide()) {
				castrated = true;
				sync(level().getGameTime());
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				player.sendOverlayMessage(Component.translatable("message.farmings.goat_wether"));
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	public String breedLabel() {
		List<Map.Entry<String, Integer>> parts = new ArrayList<>(composition().entrySet());
		parts.sort((x, y) -> y.getValue() - x.getValue());
		if (parts.size() == 1) {
			return GoatBreed.byCode(parts.get(0).getKey()).label();
		}
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, Integer> e : parts) {
			b.append(b.length() > 0 ? " " : "").append(GoatBreed.byCode(e.getKey()).label()).append(String.format(Locale.ROOT, " %.0f%%", e.getValue() / 100.0));
		}
		return b.toString();
	}

	public String readout() {
		StringBuilder b = new StringBuilder();
		if (wild) {
			b.append(String.format(Locale.ROOT, "WILD (settled %.1f of %.0f days), ", settledDays, dev.farmings.husbandry.WildBreeds.SETTLE_DAYS));
		}
		b.append(breedLabel()).append(' ').append(sex == Sex.FEMALE ? "doe" : castrated ? "wether" : "buck");
		b.append(String.format(Locale.ROOT, ", %.0f months, %.0f kg, %s", ageRealDays() / 30.4, weightKg, stage().label()));
		if (genome != null) {
			b.append(", ").append(look().describe());
		}
		if (inHeat()) {
			b.append(", IN HEAT");
		}
		if (sex == Sex.FEMALE && !stage().kid() && !pregnant() && !intersex() && !inSeason(level().getGameTime())) {
			b.append(", ANOESTROUS (out of season)");
		}
		if (pregnant()) {
			b.append(String.format(Locale.ROOT, ", in kid, due in %.1f game days", daysToKidding() / TimeScale.REAL_DAYS_PER_GAME_DAY));
		}
		if (kidsNursing() > 0) {
			b.append(", nursing ").append(kidsNursing());
		}
		if (dairy()) {
			b.append(String.format(Locale.ROOT, ", day %d in milk, %.2f kg in udder", daysInMilk(), milkKg));
		}
		if (place().shedController() != null) {
			b.append(String.format(Locale.ROOT, ", fed %.0f%%, watered %.0f%%", lastFed * 100, lastWatered * 100));
		}
		b.append(String.format(Locale.ROOT, ", health %.0f/%.0f", getHealth(), getMaxHealth()));
		return b.toString();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		int items = Herd.meatItems(composition(), weightKg, ageRealDays());
		net.minecraft.world.level.ItemLike meat = dev.farmings.FarmingsItems.goatMeat(ageRealDays() < Herd.KID_MEAT_DAYS, isOnFire());
		int max = new ItemStack(meat).getMaxStackSize();
		while (items > 0) {
			int n = Math.min(max, items);
			spawnAtLocation(level, dev.farmings.item.Perishables.stamp(new ItemStack(meat, n), level.getGameTime()));
			items -= n;
		}
	}

	/** Natural spawns follow the vanilla goat's rule (on its spawnable blocks, in light), as they take its spawn entries (goats.md). */
	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
		if (Wild.wildReason(reason)) {
			return net.minecraft.world.entity.animal.goat.Goat.checkGoatSpawnRules(net.minecraft.world.entity.EntityTypes.GOAT, level, reason, blockPosition(), getRandom());
		}
		return true;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (genome == null) {
			GoatBreed b = GoatBreed.values()[level.getRandom().nextInt(GoatBreed.values().length)];
			Sex s = level.getRandom().nextBoolean() ? Sex.FEMALE : Sex.MALE;
			if (Wild.wildReason(reason)) {
				// a wild founder: Saanen, Nubian or Boer by the wild odds, one buck per spawn group (goats.md)
				Wild.Group g = Wild.group(groupData, level);
				data = g;
				b = dev.farmings.husbandry.WildBreeds.goat(Math.floorMod(g.roll, 100) / 100.0);
				s = g.maleGiven ? Sex.FEMALE : Sex.MALE;
				g.maleGiven = true;
				wild = true;
			}
			double age = Herd.FIRST_BREEDING_AGE_DAYS + 30;
			initFounder(b, s, level.getRandom().nextLong(), age, Herd.targetKg(Map.of(b.code, 10_000), s == Sex.MALE, age), level.getLevel().getGameTime());
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
		out.putBoolean("HasKidded", hasKidded);
		out.putLong("KiddedTick", kiddedTick);
		out.putInt("Kiddings", kiddings);
		out.putInt("LastLitter", lastLitter);
		out.putBoolean("FirstLactation", firstLactation);
		out.putLong("NextHeat", nextHeatTick);
		if (ownerId != null) {
			out.putString("Owner", ownerId.toString());
		}
		out.putBoolean("Wild", wild);
		out.putDouble("SettledDays", settledDays);
		out.putString("Dam", damId);
		out.putString("SireId", sireId);
		out.putDouble("MilkKg", milkKg);
		out.putLong("LastMilkDay", lastMilkDay);
		out.putDouble("LastFed", lastFed);
		out.putDouble("LastWatered", lastWatered);
		place().save(out);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		try {
			breed = GoatBreed.byCode(in.getStringOr("Breed", GoatBreed.SAANEN.code));
			sex = Sex.valueOf(in.getStringOr("Sex", Sex.FEMALE.name()));
			genome = in.getString("Genome").map(Genome::decode).orElse(null);
		} catch (IllegalArgumentException e) {
			Farmings.LOGGER.warn("A goat record could not be read; keeping defaults: {}", e.getMessage());
		}
		in.getIntArray("Scores").ifPresent(saved -> {
			if (saved.length <= scores.length) {
				System.arraycopy(saved, 0, scores, 0, saved.length);
			}
		});
		genes = in.getIntArray("Coat").map(GoatGenes::load).orElse(null);
		birthTick = in.getLongOr("BirthTick", 0L);
		weightKg = in.getDoubleOr("WeightKg", 0.0);
		growthMarked = in.getBooleanOr("GrowthMarked", false);
		growthTick = in.getLongOr("GrowthTick", 0L);
		castrated = in.getBooleanOr("Castrated", false);
		bredTick = in.getLongOr("BredTick", -1L);
		dueTick = in.getLongOr("Due", -1L);
		sireGenome = in.getStringOr("Sire", "");
		sireGenes = in.getIntArray("SireCoat").orElse(new int[0]);
		hasKidded = in.getBooleanOr("HasKidded", false);
		kiddedTick = in.getLongOr("KiddedTick", -1L);
		kiddings = in.getIntOr("Kiddings", 0);
		lastLitter = in.getIntOr("LastLitter", 0);
		firstLactation = in.getBooleanOr("FirstLactation", false);
		nextHeatTick = in.getLongOr("NextHeat", -1L);
		String own = in.getStringOr("Owner", "");
		ownerId = own.isEmpty() ? null : UUID.fromString(own);
		wild = in.getBooleanOr("Wild", false);
		settledDays = in.getDoubleOr("SettledDays", 0);
		damId = in.getStringOr("Dam", "");
		sireId = in.getStringOr("SireId", "");
		milkKg = in.getDoubleOr("MilkKg", 0);
		lastMilkDay = in.getLongOr("LastMilkDay", -1L);
		lastFed = in.getDoubleOr("LastFed", 1);
		lastWatered = in.getDoubleOr("LastWatered", 1);
		place().load(in);
		sync(level().getGameTime());
	}

	// ---- sounds: the vanilla goat's ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.GOAT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GOAT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GOAT_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.GOAT_STEP, 0.15F, 1.0F);
	}
}

package dev.farmings.entity;

import dev.farmings.Farmings;
import dev.farmings.genetics.BreedingSeed;
import dev.farmings.genetics.Founders;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.Offspring;
import dev.farmings.genetics.Rng;
import dev.farmings.genetics.Ruleset;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.SheepGenes;
import dev.farmings.genetics.SheepLook;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.Flock;
import dev.farmings.husbandry.SheepBreed;
import dev.farmings.husbandry.SheepStage;
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
 * A sheep (docs/animals/sheep.md): an individual with a breed mix, a sex, a sheep genome, coat genes, an age, a weight and a fleece. Ages,
 * pregnancy, fleece growth and lactation are counted in game ticks and read through {@link TimeScale}; all of it is saved.
 *
 * <p>Rules live in plain Java ({@link Flock}, {@link SheepStage}); this class keeps the state and does the Minecraft side. It is written
 * apart from {@link CattleEntity} rather than sharing a base class: the two share rules, not state, and the cattle entity is guarded by its
 * game tests as it is.
 */
public class SheepEntity extends Animal implements Wild.Animal {
	private static final EntityDataAccessor<Integer> DATA_BREED = SynchedEntityData.defineId(SheepEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(SheepEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> DATA_LOOK = SynchedEntityData.defineId(SheepEntity.class, EntityDataSerializers.LONG);
	/** Fleece growth as a share of a year, 0 to 2, for the renderer. */
	private static final EntityDataAccessor<Float> DATA_FLEECE = SynchedEntityData.defineId(SheepEntity.class, EntityDataSerializers.FLOAT);
	/** Shares of the dairy (East Friesian) and meat (Suffolk, Dorper) breeds, 0-100, for the body shape. */
	private static final EntityDataAccessor<Integer> DATA_BODY = SynchedEntityData.defineId(SheepEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_UDDER = SynchedEntityData.defineId(SheepEntity.class, EntityDataSerializers.BOOLEAN);

	/** How far a ram reaches a ewe in heat: the same pen or paddock *(proposed: 8 blocks)*. */
	public static final double SERVICE_RANGE = 8.0;

	private SheepBreed breed = SheepBreed.MERINO;
	private Sex sex = Sex.FEMALE;
	private @Nullable Genome genome;
	private int[] scores = new int[Trait.values().length];
	private @Nullable SheepGenes genes;
	private long birthTick;
	private double weightKg;
	private long growthTick;
	private boolean growthMarked;
	private boolean castrated;
	private boolean retired;
	/** Game time the fleece started growing (birth or the last shearing). */
	private long fleeceTick;
	/** Game time the coat was last shed (hair sheep). */
	private long shedTick;
	private long bredTick = -1;
	private long dueTick = -1;
	private String sireGenome = "";
	private int[] sireGenes = new int[0];
	private boolean hasLambed;
	private long lambedTick = -1;
	private int lambings;
	/** Lambs born at the last lambing (alive); they nurse until weaning. */
	private int lastLitter;
	private long nextHeatTick = -1;
	private @Nullable UUID ownerId;
	/** Wild until tamed in a corral (docs/taming.md). */
	private boolean wild;
	private double settledDays;
	private String damId = "";
	private String sireId = "";
	/** Parlor milk in the udder, kg; only East Friesian blood gives any (Flock.milkKg). Refilled once per game day. */
	private double milkKg;
	private long lastMilkDay = -1;
	private double lastFed = 1;
	private double lastWatered = 1;
	private double dirty;
	/** Where the shed controller (or the parlor, the shearing stand) wants her, and the controller of her shed. Not all saved. */
	private @Nullable BlockPos home;
	private @Nullable BlockPos shedController;
	private @Nullable BlockPos target;
	private java.util.@Nullable List<BlockPos> route;
	private int routeIndex;
	private boolean routeHome;
	/** The shearing stand that has called her in, or null; while set, her shed leaves her be. Not saved: the stand calls again. */
	private @Nullable BlockPos reservedBy;
	/** Game tests: the next lambing gives this many lambs (0 = roll). */
	private int forcedLambs;
	private final List<String> lastLambing = new ArrayList<>();

	public SheepEntity(EntityType<? extends SheepEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		// The vanilla sheep's values (8 health, 0.23 speed); no source for either.
		// Follow range 64 so a sheep can path across a pasture to the spot its shed picks (proposed; vanilla animals use 16).
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.23F).add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BREED, SheepBreed.MERINO.ordinal());
		builder.define(DATA_STAGE, SheepStage.EWE_LAMB.ordinal());
		builder.define(DATA_LOOK, -1L);
		builder.define(DATA_FLEECE, 1.0F);
		builder.define(DATA_BODY, 0);
		builder.define(DATA_UDDER, false);
	}

	// ---- what the client knows ----

	public @Nullable SheepLook syncedLook() {
		long v = entityData.get(DATA_LOOK);
		return v < 0 ? null : SheepLook.unpack(v);
	}

	public SheepBreed syncedBreed() {
		SheepBreed[] all = SheepBreed.values();
		return all[Math.max(0, Math.min(all.length - 1, entityData.get(DATA_BREED)))];
	}

	public SheepStage syncedStage() {
		SheepStage[] all = SheepStage.values();
		return all[Math.max(0, Math.min(all.length - 1, entityData.get(DATA_STAGE)))];
	}

	public float syncedFleece() {
		return entityData.get(DATA_FLEECE);
	}

	/** Dairy share 0-1. */
	public float syncedDairy() {
		return (entityData.get(DATA_BODY) & 0xFF) / 100.0F;
	}

	/** Meat share 0-1. */
	public float syncedMeat() {
		return (entityData.get(DATA_BODY) >> 8 & 0xFF) / 100.0F;
	}

	public boolean syncedUdder() {
		return entityData.get(DATA_UDDER);
	}

	private void sync(long now) {
		entityData.set(DATA_BREED, breed.ordinal());
		SheepStage stage = stageAt(now);
		entityData.set(DATA_STAGE, stage.ordinal());
		if (genome != null) {
			entityData.set(DATA_LOOK, look().pack());
			Map<String, Integer> c = genome.breedCompositionBp();
			int dairy = c.getOrDefault("EFR", 0) / 100;
			int meat = (c.getOrDefault("SUF", 0) + c.getOrDefault("DOR", 0)) / 100;
			entityData.set(DATA_BODY, dairy | meat << 8);
		}
		entityData.set(DATA_FLEECE, (float) Math.min(2.0, fleeceDays(now) / 365.0));
		entityData.set(DATA_UDDER, sex == Sex.FEMALE && (hasLambed || bredTick >= 0) && stage != SheepStage.LAMB);
		boolean lamb = stage.lamb();
		if (lamb != isBaby()) {
			setAge(lamb ? -1 : 0);
		}
	}

	// ---- the animal's facts ----

	/** A founder of one breed: its genes, coat genes and a starting age and weight. */
	public void initFounder(SheepBreed breed, Sex sex, long seed, double ageRealDays, double weightKg, long now) {
		Founders.Result f = Founders.create(breed.code, sex, seed, Ruleset.defaults());
		this.breed = breed;
		this.sex = sex;
		this.genome = f.genome();
		this.genes = SheepGenes.founder(breed.code, seed);
		for (Trait t : Species.SHEEP.traits()) {
			scores[t.ordinal()] = f.phenotype().score(t);
		}
		this.birthTick = now - TimeScale.realDaysToTicks(ageRealDays);
		this.weightKg = weightKg;
		this.fleeceTick = now;
		this.shedTick = now;
		this.castrated = false;
		this.bredTick = -1;
		this.hasLambed = false;
		this.lambedTick = -1;
		this.retired = false;
		sync(now);
	}

	/** A newborn lamb from {@link Offspring}. */
	public void initLamb(Offspring.Result r, SheepGenes coat, long now) {
		this.breed = topBreed(r.genome().breedCompositionBp());
		this.sex = r.sex();
		this.genome = r.genome();
		this.genes = coat;
		for (Trait t : Species.SHEEP.traits()) {
			scores[t.ordinal()] = r.phenotype().score(t);
		}
		this.birthTick = now;
		this.weightKg = Flock.targetKg(r.genome().breedCompositionBp(), 0);
		this.fleeceTick = now;
		this.shedTick = now;
		this.bredTick = -1;
		this.hasLambed = false;
		sync(now);
	}

	static SheepBreed topBreed(Map<String, Integer> composition) {
		String top = null;
		int best = -1;
		for (Map.Entry<String, Integer> e : composition.entrySet()) {
			if (e.getValue() > best && SheepBreed.isSheep(e.getKey())) {
				best = e.getValue();
				top = e.getKey();
			}
		}
		return top == null ? SheepBreed.MERINO : SheepBreed.byCode(top);
	}

	public SheepBreed breed() {
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

	/** Coat genes; a sheep with none recorded gets founder genes of its breed from its genome hash, once. */
	public SheepGenes genes() {
		if (genes == null) {
			long seed = genome == null ? getUUID().getLeastSignificantBits() : Long.parseUnsignedLong(genome.hash().substring(0, 15), 16);
			genes = SheepGenes.founder(breed.code, seed);
		}
		return genes;
	}

	/** For game tests and art: other coat genes. */
	public void setGenesForTest(SheepGenes g) {
		this.genes = g;
		sync(level().getGameTime());
	}

	public SheepLook look() {
		return SheepLook.of(genes(), sex);
	}

	public double weightKg() {
		return weightKg;
	}

	public void setWeightKg(double kg) {
		this.weightKg = kg;
		sync(level().getGameTime());
	}

	public double ageRealDays() {
		return TimeScale.ticksToRealDays(level().getGameTime() - birthTick);
	}

	public long birthTick() {
		return birthTick;
	}

	public SheepStage stage() {
		return stageAt(level().getGameTime());
	}

	private SheepStage stageAt(long now) {
		double age = TimeScale.ticksToRealDays(now - birthTick);
		return sex == Sex.MALE ? SheepStage.male(composition(), age, weightKg, castrated) : SheepStage.female(age, hasLambed, retired);
	}

	public boolean castrated() {
		return castrated;
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

	public int lambings() {
		return lambings;
	}

	public @Nullable UUID owner() {
		return ownerId;
	}

	public void setOwner(@Nullable UUID owner) {
		this.ownerId = owner;
	}

	// ---- fleece ----

	/** Real days of fleece growth so far, capped at Flock.FLEECE_MAX_DAYS. */
	public double fleeceDays() {
		return fleeceDays(level().getGameTime());
	}

	private double fleeceDays(long now) {
		if (look().sheds()) {
			return 0;
		}
		return Math.min(Flock.FLEECE_MAX_DAYS, TimeScale.ticksToRealDays(now - fleeceTick));
	}

	/** Greasy fleece on her now, kg. Hair sheep carry none; a part-hair cross half (SheepLook.fleeceShare). */
	public double fleeceKg() {
		return Flock.fleeceKg(composition(), fleeceDays(), score(Trait.WOOL_YIELD), look().fleeceShare());
	}

	public double micron() {
		return Flock.micron(composition(), score(Trait.FLEECE_QUALITY));
	}

	public double stapleCm() {
		return Flock.stapleCm(composition(), fleeceDays());
	}

	/** A fleece is ready to shear at 300 days of growth (Flock.FLEECE_READY_DAYS). */
	public boolean fleeceReady() {
		return !stage().lamb() && fleeceKg() > 0 && fleeceDays() >= Flock.FLEECE_READY_DAYS;
	}

	/** What a fleece taken now holds; the shearing shed and hand shears turn it into an item. */
	public record Fleece(double greasyKg, double micron, double stapleCm, double cleanYield, Flock.Grade grade, SheepLook.Wool colour,
			Map<String, Integer> composition) {
	}

	/** Shears her: returns the fleece and starts a new one. Null when she has no wool to take. */
	public @Nullable Fleece shear() {
		double kg = fleeceKg();
		if (kg <= 0.01 || stage().lamb()) {
			return null;
		}
		double m = micron();
		Fleece f = new Fleece(kg, m, stapleCm(), Flock.cleanYield(composition()), Flock.grade(m), look().wool(ageRealDays()), composition());
		fleeceTick = level().getGameTime();
		sync(fleeceTick);
		return f;
	}

	/** For game tests and art: as if the fleece had grown this many real days. */
	public void setFleeceDaysForTest(double days) {
		this.fleeceTick = level().getGameTime() - TimeScale.realDaysToTicks(days);
		sync(level().getGameTime());
	}

	/** Hair sheep shed once a year (sheep.md); returns true on the tick the coat drops. */
	private boolean updateShedding(long now) {
		if (!look().sheds() || stage().lamb()) {
			return false;
		}
		if (TimeScale.ticksToRealDays(now - shedTick) >= 365) {
			shedTick = now;
			return true;
		}
		return false;
	}

	// ---- breeding and lambing ----

	public boolean inHeat() {
		return sex == Sex.FEMALE && nextHeatTick >= 0 && level().getGameTime() >= nextHeatTick
				&& level().getGameTime() < nextHeatTick + TimeScale.realDaysToTicks(Flock.HEAT_DAYS);
	}

	public void startHeatForDisplay() {
		this.nextHeatTick = level().getGameTime();
	}

	public boolean pregnant() {
		return bredTick >= 0;
	}

	private @Nullable BlockPos lure;

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
		return Flock.intakeKgDm(weightKg, lambsNursing());
	}

	@Override
	public double dailyWaterL() {
		return dailyDryMatterKg() * Flock.WATER_L_PER_KG_DM;
	}

	public boolean canConceive() {
		return genome != null && !wild && inHeat() && bredTick < 0 && !retired;
	}

	public boolean conceive(Genome sire, SheepGenes sireCoat, long now) {
		if (!canConceive() || sire.species() != Species.SHEEP) {
			return false;
		}
		this.bredTick = now;
		this.sireGenome = sire.encode();
		this.sireGenes = sireCoat.save();
		this.dueTick = now + TimeScale.realDaysToTicks(Flock.GESTATION_DAYS);
		this.nextHeatTick = -1;
		sync(now);
		return true;
	}

	/** For game tests: she is due in this many real days. */
	public void setDueInForTest(double realDays) {
		this.dueTick = level().getGameTime() + TimeScale.realDaysToTicks(realDays);
	}

	public void makeDueNow() {
		this.dueTick = level().getGameTime();
	}

	public long dueTick() {
		return dueTick;
	}

	/** Real days to lambing, or -1 if she is not in lamb. */
	public double daysToLambing() {
		return bredTick < 0 || dueTick < 0 ? -1 : TimeScale.ticksToRealDays(dueTick - level().getGameTime());
	}

	/** Real days since she last lambed, or a large number if she never has. */
	public double daysSinceLambing() {
		return hasLambed ? TimeScale.ticksToRealDays(level().getGameTime() - lambedTick) : Double.MAX_VALUE;
	}

	public void forceLambsForTest(int n) {
		this.forcedLambs = n;
	}

	public List<String> lastLambing() {
		return lastLambing;
	}

	private boolean cycles(long now) {
		if (sex != Sex.FEMALE || genome == null || retired || bredTick >= 0) {
			return false;
		}
		double age = TimeScale.ticksToRealDays(now - birthTick);
		if (!hasLambed) {
			return Flock.canFirstBreed(composition(), age, weightKg);
		}
		return TimeScale.ticksToRealDays(now - lambedTick) >= Flock.WAIT_AFTER_LAMBING_DAYS;
	}

	private void updateBreeding(ServerLevel level, long now) {
		if (sex != Sex.FEMALE || genome == null) {
			return;
		}
		if (bredTick >= 0) {
			if (!sireGenome.isEmpty() && dueTick >= 0 && now >= dueTick) {
				lamb(level, now);
			}
			return;
		}
		if (!cycles(now)) {
			nextHeatTick = -1;
			return;
		}
		long cycle = TimeScale.realDaysToTicks(Flock.CYCLE_DAYS);
		if (nextHeatTick < 0) {
			nextHeatTick = now + Math.floorMod(genome.hash().hashCode(), cycle);
		} else if (now >= nextHeatTick + TimeScale.realDaysToTicks(Flock.HEAT_DAYS)) {
			nextHeatTick += cycle * ((now - nextHeatTick) / cycle + 1);
		}
		if (inHeat()) {
			serveFromNearbyRam(level, now);
		}
	}

	/** Natural service: the nearest intact ram within reach serves a ewe in heat. */
	private void serveFromNearbyRam(ServerLevel level, long now) {
		SheepEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (SheepEntity s : level.getEntitiesOfClass(SheepEntity.class, getBoundingBox().inflate(SERVICE_RANGE))) {
			if (s != this && s.sex == Sex.MALE && s.stage() == SheepStage.RAM && s.genome != null) {
				double d = s.distanceToSqr(this);
				if (d < bestD) {
					bestD = d;
					best = s;
				}
			}
		}
		if (best != null) {
			conceive(best.genome, best.genes(), now);
		}
	}

	/**
	 * She lambs: one to three lambs by her litter odds (Flock.litter, from her breed mix and fertility), each its own Offspring with its
	 * own event id, coat genes inherited from both parents. Every roll comes from a generator seeded by the lambing.
	 */
	private void lamb(ServerLevel level, long now) {
		Genome sire = Genome.decode(sireGenome);
		String world = Long.toString(level.getSeed());
		String eventId = "lambing:" + getUUID() + ":" + now;
		Rng rng = new Rng(BreedingSeed.seed(world, eventId + ":litter", genome.hash(), sire.hash(), Ruleset.defaults().version()));
		int n = forcedLambs > 0 ? forcedLambs : Flock.litter(composition(), score(Trait.FERTILITY)).roll(rng.next());
		forcedLambs = 0;
		lastLambing.clear();
		SheepGenes sireCoat = SheepGenes.load(sireGenes);
		for (int i = 0; i < n; i++) {
			String id = eventId + (i == 0 ? "" : ":lamb" + i);
			Offspring.Result r = Offspring.create(genome, sire, id, world, Ruleset.defaults(), null);
			SheepEntity lamb = dev.farmings.FarmingsEntities.SHEEP.create(level, EntitySpawnReason.BREEDING);
			if (lamb == null) {
				continue;
			}
			SheepGenes coat = SheepGenes.inherit(genes(), sireCoat != null ? sireCoat : SheepGenes.founder(topBreed(sire.breedCompositionBp()).code, 0),
					BreedingSeed.seed(world, id, genome.hash(), sire.hash(), Ruleset.defaults().version()));
			lamb.initLamb(r, coat, now);
			lamb.ownerId = ownerId;
			lamb.damId = herdId();
			lamb.sireId = CattleEntity.herdId(sire);
			lamb.home = home;
			lamb.shedController = shedController;
			lamb.setPos(getX() + 0.6 * (i - (n - 1) / 2.0), getY(), getZ() + 0.6);
			level.addFreshEntity(lamb);
			lastLambing.add(r.sex().name().toLowerCase(Locale.ROOT));
		}
		hasLambed = true;
		lambings++;
		lastLitter = n;
		lambedTick = now;
		bredTick = -1;
		dueTick = -1;
		sireGenome = "";
		sireGenes = new int[0];
		nextHeatTick = -1;
		lastMilkDay = -1;
		sync(now);
		updateMilk(now);
	}

	// ---- milk (East Friesian blood) ----

	/** Days since lambing while she is in milk (Flock.LACTATION_DAYS), else 0. */
	public int daysInMilk() {
		if (sex != Sex.FEMALE || !hasLambed) {
			return 0;
		}
		int d = (int) Math.floor(TimeScale.ticksToRealDays(level().getGameTime() - lambedTick)) + 1;
		return d <= Flock.LACTATION_DAYS ? d : 0;
	}

	public double milkKg() {
		return milkKg;
	}

	public double takeMilk() {
		double m = milkKg;
		milkKg = 0;
		return m;
	}

	/** True for a ewe that gives parlor milk now (East Friesian blood, in milk). */
	public boolean dairyEwe() {
		return Flock.dairyShare(composition()) > 0 && daysInMilk() > 0;
	}

	private void updateMilk(long now) {
		if (sex != Sex.FEMALE || !hasLambed) {
			milkKg = 0;
			return;
		}
		long day = (now - lambedTick) / TimeScale.TICKS_PER_GAME_DAY;
		if (day != lastMilkDay) {
			lastMilkDay = day;
			int dim = (int) Math.floor(TimeScale.ticksToRealDays(now - lambedTick)) + 1;
			milkKg = Flock.milkKg(composition(), dim, score(Trait.MILK_YIELD)) * Math.min(lastFed, lastWatered);
		}
	}

	/** For game tests and art: she lambed this many real days ago. */
	public void setLambedForTest(double realDaysAgo, int litter) {
		long now = level().getGameTime();
		hasLambed = true;
		lambings = Math.max(1, lambings);
		lastLitter = litter;
		lambedTick = now - TimeScale.realDaysToTicks(realDaysAgo);
		lastMilkDay = -1;
		sync(now);
		updateMilk(now);
	}

	public double milkFatPct() {
		return Flock.FAT_PCT;
	}

	public double milkProteinPct() {
		return Flock.PROTEIN_PCT;
	}

	// ---- feed, water and housing ----

	/** Lambs she is nursing: her last litter, until they are weaned. */
	public int lambsNursing() {
		if (!hasLambed || TimeScale.ticksToRealDays(level().getGameTime() - lambedTick) >= SheepStage.WEANING_DAYS) {
			return 0;
		}
		return lastLitter;
	}

	/** Dry matter she needs in a day (0 for a lamb on its dam). */
	public double dryMatterIntakeKgPerDay() {
		return stage().lamb() ? 0 : Flock.intakeKgDm(weightKg, lambsNursing());
	}

	/** What the sheep has been eating, as a running average (feed-mill.md); balanced until a shed feeds it. */
	private dev.farmings.feed.Ration.Analysis ration = dev.farmings.feed.Ration.Analysis.BALANCED;

	public dev.farmings.feed.Ration.Analysis ration() {
		return ration;
	}

	public void recordRation(dev.farmings.feed.Ration.Analysis a) {
		ration = ration.toward(a, dev.farmings.feed.Ration.SMOOTHING);
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

	public double dirty() {
		return dirty;
	}

	public void applyDirt(double dirty, long ticks) {
		this.dirty = dirty;
		float loss = (float) dev.farmings.husbandry.Hygiene.healthLoss(dirty, getHealth(), ticks);
		if (loss > 0) {
			setHealth(getHealth() - loss);
		}
	}

	public @Nullable BlockPos shedController() {
		return shedController;
	}

	public @Nullable BlockPos home() {
		return home;
	}

	/** Her shed and her place in it (a pen cell, or a lambing pen). */
	public void assignShed(@Nullable BlockPos controller, @Nullable BlockPos home) {
		this.shedController = controller;
		this.home = home;
	}

	/** Somewhere she is sent for a while (pasture spot, shearing stand, parlor stall); null to let her go home. */
	public void sendTo(@Nullable BlockPos target) {
		this.target = target;
		if (target != null) {
			this.route = null;
		}
	}

	public @Nullable BlockPos reservedBy() {
		return reservedBy;
	}

	public void reserve(@Nullable BlockPos stand) {
		this.reservedBy = stand;
	}

	public @Nullable BlockPos sentTo() {
		return target;
	}

	/** Sends her along waypoints (a lane, the pasture gate); she starts from the waypoint nearest her. */
	public void walkRoute(List<BlockPos> waypoints, boolean home) {
		List<dev.farmings.husbandry.lane.LanePath.Pos> ps = new ArrayList<>();
		for (BlockPos b : waypoints) {
			ps.add(new dev.farmings.husbandry.lane.LanePath.Pos(b.getX(), b.getY(), b.getZ()));
		}
		this.route = waypoints.isEmpty() ? null : List.copyOf(waypoints);
		this.routeIndex = waypoints.isEmpty() ? 0 : dev.farmings.husbandry.lane.LanePath.startIndex(ps, getX(), getZ());
		this.routeHome = home;
	}

	public boolean onRoute() {
		return route != null;
	}

	public boolean walkingHome() {
		return route != null && routeHome;
	}

	public void clearRoute() {
		this.route = null;
		this.routeHome = false;
	}

	/** Follows a route, then goes to her target, else to her home cell. Outranks strolling. */
	private static final class PlaceGoal extends net.minecraft.world.entity.ai.goal.Goal {
		private final SheepEntity sheep;
		/** Where she was when last checked, and how long she has made no headway: snagged on a gate post, she gets a nudge. */
		private double lastX = Double.NaN;
		private double lastZ = Double.NaN;
		private int still;

		PlaceGoal(SheepEntity sheep) {
			this.sheep = sheep;
			setFlags(java.util.EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return sheep.route != null || sheep.target != null || (sheep.home != null && !sheep.isBaby());
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		/** Two seconds with no headway toward (x, z): a small push that way, as a snagged animal slides off a post corner, and a fresh path. */
		private void unstick(double x, double z) {
			double moved = Double.isNaN(lastX) ? 1 : Math.abs(sheep.getX() - lastX) + Math.abs(sheep.getZ() - lastZ);
			if (sheep.tickCount % 20 != 0) {
				return;
			}
			lastX = sheep.getX();
			lastZ = sheep.getZ();
			still = moved < 0.15 ? still + 1 : 0;
			if (still >= 2) {
				double dx = x - sheep.getX();
				double dz = z - sheep.getZ();
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > 0.01) {
					sheep.push(dx / d * 0.25, 0.0, dz / d * 0.25);
				}
				sheep.getNavigation().stop();
				still = 0;
			}
		}

		@Override
		public void tick() {
			if (sheep.route != null) {
				if (sheep.routeIndex >= sheep.route.size()) {
					sheep.route = null;
					sheep.routeHome = false;
					return;
				}
				BlockPos w = sheep.route.get(sheep.routeIndex);
				double dx = sheep.getX() - (w.getX() + 0.5);
				double dz = sheep.getZ() - (w.getZ() + 0.5);
				double d2 = dx * dx + dz * dz;
				if (d2 < 1.0 || (d2 < 2.25 && sheep.getNavigation().isDone())) {
					sheep.routeIndex++;
				} else {
					sheep.getNavigation().moveTo(w.getX() + 0.5, w.getY() + 0.2, w.getZ() + 0.5, 1.0);
				}
				return;
			}
			BlockPos p = sheep.target != null ? sheep.target : sheep.home;
			if (p == null) {
				return;
			}
			double x = p.getX() + 0.5;
			double z = p.getZ() + 0.5;
			double dx = sheep.getX() - x;
			double dz = sheep.getZ() - z;
			// within a block of her pen cell she is home (two blocks let a sheep stop in the gate, a block outside the pen)
			double reach = sheep.target != null ? 0.36 : 1.0;
			if (dx * dx + dz * dz > reach) {
				unstick(x, z);
				sheep.getNavigation().moveTo(x, p.getY() + 0.1, z, 1.0);
			} else {
				still = 0;
				if (sheep.target != null) {
					sheep.getNavigation().stop();
				}
			}
		}
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
		this.goalSelector.addGoal(2, new PlaceGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		Wild.addGoals(this, this.goalSelector, this::isWild);
	}

	// ---- the once-a-second bookkeeping ----

	public void settle(ServerLevel level) {
		long now = level.getGameTime();
		grow(now);
		updateMilk(now);
		updateBreeding(level, now);
		if (updateShedding(now)) {
			// a hair coat drops as a little loose wool (sheep.md: 0.5-1 kg a year); the loose-wool item comes with the shearing shed
			dropShedWool(level);
		}
		sync(now);
	}

	/** Overridden point for the shed coat: the fleece items live in the shearing step. */
	protected void dropShedWool(ServerLevel level) {
		ItemStack wool = dev.farmings.FarmingsItems.shedWool();
		if (!wool.isEmpty()) {
			spawnAtLocation(level, wool);
		}
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
		double fed = shedController != null && !stageAt(now).lamb() ? lastFed : 1;
		double mature = Flock.matureWeightKg(composition());
		double gain = Flock.gainKg(composition(), age - span, span, fed) * (stageAt(now).lamb() ? dev.farmings.feed.Ration.lambFactor(ration) : 1);
		if (gain > 0 && weightKg < mature) {
			weightKg = Math.min(mature, weightKg + gain);
		}
	}

	/** For game tests: {@code realDays} older, growing and growing wool all that time. */
	public void advanceForTest(double realDays) {
		long ticks = TimeScale.realDaysToTicks(realDays);
		birthTick -= ticks;
		growthTick -= ticks;
		fleeceTick -= ticks;
		shedTick -= ticks;
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

	/**
	 * Empty hand reads the sheep. Shears on a ram lamb make him a wether (as cattle are castrated). Shears on a sheep with wool shear her
	 * by hand ({@link dev.farmings.FarmingsItems#handShorn}).
	 */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) {
			if (!level().isClientSide()) {
				player.sendSystemMessage(Component.literal(readout()));
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.SHEARS)) {
			if (sex == Sex.MALE && !castrated && stage() == SheepStage.LAMB) {
				if (!level().isClientSide()) {
					castrated = true;
					sync(level().getGameTime());
					stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
					player.sendOverlayMessage(Component.translatable("message.farmings.wether"));
				}
				return InteractionResult.SUCCESS;
			}
			if (!stage().lamb() && fleeceKg() > 0.01) {
				if (level() instanceof ServerLevel server) {
					Fleece f = shear();
					if (f != null) {
						spawnAtLocation(server, dev.farmings.FarmingsItems.handShorn(f, server.getGameTime()));
						playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
						stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
					}
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
			return SheepBreed.byCode(parts.get(0).getKey()).label();
		}
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, Integer> e : parts) {
			b.append(b.length() > 0 ? " " : "").append(SheepBreed.byCode(e.getKey()).label())
					.append(String.format(Locale.ROOT, " %.0f%%", e.getValue() / 100.0));
		}
		return b.toString();
	}

	public String readout() {
		StringBuilder b = new StringBuilder();
		if (wild) {
			b.append(String.format(Locale.ROOT, "WILD (settled %.1f of %.0f days), ", settledDays, dev.farmings.husbandry.WildBreeds.SETTLE_DAYS));
		}
		b.append(breedLabel()).append(' ').append(sex == Sex.FEMALE ? "ewe" : castrated ? "wether" : "ram");
		b.append(String.format(Locale.ROOT, ", %.0f months, %.0f kg, %s", ageRealDays() / 30.4, weightKg, stage().label()));
		if (genome != null) {
			b.append(", ").append(look().describe());
		}
		if (look().sheds()) {
			b.append(", sheds its coat");
		} else if (!stage().lamb()) {
			b.append(String.format(Locale.ROOT, ", fleece %.1f kg greasy, %.1f micron (%s), %.0f days%s", fleeceKg(), micron(), Flock.grade(micron()).label,
					fleeceDays(), fleeceReady() ? ", READY" : ""));
		}
		if (inHeat()) {
			b.append(", IN HEAT");
		}
		if (pregnant()) {
			b.append(String.format(Locale.ROOT, ", in lamb, due in %.1f game days", daysToLambing() / TimeScale.REAL_DAYS_PER_GAME_DAY));
		}
		if (lambsNursing() > 0) {
			b.append(", nursing ").append(lambsNursing());
		}
		if (dairyEwe()) {
			b.append(String.format(Locale.ROOT, ", day %d in milk, %.2f kg in udder", daysInMilk(), milkKg));
		}
		if (shedController != null) {
			b.append(String.format(Locale.ROOT, ", fed %.0f%%, watered %.0f%%", lastFed * 100, lastWatered * 100));
		}
		b.append(String.format(Locale.ROOT, ", health %.0f/%.0f", getHealth(), getMaxHealth()));
		return b.toString();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		int items = Flock.meatItems(weightKg);
		boolean young = ageRealDays() < 365;
		boolean fire = isOnFire();
		net.minecraft.world.level.ItemLike meat = dev.farmings.FarmingsItems.sheepMeat(young, fire);
		int max = new ItemStack(meat).getMaxStackSize();
		while (items > 0) {
			int n = Math.min(max, items);
			spawnAtLocation(level, dev.farmings.item.Perishables.stamp(new ItemStack(meat, n), level.getGameTime()));
			items -= n;
		}
		Fleece f = shear();
		if (f != null) {
			spawnAtLocation(level, dev.farmings.FarmingsItems.handShorn(f, level.getGameTime()));
		}
	}

	/** Natural spawns follow the vanilla animal rule: on grass, in light 9 or more (docs/taming.md). */
	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
		if (Wild.wildReason(reason)) {
			return net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(dev.farmings.FarmingsEntities.SHEEP, level, reason, blockPosition(), getRandom());
		}
		return true;     // a summon, spawn egg or birth goes where it is put (docs/taming.md); vanilla would test the light here
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (genome == null) {
			SheepBreed b = SheepBreed.values()[level.getRandom().nextInt(SheepBreed.values().length)];
			Sex s = level.getRandom().nextBoolean() ? Sex.FEMALE : Sex.MALE;
			if (Wild.wildReason(reason)) {
				// A wild founder: the breed comes from the biome, one ram per spawn group (docs/taming.md).
				Wild.Group g = Wild.group(groupData, level);
				data = g;
				b = dev.farmings.husbandry.WildBreeds.sheep(Wild.biome(level, blockPosition()), g.roll);
				s = g.maleGiven ? Sex.FEMALE : Sex.MALE;
				g.maleGiven = true;
				wild = true;
			}
			Map<String, Integer> c = Map.of(b.code, 10_000);
			initFounder(b, s, level.getRandom().nextLong(), Flock.FIRST_BREEDING_AGE_DAYS + 30, Flock.targetKg(c, Flock.FIRST_BREEDING_AGE_DAYS + 30),
					level.getLevel().getGameTime());
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
		out.putBoolean("Retired", retired);
		out.putLong("FleeceTick", fleeceTick);
		out.putLong("ShedTick", shedTick);
		out.putLong("BredTick", bredTick);
		out.putLong("Due", dueTick);
		out.putString("Sire", sireGenome);
		out.putIntArray("SireCoat", sireGenes);
		out.putBoolean("HasLambed", hasLambed);
		out.putLong("LambedTick", lambedTick);
		out.putInt("Lambings", lambings);
		out.putInt("LastLitter", lastLitter);
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
		out.putDouble("Dirty", dirty);
		out.putLong("Home", home == null ? Long.MIN_VALUE : home.asLong());
		out.putLong("Shed", shedController == null ? Long.MIN_VALUE : shedController.asLong());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		try {
			breed = SheepBreed.byCode(in.getStringOr("Breed", SheepBreed.MERINO.code));
			sex = Sex.valueOf(in.getStringOr("Sex", Sex.FEMALE.name()));
			genome = in.getString("Genome").map(Genome::decode).orElse(null);
		} catch (IllegalArgumentException e) {
			Farmings.LOGGER.warn("A sheep record could not be read; keeping defaults: {}", e.getMessage());
		}
		in.getIntArray("Scores").ifPresent(saved -> {
			if (saved.length <= scores.length) {
				System.arraycopy(saved, 0, scores, 0, saved.length);
			}
		});
		genes = in.getIntArray("Coat").map(SheepGenes::load).orElse(null);
		birthTick = in.getLongOr("BirthTick", 0L);
		weightKg = in.getDoubleOr("WeightKg", 0.0);
		growthMarked = in.getBooleanOr("GrowthMarked", false);
		growthTick = in.getLongOr("GrowthTick", 0L);
		castrated = in.getBooleanOr("Castrated", false);
		retired = in.getBooleanOr("Retired", false);
		fleeceTick = in.getLongOr("FleeceTick", 0L);
		shedTick = in.getLongOr("ShedTick", 0L);
		bredTick = in.getLongOr("BredTick", -1L);
		dueTick = in.getLongOr("Due", -1L);
		sireGenome = in.getStringOr("Sire", "");
		sireGenes = in.getIntArray("SireCoat").orElse(new int[0]);
		hasLambed = in.getBooleanOr("HasLambed", false);
		lambedTick = in.getLongOr("LambedTick", -1L);
		lambings = in.getIntOr("Lambings", 0);
		lastLitter = in.getIntOr("LastLitter", 0);
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
		dirty = in.getDoubleOr("Dirty", 0);
		long h = in.getLongOr("Home", Long.MIN_VALUE);
		home = h == Long.MIN_VALUE ? null : BlockPos.of(h);
		long s = in.getLongOr("Shed", Long.MIN_VALUE);
		shedController = s == Long.MIN_VALUE ? null : BlockPos.of(s);
		sync(level().getGameTime());
	}

	// ---- sounds: the vanilla sheep's ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SHEEP_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SHEEP_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SHEEP_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SHEEP_STEP, 0.15F, 1.0F);
	}
}

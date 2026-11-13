package dev.farmings.entity;

import dev.farmings.Farmings;
import dev.farmings.genetics.BreedingSeed;
import dev.farmings.genetics.Founders;
import dev.farmings.genetics.Genome;
import dev.farmings.genetics.Offspring;
import dev.farmings.genetics.PigGenes;
import dev.farmings.genetics.PigLook;
import dev.farmings.genetics.Rng;
import dev.farmings.genetics.Ruleset;
import dev.farmings.genetics.Sex;
import dev.farmings.genetics.Species;
import dev.farmings.genetics.Trait;
import dev.farmings.husbandry.PigBreed;
import dev.farmings.husbandry.PigStage;
import dev.farmings.husbandry.Swine;
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
 * A Farmings pig (docs/animals/pigs.md): Large White, Duroc or Berkshire, with its genome, coat genes, farrowing big litters, piglets that
 * nurse until weaning, and cuts for the curing room. Rules live in {@link Swine}; this class keeps the state, as {@link GoatEntity} does
 * for goats. The pig barn houses it through its {@link Placement}.
 */
public class PigEntity extends Animal implements Wild.Animal {
	private static final EntityDataAccessor<Integer> DATA_BREED = SynchedEntityData.defineId(PigEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(PigEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> DATA_LOOK = SynchedEntityData.defineId(PigEntity.class, EntityDataSerializers.LONG);
	/** Duroc share 0-100 (drooping ears) and the weight in kg, for the model. */
	private static final EntityDataAccessor<Integer> DATA_BODY = SynchedEntityData.defineId(PigEntity.class, EntityDataSerializers.INT);

	private PigBreed breed = PigBreed.LARGE_WHITE;
	private Sex sex = Sex.FEMALE;
	private @Nullable Genome genome;
	private final int[] scores = new int[Trait.values().length];
	private @Nullable PigGenes genes;
	private long birthTick;
	private double weightKg;
	private long growthTick;
	private boolean growthMarked;
	private boolean castrated;
	private long bredTick = -1;
	private long dueTick = -1;
	private String sireGenome = "";
	private int[] sireGenes = new int[0];
	private boolean hasFarrowed;
	private long farrowedTick = -1;
	private int farrowings;
	private int lastLitter;
	private long nextHeatTick = -1;
	private @Nullable UUID ownerId;
	private boolean wild;
	private double settledDays;
	private String damId = "";
	private String sireId = "";
	private double lastFed = 1;
	private double lastWatered = 1;
	private @Nullable Placement place;
	private int forcedPiglets;
	private boolean forcedStillborn;
	private final List<String> lastFarrowing = new ArrayList<>();
	private dev.farmings.feed.Ration.Analysis ration = dev.farmings.feed.Ration.Analysis.BALANCED;
	private @Nullable BlockPos lure;
	/** Game tests: a service always takes (the 84% roll aside). Not saved. */
	private boolean sureConceptionForTest;

	public PigEntity(EntityType<? extends PigEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		// The vanilla pig's 10 health and 0.25 speed; follow range 64 as the other herd animals.
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.25F).add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BREED, 0);
		builder.define(DATA_STAGE, PigStage.GILT.ordinal());
		builder.define(DATA_LOOK, -1L);
		builder.define(DATA_BODY, 0);
	}

	// ---- what the client knows ----

	public @Nullable PigLook syncedLook() {
		long v = entityData.get(DATA_LOOK);
		return v < 0 ? null : PigLook.unpack(v);
	}

	public PigStage syncedStage() {
		PigStage[] all = PigStage.values();
		return all[Math.max(0, Math.min(all.length - 1, entityData.get(DATA_STAGE)))];
	}

	/** Duroc share 0-1: drooping ears. */
	public float syncedDroop() {
		return (entityData.get(DATA_BODY) & 0xFF) / 100.0F;
	}

	/** Body weight, kg, for the model's size. */
	public float syncedKg() {
		return (entityData.get(DATA_BODY) >> 8) / 1.0F;
	}

	private void sync(long now) {
		entityData.set(DATA_BREED, breed.ordinal());
		PigStage stage = stageAt(now);
		entityData.set(DATA_STAGE, stage.ordinal());
		if (genome != null) {
			entityData.set(DATA_LOOK, look().pack());
		}
		int droop = composition().getOrDefault("DUR", 0) / 100;
		entityData.set(DATA_BODY, droop | (int) Math.min(1000, Math.round(weightKg)) << 8);
		boolean young = stage.piglet();
		if (young != isBaby()) {
			setAge(young ? -1 : 0);
		}
	}

	// ---- the animal's facts ----

	public void initFounder(PigBreed breed, Sex sex, long seed, double ageRealDays, double weightKg, long now) {
		Founders.Result f = Founders.create(breed.code, sex, seed, Ruleset.defaults());
		this.breed = breed;
		this.sex = sex;
		this.genome = f.genome();
		this.genes = PigGenes.founder(breed.code, seed);
		for (Trait t : Species.PIG.traits()) {
			scores[t.ordinal()] = f.phenotype().score(t);
		}
		this.birthTick = now - TimeScale.realDaysToTicks(ageRealDays);
		this.weightKg = weightKg;
		sync(now);
	}

	public void initPiglet(Offspring.Result r, PigGenes coat, long now) {
		this.breed = Swine.topBreed(r.genome().breedCompositionBp());
		this.sex = r.sex();
		this.genome = r.genome();
		this.genes = coat;
		for (Trait t : Species.PIG.traits()) {
			scores[t.ordinal()] = r.phenotype().score(t);
		}
		this.birthTick = now;
		this.weightKg = Swine.targetKg(r.genome().breedCompositionBp(), r.sex() == Sex.MALE, 0);
		sync(now);
	}

	public PigBreed breed() {
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

	public PigGenes genes() {
		if (genes == null) {
			long seed = genome == null ? getUUID().getLeastSignificantBits() : Long.parseUnsignedLong(genome.hash().substring(0, 15), 16);
			genes = PigGenes.founder(breed.code, seed);
		}
		return genes;
	}

	public void setGenesForTest(PigGenes g) {
		this.genes = g;
		sync(level().getGameTime());
	}

	public PigLook look() {
		return PigLook.of(genes());
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

	public PigStage stage() {
		return stageAt(level().getGameTime());
	}

	private PigStage stageAt(long now) {
		double age = TimeScale.ticksToRealDays(now - birthTick);
		return sex == Sex.MALE ? PigStage.male(composition(), age, weightKg, castrated) : PigStage.female(age, hasFarrowed);
	}

	/** A sow or a boar: the breeder's feed and pen space. */
	public boolean adultBreeder() {
		PigStage s = stage();
		return s == PigStage.SOW || s == PigStage.BOAR;
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

	public int farrowings() {
		return farrowings;
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

	// ---- breeding and farrowing ----

	public boolean inHeat() {
		return sex == Sex.FEMALE && nextHeatTick >= 0 && level().getGameTime() >= nextHeatTick
				&& level().getGameTime() < nextHeatTick + TimeScale.realDaysToTicks(Swine.HEAT_DAYS);
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
	public boolean eatsGrain() {
		return true;
	}

	@Override
	public boolean isFeed(ItemStack stack) {
		return Wild.grain(stack);
	}

	@Override
	public double dailyDryMatterKg() {
		return feedKgPerDay() * 0.88;     // feed as fed is about 88% dry matter (proposed, grain)
	}

	@Override
	public double dailyWaterL() {
		return Swine.waterLPerDay(weightKg, adultBreeder(), pigletsNursing() > 0);
	}

	public double feedKgPerDay() {
		return stage().piglet() ? 0 : Swine.feedKgPerDay(weightKg, adultBreeder(), pigletsNursing() > 0);
	}

	public double manureKgPerDay() {
		PigStage s = stage();
		return Swine.manureKgPerDay(weightKg, s == PigStage.SOW || s == PigStage.GILT && pregnant(), s == PigStage.BOAR, pigletsNursing() > 0);
	}

	public boolean canConceive() {
		return genome != null && !wild && inHeat() && bredTick < 0;
	}

	/** A service: conceives 84% of the time (the farrowing rate), by a roll from the service. */
	public boolean conceive(Genome sire, PigGenes sireCoat, long now) {
		if (!canConceive() || sire.species() != Species.PIG) {
			return false;
		}
		Rng rng = new Rng(BreedingSeed.seed(Long.toString(now), "service:" + getUUID(), genome.hash(), sire.hash(), Ruleset.defaults().version()));
		if (!sureConceptionForTest && rng.next() >= Swine.CONCEPTION) {
			nextHeatTick = -1;     // missed: she cycles again
			return false;
		}
		this.bredTick = now;
		this.sireGenome = sire.encode();
		this.sireGenes = sireCoat.save();
		this.dueTick = now + TimeScale.realDaysToTicks(Swine.GESTATION_DAYS);
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

	public double daysToFarrowing() {
		return bredTick < 0 || dueTick < 0 ? -1 : TimeScale.ticksToRealDays(dueTick - level().getGameTime());
	}

	public double daysSinceFarrowing() {
		return hasFarrowed ? TimeScale.ticksToRealDays(level().getGameTime() - farrowedTick) : Double.MAX_VALUE;
	}

	public void forcePigletsForTest(int n) {
		this.forcedPiglets = n;
	}

	public void forceStillbornForTest() {
		this.forcedStillborn = true;
	}

	public List<String> lastFarrowing() {
		return lastFarrowing;
	}

	private boolean cycles(long now) {
		if (sex != Sex.FEMALE || genome == null || bredTick >= 0) {
			return false;
		}
		double age = TimeScale.ticksToRealDays(now - birthTick);
		if (!hasFarrowed) {
			return Swine.canFirstBreed(age, weightKg);
		}
		// back in heat 5 days after weaning (UW: 4-6)
		return TimeScale.ticksToRealDays(now - farrowedTick) >= Swine.WEANING_DAYS + Swine.WEAN_TO_HEAT_DAYS;
	}

	private void updateBreeding(ServerLevel level, long now) {
		if (sex != Sex.FEMALE || genome == null) {
			return;
		}
		if (bredTick >= 0) {
			if (!sireGenome.isEmpty() && dueTick >= 0 && now >= dueTick) {
				farrow(level, now);
			}
			return;
		}
		if (!cycles(now)) {
			nextHeatTick = -1;
			return;
		}
		long cycle = TimeScale.realDaysToTicks(Swine.CYCLE_DAYS);
		if (nextHeatTick < 0) {
			nextHeatTick = now + Math.floorMod(genome.hash().hashCode(), cycle);
		} else if (now >= nextHeatTick + TimeScale.realDaysToTicks(Swine.HEAT_DAYS)) {
			nextHeatTick += cycle * ((now - nextHeatTick) / cycle + 1);
		}
		if (inHeat()) {
			serveFromNearbyBoar(level, now);
		}
	}

	private void serveFromNearbyBoar(ServerLevel level, long now) {
		PigEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (PigEntity p : level.getEntitiesOfClass(PigEntity.class, getBoundingBox().inflate(Swine.SERVICE_RANGE))) {
			if (p != this && p.sex == Sex.MALE && p.stage() == PigStage.BOAR && p.genome != null && !p.wild) {
				double d = p.distanceToSqr(this);
				if (d < bestD) {
					bestD = d;
					best = p;
				}
			}
		}
		if (best != null) {
			conceive(best.genome, best.genes(), now);
		}
	}

	/** She farrows: a litter by her breed and parity (pigs.md), each piglet its own Offspring; stillborn ones aren't spawned. */
	private void farrow(ServerLevel level, long now) {
		Genome sire = Genome.decode(sireGenome);
		String world = Long.toString(level.getSeed());
		String eventId = "farrowing:" + getUUID() + ":" + now;
		Rng rng = new Rng(BreedingSeed.seed(world, eventId + ":litter", genome.hash(), sire.hash(), Ruleset.defaults().version()));
		boolean forced = forcedPiglets > 0;
		double u1 = Math.max(1e-9, rng.next());
		double u2 = rng.next();
		double z = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
		int n = forced ? forcedPiglets : Swine.litter(composition(), !hasFarrowed, score(Trait.FERTILITY), z);
		forcedPiglets = 0;
		lastFarrowing.clear();
		PigGenes sireCoat = PigGenes.load(sireGenes);
		double still = Swine.stillbornRate(composition());
		int live = 0;
		for (int i = 0; i < n; i++) {
			String id = eventId + (i == 0 ? "" : ":piglet" + i);
			double roll = rng.next();
			if (forcedStillborn || (!forced && roll < still)) {
				lastFarrowing.add("stillborn");
				continue;
			}
			Offspring.Result r = Offspring.create(genome, sire, id, world, Ruleset.defaults(), null);
			PigEntity piglet = dev.farmings.FarmingsEntities.PIG.create(level, EntitySpawnReason.BREEDING);
			if (piglet == null) {
				continue;
			}
			PigGenes coat = PigGenes.inherit(genes(), sireCoat != null ? sireCoat : PigGenes.founder(Swine.topBreed(sire.breedCompositionBp()).code, 0),
					BreedingSeed.seed(world, id, genome.hash(), sire.hash(), Ruleset.defaults().version()));
			piglet.initPiglet(r, coat, now);
			piglet.ownerId = ownerId;
			piglet.damId = herdId();
			piglet.sireId = CattleEntity.herdId(sire);
			piglet.place().copyHomeFrom(place());
			piglet.setPos(getX() + 0.4 * ((i % 4) - 1.5), getY(), getZ() + 0.4 * (i / 4));
			level.addFreshEntity(piglet);
			lastFarrowing.add(r.sex().name().toLowerCase(Locale.ROOT));
			live++;
		}
		forcedStillborn = false;
		hasFarrowed = true;
		farrowings++;
		lastLitter = live;
		farrowedTick = now;
		bredTick = -1;
		dueTick = -1;
		sireGenome = "";
		sireGenes = new int[0];
		nextHeatTick = -1;
		sync(now);
	}

	public int pigletsNursing() {
		if (!hasFarrowed || TimeScale.ticksToRealDays(level().getGameTime() - farrowedTick) >= Swine.WEANING_DAYS) {
			return 0;
		}
		return lastLitter;
	}

	public void setFarrowedForTest(double realDaysAgo, int litter) {
		long now = level().getGameTime();
		hasFarrowed = true;
		farrowings = Math.max(1, farrowings);
		lastLitter = litter;
		farrowedTick = now - TimeScale.realDaysToTicks(realDaysAgo);
		sync(now);
	}

	// ---- feed and growth ----

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

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
		this.goalSelector.addGoal(3, place().goal(this, this::isBaby));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		Wild.addGoals(this, this.goalSelector, this::isWild, Wild::grain);
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
		boolean male = sex == Sex.MALE;
		double fed = place().shedController() != null && !stageAt(now).piglet() ? lastFed : 1;
		double mature = Math.max(120, Swine.matureKg(composition(), male));
		double gain = Swine.gainKg(composition(), male, age - span, span, fed) * (stageAt(now).piglet() ? 1 : dev.farmings.feed.Ration.lambFactor(ration));
		if (gain > 0 && weightKg < mature) {
			weightKg = Math.min(mature, weightKg + gain);
		}
	}

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

	/** Empty hand reads the pig; shears on a boar piglet make him a barrow. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) {
			if (!level().isClientSide()) {
				player.sendSystemMessage(Component.literal(readout()));
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(Items.SHEARS) && sex == Sex.MALE && !castrated && stage() == PigStage.PIGLET) {
			if (!level().isClientSide()) {
				castrated = true;
				sync(level().getGameTime());
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				player.sendOverlayMessage(Component.translatable("message.farmings.barrow"));
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	public String breedLabel() {
		List<Map.Entry<String, Integer>> parts = new ArrayList<>(composition().entrySet());
		parts.sort((x, y) -> y.getValue() - x.getValue());
		if (parts.size() == 1) {
			return PigBreed.byCode(parts.get(0).getKey()).label();
		}
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, Integer> e : parts) {
			b.append(b.length() > 0 ? " " : "").append(PigBreed.byCode(e.getKey()).label()).append(String.format(Locale.ROOT, " %.0f%%", e.getValue() / 100.0));
		}
		return b.toString();
	}

	public String readout() {
		StringBuilder b = new StringBuilder();
		if (wild) {
			b.append(String.format(Locale.ROOT, "WILD (settled %.1f of %.0f days), ", settledDays, dev.farmings.husbandry.WildBreeds.SETTLE_DAYS));
		}
		b.append(breedLabel()).append(' ').append(stage().label());
		b.append(String.format(Locale.ROOT, ", %.0f days, %.1f kg", ageRealDays(), weightKg));
		if (genome != null) {
			b.append(", ").append(look().describe());
		}
		if (inHeat()) {
			b.append(", IN HEAT");
		}
		if (pregnant()) {
			b.append(String.format(Locale.ROOT, ", in pig, due in %.1f game days", daysToFarrowing() / TimeScale.REAL_DAYS_PER_GAME_DAY));
		}
		if (pigletsNursing() > 0) {
			b.append(", nursing ").append(pigletsNursing());
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
		Swine.Cuts c = Swine.cuts(weightKg);
		long now = level.getGameTime();
		boolean fire = isOnFire();
		drop(level, fire ? dev.farmings.FarmingsItems.COOKED_PORK_CHOP : dev.farmings.FarmingsItems.PORK_CHOP, c.chops(), now);
		drop(level, dev.farmings.FarmingsItems.PORK_BELLY, c.bellies(), now);
		drop(level, dev.farmings.FarmingsItems.PORK_SHOULDER, c.shoulders(), now);
		drop(level, dev.farmings.FarmingsItems.PORK_FAT, c.fat(), now);
		for (int i = 0; i < c.hams(); i++) {
			spawnAtLocation(level, dev.farmings.FarmingsItems.freshHam(c.hamKg(), now));
		}
	}

	private void drop(ServerLevel level, net.minecraft.world.level.ItemLike item, int n, long now) {
		int max = new ItemStack(item).getMaxStackSize();
		while (n > 0) {
			int k = Math.min(max, n);
			spawnAtLocation(level, dev.farmings.item.Perishables.stamp(new ItemStack(item, k), now));
			n -= k;
		}
	}

	/** Natural spawns follow the vanilla animal rule (grass, light). */
	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
		if (Wild.wildReason(reason)) {
			return Animal.checkAnimalSpawnRules(dev.farmings.FarmingsEntities.PIG, level, reason, blockPosition(), getRandom());
		}
		return true;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (genome == null) {
			PigBreed b = PigBreed.values()[level.getRandom().nextInt(PigBreed.values().length)];
			Sex s = level.getRandom().nextBoolean() ? Sex.FEMALE : Sex.MALE;
			if (Wild.wildReason(reason)) {
				Wild.Group g = Wild.group(groupData, level);
				data = g;
				b = dev.farmings.husbandry.WildBreeds.pig(Wild.biome(level, blockPosition()), g.roll);
				s = g.maleGiven ? Sex.FEMALE : Sex.MALE;
				g.maleGiven = true;
				wild = true;
			}
			double age = Swine.FIRST_BREEDING_DAYS + 30;
			initFounder(b, s, level.getRandom().nextLong(), age, Swine.targetKg(Map.of(b.code, 10_000), s == Sex.MALE, age), level.getLevel().getGameTime());
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
		out.putBoolean("HasFarrowed", hasFarrowed);
		out.putLong("FarrowedTick", farrowedTick);
		out.putInt("Farrowings", farrowings);
		out.putInt("LastLitter", lastLitter);
		out.putLong("NextHeat", nextHeatTick);
		if (ownerId != null) {
			out.putString("Owner", ownerId.toString());
		}
		out.putBoolean("Wild", wild);
		out.putDouble("SettledDays", settledDays);
		out.putString("Dam", damId);
		out.putString("SireId", sireId);
		out.putDouble("LastFed", lastFed);
		out.putDouble("LastWatered", lastWatered);
		place().save(out);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		try {
			breed = PigBreed.byCode(in.getStringOr("Breed", PigBreed.LARGE_WHITE.code));
			sex = Sex.valueOf(in.getStringOr("Sex", Sex.FEMALE.name()));
			genome = in.getString("Genome").map(Genome::decode).orElse(null);
		} catch (IllegalArgumentException e) {
			Farmings.LOGGER.warn("A pig record could not be read; keeping defaults: {}", e.getMessage());
		}
		in.getIntArray("Scores").ifPresent(saved -> {
			if (saved.length <= scores.length) {
				System.arraycopy(saved, 0, scores, 0, saved.length);
			}
		});
		genes = in.getIntArray("Coat").map(PigGenes::load).orElse(null);
		birthTick = in.getLongOr("BirthTick", 0L);
		weightKg = in.getDoubleOr("WeightKg", 0.0);
		growthMarked = in.getBooleanOr("GrowthMarked", false);
		growthTick = in.getLongOr("GrowthTick", 0L);
		castrated = in.getBooleanOr("Castrated", false);
		bredTick = in.getLongOr("BredTick", -1L);
		dueTick = in.getLongOr("Due", -1L);
		sireGenome = in.getStringOr("Sire", "");
		sireGenes = in.getIntArray("SireCoat").orElse(new int[0]);
		hasFarrowed = in.getBooleanOr("HasFarrowed", false);
		farrowedTick = in.getLongOr("FarrowedTick", -1L);
		farrowings = in.getIntOr("Farrowings", 0);
		lastLitter = in.getIntOr("LastLitter", 0);
		nextHeatTick = in.getLongOr("NextHeat", -1L);
		String own = in.getStringOr("Owner", "");
		ownerId = own.isEmpty() ? null : UUID.fromString(own);
		wild = in.getBooleanOr("Wild", false);
		settledDays = in.getDoubleOr("SettledDays", 0);
		damId = in.getStringOr("Dam", "");
		sireId = in.getStringOr("SireId", "");
		lastFed = in.getDoubleOr("LastFed", 1);
		lastWatered = in.getDoubleOr("LastWatered", 1);
		place().load(in);
		sync(level().getGameTime());
	}

	// ---- sounds: the vanilla pig's classic set ----

	private net.minecraft.world.entity.animal.pig.PigSoundVariant.PigSoundSet sounds() {
		var v = SoundEvents.PIG_SOUNDS.get(net.minecraft.world.entity.animal.pig.PigSoundVariants.SoundSet.CLASSIC);
		return isBaby() ? v.babySounds() : v.adultSounds();
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
		playSound(SoundEvents.PIG_STEP.value(), 0.15F, 1.0F);
	}
}

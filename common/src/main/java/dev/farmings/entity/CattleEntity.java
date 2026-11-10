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
public class CattleEntity extends Animal implements Wild.Animal {
	/** Breed and stage, sent to the client so the renderer can pick the right coat, model and udder. */
	private static final EntityDataAccessor<Integer> DATA_BREED = SynchedEntityData.defineId(CattleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(CattleEntity.class, EntityDataSerializers.INT);
	/** The resolved coat (VisualPhenotype.pack) and the seed for her exact markings, for the renderer. */
	private static final EntityDataAccessor<Long> DATA_LOOK = SynchedEntityData.defineId(CattleEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> DATA_MARKINGS = SynchedEntityData.defineId(CattleEntity.class, EntityDataSerializers.INT);

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
	/** Where the parlor has sent her to be milked, or null. Not saved: the controller sends her again. */
	private @Nullable BlockPos milkingStand;
	/** The cow lane she is walking (docs/multiblocks/parlor.md), waypoint by waypoint; not saved: the parlor calls her again. */
	private java.util.@Nullable List<BlockPos> route;
	private int routeIndex;
	private boolean routeHome;
	/** The holding-pen cell she waits on for a parlor stall. */
	private @Nullable BlockPos penSpot;
	/** Game tick up to which growth has been counted; valid only when {@link #growthMarked}. */
	private long growthTick;
	private boolean growthMarked;
	private long calvedTick = -1;
	/** Milk in the udder, kg. One game day's production; unmilked milk is replaced, not stacked (proposed). */
	private double milkKg;
	/** The game tick the current or next heat window opens; -1 when she does not cycle. */
	private long nextHeatTick = -1;
	/** The sire's encoded genome while she is in calf; empty if none (an art/test cow in calf has no sire and never calves). */
	private String sireGenome = "";
	/** The sire's coat genes, saved with his genome at conception (VisualGenome.save), or empty. */
	private int[] sireVisual = new int[0];
	private dev.farmings.genetics.@Nullable VisualGenome visual;
	/** The game tick she calves. */
	private long dueTick = -1;
	/** The player whose barn houses her; calf counts for the tutorial sex rule are kept per player. */
	private @Nullable UUID ownerId;
	/** Wild until tamed in a corral (docs/taming.md). */
	private boolean wild;
	private double settledDays;
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
	/** How many times she has calved; her first lactation gives 15% less (Lactation.parityFactor). */
	private int lactations;
	/** A heifer born twin to a bull who came out infertile (Breeding.FREEMARTIN_RATE). She never cycles. */
	private boolean freemartin;
	/** Her parents' herd-book IDs (the first 8 characters of their genome hash); empty for a founder. */
	private String damId = "";
	private String sireId = "";
	/** How dirty her stall is, 0 to 1, as her barn last saw it (Hygiene). */
	private double dirty;

	public CattleEntity(EntityType<? extends CattleEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BREED, CattleBreed.HOLSTEIN.ordinal());
		builder.define(DATA_STAGE, Stage.HEIFER.ordinal());
		builder.define(DATA_LOOK, -1L);
		builder.define(DATA_MARKINGS, 0);
	}

	/** Her looks as the client knows them, or null before the server has sent them. */
	public dev.farmings.genetics.@Nullable VisualPhenotype syncedLook() {
		long v = entityData.get(DATA_LOOK);
		return v < 0 ? null : dev.farmings.genetics.VisualPhenotype.unpack(v);
	}

	public int syncedMarkings() {
		return entityData.get(DATA_MARKINGS);
	}

	/** Her coat genes; an animal saved before coat genes existed gets them from her genome (VisualGenome.fallback), once. */
	public dev.farmings.genetics.VisualGenome visualGenome() {
		if (visual == null && genome != null) {
			visual = dev.farmings.genetics.VisualGenome.fallback(genome);
		}
		return visual;
	}

	public void setVisualGenome(dev.farmings.genetics.VisualGenome v) {
		this.visual = v;
		syncLook();
	}

	public dev.farmings.genetics.VisualPhenotype look() {
		return dev.farmings.genetics.VisualPhenotype.of(visualGenome(), genome.breedCompositionBp());
	}

	private void syncLook() {
		if (genome != null) {
			entityData.set(DATA_LOOK, look().pack());
			entityData.set(DATA_MARKINGS, dev.farmings.genetics.VisualPhenotype.patternSeed(genome));
		}
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

	/** Walks to the parlor stall she was sent to and stands there until she is milked (it outranks her barn stall). */
	private static final class MilkingGoal extends net.minecraft.world.entity.ai.goal.Goal {
		private final CattleEntity cow;

		MilkingGoal(CattleEntity cow) {
			this.cow = cow;
			setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return (cow.milkingStand != null || cow.route != null || cow.penSpot != null) && !cow.isBaby();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void tick() {
			// first the lane, waypoint by waypoint
			if (cow.route != null) {
				if (cow.routeIndex >= cow.route.size()) {
					cow.route = null;
					cow.routeHome = false;
					return;
				}
				BlockPos w = cow.route.get(cow.routeIndex);
				double dx = cow.getX() - (w.getX() + 0.5);
				double dz = cow.getZ() - (w.getZ() + 0.5);
				double d2 = dx * dx + dz * dz;
				// a waypoint counts as reached within a block, or within a block and a half once the pathfinder says it is there
				// (it stops short of a cell's centre for a cow this wide)
				if (d2 < 1.0 || (d2 < 2.25 && cow.getNavigation().isDone())) {
					cow.routeIndex++;
				} else {
					cow.getNavigation().moveTo(w.getX() + 0.5, w.getY() + 0.2, w.getZ() + 0.5, 1.0);
				}
				return;
			}
			BlockPos stand = cow.milkingStand != null ? cow.milkingStand : cow.penSpot;
			if (stand == null) {
				return;
			}
			double x = stand.getX() + 0.5;
			double z = stand.getZ() + 0.5;
			double y = stand.getY() + 0.25;
			if (cow.distanceToSqr(x, y, z) > 0.25) {
				cow.getNavigation().moveTo(x, y, z, 1.0);
			} else {
				cow.getNavigation().stop();
			}
		}
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MilkingGoal(this));
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new StallGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		Wild.addGoals(this, this.goalSelector, this::isWild);
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
		this.visual = dev.farmings.genetics.VisualGenome.founder(breed.code, seed);
		for (Trait t : dev.farmings.genetics.Species.CATTLE.traits()) {
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
		this.lactations = Math.max(lactations, 2);   // a display cow stands for a grown cow in a later lactation
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

	public @Nullable BlockPos milkingStand() {
		return milkingStand;
	}

	/**
	 * Sends her along a cow lane: towards the parlor (ending on {@code penSpot} to wait) or home to the barn ({@code home}, after which her
	 * stall goal takes her back to her stall). She starts from the waypoint nearest her, so a cow part way along does not turn back.
	 */
	public void walkRoute(java.util.List<BlockPos> waypoints, @Nullable BlockPos penSpot, boolean home) {
		java.util.List<dev.farmings.husbandry.lane.LanePath.Pos> ps = new java.util.ArrayList<>();
		for (BlockPos b : waypoints) {
			ps.add(new dev.farmings.husbandry.lane.LanePath.Pos(b.getX(), b.getY(), b.getZ()));
		}
		this.route = waypoints.isEmpty() ? null : java.util.List.copyOf(waypoints);
		this.routeIndex = waypoints.isEmpty() ? 0 : dev.farmings.husbandry.lane.LanePath.startIndex(ps, getX(), getZ());
		this.routeHome = home;
		this.penSpot = home ? null : penSpot;
	}

	/** The waypoint she is heading for on the lane, or null. */
	public @Nullable BlockPos routeTarget() {
		return route != null && routeIndex < route.size() ? route.get(routeIndex) : null;
	}

	public boolean onRoute() {
		return route != null;
	}

	public boolean walkingHome() {
		return route != null && routeHome;
	}

	public @Nullable BlockPos penSpot() {
		return penSpot;
	}

	/** Stops any lane walk and pen wait. */
	public void clearRoute() {
		this.route = null;
		this.routeHome = false;
		this.penSpot = null;
	}

	public void setMilkingStand(@Nullable BlockPos stand) {
		this.milkingStand = stand;
		if (stand != null) {
			// called from the pen to a stall: the lane walk is over
			this.penSpot = null;
			this.route = null;
			this.routeHome = false;
		}
	}

	/** Fat percent of her milk: her breed's, moved by her butterfat score (husbandry.MilkComponents). */
	public double milkFatPct() {
		return dev.farmings.husbandry.MilkComponents.fat(breed, score(Trait.BUTTERFAT), dev.farmings.genetics.Breeds.baseline(breed.code, Trait.BUTTERFAT));
	}

	public double milkProteinPct() {
		return dev.farmings.husbandry.MilkComponents.breedProtein(breed);
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
					score(Trait.MILK_YIELD), lastFed, lastWatered, Math.max(1, lactations), dirty);
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
		return dryMatterIntakeKgPerDay();
	}

	@Override
	public double dailyWaterL() {
		return dev.farmings.feed.Hay.waterLitresPerDay(dryMatterIntakeKgPerDay(), 0);
	}

	public boolean canConceive() {
		return genome != null && !wild && !freemartin && inHeat() && bredTick < 0;
	}

	/** A service in heat conceives (decided 2026-11-08). The sire's genome is kept until the calf is born. */
	/** Conceives to a sire whose coat genes are not known (a straw): they are taken from his genome (VisualGenome.fallback). */
	public boolean conceive(Genome sire, long nowTick) {
		return conceive(sire, dev.farmings.genetics.VisualGenome.fallback(sire), nowTick);
	}

	public boolean conceive(Genome sire, dev.farmings.genetics.VisualGenome sireCoat, long nowTick) {
		if (!canConceive()) {
			return false;
		}
		this.bredTick = nowTick;
		this.sireGenome = sire.encode();
		this.sireVisual = sireCoat.save();
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
		if (freemartin || !dev.farmings.husbandry.Breeding.cycles(breed, stageAt(now), false, ageRealDays(), weightKg, sinceCalving)) {
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

	/**
	 * She calves: one calf, or twins (Breeding.twinRate), each of which may be stillborn (Breeding.stillbirthRate). A stillborn calf is
	 * not spawned. A heifer twinned with a bull may be a freemartin. Her lactation starts either way. Every roll comes from a generator
	 * seeded by the calving, so the same calving in the same world gives the same calves.
	 */
	private void calve(ServerLevel level, long now) {
		Genome sire = Genome.decode(sireGenome);
		boolean firstCalf = !hasCalved;
		lastCalving.clear();
		CalfCounts counts = CalfCounts.get(level.getServer());
		String eventId = "calving:" + getUUID() + ":" + now;
		dev.farmings.genetics.Rng rng = new dev.farmings.genetics.Rng(dev.farmings.genetics.BreedingSeed.seed(Long.toString(level.getSeed()),
				eventId + ":birth", genome.hash(), sire.hash(), Ruleset.defaults().version()));
		boolean twinRoll = dev.farmings.husbandry.Breeding.twins(rng.next(), firstCalf);
		int count = forcedCalves > 0 ? forcedCalves : twinRoll ? 2 : 1;
		java.util.List<CattleEntity> born = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			Sex forced = ownerId == null ? null : dev.farmings.husbandry.Breeding.forcedSex(counts.count(ownerId));
			dev.farmings.genetics.Offspring.Result r = dev.farmings.genetics.Offspring.create(genome, sire, eventId + (i == 0 ? "" : ":twin" + i),
					Long.toString(level.getSeed()), Ruleset.defaults(), forced);
			boolean deadRoll = dev.farmings.husbandry.Breeding.stillborn(rng.next(), firstCalf);
			boolean dead = forcedStillborn != null ? forcedStillborn : deadRoll;
			lastCalving.add(dead ? "stillborn " + r.sex().name().toLowerCase(java.util.Locale.ROOT) : r.sex().name().toLowerCase(java.util.Locale.ROOT));
			if (dead) {
				continue;
			}
			CattleEntity calf = dev.farmings.FarmingsEntities.CATTLE.create(level, EntitySpawnReason.BREEDING);
			if (calf != null) {
				calf.initCalf(r, now, breed.matureWeightKg);
				dev.farmings.genetics.VisualGenome sireCoat = dev.farmings.genetics.VisualGenome.load(sireVisual);
				calf.setVisualGenome(dev.farmings.genetics.VisualGenome.inherit(visualGenome(), sireCoat != null ? sireCoat : dev.farmings.genetics.VisualGenome.fallback(sire),
						dev.farmings.genetics.BreedingSeed.seed(Long.toString(level.getSeed()), eventId + (i == 0 ? "" : ":twin" + i), genome.hash(), sire.hash(),
								Ruleset.defaults().version())));
				calf.ownerId = ownerId;
				calf.damId = herdId(genome);
				calf.sireId = herdId(sire);
				calf.setPos(getX() + 0.8, getY(), getZ() + i * 0.8);
				level.addFreshEntity(calf);
				born.add(calf);
				if (ownerId != null) {
					counts.add(ownerId);
				}
			}
		}
		if (count == 2) {
			boolean bull = forcedFreemartin || lastCalving.stream().anyMatch(t -> t.endsWith("male") && !t.endsWith("female"));
			for (CattleEntity c : born) {
				if (bull && c.sex == Sex.FEMALE && (forcedFreemartin || dev.farmings.husbandry.Breeding.freemartin(rng.next()))) {
					c.freemartin = true;
				}
			}
		}
		hasCalved = true;
		lactations++;
		calvedTick = now;
		bredTick = -1;
		sireGenome = "";
		sireVisual = new int[0];
		dueTick = -1;
		nextHeatTick = -1;
		lastMilkDay = -1;
		syncAge(now);
		updateMilk(now);
	}

	/** Game tests only: fix the next calving's outcome (0 calves = roll; null = roll). Not saved. */
	private int forcedCalves;
	private @Nullable Boolean forcedStillborn;
	private boolean forcedFreemartin;

	public void forceCalvingForTest(int calves, @Nullable Boolean stillborn, boolean freemartinTwin) {
		this.forcedCalves = calves;
		this.forcedStillborn = stillborn;
		this.forcedFreemartin = freemartinTwin;
	}

	/** What the last calving gave, for tests and the readout: "female", "male", "stillborn female"... Not saved. */
	private final java.util.List<String> lastCalving = new java.util.ArrayList<>();

	public java.util.List<String> lastCalving() {
		return lastCalving;
	}

	/** A herd-book ID: the first 8 characters of the genome hash, the same in every world for the same genes. */
	public static String herdId(@Nullable Genome g) {
		if (g == null) {
			return "--------";
		}
		String h = g.hash();
		return h.length() > 8 ? h.substring(0, 8) : h;
	}

	public String herdId() {
		return herdId(genome);
	}

	public String damId() {
		return damId;
	}

	public String sireId() {
		return sireId;
	}

	public long birthTick() {
		return birthTick;
	}

	public boolean castrated() {
		return castrated;
	}

	public boolean freemartin() {
		return freemartin;
	}

	public int lactations() {
		return lactations;
	}

	/** For game tests: how many times she has calved. */
	public void setLactationsForTest(int n) {
		this.lactations = n;
	}

	/** For game tests: marks her a freemartin without waiting for a twin calving. */
	public void setFreemartinForTest(boolean f) {
		this.freemartin = f;
	}

	public double dirty() {
		return dirty;
	}

	/** Her barn reports how dirty her stall is; past half dirty she slowly loses health, never below the floor (Hygiene). */
	public void applyDirt(double dirty, long ticks) {
		this.dirty = dirty;
		float loss = (float) dev.farmings.husbandry.Hygiene.healthLoss(dirty, getHealth(), ticks);
		if (loss > 0) {
			setHealth(getHealth() - loss);
		}
	}

	/** The once-a-second bookkeeping: age and stage, the udder, heat and calving. Game tests call it to skip waiting. */
	public void settle(ServerLevel level) {
		long now = level.getGameTime();
		grow(now);
		syncAge(now);
		updateMilk(now);
		updateBreeding(level, now);
	}

	/**
	 * Gains weight on the growth curve (docs/animals/growth.md): housed animals by how well they were fed (a calf needs no feed), animals
	 * outside the barn at 100%. Counts from the first time it runs, so a loaded older cow starts from now.
	 */
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
		double age = ageRealDays();
		boolean housed = stallController != null && stage() != Stage.CALF;
		double fed = housed ? lastFed : 1;
		double gain = dev.farmings.husbandry.Growth.gainKg(breed, age - span, span, fed);
		if (gain > 0 && weightKg < breed.matureWeightKg) {
			weightKg = Math.min(breed.matureWeightKg, weightKg + gain);
		}
	}

	/** For game tests: she is {@code realDays} older and has been growing that long. */
	public void advanceForTest(double realDays) {
		long ticks = TimeScale.realDaysToTicks(realDays);
		birthTick -= ticks;
		growthTick -= ticks;
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
		for (Trait t : dev.farmings.genetics.Species.CATTLE.traits()) {
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
		syncLook();
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
		// The heat cue: hearts every 3 seconds while she is in heat (a game sign; real signs are standing to be mounted and restlessness).
		if (tickCount % 60 == 0 && inHeat()) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 2, 0.3, 0.2, 0.3, 0.0);
		}
	}

	/**
	 * An empty hand reads the animal (breed, sex, age, weight, stage, breeding, milk, feed, health). Shears on a bull calf castrate him,
	 * so he grows as a steer for beef (proposed; real farms band or castrate calves young).
	 */
	@Override
	public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) {
			if (!level().isClientSide()) {
				player.sendSystemMessage(net.minecraft.network.chat.Component.literal(readout()));
			}
			return net.minecraft.world.InteractionResult.SUCCESS;
		}
		if (stack.is(net.minecraft.world.item.Items.SHEARS) && sex == Sex.MALE && !castrated && stage() == Stage.BULL_CALF) {
			if (!level().isClientSide()) {
				castrated = true;
				syncAge(level().getGameTime());
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.farmings.castrated"));
			}
			return net.minecraft.world.InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/** "holstein", or "holstein 50% angus 50%" for a cross, largest share first. */
	public String breedLabel() {
		if (genome == null) {
			return breed.assetName().replace('_', ' ');
		}
		java.util.List<java.util.Map.Entry<String, Integer>> parts = new java.util.ArrayList<>(genome.breedCompositionBp().entrySet());
		parts.sort((x, y) -> y.getValue() - x.getValue());
		if (parts.size() == 1) {
			return CattleBreed.byCode(parts.get(0).getKey()).assetName().replace('_', ' ');
		}
		StringBuilder b = new StringBuilder();
		for (java.util.Map.Entry<String, Integer> e : parts) {
			if (b.length() > 0) {
				b.append(' ');
			}
			b.append(CattleBreed.byCode(e.getKey()).assetName().replace('_', ' ')).append(' ')
					.append(String.format(java.util.Locale.ROOT, "%.0f%%", e.getValue() / 100.0));
		}
		return b.toString();
	}

	/** One line about the animal, as the readout shows it. */
	public String readout() {
		StringBuilder b = new StringBuilder();
		if (wild) {
			b.append(String.format(java.util.Locale.ROOT, "WILD (settled %.1f of %.0f days), ", settledDays, dev.farmings.husbandry.WildBreeds.SETTLE_DAYS));
		}
		b.append(breedLabel()).append(' ').append(sex == Sex.FEMALE ? "female" : castrated ? "steer" : "male");
		b.append(", ").append(String.format(java.util.Locale.ROOT, "%.0f months", ageRealDays() / 30.4));
		b.append(", ").append(String.format(java.util.Locale.ROOT, "%.0f kg", weightKg));
		b.append(", ").append(stage().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
		if (genome != null) {
			b.append(", ").append(look().describe());
		}
		if (freemartin) {
			b.append(", freemartin (infertile)");
		}
		if (inHeat()) {
			b.append(", IN HEAT");
		}
		if (isInCalf()) {
			long left = dueTick - level().getGameTime();
			b.append(dueTick >= 0 ? String.format(java.util.Locale.ROOT, ", in calf, due in %.1f game days", left / (double) TimeScale.TICKS_PER_GAME_DAY) : ", in calf");
		}
		if (Lifecycle.givesMilk(stage())) {
			b.append(", day ").append(daysInMilk()).append(" in milk, lactation ").append(Math.max(1, lactations));
			b.append(String.format(java.util.Locale.ROOT, ", %.1f kg in udder", milkKg));
		}
		if (stallController != null) {
			b.append(String.format(java.util.Locale.ROOT, ", fed %.0f%%, watered %.0f%%", lastFed * 100, lastWatered * 100));
			if (dirty > 0) {
				b.append(String.format(java.util.Locale.ROOT, ", stall %.0f%% dirty", dirty * 100));
			}
		}
		b.append(String.format(java.util.Locale.ROOT, ", health %.0f/%.0f", getHealth(), getMaxHealth()));
		return b.toString();
	}

	/** What she gives when killed: beef and steaks by carcass weight (husbandry.Carcass, 1 item = 10 kg) and 1-3 leather. */
	public dev.farmings.husbandry.Carcass.Yield carcass() {
		java.util.Map<String, Integer> comp = genome != null ? genome.breedCompositionBp() : java.util.Map.of(breed.code, 10_000);
		return dev.farmings.husbandry.Carcass.of(weightKg, comp);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		dev.farmings.husbandry.Carcass.Yield y = carcass();
		boolean fire = isOnFire();
		dropCount(level, fire ? net.minecraft.world.item.Items.COOKED_BEEF : net.minecraft.world.item.Items.BEEF, y.beefItems());
		dropCount(level, dev.farmings.FarmingsItems.steak(y.grade(), fire), y.steakItems());
		dropCount(level, net.minecraft.world.item.Items.LEATHER, 1 + random.nextInt(3));
	}

	private void dropCount(ServerLevel level, net.minecraft.world.level.ItemLike item, int count) {
		int max = new ItemStack(item).getMaxStackSize();
		while (count > 0) {
			int n = Math.min(max, count);
			spawnAtLocation(level, dev.farmings.item.Perishables.stamp(new ItemStack(item, n), level.getGameTime()));
			count -= n;
		}
	}

	/** Natural spawns follow the vanilla animal rule: on grass, in light 9 or more (docs/taming.md). */
	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
		if (Wild.wildReason(reason)) {
			return net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(dev.farmings.FarmingsEntities.CATTLE, level, reason, blockPosition(), getRandom());
		}
		return super.checkSpawnRules(level, reason);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (genome == null && Wild.wildReason(reason)) {
			// A wild founder: the breed comes from the biome, one bull per spawn group (docs/taming.md).
			Wild.Group g = Wild.group(groupData, level);
			data = g;
			CattleBreed b = dev.farmings.husbandry.WildBreeds.cattle(Wild.biome(level, blockPosition()), g.roll);
			Sex sex = g.maleGiven ? Sex.FEMALE : Sex.MALE;
			g.maleGiven = true;
			initFounder(b, sex, level.getRandom().nextLong(), Lifecycle.FIRST_BREEDING_AGE_DAYS, b.breedingWeightKg(), level.getLevel().getGameTime());
			wild = true;
		} else if (genome == null) {
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
		output.putBoolean("GrowthMarked", growthMarked);
		output.putLong("GrowthTick", growthTick);
		output.putBoolean("Castrated", castrated);
		output.putLong("BredTick", bredTick);
		output.putBoolean("HasCalved", hasCalved);
		output.putLong("CalvedTick", calvedTick);
		output.putDouble("MilkKg", milkKg);
		output.putLong("NextHeat", nextHeatTick);
		output.putString("Sire", sireGenome);
		output.putIntArray("SireVisual", sireVisual);
		if (visual != null) {
			output.putIntArray("Visual", visual.save());
		}
		output.putLong("Due", dueTick);
		if (ownerId != null) {
			output.putString("Owner", ownerId.toString());
		}
		output.putBoolean("Wild", wild);
		output.putDouble("SettledDays", settledDays);
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
		output.putInt("Lactations", lactations);
		output.putBoolean("Freemartin", freemartin);
		output.putDouble("Dirty", dirty);
		output.putString("Dam", damId);
		output.putString("SireId", sireId);
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
			// Saves from before sheep traits were added hold only the eight cattle scores; they are a prefix of the trait order.
			if (saved.length <= scores.length) {
				System.arraycopy(saved, 0, scores, 0, saved.length);
			}
		});
		birthTick = input.getLongOr("BirthTick", 0L);
		weightKg = input.getDoubleOr("WeightKg", 0.0);
		growthMarked = input.getBooleanOr("GrowthMarked", false);
		growthTick = input.getLongOr("GrowthTick", 0L);
		castrated = input.getBooleanOr("Castrated", false);
		bredTick = input.getLongOr("BredTick", -1L);
		hasCalved = input.getBooleanOr("HasCalved", false);
		calvedTick = input.getLongOr("CalvedTick", -1L);
		milkKg = input.getDoubleOr("MilkKg", 0);
		nextHeatTick = input.getLongOr("NextHeat", -1L);
		sireGenome = input.getStringOr("Sire", "");
		sireVisual = input.getIntArray("SireVisual").orElse(new int[0]);
		visual = input.getIntArray("Visual").map(dev.farmings.genetics.VisualGenome::load).orElse(null);
		dueTick = input.getLongOr("Due", -1L);
		String own = input.getStringOr("Owner", "");
		ownerId = own.isEmpty() ? null : java.util.UUID.fromString(own);
		wild = input.getBooleanOr("Wild", false);
		settledDays = input.getDoubleOr("SettledDays", 0);
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
		lactations = input.getIntOr("Lactations", hasCalved ? 1 : 0);
		freemartin = input.getBooleanOr("Freemartin", false);
		dirty = input.getDoubleOr("Dirty", 0);
		damId = input.getStringOr("Dam", "");
		sireId = input.getStringOr("SireId", "");
		entityData.set(DATA_BREED, breed.ordinal());
		entityData.set(DATA_STAGE, stageAt(level().getGameTime()).ordinal());
		syncLook();
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

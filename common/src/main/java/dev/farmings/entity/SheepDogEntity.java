package dev.farmings.entity;

import dev.farmings.husbandry.TimeScale;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A working Border Collie (docs/animals/sheep.md, "Sheep dog"). It spawns wild and is tamed in a corral on raw meat (docs/taming.md). A
 * tame dog takes whistle commands from its owner: Come bye gathers every tame sheep within 32 blocks and brings them to the owner; Walk
 * up drives them to the whistle's target; Lie down stops it and the sheep stand; That'll do sends it back to heel. It herds from the
 * flock's far side, 6 blocks behind; each sheep within 8 blocks of it is sent toward the target, and is let go within 2 blocks of it.
 */
public class SheepDogEntity extends PathfinderMob implements Wild.Animal {
	public enum Command { COME_BYE, WALK_UP, LIE_DOWN, THATLL_DO }

	public static final double GATHER_RANGE = 32;
	public static final double HEAR_RANGE = 64;
	public static final double BEHIND = 6;
	public static final double PRESSURE = 8;
	public static final double ARRIVED = 2;
	/** One raw meat item a day, 0.5 kg *(proposed; NOT FOUND)*; water about 1 L a day *(proposed: 50 mL/kg for a 20 kg dog)*. */
	public static final double MEAT_KG_PER_DAY = 0.5;
	public static final double WATER_L_PER_DAY = 1.0;

	private static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(SheepDogEntity.class, EntityDataSerializers.BOOLEAN);

	private boolean wild;
	private double settledDays;
	private @Nullable UUID ownerId;
	private @Nullable BlockPos lure;
	private Command command = Command.THATLL_DO;
	private boolean working;
	private @Nullable BlockPos target;
	private final Set<UUID> driven = new HashSet<>();

	public SheepDogEntity(EntityType<? extends SheepDogEntity> type, Level level) {
		super(type, level);
	}

	/** A Border Collie: a wolf's health and speed (no working-collie figure found, NOT FOUND). */
	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.TEMPT_RANGE, 10.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SITTING, false);
	}

	/** Lying down (drawn sitting). */
	public boolean sitting() {
		return entityData.get(DATA_SITTING);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new HerdGoal(this));
		goalSelector.addGoal(5, new HeelGoal(this));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
		Wild.addGoals(this, goalSelector, this::isWild, Wild::meat);
	}

	// ---- commands ----

	public Command command() {
		return command;
	}

	public boolean working() {
		return working;
	}

	public @Nullable BlockPos target() {
		return target;
	}

	/** A whistled command from the owner. {@code target} is where Walk up drives to; Come bye brings the flock to {@code caller}. */
	public boolean hear(Command c, @Nullable BlockPos to, Player caller) {
		if (wild || ownerId == null || !ownerId.equals(caller.getUUID())) {
			return false;
		}
		command = c;
		switch (c) {
			case COME_BYE -> {
				target = caller.blockPosition();
				working = true;
			}
			case WALK_UP -> {
				target = to;
				working = to != null;
			}
			case LIE_DOWN -> {
				working = false;
				holdSheep();
			}
			case THATLL_DO -> {
				working = false;
				releaseSheep();
			}
		}
		entityData.set(DATA_SITTING, c == Command.LIE_DOWN);
		return true;
	}

	/** The tame sheep and goats it is moving now: within reach and not yet at the target. */
	public List<SmallRuminant> flock() {
		if (target == null) {
			return List.of();
		}
		List<SmallRuminant> l = new ArrayList<>();
		for (SmallRuminant s : SmallRuminant.in(level(), getBoundingBox().inflate(GATHER_RANGE), s -> !s.isWild() && s.self().isAlive())) {
			if (s.self().blockPosition().distSqr(target) > ARRIVED * ARRIVED) {
				l.add(s);
			}
		}
		return l;
	}

	private void holdSheep() {
		for (SmallRuminant s : drivenSheep()) {
			s.sendTo(s.self().blockPosition());
		}
	}

	private void releaseSheep() {
		for (SmallRuminant s : drivenSheep()) {
			s.sendTo(null);
		}
		driven.clear();
	}

	private List<SmallRuminant> drivenSheep() {
		if (!(level() instanceof ServerLevel server)) {
			return List.of();
		}
		List<SmallRuminant> l = new ArrayList<>();
		for (UUID id : driven) {
			if (server.getEntity(id) instanceof SmallRuminant s) {
				l.add(s);
			}
		}
		return l;
	}

	private int herdSteps;

	/** One herding step (every 10 ticks): get behind the flock and press each sheep near it toward the target. */
	void herd() {
		herdSteps++;
		if (target == null) {
			working = false;
			return;
		}
		List<SmallRuminant> flock = flock();
		// sheep already at the target are let go, under their shed's rules again
		for (SmallRuminant s : drivenSheep()) {
			if (s.self().blockPosition().distSqr(target) <= ARRIVED * ARRIVED) {
				s.sendTo(null);
				driven.remove(s.self().getUUID());
			}
		}
		if (flock.isEmpty()) {
			working = false;
			command = Command.THATLL_DO;
			driven.clear();
			return;
		}
		double cx = 0;
		double cz = 0;
		for (SmallRuminant s : flock) {
			cx += s.self().getX();
			cz += s.self().getZ();
		}
		cx /= flock.size();
		cz /= flock.size();
		Vec3 t = Vec3.atBottomCenterOf(target);
		double dx = cx - t.x;
		double dz = cz - t.z;
		double d = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
		double px = cx + dx / d * BEHIND;
		double pz = cz + dz / d * BEHIND;
		getNavigation().moveTo(px, getY(), pz, 1.3);
		for (SmallRuminant s : flock) {
			if (s.self().distanceTo(this) <= PRESSURE || driven.contains(s.self().getUUID())) {
				s.sendTo(target);
				driven.add(s.self().getUUID());
			}
		}
	}

	private static final class HerdGoal extends Goal {
		private final SheepDogEntity dog;

		HerdGoal(SheepDogEntity dog) {
			this.dog = dog;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		/** Ticked every tick: it counts its own steps by the dog's tick, which skipping ticks would miss. */
		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public boolean canUse() {
			return !dog.wild && (dog.working || dog.command == Command.LIE_DOWN);
		}

		@Override
		public void tick() {
			if (dog.command == Command.LIE_DOWN) {
				dog.getNavigation().stop();
				return;
			}
			if (dog.tickCount % 10 == 0) {
				dog.herd();
			}
		}
	}

	/** Not working: keeps near its owner (within 3 blocks once it comes in). */
	private static final class HeelGoal extends Goal {
		private final SheepDogEntity dog;

		HeelGoal(SheepDogEntity dog) {
			this.dog = dog;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		/** Ticked every tick: it counts its own steps by the dog's tick, which skipping ticks would miss. */
		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		private @Nullable Player owner() {
			return dog.ownerId == null ? null : dog.level().getPlayerByUUID(dog.ownerId);
		}

		@Override
		public boolean canUse() {
			Player p = owner();
			return !dog.wild && !dog.working && dog.command != Command.LIE_DOWN && p != null && p.distanceToSqr(dog) > 25 && p.distanceToSqr(dog) < HEAR_RANGE * HEAR_RANGE;
		}

		@Override
		public boolean canContinueToUse() {
			Player p = owner();
			return canUse() && p != null && p.distanceToSqr(dog) > 9;
		}

		@Override
		public void tick() {
			Player p = owner();
			if (p != null && dog.tickCount % 10 == 0) {
				dog.getNavigation().moveTo(p, 1.2);
			}
		}
	}

	// ---- wild and taming (docs/taming.md) ----

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

	@Override
	public double dailyDryMatterKg() {
		return MEAT_KG_PER_DAY;
	}

	@Override
	public double dailyWaterL() {
		return WATER_L_PER_DAY;
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
	public boolean isFeed(net.minecraft.world.item.ItemStack stack) {
		return Wild.meat(stack);
	}

	@Override
	public Wild.Bait bait() {
		return Wild.Bait.MEAT;
	}

	@Override
	public TimeScale.Scale timeScale() {
		return TimeScale.LIVESTOCK;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide() && player.getItemInHand(hand).isEmpty()) {
			player.sendSystemMessage(Component.literal(readout()));
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	public String readout() {
		if (wild) {
			return String.format(Locale.ROOT, "Border Collie: WILD, settled %.1f days", settledDays);
		}
		String c = command.name().toLowerCase(Locale.ROOT).replace('_', ' ').replace("thatll", "that'll");
		return "Border Collie: tame, " + (working ? "working: " + c + ", " + flock().size() + " sheep to move" : c);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	/** Natural spawns follow the vanilla animal rule: on grass, in light 9 or more. */
	@Override
	public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
		if (Wild.wildReason(reason)) {
			// the vanilla animal rule, written out: Animal.checkAnimalSpawnRules takes only Animal types
			BlockPos p = blockPosition();
			return level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON) && level.getRawBrightness(p, 0) > 8;
		}
		return true;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		if (Wild.wildReason(reason)) {
			wild = true;
		}
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putBoolean("Wild", wild);
		out.putDouble("Settled", settledDays);
		if (ownerId != null) {
			out.putString("Owner", ownerId.toString());
		}
		out.putString("Command", command.name());
		out.putBoolean("Working", working);
		if (target != null) {
			out.putLong("Target", target.asLong());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		wild = in.getBooleanOr("Wild", false);
		settledDays = in.getDoubleOr("Settled", 0);
		String o = in.getStringOr("Owner", "");
		ownerId = o.isEmpty() ? null : UUID.fromString(o);
		try {
			command = Command.valueOf(in.getStringOr("Command", "THATLL_DO"));
		} catch (IllegalArgumentException e) {
			command = Command.THATLL_DO;
		}
		working = in.getBooleanOr("Working", false);
		long t = in.getLongOr("Target", Long.MIN_VALUE);
		target = t == Long.MIN_VALUE ? null : BlockPos.of(t);
		entityData.set(DATA_SITTING, command == Command.LIE_DOWN);
	}
}

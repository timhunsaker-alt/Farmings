package dev.farmings.entity;

import dev.farmings.husbandry.WildBreeds;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/** Wild animals and taming (docs/taming.md): shared by every Farmings species. */
public final class Wild {
	private Wild() {
	}

	/** What the corral needs from an animal of any species. */
	public interface Animal {
		boolean isWild();

		void setWild(boolean wild);

		/** Real days spent fed and watered in a corral. Kept if the animal leaves early. */
		double settledDays();

		void setSettledDays(double days);

		@Nullable UUID owner();

		void setOwner(@Nullable UUID owner);

		/** This species' daily dry matter (kg) and water (L) needs, for one real day. */
		double dailyDryMatterKg();

		double dailyWaterL();

		default void tame(@Nullable UUID owner) {
			setWild(false);
			setOwner(owner);
		}
	}

	/** Natural spawns are wild; summoned, egg-spawned and born animals are herd animals (proposed). */
	public static boolean wildReason(EntitySpawnReason reason) {
		return reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
	}

	/** Cattle and sheep feed that lures them: wheat or a hay block. */
	public static boolean feed(ItemStack stack) {
		return stack.is(Items.WHEAT) || stack.is(Items.HAY_BLOCK);
	}

	private static boolean holdsFeed(Player p) {
		return feed(p.getMainHandItem()) || feed(p.getOffhandItem());
	}

	/** A wild animal follows feed, and keeps away from a player with none. Herd animals follow feed too (vanilla tempt). */
	public static void addGoals(PathfinderMob mob, GoalSelector goals, BooleanSupplier wild) {
		goals.addGoal(3, new TemptGoal(mob, 1.1, Wild::feed, false));
		goals.addGoal(4, new FleeGoal(mob, wild));
	}

	/** The biome path where a spawn happens, e.g. "minecraft:plains". */
	public static String biome(ServerLevelAccessor level, net.minecraft.core.BlockPos pos) {
		return level.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("minecraft:plains");
	}

	/** One spawn group: one breed, and the first animal is the group's male. No babies in a wild group. */
	public static final class Group extends net.minecraft.world.entity.AgeableMob.AgeableMobGroupData {
		public final int roll;
		public boolean maleGiven;

		public Group(int roll) {
			super(false);
			this.roll = roll;
		}
	}

	/**
	 * A wild animal walks away from a player within its flight distance who holds no feed (vanilla's avoid goal needs a combat
	 * target, which a player is not on peaceful).
	 */
	static final class FleeGoal extends Goal {
		private final PathfinderMob mob;
		private final BooleanSupplier wild;
		private @Nullable Player from;

		FleeGoal(PathfinderMob mob, BooleanSupplier wild) {
			this.mob = mob;
			this.wild = wild;
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		private @Nullable Player threat() {
			if (!wild.getAsBoolean()) {
				return null;
			}
			Player near = null;
			double best = WildBreeds.FLIGHT_BLOCKS * WildBreeds.FLIGHT_BLOCKS;
			for (Player p : mob.level().players()) {
				double d = p.distanceToSqr(mob);
				if (d < best && p.isAlive() && !p.isSpectator() && !holdsFeed(p)) {
					best = d;
					near = p;
				}
			}
			return near;
		}

		@Override
		public boolean canUse() {
			from = threat();
			if (from == null) {
				return false;
			}
			Vec3 away = DefaultRandomPos.getPosAway(mob, 16, 7, from.position());
			return away != null && mob.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
		}

		@Override
		public boolean canContinueToUse() {
			return !mob.getNavigation().isDone() && from != null && from.distanceToSqr(mob) < 4 * WildBreeds.FLIGHT_BLOCKS * WildBreeds.FLIGHT_BLOCKS;
		}

		@Override
		public void stop() {
			from = null;
		}
	}

	/** The group for this spawn: the one passed in, or a new one. */
	public static Group group(@Nullable SpawnGroupData data, ServerLevelAccessor level) {
		return data instanceof Group g ? g : new Group(level.getRandom().nextInt(1 << 16));
	}
}

package dev.farmings.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Where a housed small ruminant belongs and where it is going (docs/multiblocks/sheep-shed.md): its shed, its home cell, a place it has
 * been sent (a pasture spot, a stall, the shearing stand), a route of waypoints, and the stall that has reserved it. Sheep and goats each
 * carry one, and its {@link #goal} walks them there.
 */
public final class Placement {
	private @Nullable BlockPos home;
	private @Nullable BlockPos shedController;
	private @Nullable BlockPos target;
	private @Nullable List<BlockPos> route;
	private int routeIndex;
	private boolean routeHome;
	/** The stall or stand that has called this animal, or null. */
	private @Nullable BlockPos reservedBy;

	public @Nullable BlockPos shedController() {
		return shedController;
	}

	public @Nullable BlockPos home() {
		return home;
	}

	/** Its shed and its place in it (a pen cell, or a lambing or kidding pen). */
	public void assignShed(@Nullable BlockPos controller, @Nullable BlockPos home) {
		this.shedController = controller;
		this.home = home;
	}

	/** Somewhere it is sent for a while; null to let it go home. */
	public void sendTo(@Nullable BlockPos target) {
		this.target = target;
		if (target != null) {
			this.route = null;
		}
	}

	public @Nullable BlockPos sentTo() {
		return target;
	}

	public @Nullable BlockPos reservedBy() {
		return reservedBy;
	}

	public void reserve(@Nullable BlockPos stand) {
		this.reservedBy = stand;
	}

	/** Sends it along waypoints (a lane, the pasture gate), starting from the waypoint nearest it at (x, z). */
	public void walkRoute(List<BlockPos> waypoints, boolean home, double x, double z) {
		List<dev.farmings.husbandry.lane.LanePath.Pos> ps = new ArrayList<>();
		for (BlockPos b : waypoints) {
			ps.add(new dev.farmings.husbandry.lane.LanePath.Pos(b.getX(), b.getY(), b.getZ()));
		}
		this.route = waypoints.isEmpty() ? null : List.copyOf(waypoints);
		this.routeIndex = waypoints.isEmpty() ? 0 : dev.farmings.husbandry.lane.LanePath.startIndex(ps, x, z);
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

	/** A newborn takes its dam's shed and home. */
	public void copyHomeFrom(Placement dam) {
		this.home = dam.home;
		this.shedController = dam.shedController;
	}

	public void save(ValueOutput out) {
		out.putLong("Home", home == null ? Long.MIN_VALUE : home.asLong());
		out.putLong("Shed", shedController == null ? Long.MIN_VALUE : shedController.asLong());
	}

	public void load(ValueInput in) {
		long h = in.getLongOr("Home", Long.MIN_VALUE);
		home = h == Long.MIN_VALUE ? null : BlockPos.of(h);
		long s = in.getLongOr("Shed", Long.MIN_VALUE);
		shedController = s == Long.MIN_VALUE ? null : BlockPos.of(s);
	}

	/** The goal that follows a route, then goes to the target, else to the home cell (not while young). Outranks strolling. */
	public Goal goal(PathfinderMob mob, BooleanSupplier young) {
		return new PlaceGoal(mob, this, young);
	}

	private static final class PlaceGoal extends Goal {
		private final PathfinderMob mob;
		private final Placement p;
		private final BooleanSupplier young;
		/** Where it was when last checked, and how long it has made no headway: snagged on a gate post, it gets a nudge. */
		private double lastX = Double.NaN;
		private double lastZ = Double.NaN;
		private int still;

		PlaceGoal(PathfinderMob mob, Placement p, BooleanSupplier young) {
			this.mob = mob;
			this.p = p;
			this.young = young;
			setFlags(java.util.EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return p.route != null || p.target != null || (p.home != null && !young.getAsBoolean());
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		/** Ticked every tick: the stuck check counts by the animal's tick, which skipping ticks would miss. */
		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		/** Two seconds with no headway toward (x, z): a small push that way, as a snagged animal slides off a post corner, and a fresh path. */
		private void unstick(double x, double z) {
			double moved = Double.isNaN(lastX) ? 1 : Math.abs(mob.getX() - lastX) + Math.abs(mob.getZ() - lastZ);
			if (mob.tickCount % 20 != 0) {
				return;
			}
			lastX = mob.getX();
			lastZ = mob.getZ();
			still = moved < 0.15 ? still + 1 : 0;
			if (still >= 2) {
				double dx = x - mob.getX();
				double dz = z - mob.getZ();
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > 0.01) {
					mob.push(dx / d * 0.25, 0.0, dz / d * 0.25);
				}
				mob.getNavigation().stop();
				still = 0;
			}
		}

		@Override
		public void tick() {
			if (p.route != null) {
				if (p.routeIndex >= p.route.size()) {
					p.route = null;
					p.routeHome = false;
					return;
				}
				BlockPos w = p.route.get(p.routeIndex);
				double dx = mob.getX() - (w.getX() + 0.5);
				double dz = mob.getZ() - (w.getZ() + 0.5);
				double d2 = dx * dx + dz * dz;
				if (d2 < 1.0 || (d2 < 2.25 && mob.getNavigation().isDone())) {
					p.routeIndex++;
				} else {
					mob.getNavigation().moveTo(w.getX() + 0.5, w.getY() + 0.2, w.getZ() + 0.5, 1.0);
				}
				return;
			}
			BlockPos at = p.target != null ? p.target : p.home;
			if (at == null) {
				return;
			}
			double x = at.getX() + 0.5;
			double z = at.getZ() + 0.5;
			double dx = mob.getX() - x;
			double dz = mob.getZ() - z;
			// within a block of its pen cell it is home (two blocks let a sheep stop in the gate, a block outside the pen)
			double reach = p.target != null ? 0.36 : 1.0;
			if (dx * dx + dz * dz > reach) {
				unstick(x, z);
				mob.getNavigation().moveTo(x, at.getY() + 0.1, z, 1.0);
			} else {
				still = 0;
				if (p.target != null) {
					mob.getNavigation().stop();
				}
			}
		}
	}
}

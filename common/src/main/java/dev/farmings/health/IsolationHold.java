package dev.farmings.health;

import dev.farmings.husbandry.health.IsolationLayout;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Sends animals to an isolation pen and keeps them there (docs/multiblocks/isolation-pen.md): a tamed animal to quarantine for 30 real days
 * when a quarantine pen with room is within 64 blocks of the corral (optional: Tim 2026-11-19, question 8), a sick one to a hospital pen
 * by the vet kit. While held, the animal walks back to the pen whenever it strays, and no housing takes it in.
 */
public final class IsolationHold {
	/** Quarantine: "a minimum of 30 days in a separate pen" (Idaho State Dept. of Agriculture). */
	public static final double QUARANTINE_DAYS = 30;
	/** How far a corral (or a sick animal) looks for a pen (proposed). */
	public static final double FIND_RADIUS = 64;

	private static final Map<Level, Set<BlockPos>> PENS = new WeakHashMap<>();

	private IsolationHold() {
	}

	/** A loaded isolation controller says where it is (refreshed every 5 s). */
	static synchronized void track(Level level, BlockPos pos) {
		PENS.computeIfAbsent(level, k -> new HashSet<>()).add(pos.immutable());
	}

	private static synchronized @Nullable IsolationController find(ServerLevel level, BlockPos near, HealthHost h, IsolationController.Mode mode) {
		Set<BlockPos> set = PENS.get(level);
		if (set == null) {
			return null;
		}
		IsolationController best = null;
		double bestD = Double.MAX_VALUE;
		for (BlockPos p : new HashSet<>(set)) {
			if (!(level.getBlockEntity(p) instanceof IsolationController c)) {
				set.remove(p);
				continue;
			}
			double d = p.distSqr(near);
			if (d <= FIND_RADIUS * FIND_RADIUS && d < bestD && c.room(level, h, mode)) {
				best = c;
				bestD = d;
			}
		}
		return best;
	}

	/** After taming: to quarantine if a pen with room is near the corral. Returns true if sent. */
	public static boolean sendToQuarantine(ServerLevel level, HealthHost h, BlockPos corral) {
		IsolationController c = find(level, corral, h, IsolationController.Mode.QUARANTINE);
		if (c == null) {
			return false;
		}
		long now = level.getGameTime();
		h.leaveHousing();
		h.health().setPen(c.getBlockPos().asLong());
		h.health().setQuarantineUntil(now + Health.scale(h).realDaysToTicks(QUARANTINE_DAYS));
		h.health().event(now, "into quarantine for " + (int) QUARANTINE_DAYS + " real days");
		return true;
	}

	/** The vet kit's isolate: to the nearest hospital pen with room. Returns a message. */
	public static String sendToHospital(ServerLevel level, HealthHost h) {
		IsolationController c = find(level, h.animal().blockPosition(), h, IsolationController.Mode.HOSPITAL);
		if (c == null) {
			return "No hospital pen with room within " + (int) FIND_RADIUS + " blocks (an isolation pen switched to HOSPITAL)";
		}
		long now = level.getGameTime();
		h.leaveHousing();
		h.health().setPen(c.getBlockPos().asLong());
		h.health().event(now, "into the sick pen");
		return h.herdId() + " goes to the sick pen at " + c.getBlockPos().toShortString();
	}

	/** Each second: a held animal strays back to its pen; a pen that is gone lets it go. */
	static void hold(ServerLevel level, HealthHost h, long now) {
		long pen = h.health().pen();
		if (pen == Long.MIN_VALUE) {
			return;
		}
		BlockPos at = BlockPos.of(pen);
		if (!level.isLoaded(at)) {
			return;
		}
		if (!(level.getBlockEntity(at) instanceof IsolationController c)) {
			h.health().setPen(Long.MIN_VALUE);
			return;
		}
		if (!c.formed() || c.check() == null || !(h.animal() instanceof PathfinderMob mob)) {
			return;
		}
		Animal a = h.animal();
		IsolationLayout.Pos best = null;
		double bestD = Double.MAX_VALUE;
		boolean inside = false;
		for (IsolationLayout.Pos p : c.check().pen()) {
			double dx = a.getX() - (p.x() + 0.5);
			double dz = a.getZ() - (p.z() + 0.5);
			double d = dx * dx + dz * dz;
			if (d < 0.6) {
				inside = true;
			}
			if (d < bestD) {
				bestD = d;
				best = p;
			}
		}
		if (!inside && best != null && Math.floorMod(now + a.getId(), 40L) < 20) {
			// a pen cell chosen by the animal's id, so a group spreads over the pen
			var cells = c.check().pen();
			IsolationLayout.Pos home = cells.get(Math.floorMod(a.getId(), cells.size()));
			mob.getNavigation().moveTo(home.x() + 0.5, home.y(), home.z() + 0.5, 1.0);
		}
	}
}

package dev.farmings.racing;

import dev.farmings.entity.HorseEntity;
import dev.farmings.husbandry.Equine;
import dev.farmings.husbandry.racing.TrackShape;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The start gate and the races run from it (docs/multiblocks/race-track.md). It finds the track it stands beside, its loop and the gate's
 * stalls (the track blocks straight across from the gate), and the finish post. Use starts a race with the horses standing in the
 * stalls: ridden ones run as their riders ride; tame unridden ones are run by an AI jockey along their lane at the pace {@link
 * dev.farmings.husbandry.Gallop} gives for the distance they have covered. Each horse's progress is measured along the track's centre
 * line; at the race distance its time is taken to the tick and its place given, and the horse's records are kept.
 */
public class StartGateEntity extends BlockEntity {
	/** Race distances (race-track.md): 2, 6, 8, 10 and 12 furlongs. */
	public static final int[] DISTANCES = {402, 1207, 1609, 2012, 2414};
	/** The count before the gate opens: three seconds. */
	public static final int COUNTDOWN_TICKS = 60;
	/** A horse off the track this long is out *(proposed)*. */
	public static final int OFF_TRACK_TICKS = 40;
	/** A race ends when the last horse is slower than this, m/s *(proposed: a slow trot)*. */
	private static final double SLOWEST_MPS = 4;
	/** The most track blocks a gate looks at. */
	private static final int MAX_CELLS = 16_384;
	/** A track must be this wide at the gate (race-track.md, proposed). */
	public static final int MIN_WIDTH = 3;

	public enum State { IDLE, COUNTDOWN, RUNNING }

	/** One horse in the race. */
	static final class Entrant {
		final UUID horse;
		final boolean ai;
		final double offset;
		double s;
		double covered;
		int offTicks;
		int finishTicks = -1;
		int place;
		boolean out;
		String name = "";

		Entrant(UUID horse, boolean ai, double s, double offset) {
			this.horse = horse;
			this.ai = ai;
			this.s = s;
			this.offset = offset;
		}
	}

	private int distanceIndex;
	private State state = State.IDLE;
	private long goTick;
	private long startTick;
	private int raceMetres;
	private final List<Entrant> entrants = new ArrayList<>();
	private final List<String> lastResult = new ArrayList<>();
	// the track, found by refresh
	private @Nullable TrackShape shape;
	private final List<BlockPos> stalls = new ArrayList<>();
	private final Set<BlockPos> posts = new HashSet<>();
	private final Set<Long> trackCells = new HashSet<>();
	private int trackY;
	private String problem = "not checked yet";
	private long refreshedAt = Long.MIN_VALUE;

	public StartGateEntity(BlockPos pos, BlockState state) {
		super(RaceBlocks.START_GATE_ENTITY, pos, state);
	}

	public int distance() {
		return DISTANCES[Math.floorMod(distanceIndex, DISTANCES.length)];
	}

	public void setDistanceForTest(int metres) {
		for (int i = 0; i < DISTANCES.length; i++) {
			if (DISTANCES[i] == metres) {
				distanceIndex = i;
			}
		}
	}

	public String nextDistance() {
		if (state != State.IDLE) {
			return "A race is on: the distance is set at " + raceMetres + " m";
		}
		distanceIndex = Math.floorMod(distanceIndex + 1, DISTANCES.length);
		setChanged();
		return String.format(Locale.ROOT, "Start gate: races over %,d m (%s)", distance(), laps(distance()));
	}

	private String laps(int metres) {
		if (shape == null || !shape.formed()) {
			return "track not formed";
		}
		return String.format(Locale.ROOT, "%.2f laps of %.0f m", metres / shape.lap(), shape.lap());
	}

	public State state() {
		return state;
	}

	public @Nullable TrackShape shape() {
		return shape;
	}

	public List<BlockPos> stalls() {
		return stalls;
	}

	public Set<BlockPos> posts() {
		return posts;
	}

	public boolean formed() {
		return problem.isEmpty();
	}

	public String problem() {
		return problem;
	}

	// ---- the track ----

	public void refresh(ServerLevel level) {
		long now = level.getGameTime();
		if (now == refreshedAt) {
			return;
		}
		refreshedAt = now;
		shape = null;
		stalls.clear();
		posts.clear();
		trackCells.clear();
		BlockPos start = null;
		Direction across = null;
		for (int dy : new int[] {0, -1}) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos p = worldPosition.relative(d).above(dy);
				if (level.getBlockState(p).is(RaceBlocks.RACE_TRACK)) {
					start = p;
					across = d;
					break;
				}
			}
			if (start != null) {
				break;
			}
		}
		if (start == null) {
			problem = "The start gate must stand beside the track";
			return;
		}
		trackY = start.getY();
		List<int[]> cells = new ArrayList<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		trackCells.add(BlockPos.asLong(start.getX(), 0, start.getZ()));
		while (!queue.isEmpty() && cells.size() < MAX_CELLS) {
			BlockPos p = queue.poll();
			cells.add(new int[] {p.getX(), p.getZ()});
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos q = p.relative(d);
				long k = BlockPos.asLong(q.getX(), 0, q.getZ());
				if (trackCells.contains(k)) {
					continue;
				}
				if (level.getBlockState(q).is(RaceBlocks.RACE_TRACK)) {
					trackCells.add(k);
					queue.add(q);
				} else {
					for (int dy = 0; dy <= 1; dy++) {
						if (level.getBlockState(q.above(dy)).is(RaceBlocks.FINISH_POST)) {
							posts.add(q.above(dy));
						}
					}
				}
			}
		}
		for (BlockPos p = start; level.getBlockState(p).is(RaceBlocks.RACE_TRACK); p = p.relative(across)) {
			stalls.add(p);
		}
		shape = TrackShape.of(cells);
		if (!shape.formed()) {
			problem = shape.problem();
		} else if (stalls.size() < MIN_WIDTH) {
			problem = "The track must be at least " + MIN_WIDTH + " blocks wide at the gate, is " + stalls.size();
		} else if (posts.isEmpty()) {
			problem = "No finish post beside the track";
		} else {
			problem = "";
		}
	}

	private boolean onTrack(Entity e) {
		BlockPos b = e.blockPosition();
		for (int dy = 1; dy <= 2; dy++) {
			if (b.getY() - dy == trackY && trackCells.contains(BlockPos.asLong(b.getX(), 0, b.getZ()))) {
				return true;
			}
		}
		return false;
	}

	// ---- a race ----

	/** Starts a race with the horses in the stalls; what to tell the player. */
	public String start(ServerLevel level) {
		if (state != State.IDLE) {
			return results();
		}
		refresh(level);
		if (!formed()) {
			return "Start gate: " + problem;
		}
		entrants.clear();
		Set<UUID> seen = new HashSet<>();
		for (BlockPos stall : stalls) {
			AABB box = new AABB(stall.getX(), stall.getY() + 1, stall.getZ(), stall.getX() + 1, stall.getY() + 3, stall.getZ() + 1);
			for (HorseEntity h : level.getEntitiesOfClass(HorseEntity.class, box, h -> !h.isWild() && h.ageRealDays() >= Equine.RIDING_AGE_DAYS)) {
				if (!seen.add(h.getUUID())) {
					continue;
				}
				double cx = stall.getX() + 0.5;
				double cz = stall.getZ() + 0.5;
				boolean ai = !h.isVehicle();
				Entrant e = new Entrant(h.getUUID(), ai, shape.project(cx, cz), shape.offset(cx, cz));
				e.name = h.raceName();
				if (ai) {
					h.holdForRace();
					h.snapTo(cx, stall.getY() + 1, cz, yaw(e.s, e.offset, cx, cz), 0);
					h.setYHeadRot(h.getYRot());
					h.setYBodyRot(h.getYRot());
				}
				entrants.add(e);
				break;
			}
		}
		if (entrants.isEmpty()) {
			return "Start gate: no horse in the stalls (a tame horse of 2 years or more, ridden or led in)";
		}
		raceMetres = distance();
		state = State.COUNTDOWN;
		goTick = level.getGameTime() + COUNTDOWN_TICKS;
		setChanged();
		return String.format(Locale.ROOT, "Start gate: %d runner%s over %,d m (%s). Gate opens in 3 s.", entrants.size(), entrants.size() == 1 ? "" : "s",
				raceMetres, laps(raceMetres));
	}

	/** Facing along the lane from (x, z). */
	private float yaw(double s, double offset, double x, double z) {
		double[] t = shape.pointAt(s + 3, offset);
		return (float) Math.toDegrees(Math.atan2(-(t[0] - x), t[1] - z));
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (state == State.IDLE) {
			return;
		}
		if (state == State.COUNTDOWN) {
			long left = goTick - now;
			if (left % 20 == 0 && left > 0) {
				announce(level, (left / 20) + "...");
			}
			if (now >= goTick) {
				state = State.RUNNING;
				startTick = now;
				announce(level, "And they're off!");
			}
			return;
		}
		boolean anyRunning = false;
		int finished = (int) entrants.stream().filter(e -> e.finishTicks >= 0).count();
		for (Entrant e : entrants) {
			if (e.finishTicks >= 0 || e.out) {
				continue;
			}
			HorseEntity h = level.getEntity(e.horse) instanceof HorseEntity x && x.isAlive() ? x : null;
			if (h == null) {
				e.out = true;
				continue;
			}
			if (e.ai) {
				drive(h, e);
			}
			double s = shape.project(h.getX(), h.getZ());
			double ds = shape.delta(e.s, s);
			e.s = s;
			e.covered += ds;
			e.offTicks = onTrack(h) ? 0 : e.offTicks + 1;
			if (e.offTicks > OFF_TRACK_TICKS) {
				e.out = true;
				release(h, e);
				continue;
			}
			if (e.covered >= raceMetres) {
				e.finishTicks = (int) (now - startTick);
				e.place = ++finished;
				h.recordRace(raceMetres, e.finishTicks, e.place == 1 && entrants.size() > 1);
				release(h, e);
				continue;
			}
			anyRunning = true;
		}
		long limit = (long) Math.ceil(raceMetres / SLOWEST_MPS * 20);
		if (!anyRunning || now - startTick > limit) {
			finish(level);
		}
	}

	/** The AI jockey: one tick along its lane at the pace for the distance covered. */
	private void drive(HorseEntity h, Entrant e) {
		double v = h.runner().speedAt(Math.max(0, e.covered));
		double step = v / 20;
		double[] t = shape.pointAt(e.s + 3 + step, e.offset);
		double dx = t[0] - h.getX();
		double dz = t[1] - h.getZ();
		double len = Math.hypot(dx, dz);
		if (len < 1e-6) {
			return;
		}
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		h.setYRot(yaw);
		h.setYHeadRot(yaw);
		h.setYBodyRot(yaw);
		h.move(MoverType.SELF, new Vec3(dx / len * step, h.onGround() ? -0.01 : -0.3, dz / len * step));
	}

	private void release(HorseEntity h, Entrant e) {
		if (e.ai) {
			h.releaseFromRace();
		}
	}

	private void finish(ServerLevel level) {
		lastResult.clear();
		lastResult.add(String.format(Locale.ROOT, "Last race: %,d m, %d runner%s", raceMetres, entrants.size(), entrants.size() == 1 ? "" : "s"));
		List<Entrant> order = new ArrayList<>(entrants);
		order.sort((a, b) -> a.finishTicks < 0 || b.finishTicks < 0 ? Boolean.compare(a.finishTicks < 0, b.finishTicks < 0) : Integer.compare(a.place, b.place));
		for (Entrant e : order) {
			if (e.finishTicks >= 0) {
				lastResult.add(String.format(Locale.ROOT, "%d. %s  %.2f s%s", e.place, e.name, e.finishTicks / 20.0, e.ai ? "" : " (ridden)"));
			} else {
				lastResult.add("-. " + e.name + "  did not finish");
				if (level.getEntity(e.horse) instanceof HorseEntity h) {
					release(h, e);
				}
			}
		}
		announce(level, lastResult.size() > 1 ? lastResult.get(1) : "No finishers");
		entrants.clear();
		state = State.IDLE;
		setChanged();
	}

	private void announce(ServerLevel level, String text) {
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 256 * 256) {
				p.sendOverlayMessage(Component.literal(text));
			}
		}
	}

	/** The last race's result, or the race in hand. */
	public String results() {
		StringBuilder b = new StringBuilder();
		if (state != State.IDLE) {
			b.append(String.format(Locale.ROOT, "Racing over %,d m:", raceMetres));
			for (Entrant e : entrants) {
				b.append(String.format(Locale.ROOT, "\n  %s %.0f m%s", e.name, Math.max(0, e.covered),
						e.finishTicks >= 0 ? String.format(Locale.ROOT, ", finished %d. in %.2f s", e.place, e.finishTicks / 20.0) : e.out ? ", out" : ""));
			}
			return b.toString();
		}
		if (lastResult.isEmpty()) {
			return "No race run yet";
		}
		return String.join("\n", lastResult);
	}

	public List<String> lastResult() {
		return lastResult;
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putInt("Distance", distanceIndex);
		out.putString("LastResult", String.join("\n", lastResult));
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		distanceIndex = in.getIntOr("Distance", 0);
		lastResult.clear();
		String r = in.getStringOr("LastResult", "");
		if (!r.isEmpty()) {
			lastResult.addAll(List.of(r.split("\n")));
		}
	}
}

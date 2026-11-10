package dev.farmings.corral;

import dev.farmings.barn.BarnPartBlock;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.TroughEntity;
import dev.farmings.entity.Wild;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.WildBreeds;
import dev.farmings.husbandry.corral.CorralLayout;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Runs a taming corral (docs/taming.md): checks the layout, draws wild animals in with the bait, feeds and waters the wild animals
 * inside, and tames each one after {@link WildBreeds#SETTLE_DAYS} real days fed and watered.
 */
public class CorralPostEntity extends BlockEntity {
	/** How far the bait draws wild animals (proposed). */
	public static final double LURE_RADIUS = 32;
	private static final int TEND_TICKS = 20;

	private CorralLayout.@Nullable Corral check;
	private final Set<Long> floor = new HashSet<>();
	private final List<BlockPos> baits = new ArrayList<>();
	private final List<BlockPos> troughs = new ArrayList<>();
	private @Nullable BlockPos baitSpot;
	private @Nullable UUID owner;
	private int tamed;

	public CorralPostEntity(BlockPos pos, BlockState state) {
		super(CorralBlocks.CORRAL_POST_ENTITY, pos, state);
	}

	public CorralLayout.@Nullable Corral check() {
		return check;
	}

	public void claim(UUID player) {
		if (owner == null) {
			owner = player;
			setChanged();
		}
	}

	public @Nullable UUID owner() {
		return owner;
	}

	// ---- the layout ----

	public void refresh(ServerLevel level) {
		check = CorralLayout.check(ground(level), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		floor.clear();
		baits.clear();
		troughs.clear();
		baitSpot = null;
		for (CorralLayout.Pos p : check.floor()) {
			floor.add(BlockPos.asLong(p.x(), p.y(), p.z()));
		}
		for (CorralLayout.Pos p : check.baits()) {
			baits.add(new BlockPos(p.x(), p.y(), p.z()));
		}
		for (CorralLayout.Pos p : check.troughs()) {
			BlockPos o = BigParts.owner(level, new BlockPos(p.x(), p.y(), p.z()));
			if (!troughs.contains(o)) {
				troughs.add(o);
			}
		}
		// where lured animals walk to: the floor cell beside the first bait trough
		for (BlockPos b : baits) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos n = b.relative(d);
				if (floor.contains(n.asLong())) {
					baitSpot = n;
					break;
				}
			}
			if (baitSpot != null) {
				break;
			}
		}
	}

	private CorralLayout.Ground ground(ServerLevel level) {
		return new CorralLayout.Ground() {
			@Override
			public CorralLayout.@Nullable Pos at(int x, int z, int nearY) {
				for (int dy : new int[] {0, 1, -1}) {
					BlockPos feet = new BlockPos(x, nearY + dy, z);
					if (standable(level, feet)) {
						return new CorralLayout.Pos(x, feet.getY(), z);
					}
				}
				return null;
			}

			@Override
			public CorralLayout.Part part(int x, int y, int z) {
				BlockState s = level.getBlockState(new BlockPos(x, y, z));
				if (s.getBlock() instanceof CorralGateBlock) {
					return CorralLayout.Part.GATE;
				}
				if (s.getBlock() == CorralBlocks.BAIT_TROUGH) {
					return CorralLayout.Part.BAIT;
				}
				if (s.getBlock() instanceof BarnPartBlock part && part.kind() == BarnParts.WATER_TROUGH) {
					return CorralLayout.Part.TROUGH;
				}
				return CorralLayout.Part.NONE;
			}
		};
	}

	private static boolean standable(ServerLevel level, BlockPos feet) {
		BlockState f = level.getBlockState(feet);
		BlockState h = level.getBlockState(feet.above());
		BlockState g = level.getBlockState(feet.below());
		if (!f.getFluidState().isEmpty() || !h.getFluidState().isEmpty() || !g.getFluidState().isEmpty()) {
			return false;
		}
		if (f.getBlock() == CorralBlocks.CORRAL_POST) {
			return false;
		}
		return f.getCollisionShape(level, feet).isEmpty() && h.getCollisionShape(level, feet.above()).isEmpty() && g.isFaceSturdy(level, feet.below(), Direction.UP);
	}

	public boolean inside(Mob m) {
		return floor.contains(m.blockPosition().asLong());
	}

	// ---- running ----

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (check == null || now % 100 == 0) {
			refresh(level);
		}
		if (now % TEND_TICKS != 0 || check == null || !check.formed()) {
			return;
		}
		boolean bait = baitKg(level) > 0;
		for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(worldPosition).inflate(LURE_RADIUS), e -> e instanceof Wild.Animal a && a.isWild())) {
			Wild.Animal a = (Wild.Animal) m;
			if (inside(m)) {
				a.setLure(null);
				tend(level, m, a, TEND_TICKS);
			} else {
				a.setLure(bait ? baitSpot : null);
			}
		}
	}

	private double baitKg(ServerLevel level) {
		double kg = 0;
		for (BlockPos b : baits) {
			if (level.getBlockEntity(b) instanceof BaitTroughEntity t) {
				kg += t.dryMatterKg();
			}
		}
		return kg;
	}

	/** Feeds and waters one wild animal for {@code ticks}; a fully fed and watered step counts toward settling. */
	public void tend(ServerLevel level, Mob m, Wild.Animal a, int ticks) {
		if (!a.isWild()) {
			return;
		}
		double share = (double) ticks / TimeScale.TICKS_PER_GAME_DAY;     // one game day is one real day's intake (the barn rule)
		double wantKg = a.dailyDryMatterKg() * share;
		double wantL = a.dailyWaterL() * share;
		double eaten = 0;
		for (BlockPos b : baits) {
			if (eaten < wantKg && level.getBlockEntity(b) instanceof BaitTroughEntity t) {
				eaten += t.take(wantKg - eaten);
			}
		}
		double drunk = 0;
		for (BlockPos p : troughs) {
			if (drunk < wantL && BigParts.entity(level, p) instanceof TroughEntity t) {
				drunk += t.take(wantL - drunk);
			}
		}
		if (eaten >= wantKg * 0.999 && drunk >= wantL * 0.999) {
			a.setSettledDays(a.settledDays() + TimeScale.ticksToRealDays(ticks));
			if (a.settledDays() >= WildBreeds.SETTLE_DAYS) {
				a.tame(owner);
				tamed++;
				setChanged();
			}
		}
	}

	public String readout(ServerLevel level) {
		if (check == null) {
			return "Corral: not checked yet";
		}
		if (!check.formed()) {
			return "Corral: " + check.problem();
		}
		int wild = 0;
		double most = 0;
		for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(worldPosition).inflate(LURE_RADIUS), e -> e instanceof Wild.Animal a && a.isWild())) {
			if (inside(m)) {
				wild++;
				most = Math.max(most, ((Wild.Animal) m).settledDays());
			}
		}
		double water = 0;
		for (BlockPos p : troughs) {
			if (BigParts.entity(level, p) instanceof TroughEntity t) {
				water += t.litres();
			}
		}
		return String.format(java.util.Locale.ROOT, "Corral: FORMED, %d cells, %d gate(s); %d wild inside (most settled %.1f of %.0f days); bait %.0f kg hay, water %.0f L; %d tamed",
				check.floor().size(), check.gates().size(), wild, most, WildBreeds.SETTLE_DAYS, baitKg(level), water, tamed);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (owner != null) {
			output.putString("Owner", owner.toString());
		}
		output.putInt("Tamed", tamed);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String own = input.getStringOr("Owner", "");
		owner = own.isEmpty() ? null : UUID.fromString(own);
		tamed = input.getIntOr("Tamed", 0);
	}
}

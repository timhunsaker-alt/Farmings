package dev.farmings.health;

import dev.farmings.barn.BarnPartBlock;
import dev.farmings.barn.BarnParts;
import dev.farmings.barn.FeederEntity;
import dev.farmings.barn.TroughEntity;
import dev.farmings.genetics.Species;
import dev.farmings.husbandry.health.IsolationLayout;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The isolation pen's controller (docs/multiblocks/isolation-pen.md): checks the pen, holds its members (quarantine or hospital), feeds and
 * waters them from the feeders and troughs beside it, and lets them go when their time is done.
 */
public class IsolationController extends BlockEntity {
	public enum Mode { QUARANTINE, HOSPITAL }

	/** The housing blocks an isolation pen must keep 8 blocks from. */
	private static final Set<String> HOUSING = Set.of("barn_controller", "stall_bed", "shed_controller", "sheep_pen", "lambing_pen", "coop_controller",
			"coop_floor", "pig_barn_controller", "slatted_floor", "farrowing_crate", "stable_controller", "horse_stall");

	private Mode mode = Mode.QUARANTINE;
	private IsolationLayout.@Nullable Check check;

	public IsolationController(BlockPos pos, BlockState state) {
		super(HealthBlocks.ISOLATION_ENTITY, pos, state);
	}

	public Mode mode() {
		return mode;
	}

	public void setMode(Mode m) {
		mode = m;
		setChanged();
	}

	public boolean formed() {
		return check != null && check.formed();
	}

	public IsolationLayout.@Nullable Check check() {
		return check;
	}

	private static IsolationLayout.Cell cell(ServerLevel level, BlockPos p) {
		BlockState s = level.getBlockState(p);
		Block b = s.getBlock();
		if (b == HealthBlocks.ISOLATION_PEN) {
			return IsolationLayout.Cell.PEN;
		}
		if (b == HealthBlocks.ISOLATION_CONTROLLER) {
			return IsolationLayout.Cell.CONTROLLER;
		}
		if (b instanceof BarnPartBlock part) {
			if (part.kind() == BarnParts.HAY_FEEDER) {
				return IsolationLayout.Cell.FEEDER;
			}
			if (part.kind() == BarnParts.WATER_TROUGH) {
				return IsolationLayout.Cell.TROUGH;
			}
		}
		var key = BuiltInRegistries.BLOCK.getKey(b);
		if (key.getNamespace().equals(dev.farmings.Farmings.MOD_ID)) {
			if (key.getPath().equals("grain_feeder")) {
				return IsolationLayout.Cell.FEEDER;
			}
			if (HOUSING.contains(key.getPath())) {
				return IsolationLayout.Cell.HOUSING;
			}
		}
		return IsolationLayout.Cell.OTHER;
	}

	public void refresh(ServerLevel level) {
		check = IsolationLayout.check((x, y, z) -> cell(level, new BlockPos(x, y, z)), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		if (formed()) {   // on each check while it stands, as Multiblock.formed does: a player who comes later still gets it
			dev.wildcatter.quest.QuestEvents.milestone(level, worldPosition, dev.farmings.quest.Milestones.ISOLATION_PEN);
		}
		IsolationHold.track(level, worldPosition);
	}

	public void serverTick(ServerLevel level) {
		long t = level.getGameTime() + worldPosition.hashCode();
		if (check == null || Math.floorMod(t, 100L) == 0) {
			refresh(level);
		}
		if (Math.floorMod(t, 20L) == 1) {
			tend(level, 20);
		}
	}

	/** The animals this pen holds. */
	public List<HealthHost> members(ServerLevel level) {
		List<HealthHost> out = new ArrayList<>();
		for (Animal a : level.getEntitiesOfClass(Animal.class, new AABB(worldPosition).inflate(Health.HOUSING_REACH), e -> e instanceof HealthHost && e.isAlive())) {
			HealthHost h = (HealthHost) a;
			if (h.health().pen() == worldPosition.asLong()) {
				out.add(h);
			}
		}
		return out;
	}

	/** Pen cells an animal of this species takes: 3 for a cow or a horse, 1.5 for a sheep or goat, 1 for a pig, a third for a bird. */
	public static double cells(Species s) {
		return switch (s) {
			case CATTLE, HORSE -> 3;
			case SHEEP, GOAT -> 1.5;
			case PIG -> 1;
			default -> 1.0 / 3;
		};
	}

	/** Whether this pen, in {@code want} mode, has room for {@code h}: one species at a time. */
	public boolean room(ServerLevel level, HealthHost h, Mode want) {
		if (!formed() || mode != want) {
			return false;
		}
		double used = 0;
		for (HealthHost m : members(level)) {
			if (m.species() != h.species()) {
				return false;
			}
			used += cells(m.species());
		}
		return used + cells(h.species()) <= check.pen().size() + 1e-9;
	}

	/** Feeds and waters the members, as the corral does its wild animals; lets go those whose time is done. */
	void tend(ServerLevel level, int ticks) {
		if (!formed()) {
			return;
		}
		long now = level.getGameTime();
		for (HealthHost h : members(level)) {
			double share = h.timeScale().ticksToRealDays(ticks);
			double wantKg = h.dailyDryMatterKg() * share;
			double wantL = h.dailyWaterL() * share;
			for (IsolationLayout.Pos p : check.feeders()) {
				if (wantKg <= 0) {
					break;
				}
				BlockPos bp = new BlockPos(p.x(), p.y(), p.z());
				if (BigParts.entity(level, bp) instanceof FeederEntity f) {
					wantKg -= f.take(wantKg);
				} else if (level.getBlockEntity(bp) instanceof dev.farmings.coop.GrainFeederEntity g) {
					wantKg -= g.take(wantKg);
				}
			}
			for (IsolationLayout.Pos p : check.troughs()) {
				if (wantL <= 0) {
					break;
				}
				if (BigParts.entity(level, new BlockPos(p.x(), p.y(), p.z())) instanceof TroughEntity tr) {
					wantL -= tr.take(wantL);
				}
			}
			var r = h.health();
			boolean done = mode == Mode.QUARANTINE ? !r.inQuarantine(now) && !r.sick() && r.notifiable() == null : !r.sick() && !anyActive(r);
			if (done) {
				r.setPen(Long.MIN_VALUE);
				r.setQuarantineUntil(-1);
				r.event(now, mode == Mode.QUARANTINE ? "released from quarantine" : "left the sick pen, recovered");
			}
		}
	}

	private static boolean anyActive(dev.farmings.husbandry.health.HealthRecord r) {
		for (var c : r.cases()) {
			if (c.stage == dev.farmings.husbandry.health.HealthRecord.Stage.SICK || c.stage == dev.farmings.husbandry.health.HealthRecord.Stage.INCUBATING
					|| c.stage == dev.farmings.husbandry.health.HealthRecord.Stage.CARRIER) {
				return true;
			}
		}
		return false;
	}

	public String readout(ServerLevel level) {
		if (check == null) {
			refresh(level);
		}
		if (!check.formed()) {
			return "Isolation pen: " + check.problem();
		}
		List<HealthHost> m = members(level);
		StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "Isolation pen (%s): FORMED, %d cells, %d animal(s)", mode, check.pen().size(), m.size()));
		long now = level.getGameTime();
		for (HealthHost h : m) {
			b.append("; ").append(h.herdId());
			if (h.health().inQuarantine(now)) {
				b.append(String.format(Locale.ROOT, " out in %.2f game days", (h.health().quarantineUntil() - now) / 24000.0));
			} else if (h.health().sick()) {
				b.append(" sick");
			}
		}
		return b.toString();
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putString("mode", mode.name());
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		try {
			mode = Mode.valueOf(in.getStringOr("mode", Mode.QUARANTINE.name()));
		} catch (IllegalArgumentException e) {
			mode = Mode.QUARANTINE;
		}
	}

	/** The controller block: right-click reads it; sneak + right-click switches mode while it holds no animal. */
	public static class ControllerBlock extends BaseEntityBlock {
		public ControllerBlock(Properties p) {
			super(p);
		}

		@Override
		public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new IsolationController(pos, state);
		}

		@Override
		public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
			return HealthBlocks.isolationTicker(level, type);
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
			if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IsolationController c) {
				ServerLevel server = (ServerLevel) level;
				c.refresh(server);
				if (player.isShiftKeyDown()) {
					if (c.members(server).isEmpty()) {
						c.setMode(c.mode == Mode.QUARANTINE ? Mode.HOSPITAL : Mode.QUARANTINE);
						player.sendSystemMessage(Component.literal("Isolation pen switched to " + c.mode));
					} else {
						player.sendSystemMessage(Component.literal("The pen must be empty to switch mode"));
					}
				} else {
					player.sendSystemMessage(Component.literal(c.readout(server)));
				}
			}
			return InteractionResult.SUCCESS;
		}
	}
}

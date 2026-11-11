package dev.farmings.hatchery;

import dev.farmings.barn.TroughEntity;
import dev.farmings.coop.GrainFeederEntity;
import dev.farmings.entity.FowlEntity;
import dev.farmings.husbandry.TimeScale;
import dev.farmings.husbandry.poultry.Fowl;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The brooder (docs/multiblocks/hatchery.md): a heat lamp that keeps up to 25 young birds within 2 blocks warm while powered. Young birds
 * under it that have no coop eat and drink from a grain feeder and water trough within 4 blocks.
 */
public class BrooderEntity extends BlockEntity implements CircuitPart, Inspectable {
	/** Power drawn (proposed: a common heat-lamp bulb rating, not looked up). */
	public static final double WATTS = 250;
	public static final int MAX_BIRDS = 25;
	public static final double REACH = 2.0;
	public static final int FEED_REACH = 4;
	private final Motor lamp = new Motor(false, Voltage.LOW, WATTS);
	private boolean forced;
	private int under;

	public BrooderEntity(BlockPos pos, BlockState state) {
		super(HatcheryBlocks.BROODER_ENTITY, pos, state);
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || lamp.output() > 0;
	}

	/** The young birds it reaches: a 5 x 5 area at its height and one below. */
	public List<FowlEntity> young(ServerLevel level) {
		AABB box = new AABB(worldPosition).inflate(REACH, 0, REACH).expandTowards(0, -1, 0);
		List<FowlEntity> l = new ArrayList<>(level.getEntitiesOfClass(FowlEntity.class, box, FowlEntity::young));
		return l.size() > MAX_BIRDS ? l.subList(0, MAX_BIRDS) : l;
	}

	public void serverTick(ServerLevel level) {
		if (level.getGameTime() % 20 == 0) {
			step(level, 20);
		}
	}

	public void step(ServerLevel level, long ticks) {
		List<FowlEntity> birds = young(level);
		under = birds.size();
		if (!powered()) {
			return;
		}
		long now = level.getGameTime();
		double dayShare = (double) ticks / TimeScale.TICKS_PER_GAME_DAY;
		List<GrainFeederEntity> feeders = new ArrayList<>();
		List<TroughEntity> troughs = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-FEED_REACH, -2, -FEED_REACH), worldPosition.offset(FEED_REACH, 0, FEED_REACH))) {
			if (level.getBlockEntity(p) instanceof GrainFeederEntity g) {
				feeders.add(g);
			} else if (level.getBlockEntity(BigParts.owner(level, p)) instanceof TroughEntity t && !troughs.contains(t)) {
				troughs.add(t);
			}
		}
		for (FowlEntity f : birds) {
			f.keepWarm(now + ticks + 20);
			if (f.coop() != null) {
				continue;     // a coop feeds its own young
			}
			if (f.home() == null) {
				f.assignCoop(null, worldPosition.below(worldPosition.getY() - (int) Math.floor(f.getY())));     // stays under the lamp
			}
			double need = Fowl.feedKgPerDay(f.species(), f.weightKg()) * dayShare;
			double got = 0;
			for (GrainFeederEntity g : feeders) {
				got += g.take(need - got);
			}
			double water = Fowl.waterLPerDay(f.species(), f.weightKg()) * dayShare;
			double drunk = 0;
			for (TroughEntity t : troughs) {
				drunk += t.take(water - drunk);
			}
			f.recordTending(need <= 0 ? 1 : Math.min(1, got / need), water <= 0 ? 1 : Math.min(1, drunk / water));
		}
	}

	public String readout() {
		return String.format(Locale.ROOT, "Brooder (%.2f kW): %s, %d young bird%s under it (room for %d); 95 F the first week, 5 F less each week",
				WATTS / 1000, powered() ? "on" : "NO POWER", under, under == 1 ? "" : "s", MAX_BIRDS);
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.LOW;
	}

	@Override
	public void build(Circuit circuit) {
		lamp.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		lamp.apply(circuit, ticks, under > 0);
	}

	@Override
	public Component inspect() {
		return Component.literal(readout() + "; " + lamp.describe());
	}
}

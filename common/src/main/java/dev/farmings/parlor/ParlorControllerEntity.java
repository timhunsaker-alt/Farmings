package dev.farmings.parlor;

import dev.farmings.FarmingsBlocks;
import dev.farmings.entity.CattleEntity;
import dev.farmings.husbandry.Stage;
import dev.farmings.husbandry.parlor.BulkTank;
import dev.farmings.husbandry.parlor.ParlorCheck;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The parlor's controller (docs/multiblocks/parlor.md): checks the structure once a second and, while it is formed, the pump runs and the
 * tank has room and is cold enough, calls the lactating cows of its barn (the barn its cow lane reaches) along the lane to the holding pen,
 * sends each waiting cow to a free stall, milks her there into the bulk tank, and sends her back along the lane. A fault (NO LANE TO BARN,
 * NO POWER, TANK FULL, TANK WARM) stops milking; the cows are not harmed and keep their milk.
 */
public class ParlorControllerEntity extends BlockEntity {
	public static final int CHECK_TICKS = 20;
	/** How far around the parlor its barn's cows are looked for: a 64-cell lane plus a 50-cell barn. */
	public static final int CALL_RADIUS = 128;
	private static final double AT_STAND_SQ = 0.6 * 0.6;

	private ParlorCheck check;
	private String fault = "";
	private dev.farmings.husbandry.lane.LanePath.Link link;

	public ParlorControllerEntity(BlockPos pos, BlockState state) {
		super(FarmingsBlocks.PARLOR_CONTROLLER_ENTITY, pos, state);
	}

	public void refresh(Level level) {
		check = ParlorWorld.check(level, worldPosition);
		link = check.formed() ? dev.farmings.husbandry.lane.LanePath.find(ParlorWorld.laneGrid(level), stallCells(), check.stalls()) : null;
	}

	/** The pen, lane and barn this parlor reaches, or null before it forms. */
	public dev.farmings.husbandry.lane.LanePath.Link link() {
		return link;
	}

	public boolean linked() {
		return link != null && link.linked();
	}

	/** The barn controller this parlor milks for, or null. */
	public BlockPos barn() {
		return linked() ? pos(link.barnController()) : null;
	}

	private List<dev.farmings.husbandry.lane.LanePath.Pos> stallCells() {
		List<dev.farmings.husbandry.lane.LanePath.Pos> out = new ArrayList<>();
		for (int i = 0; i < check.stalls(); i++) {
			for (int az = 1; az <= 2; az++) {
				BlockPos p = at(check.pumpX() + 1 + i, floorY(), az);
				out.add(new dev.farmings.husbandry.lane.LanePath.Pos(p.getX(), p.getY(), p.getZ()));
			}
		}
		return out;
	}

	private static BlockPos pos(dev.farmings.husbandry.lane.LanePath.Pos p) {
		return new BlockPos(p.x(), p.y(), p.z());
	}

	/** The lane from the barn's alley to the pen, as block positions; reversed, the way home. */
	private List<BlockPos> route(boolean home) {
		List<BlockPos> out = new ArrayList<>();
		for (dev.farmings.husbandry.lane.LanePath.Pos p : link.route()) {
			out.add(pos(p));
		}
		if (home) {
			java.util.Collections.reverse(out);
		}
		return out;
	}

	/** Whether she stands in the holding pen: within a block of a pen cell's centre (the pathfinder stops short of centres for a cow). */
	private boolean inPen(CattleEntity c) {
		for (dev.farmings.husbandry.lane.LanePath.Pos p : link.pen()) {
			double dx = c.getX() - (p.x() + 0.5);
			double dz = c.getZ() - (p.z() + 0.5);
			if (dx * dx + dz * dz < 1.0 && Math.abs(c.getY() - p.y()) <= 1.5) {
				return true;
			}
		}
		return false;
	}

	public ParlorCheck check() {
		return check;
	}

	public boolean formed() {
		return check != null && check.formed();
	}

	public String fault() {
		return fault;
	}

	// ---- positions: drawn with the controller at 0, the tank towards +x, the stalls towards +z ----

	private BlockPos at(int ax, int y, int az) {
		int[] w = check.orientation().world(ax, az);
		return new BlockPos(worldPosition.getX() + w[0], y, worldPosition.getZ() + w[1]);
	}

	private int floorY() {
		return worldPosition.getY() - 1;
	}

	public PumpEntity pump(Level level) {
		return BigParts.entity(level, at(check.pumpX(), floorY(), 0)) instanceof PumpEntity p ? p : null;
	}

	public TankEntity tank(Level level) {
		return BigParts.entity(level, at(1, floorY(), 0)) instanceof TankEntity t ? t : null;
	}

	/** Where a cow stands in stall {@code i} (0-based): the stall's first cell, on its low top. */
	public BlockPos standPos(int i) {
		return at(check.pumpX() + 1 + i, floorY(), 1);
	}

	// ---- running ----

	public void serverTick(Level level) {
		long t = level.getGameTime() + worldPosition.hashCode();
		if (check == null || Math.floorMod(t, (long) CHECK_TICKS) == 0) {
			refresh(level);
		}
		if (!formed() || Math.floorMod(t, (long) CHECK_TICKS) != 1) {
			return;
		}
		milk(level, CHECK_TICKS);
	}

	/** One round of milking for {@code ticks} of time. Run once a second; tests call it directly. */
	public void milk(Level level, int ticks) {
		if (!formed()) {
			return;
		}
		PumpEntity pump = pump(level);
		TankEntity tankEntity = tank(level);
		if (pump == null || tankEntity == null) {
			fault = "NO PUMP OR TANK";
			return;
		}
		BulkTank tank = tankEntity.tank();
		tankEntity.step(ticks);
		if (!linked()) {
			fault = "NO LANE TO BARN" + (link == null ? "" : ": " + link.problem());
			pump.setDemand(false);
			return;
		}
		List<CattleEntity> herd = herd(level);
		boolean work = false;
		for (CattleEntity c : herd) {
			work |= c.milkKg() > 0 || standIndex(c) >= 0;
		}
		pump.setDemand(work);
		fault = !pump.powered() ? (work ? "NO POWER" : "") : tank.room() <= 0 ? "TANK FULL" : tank.blocksMilking() ? "TANK WARM" : "";
		boolean ready = pump.powered() && tank.room() > 0 && !tank.blocksMilking();
		int n = check.stalls();
		boolean[] taken = new boolean[n];
		for (CattleEntity c : herd) {
			int i = standIndex(c);
			if (i < 0) {
				continue;
			}
			if (!ready || c.milkKg() <= 0) {
				c.setMilkingStand(null);
				c.walkRoute(route(true), null, true);
				continue;
			}
			taken[i] = true;
			BlockPos p = standPos(i);
			double dx = c.getX() - (p.getX() + 0.5);
			double dz = c.getZ() - (p.getZ() + 0.5);
			if (dx * dx + dz * dz <= AT_STAND_SQ) {
				double litres = c.milkKg() / BulkTank.KG_PER_L;
				if (tank.room() >= litres) {
					double fat = c.milkFatPct();
					double protein = c.milkProteinPct();
					c.takeMilk();
					tank.add(litres, BulkTank.MILK_IN_C, fat, protein);
					c.setMilkingStand(null);
					c.walkRoute(route(true), null, true);
					tankEntity.setChanged();
				}
			}
		}
		if (!ready) {
			// nothing to wait for: the cows called to the pen go home
			for (CattleEntity c : herd) {
				if (c.penSpot() != null) {
					c.walkRoute(route(true), null, true);
				}
			}
			return;
		}
		// call cows along the lane to the pen, as many as it holds at 15 sq ft a cow
		List<dev.farmings.husbandry.lane.LanePath.Pos> pen = link.pen();
		int called = 0;
		for (CattleEntity c : herd) {
			if (c.penSpot() != null) {
				called++;
			}
		}
		for (CattleEntity c : herd) {
			if (called >= link.penCapacity()) {
				break;
			}
			if (standIndex(c) < 0 && c.penSpot() == null && c.stage() == Stage.LACTATING_COW && c.milkKg() > 0) {
				c.walkRoute(route(false), pos(pen.get(called % pen.size())), false);
				called++;
			}
		}
		// a free stall takes the nearest cow that has reached the pen
		List<CattleEntity> waiting = new ArrayList<>();
		for (CattleEntity c : herd) {
			if (standIndex(c) < 0 && c.penSpot() != null && inPen(c)) {
				waiting.add(c);
			}
		}
		for (int i = 0; i < n && !waiting.isEmpty(); i++) {
			if (taken[i]) {
				continue;
			}
			BlockPos p = standPos(i);
			CattleEntity best = null;
			double bestD = Double.MAX_VALUE;
			for (CattleEntity c : waiting) {
				double d = c.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
				if (d < bestD) {
					bestD = d;
					best = c;
				}
			}
			best.setMilkingStand(p);
			waiting.remove(best);
		}
		setChanged();
	}

	/** Which of this parlor's stalls the cow has been sent to, or -1. */
	private int standIndex(CattleEntity c) {
		BlockPos s = c.milkingStand();
		if (s == null) {
			return -1;
		}
		for (int i = 0; i < check.stalls(); i++) {
			if (standPos(i).equals(s)) {
				return i;
			}
		}
		return -1;
	}

	/** The cows housed in the barn this parlor's lane reaches. */
	private List<CattleEntity> herd(Level level) {
		BlockPos barn = barn();
		AABB box = new AABB(worldPosition).inflate(CALL_RADIUS, 8, CALL_RADIUS);
		return level.getEntitiesOfClass(CattleEntity.class, box, c -> !c.isBaby() && c.isAlive() && barn.equals(c.stallController()));
	}

	public String readout(Level level) {
		if (check == null) {
			return "Parlor: not checked yet";
		}
		if (!check.formed()) {
			return "Parlor: " + check.status();
		}
		TankEntity tank = tank(level);
		String milk = tank == null ? "" : String.format(", tank %.0f L at %.1f C", tank.tank().litres(), tank.tank().tempC());
		String lane = linked() ? ", linked to the barn at " + barn().toShortString() + " (pen holds " + link.penCapacity() + ")" : "";
		return "Parlor: FORMED, " + check.stalls() + " stalls" + milk + lane + (fault.isEmpty() ? ", ready" : ", " + fault);
	}
}

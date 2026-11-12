package dev.farmings.shearing;

import dev.farmings.FarmingsItems;
import dev.farmings.entity.SheepEntity;
import dev.farmings.genetics.SheepLook;
import dev.farmings.husbandry.Flock;
import dev.farmings.husbandry.shearing.ShearingLayout;
import dev.farmings.item.FleeceItem;
import dev.farmings.shed.ShedBlocks;
import dev.farmings.shed.ShedControllerEntity;
import dev.farmings.shed.ShedFloorBlock;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The shearing stand (docs/multiblocks/shearing-shed.md): the shed's shearing machine on Wildcatter power and the shearing shed's
 * controller. While powered it calls in one sheep at a time from its sheep shed whose fleece is ready, shears her at the stand, sends the
 * fleece and skirtings to the wool table, and lets her go home. A wool press beside the table bales eight fleeces of one grade and colour.
 */
public class ShearingStandEntity extends BlockEntity implements CircuitPart, Inspectable {
	/** No shearing within this many real days of lambing (Perennia, Preparing for lambing; sourcing pass 2026-11-12). */
	public static final double SHEAR_BEFORE_LAMBING_DAYS = 28;

	private final Motor motor = new Motor(false, Voltage.LOW, ShearingLayout.MOTOR_WATTS);
	private ShearingLayout.@Nullable Check check;
	private @Nullable BlockPos shed;
	private @Nullable UUID current;
	private int progress;
	private int shorn;
	private boolean forced;
	private String fault = "";

	public ShearingStandEntity(BlockPos pos, BlockState state) {
		super(ShearingBlocks.STAND_ENTITY, pos, state);
	}

	public ShearingLayout.@Nullable Check check() {
		return check;
	}

	public @Nullable BlockPos shed() {
		return shed;
	}

	public int shorn() {
		return shorn;
	}

	public void forceRunning(boolean on) {
		forced = on;
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	private static BlockPos pos(ShearingLayout.Pos p) {
		return new BlockPos(p.x(), p.y(), p.z());
	}

	private static ShearingLayout.Cell cell(ServerLevel level, BlockPos p) {
		Block b = level.getBlockState(p).getBlock();
		if (b == ShearingBlocks.SHEARING_STAND) {
			return ShearingLayout.Cell.STAND;
		}
		if (b == ShearingBlocks.CATCHING_PEN) {
			return ShearingLayout.Cell.CATCHING_PEN;
		}
		if (b == ShedBlocks.SHEEP_PEN || b == ShedBlocks.LAMBING_PEN) {
			return ShearingLayout.Cell.SHED_FLOOR;
		}
		if (b == ShearingBlocks.WOOL_TABLE) {
			return ShearingLayout.Cell.WOOL_TABLE;
		}
		if (b == ShearingBlocks.WOOL_PRESS) {
			return ShearingLayout.Cell.WOOL_PRESS;
		}
		return ShearingLayout.Cell.OTHER;
	}

	public void refresh(ServerLevel level) {
		check = ShearingLayout.check((x, y, z) -> cell(level, new BlockPos(x, y, z)), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		shed = check.formed() ? findShed(level, pos(check.shedFloor())) : null;
	}

	private static @Nullable BlockPos findShed(ServerLevel level, BlockPos floor) {
		return dev.farmings.shed.ShedWorld.findShed(level, floor);
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (check == null || now % 100 == 0) {
			refresh(level);
			press(level);
		}
		if (now % 10 != 0) {
			return;
		}
		if (!check.formed()) {
			fault = check.status();
			release(level);
			return;
		}
		if (shed == null || !(level.getBlockEntity(shed) instanceof ShedControllerEntity controller)) {
			fault = "NO SHEEP SHED";
			release(level);
			return;
		}
		if (!powered()) {
			fault = "NO POWER";
			release(level);
			return;
		}
		fault = "";
		BlockPos point = pos(check.standPoint());
		SheepEntity sheep = current == null ? null : find(level, controller, current);
		if (sheep == null || !sheep.fleeceReady()) {
			if (sheep != null) {
				sheep.reserve(null);
				sheep.sendTo(null);
			}
			current = null;
			progress = 0;
			for (SheepEntity s : controller.members(level)) {
				if (s.fleeceReady() && s.reservedBy() == null && !(s.pregnant() && s.daysToLambing() < SHEAR_BEFORE_LAMBING_DAYS) && s.lambsNursing() == 0) {
					current = s.getUUID();
					s.reserve(worldPosition);
					s.sendTo(point);
					break;
				}
			}
			return;
		}
		sheep.sendTo(point);
		double d2 = sheep.distanceToSqr(point.getX() + 0.5, point.getY(), point.getZ() + 0.5);
		if (d2 < 4.0) {
			// the shearer catches her in the catching pen and drags her onto the board, as shearers do
			if (d2 > 0.3) {
				sheep.snapTo(point.getX() + 0.5, point.getY() + 0.125, point.getZ() + 0.5, sheep.getYRot(), sheep.getXRot());
				sheep.getNavigation().stop();
			}
			progress += 10;
			if (progress >= ShearingLayout.SHEAR_TICKS) {
				shearAt(level, sheep);
			}
		}
	}

	private static @Nullable SheepEntity find(ServerLevel level, ShedControllerEntity controller, UUID id) {
		for (SheepEntity s : controller.members(level)) {
			if (s.getUUID().equals(id)) {
				return s;
			}
		}
		return null;
	}

	private void shearAt(ServerLevel level, SheepEntity sheep) {
		SheepEntity.Fleece f = sheep.shear();
		sheep.reserve(null);
		sheep.sendTo(null);
		current = null;
		progress = 0;
		if (f == null) {
			return;
		}
		shorn++;
		level.playSound(null, worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
		double skirt = f.greasyKg() * ShearingLayout.SKIRTING_SHARE;
		ItemStack body = FleeceItem.make(FarmingsItems.FLEECE, f.greasyKg() - skirt, f.micron(), f.stapleCm(), f.cleanYield(), f.grade(), f.colour());
		ItemStack skirtings = FleeceItem.make(FarmingsItems.SKIRTINGS, skirt, f.micron(), f.stapleCm(), f.cleanYield(), f.grade().lower(), f.colour());
		BlockPos table = pos(check.table());
		for (ItemStack s : List.of(body, skirtings)) {
			ItemStack rest = level.getBlockEntity(table) instanceof WoolStoreEntity t ? t.insert(s) : s;
			if (!rest.isEmpty()) {
				net.minecraft.world.Containers.dropItemStack(level, table.getX() + 0.5, table.getY() + 1, table.getZ() + 0.5, rest);
			}
		}
		setChanged();
	}

	/** Lets the called sheep go when the stand cannot work. */
	private void release(ServerLevel level) {
		if (current != null && shed != null && level.getBlockEntity(shed) instanceof ShedControllerEntity controller) {
			SheepEntity s = find(level, controller, current);
			if (s != null) {
				s.reserve(null);
				s.sendTo(null);
			}
		}
		current = null;
		progress = 0;
	}

	/** The press bales every eight fleeces of one grade and colour on the table. */
	public void press(ServerLevel level) {
		if (check == null || !check.formed() || check.press() == null) {
			return;
		}
		if (!(level.getBlockEntity(pos(check.table())) instanceof WoolStoreEntity table) || !(level.getBlockEntity(pos(check.press())) instanceof WoolStoreEntity press)) {
			return;
		}
		for (Flock.Grade g : Flock.Grade.values()) {
			for (SheepLook.Wool c : SheepLook.Wool.values()) {
				List<ItemStack> match = new ArrayList<>();
				for (ItemStack s : table.items()) {
					if (s.is(FarmingsItems.FLEECE) && FleeceItem.grade(s) == g && FleeceItem.colour(s) == c) {
						match.add(s);
					}
				}
				int n = match.stream().mapToInt(ItemStack::getCount).sum();
				if (n < ShearingLayout.BALE_FLEECES) {
					continue;
				}
				double kg = 0;
				double micronKg = 0;
				double stapleKg = 0;
				double yieldKg = 0;
				int left = ShearingLayout.BALE_FLEECES;
				for (ItemStack s : match) {
					while (left > 0 && !s.isEmpty()) {
						double w = FleeceItem.greasyKg(s);
						kg += w;
						micronKg += w * FleeceItem.micron(s);
						stapleKg += w * FleeceItem.stapleCm(s);
						yieldKg += w * FleeceItem.cleanYield(s);
						s.shrink(1);
						left--;
					}
				}
				table.setChanged();
				ItemStack bale = FleeceItem.make(FarmingsItems.WOOL_BALE, kg, micronKg / kg, stapleKg / kg, yieldKg / kg, g, c);
				ItemStack rest = press.insert(bale);
				if (!rest.isEmpty()) {
					net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, rest);
				}
			}
		}
	}

	public String readout() {
		if (check == null) {
			return "Shearing stand: not checked yet";
		}
		if (!check.formed()) {
			return "Shearing stand: " + check.status();
		}
		StringBuilder b = new StringBuilder("Shearing stand: ").append(fault.isEmpty() ? "RUNNING" : fault);
		b.append(shed == null ? ", no sheep shed" : ", serving the shed at " + shed.toShortString());
		b.append(String.format(Locale.ROOT, ", %d shorn", shorn));
		if (current != null) {
			b.append(String.format(Locale.ROOT, ", shearing (%d%%)", progress * 100 / ShearingLayout.SHEAR_TICKS));
		}
		b.append(check.press() == null ? ", no wool press" : ", wool press");
		return b.toString();
	}

	// ---- Wildcatter ----

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.LOW;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, current != null);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format(Locale.ROOT, "Shearing machine (%.2f kW): %s", ShearingLayout.MOTOR_WATTS / 1000.0, motor.describe()));
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putInt("Shorn", shorn);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		shorn = in.getIntOr("Shorn", 0);
	}
}

package dev.farmings.health;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.farmings.Farmings;
import dev.farmings.husbandry.health.Disease;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Health state that belongs to the world, not one animal (docs/animal-health.md): which pastures are infective with foot rot (by the shed
 * whose flock grazes them), and the notifiable outbreaks and standstills.
 */
public final class HealthWorld extends SavedData {
	/** The standstill reaches every housing within 192 blocks of the outbreak (proposed: 12 chunks; the real 3 km does not fit a farm). */
	public static final int ZONE_BLOCKS = 192;
	/** A standstill lifts 28 real days after the last case recovers (proposed). */
	public static final double LIFT_DAYS = 28;

	/** An outbreak: where, which disease, whether it was reported, and when a case was last active. */
	public static final class Zone {
		public final BlockPos center;
		public final Disease disease;
		public boolean reported;
		public long lastActive;

		Zone(BlockPos center, Disease disease, boolean reported, long lastActive) {
			this.center = center;
			this.disease = disease;
			this.reported = reported;
			this.lastActive = lastActive;
		}

		public boolean covers(BlockPos p) {
			return Math.abs(p.getX() - center.getX()) <= ZONE_BLOCKS && Math.abs(p.getZ() - center.getZ()) <= ZONE_BLOCKS;
		}
	}

	private record SavedZone(long pos, String disease, boolean reported, long lastActive) {
	}

	private record Saved(Map<String, Long> pastures, List<SavedZone> zones) {
	}

	private static final Codec<SavedZone> ZONE = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("pos").forGetter(SavedZone::pos), Codec.STRING.fieldOf("disease").forGetter(SavedZone::disease),
			Codec.BOOL.fieldOf("reported").forGetter(SavedZone::reported), Codec.LONG.fieldOf("last").forGetter(SavedZone::lastActive)).apply(i, SavedZone::new));
	private static final Codec<Saved> SAVED = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("pastures").forGetter(Saved::pastures),
			ZONE.listOf().fieldOf("zones").forGetter(Saved::zones)).apply(i, Saved::new));
	public static final Codec<HealthWorld> CODEC = SAVED.xmap(HealthWorld::new, HealthWorld::save);
	public static final SavedDataType<HealthWorld> TYPE = new SavedDataType<>(Farmings.id("health"), HealthWorld::new, CODEC, DataFixTypes.LEVEL);

	private final Map<Long, Long> pastures = new HashMap<>();
	private final List<Zone> zones = new ArrayList<>();

	public HealthWorld() {
	}

	private HealthWorld(Saved s) {
		s.pastures().forEach((k, v) -> pastures.put(Long.parseLong(k), v));
		for (SavedZone z : s.zones()) {
			Disease d = Disease.byName(z.disease());
			if (d != null) {
				zones.add(new Zone(BlockPos.of(z.pos()), d, z.reported(), z.lastActive()));
			}
		}
	}

	private Saved save() {
		Map<String, Long> p = new HashMap<>();
		pastures.forEach((k, v) -> p.put(Long.toString(k), v));
		List<SavedZone> z = new ArrayList<>();
		for (Zone zone : zones) {
			z.add(new SavedZone(zone.center.asLong(), zone.disease.name(), zone.reported, zone.lastActive));
		}
		return new Saved(p, z);
	}

	public static HealthWorld get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	// ---- foot rot on pasture ----

	public boolean pastureInfective(@Nullable BlockPos housing, long now) {
		return housing != null && pastures.getOrDefault(housing.asLong(), -1L) > now;
	}

	public void infectPasture(BlockPos housing, long until) {
		pastures.merge(housing.asLong(), until, Math::max);
		setDirty();
	}

	// ---- notifiable outbreaks ----

	public List<Zone> zones() {
		return zones;
	}

	/** The zone around {@code pos} for {@code d}, or null. */
	public @Nullable Zone zoneAt(BlockPos pos, Disease d) {
		for (Zone z : zones) {
			if (z.disease == d && z.covers(pos)) {
				return z;
			}
		}
		return null;
	}

	/** A case of a notifiable disease is active at {@code pos}: keeps or starts its zone. */
	public Zone caseActive(BlockPos pos, Disease d, long now, boolean reported) {
		Zone z = zoneAt(pos, d);
		if (z == null) {
			z = new Zone(pos.immutable(), d, reported, now);
			zones.add(z);
		}
		z.lastActive = now;
		z.reported |= reported;
		setDirty();
		return z;
	}

	/** Reported standstills covering {@code pos}. */
	public boolean standstill(BlockPos pos) {
		for (Zone z : zones) {
			if (z.reported && z.covers(pos)) {
				return true;
			}
		}
		return false;
	}

	/** An unreported outbreak of {@code d} within the zone distance: it spreads beyond its own housing. */
	public boolean unreportedNear(BlockPos pos, Disease d) {
		for (Zone z : zones) {
			if (!z.reported && z.disease == d && z.covers(pos)) {
				return true;
			}
		}
		return false;
	}

	/** Drops zones whose last case is over {@link #LIFT_DAYS} old. */
	public void lift(long now) {
		boolean changed = zones.removeIf(z -> now - z.lastActive > scale(z.disease).realDaysToTicks(LIFT_DAYS));
		pastures.values().removeIf(until -> until < now);
		if (changed) {
			setDirty();
		}
	}

	static dev.farmings.husbandry.TimeScale.Scale scale(Disease d) {
		return d == Disease.HPAI ? dev.farmings.husbandry.TimeScale.FOWL : dev.farmings.husbandry.TimeScale.LIVESTOCK;
	}

	/** Game tests: forget every zone and pasture. */
	public void clearForTest() {
		zones.clear();
		pastures.clear();
		setDirty();
	}
}

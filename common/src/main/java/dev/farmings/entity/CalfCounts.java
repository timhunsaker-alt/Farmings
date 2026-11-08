package dev.farmings.entity;

import com.mojang.serialization.Codec;
import dev.farmings.Farmings;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** How many calves each player's cattle have borne, for the tutorial sex rule (first two heifers, third a bull). Stored with the overworld. */
public class CalfCounts extends SavedData {
	public static final Codec<CalfCounts> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(CalfCounts::new, c -> c.counts);
	public static final SavedDataType<CalfCounts> TYPE = new SavedDataType<>(Farmings.id("calf_counts"), CalfCounts::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Integer> counts;

	private CalfCounts() {
		this.counts = new HashMap<>();
	}

	private CalfCounts(Map<String, Integer> saved) {
		this.counts = new HashMap<>(saved);
	}

	public static CalfCounts get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public int count(UUID player) {
		return counts.getOrDefault(player.toString(), 0);
	}

	public void add(UUID player) {
		counts.merge(player.toString(), 1, Integer::sum);
		setDirty();
	}
}

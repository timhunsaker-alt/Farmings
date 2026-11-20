package dev.farmings.health;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.farmings.Farmings;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * Farmings' world rules (docs/animal-health.md, "World rules"), saved with the world and shown on Wildcatter's World Rules screen as
 * Tailings' are. A new world starts with each rule's new-world value; a world saved before (one with Farmings data already in it) starts
 * with every rule off, so updating never makes an existing herd sick.
 * <ul>
 * <li>{@code disease}: the endemic diseases (on in a new world);</li>
 * <li>{@code notifiable_disease}: foot-and-mouth, HPAI and African swine fever, and standstills (off);</li>
 * <li>{@code disease_deaths}: an untreated case may kill at the disease's case fatality (off; Tim 2026-11-19).</li>
 * </ul>
 */
public final class FarmingsRules extends SavedData {
	public static final String DISEASE = "disease";
	public static final String NOTIFIABLE = "notifiable_disease";
	public static final String DEATHS = "disease_deaths";
	public static final Map<String, Boolean> NEW_WORLD;

	static {
		Map<String, Boolean> m = new LinkedHashMap<>();
		m.put(DISEASE, true);
		m.put(NOTIFIABLE, false);
		m.put(DEATHS, false);
		NEW_WORLD = java.util.Collections.unmodifiableMap(m);
	}

	/** Game test harnesses set this so their worlds start as an existing world would (every rule off); the health tests switch rules on. */
	public static volatile boolean testWorldsAsExisting;

	public static String meaning(String key) {
		return switch (key) {
			case DISEASE -> "animals catch the endemic diseases (mastitis, BRD, scours, foot rot and the rest)";
			case NOTIFIABLE -> "foot-and-mouth, avian influenza and African swine fever, with standstills when reported";
			case DEATHS -> "an untreated case of disease may kill the animal";
			default -> "";
		};
	}

	private record Saved(boolean newWorld, Map<String, Boolean> rules) {
	}

	private static final Codec<Saved> SAVED = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("new_world", false).forGetter(Saved::newWorld),
			Codec.unboundedMap(Codec.STRING, Codec.BOOL).fieldOf("rules").forGetter(Saved::rules)).apply(i, Saved::new));
	public static final Codec<FarmingsRules> CODEC = SAVED.xmap(FarmingsRules::new, r -> new Saved(r.newWorld, Map.copyOf(r.rules)));
	public static final SavedDataType<FarmingsRules> TYPE = new SavedDataType<>(Farmings.id("rules"), FarmingsRules::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Boolean> rules = new LinkedHashMap<>();
	private boolean newWorld;

	public FarmingsRules() {
	}

	private FarmingsRules(Saved s) {
		newWorld = s.newWorld();
		rules.putAll(s.rules());
	}

	public static void registerNewWorldSetup() {
		NEW_WORLD.forEach((k, v) -> dev.wildcatter.NewWorldSetup.register(Farmings.MOD_ID, k, meaning(k), v));
	}

	public static boolean on(@Nullable MinecraftServer server, String key) {
		return server != null && get(server).rules.getOrDefault(key, false);
	}

	public static void set(MinecraftServer server, String key, boolean value) {
		FarmingsRules r = get(server);
		r.rules.put(key, value);
		r.setDirty();
	}

	public static Map<String, Boolean> all(MinecraftServer server) {
		FarmingsRules r = get(server);
		Map<String, Boolean> out = new LinkedHashMap<>();
		NEW_WORLD.keySet().forEach(k -> out.put(k, r.rules.getOrDefault(k, false)));
		return out;
	}

	public static dev.farmings.husbandry.health.HealthModel.Rules model(@Nullable MinecraftServer server) {
		return new dev.farmings.husbandry.health.HealthModel.Rules(on(server, DISEASE), on(server, NOTIFIABLE), on(server, DEATHS));
	}

	public static synchronized FarmingsRules get(MinecraftServer server) {
		FarmingsRules r = server.overworld().getDataStorage().get(TYPE);
		if (r == null) {
			r = new FarmingsRules();
			boolean existing = testWorldsAsExisting || Files.isDirectory(server.getWorldPath(LevelResource.DATA).resolve(Farmings.MOD_ID))
					|| server.overworld().getGameTime() > 24_000L * 3;
			r.newWorld = !existing;
			for (var e : NEW_WORLD.entrySet()) {
				r.rules.put(e.getKey(), r.newWorld && dev.wildcatter.NewWorldSetup.rule(Farmings.MOD_ID, e.getKey(), e.getValue()));
			}
			server.overworld().getDataStorage().set(TYPE, r);
			r.setDirty();
			Farmings.LOGGER.info("Farmings: world rules made for {} world: {}", r.newWorld ? "a new" : "an existing", r.rules);
		}
		return r;
	}
}

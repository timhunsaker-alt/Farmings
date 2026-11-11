package dev.farmings.genetics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * An animal's inherited genes: breed composition (basis points) and one allele pair per core trait. Sex, age and ids are the
 * animal's, not the genome's, so they are not here. The canonical form and hash follow FarmChain's quantitative genome
 * (sorted keys, sorted allele pairs, integers only), so the same genes always give the same string and hash.
 * Visual and discrete loci come later (docs/GENETICS.md).
 */
public record Genome(String schema, int generation, Map<String, Integer> breedCompositionBp, Map<Trait, AllelePair> coreGenome, String rulesetVersion) {
	public static final String SCHEMA_V1 = "farmings.cattle.genome.v1";
	public static final String SCHEMA_SHEEP_V1 = "farmings.sheep.genome.v1";

	private static boolean known(String schema) {
		for (Species s : Species.values()) {
			if (s.schema.equals(schema)) {
				return true;
			}
		}
		return false;
	}

	/** The species this genome's schema names. */
	public Species species() {
		return Species.ofSchema(schema);
	}

	public Genome {
		breedCompositionBp = Collections.unmodifiableMap(new TreeMap<>(breedCompositionBp));
		coreGenome = Collections.unmodifiableMap(new EnumMap<>(coreGenome));
	}

	/** The canonical string: sorted keys, sorted allele pairs, no instance state. */
	public String canonical() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"schema\":").append(quote(schema));
		sb.append(",\"breed_composition_bp\":{");
		boolean first = true;
		for (Map.Entry<String, Integer> e : new TreeMap<>(breedCompositionBp).entrySet()) {
			sb.append(first ? "" : ",").append(quote(e.getKey())).append(':').append(e.getValue());
			first = false;
		}
		sb.append("},\"core_genome\":{");
		Map<String, AllelePair> byId = new TreeMap<>();
		for (Map.Entry<Trait, AllelePair> e : coreGenome.entrySet()) {
			byId.put(e.getKey().id, e.getValue().sorted());
		}
		first = true;
		for (Map.Entry<String, AllelePair> e : byId.entrySet()) {
			sb.append(first ? "" : ",").append(quote(e.getKey())).append(":[").append(quote(e.getValue().first())).append(',').append(quote(e.getValue().second())).append(']');
			first = false;
		}
		sb.append("},\"ruleset_version\":").append(quote(rulesetVersion)).append('}');
		return sb.toString();
	}

	/** Lowercase SHA-256 hex of the canonical string: a stable fingerprint for tests and for the breeding seed. */
	public String hash() {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical().getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * A compact single-line form for saving on an animal: {@code schema;generation;ruleset;HOL:5000,JER:5000;trait=A,B|trait=A,B}.
	 * Unlike {@link #canonical()} it keeps the allele order (which parent gave which), so a saved animal comes back exactly as it was.
	 */
	public String encode() {
		StringBuilder sb = new StringBuilder();
		sb.append(schema).append(';').append(generation).append(';').append(rulesetVersion).append(';');
		boolean first = true;
		for (Map.Entry<String, Integer> e : breedCompositionBp.entrySet()) {
			sb.append(first ? "" : ",").append(e.getKey()).append(':').append(e.getValue());
			first = false;
		}
		sb.append(';');
		first = true;
		for (Trait t : Trait.values()) {
			AllelePair p = coreGenome.get(t);
			if (p != null) {
				sb.append(first ? "" : "|").append(t.id).append('=').append(p.first()).append(',').append(p.second());
				first = false;
			}
		}
		return sb.toString();
	}

	/** Reads {@link #encode()} back. Throws {@link IllegalArgumentException} on text that is not a genome. */
	public static Genome decode(String text) {
		String[] parts = text.split(";", -1);
		if (parts.length != 5 || !known(parts[0])) {
			throw new IllegalArgumentException("not a Farmings genome: " + text);
		}
		try {
			Map<String, Integer> composition = new TreeMap<>();
			for (String item : parts[3].split(",")) {
				String[] kv = item.split(":");
				composition.put(kv[0], Integer.parseInt(kv[1]));
			}
			Map<Trait, AllelePair> core = new EnumMap<>(Trait.class);
			for (String item : parts[4].split("\\|")) {
				String[] kv = item.split("=");
				String[] alleles = kv[1].split(",");
				Trait trait = null;
				for (Trait t : Trait.values()) {
					if (t.id.equals(kv[0])) {
						trait = t;
					}
				}
				if (trait == null || alleles.length != 2) {
					throw new IllegalArgumentException("bad locus " + item);
				}
				core.put(trait, new AllelePair(alleles[0], alleles[1]));
			}
			return new Genome(parts[0], Integer.parseInt(parts[1]), composition, core, parts[2]);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("not a Farmings genome: " + text, e);
		}
	}

	public List<String> breedCodes() {
		return new ArrayList<>(breedCompositionBp.keySet());
	}

	private static String quote(String s) {
		StringBuilder sb = new StringBuilder("\"");
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"' || c == '\\') {
				sb.append('\\').append(c);
			} else if (c < 0x20) {
				sb.append(String.format("\\u%04x", (int) c));
			} else {
				sb.append(c);
			}
		}
		return sb.append('"').toString();
	}
}

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

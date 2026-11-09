package dev.farmings.genetics;

import java.util.EnumMap;
import java.util.Map;

/**
 * An animal's coat and body-shape genes (docs/GENETICS.md, "Coat genes"): one allele pair per {@link VisualLocus}, inherited one allele
 * from each parent. They are kept apart from the {@link Genome} so the core genome's canonical form, hash and breeding seed are
 * unchanged; inheritance draws from its own generator, seeded from the same breeding seed with a domain tag (as FarmChain's visual
 * resolver salts its seed with ":visual:v1", spec 022 D3).
 *
 * <p>The loci and their inheritance modes are FarmChain's (BREED-SPEC §7.2-7.3). The dominance order of the extension locus is the UC
 * Davis Veterinary Genetics Laboratory's (ED > EBR > E+ > e). Founder alleles per breed are in {@link #founder}; the ones without a
 * source are marked there.
 */
public record VisualGenome(Map<VisualLocus, int[]> alleles) {
	/** XORed into the breeding seed so coat draws never share numbers with the core genome's draws. */
	public static final long VISUAL_SALT = 0x76697375616C3176L;     // "visual1v"

	public VisualGenome {
		EnumMap<VisualLocus, int[]> copy = new EnumMap<>(VisualLocus.class);
		for (VisualLocus l : VisualLocus.values()) {
			int[] p = alleles.get(l);
			copy.put(l, p == null ? new int[] {0, 0} : new int[] {l.clamp(p[0]), l.clamp(p[1])});
		}
		alleles = java.util.Collections.unmodifiableMap(copy);
	}

	public int[] pair(VisualLocus l) {
		return alleles.get(l).clone();
	}

	/** What the pair shows. */
	public int expressed(VisualLocus l) {
		return l.express(alleles.get(l)[0], alleles.get(l)[1]);
	}

	// ---- inheritance ----

	/** A calf's coat genes: one allele from each parent at every locus, chosen by a generator seeded from {@code breedingSeed}. */
	public static VisualGenome inherit(VisualGenome dam, VisualGenome sire, long breedingSeed) {
		Rng rng = new Rng(breedingSeed ^ VISUAL_SALT);
		EnumMap<VisualLocus, int[]> out = new EnumMap<>(VisualLocus.class);
		for (VisualLocus l : VisualLocus.values()) {
			int a = dam.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			int b = sire.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			out.put(l, new int[] {a, b});
		}
		return new VisualGenome(out);
	}

	// ---- founders ----

	/** A founder of one breed: each allele drawn from that breed's alleles at each locus. */
	public static VisualGenome founder(String breedCode, long seed) {
		Rng rng = new Rng(seed ^ VISUAL_SALT);
		EnumMap<VisualLocus, int[]> out = new EnumMap<>(VisualLocus.class);
		for (VisualLocus l : VisualLocus.values()) {
			out.put(l, new int[] {draw(breedCode, l, rng), draw(breedCode, l, rng)});
		}
		return new VisualGenome(out);
	}

	/**
	 * Coat genes for an animal that has none recorded (saved before coat genes existed, or a straw's sire): each allele drawn from the
	 * founder alleles of a breed picked by the animal's breed shares, from a seed taken from its genome hash, so it is always the same.
	 */
	public static VisualGenome fallback(Genome genome) {
		long seed = Long.parseUnsignedLong(genome.hash().substring(0, 15), 16);
		Rng rng = new Rng(seed ^ VISUAL_SALT);
		EnumMap<VisualLocus, int[]> out = new EnumMap<>(VisualLocus.class);
		for (VisualLocus l : VisualLocus.values()) {
			out.put(l, new int[] {draw(pickBreed(genome.breedCompositionBp(), rng), l, rng), draw(pickBreed(genome.breedCompositionBp(), rng), l, rng)});
		}
		return new VisualGenome(out);
	}

	private static String pickBreed(Map<String, Integer> compositionBp, Rng rng) {
		int total = 0;
		for (int v : compositionBp.values()) {
			total += v;
		}
		double r = rng.next() * total;
		String last = "HOL";
		for (Map.Entry<String, Integer> e : new java.util.TreeMap<>(compositionBp).entrySet()) {
			last = e.getKey();
			r -= e.getValue();
			if (r < 0) {
				return last;
			}
		}
		return last;
	}

	/**
	 * One founder allele. Weighted lists are {value, weight, value, weight, ...}; ranges are drawn uniformly between two values.
	 * Sources and proposals per breed are listed in docs/GENETICS.md ("Coat genes").
	 */
	private static int draw(String breed, VisualLocus l, Rng rng) {
		int[] w = FounderAlleles.of(breed, l);
		if (l.range) {
			return w[0] + (int) Math.floor(rng.next() * (w[1] - w[0] + 1));
		}
		int total = 0;
		for (int i = 1; i < w.length; i += 2) {
			total += w[i];
		}
		double r = rng.next() * total;
		for (int i = 0; i < w.length; i += 2) {
			r -= w[i + 1];
			if (r < 0) {
				return w[i];
			}
		}
		return w[w.length - 2];
	}

	// ---- saving: a flat int array, two per locus in locus order ----

	public int[] save() {
		int[] out = new int[VisualLocus.values().length * 2];
		for (VisualLocus l : VisualLocus.values()) {
			out[l.ordinal() * 2] = alleles.get(l)[0];
			out[l.ordinal() * 2 + 1] = alleles.get(l)[1];
		}
		return out;
	}

	/** Reads {@link #save}; null if the array is not one (an older save). */
	public static VisualGenome load(int[] saved) {
		if (saved == null || saved.length != VisualLocus.values().length * 2) {
			return null;
		}
		EnumMap<VisualLocus, int[]> out = new EnumMap<>(VisualLocus.class);
		for (VisualLocus l : VisualLocus.values()) {
			out.put(l, new int[] {saved[l.ordinal() * 2], saved[l.ordinal() * 2 + 1]});
		}
		return new VisualGenome(out);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VisualGenome v && java.util.Arrays.equals(save(), v.save());
	}

	@Override
	public int hashCode() {
		return java.util.Arrays.hashCode(save());
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("VisualGenome{");
		for (VisualLocus l : VisualLocus.values()) {
			b.append(l.id).append('=').append(l.label(alleles.get(l)[0])).append('/').append(l.label(alleles.get(l)[1])).append(' ');
		}
		return b.append('}').toString();
	}
}

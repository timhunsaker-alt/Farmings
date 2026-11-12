package dev.farmings.genetics;

import java.util.EnumMap;
import java.util.Map;

/**
 * A bird's plumage, comb and shell genes: one allele pair per {@link PoultryLocus}, kept apart from the core {@link Genome} as the sheep
 * coat genes are. Drawn from the breeding seed with the same domain salt. At a sex-linked locus a daughter takes her one Z from her sire;
 * a son takes one Z from each parent.
 */
public record PoultryGenes(Map<PoultryLocus, int[]> alleles) {
	public PoultryGenes {
		EnumMap<PoultryLocus, int[]> copy = new EnumMap<>(PoultryLocus.class);
		for (PoultryLocus l : PoultryLocus.values()) {
			int[] p = alleles.get(l);
			copy.put(l, p == null ? new int[] {0, 0} : new int[] {l.clamp(p[0]), l.clamp(p[1])});
		}
		alleles = java.util.Collections.unmodifiableMap(copy);
	}

	public int[] pair(PoultryLocus l) {
		return alleles.get(l).clone();
	}

	public int expressed(PoultryLocus l, Sex sex) {
		return l.express(alleles.get(l)[0], alleles.get(l)[1], sex);
	}

	/** How many copies of allele 1 a bird carries at a 0/1 locus (an incomplete-dominance locus is read this way); a hen's Z counts once. */
	public int count(PoultryLocus l, Sex sex) {
		int[] a = alleles.get(l);
		return l.sexLinked && sex == Sex.FEMALE ? a[0] : a[0] + a[1];
	}

	public static PoultryGenes inherit(PoultryGenes dam, PoultryGenes sire, Sex child, long breedingSeed) {
		Rng rng = new Rng(breedingSeed ^ VisualGenome.VISUAL_SALT);
		EnumMap<PoultryLocus, int[]> out = new EnumMap<>(PoultryLocus.class);
		for (PoultryLocus l : PoultryLocus.values()) {
			int fromDam = dam.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			int fromSire = sire.alleles.get(l)[rng.next() < 0.5 ? 0 : 1];
			if (l.sexLinked) {
				// The dam's single Z is slot 0; a daughter gets her W instead, so she carries only the sire's Z.
				out.put(l, child == Sex.FEMALE ? new int[] {fromSire, 0} : new int[] {fromSire, dam.alleles.get(l)[0]});
			} else {
				out.put(l, new int[] {fromDam, fromSire});
			}
		}
		return new PoultryGenes(out);
	}

	public static PoultryGenes founder(String breedCode, Sex sex, long seed) {
		Rng rng = new Rng(seed ^ VisualGenome.VISUAL_SALT);
		EnumMap<PoultryLocus, int[]> out = new EnumMap<>(PoultryLocus.class);
		for (PoultryLocus l : PoultryLocus.values()) {
			int a = draw(breedCode, l, rng);
			int b = draw(breedCode, l, rng);
			out.put(l, l.sexLinked && sex == Sex.FEMALE ? new int[] {a, 0} : new int[] {a, b});
		}
		return new PoultryGenes(out);
	}

	private static int draw(String breed, PoultryLocus l, Rng rng) {
		int[] w = PoultryLocus.founder(breed, l);
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

	public int[] save() {
		int[] out = new int[PoultryLocus.values().length * 2];
		for (PoultryLocus l : PoultryLocus.values()) {
			out[l.ordinal() * 2] = alleles.get(l)[0];
			out[l.ordinal() * 2 + 1] = alleles.get(l)[1];
		}
		return out;
	}

	public static PoultryGenes load(int[] saved) {
		if (saved == null || saved.length < 2 || saved.length % 2 != 0 || saved.length > PoultryLocus.values().length * 2) {
			return null;
		}
		EnumMap<PoultryLocus, int[]> out = new EnumMap<>(PoultryLocus.class);
		for (PoultryLocus l : PoultryLocus.values()) {
			// a bird saved before later loci were added carries their wild type (0)
			int i = l.ordinal() * 2;
			out.put(l, i + 1 < saved.length ? new int[] {saved[i], saved[i + 1]} : new int[] {0, 0});
		}
		return new PoultryGenes(out);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof PoultryGenes g && java.util.Arrays.equals(save(), g.save());
	}

	@Override
	public int hashCode() {
		return java.util.Arrays.hashCode(save());
	}
}

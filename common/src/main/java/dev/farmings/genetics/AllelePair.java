package dev.farmings.genetics;

/** Two allele ids at one locus. Order records which parent gave which (dam first); identity ignores order. */
public record AllelePair(String first, String second) {
	/** The two ids in sorted order, so a genome's identity is the allele set, not the transmission order. */
	public AllelePair sorted() {
		return first.compareTo(second) <= 0 ? this : new AllelePair(second, first);
	}

	public boolean contains(String allele) {
		return first.equals(allele) || second.equals(allele);
	}
}

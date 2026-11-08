package dev.farmings.genetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Breed composition in basis points, always summing to exactly 10,000 (BREED-SPEC 7.1). */
public final class BreedComposition {
	public static final int TOTAL_BP = 10_000;

	private BreedComposition() {
	}

	public static Map<String, Integer> purebred(String code) {
		Map<String, Integer> m = new TreeMap<>();
		m.put(code, TOTAL_BP);
		return m;
	}

	/**
	 * A calf's composition is the average of its parents', rounded to exactly 10,000 by the largest-remainder method.
	 * Ties go to the breed that sorts first, so no breed is favoured.
	 */
	public static Map<String, Integer> average(Map<String, Integer> dam, Map<String, Integer> sire) {
		TreeSet<String> codeSet = new TreeSet<>();
		codeSet.addAll(dam.keySet());
		codeSet.addAll(sire.keySet());
		List<String> codes = new ArrayList<>(codeSet);
		int n = codes.size();
		int[] floor = new int[n];
		boolean[] half = new boolean[n]; // the exact average ends in .5
		int floorSum = 0;
		for (int i = 0; i < n; i++) {
			int total = dam.getOrDefault(codes.get(i), 0) + sire.getOrDefault(codes.get(i), 0);
			floor[i] = total / 2;
			half[i] = total % 2 != 0;
			floorSum += floor[i];
		}
		int remaining = TOTAL_BP - floorSum;
		Map<String, Integer> result = new TreeMap<>();
		for (int i = 0; i < n; i++) {
			result.put(codes.get(i), floor[i]);
		}
		// Largest remainder first (.5 before .0), then sorted code order. Same result as FarmChain's sort.
		for (int pass = 0; pass < 2 && remaining > 0; pass++) {
			boolean wantHalf = pass == 0;
			for (int i = 0; i < n && remaining > 0; i++) {
				if (half[i] == wantHalf) {
					result.merge(codes.get(i), 1, Integer::sum);
					remaining--;
				}
			}
		}
		return result;
	}

	public static boolean sumsToTotal(Map<String, Integer> bp) {
		int sum = 0;
		for (int v : bp.values()) {
			if (v < 0) {
				return false;
			}
			sum += v;
		}
		return sum == TOTAL_BP;
	}
}

package dev.farmings.genetics;

/**
 * Founder coat alleles per breed (docs/GENETICS.md, "Coat genes"). A weighted list is {value, weight, value, weight, ...}; a range locus is
 * {low, high}. Where a figure has no source it is marked *(proposed)* here and in the doc.
 *
 * <ul>
 *   <li>Holstein: black and white (ED), the Holstein EBR (Telstar) allele and red (e) present (UC Davis VGL, Holstein MC1R test; Red and
 *       White Holsteins exist); the weights are *(proposed)*. Patch pattern, white 450-900, face none to white face, horned or polled
 *       (FarmChain BREED-SPEC Appendix A, the Holstein entry).</li>
 *   <li>Angus: black, polled (breed standard; Red Angus is e/e and is a separate breed here, so e is rare *(proposed)*).</li>
 *   <li>Wagyu (Japanese Black): black, horned (breed standard).</li>
 *   <li>Jersey: fawn, E+ with light to medium dilution *(unverified: Jersey shades are wild-type with modifiers; no MC1R survey found)*.</li>
 *   <li>Brown Swiss: grey-brown, drawn as ED with medium dilution *(unverified)*.</li>
 *   <li>Gyr: red to speckled white, horned, with a hump, a loose dewlap and long ears (breed description); alleles *(proposed)*.</li>
 * </ul>
 */
final class FounderAlleles {
	private FounderAlleles() {
	}

	static int[] of(String breed, VisualLocus l) {
		return switch (breed) {
			case "HOL" -> switch (l) {
				case EXTENSION -> new int[] {0, 90, 1, 3, 3, 7};
				case DILUTION -> new int[] {0, 1};
				case PATTERN -> new int[] {3, 1};
				case WHITE -> new int[] {450, 900};
				case FACE -> new int[] {0, 4, 1, 3, 2, 3, 3, 1};
				case HORNS -> new int[] {1, 95, 0, 5};
				case HUMP, DEWLAP, EARS -> new int[] {0, 1};
			};
			case "JER" -> switch (l) {
				case EXTENSION -> new int[] {2, 1};
				case DILUTION -> new int[] {1, 1, 2, 1};
				case PATTERN -> new int[] {0, 9, 3, 1};
				case WHITE -> new int[] {0, 120};
				case FACE -> new int[] {0, 1};
				case HORNS -> new int[] {1, 1};
				case HUMP, DEWLAP, EARS -> new int[] {0, 1};
			};
			case "BSW" -> switch (l) {
				case EXTENSION -> new int[] {0, 1};
				case DILUTION -> new int[] {2, 1};
				case PATTERN -> new int[] {0, 1};
				case WHITE -> new int[] {0, 40};
				case FACE -> new int[] {0, 1};
				case HORNS -> new int[] {1, 1};
				case HUMP, DEWLAP, EARS -> new int[] {0, 1};
			};
			case "GYR" -> switch (l) {
				case EXTENSION -> new int[] {3, 2, 2, 1};
				case DILUTION -> new int[] {0, 2, 1, 1};
				case PATTERN -> new int[] {2, 1};
				case WHITE -> new int[] {300, 800};
				case FACE -> new int[] {0, 1};
				case HORNS -> new int[] {1, 1};
				case HUMP, DEWLAP, EARS -> new int[] {3, 1};
			};
			case "ANG" -> switch (l) {
				case EXTENSION -> new int[] {0, 97, 3, 3};
				case DILUTION -> new int[] {0, 1};
				case PATTERN -> new int[] {0, 1};
				case WHITE -> new int[] {0, 30};
				case FACE -> new int[] {0, 1};
				case HORNS -> new int[] {0, 1};
				case HUMP, DEWLAP, EARS -> new int[] {0, 1};
			};
			case "WAG" -> switch (l) {
				case EXTENSION -> new int[] {0, 1};
				case DILUTION -> new int[] {0, 1};
				case PATTERN -> new int[] {0, 1};
				case WHITE -> new int[] {0, 20};
				case FACE -> new int[] {0, 1};
				case HORNS -> new int[] {1, 1};
				case HUMP, DEWLAP, EARS -> new int[] {0, 1};
			};
			default -> of("HOL", l);
		};
	}
}

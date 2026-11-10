package dev.farmings.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Values taken before sheep were added (2026-11-10). Adding species must never change a cattle genome, hash, calf or score, so
 * these are pinned exactly.
 */
class CattlePinTest {
	static final Ruleset RULES = Ruleset.defaults();

	static String describe(Genome g, Phenotype p) {
		StringBuilder b = new StringBuilder(g.hash());
		for (Trait t : Species.CATTLE.traits()) {
			if (g.coreGenome().containsKey(t)) {
				b.append(' ').append(t.code).append('=').append(p.score(t));
			}
		}
		return b.toString();
	}

	@Test
	void foundersAreUnchanged() {
		Founders.Result hol = Founders.create("HOL", Sex.FEMALE, 42L, RULES);
		Founders.Result gyr = Founders.create("GYR", Sex.MALE, 7L, RULES);
		assertEquals("9053b9c7877c28d7ee166ca225e43796683eb127056db8e875af59f2bb5a9d6a MY=880 B=423 P=515 FE=553 H=429 F=336 L=465 T=678", describe(hol.genome(), hol.phenotype()));
		assertEquals("a4d5dc6e96d8da2ee2b728ca1dc48500ef9d3e66cc004a772f1dbb0a6b32286e MY=613 B=772 P=727 FE=817 H=899 F=900 L=860 T=725", describe(gyr.genome(), gyr.phenotype()));
	}

	@Test
	void calvesAreUnchanged() {
		Founders.Result dam = Founders.create("HOL", Sex.FEMALE, 42L, RULES);
		Founders.Result sire = Founders.create("JER", Sex.MALE, 99L, RULES);
		Offspring.Result calf = Offspring.create(dam.genome(), sire.genome(), "calf-1", "world", RULES, null);
		assertEquals("c1336746aa57700ae98c92b4837099e273c91b3f2da75099a2a456ad2652c237 MY=759 B=716 P=681 FE=738 H=595 F=630 L=728 T=646 FEMALE", describe(calf.genome(), calf.phenotype()) + " " + calf.sex());
		assertEquals("farmings.cattle.genome.v1;1;farmings.ruleset.1;HOL:5000,JER:5000;milk_yield=MY_A0,MY_A0|butterfat=B_A4,B_A6|protein=P_A1,P_A0|feed_efficiency=FE_A3,FE_A3|health=H_A3,H_A4|fertility=F_A0,F_A5|longevity=L_A4,L_A5|temperament=T_A0,T_A2", calf.genome().encode());
	}
}

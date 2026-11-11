package dev.farmings.husbandry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WildBreedsTest {
	@Test
	void biomesMapToTheirClimate() {
		assertEquals(WildBreeds.Group.HOT_DRY, WildBreeds.group("minecraft:savanna"));
		assertEquals(WildBreeds.Group.MOUNTAIN, WildBreeds.group("meadow"));
		assertEquals(WildBreeds.Group.TEMPERATE, WildBreeds.group("minecraft:plains"));
		assertEquals(WildBreeds.Group.TEMPERATE, WildBreeds.group("somemod:odd_biome"));
		assertEquals(CattleBreed.GYR, WildBreeds.cattle("minecraft:savanna", 5));
		assertEquals(CattleBreed.WAGYU, WildBreeds.cattle("minecraft:cherry_grove", 0));
		assertEquals(SheepBreed.DORPER, WildBreeds.sheep("minecraft:jungle", 3));
	}

	@Test
	void everyGroupHasBothSpeciesAndEveryBreedSpawnsSomewhere() {
		java.util.Set<CattleBreed> c = new java.util.HashSet<>();
		java.util.Set<SheepBreed> s = new java.util.HashSet<>();
		for (WildBreeds.Group g : WildBreeds.Group.values()) {
			assertFalse(g.cattle.isEmpty());
			assertFalse(g.sheep.isEmpty());
			c.addAll(g.cattle);
			s.addAll(g.sheep);
		}
		assertEquals(CattleBreed.values().length, c.size());
		assertEquals(SheepBreed.values().length, s.size());
	}

	@Test
	void rollsCoverEachBreedInAGroup() {
		assertEquals(CattleBreed.HOLSTEIN, WildBreeds.cattle("plains", 0));
		assertEquals(CattleBreed.JERSEY, WildBreeds.cattle("plains", 1));
		assertEquals(CattleBreed.ANGUS, WildBreeds.cattle("plains", 2));
		assertEquals(CattleBreed.HOLSTEIN, WildBreeds.cattle("plains", -3));
	}

	@Test
	void fowlByBiome() {
		java.util.Map<dev.farmings.genetics.Species, Integer> plains = WildBreeds.fowlWeights("minecraft:plains", 10);
		assertEquals(java.util.Map.of(dev.farmings.genetics.Species.QUAIL, 3, dev.farmings.genetics.Species.CHICKEN, 7), plains);
		assertEquals(dev.farmings.husbandry.poultry.FowlBreed.SILKIE, WildBreeds.fowl(dev.farmings.genetics.Species.CHICKEN, "minecraft:jungle", 5));
		assertEquals(null, WildBreeds.fowl(dev.farmings.genetics.Species.DUCK, "minecraft:forest", 0));
		assertEquals(java.util.Map.of(dev.farmings.genetics.Species.TURKEY, 10), WildBreeds.fowlWeights("minecraft:forest", 10));
		assertEquals(java.util.Map.of(dev.farmings.genetics.Species.CHICKEN, 10), WildBreeds.fowlWeights("somemod:odd_biome", 10));
		for (String b : new String[] {"minecraft:plains", "minecraft:river", "minecraft:swamp", "minecraft:savanna"}) {
			for (dev.farmings.husbandry.poultry.FowlBreed f : WildBreeds.fowlBreeds(b)) {
				assertTrue(f != dev.farmings.husbandry.poultry.FowlBreed.BROAD_BREASTED_WHITE, "BBW never spawns wild");
			}
		}
	}
}

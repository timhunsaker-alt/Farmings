package dev.farmings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FarmingsTest {
	@Test
	void modIdMatchesTheBuild() {
		assertEquals("farmings", Farmings.MOD_ID);
	}
}

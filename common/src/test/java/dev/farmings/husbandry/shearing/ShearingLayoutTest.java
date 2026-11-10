package dev.farmings.husbandry.shearing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShearingLayoutTest {
	final Map<String, ShearingLayout.Cell> cells = new HashMap<>();

	void set(int x, int z, ShearingLayout.Cell c) {
		cells.put(x + "," + z, c);
	}

	ShearingLayout.Check check() {
		return ShearingLayout.check((x, y, z) -> y == 0 ? cells.getOrDefault(x + "," + z, ShearingLayout.Cell.OTHER) : ShearingLayout.Cell.OTHER, 0, 0, 0);
	}

	/** Stand at 0,0; catching pen east of it running to the shed floor; table north; press beside the table. */
	void build() {
		set(0, 0, ShearingLayout.Cell.STAND);
		set(1, 0, ShearingLayout.Cell.CATCHING_PEN);
		set(2, 0, ShearingLayout.Cell.CATCHING_PEN);
		set(3, 0, ShearingLayout.Cell.SHED_FLOOR);
		set(0, -1, ShearingLayout.Cell.WOOL_TABLE);
		set(-1, -1, ShearingLayout.Cell.WOOL_PRESS);
	}

	@Test
	void aFullShearingShedForms() {
		build();
		ShearingLayout.Check c = check();
		assertTrue(c.formed(), c.problem());
		assertEquals(new ShearingLayout.Pos(1, 0, 0), c.standPoint());
		assertEquals(new ShearingLayout.Pos(3, 0, 0), c.shedFloor());
		assertEquals(2, c.catching().size());
		assertNotNull(c.press());
	}

	@Test
	void thePressIsOptional() {
		build();
		cells.remove("-1,-1");
		assertTrue(check().formed());
		assertNull(check().press());
	}

	@Test
	void problemsAreNamed() {
		set(0, 0, ShearingLayout.Cell.STAND);
		assertEquals("No catching pen beside the shearing stand", check().status());
		set(1, 0, ShearingLayout.Cell.CATCHING_PEN);
		assertEquals("The catching pen does not open onto a sheep shed's pen", check().status());
		set(2, 0, ShearingLayout.Cell.SHED_FLOOR);
		assertEquals("No wool table beside the shearing stand", check().status());
	}
}

package dev.farmings.farm.gantry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GantryToolTest {
	@Test
	void speedsAreAsaeTypicals() {
		assertEquals(9, GantryTool.SEED_DRILL.ticksPerBlock());      // 8 km/h: 2.22 m/s, 0.45 s a block
		assertEquals(14, GantryTool.HARVESTER.ticksPerBlock());      // 5 km/h: 0.72 s
		assertEquals(7, GantryTool.MOWER.ticksPerBlock());           // 11 km/h
	}

	@Test
	void powerFromTheStandard() {
		// mower: 8 kW a metre over 8 m, plus 1.5 kW of truck drives
		assertEquals(65500, GantryTool.MOWER.watts(8, 0), 1e-6);
		// drill: 300 N a row at 0.19 m over 4 m at 2.22 m/s = 14.0 kW, plus the trucks
		assertEquals(1500 + 300.0 * (4 / 0.19) * (8.0 / 3.6), GantryTool.SEED_DRILL.watts(4, 0), 1e-6);
		// chopper over 8 m of real corn silage (5.03 kg/m2): 8 x 1.39 x 5.03 x 3.6 = 201 t/h, 6 kW + 3.3 kWh/t
		double tph = 8 * (5.0 / 3.6) * 5.03 * 3.6;
		assertEquals(1500 + 6000 + 3300 * tph, GantryTool.HARVESTER.watts(8, 5.03), 1e-6);
	}
}

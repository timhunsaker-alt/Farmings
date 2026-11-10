package dev.farmings.process.coldroom;

/** What a cell is to the walk-in fridge's forming rule (docs/multiblocks/cold-room.md). Anything else (air, chests, stone) is OTHER. */
public enum ColdRoomPart { PANEL, DOOR, EVAPORATOR, CONDENSER, OTHER;

	public boolean shell() {
		return this == PANEL || this == DOOR || this == EVAPORATOR;
	}
}

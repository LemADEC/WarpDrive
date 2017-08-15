package cr0s.warpdrive.data;

public enum EnumShipControllerCommand {
	
	OFFLINE(0),      // Offline allows to move sub-ships
	IDLE(1),
	MANUAL(2),       // Move ship around including take off and landing
	// AUTOPILOT(3), // Move ship towards a far destination
	SUMMON(4),       // Summoning crew
	HYPERDRIVE(5),   // Jump to/from Hyperspace
	GATE(6),         // Jump via jumpgate
	MAINTENANCE(7);  // Maintenance mode
	
	private final int code;
	
	EnumShipControllerCommand(final int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
}

package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

public enum EnumShipCommand implements IStringSerializable {
	
	OFFLINE     ("offline"        , false), // Offline allows to move overlapping ships
	IDLE        ("idle"           , false), //
	MANUAL      ("manual"         , true ), // Move ship around including take off and landing
	// AUTOPILOT("autopilot"         , true ), // Move ship towards a far destination
	// SUMMON   ("summon"            , false), // Summoning crew
	HYPERDRIVE  ("hyperdrive"     , true ), // Jump to/from Hyperspace
	GATE        ("gate"           , true ), // Jump via jumpgate
	MAINTENANCE ("maintenance"    , false); // Maintenance mode
	
	private final String name;
	private final boolean isMovement;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumShipCommand> ID_MAP = new HashMap<>();
	
	static {
		length = EnumShipCommand.values().length;
		for (final EnumShipCommand forceFieldShape : values()) {
			ID_MAP.put(forceFieldShape.ordinal(), forceFieldShape);
		}
	}
	
	EnumShipCommand(@Nonnull final String name, final boolean isMovement) {
		this.name = name;
		this.isMovement = isMovement;
	}
	
	public static EnumShipCommand get(final int damage) {
		return ID_MAP.get(damage);
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public boolean isMovement() {
		return isMovement;
	}
}

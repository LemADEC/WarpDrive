package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumShipCoreState implements IStringSerializable {
	
	DISCONNECTED  (0, "disconnected"),   // Not connected to controller
	IDLE          (1, "idle"),           // Ready for next command
	SCANNING      (2, "scanning"),       // Ready for next command
	ONLINE        (3, "online"),         // Computing parameters
	WARMING_UP    (4, "warming_up"),     // Warmup phase
	COOLING_DOWN  (5, "cooling_down");   // Pending cooldown
	
	private final int metadata;
	private final String unlocalizedName;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumShipCoreState> ID_MAP = new HashMap<>();
	
	static {
		length = EnumShipCoreState.values().length;
		for (EnumShipCoreState shipCoreState : values()) {
			ID_MAP.put(shipCoreState.ordinal(), shipCoreState);
		}
	}
	
	EnumShipCoreState(final int metadata, final String unlocalizedName) {
		this.metadata = metadata;
		this.unlocalizedName = unlocalizedName;
	}
	
	public int getMetadata() {
		return metadata;
	}
	
	public static EnumShipCoreState get(final int id) {
		return ID_MAP.get(id);
	}
	
	@Nonnull
	@Override
	public String getName() { return unlocalizedName; }
}

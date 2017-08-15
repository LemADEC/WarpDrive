package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumShipScannerState implements IStringSerializable {
	
	IDLE          (0, "idle"),           // Ready for next command
	SCANNING      (1, "scanning"),       // Scanning a ship
	DEPLOYING     (2, "online");         // Deploying a ship
	
	private final int metadata;
	private final String unlocalizedName;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumShipScannerState> ID_MAP = new HashMap<>();
	
	static {
		length = EnumShipScannerState.values().length;
		for (EnumShipScannerState shipCoreState : values()) {
			ID_MAP.put(shipCoreState.ordinal(), shipCoreState);
		}
	}
	
	EnumShipScannerState(final int metadata, final String unlocalizedName) {
		this.metadata = metadata;
		this.unlocalizedName = unlocalizedName;
	}
	
	public int getMetadata() {
		return metadata;
	}
	
	public static EnumShipScannerState get(final int id) {
		return ID_MAP.get(id);
	}
	
	@Nonnull
	@Override
	public String getName() { return unlocalizedName; }
}

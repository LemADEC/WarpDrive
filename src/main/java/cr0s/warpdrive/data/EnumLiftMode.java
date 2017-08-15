package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumLiftMode implements IStringSerializable {
	INACTIVE			("inactive"),
	UP					("up"),
	DOWN				("down"),
	REDSTONE			("redstone");
	
	private final String unlocalizedName;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumLiftMode> ID_MAP = new HashMap<>();
	
	static {
		length = EnumLiftMode.values().length;
		for (EnumLiftMode componentType : values()) {
			ID_MAP.put(componentType.ordinal(), componentType);
		}
	}
	
	EnumLiftMode(String unlocalizedName) {
		this.unlocalizedName = unlocalizedName;
	}
	
	public static EnumLiftMode get(final int damage) {
		return ID_MAP.get(damage);
	}

	public String getUnlocalizedName() {
		return unlocalizedName;
	}
	
	@Nonnull
	@Override
	public String getName() { return unlocalizedName; }
}

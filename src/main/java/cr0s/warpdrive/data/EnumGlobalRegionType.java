package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumGlobalRegionType implements IStringSerializable {
	
	UNDEFINED        ("-undefined-"      , true ),
	SHIP             ("ship"             , true ), // a ship core
	JUMP_GATE        ("jump_gate"        , true ), // a jump gate
	PLANET           ("planet"           , true ), // a planet (a transition plane allowing to move to another dimension)
	STAR             ("star"             , true ), // a star
	STRUCTURE        ("structure"        , true ), // a structure from WorldGeneration (moon, asteroid field, etc.)
	WARP_ECHO        ("warp_echo"        , true ), // remains of a warp
	ACCELERATOR      ("accelerator"      , false), // an accelerator setup
	TRANSPORTER      ("transporter"      , false), // a transporter room
	VIRTUAL_ASSISTANT("virtual_assistant", false), // a virtual assistant
	REACTOR          ("reactor"          , false); // a reactor
	
	private final String name;
	private final boolean hasRadarEcho;
	
	// cached values
	public static final int length;
	private static final HashMap<String, EnumGlobalRegionType> mapNames = new HashMap<>();
	
	static {
		length = EnumGlobalRegionType.values().length;
		for (final EnumGlobalRegionType enumGlobalRegionType : values()) {
			mapNames.put(enumGlobalRegionType.getName(), enumGlobalRegionType);
		}
	}
	
	EnumGlobalRegionType(final String name, final boolean hasRadarEcho) {
		this.name = name;
		this.hasRadarEcho = hasRadarEcho;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public static EnumGlobalRegionType getByName(final String name) {
		return mapNames.get(name);
	}
	
	public boolean hasRadarEcho() {
		return hasRadarEcho;
	}
}

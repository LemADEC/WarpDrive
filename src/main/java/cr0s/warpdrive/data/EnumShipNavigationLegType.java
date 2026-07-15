package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

/**
 * Identifies an autopilot navigation leg by role rather than by display string.
 * Each leg carries the ship command it maps to, a localization key, and whether
 * it requires explicit crew confirmation before the autopilot may engage it.
 */
public enum EnumShipNavigationLegType implements IStringSerializable {

	TAKEOFF           ("takeoff"           , EnumShipCommand.MANUAL    , false),
	SPACE_CRUISE      ("space_cruise"      , EnumShipCommand.MANUAL    , false),
	ORBITAL_APPROACH  ("orbital_approach"  , EnumShipCommand.MANUAL    , false),
	HYPERSPACE_ENTER  ("hyperspace_enter"  , EnumShipCommand.HYPERDRIVE, true ),
	HYPERSPACE_CRUISE ("hyperspace_cruise" , EnumShipCommand.MANUAL    , false),
	HYPERSPACE_EXIT   ("hyperspace_exit"   , EnumShipCommand.HYPERDRIVE, true ),
	LANDING           ("landing"           , EnumShipCommand.MANUAL    , true ),
	;

	private final String name;
	private final EnumShipCommand command;
	private final boolean requiresConfirmation;

	// cached values
	public static final int length;
	private static final HashMap<String, EnumShipNavigationLegType> NAME_MAP = new HashMap<>();

	static {
		length = EnumShipNavigationLegType.values().length;
		for (final EnumShipNavigationLegType legType : values()) {
			NAME_MAP.put(legType.name, legType);
		}
	}

	EnumShipNavigationLegType(@Nonnull final String name, @Nonnull final EnumShipCommand command, final boolean requiresConfirmation) {
		this.name = name;
		this.command = command;
		this.requiresConfirmation = requiresConfirmation;
	}

	@Nullable
	public static EnumShipNavigationLegType get(@Nullable final String name) {
		return name == null ? null : NAME_MAP.get(name);
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	public EnumShipCommand getCommand() {
		return command;
	}

	public boolean requiresConfirmation() {
		return requiresConfirmation;
	}

	@Nonnull
	public String getTitleKey() {
		return "warpdrive.navigation.leg." + name;
	}

	@Nonnull
	public String getDescriptionKey() {
		return "warpdrive.navigation.leg." + name + ".description";
	}
}

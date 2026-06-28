package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

/**
 * Autopilot autonomy level for a ship core.
 * OFF          - legacy single-leg behaviour, no auto-chaining.
 * ASSISTED     - plan the route but require a manual Engage/Step per leg.
 * SAFETY_STOPS - auto-chain safe legs, pause for confirmation before risky legs (hyperspace folds, landing) and on low energy.
 * FULL_AUTO    - auto-chain every leg to the destination, halting only on failure or crew pause.
 */
public enum EnumShipAutopilotMode implements IStringSerializable {

	OFF          ("off"         ),
	ASSISTED     ("assisted"    ),
	SAFETY_STOPS ("safety_stops"),
	FULL_AUTO    ("full_auto"   ),
	;

	private final String name;

	public static final int length;
	private static final HashMap<String, EnumShipAutopilotMode> NAME_MAP = new HashMap<>();

	static {
		length = EnumShipAutopilotMode.values().length;
		for (final EnumShipAutopilotMode mode : values()) {
			NAME_MAP.put(mode.name, mode);
		}
	}

	EnumShipAutopilotMode(@Nonnull final String name) {
		this.name = name;
	}

	@Nonnull
	public static EnumShipAutopilotMode get(@Nullable final String name) {
		if (name == null) {
			return OFF;
		}
		final EnumShipAutopilotMode mode = NAME_MAP.get(name.toLowerCase());
		return mode == null ? OFF : mode;
	}

	@Nonnull
	public EnumShipAutopilotMode next() {
		return values()[(ordinal() + 1) % length];
	}

	public boolean isActive() {
		return this != OFF;
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	public String getLabelKey() {
		return "warpdrive.navigation.autopilot.mode." + name;
	}
}

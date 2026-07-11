package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

/**
 * Surfaced state of a ship core autopilot run.
 */
public enum EnumShipAutopilotStatus implements IStringSerializable {

	IDLE             ("idle"            ),
	PLANNED          ("planned"         ),
	RUNNING          ("running"         ),
	WAITING_COOLDOWN ("waiting_cooldown"),
	WAITING_ENERGY   ("waiting_energy"  ),
	WAITING_CONFIRM  ("waiting_confirm" ),
	PAUSED           ("paused"          ),
	ABORTED          ("aborted"         ),
	ARRIVED          ("arrived"         ),
	;

	private final String name;

	public static final int length;
	private static final HashMap<String, EnumShipAutopilotStatus> NAME_MAP = new HashMap<>();

	static {
		length = EnumShipAutopilotStatus.values().length;
		for (final EnumShipAutopilotStatus status : values()) {
			NAME_MAP.put(status.name, status);
		}
	}

	EnumShipAutopilotStatus(@Nonnull final String name) {
		this.name = name;
	}

	@Nonnull
	public static EnumShipAutopilotStatus get(@Nullable final String name) {
		if (name == null) {
			return IDLE;
		}
		final EnumShipAutopilotStatus status = NAME_MAP.get(name.toLowerCase());
		return status == null ? IDLE : status;
	}

	/** Whether the autopilot pump should keep evaluating this run. */
	public boolean isActive() {
		return this == PLANNED || this == RUNNING
		    || this == WAITING_COOLDOWN || this == WAITING_ENERGY;
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	public String getLabelKey() {
		return "warpdrive.navigation.autopilot.status." + name;
	}
}

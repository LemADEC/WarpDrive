package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.nbt.NBTTagCompound;

public class AcceleratorControlParameter {
	
	// persistent properties
	public int controlChannel;  // final
	public boolean isEnabled = true;
	public double threshold = WarpDriveConfig.ACCELERATOR_THRESHOLD_DEFAULT;
	public String description = "-";
	
	public AcceleratorControlParameter(final int controlChannel) {
		this.controlChannel = controlChannel;
	}
	
	public AcceleratorControlParameter(final NBTTagCompound nbtTagCompound) {
		readFromNBT(nbtTagCompound);
	}
	
	private void readFromNBT(final NBTTagCompound nbtTagCompound) {
		controlChannel = nbtTagCompound.getInteger(IControlChannel.CONTROL_CHANNEL_TAG);
		isEnabled = !nbtTagCompound.hasKey("isEnabled") || nbtTagCompound.getBoolean("isEnabled");
		threshold = nbtTagCompound.getDouble("threshold");
		description = nbtTagCompound.getString("description");
	}
	
	public NBTTagCompound writeToNBT(final NBTTagCompound nbtTagCompound) {
		nbtTagCompound.setInteger(IControlChannel.CONTROL_CHANNEL_TAG, controlChannel);
		nbtTagCompound.setBoolean("isEnabled", isEnabled);
		nbtTagCompound.setDouble("threshold", threshold);
		nbtTagCompound.setString("description", description);
		return nbtTagCompound;
	}
	
	// Hash based collections need a stable hashcode, so we use a unique id instead
	@Override
	public int hashCode() {
		return controlChannel;
	}
	
	@Override
	public boolean equals(final Object object) {
		if (object instanceof AcceleratorControlParameter) {
			final AcceleratorControlParameter acceleratorControlParameter = (AcceleratorControlParameter) object;
			return controlChannel == acceleratorControlParameter.controlChannel
			    && isEnabled == acceleratorControlParameter.isEnabled
			    && threshold == acceleratorControlParameter.threshold
			    && description.equals(acceleratorControlParameter.description);
		}
		
		return false;
	}
	
	
	@Override
	public String toString() {
		return String.format("%s/%d isEnabled %s threshold %.3f '%s'",
			getClass().getSimpleName(),
			controlChannel,
			isEnabled,
			threshold,
			description);
	}
}

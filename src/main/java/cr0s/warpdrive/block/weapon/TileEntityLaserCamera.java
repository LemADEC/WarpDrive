package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ICamera;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.block.TileEntityLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumCameraType;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.fml.common.Optional;

public class TileEntityLaserCamera extends TileEntityLaser implements ICamera {
	
	private int videoChannel = -1;
	private float cameraYaw = Float.NaN;
	private float cameraPitch = Float.NaN;
	
	private static final int REGISTRY_UPDATE_INTERVAL_TICKS = 15 * 20;
	private static final int PACKET_SEND_INTERVAL_TICKS = 60 * 20;
	
	private int packetSendTicks = 10;
	private int registryUpdateTicks = 20;
	
	public TileEntityLaserCamera() {
		super();
		
		peripheralName = "warpdriveLaserCamera";
		addMethods(new String[] {
			"videoChannel"
		});
		// done by ancestor: doRequireUpgradeToInterface();
	}
	
	@Override
	public void update() {
		super.update();
		
		// Update video channel on clients (recovery mechanism, no need to go too fast)
		if (!world.isRemote) {
			packetSendTicks--;
			if (packetSendTicks <= 0) {
				packetSendTicks = PACKET_SEND_INTERVAL_TICKS;
				PacketHandler.sendVideoChannelPacket(world, pos, videoChannel);
			}
		} else {
			registryUpdateTicks--;
			if (registryUpdateTicks <= 0) {
				registryUpdateTicks = REGISTRY_UPDATE_INTERVAL_TICKS;
				if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
					WarpDrive.logger.info(String.format("%s Updating registry (%d)",
					                                    this, videoChannel ));
				}
				WarpDrive.cameras.updateInRegistry(world, pos, videoChannel, EnumCameraType.LASER_CAMERA);
			}
		}
	}
	
	@Override
	public int getVideoChannel() {
		return videoChannel;
	}
	
	@Override
	public void setVideoChannel(final int parVideoChannel) {
		if ( videoChannel != parVideoChannel
		  && IVideoChannel.isValid(parVideoChannel) ) {
			final int videoChannelOld = videoChannel;
			videoChannel = parVideoChannel;
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(String.format("%s Video channel updated from %d to %d",
				                                    this, videoChannelOld, parVideoChannel ));
			}
			markDirty();
			// force update through main thread since CC & OC are running outside the main thread
			packetSendTicks = 0;
			registryUpdateTicks = 0;
		}
	}

	@Override
	public boolean hasCameraOrientation() {
		return Float.isFinite(cameraYaw) && Float.isFinite(cameraPitch);
	}

	@Override
	public float getCameraYaw() {
		return cameraYaw;
	}

	@Override
	public float getCameraPitch() {
		return cameraPitch;
	}

	@Override
	public void setCameraOrientation(final float yaw, final float pitch) {
		cameraYaw = yaw;
		cameraPitch = pitch;
		markDirty();
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		setVideoChannel(tagCompound.getInteger("cameraFrequency") + tagCompound.getInteger(VIDEO_CHANNEL_TAG));
		if ( tagCompound.hasKey(CAMERA_YAW_TAG)
		  && tagCompound.hasKey(CAMERA_PITCH_TAG) ) {
			cameraYaw = tagCompound.getFloat(CAMERA_YAW_TAG);
			cameraPitch = tagCompound.getFloat(CAMERA_PITCH_TAG);
		} else {
			cameraYaw = Float.NaN;
			cameraPitch = Float.NaN;
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		tagCompound.setInteger(VIDEO_CHANNEL_TAG, videoChannel);
		if (hasCameraOrientation()) {
			tagCompound.setFloat(CAMERA_YAW_TAG, cameraYaw);
			tagCompound.setFloat(CAMERA_PITCH_TAG, cameraPitch);
		}
		return tagCompound;
	}
	
	@Override
	public void invalidate() {
		WarpDrive.cameras.removeFromRegistry(world, pos);
		super.invalidate();
	}
	
	@Override
	public void onChunkUnload() {
		WarpDrive.cameras.removeFromRegistry(world, pos);
		super.onChunkUnload();
	}
	
	// Common OC/CC methods
	public Object[] videoChannel(final Object[] arguments) {
		if (arguments.length == 1) {
			setVideoChannel(Commons.toInt(arguments[0]));
		}
		return new Integer[] { getVideoChannel() };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] videoChannel(final Context context, final Arguments arguments) {
		return videoChannel(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "videoChannel":
			return videoChannel(arguments);
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s Beam '%d' Camera '%d' %s",
		                     getClass().getSimpleName(),
		                     beamFrequency,
		                     videoChannel,
		                     Commons.format(world, pos));
	}
}

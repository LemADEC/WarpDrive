package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.block.TileEntityAbstractInterfaced;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CameraRegistryItem;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Optional;

public class TileEntityMonitor extends TileEntityAbstractInterfaced implements IVideoChannel {
	
	private int videoChannel = -1;
	
	private static final int PACKET_SEND_INTERVAL_TICKS = 60 * 20;
	private int packetSendTicks = 10;
	
	public TileEntityMonitor() {
		super();
		
		peripheralName = "warpdriveMonitor";
		addMethods(new String[] {
			"videoChannel"
		});
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (!worldObj.isRemote) {
			packetSendTicks--;
			if (packetSendTicks <= 0) {
				packetSendTicks = PACKET_SEND_INTERVAL_TICKS;
				PacketHandler.sendVideoChannelPacket(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, videoChannel);
			}
		}
	}
	
	@Override
	public int getVideoChannel() {
		return videoChannel;
	}
	
	@Override
	public void setVideoChannel(final int parVideoChannel) {
		if (videoChannel != parVideoChannel && (parVideoChannel <= VIDEO_CHANNEL_MAX) && (parVideoChannel > VIDEO_CHANNEL_MIN)) {
			videoChannel = parVideoChannel;
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(this + " Monitor video channel set to " + videoChannel);
			}
			// force update through main thread since CC runs on server as 'client'
			packetSendTicks = 0;
			markDirty();
		}
	}
	
	private String getVideoChannelStatus() {
		if (videoChannel == -1) {
			return StatCollector.translateToLocalFormatted("warpdrive.video_channel.statusLine.undefined");
		} else if (videoChannel < 0) {
			return StatCollector.translateToLocalFormatted("warpdrive.video_channel.statusLine.invalid", videoChannel);
		} else {
			CameraRegistryItem camera = WarpDrive.cameras.getCameraByVideoChannel(worldObj, videoChannel);
			if (camera == null) {
				return StatCollector.translateToLocalFormatted("warpdrive.video_channel.statusLine.invalidOrNotLoaded", videoChannel);
			} else if (camera.isTileEntity(this)) {
				return StatCollector.translateToLocalFormatted("warpdrive.video_channel.statusLine.valid", videoChannel);
			} else {
				return StatCollector.translateToLocalFormatted("warpdrive.video_channel.statusLine.validCamera",
						videoChannel,
						camera.position.chunkPosX,
						camera.position.chunkPosY,
						camera.position.chunkPosZ);
			}
		}
	}
	
	@Override
	public String getStatus() {
		return super.getStatus()
				+ getVideoChannelStatus();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		videoChannel = tag.getInteger("frequency") + tag.getInteger(VIDEO_CHANNEL_TAG);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger(VIDEO_CHANNEL_TAG, videoChannel);
	}
	
	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 10, tagCompound);
	}
	
	@Override
	public void onDataPacket(NetworkManager networkManager, S35PacketUpdateTileEntity packet) {
		NBTTagCompound tagCompound = packet.func_148857_g();
		readFromNBT(tagCompound);
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] videoChannel(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setVideoChannel(arguments.checkInteger(0));
		}
		return new Integer[] { videoChannel };
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		if (methodName.equals("videoChannel")) {
			if (arguments.length == 1) {
				setVideoChannel(Commons.toInt(arguments[0]));
			}
			return new Integer[] { videoChannel };
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s %d @ \'%s\' (%d %d %d)", 
				getClass().getSimpleName(),
				videoChannel,
				worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(),
						xCoord, yCoord, zCoord);
	}
}
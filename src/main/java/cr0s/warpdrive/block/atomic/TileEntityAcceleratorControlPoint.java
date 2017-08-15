package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.block.TileEntityAbstractInterfaced;
import cr0s.warpdrive.config.WarpDriveConfig;
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

public class TileEntityAcceleratorControlPoint extends TileEntityAbstractInterfaced implements IControlChannel {
	
	// persistent properties
	private boolean isEnabled = true;
	private int controlChannel = -1;
	
	// computed properties
	private static final int UPDATE_INTERVAL_TICKS = 20;
	private int updateTicks;
	
	public TileEntityAcceleratorControlPoint() {
		super();
		
		peripheralName = "warpdriveAcceleratorControlPoint";
		addMethods(new String[] {
			"enable",
			"state",
			"controlChannel"
		});
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		updateTicks--;
		if (updateTicks <= 0) {
			updateTicks = UPDATE_INTERVAL_TICKS;
			updateMetadata((controlChannel == -1) || !isEnabled ? 0 : 1);
		}
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
	}
	
	@Override
	public int getControlChannel() {
		return controlChannel;
	}
	
	@Override
	public void setControlChannel(final int controlChannel) {
		if (this.controlChannel != controlChannel) {
			this.controlChannel = controlChannel;
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(this + " Accelerator control point controlChannel channel set to " + controlChannel);
			}
			// force update through main thread since CC runs on server as 'client'
			markDirty();
		}
	}
	
	private String getControlChannelStatus() {
		if (controlChannel == -1) {
			return StatCollector.translateToLocalFormatted("warpdrive.control_channel.statusLine.undefined");
		} else if (controlChannel < CONTROL_CHANNEL_MIN || controlChannel > CONTROL_CHANNEL_MAX) {
			return StatCollector.translateToLocalFormatted("warpdrive.control_channel.statusLine.invalid", controlChannel);
		} else {
			return StatCollector.translateToLocalFormatted("warpdrive.control_channel.statusLine.valid", controlChannel);
		}
	}
	
	@Override
	public String getStatus() {
		return super.getStatus()
		       + getControlChannelStatus();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		isEnabled = !tagCompound.hasKey("isEnabled") || tagCompound.getBoolean("isEnabled");
		controlChannel = tagCompound.getInteger(CONTROL_CHANNEL_TAG);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setBoolean("isEnabled", isEnabled);
		tag.setInteger(CONTROL_CHANNEL_TAG, controlChannel);
	}
	
	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tagCompound);
	}
	
	@Override
	public void onDataPacket(NetworkManager networkManager, S35PacketUpdateTileEntity packet) {
		NBTTagCompound tagCompound = packet.func_148857_g();
		readFromNBT(tagCompound);
	}
	
	public boolean getIsEnabled() {
		return isEnabled;
	}
	
	public void setIsEnabled(final boolean isEnabled) {
		this.isEnabled = isEnabled;
		WarpDrive.starMap.onBlockUpdated(worldObj, xCoord, yCoord, zCoord, getBlockType(), getBlockMetadata());
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] enable(Context context, Arguments arguments) {
		return enable(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] controlChannel(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setControlChannel(arguments.checkInteger(0));
		}
		return new Integer[] { controlChannel };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] state(Context context, Arguments arguments) {
		return state();
	}
	
	// Common OC/CC methods
	public Object[] enable(Object[] arguments) {
		if (arguments.length == 1) {
			boolean enable;
			try {
				enable = Commons.toBool(arguments[0]);
			} catch (Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(this + " LUA error on enable(): Boolean expected for 1st argument " + arguments[0]);
				}
				return new Object[] { isEnabled };
			}
			setIsEnabled(enable);
		}
		return new Object[] { isEnabled };
	}
	
	private Object[] state() {    // isConnected, isPowered, shape
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isEnabled, controlChannel };
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		try {
			switch (methodName) {
			case "enable":
				return enable(arguments);
			
			case "controlChannel":
				if (arguments.length == 1) {
					setControlChannel(Commons.toInt(arguments[0]));
				}
				return new Integer[] { controlChannel };
				
			case "state":
				return state();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			return new String[] { exception.getMessage() };
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
}

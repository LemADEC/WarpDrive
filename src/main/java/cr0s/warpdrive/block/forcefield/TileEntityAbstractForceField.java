package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.ForceFieldRegistry;
import cr0s.warpdrive.data.Vector3;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Optional;

public class TileEntityAbstractForceField extends TileEntityAbstractEnergy implements IBeamFrequency {
	
	// persistent properties
	protected byte tier = -1;
	protected int beamFrequency = -1;
	public boolean isEnabled = true;
	
	// computed properties
	protected Vector3 vRGB;
	protected boolean isConnected = false;
	
	public TileEntityAbstractForceField() {
		super();

		addMethods(new String[]{
			"enable",
			"beamFrequency"
		});
	}
	
	@Override
	protected void onFirstUpdateTick() {
		Block block = getBlockType();
		if (block instanceof BlockAbstractForceField) {
			tier = ((BlockAbstractForceField) block).tier;
		} else {
			WarpDrive.logger.error("Missing block for " + this + " at " + worldObj + " " + xCoord + " " + yCoord + " " + zCoord);
		}
		if (beamFrequency >= 0 && beamFrequency <= IBeamFrequency.BEAM_FREQUENCY_MAX) {
			ForceFieldRegistry.updateInRegistry(this);
		}
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		// Frequency is not set
		final boolean new_isConnected = beamFrequency > 0 && beamFrequency <= IBeamFrequency.BEAM_FREQUENCY_MAX;
		if (isConnected != new_isConnected) {
			isConnected = new_isConnected;
			markDirty();
		}
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
		ForceFieldRegistry.removeFromRegistry(this);
	}
	
	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		// reload chunks as needed
		// ForceFieldRegistry.removeFromRegistry(this);
	}
	
	@Override
	public int getBeamFrequency() {
		return beamFrequency;
	}
	
	@Override
	public void setBeamFrequency(final int parBeamFrequency) {
		if (beamFrequency != parBeamFrequency && (parBeamFrequency <= BEAM_FREQUENCY_MAX) && (parBeamFrequency > BEAM_FREQUENCY_MIN)) {
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(this + " Beam frequency set from " + beamFrequency + " to " + parBeamFrequency);
			}
			if (hasWorldObj()) {
				ForceFieldRegistry.removeFromRegistry(this);
			}
			beamFrequency = parBeamFrequency;
			vRGB = IBeamFrequency.getBeamColor(beamFrequency);
		}
		markDirty();
		if (hasWorldObj()) {
			ForceFieldRegistry.updateInRegistry(this);
		}
	}
	
	String getBeamFrequencyStatus() {
		if (beamFrequency == -1) {
			return StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.statusLine.undefined");
		} else if (beamFrequency < 0) {
			return StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.statusLine.invalid", beamFrequency);
		} else {
			return StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.statusLine.valid", beamFrequency);
		}
	}
	
	@Override
	public String getStatus() {
		return super.getStatus()
			+ "\n" + getBeamFrequencyStatus();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		tier = tagCompound.getByte("tier");
		setBeamFrequency(tagCompound.getInteger(BEAM_FREQUENCY_TAG));
		isEnabled = !tagCompound.hasKey("isEnabled") || tagCompound.getBoolean("isEnabled");
		isConnected = tagCompound.getBoolean("isConnected");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setByte("tier", tier);
		tagCompound.setInteger(BEAM_FREQUENCY_TAG, beamFrequency);
		tagCompound.setBoolean("isEnabled", isEnabled);
		tagCompound.setBoolean("isConnected", isConnected);
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
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] enable(Context context, Arguments arguments) {
		return enable(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] beamFrequency(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setBeamFrequency(arguments.checkInteger(0));
		}
		return new Integer[] { beamFrequency };
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
			isEnabled = enable;
		}
		return new Object[] { isEnabled };
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
				
			case "beamFrequency":
				if (arguments.length == 1) {
					setBeamFrequency(Commons.toInt(arguments[0]));
				}
				return new Integer[]{ beamFrequency };
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			return new String[] { exception.getMessage() };
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s Beam \'%d\' @ \'%s\' (%d %d %d)", getClass().getSimpleName(),
			beamFrequency, worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(), xCoord, yCoord, zCoord);
	}
}

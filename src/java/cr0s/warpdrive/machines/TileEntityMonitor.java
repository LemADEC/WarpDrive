package cr0s.warpdrive.machines;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import cr0s.warpdrive.PacketHandler;
import cr0s.warpdrive.WarpDrive;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;

public class TileEntityMonitor extends WarpInterfacedTE {
	private int frequency = -1;

	private final static int PACKET_SEND_INTERVAL_TICKS = 60 * 20;
	private int packetSendTicks = 20;

	public TileEntityMonitor() {
		super();
		peripheralName = "monitor";
		methodsArray = new String[] {
			"freq"
		};
	}
	
	@Override
	public void updateEntity() {
		if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
			packetSendTicks--;
			if (packetSendTicks <= 0) {
				packetSendTicks = PACKET_SEND_INTERVAL_TICKS;
				PacketHandler.sendFreqPacket(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, frequency);
			}
			return;
		}
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int parFrequency) {
		if (frequency != parFrequency) {
			frequency = parFrequency;
			WarpDrive.debugPrint("" + this + " Monitor frequency set to " + frequency);
	        // force update through main thread since CC runs on server as 'client'
	        packetSendTicks = 0;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		frequency = tag.getInteger("frequency");
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("frequency", frequency);
	}

	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] freq(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setFrequency(arguments.checkInteger(0));
		}
		return new Integer[] { frequency };
	}

	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
    	String methodName = methodsArray[method];
    	if (methodName.equals("freq")) {
			if (arguments.length == 1) {
				setFrequency(toInt(arguments[0]));
			}
			return new Integer[] { frequency };
    	}
    	return null;
	}
	
	@Override
	public String toString() {
        return String.format("%s/%d \'%d\' @ \'%s\' %d, %d, %d", new Object[] {
       		getClass().getSimpleName(),
       		Integer.valueOf(hashCode()),
       		frequency,
       		worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(),
       		xCoord, yCoord, zCoord});
	}
}
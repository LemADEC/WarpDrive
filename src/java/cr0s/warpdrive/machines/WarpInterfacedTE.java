package cr0s.warpdrive.machines;

import java.util.HashMap;
import java.util.Set;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cr0s.warpdrive.WarpDriveConfig;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

@Optional.InterfaceList({
	@Optional.Interface(iface = "li.cil.oc.api.network.Environment", modid = "OpenComputers"),
	@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft")
})
public abstract class WarpInterfacedTE extends WarpTE implements IPeripheral, Environment {
	WarpInterfacedTE() {
		super();
		if (Loader.isModLoaded("OpenComputers")) {
			OC_node = Network.newNode(this, Visibility.Network).withComponent(peripheralName).create();
		}
	}
	
	// Common computer properties
	protected String peripheralName = null;
	protected String[] methodsArray = {};
	
	// OpenComputer specific properties
	protected Node		OC_node = null;
	protected boolean	OC_addedToNetwork = false;
	
	// ComputerCraft specific properties
	protected HashMap<Integer, IComputerAccess> connectedComputers = new HashMap<Integer, IComputerAccess>();
	
	// TileEntity overrides, notably for OpenComputer
	@Override
 	public void updateEntity() {
		super.updateEntity();
		if (Loader.isModLoaded("OpenComputers")) {
			if (!OC_addedToNetwork) {
				OC_addedToNetwork = true;
				Network.joinOrCreateNetwork(this);
			}
		}
	}
	
	@Override
	public void invalidate() {
		if (Loader.isModLoaded("OpenComputers")) {
			if (OC_node != null) {
				OC_node.remove();
				OC_node = null;
			}
		}
		super.invalidate();
	}
	
	@Override
	public void onChunkUnload() {
		if (Loader.isModLoaded("OpenComputers")) {
			if (OC_node != null) {
				OC_node.remove();
				OC_node = null;
			}
		}
		super.onChunkUnload();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		if (Loader.isModLoaded("OpenComputers")) {
			if (OC_node != null && OC_node.host() == this) {
				OC_node.load(tag.getCompoundTag("oc:node"));
			}
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		if (Loader.isModLoaded("OpenComputers")) {
			if (OC_node != null && OC_node.host() == this) {
				final NBTTagCompound nodeNbt = new NBTTagCompound();
				OC_node.save(nodeNbt);
				tag.setTag("oc:node", nodeNbt);
			}
		}
	}
	
	@Override
	public int hashCode() {
		return (((((super.hashCode() + worldObj.provider.dimensionId << 4) + xCoord) << 4) + yCoord) << 4) + zCoord;
	}
	
	// Dirty cheap conversion methods
	protected Object[] argumentsOCtoCC(Arguments args) {
		Object[] arguments = new Object[args.count()];
		int index = 0;
		for (Object arg:args) {
			arguments[index] = arg;
			index++;
		}
		return arguments;
	}
	
	// ComputerCraft methods
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public String getType() {
		return peripheralName;
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public String[] getMethodNames() {
		return methodsArray;
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		// empty stub
		return null;
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public void attach(IComputerAccess computer) {
		int id = computer.getID();
		connectedComputers.put(id, computer);
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public void detach(IComputerAccess computer) {
		int id = computer.getID();
		if (connectedComputers.containsKey(id)) {
			connectedComputers.remove(id);
		}
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public boolean equals(IPeripheral other) {
		// WarpDrive.debugPrint("WarpInterfacedTE.equals");
		return other.hashCode() == hashCode();
	}
	
	// Computer abstraction methods
	protected void sendEvent(String eventName, Object[] arguments) {
		// WarpDrive.debugPrint("" + this + " Sending event '" + eventName + "'");
		if (WarpDriveConfig.isCCLoaded) {
			Set<Integer> keys = connectedComputers.keySet();
			for(Integer key:keys) {
				IComputerAccess comp = connectedComputers.get(key);
				comp.queueEvent(eventName, arguments);
			}
		}
	}
	
	// OpenComputers methods
	@Override
	@Optional.Method(modid = "OpenComputers")
	public Node node() {
		return OC_node;
	}
	
	@Override
	@Optional.Method(modid = "OpenComputers")
	public void onConnect(Node node) {
		// nothing special
	}
	
	@Override
	@Optional.Method(modid = "OpenComputers")
	public void onDisconnect(Node node) {
		// nothing special
	}
	
	@Override
	@Optional.Method(modid = "OpenComputers")
	public void onMessage(Message message) {
		// nothing special
	}
}

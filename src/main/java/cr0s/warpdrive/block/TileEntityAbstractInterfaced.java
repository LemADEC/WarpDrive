package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;

// OpenComputer API: https://github.com/MightyPirates/OpenComputers/tree/master-MC1.7.10/src/main/java/li/cil/oc/api

@Optional.InterfaceList({
	@Optional.Interface(iface = "li.cil.oc.api.network.Environment", modid = "OpenComputers"),
	@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft")
})
public abstract class TileEntityAbstractInterfaced extends TileEntityAbstractBase implements IPeripheral, Environment, cr0s.warpdrive.api.computer.IInterfaced {
	
	// Common computer properties
	protected String peripheralName = null;
	private String[] methodsArray = {};
	
	// String returned to LUA script in case of error
	public static final String COMPUTER_ERROR_TAG = "!ERROR!";
	
	// pre-loaded scripts support
	private volatile ManagedEnvironment OC_fileSystem = null;
	private volatile boolean CC_hasResource = false;
	private volatile boolean OC_hasResource = false;
	protected volatile List<String> CC_scripts = null;
	
	// OpenComputer specific properties
	protected boolean 	OC_enable = true;
	protected Node		OC_node = null;
	protected boolean	OC_addedToNetwork = false;
	
	// ComputerCraft specific properties
	protected final HashMap<Integer, IComputerAccess> connectedComputers = new HashMap<>();
	
	public TileEntityAbstractInterfaced() {
		super();
		addMethods(new String[] {
				"interfaced",
				"position",
				"version"
		});
	}
	
	// WarpDrive abstraction layer
	protected void addMethods(final String[] methodsToAdd) {
		if (methodsArray == null) {
			methodsArray = methodsToAdd;
		} else {
			int currentLength = methodsArray.length;
			methodsArray = Arrays.copyOf(methodsArray, methodsArray.length + methodsToAdd.length);
			for (String method : methodsToAdd) {
				methodsArray[currentLength] = method;
				currentLength++;
			}
		}
	}
	
	protected String getMethodName(final int methodIndex) {
		return methodsArray[methodIndex];
	}
	
	protected String getMethodName(final int methodIndex, final Object[] arguments) {
		final String methodName = methodsArray[methodIndex];
		
		if (WarpDrive.isDev && WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("LUA call to %s.%s(%s)", peripheralName, methodName, Commons.format(arguments)));
		}
		
		return methodName;
	}
	
	private boolean assetExist(final String resourcePath) {
		URL url = getClass().getResource(resourcePath);
		return (url != null);
	}
	
	// TileEntity overrides, notably for OpenComputer
	@Override
 	public void updateEntity() {
		super.updateEntity();
		
		if (WarpDriveConfig.isOpenComputersLoaded) {
			if (!OC_addedToNetwork && OC_enable) {
				OC_addedToNetwork = true;
				Network.joinOrCreateNetwork(this);
			}
		}
	}
	
	@Override
	public void validate() {
		if (WarpDriveConfig.isComputerCraftLoaded) {
			String CC_path = "/assets/" + WarpDrive.MODID.toLowerCase() + "/lua.ComputerCraft/" + peripheralName;
			CC_hasResource = assetExist(CC_path);
		}
		
		// deferred constructor so the derived class can finish it's initialization first
		if (WarpDriveConfig.isOpenComputersLoaded && OC_node == null) {
			OC_constructor();
		}
		super.validate();
	}
	
	@Override
	public void invalidate() {
		if (WarpDriveConfig.isOpenComputersLoaded) {
			if (OC_node != null) {
				OC_node.remove();
				OC_node = null;
			}
		}
		super.invalidate();
	}
	
	@Override
	public void onChunkUnload() {
		if (WarpDriveConfig.isOpenComputersLoaded) {
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
		if ( WarpDriveConfig.isOpenComputersLoaded
		  && FMLCommonHandler.instance().getEffectiveSide().isServer() ) {
			if (OC_node == null) {
				OC_constructor();
			}
			if (OC_node != null && OC_node.host() == this) {
				OC_node.load(tag.getCompoundTag("oc:node"));
			} else if (tag.hasKey("oc:node")) {
				WarpDrive.logger.error(this + " OC node failed to construct or wrong host, ignoring NBT node data read...");
			}
			if (OC_fileSystem != null && OC_fileSystem.node() != null) {
				OC_fileSystem.node().load(tag.getCompoundTag("oc:fs"));
			} else if (OC_hasResource) {
				WarpDrive.logger.error(this + " OC filesystem failed to construct or wrong node, ignoring NBT filesystem data read...");
			}
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		if (WarpDriveConfig.isOpenComputersLoaded) {
			if (OC_node != null && OC_node.host() == this) {
				final NBTTagCompound nbtNode = new NBTTagCompound();
				OC_node.save(nbtNode);
				tag.setTag("oc:node", nbtNode);
			}
			if (OC_fileSystem != null && OC_fileSystem.node() != null) {
				final NBTTagCompound nbtFileSystem = new NBTTagCompound();
				OC_fileSystem.node().save(nbtFileSystem);
				tag.setTag("oc:fs", nbtFileSystem);
			}
		}
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound nbtTagCompound) {
		nbtTagCompound = super.writeItemDropNBT(nbtTagCompound);
		nbtTagCompound.removeTag("oc:node");
		nbtTagCompound.removeTag("oc:fs");
		return nbtTagCompound;
	}
	
	@Override
	public int hashCode() {
		return (((((super.hashCode() + (worldObj == null ? 0 : worldObj.provider.dimensionId) << 4) + xCoord) << 4) + yCoord) << 4) + zCoord;
	}
	
	// Dirty cheap conversion methods
	@Optional.Method(modid = "OpenComputers")
	protected Object[] argumentsOCtoCC(Arguments args) {
		Object[] arguments = new Object[args.count()];
		int index = 0;
		for (Object arg:args) {
			if (args.isString(index)) {
				arguments[index] = args.checkString(index);
			} else {
				arguments[index] = arg;
			}
			index++;
		}
		return arguments;
	}
	
	// Declare type
	@Override
	public Object[] interfaced() {
		return new String[] { "I'm a WarpDrive computer interfaced tile entity." };
	}
	
	// Return block coordinates
	@Override
	public Object[] position() {
		return new Object[] { xCoord, yCoord, zCoord, "?", xCoord, yCoord, zCoord };
	}
	
	// Return version
	@Override
	public Object[] version() {
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info("Version is " + WarpDrive.VERSION + " isDev " + WarpDrive.isDev);
		}
		String[] strings = WarpDrive.VERSION.split("-");
		if (WarpDrive.isDev) {
			strings = strings[strings.length - 2].split("\\.");
		} else {
			strings = strings[strings.length - 1].split("\\.");
		}
		ArrayList<Integer> integers = new ArrayList<>(strings.length);
		for (String string : strings) {
			integers.add(Integer.parseInt(string));
		}
		return integers.toArray();
	}
	
	// ComputerCraft IPeripheral methods
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
		final String methodName = getMethodName(method);
		
		switch (methodName) {
		case "interfaced":
			return interfaced();
			
		case "position":
			return position();
			
		case "version":
			return version();
		}
		
		return null;
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public void attach(IComputerAccess computer) {
		int id = computer.getID();
		connectedComputers.put(id, computer);
		if (CC_hasResource && WarpDriveConfig.G_LUA_SCRIPTS != WarpDriveConfig.LUA_SCRIPTS_NONE) {
			try {
				final String modid = WarpDrive.MODID.toLowerCase();
				final String folderPeripheral = peripheralName.replace(modid, modid + "/");
				computer.mount("/" + modid           , ComputerCraftAPI.createResourceMount(WarpDrive.class, modid, "lua.ComputerCraft/common"));
				computer.mount("/" + folderPeripheral, ComputerCraftAPI.createResourceMount(WarpDrive.class, modid, "lua.ComputerCraft/" + peripheralName));
				computer.mount("/warpupdater"        , ComputerCraftAPI.createResourceMount(WarpDrive.class, modid, "lua.ComputerCraft/common/updater"));
				if (WarpDriveConfig.G_LUA_SCRIPTS == WarpDriveConfig.LUA_SCRIPTS_ALL) {
					for (String script : CC_scripts) {
						computer.mount("/" + script, ComputerCraftAPI.createResourceMount(WarpDrive.class, modid, "lua.ComputerCraft/" + peripheralName + "/" + script));
					}
				}
			} catch (Exception exception) {
				exception.printStackTrace();
				WarpDrive.logger.error("Failed to mount ComputerCraft scripts for " + peripheralName);
			}
		}
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
		return other.hashCode() == hashCode();
	}
	
	// Computer abstraction methods
	protected void sendEvent(String eventName, Object... arguments) {
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(this + " Sending event '" + eventName + "'");
		}
		if (WarpDriveConfig.isComputerCraftLoaded) {
			for(Map.Entry<Integer, IComputerAccess> integerIComputerAccessEntry : connectedComputers.entrySet()) {
				IComputerAccess comp = integerIComputerAccessEntry.getValue();
				comp.queueEvent(eventName, arguments);
			}
		}
		if (WarpDriveConfig.isOpenComputersLoaded) {
			if (OC_node != null && OC_node.network() != null) {
				if (arguments == null || arguments.length == 0) {
					OC_node.sendToReachable("computer.signal", eventName);
				} else {
					Object[] eventWithArguments = new Object[arguments.length + 1];
					eventWithArguments[0] = eventName;
					int index = 1;
					for (Object object : arguments) {
						eventWithArguments[index] = object;
						index++;
					}
					OC_node.sendToReachable("computer.signal", eventWithArguments);
				}
			}
		}
	}
	
	// OpenComputers methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] position(Context context, Arguments arguments) {
		return position();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] version(Context context, Arguments arguments) {
		return version();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] interfaced(Context context, Arguments arguments) {
		return interfaced();
	}
	
	@Optional.Method(modid = "OpenComputers")
	private void OC_constructor() {
		assert(OC_node == null);
		final String OC_path = "/assets/" + WarpDrive.MODID.toLowerCase() + "/lua.OpenComputers/" + peripheralName;
		OC_hasResource = assetExist(OC_path);
		OC_node = Network.newNode(this, Visibility.Network).withComponent(peripheralName).create();
		if (OC_node != null && OC_hasResource && WarpDriveConfig.G_LUA_SCRIPTS != WarpDriveConfig.LUA_SCRIPTS_NONE) {
			OC_fileSystem = FileSystem.asManagedEnvironment(FileSystem.fromClass(getClass(), WarpDrive.MODID.toLowerCase(), "lua.OpenComputers/" + peripheralName), peripheralName);
			((Component) OC_fileSystem.node()).setVisibility(Visibility.Network);
		}
	}
	
	@Override
	@Optional.Method(modid = "OpenComputers")
	public Node node() {
		return OC_node;
	}
	
	@Override
	@Optional.Method(modid = "OpenComputers")
	public void onConnect(Node node) {
		if (node.host() instanceof Context) {
			// Attach our file system to new computers we get connected to.
			// Note that this is also called for all already present computers
			// when we're added to an already existing network, so we don't
			// have to loop through the existing nodes manually.
			if (OC_fileSystem != null) {
				node.connect(OC_fileSystem.node());
			}
		}
	}
	
	@Override
	@Optional.Method(modid = "OpenComputers")
	public void onDisconnect(Node node) {
		if (OC_fileSystem != null) {
			if (node.host() instanceof Context) {
				// Disconnecting from a single computer
				node.disconnect(OC_fileSystem.node());
			} else if (node == OC_node) {
				// Disconnecting from the network
				OC_fileSystem.node().remove();
			}
		}
	}
	
	@Override
	@Optional.Method(modid = "OpenComputers")
	public void onMessage(Message message) {
		// nothing special
	}
}

package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.FunctionGet;
import cr0s.warpdrive.api.FunctionSetVector;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.WarpDriveConfig.EnumLUAscripts;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.item.ItemComponent;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.thread.EffectiveSide;

// OpenComputers API: https://github.com/MightyPirates/OpenComputers/tree/master-MC1.7.10/src/main/java/li/cil/oc/api

public abstract class TileEntityAbstractInterfaced extends TileEntityAbstractBase implements Environment, cr0s.warpdrive.api.computer.IInterfaced {
	
	// global constants
	private static final String CC_folderRootDataPack = "lua.computercraft";
	private static final String OC_folderRootDataPack = "lua.opencomputers";
	
	// global storage
	private static final ConcurrentHashMap<String, Object> CC_mountGlobals = new ConcurrentHashMap<>(32);
	private static final UpgradeSlot upgradeSlotComputerInterface = new UpgradeSlot("base.computer_interface",
	                                                                                ItemComponent.getItemStackNoCache(EnumComponentType.COMPUTER_INTERFACE, 1),
	                                                                                1);
	protected static final Object[] RESULT_YIELD = { "-yield LUA coroutine (not an actual result)-" };
	
	// Common computer properties
	protected String peripheralName = null;
	private String[] methodsArray = {};
	private boolean isAlwaysInterfaced = true;
	
	// String returned to LUA script in case of error
	public static final String COMPUTER_ERROR_TAG = "!ERROR!";
	
	// Cached resource presence to reduce disk access on chunk loading
	private static final HashMap<String, Boolean> hashAssetExist = new HashMap<>(128);
	
	// pre-loaded scripts support
	private volatile ManagedEnvironment OC_fileSystem = null;
	private volatile boolean CC_hasResource = false;
	private volatile String CC_folderPeripheralDataPack = null;
	private volatile String CC_folderPeripheralLUA      = null;
	private volatile boolean OC_hasResource = false;
	protected volatile List<String> CC_scripts = null;
	
	// OpenComputer specific properties
	private Node     OC_node = null;
	private boolean  OC_addedToNetwork = false;
	
	// ComputerCraft specific properties
	@CapabilityInject(IPeripheral.class)
	private static Capability<IPeripheral> CC_CAPABILITY_PERIPHERAL = null;
	private final ConcurrentHashMap<IComputerAccess, CopyOnWriteArraySet<String>> CC_connectedComputers = new ConcurrentHashMap<>();
	
	public TileEntityAbstractInterfaced(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		addMethods(new String[] {
				"isInterfaced",
				"getLocalPosition",
				"getTier",
				"getUpgrades",
				"getVersion",
		});
	}
	
	// WarpDrive abstraction layer
	protected void doRequireUpgradeToInterface() {
		assert isAlwaysInterfaced;
		
		isAlwaysInterfaced = false;
		registerUpgradeSlot(upgradeSlotComputerInterface);
	}
	
	@Override
	protected void onUpgradeChanged(@Nonnull final UpgradeSlot upgradeSlot, final int countNew, final boolean isAdded) {
		if (upgradeSlot.equals(upgradeSlotComputerInterface)) {
			if (isAdded) {
				if (WarpDriveConfig.isComputerCraftLoaded) {
					CC_mount();
				}
				if (WarpDriveConfig.isOpenComputersLoaded) {
					OC_constructor();
				}
			} else {
				if (WarpDriveConfig.isComputerCraftLoaded) {
					CC_unmount();
				}
				if (WarpDriveConfig.isOpenComputersLoaded) {
					OC_destructor();
				}
			}
		}
		super.onUpgradeChanged(upgradeSlot, countNew, isAdded);
	}
	
	@Override
	public boolean isInterfaceEnabled() {
		return isAlwaysInterfaced || getUpgradeCount(upgradeSlotComputerInterface) > 0;
	}
	
	protected void addMethods(final String[] methodsToAdd) {
		if (methodsArray == null) {
			methodsArray = methodsToAdd;
		} else {
			int currentLength = methodsArray.length;
			methodsArray = Arrays.copyOf(methodsArray, methodsArray.length + methodsToAdd.length);
			for (final String method : methodsToAdd) {
				methodsArray[currentLength] = method;
				currentLength++;
			}
		}
	}
	
	private boolean assetExist(final String resourcePath) {
		Boolean exist = hashAssetExist.get(resourcePath);
		if (exist == null) {
			final URL url = getClass().getResource(resourcePath);
			exist = (url != null);
			hashAssetExist.put(resourcePath, exist);
		}
		return exist;
	}
	
	// TileEntity overrides
	@Override
 	public void tick() {
		super.tick();
		
		if (WarpDriveConfig.isOpenComputersLoaded) {
			if (!OC_addedToNetwork && isInterfaceEnabled()) {
				OC_addedToNetwork = true;
				Network.joinOrCreateNetwork(this);
			}
		}
	}
	
	@Override
	public void validate() {
		if (WarpDriveConfig.isComputerCraftLoaded) {
			CC_folderPeripheralDataPack = CC_folderRootDataPack + "/" + peripheralName.replace(WarpDrive.MODID, "").toLowerCase();
			CC_folderPeripheralLUA      = peripheralName.replace(WarpDrive.MODID, WarpDrive.MODID + "/");
			final String CC_path = "/data/" + WarpDrive.MODID + "/" + CC_folderPeripheralDataPack;
			CC_hasResource = assetExist(CC_path);
		}
		
		// deferred constructor so the derived class can finish it's initialization first
		if (WarpDriveConfig.isOpenComputersLoaded && OC_node == null && isInterfaceEnabled()) {
			OC_constructor();
		}
		super.validate();
	}
	
	@Override
	public void remove() {
		if (WarpDriveConfig.isOpenComputersLoaded) {
			OC_destructor();
		}
		super.remove();
	}
	
	@Override
	public void onChunkUnloaded() {
		if (WarpDriveConfig.isOpenComputersLoaded) {
			OC_destructor();
		}
		super.onChunkUnloaded();
	}
	
	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability, @Nullable final Direction side) {
		if ( WarpDriveConfig.isComputerCraftLoaded
		  && isInterfaceEnabled()
		  && capability == CC_CAPABILITY_PERIPHERAL ) {
			LazyOptional<T> result = CC_getCapability(capability);
			if (result.isPresent()) {
				return result;
			}
		}
		return super.getCapability(capability, side);
	}
	
	private LazyOptional<?> CC_peripheralCapability = null;
	
	@Nonnull
	public <T> LazyOptional<T> CC_getCapability(@Nonnull final Capability<T> capability) {
		if (capability == CC_CAPABILITY_PERIPHERAL) {
			if (CC_peripheralCapability == null) {
				CC_peripheralCapability = LazyOptional.of(() -> new IDynamicPeripheral() {
					
					@Nonnull
					@Override
					public String getType() {
						return CC_getType();
					}
					
					@Override
					public void attach(@Nonnull IComputerAccess computerAccess) {
						CC_attach(computerAccess);
					}
					
					@Override
					public void detach(@Nonnull IComputerAccess computerAccess) {
						CC_detach(computerAccess);
					}
					
					@Nonnull
					@Override
					public String[] getMethodNames() {
						return CC_getMethodNames();
					}
					
					@Nonnull
					@Override
					public MethodResult callMethod(@Nonnull final IComputerAccess computerAccess, @Nonnull final ILuaContext context,
					                               final int method, @Nonnull final IArguments iArguments) throws LuaException {
						return CC_callMethod(computerAccess, context, method, iArguments);
					}
					
					@Nullable
					@Override
					public Object getTarget() {
						return getTileEntity();
					}
					
					@Override
					public boolean equals(@Nullable IPeripheral peripheral) {
						return peripheral == this
						    || ( peripheral != null
						      && peripheral.getTarget() == getTarget() );
					}
				});
			}
			return CC_peripheralCapability.cast();
		}
		return LazyOptional.empty();
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		if ( WarpDriveConfig.isOpenComputersLoaded
		  && EffectiveSide.get() == LogicalSide.SERVER
		  && isInterfaceEnabled() ) {
			if (OC_node == null) {
				OC_constructor();
			}
			/* TODO MC1.15 enable OC support once it's updated
			if (OC_node != null && OC_node.host() == this) {
				OC_node.load(tagCompound.getCompound("oc:node"));
			} else if (tagCompound.contains("oc:node")) {
				WarpDrive.logger.error(String.format("%s OC node failed to construct or wrong host, ignoring NBT node data read...",
				                                     this ));
			}
			if (OC_fileSystem != null && OC_fileSystem.node() != null) {
				OC_fileSystem.node().load(tagCompound.getCompound("oc:fs"));
			} else if (OC_hasResource) {
				WarpDrive.logger.error(String.format("%s OC filesystem failed to construct or wrong node, ignoring NBT filesystem data read...",
				                                     this ));
			}
			*/
		}
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		if (WarpDriveConfig.isOpenComputersLoaded) {
			if (OC_node != null && OC_node.host() == this) {
				final CompoundNBT nbtNode = new CompoundNBT();
				/* TODO MC1.15 enable OC support once it's updated
				OC_node.save(nbtNode);
				*/
				tagCompound.put("oc:node", nbtNode);
			}
			if (OC_fileSystem != null && OC_fileSystem.node() != null) {
				final CompoundNBT nbtFileSystem = new CompoundNBT();
				/* TODO MC1.15 enable OC support once it's updated
				OC_fileSystem.node().save(nbtFileSystem);
				*/
				tagCompound.put("oc:fs", nbtFileSystem);
			}
		}
		return tagCompound;
	}
	
	@Override
	public CompoundNBT writeItemDropNBT(CompoundNBT tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		tagCompound.remove("oc:node");
		tagCompound.remove("oc:fs");
		return tagCompound;
	}
	
	@Override
	public int hashCode() {
		return (((((super.hashCode() + (world == null ? 0 : world.getDimension().getType().getId()) << 4) + pos.getX()) << 4) + pos.getY()) << 4) + pos.getZ();
	}
	
	// Interface proxies are used to
	// - convert arguments,
	// - log LUA calls,
	// - block connection when missing the Computer interface upgrade
	// note: direct API calls remains possible without upgrade, as it's lore dependant
	@Nonnull
	protected Object[] OC_convertArgumentsAndLogCall(@Nonnull final Context context, @Nonnull final Arguments args) {
		final Object[] arguments = new Object[args.count()];
		int index = 0;
		for (final Object arg : args) {
			if (args.isString(index)) {
				arguments[index] = args.checkString(index);
			} else {
				arguments[index] = arg;
			}
			index++;
		}
		if (WarpDriveConfig.LOGGING_LUA) {
			final String methodName = Commons.getMethodName(1);
			WarpDrive.logger.info(String.format("[OC] LUA call %s from %s to %s.%s(%s)",
			                                    Commons.format(world, pos),
			                                    context.node().address(),
			                                    peripheralName, methodName, Commons.format(arguments)));
		}
		if (!isInterfaceEnabled()) {
			throw new RuntimeException("Missing Computer interface upgrade.");
		}
		return arguments;
	}
	
	private String CC_getMethodNameAndLogCall(@Nonnull final IComputerAccess computerAccess, final int methodIndex, @Nonnull final Object[] arguments) {
		final String methodName = methodsArray[methodIndex];
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("[CC] LUA call %s from %d:%s to %s.%s(%s)",
			                                    Commons.format(world, pos),
			                                    computerAccess.getID(), computerAccess.getAttachmentName(),
			                                    peripheralName, methodName, Commons.format(arguments)));
		}
		if ( !isInterfaceEnabled()
		  && !"isInterfaced".equals(methodName) ) {
			throw new RuntimeException("Missing Computer interface upgrade.");
		}
		return methodName;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] isInterfaced() {
		if (isInterfaceEnabled()) {
			return new Object[] { true, "I'm a WarpDrive computer interfaced tile entity." };
		} else {
			return new Object[] { false, "Missing Computer interface upgrade." };
		}
	}
	
	@Override
	public Object[] getLocalPosition() {
		return new Object[] { pos.getX(), pos.getY(), pos.getZ() };
	}
	
	@Override
	public Object[] getTier() {
		return new Object[] { enumTier.getIndex(), enumTier.getName() };
	}
	
	@Override
	public Object[] getUpgrades() {
		return new Object[] { isUpgradeable(), Commons.removeFormatting( getUpgradeStatus(false).getString() ) };
	}
	
	@Override
	public Integer[] getVersion() {
		return WarpDrive.MOD_VERSION_NUMBERS;
	}
	
	// Common WarpDrive API
	public boolean computer_isConnected() {
		if ( WarpDriveConfig.isComputerCraftLoaded
		  && !CC_connectedComputers.isEmpty() ) {
			return true;
		}
		if ( WarpDriveConfig.isOpenComputersLoaded
		  && OC_node != null ) {
			final Iterable<Node> iterableNodes = OC_node.reachableNodes();
			return iterableNodes.iterator().hasNext();
		}
		return false;
	}
	
	protected VectorI computer_getVectorI(final VectorI vDefault, @Nonnull final Object[] arguments) {
		try {
			if (arguments.length == 3) {
				final int x = Commons.toInt(arguments[0]);
				final int y = Commons.toInt(arguments[1]);
				final int z = Commons.toInt(arguments[2]);
				return new VectorI(x, y, z);
			}
		} catch (final NumberFormatException exception) {
			// ignore
		}
		return vDefault;
	}
	
	@Nonnull
	protected Object[] computer_getOrSetVector3(@Nonnull final FunctionGet<Vector3> getVector,
	                                            @Nonnull final FunctionSetVector<Float> setVector,
	                                            @Nonnull final TileEntityAbstractBase.UpgradeSlot upgradeSlot,
	                                            final Object[] arguments) {
		if ( arguments != null
		  && arguments.length > 0
		  && arguments[0] != null ) {
			try {
				if (arguments.length == 1) {
					final float value = Commons.toFloat(arguments[0]);
					setVector.apply(value, value, value);
				} else if (arguments.length == 2) {
					final float valueXZ = Commons.toFloat(arguments[0]);
					final float valueY = Commons.toFloat(arguments[1]);
					setVector.apply(valueXZ, valueY, valueXZ);
				} else if (arguments.length == 3) {
					final float valueX = Commons.toFloat(arguments[0]);
					final float valueY = Commons.toFloat(arguments[1]);
					final float valueZ = Commons.toFloat(arguments[2]);
					setVector.apply(valueX, valueY, valueZ);
				}
			} catch (final Exception exception) {
				final String message = String.format("Float expected for all arguments %s",
				                                     Arrays.toString(arguments));
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on %s: %s",
					                                     this, setVector, message));
				}
				final Vector3 v3Actual = getVector.apply();
				return new Object[] { v3Actual.x, v3Actual.y, v3Actual.z, message };
			}
		}
		final Vector3 v3Actual = getVector.apply();
		if (hasUpgrade(upgradeSlot)) {
			return new Double[] { v3Actual.x, v3Actual.y, v3Actual.z };
		} else {
			return new Object[] { v3Actual.x, v3Actual.y, v3Actual.z, "Missing " + upgradeSlot.itemStack.getDisplayName() };
		}
	}
	
	protected UUID computer_getUUID(final UUID uuidDefault, @Nonnull final Object[] arguments) {
		try {
			if (arguments.length == 1 && arguments[0] != null) {
				if (arguments[0] instanceof UUID) {
					return (UUID) arguments[0];
				}
				if (arguments[0] instanceof String) {
					return UUID.fromString((String) arguments[0]);
				}
			}
		} catch (final IllegalArgumentException exception) {
			// ignore
		}
		return uuidDefault;
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Nonnull
	public String CC_getType() {
		assert peripheralName != null;
		return peripheralName;
	}
	
	@Nonnull
	public String[] CC_getMethodNames() {
		return methodsArray;
	}
	
	@Nonnull
	final public MethodResult CC_callMethod(@Nonnull final IComputerAccess computerAccess, @Nonnull final ILuaContext context,
	                                        final int method, @Nonnull final IArguments iArguments) throws LuaException {
		final Object[] objectArguments = iArguments.getAll();
		final String methodName = CC_getMethodNameAndLogCall(computerAccess, method, objectArguments);
		
		// we separate the proxy from the logs so children can override the proxy without having to handle the logs themselves
		// we split the interface to reuse the code when yielding
		return CC_callMethod(computerAccess, methodName, objectArguments);
	}
	
	@Nonnull
	private MethodResult CC_callMethod(@Nonnull final IComputerAccess computerAccess,
	                                   final String methodName, @Nonnull final Object[] arguments) throws LuaException {
		try {
			final Object[] result = CC_callMethod(methodName, arguments);
			if (WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.info(String.format("[CC] LUA call is returning %s",
				                                    Commons.format(result)) );
			}
			return result == RESULT_YIELD ? MethodResult.yield(null, arguments2 -> CC_callMethod(computerAccess, methodName, arguments2) )
			                              : MethodResult.of(result);
		} catch (final Exception exception) {
			if ( WarpDriveConfig.LOGGING_LUA
			  || Commons.throttleMe("LUA exception") ) {
				exception.printStackTrace(WarpDrive.printStreamError);
			}
			
			throw new LuaException(String.format("Internal exception %s from %d:%s to %s.%s(%s)\nCheck server logs for details.",
			                                     Commons.format(world, pos),
			                                     computerAccess.getID(), computerAccess.getAttachmentName(),
			                                     peripheralName, methodName, Commons.format(arguments) ));
		}
	}
	
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "isInterfaced":
			return isInterfaced();
			
		case "getLocalPosition":
			return getLocalPosition();
			
		case "getTier":
			return getTier();
			
		case "getUpgrades":
			return getUpgrades();
			
		case "getVersion":
			return getVersion();
			
		default:
			return null;
		}
	}
	
	public void CC_attach(@Nonnull final IComputerAccess computerAccess) {
		if (CC_connectedComputers.containsKey(computerAccess)) {
			WarpDrive.logger.error(String.format("[CC] Already attached %s %s with %d:%s, ignoring...",
			                                     peripheralName, Commons.format(world, pos),
			                                     computerAccess.getID(), computerAccess.getAttachmentName() ));
			return;
		}
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("[CC] Attaching %s %s with %d:%s",
			                                    peripheralName, Commons.format(world, pos),
			                                    computerAccess.getID(), computerAccess.getAttachmentName() ));
		}
		final CopyOnWriteArraySet<String> mountedLocations = new CopyOnWriteArraySet<>();
		CC_connectedComputers.put(computerAccess, mountedLocations);
		if (isInterfaceEnabled()) {
			CC_mount(computerAccess, mountedLocations);
		}
	}
	
	private void CC_mount() {
		for (final Entry<IComputerAccess, CopyOnWriteArraySet<String>> entry : CC_connectedComputers.entrySet()) {
			CC_mount(entry.getKey(), entry.getValue());
		}
	}
	
	private void CC_mount(@Nonnull final IComputerAccess computerAccess, @Nonnull final CopyOnWriteArraySet<String> mountedLocations) {
		if (CC_hasResource && WarpDriveConfig.G_LUA_SCRIPTS != EnumLUAscripts.NONE) {
			try {
				CC_mount(computerAccess, mountedLocations, CC_folderRootDataPack + "/common", "/" + WarpDrive.MODID);
				
				CC_mount(computerAccess, mountedLocations, CC_folderPeripheralDataPack, "/" + CC_folderPeripheralLUA);
				
				if (WarpDriveConfig.G_LUA_SCRIPTS == EnumLUAscripts.ALL) {
					for (final String script : CC_scripts) {
						CC_mount(computerAccess, mountedLocations, CC_folderPeripheralDataPack + "/" + script, "/" + script);
					}
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to mount ComputerCraft scripts for %s %s, isFirstTick %s",
				                                     peripheralName,
				                                     Commons.format(world, pos),
				                                     isFirstTick()));
			}
		}
	}
	private void CC_mount(@Nonnull final IComputerAccess computerAccess, @Nonnull final CopyOnWriteArraySet<String> mountedLocations,
	                      @Nonnull final String pathAsset, @Nonnull final String pathLUA) {
		IMount mountCommon = (IMount) CC_mountGlobals.get(pathAsset);
		if (mountCommon == null) {
			mountCommon = ComputerCraftAPI.createResourceMount(WarpDrive.MODID, pathAsset);
			assert mountCommon != null;
			CC_mountGlobals.put(pathAsset, mountCommon);
		}
		final String pathLUA_actual = computerAccess.mount(pathLUA, mountCommon);
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("[CC] %s Mounted %s to %s as %s",
			                                    Commons.format(world, pos),
			                                    pathAsset, pathLUA, pathLUA_actual));
		}
		if (pathLUA_actual != null) {
			mountedLocations.add(pathLUA_actual);
		}
	}
	private void CC_unmount() {
		for (final Entry<IComputerAccess, CopyOnWriteArraySet<String>> entry : CC_connectedComputers.entrySet()) {
			CC_unmount(entry.getKey(), entry.getValue());
		}
	}
	private void CC_unmount(@Nonnull final IComputerAccess computerAccess) {
		final CopyOnWriteArraySet<String> mountedLocations = CC_connectedComputers.get(computerAccess);
		if (mountedLocations != null) {
			CC_unmount(computerAccess, mountedLocations);
		}
	}
	private void CC_unmount(@Nonnull final IComputerAccess computerAccess, @Nonnull final CopyOnWriteArraySet<String> mountedLocation) {
		if (CC_hasResource && WarpDriveConfig.G_LUA_SCRIPTS != EnumLUAscripts.NONE) {
			try {
				for (final String pathLUA : mountedLocation) {
					if (WarpDriveConfig.LOGGING_LUA) {
						WarpDrive.logger.info(String.format("[CC] %s Unmounting %s",
						                                    Commons.format(world, pos),
						                                    pathLUA ));
					}
					computerAccess.unmount(pathLUA);
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to unmount ComputerCraft scripts for %s %s, isFirstTick %s",
				                                     peripheralName,
				                                     Commons.format(world, pos),
				                                     isFirstTick()));
			} finally {
				mountedLocation.clear();
			}
		}
	}
	
	public void CC_detach(@Nonnull final IComputerAccess computerAccess) {
		if (!CC_connectedComputers.containsKey(computerAccess)) {
			WarpDrive.logger.error(String.format("[CC] Already detached %s %s from %d:%s, ignoring...",
			                                     peripheralName, Commons.format(world, pos),
			                                     computerAccess.getID(), computerAccess.getAttachmentName() ));
			return;
		}
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("[CC] Detaching %s %s from %d:%s",
			                                    peripheralName, Commons.format(world, pos),
			                                    computerAccess.getID(), computerAccess.getAttachmentName() ));
		}
		if (isInterfaceEnabled()) {
			CC_unmount(computerAccess);
		}
		CC_connectedComputers.remove(computerAccess);
	}
	
	// Computer abstraction methods
	protected void sendEvent(final String eventName, final Object... arguments) {
		if (!isInterfaceEnabled()) {
			return;
		}
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(this + " Sending event '" + eventName + "'");
		}
		if (WarpDriveConfig.isComputerCraftLoaded) {
			for (final IComputerAccess computerAccess : CC_connectedComputers.keySet()) {
				computerAccess.queueEvent(eventName, arguments);
			}
		}
		if ( WarpDriveConfig.isOpenComputersLoaded
		  && OC_node != null
		  && OC_node.network() != null ) {
			if (arguments == null || arguments.length == 0) {
				OC_node.sendToReachable("computer.signal", eventName);
			} else {
				final Object[] eventWithArguments = new Object[arguments.length + 1];
				eventWithArguments[0] = eventName;
				int index = 1;
				for (final Object object : arguments) {
					eventWithArguments[index] = object;
					index++;
				}
				OC_node.sendToReachable("computer.signal", eventWithArguments);
			}
		}
	}
	
	// OpenComputers methods
	@Callback(direct = true)
	public Object[] isInterfaced(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return isInterfaced();
	}
	
	@Callback(direct = true)
	public Object[] getLocalPosition(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getLocalPosition();
	}
	
	@Callback(direct = true)
	public Object[] getUpgrades(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getUpgrades();
	}
	
	@Callback(direct = true)
	public Object[] getTier(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getTier();
	}
	
	@Callback(direct = true)
	public Object[] getVersion(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getVersion();
	}
	
	private void OC_constructor() {
		assert OC_node == null;
		String folderPeripheralDataPack = OC_folderRootDataPack + "/" + peripheralName.replace(WarpDrive.MODID, "").toLowerCase();
		final String OC_path = "/data/" + WarpDrive.MODID + "/" + folderPeripheralDataPack;
		OC_hasResource = assetExist(OC_path);
		OC_node = Network.newNode(this, Visibility.Network).withComponent(peripheralName).create();
		if (OC_node != null && OC_hasResource && WarpDriveConfig.G_LUA_SCRIPTS != EnumLUAscripts.NONE) {
			OC_fileSystem = FileSystem.asManagedEnvironment(FileSystem.fromClass(getClass(), WarpDrive.MODID, folderPeripheralDataPack), peripheralName);
			((Component) OC_fileSystem.node()).setVisibility(Visibility.Network);
		}
		// note: we can't join the network right away, it's postponed to next tick
	}
	
	private void OC_destructor() {
		if (OC_node != null) {
			if (OC_fileSystem != null) {
				OC_fileSystem.node().remove();
				OC_fileSystem = null;
			}
			OC_node.remove();
			OC_node = null;
			OC_addedToNetwork = false;
		}
	}
	
	@Override
	public Node node() {
		return OC_node;
	}
	
	@Override
	public void onConnect(@Nonnull final Node node) {
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
	public void onDisconnect(@Nonnull final Node node) {
		if (OC_fileSystem != null) {
			if (node.host() instanceof Context) {
				// Disconnecting from a single computer
				node.disconnect(OC_fileSystem.node());
			} else if (node.equals(OC_node)) {
				// Disconnecting from the network
				OC_fileSystem.node().remove();
			}
		}
	}
	
	@Override
	public void onMessage(@Nonnull final Message message) {
		// nothing special
	}
}
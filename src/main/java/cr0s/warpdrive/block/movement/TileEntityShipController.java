package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.computer.IShipController;
import cr0s.warpdrive.block.TileEntityAbstractInterfaced;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnumShipControllerCommand;
import cr0s.warpdrive.data.VectorI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;

public class TileEntityShipController extends TileEntityAbstractInterfaced implements IShipController {
	
	// persistent properties
	private int moveFront = 0;
	private int moveUp = 0;
	private int moveRight = 0;
	private byte rotationSteps = 0;
	private EnumShipControllerCommand command = EnumShipControllerCommand.IDLE;
	protected boolean isEnabled = false;
	private String nameTarget = "";
	
	// Dimensions
	private int front, right, up;
	private int back, left, down;
	private boolean isPendingScan = false;
	
	// Player attaching
	public final ArrayList<String> players = new ArrayList<>();
	
	// computed properties
	private final int updateInterval_ticks = 20 * WarpDriveConfig.SHIP_CONTROLLER_UPDATE_INTERVAL_SECONDS;
	private int updateTicks = updateInterval_ticks;
	private int bootTicks = 20;
	
	private WeakReference<TileEntityShipCore> tileEntityShipCoreWeakReference = null;
	
	public TileEntityShipController() {
		super();
		
		peripheralName = "warpdriveShipController";
		addMethods(new String[] {
				"isAssemblyValid",
				"getOrientation",
				"isInSpace",
				"isInHyperspace",
				"shipName",
				"dim_positive",
				"dim_negative",
				"energy",
				"getAttachedPlayers",
				"command",
				"enable",
				"getShipSize",
				"getMaxJumpDistance",
				"movement",
				"rotationSteps",
				"targetName",
				"getEnergyRequired",
		});
		CC_scripts = Collections.singletonList("startup");
	}
    
    @Override
    public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		// accelerate update ticks during boot
		if (bootTicks > 0) {
			bootTicks--;
			if (tileEntityShipCoreWeakReference == null) {
				updateTicks = 1;
			}
		}
		updateTicks--;
		if (updateTicks <= 0) {
			updateTicks = updateInterval_ticks;
			
			final TileEntityShipCore tileEntityShipCore = findCoreBlock();
			if (tileEntityShipCore != null) {
				if ( tileEntityShipCoreWeakReference == null
				  || tileEntityShipCore != tileEntityShipCoreWeakReference.get() ) {
					tileEntityShipCoreWeakReference = new WeakReference<>(tileEntityShipCore);
				}
				
				if (command.getCode() != getBlockMetadata()) {
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, command.getCode(), 1 + 2);  // Activated
				}
				if ( isPendingScan
				  && tileEntityShipCore.isAttached(this) ) {
					isPendingScan = false;
					final StringBuilder reason = new StringBuilder();
					try {
						if (!tileEntityShipCore.validateShipSpatialParameters(this, reason)) {
							tileEntityShipCore.messageToAllPlayersOnShip(reason.toString());
						}
					} catch (Exception exception) {
						exception.printStackTrace();
						WarpDrive.logger.info(this + " Exception in validateShipSpatialParameters, reason: " + reason.toString());
					}
				}
			} else if (getBlockMetadata() != 0) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 1 + 2);  // Inactive
			}
		}
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		players.clear();
		if (tagCompound.hasKey("players", NBT.TAG_STRING)) {// legacy up to 1.3.30
			final String namePlayers_tag = tagCompound.getString("players");
			final String[] namePlayers_table = namePlayers_tag.split("\\|");
			for (final String namePlayer : namePlayers_table) {
				if (!namePlayer.isEmpty()) {
					players.add(namePlayer);
				}
			}
		} else {
			final NBTTagList tagListPlayers = tagCompound.getTagList("players", Constants.NBT.TAG_STRING);
			for(int index = 0; index < tagListPlayers.tagCount(); index++) {
				final String namePlayer = tagListPlayers.getStringTagAt(index);
				players.add(namePlayer);
			}
		}
		
		isEnabled = tagCompound.hasKey("isEnabled") && tagCompound.getBoolean("isEnabled");
		setCommand(tagCompound.getString("command"));
		setFront(tagCompound.getInteger("front"));
		setRight(tagCompound.getInteger("right"));
		setUp(tagCompound.getInteger("up"));
		setBack(tagCompound.getInteger("back"));
		setLeft(tagCompound.getInteger("left"));
		setDown(tagCompound.getInteger("down"));
		setMovement(
			tagCompound.getInteger("moveFront"),
			tagCompound.getInteger("moveUp"),
			tagCompound.getInteger("moveRight") );
		setRotationSteps(tagCompound.getByte("rotationSteps"));
		nameTarget = tagCompound.getString("nameTarget");
	}
	
	@Override
	public void writeToNBT(final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		
		final NBTTagList tagListPlayers = new NBTTagList();
		for (final String namePlayer : players) {
			NBTTagString tagStringPlayer = new NBTTagString(namePlayer);
			tagListPlayers.appendTag(tagStringPlayer);
		}
		tagCompound.setTag("players", tagListPlayers);
		
		tagCompound.setBoolean("isEnabled", isEnabled);
		tagCompound.setString("command", command.name());
		tagCompound.setInteger("front", front);
		tagCompound.setInteger("right", right);
		tagCompound.setInteger("up", up);
		tagCompound.setInteger("back", back);
		tagCompound.setInteger("left", left);
		tagCompound.setInteger("down", down);
		tagCompound.setInteger("moveFront", moveFront);
		tagCompound.setInteger("moveUp", moveUp);
		tagCompound.setInteger("moveRight", moveRight);
		tagCompound.setByte("rotationSteps", rotationSteps);
		tagCompound.setString("nameTarget", nameTarget);
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound nbtTagCompound) {
		nbtTagCompound = super.writeItemDropNBT(nbtTagCompound);
		
		nbtTagCompound.removeTag("players");
		
		nbtTagCompound.removeTag("isEnabled");
		nbtTagCompound.removeTag("command");
		nbtTagCompound.removeTag("front");
		nbtTagCompound.removeTag("right");
		nbtTagCompound.removeTag("up");
		nbtTagCompound.removeTag("back");
		nbtTagCompound.removeTag("left");
		nbtTagCompound.removeTag("down");
		nbtTagCompound.removeTag("moveFront");
		nbtTagCompound.removeTag("moveUp");
		nbtTagCompound.removeTag("moveRight");
		nbtTagCompound.removeTag("rotationSteps");
		nbtTagCompound.removeTag("nameTarget");
		return nbtTagCompound;
	}
	
	@Override
	public String getStatus() {
		return super.getStatus()
				+ StatCollector.translateToLocalFormatted("warpdrive.ship.attachedPlayers",
						getAttachedPlayersList());
	}
	
	private TileEntityShipCore findCoreBlock() {
		TileEntity tileEntity;
		
		tileEntity = worldObj.getTileEntity(xCoord + 1, yCoord, zCoord);
		if (tileEntity != null && tileEntity instanceof TileEntityShipCore) {
			return (TileEntityShipCore) tileEntity;
		}
		
		tileEntity = worldObj.getTileEntity(xCoord - 1, yCoord, zCoord);
		if (tileEntity != null && tileEntity instanceof TileEntityShipCore) {
			return (TileEntityShipCore) tileEntity;
		}
		
		tileEntity = worldObj.getTileEntity(xCoord, yCoord, zCoord + 1);
		if (tileEntity != null && tileEntity instanceof TileEntityShipCore) {
			return (TileEntityShipCore) tileEntity;
		}
		
		tileEntity = worldObj.getTileEntity(xCoord, yCoord, zCoord - 1);
		if (tileEntity != null && tileEntity instanceof TileEntityShipCore) {
			return (TileEntityShipCore) tileEntity;
		}
		
		return null;
	}
	
	protected void cooldownDone() {
		sendEvent("shipCoreCooldownDone");
	}
	
	protected String attachPlayer(final EntityPlayer entityPlayer) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		for (int i = 0; i < players.size(); i++) {
			final String name = players.get(i);
			
			if (entityPlayer.getDisplayName().equals(name)) {
				players.remove(i);
				return StatCollector.translateToLocalFormatted("warpdrive.guide.prefix",
				                                               getBlockType().getLocalizedName())
				       + StatCollector.translateToLocalFormatted("warpdrive.ship.playerDetached",
				                                                 tileEntityShipCore != null && !tileEntityShipCore.shipName.isEmpty() ? tileEntityShipCore.shipName : "-",
				                                                 getAttachedPlayersList());
			}
		}
		
		entityPlayer.attackEntityFrom(DamageSource.generic, 1);
		players.add(entityPlayer.getDisplayName());
		return StatCollector.translateToLocalFormatted("warpdrive.guide.prefix",
		                                               getBlockType().getLocalizedName())
		       + StatCollector.translateToLocalFormatted("warpdrive.ship.playerAttached",
		                                                 tileEntityShipCore != null && !tileEntityShipCore.shipName.isEmpty() ? tileEntityShipCore.shipName : "-",
		                                                 getAttachedPlayersList());
	}
	
	protected String getAttachedPlayersList() {
		if (players.isEmpty()) {
			return "<nobody>";
		}
		
		final StringBuilder list = new StringBuilder("");
		
		for (int i = 0; i < players.size(); i++) {
			final String nick = players.get(i);
			list.append(nick).append(((i == players.size() - 1) ? "" : ", "));
		}
		
		return list.toString();
	}
	
	protected int getFront() {
		return front;
	}
	
	private void setFront(int front) {
		this.front = front;
		isPendingScan = true;
	}
	
	protected int getRight() {
		return right;
	}
	
	private void setRight(int right) {
		this.right = right;
		isPendingScan = true;
	}
	
	protected int getUp() {
		return up;
	}
	
	private void setUp(int up) {
		this.up = up;
		isPendingScan = true;
	}
	
	protected int getBack() {
		return back;
	}
	
	private void setBack(int back) {
		this.back = back;
		isPendingScan = true;
	}
	
	protected int getLeft() {
		return left;
	}
	
	private void setLeft(int left) {
		this.left = left;
		isPendingScan = true;
	}
	
	protected int getDown() {
		return down;
	}
	
	private void setDown(int down) {
		this.down = down;
		isPendingScan = true;
	}
	
	public EnumShipControllerCommand getCommand() {
		return command;
	}
	
	private void setCommand(final String command) {
		for(EnumShipControllerCommand enumShipControllerCommand : EnumShipControllerCommand.values()) {
			if (enumShipControllerCommand.name().equalsIgnoreCase(command)) {
				this.command = enumShipControllerCommand;
				markDirty();
				if (WarpDriveConfig.LOGGING_LUA && hasWorldObj()) {
					WarpDrive.logger.info(String.format("%s Command set to %s (%d)",
					                                    this, this.command, this.command.getCode()));
				}
			}
		}
	}
	
	protected void commandDone(final boolean success, final String reason) {
		isEnabled = false;
		command = EnumShipControllerCommand.IDLE;
		if (!success) {
			final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
			if (tileEntityShipCore != null) {
				tileEntityShipCore.messageToAllPlayersOnShip(reason);
			}
		}
	}
	
	protected VectorI getMovement() {
		return new VectorI(moveFront, moveUp, moveRight);
	}
	
	private void setMovement(final int moveFront, final int moveUp, final int moveRight) {
		this.moveFront = moveFront;
		this.moveUp = moveUp;
		this.moveRight = moveRight;
		markDirty();
		if (WarpDriveConfig.LOGGING_LUA && hasWorldObj()) {
			WarpDrive.logger.info(String.format("%s Movement set to %d front, %d up, %d right",
			                                    this, this.moveFront, this.moveUp, this.moveRight));
		}
	}
	
	protected byte getRotationSteps() {
		return rotationSteps;
	}
	
	private void setRotationSteps(final byte rotationSteps) {
		this.rotationSteps = (byte) ((rotationSteps + 4) % 4);
		markDirty();
		if (WarpDriveConfig.LOGGING_LUA && hasWorldObj()) {
			WarpDrive.logger.info(String.format("%s Movement set to %d rotation steps",
			                                    this, this.rotationSteps));
		}
	}
	
	protected String getTargetName() {
		return nameTarget;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] position() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		
		return new Object[] { tileEntityShipCore.xCoord, tileEntityShipCore.yCoord, tileEntityShipCore.zCoord, "?", tileEntityShipCore.xCoord, tileEntityShipCore.yCoord, tileEntityShipCore.zCoord };
	}
	
	@Override
	public Object[] isAssemblyValid() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "No core detected" };
		}
		return new Object[] { true, "ok" };
	}
	
	@Override
	public Object[] getOrientation() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore != null) {
			return new Object[] { tileEntityShipCore.facing.offsetX, 0, tileEntityShipCore.facing.offsetZ };
		}
		return null;
	}
	
	@Override
	public Object[] isInSpace() {
		return new Boolean[] { CelestialObjectManager.isInSpace(worldObj, xCoord, zCoord) };
	}
	
	@Override
	public Object[] isInHyperspace() {
		return new Boolean[] { CelestialObjectManager.isInHyperspace(worldObj, xCoord, zCoord) };
	}
	
	@Override
	public Object[] shipName(Object[] arguments) {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? findCoreBlock() : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		if (arguments.length == 1) {
			final String shipNamePrevious = tileEntityShipCore.shipName;
			tileEntityShipCore.shipName = ((String) arguments[0]).replace("/", "").replace(".", "").replace("\\", ".");
			if ( tileEntityShipCore.shipName == null
			  || !tileEntityShipCore.shipName.equals(shipNamePrevious) ) {
				WarpDrive.logger.info(String.format("Ship renamed from '%s' to '%s' with player(s) %s",
				                                    shipNamePrevious == null ? "-null-" : shipNamePrevious,
				                                    tileEntityShipCore.shipName,
				                                    tileEntityShipCore.getAllPlayersOnShip()));
			}
		}
		return new Object[] { tileEntityShipCore.shipName };
	}
	
	@Override
	public Object[] dim_positive(Object[] arguments) {
		try {
			if (arguments.length == 3) {
				int argInt0, argInt1, argInt2;
				argInt0 = Commons.clamp(0, WarpDriveConfig.SHIP_MAX_SIDE_SIZE, Math.abs(Commons.toInt(arguments[0])));
				argInt1 = Commons.clamp(0, WarpDriveConfig.SHIP_MAX_SIDE_SIZE, Math.abs(Commons.toInt(arguments[1])));
				argInt2 = Commons.clamp(0, WarpDriveConfig.SHIP_MAX_SIDE_SIZE, Math.abs(Commons.toInt(arguments[2])));
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.info(this + " Positive dimensions set to front " + argInt0 + ", right " + argInt1 + ", up " + argInt2);
				}
				setFront(argInt0);
				setRight(argInt1);
				setUp(Math.min(255 - yCoord, argInt2));
			}
		} catch (Exception exception) {
			return new Integer[] { getFront(), getRight(), getUp() };
		}
		
		return new Integer[] { getFront(), getRight(), getUp() };
	}
	
	@Override
	public Object[] dim_negative(Object[] arguments) {
		try {
			if (arguments.length == 3) {
				int argInt0, argInt1, argInt2;
				argInt0 = Commons.clamp(0, WarpDriveConfig.SHIP_MAX_SIDE_SIZE, Math.abs(Commons.toInt(arguments[0])));
				argInt1 = Commons.clamp(0, WarpDriveConfig.SHIP_MAX_SIDE_SIZE, Math.abs(Commons.toInt(arguments[1])));
				argInt2 = Commons.clamp(0, WarpDriveConfig.SHIP_MAX_SIDE_SIZE, Math.abs(Commons.toInt(arguments[2])));
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.info(this + " Negative dimensions set to back " + argInt0 + ", left " + argInt1 + ", down " + argInt2);
				}
				setBack(argInt0);
				setLeft(argInt1);
				setDown(Math.min(yCoord, argInt2));
			}
		} catch (Exception exception) {
			return new Integer[] { getBack(), getLeft(), getDown() };
		}
		
		return new Integer[] { getBack(), getLeft(), getDown() };
	}
	
	@Override
	public Object[] getAttachedPlayers() {
		final StringBuilder list = new StringBuilder();
		
		if (!players.isEmpty()) {
			for (int i = 0; i < players.size(); i++) {
				final String nick = players.get(i);
				list.append(nick).append((i == players.size() - 1) ? "" : ",");
			}
		}
		
		return new Object[] { list.toString(), players.toArray() };
	}
	
	@Override
	public Object[] energy() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return tileEntityShipCore.energy();
	}
	
	@Override
	public Object[] command(Object[] arguments) {
		try {
			if (arguments.length == 1) {
				setCommand(arguments[0].toString());
			}
		} catch (Exception exception) {
			return new Object[] { command.toString() };
		}
		
		return new Object[] { command.toString() };
	}
	
	@Override
	public Object[] enable(Object[] arguments) {
		if (arguments.length == 1) {
			isEnabled = Commons.toBool(arguments[0]);
			if (WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.info(this + " enable(" + isEnabled + ")");
			}
		}
		return new Object[] { isEnabled };
	}
	
	@Override
	public Object[] getShipSize() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return null;
		}
		return new Object[] { tileEntityShipCore.shipMass, tileEntityShipCore.shipVolume };
	}
	
	@Override
	public Object[] movement(Object[] arguments) {
		try {
			if (arguments.length == 3) {
				setMovement(Commons.toInt(arguments[0]), Commons.toInt(arguments[1]), Commons.toInt(arguments[2]));
			}
		} catch (Exception exception) {
			return new Integer[] { moveFront, moveUp, moveRight };
		}
		
		return new Integer[] { moveFront, moveUp, moveRight };
	}
	
	@Override
	public Object[] getMaxJumpDistance() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "No ship core detected" };
		}
		
		final StringBuilder reason = new StringBuilder();
		final int maximumDistance_blocks = tileEntityShipCore.getMaxJumpDistance(this, command, reason);
		if (maximumDistance_blocks < 0) {
			return new Object[] { false, reason.toString() };
		}
		return new Object[] { true, maximumDistance_blocks };
	}
	
	@Override
	public Object[] rotationSteps(Object[] arguments) {
		try {
			if (arguments.length == 1) {
				setRotationSteps((byte) Commons.toInt(arguments[0]));
			}
		} catch (Exception exception) {
			return new Integer[] { (int) rotationSteps };
		}
		
		return new Integer[] { (int) rotationSteps };
	}
	
	@Override
	public Object[] targetName(Object[] arguments) {
		if (arguments.length == 1) {
			this.nameTarget = (String) arguments[0];
		}
		return new Object[] { nameTarget };
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		if (tileEntityShipCore == null) {
			return new Object[] { false, "No ship core detected" };
		}
		
		final StringBuilder reason = new StringBuilder();
		final int energyRequired = tileEntityShipCore.getEnergyRequired(this, command, reason);
		if (energyRequired < 0) {
			return new Object[] { false, reason.toString() };
		}
		return new Object[] { true, energyRequired };
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] isAssemblyValid(Context context, Arguments arguments) {
		return isAssemblyValid();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getOrientation(Context context, Arguments arguments) {
		return getOrientation();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] isInSpace(Context context, Arguments arguments) {
		return isInSpace();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] isInHyperspace(Context context, Arguments arguments) {
		return isInHyperspace();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] shipName(Context context, Arguments arguments) {
		return shipName(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] dim_positive(Context context, Arguments arguments) {
		return dim_positive(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] dim_negative(Context context, Arguments arguments) {
		return dim_negative(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] energy(Context context, Arguments arguments) {
		return energy();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getAttachedPlayers(Context context, Arguments arguments) {
		return getAttachedPlayers();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] command(Context context, Arguments arguments) {
		return command(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] enable(Context context, Arguments arguments) {
		return enable(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getShipSize(Context context, Arguments arguments) {
		return getShipSize();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getMaxJumpDistance(Context context, Arguments arguments) {
		return getMaxJumpDistance();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] movement(Context context, Arguments arguments) {
		return movement(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] rotationSteps(Context context, Arguments arguments) {
		return rotationSteps(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] targetName(Context context, Arguments arguments) {
		return targetName(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getEnergyRequired(Context context, Arguments arguments) {
		return getEnergyRequired();
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method, arguments);
		
		switch (methodName) {
		case "isAssemblyValid":
			return isAssemblyValid();
			
		case "getOrientation":
			return getOrientation();
			
		case "isInSpace":
			return isInSpace();
			
		case "isInHyperspace":
			return isInHyperspace();
			
		case "shipName":
			return shipName(arguments);
			
		case "dim_positive":
			return dim_positive(arguments);
			
		case "dim_negative":
			return dim_negative(arguments);
			
		case "energy":
			return energy();
			
		case "getAttachedPlayers":
			return getAttachedPlayers();
			
		case "command":
			return command(arguments);
			
		case "enable":
			return enable(arguments);
			
		case "getShipSize":
			return getShipSize();
			
		case "getMaxJumpDistance":
			return getMaxJumpDistance();
			
		case "movement":
			return movement(arguments);
			
		case "rotationSteps":
			return rotationSteps(arguments);
			
		case "targetName":
			return targetName(arguments);
			
		case "getEnergyRequired":
			return getEnergyRequired();
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public String toString() {
		final TileEntityShipCore tileEntityShipCore = tileEntityShipCoreWeakReference == null ? null : tileEntityShipCoreWeakReference.get();
		return String.format("%s \'%s\' @ \'%s\' (%d %d %d)", getClass().getSimpleName(),
		                     tileEntityShipCore == null ? "-NULL-" : tileEntityShipCore.shipName, 
		                     worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(),
		                     xCoord, yCoord, zCoord);
	}
}

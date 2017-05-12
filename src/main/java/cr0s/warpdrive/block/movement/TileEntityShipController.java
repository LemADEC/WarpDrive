package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractInterfaced;
import cr0s.warpdrive.block.movement.TileEntityShipCore.EnumShipCoreMode;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.VectorI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Optional;

public class TileEntityShipController extends TileEntityAbstractInterfaced {
	
	// Variables
	private int distance = 0;
	private int direction = 0;
	private int moveFront = 0;
	private int moveUp = 0;
	private int moveRight = 0;
	private byte rotationSteps = 0;
	private EnumShipCoreMode mode = EnumShipCoreMode.IDLE;
	
	private boolean jumpFlag = false;
	private boolean summonFlag = false;
	private String toSummon = "";
	
	private String targetJumpgateName = "";
	
	// Dimensions
	private int front, right, up;
	private int back, left, down;
	
	// Player attaching
	public final ArrayList<String> players = new ArrayList<>();
	public String playersString = "";
	
	private String beaconFrequency = "";
	
	private final int updateInterval_ticks = 20 * WarpDriveConfig.SHIP_CONTROLLER_UPDATE_INTERVAL_SECONDS;
	private int updateTicks = updateInterval_ticks;
	private int bootTicks = 20;
	
	private TileEntityShipCore core = null;
	
	public TileEntityShipController() {
		super();
		
		peripheralName = "warpdriveShipController";
		addMethods(new String[] {
				"dim_positive",
				"dim_negative",
				"mode",
				"distance",
				"direction",
				"energy",
				"getAttachedPlayers",
				"summon",
				"summon_all",
				"jump",
				"getShipSize",
				"beaconFrequency",
				"getOrientation",
				"coreFrequency",
				"isInSpace",
				"isInHyperspace",
				"targetJumpgate",
				"isAttached",
				"getEnergyRequired",
				"movement",
				"rotationSteps",
				"getMaxJumpDistance"
		});
		CC_scripts = Arrays.asList("startup");
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
			if (core == null) {
				updateTicks = 1;
			}
		}
		updateTicks--;
		if (updateTicks <= 0) {
			updateTicks = updateInterval_ticks;
			
			core = findCoreBlock();
			if (core != null) {
				if (mode.getCode() != getBlockMetadata()) {
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, mode.getCode(), 1 + 2);  // Activated
				}
			} else if (getBlockMetadata() != 0) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 1 + 2);  // Inactive
			}
		}
	}
	
	private void setMode(final int mode) {
		EnumShipCoreMode[] modes = EnumShipCoreMode.values();
		if (modes[mode] != null) {
			this.mode = modes[mode];
			markDirty();
			if (WarpDriveConfig.LOGGING_JUMP && hasWorldObj()) {
				WarpDrive.logger.info(this + " Mode set to " + this.mode + " (" + this.mode.getCode() + ")");
			}
		}
	}
	
	private void setDirection(final int parDirection) {
		if (parDirection == 1) {
			direction = -1;
		} else if (parDirection == 2) {
			direction = -2;
		} else if (parDirection == 255) {
			direction = 270;
		} else {
			direction = parDirection;
		}
		markDirty();
		if (WarpDriveConfig.LOGGING_JUMP && hasWorldObj()) {
			WarpDrive.logger.info(this + " Direction set to " + direction);
		}
	}
	
	private void setMovement(final int parMoveFront, final int parMoveUp, final int parMoveRight) {
		moveFront = parMoveFront;
		moveUp = parMoveUp;
		moveRight = parMoveRight;
		markDirty();
		if (WarpDriveConfig.LOGGING_JUMP && hasWorldObj()) {
			WarpDrive.logger.info(this + " Movement set to " + moveFront + " front, " + moveUp + " up, " + moveRight + " right");
		}
	}
	
	private void setRotationSteps(final byte parRotationSteps) {
		rotationSteps = (byte) ((parRotationSteps + 4) % 4);
		markDirty();
		if (WarpDriveConfig.LOGGING_JUMP && hasWorldObj()) {
			WarpDrive.logger.info(this + " RotationSteps set to " + rotationSteps);
		}
	}
	
	private void doJump() {
		if (core != null) {
			// Adding random ticks to warmup
			core.randomWarmupAddition = worldObj.rand.nextInt(WarpDriveConfig.SHIP_WARMUP_RANDOM_TICKS);
		} else {
			WarpDrive.logger.error(this + " doJump without a core");
		}
		
		setJumpFlag(true);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		setMode(tag.getInteger("mode"));
		setFront(tag.getInteger("front"));
		setRight(tag.getInteger("right"));
		setUp(tag.getInteger("up"));
		setBack(tag.getInteger("back"));
		setLeft(tag.getInteger("left"));
		setDown(tag.getInteger("down"));
		setDistance(tag.getInteger("distance"));
		setDirection(tag.getInteger("direction"));
		setMovement(tag.getInteger("moveFront"), tag.getInteger("moveUp"), tag.getInteger("moveRight"));
		setRotationSteps(tag.getByte("rotationSteps"));
		playersString = tag.getString("players");
		updatePlayersList();
		setBeaconFrequency(tag.getString("bfreq"));
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		updatePlayersString();
		tag.setString("players", playersString);
		tag.setInteger("mode", mode.getCode());
		tag.setInteger("front", front);
		tag.setInteger("right", right);
		tag.setInteger("up", up);
		tag.setInteger("back", back);
		tag.setInteger("left", left);
		tag.setInteger("down", down);
		tag.setInteger("distance", distance);
		tag.setInteger("direction", direction);
		tag.setInteger("moveFront", moveFront);
		tag.setInteger("moveUp", moveUp);
		tag.setInteger("moveRight", moveRight);
		tag.setByte("rotationSteps", rotationSteps);
		tag.setString("bfreq", getBeaconFrequency());
		// FIXME: shouldn't we save boolean jumpFlag, boolean summonFlag, String toSummon, String targetJumpgateName?
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound nbtTagCompound) {
		nbtTagCompound = super.writeItemDropNBT(nbtTagCompound);
		nbtTagCompound.removeTag("players");
		nbtTagCompound.removeTag("mode");
		nbtTagCompound.removeTag("front");
		nbtTagCompound.removeTag("right");
		nbtTagCompound.removeTag("up");
		nbtTagCompound.removeTag("back");
		nbtTagCompound.removeTag("left");
		nbtTagCompound.removeTag("down");
		nbtTagCompound.removeTag("distance");
		nbtTagCompound.removeTag("direction");
		nbtTagCompound.removeTag("moveFront");
		nbtTagCompound.removeTag("moveUp");
		nbtTagCompound.removeTag("moveRight");
		nbtTagCompound.removeTag("rotationSteps");
		nbtTagCompound.removeTag("bfreq");
		return nbtTagCompound;
	}
	
	public String attachPlayer(EntityPlayer entityPlayer) {
		for (int i = 0; i < players.size(); i++) {
			String name = players.get(i);
			
			if (entityPlayer.getDisplayName().equals(name)) {
				players.remove(i);
				return StatCollector.translateToLocalFormatted("warpdrive.guide.prefix",
						getBlockType().getLocalizedName())
					+ StatCollector.translateToLocalFormatted("warpdrive.ship.playerDetached",
							core != null && !core.shipName.isEmpty() ? core.shipName : "-",
							getAttachedPlayersList());
			}
		}
		
		entityPlayer.attackEntityFrom(DamageSource.generic, 1);
		players.add(entityPlayer.getDisplayName());
		updatePlayersString();
		return StatCollector.translateToLocalFormatted("warpdrive.guide.prefix",
					getBlockType().getLocalizedName())
				+ StatCollector.translateToLocalFormatted("warpdrive.ship.playerAttached",
						core != null && !core.shipName.isEmpty() ? core.shipName : "-",
						getAttachedPlayersList());
	}
	
	@Override
	public String getStatus() {
		return super.getStatus()
				+ StatCollector.translateToLocalFormatted("warpdrive.ship.attachedPlayers",
						getAttachedPlayersList());
	}
	
	public void updatePlayersString() {
		String nick;
		playersString = "";
		
		for (int i = 0; i < players.size(); i++) {
			nick = players.get(i);
			playersString += nick + "|";
		}
	}
	
	public void updatePlayersList() {
		String[] playersArray = playersString.split("\\|");
		
		for (int i = 0; i < playersArray.length; i++) {
			String nick = playersArray[i];
			
			if (!nick.isEmpty()) {
				players.add(nick);
			}
		}
	}
	
	public String getAttachedPlayersList() {
		StringBuilder list = new StringBuilder("");
		
		for (int i = 0; i < players.size(); i++) {
			String nick = players.get(i);
			list.append(nick + ((i == players.size() - 1) ? "" : ", "));
		}
		
		if (players.isEmpty()) {
			list = new StringBuilder("<nobody>");
		}
		
		return list.toString();
	}
	
	public boolean isJumpFlag() {
		return jumpFlag;
	}
	
	public void setJumpFlag(boolean jumpFlag) {
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " setJumpFlag(" + jumpFlag + ")");
		}
		this.jumpFlag = jumpFlag;
	}
	
	public int getFront() {
		return front;
	}
	
	private void setFront(int front) {
		this.front = front;
	}
	
	public int getRight() {
		return right;
	}
	
	private void setRight(int right) {
		this.right = right;
	}
	
	public int getUp() {
		return up;
	}
	
	private void setUp(int up) {
		this.up = up;
	}
	
	public int getBack() {
		return back;
	}
	
	private void setBack(int back) {
		this.back = back;
	}
	
	public int getLeft() {
		return left;
	}
	
	private void setLeft(int left) {
		this.left = left;
	}
	
	public int getDown() {
		return down;
	}
	
	private void setDown(int down) {
		this.down = down;
	}
	
	public int getMaxJumpDistance() {
		int maxDistance = WarpDriveConfig.SHIP_MAX_JUMP_DISTANCE;
		if (worldObj == null || WarpDrive.starMap.isInHyperspace(worldObj, xCoord, zCoord)) {
			maxDistance *= WarpDriveConfig.SHIP_HYPERSPACE_ACCELERATION;
		}
		return maxDistance;
	}
	
	private void setDistance(final int distance) {
		this.distance = Math.max(1, Math.min(getMaxJumpDistance(), distance));
		if (WarpDriveConfig.LOGGING_JUMP && hasWorldObj()) {
			WarpDrive.logger.info(this + " Jump distance set to " + distance);
		}
	}
	
	public int getDistance() {
		return distance;
	}
	
	public int getDirection() {
		return direction;
	}
	
	public byte getRotationSteps() {
		return rotationSteps;
	}
	
	public EnumShipCoreMode getMode() {
		return mode;
	}
	
	public boolean isSummonAllFlag() {
		return summonFlag;
	}
	
	public void setSummonAllFlag(boolean summonFlag) {
		this.summonFlag = summonFlag;
	}
	
	public String getToSummon() {
		return toSummon;
	}
	
	public void setToSummon(String toSummon) {
		this.toSummon = toSummon;
	}
	
	public String getBeaconFrequency() {
		return beaconFrequency;
	}
	
	public void setBeaconFrequency(String beaconFrequency) {
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(this + " Beacon frequency set to " + beaconFrequency);
		}
		this.beaconFrequency = beaconFrequency;
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
	
	// OpenComputer callback methods
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
	public Object[] mode(Context context, Arguments arguments) {
		return mode(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] distance(Context context, Arguments arguments) {
		return distance(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] direction(Context context, Arguments arguments) {
		return direction(argumentsOCtoCC(arguments));
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
	public Object[] getAttachedPlayers(Context context, Arguments arguments) {
		return getAttachedPlayers();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] summon(Context context, Arguments arguments) {
		return summon(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] summon_all(Context context, Arguments arguments) {
		setSummonAllFlag(true);
		return null;
	}
	
	@Override
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] position(Context context, Arguments arguments) {
		if (core == null) {
			return null;
		}
		
		return new Object[] { core.xCoord, core.yCoord, core.zCoord };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] energy(Context context, Arguments arguments) {
		if (core == null) {
			return null;
		}
		
		return core.energy();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getMaxJumpDistance(Context context, Arguments arguments) {
		return new Object[] { getMaxJumpDistance() };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getEnergyRequired(Context context, Arguments arguments) {
		if (core == null) {
			return null;
		}
		
		return getEnergyRequired(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] jump(Context context, Arguments arguments) {
		doJump();
		return null;
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getShipSize(Context context, Arguments arguments) {
		return getShipSize();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] beaconFrequency(Context context, Arguments arguments) {
		return beaconFrequency(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getOrientation(Context context, Arguments arguments) {
		if (core != null) {
			return new Object[] { core.dx, 0, core.dz };
		}
		return null;
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] coreFrequency(Context context, Arguments arguments) {
		return shipName(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] isInSpace(Context context, Arguments arguments) {
		return new Boolean[] { WarpDrive.starMap.isInSpace(worldObj, xCoord, zCoord) };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] isInHyperspace(Context context, Arguments arguments) {
		return new Boolean[] { WarpDrive.starMap.isInHyperspace(worldObj, xCoord, zCoord) };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] targetJumpgate(Context context, Arguments arguments) {
		return targetJumpgate(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] isAttached(Context context, Arguments arguments) {
		if (core != null) {
			return new Object[] { core.controller != null };
		}
		return null;
	}
	
	// Common OC/CC methods
	private Object[] dim_positive(Object[] arguments) {
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
	
	private Object[] dim_negative(Object[] arguments) {
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
	
	private Object[] mode(Object[] arguments) {
		try {
			if (arguments.length == 1) {
				setMode(Commons.toInt(arguments[0]));
			}
		} catch (Exception exception) {
			return new Integer[] { mode.getCode() };
		}
		
		return new Integer[] { mode.getCode() };
	}
	
	private Object[] distance(Object[] arguments) {
		try {
			if (arguments.length == 1) {
				setDistance(Commons.toInt(arguments[0]));
			}
		} catch (Exception exception) {
			return new Integer[] { getDistance() };
		}
		
		return new Integer[] { getDistance() };
	}
	
	private Object[] direction(Object[] arguments) {
		try {
			if (arguments.length == 1) {
				setDirection(Commons.toInt(arguments[0]));
			}
		} catch (Exception exception) {
			return new Integer[] { getDirection() };
		}
		
		return new Integer[] { getDirection() };
	}
	
	private Object[] movement(Object[] arguments) {
		try {
			if (arguments.length == 3) {
				setMovement(Commons.toInt(arguments[0]), Commons.toInt(arguments[1]), Commons.toInt(arguments[2]));
			}
		} catch (Exception exception) {
			return new Integer[] { moveFront, moveUp, moveRight };
		}
		
		return new Integer[] { moveFront, moveUp, moveRight };
	}
	
	private Object[] rotationSteps(Object[] arguments) {
		try {
			if (arguments.length == 1) {
				setRotationSteps((byte) Commons.toInt(arguments[0]));
			}
		} catch (Exception exception) {
			return new Integer[] { (int) rotationSteps };
		}
		
		return new Integer[] { (int) rotationSteps };
	}
	
	private Object[] getAttachedPlayers() {
		String list = "";
		
		if (!players.isEmpty()) {
			for (int i = 0; i < players.size(); i++) {
				String nick = players.get(i);
				list += nick + ((i == players.size() - 1) ? "" : ",");
			}
		}
		
		return new Object[] { list, players.toArray() };
	}
	
	private Object[] summon(Object[] arguments) {
		if (arguments.length != 1) {
			return new Object[] { false };
		}
		int playerIndex;
		try {
			playerIndex = Commons.toInt(arguments[0]);
		} catch (Exception exception) {
			return new Object[] { false };
		}
		
		if (playerIndex >= 0 && playerIndex < players.size()) {
			setToSummon(players.get(playerIndex));
			return new Object[] { true };
		}
		return new Object[] { false };
	}
	
	private Object[] getEnergyRequired(Object[] arguments) {
		try {
			if (arguments.length == 1 && core != null) {
				return new Object[] { core.calculateRequiredEnergy(getMode(), core.shipMass, Commons.toInt(arguments[0])) };
			}
		} catch (Exception exception) {
			return new Integer[] { -1 };
		}
		return new Integer[] { -1 };
	}
	
	private Object[] getShipSize() {
		if (core == null) {
			return null;
		}
		StringBuilder reason = new StringBuilder();
		try {
			if (!core.validateShipSpatialParameters(reason)) {
				core.messageToAllPlayersOnShip(reason.toString());
				if (core.controller == null) {
					return null;
				}
			}
			return new Object[] { core.shipMass };
		} catch (Exception exception) {
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {// disabled by default to avoid console spam as ship size is checked quite frequently
				exception.printStackTrace();
			}
			return null;
		}
	}
	
	private Object[] beaconFrequency(Object[] arguments) {
		if (arguments.length == 1) {
			setBeaconFrequency((String) arguments[0]);
		}
		return new Object[] { beaconFrequency };
	}
	
	private Object[] shipName(Object[] arguments) { 
		if (core == null) {
			return null;
		}
		if (arguments.length == 1) {
			core.shipName = ((String) arguments[0]).replace("/", "").replace(".", "").replace("\\", ".");
		}
		return new Object[] { core.shipName };
	}
	
	private Object[] targetJumpgate(Object[] arguments) { 
		if (arguments.length == 1) {
			setTargetJumpgateName((String) arguments[0]);
		}
		return new Object[] { targetJumpgateName };
	}
	
	protected void cooldownDone() {
		sendEvent("shipCoreCooldownDone");
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		String methodName = getMethodName(method);
		
		switch (methodName) {
			case "dim_positive": // dim_positive (front, right, up)
				return dim_positive(arguments);
			
			case "dim_negative": // dim_negative (back, left, down)
				return dim_negative(arguments);
			
			case "mode": // mode (mode)
				return mode(arguments);
			
			case "distance": // distance (distance)
				return distance(arguments);
			
			case "direction": // direction (direction)
				return direction(arguments);
			
			case "getAttachedPlayers":
				return getAttachedPlayers();
			
			case "summon":
				return summon(arguments);
			
			case "summon_all":
				setSummonAllFlag(true);
				break;
			
			case "position":
				if (core == null) {
					return null;
				}
				return new Object[] { core.xCoord, core.yCoord, core.zCoord };
			
			case "energy":
				if (core == null) {
					return null;
				}
				return core.energy();
			
			case "getEnergyRequired": // getEnergyRequired(distance)
				return getEnergyRequired(arguments);
			
			case "jump":
				doJump();
				break;
			
			case "getShipSize":
				return getShipSize();
			
			case "beaconFrequency":
				return beaconFrequency(arguments);
			
			case "getOrientation":
				if (core != null) {
					return new Object[] { core.dx, 0, core.dz };
				}
				return null;
			
			case "coreFrequency":
				return shipName(arguments);
			
			case "isInSpace":
				return new Boolean[] { WarpDrive.starMap.isInSpace(worldObj, xCoord, zCoord) };
			
			case "isInHyperspace":
				return new Boolean[] { WarpDrive.starMap.isInHyperspace(worldObj, xCoord, zCoord) };
			
			case "targetJumpgate":
				return targetJumpgate(arguments);
			
			case "isAttached":
				if (core != null) {
					return new Object[] { core.controller != null };
				}
				break;
			
			case "movement":
				return movement(arguments);
			
			case "rotationSteps":
				return rotationSteps(arguments);
			
			case "getMaxJumpDistance":
				return new Object[] { getMaxJumpDistance() };
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	public String getTargetJumpgateName() {
		return targetJumpgateName;
	}

	public void setTargetJumpgateName(String parTargetJumpgateName) {
		targetJumpgateName = parTargetJumpgateName;
	}
	
	public VectorI getMovement() {
		if (moveFront != 0 || moveUp != 0 || moveRight != 0) {
			return new VectorI(moveFront, moveUp, moveRight);
		}
		switch (direction) {
		case -1:
			return new VectorI(0, distance, 0);
			
		case -2:
			return new VectorI(0, -distance, 0);
			
		case 0:
			return new VectorI(distance, 0, 0);
			
		case 180:
			return new VectorI(-distance, 0, 0);
			
		case 90:
			return new VectorI(0, 0, -distance);
			
		case 270:
			return new VectorI(0, 0, distance);
			
		default:
			WarpDrive.logger.error(this + " Invalid direction " + direction);
			return new VectorI(0, 0, 0);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s \'%s\' @ \'%s\' (%d %d %d)", getClass().getSimpleName(),
			core == null ? beaconFrequency : core.shipName,
			worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(),
			xCoord, yCoord, zCoord);
	}
}

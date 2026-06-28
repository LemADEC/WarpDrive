package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.IMultiBlockCore;
import cr0s.warpdrive.api.computer.IShipController;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumShipCommand;
import cr0s.warpdrive.data.VectorI;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

import net.minecraftforge.fml.common.Optional;

public abstract class TileEntityAbstractShipController extends TileEntityAbstractEnergyCoreOrController implements IShipController {

	// Defensive cap on a single movement component. Gameplay range is enforced by ShipMovementCosts;
	// this only rejects pathological GUI/computer/NBT values before they reach world-coordinate math.
	protected static final int SHIP_MOVEMENT_INPUT_LIMIT = 30000000;

	// persistent properties
	private int front, right, up;
	private int back, left, down;
	
	private int moveFront = 0;
	private int moveUp = 0;
	private int moveRight = 0;
	private byte rotationSteps = 0;
	
	protected EnumShipCommand enumShipCommand = EnumShipCommand.IDLE;
	protected boolean isCommandConfirmed = false;
	
	public TileEntityAbstractShipController() {
		super();
		
		addMethods(new String[] {
				"getOrientation",
				"isInSpace",
				"isInHyperspace",
				"dim_positive",
				"dim_negative",
				"command",
				"getShipSize",
				"getMaxJumpDistance",
				"movement",
				"rotationSteps",
				"state",
				"validateMovement",
				"validateNavigation",
				});
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		setFront(tagCompound.getInteger("front"));
		setRight(tagCompound.getInteger("right"));
		setUp   (tagCompound.getInteger("up"));
		setBack (tagCompound.getInteger("back"));
		setLeft (tagCompound.getInteger("left"));
		setDown (tagCompound.getInteger("down"));
		
		setMovement(
				tagCompound.getInteger("moveFront"),
				tagCompound.getInteger("moveUp"),
				tagCompound.getInteger("moveRight") );
		setRotationSteps(tagCompound.getByte("rotationSteps"));
		
		final boolean isConfirmed = tagCompound.hasKey("commandConfirmed") && tagCompound.getBoolean("commandConfirmed");
		setCommand(tagCompound.getString("commandName"), isConfirmed);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setInteger("front", getFront());
		tagCompound.setInteger("right", getRight());
		tagCompound.setInteger("up", getUp());
		tagCompound.setInteger("back", getBack());
		tagCompound.setInteger("left", getLeft());
		tagCompound.setInteger("down", getDown());
		
		tagCompound.setInteger("moveFront", moveFront);
		tagCompound.setInteger("moveUp", moveUp);
		tagCompound.setInteger("moveRight", moveRight);
		tagCompound.setByte("rotationSteps", rotationSteps);
		
		tagCompound.setString("commandName", enumShipCommand.getName());
		tagCompound.setBoolean("commandConfirmed", isCommandConfirmed);
		
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = super.getUpdateTag();
		
		tagCompound.setInteger("moveFront", moveFront);
		tagCompound.setInteger("moveUp", moveUp);
		tagCompound.setInteger("moveRight", moveRight);
		tagCompound.setByte("rotationSteps", rotationSteps);
		
		return tagCompound;
	}
	
	@Override
	public void onDataPacket(@Nonnull final NetworkManager networkManager, @Nonnull final SPacketUpdateTileEntity packet) {
		super.onDataPacket(networkManager, packet);
		
		final NBTTagCompound tagCompound = packet.getNbtCompound();
		
		setMovement(
				tagCompound.getInteger("moveFront"),
				tagCompound.getInteger("moveUp"),
				tagCompound.getInteger("moveRight") );
		setRotationSteps(tagCompound.getByte("rotationSteps"));
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		tagCompound.removeTag("front");
		tagCompound.removeTag("right");
		tagCompound.removeTag("up");
		tagCompound.removeTag("back");
		tagCompound.removeTag("left");
		tagCompound.removeTag("down");
		
		tagCompound.removeTag("moveFront");
		tagCompound.removeTag("moveUp");
		tagCompound.removeTag("moveRight");
		tagCompound.removeTag("rotationSteps");
		
		tagCompound.removeTag("commandName");
		tagCompound.removeTag("commandConfirmed");
		
		return tagCompound;
	}
	
	protected void cooldownDone() {
		sendEvent("shipCoreCooldownDone");
	}
	
	public EnumShipCommand getCommand() {
		return enumShipCommand;
	}
	
	protected void setCommand(final String command, final boolean isConfirmed) {
		for (final EnumShipCommand enumShipCommand : EnumShipCommand.values()) {
			if (enumShipCommand.name().equalsIgnoreCase(command)) {
				this.isCommandConfirmed = false;
				this.enumShipCommand = enumShipCommand;
				this.isCommandConfirmed = isConfirmed;
				markDirty();
				if (WarpDriveConfig.LOGGING_LUA && hasWorld() && !world.isRemote) {
					WarpDrive.logger.info(String.format("%s Command set to %s (%s)",
					                                    this, this.enumShipCommand, this.isCommandConfirmed));
				}
				break;
			}
		}
	}
	
	protected void commandDone(final boolean success, final WarpDriveText reason) {
		isCommandConfirmed = false;
		enumShipCommand = EnumShipCommand.IDLE;
	}
	
	protected int getFront() {
		return front;
	}
	
	private void setFront(final int front) {
		this.front = front;
		markDirtyParameters();
	}
	
	protected int getRight() {
		return right;
	}
	
	private void setRight(final int right) {
		this.right = right;
		markDirtyParameters();
	}
	
	protected int getUp() {
		return up;
	}
	
	private void setUp(final int up) {
		this.up = up;
		markDirtyParameters();
	}
	
	protected int getBack() {
		return back;
	}
	
	private void setBack(final int back) {
		this.back = back;
		markDirtyParameters();
	}
	
	protected int getLeft() {
		return left;
	}
	
	private void setLeft(final int left) {
		this.left = left;
		markDirtyParameters();
	}
	
	protected int getDown() {
		return down;
	}
	
	private void setDown(final int down) {
		this.down = down;
		markDirtyParameters();
	}
	
	protected VectorI getMovement() {
		return new VectorI(moveFront, moveUp, moveRight);
	}
	
	protected void setMovement(final int moveFront, final int moveUp, final int moveRight) {
		// clamp at the single chokepoint so neither the GUI nor a computer can push a value that
		// overflows the downstream Math.abs()/magnitude arithmetic (e.g. Integer.MIN_VALUE)
		this.moveFront = Commons.clamp(-SHIP_MOVEMENT_INPUT_LIMIT, SHIP_MOVEMENT_INPUT_LIMIT, moveFront);
		this.moveUp    = Commons.clamp(-SHIP_MOVEMENT_INPUT_LIMIT, SHIP_MOVEMENT_INPUT_LIMIT, moveUp);
		this.moveRight = Commons.clamp(-SHIP_MOVEMENT_INPUT_LIMIT, SHIP_MOVEMENT_INPUT_LIMIT, moveRight);
		markDirty();
	}
	
	protected byte getRotationSteps() {
		return rotationSteps;
	}
	
	private void setRotationSteps(final byte rotationSteps) {
		this.rotationSteps = (byte) ((rotationSteps + 4) % 4);
		markDirty();
	}
	
	@Override
	public void onCoreUpdated(@Nonnull final IMultiBlockCore multiblockCore) {
		super.onCoreUpdated(multiblockCore);
		
		assert multiblockCore instanceof TileEntityShipCore;
		final TileEntityShipCore tileEntityShipCore = (TileEntityShipCore) multiblockCore;
		final boolean isChanged = front != tileEntityShipCore.getFront()
		                       || right != tileEntityShipCore.getRight()
		                       || up != tileEntityShipCore.getUp()
		                       || back != tileEntityShipCore.getBack()
		                       || left != tileEntityShipCore.getLeft()
		                       || down != tileEntityShipCore.getDown();
		front = tileEntityShipCore.getFront();
		right = tileEntityShipCore.getRight();
		up    = tileEntityShipCore.getUp();
		back  = tileEntityShipCore.getBack();
		left  = tileEntityShipCore.getLeft();
		down  = tileEntityShipCore.getDown();
		if (isChanged) {
			markDirtyParameters();
			markDirty();
		}
	}
	
	// Common OC/CC methods
	@Override
	abstract public Object[] getOrientation();
	
	@Override
	abstract public Object[] isInSpace();
	
	@Override
	abstract public Object[] isInHyperspace();
	
	@Override
	public Object[] dim_positive(final Object[] arguments) {
		try {
			if (arguments != null && arguments.length == 3) {
				final int argInt0 = Commons.clamp(0, WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()], Math.abs(Commons.toInt(arguments[0])));
				final int argInt1 = Commons.clamp(0, WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()], Math.abs(Commons.toInt(arguments[1])));
				final int argInt2 = Commons.clamp(0, WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()], Math.abs(Commons.toInt(arguments[2])));
				final int frontPrevious = front;
				final int rightPrevious = right;
				final int upPrevious = up;
				setFront(argInt0);
				setRight(argInt1);
				setUp(Math.min(255 - pos.getY(), argInt2));
				if ( front != frontPrevious
				  || right != rightPrevious
				  || up != upPrevious ) {
					markDirty();
				}
			}
		} catch (final Exception exception) {
			if (WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.info(String.format("%s Invalid arguments to dim_positive(): %s",
				                                    this, Commons.format(arguments)));
			}
		}
		
		return new Integer[] { getFront(), getRight(), getUp() };
	}
	
	@Override
	public Object[] dim_negative(final Object[] arguments) {
		try {
			if (arguments != null && arguments.length == 3) {
				final int argInt0 = Commons.clamp(0, WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()], Math.abs(Commons.toInt(arguments[0])));
				final int argInt1 = Commons.clamp(0, WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()], Math.abs(Commons.toInt(arguments[1])));
				final int argInt2 = Commons.clamp(0, WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()], Math.abs(Commons.toInt(arguments[2])));
				final int backPrevious = back;
				final int leftPrevious = left;
				final int downPrevious = down;
				setBack(argInt0);
				setLeft(argInt1);
				setDown(Math.min(pos.getY(), argInt2));
				if ( back != backPrevious
				  || left != leftPrevious
				  || down != downPrevious ) {
					markDirty();
				}
			}
		} catch (final Exception exception) {
			if (WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.info(String.format("%s Invalid arguments to dim_negative(): %s",
				                                    this, Commons.format(arguments)));
			}
		}
		
		return new Integer[] { getBack(), getLeft(), getDown() };
	}
	
	@Override
	public Object[] command(final Object[] arguments) {
		try {
			if ( arguments.length == 2
			  && arguments[0] != null ) {
				setCommand(arguments[0].toString(), Commons.toBool(arguments[1]));
			}
		} catch (final Exception exception) {
			return new Object[] { enumShipCommand.toString() };
		}
		
		return new Object[] { enumShipCommand.toString() };
	}
	
	@Override
	abstract public Object[] getShipSize();
	
	@Override
	public Object[] movement(final Object[] arguments) {
		try {
			if (arguments.length == 3) {
				setMovement(Commons.toInt(arguments[0]), Commons.toInt(arguments[1]), Commons.toInt(arguments[2]));
			}
		} catch (final Exception exception) {
			return new Integer[] { moveFront, moveUp, moveRight };
		}
		
		return new Integer[] { moveFront, moveUp, moveRight };
	}
	
	@Override
	abstract public Object[] getMaxJumpDistance();
	
	abstract public Object[] validateMovement(final Object[] arguments);
	
	abstract public Object[] validateNavigation();
	
	@Override
	public Object[] rotationSteps(final Object[] arguments) {
		try {
			if (arguments.length == 1 && arguments[0] != null) {
				setRotationSteps((byte) Commons.toInt(arguments[0]));
			}
		} catch (final Exception exception) {
			return new Integer[] { (int) rotationSteps };
		}
		
		return new Integer[] { (int) rotationSteps };
	}
	
	@Override
	abstract public Object[] state();
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getOrientation(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getOrientation();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] isInSpace(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return isInSpace();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] isInHyperspace(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return isInHyperspace();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] dim_positive(final Context context, final Arguments arguments) {
		return dim_positive(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] dim_negative(final Context context, final Arguments arguments) {
		return dim_negative(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] command(final Context context, final Arguments arguments) {
		return command(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getShipSize(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getShipSize();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getMaxJumpDistance(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getMaxJumpDistance();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] movement(final Context context, final Arguments arguments) {
		return movement(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] rotationSteps(final Context context, final Arguments arguments) {
		return rotationSteps(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] validateMovement(final Context context, final Arguments arguments) {
		return validateMovement(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] validateNavigation(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return validateNavigation();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getOrientation":
			return getOrientation();
		
		case "isInSpace":
			return isInSpace();
		
		case "isInHyperspace":
			return isInHyperspace();
		
		case "dim_positive":
			return dim_positive(arguments);
		
		case "dim_negative":
			return dim_negative(arguments);
		
		case "command":
			return command(arguments);
		
		case "getShipSize":
			return getShipSize();
		
		case "getMaxJumpDistance":
			return getMaxJumpDistance();
		
		case "movement":
			return movement(arguments);
		
		case "rotationSteps":
			return rotationSteps(arguments);
		
		case "state":
			return state();
		
		case "validateMovement":
			return validateMovement(arguments);
		
		case "validateNavigation":
			return validateNavigation();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}

package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityEnanReactorLaser extends TileEntityAbstractLaser {
	Vector3 myVec;
	Vector3 reactorVec;
	ForgeDirection side = ForgeDirection.UNKNOWN;
	TileEntityEnanReactorCore reactor;
	
	private boolean isFirstUpdate = true;
	
	public TileEntityEnanReactorLaser() {
		super();
		
		addMethods(new String[] {
				"hasReactor",
				"side",
				"stabilize"
		});
		peripheralName = "warpdriveEnanReactorLaser";
		laserMediumMaxCount = 1;
		directionsValidLaserMedium = new ForgeDirection[] { ForgeDirection.UP, ForgeDirection.DOWN };
	}
	
	public TileEntityEnanReactorCore scanForReactor() {
		reactor = null;
		TileEntity tileEntity;
		// I AM ON THE NORTH SIDE
		side = ForgeDirection.UNKNOWN;
		tileEntity = worldObj.getTileEntity(xCoord, yCoord, zCoord + 2);
		if (tileEntity instanceof TileEntityEnanReactorCore && worldObj.isAirBlock(xCoord, yCoord, zCoord + 1)) {
			side = ForgeDirection.NORTH;
			reactor = (TileEntityEnanReactorCore) tileEntity;
		}
		
		// I AM ON THE SOUTH SIDE
		tileEntity = worldObj.getTileEntity(xCoord, yCoord, zCoord - 2);
		if (tileEntity instanceof TileEntityEnanReactorCore && worldObj.isAirBlock(xCoord, yCoord, zCoord - 1)) {
			side = ForgeDirection.SOUTH;
			reactor = (TileEntityEnanReactorCore) tileEntity;
		}
		
		// I AM ON THE WEST SIDE
		tileEntity = worldObj.getTileEntity(xCoord + 2, yCoord, zCoord);
		if (tileEntity instanceof TileEntityEnanReactorCore && worldObj.isAirBlock(xCoord + 1, yCoord, zCoord)) {
			side = ForgeDirection.WEST;
			reactor = (TileEntityEnanReactorCore) tileEntity;
		}
		
		// I AM ON THE EAST SIDE
		tileEntity = worldObj.getTileEntity(xCoord - 2, yCoord, zCoord);
		if (tileEntity instanceof TileEntityEnanReactorCore && worldObj.isAirBlock(xCoord - 1, yCoord, zCoord)) {
			side = ForgeDirection.EAST;
			reactor = (TileEntityEnanReactorCore) tileEntity;
		}
		
		setMetadata();
		
		if (reactor != null) {
			reactorVec = new Vector3(reactor).translate(0.5);
		}
		return reactor;
	}
	
	private void setMetadata() {
		int metadata = 0;
		if (side != ForgeDirection.UNKNOWN) {
			metadata = side.ordinal() - 1;
		}
		if (getBlockMetadata() != metadata) {
			worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, metadata, 3);
		}
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (isFirstUpdate) {
			isFirstUpdate = false;
			scanForReactor();
			myVec = new Vector3(this).translate(0.5);
		}
	}
	
	public void unlink() {
		side = ForgeDirection.UNKNOWN;
		setMetadata();
	}
	
	@Override
	public void updatedNeighbours() {
		super.updatedNeighbours();
		
		scanForReactor();
	}
	
	private void stabilize(final int energy) {
		if (energy <= 0) {
			return;
		}
		
		scanForReactor();
		if (directionLaserMedium == ForgeDirection.UNKNOWN) {
			return;
		}
		if (reactor == null) {
			return;
		}
		if (consumeEnergyFromLaserMediums(energy, false)) {
			if (WarpDriveConfig.LOGGING_ENERGY && WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.info("ReactorLaser on " + side + " side sending " + energy);
			}
			reactor.decreaseInstability(side, energy);
			PacketHandler.sendBeamPacket(worldObj, myVec, reactorVec, 0.1F, 0.2F, 1.0F, 25, 50, 100);
		}
	}
	
	@Override
	public void writeToNBT(final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
	}
	
	// OpenComputers callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] hasReactor(Context context, Arguments arguments) {
		return new Object[] { scanForReactor() != null };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] stabilize(Context context, Arguments arguments) {
		if (arguments.count() >= 1) {
			stabilize(arguments.checkInteger(0));
		}
		
		return null;
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] side(Context context, Arguments arguments) {
		return new Object[] { side.ordinal() - 2 };
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		switch (methodName) {
		case "hasReactor":
			return new Object[] { scanForReactor() != null };
			
		case "stabilize":
			if (arguments.length >= 1) {
				stabilize(Commons.toInt(arguments[0]));
			}
			break;
			
		case "side":
			return new Object[] { side.ordinal() - 2 };
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
}
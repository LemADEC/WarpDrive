package cr0s.warpdrive.block.movement;

import java.util.List;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.Optional;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;

public class TileEntityLift extends TileEntityAbstractEnergy {
	private static final int MODE_REDSTONE = -1;
	private static final int MODE_INACTIVE = 0;
	private static final int MODE_UP = 1;
	private static final int MODE_DOWN = 2;
	
	private int firstUncoveredY;
	private int mode = MODE_INACTIVE;
	private boolean isEnabled = false;
	private boolean computerEnabled = true;
	private int computerMode = MODE_REDSTONE;
	
	private int tickCount = 0;
	
	public TileEntityLift() {
		super();
		IC2_sinkTier = 2;
		IC2_sourceTier = 2;
		peripheralName = "warpdriveLift";
		addMethods(new String[] {
				"mode",
				"active"
		});
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		tickCount++;
		if (tickCount >= WarpDriveConfig.LIFT_UPDATE_INTERVAL_TICKS) {
			tickCount = 0;
			
			// Switching mode
			if (  computerMode == MODE_DOWN
			  || (computerMode == MODE_REDSTONE && worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord))) {
				mode = MODE_DOWN;
			} else {
				mode = MODE_UP;
			}
			
			isEnabled = computerEnabled
				     && isPassableBlock(yCoord + 1)
				     && isPassableBlock(yCoord + 2)
				     && isPassableBlock(yCoord - 1)
				     && isPassableBlock(yCoord - 2);
			
			if (energy_getEnergyStored() < WarpDriveConfig.LIFT_ENERGY_PER_ENTITY || !isEnabled) {
				mode = MODE_INACTIVE;
				if (getBlockMetadata() != 0) {
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 2); // disabled
				}
				return;
			}
			
			if (getBlockMetadata() != mode) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, mode, 2); // current mode
			}
			
			// Launch a beam: search non-air blocks under lift
			for (int ny = yCoord - 2; ny > 0; ny--) {
				if (!isPassableBlock(ny)) {
					firstUncoveredY = ny + 1;
					break;
				}
			}
			
			if (yCoord - firstUncoveredY >= 2) {
				if (mode == MODE_UP) {
					PacketHandler.sendBeamPacket(worldObj,
							new Vector3(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D),
							new Vector3(xCoord + 0.5D, yCoord, zCoord + 0.5D),
							0f, 1f, 0f, 40, 0, 100);
				} else if (mode == MODE_DOWN) {
					PacketHandler.sendBeamPacket(worldObj,
							new Vector3(xCoord + 0.5D, yCoord, zCoord + 0.5D),
							new Vector3(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D), 0f,
							0f, 1f, 40, 0, 100);
				}
				
				liftEntity();
			}
		}
	}
	
	private boolean isPassableBlock(int yPosition) {
		Block block = worldObj.getBlock(xCoord, yPosition, zCoord);
		return block.isAssociatedBlock(Blocks.air)
			|| worldObj.isAirBlock(xCoord, yPosition, zCoord)
			|| block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yPosition, zCoord) == null;
	}
	
	private void liftEntity() {
		final double CUBE_RADIUS = 0.4;
		double xMax, zMax;
		double xMin, zMin;
		
		xMin = xCoord + 0.5 - CUBE_RADIUS;
		xMax = xCoord + 0.5 + CUBE_RADIUS;
		zMin = zCoord + 0.5 - CUBE_RADIUS;
		zMax = zCoord + 0.5 + CUBE_RADIUS;
		
		// Lift up
		if (mode == MODE_UP) {
			AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(xMin, firstUncoveredY, zMin, xMax, yCoord, zMax);
			List list = worldObj.getEntitiesWithinAABBExcludingEntity(null, aabb);
			if (list != null) {
				for (Object o : list) {
					if ( o != null
					  && o instanceof EntityLivingBase
					  && energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, true)) {
						((EntityLivingBase) o).setPositionAndUpdate(xCoord + 0.5D, yCoord + 1.0D, zCoord + 0.5D);
						PacketHandler.sendBeamPacket(worldObj,
								new Vector3(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D),
								new Vector3(xCoord + 0.5D, yCoord, zCoord + 0.5D),
								1F, 1F, 0F, 40, 0, 100);
						worldObj.playSoundEffect(xCoord + 0.5D, yCoord, zCoord + 0.5D, "warpdrive:hilaser", 4F, 1F);
						energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, false);
					}
				}
			}
		} else if (mode == MODE_DOWN) {
			AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(xMin,
					Math.min(firstUncoveredY + 4.0D, yCoord), zMin, xMax, yCoord + 2.0D, zMax);
			List list = worldObj.getEntitiesWithinAABBExcludingEntity(null, aabb);
			if (list != null) {
				for (Object o : list) {
					if ( o != null
					  && o instanceof EntityLivingBase
					  && energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, true)) {
						((EntityLivingBase) o).setPositionAndUpdate(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D);
						PacketHandler.sendBeamPacket(worldObj,
								new Vector3(xCoord + 0.5D, yCoord, zCoord + 0.5D),
								new Vector3(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D), 1F, 1F, 0F, 40, 0, 100);
						worldObj.playSoundEffect(xCoord + 0.5D, yCoord, zCoord + 0.5D, "warpdrive:hilaser", 4F, 1F);
						energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, false);
					}
				}
			}
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		if (tag.hasKey("mode")) {
			mode = clamp(-1, 2, tag.getByte("mode"));
		}
		if (tag.hasKey("computerEnabled")) {
			computerEnabled = tag.getBoolean("computerEnabled");
		}
		if (tag.hasKey("computerMode")) {
			computerMode = clamp(-1, 2, tag.getByte("computerMode"));
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setByte("mode", (byte)mode);
		tag.setBoolean("computerEnabled", computerEnabled);
		tag.setByte("computerMode", (byte)computerMode);
	}
	
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.LIFT_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(ForgeDirection from) {
		return true;
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] mode(Context context, Arguments arguments) {
		return mode(
			new Object[] {
				arguments.checkString(0)
			}
		);
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] active(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			computerEnabled = arguments.checkBoolean(0);
			markDirty();
		}
		return new Object[] { !computerEnabled && isEnabled };
	}
	
	private Object[] mode(Object[] arguments) {
		if (arguments.length == 1) {
			if (arguments[0].toString().equals("up")) {
				computerMode = MODE_UP;
			} else if (arguments[0].toString().equals("down")) {
				computerMode = MODE_DOWN;
			} else {
				computerMode = MODE_REDSTONE;
			}
			markDirty();
		}
		
		switch (computerMode) {
		case MODE_REDSTONE:
			return new Object[] { "redstone" };
		case MODE_UP:
			return new Object[] { "up" };
		case MODE_DOWN:
			return new Object[] { "down" };
		default:
			break;
		}
		return null;
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		String methodName = getMethodName(method);
		
		if (methodName.equals("mode")) {
			return mode(arguments);
			
		} else if (methodName.equals("active")) {
			if (arguments.length == 1) {
				computerEnabled = toBool(arguments[0]);
			}
			return new Object[] { !computerEnabled && isEnabled };
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
}

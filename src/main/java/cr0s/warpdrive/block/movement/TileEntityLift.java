package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.computer.ILift;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumLiftMode;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityLift extends TileEntityAbstractEnergy implements ILift {
	
	final double LIFT_GRAB_RADIUS = 0.4;
	
	// persistent properties
	private EnumLiftMode mode = EnumLiftMode.INACTIVE;
	private boolean isEnabled = true;
	private EnumLiftMode computerMode = EnumLiftMode.REDSTONE;
	
	// computed properties
	private int updateTicks = 0;
	private boolean isActive = false;
	private boolean isValid = false;
	private int firstUncoveredY;
	
	public TileEntityLift() {
		super();
		IC2_sinkTier = 2;
		IC2_sourceTier = 2;
		peripheralName = "warpdriveLift";
		addMethods(new String[] {
				"enable",
				"mode",
				"state"
		});
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		updateTicks--;
		if (updateTicks < 0) {
			updateTicks = WarpDriveConfig.LIFT_UPDATE_INTERVAL_TICKS;
			
			// Switching mode
			if (  computerMode == EnumLiftMode.DOWN
			  || (computerMode == EnumLiftMode.REDSTONE && worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord))) {
				mode = EnumLiftMode.DOWN;
			} else {
				mode = EnumLiftMode.UP;
			}
			
			isValid = isPassableBlock(yCoord + 1)
			       && isPassableBlock(yCoord + 2)
			       && isPassableBlock(yCoord - 1)
			       && isPassableBlock(yCoord - 2);
			isActive = isEnabled && isValid;
			
			if (energy_getEnergyStored() < WarpDriveConfig.LIFT_ENERGY_PER_ENTITY || !isActive) {
				mode = EnumLiftMode.INACTIVE;
				if (getBlockMetadata() != 0) {
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 2); // disabled
				}
				return;
			}
			
			if (getBlockMetadata() != mode.ordinal()) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, mode.ordinal(), 2); // current mode
			}
			
			// Launch a beam: search non-air blocks under lift
			for (int ny = yCoord - 2; ny > 0; ny--) {
				if (!isPassableBlock(ny)) {
					firstUncoveredY = ny + 1;
					break;
				}
			}
			
			if (yCoord - firstUncoveredY >= 2) {
				if (mode == EnumLiftMode.UP) {
					PacketHandler.sendBeamPacket(worldObj,
							new Vector3(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D),
							new Vector3(xCoord + 0.5D, yCoord, zCoord + 0.5D),
							0f, 1f, 0f, 40, 0, 100);
				} else if (mode == EnumLiftMode.DOWN) {
					PacketHandler.sendBeamPacket(worldObj,
							new Vector3(xCoord + 0.5D, yCoord, zCoord + 0.5D),
							new Vector3(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D), 0f,
							0f, 1f, 40, 0, 100);
				}
				
				if (liftEntity()) {
					updateTicks = WarpDriveConfig.LIFT_ENTITY_COOLDOWN_TICKS;
				}
			}
		}
	}
	
	private boolean isPassableBlock(int yPosition) {
		Block block = worldObj.getBlock(xCoord, yPosition, zCoord);
		return block.isAssociatedBlock(Blocks.air)
			|| worldObj.isAirBlock(xCoord, yPosition, zCoord)
			|| block.getCollisionBoundingBoxFromPool(worldObj, xCoord, yPosition, zCoord) == null;
	}
	
	private boolean liftEntity() {
		final double xMin = xCoord + 0.5 - LIFT_GRAB_RADIUS;
		final double xMax = xCoord + 0.5 + LIFT_GRAB_RADIUS;
		final double zMin = zCoord + 0.5 - LIFT_GRAB_RADIUS;
		final double zMax = zCoord + 0.5 + LIFT_GRAB_RADIUS;
		boolean isTransferDone = false; 
		
		// Lift up
		if (mode == EnumLiftMode.UP) {
			final AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
					xMin, firstUncoveredY, zMin,
					xMax, yCoord, zMax);
			final List list = worldObj.getEntitiesWithinAABBExcludingEntity(null, aabb);
			if (list != null) {
				for (Object object : list) {
					if ( object != null
					  && object instanceof EntityLivingBase
					  && energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, true)) {
						((EntityLivingBase) object).setPositionAndUpdate(xCoord + 0.5D, yCoord + 1.0D, zCoord + 0.5D);
						PacketHandler.sendBeamPacket(worldObj,
								new Vector3(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D),
								new Vector3(xCoord + 0.5D, yCoord, zCoord + 0.5D),
								1F, 1F, 0F, 40, 0, 100);
						worldObj.playSoundEffect(xCoord + 0.5D, yCoord, zCoord + 0.5D, "warpdrive:hilaser", 4F, 1F);
						energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, false);
						isTransferDone = true;
					}
				}
			}
			
		} else if (mode == EnumLiftMode.DOWN) {
			final AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
					xMin, Math.min(firstUncoveredY + 4.0D, yCoord), zMin,
					xMax, yCoord + 2.0D, zMax);
			final List list = worldObj.getEntitiesWithinAABBExcludingEntity(null, aabb);
			if (list != null) {
				for (Object object : list) {
					if ( object != null
					  && object instanceof EntityLivingBase
					  && energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, true)) {
						((EntityLivingBase) object).setPositionAndUpdate(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D);
						PacketHandler.sendBeamPacket(worldObj,
								new Vector3(xCoord + 0.5D, yCoord, zCoord + 0.5D),
								new Vector3(xCoord + 0.5D, firstUncoveredY, zCoord + 0.5D), 1F, 1F, 0F, 40, 0, 100);
						worldObj.playSoundEffect(xCoord + 0.5D, yCoord, zCoord + 0.5D, "warpdrive:hilaser", 4F, 1F);
						energy_consume(WarpDriveConfig.LIFT_ENERGY_PER_ENTITY, false);
						isTransferDone = true;
					}
				}
			}
		}
		
		return isTransferDone;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		if (tag.hasKey("mode")) {
			final byte byteValue = tag.getByte("mode");
			mode = EnumLiftMode.get(Commons.clamp(0, 3, byteValue == -1 ? 3 : byteValue));
		}
		if (tag.hasKey("computerEnabled")) {
			isEnabled = tag.getBoolean("computerEnabled");  // up to 1.3.30 included
		} else if (tag.hasKey("isEnabled")) {
			isEnabled = tag.getBoolean("isEnabled");
		}
		if (tag.hasKey("computerMode")) {
			final byte byteValue = tag.getByte("computerMode");
			computerMode = EnumLiftMode.get(Commons.clamp(0, 3, byteValue == -1 ? 3 : byteValue));
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setByte("mode", (byte) mode.ordinal());
		tag.setBoolean("isEnabled", isEnabled);
		tag.setByte("computerMode", (byte) computerMode.ordinal());
	}
	
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.LIFT_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(ForgeDirection from) {
		return true;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] enable(Object[] arguments) {
		if (arguments.length == 1) {
			isEnabled = Commons.toBool(arguments[0]);
			markDirty();
		}
		return new Object[] { isEnabled };
	}
	
	@Override
	public Object[] mode(Object[] arguments) {
		if (arguments.length == 1 && arguments[0] instanceof String) {
			final String stringValue = (String) arguments[0];
			if (stringValue.equalsIgnoreCase("up")) {
				computerMode = EnumLiftMode.UP;
			} else if (stringValue.equalsIgnoreCase("down")) {
				computerMode = EnumLiftMode.DOWN;
			} else {
				computerMode = EnumLiftMode.REDSTONE;
			}
			markDirty();
		}
		
		return new Object[] { computerMode.getName() };
	}
	
	@Override
	public Object[] state() {
		final int energy = energy_getEnergyStored();
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isActive, energy, isValid, isEnabled, computerMode.getName() };
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] enable(Context context, Arguments arguments) {
		return enable(argumentsOCtoCC(arguments));
	}
	
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
	public Object[] state(Context context, Arguments arguments) {
		return state();
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		switch (methodName) {
		case "enable":
			return enable(arguments);
			
		case "mode":
			return mode(arguments);
		
		case "state":
			return state();
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
}

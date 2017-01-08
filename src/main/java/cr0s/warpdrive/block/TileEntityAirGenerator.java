package cr0s.warpdrive.block;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.Optional;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

public class TileEntityAirGenerator extends TileEntityAbstractEnergy {
	private int cooldownTicks = 0;
	private boolean isEnabled = true;
	private static final int START_CONCENTRATION_VALUE = 15;
	
	public TileEntityAirGenerator() {
		super();
		
		peripheralName = "warpdriveAirGenerator";
		addMethods(new String[] {
				"enable"
		});
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		if (isInvalid()) {
			return;
		}
		
		// Air generator works only in space & hyperspace
		if (worldObj.provider.dimensionId != WarpDriveConfig.G_SPACE_DIMENSION_ID && worldObj.provider.dimensionId != WarpDriveConfig.G_HYPERSPACE_DIMENSION_ID) {
			if (getBlockMetadata() != 0) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 2); // set disabled texture
			}
			return;
		}

		cooldownTicks++;
		if (cooldownTicks > WarpDriveConfig.AIRGEN_AIR_GENERATION_TICKS) {
			if (isEnabled && energy_consume(WarpDriveConfig.AIRGEN_ENERGY_PER_NEWAIRBLOCK, true)) {
				if (getBlockMetadata() != 1) {
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 2); // set enabled texture
				}
			} else {
				if (getBlockMetadata() != 0) {
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 2); // set disabled texture
				}
			}
			releaseAir(1, 0, 0);
			releaseAir(-1, 0, 0);
			releaseAir(0, 1, 0);
			releaseAir(0, -1, 0);
			releaseAir(0, 0, 1);
			releaseAir(0, 0, -1);
			
			cooldownTicks = 0;
		}
	}
	
	private void releaseAir(int xOffset, int yOffset, int zOffset) {
		Block block = worldObj.getBlock(xCoord + xOffset, yCoord + yOffset, zCoord + zOffset);
		if (block.isAir(worldObj, xCoord + xOffset, yCoord + yOffset, zCoord + zOffset)) {// can be air
			int energy_cost = (!block.isAssociatedBlock(WarpDrive.blockAir)) ? WarpDriveConfig.AIRGEN_ENERGY_PER_NEWAIRBLOCK : WarpDriveConfig.AIRGEN_ENERGY_PER_EXISTINGAIRBLOCK;
			if (isEnabled && energy_consume(energy_cost, true)) {// enough energy and enabled
				if (worldObj.setBlock(xCoord + xOffset, yCoord + yOffset, zCoord + zOffset, WarpDrive.blockAir, START_CONCENTRATION_VALUE, 2)) {
					// (needs to renew air or was not maxed out)
					energy_consume(WarpDriveConfig.AIRGEN_ENERGY_PER_NEWAIRBLOCK, false);
				} else {
					energy_consume(WarpDriveConfig.AIRGEN_ENERGY_PER_EXISTINGAIRBLOCK, false);
				}
			} else {// low energy => remove air block
				if (block.isAssociatedBlock(WarpDrive.blockAir)) {
					int metadata = worldObj.getBlockMetadata(xCoord + xOffset, yCoord + yOffset, zCoord + zOffset);
					if (metadata > 4) {
						worldObj.setBlockMetadataWithNotify(xCoord + xOffset, yCoord + yOffset, zCoord + zOffset, metadata - 4, 2);
					} else if (metadata > 1) {
						worldObj.setBlockMetadataWithNotify(xCoord + xOffset, yCoord + yOffset, zCoord + zOffset, 1, 2);
					} else {
						// worldObj.setBlockMetadataWithNotify(xCoord + xOffset, yCoord + yOffset,  zCoord + zOffset, 0, 0, 2);
					}
				}
			}
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		isEnabled = tag.getBoolean("isEnabled");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setBoolean("isEnabled", isEnabled);
	}
	
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.AIRGEN_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(ForgeDirection from) {
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("%s @ \'%s\' (%d %d %d)",
		getClass().getSimpleName(),
		worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(),
		xCoord, yCoord, zCoord);
	}
	
	public Object[] enable(Object[] arguments) {
		if (arguments.length == 1) {
			isEnabled = toBool(arguments[0]);
		}
		return new Object[] { isEnabled };
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] enable(Context context, Arguments arguments) {
			return enable(argumentsOCtoCC(arguments));
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		String methodName = getMethodName(method);
		
		switch (methodName) {
			case "enable": 
				return enable(arguments);		
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
}

package cr0s.warpdrive.machines;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;

import java.util.HashMap;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import cr0s.warpdrive.api.IBlockUpdateDetector;
import cr0s.warpdrive.data.EnumUpgradeTypes;

public abstract class WarpEnergyTE extends WarpInterfacedTE implements IEnergyHandler, IEnergySink, IEnergySource, IBlockUpdateDetector {
	protected boolean addedToEnergyNet = false;
	protected int energyStored_internal = 0;
	private static final double EU_PER_INTERNAL = 1.0D;
	private static final double RF_PER_INTERNAL = 1800.0D / 437.5D;
	
	private int scanTickCount = -1;
	private IEnergyHandler[] TE_energyHandlers = new IEnergyHandler[ForgeDirection.VALID_DIRECTIONS.length];
	protected HashMap<EnumUpgradeTypes,Integer> upgrades = new HashMap<EnumUpgradeTypes,Integer>();
 	
	public Object[] getUpgrades()
	{
		Object[] retVal = new Object[EnumUpgradeTypes.values().length];
		for(EnumUpgradeTypes type : EnumUpgradeTypes.values())
		{
			int am = 0;
			if(upgrades.containsKey(type))
				am = upgrades.get(type);
			retVal[type.ordinal()] = type.toString() + ":" + am;
		}	
		return retVal;
	}
	
	// WarpDrive methods
	protected static int convertInternalToRF(int energy) {
		return (int)Math.round(energy * RF_PER_INTERNAL);
	}
	
	protected static int convertRFtoInternal(int energy) {
		return (int)Math.round(energy / RF_PER_INTERNAL);
	}
	
	protected static double convertInternalToEU(int energy) {
		return Math.round(energy * EU_PER_INTERNAL);
	}
	
	protected static int convertEUtoInternal(double amount) {
		return (int)Math.round(amount / EU_PER_INTERNAL);
	}

	
	public int getEnergyStored() {
		return energyStored_internal;
	}
	
	// Methods to override
	/**
	 * Should return the maximum amount of energy that can be stored (measured in internal energy units).
	 */
	public int getMaxEnergyStored() {
		return 0;
	}
	
	/**
	 * Should return the maximum amount of energy that can be output (measured in internal energy units).
	 */
	public int getPotentialEnergyOutput() {
		return 0;
	}
	
	/**
	 * Remove energy from storage, called after actual output happened (measured in internal energy units).
	 * Override this to use custom storage or measure output statistics.
	 */
	protected void energyOutputDone(int energyOutput_internal) {
		consumeEnergy(energyOutput_internal, false);
	}
	
	/**
	 * Should return true if that direction can receive energy.
	 */
	public boolean canInputEnergy(ForgeDirection from) {
		return false;
	}
	
	/**
	 * Should return true if that direction can output energy.
	 */
	public boolean canOutputEnergy(ForgeDirection to) {
		return false;
	}
	
	/**
	 * Consume energy from storage for internal usage or after outputting (measured in internal energy units).
	 * Override this to use custom storage or measure energy consumption statistics (internal usage or output).
	 */
	protected boolean consumeEnergy(int amount_internal, boolean simulate) {
		int amountUpgraded = amount_internal;
		if (upgrades.containsKey(EnumUpgradeTypes.Power)) {
			double valueMul = Math.pow(0.8,upgrades.get(EnumUpgradeTypes.Power));
			amountUpgraded = (int) Math.ceil(valueMul * amountUpgraded);
		}
		
		if (upgrades.containsKey(EnumUpgradeTypes.Range)) {
			double valueMul = Math.pow(1.2,upgrades.get(EnumUpgradeTypes.Range));
			amountUpgraded = (int) Math.ceil(valueMul * amountUpgraded);
		}
		
		if (upgrades.containsKey(EnumUpgradeTypes.Speed)) {
			double valueMul = Math.pow(1.2,upgrades.get(EnumUpgradeTypes.Speed));
			amountUpgraded = (int) Math.ceil(valueMul * amountUpgraded);
		}
		// FIXME: upgrades balancing & implementation to be done...
		
		if (getEnergyStored() >= amount_internal) {
			if (!simulate) {
				energyStored_internal -= amount_internal;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Consume all internal energy and return it's value (measured in internal energy units).
	 * Override this to use custom storage or measure energy consumption statistics of this kind.
	 */
	protected int consumeAllEnergy() {
		int temp = energyStored_internal;
		energyStored_internal = 0;
		return temp;
	}
	
	public Object[] getEnergyLevel() {
		return new Object[] { getEnergyStored(), getMaxEnergyStored() };
	}

    public String getStatus() {
    	if (getMaxEnergyStored() != 0) {
    		return getBlockType().getLocalizedName() + String.format(" energy level is %.0f/%.0f EU.", convertInternalToEU(getEnergyStored()), convertInternalToEU(getMaxEnergyStored()));
    	} else {
    		return getBlockType().getLocalizedName();
    	}
    }
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] getEnergyLevel(Context context, Arguments arguments) {
		return getEnergyLevel();
	}
    
	// Minecraft overrides
    @Override
    public void updateEntity() {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            return;
        }
        
        // IndustrialCraft2
        if (!addedToEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            addedToEnergyNet = true;
        }
        
        // Thermal Expansion
		scanTickCount++;
		if(scanTickCount >= 20) {
			scanTickCount = 0;
			scanForEnergyHandlers();
		}
		outputEnergy();
    }

    @Override
    public void onChunkUnload() {
        if (addedToEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            addedToEnergyNet = false;
        }
        
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        if (addedToEnergyNet) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            addedToEnergyNet = false;
        }
        
        super.invalidate();
    }
    
    
    // IndustrialCraft IEnergySink interface
    @Override
    public double getDemandedEnergy() {
        return Math.max(0.0D, convertInternalToEU(getMaxEnergyStored() - energyStored_internal));
    }
    
    @Override
    public double injectEnergy(ForgeDirection from, double amount_EU, double voltage) {
        int leftover_internal = 0;
        energyStored_internal += convertEUtoInternal(amount_EU);
        
        if (energyStored_internal > getMaxEnergyStored()) {
        	leftover_internal = (energyStored_internal - getMaxEnergyStored());
            energyStored_internal = getMaxEnergyStored();
        }
        
        return convertInternalToEU(leftover_internal);
    }
    
    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection from) {
        return canInputEnergy(from);
    }
    
    // IndustrialCraft IEnergySource interface
	@Override
	public double getOfferedEnergy() {
		return convertInternalToEU(getPotentialEnergyOutput());
	}
	
	@Override
	public void drawEnergy(double amount_EU) {
		energyOutputDone(convertEUtoInternal(amount_EU));
	}
	
	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection to) {
		return canOutputEnergy(to);
	}
	
    
    // ThermalExpansion IEnergyHandler interface
	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive_RF, boolean simulate) {
		if (!canInputEnergy(from)) {
			return 0;
		}
		
		int maxStored_RF = getMaxEnergyStored(from);
		if (maxStored_RF == 0) {
			return 0;
		}
		int energyStored_RF = getEnergyStored(from);
		
		int toAdd_RF = Math.min(maxReceive_RF, maxStored_RF - energyStored_RF);
		if (!simulate) {
			energyStored_internal = Math.min(getMaxEnergyStored(), energyStored_internal + convertRFtoInternal(toAdd_RF));
		}
		
		return toAdd_RF;
	}
	
	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract_RF, boolean simulate) {
		if (!canOutputEnergy(from)) {
			return 0;
		}
		
		int potentialEnergyOutput_internal = getPotentialEnergyOutput();
		int energyExtracted_internal = Math.min(convertRFtoInternal(maxExtract_RF), potentialEnergyOutput_internal);
		if (!simulate) {
			energyOutputDone(energyExtracted_internal);
			// WarpDrive.debugPrint("extractEnergy Potential " + potentialEnergyOutput_internal + " EU, Requested " + maxExtract_RF + " RF, energyExtracted_internal " + energyExtracted_internal + "(" + convertInternalToRF(energyExtracted_internal) + " RF)");
		}
		return convertInternalToRF(energyExtracted_internal);
	}
	
	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
		return (getMaxEnergyStored() != 0) && (canInputEnergy(from) || canOutputEnergy(from)); // FIXME deadlock risk
	}
	
	@Override
	public int getEnergyStored(ForgeDirection from) {
		if (canConnectEnergy(from)) {
			return convertInternalToRF(getEnergyStored());
		}
		return 0;
	}
	
	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		return canConnectEnergy(from) ? convertInternalToRF(getMaxEnergyStored()) : 0;
	}
	
	
	// WarpDrive overrides for Thermal Expansion FIXME: are we really supposed to do this?
	private void outputEnergy(ForgeDirection from, IEnergyHandler ieh) {
		if (ieh == null || worldObj.getTileEntity(xCoord + from.offsetX, yCoord + from.offsetY, zCoord + from.offsetZ) == null) {
			return;
		}
		int potentialEnergyOutput_internal = getPotentialEnergyOutput();
		if (potentialEnergyOutput_internal > 0) {
			int energyToOutput_RF = ieh.receiveEnergy(from.getOpposite(), convertInternalToRF(potentialEnergyOutput_internal), true);
			if (energyToOutput_RF > 0) {
				int energyOutputed_RF = ieh.receiveEnergy(from.getOpposite(), energyToOutput_RF, false);
				energyOutputDone(convertRFtoInternal(energyOutputed_RF));
				// WarpDrive.debugPrint("ForcedOutputEnergy Potential " + potentialEnergyOutput_internal + " EU, Actual output " + energyOutputed_RF + " RF, simulated at " + energyToOutput_RF + " RF");
			}
		}
	}
	
	private void outputEnergy() {
		for(ForgeDirection from: ForgeDirection.VALID_DIRECTIONS) {
			if (TE_energyHandlers[from.ordinal()] != null) {
				outputEnergy(from, TE_energyHandlers[from.ordinal()]);
			}
		}
	}
	
	
	// Forge overrides
	@Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        energyStored_internal = tag.getInteger("energy");
        if (energyStored_internal > getMaxEnergyStored()) {
        	energyStored_internal = getMaxEnergyStored();
        }
        if (tag.hasKey("upgrades")) {
        	NBTTagCompound upgradeTag = tag.getCompoundTag("upgrades");
        	for(EnumUpgradeTypes type : EnumUpgradeTypes.values()) {
        		if (upgradeTag.hasKey(type.toString()) && upgradeTag.getInteger(type.toString()) !=  0) {
	        		upgrades.put(type, upgradeTag.getInteger(type.toString()));
        		}
        	}
        }
    }
	
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (energyStored_internal < 0) {
        	energyStored_internal = 0;
        }
        tag.setInteger("energy", this.energyStored_internal);
        if (upgrades.size() > 0) {
        	NBTTagCompound upgradeTag = new NBTTagCompound();
        	for(EnumUpgradeTypes type : EnumUpgradeTypes.values()) {
        		if (upgrades.containsKey(type)) {
        			upgradeTag.setInteger(type.toString(), upgrades.get(type));
        		}
        	}
        	tag.setTag("upgrades", upgradeTag);
        }
    }
    
    // WarpDrive overrides
	@Override
	public void updatedNeighbours() {
		scanForEnergyHandlers();
	}
	
	public void scanForEnergyHandlers() {
		for(ForgeDirection from : ForgeDirection.VALID_DIRECTIONS) {
			boolean iehFound = false;
			if (canConnectEnergy(from)) {
				TileEntity te = worldObj.getTileEntity(xCoord + from.offsetX, yCoord + from.offsetY, zCoord + from.offsetZ);
				if (te != null && te instanceof IEnergyHandler) {
					IEnergyHandler ieh = (IEnergyHandler)te;
					if (ieh.canConnectEnergy(from.getOpposite())) {
						iehFound = true;
						TE_energyHandlers[from.ordinal()] = ieh;
					}
				}
			}
			if (!iehFound) {
				TE_energyHandlers[from.ordinal()] = null;
			}
		}
	}
}
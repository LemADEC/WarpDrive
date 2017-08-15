package cr0s.warpdrive.block;

import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyReceiver;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

@Optional.InterfaceList({
	@Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHCore"),
	@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2"),
	@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "IC2")
})
public abstract class TileEntityAbstractEnergy extends TileEntityAbstractInterfaced implements IEnergyHandler, IEnergySink, IEnergySource, cr0s.warpdrive.api.computer.IEnergy {
	private boolean addedToEnergyNet = false;
	private long energyStored_internal = 0;
	public static final double EU_PER_INTERNAL = 1.0D;
	public static final double RF_PER_INTERNAL = 1800.0D / 437.5D;
	public static final int IC2_sinkTier_max = Integer.MAX_VALUE;
	public static final int IC2_sourceTier_max = 20;
	protected int IC2_sinkTier = 3;
	protected int IC2_sourceTier = 3;
	
	private static final int SCAN_INTERVAL_TICKS = 20;
	private int scanTickCount = SCAN_INTERVAL_TICKS;
	
	private Object[] cofhEnergyReceivers;
	
	public TileEntityAbstractEnergy() {
		super();
		if (WarpDriveConfig.isCoFHCoreLoaded) {
			CoFH_initialiseAPI();
		}
		addMethods(new String[] { "energy" });
	}
	
	// WarpDrive methods
	public static int convertInternalToRF_ceil(final long energy) {
		return (int) Math.ceil(energy * RF_PER_INTERNAL);
	}
	
	public static int convertInternalToRF_floor(final long energy) {
		return (int) Math.floor(energy * RF_PER_INTERNAL);
	}
	
	public static long convertRFtoInternal_ceil(final int energy) {
		return (long) Math.ceil(energy / RF_PER_INTERNAL);
	}
	
	public static long convertRFtoInternal_floor(final int energy) {
		return (long) Math.floor(energy / RF_PER_INTERNAL);
	}
	
	public static double convertInternalToEU_ceil(final long energy) {
		return Math.ceil(energy * EU_PER_INTERNAL);
	}
	
	public static double convertInternalToEU_floor(final long energy) {
		return Math.floor(energy * EU_PER_INTERNAL);
	}
	
	public static long convertEUtoInternal_ceil(final double amount) {
		return (long) Math.ceil(amount / EU_PER_INTERNAL);
	}
	
	public static long convertEUtoInternal_floor(final double amount) {
		return (long) Math.floor(amount / EU_PER_INTERNAL);
	}
	
	public int energy_getEnergyStored() {
		return (int) Commons.clamp(0L, energy_getMaxStorage(), energyStored_internal);
	}
	
	// Methods to override
	
	/**
	 * Return the maximum amount of energy that can be stored (measured in internal energy units).
	 */
	public int energy_getMaxStorage() {
		return 0;
	}
	
	/**
	 * Return the maximum amount of energy that can be output (measured in internal energy units).
	 */
	public int energy_getPotentialOutput() {
		return 0;
	}
	
	/**
	 * Remove energy from storage, called after actual output happened (measured in internal energy units).
	 * Override this to use custom storage or measure output statistics.
	 */
	protected void energy_outputDone(final long energyOutput_internal) {
		energy_consume(energyOutput_internal);
	}
	
	/**
	 * Should return true if that direction can receive energy.
	 */
	@SuppressWarnings("UnusedParameters")
	public boolean energy_canInput(ForgeDirection from) {
		return false;
	}
	
	/**
	 * Should return true if that direction can output energy.
	 */
	@SuppressWarnings("UnusedParameters")
	public boolean energy_canOutput(ForgeDirection to) {
		return false;
	}
	
	/**
	 * Consume energy from storage for internal usage or after outputting (measured in internal energy units).
	 * Override this to use custom storage or measure energy consumption statistics (internal usage or output).
	 */
	public boolean energy_consume(final long amount_internal, final boolean simulate) {
		if (energy_getEnergyStored() >= amount_internal) {
			if (!simulate) {
				energy_consume(amount_internal);
			}
			return true;
		}
		return false;
	}
	public void energy_consume(final long amount_internal) {
		energyStored_internal -= amount_internal;
	}
	
	public String getEnergyStatus() {
		if (energy_getMaxStorage() == 0) {
			return "";
		}
		return StatCollector.translateToLocalFormatted("warpdrive.energy.statusLine",
			Commons.format((long) convertInternalToEU_floor(energy_getEnergyStored())),
			Commons.format((long) convertInternalToEU_floor(energy_getMaxStorage())) );
	}
	
	@Override
	public String getStatus() {
		final String strEnergyStatus = getEnergyStatus();
		return super.getStatus()
		       + (strEnergyStatus.isEmpty() ? "" : "\n" + strEnergyStatus);
	}
	
	// Common OC/CC methods
	@Override
	public Object[] energy() {
		return new Object[] { energy_getEnergyStored(), energy_getMaxStorage() };
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] energy(Context context, Arguments arguments) {
		return energy();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		if (methodName.equals("energy")) {
			return energy();
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	// Minecraft overrides
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " updateEntity");
		}
		
		// IndustrialCraft2
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			IC2_addToEnergyNet();
		}
		
		// Thermal Expansion
		if (WarpDriveConfig.isCoFHCoreLoaded) {
			scanTickCount--;
			if (scanTickCount <= 0) {
				scanTickCount = SCAN_INTERVAL_TICKS;
				CoFH_scanForEnergyHandlers();
			}
			CoFH_outputEnergy();
		}
	}
	
	@Override
	public void onChunkUnload() {
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			IC2_removeFromEnergyNet();
		}
		
		super.onChunkUnload();
	}
	
	@Override
	public void invalidate() {
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			IC2_removeFromEnergyNet();
		}
		
		super.invalidate();
	}
	
	// IndustrialCraft IEnergySink interface
	@Override
	@Optional.Method(modid = "IC2")
	public double getDemandedEnergy() {
		return Math.max(0.0D, convertInternalToEU_floor(energy_getMaxStorage() - energy_getEnergyStored()));
	}
	
	@Override
	@Optional.Method(modid = "IC2")
	public double injectEnergy(ForgeDirection from, double amount_EU, double voltage) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " [IC2]injectEnergy from " + from  + "(" + energy_canInput(from) + ") amount " + amount_EU + " voltage " + voltage);
		}
		if (energy_canInput(from)) {
			long leftover_internal = 0;
			energyStored_internal += convertEUtoInternal_floor(amount_EU);
			
			if (energyStored_internal > energy_getMaxStorage()) {
				leftover_internal = (energyStored_internal - energy_getMaxStorage());
				energyStored_internal = energy_getMaxStorage();
			}
			
			return convertInternalToEU_floor(leftover_internal);
		} else {
			return amount_EU;
		}
	}
	
	@Override
	@Optional.Method(modid = "IC2")
	public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection from) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " [IC2]acceptsEnergyFrom emitter " + emitter + " from " + from + " => " + energy_canInput(from));
		}
		return energy_canInput(from);
	}
	
	// IndustrialCraft IEnergySource interface
	@Override
	@Optional.Method(modid = "IC2")
	public double getOfferedEnergy() {
		return convertInternalToEU_floor(energy_getPotentialOutput());
	}
	
	@Override
	@Optional.Method(modid = "IC2")
	public void drawEnergy(double amount_EU) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " [IC2]drawEnergy amount_EU " + amount_EU);
		}
		energy_outputDone(convertEUtoInternal_ceil(amount_EU));
	}
	
	@Override
	@Optional.Method(modid = "IC2")
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection to) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " [IC2]emitsEnergyTo receiver " + receiver + " to " + to + " => " + energy_canOutput(to));
		}
		return energy_canOutput(to);
	}
	
	@Optional.Method(modid = "IC2")
	private void IC2_addToEnergyNet() {
		if (!addedToEnergyNet) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
			addedToEnergyNet = true;
		}
	}
	
	@Optional.Method(modid = "IC2")
	private void IC2_removeFromEnergyNet() {
		if (addedToEnergyNet) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			addedToEnergyNet = false;
		}
	}
	
	@Override
	@Optional.Method(modid = "IC2")
	public int getSinkTier() {
		return IC2_sinkTier;
	}
	
	@Override
	@Optional.Method(modid = "IC2")
	public int getSourceTier() {
		return IC2_sourceTier;
	}
	
	
	// ThermalExpansion IEnergyHandler interface
	@Override
	@Optional.Method(modid = "CoFHCore")	/* IEnergyReceiver */
	public int receiveEnergy(ForgeDirection from, int maxReceive_RF, boolean simulate) {
		if (!energy_canInput(from)) {
			return 0;
		}
		
		final int maxStored_RF = getMaxEnergyStored(from);
		if (maxStored_RF == 0) {
			return 0;
		}
		final int energyStored_RF = getEnergyStored(from);
		
		final int toAdd_RF = Math.min(maxReceive_RF, maxStored_RF - energyStored_RF);
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " [CoFH]receiveEnergy from " + from + " maxReceive_RF " + maxReceive_RF + " simulate " + simulate + " energy_RF " + energyStored_RF + "/" + maxStored_RF + " toAdd_RF " + toAdd_RF);
		}
		if (!simulate) {
			energyStored_internal = Math.min(energy_getMaxStorage(), energy_getEnergyStored() + convertRFtoInternal_floor(toAdd_RF));
		}
		
		return toAdd_RF;
	}
	
	@Override
	@Optional.Method(modid = "CoFHCore")	/* IEnergyProvider */
	public int extractEnergy(ForgeDirection from, int maxExtract_RF, boolean simulate) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " [CoFH]extractEnergy from " + from + " maxExtract_RF " + maxExtract_RF + " simulate " + simulate);
		}
		if (!energy_canOutput(from)) {
			return 0;
		}
		
		final long potentialEnergyOutput_internal = energy_getPotentialOutput();
		final long energyExtracted_internal = Math.min(convertRFtoInternal_ceil(maxExtract_RF), potentialEnergyOutput_internal);
		if (!simulate) {
			energy_outputDone(energyExtracted_internal);
		}
		return convertInternalToRF_floor(energyExtracted_internal);
	}
	
	@Override
	@Optional.Method(modid = "CoFHCore")	/* IEnergyConnection */
	public boolean canConnectEnergy(ForgeDirection from) {
		return (energy_getMaxStorage() != 0) && (energy_canInput(from) || energy_canOutput(from)); // Warning: deadlock risk depending on child implementation
	}
	
	@Override
	@Optional.Method(modid = "CoFHCore")	/* IEnergyReceiver and IEnergyProvider */
	public int getEnergyStored(ForgeDirection from) {
		return canConnectEnergy(from) ? convertInternalToRF_floor(energy_getEnergyStored()) : 0;
	}
	
	@Override
	@Optional.Method(modid = "CoFHCore")	/* IEnergyReceiver and IEnergyProvider */
	public int getMaxEnergyStored(ForgeDirection from) {
		return canConnectEnergy(from) ? convertInternalToRF_floor(energy_getMaxStorage()) : 0;
	}
	
	
	// WarpDrive overrides for Thermal Expansion
	@Optional.Method(modid = "CoFHCore")
	private void CoFH_outputEnergy(ForgeDirection from, IEnergyReceiver energyReceiver) {
		if (energyReceiver == null || worldObj.getTileEntity(xCoord + from.offsetX, yCoord + from.offsetY, zCoord + from.offsetZ) == null) {
			return;
		}
		if (!energy_canOutput(from)) {
			return;
		}
		final long potentialEnergyOutput_internal = energy_getPotentialOutput();
		if (potentialEnergyOutput_internal > 0) {
			final int potentialEnergyOutput_RF = convertInternalToRF_floor(potentialEnergyOutput_internal);
			final int energyToOutput_RF = energyReceiver.receiveEnergy(from.getOpposite(), potentialEnergyOutput_RF, true);
			if (energyToOutput_RF > 0) {
				final int energyOutputted_RF = energyReceiver.receiveEnergy(from.getOpposite(), energyToOutput_RF, false);
				energy_outputDone(convertRFtoInternal_ceil(energyOutputted_RF));
			}
		}
	}
	
	@Optional.Method(modid = "CoFHCore")
	private void CoFH_outputEnergy() {
		for (ForgeDirection from : ForgeDirection.VALID_DIRECTIONS) {
			if (cofhEnergyReceivers[from.ordinal()] != null) {
				CoFH_outputEnergy(from, (IEnergyReceiver) cofhEnergyReceivers[from.ordinal()]);
			}
		}
	}
	
	@Optional.Method(modid = "CoFHCore")
	private void CoFH_initialiseAPI() {
		cofhEnergyReceivers = new IEnergyReceiver[ForgeDirection.VALID_DIRECTIONS.length];
	}
	
	
	// Forge overrides
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		energyStored_internal = tag.getLong("energy");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setLong("energy", energy_getEnergyStored());
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound nbtTagCompound) {
		nbtTagCompound = super.writeItemDropNBT(nbtTagCompound);
		nbtTagCompound.removeTag("energy");
		return nbtTagCompound;
	}
	
	// WarpDrive overrides
	@Override
	public void updatedNeighbours() {
		super.updatedNeighbours();
		
		if (WarpDriveConfig.isCoFHCoreLoaded) {
			CoFH_scanForEnergyHandlers();
		}
	}
	
	@SuppressWarnings("UnusedParameters")
	protected void energy_resetConnections(final EnumFacing facing) {
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			IC2_removeFromEnergyNet();
		}
		scanTickCount = -1;
	}
	
	@Optional.Method(modid = "CoFHCore")
	private void CoFH_scanForEnergyHandlers() {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " [CoFH]CoFH_scanForEnergyHandlers");
		}
		for (ForgeDirection from : ForgeDirection.VALID_DIRECTIONS) {
			boolean energyReceiverFound = false;
			if (canConnectEnergy(from)) {
				TileEntity tileEntity = worldObj.getTileEntity(xCoord + from.offsetX, yCoord + from.offsetY, zCoord + from.offsetZ);
				if (tileEntity != null && tileEntity instanceof IEnergyReceiver) {
					IEnergyReceiver energyReceiver = (IEnergyReceiver) tileEntity;
					if (energyReceiver.canConnectEnergy(from.getOpposite())) {
						energyReceiverFound = true;
						cofhEnergyReceivers[from.ordinal()] = energyReceiver;
					}
				}
			}
			if (!energyReceiverFound) {
				cofhEnergyReceivers[from.ordinal()] = null;
			}
		}
	}
}
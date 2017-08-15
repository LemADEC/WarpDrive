package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.RadarEcho;
import cr0s.warpdrive.data.StarMapRegistry;
import cr0s.warpdrive.data.Vector3;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityRadar extends TileEntityAbstractEnergy {
	
	private ArrayList<RadarEcho> results;
	
	// radius defined for next scan
	private int radius = 0;
	
	// radius for ongoing scan
	private int scanningRadius = 0;
	private int scanningDuration_ticks = 0;
	private int scanning_ticks = 0;
	
	public TileEntityRadar() {
		super();
		peripheralName = "warpdriveRadar";
		addMethods(new String[] {
				"radius",
				"getEnergyRequired",
				"start",
				"getScanDuration",
				"getResults",
				"getResultsCount",
				"getResult"
			});
		CC_scripts = Arrays.asList("scan", "ping");
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		try {
			if (getBlockMetadata() == 2) {
				scanning_ticks++;
				if (scanning_ticks > scanningDuration_ticks) {
					results = WarpDrive.starMap.getRadarEchos(this, scanningRadius);
					if (WarpDriveConfig.LOGGING_RADAR) {
						WarpDrive.logger.info(this + " Scan found " + results.size() + " results in " + scanningRadius + " radius...");
					}
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 1 + 2);
					scanning_ticks = 0;
				}
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
	}
	
	private int calculateEnergyRequired(final int parRadius) {
		return (int)Math.round(Math.max(WarpDriveConfig.RADAR_SCAN_MIN_ENERGY_COST,
				  WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[0]
				+ WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[1] * parRadius
				+ WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[2] * parRadius * parRadius
				+ WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[3] * parRadius * parRadius * parRadius));
	}
	
	private int calculateScanDuration(final int parRadius) {
		return (int)Math.round(20 * Math.max(WarpDriveConfig.RADAR_SCAN_MIN_DELAY_SECONDS,
				  WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[0]
				+ WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[1] * parRadius
				+ WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[2] * parRadius * parRadius
				+ WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[3] * parRadius * parRadius * parRadius));
	}
	
	// Common OC/CC methods
	@Override
	public Object[] position() {
		final CelestialObject celestialObject = CelestialObjectManager.get(worldObj, xCoord, zCoord);
		if (celestialObject != null) {
			Vector3 vec3Position = StarMapRegistry.getUniversalCoordinates(celestialObject, xCoord, yCoord, zCoord);
			return new Object[] { xCoord, yCoord, zCoord, celestialObject.getDisplayName(), vec3Position.x, vec3Position.y, vec3Position.z };
		} else {
			String name = worldObj.getWorldInfo().getWorldName();
			if (name == null || name.isEmpty()) {
				name = "DIM" + worldObj.provider.dimensionId;
			}
			return new Object[] { xCoord, yCoord, zCoord, name, xCoord, yCoord, zCoord };
		}
	}
	
	private Object[] radius(Object[] arguments) {
		if (arguments.length == 1 && getBlockMetadata() != 2) {
			int newRadius;
			try {
				newRadius = Commons.toInt(arguments[0]);
			} catch(Exception exception) {
				return new Integer[] { radius };
			}
			radius = Commons.clamp(0, 10000, newRadius);
		}
		return new Integer[] { radius };
	}
	
	private Object[] getEnergyRequired(Object[] arguments) {
		try {
			if (arguments.length == 1) {
				return new Object[] { calculateEnergyRequired(Commons.toInt(arguments[0])) };
			}
		} catch (Exception exception) {
			return new Integer[] { -1 };
		}
		return new Integer[] { -1 };
	}
	
	private Object[] getScanDuration(Object[] arguments) {
		try {
			if (arguments.length == 1) {
				return new Object[] { 0.050D * calculateScanDuration(Commons.toInt(arguments[0])) };
			}
		} catch (Exception exception) {
			return new Integer[] { -1 };
		}
		return new Integer[] { -1 };
	}

	private Object[] start() {
		// always clear results
		results = null;
		
		// validate parameters
		if (radius <= 0 || radius > 10000) {
			radius = 0;
			return new Object[] { false, "Invalid radius" };
		}
		int energyRequired = calculateEnergyRequired(radius);
		if (!energy_consume(energyRequired, false)) {
			return new Object[] { false, "Insufficient energy" };
		}
		
		// Begin searching
		scanningRadius = radius;
		scanningDuration_ticks = calculateScanDuration(radius);
		scanning_ticks = 0;
		if (getBlockMetadata() != 2) {
			worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 2, 1 + 2);
		}
		if (WarpDriveConfig.LOGGING_RADAR) {
			WarpDrive.logger.info(this + "Starting scan over radius " + scanningRadius + " for " + energyRequired + " EU, results expected in " + scanningDuration_ticks + " ticks");
		}
		return new Object[] { true };
	}
	
	private Object[] getResults() {
		if (results == null) {
			return null;
		}
		Object[] objectResults = new Object[results.size()];
		int index = 0;
		for (RadarEcho radarEcho : results) {
			objectResults[index++] = new Object[] {
					radarEcho.type,
					radarEcho.name == null ? "" : radarEcho.name,
					radarEcho.x, radarEcho.y, radarEcho.z,
					radarEcho.mass };
		}
		return objectResults;
	}
	
	private Object[] getResultsCount() {
		if (results != null) {
			return new Integer[] { results.size() };
		}
		return new Integer[] { -1 };
	}
	
	private Object[] getResult(Object[] arguments) {
		if (arguments.length == 1 && (results != null)) {
			int index;
			try {
				index = Commons.toInt(arguments[0]);
			} catch(Exception exception) {
				return new Object[] { false, COMPUTER_ERROR_TAG, null, 0, 0, 0 };
			}
			if (index >= 0 && index < results.size()) {
				RadarEcho radarEcho = results.get(index);
				if (radarEcho != null) {
					return new Object[] {
							true,
							radarEcho.type,
							radarEcho.name == null ? "" : radarEcho.name,
							radarEcho.x, radarEcho.y, radarEcho.z,
							radarEcho.mass };
				}
			}
		}
		return new Object[] { false, COMPUTER_ERROR_TAG, null, 0, 0, 0 };
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] radius(Context context, Arguments arguments) {
		return radius(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getEnergyRequired(Context context, Arguments arguments) {
		return getEnergyRequired(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getScanDuration(Context context, Arguments arguments) {
		return getScanDuration(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] start(Context context, Arguments arguments) {
		return start();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getResults(Context context, Arguments arguments) {
		return getResults();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getResultsCount(Context context, Arguments arguments) {
		return getResultsCount();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getResult(Context context, Arguments arguments) {
		return getResult(argumentsOCtoCC(arguments));
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public void attach(IComputerAccess computer) {
		super.attach(computer);
		if (getBlockMetadata() == 0) {
			worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 1 + 2);
		}
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public void detach(IComputerAccess computer) {
		super.detach(computer);
		// worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 1 + 2);
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		switch (methodName) {
		case "radius":
			return radius(arguments);
		
		case "getEnergyRequired":
			return getEnergyRequired(arguments);
		
		case "getScanDuration":
			return getScanDuration(arguments);
		
		case "start":
			return start();
		
		case "getResults":
			return getResults();
		
		case "getResultsCount":
			return getResultsCount();
		
		case "getResult":
			return getResult(arguments);
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.RADAR_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(ForgeDirection from) {
		return true;
	}
}

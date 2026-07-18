package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.block.TileEntityAbstractEnergyConsumer;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.RadarEcho;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.EnumRadarMode;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

public class TileEntityRadar extends TileEntityAbstractEnergyConsumer {
	
	private ArrayList<RadarEcho> results;
	
	// persistent properties
	private int radius = 0;
	
	private boolean isScanning = false;
	private int scanning_radius = 0;
	private int scanning_countdown_ticks = 0;
	
	public TileEntityRadar(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		peripheralName = "warpdriveRadar";
		addMethods(new String[] {
				"getGlobalPosition",
				"radius",
				"start",
				"getScanDuration",
				"getResults",
				"getResultsCount",
				"getResult"
			});
		CC_scripts = Arrays.asList("scan", "ping");
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.RADAR_MAX_ENERGY_STORED,
		                     65536, 0,
		                     "EV", 2, "EV", 0);
	}
	
	@Override
	public void tick() {
		super.tick();
		
		assert world != null;
		if (world.isRemote()) {
			return;
		}
		
		final BlockState blockState = world.getBlockState(pos);
		if (!isScanning) {
			if (computer_isConnected()) {
				updateBlockState(blockState, BlockRadar.MODE, EnumRadarMode.ACTIVE);
			} else {
				updateBlockState(blockState, BlockRadar.MODE, EnumRadarMode.INACTIVE);
			}
		} else {
			updateBlockState(blockState, BlockRadar.MODE, EnumRadarMode.SCANNING);
			try {
				scanning_countdown_ticks--;
				if (scanning_countdown_ticks <= 0) {
					results = GlobalRegionManager.getRadarEchos(this, scanning_radius);
					isScanning = false;
					if (WarpDriveConfig.LOGGING_RADAR) {
						WarpDrive.logger.info(String.format("%s Scan found %d results in %d radius...",
						                                    this, results.size(), scanning_radius));
					}
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
			}
		}
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		
		radius = tagCompound.getInt("radius");
		isScanning = tagCompound.getBoolean("isScanning");
		scanning_radius = tagCompound.getInt("scanning_radius");
		scanning_countdown_ticks = tagCompound.getInt("scanning_countdown");
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		
		tagCompound.putInt("radius", radius);
		tagCompound.putBoolean("isScanning", isScanning);
		tagCompound.putInt("scanning_radius", scanning_radius);
		tagCompound.putInt("scanning_countdown", scanning_countdown_ticks);
		
		return tagCompound;
	}
	
	private long calculateEnergyRequired(final int parRadius) {
		return Math.round(Math.max(WarpDriveConfig.RADAR_SCAN_MIN_ENERGY_COST,
				  WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[0]
				+ WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[1] * parRadius
				+ WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[2] * parRadius * parRadius
				+ WarpDriveConfig.RADAR_SCAN_ENERGY_COST_FACTORS[3] * parRadius * parRadius * parRadius));
	}
	
	private int calculateScanDuration(final int parRadius) {
		return (int) Math.round(20 * Math.max(WarpDriveConfig.RADAR_SCAN_MIN_DELAY_SECONDS,
				  WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[0]
				+ WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[1] * parRadius
				+ WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[2] * parRadius * parRadius
				+ WarpDriveConfig.RADAR_SCAN_DELAY_FACTORS_SECONDS[3] * parRadius * parRadius * parRadius));
	}
	
	// Common OC/CC methods
	public Object[] getGlobalPosition() {
		// check for optical sensors
		if (false) {
			return new Object[] { false, GlobalRegionManager.GALAXY_UNDEFINED, pos.getX(), pos.getY(), pos.getZ(), Commons.format(world) };
		}
		
		// check for registered celestial object
		final CelestialObject celestialObject = CelestialObjectManager.get(world, pos.getX(), pos.getZ());
		if (celestialObject == null) {
			return new Object[] { false, GlobalRegionManager.GALAXY_UNDEFINED, pos.getX(), pos.getY(), pos.getZ(), Commons.format(world) };
		}
		
		// get actual coordinates
		final String galaxyName = GlobalRegionManager.getGalaxyName(celestialObject, pos.getX(), pos.getY(), pos.getZ());
		final Vector3 vec3Position = GlobalRegionManager.getUniversalCoordinates(celestialObject, pos.getX(), pos.getY(), pos.getZ());
		return new Object[] { true, galaxyName, vec3Position.x, vec3Position.y, vec3Position.z, celestialObject.getDisplayName() };
	}
	
	private Object[] radius(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && !isScanning) {
			final int newRadius;
			try {
				newRadius = Commons.toInt(arguments[0]);
			} catch (final Exception exception) {
				return new Integer[] { radius };
			}
			radius = Commons.clamp(0, 10000, newRadius);
		}
		return new Integer[] { radius };
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				true,
				EnergyWrapper.convert(calculateEnergyRequired(radius), units) };
	}
	
	@Nonnull
	private Object[] getScanDuration() {
		return new Object[] { 0.050D * calculateScanDuration(radius) };
	}
	
	@Nonnull
	private Object[] start() {
		if (isScanning) {
			return new Object[] { false, String.format("Already scanning, %.3f seconds to go", scanning_countdown_ticks / 20.0F) };
		}
		
		// always clear results
		results = null;
		
		// validate parameters
		if (radius <= 0 || radius > 10000) {
			radius = 0;
			return new Object[] { false, "Invalid radius" };
		}
		final long energyRequired = calculateEnergyRequired(radius);
		if (!energy_consume(energyRequired, false)) {
			return new Object[] { false, "Insufficient energy" };
		}
		
		// Begin searching
		scanning_radius = radius;
		scanning_countdown_ticks = calculateScanDuration(radius);
		isScanning = true;
		if (WarpDriveConfig.LOGGING_RADAR) {
			WarpDrive.logger.info(String.format("%s Starting scan over radius %d for %s %s, results expected in %s ticks",
			                                    this, scanning_radius,
			                                    EnergyWrapper.format(energyRequired, null), WarpDriveConfig.ENERGY_DISPLAY_UNITS,
			                                    scanning_countdown_ticks));
		}
		return new Object[] { true, String.format("Scanning started, %.3f seconds to go", scanning_countdown_ticks / 20.0F) };
	}
	
	@Nullable
	private Object[] getResults() {
		if (results == null) {
			return null;
		}
		final Object[] objectResults = new Object[results.size()];
		int index = 0;
		for (final RadarEcho radarEcho : results) {
			objectResults[index++] = new Object[] {
					radarEcho.type,
					radarEcho.name == null ? "" : radarEcho.name,
					radarEcho.x, radarEcho.y, radarEcho.z,
					radarEcho.mass };
		}
		return objectResults;
	}
	
	@Nonnull
	private Object[] getResultsCount() {
		if (results != null) {
			return new Integer[] { results.size() };
		}
		return new Integer[] { -1 };
	}
	
	@Nonnull
	private Object[] getResult(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && (results != null)) {
			final int index;
			try {
				index = Commons.toInt(arguments[0]);
			} catch(final Exception exception) {
				return new Object[] { false, COMPUTER_ERROR_TAG, null, 0, 0, 0 };
			}
			if (index >= 0 && index < results.size()) {
				final RadarEcho radarEcho = results.get(index);
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
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] getGlobalPosition(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getGlobalPosition();
	}
	
	@Callback(direct = true)
	public Object[] radius(final Context context, final Arguments arguments) {
		return radius(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] getScanDuration(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getScanDuration();
	}
	
	@Callback(direct = true)
	public Object[] start(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return start();
	}
	
	@Callback(direct = true)
	public Object[] getResults(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getResults();
	}
	
	@Callback(direct = true)
	public Object[] getResultsCount(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getResultsCount();
	}
	
	@Callback(direct = true)
	public Object[] getResult(final Context context, final Arguments arguments) {
		return getResult(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getGlobalPosition":
			return getGlobalPosition();
			
		case "radius":
			return radius(arguments);
			
		case "getScanDuration":
			return getScanDuration();
			
		case "start":
			return start();
			
		case "getResults":
			return getResults();
			
		case "getResultsCount":
			return getResultsCount();
			
		case "getResult":
			return getResult(arguments);
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
	
	@Override
	public boolean energy_canInput(final Direction from) {
		return true;
	}
}
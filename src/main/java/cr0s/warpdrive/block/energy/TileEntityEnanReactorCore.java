package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.computer.IEnanReactorCore;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.config.WarpDriveConfig;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.Arrays;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityEnanReactorCore extends TileEntityAbstractEnergy implements IEnanReactorCore {
	
	private int containedEnergy = 0;
	
	// generation & instability is 'per tick'
	private static final int PR_MIN_GENERATION = 4;
	private static final int PR_MAX_GENERATION = 64000;
	private static final double PR_MIN_INSTABILITY = 0.004D;
	private static final double PR_MAX_INSTABILITY = 0.060D;
	
	// explosion parameters
	private static final int PR_MAX_EXPLOSION_RADIUS = 6;
	private static final double PR_MAX_EXPLOSION_REMOVAL_CHANCE = 0.1D;
	
	// laser stabilization is per shot
	// target is to consume 10% max output power every second, hence 2.5% per side
	// laser efficiency is 33% at 16% power (target spot), 50% at 24% power, 84% at 50% power, etc.
	// 10% * 20 * PR_MAX_GENERATION / (4 * 0.16) => ~200kRF => ~ max laser energy
	private static final double PR_MAX_LASER_ENERGY = 200000.0D;
	private static final double PR_MAX_LASER_EFFECT = PR_MAX_INSTABILITY * 20 / 0.33D;
	
	private int tickCount = 0;
	
	private final double[] instabilityValues = { 0.0D, 0.0D, 0.0D, 0.0D }; // no instability  = 0, explosion = 100
	private float lasersReceived = 0;
	private int lastGenerationRate = 0;
	private int releasedThisTick = 0; // amount of energy released during current tick update
	private long releasedThisCycle = 0; // amount of energy released during current cycle
	private long releasedLastCycle = 0;
	
	private boolean hold = true; // hold updates and power output until reactor is controlled (i.e. don't explode on chunk-loading while computer is booting)
	private boolean isEnabled = false;
	private static final int MODE_DONT_RELEASE = 0;
	private static final int MODE_MANUAL_RELEASE = 1;
	private static final int MODE_RELEASE_ABOVE = 2;
	private static final int MODE_RELEASE_AT_RATE = 3;
	private static final String[] MODE_STRING = { "OFF", "MANUAL", "ABOVE", "RATE" };
	private int releaseMode = 0;
	private int releaseRate = 0;
	private int releaseAbove = 0;
	
	private boolean init = false;
	
	public TileEntityEnanReactorCore() {
		super();
		peripheralName = "warpdriveEnanReactorCore";
		addMethods(new String[] {
			"enable",
			"energy",		// returns energy, max energy, energy rate
			"instability",	// returns ins0,1,2,3
			"release",		// releases all energy
			"releaseRate",	// releases energy when more than arg0 is produced
			"releaseAbove",	// releases any energy above arg0 amount
			"state"
		});
		CC_scripts = Arrays.asList("startup");
	}
	
	private void increaseInstability(ForgeDirection from, boolean isNatural) {
		if (energy_canOutput(from)) {
			return;
		}
		
		int side = from.ordinal() - 2;
		if (containedEnergy > WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * PR_MIN_GENERATION * 100) {
			double amountToIncrease = WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS
					* Math.max(PR_MIN_INSTABILITY, PR_MAX_INSTABILITY * Math.pow((worldObj.rand.nextDouble() * containedEnergy) / WarpDriveConfig.ENAN_REACTOR_MAX_ENERGY_STORED, 0.1));
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("increaseInstability %.5f", amountToIncrease));
			}
			instabilityValues[side] += amountToIncrease * (isNatural ? 1.0D : 0.25D);
		} else {
			double amountToDecrease = WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * Math.max(PR_MIN_INSTABILITY, instabilityValues[side] * 0.02D);
			instabilityValues[side] = Math.max(0.0D, instabilityValues[side] - amountToDecrease);
		}
	}
	
	private void increaseInstability(final boolean isNatural) {
		increaseInstability(ForgeDirection.NORTH, isNatural);
		increaseInstability(ForgeDirection.SOUTH, isNatural);
		increaseInstability(ForgeDirection.EAST, isNatural);
		increaseInstability(ForgeDirection.WEST, isNatural);
	}
	
	public void decreaseInstability(final ForgeDirection from, final int energy) {
		if (energy_canOutput(from)) {
			return;
		}
		
		// laser is active => start updating reactor
		hold = false;
		
		int amount = convertInternalToRF_floor(energy);
		if (amount <= 1) {
			return;
		}
		
		lasersReceived = Math.min(10.0F, lasersReceived + 1F / WarpDriveConfig.ENAN_REACTOR_MAX_LASERS_PER_SECOND);
		double nospamFactor = 1.0;
		if (lasersReceived > 1.0F) {
			nospamFactor = 0.5;
			worldObj.newExplosion((Entity) null, xCoord + from.offsetX, yCoord + from.offsetY, zCoord + from.offsetZ, 1, false, false);
			// increaseInstability(from, false);
			// increaseInstability(false);
		}
		double normalisedAmount = Math.min(1.0D, Math.max(0.0D, amount / PR_MAX_LASER_ENERGY)); // 0.0 to 1.0
		double baseLaserEffect = 0.5D + 0.5D * Math.cos(Math.PI - (1.0D + Math.log10(0.1D + 0.9D * normalisedAmount)) * Math.PI); // 0.0 to 1.0
		double randomVariation = 0.8D + 0.4D * worldObj.rand.nextDouble(); // ~1.0
		double amountToRemove = PR_MAX_LASER_EFFECT * baseLaserEffect * randomVariation * nospamFactor;
		
		int side = from.ordinal() - 2;
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			if (side == 3) {
				WarpDrive.logger.info("Instability on " + from
					+ " decreased by " + String.format("%.1f", amountToRemove) + "/" + String.format("%.1f", PR_MAX_LASER_EFFECT)
					+ " after consuming " + amount + "/" + PR_MAX_LASER_ENERGY + " lasersReceived is " + String.format("%.1f", lasersReceived) + " hence nospamFactor is " + nospamFactor);
			}
		}
		
		instabilityValues[side] = Math.max(0, instabilityValues[side] - amountToRemove);
		
		updateSideTextures();
	}
	
	private void generateEnergy() {
		double stabilityOffset = 0.5;
		for (int i = 0; i < 4; i++) {
			stabilityOffset *= Math.max(0.01D, instabilityValues[i] / 100.0D);
		}
		
		if (isEnabled) {// producing, instability increase output, you want to take the risk
			int amountToGenerate = (int) Math.ceil(WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * (0.5D + stabilityOffset)
					* (PR_MIN_GENERATION + PR_MAX_GENERATION * Math.pow(containedEnergy / (double) WarpDriveConfig.ENAN_REACTOR_MAX_ENERGY_STORED, 0.6D)));
			containedEnergy = Math.min(containedEnergy + amountToGenerate, WarpDriveConfig.ENAN_REACTOR_MAX_ENERGY_STORED);
			lastGenerationRate = amountToGenerate / WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS;
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info("Generated " + amountToGenerate);
			}
		} else {// decaying over 20s without producing power, you better have power for those lasers
			int amountToDecay = (int) (WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * (1.0D - stabilityOffset) * (PR_MIN_GENERATION + containedEnergy * 0.01D));
			containedEnergy = Math.max(0, containedEnergy - amountToDecay);
			lastGenerationRate = 0;
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info("Decayed " + amountToDecay);
			}
		}
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("tickCount %d releasedThisTick %6d lasersReceived %.5f releasedThisCycle %6d containedEnergy %8d",
			                                    tickCount, releasedThisTick, lasersReceived, releasedThisCycle, containedEnergy));
		}
		releasedThisTick = 0;
		
		lasersReceived = Math.max(0.0F, lasersReceived - 0.05F);
		tickCount++;
		if (tickCount < WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS) {
			return;
		}
		tickCount = 0;
		releasedLastCycle = releasedThisCycle;
		releasedThisCycle = 0;
		
		if (!init) {
			init = true;
			updatedNeighbours();
		}
		
		updateSideTextures();
		
		if (!hold) {// still loading/booting => hold simulation
			// unstable at all time
			if (shouldExplode()) {
				explode();
			}
			increaseInstability(true);
			
			generateEnergy();
		}
		
		sendEvent("reactorPulse", lastGenerationRate);
	}
	
	private void explode() {
		// remove blocks randomly up to x blocks around (breaking whatever protection is there)
		double normalizedEnergy = containedEnergy / (double) WarpDriveConfig.ENAN_REACTOR_MAX_ENERGY_STORED;
		int radius = (int) Math.round(PR_MAX_EXPLOSION_RADIUS * Math.pow(normalizedEnergy, 0.125));
		double chanceOfRemoval = PR_MAX_EXPLOSION_REMOVAL_CHANCE * Math.pow(normalizedEnergy, 0.125);
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(this + " Explosion radius is " + radius + ", Chance of removal is " + chanceOfRemoval);
		}
		if (radius > 1) {
			float bedrockExplosionResistance = Blocks.bedrock.getExplosionResistance(null);
			for (int x = xCoord - radius; x <= xCoord + radius; x++) {
				for (int y = yCoord - radius; y <= yCoord + radius; y++) {
					for (int z = zCoord - radius; z <= zCoord + radius; z++) {
						if (z != zCoord || y != yCoord || x != xCoord) {
							if (worldObj.rand.nextDouble() < chanceOfRemoval) {
								if (worldObj.getBlock(x, y, z).getExplosionResistance(null) >= bedrockExplosionResistance) {
									worldObj.setBlockToAir(x, y, z);
								}
							}
						}
					}
				}
			}
		}
		
		// remove reactor
		worldObj.setBlockToAir(xCoord, yCoord, zCoord);
		
		// set a few augmented TnT around reactor core
		for (int i = 0; i < 3; i++) {
			worldObj.newExplosion((Entity) null,
				xCoord + worldObj.rand.nextInt(3) - 0.5D,
				yCoord + worldObj.rand.nextInt(3) - 0.5D,
				zCoord + worldObj.rand.nextInt(3) - 0.5D,
				4.0F + worldObj.rand.nextInt(3), true, true);
		}
	}
	
	private void updateSideTextures() {
		double maxInstability = 0.0D;
		for (Double ins : instabilityValues) {
			if (ins > maxInstability) {
				maxInstability = ins;
			}
		}
		int instabilityNibble = (int) Math.max(0, Math.min(3, Math.round(maxInstability / 25.0D)));
		int energyNibble = (int) Math.max(0, Math.min(3, Math.round(4.0D * containedEnergy / WarpDriveConfig.ENAN_REACTOR_MAX_ENERGY_STORED)));
		
		int metadata = 4 * instabilityNibble + energyNibble;
		if (getBlockMetadata() != metadata) {
			worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, metadata, 3);
		}
	}
	
	private boolean shouldExplode() {
		boolean exploding = false;
		for (int i = 0; i < 4; i++) {
			exploding = exploding || (instabilityValues[i] >= 100);
		}
		exploding &= (worldObj.rand.nextInt(4) == 2);
		
		if (exploding) {
			WarpDrive.logger.info(this
				+ String.format(" Explosion triggered, Instability is [%.2f, %.2f, %.2f, %.2f], Energy stored is %d, Laser received is %.2f, %s",
				instabilityValues[0], instabilityValues[1], instabilityValues[2], instabilityValues[3],
				containedEnergy, lasersReceived, isEnabled ? "ENABLED" : "DISABLED"));
			isEnabled = false;
		}
		return exploding;
	}
	
	@Override
	public void updatedNeighbours() {
		super.updatedNeighbours();
		
		int[] offsetsX = { 0, 0, -2, 2 };
		int[] offsetsZ = { 2, -2, 0, 0 };
		
		TileEntity tileEntity;
		for (int i = 0; i < 4; i++) {
			tileEntity = worldObj.getTileEntity(xCoord + offsetsX[i], yCoord, zCoord + offsetsZ[i]);
			if (tileEntity instanceof TileEntityEnanReactorLaser) {
				((TileEntityEnanReactorLaser) tileEntity).scanForReactor();
			}
		}
	}
	
	// Common OC/CC methods
	@Override
	public Object[] enable(Object[] arguments) {
		if (arguments.length == 1) {
			boolean enableRequest;
			try {
				enableRequest = Commons.toBool(arguments[0]);
			} catch (Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(this + " LUA error on enable(): Boolean expected for 1st argument " + arguments[0]);
				}
				return enable(new Object[0]);
			}
			if (isEnabled && !enableRequest) {
				sendEvent("reactorDeactivation");
			} else if (!isEnabled && enableRequest) {
				sendEvent("reactorActivation");
			}
			isEnabled = enableRequest;
		}
		return new Object[] { isEnabled };
	}
	
	@Override
	public Object[] release(Object[] arguments) {
		if (arguments.length == 1) {
			boolean releaseRequested;
			try {
				releaseRequested = Commons.toBool(arguments[0]);
			} catch (Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(this + " LUA error on release(): Boolean expected for 1st argument " + arguments[0]);
				}
				return new Object[] { releaseMode != MODE_DONT_RELEASE };
			}
			
			releaseMode = releaseRequested ? MODE_MANUAL_RELEASE : MODE_DONT_RELEASE;
			releaseAbove = 0;
			releaseRate = 0;
		}
		return new Object[] { releaseMode != MODE_DONT_RELEASE };
	}
	
	@Override
	public Object[] releaseRate(Object[] arguments) {
		if (arguments.length == 1) {
			int releaseRateRequested;
			try {
				releaseRateRequested = Commons.toInt(arguments[0]);
			} catch (Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(this + " LUA error on releaseRate(): Integer expected for 1st argument " + arguments[0]);
				}
				return new Object[] { MODE_STRING[releaseMode], releaseRate };
			}
			
			if (releaseRateRequested <= 0) {
				releaseMode = MODE_DONT_RELEASE;
				releaseRate = 0;
			} else {
				// player has to adjust it
				releaseRate = releaseRateRequested;
				releaseMode = MODE_RELEASE_AT_RATE;
			}
		}
		return new Object[] { MODE_STRING[releaseMode], releaseRate };
	}
	
	@Override
	public Object[] releaseAbove(Object[] arguments) {
		int releaseAboveRequested;
		try {
			releaseAboveRequested = Commons.toInt(arguments[0]);
		} catch (Exception exception) {
			if (WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.error(this + " LUA error on releaseAbove(): Integer expected for 1st argument " + arguments[0]);
			}
			return new Object[] { MODE_STRING[releaseMode], releaseAbove };
		}
		
		if (releaseAboveRequested <= 0) {
			releaseMode = 0;
			releaseAbove = MODE_DONT_RELEASE;
		} else {
			releaseMode = MODE_RELEASE_ABOVE;
			releaseAbove = releaseAboveRequested;
		}
		
		return new Object[] { MODE_STRING[releaseMode], releaseAbove };
	}
	
	@Override
	public Object[] state() {
		final String status = getStatusHeaderInPureText();
		if (releaseMode == MODE_DONT_RELEASE || releaseMode == MODE_MANUAL_RELEASE) {
			return new Object[] { status, isEnabled, containedEnergy, MODE_STRING[releaseMode], 0 };
		} else if (releaseMode == MODE_RELEASE_ABOVE) {
			return new Object[] { status, isEnabled, containedEnergy, MODE_STRING[releaseMode], releaseAbove };
		} else {
			return new Object[] { status, isEnabled, containedEnergy, MODE_STRING[releaseMode], releaseRate };
		}
	}
	
	// OpenComputer callback methods
	@Override
	public Object[] energy() {
		return new Object[] { containedEnergy, WarpDriveConfig.ENAN_REACTOR_MAX_ENERGY_STORED, releasedLastCycle / WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] enable(Context context, Arguments arguments) {
		return enable(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] release(Context context, Arguments arguments) {
		return release(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] releaseRate(Context context, Arguments arguments) {
		return releaseRate(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] releaseAbove(Context context, Arguments arguments) {
		return releaseAbove(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] instability(Context context, Arguments arguments) {
		// computer is alive => start updating reactor
		hold = false;
		return new Double[] { instabilityValues[0], instabilityValues[1], instabilityValues[2], instabilityValues[3] };
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
		// computer is alive => start updating reactor
		hold = false;
		
		final String methodName = getMethodName(method);
		
		try {
			switch (methodName) {
			case "enable":
				return enable(arguments);
				
			case "energy":
				return energy();
				
			case "instability":
				Object[] retVal = new Object[4];
				for (int i = 0; i < 4; i++) {
					retVal[i] = instabilityValues[i];
				}
				return retVal;
				
			case "release":
				return release(arguments);
				
			case "releaseRate":
				return releaseRate(arguments);
				
			case "releaseAbove":
				return releaseAbove(arguments);
				
			case "state":
				return state();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			return new String[] { exception.getMessage() };
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	// POWER INTERFACES
	@Override
	public int energy_getPotentialOutput() {
		if (hold) {// still loading/booting => hold output
			return 0;
		}
		int result = 0;
		int capacity = Math.max(0, 2 * lastGenerationRate - releasedThisTick);
		if (releaseMode == MODE_MANUAL_RELEASE) {
			result = Math.min(Math.max(0, containedEnergy), capacity);
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info("PotentialOutput Manual " + result + " RF (" + convertRFtoInternal_floor(result) + " internal) capacity " + capacity);
			}
		} else if (releaseMode == MODE_RELEASE_ABOVE) {
			result = Math.min(Math.max(0, containedEnergy - releaseAbove), capacity);
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info("PotentialOutput Above " + result + " RF (" + convertRFtoInternal_floor(result) + " internal) capacity " + capacity);
			}
		} else if (releaseMode == MODE_RELEASE_AT_RATE) {
			int remainingRate = Math.max(0, releaseRate - releasedThisTick);
			result = Math.min(Math.max(0, containedEnergy), Math.min(remainingRate, capacity));
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info("PotentialOutput Rated " + result + " RF (" + convertRFtoInternal_floor(result) + " internal) remainingRate " + remainingRate + " RF/t capacity " + capacity);
			}
		}
		return (int) convertRFtoInternal_floor(result);
	}
	
	@Override
	public boolean energy_canOutput(ForgeDirection from) {
		return from.equals(ForgeDirection.UP) || from.equals(ForgeDirection.DOWN);
	}
	
	@Override
	protected void energy_outputDone(final long energyOutput_internal) {
		final long energyOutput_RF = convertInternalToRF_ceil(energyOutput_internal);
		containedEnergy -= energyOutput_RF;
		if (containedEnergy < 0) {
			containedEnergy = 0;
		}
		releasedThisTick += energyOutput_RF;
		releasedThisCycle += energyOutput_RF;
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info("OutputDone " + energyOutput_internal + " (" + energyOutput_RF + " RF)");
		}
	}
	
	@Override
	public int energy_getEnergyStored() {
		return (int) Commons.clamp(0L, energy_getMaxStorage(), convertRFtoInternal_floor(containedEnergy));
	}
	
	@Override
	public int energy_getMaxStorage() {
		return (int) convertRFtoInternal_floor(WarpDriveConfig.ENAN_REACTOR_MAX_ENERGY_STORED);
	}
	
	// Forge overrides
	@Override
	public void writeToNBT(final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setInteger("energy", containedEnergy);
		tagCompound.setInteger("releaseMode", releaseMode);
		tagCompound.setInteger("releaseRate", releaseRate);
		tagCompound.setInteger("releaseAbove", releaseAbove);
		tagCompound.setDouble("i0", instabilityValues[0]);
		tagCompound.setDouble("i1", instabilityValues[1]);
		tagCompound.setDouble("i2", instabilityValues[2]);
		tagCompound.setDouble("i3", instabilityValues[3]);
		tagCompound.setBoolean("isEnabled", isEnabled);
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		containedEnergy = tagCompound.getInteger("energy");
		releaseMode = tagCompound.getInteger("releaseMode");
		releaseRate = tagCompound.getInteger("releaseRate");
		releaseAbove = tagCompound.getInteger("releaseAbove");
		instabilityValues[0] = tagCompound.getDouble("i0");
		instabilityValues[1] = tagCompound.getDouble("i1");
		instabilityValues[2] = tagCompound.getDouble("i2");
		instabilityValues[3] = tagCompound.getDouble("i3");
		isEnabled = tagCompound.getBoolean("active")    // up to 1.3.30 included
		         || tagCompound.getBoolean("isEnabled");
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		tagCompound.removeTag("energy");
		tagCompound.removeTag("releaseMode");
		tagCompound.removeTag("releaseRate");
		tagCompound.removeTag("releaseAbove");
		tagCompound.removeTag("i0");
		tagCompound.removeTag("i1");
		tagCompound.removeTag("i2");
		tagCompound.removeTag("i3");
		tagCompound.removeTag("isEnabled");
		return tagCompound;
	}
	
	@Override
	public String toString() {
		return String.format("%s \'%s\' @ \'%s\' (%d %d %d)",
			getClass().getSimpleName(),
			connectedComputers == null ? "~NULL~" : connectedComputers,
			worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(),
			xCoord, yCoord, zCoord);
	}
}
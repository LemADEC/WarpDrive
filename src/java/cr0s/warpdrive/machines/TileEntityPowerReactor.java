package cr0s.warpdrive.machines;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.FMLCommonHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;
import cr0s.warpdrive.api.IBlockUpdateDetector;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;

public class TileEntityPowerReactor extends WarpEnergyTE implements IBlockUpdateDetector {
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
	// target is to consume 10% max output power every second, hence 2.5% per
	// side
	// laser efficiency is 33% at 16% power (target spot), 50% at 24% power, 84%
	// at 50% power, etc.
	// 10% * 20 * PR_MAX_GENERATION / (4 * 0.16) => ~200kRF => ~ max laser
	// energy
	private static final double PR_MAX_LASER_ENERGY = 200000.0D;
	private static final double PR_MAX_LASER_EFFECT = PR_MAX_INSTABILITY * 20 / 0.33D;

	private int tickCount = 0;

	private double[] instabilityValues = { 0.0D, 0.0D, 0.0D, 0.0D }; // no
																		// instability
																		// = 0,
																		// explosion
																		// = 100
	private float lasersReceived = 0;
	private int lastGenerationRate = 0;
	private int releasedThisTick = 0; // amount of energy released during
										// current tick update
	private int releasedThisCycle = 0; // amount of energy released during
										// current cycle
	private int releasedLastCycle = 0;

	private boolean hold = true; // hold updates and power output until reactor
									// is controlled (i.e. don't explode on
									// chunk-loading while computer is booting)
	private boolean active = false;
	private static final int MODE_DONT_RELEASE = 0;
	private static final int MODE_MANUAL_RELEASE = 1;
	private static final int MODE_RELEASE_ABOVE = 2;
	private static final int MODE_RELEASE_AT_RATE = 3;
	private static final String[] MODE_STRING = { "OFF", "MANUAL", "ABOVE", "RATE" };
	private int releaseMode = 0;
	private int releaseRate = 0;
	private int releaseAbove = 0;

	private boolean init = false;

	public TileEntityPowerReactor() {
		super();
		peripheralName = "warpdriveReactor";
		methodsArray = new String[] { "active", "energy", // returns energy, max
															// energy, energy
															// rate
				"instability", // returns ins0,1,2,3
				"release", // releases all energy
				"releaseRate", // releases energy when more than arg0 is
								// produced
				"releaseAbove", // releases any energy above arg0 amount
				"help" // returns help on arg0 function
		};
	}

	private void increaseInstability(ForgeDirection from, boolean isNatural) {
		if (canOutputEnergy(from) || hold) {
			return;
		}

		int side = from.ordinal() - 2;
		if (containedEnergy > WarpDriveConfig.PR_TICK_TIME * PR_MIN_GENERATION * 100) {
			double amountToIncrease = WarpDriveConfig.PR_TICK_TIME
					* Math.max(PR_MIN_INSTABILITY,
							PR_MAX_INSTABILITY * Math.pow((worldObj.rand.nextDouble() * containedEnergy) / WarpDriveConfig.PR_MAX_ENERGY, 0.1));
			// WarpDrive.debugPrint("InsInc" + amountToIncrease);
			instabilityValues[side] += amountToIncrease * (isNatural ? 1.0D : 0.25D);
		} else {
			double amountToDecrease = WarpDriveConfig.PR_TICK_TIME * Math.max(PR_MIN_INSTABILITY, instabilityValues[side] * 0.02D);
			instabilityValues[side] = Math.max(0.0D, instabilityValues[side] - amountToDecrease);
		}
	}

	private void increaseInstability(boolean isNatural) {
		increaseInstability(ForgeDirection.NORTH, isNatural);
		increaseInstability(ForgeDirection.SOUTH, isNatural);
		increaseInstability(ForgeDirection.EAST, isNatural);
		increaseInstability(ForgeDirection.WEST, isNatural);
	}

	public void decreaseInstability(ForgeDirection from, int energy) {
		if (canOutputEnergy(from)) {
			return;
		}

		// laser is active => start updating reactor
		hold = false;

		int amount = convertInternalToRF(energy);
		if (amount <= 1) {
			return;
		}

		lasersReceived = Math.min(10.0F, lasersReceived + 1F / WarpDriveConfig.PR_MAX_LASERS);
		double nospamFactor = 1.0;
		if (lasersReceived > 1.0F) {
			nospamFactor = 0.5;
			worldObj.newExplosion((Entity) null, xCoord + from.offsetX, yCoord + from.offsetY, zCoord + from.offsetZ, 1, false, false);
			// increaseInstability(from, false);
			// increaseInstability(false);
		}
		double normalisedAmount = Math.min(1.0D, Math.max(0.0D, amount / PR_MAX_LASER_ENERGY)); // 0.0
																								// to
																								// 1.0
		double baseLaserEffect = 0.5D + 0.5D * Math.cos(Math.PI - (1.0D + Math.log10(0.1D + 0.9D * normalisedAmount)) * Math.PI); // 0.0
																																	// to
																																	// 1.0
		double randomVariation = 0.8D + 0.4D * worldObj.rand.nextDouble(); // ~1.0
		double amountToRemove = PR_MAX_LASER_EFFECT * baseLaserEffect * randomVariation * nospamFactor;

		int side = from.ordinal() - 2;

		/*
		 * if (side == 3) WarpDrive.debugPrint("Instability on " +
		 * from.toString() + " decreased by " + String.format("%.1f",
		 * amountToRemove) + "/" + String.format("%.1f", PR_MAX_LASER_EFFECT) +
		 * " after consuming " + amount + "/" + PR_MAX_LASER_ENERGY +
		 * " lasersReceived is " + String.format("%.1f", lasersReceived) +
		 * " hence nospamFactor is " + nospamFactor); /*
		 */

		instabilityValues[side] = Math.max(0, instabilityValues[side] - amountToRemove);

		updateSideTextures();
	}

	private void generateEnergy() {
		double stabilityOffset = 0.5;
		for (int i = 0; i < 4; i++) {
			stabilityOffset *= Math.max(0.01D, instabilityValues[i] / 100.0D);
		}

		if (active) {// producing, instability increase output, you want to take
						// the risk
			int amountToGenerate = (int) Math.ceil(WarpDriveConfig.PR_TICK_TIME * (0.5D + stabilityOffset)
					* (PR_MIN_GENERATION + PR_MAX_GENERATION * Math.pow(containedEnergy / (double) WarpDriveConfig.PR_MAX_ENERGY, 0.6D)));
			containedEnergy = Math.min(containedEnergy + amountToGenerate, WarpDriveConfig.PR_MAX_ENERGY);
			lastGenerationRate = amountToGenerate / WarpDriveConfig.PR_TICK_TIME;
			// WarpDrive.debugPrint("Generated " + amountToGenerate);
		} else {// decaying over 20s without producing power, you better have
				// power for those lasers
			int amountToDecay = (int) (WarpDriveConfig.PR_TICK_TIME * (1.0D - stabilityOffset) * (PR_MIN_GENERATION + containedEnergy * 0.01D));
			containedEnergy = Math.max(0, containedEnergy - amountToDecay);
			lastGenerationRate = 0;
			// WarpDrive.debugPrint("Decayed " + amountToDecay);
		}
	}

	@Override
	public void updateEntity() {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			return;
		}
		super.updateEntity();

		// WarpDrive.debugPrint("tickCount " + tickCount + " releasedThisTick "
		// + releasedThisTick + " lasersReceived " + lasersReceived +
		// " releasedThisCycle " + releasedThisCycle + " containedEnergy " +
		// containedEnergy);
		releasedThisTick = 0;

		lasersReceived = Math.max(0.0F, lasersReceived - 0.05F);
		tickCount++;
		if (tickCount < WarpDriveConfig.PR_TICK_TIME) {
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

		// unstable at all time
		if (shouldExplode()) {
			explode();
		}
		increaseInstability(true);

		generateEnergy();

		sendEvent("reactorPulse", new Object[] { lastGenerationRate });
	}

	private void explode() {
		// remove blocks randomly up to x blocks around (breaking whatever
		// protection is there)
		double normalizedEnergy = containedEnergy / (double) WarpDriveConfig.PR_MAX_ENERGY;
		int radius = (int) Math.round(PR_MAX_EXPLOSION_RADIUS * Math.pow(normalizedEnergy, 0.125));
		double c = PR_MAX_EXPLOSION_REMOVAL_CHANCE * Math.pow(normalizedEnergy, 0.125);
		WarpDrive.debugPrint(this + " Explosion radius is " + radius + ", Chance of removal is " + c);
		if (radius > 1) {
			for (int x = xCoord - radius; x <= xCoord + radius; x++) {
				for (int y = yCoord - radius; y <= yCoord + radius; y++) {
					for (int z = zCoord - radius; z <= zCoord + radius; z++) {
						if (z != zCoord || y != yCoord || x != xCoord) {
							if (worldObj.rand.nextDouble() < c) {
								worldObj.setBlockToAir(x, y, z);
							}
						}
					}
				}
			}
		}
		// remove reactor
		worldObj.setBlockToAir(xCoord, yCoord, zCoord);
		// set a few TnT augmented around reactor
		for (int i = 0; i < 3; i++) {
			worldObj.newExplosion((Entity) null, xCoord + worldObj.rand.nextInt(3) - 0.5D, yCoord + worldObj.rand.nextInt(3) - 0.5D,
					zCoord + worldObj.rand.nextInt(3) - 0.5D, 4.0F + worldObj.rand.nextInt(3), true, true);
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
		int energyNibble = (int) Math.max(0, Math.min(3, Math.round(4.0D * containedEnergy / WarpDriveConfig.PR_MAX_ENERGY)));

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
			WarpDrive.print(this
					+ String.format(" Explosion trigerred, Instability is [%.2f, %.2f, %.2f, %.2f], Energy stored is %d, Laser received is %.2f, %s",
							new Object[] { instabilityValues[0], instabilityValues[1], instabilityValues[2], instabilityValues[3], this.containedEnergy,
									this.lasersReceived, this.active ? "ACTIVE" : "INACTIVE" }));
			active = false;
		}
		return exploding;
	}

	// Takes the arguments passed by function call and returns an appropriate
	// string
	private static String helpStr(Object[] args) {
		if (args.length > 0) {
			String arg = args[0].toString().toLowerCase();
			if (arg.equals("getactive")) {
				return "getActive(): returns true if the reactor is active and false otherwise";
			} else if (arg.equals("setactive")) {
				return "setActive(bool): activates the reactor if passed true and deactivates if passed false";
			} else if (arg.equals("energy")) {
				return WarpDrive.defEnergyStr;
			} else if (arg.equals("instability")) {
				return "instability(): returns the 4 instability values (100 is the point when the reactor explodes)";
			} else if (arg.equals("release")) {
				return "release(bool): sets the reactor to output all energy or disables outputting of energy";
			} else if (arg.equals("releaserate")) {
				return "releaseRate(int): sets the reactor to try to release exactly int/tick";
			} else if (arg.equals("releaseabove")) {
				return "releaseAbove(int): releases all energy above stored int";
			}
		}
		return WarpDrive.defHelpStr;
	}

	@Override
	public void updatedNeighbours() {
		TileEntity te;
		super.updatedNeighbours();

		int[] xo = { 0, 0, -2, 2 };
		int[] zo = { 2, -2, 0, 0 };

		for (int i = 0; i < 4; i++) {
			te = worldObj.getBlockTileEntity(xCoord + xo[i], yCoord, zCoord + zo[i]);
			if (te instanceof TileEntityPowerLaser) {
				((TileEntityPowerLaser) te).scanForReactor();
			}
		}
	}

	// OpenComputer callback methods
	// FIXME: implement OpenComputers...

	public Object[] active(Object[] arguments) throws Exception {
		if (arguments.length == 1) {
			boolean activate = false;
			try {
				activate = toBool(arguments[0]);
			} catch (Exception e) {
				throw new Exception("Function expects an boolean value");
			}
			if (active && !activate) {
				sendEvent("reactorDeactivation", null);
			} else if (!active && activate) {
				sendEvent("reactorActivation", null);
			}
			active = activate;
		}
		if (releaseMode == MODE_DONT_RELEASE || releaseMode == MODE_MANUAL_RELEASE) {
			return new Object[] { active, MODE_STRING[releaseMode], 0 };
		} else if (releaseMode == MODE_RELEASE_ABOVE) {
			return new Object[] { active, MODE_STRING[releaseMode], releaseAbove };
		} else {
			return new Object[] { active, MODE_STRING[releaseMode], releaseRate };
		}
	}

	private Object[] release(Object[] arguments) throws Exception {
		boolean doRelease = false;
		if (arguments.length > 0) {
			try {
				doRelease = toBool(arguments[0]);
			} catch (Exception e) {
				throw new Exception("Function expects an boolean value");
			}

			releaseMode = doRelease ? MODE_MANUAL_RELEASE : MODE_DONT_RELEASE;
			releaseAbove = 0;
			releaseRate = 0;
		}
		return new Object[] { releaseMode != MODE_DONT_RELEASE };
	}

	private Object[] releaseRate(Object[] arguments) throws Exception {
		int rate = -1;
		try {
			rate = toInt(arguments[0]);
		} catch (Exception e) {
			throw new Exception("Function expects an integer value");
		}

		if (rate <= 0) {
			releaseMode = MODE_DONT_RELEASE;
			releaseRate = 0;
		} else {
			/*
			 * releaseAbove = (int)Math.ceil(Math.pow(rate, 1.0 / 0.6));
			 * WarpDrive.debugPrint("releaseAbove " + releaseAbove); releaseMode
			 * = MODE_RELEASE_ABOVE;/*
			 */
			// player has to adjust it
			releaseRate = rate;
			releaseMode = MODE_RELEASE_AT_RATE;
		}

		return new Object[] { MODE_STRING[releaseMode], releaseRate };
	}

	private Object[] releaseAbove(Object[] arguments) throws Exception {
		int above = -1;
		try {
			above = toInt(arguments[0]);
		} catch (Exception e) {
			throw new Exception("Function expects an integer value");
		}

		if (above <= 0) {
			releaseMode = 0;
			releaseAbove = MODE_DONT_RELEASE;
		} else {
			releaseMode = MODE_RELEASE_ABOVE;
			releaseAbove = above;
		}

		return new Object[] { MODE_STRING[releaseMode], releaseAbove };
	}

	// ComputerCraft IPeripheral methods implementation
	@Override
	public void attach(IComputerAccess computer) {
		super.attach(computer);
		int id = computer.getID();
		connectedComputers.put(id, computer);
		if (WarpDriveConfig.G_LUA_SCRIPTS != WarpDriveConfig.LUA_SCRIPTS_NONE) {
			computer.mount("/power", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/power"));
			computer.mount("/warpupdater", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/common/updater"));
			if (WarpDriveConfig.G_LUA_SCRIPTS == WarpDriveConfig.LUA_SCRIPTS_ALL) {
				computer.mount("/startup", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/power/startup"));
			}
		}
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception {
		// computer is alive => start updating reactor
		hold = false;

		String methodName = methodsArray[method];

		if (methodName.equals("active")) {
			return active(arguments);

		} else if (methodName.equals("energy")) {
			return new Object[] { containedEnergy, WarpDriveConfig.PR_MAX_ENERGY, releasedLastCycle / WarpDriveConfig.PR_TICK_TIME };

		} else if (methodName.equals("instability")) {
			Object[] retVal = new Object[4];
			for (int i = 0; i < 4; i++) {
				retVal[i] = instabilityValues[i];
			}
			return retVal;

		} else if (methodName.equals("release")) {
			return release(arguments);

		} else if (methodName.equals("releaseRate")) {
			return releaseRate(arguments);

		} else if (methodName.equals("releaseAbove")) {
			return releaseAbove(arguments);

		} else if (methodName.equals("help")) {
			return new Object[] { helpStr(arguments) };
		}

		return null;
	}

	// POWER INTERFACES
	@Override
	public int getPotentialEnergyOutput() {
		if (hold) {// still loading/booting => hold output
			return 0;
		}
		int result = 0;
		int capacity = Math.max(0, 2 * lastGenerationRate - releasedThisTick);
		if (releaseMode == MODE_MANUAL_RELEASE) {
			result = Math.min(Math.max(0, containedEnergy), capacity);
			// WarpDrive.debugPrint("PotentialOutput Manual " + result + " RF ("
			// + convertRFtoInternal(result) + " internal) capacity " +
			// capacity);
		} else if (releaseMode == MODE_RELEASE_ABOVE) {
			result = Math.min(Math.max(0, containedEnergy - releaseAbove), capacity);
			// WarpDrive.debugPrint("PotentialOutput Above " + result + " RF ("
			// + convertRFtoInternal(result) + " internal) capacity " +
			// capacity);
		} else if (releaseMode == MODE_RELEASE_AT_RATE) {
			int remainingRate = Math.max(0, releaseRate - releasedThisTick);
			result = Math.min(Math.max(0, containedEnergy), Math.min(remainingRate, capacity));
			// WarpDrive.debugPrint("PotentialOutput Rated " + result + " RF ("
			// + convertRFtoInternal(result) + " internal) remainingRate " +
			// remainingRate + " RF/t capacity " + capacity);
		}
		return convertRFtoInternal(result);
	}

	@Override
	public boolean canOutputEnergy(ForgeDirection from) {
		if (from.equals(ForgeDirection.UP) || from.equals(ForgeDirection.DOWN)) {
			return true;
		}
		return false;
	}

	@Override
	protected void energyOutputDone(int energyOutput_internal) {
		int energyOutput_RF = convertInternalToRF(energyOutput_internal);
		containedEnergy -= energyOutput_RF;
		if (containedEnergy < 0) {
			containedEnergy = 0;
		}
		releasedThisTick += energyOutput_RF;
		releasedThisCycle += energyOutput_RF;
		// WarpDrive.debugPrint("OutputDone " + energyOutput_internal + " (" +
		// energyOutput_RF + " RF)");
	}

	@Override
	public int getEnergyStored() {
		return convertRFtoInternal(containedEnergy);
	}

	@Override
	public int getMaxEnergyStored() {
		return convertRFtoInternal(WarpDriveConfig.PR_MAX_ENERGY);
	}

	// Forge overrides
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("energy", containedEnergy);
		nbt.setInteger("releaseMode", releaseMode);
		nbt.setInteger("releaseRate", releaseRate);
		nbt.setInteger("releaseAbove", releaseAbove);
		nbt.setDouble("i0", instabilityValues[0]);
		nbt.setDouble("i1", instabilityValues[1]);
		nbt.setDouble("i2", instabilityValues[2]);
		nbt.setDouble("i3", instabilityValues[3]);
		nbt.setBoolean("active", active);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		containedEnergy = nbt.getInteger("energy");
		releaseMode = nbt.getInteger("releaseMode");
		releaseRate = nbt.getInteger("releaseRate");
		releaseAbove = nbt.getInteger("releaseAbove");
		instabilityValues[0] = nbt.getDouble("i0");
		instabilityValues[1] = nbt.getDouble("i1");
		instabilityValues[2] = nbt.getDouble("i2");
		instabilityValues[3] = nbt.getDouble("i3");
		active = nbt.getBoolean("active");
	}

	@Override
	public String toString() {
		return String.format("%s \'%s\' @ \'%s\' %.2f, %.2f, %.2f", new Object[] { getClass().getSimpleName(),
				this.connectedComputers == null ? "~NULL~" : this.connectedComputers, worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(),
				Double.valueOf(xCoord), Double.valueOf(yCoord), Double.valueOf(zCoord) });
	}
}
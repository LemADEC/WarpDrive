package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;

public class TileEntityMiningLaser extends TileEntityAbstractMiner {
	
	private final boolean canSilktouch = (WarpDriveConfig.MINING_LASER_SILKTOUCH_DEUTERIUM_L <= 0 || FluidRegistry.isFluidRegistered("deuterium"));
	
	private boolean isActive() {
		return currentState != STATE_IDLE;
	}
	
	private int layerOffset = 1;
	private boolean mineAllBlocks = true;
	
	private int delayTicks = 0;
	
	private static final int STATE_IDLE = 0;
	private static final int STATE_WARMUP = 1;
	private static final int STATE_SCANNING = 2;
	private static final int STATE_MINING = 3;
	private int currentState = 0;
	
	private boolean enoughPower = false;
	private int currentLayer;
	
	private final ArrayList<VectorI> valuablesInLayer = new ArrayList<>();
	private int valuableIndex = 0;
	
	public TileEntityMiningLaser() {
		super();
		laserOutputSide = ForgeDirection.DOWN;
		peripheralName = "warpdriveMiningLaser";
		addMethods(new String[] {
				"start",
				"stop",
				"state",
				"offset",
				"onlyOres",
				"silktouch"
		});
		CC_scripts = Arrays.asList("mine", "stop");
		laserMediumMaxCount = WarpDriveConfig.MINING_LASER_MAX_MEDIUMS_COUNT;
	}
	
	@SuppressWarnings("UnnecessaryReturnStatement")
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		if (currentState == STATE_IDLE) {
			delayTicks = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
			updateMetadata(BlockMiningLaser.ICON_IDLE);
			
			// force start if no computer control is available
			if (!WarpDriveConfig.isComputerCraftLoaded && !WarpDriveConfig.isOpenComputersLoaded) {
				enableSilktouch = false;
				layerOffset = 1;
				mineAllBlocks = true;
				start();
			}
			return;
		}
		
		final boolean isOnPlanet = CelestialObjectManager.hasAtmosphere(worldObj, xCoord, zCoord);
		
		delayTicks--;
		if (currentState == STATE_WARMUP) {
			updateMetadata(BlockMiningLaser.ICON_SCANNING_LOW_POWER);
			if (delayTicks < 0) {
				delayTicks = WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS;
				currentState = STATE_SCANNING;
				updateMetadata(BlockMiningLaser.ICON_SCANNING_LOW_POWER);
				return;
			}
			
		} else if (currentState == STATE_SCANNING) {
			if (delayTicks == WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS - 1) {
				// check power level
				enoughPower = consumeEnergyFromLaserMediums(isOnPlanet ? WarpDriveConfig.MINING_LASER_PLANET_ENERGY_PER_LAYER : WarpDriveConfig.MINING_LASER_SPACE_ENERGY_PER_LAYER, true);
				if (!enoughPower) {
					updateMetadata(BlockMiningLaser.ICON_SCANNING_LOW_POWER);
					delayTicks = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
					return;
				} else {
					updateMetadata(BlockMiningLaser.ICON_SCANNING_POWERED);
				}
				
				// show current layer
				int age = Math.max(40, 5 * WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS);
				double xMax = xCoord + WarpDriveConfig.MINING_LASER_RADIUS_BLOCKS + 1.0D;
				double xMin = xCoord - WarpDriveConfig.MINING_LASER_RADIUS_BLOCKS + 0.0D;
				double zMax = zCoord + WarpDriveConfig.MINING_LASER_RADIUS_BLOCKS + 1.0D;
				double zMin = zCoord - WarpDriveConfig.MINING_LASER_RADIUS_BLOCKS + 0.0D;
				double y = currentLayer + 1.0D;
				PacketHandler.sendBeamPacket(worldObj, new Vector3(xMin, y, zMin), new Vector3(xMax, y, zMin), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(worldObj, new Vector3(xMax, y, zMin), new Vector3(xMax, y, zMax), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(worldObj, new Vector3(xMax, y, zMax), new Vector3(xMin, y, zMax), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(worldObj, new Vector3(xMin, y, zMax), new Vector3(xMin, y, zMin), 0.3F, 0.0F, 1.0F, age, 0, 50);
				
			} else if (delayTicks < 0) {
				delayTicks = WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS;
				if (currentLayer <= 0) {
					stop();
					return;
				}
				
				// consume power
				enoughPower = consumeEnergyFromLaserMediums(isOnPlanet ? WarpDriveConfig.MINING_LASER_PLANET_ENERGY_PER_LAYER : WarpDriveConfig.MINING_LASER_SPACE_ENERGY_PER_LAYER, false);
				if (!enoughPower) {
					updateMetadata(BlockMiningLaser.ICON_SCANNING_LOW_POWER);
					delayTicks = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
					return;
				} else {
					updateMetadata(BlockMiningLaser.ICON_SCANNING_POWERED);
				}
				
				// scan
				scanLayer();
				if (!valuablesInLayer.isEmpty()) {
					int r = (int) Math.ceil(WarpDriveConfig.MINING_LASER_RADIUS_BLOCKS / 2.0D);
					int offset = (yCoord - currentLayer) % (2 * r);
					int age = Math.max(20, Math.round(2.5F * WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS));
					double y = currentLayer + 1.0D;
					PacketHandler.sendBeamPacket(worldObj, laserOutput, new Vector3(xCoord - r + offset, y, zCoord + r).translate(0.3D),
							0.0F, 0.0F, 1.0F, age, 0, 50);
					PacketHandler.sendBeamPacket(worldObj, laserOutput, new Vector3(xCoord + r, y, zCoord + r - offset).translate(0.3D),
							0.0F, 0.0F, 1.0F, age, 0, 50);
					PacketHandler.sendBeamPacket(worldObj, laserOutput, new Vector3(xCoord + r - offset, y, zCoord - r).translate(0.3D),
							0.0F, 0.0F, 1.0F, age, 0, 50);
					PacketHandler.sendBeamPacket(worldObj, laserOutput, new Vector3(xCoord - r, y, zCoord - r + offset).translate(0.3D),
							0.0F, 0.0F, 1.0F, age, 0, 50);
					worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:hilaser", 4F, 1F);
					
					delayTicks = WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS;
					currentState = STATE_MINING;
					updateMetadata(BlockMiningLaser.ICON_MINING_POWERED);
					return;
					
				} else {
					worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
					currentLayer--;
				}
			}
		} else if (currentState == STATE_MINING) {
			if (delayTicks < 0) {
				delayTicks = WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS;
				
				if (valuableIndex < 0 || valuableIndex >= valuablesInLayer.size()) {
					delayTicks = WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS;
					currentState = STATE_SCANNING;
					updateMetadata(BlockMiningLaser.ICON_SCANNING_POWERED);
					
					// rescan same layer
					scanLayer();
					if (valuablesInLayer.size() <= 0) {
						currentLayer--;
					}
					return;
				}
				
				// consume power
				int requiredPower = isOnPlanet ? WarpDriveConfig.MINING_LASER_PLANET_ENERGY_PER_BLOCK : WarpDriveConfig.MINING_LASER_SPACE_ENERGY_PER_BLOCK;
				if (!mineAllBlocks) {
					requiredPower *= WarpDriveConfig.MINING_LASER_ORESONLY_ENERGY_FACTOR;
				}
				if (enableSilktouch) {
					requiredPower *= WarpDriveConfig.MINING_LASER_SILKTOUCH_ENERGY_FACTOR;
				}
				enoughPower = consumeEnergyFromLaserMediums(requiredPower, false);
				if (!enoughPower) {
					updateMetadata(BlockMiningLaser.ICON_MINING_LOW_POWER);
					return;
				} else {
					updateMetadata(BlockMiningLaser.ICON_MINING_POWERED);
				}
				
				VectorI valuable = valuablesInLayer.get(valuableIndex);
				valuableIndex++;
				
				// Mine valuable ore
				Block block = worldObj.getBlock(valuable.x, valuable.y, valuable.z);
				
				// Skip if block is too hard or its empty block (check again in case it changed)
				if (!canDig(block, valuable.x, valuable.y, valuable.z)) {
					delayTicks = Math.round(WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS * 0.2F);
					return;
				}
				int age = Math.max(10, Math.round((4 + worldObj.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
				PacketHandler.sendBeamPacket(worldObj, laserOutput, new Vector3(valuable.x, valuable.y, valuable.z).translate(0.5D),
						1.0F, 1.0F, 0.0F, age, 0, 50);
				worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
				harvestBlock(valuable);
			}
		}
	}
	
	@Override
	protected void stop() {
		super.stop();
		currentState = STATE_IDLE;
		updateMetadata(BlockMiningLaser.ICON_IDLE);
	}
	
	private boolean canDig(Block block, int x, int y, int z) {
		// ignore air
		if (worldObj.isAirBlock(x, y, z)) {
			return false;
		}
		// check blacklists
		if (Dictionary.BLOCKS_SKIPMINING.contains(block)) {
			return false;
		}
		if (Dictionary.BLOCKS_STOPMINING.contains(block)) {
			stop();
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(this + " Mining stopped by " + block + " at (" + x + " " + y + " " + z + ")");
			}
			return false;
		}
		// check whitelist
		if (Dictionary.BLOCKS_MINING.contains(block) || Dictionary.BLOCKS_ORES.contains(block)) {
			return true;
		}
		// check area protection
		if (isBlockBreakCanceled(null, worldObj, x, y, z)) {
			stop();
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(this + " Mining stopped by cancelled event at (" + x + " " + y + " " + z + ")");
			}
			return false;
		}
		// check default (explosion resistance is used to test for force fields and reinforced blocks, basically preventing mining a base or ship) 
		if (block.getExplosionResistance(null) <= Blocks.obsidian.getExplosionResistance(null)) {
			return true;
		}
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(this + " Rejecting " + block + " at (" + x + " " + y + " " + z + ")");
		}
		return false;
	}
	
	private void scanLayer() {
		// WarpDrive.logger.info("Scanning layer");
		Block block;
		for (int y = yCoord - 1; y > currentLayer; y --) {
			block = worldObj.getBlock(xCoord, y, zCoord);
			if (Dictionary.BLOCKS_STOPMINING.contains(block)) {
				stop();
				if (WarpDriveConfig.LOGGING_COLLECTION) {
					WarpDrive.logger.info(this + " Mining stopped by " + block + " at (" + xCoord + " " + y + " " + zCoord + ")");
				}
				return;
			}
		}

		valuablesInLayer.clear();
		valuableIndex = 0;
		int radius, x, z;
		int xMax, zMax;
		int xMin, zMin;
		
		// Search for valuable blocks
		x = xCoord;
		z = zCoord;
		block = worldObj.getBlock(x, currentLayer, z);
		if (canDig(block, x, currentLayer, z)) {
			if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(block)) {// Quarry collects all blocks or only collect valuables blocks
				valuablesInLayer.add(new VectorI(x, currentLayer, z));
			}
		}
		for (radius = 1; radius <= WarpDriveConfig.MINING_LASER_RADIUS_BLOCKS; radius++) {
			xMax = xCoord + radius;
			xMin = xCoord - radius;
			zMax = zCoord + radius;
			zMin = zCoord - radius;
			x = xCoord;
			z = zMin;
			for (; x <= xMax; x++) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(block)) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(new VectorI(x, currentLayer, z));
					}
				}
			}
			x = xMax;
			z++;
			for (; z <= zMax; z++) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(block)) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(new VectorI(x, currentLayer, z));
					}
				}
			}
			x--;
			z = zMax;
			for (; x >= xMin; x--) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(block)) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(new VectorI(x, currentLayer, z));
					}
				}
			}
			x = xMin;
			z--;
			for (; z > zMin; z--) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(block)) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(new VectorI(x, currentLayer, z));
					}
				}
			}
			x = xMin;
			z = zMin;
			for (; x < xCoord; x++) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(block)) {// Quarry collects all blocks or only collect valuables blocks
						valuablesInLayer.add(new VectorI(x, currentLayer, z));
					}
				}
			}
		}

		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(this + " Found " + valuablesInLayer.size() + " valuables");
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		layerOffset = tag.getInteger("layerOffset");
		mineAllBlocks = tag.getBoolean("mineAllBlocks");
		currentState = tag.getInteger("currentState");
		currentLayer = tag.getInteger("currentLayer");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("layerOffset", layerOffset);
		tag.setBoolean("mineAllBlocks", mineAllBlocks);
		tag.setInteger("currentState", currentState);
		tag.setInteger("currentLayer", currentLayer);
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] start(Context context, Arguments arguments) {
		return start();
	}
	
	@SuppressWarnings("SameReturnValue")
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] stop(Context context, Arguments arguments) {
		stop();
		return null;
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] state(Context context, Arguments arguments) {
		return state();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] offset(Context context, Arguments arguments) {
		return offset(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] onlyOres(Context context, Arguments arguments) {
		return onlyOres(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] silktouch(Context context, Arguments arguments) {
		return silktouch(argumentsOCtoCC(arguments));
	}
	
	// Common OC/CC methods
	private Object[] start() {
		if (isActive()) {
			return new Object[] { false, "Already started" };
		}
		
		enableSilktouch &= canSilktouch;
		delayTicks = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
		currentState = STATE_WARMUP;
		currentLayer = yCoord - layerOffset - 1;
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(this + " Starting from Y " + currentLayer + " with silktouch " + enableSilktouch);
		}
		return new Boolean[] { true };
	}
	
	private Object[] state() {
		final int energy = getEnergyStored();
		final String status = getStatusHeaderInPureText();
		final Integer retValuablesInLayer, retValuablesMined;
		if (isActive()) {
			retValuablesInLayer = valuablesInLayer.size();
			retValuablesMined = valuableIndex;
			
			return new Object[] { status, isActive(), energy, currentLayer, retValuablesMined, retValuablesInLayer };
		}
		return new Object[] { status, isActive(), energy, currentLayer, 0, 0 };
	}
	
	private Object[] onlyOres(Object[] arguments) {
		if (arguments.length == 1) {
			try {
				mineAllBlocks = ! Commons.toBool(arguments[0]);
				markDirty();
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.info(this + " onlyOres set to " + !mineAllBlocks);
				}
			} catch (Exception exception) {
				return new Object[] { !mineAllBlocks };
			}
		}
		return new Object[] { !mineAllBlocks };
	}
	
	private Object[] offset(Object[] arguments) {
		if (arguments.length == 1) {
			try {
				layerOffset = Math.min(256, Math.abs(Commons.toInt(arguments[0])));
				markDirty();
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.info(this + " offset set to " + layerOffset);
				}
			} catch (Exception exception) {
				return new Integer[] { layerOffset };
			}
		}
		return new Integer[] { layerOffset };
	}
	
	private Object[] silktouch(Object[] arguments) {
		if (arguments.length == 1) {
			try {
				enableSilktouch = Commons.toBool(arguments[0]);
				markDirty();
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.info(this + " silktouch set to " + enableSilktouch);
				}
			} catch (Exception exception) {
				return new Object[] { enableSilktouch };
			}
		}
		return new Object[] { enableSilktouch };
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		switch (methodName) {
		case "start":
			return start();
			
		case "stop":
			stop();
			return null;
			
		case "state":
			return state();
			
		case "offset":
			return offset(arguments);
			
		case "onlyOres":
			return onlyOres(arguments);
			
		case "silktouch":
			return silktouch(arguments);
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public String getStatusHeader() {
		final int energy = getEnergyStored();
		String state = "IDLE (not mining)";
		if (currentState == STATE_IDLE) {
			state = "IDLE (not mining)";
		} else if (currentState == STATE_WARMUP) {
			state = "Warming up...";
		} else if (currentState == STATE_SCANNING) {
			if (mineAllBlocks) {
				state = "Scanning all";
			} else {
				state = "Scanning ores";
			}
		} else if (currentState == STATE_MINING) {
			if (mineAllBlocks) {
				state = "Mining all";
			} else {
				state = "Mining ores";
			}
			if (enableSilktouch) {
				state = state + " with silktouch";
			}
		}
		if (energy <= 0) {
			state = state + " - Out of energy";
		} else if (((currentState == STATE_SCANNING) || (currentState == STATE_MINING)) && !enoughPower) {
			state = state + " - Not enough power";
		}
		return state;
	}
	
	@Override
	public String toString() {
		return String.format("%s @ \'%s\' %d, %d, %d",
			getClass().getSimpleName(), worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(), xCoord, yCoord, zCoord);
	}
}

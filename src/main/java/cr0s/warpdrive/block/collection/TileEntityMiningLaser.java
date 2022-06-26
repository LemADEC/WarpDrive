package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumMiningLaserMode;
import cr0s.warpdrive.data.FluidWrapper;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.Explosion.Mode;

import net.minecraftforge.registries.ForgeRegistries;

public class TileEntityMiningLaser extends TileEntityAbstractMiner {
	
	// global properties
	private static final UpgradeSlot upgradeSlotPumping = new UpgradeSlot("mining_laser.pumping",
	                                                                      ItemComponent.getItemStackNoCache(EnumComponentType.PUMP, 1),
	                                                                      20);
	private static final boolean canSilktouch = WarpDriveConfig.MINING_LASER_MINE_SILKTOUCH_DEUTERIUM_MB <= 0 
	                                         || ForgeRegistries.FLUIDS.containsKey(new ResourceLocation("deuterium"));
	
	// persistent properties
	private int layerOffset = 1;
	private boolean mineAllBlocks = true;
	
	private static final int STATE_IDLE = 0;
	private static final int STATE_WARMING_UP = 1;
	private static final int STATE_START_SCANNING = 2;
	private static final int STATE_DO_SCANNING = 3;
	private static final int STATE_MINING = 4;
	private int stateCurrent = 0;
	
	private int currentLayer;
	
	// computed properties
	private float explosionResistanceMax = 10000.0F;
	private Explosion explosion;
	private float viscosityMax = 0.0F;
	private int radiusX_actual = 0;
	private int radiusZ_actual = 0;
	private int energyScanning = Integer.MAX_VALUE;
	private int energyMining = Integer.MAX_VALUE;
	private int tickUpdateParameters = 0;
	private int tickCurrentTask = 0;
	private boolean isPowered = false;
	private int radiusCapacity = WarpDriveConfig.MINING_LASER_RADIUS_NO_LASER_MEDIUM;
	private final ArrayList<BlockPos> blockPosValuables = new ArrayList<>();
	private int indexValuable = 0;
	
	public TileEntityMiningLaser(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		laserOutputSide = Direction.DOWN;
		peripheralName = "warpdriveMiningLaser";
		addMethods(new String[] {
				"state",
				"offset",
				"onlyOres",
				"silktouch"
		});
		CC_scripts = Arrays.asList("mine", "stop");
		doRequireUpgradeToInterface();
		
		laserMedium_maxCount = WarpDriveConfig.MINING_LASER_MAX_MEDIUMS_COUNT;
		registerUpgradeSlot(upgradeSlotPumping);
	}
	
	@Override
	protected void onFirstTick() {
		super.onFirstTick();
		assert world != null;
		
		explosion = new Explosion(world, null, pos.getX(), pos.getY(), pos.getZ(), 1, true, Mode.BREAK);
		explosionResistanceMax = Blocks.OBSIDIAN.getExplosionResistance(null, world, pos, null, explosion);
		updateParameters();
	}
	
	private void updateParameters() {
		viscosityMax = getUpgradeCount(upgradeSlotPumping) * 2500;
		
		final boolean hasAtmosphere = CelestialObjectManager.hasAtmosphere(world);
		
		radiusCapacity = WarpDriveConfig.MINING_LASER_RADIUS_NO_LASER_MEDIUM
		                 + (int) Math.floor(cache_laserMedium_factor * WarpDriveConfig.MINING_LASER_RADIUS_PER_LASER_MEDIUM);
		radiusX_actual = radiusCapacity;
		radiusZ_actual = radiusCapacity;
		
		energyScanning = hasAtmosphere ? WarpDriveConfig.MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_ATMOSPHERE : WarpDriveConfig.MINING_LASER_SCAN_ENERGY_PER_LAYER_IN_VOID;
		energyMining = hasAtmosphere ? WarpDriveConfig.MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_ATMOSPHERE : WarpDriveConfig.MINING_LASER_MINE_ENERGY_PER_BLOCK_IN_VOID;
		if (!mineAllBlocks) {
			energyMining *= WarpDriveConfig.MINING_LASER_MINE_ORES_ONLY_ENERGY_FACTOR;
		}
		if (enableSilktouch) {
			energyMining *= WarpDriveConfig.MINING_LASER_MINE_SILKTOUCH_ENERGY_FACTOR;
		}
		
		enableSilktouch &= canSilktouch;
	}
	
	@Override
	public void tick() {
		super.tick();
		
		assert world != null;
		if (world.isRemote()) {
			return;
		}
		
		if (!isEnabled) {
			if (stateCurrent != STATE_IDLE) {
				stateCurrent = STATE_IDLE;
				tickCurrentTask = 0;
				updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.INACTIVE);
			}
			
			// force start if no computer control is available
			if (!isInterfaceEnabled()) {
				enableSilktouch = false;
				layerOffset = 1;
				mineAllBlocks = true;
				setIsEnabled(true);
			}
			return;
		}
		
		// periodically update parameters from main thread
		tickUpdateParameters--;
		if (tickUpdateParameters <= 0) {
			tickUpdateParameters = WarpDriveConfig.MINING_LASER_SETUP_UPDATE_PARAMETERS_TICKS;
			updateParameters();
		}
		
		// execute state transitions
		tickCurrentTask--;
		if (tickCurrentTask > 0) {
			return;
		}
		
		switch (stateCurrent) {
		case STATE_IDLE:
			currentLayer = pos.getY() - layerOffset - 1;
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(String.format("%s Starting from Y %d with silktouch %s",
				                                    this, currentLayer, enableSilktouch));
			}
			
			stateCurrent = STATE_WARMING_UP;
			tickCurrentTask = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
			updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_LOW_POWER);
			break;
		
		case STATE_WARMING_UP:
			stateCurrent = STATE_START_SCANNING;
			tickCurrentTask = 0;
			updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_LOW_POWER);
			break;
		
		case STATE_START_SCANNING:
			// check power level
			isPowered = laserMedium_consumeExactly(energyScanning, true);
			if (!isPowered) {
				tickCurrentTask = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
				updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_LOW_POWER);
				return;
			}
			
			// show current layer
			{
				final int age = Math.max(40, 5 * WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS);
				final int y = currentLayer + 1;
				PacketHandler.sendScanningPacket(world,
				                                 pos.getX() - radiusX_actual, y, pos.getZ() - radiusZ_actual,
				                                 pos.getX() + radiusX_actual + 1, y, pos.getZ() + radiusZ_actual + 1,
				                                 0.3F, 0.0F, 1.0F, age);
			}
			
			stateCurrent = STATE_DO_SCANNING;
			tickCurrentTask = WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS;
			updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_POWERED);
			break;
			
		case STATE_DO_SCANNING:
			if (currentLayer <= 0) {
				setIsEnabled(false);
				tickCurrentTask = 0;
				return;
			}
			
			// consume power
			isPowered = laserMedium_consumeExactly(energyScanning, false);
			if (!isPowered) {
				updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_LOW_POWER);
				tickCurrentTask = WarpDriveConfig.MINING_LASER_WARMUP_DELAY_TICKS;
				return;
			} else {
				updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_POWERED);
			}
			
			// scan
			scanLayer();
			if (blockPosValuables.isEmpty()) {
				world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 1.0F, 0.85F + 0.30F * world.rand.nextFloat());
				currentLayer--;
				tickCurrentTask = WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS;
				return;
			}
			
			// show end of scan with a spinning cone
			{
				final int r = (int) Math.ceil(radiusCapacity / 2.0D);
				final int offset = (pos.getY() - currentLayer) % (2 * r);
				final int age = Math.max(20, Math.round(2.5F * WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS));
				final double y = currentLayer + 1.0D;
				PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(pos.getX() - r + offset, y, pos.getZ() + r).translate(0.3D),
				                             0.0F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(pos.getX() + r, y, pos.getZ() + r - offset).translate(0.3D),
				                             0.0F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(pos.getX() + r - offset, y, pos.getZ() - r).translate(0.3D),
				                             0.0F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(pos.getX() - r, y, pos.getZ() - r + offset).translate(0.3D),
				                             0.0F, 0.0F, 1.0F, age, 0, 50);
				world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.BLOCKS, 1.0F, 0.85F + 0.30F * world.rand.nextFloat());
			}
			
			// switch to mining
			if (stateCurrent == STATE_DO_SCANNING) {// remain stopped if an hard block was encountered
				stateCurrent = STATE_MINING;
			}
			tickCurrentTask = WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS;
			updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.MINING_POWERED);
			break;
		
		case STATE_MINING:
			tickCurrentTask = WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS;
			
			if (indexValuable < 0 || indexValuable >= blockPosValuables.size()) {
				stateCurrent = STATE_START_SCANNING;
				tickCurrentTask = WarpDriveConfig.MINING_LASER_SCAN_DELAY_TICKS;
				updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.SCANNING_POWERED);
				
				// rescan same layer
				scanLayer();
				if (blockPosValuables.size() <= 0) {
					currentLayer--;
				}
				return;
			}
			
			// consume power
			isPowered = laserMedium_consumeExactly(energyMining, false);
			if (!isPowered) {
				updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.MINING_LOW_POWER);
				return;
			} else {
				updateBlockState(null, BlockMiningLaser.MODE, EnumMiningLaserMode.MINING_POWERED);
			}
			
			final BlockPos blockPosValuable = blockPosValuables.get(indexValuable);
			indexValuable++;
			
			// Mine valuable ore
			final BlockState blockStateValuable = world.getBlockState(blockPosValuable);
			
			// Skip if block is too hard or its empty block (check again in case it changed)
			if (!canDig(blockStateValuable, blockPosValuable)) {
				tickCurrentTask = Math.round(WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS * 0.2F);
				return;
			}
			
			{
				final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
				PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(blockPosValuable).translate(0.5D),
						1.0F, 1.0F, 0.0F, age, 0, 50);
				world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 1.0F, 0.85F + 0.30F * world.rand.nextFloat());
			}
			harvestBlock(blockPosValuable, blockStateValuable);
			break;
		
		default:
			stateCurrent = STATE_IDLE;
			WarpDrive.logger.error(String.format("%s Invalid state %d, please report to mod author",
			                                     this, stateCurrent));
			break;
		}
	}
	
	private boolean canDig(@Nonnull final BlockState blockState, final BlockPos blockPos) {
		assert world != null;
		final Block block = blockState.getBlock();
		// ignore air
		if (world.isAirBlock(blockPos)) {
			return false;
		}
		// check blacklists
		if (Dictionary.BLOCKS_SKIPMINING.contains(block)) {
			return false;
		}
		if (Dictionary.BLOCKS_STOPMINING.contains(block)) {
			setIsEnabled(false);
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(String.format("%s Mining stopped by %s %s",
				                                    this, blockState, Commons.format(world, blockPos)));
			}
			return false;
		}
		// check area protection
		if (isBlockBreakCanceled(null, world, blockPos)) {
			setIsEnabled(false);
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(String.format("%s Mining stopped by cancelled event %s",
				                                    this, Commons.format(world, blockPos)));
			}
			return false;
		}
		// check liquids
		final Fluid fluid = FluidWrapper.getFluid(blockState);
		if (fluid != null) {
			return viscosityMax >= fluid.getAttributes().getViscosity();
		}
		// check whitelist
		if ( Dictionary.BLOCKS_MINING.contains(block)
		  || Dictionary.BLOCKS_ORES.contains(block) ) {
			return true;
		}
		// check default (explosion resistance is used to test for force fields and reinforced blocks, basically preventing mining a base or ship)
		final float explosionResistance = blockState.getExplosionResistance(world, blockPos, null, explosion);
		if (explosionResistance <= explosionResistanceMax) {
			return true;
		}
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(String.format("%s Rejecting %s %s %s with explosion resistance %.1f",
			                                    this, blockState, blockState.getBlock().getRegistryName(),
			                                    Commons.format(world, blockPos), explosionResistance));
		}
		return false;
	}
	
	private void scanLayer() {
		assert world != null;
		// WarpDrive.logger.info("Scanning layer");
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(pos);
		BlockState blockState;
		for (int y = pos.getY() - 1; y > currentLayer; y --) {
			mutableBlockPos.setPos(pos.getX(), y, pos.getZ());
			blockState = world.getBlockState(mutableBlockPos);
			if (Dictionary.BLOCKS_STOPMINING.contains(blockState.getBlock())) {
				setIsEnabled(false);
				if (WarpDriveConfig.LOGGING_COLLECTION) {
					WarpDrive.logger.info(String.format("%s Mining stopped by %s %s",
					                                    this, blockState, Commons.format(world, pos)));
				}
				return;
			}
		}
		
		BlockPos blockPos;
		blockPosValuables.clear();
		indexValuable = 0;
		int radius, x, z;
		int xMax, zMax;
		int xMin, zMin;
		
		// Search for valuable blocks
		x = pos.getX();
		z = pos.getZ();
		blockPos = new BlockPos(x, currentLayer, z);
		blockState = world.getBlockState(blockPos);
		if (canDig(blockState, blockPos)) {
			if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
				blockPosValuables.add(blockPos);
			}
		}
		for (radius = 1; radius <= radiusCapacity; radius++) {
			xMax = pos.getX() + radius;
			xMin = pos.getX() - radius;
			zMax = pos.getZ() + radius;
			zMin = pos.getZ() - radius;
			x = pos.getX();
			z = zMin;
			for (; x <= xMax; x++) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						blockPosValuables.add(blockPos);
					}
				}
			}
			x = xMax;
			z++;
			for (; z <= zMax; z++) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						blockPosValuables.add(blockPos);
					}
				}
			}
			x--;
			z = zMax;
			for (; x >= xMin; x--) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						blockPosValuables.add(blockPos);
					}
				}
			}
			x = xMin;
			z--;
			for (; z > zMin; z--) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						blockPosValuables.add(blockPos);
					}
				}
			}
			x = xMin;
			z = zMin;
			for (; x < pos.getX(); x++) {
				blockPos = new BlockPos(x, currentLayer, z);
				blockState = world.getBlockState(blockPos);
				if (canDig(blockState, blockPos)) {
					if (mineAllBlocks || Dictionary.BLOCKS_ORES.contains(blockState.getBlock())) {// Quarry collects all blocks or only collect valuables blocks
						blockPosValuables.add(blockPos);
					}
				}
			}
		}
		
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(String.format("%s Found %s valueables",
			                                    this, blockPosValuables.size()));
		}
	}
	
	@Override
	protected void onUpgradeChanged(@Nonnull final UpgradeSlot upgradeSlot, final int countNew, final boolean isAdded) {
		super.onUpgradeChanged(upgradeSlot, countNew, isAdded);
		if (upgradeSlot.equals(upgradeSlotPumping)) {
			updateParameters();
		}
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		
		layerOffset = tagCompound.getInt("layerOffset");
		mineAllBlocks = tagCompound.getBoolean("mineAllBlocks");
		stateCurrent = tagCompound.getInt("stateCurrent");
		currentLayer = tagCompound.getInt("currentLayer");
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		
		tagCompound.putInt("layerOffset", layerOffset);
		tagCompound.putBoolean("mineAllBlocks", mineAllBlocks);
		tagCompound.putInt("stateCurrent", stateCurrent);
		tagCompound.putInt("currentLayer", currentLayer);
		return tagCompound;
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	@Callback(direct = true)
	public Object[] offset(final Context context, final Arguments arguments) {
		return offset(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] onlyOres(final Context context, final Arguments arguments) {
		return onlyOres(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] silktouch(final Context context, final Arguments arguments) {
		return silktouch(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// Common OC/CC methods
	private Object[] state() {
		final String units = energy_getDisplayUnits();
		final long energy = EnergyWrapper.convert(laserMedium_getEnergyStored(true), units);
		final String status = getStatusHeaderInPureText();
		final int return_indexValuable, return_countValuables;
		if (stateCurrent != STATE_IDLE) {
			return_indexValuable = indexValuable;
			return_countValuables = blockPosValuables.size();
		} else {
			return_indexValuable = 0;
			return_countValuables = 0;
		}
		return new Object[] { status, stateCurrent != STATE_IDLE, energy, currentLayer, return_indexValuable, return_countValuables };
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] { true,
		                      EnergyWrapper.convert(energyScanning, units),
		                      EnergyWrapper.convert(energyMining, units) };
	}
	
	private Object[] onlyOres(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				mineAllBlocks = ! Commons.toBool(arguments[0]);
				markDirty();
			} catch (final Exception exception) {
				return new Object[] { !mineAllBlocks };
			}
		}
		return new Object[] { !mineAllBlocks };
	}
	
	private Object[] offset(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				layerOffset = Math.min(256, Math.abs(Commons.toInt(arguments[0])));
				markDirty();
			} catch (final Exception exception) {
				return new Integer[] { layerOffset };
			}
		}
		return new Integer[] { layerOffset };
	}
	
	private Object[] silktouch(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				enableSilktouch = Commons.toBool(arguments[0]);
				markDirty();
			} catch (final Exception exception) {
				return new Object[] { enableSilktouch };
			}
		}
		return new Object[] { enableSilktouch };
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "state":
			return state();
			
		case "offset":
			return offset(arguments);
			
		case "onlyOres":
			return onlyOres(arguments);
			
		case "silktouch":
			return silktouch(arguments);
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
	
	@Override
	public WarpDriveText getStatusHeader() {
		final int energy = laserMedium_getEnergyStored(true);
		WarpDriveText textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.error.internal_check_console");
		if (stateCurrent == STATE_IDLE) {
			textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.mining_laser.status_line.idle");
		} else if (stateCurrent == STATE_WARMING_UP) {
			textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.mining_laser.status_line.warming_up");
		} else if ( stateCurrent == STATE_START_SCANNING
		         || stateCurrent == STATE_DO_SCANNING ) {
			if (mineAllBlocks) {
				textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.mining_laser.status_line.scanning_all");
			} else {
				textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.mining_laser.status_line.scanning_ores");
			}
		} else if (stateCurrent == STATE_MINING) {
			if (!enableSilktouch) {
				if (mineAllBlocks) {
					textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.mining_laser.status_line.mining_all");
				} else {
					textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.mining_laser.status_line.mining_ores");
				}
			} else {
				if (mineAllBlocks) {
					textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.mining_laser.status_line.mining_all_with_silktouch");
				} else {
					textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.mining_laser.status_line.mining_ores_with_silktouch");
				}
			}
		}
		if (energy <= 0) {
			textState.appendSibling(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.mining_laser.status_line._insufficient_energy"));
		} else if ( ( stateCurrent == STATE_START_SCANNING
		           || stateCurrent == STATE_DO_SCANNING
		           || stateCurrent == STATE_MINING )
		         && !isPowered ) {
			textState.appendSibling(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.mining_laser.status_line._insufficient_energy"));
		}
		return textState;
	}
}
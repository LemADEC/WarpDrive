package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockStatePos;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumLaserTreeFarmMode;
import cr0s.warpdrive.data.EnumTaskResult;
import cr0s.warpdrive.data.InventoryWrapper;
import cr0s.warpdrive.data.MutableBlockItemUseContext;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorldReader;

import net.minecraftforge.common.IPlantable;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class TileEntityLaserTreeFarm extends TileEntityAbstractMiner {
	
	// persistent properties
	private int radiusX_requested = WarpDriveConfig.TREE_FARM_totalMaxRadius;
	private int radiusZ_requested = WarpDriveConfig.TREE_FARM_totalMaxRadius;
	private boolean breakLeaves = true;
	private boolean tapTrees = true;
	
	private static final int STATE_IDLE = 0;
	private static final int STATE_WARMING_UP = 1;
	private static final int STATE_SCANNING = 2;
	private static final int STATE_PLANTING = 3;
	private static final int STATE_HARVESTING = 4;
	private int currentState = STATE_IDLE;
	
	// computed properties
	private int radiusX_actual = radiusX_requested;
	private int radiusZ_actual = radiusZ_requested;
	private AxisAlignedBB axisAlignedBBSoil = null;
	private AxisAlignedBB axisAlignedBBScan = null;
	private int maxDistance = 0;
	private int energyScanning;
	private int energyTappingWetSpot;
	private int energyTappingRubberLog;
	private int energyHarvestingLog;
	private int energyHarvestingLeaf;
	private int energyPlanting;
	
	private boolean isPowered = false;
	
	private int totalHarvested = 0;
	
	private int tickCurrentTask = 0;
	
	private ArrayList<BlockPos> blockPosSoils = new ArrayList<>(0);
	private int indexSoil = 0;
	private ArrayList<BlockStatePos> blockPosValuables = new ArrayList<>(0);
	private int indexValuable = 0;
	
	public TileEntityLaserTreeFarm(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		laserOutputSide = Direction.UP;
		peripheralName = "warpdriveLaserTreeFarm";
		addMethods(new String[] {
				"start",
				"stop",
				"radius",
				"state",
				"breakLeaves",
				"silktouch",
				"tapTrees"
		});
		CC_scripts = Arrays.asList("farm", "stop");
		doRequireUpgradeToInterface();
		
		laserMedium_maxCount = WarpDriveConfig.TREE_FARM_MAX_MEDIUMS_COUNT;
	}
	
	@Override
	protected void onFirstTick() {
		super.onFirstTick();
		assert world != null;
		
		if (currentState == STATE_PLANTING || currentState == STATE_HARVESTING) {
			updateParameters();
			try {
				blockPosSoils = calculate_getSoilPositions(world, axisAlignedBBSoil);
				indexSoil = 0;
				blockPosValuables = calculate_getValuableStatePositions(world, axisAlignedBBScan, breakLeaves, maxDistance, this::comparatorSortLogsAndLeaves);
				indexValuable = 0;
			} catch (final Exception exception) {
				// not so supposed to happen, so just dump logs for now
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("%s Calculation failed, please report to mod author %s",
				                                     this, exception.getMessage() ));
				// retry with a slight delay
				currentState = STATE_SCANNING;
				tickCurrentTask = 2;
			}
		}
	}
	
	@SuppressWarnings("UnnecessaryReturnStatement")
	@Override
	public void tick() {
		super.tick();
		
		assert world != null;
		if (world.isRemote()) {
			return;
		}
		
		final BlockState blockState = world.getBlockState(pos);
		if (!isEnabled) {
			currentState = STATE_IDLE;
			tickCurrentTask = 0;
			updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.INACTIVE);
			
			// force start if no computer control is available
			if (!isInterfaceEnabled()) {
				breakLeaves = true;
				enableSilktouch = false;
				tapTrees = true;
				setIsEnabled(true);
			}
			return;
		}
		
		tickCurrentTask--;
		
		updateParameters();
		
		// state machine
		if (currentState == STATE_IDLE) {
			totalHarvested = 0;
			currentState = STATE_WARMING_UP;
			tickCurrentTask = 0;
			
		} else if (currentState == STATE_WARMING_UP) {
			if (isPowered) {
				updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_POWERED);
			} else {
				updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_LOW_POWER);
			}
			// wait for cool down
			if (tickCurrentTask >= 0) {
				return;
			}
			
			// validate environment: loaded chunks
			final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
			final BlockState blockStateMinMin = Commons.getBlockState_noChunkLoading(world, mutableBlockPos.setPos(axisAlignedBBScan.minX, axisAlignedBBScan.minY, axisAlignedBBScan.minZ));
			final BlockState blockStateMinMax = Commons.getBlockState_noChunkLoading(world, mutableBlockPos.setPos(axisAlignedBBScan.minX, axisAlignedBBScan.minY, axisAlignedBBScan.maxZ));
			final BlockState blockStateMaxMin = Commons.getBlockState_noChunkLoading(world, mutableBlockPos.setPos(axisAlignedBBScan.maxX, axisAlignedBBScan.maxY, axisAlignedBBScan.minZ));
			final BlockState blockStateMaxMax = Commons.getBlockState_noChunkLoading(world, mutableBlockPos.setPos(axisAlignedBBScan.maxX, axisAlignedBBScan.maxY, axisAlignedBBScan.maxZ));
			if ( blockStateMinMin == null 
			  || blockStateMinMax == null
			  || blockStateMaxMin == null
			  || blockStateMaxMax == null ) {
				currentState = STATE_WARMING_UP;
				tickCurrentTask = WarpDriveConfig.TREE_FARM_WARM_UP_DELAY_TICKS;
				return;
			}
			
			// validate environment: clearance above
			final BlockState blockStateAbove = world.getBlockState(pos.up());
			final Block blockAbove = blockStateAbove.getBlock();
			if ( !Dictionary.isLog(blockAbove)
			  && !Dictionary.isLeaf(blockAbove)
			  && !blockAbove.isAir(blockStateAbove, world, pos.up()) ) {
				PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
				                                      new Vector3(pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D),
				                                      new Vector3(0.0D, 0.0D, 0.0D),
				                                      1.0F, 1.0F, 1.0F,
				                                      1.0F, 1.0F, 1.0F,
				                                      32);
				
				currentState = STATE_WARMING_UP;
				tickCurrentTask = WarpDriveConfig.TREE_FARM_WARM_UP_DELAY_TICKS;
				return;
			}
			
			// consume energy
			isPowered = laserMedium_consumeExactly(energyScanning, false);
			if (!isPowered) {
				currentState = STATE_WARMING_UP;
				tickCurrentTask = WarpDriveConfig.TREE_FARM_WARM_UP_DELAY_TICKS;
				return;
			}
			
			// show current layer
			final int age = Math.max(40, 2 * WarpDriveConfig.TREE_FARM_SCAN_DELAY_TICKS);
			final int y = pos.getY() + world.rand.nextInt(9);
			PacketHandler.sendScanningPacket(world,
			                                 pos.getX() - radiusX_actual    , y, pos.getZ() - radiusZ_actual    ,
			                                 pos.getX() + radiusX_actual + 1, y, pos.getZ() + radiusZ_actual + 1,
			                                 0.3F, 0.0F, 1.0F, age);
			
			// switch to scanning
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.debug("Start scanning");
			}
			isDirty.set(true);
			currentState = STATE_SCANNING;
			tickCurrentTask = WarpDriveConfig.TREE_FARM_SCAN_DELAY_TICKS;
			return;
			
		} else if (currentState == STATE_SCANNING) {
			if (isPowered) {
				updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_POWERED);
			} else {
				updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_LOW_POWER);
			}
			// start thread, wait for it to be done
			if (!isCalculated()) {
				calculation_start();
				return;
			}
			// wait for task delay
			if (tickCurrentTask >= 0) {
				return;
			}
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.debug("Scanning done");
			}
			
			if (!blockPosSoils.isEmpty() && !InventoryWrapper.getConnectedInventories(world, pos).isEmpty()) {
				world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.BLOCKS, 1.0F, 0.85F + 0.30F * world.rand.nextFloat());
				currentState = STATE_PLANTING;
				tickCurrentTask = WarpDriveConfig.TREE_FARM_PLANT_DELAY_TICKS;
				return;
				
			} else if (!blockPosValuables.isEmpty()) {
				world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.BLOCKS, 1.0F, 0.85F + 0.30F * world.rand.nextFloat());
				currentState = STATE_HARVESTING;
				tickCurrentTask = WarpDriveConfig.TREE_FARM_HARVEST_LOG_DELAY_TICKS;
				return;
				
			} else {
				world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 1.0F, 0.85F + 0.30F * world.rand.nextFloat());
				currentState = STATE_WARMING_UP;
				tickCurrentTask = WarpDriveConfig.TREE_FARM_WARM_UP_DELAY_TICKS;
				return;
			}
			
		} else if (currentState == STATE_PLANTING) {
			if (isPowered) {
				updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.PLANTING_POWERED);
			} else {
				updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.PLANTING_LOW_POWER);
			}
			// wait for task delay
			if (tickCurrentTask >= 0) {
				return;
			}
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.debug("Planting");
			}
			
			// planting done, move to harvesting
			if (indexSoil >= blockPosSoils.size()) {
				currentState = STATE_HARVESTING;
				tickCurrentTask = WarpDriveConfig.TREE_FARM_HARVEST_LOG_DELAY_TICKS;
				return;
			}
			
			// get current block
			final BlockPos blockPosSoil = blockPosSoils.get(indexSoil);
			final BlockState blockStateSoil = world.getBlockState(blockPosSoil);
			indexSoil++;
			
			switch (doPlanting(blockPosSoil, blockStateSoil)) {
			case CONTINUE:
				break;
				
			case RETRY:
				tickCurrentTask = WarpDriveConfig.TREE_FARM_WARM_UP_DELAY_TICKS;
				indexSoil--;
				break;
				
			case SKIP:
				indexSoil = blockPosSoils.size();
				break;
			}
			
		} else if (currentState == STATE_HARVESTING) {
			if (isPowered) {
				updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.FARMING_POWERED);
			} else {
				updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.FARMING_LOW_POWER);
			}
			// wait for task delay
			if (tickCurrentTask >= 0) {
				return;
			}
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.debug("Harvest/tap tick");
			}
			
			// harvesting done, back to scanning
			if (indexValuable >= blockPosValuables.size()) {
				currentState = STATE_WARMING_UP;
				tickCurrentTask = 0;
				return;
			}
			
			// get current block
			final BlockStatePos blockStatePosValuable = blockPosValuables.get(indexValuable);
			final BlockPos blockPosValuable = blockStatePosValuable.blockPos;
			final BlockState blockStateValuable = world.getBlockState(blockPosValuable);
			indexValuable++;
			
			// validate cache
			// if a tree grew, we skip it for next scan so the top don't remain floating => block has changed
			// if leaf decayed, it's gone anyway => block has changed
			// if IC2 rubber spot is now wet, we continue => blocks hasn't changed, only metadata
			if (blockStateValuable.getBlock() != blockStatePosValuable.blockState.getBlock()) {
				return;
			}
			
			switch (doHarvesting(blockPosValuable, blockStateValuable)) {
			case CONTINUE:
				break;
				
			case RETRY:
				tickCurrentTask = WarpDriveConfig.TREE_FARM_WARM_UP_DELAY_TICKS;
				indexValuable--;
				break;
				
			case SKIP:
				indexValuable = blockPosValuables.size();
				break;
			}
			
		}
	}
	
	private EnumTaskResult doPlanting(final BlockPos blockPosSoil, final BlockState blockStateSoil) {
		assert world != null;
		final BlockPos blockPosPlant = blockPosSoil.add(0, 1, 0);
		final MutableBlockItemUseContext blockItemUseContext = new MutableBlockItemUseContext(
				world, null, Hand.MAIN_HAND, ItemStack.EMPTY,
				new BlockRayTraceResult(new Vec3d(blockPosPlant), Direction.UP, blockPosPlant, false) );
		
		final Collection<Object> inventories = InventoryWrapper.getConnectedInventories(world, pos);
		
		int indexSlotPlant = 0;
		int countPlantable = 0;
		ItemStack itemStackPlant = null;
		BlockState blockStatePlant = null;
		Object inventoryPlant = null;
		for (final Object inventoryLoop : inventories) {
			indexSlotPlant = 0;
			while (indexSlotPlant < InventoryWrapper.getSize(inventoryLoop) && blockStatePlant == null) {
				itemStackPlant = InventoryWrapper.getStackInSlot(inventoryLoop, indexSlotPlant);
				if (itemStackPlant.isEmpty()) {
					indexSlotPlant++;
					continue;
				}
				final Block blockFromItem;
				if (itemStackPlant.getItem() instanceof BlockItem) {
					blockFromItem = Block.getBlockFromItem(itemStackPlant.getItem());
				} else {
					blockFromItem = null;
				}
				if ( !(itemStackPlant.getItem() instanceof IPlantable)
				  && !(blockFromItem instanceof IPlantable) ) {
					indexSlotPlant++;
					continue;
				}
				countPlantable++;
				final IPlantable plantable = (IPlantable) ((itemStackPlant.getItem() instanceof IPlantable) ? itemStackPlant.getItem() : blockFromItem);
				if (itemStackPlant.getItem() instanceof BlockItem) {
					final BlockItem itemBlock = (BlockItem) itemStackPlant.getItem();
					final Block block = itemBlock.getBlock();
					blockStatePlant = block.getDefaultState();
				} else {
					blockStatePlant = plantable.getPlant(world, blockPosPlant);
				}
				if (WarpDriveConfig.LOGGING_COLLECTION) {
					WarpDrive.logger.info(String.format("Slot %d as %s which plantable %s as block %s",
					                                    indexSlotPlant, itemStackPlant, plantable, blockStatePlant));
				}
				
				if (!blockStateSoil.getBlock().canSustainPlant(blockStateSoil, world, blockPosSoil, Direction.UP, plantable)) {
					blockStatePlant = null;
					indexSlotPlant++;
					continue;
				}
				
				blockItemUseContext.setItemStack(itemStackPlant);
				if (!blockItemUseContext.canPlace()) {
					blockStatePlant = null;
					indexSlotPlant++;
					continue;
				}
				
				inventoryPlant = inventoryLoop;
			}
			
			// exit the loop if we've found a valid plant
			if (blockStatePlant != null) {
				break;
			}
		}
		
		// no plantable found at all, skip this state
		if (countPlantable <= 0) {
			return EnumTaskResult.SKIP;
		}
		
		// no plantable found for this soil, moving on...
		if ( blockStatePlant == null
		  || itemStackPlant.isEmpty()
		  || inventoryPlant == null ) {
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.debug("No plantable found");
			}
			return EnumTaskResult.CONTINUE;
		}
		
		// check area protection
		if (isBlockPlaceCanceled(null, world, blockPosPlant, blockStatePlant)) {
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(String.format("%s Planting cancelled %s",
				                                    this, Commons.format(world, blockPosPlant)));
			}
			return EnumTaskResult.CONTINUE;
		}
		
		// consume power
		isPowered = laserMedium_consumeExactly(energyPlanting, false);
		if (!isPowered) {
			return EnumTaskResult.RETRY;
		}
		
		InventoryWrapper.decrStackSize(inventoryPlant, indexSlotPlant, 1);
		
		// totalPlanted++;
		final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
		PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(blockPosPlant).translate(0.5D),
		                             0.2F, 0.7F, 0.4F, age, 0, 50);
		world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 1.0F, 0.35F + 0.30F * world.rand.nextFloat());
		world.setBlockState(blockPosPlant, blockStatePlant, 3);
		
		tickCurrentTask = WarpDriveConfig.TREE_FARM_PLANT_DELAY_TICKS;
		return EnumTaskResult.CONTINUE;
	}
	
	private EnumTaskResult doHarvesting(final BlockPos blockPosValuable, final BlockState blockStateValuable) {
		assert world != null;
		// check area protection
		if (isBlockBreakCanceled(null, world, blockPosValuable)) {
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(String.format("%s Harvesting cancelled %s",
				                                    this, Commons.format(world, blockPosValuable) ));
			}
			return EnumTaskResult.CONTINUE;
		}
		
		// save the rubber producing blocks in tapping mode
		final boolean isLog = Dictionary.isLog(blockStateValuable.getBlock());
		if (isLog && tapTrees) {
			// IC2 rubber tree wet spot
			/* TODO MC1.15 enable IC2 support once it's updated
			if (blockStateValuable.getBlock() == WarpDriveConfig.IC2_rubberWood) {
				final int metadata = blockStateValuable.getBlock().getMetaFromState(blockStateValuable);
				if (metadata >= 2 && metadata <= 5) {
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.info(String.format("Tapping rubber wood wet-spot %s (%d) %s",
						                                    blockStateValuable, metadata, Commons.format(world, blockPosValuable) ));
					}
					
					// consume power
					isPowered = laserMedium_consumeExactly(energyTappingWetSpot, false);
					if (!isPowered) {
						return EnumTaskResult.RETRY;
					}
					
					final ItemStack itemStackResin = WarpDriveConfig.IC2_Resin.copy();
					itemStackResin.setCount( (int) Math.round(Math.random() * 4.0D) );
					if (InventoryWrapper.addToConnectedInventories(world, pos, itemStackResin)) {
						setIsEnabled(false);
					}
					totalHarvested += itemStackResin.getCount();
					final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.TREE_FARM_HARVEST_LOG_DELAY_TICKS));
					PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(blockPosValuable).translate(0.5D),
					                             0.8F, 0.8F, 0.2F, age, 0, 50);
					world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 1.0F, 0.35F + 0.30F * world.rand.nextFloat());
					
					world.setBlockState(blockPosValuable, blockStateValuable.getBlock().getStateFromMeta(metadata + 6), 3);
					
					tickCurrentTask = WarpDriveConfig.TREE_FARM_TAP_WET_SPOT_DELAY_TICKS;
					return EnumTaskResult.CONTINUE;
					
				} else if (metadata != 0 && metadata != 1) {
					tickCurrentTask = WarpDriveConfig.TREE_FARM_TAP_DRY_SPOT_DELAY_TICKS;
					return EnumTaskResult.CONTINUE;
				}
			}
			*/
			
			// Jungle wood to raw rubber
			if (blockStateValuable.getBlock().getTags().contains(BlockTags.JUNGLE_LOGS.getId())) {
				if (WarpDriveConfig.LOGGING_COLLECTION) {
					WarpDrive.logger.info(String.format("Tapping jungle wood %s %s",
					                                    blockStateValuable, Commons.format(world, blockPosValuable)) );
				}
				
				// consume power
				isPowered = laserMedium_consumeExactly(energyTappingRubberLog, false);
				if (!isPowered) {
					return EnumTaskResult.RETRY;
				}
				
				final ItemStack itemStackRawRubber = ItemComponent.getItemStackNoCache(EnumComponentType.RAW_RUBBER, 1);
				if (InventoryWrapper.addToConnectedInventories(world, pos, itemStackRawRubber)) {
					setIsEnabled(false);
				}
				totalHarvested += itemStackRawRubber.getCount();
				final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.TREE_FARM_HARVEST_LOG_DELAY_TICKS));
				PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(blockPosValuable).translate(0.5D),
				                             0.8F, 0.8F, 0.2F, age, 0, 50);
				world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 1.0F, 0.35F + 0.30F * world.rand.nextFloat());
				
				world.removeBlock(blockPosValuable, false);
				
				tickCurrentTask = WarpDriveConfig.TREE_FARM_TAP_RUBBER_LOG_DELAY_TICKS;
				return EnumTaskResult.CONTINUE;
			}
		}
		
		// actually break the block
		final boolean isLeaf = Dictionary.isLeaf(blockStateValuable.getBlock());
		final boolean isStackingPlant = Dictionary.isStackingPlant(blockStateValuable.getBlock());
		final boolean isGrown = blockStateValuable.getBlock() instanceof IGrowable
		                     && !((IGrowable) blockStateValuable.getBlock()).canGrow(world, blockPosValuable, blockStateValuable, world.isRemote);
		if ( isLog
		  || (breakLeaves && isLeaf)
		  || isGrown
		  || isStackingPlant ) {
			// consume power
			final int energyCost = isLog ? energyHarvestingLog : energyHarvestingLeaf;
			isPowered = laserMedium_consumeExactly(energyCost, false);
			if (!isPowered) {
				return EnumTaskResult.RETRY;
			}
			
			totalHarvested++;
			final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
			PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(blockPosValuable).translate(0.5D),
			                             0.2F, 0.7F, 0.4F, age, 0, 50);
			world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 1.0F, 0.85F + 0.30F * world.rand.nextFloat());
			
			harvestBlock(blockPosValuable, blockStateValuable);
			
			tickCurrentTask = isLog || isGrown ? WarpDriveConfig.TREE_FARM_HARVEST_LOG_DELAY_TICKS
			                                   : enableSilktouch ? WarpDriveConfig.TREE_FARM_SILKTOUCH_LEAF_DELAY_TICKS
			                                                     : WarpDriveConfig.TREE_FARM_BREAK_LEAF_DELAY_TICKS;
			return EnumTaskResult.CONTINUE;
		}
		
		return EnumTaskResult.CONTINUE;
	}
	
	private void updateParameters() {
		final int maxRadius = WarpDriveConfig.TREE_FARM_MAX_RADIUS_NO_LASER_MEDIUM
		                    + (int) Math.floor(cache_laserMedium_factor * WarpDriveConfig.TREE_FARM_MAX_RADIUS_PER_LASER_MEDIUM);
		radiusX_actual = Math.min(radiusX_requested, maxRadius);
		radiusZ_actual = Math.min(radiusZ_requested, maxRadius);
		
		axisAlignedBBSoil = new AxisAlignedBB(
				pos.getX() - radiusX_actual,
				pos.getY(),
				pos.getZ() - radiusZ_actual,
				pos.getX() + radiusX_actual,
				pos.getY() + 8,
				pos.getZ() + radiusZ_actual);
		axisAlignedBBScan = new AxisAlignedBB(
				pos.getX() - radiusX_actual,
				pos.getY() + 1,
				pos.getZ() - radiusZ_actual,
				pos.getX() + radiusX_actual,
				pos.getY() + 1 + (tapTrees ? 8 : 0),
				pos.getZ() + radiusZ_actual);
		
		maxDistance = WarpDriveConfig.TREE_FARM_MAX_DISTANCE_NO_LASER_MEDIUM
		            + (int) Math.floor(cache_laserMedium_factor * WarpDriveConfig.TREE_FARM_MAX_DISTANCE_PER_MEDIUM);
		
		energyScanning = WarpDriveConfig.TREE_FARM_SCAN_ENERGY_PER_SURFACE * (1 + 2 * radiusX_actual) * (1 + 2 * radiusZ_actual);
		energyTappingWetSpot = WarpDriveConfig.TREE_FARM_TAP_WET_SPOT_ENERGY_PER_BLOCK;
		energyTappingRubberLog = WarpDriveConfig.TREE_FARM_TAP_RUBBER_LOG_ENERGY_PER_BLOCK;
		energyHarvestingLog = (enableSilktouch ? WarpDriveConfig.TREE_FARM_SILKTOUCH_LOG_ENERGY_PER_BLOCK : WarpDriveConfig.TREE_FARM_HARVEST_LOG_ENERGY_PER_BLOCK );
		energyHarvestingLeaf = (enableSilktouch ? WarpDriveConfig.TREE_FARM_SILKTOUCH_LEAF_ENERGY_PER_BLOCK : WarpDriveConfig.TREE_FARM_HARVEST_LEAF_ENERGY_PER_BLOCK);
		energyPlanting = WarpDriveConfig.TREE_FARM_PLANT_ENERGY_PER_BLOCK;
	}
	
	@Override
	protected boolean calculation_start() {
		final boolean isStarting = super.calculation_start();
		if (isStarting) {
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(String.format("Calculation initiated for %s",
				                                    this));
			}
			blockPosSoils = new ArrayList<>(0);
			blockPosValuables = new ArrayList<>(0);
			
			new ThreadCalculation(this).start();
		}
		return isStarting;
	}
	
	private void calculation_done(final ArrayList<BlockPos> blockPosSoils, final ArrayList<BlockStatePos> blockPosValuables) {
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(String.format("Calculation done for %s",
			                                    this));
		}
		if (blockPosSoils == null || blockPosValuables == null) {
			this.blockPosSoils = new ArrayList<>(0);
			this.blockPosValuables = new ArrayList<>(0);
			currentState = STATE_WARMING_UP;
			tickCurrentTask = WarpDriveConfig.TREE_FARM_WARM_UP_DELAY_TICKS;
		} else {
			this.blockPosSoils = blockPosSoils;
			this.blockPosValuables = blockPosValuables;
		}
		indexSoil = 0;
		indexValuable = 0;
		calculation_done();
	}
	
	private static ArrayList<BlockPos> calculate_getSoilPositions(@Nonnull final IWorldReader worldReader,
	                                                              @Nonnull final AxisAlignedBB axisAlignedBB) throws ExceptionChunkNotLoaded {
		final boolean isSafeThread = Commons.isSafeThread();
		final int xMin = (int) axisAlignedBB.minX;
		final int xMax = (int) axisAlignedBB.maxX;
		final int yMin = (int) axisAlignedBB.minY;
		final int yMax = (int) axisAlignedBB.maxY;
		final int zMin = (int) axisAlignedBB.minZ;
		final int zMax = (int) axisAlignedBB.maxZ;
		final int volume = (xMax - xMin) * (yMax - yMin) * (zMax - zMin);
		
		// note: soil is only added when it has air above it
		
		final ArrayList<BlockPos> blockPosSoils = new ArrayList<>(volume);
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
		for (int x = xMin; x <= xMax; x++) {
			for (int z = zMin; z <= zMax; z++) {
				// optimized loop to avoid getting twice the blockstate of each block, we're scanning from top to bottom
				mutableBlockPos.setPos(x, yMax + 1, z);
				
				BlockState blockStateAbove = isSafeThread ? worldReader.getBlockState(mutableBlockPos) : Commons.getBlockState_noChunkLoading(worldReader, mutableBlockPos);
				if (blockStateAbove == null) {
					// chunk isn't loaded, abort treatment or it'll trigger a CME
					throw new ExceptionChunkNotLoaded(String.format("Soil calculation aborted %s",
					                                                Commons.format(worldReader, mutableBlockPos) ));
				}
				
				do {
					final boolean isAirAbove = blockStateAbove.getBlock().isAir(blockStateAbove, worldReader, mutableBlockPos);
					mutableBlockPos.setY(mutableBlockPos.getY() - 1);
					final BlockState blockStateCandidate = worldReader.getBlockState(mutableBlockPos);
					if (isAirAbove && Dictionary.isSoil(blockStateCandidate.getBlock())) {
						if (WarpDriveConfig.LOGGING_COLLECTION) {
							WarpDrive.logger.info(String.format("Found soil %s",
							                                    Commons.format(worldReader, mutableBlockPos) ));
						}
						blockPosSoils.add(mutableBlockPos.toImmutable());
						
						mutableBlockPos.setY(mutableBlockPos.getY() - 1);
						blockStateAbove = isSafeThread ? worldReader.getBlockState(mutableBlockPos) : Commons.getBlockState_noChunkLoading(worldReader, mutableBlockPos);
						if (blockStateAbove == null) {
							// chunk isn't loaded, abort treatment or it'll trigger a CME
							throw new ExceptionChunkNotLoaded(String.format("Soil calculation aborted %s",
							                                                Commons.format(worldReader, mutableBlockPos) ));
						}
					} else {
						blockStateAbove = blockStateCandidate;
					}
				} while (mutableBlockPos.getY() > yMin);
			}
		}
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(String.format("Found %d soils",
			                                    blockPosSoils.size() ));
		}
		return blockPosSoils;
	}
	
	private static ArrayList<BlockStatePos> calculate_getValuableStatePositions(@Nonnull final IWorldReader worldReader,
	                                                                            @Nonnull final AxisAlignedBB axisAlignedBB,
	                                                                            final boolean breakLeaves,
	                                                                            final int maxLogDistance,
	                                                                            final Comparator<BlockStatePos> comparator) throws ExceptionChunkNotLoaded {
		final boolean isSafeThread = Commons.isSafeThread();
		final int xMin = (int) axisAlignedBB.minX;
		final int xMax = (int) axisAlignedBB.maxX;
		final int yMin = (int) axisAlignedBB.minY;
		final int yMax = (int) axisAlignedBB.maxY;
		final int zMin = (int) axisAlignedBB.minZ;
		final int zMax = (int) axisAlignedBB.maxZ;
		final int volume = (xMax - xMin) * (yMax - yMin) * (zMax - zMin);
		
		final Collection<BlockPos> logPositions = new HashSet<>(volume);
		final Collection<BlockStatePos> cropBlockStatePositions = new HashSet<>(volume);
		
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
		for (int y = yMin; y <= yMax; y++) {
			for (int x = xMin; x <= xMax; x++) {
				for (int z = zMin; z <= zMax; z++) {
					mutableBlockPos.setPos(x, y, z);
					final BlockState blockState = isSafeThread ? worldReader.getBlockState(mutableBlockPos) : Commons.getBlockState_noChunkLoading(worldReader, mutableBlockPos);
					if (blockState == null) {
						// chunk isn't loaded, abort treatment or it'll trigger a CME
						throw new ExceptionChunkNotLoaded(String.format("Valuable calculation aborted %s",
						                                                Commons.format(worldReader, mutableBlockPos) ));
					}
					if (blockState.getMaterial() == Material.AIR) {
						continue;
					}
					
					final Block block = blockState.getBlock();
					if (Dictionary.isLog(block)) {
						if (!logPositions.contains(mutableBlockPos)) {
							if (WarpDriveConfig.LOGGING_COLLECTION) {
								WarpDrive.logger.info(String.format("Found tree base %s",
								                                    Commons.format(worldReader, mutableBlockPos) ));
							}
							logPositions.add(mutableBlockPos.toImmutable());
						}
					}
					if (block instanceof IGrowable) {
						if (((IGrowable) block).canGrow((World) worldReader, mutableBlockPos, blockState, false)) {
							continue;
						}
						if (WarpDriveConfig.LOGGING_COLLECTION) {
							WarpDrive.logger.info(String.format("Found grown crop %s",
							                                    Commons.format(worldReader, mutableBlockPos) ));
						}
						cropBlockStatePositions.add(new BlockStatePos(mutableBlockPos, blockState));
					}
					// note: mutableBlockPos value may change from here
					if (Dictionary.isStackingPlant(block)) {
						mutableBlockPos.setPos(x, y + 1, z);
						final BlockState blockStateAbove = isSafeThread ? worldReader.getBlockState(mutableBlockPos) : Commons.getBlockState_noChunkLoading(worldReader, mutableBlockPos);
						if (blockState.equals(blockStateAbove)) {
							if (WarpDriveConfig.LOGGING_COLLECTION) {
								WarpDrive.logger.info(String.format("Found stacked reed or cactus %s",
								                                    Commons.format(worldReader, mutableBlockPos) ));
							}
							logPositions.add(mutableBlockPos.toImmutable());
						}
					}
				}
			}
		}
		if ( logPositions.isEmpty()
		  && cropBlockStatePositions.isEmpty() ) {
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info("Found no valuable");
			}
			return new ArrayList<>();
		}
		
		final HashSet<Block> blockResults = breakLeaves ? Dictionary.getLogsLeavesAndStackings() : Dictionary.getLogsAndStackings();
		final Set<BlockStatePos> blockStatePositions = Commons.getConnectedBlockStatePos(worldReader, logPositions, Commons.DIRECTIONS_UP_CONE,
		                                                                                 Dictionary.getLogsLeavesAndStackings(), blockResults, maxLogDistance);
		blockStatePositions.addAll(cropBlockStatePositions);
		
		final ArrayList<BlockStatePos> blockStatePosList = new ArrayList<>(blockStatePositions);
		blockStatePosList.sort(comparator);
		
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(String.format("Found %d valuables",
			                                    blockStatePosList.size()));
		}
		return blockStatePosList;
	}
	
	private int comparatorSortLogsAndLeaves(@Nonnull final BlockStatePos o1, @Nonnull final BlockStatePos o2) {
		// first, we clear central from bottom to top
		if (o1.blockPos.getX() == pos.getX() && o1.blockPos.getZ() == pos.getZ()) {
			if (o2.blockPos.getX() == pos.getX() && o2.blockPos.getZ() == pos.getZ()) {
				return o1.blockPos.getY() - o2.blockPos.getY();
			} else {
				return -1;
			}
		} else if (o2.blockPos.getX() == pos.getX() && o2.blockPos.getZ() == pos.getZ()) {
			return +1;
		}
		
		// second, we clear central from top to bottom
		if (o1.blockPos.getY() != o2.blockPos.getY()) {
			return o2.blockPos.getY() - o1.blockPos.getY();
		}
		
		// third, we remove leaves from center to exterior
		if (Dictionary.isLeaf(o1.blockState.getBlock())) {
			if (Dictionary.isLeaf(o2.blockState.getBlock())) {
				return ( (o1.blockPos.getX() - pos.getX()) * (o1.blockPos.getX() - pos.getX())
				       + (o1.blockPos.getZ() - pos.getZ()) * (o1.blockPos.getZ() - pos.getZ()) )
				     - ( (o2.blockPos.getX() - pos.getX()) * (o2.blockPos.getX() - pos.getX())
				       + (o2.blockPos.getZ() - pos.getZ()) * (o2.blockPos.getZ() - pos.getZ()) );
			} else {
				return -1;
			}
		} else if (Dictionary.isLeaf(o2.blockState.getBlock())) {
			return +1;
		}
		
		// last, from center to exterior
		final int rangeDifference = ( (o1.blockPos.getX() - pos.getX()) * (o1.blockPos.getX() - pos.getX())
		                            + (o1.blockPos.getZ() - pos.getZ()) * (o1.blockPos.getZ() - pos.getZ()) )
		                          - ( (o2.blockPos.getX() - pos.getX()) * (o2.blockPos.getX() - pos.getX())
		                            + (o2.blockPos.getZ() - pos.getZ()) * (o2.blockPos.getZ() - pos.getZ()) );
		if (rangeDifference == 0) {
			if (o1.blockPos.getX() != o2.blockPos.getX()) {
				return o1.blockPos.getX() - o2.blockPos.getX();
			} else {
				return o1.blockPos.getZ() - o2.blockPos.getZ();
			}
		}
		return rangeDifference;
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		tagCompound.putInt("radiusX", radiusX_requested);
		tagCompound.putInt("radiusZ", radiusZ_requested);
		tagCompound.putBoolean("breakLeaves", breakLeaves);
		tagCompound.putBoolean("tapTrees", tapTrees);
		tagCompound.putInt("currentState", currentState);
		return tagCompound;
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		radiusX_requested = tagCompound.getInt("radiusX");
		radiusX_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, radiusX_requested);
		radiusZ_requested = tagCompound.getInt("radiusZ");
		radiusZ_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, radiusZ_requested);
		
		breakLeaves     = tagCompound.getBoolean("breakLeaves");
		tapTrees        = tagCompound.getBoolean("tapTrees");
		currentState    = tagCompound.getInt("currentState");
	}
	
	// Returns scan, wet tap, jungle tap, harvest log, harvest leaf, plant energy costs
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] { true,
		                      EnergyWrapper.convert(energyScanning, units),
		                      EnergyWrapper.convert(energyTappingWetSpot, units),
		                      EnergyWrapper.convert(energyTappingRubberLog, units),
		                      EnergyWrapper.convert(energyHarvestingLog, units),
		                      EnergyWrapper.convert(energyHarvestingLeaf, units),
		                      EnergyWrapper.convert(energyPlanting, units) };
	}
	
	// Common OC/CC methods
	private Object[] state() {
		final int energy = laserMedium_getEnergyStored(true);
		final String status = getStatusHeaderInPureText();
		final int return_indexValuable, return_countValuables;
		if (currentState != STATE_IDLE) {
			return_indexValuable = indexValuable;
			return_countValuables = blockPosValuables.size();
		} else {
			return_indexValuable = 0;
			return_countValuables = 0;
		}
		return new Object[] { status, currentState != STATE_IDLE, energy, totalHarvested, return_indexValuable, return_countValuables };
	}
	
	private Object[] radius(@Nonnull final Object[] arguments) {
		try {
			if (arguments.length == 1 && arguments[0] != null) {
				radiusX_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, Commons.toInt(arguments[0]));
				radiusZ_requested = radiusX_requested;
				markDirty();
			} else if (arguments.length == 2) {
				radiusX_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, Commons.toInt(arguments[0]));
				radiusZ_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, Commons.toInt(arguments[1]));
				markDirty();
			}
		} catch(final NumberFormatException exception) {
			radiusX_requested = WarpDriveConfig.TREE_FARM_totalMaxRadius;
			radiusZ_requested = WarpDriveConfig.TREE_FARM_totalMaxRadius;
		}
		return new Integer[] { radiusX_requested, radiusZ_requested };
	}
	
	private Object[] breakLeaves(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				breakLeaves = Commons.toBool(arguments[0]);
				markDirty();
			} catch (final Exception exception) {
				return new Object[] { breakLeaves };
			}
		}
		return new Object[] { breakLeaves };
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
	
	private Object[] tapTrees(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				tapTrees = Commons.toBool(arguments[0]);
				markDirty();
			} catch (final Exception exception) {
				return new Object[] { tapTrees };
			}
		}
		return new Object[] { tapTrees };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	@Callback(direct = true)
	public Object[] radius(final Context context, final Arguments arguments) {
		return radius(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] breakLeaves(final Context context, final Arguments arguments) {
		return breakLeaves(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] silktouch(final Context context, final Arguments arguments) {
		return silktouch(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] tapTrees(final Context context, final Arguments arguments) {
		return tapTrees(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "state":
			return state();
			
		case "radius":
			return radius(arguments);
			
		case "breakLeaves":
			return breakLeaves(arguments);
			
		case "silktouch":
			return silktouch(arguments);
			
		case "tapTrees":
			return tapTrees(arguments);
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
	
	@Override
	public WarpDriveText getStatusHeader() {
		final int energy = laserMedium_getEnergyStored(true);
		WarpDriveText textState = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.error.internal_check_console");
		if (currentState == STATE_IDLE) {
			textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.idle");
		} else if (currentState == STATE_WARMING_UP) {
			textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.warming_up");
		} else if (currentState == STATE_SCANNING) {
			if (breakLeaves) {
				textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.scanning_all");
			} else {
				textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.scanning_logs");
			}
		} else if (currentState == STATE_HARVESTING) {
			if (!tapTrees) {
				if (!enableSilktouch) {
					if (breakLeaves) {
						textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.harvesting_all");
					} else {
						textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.harvesting_logs");
					}
				} else {
					if (breakLeaves) {
						textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.harvesting_all_with_silktouch");
					} else {
						textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.harvesting_logs_with_silktouch");
					}
				}
			} else {
				if (!enableSilktouch) {
					if (breakLeaves) {
						textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.tapping_all");
					} else {
						textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.tapping_logs");
					}
				} else {
					if (breakLeaves) {
						textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.tapping_all_with_silktouch");
					} else {
						textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.tapping_logs_with_silktouch");
					}
				}
			}
		} else if (currentState == STATE_PLANTING) {
			textState = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.laser_tree_farm.status_line.planting");
		}
		if (energy <= 0) {
			textState.appendSibling(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.mining_laser.status_line._insufficient_energy"));
		} else if ( (currentState != STATE_IDLE)
		         && (currentState != STATE_WARMING_UP)
		         && !isPowered ) {
			textState.appendSibling(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.mining_laser.status_line._insufficient_energy"));
		}
		return textState;
	}
	
	private static class ThreadCalculation extends Thread {
		
		private final WeakReference<TileEntity> weakTileEntity;
		private final String stringTileEntity;
		
		ThreadCalculation(final TileEntity tileEntity) {
			this.weakTileEntity = new WeakReference<>(tileEntity);
			stringTileEntity = tileEntity.toString();
		}
		
		@Override
		public void run() {
			ArrayList<BlockPos> blockPosSoils = null;
			ArrayList<BlockStatePos> blockStatePosValuables = null;
			
			// calculation start is done synchronously, by caller
			try {
				TileEntity tileEntity = weakTileEntity.get();
				if (!(tileEntity instanceof TileEntityLaserTreeFarm)) {
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.error(String.format("%s Scanning aborted",
						                                     this ));
					}
					
				} else {
					// collect what we need, then release the object
					final IWorldReader worldReader = tileEntity.getWorld();
					assert worldReader != null;
					final AxisAlignedBB axisAlignedBBSoil = ((TileEntityLaserTreeFarm) tileEntity).axisAlignedBBSoil;
					final AxisAlignedBB axisAlignedBBScan = ((TileEntityLaserTreeFarm) tileEntity).axisAlignedBBScan;
					final boolean breakLeaves = ((TileEntityLaserTreeFarm) tileEntity).breakLeaves;
					final int maxDistance = ((TileEntityLaserTreeFarm) tileEntity).maxDistance;
					final Comparator<BlockStatePos> comparator = ((TileEntityLaserTreeFarm) tileEntity)::comparatorSortLogsAndLeaves;
					//noinspection UnusedAssignment
					tileEntity = null;
					
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.debug(String.format("%s Calculation started for %s",
						                                     this, stringTileEntity ));
					}
					
					blockPosSoils = calculate_getSoilPositions(worldReader, axisAlignedBBSoil);
					blockStatePosValuables = calculate_getValuableStatePositions(worldReader, axisAlignedBBScan,
					                                                             breakLeaves, maxDistance, comparator);
					
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.debug(String.format("%s Calculation done: %s soil positions, %s valuables positions",
						                                     this, blockPosSoils.size(), blockStatePosValuables.size() ));
					}
				}
			} catch (final ExceptionChunkNotLoaded exception) {
				blockPosSoils = null;
				blockStatePosValuables = null;
				WarpDrive.logger.warn(String.format("%s Calculation aborted to prevent chunk loading for %s",
				                                    this, stringTileEntity ));
			} catch (final Exception exception) {
				blockPosSoils = null;
				blockStatePosValuables = null;
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("%s Calculation failed for %s",
				                                     this, stringTileEntity ));
			}
			
			final TileEntity tileEntity = weakTileEntity.get();
			if (tileEntity instanceof TileEntityLaserTreeFarm) {
				((TileEntityLaserTreeFarm) tileEntity).calculation_done(blockPosSoils, blockStatePosValuables);
			}
		}
	}
}

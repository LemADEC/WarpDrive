package cr0s.warpdrive.block.building;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.ISequencerCallbacks;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractMachine;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnumShipScannerState;
import cr0s.warpdrive.data.JumpBlock;
import cr0s.warpdrive.data.JumpShip;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Transformation;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.event.DeploySequencer;
import cr0s.warpdrive.item.ItemShipToken;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;

import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

public class TileEntityShipScanner extends TileEntityAbstractMachine implements ISequencerCallbacks {
	
	// persistent properties
	private String schematicFileName = "";
	private int targetX, targetY, targetZ;
	private byte rotationSteps;
	public BlockState blockStateCamouflage;
	protected int colorMultiplierCamouflage;
	protected int lightCamouflage;
	
	// computed properties
	private AxisAlignedBB aabbRender = null;
	private boolean isShipToken;
	private EnumShipScannerState enumShipScannerState = EnumShipScannerState.IDLE;
	private TileEntityShipCore shipCore = null;
	
	private int laserTicks = 0;
	private int scanTicks = 0;
	private int deployTicks = 0;
	
	private String playerName = "";
	
	private JumpShip jumpShip;
	private int blocksToDeployCount;
	
	public TileEntityShipScanner(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		peripheralName = "warpdriveShipScanner";
		addMethods(new String[] {
				"scan",
				"fileName",
				"deploy",
				"state"
		});
	}
	
	@Nonnull
	@Override
	@OnlyIn(Dist.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		if (aabbRender == null) {
			aabbRender = new AxisAlignedBB(
					pos.getX() - 1.0D, pos.getY()       , pos.getZ() - 1.0D,
					pos.getX() + 2.0D, pos.getY() + 2.0D, pos.getZ() + 2.0D);
		}
		return aabbRender;
	}
	
	@Override
	public void tick() {
		super.tick();
		assert world != null;
		
		if (world.isRemote()) {
			return;
		}
		
		final BlockState blockState = world.getBlockState(pos);
		updateBlockState(blockState, BlockProperties.ACTIVE, enumShipScannerState != EnumShipScannerState.IDLE);
		
		// Trigger deployment by player, provided setup is done
		if (!isEnabled) {
			enumShipScannerState = EnumShipScannerState.IDLE; // disable scanner
			return;
		}
		
		final boolean isSetupDone = targetX != 0 || targetY != 0 || targetZ != 0;
		if (isSetupDone) {
			if (enumShipScannerState == EnumShipScannerState.IDLE) {
				checkPlayerForShipToken();
			}
			if (enumShipScannerState != EnumShipScannerState.DEPLOYING) {
				enumShipScannerState = EnumShipScannerState.IDLE; // disable scanner
				return;
			}
			
		} else if (enumShipScannerState != EnumShipScannerState.DEPLOYING && shipCore == null) {// Ship core is not found
			laserTicks++;
			if (laserTicks > 20) {
				PacketHandler.sendBeamPacket(world,
				                             new Vector3(this).translate(0.5D),
				                             new Vector3(pos.getX(), pos.getY() + 5, pos.getZ()).translate(0.5D), 
				                             1.0F, 0.2F, 0.0F, 40, 0, 100);
				laserTicks = 0;
			}
			return;
		}
		
		switch (enumShipScannerState) {
		case IDLE:// inactive
			// assert shipCore != null;
			laserTicks++;
			if (laserTicks > 20) {
				PacketHandler.sendBeamPacket(world,
				                             new Vector3(this).translate(0.5D),
				                             new Vector3(shipCore).translate(0.5D),
				                             0.0F, 1.0F, 0.2F, 40, 0, 100);
				laserTicks = 0;
			}
			break;
			
		case SCANNING:// active and scanning
			laserTicks++;
			if (laserTicks > 5) {
				laserTicks = 0;
				
				for (int index = 0; index < 10; index++) {
					final int randomX = shipCore.minX + world.rand.nextInt(shipCore.maxX - shipCore.minX + 1);
					final int randomY = shipCore.minY + world.rand.nextInt(shipCore.maxY - shipCore.minY + 1);
					final int randomZ = shipCore.minZ + world.rand.nextInt(shipCore.maxZ - shipCore.minZ + 1);
					
					world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 1.0F, 1F);
					final float r = world.rand.nextFloat() - world.rand.nextFloat();
					final float g = world.rand.nextFloat() - world.rand.nextFloat();
					final float b = world.rand.nextFloat() - world.rand.nextFloat();
					
					PacketHandler.sendBeamPacket(world,
							new Vector3(this).translate(0.5D),
							new Vector3(randomX, randomY, randomZ).translate(0.5D),
							r, g, b, 15, 0, 100);
				}
			}
			
			scanTicks++;
			if (scanTicks > 20 * (1 + shipCore.shipMass / WarpDriveConfig.SS_SCAN_BLOCKS_PER_SECOND)) {
				enumShipScannerState = EnumShipScannerState.IDLE; // disable scanner
			}
			break;
			
		case DEPLOYING:// active and deploying
			if (deployTicks == 0) {
				final DeploySequencer sequencer = new DeploySequencer(jumpShip, getWorld(), isShipToken, targetX, targetY, targetZ, rotationSteps);
				
				// deploy at most (jump speed / 4), at least (deploy speed), optimally in 10 seconds 
				final int optimumSpeed = Math.round(blocksToDeployCount * WarpDriveConfig.SS_DEPLOY_INTERVAL_TICKS / (20.0F * 10.0F));
				final int blockToDeployPerTick = Math.max(WarpDriveConfig.SS_DEPLOY_BLOCKS_PER_INTERVAL,
				                                          Math.min(WarpDriveConfig.G_BLOCKS_PER_TICK / 4, optimumSpeed));
				if (WarpDrive.isDev && WarpDriveConfig.LOGGING_BUILDING) {
					WarpDrive.logger.info(String.format("optimumSpeed %d blockToDeployPerTick %d",
					                                    optimumSpeed, blockToDeployPerTick));
				}
				sequencer.setBlocksPerTick(blockToDeployPerTick);
				sequencer.setRequester(playerName, isShipToken);
				sequencer.setEffectSource(new Vector3(this).translate(0.5D));
				sequencer.setCallback(this);
				sequencer.enable();
			}
			
			deployTicks++;
			if (deployTicks > 20.0F * 60.0F) {
				// timeout in sequencer?
				WarpDrive.logger.info(this + " Deployment timeout?");
				deployTicks = 0;
				enumShipScannerState = EnumShipScannerState.IDLE; // disable scanner
				shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_PERIOD_TICKS * 3;
			}
			break;
			
		default:
			WarpDrive.logger.error("Invalid ship scanner state, forcing to IDLE...");
			enumShipScannerState = EnumShipScannerState.IDLE;
			break;
		}
	}
	
	@Override
	public void sequencer_finished() {
		switch (enumShipScannerState) {
//		case IDLE:// inactive
//			break;
		
//		case SCANNING:// active and scanning
//			break;
		
		case DEPLOYING:// active and deploying
			enumShipScannerState = EnumShipScannerState.IDLE; // disable scanner
			if (WarpDriveConfig.LOGGING_BUILDING) {
				WarpDrive.logger.info(this + " Deployment done");
			}
			shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_PERIOD_TICKS * 3;
			break;
		
		default:
			WarpDrive.logger.error(String.format("%s Invalid ship scanner state, forcing to IDLE...",
			                                     this));
			enumShipScannerState = EnumShipScannerState.IDLE;
			break;
		}
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		final boolean isValid = super.doScanAssembly(isDirty, textReason);
		assert world != null;
		
		BlockState blockStateShipCoreTooHigh = null;
		TileEntityShipCore tileEntityShipCore = null;
		
		// Search for ship cores above
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(pos);
		for (int newY = pos.getY() + 1; newY <= 255; newY++) {
			mutableBlockPos.setY(newY);
			final BlockState blockState = world.getBlockState(mutableBlockPos);
			if (blockState.getBlock() instanceof BlockShipCore) {
				// validate the ship assembly
				final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
				if ( !(tileEntity instanceof TileEntityShipCore)
				  || tileEntity.isRemoved()
				  || !((TileEntityShipCore) tileEntity).isAssemblyValid()) {
					continue;
				}
				
				// validate tier
				if (((BlockShipCore) blockState.getBlock()).getTier().getIndex() > enumTier.getIndex()) {
					blockStateShipCoreTooHigh = blockState;
					continue;
				}
				
				tileEntityShipCore = (TileEntityShipCore) tileEntity;
				break;
			}
		}
		
		// compute result
		shipCore = tileEntityShipCore;
		if (shipCore == null) {
			if (blockStateShipCoreTooHigh == null) {
				textReason.append(Commons.getStyleWarning(), "warpdrive.builder.status_line.no_ship_core_in_range");
			} else {
				textReason.append(Commons.getStyleWarning(), "warpdrive.builder.status_line.ship_is_higher_tier",
				                  blockStateShipCoreTooHigh.getBlock().getNameTextComponent(),
				                  getBlockState().getBlock().getNameTextComponent() );
			}
		}
		return isValid && shipCore != null; 
	}
	
	private boolean saveShipToSchematic(final String fileName, final WarpDriveText reason) {
		assert world != null;
		if (!shipCore.isAssemblyValid()) {
			return false;
		}
		final short width = (short) (shipCore.maxX - shipCore.minX + 1);
		final short length = (short) (shipCore.maxZ - shipCore.minZ + 1);
		final short height = (short) (shipCore.maxY - shipCore.minY + 1);
		final int size = width * length * height;
		
		if (width <= 0 || length <= 0 || height <= 0) {
			reason.append(Commons.getStyleWarning(), "warpdrive.scanner.guide.invalid_ship_dimensions");
			return false;
		}
		
		// Save header
		final CompoundNBT schematic = new CompoundNBT();
		
		schematic.putShort("Width", width);
		schematic.putShort("Length", length);
		schematic.putShort("Height", height);
		schematic.putInt("shipMass", shipCore.shipMass);
		schematic.putString("shipName", shipCore.name);
		schematic.putInt("shipVolume", shipCore.shipVolume);
		
		// Save new format
		final JumpShip ship = new JumpShip();
		ship.world = shipCore.getWorld();
		ship.core = shipCore.getPos();
		ship.dx = shipCore.facing.getXOffset();
		ship.dz = shipCore.facing.getZOffset();
		ship.minX = shipCore.minX;
		ship.maxX = shipCore.maxX;
		ship.minY = shipCore.minY;
		ship.maxY = shipCore.maxY;
		ship.minZ = shipCore.minZ;
		ship.maxZ = shipCore.maxZ;
		ship.shipCore = shipCore;
		if (!ship.save(reason)) {
			return false;
		}
		final CompoundNBT tagCompoundShip = new CompoundNBT();
		ship.write(tagCompoundShip);
		schematic.put("ship", tagCompoundShip);
		
		// Storage collections
		final INBT[] nbtBlockStates = new INBT[size];
		final ListNBT tileEntitiesList = new ListNBT();
		
		// Scan the whole area
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				for (int z = 0; z < length; z++) {
					final BlockPos blockPos = new BlockPos(shipCore.minX + x, shipCore.minY + y, shipCore.minZ + z);
					BlockState blockState = world.getBlockState(blockPos);
					
					// Skip leftBehind and anchor blocks
					if ( Dictionary.BLOCKS_LEFTBEHIND.contains(blockState.getBlock())
					  || Dictionary.BLOCKS_ANCHOR.contains(blockState.getBlock()) ) {
						blockState = Blocks.AIR.getDefaultState();
					}
					
					final int index = x + (y * length + z) * width;
					nbtBlockStates[index] = Commons.writeBlockStateToNBT(blockState);
					
					if (blockState.getBlock() != Blocks.AIR) {
						final TileEntity tileEntity = world.getTileEntity(blockPos);
						if (tileEntity != null) {
							try {
								final CompoundNBT tagTileEntity = new CompoundNBT();
								tileEntity.write(tagTileEntity);
								
								JumpBlock.removeUniqueIDs(tagTileEntity);
								
								// Transform TE's coordinates from local axis to .schematic offset-axis
								// Warning: this is a cheap workaround for World Edit. Use the native format for proper transformation
								tagTileEntity.putInt("x", tileEntity.getPos().getX() - shipCore.minX);
								tagTileEntity.putInt("y", tileEntity.getPos().getY() - shipCore.minY);
								tagTileEntity.putInt("z", tileEntity.getPos().getZ() - shipCore.minZ);
								
								tileEntitiesList.add(tagTileEntity);
							} catch (final Exception exception) {
								exception.printStackTrace(WarpDrive.printStreamError);
							}
						}
					}
				}
			}
		}
		
		schematic.putString("Materials", "Alpha");
		final ListNBT tagListBlocks = new ListNBT();
		tagListBlocks.addAll(Arrays.asList(nbtBlockStates));
		schematic.put("BlockStates", tagListBlocks);
		
		schematic.put("Entities", new ListNBT()); // don't save entities
		schematic.put("TileEntities", tileEntitiesList);
		
		Commons.writeNBTToFile(fileName, schematic);
		
		return true;
	}
	
	// Begins ship scan
	private boolean scanShip(final WarpDriveText reason) {
		// Enable scanner
		enumShipScannerState = EnumShipScannerState.SCANNING;
		final File file = new File(WarpDriveConfig.G_SCHEMATICS_LOCATION);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs()) {
				return false;
			}
		}
		
		// Generate unique file name
		final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss's'SSS");
		final String shipName = Commons.sanitizeFileName(shipCore.name.replaceAll("[^-~]", "")
		                                                              .replaceAll(" ", "_"));
		do {
			final Date now = new Date();
			schematicFileName = shipName + "_" + sdfDate.format(now);
		} while (new File(WarpDriveConfig.G_SCHEMATICS_LOCATION + "/" + schematicFileName + ".schematic").exists());
		
		if (!saveShipToSchematic(WarpDriveConfig.G_SCHEMATICS_LOCATION + "/" + schematicFileName + ".schematic", reason)) {
			return false;
		}
		reason.appendSibling(new StringTextComponent(schematicFileName));
		return true;
	}
	
	// Returns true on success and reason string
	private boolean deployShip(final String fileName, final int offsetX, final int offsetY, final int offsetZ,
	                           final byte rotationSteps, final boolean isForced, final WarpDriveText reason) {
		assert world != null;
		targetX = pos.getX() + offsetX;
		targetY = pos.getY() + offsetY;
		targetZ = pos.getZ() + offsetZ;
		this.rotationSteps = rotationSteps;
		
		jumpShip = JumpShip.createFromFile(fileName, reason);
		if (jumpShip == null) {
			return false;
		}
		
		blocksToDeployCount = jumpShip.jumpBlocks.length;
		if (WarpDriveConfig.LOGGING_BUILDING) {
			WarpDrive.logger.info(String.format("%s Loaded %d blocks to deploy",
			                                    this, blocksToDeployCount));
		}
		
		// Validate context
		{
			// Check tier constrains
			if ( jumpShip.maxX - jumpShip.minX + 1 > WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()]
			  || jumpShip.maxY - jumpShip.minY + 1 > WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()]
			  || jumpShip.maxZ - jumpShip.minZ + 1 > WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()] ) {
				reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.ship_is_higher_tier");
				return false;
			}
			if ( jumpShip.actualMass > WarpDriveConfig.SHIP_MASS_MAX_ON_PLANET_SURFACE
			  && CelestialObjectManager.isPlanet(world, pos.getX(), pos.getZ()) ) {
				reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.too_much_mass_for_planet",
				                                       WarpDriveConfig.SHIP_MASS_MAX_ON_PLANET_SURFACE, jumpShip.actualMass );
				return false;
			}
			
			// note: we don't check for minimum mass so we can build a shuttle with a corvette builder
			
			if (jumpShip.actualMass > WarpDriveConfig.SHIP_MASS_MAX_BY_TIER[enumTier.getIndex()]) {
				reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.ship_is_higher_tier");
				return false;
			}
			
			// Check distance
			final double dX = pos.getX() - targetX;
			final double dY = pos.getY() - targetY;
			final double dZ = pos.getZ() - targetZ;
			final double distance = MathHelper.sqrt(dX * dX + dY * dY + dZ * dZ);
			
			if (distance > WarpDriveConfig.SS_MAX_DEPLOY_RADIUS_BLOCKS) {
				reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.deploying_out_of_range",
				              WarpDriveConfig.SS_MAX_DEPLOY_RADIUS_BLOCKS);
				return false;
			}
			
			// Compute target area
			final Transformation transformation = new Transformation(jumpShip, world,
			                                                         targetX - jumpShip.core.getX(),
			                                                         targetY - jumpShip.core.getY(),
			                                                         targetZ - jumpShip.core.getZ(),
			                                                         rotationSteps);
			final BlockPos targetLocation1 = transformation.apply(jumpShip.minX, jumpShip.minY, jumpShip.minZ);
			final BlockPos targetLocation2 = transformation.apply(jumpShip.maxX, jumpShip.maxY, jumpShip.maxZ);
			final BlockPos targetLocationMin = new BlockPos(
			                Math.min(targetLocation1.getX(), targetLocation2.getX()) - 1,
			    Math.max(0, Math.min(targetLocation1.getY(), targetLocation2.getY()) - 1),
			                Math.min(targetLocation1.getZ(), targetLocation2.getZ()) - 1);
			final BlockPos targetLocationMax = new BlockPos(
			                  Math.max(targetLocation1.getX(), targetLocation2.getX()) + 1,
			    Math.min(255, Math.max(targetLocation1.getY(), targetLocation2.getY()) + 1),
			                  Math.max(targetLocation1.getZ(), targetLocation2.getZ()) + 1);
			
			if (isForced) {
				if (!isShipCoreClear(world, new BlockPos(targetX, targetY, targetZ), playerName, reason)) {
					if (WarpDriveConfig.LOGGING_BUILDING) {
						WarpDrive.logger.info(String.format("Deployment collision detected at (%d %d %d): no room for Ship core",
						                                    targetX, targetY, targetZ));
					}
					return false;
				}
				
				// Clear specified area for any blocks to avoid corruption and ensure clean full ship
				for (int x = targetLocationMin.getX(); x <= targetLocationMax.getX(); x++) {
					for (int y = targetLocationMin.getY(); y <= targetLocationMax.getY(); y++) {
						for (int z = targetLocationMin.getZ(); z <= targetLocationMax.getZ(); z++) {
							world.removeBlock(new BlockPos(x, y, z), false);
						}
					}
				}
				
			} else {
				
				// Check specified area for occupation by blocks
				// If specified area is occupied, break deployment with error message
				int occupiedBlockCount = 0;
				final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
				for (int x = targetLocationMin.getX(); x <= targetLocationMax.getX(); x++) {
					for (int y = targetLocationMin.getY(); y <= targetLocationMax.getY(); y++) {
						for (int z = targetLocationMin.getZ(); z <= targetLocationMax.getZ(); z++) {
							mutableBlockPos.setPos(x, y, z);
							if (!world.isAirBlock(mutableBlockPos)) {
								occupiedBlockCount++;
								if (occupiedBlockCount == 1 || (occupiedBlockCount <= 100 && world.rand.nextInt(10) == 0)) {
									PacketHandler.sendSpawnParticlePacket(world, "explosionLarge", (byte) 5,
									                                      new Vector3(mutableBlockPos), new Vector3(0, 0, 0),
									                                      0.70F + 0.25F * world.rand.nextFloat(), 0.70F + 0.25F * world.rand.nextFloat(), 0.20F + 0.30F * world.rand.nextFloat(),
									                                      0.10F + 0.10F * world.rand.nextFloat(), 0.10F + 0.20F * world.rand.nextFloat(), 0.10F + 0.30F * world.rand.nextFloat(),
									                                      WarpDriveConfig.SS_MAX_DEPLOY_RADIUS_BLOCKS);
								}
								if (WarpDriveConfig.LOGGING_BUILDING) {
									WarpDrive.logger.info(String.format("Deployment collision detected %s",
									                                    Commons.format(world, x, y, z)));
								}
							}
						}
					}
				}
				if (occupiedBlockCount > 0) {
					reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.deployment_area_occupied_by_blocks",
					              occupiedBlockCount);
					return false;
				}
			}
		}
		
		// initiate deployment sequencer
		deployTicks = 0;
		
		isShipToken = isForced;
		enumShipScannerState = EnumShipScannerState.DEPLOYING;
		reason.append(Commons.getStyleCorrect(), "warpdrive.builder.guide.deploying_ship",
		              fileName);
		return true;
	}
	
	private static boolean isShipCoreClear(@Nonnull final World world, final BlockPos blockPos,
	                                       final String nameRequestingPlayer, final WarpDriveText reason) {
		final BlockState blockState = world.getBlockState(blockPos);
		if (blockState.getBlock().isAir(blockState, world, blockPos)) {
			return true;
		}
		
		if (!(blockState.getBlock() instanceof BlockShipCore)) {
			PacketHandler.sendSpawnParticlePacket(world, "explosionLarge", (byte) 5,
			                                      new Vector3(blockPos), new Vector3(0, 0, 0),
			                                      0.70F + 0.25F * world.rand.nextFloat(), 0.70F + 0.25F * world.rand.nextFloat(), 0.20F + 0.30F * world.rand.nextFloat(),
			                                      0.10F + 0.10F * world.rand.nextFloat(), 0.10F + 0.20F * world.rand.nextFloat(), 0.10F + 0.30F * world.rand.nextFloat(),
			                                      WarpDriveConfig.SS_MAX_DEPLOY_RADIUS_BLOCKS);
			reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.deployment_area_occupied_by_block",
			              blockState.getBlock().getNameTextComponent(),
			              blockPos.getX(), blockPos.getY(), blockPos.getZ());
			return false;
		}
		
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityShipCore)) {
			reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.deployment_area_corrupted_tile_entity",
			              tileEntity);
			reason.append(Commons.getStyleCommand(), "warpdrive.builder.guide.contact_an_admin",
			              Commons.format(world, blockPos));
			WarpDrive.logger.error(reason.toString());
			PacketHandler.sendSpawnParticlePacket(world, "explosionLarge", (byte) 5,
			                                      new Vector3(blockPos), new Vector3(0, 0, 0),
			                                      0.70F + 0.25F * world.rand.nextFloat(), 0.70F + 0.25F * world.rand.nextFloat(), 0.20F + 0.30F * world.rand.nextFloat(),
			                                      0.10F + 0.10F * world.rand.nextFloat(), 0.10F + 0.20F * world.rand.nextFloat(), 0.10F + 0.30F * world.rand.nextFloat(),
			                                      WarpDriveConfig.SS_MAX_DEPLOY_RADIUS_BLOCKS);
			return false;
		}
		
		final TileEntityShipCore tileEntityShipCore = (TileEntityShipCore) tileEntity;
		final String namePlayersAboard = tileEntityShipCore.getAllPlayersInArea();
		if (!namePlayersAboard.isEmpty()) {
			reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.deployment_area_with_active_crew",
			              namePlayersAboard);
			reason.append(Commons.getStyleCommand(), "warpdrive.builder.guide.wait_your_turn");
			return false;
		}
		
		if (tileEntityShipCore.isBusy()) {
			reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.deployment_area_is_busy");
			reason.append(Commons.getStyleCommand(), "warpdrive.builder.guide.wait_your_turn");
			return false;
		}
		
		final String nameOnlineCrew = tileEntityShipCore.getFirstOnlineCrew();
		if (nameOnlineCrew == null || nameOnlineCrew.isEmpty()) {
			return true;
		}
		
		if (nameOnlineCrew.equals(nameRequestingPlayer)) {
			reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.deployment_area_occupied_by_your_ship1",
			              nameOnlineCrew);
			reason.append(Commons.getStyleCommand(), "warpdrive.builder.guide.deployment_area_occupied_by_your_ship2");
			return false;
		}
		
		reason.append(Commons.getStyleWarning(), "warpdrive.builder.guide.deployment_area_occupied_by_online_player1",
		              nameOnlineCrew);
		reason.append(Commons.getStyleCommand(), "warpdrive.builder.guide.deployment_area_occupied_by_online_player2");
		return false;
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		schematicFileName = tagCompound.getString("schematic");
		targetX = tagCompound.getInt("targetX");
		targetY = tagCompound.getInt("targetY");
		targetZ = tagCompound.getInt("targetZ");
		rotationSteps = tagCompound.getByte("rotationSteps");
		if (tagCompound.contains("camouflage")) {
			final CompoundNBT nbtCamouflage = tagCompound.getCompound("camouflage");
			try {
				blockStateCamouflage = Commons.readBlockStateFromNBT(nbtCamouflage.get("state"));
				colorMultiplierCamouflage = nbtCamouflage.getInt("color");
				lightCamouflage = nbtCamouflage.getByte("light");
				if (Dictionary.BLOCKS_NOCAMOUFLAGE.contains(blockStateCamouflage.getBlock())) {
					blockStateCamouflage = null;
					colorMultiplierCamouflage = 0;
					lightCamouflage = 0;
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				blockStateCamouflage = null;
				colorMultiplierCamouflage = 0;
				lightCamouflage = 0;
			}
		} else {
			blockStateCamouflage = null;
			colorMultiplierCamouflage = 0;
			lightCamouflage = 0;
		}
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		tagCompound.putString("schematic", schematicFileName);
		tagCompound.putInt("targetX", targetX);
		tagCompound.putInt("targetY", targetY);
		tagCompound.putInt("targetZ", targetZ);
		tagCompound.putByte("rotationSteps", rotationSteps);
		if (blockStateCamouflage != null && blockStateCamouflage.getBlock() != Blocks.AIR) {
			final CompoundNBT nbtCamouflage = new CompoundNBT();
			assert blockStateCamouflage.getBlock().getRegistryName() != null;
			nbtCamouflage.put("state", Commons.writeBlockStateToNBT(blockStateCamouflage));
			nbtCamouflage.putInt("color", colorMultiplierCamouflage);
			nbtCamouflage.putByte("light", (byte) lightCamouflage);
			tagCompound.put("camouflage", nbtCamouflage);
		}
		return tagCompound;
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] scan(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return scan();
	}
	
	@Callback(direct = true)
	public Object[] filename(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return filename();
	}
	
	@Callback(direct = true)
	public Object[] deploy(final Context context, final Arguments arguments) {
		return deploy(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	@Nonnull
	private Object[] scan() {
		// Already scanning?
		if (enumShipScannerState != EnumShipScannerState.IDLE) {
			return new Object[] { false, "Already active" };
		}
		
		if (shipCore == null) {
			return new Object[] { false, "No ship core in range" };
		}
		final WarpDriveText reason = new WarpDriveText();
		final boolean success = scanShip(reason);
		return new Object[] { success, 3, Commons.removeFormatting( reason.getString() ) };
	}
	
	@Nonnull
	private Object[] filename() {
		if (enumShipScannerState != EnumShipScannerState.IDLE && !schematicFileName.isEmpty()) {
			if (enumShipScannerState == EnumShipScannerState.DEPLOYING) {
				return new Object[] { false, "Deployment in progress. Please wait..." };
			} else {
				return new Object[] { false, "Scan in progress. Please wait..." };
			}
		}
		
		return new Object[] { true, schematicFileName };
	}
	
	@Nonnull
	private Object[] deploy(@Nonnull final Object[] arguments) {
		assert world != null;
		if (arguments.length != 5) {
			return new Object[] { false, "Invalid arguments count, you need <.schematic file name>, <offsetX>, <offsetY>, <offsetZ>, <rotationSteps>!" };
		}
		final String fileName;
		final int x;
		final int y;
		final int z;
		final byte rotationSteps;
		
		try {
			fileName = Commons.toString(arguments[0]);
			x = Commons.toInt(arguments[1]);
			y = Commons.toInt(arguments[2]);
			z = Commons.toInt(arguments[3]);
			final int intRotationSteps = Commons.toInt(arguments[4]);
			rotationSteps = (byte) ((1024 + intRotationSteps) % 4);
		} catch (final Exception exception) {
			if (WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.info(String.format("%s Invalid arguments to deploy(): %s",
				                                    this, Commons.format(arguments)));
			}
			return new Object[] { false, "Invalid argument format, you need <.schematic file name>, <offsetX>, <offsetY>, <offsetZ>, <rotationSteps>!" };
		}
		
		if (enumShipScannerState != EnumShipScannerState.IDLE) {
			return new Object[] { false, String.format("Invalid state, expecting IDLE, found %s", enumShipScannerState.toString()) };
		}
		
		final PlayerEntity entityPlayer = world.getClosestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 8.0D, false);
		if (entityPlayer == null) {
			return new Object[] { false, "Invalid context, no player in range" };
		}
		if (!entityPlayer.abilities.isCreativeMode) {
			return new Object[] { false, "Only a creative player can use this command" };
		}
		
		final WarpDriveText reason = new WarpDriveText();
		final boolean isSuccess = deployShip(fileName, x, y, z, rotationSteps, false, reason);
		
		// update player name since we're deploying from LUA
		playerName = entityPlayer.getName().getString();
		
		return new Object[] { isSuccess, Commons.removeFormatting( reason.getString() ) };
	}
	
	@Nonnull
	private Object[] state() {
		switch (enumShipScannerState) {
		default:
		case IDLE:
			return new Object[] { false, "IDLE", 0, 0 };
		case SCANNING:
			return new Object[] { true, "Scanning", 0, 0 };
		case DEPLOYING:
			return new Object[] { true, "Deploying", 0, blocksToDeployCount };
		}
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "scan":
			return scan();
			
		case "fileName":
			return filename();
			
		case "deploy": // deploy(schematicFileName, offsetX, offsetY, offsetZ)
			return deploy(arguments);
			
		case "state":
			return state();
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
	
	private static final int SHIP_TOKEN_UPDATE_PERIOD_TICKS = 20;
	private static final int SHIP_TOKEN_UPDATE_DELAY_FAILED_PRECONDITION_TICKS = 3 * 20;
	private static final int SHIP_TOKEN_UPDATE_DELAY_FAILED_DEPLOY_TICKS = 5 * 20;
	private int shipToken_nextUpdate_ticks = 5;
	private static final int SHIP_TOKEN_PLAYER_WARMUP_PERIODS = 5;
	private UUID shipToken_idPlayer = null;
	private int shipToken_countWarmup = SHIP_TOKEN_PLAYER_WARMUP_PERIODS;
	private String shipToken_nameSchematic = "";
	private void checkPlayerForShipToken() {
		assert world != null;
		// cool down to prevent player chat spam and server lag
		shipToken_nextUpdate_ticks--;
		if (shipToken_nextUpdate_ticks > 0) {
			return;
		}
		shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_PERIOD_TICKS;
		
		// find a unique player in range
		final AxisAlignedBB axisalignedbb = new AxisAlignedBB(pos.getX() - 1.0D, pos.getY() + 1.0D, pos.getZ() - 1.0D,
		                                                      pos.getX() + 1.99D, pos.getY() + 5.0D, pos.getZ() + 1.99D);
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		final List<PlayerEntity> entityPlayers = new ArrayList<>(10);
		for (final Object object : list) {
			if (object instanceof PlayerEntity) {
				entityPlayers.add((PlayerEntity) object);
			}
		}
		if (entityPlayers.isEmpty()) {
			shipToken_idPlayer = null;
			return;
		}
		if (entityPlayers.size() > 1) {
			for (final PlayerEntity entityPlayer : entityPlayers) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.builder.guide.too_many_players"));
				shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_DELAY_FAILED_PRECONDITION_TICKS;
			}
			shipToken_idPlayer = null;
			return;
		}
		final PlayerEntity entityPlayer = entityPlayers.get(0);
		
		// check inventory
		int slotIndex = 0;
		ItemStack itemStack = null;
		for (; slotIndex < entityPlayer.inventory.getSizeInventory(); slotIndex++) {
			itemStack = entityPlayer.inventory.getStackInSlot(slotIndex);
			if ( !itemStack.isEmpty()
			  && itemStack.getItem() instanceof ItemShipToken
			  && itemStack.getCount() >= 1 ) {
				break;
			}
		}
		if ( itemStack == null
		  || slotIndex >= entityPlayer.inventory.getSizeInventory() ) {
			Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.builder.guide.no_ship_token"));
			shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_DELAY_FAILED_PRECONDITION_TICKS;
			shipToken_idPlayer = null;
			return;
		}
		
		// short warm-up so payer can cancel eventually
		if ( entityPlayer.getUniqueID() != shipToken_idPlayer
		  || !shipToken_nameSchematic.equals(ItemShipToken.getSchematicName(itemStack)) ) {
			shipToken_idPlayer = entityPlayer.getUniqueID();
			shipToken_countWarmup = SHIP_TOKEN_PLAYER_WARMUP_PERIODS + 1;
			shipToken_nameSchematic = ItemShipToken.getSchematicName(itemStack);
			Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.builder.guide.ship_token_detected",
			                                                       shipToken_nameSchematic));
		}
		shipToken_countWarmup--;
		if (shipToken_countWarmup > 0) {
			Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleNormal(), "warpdrive.builder.guide.ship_materialization_countdown",
			                                                       shipToken_nameSchematic, shipToken_countWarmup));
			return;
		}
		// warm-up done
		shipToken_idPlayer = null;
		playerName = entityPlayer.getName().getString();
		
		// try deploying
		final WarpDriveText reason = new WarpDriveText();
		final boolean isSuccess = deployShip(ItemShipToken.getSchematicName(itemStack),
		                                     targetX - pos.getX(), targetY - pos.getY(), targetZ - pos.getZ(), rotationSteps,
		                                     true, reason);
		if (!isSuccess) {
			// failed
			Commons.addChatMessage(entityPlayer, reason);
			shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_DELAY_FAILED_DEPLOY_TICKS;
			return;
		}
		Commons.addChatMessage(entityPlayer, reason);
		
		// success => remove token
		if (!entityPlayer.isCreative()) {
			itemStack.shrink(1);
			entityPlayer.inventory.setInventorySlotContents(slotIndex, itemStack);
			entityPlayer.inventory.markDirty();
		}
	}
}
package cr0s.warpdrive.block.building;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumShipScannerState;
import cr0s.warpdrive.data.JumpBlock;
import cr0s.warpdrive.data.JumpShip;
import cr0s.warpdrive.data.Transformation;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.item.ItemShipToken;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityShipScanner extends TileEntityAbstractEnergy {
	
	private static final int SHIP_TOKEN_MAX_RETRY_COUNT = 5;
	
	// persistent properties
	private String schematicFileName = "";
	private int targetX, targetY, targetZ;
	private byte rotationSteps;
	public Block blockCamouflage;
	public int metadataCamouflage;
	protected int colorMultiplierCamouflage;
	protected int lightCamouflage;
	
	// computed properties
	private EnumShipScannerState enumShipScannerState = EnumShipScannerState.IDLE;
	private TileEntityShipCore shipCore = null;
	
	private int laserTicks = 0;
	private int scanTicks = 0;
	private int deployDelayTicks = 0;
	private int deployRetryCounts = 0;
	
	private int searchTicks = 0;
	
	private String playerName = "";
	
	private JumpShip jumpShip;
	private int currentDeployIndex;
	private int blocksToDeployCount;
	
	public TileEntityShipScanner() {
		super();
		
		peripheralName = "warpdriveShipScanner";
		addMethods(new String[] {
				"scan",
				"fileName",
				"deploy",
				"state"
		});
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (worldObj.isRemote) {
			return;
		}
		
		searchTicks++;
		if (searchTicks > WarpDriveConfig.SS_SEARCH_INTERVAL_TICKS) {
			searchTicks = 0;
			shipCore = searchShipCore();
		}
		
		// Trigger deployment by player, provided setup is done
		final boolean isSetupDone = targetX != 0 || targetY != 0 || targetZ != 0;
		if (isSetupDone) {
			if (enumShipScannerState == EnumShipScannerState.IDLE) {
				checkPlayerForShipToken();
			}
			if (enumShipScannerState != EnumShipScannerState.DEPLOYING) {
				setState(EnumShipScannerState.IDLE); // disable scanner
				return;
			}
			
		} else if (enumShipScannerState != EnumShipScannerState.DEPLOYING && shipCore == null) {// Ship core is not found
			laserTicks++;
			if (laserTicks > 20) {
				PacketHandler.sendBeamPacket(worldObj, 
				                             new Vector3(this).translate(0.5D), 
				                             new Vector3(xCoord, yCoord + 5, zCoord).translate(0.5D), 
				                             1.0F, 0.2F, 0.0F, 40, 0, 100);
				laserTicks = 0;
			}
			return;
		}
		
		switch (enumShipScannerState) {
		case IDLE:// inactive
			if (shipCore != null) {// and ship core found
				laserTicks++;
				if (laserTicks > 20) {
					PacketHandler.sendBeamPacket(worldObj,
					                             new Vector3(this).translate(0.5D),
					                             new Vector3(shipCore.xCoord, shipCore.yCoord, shipCore.zCoord).translate(0.5D),
					                             0.0F, 1.0F, 0.2F, 40, 0, 100);
					laserTicks = 0;
				}
			}
			break;
			
		case SCANNING:// active and scanning
			laserTicks++;
			if (laserTicks > 5) {
				laserTicks = 0;
				
				for (int index = 0; index < 10; index++) {
					int randomX = shipCore.minX + worldObj.rand.nextInt(shipCore.maxX - shipCore.minX + 1);
					int randomY = shipCore.minY + worldObj.rand.nextInt(shipCore.maxY - shipCore.minY + 1);
					int randomZ = shipCore.minZ + worldObj.rand.nextInt(shipCore.maxZ - shipCore.minZ + 1);
					
					worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
					float r = worldObj.rand.nextFloat() - worldObj.rand.nextFloat();
					float g = worldObj.rand.nextFloat() - worldObj.rand.nextFloat();
					float b = worldObj.rand.nextFloat() - worldObj.rand.nextFloat();
					
					PacketHandler.sendBeamPacket(worldObj,
							new Vector3(this).translate(0.5D),
							new Vector3(randomX, randomY, randomZ).translate(0.5D),
							r, g, b, 15, 0, 100);
				}
			}
			
			scanTicks++;
			if (scanTicks > 20 * (1 + shipCore.shipMass / WarpDriveConfig.SS_SCAN_BLOCKS_PER_SECOND)) {
				setState(EnumShipScannerState.IDLE); // disable scanner
				scanTicks = 0;
			}
			break;
			
		case DEPLOYING:// active and deploying
			deployDelayTicks++;
			if (deployDelayTicks > WarpDriveConfig.SS_DEPLOY_INTERVAL_TICKS) {
				deployDelayTicks = 0;
				
				// refresh player object
				final EntityPlayerMP entityPlayerMP = MinecraftServer.getServer().getConfigurationManager().func_152612_a(playerName);
				
				// deploy at most (jump speed / 4), at least (deploy speed), optimally in 10 seconds 
				final int optimumSpeed = Math.round(blocksToDeployCount * WarpDriveConfig.SS_DEPLOY_INTERVAL_TICKS / (20 * 10.0F));
				final int blockToDeployPerTick = Math.max(WarpDriveConfig.SS_DEPLOY_BLOCKS_PER_INTERVAL,
						Math.min(WarpDriveConfig.G_BLOCKS_PER_TICK / 4, optimumSpeed));
				final int blocksToDeployCurrentTick = Math.min(blockToDeployPerTick, blocksToDeployCount - currentDeployIndex);
				final int periodLaserEffect = Math.max(1, (blocksToDeployCurrentTick / 10));
				if (WarpDrive.isDev && WarpDriveConfig.LOGGING_BUILDING) {
					WarpDrive.logger.info("optimumSpeed " + optimumSpeed + " blockToDeployPerTick " + blockToDeployPerTick + " blocksToDeployCurrentTick " + blocksToDeployCurrentTick + " currentDeployIndex " + currentDeployIndex);
				}
				
				// deployment done?
				if (blocksToDeployCurrentTick == 0) {
					if (playerName != null && !playerName.isEmpty()) {
						final TileEntity tileEntity = worldObj.getTileEntity(targetX, targetY, targetZ);
						if (tileEntity instanceof TileEntityShipCore) {
							final boolean isSuccess = ((TileEntityShipCore) tileEntity).summonOwnerOnDeploy(entityPlayerMP);
							if (isSuccess) {
								if (entityPlayerMP != null) {
									Commons.addChatMessage(entityPlayerMP, "§6" + "Welcome aboard captain. Use the computer to get moving...");
								}
							} else {
								deployRetryCounts--;
								WarpDrive.logger.warn(String.format("Ship scanner failed to assign new captain, %d retries left",
								                                    deployRetryCounts));
								if (deployRetryCounts > 0) {
									return;
								}
							}
						} else {
							WarpDrive.logger.warn(String.format("Ship scanner unable to detect ship core after deployment, found %s",
							                                    tileEntity));
							deployRetryCounts--;
							if (deployRetryCounts > 0) {
								return;
							}
						}
					}
					
					setState(EnumShipScannerState.IDLE); // disable scanner
					if (WarpDriveConfig.LOGGING_BUILDING) {
						WarpDrive.logger.info(this + " Deployment done");
					}
					shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_PERIOD_TICKS * 3;
					return;
				}
				
				if (WarpDriveConfig.LOGGING_BUILDING) {
					WarpDrive.logger.info(this + " Deploying " + blocksToDeployCurrentTick + " more blocks");
				}
				Transformation transformation = new Transformation(jumpShip, worldObj, targetX - jumpShip.coreX, targetY - jumpShip.coreY, targetZ - jumpShip.coreZ, rotationSteps);
				int index = 0;
				while (index < blocksToDeployCurrentTick && currentDeployIndex < blocksToDeployCount) {
					// Deploy single block
					JumpBlock jumpBlock = jumpShip.jumpBlocks[currentDeployIndex];
					
					if (jumpBlock == null) {
						if (WarpDriveConfig.LOGGING_BUILDING) {
							WarpDrive.logger.info("At index " + currentDeployIndex + ", skipping undefined block");
						}
					} else if (jumpBlock.block == Blocks.air) {
						if (WarpDriveConfig.LOGGING_BUILDING) {
							WarpDrive.logger.info("At index " + currentDeployIndex + ", skipping air block");
						}
					} else if (Dictionary.BLOCKS_ANCHOR.contains(jumpBlock.block)) {
						if (WarpDriveConfig.LOGGING_BUILDING) {
							WarpDrive.logger.info("At index " + currentDeployIndex + ", skipping anchor block " + jumpBlock.block);
						}
					} else {
						index++;
						if (WarpDriveConfig.LOGGING_BUILDING) {
							WarpDrive.logger.info("At index " + currentDeployIndex + ", deploying block " + Block.blockRegistry.getNameForObject(jumpBlock.block) + ":" + jumpBlock.blockMeta
								+ " tileEntity " + jumpBlock.blockTileEntity + " NBT " + jumpBlock.blockNBT);
						}
						
						// clear computers unique identifiers when using token
						if (!playerName.isEmpty()) {
							jumpBlock.removeUniqueIDs();
						}
						
						ChunkCoordinates targetLocation = transformation.apply(jumpBlock.x, jumpBlock.y, jumpBlock.z);
						Block blockAtTarget = worldObj.getBlock(targetLocation.posX, targetLocation.posY, targetLocation.posZ);
						if (blockAtTarget == Blocks.air || Dictionary.BLOCKS_EXPANDABLE.contains(blockAtTarget)) {
							jumpBlock.deploy(worldObj, transformation);
							
							if (index % periodLaserEffect == 0) {
								worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 0.5F, 1.0F);
								
								PacketHandler.sendBeamPacket(worldObj,
										new Vector3(this).translate(0.5D),
										new Vector3(targetLocation.posX, targetLocation.posY, targetLocation.posZ).translate(0.5D),
										0f, 1f, 0f, 15, 0, 100);
							}
							worldObj.playSoundEffect(targetLocation.posX + 0.5F, targetLocation.posY + 0.5F, targetLocation.posZ + 0.5F,
								jumpBlock.block.stepSound.func_150496_b(), (jumpBlock.block.stepSound.getVolume() + 1.0F) / 2.0F, jumpBlock.block.stepSound.getPitch() * 0.8F);
							
						} else {
							if (WarpDriveConfig.LOGGING_BUILDING) {
								WarpDrive.logger.info("Target position is occupied, skipping");
							}
							worldObj.newExplosion(null, targetX + jumpBlock.x, targetY + jumpBlock.y, targetZ + jumpBlock.z, 3, false, false);
							WarpDrive.logger.info("Deployment collision detected at " + (targetX + jumpBlock.x) + " " + (targetY + jumpBlock.y) + " " + (targetZ + jumpBlock.z));
						}
					}
					
					currentDeployIndex++;
					
					// Warn owner if deployment done but wait next tick for teleportation 
					if (currentDeployIndex >= blocksToDeployCount) {
						if (entityPlayerMP != null) {
							Commons.addChatMessage(entityPlayerMP, "Ship complete. Teleporting captain to the main deck");
						}
					}
				}
			}
			break;
			
		default:
			WarpDrive.logger.error("Invalid ship scanner state, forcing to IDLE...");
			setState(EnumShipScannerState.IDLE);
			break;
		}
	}
	
	private void setState(final EnumShipScannerState newState) {
		if (enumShipScannerState == newState) {
			return;
		}
		enumShipScannerState = newState;
		if (blockCamouflage == null) {
			if (getBlockMetadata() == newState.getMetadata()) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, newState.getMetadata(), 2);
			}
		} else {
			if (getBlockMetadata() != metadataCamouflage) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, metadataCamouflage, 2);
			}
		}
	}
	
	private TileEntityShipCore searchShipCore() {
		StringBuilder reason = new StringBuilder();
		TileEntityShipCore tileEntityShipCore = null;
		
		// Search for ship cores above
		for (int newY = yCoord + 1; newY <= 255; newY++) {
			if (worldObj.getBlock(xCoord, newY, zCoord).isAssociatedBlock(WarpDrive.blockShipCore)) { // found ship core above
				tileEntityShipCore = (TileEntityShipCore) worldObj.getTileEntity(xCoord, newY, zCoord);
				if (tileEntityShipCore != null) {
					if (!tileEntityShipCore.validateShipSpatialParameters(reason)) { // If we can't refresh ship's spatial parameters
						tileEntityShipCore = null;
					}
				}
				
				break;
			}
		}
		
		return tileEntityShipCore;
	}
	
	private int getScanningEnergyCost(int size) {
		if (WarpDriveConfig.SS_ENERGY_PER_BLOCK_SCAN >= 0) {
			return size * WarpDriveConfig.SS_ENERGY_PER_BLOCK_SCAN;
		} else {
			return WarpDriveConfig.SS_MAX_ENERGY_STORED;
		}
	}
	
	private int getDeploymentEnergyCost(int size) {
		if (WarpDriveConfig.SS_ENERGY_PER_BLOCK_DEPLOY >= 0) {
			return size * WarpDriveConfig.SS_ENERGY_PER_BLOCK_DEPLOY;
		} else {
			return WarpDriveConfig.SS_MAX_ENERGY_STORED;
		}
	}
	
	private boolean saveShipToSchematic(String fileName, StringBuilder reason) {
		if (!shipCore.validateShipSpatialParameters(reason)) {
			return false;
		}
		short width = (short) (shipCore.maxX - shipCore.minX + 1);
		short length = (short) (shipCore.maxZ - shipCore.minZ + 1);
		short height = (short) (shipCore.maxY - shipCore.minY + 1);
		int size = width * length * height;
		
		if (width <= 0 || length <= 0 || height <= 0) {
			reason.append("Invalid ship dimensions, nothing to scan");
			return false;
		}
		
		// Consume energy
		int energyCost = getScanningEnergyCost(shipCore.shipMass);
		if (!energy_consume(energyCost, false)) {
			reason.append(String.format("Insufficient energy (%d required)", energyCost));
			return false;
		}
		
		// Save header
		NBTTagCompound schematic = new NBTTagCompound();
		
		schematic.setShort("Width", width);
		schematic.setShort("Length", length);
		schematic.setShort("Height", height);
		schematic.setInteger("shipMass", shipCore.shipMass);
		schematic.setString("shipName", shipCore.shipName);
		schematic.setInteger("shipVolume", shipCore.shipVolume);
		
		// Save new format
		JumpShip ship = new JumpShip();
		ship.worldObj = shipCore.getWorldObj();
		ship.coreX = shipCore.xCoord;
		ship.coreY = shipCore.yCoord;
		ship.coreZ = shipCore.zCoord;
		ship.dx = shipCore.facing.offsetX;
		ship.dz = shipCore.facing.offsetZ;
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
		NBTTagCompound tagCompoundShip = new NBTTagCompound();
		ship.writeToNBT(tagCompoundShip);
		schematic.setTag("ship", tagCompoundShip);
		
		// Storage collections
		String stringBlockRegistryNames[] = new String[size];
		byte byteMetadatas[] = new byte[size];
		NBTTagList tileEntitiesList = new NBTTagList();
		
		// Scan the whole area
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				for (int z = 0; z < length; z++) {
					Block block = worldObj.getBlock(shipCore.minX + x, shipCore.minY + y, shipCore.minZ + z);
					
					// Skip leftBehind and anchor blocks
					if (Dictionary.BLOCKS_LEFTBEHIND.contains(block) || Dictionary.BLOCKS_ANCHOR.contains(block)) {
						block = Blocks.air;
					}
					
					int index = x + (y * length + z) * width;
					stringBlockRegistryNames[index] = Block.blockRegistry.getNameForObject(block);
					byteMetadatas[index] = (byte) worldObj.getBlockMetadata(shipCore.minX + x, shipCore.minY + y, shipCore.minZ + z);
					
					if (!block.isAssociatedBlock(Blocks.air)) {
						TileEntity tileEntity = worldObj.getTileEntity(shipCore.minX + x, shipCore.minY + y, shipCore.minZ + z);
						if (tileEntity != null) {
							try {
								NBTTagCompound tagTileEntity = new NBTTagCompound();
								tileEntity.writeToNBT(tagTileEntity);
								
								JumpBlock.removeUniqueIDs(tagTileEntity);
								
								// Transform TE's coordinates from local axis to .schematic offset-axis
								// Warning: this is a cheap workaround for World Edit. Use the native format for proper transformation
								tagTileEntity.setInteger("x", tileEntity.xCoord - shipCore.minX);
								tagTileEntity.setInteger("y", tileEntity.yCoord - shipCore.minY);
								tagTileEntity.setInteger("z", tileEntity.zCoord - shipCore.minZ);
								
								tileEntitiesList.appendTag(tagTileEntity);
							} catch (Exception exception) {
								exception.printStackTrace();
							}
						}
					}
				}
			}
		}
		
		schematic.setString("Materials", "Alpha");
		NBTTagList tagListBlocks = new NBTTagList();
		for (String stringRegistryName : stringBlockRegistryNames) {
			tagListBlocks.appendTag(new NBTTagString(stringRegistryName));
		}
		schematic.setTag("Blocks", tagListBlocks);
		schematic.setByteArray("Data", byteMetadatas);
		
		schematic.setTag("Entities", new NBTTagList()); // don't save entities
		schematic.setTag("TileEntities", tileEntitiesList);
		
		Commons.writeNBTToFile(fileName, schematic);
		
		return true;
	}
	
	// Begins ship scan
	private boolean scanShip(StringBuilder reason) {
		// Enable scanner
		setState(EnumShipScannerState.SCANNING);
		File file = new File(WarpDriveConfig.G_SCHEMALOCATION);
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs()) {
				return false;
			}
		}
		
		// Generate unique file name
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss's'SSS");
		String shipName = shipCore.shipName.replaceAll("[^ -~]", "").replaceAll("[:/\\\\]", "");
		do {
			Date now = new Date();
			schematicFileName = shipName + "_" + sdfDate.format(now);
		} while (new File(WarpDriveConfig.G_SCHEMALOCATION + "/" + schematicFileName + ".schematic").exists());
		
		if (!saveShipToSchematic(WarpDriveConfig.G_SCHEMALOCATION + "/" + schematicFileName + ".schematic", reason)) {
			return false;
		}
		reason.append(schematicFileName);
		return true;
	}
	
	// Returns error code and reason string
	private int deployShip(final String fileName, final int offsetX, final int offsetY, final int offsetZ,
	                       final byte rotationSteps, final boolean isForced, final StringBuilder reason) {
		targetX = xCoord + offsetX;
		targetY = yCoord + offsetY;
		targetZ = zCoord + offsetZ;
		this.rotationSteps = rotationSteps;
		
		jumpShip = JumpShip.createFromFile(fileName, reason);
		if (jumpShip == null) {
			return -1;
		}
		
		blocksToDeployCount = jumpShip.jumpBlocks.length;
		if (WarpDriveConfig.LOGGING_BUILDING) {
			WarpDrive.logger.info(String.format("[ShipScanner] Loaded %d blocks to deploy", blocksToDeployCount));
		}
		
		// Validate context
		{
			// Check distance
			double dX = xCoord - targetX;
			double dY = yCoord - targetY;
			double dZ = zCoord - targetZ;
			double distance = MathHelper.sqrt_double(dX * dX + dY * dY + dZ * dZ);
			
			if (distance > WarpDriveConfig.SS_MAX_DEPLOY_RADIUS_BLOCKS) {
				reason.append(String.format("Cannot deploy ship more than %d blocks away from scanner.", WarpDriveConfig.SS_MAX_DEPLOY_RADIUS_BLOCKS));
				return 5;
			}
			
			// Consume energy
			int energyCost = getDeploymentEnergyCost(blocksToDeployCount);
			if (!energy_consume(energyCost, false)) {
				reason.append(String.format("Insufficient energy (%d required)", energyCost));
				return 1;
			}
			
			// Compute target area
			Transformation transformation = new Transformation(jumpShip, worldObj, targetX - jumpShip.coreX, targetY - jumpShip.coreY, targetZ - jumpShip.coreZ, rotationSteps);
			ChunkCoordinates targetLocation1 = transformation.apply(jumpShip.minX, jumpShip.minY, jumpShip.minZ);
			ChunkCoordinates targetLocation2 = transformation.apply(jumpShip.maxX, jumpShip.maxY, jumpShip.maxZ);
			ChunkCoordinates targetLocationMin = new ChunkCoordinates(
			                Math.min(targetLocation1.posX, targetLocation2.posX) - 1,
			    Math.max(0, Math.min(targetLocation1.posY, targetLocation2.posY) - 1),
			                Math.min(targetLocation1.posZ, targetLocation2.posZ) - 1);
			ChunkCoordinates targetLocationMax = new ChunkCoordinates(
			                  Math.max(targetLocation1.posX, targetLocation2.posX) + 1,
			    Math.min(255, Math.max(targetLocation1.posY, targetLocation2.posY) + 1),
			                  Math.max(targetLocation1.posZ, targetLocation2.posZ) + 1);
			
			if (isForced) {
				if (!worldObj.isAirBlock(targetX, targetY, targetZ)) {
					worldObj.newExplosion(null, targetX, targetY, targetZ, 1, false, false);
					if (WarpDriveConfig.LOGGING_BUILDING) {
						WarpDrive.logger.info(String.format("Deployment collision detected at %d %d %d",
						                                    targetX, targetY, targetZ));
					}
					reason.append(String.format("Deployment area occupied with existing ship.\nCan't deploy new ship at %d %d %d",
					                            targetX, targetY, targetZ));
					return 2;
				}
				
				// Clear specified area for any blocks to avoid corruption and ensure clean full ship
				for (int x = targetLocationMin.posX; x <= targetLocationMax.posX; x++) {
					for (int y = targetLocationMin.posY; y <= targetLocationMax.posY; y++) {
						for (int z = targetLocationMin.posZ; z <= targetLocationMax.posZ; z++) {
							worldObj.setBlockToAir(x, y, z);
						}
					}
				}
				
			} else {
				
				// Check specified area for occupation by blocks
				// If specified area is occupied, break deployment with error message
				int occupiedBlockCount = 0;
				for (int x = targetLocationMin.posX; x <= targetLocationMax.posX; x++) {
					for (int y = targetLocationMin.posY; y <= targetLocationMax.posY; y++) {
						for (int z = targetLocationMin.posZ; z <= targetLocationMax.posZ; z++) {
							if (!worldObj.isAirBlock(x, y, z)) {
								occupiedBlockCount++;
								if (occupiedBlockCount == 1 || (occupiedBlockCount <= 100 && worldObj.rand.nextInt(10) == 0)) {
									worldObj.newExplosion(null, x, y, z, 1, false, false);
								}
								if (WarpDriveConfig.LOGGING_BUILDING) {
									WarpDrive.logger.info("Deployment collision detected at " + x + " " + y + " " + z);
								}
							}
						}
					}
				}
				if (occupiedBlockCount > 0) {
					reason.append(String.format("Deployment area occupied with %d blocks. Can't deploy ship.", occupiedBlockCount));
					return 2;
				}
			}
		}
		
		// initiate deployment sequencer
		currentDeployIndex = 0;
		deployRetryCounts = SHIP_TOKEN_MAX_RETRY_COUNT;
		
		setState(EnumShipScannerState.DEPLOYING);
		reason.append(String.format("Deploying ship '%s'...", fileName));
		return 3;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tatagCompound) {
		super.readFromNBT(tatagCompound);
		schematicFileName = tatagCompound.getString("schematic");
		targetX = tatagCompound.getInteger("targetX");
		targetY = tatagCompound.getInteger("targetY");
		targetZ = tatagCompound.getInteger("targetZ");
		rotationSteps = tatagCompound.getByte("rotationSteps");
		if (tatagCompound.hasKey("camouflageBlock")) {
			try {
				blockCamouflage = Block.getBlockFromName(tatagCompound.getString("camouflageBlock"));
				metadataCamouflage = tatagCompound.getByte("camouflageMeta");
				colorMultiplierCamouflage = tatagCompound.getInteger("camouflageColorMultiplier");
				lightCamouflage = tatagCompound.getByte("camouflageLight");
				if (Dictionary.BLOCKS_NOCAMOUFLAGE.contains(blockCamouflage)) {
					blockCamouflage = null;
					metadataCamouflage = 0;
					colorMultiplierCamouflage = 0;
					lightCamouflage = 0;
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		} else {
			blockCamouflage = null;
			metadataCamouflage = 0;
			colorMultiplierCamouflage = 0;
			lightCamouflage = 0;
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setString("schematic", schematicFileName);
		tagCompound.setInteger("targetX", targetX);
		tagCompound.setInteger("targetY", targetY);
		tagCompound.setInteger("targetZ", targetZ);
		tagCompound.setByte("rotationSteps", rotationSteps);
		if (blockCamouflage != null) {
			tagCompound.setString("camouflageBlock", Block.blockRegistry.getNameForObject( blockCamouflage));
			tagCompound.setByte("camouflageMeta", (byte) metadataCamouflage);
			tagCompound.setInteger("camouflageColorMultiplier",  colorMultiplierCamouflage);
			tagCompound.setByte("camouflageLight", (byte) lightCamouflage);
		}
	}
	
	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, -1, tagCompound);
	}
	
	@Override
	public void onDataPacket(NetworkManager networkManager, S35PacketUpdateTileEntity packet) {
		NBTTagCompound tagCompound = packet.func_148857_g();
		readFromNBT(tagCompound);
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] scan(Context context, Arguments arguments) {
		return scan();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] filename(Context context, Arguments arguments) {
		return filename();
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] deploy(Context context, Arguments arguments) {
		return deploy(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] state(Context context, Arguments arguments) {
		return state();
	}
	
	private Object[] scan() {
		// Already scanning?
		if (enumShipScannerState != EnumShipScannerState.IDLE) {
			return new Object[] { false, 0, "Already active" };
		}
		
		if (shipCore == null) {
			return new Object[] { false, 1, "Ship-Core not found" };
		}
		int energyCost = getScanningEnergyCost(shipCore.shipMass);
		if (!energy_consume(energyCost, true)) {
			return new Object[] { false, 2, "Not enough energy! " + energyCost + " required." };
		} else {
			StringBuilder reason = new StringBuilder();
			boolean success = scanShip(reason);
			return new Object[] { success, 3, reason.toString() };
		}
	}
	
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
	
	private Object[] deploy(Object[] arguments) {
		if (arguments.length == 5) {
			String fileName = (String) arguments[0];
			int x = Commons.toInt(arguments[1]);
			int y = Commons.toInt(arguments[2]);
			int z = Commons.toInt(arguments[3]);
			byte rotationSteps = (byte) Commons.toInt(arguments[4]);
			
			if (!new File(WarpDriveConfig.G_SCHEMALOCATION + "/" + fileName + ".schematic").exists()) {
				return new Object[] { 0, "Specified schematic file was not found!" };
			} else {
				final StringBuilder reason = new StringBuilder();
				final int result = deployShip(fileName, x, y, z, rotationSteps, false, reason);
				playerName = "";
				return new Object[] { result, reason.toString() };
			}
		} else {
			return new Object[] { 4, "Invalid arguments count, you need .schematic file name, offsetX, offsetY, offsetZ, rotationSteps!" };
		}
	}
	
	private Object[] state() {
		switch (enumShipScannerState) {
		default:
		case IDLE:
			return new Object[] { false, "IDLE", 0, 0 };
		case SCANNING:
			return new Object[] { true, "Scanning", 0, 0 };
		case DEPLOYING:
			return new Object[] { true, "Deploying", currentDeployIndex, blocksToDeployCount };
		}
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		switch (methodName) {
			case "scan":
				return scan();

			case "fileName":
				return filename();

			case "deploy": // deploy(schematicFileName, offsetX, offsetY, offsetZ)
				return deploy(arguments);

			case "state":
				return state();
		}
		
		return super.callMethod(computer, context, method, arguments);
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
		// cooldown to prevent player chat spam and server lag
		shipToken_nextUpdate_ticks--;
		if (shipToken_nextUpdate_ticks > 0) {
			return;
		}
		shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_PERIOD_TICKS;
		
		// find a unique player in range
		final AxisAlignedBB axisalignedbb = AxisAlignedBB.getBoundingBox(xCoord - 1.0D, yCoord + 1.0D, zCoord - 1.0D, 
		                                                                 xCoord + 1.99D, yCoord + 5.0D, zCoord + 1.99D);
		final List list = worldObj.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		final List<EntityPlayer> entityPlayers = new ArrayList<>(10);
		for (Object object : list) {
			if (object instanceof EntityPlayer) {
				entityPlayers.add((EntityPlayer) object);
			}
		}
		if (entityPlayers.isEmpty()) {
			shipToken_idPlayer = null;
			return;
		}
		if (entityPlayers.size() > 1) {
			for (EntityPlayer entityPlayer : entityPlayers) {
				Commons.addChatMessage(entityPlayer, "§c" + "Too many players detected: please stand in the beam one at a time.");
				shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_DELAY_FAILED_PRECONDITION_TICKS;
			}
			shipToken_idPlayer = null;
			return;
		}
		final EntityPlayer entityPlayer = entityPlayers.get(0);
		
		// check inventory
		int slotIndex = 0;
		ItemStack itemStack = null;
		for (; slotIndex < entityPlayer.inventory.getSizeInventory(); slotIndex++) {
			itemStack = entityPlayer.inventory.getStackInSlot(slotIndex);
			if ( itemStack != null
			  && itemStack.getItem() == WarpDrive.itemShipToken
			  && itemStack.stackSize >= 1) {
				break;
			}
		}
		if ( itemStack == null
		  || slotIndex >= entityPlayer.inventory.getSizeInventory() ) {
			Commons.addChatMessage(entityPlayer, "Please come back once you've a Ship token.");
			shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_DELAY_FAILED_PRECONDITION_TICKS;
			shipToken_idPlayer = null;
			return;
		}
		
		// short warmup so payer can cancel eventually
		if ( entityPlayer.getUniqueID() != shipToken_idPlayer
		  || !shipToken_nameSchematic.equals(ItemShipToken.getSchematicName(itemStack)) ) {
			shipToken_idPlayer = entityPlayer.getUniqueID();
			shipToken_countWarmup = SHIP_TOKEN_PLAYER_WARMUP_PERIODS + 1;
			shipToken_nameSchematic = ItemShipToken.getSchematicName(itemStack);
			Commons.addChatMessage(entityPlayer, "§6" + String.format("Ship token '%1$s' detected!", shipToken_nameSchematic));
		}
		shipToken_countWarmup--;
		if (shipToken_countWarmup > 0) {
			Commons.addChatMessage(entityPlayer, String.format("Stand by for ship materialization in %2$d...",
			                                                   shipToken_nameSchematic, shipToken_countWarmup));
			return;
		}
		// warmup done
		shipToken_idPlayer = null;
		playerName = entityPlayer.getCommandSenderName();
		
		// try deploying
		final StringBuilder reason = new StringBuilder();
		deployShip(ItemShipToken.getSchematicName(itemStack), targetX - xCoord, targetY - yCoord, targetZ - zCoord, rotationSteps, true, reason);
		if (enumShipScannerState == EnumShipScannerState.IDLE) {
			// failed
			Commons.addChatMessage(entityPlayer, "§c" + reason.toString());
			shipToken_nextUpdate_ticks = SHIP_TOKEN_UPDATE_DELAY_FAILED_DEPLOY_TICKS;
			return;
		}
		Commons.addChatMessage(entityPlayer, "§6" + reason.toString());
		
		// success => remove token
		if (!entityPlayer.capabilities.isCreativeMode) {
			itemStack.stackSize--;
			if (itemStack.stackSize > 0) {
				entityPlayer.inventory.setInventorySlotContents(slotIndex, itemStack);
			} else {
				entityPlayer.inventory.setInventorySlotContents(slotIndex, null);
			}
			entityPlayer.inventory.markDirty();
		}
	}
	
	// IEnergySink methods implementation
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.SS_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(ForgeDirection from) {
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("%s @ \'%s\' (%d %d %d)", getClass().getSimpleName(),
			worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(), xCoord, yCoord, zCoord);
	}
}

package cr0s.warpdrive.event;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.EnumShipMovementType;
import cr0s.warpdrive.data.JumpBlock;
import cr0s.warpdrive.data.JumpShip;
import cr0s.warpdrive.data.MovingEntity;
import cr0s.warpdrive.data.Transformation;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.world.SpaceTeleporter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

public class JumpSequencer extends AbstractSequencer {
	
	// Jump vector
	private Transformation transformation;
	
	private final EnumShipMovementType shipMovementType;
	private int moveX, moveY, moveZ;
	private final byte rotationSteps;
	private String nameTarget;
	
	private final World sourceWorld;
	private World targetWorld;
	private Ticket sourceWorldTicket;
	private Ticket targetWorldTicket;
	
	private boolean collisionDetected = false;
	private ArrayList<Vector3> collisionAtSource;
	private ArrayList<Vector3> collisionAtTarget;
	private float collisionStrength = 0;
	
	private boolean isEnabled = false;
	private static final int STATE_IDLE = 0;
	private static final int STATE_CHUNKLOADING = 1;
	private static final int STATE_SAVING = 2;
	private static final int STATE_BORDERS = 3;
	private static final int STATE_TRANSFORMER = 4;
	private static final int STATE_BLOCKS = 5;
	private static final int STATE_EXTERNALS = 6;
	private static final int STATE_ENTITIES = 7;
	private static final int STATE_REMOVING = 8;
	private static final int STATE_CHUNKUNLOADING = 9;
	private static final int STATE_FINISHING = 10;
	private int state = STATE_IDLE;
	private int actualIndexInShip = 0;
	
	private final JumpShip ship;
	private boolean betweenWorlds;
	
	private final int destX;
	private final int destY;
	private final int destZ;
	
	private long msCounter = 0;
	private int ticks = 0;
	
	public JumpSequencer(final TileEntityShipCore shipCore, final EnumShipMovementType shipMovementType, final String nameTarget,
	                     final int moveX, final int moveY, final int moveZ, final byte rotationSteps,
	                     final int destX, final int destY, final int destZ) {
		this.sourceWorld = shipCore.getWorldObj();
		this.ship = new JumpShip();
		this.ship.worldObj = sourceWorld;
		this.ship.coreX = shipCore.xCoord;
		this.ship.coreY = shipCore.yCoord;
		this.ship.coreZ = shipCore.zCoord;
		this.ship.dx = shipCore.facing.offsetX;
		this.ship.dz = shipCore.facing.offsetZ;
		this.ship.minX = shipCore.minX;
		this.ship.maxX = shipCore.maxX;
		this.ship.minY = shipCore.minY;
		this.ship.maxY = shipCore.maxY;
		this.ship.minZ = shipCore.minZ;
		this.ship.maxZ = shipCore.maxZ;
		this.ship.shipCore = shipCore;
		this.shipMovementType = shipMovementType;
		this.moveX = moveX;
		this.moveY = moveY;
		this.moveZ = moveZ;
		this.rotationSteps = rotationSteps;
		this.nameTarget = nameTarget;
		this.destX = destX;
		this.destY = destY;
		this.destZ = destZ;
		
		// set when preparing jump
		targetWorld = null;
		
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Sequencer created");
		}
	}
	
	public void enable() {
		isEnabled = true;
		register();
	}
	
	public void disable(String reason) {
		if (!isEnabled) {
			return;
		}
		
		isEnabled = false;
		
		if (WarpDriveConfig.LOGGING_JUMP) {
			if (reason == null || reason.isEmpty()) {
				WarpDrive.logger.info(this + " Killing jump sequencer...");
			} else {
				WarpDrive.logger.info(this + " Killing jump sequencer... (" + reason + ")");
			}
		}
		
		unforceChunks();
		unregister();
	}
	
	private static final boolean enforceEntitiesPosition = false;
	
	@Override
	public boolean onUpdate() {
		if (sourceWorld.isRemote) {
			return false;
		}
		
		if (!isEnabled) {
			if (WarpDriveConfig.LOGGING_JUMP) {
				WarpDrive.logger.info(this + " Removing from onUpdate...");
			}
			return false;
		}
		
		if (ship.minY < 0 || ship.maxY > 255) {
			String msg = "Invalid Y coordinate(s), check ship dimensions...";
			ship.messageToAllPlayersOnShip(msg);
			disable(msg);
			return true;
		}
		
		ticks++;
		switch (state) {
		case STATE_IDLE:
			// blank state in case we got desync
			msCounter = System.currentTimeMillis();
			if (isEnabled) {
				state = STATE_CHUNKLOADING;
			}
			break;
			
		case STATE_CHUNKLOADING:
			state_chunkLoading();
			if (isEnabled) {
				actualIndexInShip = 0;
				state = STATE_SAVING;
			}
			break;
			
		case STATE_SAVING:
			state_saving();
			if (isEnabled) {
				actualIndexInShip = 0;
				state = STATE_BORDERS;
			}
			break;
			
		case STATE_BORDERS:
			state_borders();
			if (isEnabled) {
				actualIndexInShip = 0;
				state = STATE_TRANSFORMER;
			}
			break;
			
		case STATE_TRANSFORMER:
			state_transformer();
			if (isEnabled) {
				actualIndexInShip = 0;
				state = STATE_BLOCKS;
			}
			break;
			
		case STATE_BLOCKS:
			state_moveBlocks();
			if (actualIndexInShip >= ship.jumpBlocks.length - 1) {
				actualIndexInShip = 0;
				state = STATE_EXTERNALS;
			}
			break;
			
		case STATE_EXTERNALS:
			state_moveExternals();
			if (actualIndexInShip >= ship.jumpBlocks.length - 1) {
				state = STATE_ENTITIES;
			}
			break;
			
		case STATE_ENTITIES:
			state_moveEntities();
			actualIndexInShip = 0;
			state = STATE_REMOVING;
			break;
			
		case STATE_REMOVING:
			if (enforceEntitiesPosition) {
				restoreEntitiesPosition();
			}
			state_removeBlocks();
			
			if (actualIndexInShip >= ship.jumpBlocks.length - 1) {
				state = STATE_CHUNKUNLOADING;
			}
			break;
			
		case STATE_CHUNKUNLOADING:
			state_chunkUnloading();
			state = STATE_FINISHING;
			break;
			
		case STATE_FINISHING:
			state_finishing();
			state = STATE_IDLE;
			break;
			
		default:
			String msg = "Invalid state, aborting jump...";
			ship.messageToAllPlayersOnShip(msg);
			disable(msg);
			return true;
		}
		return true;
	}
	
	private boolean forceSourceChunks(StringBuilder reason) {
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Forcing source chunks in " + sourceWorld.provider.getDimensionName());
		}
		sourceWorldTicket = ForgeChunkManager.requestTicket(WarpDrive.instance, sourceWorld, Type.NORMAL);
		if (sourceWorldTicket == null) {
			reason.append(String.format("Chunkloading rejected in source world %s. Aborting.", sourceWorld.getWorldInfo().getWorldName()));
			return false;
		}
		int x1 = ship.minX >> 4;
		int x2 = ship.maxX >> 4;
		int z1 = ship.minZ >> 4;
		int z2 = ship.maxZ >> 4;
		int chunkCount = 0;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				chunkCount++;
				if (chunkCount > sourceWorldTicket.getMaxChunkListDepth()) {
					reason.append(String.format("Ship is extending over %d chunks in source world, this is too much! Max is currently set to %d in config/forgeChunkLoading.cfg. Aborting.",
					                            (x2 - x1 + 1) * (z2 - z1 + 1),
					                            sourceWorldTicket.getMaxChunkListDepth()));
					return false;
				}
				ForgeChunkManager.forceChunk(sourceWorldTicket, new ChunkCoordIntPair(x, z));
			}
		}
		return true;
	}
	
	private boolean forceTargetChunks(StringBuilder reason) {
		LocalProfiler.start("Jump.forceTargetChunks");
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Forcing target chunks in " + targetWorld.provider.getDimensionName());
		}
		targetWorldTicket = ForgeChunkManager.requestTicket(WarpDrive.instance, targetWorld, Type.NORMAL);
		if (targetWorldTicket == null) {
			reason.append(String.format("Chunkloading rejected in target world %s. Aborting.",
				sourceWorld.getWorldInfo().getWorldName()));
			return false;
		}
		
		ChunkCoordinates targetMin = transformation.apply(ship.minX, ship.minY, ship.minZ);
		ChunkCoordinates targetMax = transformation.apply(ship.maxX, ship.maxY, ship.maxZ);
		int x1 = Math.min(targetMin.posX, targetMax.posX) >> 4;
		int x2 = Math.max(targetMin.posX, targetMax.posX) >> 4;
		int z1 = Math.min(targetMin.posZ, targetMax.posZ) >> 4;
		int z2 = Math.max(targetMin.posZ, targetMax.posZ) >> 4;
		int chunkCount = 0;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				chunkCount++;
				if (chunkCount > targetWorldTicket.getMaxChunkListDepth()) {
					reason.append(String.format("Ship is extending over %d chunks in target world, this is too much! Max is currently set to %d in config/forgeChunkLoading.cfg. Aborting.",
					                            (x2 - x1 + 1) * (z2 - z1 + 1),
					                            targetWorldTicket.getMaxChunkListDepth()));
					return false;
				}
				ForgeChunkManager.forceChunk(targetWorldTicket, new ChunkCoordIntPair(x, z));
			}
		}
		LocalProfiler.stop();
		return true;
	}
	
	private void unforceChunks() {
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Unforcing chunks");
		}
		
		int x1, x2, z1, z2;
		if (sourceWorldTicket != null) {
			x1 = ship.minX >> 4;
			x2 = ship.maxX >> 4;
			z1 = ship.minZ >> 4;
			z2 = ship.maxZ >> 4;
			for (int x = x1; x <= x2; x++) {
				for (int z = z1; z <= z2; z++) {
					sourceWorld.getChunkFromChunkCoords(x, z).generateSkylightMap();
					ForgeChunkManager.unforceChunk(sourceWorldTicket, new ChunkCoordIntPair(x, z));
				}
			}
			ForgeChunkManager.releaseTicket(sourceWorldTicket);
			sourceWorldTicket = null;
		}
		
		if (targetWorldTicket != null) {
			ChunkCoordinates targetMin = transformation.apply(ship.minX, ship.minY, ship.minZ);
			ChunkCoordinates targetMax = transformation.apply(ship.maxX, ship.maxY, ship.maxZ);
			x1 = Math.min(targetMin.posX, targetMax.posX) >> 4;
			x2 = Math.max(targetMin.posX, targetMax.posX) >> 4;
			z1 = Math.min(targetMin.posZ, targetMax.posZ) >> 4;
			z2 = Math.max(targetMin.posZ, targetMax.posZ) >> 4;
			for (int x = x1; x <= x2; x++) {
				for (int z = z1; z <= z2; z++) {
					targetWorld.getChunkFromChunkCoords(x, z).generateSkylightMap();
					ForgeChunkManager.unforceChunk(targetWorldTicket, new ChunkCoordIntPair(x, z));
				}
			}
			ForgeChunkManager.releaseTicket(targetWorldTicket);
			targetWorldTicket = null;
		}
	}
	
	private void state_chunkLoading() {
		LocalProfiler.start("Jump.chunkLoading");
		
		StringBuilder reason = new StringBuilder();
		
		if (!forceSourceChunks(reason)) {
			String msg = reason.toString();
			disable(msg);
			ship.messageToAllPlayersOnShip(msg);
			LocalProfiler.stop();
			return;
		}
		
		LocalProfiler.stop();
	}
	
	private void state_saving() {
		LocalProfiler.start("Jump.saving");
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Saving ship...");
		}
		
		StringBuilder reason = new StringBuilder();
		
		if (!ship.save(reason)) {
			String msg = reason.toString();
			disable(msg);
			ship.messageToAllPlayersOnShip(msg);
			LocalProfiler.stop();
			return;
		}
		
		LocalProfiler.stop();
	}
	
	private void state_borders() {
		LocalProfiler.start("Jump.borders");
		
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Checking ship borders...");
		}
		
		StringBuilder reason = new StringBuilder();
		
		if (!ship.checkBorders(reason)) {
			String msg = reason.toString();
			disable(msg);
			ship.messageToAllPlayersOnShip(msg);
			LocalProfiler.stop();
			return;
		}
		
		File file = new File(WarpDriveConfig.G_SCHEMALOCATION + "/auto");
		if (!file.exists() || !file.isDirectory()) {
			if (!file.mkdirs()) {
				WarpDrive.logger.warn("Unable to create auto-backup folder, skipping...");
				LocalProfiler.stop();
				return;
			}
		}
		
		try {
			// Generate unique file name
			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss's'SSS");
			String shipName = ship.shipCore.shipName.replaceAll("[^ -~]", "").replaceAll("[:/\\\\]", "");
			String schematicFileName;
			do {
				Date now = new Date();
				schematicFileName = WarpDriveConfig.G_SCHEMALOCATION + "/auto/" + shipName + "_" + sdfDate.format(now) + ".schematic";
			} while (new File(schematicFileName).exists());
			
			// Save header
			NBTTagCompound schematic = new NBTTagCompound();
			
			short width = (short) (ship.shipCore.maxX - ship.shipCore.minX + 1);
			short length = (short) (ship.shipCore.maxZ - ship.shipCore.minZ + 1);
			short height = (short) (ship.shipCore.maxY - ship.shipCore.minY + 1);
			schematic.setShort("Width", width);
			schematic.setShort("Length", length);
			schematic.setShort("Height", height);
			schematic.setInteger("shipMass", ship.shipCore.shipMass);
			schematic.setString("shipName", ship.shipCore.shipName);
			schematic.setInteger("shipVolume", ship.shipCore.shipVolume);
			NBTTagCompound tagCompoundShip = new NBTTagCompound();
			ship.writeToNBT(tagCompoundShip);
			schematic.setTag("ship", tagCompoundShip);
			writeNBTToFile(schematicFileName, schematic);
			if (WarpDriveConfig.LOGGING_JUMP && WarpDrive.isDev) {
				WarpDrive.logger.info(this + " Ship saved as " + schematicFileName);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		
		msCounter = System.currentTimeMillis();
		LocalProfiler.stop();
	}
	
	private void writeNBTToFile(String fileName, NBTTagCompound nbttagcompound) {
		WarpDrive.logger.info(this + " Saving ship state prior to jump in " + fileName);
		
		try {
			File file = new File(fileName);
			if (!file.exists()) {
				//noinspection ResultOfMethodCallIgnored
				file.createNewFile();
			}
			
			FileOutputStream fileoutputstream = new FileOutputStream(file);
			
			CompressedStreamTools.writeCompressed(nbttagcompound, fileoutputstream);
			
			fileoutputstream.close();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	private void state_transformer() {
		LocalProfiler.start("Jump.transformer");
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Transformer evaluation...");
		}
		
		StringBuilder reason = new StringBuilder();
		
		betweenWorlds = shipMovementType == EnumShipMovementType.PLANET_TAKEOFF
		             || shipMovementType == EnumShipMovementType.PLANET_LANDING
		             || shipMovementType == EnumShipMovementType.HYPERSPACE_EXITING
		             || shipMovementType == EnumShipMovementType.HYPERSPACE_ENTERING;
		
		{// compute targetWorld and movement vector (moveX, moveY, moveZ)
			final CelestialObject celestialObjectSource = CelestialObjectManager.get(sourceWorld, ship.coreX, ship.coreZ);
			final boolean isTargetWorldFound = computeTargetWorld(celestialObjectSource, shipMovementType, reason);
			if (!isTargetWorldFound) {
				LocalProfiler.stop();
				ship.messageToAllPlayersOnShip(reason.toString());
				disable(reason.toString());
				return;
			}
		}
		
		// Check mass constrains
		if ( CelestialObjectManager.isPlanet(sourceWorld, ship.coreX, ship.coreZ)
		  || CelestialObjectManager.isPlanet(targetWorld, ship.coreX + moveX, ship.coreZ + moveZ) ) {
			if (!ship.isUnlimited() && ship.actualMass > WarpDriveConfig.SHIP_VOLUME_MAX_ON_PLANET_SURFACE) {
				LocalProfiler.stop();
				String msg = "Ship is too big for a planet (max is " + WarpDriveConfig.SHIP_VOLUME_MAX_ON_PLANET_SURFACE + " blocks)";
				ship.messageToAllPlayersOnShip(msg);
				disable(msg);
				return;
			}
		}
		
		if (betweenWorlds && WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " From world " + sourceWorld.provider.getDimensionName() + " to " + targetWorld.provider.getDimensionName());
		}
		
		// Calculate jump vector
		Boolean isPluginCheckDone = false;
		String firstAdjustmentReason = "";
		switch (shipMovementType) {
		case GATE_ACTIVATING:
		case CREATIVE:
			moveX = destX - ship.coreX;
			moveY = destY - ship.coreY;
			moveZ = destZ - ship.coreZ;
			break;
			
		case PLANET_TAKEOFF:
			// enter space at current altitude
			moveY = 0;
			break;
			
		case PLANET_LANDING:
			// re-enter atmosphere at max altitude
			moveY = 245 - ship.maxY;
			break;
			
		case PLANET_MOVING:
		case SPACE_MOVING:
		case HYPERSPACE_MOVING:
			if ((ship.maxY + moveY) > 255) {
				moveY = 255 - ship.maxY;
			}
			
			if ((ship.minY + moveY) < 5) {
				moveY = 5 - ship.minY;
			}
			
			// Do not check in long jumps
			int rangeX = Math.abs(moveX) - (ship.maxX - ship.minX);
			int rangeZ = Math.abs(moveZ) - (ship.maxZ - ship.minZ);
			if (Math.max(rangeX, rangeZ) < 256) {
				firstAdjustmentReason = getPossibleJumpDistance();
				isPluginCheckDone = true;
			}
			break;
			
		case HYPERSPACE_ENTERING:
		case HYPERSPACE_EXITING:
			break;
			
		default:
			WarpDrive.logger.error(String.format("Invalid movement type %s in JumpSequence.", shipMovementType));
			break;
		}
		transformation = new Transformation(ship, targetWorld, moveX, moveY, moveZ, rotationSteps);
		
		{
			ChunkCoordinates target1 = transformation.apply(ship.minX, ship.minY, ship.minZ);
			ChunkCoordinates target2 = transformation.apply(ship.maxX, ship.maxY, ship.maxZ);
			AxisAlignedBB aabbSource = AxisAlignedBB.getBoundingBox(ship.minX, ship.minY, ship.minZ, ship.maxX, ship.maxY, ship.maxZ);
			aabbSource.expand(1.0D, 1.0D, 1.0D);
			AxisAlignedBB aabbTarget = AxisAlignedBB.getBoundingBox(
					Math.min(target1.posX, target2.posX), Math.min(target1.posY, target2.posY), Math.min(target1.posZ, target2.posZ),
					Math.max(target1.posX, target2.posX), Math.max(target1.posY, target2.posY), Math.max(target1.posZ, target2.posZ));
			// Validate positions aren't overlapping
			if (!betweenWorlds && aabbSource.intersectsWith(aabbTarget)) {
				// render fake explosions
				doCollisionDamage(false);
				
				// cancel jump
				String msg;
				if (firstAdjustmentReason == null || firstAdjustmentReason.isEmpty()) {
					msg = "Source and target areas are overlapping, jump aborted! Try increasing jump distance...";
				} else {
					msg = firstAdjustmentReason + "\nNot enough space after adjustment, jump aborted!";
				}
				disable(msg);
				ship.messageToAllPlayersOnShip(msg);
				LocalProfiler.stop();
				return;
			}
			
			// Check world border
			CelestialObject celestialObjectTarget = CelestialObjectManager.get(targetWorld, (int) aabbTarget.minX, (int) aabbTarget.minZ);
			if (celestialObjectTarget == null) {
				if (WarpDriveConfig.LOGGING_JUMP) {
					WarpDrive.logger.error(String.format("There's no world border defined for dimension %s (%d)",
						targetWorld.provider.getDimensionName(), targetWorld.provider.dimensionId));
				}
				
			} else {
				// are we in range?
				if (!celestialObjectTarget.isInsideBorder(aabbTarget)) {
					final AxisAlignedBB axisAlignedBB = celestialObjectTarget.getWorldBorderArea();
					final String message = String.format(
						  "Target ship position is outside planet border, unable to jump!\n"
						+ "World borders are (%d %d %d) to (%d %d %d).",
						(int) axisAlignedBB.minX, (int) axisAlignedBB.minY, (int) axisAlignedBB.minZ,
						(int) axisAlignedBB.maxX, (int) axisAlignedBB.maxY, (int) axisAlignedBB.maxZ );
					LocalProfiler.stop();
					ship.messageToAllPlayersOnShip(message);
					disable(message);
					return;
				}
			}
		}
		if (!isPluginCheckDone) {
			CheckMovementResult checkMovementResult = checkCollisionAndProtection(transformation, true, "target");
			if (checkMovementResult != null) {
				String msg = checkMovementResult.reason + "\nJump aborted!";
				disable(msg);
				ship.messageToAllPlayersOnShip(msg);
				LocalProfiler.stop();
				return;
			}
		}
		
		if (!forceTargetChunks(reason)) {
			String msg = reason.toString();
			disable(msg);
			ship.messageToAllPlayersOnShip(msg);
			LocalProfiler.stop();
			return;
		}
		
		{
			String msg = ship.saveEntities();
			if (msg != null) {
				disable(msg);
				ship.messageToAllPlayersOnShip(msg);
				LocalProfiler.stop();
				return;
			}
			if (WarpDriveConfig.LOGGING_JUMP) {
				WarpDrive.logger.info(this + " Saved " + ship.entitiesOnShip.size() + " entities from ship");
			}
		}
		
		switch (shipMovementType) {
		case HYPERSPACE_ENTERING:
			ship.messageToAllPlayersOnShip("Entering hyperspace...");
			break;
		case HYPERSPACE_EXITING:
			ship.messageToAllPlayersOnShip("Leaving hyperspace..");
			break;
		case GATE_ACTIVATING:
			ship.messageToAllPlayersOnShip(String.format("Engaging jumpgate towards %s!",
			                                             nameTarget));
			break;
		// case GATE_ACTIVATING:
		// 	ship.messageToAllPlayersOnShip(String.format("Jumping to coordinates (%d %d %d)!",
		// 	                                             destX, destY, destZ));
		// 	break;
		default:
			ship.messageToAllPlayersOnShip(String.format("Jumping of %d blocks (XYZ %d %d %d)", 
			                                             (int) Math.ceil(Math.sqrt(moveX * moveX + moveY * moveY + moveZ * moveZ)),
			                                             moveX, moveY, moveZ));
			break;
		}
		
		switch (rotationSteps) {
			case 1:
				ship.messageToAllPlayersOnShip("Turning to the right");
				break;
			case 2:
				ship.messageToAllPlayersOnShip("Turning back");
				break;
			case 3:
				ship.messageToAllPlayersOnShip("Turning to the left");
				break;
			default:
				break;
		}
		
		LocalProfiler.stop();
		if (WarpDrive.isDev && WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(String.format("Removing TE duplicates: tileEntities in target world before jump: %d",
			                                    targetWorld.loadedTileEntityList.size()));
		}
	}
	
	private boolean computeTargetWorld(final CelestialObject celestialObjectSource, final EnumShipMovementType shipMovementType, final StringBuilder reason) {
		switch (shipMovementType) {
		case HYPERSPACE_EXITING: {
			CelestialObject celestialObject = CelestialObjectManager.getClosestChild(sourceWorld, ship.coreX, ship.coreZ);
			// anything defined?
			if (celestialObject == null) {
				reason.append(String.format("Unable to reach space from this location!\nThere's no celestial object defined for current dimension %s (%d).",
				                            sourceWorld.provider.getDimensionName(), sourceWorld.provider.dimensionId));
				return false;
			}
			
			// are we clear for transit?
			final double distanceSquared = celestialObject.getSquareDistanceInParent(sourceWorld.provider.dimensionId, ship.coreX, ship.coreZ);
			if (distanceSquared > 0.0D) {
				AxisAlignedBB axisAlignedBB = celestialObject.getAreaInParent();
				reason.append(String.format(
						"Ship is outside any solar system, unable to reach space!\n"
						+ "Closest transition area is ~%d m away (%d %d %d) to (%d %d %d).",
						(int) Math.sqrt(distanceSquared),
						(int) axisAlignedBB.minX, (int) axisAlignedBB.minY, (int) axisAlignedBB.minZ,
						(int) axisAlignedBB.maxX, (int) axisAlignedBB.maxY, (int) axisAlignedBB.maxZ));
				return false;
			}
			
			// is world available?
			final int dimensionIdSpace = celestialObject.dimensionId;
			targetWorld = MinecraftServer.getServer().worldServerForDimension(dimensionIdSpace);
			if (targetWorld == null) {
				reason.append(String.format("Unable to load Space dimension %d, aborting jump.",
				                            dimensionIdSpace));
				return false;
			}
			
			// update movement vector
			final VectorI vEntry = celestialObject.getEntryOffset();
			moveX = vEntry.x;
			moveZ = vEntry.z;
		}
		break;
		
		case HYPERSPACE_ENTERING: {
			// anything defined?
			if (celestialObjectSource.parent == null) {
				reason.append(String.format("Unable to reach hyperspace!\nThere's no parent defined for current dimension %s (%d).",
				                            sourceWorld.provider.getDimensionName(), sourceWorld.provider.dimensionId));
				return false;
			}
			// (target world border is checked systematically after movement checks)
			
			// is world available?
			final int dimensionIdHyperspace = celestialObjectSource.parent.dimensionId;
			targetWorld = MinecraftServer.getServer().worldServerForDimension(dimensionIdHyperspace);
			if (targetWorld == null) {
				reason.append(String.format("Unable to load Hyperspace dimension %d, aborting jump.",
				                            dimensionIdHyperspace));
				return false;
			}
			
			// update movement vector
			final VectorI vEntry = celestialObjectSource.getEntryOffset();
			moveX = -vEntry.x;
			moveZ = -vEntry.z;
		}
		break;
		
		case PLANET_TAKEOFF: {
			// anything defined?
			if (celestialObjectSource.parent == null) {
				reason.append(String.format("Unable to take off!\nThere's no parent defined for current dimension %s (%d).",
				                            sourceWorld.provider.getDimensionName(), sourceWorld.provider.dimensionId));
				return false;
			}
			
			// are we clear for transit?
			final double distanceSquared = celestialObjectSource.getSquareDistanceOutsideBorder(ship.coreX, ship.coreZ);
			if (distanceSquared > 0) {
				final AxisAlignedBB axisAlignedBB = celestialObjectSource.getAreaToReachParent();
				reason.append(String.format(
						"Ship is outside planet border, unable to reach space!\n"
						+ "Closest transition area is ~%d m away (%d %d %d) to (%d %d %d).",
						(int) Math.sqrt(distanceSquared),
						(int) axisAlignedBB.minX, (int) axisAlignedBB.minY, (int) axisAlignedBB.minZ,
						(int) axisAlignedBB.maxX, (int) axisAlignedBB.maxY, (int) axisAlignedBB.maxZ));
				return false;
			}
			
			// is world available?
			final int dimensionIdSpace = celestialObjectSource.parent.dimensionId;
			targetWorld = MinecraftServer.getServer().worldServerForDimension(dimensionIdSpace);
			if (targetWorld == null) {
				reason.append(String.format("Unable to load Space dimension %d, aborting jump.",
				                            dimensionIdSpace));
				return false;
			}
			
			// update movement vector
			final VectorI vEntry = celestialObjectSource.getEntryOffset();
			moveX = -vEntry.x;
			moveZ = -vEntry.z;
		}
		break;
		
		case PLANET_LANDING: {
			final CelestialObject celestialObject = CelestialObjectManager.getClosestChild(sourceWorld, ship.coreX, ship.coreZ);
			// anything defined?
			if (celestialObject == null) {
				reason.append("No planet exists in this dimension, there's nowhere to land!");
				return false;
			}
			
			// are we in orbit?
			final double distanceSquared = celestialObject.getSquareDistanceInParent(sourceWorld.provider.dimensionId, ship.coreX, ship.coreZ);
			if (distanceSquared > 0.0D) {
				AxisAlignedBB axisAlignedBB = celestialObject.getAreaInParent();
				reason.append(String.format(
						"No planet in range, unable to enter atmosphere!\n"
						+ "Closest planet is %d m away (%d %d %d) to (%d %d %d).",
						(int) Math.sqrt(distanceSquared),
						(int) axisAlignedBB.minX, (int) axisAlignedBB.minY, (int) axisAlignedBB.minZ,
						(int) axisAlignedBB.maxX, (int) axisAlignedBB.maxY, (int) axisAlignedBB.maxZ));
				return false;
			}
			
			// validate world availability
			targetWorld = MinecraftServer.getServer().worldServerForDimension(celestialObject.dimensionId);
			if (targetWorld == null) {
				reason.append(String.format("Sorry, we can't land here. Dimension %d isn't defined. It might be a decorative planet or a server misconfiguration",
				                            celestialObject.dimensionId));
				return false;
			}
			
			// update movement vector
			final VectorI vEntry = celestialObject.getEntryOffset();
			moveX = vEntry.x;
			moveZ = vEntry.z;
		}
		break;
			
		case SPACE_MOVING:
		case HYPERSPACE_MOVING:
		case PLANET_MOVING:
			targetWorld = sourceWorld;
			break;
			
		case GATE_ACTIVATING:
			// @TODO Jumpgate reimplementation
		default:
			reason.append(String.format("Invalid movement type %s", shipMovementType));
			return false;
		}
		
		return true;
	}
	
	private void state_moveBlocks() {
		LocalProfiler.start("Jump.moveBlocks");
		int blocksToMove = Math.min(WarpDriveConfig.G_BLOCKS_PER_TICK, ship.jumpBlocks.length - actualIndexInShip);
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Moving ship blocks " + actualIndexInShip + " to " + (actualIndexInShip + blocksToMove - 1) + " / " + (ship.jumpBlocks.length - 1));
		}
		
		for (int index = 0; index < blocksToMove; index++) {
			if (actualIndexInShip >= ship.jumpBlocks.length) {
				break;
			}
			
			JumpBlock jumpBlock = ship.jumpBlocks[actualIndexInShip];
			if (jumpBlock != null) {
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					WarpDrive.logger.info("Deploying from " + jumpBlock.x + " " + jumpBlock.y + " " + jumpBlock.z + " of " + jumpBlock.block + "@" + jumpBlock.blockMeta);
				}
				jumpBlock.deploy(targetWorld, transformation);
				
				sourceWorld.removeTileEntity(jumpBlock.x, jumpBlock.y, jumpBlock.z);
			}
			actualIndexInShip++;
		}
		
		LocalProfiler.stop();
	}
	
	private void state_moveExternals() {
		LocalProfiler.start("Jump.moveExternals");
		int blocksToMove = Math.min(WarpDriveConfig.G_BLOCKS_PER_TICK, ship.jumpBlocks.length - actualIndexInShip);
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Moving ship externals from " + actualIndexInShip + " / " + (ship.jumpBlocks.length - 1));
		}
		int index = 0;
		while (index < blocksToMove && actualIndexInShip < ship.jumpBlocks.length) {
			final JumpBlock jumpBlock = ship.jumpBlocks[ship.jumpBlocks.length - actualIndexInShip - 1];
			if (jumpBlock == null) {
				if (WarpDriveConfig.LOGGING_JUMP) {
					WarpDrive.logger.info(this + " Moving ship externals: unexpected null found at ship[" + actualIndexInShip + "]");
				}
				actualIndexInShip++;
				continue;
			}
			
			if (jumpBlock.externals != null) {
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					WarpDrive.logger.info("Moving externals for block " + jumpBlock.block + "@" + jumpBlock.blockMeta + " at " + jumpBlock.x + " " + jumpBlock.y + " " + jumpBlock.z);
				}
				for (Entry<String, NBTBase> external : jumpBlock.externals.entrySet()) {
					final IBlockTransformer blockTransformer = WarpDriveConfig.blockTransformers.get(external.getKey());
					if (blockTransformer != null) {
						blockTransformer.removeExternals(sourceWorld, jumpBlock.x, jumpBlock.y, jumpBlock.z,
						                                 jumpBlock.block, jumpBlock.blockMeta, jumpBlock.blockTileEntity);
						
						final ChunkCoordinates target = transformation.apply(jumpBlock.x, jumpBlock.y, jumpBlock.z);
						final TileEntity newTileEntity = jumpBlock.blockTileEntity == null ? null : targetWorld.getTileEntity(target.posX, target.posY, target.posZ);
						blockTransformer.restoreExternals(targetWorld, target.posX, target.posY, target.posZ,
						                                  jumpBlock.block, jumpBlock.blockMeta, newTileEntity, transformation, external.getValue());
					}
				}
				index++;
			}
			actualIndexInShip++;
		}
		LocalProfiler.stop();
	}
	
	private void state_moveEntities() {
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Moving entities");
		}
		LocalProfiler.start("Jump.moveEntities");
		
		if (ship.entitiesOnShip != null) {
			for (MovingEntity me : ship.entitiesOnShip) {
				Entity entity = me.entity;
				
				if (entity == null) {
					continue;
				}
				
				double oldEntityX = me.oldX;
				double oldEntityY = me.oldY;
				double oldEntityZ = me.oldZ;
				Vec3 target = transformation.apply(oldEntityX, oldEntityY, oldEntityZ);
				double newEntityX = target.xCoord;
				double newEntityY = target.yCoord;
				double newEntityZ = target.zCoord;
				
				if (WarpDriveConfig.LOGGING_JUMP) {
					WarpDrive.logger.info(String.format("Entity moving: (%.2f %.2f %.2f) -> (%.2f %.2f %.2f) entity %s",
							oldEntityX, oldEntityY, oldEntityZ,
							newEntityX, newEntityY, newEntityZ, entity.toString()));
				}
				
				// Travel to another dimension if needed
				if (betweenWorlds) {
					MinecraftServer server = MinecraftServer.getServer();
					WorldServer from = server.worldServerForDimension(sourceWorld.provider.dimensionId);
					WorldServer to = server.worldServerForDimension(targetWorld.provider.dimensionId);
					SpaceTeleporter teleporter = new SpaceTeleporter(to, 0,
							MathHelper.floor_double(newEntityX),
							MathHelper.floor_double(newEntityY),
							MathHelper.floor_double(newEntityZ));
					
					if (entity instanceof EntityPlayerMP) {
						EntityPlayerMP player = (EntityPlayerMP) entity;
						server.getConfigurationManager().transferPlayerToDimension(player, targetWorld.provider.dimensionId, teleporter);
						player.sendPlayerAbilities();
					} else {
						server.getConfigurationManager().transferEntityToWorld(entity, sourceWorld.provider.dimensionId, from, to, teleporter);
					}
				}
				
				// Update position
				transformation.rotate(entity);
				if (entity instanceof EntityPlayerMP) {
					EntityPlayerMP player = (EntityPlayerMP) entity;
					
					ChunkCoordinates bedLocation = player.getBedLocation(sourceWorld.provider.dimensionId);
					
					if (bedLocation != null
					  && ship.minX <= bedLocation.posX && ship.maxX >= bedLocation.posX
					  && ship.minY <= bedLocation.posY && ship.maxY >= bedLocation.posY
					  && ship.minZ <= bedLocation.posZ && ship.maxZ >= bedLocation.posZ) {
						bedLocation = transformation.apply(bedLocation);
						player.setSpawnChunk(bedLocation, false, targetWorld.provider.dimensionId);
					}
					player.setPositionAndUpdate(newEntityX, newEntityY, newEntityZ);
				} else {
					entity.setPosition(newEntityX, newEntityY, newEntityZ);
				}
			}
		}
		
		LocalProfiler.stop();
	}
	
	private void state_removeBlocks() {
		LocalProfiler.start("Jump.removeBlocks");
		int blocksToMove = Math.min(WarpDriveConfig.G_BLOCKS_PER_TICK, ship.jumpBlocks.length - actualIndexInShip);
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Removing ship blocks " + actualIndexInShip + " to " + (actualIndexInShip + blocksToMove - 1) + " / " + (ship.jumpBlocks.length - 1));
		}
		for (int index = 0; index < blocksToMove; index++) {
			if (actualIndexInShip >= ship.jumpBlocks.length) {
				break;
			}
			JumpBlock jumpBlock = ship.jumpBlocks[ship.jumpBlocks.length - actualIndexInShip - 1];
			if (jumpBlock == null) {
				if (WarpDriveConfig.LOGGING_JUMP) {
					WarpDrive.logger.info(this + " Removing ship part: unexpected null found at ship[" + actualIndexInShip + "]");
				}
				actualIndexInShip++;
				continue;
			}
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info("Removing block " + jumpBlock.block + "@" + jumpBlock.blockMeta + " at " + jumpBlock.x + " " + jumpBlock.y + " " + jumpBlock.z);
			}
			
			if (jumpBlock.blockTileEntity != null) {
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					WarpDrive.logger.info("Removing tile entity at " + jumpBlock.x + " " + jumpBlock.y + " " + jumpBlock.z);
				}
				sourceWorld.removeTileEntity(jumpBlock.x, jumpBlock.y, jumpBlock.z);
			}
			try {
				JumpBlock.setBlockNoLight(sourceWorld, jumpBlock.x, jumpBlock.y, jumpBlock.z, Blocks.air, 0, 2);
			} catch (Exception exception) {
				WarpDrive.logger.info("Exception while removing " + jumpBlock.block + "@" + jumpBlock.blockMeta + " at " + jumpBlock.x + " " + jumpBlock.y + " " + jumpBlock.z);
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					exception.printStackTrace();
				}
			}
			
			ChunkCoordinates target = transformation.apply(jumpBlock.x, jumpBlock.y, jumpBlock.z); 
			JumpBlock.refreshBlockStateOnClient(targetWorld, target.posX, target.posY, target.posZ);
			
			actualIndexInShip++;
		}
		LocalProfiler.stop();
	}
	
	private void state_chunkUnloading() {
		LocalProfiler.start("Jump.chunkUnloading");
		
		unforceChunks();
		
		LocalProfiler.stop();
	}
	
	/**
	 * Finishing jump: cleanup, collision effects and delete self
	 **/
	@SuppressWarnings("unchecked")
	private void state_finishing() {
		LocalProfiler.start("Jump.finishing()");
		// FIXME TileEntity duplication workaround
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Jump done in " + ((System.currentTimeMillis() - msCounter) / 1000F) + " seconds and " + ticks + " ticks");
		}
		final int countBefore = targetWorld.loadedTileEntityList.size();
		
		try {
			targetWorld.loadedTileEntityList = removeDuplicates(targetWorld.loadedTileEntityList);
		} catch (Exception exception) {
			if (WarpDriveConfig.LOGGING_JUMP) {
				WarpDrive.logger.info("TE Duplicates removing exception: " + exception.getMessage());
				exception.printStackTrace();
			}
		}
		
		doCollisionDamage(true);
		
		disable("Jump done");
		final int countAfter = targetWorld.loadedTileEntityList.size();
		if (WarpDriveConfig.LOGGING_JUMP && countBefore != countAfter) {
			WarpDrive.logger.info(String.format("Removing TE duplicates: tileEntities in target world after jump, cleanup %d -> %d",
			                      countBefore, countAfter));
		}
		LocalProfiler.stop();
	}
	
	private String getPossibleJumpDistance() {
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Calculating possible jump distance...");
		}
		int originalRange = Math.max(Math.abs(moveX), Math.max(Math.abs(moveY), Math.abs(moveZ)));
		int testRange = originalRange;
		int blowPoints = 0;
		collisionDetected = false;
		
		CheckMovementResult result;
		String firstAdjustmentReason = "";
		while (testRange >= 0) {
			// Is there enough space in destination point?
			result = checkMovement(testRange / (double)originalRange, false);
			if (result == null) {
				break;
			}
			if (firstAdjustmentReason.isEmpty()) {
				firstAdjustmentReason = result.reason;
			}
			
			if (result.isCollision) {
				blowPoints++;
			}
			testRange--;
		}
		VectorI finalMovement = getMovementVector(testRange / (double)originalRange);
		
		if (originalRange != testRange && WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Jump range adjusted from " + originalRange + " to " + testRange + " after " + blowPoints + " collisions");
		}
		
		// Register explosion(s) at collision point
		if (blowPoints > WarpDriveConfig.SHIP_COLLISION_TOLERANCE_BLOCKS) {
			result = checkMovement(Math.min(1.0D, Math.max(0.0D, (testRange + 1) / (double)originalRange)), true);
			if (result != null) {
				/*
				 * Strength scaling:
				 * Wither skull = 1
				 * Creeper = 3 or 6
				 * TNT = 4
				 * TNT cart = 4 to 11.5
				 * Wither boom = 5
				 * Endercrystal = 6
				 */
				float massCorrection = 0.5F
						+ (float) Math.sqrt(Math.min(1.0D, Math.max(0.0D, ship.shipCore.shipMass - WarpDriveConfig.SHIP_VOLUME_MAX_ON_PLANET_SURFACE)
								/ WarpDriveConfig.SHIP_VOLUME_MIN_FOR_HYPERSPACE));
				collisionDetected = true;
				collisionStrength = (4.0F + blowPoints - WarpDriveConfig.SHIP_COLLISION_TOLERANCE_BLOCKS) * massCorrection;
				collisionAtSource = result.atSource;
				collisionAtTarget = result.atTarget;
				WarpDrive.logger.info(this + " Reporting " + collisionAtTarget.size() + " collisions points after " + blowPoints
							+ " blowPoints with " + String.format("%.2f", massCorrection) + " ship mass correction => "
							+ String.format("%.2f", collisionStrength) + " explosion strength");
			} else {
				WarpDrive.logger.error("WarpDrive error: unable to compute collision points, ignoring...");
			}
		}
		
		// Update movement after computing collision points 
		moveX = finalMovement.x;
		moveY = finalMovement.y;
		moveZ = finalMovement.z;
		return firstAdjustmentReason;
	}
	
	private void doCollisionDamage(boolean atTarget) {
		if (!collisionDetected) {
			if (WarpDriveConfig.LOGGING_JUMP) {
				WarpDrive.logger.info(this + " doCollisionDamage No collision detected...");
			}
			return;
		}
		ArrayList<Vector3> collisionPoints = atTarget ? collisionAtTarget : collisionAtSource;
		Vector3 min = collisionPoints.get(0).clone();
		Vector3 max = collisionPoints.get(0).clone();
		for (Vector3 v : collisionPoints) {
			if (min.x > v.x) {
				min.x = v.x;
			} else if (max.x < v.x) {
				max.x = v.x;
			}
			if (min.y > v.y) {
				min.y = v.y;
			} else if (max.y < v.y) {
				max.y = v.y;
			}
			if (min.z > v.z) {
				min.z = v.z;
			} else if (max.z < v.z) {
				max.z = v.z;
			}
		}
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Ship collision from " + min + " to " + max);
		}
		
		// inform players on board
		double rx = Math.round(min.x + sourceWorld.rand.nextInt(Math.max(1, (int) (max.x - min.x))));
		double ry = Math.round(min.y + sourceWorld.rand.nextInt(Math.max(1, (int) (max.y - min.y))));
		double rz = Math.round(min.z + sourceWorld.rand.nextInt(Math.max(1, (int) (max.z - min.z))));
		ship.messageToAllPlayersOnShip("Ship collision detected around " + (int) rx + ", " + (int) ry + ", " + (int) rz + ". Damage report pending...");
		
		// randomize if too many collision points
		int nbExplosions = Math.min(5, collisionPoints.size());
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info("doCollisionDamage nbExplosions " + nbExplosions + "/" + collisionPoints.size());
		}
		for (int i = 0; i < nbExplosions; i++) {
			// get location
			Vector3 current;
			if (nbExplosions < collisionPoints.size()) {
				if (WarpDriveConfig.LOGGING_JUMP) {
					WarpDrive.logger.info("doCollisionDamage random #" + i);
				}
				current = collisionPoints.get(sourceWorld.rand.nextInt(collisionPoints.size()));
			} else {
				if (WarpDriveConfig.LOGGING_JUMP) {
					WarpDrive.logger.info("doCollisionDamage get " + i);
				}
				current = collisionPoints.get(i);
			}
			
			// compute explosion strength with a jitter, at least 1 TNT
			float strength = Math.max(4.0F, collisionStrength / nbExplosions - 2.0F + 2.0F * sourceWorld.rand.nextFloat());
			
			(atTarget ? targetWorld : sourceWorld).newExplosion(null, current.x, current.y, current.z, strength, atTarget, atTarget);
			WarpDrive.logger.info("Ship collision caused explosion at " + current.x + " " + current.y + " " + current.z + " with strength " + strength);
		}
	}
	
	private void restoreEntitiesPosition() {
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Restoring entities position");
		}
		LocalProfiler.start("Jump.restoreEntitiesPosition");
		
		if (ship.entitiesOnShip != null) {
			for (MovingEntity movingEntity : ship.entitiesOnShip) {
				Entity entity = movingEntity.entity;
				
				if (entity == null) {
					continue;
				}
				
				if (WarpDriveConfig.LOGGING_JUMP) {
					WarpDrive.logger.info("Entity restoring position at (" + movingEntity.oldX + " " + movingEntity.oldY + " " + movingEntity.oldZ + ")");
				}
				
				// Update position
				if (entity instanceof EntityPlayerMP) {
					EntityPlayerMP player = (EntityPlayerMP) entity;
					
					player.setPositionAndUpdate(movingEntity.oldX, movingEntity.oldY, movingEntity.oldZ);
				} else {
					entity.setPosition(movingEntity.oldX, movingEntity.oldY, movingEntity.oldZ);
				}
			}
		}
		
		LocalProfiler.stop();
	}
	
	private class CheckMovementResult {
		final ArrayList<Vector3> atSource;
		final ArrayList<Vector3> atTarget;
		boolean isCollision = false;
		public String reason = "";
		
		CheckMovementResult() {
			atSource = new ArrayList<>(1);
			atTarget = new ArrayList<>(1);
			isCollision = false;
			reason = "Unknown reason";
		}
		
		public void add(double sx, double sy, double sz, double tx, double ty, double tz, boolean pisCollision, String preason) {
			atSource.add(new Vector3(sx, sy, sz));
			atTarget.add(new Vector3(tx, ty, tz));
			isCollision = isCollision || pisCollision;
			reason = preason;
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info("CheckMovementResult " + sx + ", " + sy + ", " + sz + " -> " + tx + ", " + ty + ", " + tz + " " + isCollision + " '" + reason + "'");
			}
		}
	}
	
	private CheckMovementResult checkCollisionAndProtection(ITransformation transformation, final boolean fullCollisionDetails, final String context) {
		CheckMovementResult result = new CheckMovementResult();
		VectorI offset = new VectorI((int)Math.signum(moveX), (int)Math.signum(moveY), (int)Math.signum(moveZ));
		
		int x, y, z;
		ChunkCoordinates coordTarget;
		ChunkCoordinates coordCoreAtTarget = transformation.apply(ship.coreX, ship.coreY, ship.coreZ);
		Block blockSource;
		Block blockTarget;
		for (y = ship.minY; y <= ship.maxY; y++) {
			for (x = ship.minX; x <= ship.maxX; x++) {
				for (z = ship.minZ; z <= ship.maxZ; z++) {
					coordTarget = transformation.apply(x, y, z);
					blockSource = sourceWorld.getBlock(x, y, z);
					blockTarget = targetWorld.getBlock(coordTarget.posX, coordTarget.posY, coordTarget.posZ);
					if (Dictionary.BLOCKS_ANCHOR.contains(blockTarget)) {
						result.add(x, y, z,
						           coordTarget.posX + 0.5D - offset.x,
						           coordTarget.posY + 0.5D - offset.y,
						           coordTarget.posZ + 0.5D - offset.z,
						           true,
						           String.format("Impassable %s detected at destination (%d %d %d)", 
						                         blockTarget.getLocalizedName(), coordTarget.posX, coordTarget.posY, coordTarget.posZ) );
						if (!fullCollisionDetails) {
							return result;
						} else if (WarpDriveConfig.LOGGING_JUMP) {
							WarpDrive.logger.info("Anchor collision at " + context);
						}
					}
					
					if ( blockSource != Blocks.air
					  && !Dictionary.BLOCKS_EXPANDABLE.contains(blockSource)
					  && blockTarget != Blocks.air
					  && !Dictionary.BLOCKS_EXPANDABLE.contains(blockTarget)) {
						result.add(x, y, z,
						           coordTarget.posX + 0.5D + offset.x * 0.1D,
						           coordTarget.posY + 0.5D + offset.y * 0.1D,
						           coordTarget.posZ + 0.5D + offset.z * 0.1D,
						           true,
						           String.format("Obstacle %s detected at (%d %d %d)",
						                         blockTarget.getLocalizedName(), coordTarget.posX, coordTarget.posY, coordTarget.posZ) );
						if (!fullCollisionDetails) {
							return result;
						} else if (WarpDriveConfig.LOGGING_JUMP) {
							WarpDrive.logger.info("Hard collision at " + context);
						}
					}
					
					if (blockSource != Blocks.air && CommonProxy.isBlockPlaceCanceled(null, coordCoreAtTarget.posX, coordCoreAtTarget.posY, coordCoreAtTarget.posZ,
						targetWorld, coordTarget.posX, coordTarget.posY, coordTarget.posZ, blockSource, 0)) {
						result.add(x, y, z,
						           coordTarget.posX,
						           coordTarget.posY,
						           coordTarget.posZ,
						           false,
							       String.format("Ship is entering a protected area at (%d %d %d)",
							                     coordTarget.posX, coordTarget.posY, coordTarget.posZ) );
						return result;
					}
				}
			}
		}
		
		if (fullCollisionDetails && result.isCollision) {
			return result;
		} else {
			return null;
		}
	}
	
	private CheckMovementResult checkMovement(final double ratio, final boolean fullCollisionDetails) {
		CheckMovementResult result = new CheckMovementResult();
		VectorI testMovement = getMovementVector(ratio);
		if ((moveY > 0 && ship.maxY + testMovement.y > 255) && !betweenWorlds) {
			result.add(ship.coreX, ship.maxY + testMovement.y,
				ship.coreZ, ship.coreX + 0.5D,
				ship.maxY + testMovement.y + 1.0D,
				ship.coreZ + 0.5D,
				false, "Ship core is moving too high");
			return result;
		}
		
		if ((moveY < 0 && ship.minY + testMovement.y <= 8) && !betweenWorlds) {
			result.add(ship.coreX, ship.minY + testMovement.y, ship.coreZ,
				ship.coreX + 0.5D,
				ship.maxY + testMovement.y,
				ship.coreZ + 0.5D,
				false, "Ship core is moving too low");
			return result;
		}
		
		ITransformation testTransformation = new Transformation(ship, targetWorld, testMovement.x, testMovement.y, testMovement.z, rotationSteps);
		return checkCollisionAndProtection(testTransformation, fullCollisionDetails, "ratio " + ratio + " testMovement " + testMovement);
	}
	
	private VectorI getMovementVector(final double ratio) {
		return new VectorI((int)Math.round(moveX * ratio), (int)Math.round(moveY * ratio), (int)Math.round(moveZ * ratio));
	}
	
	private static ArrayList<Object> removeDuplicates(List<TileEntity> l) {
		@SuppressWarnings("Convert2Lambda")
		Set<TileEntity> s = new TreeSet<>(new Comparator<TileEntity>() {
			@Override
			public int compare(TileEntity o1, TileEntity o2) {
				if (o1.xCoord == o2.xCoord && o1.yCoord == o2.yCoord && o1.zCoord == o2.zCoord) {
					if (WarpDriveConfig.LOGGING_JUMP) {
						WarpDrive.logger.warn(String.format("Removing TE duplicates: detected duplicate in %s @ %d %d %d: %s vs %s",
						                                    o1.getWorldObj().provider.getDimensionName(),
						                                    o1.xCoord, o1.yCoord, o1.zCoord,
						                                    o1, o2));
						final NBTTagCompound nbtTagCompound1 = new NBTTagCompound();
						o1.writeToNBT(nbtTagCompound1);
						final NBTTagCompound nbtTagCompound2 = new NBTTagCompound();
						o2.writeToNBT(nbtTagCompound2);
						WarpDrive.logger.warn(String.format("First  NBT is %s", nbtTagCompound1));
						WarpDrive.logger.warn(String.format("Second NBT is %s", nbtTagCompound2));
					}
					return 0;
				} else {
					return 1;
				}
			}
		});
		s.addAll(l);
		return new ArrayList<>(Arrays.asList(s.toArray()));
	}
	
	@Override
	protected void readFromNBT(NBTTagCompound nbttagcompound) {
		WarpDrive.logger.error(this + " readFromNBT()");
	}
	
	@Override
	protected void writeToNBT(NBTTagCompound nbttagcompound) {
		WarpDrive.logger.error(this + " writeToNBT()");
	}
	
	@Override
	public String toString() {
		return String.format("%s/%d \'%s\' @ \'%s\' (%d %d %d) #%d",
			getClass().getSimpleName(), hashCode(),
			(ship == null || ship.shipCore == null) ? "~NULL~" : (ship.shipCore.uuid + ":" + ship.shipCore.shipName),
			sourceWorld == null ? "~NULL~" : sourceWorld.getWorldInfo().getWorldName(),
			ship == null ? -1 : ship.coreX, ship == null ? -1 : ship.coreY, ship == null ? -1 : ship.coreZ,
			ticks);
	}
}

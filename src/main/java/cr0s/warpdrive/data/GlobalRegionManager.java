package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.block.atomic.BlockAcceleratorCore;
import cr0s.warpdrive.block.detection.BlockVirtualAssistant;
import cr0s.warpdrive.block.detection.TileEntityVirtualAssistant;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.block.movement.BlockTransporterCore;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

import org.apache.commons.lang3.text.WordUtils;

/**
 * Registry of all known ships and other global regions in the world.
 * 
 * @author LemADEC
 */
public class GlobalRegionManager {
	
	public static String GALAXY_UNDEFINED = "???";
	
	private static final HashMap<Integer, CopyOnWriteArraySet<GlobalRegion>> registry = new HashMap<>();
	private static final HashMap<Integer, HashMap<EnumGlobalRegionType, CopyOnWriteArraySet<GlobalRegion>>> registryByType = new HashMap<>();
	private static final HashMap<Integer, HashMap<Long, CopyOnWriteArraySet<GlobalRegion>>> registryByChunk = new HashMap<>();
	private static final HashMap<UUID, GlobalRegion> registryByUUID = new HashMap<>();
	private static final HashMap<String, CopyOnWriteArraySet<GlobalRegion>> registryByName = new HashMap<>();
	private static int countAdd = 0;
	private static int countRemove = 0;
	private static int countRead = 0;
	
	public static void updateInRegistry(@Nonnull final IGlobalRegionProvider globalRegionProvider) {
		// validate context
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.error(String.format("Non-threadsafe call to GlobalRegionManager:updateInRegistry outside main thread, for %s",
			                                     globalRegionProvider ));
			return;
		}
		if (globalRegionProvider.getSignatureUUID() == null) {
			WarpDrive.logger.error(String.format("Ignoring invalid IGlobalRegionProvider with no UUID %s",
			                                     globalRegionProvider ));
			return;
		}
		
		// update statistics
		countRead++;
		if (WarpDriveConfig.LOGGING_GLOBAL_REGION_REGISTRY) {
			if (countRead % 1000 == 0) {
				WarpDrive.logger.info(String.format("Global region registry stats: read %d add %d remove %d => %.2f%% read",
				                                    countRead, countAdd, countRemove, ((float) countRead) / (countRemove + countRead + countAdd)));
			}
		}
		
		// get dimension
		CopyOnWriteArraySet<GlobalRegion> setRegistryItems = registry.get(globalRegionProvider.getDimension());
		if (setRegistryItems == null) {
			setRegistryItems = new CopyOnWriteArraySet<>();
		}
		
		// get entry
		final ArrayList<GlobalRegion> listToRemove = new ArrayList<>(3);
		final UUID uuidTileEntity = globalRegionProvider.getSignatureUUID();
		for (final GlobalRegion registryItem : setRegistryItems) {
			if (registryItem.uuid == null) {
				WarpDrive.logger.error(String.format("Removing invalid IGlobalRegionProvider %s",
				                                     registryItem));
				listToRemove.add(registryItem);
				continue;
			}
			
			if (registryItem.sameCoordinates(globalRegionProvider)) {
				if ( registryItem.type.equals(globalRegionProvider.getGlobalRegionType())
				  && registryItem.uuid.equals(uuidTileEntity) ) {// already registered
					// note: in-place update only works as long as hashcode remains unchanged, that means same position, same type, same UUID
					registryItem.update(globalRegionProvider);
					setRegistryItems.removeAll(listToRemove);
					rebuildIndexes();
					if (WarpDriveConfig.LOGGING_GLOBAL_REGION_REGISTRY) {
						printRegistry("updated");
					}
					return;
				} else {
					listToRemove.add(registryItem);
				}
			}
		}
		setRegistryItems.removeAll(listToRemove);
		
		// not found => add
		countAdd++;
		setRegistryItems.add(new GlobalRegion(globalRegionProvider));
		registry.put(globalRegionProvider.getDimension(), setRegistryItems);
		rebuildIndexes();
		if (WarpDriveConfig.LOGGING_GLOBAL_REGION_REGISTRY) {
			printRegistry("added");
		}
	}
	
	public static void removeFromRegistry(@Nonnull final IGlobalRegionProvider globalRegionProvider) {
		
		countRead++;
		final Set<GlobalRegion> setRegistryItems = registry.get(globalRegionProvider.getDimension());
		if (setRegistryItems == null) {
			// noting to remove
			return;
		}
		
		for (final GlobalRegion registryItem : setRegistryItems) {
			if (registryItem.sameCoordinates(globalRegionProvider)) {
				// found it, remove and exit
				countRemove++;
				setRegistryItems.remove(registryItem);
				rebuildIndexes();
				return;
			}
		}
		// not found => ignore it
	}
	
	@Nullable
	public static GlobalRegion getByName(final EnumGlobalRegionType enumGlobalRegionType, final String name) {
		final Set<GlobalRegion> setByName = registryByName.get(name == null ? "" : name);
		if (setByName != null) {
			for (final GlobalRegion globalRegion : setByName) {
				if ( enumGlobalRegionType == null
				  || globalRegion.type == enumGlobalRegionType ) {
					return globalRegion;
				}
			}
		}
		for (final Integer dimensionId : registry.keySet()) {
			final CopyOnWriteArraySet<GlobalRegion> setGlobalRegions = registry.get(dimensionId);
			if (setGlobalRegions == null) {
				continue;
			}
			
			for (final GlobalRegion globalRegion : setGlobalRegions) {
				if ( ( enumGlobalRegionType == null
				    || globalRegion.type == enumGlobalRegionType )
				  && globalRegion.name.equals(name) ) {
					return globalRegion;
				}
			}
		}
		return null;
	}
	
	@Nullable
	public static GlobalRegion getByUUID(final EnumGlobalRegionType enumGlobalRegionType, final UUID uuid) {
		if (uuid == null) {
			return null;
		}
		final GlobalRegion globalRegionCached = registryByUUID.get(uuid);
		if ( globalRegionCached != null
		  && ( enumGlobalRegionType == null
		    || globalRegionCached.type == enumGlobalRegionType ) ) {
			return globalRegionCached;
		}
		for (final Integer dimensionId : registry.keySet()) {
			final CopyOnWriteArraySet<GlobalRegion> setGlobalRegions = registry.get(dimensionId);
			if (setGlobalRegions == null) {
				continue;
			}
			
			for (final GlobalRegion globalRegion : setGlobalRegions) {
				if ( ( enumGlobalRegionType == null
				    || globalRegion.type == enumGlobalRegionType )
				  && globalRegion.uuid.equals(uuid) ) {
					return globalRegion;
				}
			}
		}
		return null;
	}
	
	public static String listByKeyword(final EnumGlobalRegionType enumGlobalRegionType, final String keyword) {
		final int MAX_LENGTH = 2000;
		final StringBuilder resultMatch = new StringBuilder();
		final StringBuilder resultCaseInsensitive = new StringBuilder();
		final StringBuilder resultContains = new StringBuilder();
		for (final Integer dimensionId : registry.keySet()) {
			final CopyOnWriteArraySet<GlobalRegion> setGlobalRegions = registry.get(dimensionId);
			if (setGlobalRegions == null) {
				continue;
			}
			
			for (final GlobalRegion globalRegion : setGlobalRegions) {
				if (globalRegion.type == enumGlobalRegionType) {
					if (globalRegion.name.equals(keyword)) {
						if (resultMatch.length() < MAX_LENGTH) {
							if (resultMatch.length() > 0) {
								resultMatch.append("\n");
							}
							resultContains.append(String.format("%s '%s' found in %s",
							                                    WordUtils.capitalize(enumGlobalRegionType.getName()),
							                                    globalRegion.name,
							                                    globalRegion.getFormattedLocation()));
						} else {
							resultMatch.append(".");
						}
					} else if (globalRegion.name.equalsIgnoreCase(keyword)) {
						if (resultCaseInsensitive.length() < MAX_LENGTH) {
							if (resultCaseInsensitive.length() > 0) {
								resultCaseInsensitive.append("\n");
							}
							resultContains.append(String.format("%s '%s' found in %s",
							                                    WordUtils.capitalize(enumGlobalRegionType.getName()),
							                                    globalRegion.name,
							                                    globalRegion.getFormattedLocation()));
						} else {
							resultCaseInsensitive.append(".");
						}
					} else if (globalRegion.name.contains(keyword)) {
						if (resultContains.length() < MAX_LENGTH) {
							if (resultContains.length() > 0) {
								resultContains.append("\n");
							}
							resultContains.append(String.format("%s '%s' found in %s",
							                                    WordUtils.capitalize(enumGlobalRegionType.getName()),
							                                    globalRegion.name,
							                                    globalRegion.getFormattedLocation()));
						} else {
							resultContains.append(".");
						}
					}
				}
			}
		}
		
		if (resultMatch.length() > 0) {
			return resultMatch.toString();
		}
		if (resultCaseInsensitive.length() > 0) {
			return resultCaseInsensitive.toString();
		}
		if (resultContains.length() > 0) {
			return resultContains.toString();
		}
		return String.format("No %s found with name '%s'",
		                     enumGlobalRegionType.getName(), keyword );
	}
	
	@Nullable
	public static GlobalRegion getNearest(final EnumGlobalRegionType enumGlobalRegionType, @Nonnull final World world, @Nonnull final BlockPos blockPos) {
		final Set<GlobalRegion> setGlobalRegions = getRegionsByType(world.provider.getDimension(), enumGlobalRegionType);
		if (setGlobalRegions == null) {
			return null;
		}
		
		double distanceSquared_min = Double.MAX_VALUE;
		GlobalRegion result = null;
		for (final GlobalRegion globalRegion : setGlobalRegions) {
			if ( enumGlobalRegionType != null
			  && globalRegion.type != enumGlobalRegionType ) {
				continue;
			}
			
			final double dX = globalRegion.x - blockPos.getX();
			final double dY = globalRegion.y - blockPos.getY();
			final double dZ = globalRegion.z - blockPos.getZ();
			final double distanceSquared = dX * dX + dY * dY + dZ * dZ;
			
			if ( distanceSquared < distanceSquared_min) {
				distanceSquared_min = distanceSquared;
				result = globalRegion;
			}
		}
		
		return result;
	}
	
	@Nonnull
	public static ArrayList<GlobalRegion> getContainers(final EnumGlobalRegionType enumGlobalRegionType, @Nonnull final World world, @Nonnull final BlockPos blockPos) {
		final Set<GlobalRegion> setGlobalRegions = getRegionsByChunk(world.provider.getDimension(), blockPos);
		if (setGlobalRegions == null) {
			return new ArrayList<>(0);
		}
		
		final ArrayList<GlobalRegion> listContainers = new ArrayList<>(5);
		for (final GlobalRegion globalRegion : setGlobalRegions) {
			if ( enumGlobalRegionType != null
			  && globalRegion.type != enumGlobalRegionType ) {
				continue;
			}
			
			if (!globalRegion.contains(blockPos)) {
				continue;
			}
			
			// sanitize tile entity
			// note: since player is in range, we can safely load the tile entity
			final TileEntity tileEntity = world.getTileEntity(globalRegion.getBlockPos());
			if (!(tileEntity instanceof IGlobalRegionProvider)) {
				cleanup();
				continue;
			}
			
			listContainers.add(globalRegion);
		}
		
		return listContainers;
	}
	
	public static boolean onBlockUpdating(@Nullable final Entity entity, @Nonnull final World world, @Nonnull final BlockPos blockPos, final IBlockState blockState) {
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.error(String.format("Non-threadsafe call to GlobalRegionManager:onBlockUpdating outside main thread, for %s %s",
			                                     blockState, Commons.format(world, blockPos) ));
			return false;
		}
		final Set<GlobalRegion> setGlobalRegions = getRegionsByChunk(world.provider.getDimension(), blockPos);
		if (setGlobalRegions == null) {
			return true;
		}
		boolean isAllowed = true;
		for (final GlobalRegion registryItem : setGlobalRegions) {
			if ( registryItem.contains(blockPos)
			  && !registryItem.getBlockPos().equals(blockPos) ) {
				final TileEntity tileEntity = world.getTileEntity(registryItem.getBlockPos());
				if (tileEntity instanceof IGlobalRegionProvider) {
					isAllowed = isAllowed && ((IGlobalRegionProvider) tileEntity).onBlockUpdatingInArea(entity, blockPos, blockState);
				}
			}
		}
		return isAllowed;
	}
	
	public static boolean onChatReceived(@Nonnull final EntityPlayer entityPlayer, @Nonnull final String message) {
		if (!Commons.isSafeThread()) {
			WarpDrive.logger.error(String.format("Non-threadsafe call to GlobalRegionManager:onChatReceived outside main thread, for %s %s",
			                                     entityPlayer, message ));
			return false;
		}
		final BlockPos blockPos = entityPlayer.getPosition();
		final Set<GlobalRegion> setGlobalRegions = getRegionsByChunk(entityPlayer.world.provider.getDimension(), blockPos);
		if (setGlobalRegions == null) {
			return true;
		}
		boolean isCancelled = false;
		for (final GlobalRegion registryItem : setGlobalRegions) {
			if (registryItem.type == EnumGlobalRegionType.VIRTUAL_ASSISTANT
			  && registryItem.contains(blockPos) ) {
				final TileEntity tileEntity = entityPlayer.world.getTileEntity(registryItem.getBlockPos());
				if (tileEntity instanceof TileEntityVirtualAssistant) {
					isCancelled = isCancelled || ((TileEntityVirtualAssistant) tileEntity).onChatReceived(entityPlayer, message);
				}
			}
		}
		return isCancelled;
	}
	
	@Nonnull
	public static ArrayList<RadarEcho> getRadarEchos(@Nonnull final TileEntity tileEntity, final int radius) {
		final ArrayList<RadarEcho> arrayListRadarEchos = new ArrayList<>(registry.size());
		cleanup();
		
		final CelestialObject celestialObject = CelestialObjectManager.get(tileEntity.getWorld(), tileEntity.getPos().getX(), tileEntity.getPos().getZ());
		final Vector3 vectorRadar = getUniversalCoordinates(
			celestialObject,
			tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());
		// printRegistry();
		final int radius2 = radius * radius;
		for (final Map.Entry<Integer, CopyOnWriteArraySet<GlobalRegion>> entryDimension : registry.entrySet()) {
			for (final GlobalRegion globalRegion : entryDimension.getValue()) {
				if (!globalRegion.type.hasRadarEcho()) {
					continue;
				}
				final Vector3 vectorItem = globalRegion.getUniversalCoordinates(tileEntity.getWorld().isRemote);
				if (vectorItem == null) {
					continue;
				}
				final double dX = vectorItem.x - vectorRadar.x;
				final double dY = vectorItem.y - vectorRadar.y;
				final double dZ = vectorItem.z - vectorRadar.z;
				final double distance2 = dX * dX + dY * dY + dZ * dZ;
				if (distance2 > radius2) {
					continue;
				}
				if (globalRegion.isolationRate != 0.0D
				    && tileEntity.getWorld().rand.nextDouble() < globalRegion.isolationRate) {
					continue;
				}
				
				arrayListRadarEchos.add( new RadarEcho(globalRegion.type.getName(),
				                                       vectorItem,
				                                       globalRegion.mass,
				                                       globalRegion.name));
			}
		}
		
		return arrayListRadarEchos;
	}
	
	public static String getGalaxyName(final CelestialObject celestialObject, final double x, final double y, final double z) {
		if (celestialObject == null) {
			// not a registered area
			return GALAXY_UNDEFINED;
		}
		CelestialObject celestialObjectNode = celestialObject;
		boolean hasHyperspace = celestialObjectNode.isHyperspace();
		while (celestialObjectNode.parent != null) {
			celestialObjectNode = celestialObjectNode.parent;
			hasHyperspace |= celestialObjectNode.isHyperspace();
		}
		return hasHyperspace ? celestialObjectNode.getDisplayName() : GALAXY_UNDEFINED;
	}
	
	public static Vector3 getUniversalCoordinates(final CelestialObject celestialObject, final double x, final double y, final double z) {
		if (celestialObject == null) {
			// not a registered area
			return null;
		}
		final Vector3 vec3Result = new Vector3(x, y + 512.0D, z);
		CelestialObject celestialObjectNode = celestialObject;
		boolean hasHyperspace = celestialObjectNode.isHyperspace();
		while (celestialObjectNode.parent != null) {
			final VectorI vEntry = celestialObjectNode.getEntryOffset();
			vec3Result.x -= vEntry.x;
			vec3Result.y -= 256.0D;
			vec3Result.z -= vEntry.z;
			celestialObjectNode = celestialObjectNode.parent;
			hasHyperspace |= celestialObjectNode.isHyperspace();
		}
		return hasHyperspace ? vec3Result : null;
	}

	@Nullable
	private static Set<GlobalRegion> getRegionsByType(final int dimensionId, @Nullable final EnumGlobalRegionType enumGlobalRegionType) {
		if (enumGlobalRegionType == null) {
			return registry.get(dimensionId);
		}
		final HashMap<EnumGlobalRegionType, CopyOnWriteArraySet<GlobalRegion>> mapByType = registryByType.get(dimensionId);
		return mapByType == null ? null : mapByType.get(enumGlobalRegionType);
	}

	@Nullable
	private static Set<GlobalRegion> getRegionsByChunk(final int dimensionId, @Nonnull final BlockPos blockPos) {
		final HashMap<Long, CopyOnWriteArraySet<GlobalRegion>> mapByChunk = registryByChunk.get(dimensionId);
		return mapByChunk == null ? null : mapByChunk.get(ChunkPos.asLong(blockPos.getX() >> 4, blockPos.getZ() >> 4));
	}

	private static void rebuildIndexes() {
		registryByType.clear();
		registryByChunk.clear();
		registryByUUID.clear();
		registryByName.clear();
		for (final Map.Entry<Integer, CopyOnWriteArraySet<GlobalRegion>> entryDimension : registry.entrySet()) {
			final int dimensionId = entryDimension.getKey();
			final HashMap<EnumGlobalRegionType, CopyOnWriteArraySet<GlobalRegion>> mapByType = new HashMap<>();
			final HashMap<Long, CopyOnWriteArraySet<GlobalRegion>> mapByChunk = new HashMap<>();
			for (final GlobalRegion globalRegion : entryDimension.getValue()) {
				if (globalRegion == null) {
					continue;
				}
				CopyOnWriteArraySet<GlobalRegion> setByType = mapByType.get(globalRegion.type);
				if (setByType == null) {
					setByType = new CopyOnWriteArraySet<>();
					mapByType.put(globalRegion.type, setByType);
				}
				setByType.add(globalRegion);
				if (globalRegion.uuid != null) {
					registryByUUID.put(globalRegion.uuid, globalRegion);
				}
				final String name = globalRegion.name == null ? "" : globalRegion.name;
				CopyOnWriteArraySet<GlobalRegion> setByName = registryByName.get(name);
				if (setByName == null) {
					setByName = new CopyOnWriteArraySet<>();
					registryByName.put(name, setByName);
				}
				setByName.add(globalRegion);
				for (int xChunk = globalRegion.minX >> 4; xChunk <= globalRegion.maxX >> 4; xChunk++) {
					for (int zChunk = globalRegion.minZ >> 4; zChunk <= globalRegion.maxZ >> 4; zChunk++) {
						final Long keyChunk = ChunkPos.asLong(xChunk, zChunk);
						CopyOnWriteArraySet<GlobalRegion> setByChunk = mapByChunk.get(keyChunk);
						if (setByChunk == null) {
							setByChunk = new CopyOnWriteArraySet<>();
							mapByChunk.put(keyChunk, setByChunk);
						}
						setByChunk.add(globalRegion);
					}
				}
			}
			registryByType.put(dimensionId, mapByType);
			registryByChunk.put(dimensionId, mapByChunk);
		}
	}
	
	public static void printRegistry(final String trigger) {
		WarpDrive.logger.info(String.format("Global region registry after %s:", trigger));
		
		for (final Map.Entry<Integer, CopyOnWriteArraySet<GlobalRegion>> entryDimension : registry.entrySet()) {
			final StringBuilder message = new StringBuilder();
			for (final GlobalRegion registryItem : entryDimension.getValue()) {
				message.append(String.format("\n- %s '%s' @ DIM%d (%d %d %d) with %.3f isolation rate",
				                             registryItem.type, registryItem.name,
				                             registryItem.dimensionId, registryItem.x, registryItem.y, registryItem.z,
				                             registryItem.isolationRate));
			}
			WarpDrive.logger.info(String.format("- %d entries in dimension %d: %s",
			                                    entryDimension.getValue().size(), entryDimension.getKey(), message ));
		}
	}
	
	@Nullable
	public static TileEntityShipCore getIntersectingShipCore(@Nonnull final TileEntityShipCore shipCore1) {
		cleanup();
		
		if (!shipCore1.isAssemblyValid()) {
			WarpDrive.logger.error(String.format("isShipCoreIntersectingWithOthers() with invalid ship %s, assuming intersection",
			                                     shipCore1));
			return null;
		}
		final AxisAlignedBB aabb1 = shipCore1.getGlobalRegionArea();		
		
		final CopyOnWriteArraySet<GlobalRegion> setRegistryItems = registry.get(shipCore1.getWorld().provider.getDimension());
		if (setRegistryItems == null) {
			return null;
		}
		for (final GlobalRegion registryItem : setRegistryItems) {
			assert registryItem.dimensionId == shipCore1.getWorld().provider.getDimension();
			
			// only check ships
			if (registryItem.type != EnumGlobalRegionType.SHIP) {
				continue;
			}
			
			// Skip self
			if ( registryItem.x == shipCore1.getPos().getX()
			  && registryItem.y == shipCore1.getPos().getY()
			  && registryItem.z == shipCore1.getPos().getZ() ) {
				continue;
			}
			
			// Compare areas for intersection
			final AxisAlignedBB aabb2 = registryItem.getArea();
			if (!aabb1.intersects(aabb2)) {
				continue;
			}
			
			// Skip missing ship cores
			final TileEntity tileEntity = shipCore1.getWorld().getTileEntity(registryItem.getBlockPos());
			if (!(tileEntity instanceof TileEntityShipCore)) {
				continue;
			}
			final TileEntityShipCore shipCore2 = (TileEntityShipCore) tileEntity;
			
			// Skip invalid ships
			if (!shipCore2.isAssemblyValid()) {
				continue;
			}
			
			// Skip offline ship cores
			if (shipCore2.isOffline()) {
				continue;
			}
			
			// Skip lower tiers ships
			if (shipCore2.getTierIndex() < shipCore1.getTierIndex()) {
				continue;
			}
			
			// ship is intersecting, online and valid
			return shipCore2;
		}
		
		return null;
	}
	
	// do not call during tileEntity construction (readFromNBT and validate)
	private static boolean isExceptionReported = false;
	private static void cleanup() {
		if (!Commons.throttleMe("Global region registry cleanup", 180000)) {
			return;
		}
		LocalProfiler.start("Global region registry cleanup");
		
		boolean isValid;
		for (final Map.Entry<Integer, CopyOnWriteArraySet<GlobalRegion>> entryDimension : registry.entrySet()) {
			final WorldServer world = DimensionManager.getWorld(entryDimension.getKey());
			// skip unloaded worlds
			if (world == null) {
				continue;
			}
			for (final GlobalRegion registryItem : entryDimension.getValue()) {
				isValid = false;
				if (registryItem != null) {
					
					boolean isLoaded;
					if (world.getChunkProvider() instanceof ChunkProviderServer) {
						final ChunkProviderServer chunkProviderServer = world.getChunkProvider();
						try {
							final Chunk chunk = chunkProviderServer.loadedChunks.get(ChunkPos.asLong(registryItem.x >> 4, registryItem.z >> 4));
							isLoaded = chunk != null && chunk.isLoaded();
						} catch (final NoSuchFieldError exception) {
							if (!isExceptionReported) {
								exception.printStackTrace(WarpDrive.printStreamError);
								WarpDrive.logger.info(String.format("Unable to check non-loaded chunks for GlobalRegion %s",
								                                    registryItem));
								isExceptionReported = true;
							}
							isLoaded = chunkProviderServer.chunkExists(registryItem.x >> 4, registryItem.z >> 4);
						}
					} else {
						isLoaded = world.getChunkProvider().chunkExists(registryItem.x >> 4, registryItem.z >> 4);
					}
					// skip unloaded chunks
					if (!isLoaded) {
						if (WarpDrive.isDev && WarpDriveConfig.LOGGING_GLOBAL_REGION_REGISTRY) {
							WarpDrive.logger.debug(String.format("Skipping non-loaded GlobalRegion %s",
							                                     registryItem));
						}
						continue;
					}
					
					// get block and tile entity
					final Block block = world.getBlockState(registryItem.getBlockPos()).getBlock();
					
					final TileEntity tileEntity = world.getTileEntity(registryItem.getBlockPos());
					isValid = true;
					if (registryItem.type == null) {
						isValid = false;
					} else switch (registryItem.type) {
					case UNDEFINED:
						break;
					case SHIP:
						isValid = block instanceof BlockShipCore && tileEntity != null && !tileEntity.isInvalid();
						break;
					case PLANET:
						break;
					case STAR:
						break;
					case STRUCTURE:
						break;
					case WARP_ECHO:
						break;
					case ACCELERATOR:
						isValid = block instanceof BlockAcceleratorCore && tileEntity != null && !tileEntity.isInvalid();
						break;
					case TRANSPORTER:
						isValid = block instanceof BlockTransporterCore && tileEntity != null && !tileEntity.isInvalid();
						break;
					case VIRTUAL_ASSISTANT:
						isValid = block instanceof BlockVirtualAssistant && tileEntity != null && !tileEntity.isInvalid();
						break;
					default:
						break;
					}
				}
				
				if (!isValid) {
					if (registryItem == null) {
						WarpDrive.logger.warn("Cleaning up global region object ~null~");
					} else {
						WarpDrive.logger.warn(String.format("Cleaning up global region object %s at dimension %d (%d %d %d)",
						                                    registryItem.type,
						                                    registryItem.dimensionId, registryItem.x, registryItem.y, registryItem.z ));
					}
					countRemove++;
					entryDimension.getValue().remove(registryItem);
				}
			}
		}
		rebuildIndexes();
		
		LocalProfiler.stop();
	}
	
	public static void readFromNBT(@Nullable final NBTTagCompound tagCompound) {
		if ( tagCompound == null
		  || ( !tagCompound.hasKey("starMapRegistryItems")
		    && !tagCompound.hasKey("globalRegions") ) ) {
			registry.clear();
			rebuildIndexes();
			return;
		}
		
		// read all entries in a flat structure
		final NBTTagList tagList;
		if (tagCompound.hasKey("starMapRegistryItems")) {
			tagList = tagCompound.getTagList("starMapRegistryItems", Constants.NBT.TAG_COMPOUND);
		} else {
			tagList = tagCompound.getTagList("globalRegions", Constants.NBT.TAG_COMPOUND);
		}
		final GlobalRegion[] registryFlat = new GlobalRegion[tagList.tagCount()];
		final HashMap<Integer, Integer> sizeDimensions = new HashMap<>();
		for (int index = 0; index < tagList.tagCount(); index++) {
			final GlobalRegion globalRegion = new GlobalRegion(tagList.getCompoundTagAt(index));
			if (globalRegion.type == null) {
				continue;
			}
			registryFlat[index] = globalRegion;
			
			// update stats
			Integer count = sizeDimensions.computeIfAbsent(globalRegion.dimensionId, k -> 0);
			count++;
			sizeDimensions.put(globalRegion.dimensionId, count);
		}
		
		// pre-build the local collections using known stats to avoid re-allocations
		final HashMap<Integer, ArrayList<GlobalRegion>> registryLocal = new HashMap<>();
		for (final Entry<Integer, Integer> entryDimension : sizeDimensions.entrySet()) {
			registryLocal.put(entryDimension.getKey(), new ArrayList<>(entryDimension.getValue()));
		}
		
		// fill the local collections
		for (final GlobalRegion globalRegion : registryFlat) {
			if (globalRegion == null) {
				continue;
			}
			registryLocal.get(globalRegion.dimensionId).add(globalRegion);
		}
		
		// transfer to main one
		registry.clear();
		for (final Entry<Integer, ArrayList<GlobalRegion>> entry : registryLocal.entrySet()) {
			registry.put(entry.getKey(), new CopyOnWriteArraySet<>(entry.getValue()));
		}
		rebuildIndexes();
	}
	
	public static void writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		final NBTTagList tagList = new NBTTagList();
		for (final CopyOnWriteArraySet<GlobalRegion> globalRegions : registry.values()) {
			for (final GlobalRegion globalRegion : globalRegions) {
				final NBTTagCompound tagCompoundItem = new NBTTagCompound();
				globalRegion.writeToNBT(tagCompoundItem);
				tagList.appendTag(tagCompoundItem);
			}
		}
		tagCompound.setTag("globalRegions", tagList);
	}
}

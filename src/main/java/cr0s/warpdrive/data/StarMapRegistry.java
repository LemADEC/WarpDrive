package cr0s.warpdrive.data;

import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IStarMapRegistryTileEntity;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.StarMapRegistryItem.EnumStarMapEntryType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;

/**
 * Registry of all known ships, jumpgates, etc. in the world
 * 
 * @author LemADEC
 */
public class StarMapRegistry {
	
	private final HashMap<Integer, CopyOnWriteArraySet<StarMapRegistryItem>> registry;
	private int countAdd = 0;
	private int countRemove = 0;
	private int countRead = 0;
	
	public StarMapRegistry() {
		registry = new HashMap<>();
	}
	
	public void updateInRegistry(final IStarMapRegistryTileEntity tileEntity) {
		assert(tileEntity instanceof TileEntity);
		
		countRead++;
		if (WarpDriveConfig.LOGGING_STARMAP) {
			if (countRead % 1000 == 0) {
				WarpDrive.logger.info("Starmap registry stats: read " + countRead + " add " + countAdd + " remove " + countRemove + " => " + ((float) countRead) / (countRemove + countRead + countAdd) + "% read");
			}
		}
		CopyOnWriteArraySet<StarMapRegistryItem> setRegistryItems = registry.get(((TileEntity) tileEntity).getWorldObj().provider.dimensionId);
		if (setRegistryItems == null) {
			setRegistryItems = new CopyOnWriteArraySet<>();
		}
		for (final StarMapRegistryItem registryItem : setRegistryItems) {
			if (registryItem.sameIdOrCoordinates(tileEntity)) {
				// already registered
				registryItem.update(tileEntity);    // @TODO probably not thread safe
				return;
			}
		}
		
		// not found => add
		countAdd++;
		setRegistryItems.add(new StarMapRegistryItem(tileEntity));
		registry.put(((TileEntity) tileEntity).getWorldObj().provider.dimensionId, setRegistryItems);
		if (WarpDriveConfig.LOGGING_STARMAP) {
			printRegistry("added");
		}
	}
	
	public void removeFromRegistry(final IStarMapRegistryTileEntity tileEntity) {
		assert(tileEntity instanceof TileEntity);
		
		countRead++;
		final Set<StarMapRegistryItem> setRegistryItems = registry.get(((TileEntity) tileEntity).getWorldObj().provider.dimensionId);
		if (setRegistryItems == null) {
			// noting to remove
			return;
		}
		
		for (final StarMapRegistryItem registryItem : setRegistryItems) {
			if (registryItem.isSameTileEntity(tileEntity)) {
				// found it, remove and exit
				countRemove++;
				setRegistryItems.remove(registryItem);
				return;
			}
		}
		// not found => ignore it
	}
	
	public String find(final String nameShip) {
		final int MAX_LENGTH = 2000;
		final StringBuilder resultMatch = new StringBuilder();
		final StringBuilder resultCaseInsensitive = new StringBuilder();
		final StringBuilder resultContains = new StringBuilder();
		for (final Integer dimensionId : registry.keySet()) {
			final CopyOnWriteArraySet<StarMapRegistryItem> setStarMapRegistryItems = registry.get(dimensionId);
			if (setStarMapRegistryItems == null) {
				continue;
			}
			
			for (final StarMapRegistryItem starMapRegistryItem : setStarMapRegistryItems) {
				if (starMapRegistryItem.type == EnumStarMapEntryType.SHIP) {
					if (starMapRegistryItem.name.equals(nameShip)) {
						if (resultMatch.length() < MAX_LENGTH) {
							if (resultMatch.length() > 0) {
								resultMatch.append("\n");
							}
							resultContains.append(String.format("Ship '%s' found in %s",
							                                    starMapRegistryItem.name,
							                                    starMapRegistryItem.getFormattedLocation()));
						} else {
							resultMatch.append(".");
						}
					} else if (starMapRegistryItem.name.equalsIgnoreCase(nameShip)) {
						if (resultCaseInsensitive.length() < MAX_LENGTH) {
							if (resultCaseInsensitive.length() > 0) {
								resultCaseInsensitive.append("\n");
							}
							resultContains.append(String.format("Ship '%s' found in %s",
							                                    starMapRegistryItem.name,
							                                    starMapRegistryItem.getFormattedLocation()));
						} else {
							resultCaseInsensitive.append(".");
						}
					} else if (starMapRegistryItem.name.contains(nameShip)) {
						if (resultContains.length() < MAX_LENGTH) {
							if (resultContains.length() > 0) {
								resultContains.append("\n");
							}
							resultContains.append(String.format("Ship '%s' found in %s",
							                                    starMapRegistryItem.name,
							                                    starMapRegistryItem.getFormattedLocation()));
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
		return String.format("No ship found with name '%s'", nameShip);
	}
	
	public void onBlockUpdated(final World world, final int x, final int y, final int z, final Block block, final int metadata) {
		final CopyOnWriteArraySet<StarMapRegistryItem> setStarMapRegistryItems = registry.get(world.provider.dimensionId);
		if (setStarMapRegistryItems == null) {
			return;
		}
		for (final StarMapRegistryItem registryItem : setStarMapRegistryItems) {
			if (registryItem.contains(x, y, z)) {
				final TileEntity tileEntity = world.getTileEntity(registryItem.x, registryItem.y, registryItem.z);
				if (tileEntity instanceof IStarMapRegistryTileEntity) {
					((IStarMapRegistryTileEntity) tileEntity).onBlockUpdatedInArea(new VectorI(x, y, z), block, metadata);
				}
			}
		}
	}
	
	public static double getGravity(final Entity entity) {
		final CelestialObject celestialObject = CelestialObjectManager.get(entity.worldObj, (int) entity.posX, (int) entity.posZ);
		return celestialObject == null ? 1.0D : celestialObject.getGravity();
	}
	
	public static int getSpaceDimensionId(final World world, final int x, final int z) {
		CelestialObject celestialObject = CelestialObjectManager.get(world, x, z);
		if (celestialObject == null) {
			return world.provider.dimensionId;
		}
		// already in space?
		if (celestialObject.isSpace()) {
			return celestialObject.dimensionId;
		}
		// coming from hyperspace?
		if (celestialObject.isHyperspace()) {
			celestialObject = CelestialObjectManager.getClosestChild(world, x, z);
			return celestialObject == null ? 0 : celestialObject.dimensionId;
		}
		// coming from a planet?
		while (celestialObject != null && !celestialObject.isSpace()) {
			celestialObject = celestialObject.parent;
		}
		return celestialObject == null ? 0 : celestialObject.dimensionId;
	}
	
	public static int getHyperspaceDimensionId(final World world, final int x, final int z) {
		CelestialObject celestialObject = CelestialObjectManager.get(world, x, z);
		if (celestialObject == null) {
			return world.provider.dimensionId;
		}
		// already in hyperspace?
		if (celestialObject.isHyperspace()) {
			return celestialObject.dimensionId;
		}
		// coming from space?
		if (celestialObject.isSpace()) {
			return celestialObject.parent.dimensionId;
		}
		// coming from a planet?
		while (celestialObject != null && !celestialObject.isSpace()) {
			celestialObject = celestialObject.parent;
		}
		return celestialObject == null || celestialObject.parent == null ? 0 : celestialObject.parent.dimensionId;
	}
	
	public static int getDimensionId(final String stringDimension, final Entity entity) {
		switch (stringDimension.toLowerCase()) {
		case "world":
		case "overworld":
		case "0":
			return 0;
		case "nether":
		case "thenether":
		case "-1":
			return -1;
		case "s":
		case "space":
			return getSpaceDimensionId(entity.worldObj, (int) entity.posX, (int) entity.posZ);
		case "h":
		case "hyper":
		case "hyperspace":
			return getHyperspaceDimensionId(entity.worldObj, (int) entity.posX, (int) entity.posZ);
		default:
			try {
				return Integer.parseInt(stringDimension);
			} catch(Exception exception) {
				// exception.printStackTrace();
				WarpDrive.logger.info("Invalid dimension '" + stringDimension + "', expecting integer or overworld/nether/end/theend/space/hyper/hyperspace");
			}
		}
		return 0;
	}
	
	public ArrayList<RadarEcho> getRadarEchos(final TileEntity tileEntity, final int radius) {
		final ArrayList<RadarEcho> arrayListRadarEchos = new ArrayList<>(registry.size());
		cleanup();
		
		final CelestialObject celestialObject = CelestialObjectManager.get(tileEntity.getWorldObj(), tileEntity.xCoord, tileEntity.zCoord);
		final Vector3 vectorRadar = getUniversalCoordinates(
			celestialObject,
			tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
		// printRegistry();
		int radius2 = radius * radius;
		for (Map.Entry<Integer, CopyOnWriteArraySet<StarMapRegistryItem>> entryDimension : registry.entrySet()) {
			for (StarMapRegistryItem starMapRegistryItem : entryDimension.getValue()) {
				if (starMapRegistryItem.type == EnumStarMapEntryType.ACCELERATOR) {
					continue;
				}
				final Vector3 vectorItem = starMapRegistryItem.getUniversalCoordinates(tileEntity.getWorldObj().isRemote);
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
				if ( starMapRegistryItem.isolationRate != 0.0D
				  && tileEntity.getWorldObj().rand.nextDouble() < starMapRegistryItem.isolationRate) {
					continue;
				}
				
				arrayListRadarEchos.add( new RadarEcho(starMapRegistryItem.type.getName(),
				                                       vectorItem,
				                                       starMapRegistryItem.mass,
				                                       starMapRegistryItem.name) );
			}
		}
		
		return arrayListRadarEchos;
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
	
	public void printRegistry(final String trigger) {
		WarpDrive.logger.info("Starmap registry (" + registry.size() + " entries after " + trigger + "):");
		
		for (Map.Entry<Integer, CopyOnWriteArraySet<StarMapRegistryItem>> entryDimension : registry.entrySet()) {
			StringBuilder message = new StringBuilder();
			for (StarMapRegistryItem registryItem : entryDimension.getValue()) {
				message.append(String.format("\n- %s '%s' @ DIM%d (%d %d %d) with %.3f isolation rate",
				                             registryItem.type, registryItem.name,
				                             registryItem.dimensionId, registryItem.x, registryItem.y, registryItem.z,
				                             registryItem.isolationRate));
			}
			WarpDrive.logger.info(String.format("- %d entries in dimension %d: %s",
			                                    entryDimension.getValue().size(), entryDimension.getKey(), message.toString()));
		}
	}
	
	public boolean isWarpCoreIntersectsWithOthers(TileEntityShipCore core) {
		final StringBuilder reason = new StringBuilder();
		AxisAlignedBB aabb1, aabb2;
		cleanup();
		
		core.validateShipSpatialParameters(reason);
		aabb1 = AxisAlignedBB.getBoundingBox(core.minX, core.minY, core.minZ, core.maxX, core.maxY, core.maxZ);
		
		CopyOnWriteArraySet<StarMapRegistryItem> setRegistryItems = registry.get(core.getWorldObj().provider.dimensionId);
		if (setRegistryItems == null) {
			return false;
		}
		for (StarMapRegistryItem registryItem : setRegistryItems) {
			assert(registryItem.dimensionId == core.getWorldObj().provider.dimensionId);
			
			// only check cores
			if (registryItem.type != EnumStarMapEntryType.SHIP) {
				continue;
			}
			
			// Skip self
			if (registryItem.x == core.xCoord && registryItem.y == core.yCoord && registryItem.z == core.zCoord) {
				continue;
			}
			
			// Skip missing ship cores
			TileEntity tileEntity = core.getWorldObj().getTileEntity(registryItem.x, registryItem.y, registryItem.z);
			if (!(tileEntity instanceof TileEntityShipCore)) {
				continue;
			}
			TileEntityShipCore shipCore = (TileEntityShipCore) core.getWorldObj().getTileEntity(registryItem.x, registryItem.y, registryItem.z);
			
			// Skip offline ship cores
			if (shipCore.isOffline()) {
				continue;
			}
			
			// Skip invalid ships
			if (!shipCore.validateShipSpatialParameters(reason)) {
				continue;
			}
			
			// Compare areas for intersection
			aabb2 = AxisAlignedBB.getBoundingBox(registryItem.minX, registryItem.minY, registryItem.minZ, registryItem.maxX, registryItem.maxY, registryItem.maxZ);
			if (aabb1.intersectsWith(aabb2)) {
				return true;
			}
		}
		
		return false;
	}
	
	// do not call during tileEntity construction (readFromNBT and validate)
	private void cleanup() {
		LocalProfiler.start("Starmap registry cleanup");
		
		boolean isValid;
		for (Map.Entry<Integer, CopyOnWriteArraySet<StarMapRegistryItem>> entryDimension : registry.entrySet()) {
			final WorldServer world = DimensionManager.getWorld(entryDimension.getKey());
			// skip unloaded worlds
			if (world == null) {
				continue;
			}
			for (StarMapRegistryItem registryItem : entryDimension.getValue()) {
				isValid = false;
				if (registryItem != null) {
					
					boolean isLoaded;
					if (world.getChunkProvider() instanceof ChunkProviderServer) {
						final ChunkProviderServer chunkProviderServer = (ChunkProviderServer) world.getChunkProvider();
						try {
							isLoaded = chunkProviderServer.loadedChunkHashMap.containsItem(ChunkCoordIntPair.chunkXZ2Int(registryItem.x >> 4, registryItem.z >> 4));
						} catch (NoSuchFieldError exception) {
							isLoaded = chunkProviderServer.chunkExists(registryItem.x >> 4, registryItem.z >> 4);
						}
					} else {
						isLoaded = world.getChunkProvider().chunkExists(registryItem.x >> 4, registryItem.z >> 4);
					}
					// skip unloaded chunks
					if (!isLoaded) {
						continue;
					}
					
					// get block and tile entity
					final Block block = world.getBlock(registryItem.x, registryItem.y, registryItem.z);
					
					final TileEntity tileEntity = world.getTileEntity(registryItem.x, registryItem.y, registryItem.z);
					isValid = true;
					switch (registryItem.type) {
						case UNDEFINED:
							break;
						case SHIP:
							isValid = block == WarpDrive.blockShipCore && tileEntity != null && !tileEntity.isInvalid();
							break;
						case JUMPGATE:
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
							isValid = block == WarpDrive.blockAcceleratorController && tileEntity != null && !tileEntity.isInvalid();
							break;
						default:
							break;
					}
				}
				
				if (!isValid) {
					if (WarpDriveConfig.LOGGING_STARMAP) {
						if (registryItem == null) {
							WarpDrive.logger.info("Cleaning up starmap object ~null~");
						} else {
							WarpDrive.logger.info("Cleaning up starmap object " + registryItem.type + " at "
							                      + registryItem.dimensionId + " " + registryItem.x + " " + registryItem.y + " " + registryItem.z);
						}
					}
					countRemove++;
					entryDimension.getValue().remove(registryItem);
				}
			}
		}
		
		LocalProfiler.stop();
	}
	
	public void readFromNBT(NBTTagCompound tagCompound) {
		if (tagCompound == null || !tagCompound.hasKey("starMapRegistryItems")) {
			registry.clear();
			return;
		}
		
		// read all entries in a flat structure
		final NBTTagList tagList = tagCompound.getTagList("starMapRegistryItems", Constants.NBT.TAG_COMPOUND);
		final StarMapRegistryItem[] registryFlat = new StarMapRegistryItem[tagList.tagCount()];
		final HashMap<Integer, Integer> sizeDimensions = new HashMap<>();
		for(int index = 0; index < tagList.tagCount(); index++) {
			final StarMapRegistryItem starMapRegistryItem = new StarMapRegistryItem(tagList.getCompoundTagAt(index));
			registryFlat[index] = starMapRegistryItem;
			
			// update stats
			Integer count = sizeDimensions.computeIfAbsent(starMapRegistryItem.dimensionId, k -> (Integer) 0);
			count++;
			sizeDimensions.put(starMapRegistryItem.dimensionId, count);
		}
		
		// pre-build the local collections using known stats to avoid re-allocations
		final HashMap<Integer, ArrayList<StarMapRegistryItem>> registryLocal = new HashMap<>();
		for(Entry<Integer, Integer> entryDimension : sizeDimensions.entrySet()) {
			registryLocal.put(entryDimension.getKey(), new ArrayList<>(entryDimension.getValue()));
		}
		
		// fill the local collections
		for(StarMapRegistryItem starMapRegistryItem : registryFlat) {
			registryLocal.get(starMapRegistryItem.dimensionId).add(starMapRegistryItem);
		}
		
		// transfer to main one
		registry.clear();
		for(Entry<Integer, ArrayList<StarMapRegistryItem>> entry : registryLocal.entrySet()) {
			registry.put(entry.getKey(), new CopyOnWriteArraySet<>(entry.getValue()));
		}
	}
	
	public void writeToNBT(final NBTTagCompound tagCompound) {
		final NBTTagList tagList = new NBTTagList();
		for(CopyOnWriteArraySet<StarMapRegistryItem> starMapRegistryItems : registry.values()) {
			for(StarMapRegistryItem starMapRegistryItem : starMapRegistryItems) {
				final NBTTagCompound tagCompoundItem = new NBTTagCompound();
				starMapRegistryItem.writeToNBT(tagCompoundItem);
				tagList.appendTag(tagCompoundItem);
			}
		}
		tagCompound.setTag("starMapRegistryItems", tagList);
	}
}

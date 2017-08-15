package cr0s.warpdrive.event;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.ChunkData;
import cr0s.warpdrive.data.StateAir;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

public class ChunkHandler {
	
	private static final long CHUNK_HANDLER_UNLOADED_CHUNK_MAX_AGE_MS = 30000L;
	
	// persistent properties
	private static final Map<Integer, Map<Long, ChunkData>> registryClient = new HashMap<>(32);
	private static final Map<Integer, Map<Long, ChunkData>> registryServer = new HashMap<>(32);
	
	// computed properties
	public static long delayLogging = 0;
	
	/* event catchers */
	@SubscribeEvent
	public void onLoadWorld(WorldEvent.Load event) {
		if (event.world.isRemote || event.world.provider.dimensionId == 0) {
			if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
				WarpDrive.logger.info(String.format("%s world %s load.",
				                                    event.world.isRemote ? "Client" : "Server",
				                                    event.world.provider.getDimensionName()));
			}
		}
		
		if ( !event.world.isRemote
		  && event.world.provider.dimensionId == 0 ) {
			// load star map
			final String filename = String.format("%s/%s.dat", event.world.getSaveHandler().getWorldDirectory().getPath(), WarpDrive.MODID);
			final NBTTagCompound tagCompound = Commons.readNBTFromFile(filename);
			WarpDrive.starMap.readFromNBT(tagCompound);
		}
	}
	
	// new chunks aren't loaded
	public static void onGenerated(final World world, final int chunkX, final int chunkZ) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk [%d, %d] generating",
			                                    world.isRemote ? "Client" : "Server",
			                                    world.provider.getDimensionName(),
			                                    chunkX, chunkZ));
		}
		
		final ChunkData chunkData = getChunkData(world.isRemote, world.provider.dimensionId, chunkX, chunkZ, true);
		assert(chunkData != null);
		// (world can load a non-generated chunk, or the chunk be regenerated, so we reset only as needed)
		if (!chunkData.isLoaded()) {
			chunkData.load(new NBTTagCompound());
		}
	}
	
	// (server side only)
	@SubscribeEvent
	public void onLoadChunkData(ChunkDataEvent.Load event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s loading data (1)", 
			                                    event.world.isRemote ? "Client" : "Server",
			                                    event.world.provider.getDimensionName(),
			                                    event.getChunk().getChunkCoordIntPair()));
		}
		
		final ChunkData chunkData = getChunkData(event.world.isRemote, event.world.provider.dimensionId, event.getChunk().xPosition, event.getChunk().zPosition, true);
		assert(chunkData != null);
		chunkData.load(event.getData());
	}
	
	// (called after data loading, only useful client side)
	@SubscribeEvent
	public void onLoadChunk(ChunkEvent.Load event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s loaded (2)",
			                                    event.world.isRemote ? "Client" : "Server",
			                                    event.world.provider.getDimensionName(),
			                                    event.getChunk().getChunkCoordIntPair()));
		}
		
		if (event.world.isRemote) {
			final ChunkData chunkData = getChunkData(event.world.isRemote, event.world.provider.dimensionId, event.getChunk().xPosition, event.getChunk().zPosition, true);
			assert(chunkData != null);
			chunkData.load(new NBTTagCompound());
		}
	}
	/*
	// (server side only)
	@SubscribeEvent
	public void onWatchChunk(ChunkWatchEvent.Watch event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("World %s chunk %s watch by %s",
			                                    event.player.worldObj.provider.getDimensionName(),
			                                    event.chunk,
			                                    event.player));
		}
	}
	/**/
	// (server side only)
	// not called when chunk wasn't changed since last save?
	@SubscribeEvent
	public void onSaveChunkData(ChunkDataEvent.Save event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s save data",
			                                    event.world.isRemote ? "Client" : "Server",
			                                    event.world.provider.getDimensionName(),
			                                    event.getChunk().getChunkCoordIntPair()));
		}
		final ChunkData chunkData = getChunkData(event.world.isRemote, event.world.provider.dimensionId, event.getChunk().xPosition, event.getChunk().zPosition, false);
		if (chunkData != null) {
			chunkData.save(event.getData());
		} else if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.error(String.format("%s world %s chunk %s is saving data without loading it first!",
			                                     event.world.isRemote ? "Client" : "Server",
			                                     event.world.provider.getDimensionName(),
			                                     event.getChunk().getChunkCoordIntPair()));
		}
	}
	
	// (server side only)
	@SubscribeEvent
	public void onSaveWorld(WorldEvent.Save event) {
		if (event.world.provider.dimensionId != 0) {
			return;
		}
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s saved.",
			                                    event.world.isRemote ? "Client" : "Server",
			                                    event.world.provider.getDimensionName()));
		}
		
		if (event.world.isRemote) {
			return;
		}
		
		// save star map
		final String filename = String.format("%s/%s.dat", event.world.getSaveHandler().getWorldDirectory().getPath(), WarpDrive.MODID);
		final NBTTagCompound tagCompound = new NBTTagCompound();
		WarpDrive.starMap.writeToNBT(tagCompound);
		Commons.writeNBTToFile(filename, tagCompound);
	}
	
	@SubscribeEvent
	public void onUnloadWorld(WorldEvent.Unload event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s unload",
			                                    event.world.isRemote ? "Client" : "Server",
			                                    event.world.provider.getDimensionName()));
		}
		
		// get dimension data
		LocalProfiler.updateCallStat("onUnloadWorld");
		final Map<Integer, Map<Long, ChunkData>> registry = event.world.isRemote ? registryClient : registryServer;
		final Map<Long, ChunkData> mapRegistryItems = registry.get(event.world.provider.dimensionId);
		if (mapRegistryItems != null) {
			// unload chunks during shutdown
			for (ChunkData chunkData : mapRegistryItems.values()) {
				if (chunkData.isLoaded()) {
					chunkData.unload();
				}
			}
		}
		
		// @TODO unload star map
	}
	
	
	// (not called when closing SSP game)
	@SubscribeEvent
	public void onUnloadChunk(ChunkEvent.Unload event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s unload",
			                                    event.world.isRemote ? "Client" : "Server",
			                                    event.world.provider.getDimensionName(),
			                                    event.getChunk().getChunkCoordIntPair()));
		}
		
		final ChunkData chunkData = getChunkData(event.world.isRemote, event.world.provider.dimensionId, event.getChunk().xPosition, event.getChunk().zPosition, false);
		if (chunkData != null) {
			chunkData.unload();
		} else if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.error(String.format("%s world %s chunk %s is unloading without loading it first!", 
			                                     event.world.isRemote ? "Client" : "Server",
			                                     event.world.provider.getDimensionName(),
			                                     event.getChunk().getChunkCoordIntPair()));
		}
	}
	/*
	// (not called when closing SSP game)
	// warning: will return invalid world when switching dimensions
	@SubscribeEvent
	public void onUnwatchChunk(ChunkWatchEvent.UnWatch event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s unwatch by %s",
			                                    event.player.worldObj.isRemote ? "Client" : "Server",
			                                    event.player.worldObj.provider.getDimensionName(),
			                                    event.chunk,
			                                    event.player));
		}
	}
	/**/
	@SubscribeEvent
	public void onWorldTick(WorldTickEvent event) {
		if (event.side != Side.SERVER || event.phase != Phase.END) {
			return;
		}
		// @TODO: add workaround for other mods doing bad multi-threading
		updateTick(event.world);
	}
	
	public static void onBlockUpdated(final World world, final int x, final int y, final int z) {
		if (!world.isRemote) {
			final ChunkData chunkData = getChunkData(world, x, y, z, false);
			if (chunkData != null) {
				chunkData.onBlockUpdated(x, y, z);
			} else {
				WarpDrive.logger.error(String.format("%s world %s block updating at (%d %d %d), while chunk isn't loaded!",
				                                     world.isRemote ? "Client" : "Server",
				                                     world.provider.getDimensionName(),
				                                     x, y, z));
			}
		}
	}
	
	/* internal access */
	public static ChunkData getChunkData(final World world, final int x, final int y, final int z, final boolean doCreate) {
		return getChunkData(world.isRemote, world.provider.dimensionId, x, y, z, doCreate);
	}
	
	private static ChunkData getChunkData(final boolean isRemote, final int dimensionId, final int x, final int y, final int z, final boolean doCreate) {
		assert (y >= 0 && y <= 255);
		return getChunkData(isRemote, dimensionId, x >> 4, z >> 4, doCreate);
	}
	
	private static ChunkData getChunkData(final boolean isRemote, final int dimensionId, final int xChunk, final int zChunk, final boolean doCreate) {
		// get dimension data
		LocalProfiler.updateCallStat("getChunkData");
		final Map<Integer, Map<Long, ChunkData>> registry = isRemote ? registryClient : registryServer;
		Map<Long, ChunkData> mapRegistryItems = registry.get(dimensionId);
		// (lambda expressions are forcing synchronisation, so we don't use them here)
		//noinspection Java8MapApi
		if (mapRegistryItems == null) {
			if (!doCreate) {
				return null;
			}
			// TLongObjectMap<ChunkData> m = TCollections.synchronizedMap(new TLongObjectHashMap<ChunkData>(2048) );
			// @TODO: http://trove4j.sourceforge.net/javadocs/gnu/trove/TCollections.html#synchronizedMap(gnu.trove.map.TLongObjectMap)
			mapRegistryItems = new LinkedHashMap<>(2048); // Collections.synchronizedMap(new LinkedHashMap<>(2048));
			registry.put(dimensionId, mapRegistryItems);
		}
		// get chunk data
		final long index = ChunkCoordIntPair.chunkXZ2Int(xChunk, zChunk);
		ChunkData chunkData = mapRegistryItems.get(index);
		// (lambda expressions are forcing synchronisation, so we don't use them here)
		//noinspection Java8MapApi
		if (chunkData == null) {
			if (!doCreate) {
				return null;
			}
			chunkData = new ChunkData(xChunk, zChunk);
			if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
				WarpDrive.logger.info(String.format("%s world DIM%d chunk %s is being added to the registry",
				                                    isRemote ? "Client" : "Server",
				                                    dimensionId,
				                                    chunkData.getChunkCoords()));
			}
			mapRegistryItems.put(index, chunkData);
		}
		return chunkData;
	}
	
	private static boolean isLoaded(final Map<Long, ChunkData> mapRegistryItems, final int xChunk, final int zChunk) {
		// get chunk data
		final long index = ChunkCoordIntPair.chunkXZ2Int(xChunk, zChunk);
		final ChunkData chunkData = mapRegistryItems.get(index);
		return chunkData != null && chunkData.isLoaded();
	}
	
	/* commons */
	public static boolean isLoaded(final World world, final int x, final int y, final int z) {
		final ChunkData chunkData = getChunkData(world, x, y, z, false);
		return chunkData != null && chunkData.isLoaded();
	}
	
	/* air handling */
	public static StateAir getStateAir(final World world, final int x, final int y, final int z) {
		return getChunkData(world, x, y, z, true).getStateAir(world, x, y, z);
	}
	
	public static void updateTick(final World world) {
		// get dimension data
		LocalProfiler.updateCallStat("updateTick");
		final Map<Integer, Map<Long, ChunkData>> registry = world.isRemote ? registryClient : registryServer;
		final Map<Long, ChunkData> mapRegistryItems = registry.get(world.provider.dimensionId);
		if (mapRegistryItems == null) {
			return;
		}
		int countLoaded = 0;
		final long timeForRemoval = System.currentTimeMillis() - CHUNK_HANDLER_UNLOADED_CHUNK_MAX_AGE_MS;
		final long timeForThrottle = System.currentTimeMillis() + 200;
		for(final Iterator<Entry<Long, ChunkData>> entryIterator = mapRegistryItems.entrySet().iterator(); entryIterator.hasNext(); ) {
			final Map.Entry<Long, ChunkData> entryChunkData = entryIterator.next();
			final ChunkData chunkData = entryChunkData.getValue();
			// update loaded chunks, remove old unloaded chunks
			if (chunkData.isLoaded()) {
				countLoaded++;
				if (System.currentTimeMillis() < timeForThrottle) {
					updateTickLoopStep(world, mapRegistryItems, entryChunkData.getValue());
				}
			} else if (chunkData.timeUnloaded < timeForRemoval) {
				if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
					WarpDrive.logger.info(String.format("%s world %s chunk %s is being removed from updateTick (size is %d)", 
					                                    world.isRemote ? "Client" : "Server",
					                                    world.provider.getDimensionName(),
					                                    chunkData.getChunkCoords(),
					                                    mapRegistryItems.size()));
				}
				entryIterator.remove();
			}
		}
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			if (world.provider.dimensionId == 0) {
				delayLogging = (delayLogging + 1) % 4096;
			}
			if (delayLogging == 1) {
				WarpDrive.logger.info(String.format("Dimension %d has %d / %d chunks loaded",
				                                    world.provider.dimensionId,
				                                    countLoaded,
				                                    mapRegistryItems.size()));
			}
		}
	}
	
	public static void updateTickLoopStep(final World world, final Map<Long, ChunkData> mapRegistryItems, final ChunkData chunkData) {
		final ChunkCoordIntPair chunkCoordIntPair = chunkData.getChunkCoords();
		// skip empty chunks (faster and more frequent)
		// ship chunk with unloaded neighbours
		if ( chunkData.isNotEmpty()
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.chunkXPos + 1, chunkCoordIntPair.chunkZPos)
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.chunkXPos - 1, chunkCoordIntPair.chunkZPos)
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.chunkXPos, chunkCoordIntPair.chunkZPos + 1)
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.chunkXPos, chunkCoordIntPair.chunkZPos - 1) ) {
			chunkData.updateTick(world);
		}
	}
}
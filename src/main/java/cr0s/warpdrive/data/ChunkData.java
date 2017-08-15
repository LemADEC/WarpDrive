package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.event.ChunkHandler;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class ChunkData {
	
	private static final String TAG_CHUNK_MOD_DATA = WarpDrive.MODID;
	private static final String TAG_VERSION = "version";
	private static final String TAG_AIR = "air";
	private static final String TAG_AIR_SEGMENT_DATA = "data";
	private static final String TAG_AIR_SEGMENT_DELAY = "delay";
	private static final String TAG_AIR_SEGMENT_Y = "y";
	private static final long RELOAD_DELAY_MIN_MS = 100;
	private static final long LOAD_UNLOAD_DELAY_MIN_MS = 1000;
	private static final long SAVE_SAVE_DELAY_MIN_MS = 100;
	
	private static final int CHUNK_SIZE_SEGMENTS = 16;       // 16 segments of 16x16x16 blocks
	private static final int SEGMENT_SIZE_BLOCKS = 16 * 256;
	private static final int INVALID_DATA_INDEX = 0xFF7F;    // central block in chunk top
	
	// persistent properties
	private int[][] dataAirSegments = new int[CHUNK_SIZE_SEGMENTS][];
	private byte[][] tickAirSegments = new byte[CHUNK_SIZE_SEGMENTS][];
	
	// computed properties
	private int tickCurrent = (int) (Math.random() * 4096.0D);
	private final ChunkCoordIntPair chunkCoordIntPair;
	private boolean isLoaded;
	public long timeLoaded;
	public long timeSaved;
	public long timeUnloaded;
	public boolean isModified;
	
	public ChunkData(final int xChunk, final int zChunk) {
		this.chunkCoordIntPair = new ChunkCoordIntPair(xChunk, zChunk);
		isLoaded = false;
		timeLoaded = 0L;
		timeSaved = 0L;
		timeUnloaded = 0L;
	}
	
	public void load(final NBTTagCompound nbtTagCompoundChunk) {
		// check consistency
		assert(!isLoaded);
		
		// detects fast reloading
		final long time = System.currentTimeMillis();
		if ( WarpDriveConfig.LOGGING_CHUNK_HANDLER
		  && timeUnloaded != 0L
		  && time - timeUnloaded < RELOAD_DELAY_MIN_MS ) {
			WarpDrive.logger.warn(String.format("Chunk %s (%d %d %d) is reloading after only %d ms", 
			                                    chunkCoordIntPair, 
			                                    getChunkPosition().chunkPosX, getChunkPosition().chunkPosY, getChunkPosition().chunkPosZ, 
			                                    time - timeUnloaded));
		}
		
		// load defaults
		Arrays.fill(dataAirSegments, null);
		Arrays.fill(tickAirSegments, null);
		isModified = false;
		
		// check version
		if (nbtTagCompoundChunk.hasKey(TAG_CHUNK_MOD_DATA)) {
			final NBTTagCompound nbtTagCompound = nbtTagCompoundChunk.getCompoundTag(TAG_CHUNK_MOD_DATA);
			final int version = nbtTagCompound.getInteger(TAG_VERSION);
			assert (version == 0 || version == 1);
			
			// load from NBT data
			if (version == 1) {
				final NBTTagList nbtTagList = nbtTagCompound.getTagList(TAG_AIR, Constants.NBT.TAG_COMPOUND);
				if (nbtTagList.tagCount() != CHUNK_SIZE_SEGMENTS) {
					if (nbtTagList.tagCount() != 0) {
						WarpDrive.logger.error(String.format("Chunk %s (%d %d %d) loaded with invalid data, restoring default",
						                                     chunkCoordIntPair,
						                                     getChunkPosition().chunkPosX, getChunkPosition().chunkPosY, getChunkPosition().chunkPosZ));
					}
				} else {
					// check all segments
					for (int indexSegment = 0; indexSegment < CHUNK_SIZE_SEGMENTS; indexSegment++) {
						final NBTTagCompound nbtTagCompoundInList = nbtTagList.getCompoundTagAt(indexSegment);
						
						// get raw data
						final int[] intData = nbtTagCompoundInList.getIntArray(TAG_AIR_SEGMENT_DATA);
						// skip invalid or empty segments
						if (intData.length != SEGMENT_SIZE_BLOCKS) {
							if (intData.length != 0) {
								WarpDrive.logger.error(String.format("Chunk %s (%d %d %d) loaded with invalid segment %d, restoring default",
								                                     chunkCoordIntPair,
								                                     getChunkPosition().chunkPosX, getChunkPosition().chunkPosY, getChunkPosition().chunkPosZ,
								                                     indexSegment));
							}
							continue;
						}
						
						// validate segment index
						final int indexRead = nbtTagCompoundInList.getByte(TAG_AIR_SEGMENT_Y);
						if (indexRead != indexSegment) {
							WarpDrive.logger.error(String.format("Error while loading %s: bad index read %d expecting %d", this, indexRead, indexSegment));
						}
						
						// get tick delay
						byte[] byteTick = nbtTagCompoundInList.getByteArray(TAG_AIR_SEGMENT_DELAY);
						// reset undefined delays
						if (byteTick.length != SEGMENT_SIZE_BLOCKS) {
							byteTick = new byte[SEGMENT_SIZE_BLOCKS];
							Arrays.fill(byteTick, (byte) 0);
						}
						
						// load data with basic filtering
						dataAirSegments[indexSegment] = new int[SEGMENT_SIZE_BLOCKS];
						tickAirSegments[indexSegment] = new byte[SEGMENT_SIZE_BLOCKS];
						for (int indexBlock = 0; indexBlock < SEGMENT_SIZE_BLOCKS; indexBlock++) {
							dataAirSegments[indexSegment][indexBlock] = intData[indexBlock] & StateAir.USED_MASK;
							tickAirSegments[indexSegment][indexBlock] = (byte) (byteTick[indexBlock] & 0x7F);
							if ( WarpDrive.isDev && WarpDriveConfig.LOGGING_CHUNK_HANDLER
							  && dataAirSegments[indexSegment][indexBlock] != 0 ) {
								final ChunkPosition chunkPosition = getPositionFromDataIndex(indexSegment, indexBlock);
								WarpDrive.logger.info(String.format("Loading %s segment %2d index %4d (%d %d %d) 0x%8x",
								                                    this, indexSegment, indexBlock,
								                                    chunkPosition.chunkPosX, chunkPosition.chunkPosY, chunkPosition.chunkPosZ,
								                                    dataAirSegments[indexSegment][indexBlock]));
							}
						}
					}// for indexSegment
				}
			}// version 1
		}// has data
		
		// mark as loaded
		timeLoaded = time;
		isLoaded = true;
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("Chunk %s (%d %d %d) is now loaded",
			                                    chunkCoordIntPair,
			                                    getChunkPosition().chunkPosX, getChunkPosition().chunkPosY, getChunkPosition().chunkPosZ));
		}
	}
	
	public void onBlockUpdated(final int x, final int y, final int z) {
		final int indexData = getDataIndex(x, y, z);
		// get segment
		final int[] dataAirSegment = dataAirSegments[indexData >> 12];
		if (dataAirSegment == null) {
			return;
		}
		// force update of related block cache
		dataAirSegment[indexData & 0xFFF] = dataAirSegment[indexData & 0xFFF] & ~StateAir.BLOCK_MASK;
		
		// get current tick delay
		final byte[] tickAirSegment = tickAirSegments[indexData >> 12];
		final byte tickAir = tickAirSegment[indexData & 0xFFF];
		final int delay = (0x80 + tickAir - tickCurrent) & 0x7F;
		// reduce to lower than 16 ticks
		if (delay > 15 && delay != WarpDriveConfig.BREATHING_AIR_SIMULATION_DELAY_TICKS + (dataAirSegment[indexData & 0xFFF] & StateAir.CONCENTRATION_MASK)) {
			tickAirSegment[indexData & 0xFFF] = (byte) ((tickCurrent + delay & 0x0F) & 0x7F);
		}
	}
	
	public void save(NBTTagCompound nbtTagCompoundChunk) {
		// check consistency
		// (unload happens before saving)
		
		isModified = false;
		
		// detects fast saving
		final long time = System.currentTimeMillis();
		if ( WarpDriveConfig.LOGGING_CHUNK_HANDLER
		  && isLoaded
		  && timeSaved != 0L
		  && time - timeSaved < SAVE_SAVE_DELAY_MIN_MS ) {
			WarpDrive.logger.warn(String.format("Chunk %s (%d %d %d) is saving after only %d ms",
			                                    chunkCoordIntPair,
			                                    getChunkPosition().chunkPosX, getChunkPosition().chunkPosY, getChunkPosition().chunkPosZ, 
			                                    time - timeSaved));
		}
		
		// save to NBT data
		NBTTagCompound nbtTagCompound =  new NBTTagCompound();
		nbtTagCompoundChunk.setTag(TAG_CHUNK_MOD_DATA, nbtTagCompound);
		nbtTagCompound.setInteger(TAG_VERSION, 1);
		
		NBTTagList nbtTagList = new NBTTagList();
		
		// check all segments
		int countEmptySegments = 0;
		final int[] intData = new int[SEGMENT_SIZE_BLOCKS];
		final byte[] byteTick = new byte[SEGMENT_SIZE_BLOCKS];
		for (int indexSegment = 0; indexSegment < CHUNK_SIZE_SEGMENTS; indexSegment++) {
			final NBTTagCompound nbtTagCompoundInList = new NBTTagCompound();
			
			// skip empty segment
			if (dataAirSegments[indexSegment] != null) {
				// merge data and check for purge
				int countEmptyBlocks = 0;
				
				for (int indexBlock = 0; indexBlock < SEGMENT_SIZE_BLOCKS; indexBlock++) {
					final int dataAir = dataAirSegments[indexSegment][indexBlock];
					if (StateAir.isEmptyData(dataAir)) {
						countEmptyBlocks++;
						intData[indexBlock] = StateAir.AIR_DEFAULT;
						byteTick[indexBlock] = (byte) 0;
					} else {
						intData[indexBlock] = dataAir;
						byteTick[indexBlock] = tickAirSegments[indexSegment][indexBlock];
						
						if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
							final ChunkPosition chunkPosition = getPositionFromDataIndex(indexSegment, indexBlock);
							WarpDrive.logger.info(String.format("Saving %s segment %2d index %4d (%d %d %d) 0x%8x",
							                                    this, indexSegment, indexBlock,
							                                    chunkPosition.chunkPosX, chunkPosition.chunkPosY, chunkPosition.chunkPosZ,
							                                    dataAir));
						}
					}
				}
				
				if (countEmptyBlocks == SEGMENT_SIZE_BLOCKS) {
					countEmptySegments++;
				} else {
					nbtTagCompoundInList.setIntArray(TAG_AIR_SEGMENT_DATA, intData.clone());
					nbtTagCompoundInList.setByteArray(TAG_AIR_SEGMENT_DELAY, byteTick.clone());
					nbtTagCompoundInList.setByte(TAG_AIR_SEGMENT_Y, (byte) indexSegment);
				}
			} else {
				countEmptySegments++;
			}
			nbtTagList.appendTag(nbtTagCompoundInList);
		}
		
		// ignore tag if all segments are empty
		if (countEmptySegments != CHUNK_SIZE_SEGMENTS) {
			nbtTagCompound.setTag(TAG_AIR, nbtTagList);
		}
		
		// mark as saved
		timeSaved = time;
	}
	
	public void unload() {
		// check consistency
		if ( !isLoaded
		  && timeUnloaded != 0L ) {
			if (timeLoaded != 0L) {
				WarpDrive.logger.warn(String.format("Chunk %s (%d %d %d) is already unloaded, timings are loaded %d saved %d unloaded %d",
				                                    chunkCoordIntPair,
				                                    getChunkPosition().chunkPosX, getChunkPosition().chunkPosY, getChunkPosition().chunkPosZ,
				                                    timeLoaded,
				                                    timeSaved,
				                                    timeUnloaded));
			}
			return;
		}
		
		// detects fast unloading
		final long time = System.currentTimeMillis();
		if ( WarpDriveConfig.LOGGING_CHUNK_HANDLER
		  && timeUnloaded != 0L
		  && time - timeUnloaded < LOAD_UNLOAD_DELAY_MIN_MS ) {
			WarpDrive.logger.warn(String.format("Chunk %s (%d %d %d) is unloading after only %d ms",
			                                    chunkCoordIntPair, 
			                                    getChunkPosition().chunkPosX, getChunkPosition().chunkPosY, getChunkPosition().chunkPosZ, 
			                                    time - timeUnloaded));
		}
		
		// mark as loaded
		timeUnloaded = time;
		isLoaded = false;
	}
	
	public boolean isLoaded() {
		return isLoaded;
	}
	
	
	/* common data handling */
	public ChunkCoordIntPair getChunkCoords() {
		return chunkCoordIntPair;
	}
	
	public ChunkPosition getChunkPosition() {
		return chunkCoordIntPair.func_151349_a(128);
	}
	
	protected boolean isInside(final int x, @SuppressWarnings("unused") final int y, final int z) {
		final int xInChunk = x - (chunkCoordIntPair.chunkXPos << 4);
		// final int yInChunk = Commons.clamp(0, 255, y);
		final int zInChunk = z - (chunkCoordIntPair.chunkZPos << 4);
		return xInChunk >= 0 && xInChunk <= 15 && zInChunk >= 0 && zInChunk <= 15;
	}
	
	private int getDataIndex(final int x, final int y, final int z) {
		final int xInChunk = x - (chunkCoordIntPair.chunkXPos << 4);
		final int yInChunk = Commons.clamp(0, 255, y);
		final int zInChunk = z - (chunkCoordIntPair.chunkZPos << 4);
		if (xInChunk < 0 || xInChunk > 15 || zInChunk < 0 || zInChunk > 15) {
			WarpDrive.logger.error(String.format("Invalid block position provided (%d %d %d) is outside of chunk %s at (%d %d %d)",
					x, y, z,
					chunkCoordIntPair,
					getChunkPosition().chunkPosX, getChunkPosition().chunkPosY, getChunkPosition().chunkPosZ));
			return INVALID_DATA_INDEX;
		}
		return yInChunk << 8 | xInChunk << 4 | zInChunk;
	}
	
	private ChunkPosition getPositionFromDataIndex(final int indexSegment, final int indexBlock) {
		final int x = (chunkCoordIntPair.chunkXPos << 4) + ((indexBlock & 0x00F0) >> 4);
		final int y = (indexSegment << 4) + ((indexBlock & 0x0F00) >> 8);
		final int z = (chunkCoordIntPair.chunkZPos << 4) + (indexBlock & 0x000F);
		return new ChunkPosition(x, y, z);
	}
	
	
	/* air data handling */
	public int getDataAir(final int x, final int y, final int z) {
		final int indexData = getDataIndex(x, y, z);
		// self-test for index mapping
		if (WarpDrive.isDev) {
			final ChunkPosition chunkPosition = getPositionFromDataIndex(indexData >> 12, indexData & 0x0FFF);
			assert(chunkPosition.chunkPosX == x && chunkPosition.chunkPosY == y && chunkPosition.chunkPosZ == z);
		}
		// get segment
		final int[] dataAirSegment = dataAirSegments[indexData >> 12];
		if (dataAirSegment == null) {
			return StateAir.AIR_DEFAULT;
		}
		// get block
		return dataAirSegment[indexData & 0xFFF];
	}
	
	public void setDataAir(final int x, final int y, final int z, final int dataAirBlock) {
		final int indexData = getDataIndex(x, y, z);
		
		// get segment
		int[] dataAirSegment = dataAirSegments[indexData >> 12];
		byte[] tickAirSegment = tickAirSegments[indexData >> 12];
		if (dataAirSegment == null) {
			// don't create unless we have data to save
			if (StateAir.isEmptyData(dataAirBlock)) {
				return;
			}
			// create new segment
			dataAirSegment = new int[SEGMENT_SIZE_BLOCKS];
			dataAirSegments[indexData >> 12] = dataAirSegment;
			tickAirSegment = new byte[SEGMENT_SIZE_BLOCKS];
			tickAirSegments[indexData >> 12] = tickAirSegment;
			isModified = true;
		}
		
		// set block
		if (dataAirSegment[indexData & 0xFFF] != dataAirBlock) {
			dataAirSegment[indexData & 0xFFF] = dataAirBlock;
			isModified = true;
		}
		
		// set delay
		final byte delay = (byte) (WarpDriveConfig.BREATHING_AIR_SIMULATION_DELAY_TICKS + (dataAirBlock & StateAir.CONCENTRATION_MASK));
		tickAirSegment[indexData & 0xFFF] = (byte) ((tickCurrent + delay) & 0x7F); 
	}
	
	public StateAir getStateAir(final World world, final int x, final int y, final int z) {
		final StateAir stateAir = new StateAir(this);
		stateAir.refresh(world, x, y, z);
		return stateAir;
	}
	
	public boolean hasAir() {
		if (dataAirSegments == null) {
			return false;
		}
		for (final int[] dataAirSegment : dataAirSegments) {
			if (dataAirSegment == null) {
				continue;
			}
			for (final int dataAirBlock : dataAirSegment) {
				if ((dataAirBlock & StateAir.CONCENTRATION_MASK) != 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isNotEmpty() {
		if (dataAirSegments == null) {
			return false;
		}
		for (final int[] dataAirSegment : dataAirSegments) {
			if (dataAirSegment == null) {
				continue;
			}
			for (final int dataAirBlock : dataAirSegment) {
				if (!StateAir.isEmptyData(dataAirBlock)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void updateTick(final World world) {
		// skip empty chunk
		if (dataAirSegments == null) {
			return;
		}
		
		tickCurrent = (tickCurrent + 1) & 0xFF;
		int countBlocks = 0;
		int countTickingBlocks = 0;
		for (int indexSegment = 0; indexSegment < CHUNK_SIZE_SEGMENTS; indexSegment++) {
			int[] dataAirSegment = dataAirSegments[indexSegment];
			byte[] tickAirSegment = tickAirSegments[indexSegment];
			
			// skip empty segments
			if (dataAirSegment == null) {
				continue;
			}
			
			// scan all blocks
			int countEmpty = 0;
			countBlocks += dataAirSegment.length;
			for (int indexBlock = 0; indexBlock < SEGMENT_SIZE_BLOCKS; indexBlock++) {
				final int dataAirBlock = dataAirSegment[indexBlock];
				final byte tickAirBlock = tickAirSegment[indexBlock];
				// skip empty positions
				if (StateAir.isEmptyData(dataAirBlock)) {
					countEmpty++;
					continue;
				}
				// increase update speed in low pressure areas 
				if ((tickCurrent & 0x7F) != tickAirBlock) {
					continue;
				}
				// update
				countTickingBlocks++;
				final int x = (chunkCoordIntPair.chunkXPos << 4) + ((indexBlock & 0x00F0) >> 4);
				final int y = (indexSegment << 4) + ((indexBlock & 0x0F00) >> 8);
				final int z = (chunkCoordIntPair.chunkZPos << 4) + (indexBlock & 0x000F);
				AirSpreader.execute(world, x, y, z);
			}
			
			// clear empty segment
			if (countEmpty == dataAirSegment.length) {
				dataAirSegments[indexSegment] = null;
				tickAirSegments[indexSegment] = null;
			}
		}
		AirSpreader.clearCache();
		if (isModified) {
			isModified = false;
			world.getChunkFromChunkCoords(chunkCoordIntPair.chunkXPos, chunkCoordIntPair.chunkZPos).setChunkModified();
		}
		if ( WarpDriveConfig.LOGGING_CHUNK_HANDLER
		  && ChunkHandler.delayLogging == 0
		  && countBlocks != 0 ) {
			WarpDrive.logger.info(String.format("Dimension %d chunk (%d %d) had %d / %d blocks ticked",
			                                    world.provider.dimensionId,
			                                    chunkCoordIntPair.chunkXPos,
			                                    chunkCoordIntPair.chunkZPos,
			                                    countTickingBlocks,
			                                    countBlocks));
		}
	}
	
	
	/* object overrides */
	@Override
	public int hashCode() {
		return chunkCoordIntPair.chunkXPos & 0xFFFF | (chunkCoordIntPair.chunkZPos & 0xFFFF) << 16;
	}
	
	@Override
	public boolean equals(final Object object) {
		if (this == object) {
			return true;
		} else if (!(object instanceof ChunkData)) {
			return false;
		} else {
			final ChunkData chunkData = (ChunkData) object;
			return chunkCoordIntPair.chunkXPos == chunkData.chunkCoordIntPair.chunkXPos
			    && chunkCoordIntPair.chunkZPos == chunkData.chunkCoordIntPair.chunkZPos;
		}
	}
	
	@Override
	public String toString() {
		final ChunkPosition chunkPosition = getChunkPosition();
		return String.format("%s (%d %d @ %d %d %d) isLoaded %s hasAir %s isNotEmpty %s )", 
		                     getClass().getSimpleName(),
		                     chunkCoordIntPair.chunkXPos, chunkCoordIntPair.chunkZPos,
		                     chunkPosition.chunkPosX, chunkPosition.chunkPosY, chunkPosition.chunkPosZ, 
		                     isLoaded, hasAir(), isNotEmpty());
	}
}
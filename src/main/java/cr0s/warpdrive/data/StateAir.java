package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.BlockAbstractOmnipanel;
import cr0s.warpdrive.block.breathing.BlockAirFlow;
import cr0s.warpdrive.block.breathing.BlockAirSource;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.event.ChunkHandler;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidBase;

public class StateAir {
	
	public static final int AIR_DEFAULT = 0x060000C0;      // default is the unknown state
	
	// highest bit is unusable since Java only supports signed primitives (mostly)
	static final int USED_MASK                 = 0b01110111111111111111111100001111;
	
	static final int CONCENTRATION_MASK        = 0b00000000000000000000000000011111;
	static final int CONCENTRATION_MAX         = 0b00000000000000000000000000011111;
	static final int GENERATOR_DIRECTION_MASK  = 0b00000000000000000000000011100000;
	static final int GENERATOR_PRESSURE_MASK   = 0b00000000000000001111111100000000;
	static final int VOID_PRESSURE_MASK        = 0b00000000111111110000000000000000;
	static final int VOID_DIRECTION_MASK       = 0b00000111000000000000000000000000;
	static final int BLOCK_MASK                = 0b01110000000000000000000000000000;
	static final int GENERATOR_DIRECTION_SHIFT = 5;
	static final int GENERATOR_PRESSURE_SHIFT  = 8;
	static final int VOID_PRESSURE_SHIFT       = 16;
	static final int VOID_DIRECTION_SHIFT      = 24;
	static final int GENERATOR_PRESSURE_MAX    = 255;
	static final int VOID_PRESSURE_MAX         = 255;
	
	static final int BLOCK_UNKNOWN             = 0b00000000000000000000000000000000;   // 00000000 = not read yet
	static final int BLOCK_SEALER              = 0b00010000000000000000000000000000;   // 10000000 = any full, non-air block: stone, etc.
	static final int BLOCK_AIR_PLACEABLE       = 0b00100000000000000000000000000000;   // 20000000 = vanilla air/void, modded replaceable air
	static final int BLOCK_AIR_FLOW            = 0b00110000000000000000000000000000;   // 30000000 = WarpDrive air flow (i.e. block is already placed, let it be)
	static final int BLOCK_AIR_SOURCE          = 0b01000000000000000000000000000000;   // 40000000 = WarpDrive air source
	static final int BLOCK_AIR_NON_PLACEABLE_V = 0b01010000000000000000000000000000;   // 50000000 = any non-full block that leaks only vertically (glass panes)
	static final int BLOCK_AIR_NON_PLACEABLE_H = 0b01100000000000000000000000000000;   // 60000000 = any non-full block that leaks only horizontally (enchantment table, tiled dirt, fluid)
	static final int BLOCK_AIR_NON_PLACEABLE   = 0b01110000000000000000000000000000;   // 70000000 = any non-full block that leaks all around (crops, piping)
	
	// Tick is skipped if all bits are 0 in the TICKING_MASK
	static final int TICKING_MASK              = VOID_PRESSURE_MASK | GENERATOR_PRESSURE_MASK | CONCENTRATION_MASK;
	
	private ChunkData chunkData;
	private Chunk chunk;
	private int x;
	private int y;
	private int z;
	protected int dataAir;    // original air data provided
	protected Block block;    // original block
	public byte concentration;
	public short pressureGenerator;
	public short pressureVoid;
	public ForgeDirection directionGenerator;   // direction toward source
	public ForgeDirection directionVoid;        // direction toward source
	
	public StateAir(final ChunkData chunkData) {
		this.chunkData = chunkData;
		this.chunk = null;
	}
	
	public void refresh(final World world, final int x, final int y, final int z) {
		this.x = x;
		this.y = y;
		this.z = z;
		refresh(world);
	}
	
	public void refresh(final World world, final StateAir stateAir, final ForgeDirection forgeDirection) {
		x = stateAir.x + forgeDirection.offsetX;
		y = stateAir.y + forgeDirection.offsetY;
		z = stateAir.z + forgeDirection.offsetZ;
		refresh(world);
	}
	
	private void refresh(final World world) {
		// update chunk cache
		if (chunkData == null || !chunkData.isInside(x, y, z)) {
			chunkData = ChunkHandler.getChunkData(world, x, y, z, false);
			if (chunkData == null) {
				WarpDrive.logger.error(String.format("State air trying to get data from an non-loaded chunk in %s @ (%d %d %d)",
				                                     world.provider.getDimensionName(), x, y, z));
				assert(false);
			}
			chunk = null;
		}
		if (chunk == null) {
			chunk = world.getChunkFromBlockCoords(x, z);
		}
		
		// get actual data
		block = null;
		dataAir = chunkData.getDataAir(x, y, z);
		if (dataAir == 0) {
			dataAir = AIR_DEFAULT;
		}
		
		// extract scalar values
		concentration = (byte) (dataAir & CONCENTRATION_MASK);
		pressureGenerator = (short) ((dataAir & GENERATOR_PRESSURE_MASK) >> GENERATOR_PRESSURE_SHIFT);
		pressureVoid = (short) ((dataAir & VOID_PRESSURE_MASK) >> VOID_PRESSURE_SHIFT);
		directionGenerator = ForgeDirection.getOrientation((dataAir & GENERATOR_DIRECTION_MASK) >> GENERATOR_DIRECTION_SHIFT);
		directionVoid = ForgeDirection.getOrientation((dataAir & VOID_DIRECTION_MASK) >> VOID_DIRECTION_SHIFT);
		
		// update block cache
		if ((dataAir & BLOCK_MASK) == BLOCK_UNKNOWN) {
			updateBlockCache(world);
		}
		updateVoidSource();
	}
	
	public void clearCache() {
		// clear cached chunk references at end of tick
		// this is required for chunk unloading and object refreshing
		chunkData = null;
		chunk = null;
	}
	
	public void updateBlockCache(final World world) {
		if (y >= 0 && y < 256) {
			block = chunk.getBlock(x & 15, y, z & 15);
		} else {
			block = Blocks.air;
		}
		updateBlockType(world);
	}
	
	private void updateVoidSource() {
		if (!isAir()) {// sealed blocks have no pressure
			setGenerator((short) 0, ForgeDirection.UNKNOWN);
			setVoid((short) 0, ForgeDirection.UNKNOWN);
			
		} else if (pressureGenerator == 0) {// no generator in range => clear to save resources
			setVoid((short) 0, ForgeDirection.UNKNOWN);
			
		} else if (pressureGenerator == 1) {// at generator range => this is a void source
			setVoid((short) VOID_PRESSURE_MAX, directionGenerator.getOpposite());
			
		} else if (y == 0) {// at bottom of map => this is a void source
			setVoid((short) VOID_PRESSURE_MAX, ForgeDirection.DOWN);
			
		} else if (y == 255) {// at top of map => this is a void source
			setVoid((short) VOID_PRESSURE_MAX, ForgeDirection.UP);
			
		} else if (block != null) {// only check if block was updated
			// check if sky is visible, which means we're in the void
			// note: on 1.7.10, getHeightValue() is for seeing the sky (it goes through transparent blocks)
			// getPrecipitationHeight() returns the altitude of the highest block that stops movement or is a liquid
			final int highestBlock = chunk.getPrecipitationHeight(x & 15, z & 15);
			final boolean isVoid = highestBlock < y;
			if (isVoid) {
				setVoid((short) VOID_PRESSURE_MAX, ForgeDirection.UP);
			} else if (pressureVoid == VOID_PRESSURE_MAX) {
				setVoid((short) 0, ForgeDirection.UNKNOWN);
			}
		}
		// (propagation is done when spreading air itself)
	}
	
	private void setBlockToNoAir(final World world) {
		world.setBlock(x, y, z, Blocks.air, 0, 2);
		block = Blocks.air;
		updateBlockType(world);
	}
	
	private void setBlockToAirFlow(final World world) {
		world.setBlock(x, y, z, WarpDrive.blockAirFlow, 0, 2);
		block = WarpDrive.blockAirFlow;
		updateBlockType(world);
	}
	
	public boolean setAirSource(final World world, final ForgeDirection direction, final short pressure) {
		assert(block != null);
		
		final boolean isPlaceable = (dataAir & BLOCK_MASK) == BLOCK_AIR_PLACEABLE || (dataAir & BLOCK_MASK) == BLOCK_AIR_FLOW || (dataAir & BLOCK_MASK) == BLOCK_AIR_SOURCE;
		final boolean updateRequired = (block != WarpDrive.blockAirSource)
		         || pressureGenerator != pressure
		         || pressureVoid != 0
		         || concentration != CONCENTRATION_MAX;
		
		if (updateRequired && isPlaceable) {
			// block metadata is direction going away from generator, while internal direction is towards the generator
			world.setBlock(x, y, z, WarpDrive.blockAirSource, direction.ordinal(), 2);
			block = WarpDrive.blockAirSource;
			updateBlockType(world);
			setGeneratorAndUpdateVoid(world, pressure, direction.getOpposite());
			setConcentration(world, (byte) CONCENTRATION_MAX);
		}
		return updateRequired;
	}
	
	public void removeAirSource(final World world) {
		setBlockToAirFlow(world);
		setConcentration(world, (byte) 1);
	}
	
	private void updateBlockType(final World world) {
		assert(block != null);
		final int typeBlock;
		if (block instanceof BlockAirFlow) {
			typeBlock = BLOCK_AIR_FLOW;
			
		} else if (block == Blocks.air) {// vanilla air
			typeBlock = BLOCK_AIR_PLACEABLE;
			
		} else if (block.getMaterial() == Material.leaves || block.isFoliage(world, x, y, z)) {// leaves and assimilated
			typeBlock = BLOCK_AIR_NON_PLACEABLE;
			
		} else if (block instanceof BlockAirSource) {
			typeBlock = BLOCK_AIR_SOURCE;
			
		} else if (block.isNormalCube()) {
			typeBlock = BLOCK_SEALER;
			
		} else if (block instanceof BlockAbstractOmnipanel) {
			typeBlock = BLOCK_SEALER;
			
		} else if (block instanceof BlockStaticLiquid || block instanceof BlockDynamicLiquid) {// vanilla liquid (water & lava sources or flowing)
			// metadata = 0 for source, 8/9 for vertical flow
			// 2 superposed sources would still be 0, so we can't use metadata. Instead, we're testing explicitly the block above
			// we assume it's the same fluid, since water and lava won't mix anyway
			final Block blockAbove = world.getBlock(x, y + 1, z);
			if (blockAbove == block || blockAbove instanceof BlockStaticLiquid || blockAbove instanceof BlockDynamicLiquid) {
				typeBlock = BLOCK_SEALER;
			} else {
				typeBlock = BLOCK_AIR_NON_PLACEABLE_H;
			}
			
		} else if (block instanceof BlockFluidBase) {// forge fluid
			// metadata = 0 for source, 1 for flowing full (first horizontal or any vertical, 2+ for flowing away
			// check density to get fluid direction
			final int density = BlockFluidBase.getDensity(world, x, y, z);
			// positive density means fluid flowing down, so checking upper block
			final Block blockFlowing = world.getBlock(x, y + (density > 0 ? 1 : -1), z);
			if (blockFlowing == block) {
				typeBlock = BLOCK_SEALER;
			} else {
				typeBlock = BLOCK_AIR_NON_PLACEABLE_H;
			}
			
		} else if (block.isAir(world, x, y, z) || block.isReplaceable(world, x, y, z)) {// decoration like grass, modded replaceable air
			typeBlock = BLOCK_AIR_NON_PLACEABLE;
			
		} else if (block instanceof BlockPane) {
			typeBlock = BLOCK_AIR_NON_PLACEABLE_V;
			
		} else {
			final AxisAlignedBB axisAlignedBB = block.getCollisionBoundingBoxFromPool(world, x, y, z);
			if (axisAlignedBB == null) {
				typeBlock = BLOCK_AIR_NON_PLACEABLE;
			} else {
				final boolean fullX = axisAlignedBB.maxX - axisAlignedBB.minX > 0.99D;
				final boolean fullY = axisAlignedBB.maxY - axisAlignedBB.minY > 0.99D;
				final boolean fullZ = axisAlignedBB.maxZ - axisAlignedBB.minZ > 0.99D;
				if (fullX && fullY && fullZ) {// all axis are full, it's probably a full block with custom render
					typeBlock = BLOCK_SEALER;
				} else if (fullX && fullZ) {// it's sealed vertically, leaking horizontally
					typeBlock = BLOCK_AIR_NON_PLACEABLE_H;
				} else if (fullY && (fullX || fullZ)) {// it's sealed horizontally, leaking vertically
					typeBlock = BLOCK_AIR_NON_PLACEABLE_V;
				} else {// at most one axis is full => no side is full => leaking all around
					typeBlock = BLOCK_AIR_NON_PLACEABLE;
				}
			}
		}
		
		// save only as needed (i.e. block type changed)
		if ((dataAir & BLOCK_MASK) != typeBlock) {
			dataAir = (dataAir & ~BLOCK_MASK) | typeBlock;
			chunkData.setDataAir(x, y, z, dataAir);
		}
	}
	
	public void setConcentration(final World world, final byte concentrationNew) {
		// update world as needed
		// any air concentration?
		assert(concentrationNew >= 0 && concentrationNew <= CONCENTRATION_MAX);
		if (concentrationNew == 0) {
			if (isAirFlow()) {// remove air block...
				// confirm block state
				if (block == null) {
					updateBlockCache(world);
				}
				// remove our block if it's actually there
				if (isAirFlow()) {
					setBlockToNoAir(world);
				}
			}
			
		} else {
			if ((dataAir & BLOCK_MASK) == BLOCK_AIR_PLACEABLE) {// add air block...
				// confirm block state
				if (block == null) {
					final int dataAirLegacy = dataAir;
					updateBlockCache(world);
					if ((dataAir & BLOCK_MASK) != BLOCK_AIR_PLACEABLE) {
						// state was out of sync => skip
						if (WarpDrive.isDev) {
							WarpDrive.logger.info(String.format("Desynchronized air state detected at %d %d %d: %8x -> %s",
							                                    x, y, z, dataAirLegacy, this));
						}
						return;
					}
				}
				setBlockToAirFlow(world);
			}
		}
		
		if (concentration != concentrationNew) {
			dataAir = (dataAir & ~CONCENTRATION_MASK) | concentrationNew;
			concentration = concentrationNew;
			chunkData.setDataAir(x, y, z, dataAir);
		}
		if (WarpDriveConfig.BREATHING_AIR_BLOCK_DEBUG && isAirFlow()) {
			if (block == null) {
				updateBlockCache(world);
			}
			if (isAirFlow()) {
				world.setBlockMetadataWithNotify(x, y, z, (int) concentrationNew, 3);
			}
		}
	}
	
	protected void setGeneratorAndUpdateVoid(final World world, final short pressureNew, final ForgeDirection directionNew) {
		if (pressureNew == 0 && pressureVoid > 0) {
			removeGeneratorAndCascade(world);
		} else {
			setGenerator(pressureNew, directionNew);
			updateVoidSource();
		}
	}
	
	private void setGenerator(final short pressureNew, final ForgeDirection directionNew) {
		boolean isUpdated = false;
		if (pressureNew != pressureGenerator) {
			assert (pressureNew >= 0 && pressureNew <= GENERATOR_PRESSURE_MAX);
			
			dataAir = (dataAir & ~GENERATOR_PRESSURE_MASK) | (pressureNew << GENERATOR_PRESSURE_SHIFT);
			pressureGenerator = pressureNew;
			isUpdated = true;
		}
		if (directionNew != directionGenerator) {
			dataAir = (dataAir & ~GENERATOR_DIRECTION_MASK) | (directionNew.ordinal() << GENERATOR_DIRECTION_SHIFT);
			directionGenerator = directionNew;
			isUpdated = true;
		}
		if (isUpdated) {
			chunkData.setDataAir(x, y, z, dataAir);
		}
		assert (pressureGenerator != 0 || directionGenerator == ForgeDirection.UNKNOWN);
		assert (pressureGenerator == 0 || pressureGenerator == GENERATOR_PRESSURE_MAX || directionGenerator != ForgeDirection.UNKNOWN);
		assert (pressureGenerator == 0 || directionGenerator != ForgeDirection.UNKNOWN);
	}
	protected void removeGeneratorAndCascade(final World world) {
		removeGeneratorAndCascade(world, WarpDriveConfig.BREATHING_VOLUME_UPDATE_DEPTH_BLOCKS);
	}
	private void removeGeneratorAndCascade(final World world, final int depth) {
		if (pressureGenerator != 0) {
			assert (directionGenerator != ForgeDirection.UNKNOWN);
			dataAir = (dataAir & ~(GENERATOR_PRESSURE_MASK | GENERATOR_DIRECTION_MASK)) | (ForgeDirection.UNKNOWN.ordinal() << GENERATOR_DIRECTION_SHIFT);
			pressureGenerator = 0;
			directionGenerator = ForgeDirection.UNKNOWN;
			chunkData.setDataAir(x, y, z, dataAir);
			if (depth > 0) {
				final StateAir stateAir = new StateAir(chunkData);
				for (final ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
					stateAir.refresh(world, this, direction);
					if ( stateAir.pressureGenerator > 0
					  && stateAir.directionGenerator == direction.getOpposite() ) {
						stateAir.removeGeneratorAndCascade(world, depth - 1);
					}
				}
			}
		}
	}
	
	protected void setVoid(final short pressureNew, final ForgeDirection directionNew) {
		boolean isUpdated = false;
		if (pressureNew != pressureVoid) {
			assert (pressureNew >= 0 && pressureNew <= VOID_PRESSURE_MAX);
			
			dataAir = (dataAir & ~VOID_PRESSURE_MASK) | (pressureNew << VOID_PRESSURE_SHIFT);
			pressureVoid = pressureNew;
			isUpdated = true;
		}
		if (directionNew != directionVoid) {
			dataAir = (dataAir & ~VOID_DIRECTION_MASK) | (directionNew.ordinal() << VOID_DIRECTION_SHIFT);
			directionVoid = directionNew;
			isUpdated = true;
		}
		if (isUpdated) {
			chunkData.setDataAir(x, y, z, dataAir);
		}
		assert (pressureVoid != 0 || directionVoid == ForgeDirection.UNKNOWN);
		assert (pressureVoid == 0 || directionVoid != ForgeDirection.UNKNOWN);
		assert (pressureVoid == 0 || pressureVoid == VOID_PRESSURE_MAX || directionVoid != ForgeDirection.UNKNOWN);
	}
	protected void removeVoidAndCascade(final World world) {
		removeVoidAndCascade(world, WarpDriveConfig.BREATHING_VOLUME_UPDATE_DEPTH_BLOCKS);
	}
	private void removeVoidAndCascade(final World world, final int depth) {
		if (pressureVoid != 0) {
			assert (directionVoid != ForgeDirection.UNKNOWN);
			dataAir = (dataAir & ~(VOID_PRESSURE_MASK | VOID_DIRECTION_MASK)) | (ForgeDirection.UNKNOWN.ordinal() << VOID_DIRECTION_SHIFT);
			pressureVoid = 0;
			directionVoid = ForgeDirection.UNKNOWN;
			chunkData.setDataAir(x, y, z, dataAir);
			if (depth > 0) {
				final StateAir stateAir = new StateAir(chunkData);
				for (final ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
					stateAir.refresh(world, this, direction);
					if (stateAir.pressureVoid > 0 && stateAir.directionVoid == direction.getOpposite()) {
						stateAir.removeVoidAndCascade(world, depth - 1);
					}
				}
			}
		}
	}
	
	public boolean isAir() {
		return (dataAir & BLOCK_MASK) != BLOCK_SEALER;
	}
	
	public boolean isAir(final ForgeDirection forgeDirection) {
		switch (dataAir & BLOCK_MASK) {
			case BLOCK_SEALER              : return false;
			case BLOCK_AIR_PLACEABLE       : return true;
			case BLOCK_AIR_FLOW            : return true;
			case BLOCK_AIR_SOURCE          : return true;
			case BLOCK_AIR_NON_PLACEABLE_V : return forgeDirection.offsetY != 0;
			case BLOCK_AIR_NON_PLACEABLE_H : return forgeDirection.offsetY == 0;
			case BLOCK_AIR_NON_PLACEABLE   : return true;
			default: return false;
		}
	}
	
	public boolean isAirSource() {
		return (dataAir & BLOCK_MASK) == BLOCK_AIR_SOURCE;
	}
	
	public boolean isAirFlow() {
		return (dataAir & BLOCK_MASK) == BLOCK_AIR_FLOW;
	}
	
	public boolean isVoidSource() {
		return pressureVoid == VOID_PRESSURE_MAX;
	}
	
	protected boolean isLeakingHorizontally() {
		return (dataAir & BLOCK_MASK) == BLOCK_AIR_NON_PLACEABLE_H;
	}
	
	protected boolean isLeakingVertically() {
		return (dataAir & BLOCK_MASK) == BLOCK_AIR_NON_PLACEABLE_V;
	}
	
	protected static boolean isEmptyData(final int dataAir) {
		return (dataAir & TICKING_MASK) == 0
		    && (dataAir & StateAir.BLOCK_MASK) != StateAir.BLOCK_AIR_FLOW;
	}
	
	private static final short[] rotDirection = {  0,  1,  5,  4,  2,  3,  6,  7 };
	private static int rotateDirection(final int direction, final byte rotationSteps) {
		switch (rotationSteps) {
		case 1:
			return rotDirection[direction];
		case 2:
			return rotDirection[rotDirection[direction]];
		case 3:
			return rotDirection[rotDirection[rotDirection[direction]]];
		default:
			return direction;
		}
	}
	
	public static int rotate(final int dataAir, final byte rotationSteps) {
		final int dataNoDirection = dataAir & ~(GENERATOR_DIRECTION_MASK | VOID_DIRECTION_MASK);
		final int directionGenerator = rotateDirection((dataAir & GENERATOR_DIRECTION_MASK) >> GENERATOR_DIRECTION_SHIFT, rotationSteps);
		final int directionVoid = rotateDirection((dataAir & VOID_DIRECTION_MASK) >> VOID_DIRECTION_SHIFT, rotationSteps);
		return dataNoDirection | (directionGenerator << GENERATOR_DIRECTION_SHIFT) | (directionVoid << VOID_DIRECTION_SHIFT);
	}
	
	public static void dumpAroundEntity(final EntityPlayer entityPlayer) {
		StateAir stateAirs[][][] = new StateAir[3][3][3];
		for (int dy = -1; dy <= 1; dy++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dx = -1; dx <= 1; dx++) {
					StateAir stateAir = new StateAir(null);
					stateAir.refresh(entityPlayer.worldObj,
					                 MathHelper.floor_double(entityPlayer.posX) + dx,
					                 MathHelper.floor_double(entityPlayer.posY) + dy,
					                 MathHelper.floor_double(entityPlayer.posZ) + dz);
					stateAirs[dx + 1][dy + 1][dz + 1] = stateAir;
				}
			}
		}
		StringBuilder message = new StringBuilder("------------------------------------------------\n§3Air, §aGenerator §7and §dVoid §7stats at " + entityPlayer.ticksExisted);
		for (int indexY = 2; indexY >= 0; indexY--) {
			for (int indexZ = 2; indexZ >= 0; indexZ--) {
				message.append("\n");
				for (int indexX = 0; indexX <= 2; indexX++) {
					StateAir stateAir = stateAirs[indexX][indexY][indexZ];
					final String stringValue = String.format("%2d", 100 + stateAir.concentration).substring(1);
					message.append(String.format("§3%s ", stringValue));
				}
				message.append("§f| ");
				for (int indexX = 0; indexX <= 2; indexX++) {
					StateAir stateAir = stateAirs[indexX][indexY][indexZ];
					final String stringValue = String.format("%X", 0x100 + stateAir.pressureGenerator).substring(1);
					final String stringDirection = directionToChar(stateAir.directionGenerator);
					message.append(String.format("§e%s §a%s ", stringValue, stringDirection));
				}
				message.append("§f| ");
				for (int indexX = 0; indexX <= 2; indexX++) {
					StateAir stateAir = stateAirs[indexX][indexY][indexZ];
					final String stringValue = String.format("%X", 0x100 + stateAir.pressureVoid).substring(1);
					final String stringDirection = directionToChar(stateAir.directionVoid);
					message.append(String.format("§e%s §d%s ", stringValue, stringDirection));
				}
				if (indexZ == 2) message.append("§f\\");
				else if (indexZ == 1) message.append(String.format("§f  > y = %d", stateAirs[1][indexY][indexZ].y));
				else message.append("§f/");
			}
		}
		Commons.addChatMessage(entityPlayer, message.toString());
	}
	
	private static String directionToChar(final ForgeDirection direction) {
		switch (direction) {
		case UP     : return "U";
		case DOWN   : return "D";
		case NORTH  : return "N";
		case SOUTH  : return "S";
		case EAST   : return "E";
		case WEST   : return "W";
		case UNKNOWN: return "?";
		default     : return "x";
		}
	}
	
	@Override
	public String toString() {
		return String.format("StateAir @ (%6d %3d %6d) data 0x%08x, concentration %d, block %s",
		                     x, y, z, dataAir, concentration, block);
	}
}

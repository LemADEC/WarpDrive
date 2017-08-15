package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatCarpentersBlocks implements IBlockTransformer {
	
	private static Class<?> blockCoverable;
	// private static Class<?> blockCarpentersBarrier;
	private static Class<?> blockCarpentersBed;
	// private static Class<?> blockCarpentersBlock;
	private static Class<?> blockCarpentersButton;
	private static Class<?> blockCarpentersCollapsibleBlock;
	// private static Class<?> blockCarpentersDaylightSensor;
	private static Class<?> blockCarpentersDoor;
	// private static Class<?> blockCarpentersFlowerPot;
	private static Class<?> blockCarpentersGarageDoor;
	private static Class<?> blockCarpentersGate;
	private static Class<?> blockCarpentersHatch;
	private static Class<?> blockCarpentersLadder;
	private static Class<?> blockCarpentersLever;
	private static Class<?> blockCarpentersPressurePlate;
	private static Class<?> blockCarpentersSafe;
	private static Class<?> blockCarpentersSlope;
	private static Class<?> blockCarpentersStairs;
	private static Class<?> blockCarpentersTorch;
	
	
	public static void register() {
		try {
			blockCoverable = Class.forName("com.carpentersblocks.block.BlockCoverable");    // common
			// blockCarpentersBarrier = Class.forName("com.carpentersblocks.block.BlockCarpentersBarrier"); no visible change
			blockCarpentersBed = Class.forName("com.carpentersblocks.block.BlockCarpentersBed");
			// blockCarpentersBlock = Class.forName("com.carpentersblocks.block.BlockCarpentersBlock");
			blockCarpentersButton = Class.forName("com.carpentersblocks.block.BlockCarpentersButton");
			blockCarpentersCollapsibleBlock = Class.forName("com.carpentersblocks.block.BlockCarpentersCollapsibleBlock");
			// blockCarpentersDaylightSensor = Class.forName("com.carpentersblocks.block.BlockCarpentersDaylightSensor"); no visible change
			blockCarpentersDoor = Class.forName("com.carpentersblocks.block.BlockCarpentersDoor");
			// blockCarpentersFlowerPot = Class.forName("com.carpentersblocks.block.BlockCarpentersFlowerPot"); no visible change
			blockCarpentersGarageDoor = Class.forName("com.carpentersblocks.block.BlockCarpentersGarageDoor");
			blockCarpentersGate = Class.forName("com.carpentersblocks.block.BlockCarpentersGate");
			blockCarpentersHatch = Class.forName("com.carpentersblocks.block.BlockCarpentersHatch");
			blockCarpentersLadder = Class.forName("com.carpentersblocks.block.BlockCarpentersLadder");
			blockCarpentersLever = Class.forName("com.carpentersblocks.block.BlockCarpentersLever");
			blockCarpentersPressurePlate = Class.forName("com.carpentersblocks.block.BlockCarpentersPressurePlate");
			blockCarpentersSafe = Class.forName("com.carpentersblocks.block.BlockCarpentersSafe");
			blockCarpentersSlope = Class.forName("com.carpentersblocks.block.BlockCarpentersSlope");
			blockCarpentersStairs = Class.forName("com.carpentersblocks.block.BlockCarpentersStairs");
			blockCarpentersTorch = Class.forName("com.carpentersblocks.block.BlockCarpentersTorch");
			
			WarpDriveConfig.registerBlockTransformer("CarpentersBlocks", new CompatCarpentersBlocks());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return blockCoverable.isInstance(block);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, StringBuilder reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	// private static final byte[] rotSlope = {  2,  3,  1,  0,  6,  7,  5,  4, 10, 11, 9, 8, 14, 15, 13, 12, 18, 19, 17, 16, 22, 23, 21, 20, 26, 27, 25, 24, 30, 31, 29, 28, 34, 35, 33, 32, 38, 39, 37, 36, 42, 43, 41, 40, 44, 45, 48, 49, 47, 46, 51, 50, 54, 55, 53, 52, 58, 59, 57, 56, 60, 63, 64, 62, 61 };
	
	private static final int[]  rotBed    = {  1,  2,  3,  0,  5,  6,  7,  4 }; // actual is * 8192
	private static final byte[] rotButton = {  0,  1,  5,  4,  2,  3,  6,  7 }; // Button, pressure plate, torch, garage door
	private static final int[]  rotDoor   = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 13, 14, 15, 12 }; // actual is * 16
	private static final int[]  rotGate   = {  2,  1,  0,  3,  7,  6,  4,  5 }; // actual is * 16
	private static final int[]  rotHatch  = { 12, 13, 14, 15,  8,  9, 10, 11,  0,  1,  2,  3,  4,  5,  6,  7 }; // actual is * 8
	private static final byte[] rotLadder = {  1,  0,  5,  4,  2,  3 };
	private static final byte[] rotLever  = { 16, 17,  2,  3, 18, 19,  6,  7, 24, 25, 10, 11, 26, 27, 14, 15,  0,  1,  5,  4, 20, 21, 22, 23,  8,  9, 13, 12, 28, 29, 30, 31, 32 };
	private static final byte[] rotSafe   = {  1,  2,  3,  0 };
	
	//                                         0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30  31  32  33  34  35  36  37  38  39  40  41  42  43  44  45  46  47  48  49  50  51  52  53  54  55  56  57  58  59  60  61  62  63  64 
	private static final byte[] rotSlope  = {  3,  2,  0,  1,  7,  6,  4,  5, 11, 10,  8,  9, 15, 14, 12, 13, 19, 18, 16, 17, 23, 22, 20, 21, 27, 26, 24, 25, 31, 30, 28, 29, 35, 34, 32, 33, 39, 38, 36, 37, 43, 42, 40, 41, 44, 45, 49, 48, 46, 47, 51, 50, 55, 54, 52, 53, 59, 58, 56, 57, 60, 64, 63, 61, 62 };
	
	// Unused indexes for Stairs:                                                             12  13  14  15  16  17  18  19                                  28  29  30  31  32  33  34  35  36  37  38  39  40  41  42  43  44  45  46  47  48  49  50  51  52  53  54  55  56  57  58  59  60  61  62  63  64 
	private static final byte[] rotStair  = {  3,  2,  0,  1,  7,  6,  4,  5, 11, 10,  8,  9, 15, 14, 12, 13, 19, 18, 16, 17, 23, 22, 20, 21, 27, 26, 24, 25, 31, 30, 28, 29, 35, 34, 32, 33, 39, 38, 36, 37, 43, 42, 40, 41, 44, 45, 49, 48, 46, 47, 51, 50, 55, 54, 52, 53, 59, 58, 56, 57, 60, 64, 63, 61, 62 };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if (blockCarpentersBed.isInstance(block)) {
			final int metadataNBT = nbtTileEntity.getInteger("cbMetadata");
			final int rotation = (metadataNBT >> 13) & 7;
			final int state = metadataNBT & 0x1FFF;
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("cbMetadata", state | (rotBed[rotation] << 13));
				return metadata;
			case 2:
				nbtTileEntity.setInteger("cbMetadata", state | (rotBed[rotBed[rotation]] << 13));
				return metadata;
			case 3:
				nbtTileEntity.setInteger("cbMetadata", state | (rotBed[rotBed[rotBed[rotation]]] << 13));
				return metadata;
			default:
				return metadata;
			}
		}
		
		if ( blockCarpentersButton.isInstance(block)
		  || blockCarpentersPressurePlate.isInstance(block)
		  || blockCarpentersTorch.isInstance(block) ) {
			final byte metadataNBT = nbtTileEntity.getByte("cbMetadata");
			final int rotation = metadataNBT & 0x07;
			final int state = metadataNBT & 0xF8;
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("cbMetadata", (byte) (state | rotButton[rotation]));
				return metadata;
			case 2:
				nbtTileEntity.setByte("cbMetadata", (byte) (state | rotButton[rotButton[rotation]]));
				return metadata;
			case 3:
				nbtTileEntity.setByte("cbMetadata", (byte) (state | rotButton[rotButton[rotButton[rotation]]]));
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersCollapsibleBlock.isInstance(block)) {
			final int metadataNBT = nbtTileEntity.getInteger("cbMetadata");
			final int state = metadataNBT & 7;
			final int weightXPZP = (metadataNBT & 0x7FFF07) >>  3;
			final int weightXNZP = (metadataNBT & 0x7C1FFF) >> 13;
			final int weightXNZN = (metadataNBT & 0x03FFFF) >> 18;
			final int weightXPZN = (metadataNBT & 0x7FE0FF) >>  8;
			
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("cbMetadata", state | (weightXPZP << 13) | (weightXNZP << 18) | (weightXNZN <<  8) | (weightXPZN <<  3));
				return metadata;
			case 2:
				nbtTileEntity.setInteger("cbMetadata", state | (weightXPZP << 18) | (weightXNZP <<  8) | (weightXNZN <<  3) | (weightXPZN << 13));
				return metadata;
			case 3:
				nbtTileEntity.setInteger("cbMetadata", state | (weightXPZP <<  8) | (weightXNZP <<  3) | (weightXNZN << 13) | (weightXPZN << 18));
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersDoor.isInstance(block)) {
			final int metadataNBT = nbtTileEntity.getInteger("cbMetadata");
			final int rotation = (metadataNBT >> 4) & 15;
			final int state = metadataNBT & 0xFF0F;
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("cbMetadata", state | (rotDoor[rotation] << 4));
				return metadata;
			case 2:
				nbtTileEntity.setInteger("cbMetadata", state | (rotDoor[rotDoor[rotation]] << 4));
				return metadata;
			case 3:
				nbtTileEntity.setInteger("cbMetadata", state | (rotDoor[rotDoor[rotDoor[rotation]]] << 4));
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersGarageDoor.isInstance(block)) {
			final int metadataNBT = nbtTileEntity.getInteger("cbMetadata");
			final int rotation = (metadataNBT >> 4) & 7;
			final int state = metadataNBT & 0xFF8F;
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("cbMetadata", state | (rotButton[rotation] << 4));
				return metadata;
			case 2:
				nbtTileEntity.setInteger("cbMetadata", state | (rotButton[rotButton[rotation]] << 4));
				return metadata;
			case 3:
				nbtTileEntity.setInteger("cbMetadata", state | (rotButton[rotButton[rotButton[rotation]]] << 4));
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersGate.isInstance(block)) {
			final int metadataNBT = nbtTileEntity.getInteger("cbMetadata");
			final int rotation = (metadataNBT >> 4) & 7;
			final int state = metadataNBT & 0xFF8F;
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("cbMetadata", state | (rotGate[rotation] << 4));
				return metadata;
			case 2:
				nbtTileEntity.setInteger("cbMetadata", state | (rotGate[rotGate[rotation]] << 4));
				return metadata;
			case 3:
				nbtTileEntity.setInteger("cbMetadata", state | (rotGate[rotGate[rotGate[rotation]]] << 4));
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersHatch.isInstance(block)) {
			final int metadataNBT = nbtTileEntity.getInteger("cbMetadata");
			final int rotation = (metadataNBT >> 3) & 15;
			final int state = metadataNBT & 0xFF87;
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("cbMetadata", state | (rotHatch[rotation] << 3));
				return metadata;
			case 2:
				nbtTileEntity.setInteger("cbMetadata", state | (rotHatch[rotHatch[rotation]] << 3));
				return metadata;
			case 3:
				nbtTileEntity.setInteger("cbMetadata", state | (rotHatch[rotHatch[rotHatch[rotation]]] << 3));
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersLadder.isInstance(block)) {
			final byte metadataNBT = nbtTileEntity.getByte("cbMetadata");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("cbMetadata", rotLadder[metadataNBT]);
				return metadata;
			case 2:
				nbtTileEntity.setByte("cbMetadata", rotLadder[rotLadder[metadataNBT]]);
				return metadata;
			case 3:
				nbtTileEntity.setByte("cbMetadata", rotLadder[rotLadder[rotLadder[metadataNBT]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersLever.isInstance(block)) {
			int metadataNBT = nbtTileEntity.getInteger("cbMetadata");
			metadataNBT = metadataNBT >= 64 ? metadataNBT - 48 : metadataNBT;
			switch (rotationSteps) {
			case 1:
				metadataNBT = rotLever[metadataNBT];
				metadataNBT = metadataNBT >= 16 ? metadataNBT + 48 : metadataNBT;
				nbtTileEntity.setInteger("cbMetadata", metadataNBT);
				return metadata;
			case 2:
				metadataNBT = rotLever[rotLever[metadataNBT]];
				metadataNBT = metadataNBT >= 16 ? metadataNBT + 48 : metadataNBT;
				nbtTileEntity.setInteger("cbMetadata", metadataNBT);
				return metadata;
			case 3:
				metadataNBT = rotLever[rotLever[rotLever[metadataNBT]]];
				metadataNBT = metadataNBT >= 16 ? metadataNBT + 48 : metadataNBT;
				nbtTileEntity.setInteger("cbMetadata", metadataNBT);
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersSafe.isInstance(block)) {
			final byte metadataNBT = nbtTileEntity.getByte("cbMetadata");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("cbMetadata", rotSafe[metadataNBT]);
				return metadata;
			case 2:
				nbtTileEntity.setByte("cbMetadata", rotSafe[rotSafe[metadataNBT]]);
				return metadata;
			case 3:
				nbtTileEntity.setByte("cbMetadata", rotSafe[rotSafe[rotSafe[metadataNBT]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersSlope.isInstance(block)) {
			final byte metadataNBT = nbtTileEntity.getByte("cbMetadata");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("cbMetadata", rotSlope[metadataNBT]);
				return metadata;
			case 2:
				nbtTileEntity.setByte("cbMetadata", rotSlope[rotSlope[metadataNBT]]);
				return metadata;
			case 3:
				nbtTileEntity.setByte("cbMetadata", rotSlope[rotSlope[rotSlope[metadataNBT]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (blockCarpentersStairs.isInstance(block)) {
			// metadata is original block placement, doesn't seem to influence rendering
			final byte metadataNBT = nbtTileEntity.getByte("cbMetadata");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("cbMetadata", rotStair[metadataNBT]);
				return metadata;
			case 2:
				nbtTileEntity.setByte("cbMetadata", rotStair[rotStair[metadataNBT]]);
				return metadata;
			case 3:
				nbtTileEntity.setByte("cbMetadata", rotStair[rotStair[rotStair[metadataNBT]]]);
				return metadata;
			default:
				return metadata;
			}
		}
			
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}

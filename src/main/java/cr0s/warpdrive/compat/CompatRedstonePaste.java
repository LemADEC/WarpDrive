package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatRedstonePaste implements IBlockTransformer {
	
	private static Class<?> classTileEntityRedstonePaste;
	
	public static void register() {
		try {
			classTileEntityRedstonePaste = Class.forName("fyber.redstonepastemod.TileEntityRedstonePaste");
			WarpDriveConfig.registerBlockTransformer("RedstonePasteMod", new CompatRedstonePaste());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classTileEntityRedstonePaste.isInstance(tileEntity);
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
	
	private static final int[] rotFaceIndex      = {  0,  1,  5,  4,  2,  3 };
	// Redstone wire rotations
	private static final int[] rotFaceHorizontal     = {  0,  8,  4, 12,  1,  9,  5, 13,  2, 10,  6, 14,  3, 11,  7, 15 };
	private static final int[] rotFaceVerticalCorner = {  0,  1,  2,  3,  8,  9, 10, 11,  4,  5,  6,  7, 12, 13, 14, 15 };
	//                                               = { ok, ok, ok, ok, yy, zz, xx, tt, yy, zz, xx, tt, ok, ok, ok, ok };
	
	// Repeater (normal/lit), Comparator rotations
	private static final int[] rotFaceDataHorizontal     = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 13, 14, 15, 12,
														     17, 18, 19, 16, 21, 22, 23, 20, 25, 26, 27, 24, 29, 30, 31, 28 };
	private static final int[] rotFaceDataVerticalCorner = {  0,  3,  2,  1,  4,  7,  6,  5,  8, 11, 10,  9, 12, 15, 14, 13,
															 16, 19, 18, 17, 20, 23, 22, 21, 24, 27, 26, 25, 28, 31, 30, 29 };
	//                                                       ok, aa, ok, aa, ok, ee, ok, ee, ok, bb, ok, bb, ok, ff, ok, ff,
	//                                                       ok, cc, ok, cc, ok, gg, ok, gg, ok, dd, ok, dd, ok, hh, ok, hh };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if (nbtTileEntity.hasKey("faces") && nbtTileEntity.hasKey("facedata") && nbtTileEntity.hasKey("facetype")) {
			int[] oldFaces = nbtTileEntity.getIntArray("faces");
			int[] oldFacedata = nbtTileEntity.getIntArray("facedata");
			int[] oldFacetype = nbtTileEntity.getIntArray("facetype");
			int[] newFaces = new int[6];
			int[] newFacedata = new int[6];
			int[] newFaceType = new int[6];
			int newIndex;
			for (int oldIndex = 0; oldIndex < 6; oldIndex++) {
				switch (rotationSteps) {
				case 1:
					newIndex = rotFaceIndex[oldIndex];
					break;
				case 2:
					newIndex = rotFaceIndex[rotFaceIndex[oldIndex]];
					break;
				case 3:
					newIndex = rotFaceIndex[rotFaceIndex[rotFaceIndex[oldIndex]]];
					break;
				default:
					newIndex = oldIndex;
					break;
				}
				
				// same type
				newFaceType[newIndex] = oldFacetype[oldIndex];
				
				switch (oldFacetype[oldIndex]) {
				case 0: // redstone wiring
					// same signal strength
					newFacedata[newIndex] = oldFacedata[oldIndex];
					
					// rotate top/bottom or when changing corner
					if (oldIndex == 0 || oldIndex == 1) {// top or bottom
						switch (rotationSteps) {
						case 1:
							newFaces[newIndex] = rotFaceHorizontal[oldFaces[oldIndex]];
							break;
						case 2:
							newFaces[newIndex] = rotFaceHorizontal[rotFaceHorizontal[oldFaces[oldIndex]]];
							break;
						case 3:
							newFaces[newIndex] = rotFaceHorizontal[rotFaceHorizontal[rotFaceHorizontal[oldFaces[oldIndex]]]];
							break;
						default:
							newFaces[newIndex] = oldFaces[oldIndex];
							break;
						}
					} else if ((oldIndex == 2 || oldIndex == 5) && (newIndex == 2 || newIndex == 5)) {// same corner = no change
						newFaces[newIndex] = oldFaces[oldIndex];
					} else if ((oldIndex == 3 || oldIndex == 4) && (newIndex == 3 || newIndex == 4)) {// same corner = no change
						newFaces[newIndex] = oldFaces[oldIndex];
					} else {
						newFaces[newIndex] = rotFaceVerticalCorner[oldFaces[oldIndex]];
					}
					break;
					
				case 2: // repeater
					// same wiring (none)
					newFaces[newIndex] = oldFaces[oldIndex];
					
					// rotate top/bottom or when changing corner
					if (oldIndex == 0 || oldIndex == 1) {// top or bottom
						switch (rotationSteps) {
						case 1:
							newFacedata[newIndex] = rotFaceDataHorizontal[oldFacedata[oldIndex]];
							break;
						case 2:
							newFacedata[newIndex] = rotFaceDataHorizontal[rotFaceDataHorizontal[oldFacedata[oldIndex]]];
							break;
						case 3:
							newFacedata[newIndex] = rotFaceDataHorizontal[rotFaceDataHorizontal[rotFaceDataHorizontal[oldFacedata[oldIndex]]]];
							break;
						default:
							newFacedata[newIndex] = oldFacedata[oldIndex];
							break;
						}
					} else if ((oldIndex == 2 || oldIndex == 5) && (newIndex == 2 || newIndex == 5)) {// same corner = no change
						newFacedata[newIndex] = oldFacedata[oldIndex];
					} else if ((oldIndex == 3 || oldIndex == 4) && (newIndex == 3 || newIndex == 4)) {// same corner = no change
						newFacedata[newIndex] = oldFacedata[oldIndex];
					} else {
						newFacedata[newIndex] = rotFaceDataVerticalCorner[oldFacedata[oldIndex]];
					}
					break;
					
				case 3: // comparator
					// same wiring (none)
					newFaces[newIndex] = oldFaces[oldIndex];
					
					int rawData        = oldFacedata[oldIndex] & 0x03;
					int signalStrength = oldFacedata[oldIndex] & 0xFC;
					
					// rotate top/bottom or when changing corner
					if (oldIndex == 0 || oldIndex == 1) {// top or bottom
						switch (rotationSteps) {
						case 1:
							newFacedata[newIndex] = rotFaceDataHorizontal[rawData] + signalStrength;
							break;
						case 2:
							newFacedata[newIndex] = rotFaceDataHorizontal[rotFaceDataHorizontal[rawData]] + signalStrength;
							break;
						case 3:
							newFacedata[newIndex] = rotFaceDataHorizontal[rotFaceDataHorizontal[rotFaceDataHorizontal[rawData]]] + signalStrength;
							break;
						default:
							newFacedata[newIndex] = rawData + signalStrength;
							break;
						}
					} else if ((oldIndex == 2 || oldIndex == 5) && (newIndex == 2 || newIndex == 5)) {// same corner = no change
						newFacedata[newIndex] = rawData + signalStrength;
					} else if ((oldIndex == 3 || oldIndex == 4) && (newIndex == 3 || newIndex == 4)) {// same corner = no change
						newFacedata[newIndex] = rawData + signalStrength;
					} else {
						newFacedata[newIndex] = rotFaceVerticalCorner[rawData] + signalStrength;
					}
					break;
					
				default: // unknown
					newFacedata[newIndex] = oldFacedata[oldIndex];
					newFaces[newIndex] = oldFaces[oldIndex];
					break;
				}
			}
			
			nbtTileEntity.setIntArray("faces", newFaces);
			nbtTileEntity.setIntArray("facedata", newFacedata);
			nbtTileEntity.setIntArray("facetype", newFaceType);
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

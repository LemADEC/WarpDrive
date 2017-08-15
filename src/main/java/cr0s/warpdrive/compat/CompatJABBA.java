package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatJABBA implements IBlockTransformer {
	
	private static Class<?> classBlockBarrel;
	
	public static void register() {
		try {
			classBlockBarrel = Class.forName("mcp.mobius.betterbarrels.common.blocks.BlockBarrel");
			WarpDriveConfig.registerBlockTransformer("JABBA", new CompatJABBA());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockBarrel.isInstance(block);
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
	
	private static final int[] mrot = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	private int[] rotate_integerArray(final byte rotationSteps, final int[] data) {
		int[] newData = data.clone();
		for (int index = 0; index < data.length; index++) {
			switch (rotationSteps) {
			case 1:
				newData[mrot[index]] = data[index];
				break;
			case 2:
				newData[mrot[mrot[index]]] = data[index];
				break;
			case 3:
				newData[mrot[mrot[mrot[index]]]] = data[index];
				break;
			default:
				break;
			}
		}
		return newData;
	}
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if (nbtTileEntity.hasKey("rotation")) {
			int rotation = nbtTileEntity.getInteger("rotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("rotation", mrot[rotation]);
				break;
			case 2:
				nbtTileEntity.setInteger("rotation", mrot[mrot[rotation]]);
				break;
			case 3:
				nbtTileEntity.setInteger("rotation", mrot[mrot[mrot[rotation]]]);
				break;
			default:
				break;
			}
		}
		
		if (nbtTileEntity.hasKey("orientation")) {
			int orientation = nbtTileEntity.getInteger("orientation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("orientation", mrot[orientation]);
				break;
			case 2:
				nbtTileEntity.setInteger("orientation", mrot[mrot[orientation]]);
				break;
			case 3:
				nbtTileEntity.setInteger("orientation", mrot[mrot[mrot[orientation]]]);
				break;
			default:
				break;
			}
		}
		
		if (nbtTileEntity.hasKey("sideUpgrades")) {
			nbtTileEntity.setIntArray("sideUpgrades", rotate_integerArray(rotationSteps, nbtTileEntity.getIntArray("sideUpgrades")));
		}
		
		if (nbtTileEntity.hasKey("sideMeta")) {
			nbtTileEntity.setIntArray("sideMeta", rotate_integerArray(rotationSteps, nbtTileEntity.getIntArray("sideMeta")));
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

package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatEvilCraft implements IBlockTransformer {
	
	private static Class<?> classBlockConfigurableBlockContainer;
	
	public static void register() {
		try {
			classBlockConfigurableBlockContainer = Class.forName("evilcraft.core.config.configurable.ConfigurableBlockContainer");
			WarpDriveConfig.registerBlockTransformer("evilcraft", new CompatEvilCraft());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockConfigurableBlockContainer.isInstance(block);
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
	
	private static final int[]  rotRotation       = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// tile entity rotations
		if (nbtTileEntity.hasKey("rotatable")) {
			if (!nbtTileEntity.getBoolean("rotatable")) {
				return metadata;
			}
			int rotation = nbtTileEntity.getInteger("rotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("rotation", rotRotation[rotation]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("rotation", rotRotation[rotRotation[rotation]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("rotation", rotRotation[rotRotation[rotRotation[rotation]]]);
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

package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatThermalExpansion implements IBlockTransformer {
	
	private static Class<?> tileEntityTEBase;
	
	public static void register() {
		try {
			tileEntityTEBase = Class.forName("cofh.thermalexpansion.block.TileTEBase");
			
			WarpDriveConfig.registerBlockTransformer("ThermalExpansion", new CompatThermalExpansion());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return tileEntityTEBase.isInstance(tileEntity);
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
	
	private static final short[] mrot            = {  0,  1,  5,  4,  2,  3 };
	
	private static final int[]   rotFacing       = {  0,  1,  5,  4,  2,  3 };
	private static final byte[]  rotLightDefault = {  0,  1,  5,  4,  2,  3 };
	private static final byte[]  rotLightStyle4  = {  8,  9, 13, 12,  4,  5,  6,  7,  0,  1, 10, 11,  2,  3, 14, 15 };
	
	private byte[] rotate_byteArray(final byte rotationSteps, final byte[] data) {
		byte[] newData = data.clone();
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
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// lights
		if (nbtTileEntity.hasKey("Align")) {
			byte style = nbtTileEntity.getByte("Style");
			byte align = nbtTileEntity.getByte("Align");
			if (style == 4) {
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("Align", rotLightStyle4[align]);
					break;
				case 2:
					nbtTileEntity.setByte("Align", rotLightStyle4[rotLightStyle4[align]]);
					break;
				case 3:
					nbtTileEntity.setByte("Align", rotLightStyle4[rotLightStyle4[rotLightStyle4[align]]]);
					break;
				default:
					break;
				}
				
			} else {
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("Align", rotLightDefault[align]);
					break;
				case 2:
					nbtTileEntity.setByte("Align", rotLightDefault[rotLightDefault[align]]);
					break;
				case 3:
					nbtTileEntity.setByte("Align", rotLightDefault[rotLightDefault[rotLightDefault[align]]]);
					break;
				default:
					break;
				}
			}
			return metadata;
		}
		
		// machines
		if (nbtTileEntity.hasKey("Facing")) {
			int facing = nbtTileEntity.getInteger("Facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("Facing", rotFacing[facing]);
				break;
			case 2:
				nbtTileEntity.setInteger("Facing", rotFacing[rotFacing[facing]]);
				break;
			case 3:
				nbtTileEntity.setInteger("Facing", rotFacing[rotFacing[rotFacing[facing]]]);
				break;
			default:
				break;
			}
		}
		
		if (nbtTileEntity.hasKey("SideCache")) {
			nbtTileEntity.setByteArray("SideCache", rotate_byteArray(rotationSteps, nbtTileEntity.getByteArray("SideCache")));
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

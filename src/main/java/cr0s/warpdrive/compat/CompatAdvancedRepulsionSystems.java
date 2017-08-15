package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatAdvancedRepulsionSystems implements IBlockTransformer {
	
	private static Class<?> classBlockDFDoor;
	private static Class<?> classBlockMachine;
	private static Class<?> classBlockUpgrades;
	private static Class<?> classBlockBeam;
	
	public static void register() {
		try {
			classBlockDFDoor = Class.forName("mods.immibis.ars.DeFence.BlockDFDoor");
			classBlockBeam = Class.forName("mods.immibis.ars.beams.BlockBeam");
			classBlockMachine = Class.forName("mods.immibis.ars.BlockMachine");
			classBlockUpgrades = Class.forName("mods.immibis.ars.BlockUpgrades");
			WarpDriveConfig.registerBlockTransformer("AdvancedRepulsionSystems", new CompatAdvancedRepulsionSystems());
		} catch(ClassNotFoundException | SecurityException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockDFDoor.isInstance(block)
			|| classBlockBeam.isInstance(block)
			|| classBlockMachine.isInstance(block)
			|| classBlockUpgrades.isInstance(block);
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
	
	//                                           {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 }
	private static final byte[]  mrotBeam      = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 14, 15 };
	private static final short[] rotFacing     = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[]  rotOutputFace = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if (classBlockBeam.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotBeam[metadata];
			case 2:
				return mrotBeam[mrotBeam[metadata]];
			case 3:
				return mrotBeam[mrotBeam[mrotBeam[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (nbtTileEntity.hasKey("facing")) {
			short facing = nbtTileEntity.getShort("facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setShort("facing", rotFacing[facing]);
				return metadata;
			case 2:
				nbtTileEntity.setShort("facing", rotFacing[rotFacing[facing]]);
				return metadata;
			case 3:
				nbtTileEntity.setShort("facing", rotFacing[rotFacing[rotFacing[facing]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (nbtTileEntity.hasKey("outputFace")) {
			byte outputFace = nbtTileEntity.getByte("outputFace");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("outputFace", rotOutputFace[outputFace]);
				return metadata;
			case 2:
				nbtTileEntity.setByte("outputFace", rotOutputFace[rotOutputFace[outputFace]]);
				return metadata;
			case 3:
				nbtTileEntity.setByte("outputFace", rotOutputFace[rotOutputFace[rotOutputFace[outputFace]]]);
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

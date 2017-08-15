package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatBiblioCraft implements IBlockTransformer {
	
	private static Class<?> classBlockArmorStand;
	private static Class<?> classBlockPrintingPress;
	
	public static void register() {
		try {
			classBlockArmorStand = Class.forName("jds.bibliocraft.blocks.BlockArmorStand");
			classBlockPrintingPress = Class.forName("jds.bibliocraft.blocks.BlockPrintPress");
			WarpDriveConfig.registerBlockTransformer("BiblioCraft", new CompatBiblioCraft());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		if (block == null) {
			return false;
		}
		String canonicalName = block.getClass().getCanonicalName();
		return canonicalName != null && canonicalName.startsWith("jds.bibliocraft.");
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
	
	private static final byte[] mrotArmorStand = {  1,  2,  3,  0,  5,  6,  7,  4,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  rotAngle       = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  rotCaseAngle   = {  0,  1,  2,  3,  5,  6,  7,  4,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// metadata rotations
		if (classBlockArmorStand.isInstance(block) || classBlockPrintingPress.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotArmorStand[metadata];
			case 2:
				return mrotArmorStand[mrotArmorStand[metadata]];
			case 3:
				return mrotArmorStand[mrotArmorStand[mrotArmorStand[metadata]]];
			default:
				return metadata;
			}
		}
		
		// tile entity rotations
		String key = null;
		if (nbtTileEntity.hasKey("angle")) {
			key = "angle";
		} else if (nbtTileEntity.hasKey("Angle")) {
			key = "Angle";
		} else if (nbtTileEntity.hasKey("deskAngle")) {
			key = "deskAngle";
		} else if (nbtTileEntity.hasKey("labelAngle")) {
			key = "labelAngle";
		} else if (nbtTileEntity.hasKey("bookcaseAngle")) {
			key = "bookcaseAngle";
		} else if (nbtTileEntity.hasKey("rackAngle")) {
			key = "rackAngle";
		} else if (nbtTileEntity.hasKey("genericShelfAngle")) {
			key = "genericShelfAngle";
		} else if (nbtTileEntity.hasKey("potionshelfAngle")) {
			key = "potionshelfAngle";
		}
		if (key != null) {
			int angle = nbtTileEntity.getInteger(key);
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger(key, rotAngle[angle]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger(key, rotAngle[rotAngle[angle]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger(key, rotAngle[rotAngle[rotAngle[angle]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		
		if (nbtTileEntity.hasKey("caseAngle")) {
			int angle = nbtTileEntity.getInteger("caseAngle");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("caseAngle", rotCaseAngle[angle]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("caseAngle", rotCaseAngle[rotCaseAngle[angle]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("caseAngle", rotCaseAngle[rotCaseAngle[rotCaseAngle[angle]]]);
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

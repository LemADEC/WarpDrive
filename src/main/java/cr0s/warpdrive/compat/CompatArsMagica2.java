package cr0s.warpdrive.compat;

import am2.api.power.IPowerNode;
import am2.power.PowerNodeRegistry;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.Constants;

public class CompatArsMagica2 implements IBlockTransformer {
	
	private static Class<?> classBlockInscriptionTable;
	private static Class<?> classBlockKeystoneReceptacle;
	private static Class<?> classBlockLectern;
	private static Class<?> classBlockMagiciansWorkbench;
	private static Class<?> classBlockOcculus;
	
	public static void register() {
		try {
			classBlockInscriptionTable = Class.forName("am2.blocks.BlockInscriptionTable");
			classBlockKeystoneReceptacle = Class.forName("am2.blocks.BlockKeystoneReceptacle");
			classBlockLectern = Class.forName("am2.blocks.BlockLectern");
			classBlockMagiciansWorkbench = Class.forName("am2.blocks.BlockMagiciansWorkbench");
			classBlockOcculus = Class.forName("am2.blocks.BlockOcculus");
			WarpDriveConfig.registerBlockTransformer("arsmagica2", new CompatArsMagica2());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockInscriptionTable.isInstance(block)
			|| classBlockKeystoneReceptacle.isInstance(block)
			|| classBlockLectern.isInstance(block)
			|| classBlockMagiciansWorkbench.isInstance(block)
			|| classBlockOcculus.isInstance(block)
			|| tileEntity instanceof IPowerNode;
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, StringBuilder reason) {
		return true;
	}
	
	@Override
	@Optional.Method(modid = "arsmagica2")
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		if (tileEntity instanceof IPowerNode) {
			return PowerNodeRegistry.For(tileEntity.getWorldObj()).getDataCompoundForNode((IPowerNode) tileEntity);
		}
		return null;
	}
	
	@Override
	@Optional.Method(modid = "arsmagica2")
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		if (tileEntity instanceof IPowerNode) {
			PowerNodeRegistry.For(tileEntity.getWorldObj()).removePowerNode((IPowerNode) tileEntity);
		}
	}
	
	private static final int[] mrotInscriptionTable   = {  0,  4,  1,  2,  3,  5,  6,  7,  8, 12,  9, 10, 11, 13, 14, 15 };
	private static final int[] mrotKeystoneReceptacle = {  3,  0,  1,  2,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[] mrotLectern            = {  0,  4,  1,  2,  3,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// same as Magicians workbench & Occulus
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if (classBlockInscriptionTable.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotInscriptionTable[metadata];
			case 2:
				return mrotInscriptionTable[mrotInscriptionTable[metadata]];
			case 3:
				return mrotInscriptionTable[mrotInscriptionTable[mrotInscriptionTable[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (classBlockKeystoneReceptacle.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotKeystoneReceptacle[metadata];
			case 2:
				return mrotKeystoneReceptacle[mrotKeystoneReceptacle[metadata]];
			case 3:
				return mrotKeystoneReceptacle[mrotKeystoneReceptacle[mrotKeystoneReceptacle[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (classBlockLectern.isInstance(block) || classBlockMagiciansWorkbench.isInstance(block) || classBlockOcculus.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotLectern[metadata];
			case 2:
				return mrotLectern[mrotLectern[metadata]];
			case 3:
				return mrotLectern[mrotLectern[mrotLectern[metadata]]];
			default:
				return metadata;
			}
		}
		
		return metadata;
	}
	
	@Override
	@Optional.Method(modid = "arsmagica2")
	public void restoreExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		if (!(tileEntity instanceof IPowerNode) || nbtBase == null) {
			return;
		}
		NBTTagCompound nbtTagCompound = (NBTTagCompound) nbtBase;
		
		// powerAmounts
		// (no changes)
		
		// powerPathList
		NBTTagList powerPathList = nbtTagCompound.getTagList("powerPathList", Constants.NBT.TAG_COMPOUND);
		if (powerPathList != null) {
			for (int powerPathIndex = 0; powerPathIndex < powerPathList.tagCount(); powerPathIndex++) {
				NBTTagCompound powerPathEntry = (NBTTagCompound) powerPathList.removeTag(0);
				
				// powerPathList[powerPathIndex].powerType
				// (no change)
				
				// powerPathList[powerPathIndex].nodePaths
				NBTTagList nodePaths = powerPathEntry.getTagList("nodePaths", Constants.NBT.TAG_LIST);
				if (nodePaths != null) {
					for (int nodePathIndex = 0; nodePathIndex < nodePaths.tagCount(); nodePathIndex++) {
						// we can't directly access it, hence removing then adding back later on
						NBTTagList nodeList = (NBTTagList) nodePaths.removeTag(0);
						if (nodeList != null) {
							for (int nodeIndex = 0; nodeIndex < nodeList.tagCount(); nodeIndex++) {
								NBTTagCompound node = (NBTTagCompound) nodeList.removeTag(0);
								// read coordinates
								Vec3 target = transformation.apply(node.getFloat("Vec3_x"), node.getFloat("Vec3_y"), node.getFloat("Vec3_z"));
								node.setFloat("Vec3_x", (float)target.xCoord);
								node.setFloat("Vec3_y", (float)target.yCoord);
								node.setFloat("Vec3_z", (float)target.zCoord);
								//tack the node on to the power path
								nodeList.appendTag(node);
							}
							nodePaths.appendTag(nodeList);
						}
					}
					powerPathEntry.setTag("nodePaths", nodePaths);
				}
				powerPathList.appendTag(powerPathEntry);
			}
			nbtTagCompound.setTag("powerPathList", powerPathList);
		}
		
		World targetWorld = transformation.getTargetWorld();
		ChunkCoordinates target = transformation.apply(tileEntity);
		TileEntity tileEntityTarget = targetWorld.getTileEntity(target.posX, target.posY, target.posZ);
		if (tileEntityTarget == null) {
			WarpDrive.logger.error("ArsMagica2 compat: No tile entity found at target location " + target + ". We might loose mana network " + nbtBase + ".");
		} else if (!(tileEntityTarget instanceof IPowerNode)) {
			WarpDrive.logger.error("ArsMagica2 compat: invalid tile entity " + tileEntityTarget + " found at target location " + target + ".");
		} else {
			PowerNodeRegistry.For(targetWorld).setDataCompoundForNode((IPowerNode) tileEntityTarget, nbtTagCompound);
		}
	}
}

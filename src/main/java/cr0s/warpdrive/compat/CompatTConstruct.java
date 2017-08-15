package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatTConstruct implements IBlockTransformer {
	
	private static Class<?> classBlockDryingRack;
	private static Class<?> classTileFurnaceLogic;
	private static Class<?> classTileFaucetLogic;
	private static Class<?> classTileSmelteryDrainLogic;
	private static Class<?> classTileSmelteryLogic;
	
	public static void register() {
		try {
			classBlockDryingRack = Class.forName("tconstruct.armor.blocks.DryingRack");
			classTileFurnaceLogic = Class.forName("tconstruct.tools.logic.FurnaceLogic");
			classTileFaucetLogic = Class.forName("tconstruct.smeltery.logic.FaucetLogic");
			classTileSmelteryDrainLogic = Class.forName("tconstruct.smeltery.logic.SmelteryDrainLogic");
			classTileSmelteryLogic = Class.forName("tconstruct.smeltery.logic.SmelteryLogic");
			WarpDriveConfig.registerBlockTransformer("TConstruct", new CompatTConstruct());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockDryingRack.isInstance(block)
			|| classTileFurnaceLogic.isInstance(tileEntity)
			|| classTileFaucetLogic.isInstance(tileEntity)
			|| classTileSmelteryDrainLogic.isInstance(tileEntity)
			|| classTileSmelteryLogic.isInstance(tileEntity);
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
	
	private static final int[]  mrotDryingRack = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] rotDirection   = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// metadata = 2 5 3 4 Drying rack
		if (classBlockDryingRack.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotDryingRack[metadata];
			case 2:
				return mrotDryingRack[mrotDryingRack[metadata]];
			case 3:
				return mrotDryingRack[mrotDryingRack[mrotDryingRack[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (nbtTileEntity.hasKey("Direction")) {
			short direction = nbtTileEntity.getByte("Direction");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("Direction", rotDirection[direction]);
				return metadata;
			case 2:
				nbtTileEntity.setByte("Direction", rotDirection[rotDirection[direction]]);
				return metadata;
			case 3:
				nbtTileEntity.setByte("Direction", rotDirection[rotDirection[rotDirection[direction]]]);
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

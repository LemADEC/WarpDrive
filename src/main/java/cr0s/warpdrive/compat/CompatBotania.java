package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public class CompatBotania implements IBlockTransformer {
	
	private static Class<?> classBlockModContainer;
	private static Class<?> classBlockAvatar;
	private static Class<?> classBlockFelPumpkin;
	private static Class<?> classBlockSpecialFlower;
	
	public static void register() {
		try {
			classBlockModContainer  = Class.forName("vazkii.botania.common.block.BlockModContainer");
			classBlockAvatar        = Class.forName("vazkii.botania.common.block.BlockAvatar");
			classBlockFelPumpkin    = Class.forName("vazkii.botania.common.block.BlockFelPumpkin");
			classBlockSpecialFlower = Class.forName("vazkii.botania.common.block.BlockSpecialFlower");
			WarpDriveConfig.registerBlockTransformer("Botania", new CompatBotania());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockModContainer.isInstance(block)
			|| classBlockFelPumpkin.isInstance(block)
			|| classBlockSpecialFlower.isInstance(block);
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
	
	// -----------------------------------------    {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotAvatar       = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotFelPumpkin   = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		
		if (classBlockAvatar.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotAvatar[metadata];
			case 2:
				return mrotAvatar[mrotAvatar[metadata]];
			case 3:
				return mrotAvatar[mrotAvatar[mrotAvatar[metadata]]];
			default:
				return metadata;
			}
		}
		if (classBlockFelPumpkin.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotFelPumpkin[metadata];
			case 2:
				return mrotFelPumpkin[mrotFelPumpkin[metadata]];
			case 3:
				return mrotFelPumpkin[mrotFelPumpkin[mrotFelPumpkin[metadata]]];
			default:
				return metadata;
			}
		}
		
		if ( nbtTileEntity.hasKey("bindX")
		  && nbtTileEntity.hasKey("bindY") 
		  && nbtTileEntity.hasKey("bindZ") ) {
			final ChunkCoordinates targetBind = transformation.apply(nbtTileEntity.getInteger("bindX"), nbtTileEntity.getInteger("bindY"), nbtTileEntity.getInteger("bindZ"));
			nbtTileEntity.setInteger("bindX", targetBind.posX);
			nbtTileEntity.setInteger("bindY", targetBind.posY);
			nbtTileEntity.setInteger("bindZ", targetBind.posZ);
		}
		
		if (nbtTileEntity.hasKey("subTileCmp")) {
			final NBTTagCompound nbtSubTileCmp = nbtTileEntity.getCompoundTag("subTileCmp");
			if ( nbtSubTileCmp.hasKey("collectorX")
			  && nbtSubTileCmp.hasKey("collectorY")
			  && nbtSubTileCmp.hasKey("collectorZ") ) {
				final ChunkCoordinates targetCollector = transformation.apply(nbtSubTileCmp.getInteger("collectorX"), nbtSubTileCmp.getInteger("collectorY"), nbtSubTileCmp.getInteger("collectorZ"));
				nbtSubTileCmp.setInteger("collectorX", targetCollector.posX);
				nbtSubTileCmp.setInteger("collectorY", targetCollector.posY);
				nbtSubTileCmp.setInteger("collectorZ", targetCollector.posZ);
			}
		}
		
		if (nbtTileEntity.hasKey("rotationX")) {
			final float rotationX = nbtTileEntity.getInteger("rotationX");
			nbtTileEntity.setFloat("rotationX", (rotationX + 270.0F * rotationSteps) % 360.0F);
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

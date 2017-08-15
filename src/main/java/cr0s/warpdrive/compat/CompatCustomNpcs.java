package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatCustomNpcs implements IBlockTransformer {
	
	private static Class<?> classBlockBlood;
	private static Class<?> classBlockBorder;
	private static Class<?> classBlockCarpentryBench;
	private static Class<?> classBlockMailbox;
	private static Class<?> classTileColorable;
	private static Class<?> classTileBigSign;
	
	public static void register() {
		try {
			classBlockBlood          = Class.forName("noppes.npcs.blocks.BlockBlood");
			classBlockBorder          = Class.forName("noppes.npcs.blocks.BlockBorder");
			classBlockCarpentryBench = Class.forName("noppes.npcs.blocks.BlockCarpentryBench");
			classBlockMailbox        = Class.forName("noppes.npcs.blocks.BlockMailbox");
			classTileColorable       = Class.forName("noppes.npcs.blocks.tiles.TileColorable");
			classTileBigSign         = Class.forName("noppes.npcs.blocks.tiles.TileBigSign");
			WarpDriveConfig.registerBlockTransformer("CustomNpcs", new CompatCustomNpcs());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockBlood.isInstance(block)
		    || classBlockBorder.isInstance(block)
		    || classBlockCarpentryBench.isInstance(block)
		    || classBlockMailbox.isInstance(block)
		    || classTileColorable.isInstance(tileEntity)
			|| classTileBigSign.isInstance(tileEntity);
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
	
	// Transformation handling required:
	// metadata
	// noppes.npcs.blocks.BlockBlood             Blood    0 1 2 3
	// noppes.npcs.blocks.BlockCarpentryBench    Anvil    0 1 2 3 4 5 6 7
	// noppes.npcs.blocks.BlockMailbox           Mailbox  0 1 2 3 4 5 6 7
	//
	// NBT tags
	// noppes.npcs.blocks.BlockBorder            BorderRotation (integer)  0 1 2 3
	// noppes.npcs.blocks.tiles.TileColorable    BannerRotation (integer)  0 1 2 3           mostly, but not limited to noppes.npcs.blocks.BlockRotated
	// noppes.npcs.blocks.tiles.TileBigSign      SignRotation   (integer)  0 1 2 3				
	
	
	// -----------------------------------------          {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrot4                  = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrot8                  = {  1,  2,  3,  0,  5,  6,  7,  4,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if (classBlockBlood.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrot4[metadata];
			case 2:
				return mrot4[mrot4[metadata]];
			case 3:
				return mrot4[mrot4[mrot4[metadata]]];
			default:
				return metadata;
			}
		}
		if ( classBlockCarpentryBench.isInstance(block)
		  || classBlockMailbox.isInstance(block) ) {
			switch (rotationSteps) {
			case 1:
				return mrot8[metadata];
			case 2:
				return mrot8[mrot8[metadata]];
			case 3:
				return mrot8[mrot8[mrot8[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (nbtTileEntity.hasKey("BannerRotation")) {
			final int BannerRotation = nbtTileEntity.getInteger("BannerRotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("BannerRotation", mrot4[BannerRotation]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("BannerRotation", mrot4[mrot4[BannerRotation]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("BannerRotation", mrot4[mrot4[mrot4[BannerRotation]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (nbtTileEntity.hasKey("BorderRotation")) {
			final int BorderRotation = nbtTileEntity.getInteger("BorderRotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("BorderRotation", mrot4[BorderRotation]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("BorderRotation", mrot4[mrot4[BorderRotation]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("BorderRotation", mrot4[mrot4[mrot4[BorderRotation]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		if (nbtTileEntity.hasKey("SignRotation")) {
			final int SignRotation = nbtTileEntity.getInteger("SignRotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("SignRotation", mrot4[SignRotation]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("SignRotation", mrot4[mrot4[SignRotation]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("SignRotation", mrot4[mrot4[mrot4[SignRotation]]]);
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

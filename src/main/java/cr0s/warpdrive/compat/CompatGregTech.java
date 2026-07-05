package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants.NBT;

public class CompatGregTech implements IBlockTransformer {
	
	private static Class<?> classMetaTileEntityHolder;
	private static Class<?> classTileEntityPipeBase;
	
	public static void register() {
		try {
			classMetaTileEntityHolder = Class.forName("gregtech.api.metatileentity.MetaTileEntityHolder");
			classTileEntityPipeBase   = Class.forName("gregtech.api.pipenet.tile.TileEntityPipeBase");
			
			WarpDriveConfig.registerBlockTransformer("gregtech", new CompatGregTech());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classMetaTileEntityHolder.isInstance(tileEntity)
			|| classTileEntityPipeBase.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	/*
	As of gregtech-1.12.2-2.1.4
	gregtech.api.pipenet.tile.TileEntityPipeBase (cables and fluid pipes)
	    BlockedConnections int (1 << side.func_176745_a())
	    Connections int (1 << side.func_176745_a())
		Covers[].Side byte 2 5 3 4
	gregtech.api.metatileentity.MetaTileEntityHolder (machine)
		MetaTileEntity.FrontFacing int 2 5 3 4
		MetaTileEntity.OutputFacing int 2 5 3 4
		MetaTileEntity.OutputFacingF int 2 5 3 4
		MetaTileEntity.Covers[].Side byte 2 5 3 4
	*/
	
	// -----------------------------------------    {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   rotFacing        = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	private void rotateConnection(@Nonnull final NBTTagCompound tagCompound, @Nonnull final String key, final byte rotationSteps) {
		if (!tagCompound.hasKey(key)) {
			return;
		}
		
		final int connectionsOld = tagCompound.getInteger(key);
		int connectionsNew = connectionsOld & 0x3; // keep bottom and top
		for (final EnumFacing enumFacing : EnumFacing.HORIZONTALS) {
			// get state for current face
			final int indexFacingOld = enumFacing.getIndex();
			final boolean isBlockedOrConnected = (connectionsOld & (1 << indexFacingOld)) != 0;
			if (!isBlockedOrConnected) {
				continue;
			}
			// get new face
			final int indexFacingNew;
			switch (rotationSteps) {
			case 1:
				indexFacingNew = rotFacing[indexFacingOld];
				break;
			case 2:
				indexFacingNew = rotFacing[rotFacing[indexFacingOld]];
				break;
			case 3:
				indexFacingNew = rotFacing[rotFacing[rotFacing[indexFacingOld]]];
				break;
			default:
				indexFacingNew = indexFacingOld;
				break;
			}
			// apply state on new face
			connectionsNew |= (1 << indexFacingNew);
		}
		tagCompound.setInteger(key, connectionsNew);
	}
	
	private void rotateFaceInteger(@Nonnull final NBTTagCompound tagCompound, @Nonnull final String key, final byte rotationSteps) {
		if (!tagCompound.hasKey(key)) {
			return;
		}
		
		final int frontFacing = tagCompound.getInteger(key);
		switch (rotationSteps) {
		case 1:
			tagCompound.setInteger(key, rotFacing[frontFacing]);
			break;
		case 2:
			tagCompound.setInteger(key, rotFacing[rotFacing[frontFacing]]);
			break;
		case 3:
			tagCompound.setInteger(key, rotFacing[rotFacing[rotFacing[frontFacing]]]);
			break;
		default:
			break;
		}
	}
	
	private void rotateCovers(@Nonnull final NBTTagCompound tagCompound, final byte rotationSteps) {
		final String key = "Covers";
		if (!tagCompound.hasKey(key)) {
			return;
		}
		
		final NBTTagList tagListCovers = tagCompound.getTagList("Covers", NBT.TAG_COMPOUND);
		final int count = tagListCovers.tagCount();
		for (int index = 0; index < count; index++) {
			final NBTTagCompound tagCover = (NBTTagCompound) tagListCovers.get(index);
			final int side = tagCover.getByte("Side");
			switch (rotationSteps) {
			case 1:
				tagCover.setByte("Side", (byte) rotFacing[side]);
				break;
			case 2:
				tagCover.setByte("Side", (byte) rotFacing[rotFacing[side]]);
				break;
			case 3:
				tagCover.setByte("Side", (byte) rotFacing[rotFacing[rotFacing[side]]]);
				break;
			}
		}
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if ( rotationSteps == 0
		  || nbtTileEntity == null ) {
			return metadata;
		}
		
		// BlockedConnections int (1 << side.func_176745_a())
		rotateConnection(nbtTileEntity, "BlockedConnections", rotationSteps);
		rotateConnection(nbtTileEntity, "Connections", rotationSteps);
		rotateCovers(nbtTileEntity, rotationSteps);
		
		// MetaTileEntity.FrontFacing int 2 5 3 4
		if (nbtTileEntity.hasKey("MetaTileEntity")) {
			final NBTTagCompound tagCompoundMetaTileEntity = nbtTileEntity.getCompoundTag("MetaTileEntity");
			rotateFaceInteger(tagCompoundMetaTileEntity, "FrontFacing", rotationSteps);
			rotateFaceInteger(tagCompoundMetaTileEntity, "OutputFacing", rotationSteps);
			rotateFaceInteger(tagCompoundMetaTileEntity, "OutputFacingF", rotationSteps);
			rotateCovers(tagCompoundMetaTileEntity, rotationSteps);
		}
		
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// refresh client @TODO
		// (not working in SSP) world.notifyBlockUpdate(blockPos, blockState, blockState, 2);
	}
}

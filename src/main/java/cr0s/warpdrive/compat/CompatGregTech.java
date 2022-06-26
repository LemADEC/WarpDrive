package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
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
	public boolean isApplicable(final BlockState blockState, final TileEntity tileEntity) {
		return classMetaTileEntityHolder.isInstance(tileEntity)
			|| classTileEntityPipeBase.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final BlockState blockState, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public INBT saveExternals(final World world, final int x, final int y, final int z,
	                             final BlockState blockState, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final BlockState blockState, final TileEntity tileEntity) {
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
	
	private void rotateConnection(@Nonnull final CompoundNBT tagCompound, @Nonnull final String key, final byte rotationSteps) {
		if (!tagCompound.contains(key)) {
			return;
		}
		
		final int connectionsOld = tagCompound.getInt(key);
		int connectionsNew = connectionsOld & 0x3; // keep bottom and top
		for (final Direction enumFacing : Direction.Plane.HORIZONTAL) {
			// get state for current face
			final int indexFacingOld = enumFacing.getIndex();
			final boolean isBlocked = (connectionsOld & (1 << indexFacingOld)) != 0;
			if (!isBlocked) {
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
		tagCompound.putInt(key, connectionsNew);
	}
	
	private void rotateFaceInteger(@Nonnull final CompoundNBT tagCompound, @Nonnull final String key, final byte rotationSteps) {
		if (!tagCompound.contains(key)) {
			return;
		}
		
		final int frontFacing = tagCompound.getInt(key);
		switch (rotationSteps) {
		case 1:
			tagCompound.putInt(key, rotFacing[frontFacing]);
			break;
		case 2:
			tagCompound.putInt(key, rotFacing[rotFacing[frontFacing]]);
			break;
		case 3:
			tagCompound.putInt(key, rotFacing[rotFacing[rotFacing[frontFacing]]]);
			break;
		default:
			break;
		}
	}
	
	private void rotateCovers(@Nonnull final CompoundNBT tagCompound, final byte rotationSteps) {
		final String key = "Covers";
		if (!tagCompound.contains(key)) {
			return;
		}
		
		final ListNBT tagListCovers = tagCompound.getList("Covers", NBT.TAG_COMPOUND);
		for (INBT tagListCover : tagListCovers) {
			final CompoundNBT tagCover = (CompoundNBT) tagListCover;
			final int side = tagCover.getByte("Side");
			switch (rotationSteps) {
			case 1:
				tagCover.putByte("Side", (byte) rotFacing[side]);
				break;
			case 2:
				tagCover.putByte("Side", (byte) rotFacing[rotFacing[side]]);
				break;
			case 3:
				tagCover.putByte("Side", (byte) rotFacing[rotFacing[rotFacing[side]]]);
				break;
			}
		}
	}
	
	@Override
	public BlockState rotate(final BlockState blockState, final CompoundNBT nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if ( rotationSteps == 0
		  || nbtTileEntity == null ) {
			return blockState;
		}
		
		// BlockedConnections int (1 << side.func_176745_a())
		rotateConnection(nbtTileEntity, "BlockedConnections", rotationSteps);
		rotateConnection(nbtTileEntity, "Connections", rotationSteps);
		rotateCovers(nbtTileEntity, rotationSteps);
		
		// MetaTileEntity.FrontFacing int 2 5 3 4
		if (nbtTileEntity.contains("MetaTileEntity")) {
			final CompoundNBT tagCompoundMetaTileEntity = nbtTileEntity.getCompound("MetaTileEntity");
			rotateFaceInteger(tagCompoundMetaTileEntity, "FrontFacing", rotationSteps);
			rotateFaceInteger(tagCompoundMetaTileEntity, "OutputFacing", rotationSteps);
			rotateFaceInteger(tagCompoundMetaTileEntity, "OutputFacingF", rotationSteps);
			rotateCovers(tagCompoundMetaTileEntity, rotationSteps);
		}
		
		return blockState;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final BlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final INBT nbtBase) {
		// refresh client @TODO
		// (not working in SSP) world.notifyBlockUpdate(blockPos, blockState, blockState, 2);
	}
}

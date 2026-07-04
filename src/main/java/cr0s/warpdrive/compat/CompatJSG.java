package cr0s.warpdrive.compat;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatJSG implements IBlockTransformer {
	
	private static Class<?> classDHDAbstractTile;
	private static Class<?> classDHDAbstractBlock;
	private static Class<?> classStargateClassicBaseTile;
	private static Class<?> classStargateAbstractBaseTile;
	private static Class<?> classStargateAbstractMemberTile;
	private static Method methodStargateClassicBaseTile_sgStateDescription;
	
	public static void register() {
		try {
			classDHDAbstractTile = Class.forName("tauri.dev.jsg.tileentity.dialhomedevice.DHDAbstractTile");
			classDHDAbstractBlock = Class.forName("tauri.dev.jsg.block.dialhomedevice.DHDAbstractBlock");
			classStargateClassicBaseTile = Class.forName("tauri.dev.jsg.tileentity.stargate.StargateClassicBaseTile");
			classStargateAbstractBaseTile   = Class.forName("tauri.dev.jsg.tileentity.stargate.StargateAbstractBaseTile");
			classStargateAbstractMemberTile = Class.forName("tauri.dev.jsg.tileentity.stargate.StargateAbstractMemberTile");
			methodStargateClassicBaseTile_sgStateDescription = classStargateAbstractBaseTile.getMethod("getStargateState");
			WarpDriveConfig.registerBlockTransformer("jsg", new CompatJSG());
		} catch(final ClassNotFoundException | NoSuchMethodException | SecurityException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		if (classStargateAbstractMemberTile.isInstance(tileEntity)
	     || classDHDAbstractTile.isInstance(tileEntity)
		 || classStargateAbstractBaseTile.isInstance(tileEntity)
		 || classDHDAbstractBlock.isInstance(block)) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		if (classStargateClassicBaseTile.isInstance(tileEntity)) {
			try {
				final Object object = methodStargateClassicBaseTile_sgStateDescription.invoke(tileEntity);
				final String state = object.toString();
				if (!state.equalsIgnoreCase("Idle")) {
					reason.append(Commons.getStyleWarning(), "warpdrive.compat.guide.stargate_is_active", state);
					return false;
				}
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
				exception.printStackTrace();
			}
		}
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

	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();

		// Translate position of the linked base for member blocks
		if (nbtTileEntity.hasKey("basePos")) {
			//Convert the long to a BlockPos then apply the transformation
			final BlockPos basePos = transformation.apply(BlockPos.fromLong(nbtTileEntity.getLong("basePos")));
			nbtTileEntity.setLong("basePos", basePos.toLong());
		}
		
		//get the blockstate
		final IBlockState blockState = block.getStateFromMeta(metadata);

		// Rotation for stargate blocks, I don't know why the default rotation system doesn't work for them
		//get the facing property
		final PropertyEnum<EnumFacing> propertyFacing = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
		//check if the block has the facing property
		if (blockState.getProperties().containsKey(propertyFacing)) {
			//get the facing value
			final EnumFacing facing = blockState.getValue(propertyFacing);
			//rotate the facing value
			final EnumFacing facingRotated;
			switch (rotationSteps) {
			case 1:
				facingRotated = facing.rotateY();
				break;
			case 2:
				facingRotated = facing.rotateY().rotateY();
				break;
			case 3:
				facingRotated = facing.rotateY().rotateY().rotateY();
				break;
			default:
				facingRotated = facing;
				break;
			}
			//set the facing value
			return block.getMetaFromState(blockState.withProperty(propertyFacing, facingRotated));
		}

		// Rotation for DHD block. The DHD uses a non-standard 16 step rotation system with a custom block property which is why this code is so messy

		//get the rotation property
		final PropertyInteger propertyRotation = PropertyInteger.create("rotation", 0, 15);
		//check if the block has the rotation property
		if (blockState.getProperties().containsKey(propertyRotation)) {
		    //get the rotation value
		    final int DHDRotation = blockState.getValue(propertyRotation);
		
		    int increment;
		
		    switch (rotationSteps) {
		    case 1: 
		        increment = 4;
		        break;
		    case 2:
		        increment = 8;
		        break;
		    case 3:
		        increment = 12;
		        break;
		    default:
		        increment = 0;
		        break;
		    }
		
		    int newRotation = (DHDRotation + increment) % 16;
		    return block.getMetaFromState(blockState.withProperty(propertyRotation, newRotation));
		}
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}

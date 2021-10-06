package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import ic2.api.energy.tile.IEnergyTile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import ic2.api.energy.EnergyNet;

public class CompatIndustrialCraft2 implements IBlockTransformer {
	
	private static Class<?> classIC2tileEntity;
	private static boolean isExperimental = false;
	
	public static void register() {
		try {
			try {
				// first, try IC2 Experimental
				classIC2tileEntity = Class.forName("ic2.core.block.TileEntityBlock");
				isExperimental = true;
			} catch (final ClassNotFoundException exception) {
				// then, try IC2 Classic
				classIC2tileEntity = Class.forName("ic2.core.block.base.tile.TileEntityBlock");
			}
			WarpDriveConfig.registerBlockTransformer("ic2", new CompatIndustrialCraft2());
		} catch (final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classIC2tileEntity.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
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
		// We need explicitly remove the tile entity from energy net and notify clients.
		// Either way jumping to previous coordinates will break rendering and energy grid.
		// See: https://github.com/LemADEC/WarpDrive/issues/503
		if (!isExperimental && tileEntity instanceof IEnergyTile) {
			final BlockPos blockPos = tileEntity.getPos();
			EnergyNet.instance.removeTile((IEnergyTile) tileEntity);
			world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 3);
		}
	}
	
	private static final short[] mrotFacing    = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		if (nbtTileEntity.getBoolean("targetSet")) {
			final int targetX = nbtTileEntity.getInteger("targetX");
			final int targetY = nbtTileEntity.getInteger("targetY");
			final int targetZ = nbtTileEntity.getInteger("targetZ");
			if (transformation.isInside(targetX, targetY, targetZ)) {
				final BlockPos chunkCoordinates = transformation.apply(targetX, targetY, targetZ);
				nbtTileEntity.setInteger("targetX", chunkCoordinates.getX());
				nbtTileEntity.setInteger("targetY", chunkCoordinates.getY());
				nbtTileEntity.setInteger("targetZ", chunkCoordinates.getZ());
			}
		}
		
		if ( rotationSteps == 0
		  || !nbtTileEntity.hasKey("facing")) {
			return metadata;
		}
		
		final short facing = nbtTileEntity.getShort("facing");
		switch (rotationSteps) {
		case 1:
			nbtTileEntity.setShort("facing", mrotFacing[facing]);
			return metadata;
		case 2:
			nbtTileEntity.setShort("facing", mrotFacing[mrotFacing[facing]]);
			return metadata;
		case 3:
			nbtTileEntity.setShort("facing", mrotFacing[mrotFacing[mrotFacing[facing]]]);
			return metadata;
		default:
			return metadata;
		}
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// IC2 Classic has its own approach to detect energy blocks and connect to them
		// we need to force a reconnection by simulating chunk unloading and a new 'first tick'
		if ( !isExperimental
		  && tileEntity != null ) {
			tileEntity.onChunkUnload();
			tileEntity.onLoad();
		}
	}
}

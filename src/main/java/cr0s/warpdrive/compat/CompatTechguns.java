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

public class CompatTechguns implements IBlockTransformer {
	
	private static Class<?> classBlockLadder;
	private static Class<?> classBlockLamp;
	private static Class<?> classBlockBasicMachine;
	private static Class<?> classBlockMultiBlockMachineBlock;
	
	public static void register() {
		try {
			classBlockLadder = Class.forName("techguns.blocks.BlockTGLadder");
			classBlockLamp = Class.forName("techguns.blocks.BlockTGLamp");
			classBlockBasicMachine = Class.forName("techguns.blocks.machines.BasicMachine");
			classBlockMultiBlockMachineBlock = Class.forName("techguns.blocks.machines.MultiBlockMachineBlock");
			WarpDriveConfig.registerBlockTransformer("Techguns", new CompatTechguns());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockLadder.isInstance(block)
		    || classBlockLamp.isInstance(block)
		    || classBlockBasicMachine.isInstance(block)
		    || classBlockMultiBlockMachineBlock.isInstance(block);
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
	// Block ladder: (metadata) 0 3 1 2 / 4 7 5 6 / 8 11 9 10 / 12 15 13 14             mrotLadder          techguns.blocks.BlockTGLadder
	// Block lamp: (metadata) 0 / 1 / 2 / 3 6 4 5 / 7 / 8 / 9 / 10 / 11 14 12 13 / 15   mrotLamp            techguns.blocks.BlockTGLamp
	// Tile basic machine: (metadata 0/1/4/9), rotation (byte) 0 1 2 3                  rotBasicMachine 	techguns.blocks.machines.BasicMachine:0/1/4/9
	// Tile basic machine: (metadata 2)                                                 -none-              techguns.blocks.machines.BasicMachine:2
	// Tile basic machine: (metadata 3 or 5), rotation (byte) 0 -1 -2 1                 rotRepairCamoBench  techguns.blocks.machines.BasicMachine:3/5
	// Tile basic machine: (metadata 6 or 8),
	//                      orientation (byte)   0 / 1 / 2 5 3 4                        orientExplosiveCharge	techguns.blocks.machines.BasicMachine:6/8
	//                      rotation (byte)      0 3 1 2                                rotExplosiveCharge
	
	
	// -----------------------------------------          {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotLadder             = {  3,  2,  0,  1,  7,  6,  4,  5, 11, 10,  8,  9, 15, 14, 12, 13 };
	private static final int[]   mrotLamp               = {  0,  1,  2,  6,  5,  3,  4,  7,  8,  9, 10, 14, 13, 11, 12, 15 };
	private static final byte[]  rotBasicMachine        = {  1,  2,  3,  0,  5,  3,  4,  7,  8,  9, 10, 14, 13, 11, 12, 15 };
	private static final byte[]  orientExplosiveCharge  = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[]  rotExplosiveCharge     = {  3,  2,  0,  1,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	private static byte rotRepairCamoBench(final byte value) {
		switch (value) {
		case  1: return (byte)  0;
		case  0: return (byte) -1;
		case -1: return (byte) -2;
		case -2: return (byte)  1;
		default: return value;
		}
	}
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		if (classBlockLadder.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotLadder[metadata];
			case 2:
				return mrotLadder[mrotLadder[metadata]];
			case 3:
				return mrotLadder[mrotLadder[mrotLadder[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (classBlockLamp.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotLamp[metadata];
			case 2:
				return mrotLamp[mrotLamp[metadata]];
			case 3:
				return mrotLamp[mrotLamp[mrotLamp[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (classBlockBasicMachine.isInstance(block)) {
			switch(metadata) {
			case 0:
			case 1:
			case 4:
			case 9:
				if (nbtTileEntity.hasKey("rotation")) {
					final byte rotation = nbtTileEntity.getByte("rotation");
					switch (rotationSteps) {
					case 1:
						nbtTileEntity.setByte("rotation", rotBasicMachine[rotation]);
						return metadata;
					case 2:
						nbtTileEntity.setByte("rotation", rotBasicMachine[rotBasicMachine[rotation]]);
						return metadata;
					case 3:
						nbtTileEntity.setByte("rotation", rotBasicMachine[rotBasicMachine[rotBasicMachine[rotation]]]);
						return metadata;
					default:
						return metadata;
					}
				}
				break;
			
			case 2:
				// no rotation
				return metadata;
			
			case 3:
			case 5:
				if (nbtTileEntity.hasKey("rotation")) {
					final byte rotation = nbtTileEntity.getByte("rotation");
					switch (rotationSteps) {
					case 1:
						nbtTileEntity.setByte("rotation", rotRepairCamoBench(rotation));
						return metadata;
					case 2:
						nbtTileEntity.setByte("rotation", rotRepairCamoBench(rotRepairCamoBench(rotation)));
						return metadata;
					case 3:
						nbtTileEntity.setByte("rotation", rotRepairCamoBench(rotRepairCamoBench(rotRepairCamoBench(rotation))));
						return metadata;
					default:
						return metadata;
					}
				}
				break;
			
			case 6:
			case 8:
				if (nbtTileEntity.hasKey("orientation")) {
					final byte orientation = nbtTileEntity.getByte("orientation");
					final byte rotation = nbtTileEntity.getByte("rotation");
					if (orientation == 0 || orientation == 1) {
						return metadata;
					}
					switch (rotationSteps) {
					case 1:
						nbtTileEntity.setByte("orientation", orientExplosiveCharge[orientation]);
						nbtTileEntity.setByte("rotation", rotExplosiveCharge[rotation]);
						return metadata;
					case 2:
						nbtTileEntity.setByte("orientation", orientExplosiveCharge[orientExplosiveCharge[orientation]]);
						nbtTileEntity.setByte("rotation", rotExplosiveCharge[rotExplosiveCharge[rotation]]);
						return metadata;
					case 3:
						nbtTileEntity.setByte("orientation", orientExplosiveCharge[orientExplosiveCharge[orientExplosiveCharge[orientation]]]);
						nbtTileEntity.setByte("rotation", rotExplosiveCharge[rotExplosiveCharge[rotExplosiveCharge[rotation]]]);
						return metadata;
					default:
						return metadata;
					}
				}
				break;
			
			case 7:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			default:
				return metadata;
			}
		}
		
		if ( classBlockMultiBlockMachineBlock.isInstance(block)
		  && nbtTileEntity.hasKey("hasMaster")
		  && nbtTileEntity.getBoolean("hasMaster") ) {
			final int xMaster = nbtTileEntity.getInteger("masterX");
			final int yMaster = nbtTileEntity.getShort("masterY");
			final int zMaster = nbtTileEntity.getInteger("masterZ");
			final ChunkCoordinates chunkCoordinatesMaster = transformation.apply(xMaster, yMaster, zMaster);
			nbtTileEntity.setInteger("masterX", chunkCoordinatesMaster.posX);
			nbtTileEntity.setInteger("masterY", chunkCoordinatesMaster.posY);
			nbtTileEntity.setInteger("masterZ", chunkCoordinatesMaster.posZ);
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

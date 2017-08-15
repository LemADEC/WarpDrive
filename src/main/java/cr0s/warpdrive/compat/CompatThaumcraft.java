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

import net.minecraftforge.common.util.Constants;

public class CompatThaumcraft implements IBlockTransformer {
	
	private static Class<?> classBlockArcaneDoor;
	private static Class<?> classBlockChestHungry;
	private static Class<?> classBlockEssentiaReservoir;
	private static Class<?> classBlockJar;
	private static Class<?> classBlockMetalDevice;
	private static Class<?> classBlockMirror;
	private static Class<?> classBlockTable;
	private static Class<?> classBlockTube;
	private static Class<?> classBlockWoodenDevice;
	
	public static void register() {
		try {
			classBlockArcaneDoor = Class.forName("thaumcraft.common.blocks.BlockArcaneDoor");
			classBlockChestHungry = Class.forName("thaumcraft.common.blocks.BlockChestHungry");
			classBlockEssentiaReservoir = Class.forName("thaumcraft.common.blocks.BlockEssentiaReservoir");
			classBlockJar = Class.forName("thaumcraft.common.blocks.BlockJar");
			classBlockMetalDevice = Class.forName("thaumcraft.common.blocks.BlockMetalDevice");
			classBlockMirror = Class.forName("thaumcraft.common.blocks.BlockMirror");
			classBlockTable = Class.forName("thaumcraft.common.blocks.BlockTable");
			classBlockTube = Class.forName("thaumcraft.common.blocks.BlockTube");
			classBlockWoodenDevice = Class.forName("thaumcraft.common.blocks.BlockWoodenDevice");
			WarpDriveConfig.registerBlockTransformer("Thaumcraft", new CompatThaumcraft());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockArcaneDoor.isInstance(block)
			|| classBlockChestHungry.isInstance(block)
			|| classBlockEssentiaReservoir.isInstance(block)
			|| classBlockJar.isInstance(block)
			|| classBlockMetalDevice.isInstance(block)
			|| classBlockMirror.isInstance(block)
			|| classBlockTable.isInstance(block)
			|| classBlockTube.isInstance(block)
			|| classBlockWoodenDevice.isInstance(block);
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
	
	// Vanilla supported: stairs
	// Not rotating: arcane workbench, deconstruction table, crystals, candles, crucible, alchemical centrifuge
	
	// Transformation handling required:
	// Tile arcane door: (metadata) 0 1 2 3 / 8						mrotArcaneDoor	thaumcraft.common.blocks.BlockArcaneDoor
	// Tile Hungry chest: (metadata) 2 5 3 4						mrotHungryChest	thaumcraft.common.blocks.BlockChestHungry
	// Tile essentia reservoir: face (byte) 0 / 1 / 2 5 3 4			rotForgeByte	thaumcraft.common.blocks.BlockEssentiaReservoir
	// Tile jar: facing (byte) 2 5 3 4								rotForgeByte	thaumcraft.common.blocks.BlockJar
	// Tile vis relay: orientation (short) 0 / 1 / 2 5 3 4			rotForgeShort	thaumcraft.common.blocks.BlockMetalDevice
	// Tile arcane lamp: orientation (int) 2 5 3 4					rotForgeInt		thaumcraft.common.blocks.BlockMetalDevice
	// Tile syphon (Arcane alembic): facing (byte) 2 5 3 4			rotForgeByte	thaumcraft.common.blocks.BlockMetalDevice
	// Tile mirror: (metadata) 0 / 1 / 2 5 3 4 / 6 / 7 / 8 11 9 10	mrotMirror		thaumcraft.common.blocks.BlockMirror
	// Tile mirror: linkX/Y/Z (int)									n/a				thaumcraft.common.blocks.BlockMirror
	// Tile table: (metadata) 0 1 / 2 5 3 4 / 6 9 7 8				mrotTable		thaumcraft.common.blocks.BlockTable
	// Tile tube, Tile tube valve: side (int) 0 / 1 / 2 5 3 4		rotForgeInt		thaumcraft.common.blocks.BlockTube
	// Tile essentia crystalizer: face (byte) 0 / 1 / 2 5 3 4		rotForgeByte	thaumcraft.common.blocks.BlockTube
	// Tile bellows: orientation (byte) 0 / 1 / 2 5 3 4				rotForgeByte	thaumcraft.common.blocks.BlockWoodenDevice
	// Tile arcane bore base: orientation (int) 2 5 3 4				rotForgeInt		thaumcraft.common.blocks.BlockWoodenDevice
	// Tile banner: facing (byte) 0 4 8 12							rotBanner		thaumcraft.common.blocks.BlockWoodenDevice
	
	// -----------------------------------------    {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotArcaneDoor   = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotHungryChest  = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotMirror       = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 14, 15 };
	private static final int[]   mrotTable        = {  1,  0,  5,  4,  2,  3,  9,  8,  6,  7, 10, 11, 12, 13, 14, 15 };
	private static final byte[]  rotForgeByte     = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   rotForgeInt      = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final short[] rotForgeShort    = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[]  rotBanner        = {  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,  0,  1,  2,  3 };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		
		if (classBlockArcaneDoor.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotArcaneDoor[metadata];
			case 2:
				return mrotArcaneDoor[mrotArcaneDoor[metadata]];
			case 3:
				return mrotArcaneDoor[mrotArcaneDoor[mrotArcaneDoor[metadata]]];
			default:
				return metadata;
			}
		}
		if (classBlockChestHungry.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotHungryChest[metadata];
			case 2:
				return mrotHungryChest[mrotHungryChest[metadata]];
			case 3:
				return mrotHungryChest[mrotHungryChest[mrotHungryChest[metadata]]];
			default:
				return metadata;
			}
		}
		if (classBlockEssentiaReservoir.isInstance(block)) {
			if (nbtTileEntity.hasKey("face")) {
				short direction = nbtTileEntity.getByte("face");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("face", rotForgeByte[direction]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("face", rotForgeByte[rotForgeByte[direction]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("face", rotForgeByte[rotForgeByte[rotForgeByte[direction]]]);
					return metadata;
				default:
					return metadata;
				}
			}
		}
		if (classBlockJar.isInstance(block)) {
			if (nbtTileEntity.hasKey("facing")) {
				short direction = nbtTileEntity.getByte("facing");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("facing", rotForgeByte[direction]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("facing", rotForgeByte[rotForgeByte[direction]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("facing", rotForgeByte[rotForgeByte[rotForgeByte[direction]]]);
					return metadata;
				default:
					return metadata;
				}
			}
		}
		if (classBlockMetalDevice.isInstance(block)) {
			if (nbtTileEntity.hasKey("orientation") && nbtTileEntity.hasKey("orientation", Constants.NBT.TAG_SHORT)) {
				short orientation = nbtTileEntity.getShort("orientation");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setShort("orientation", rotForgeShort[orientation]);
					return metadata;
				case 2:
					nbtTileEntity.setShort("orientation", rotForgeShort[rotForgeShort[orientation]]);
					return metadata;
				case 3:
					nbtTileEntity.setShort("orientation", rotForgeShort[rotForgeShort[rotForgeShort[orientation]]]);
					return metadata;
				default:
					return metadata;
				}
			}
			if (nbtTileEntity.hasKey("orientation") && nbtTileEntity.hasKey("orientation", Constants.NBT.TAG_INT)) {
				int orientation = nbtTileEntity.getInteger("orientation");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setInteger("orientation", rotForgeInt[orientation]);
					return metadata;
				case 2:
					nbtTileEntity.setInteger("orientation", rotForgeInt[rotForgeInt[orientation]]);
					return metadata;
				case 3:
					nbtTileEntity.setInteger("orientation", rotForgeInt[rotForgeInt[rotForgeInt[orientation]]]);
					return metadata;
				default:
					return metadata;
				}
			}
			if (nbtTileEntity.hasKey("facing") && nbtTileEntity.hasKey("facing", Constants.NBT.TAG_BYTE)) {
				byte facing = nbtTileEntity.getByte("facing");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("facing", rotForgeByte[facing]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("facing", rotForgeByte[rotForgeByte[facing]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("facing", rotForgeByte[rotForgeByte[rotForgeByte[facing]]]);
					return metadata;
				default:
					return metadata;
				}
			}
		}
		if (classBlockMirror.isInstance(block)) {
			if (nbtTileEntity.hasKey("linkX") && nbtTileEntity.hasKey("linkY") && nbtTileEntity.hasKey("linkZ")) {
				ChunkCoordinates targetLink = transformation.apply(nbtTileEntity.getInteger("linkX"), nbtTileEntity.getInteger("linkY"), nbtTileEntity.getInteger("linkZ"));
				nbtTileEntity.setInteger("linkX", targetLink.posX);
				nbtTileEntity.setInteger("linkY", targetLink.posY);
				nbtTileEntity.setInteger("linkZ", targetLink.posZ);
			}
			
			switch (rotationSteps) {
			case 1:
				return mrotMirror[metadata];
			case 2:
				return mrotMirror[mrotMirror[metadata]];
			case 3:
				return mrotMirror[mrotMirror[mrotMirror[metadata]]];
			default:
				return metadata;
			}
		}
		if (classBlockTable.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotTable[metadata];
			case 2:
				return mrotTable[mrotTable[metadata]];
			case 3:
				return mrotTable[mrotTable[mrotTable[metadata]]];
			default:
				return metadata;
			}
		}
		if (classBlockTube.isInstance(block)) {
			if (nbtTileEntity.hasKey("side") && nbtTileEntity.hasKey("side", Constants.NBT.TAG_INT)) {
				int side = nbtTileEntity.getByte("side");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setInteger("side", rotForgeInt[side]);
					return metadata;
				case 2:
					nbtTileEntity.setInteger("side", rotForgeInt[rotForgeInt[side]]);
					return metadata;
				case 3:
					nbtTileEntity.setInteger("side", rotForgeInt[rotForgeInt[rotForgeInt[side]]]);
					return metadata;
				default:
					return metadata;
				}
			}
			if (nbtTileEntity.hasKey("face") && nbtTileEntity.hasKey("face", Constants.NBT.TAG_BYTE)) {
				byte face = nbtTileEntity.getByte("face");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("face", rotForgeByte[face]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("face", rotForgeByte[rotForgeByte[face]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("face", rotForgeByte[rotForgeByte[rotForgeByte[face]]]);
					return metadata;
				default:
					return metadata;
				}
			}
		}
		if (classBlockWoodenDevice.isInstance(block)) {
			// Tile bellows: orientation (byte) 0 / 1 / 2 5 3 4				rotForgeByte	thaumcraft.common.blocks.BlockWoodenDevice
			// Tile arcane bore base: orientation (int) 2 5 3 4				rotForgeInt		thaumcraft.common.blocks.BlockWoodenDevice
			// Tile banner: facing (byte) 0 4 8 12							rotBanner		thaumcraft.common.blocks.BlockWoodenDevice
			if (nbtTileEntity.hasKey("orientation") && nbtTileEntity.hasKey("orientation", Constants.NBT.TAG_BYTE)) {
				byte orientation = nbtTileEntity.getByte("orientation");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("orientation", rotForgeByte[orientation]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("orientation", rotForgeByte[rotForgeByte[orientation]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("orientation", rotForgeByte[rotForgeByte[rotForgeByte[orientation]]]);
					return metadata;
				default:
					return metadata;
				}
			}
			if (nbtTileEntity.hasKey("orientation") && nbtTileEntity.hasKey("orientation", Constants.NBT.TAG_INT)) {
				int orientation = nbtTileEntity.getByte("orientation");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setInteger("orientation", rotForgeInt[orientation]);
					return metadata;
				case 2:
					nbtTileEntity.setInteger("orientation", rotForgeInt[rotForgeInt[orientation]]);
					return metadata;
				case 3:
					nbtTileEntity.setInteger("orientation", rotForgeInt[rotForgeInt[rotForgeInt[orientation]]]);
					return metadata;
				default:
					return metadata;
				}
			}
			if (nbtTileEntity.hasKey("facing") && nbtTileEntity.hasKey("facing", Constants.NBT.TAG_BYTE)) {
				byte facing = nbtTileEntity.getByte("facing");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("facing", rotBanner[facing]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("facing", rotBanner[rotBanner[facing]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("facing", rotBanner[rotBanner[rotBanner[facing]]]);
					return metadata;
				default:
					return metadata;
				}
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

package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class CompatComputerCraft implements IBlockTransformer {
	
	private static Class<?> classBlockDirectional;
	private static Class<?> classBlockComputerBase;
	private static Class<?> classBlockCable;
	private static Class<?> classBlockPeripheral;
	private static Class<?> classBlockTurtle;
	
	public static void register() {
		try {
			classBlockDirectional  = Class.forName("dan200.computercraft.shared.common.BlockDirectional");
			classBlockComputerBase = Class.forName("dan200.computercraft.shared.computer.blocks.BlockComputerBase");
			classBlockCable        = Class.forName("dan200.computercraft.shared.peripheral.common.BlockCable");
			classBlockPeripheral   = Class.forName("dan200.computercraft.shared.peripheral.common.BlockPeripheral");
			classBlockTurtle       = Class.forName("dan200.computercraft.shared.turtle.blocks.BlockTurtle");
			WarpDriveConfig.registerBlockTransformer("ComputerCraft", new CompatComputerCraft());
		} catch(ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockDirectional.isInstance(block);
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
	
	// CC-Computer rotations
	private static final int[] mrotComputer      = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 13, 12, 10, 11, 14, 15 };	// computer (normal/advanced)
	// CC-Peripheral rotations
	private static final int[] mrotPeripheral    = {  0,  1,  5,  4,  2,  3,  9,  8,  6,  7, 10, 11, 12, 13, 14, 15 };	// disk drive (2-5), wireless modem (0-1/6-9), monitor (10/12), printer (11)
	// CC-Cable rotations
	private static final int[] mrotWiredModem    = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 14, 15 };	// wired modem, cable
	// NBT rotations for printer, monitor and turtles
	private static final int[] rotDir            = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16 };	// printer, monitor, turtle
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if (classBlockComputerBase.isInstance(block) && !classBlockTurtle.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotComputer[metadata];
			case 2:
				return mrotComputer[mrotComputer[metadata]];
			case 3:
				return mrotComputer[mrotComputer[mrotComputer[metadata]]];
			default:
				return metadata;
			}
		}
		if (classBlockCable.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotWiredModem[metadata];
			case 2:
				return mrotWiredModem[mrotWiredModem[metadata]];
			case 3:
				return mrotWiredModem[mrotWiredModem[mrotWiredModem[metadata]]];
			default:
				return metadata;
			}
		}
		if (classBlockPeripheral.isInstance(block)) {
			if (metadata >= 2 && metadata <= 9) {// disk drive, wireless modem, monitor, printer 
				switch (rotationSteps) {
				case 1:
					return mrotPeripheral[metadata];
				case 2:
					return mrotPeripheral[mrotPeripheral[metadata]];
				case 3:
					return mrotPeripheral[mrotPeripheral[mrotPeripheral[metadata]]];
				default:
					return metadata;
				}
			}
			if (!nbtTileEntity.hasKey("dir")) {// unknown
				WarpDrive.logger.error("Unknown ComputerCraft Peripheral block " + block + " with metadata " + metadata + " and tile entity " + nbtTileEntity);
				return metadata;
			}
			// printer or monitor have the dir tag
		} else if (!nbtTileEntity.hasKey("dir")) {// unknown
			WarpDrive.logger.error("Unknown ComputerCraft directional block " + block + " with metadata " + metadata + " and tile entity " + nbtTileEntity);
			return metadata;
		}
		// turtles and others
		int dir = nbtTileEntity.getInteger("dir");
		switch (rotationSteps) {
		case 1:
			nbtTileEntity.setInteger("dir", rotDir[dir]);
			return metadata;
		case 2:
			nbtTileEntity.setInteger("dir", rotDir[rotDir[dir]]);
			return metadata;
		case 3:
			nbtTileEntity.setInteger("dir", rotDir[rotDir[rotDir[dir]]]);
			return metadata;
		default:
			return metadata;
		}
	}
	
	@Override
	public void restoreExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}

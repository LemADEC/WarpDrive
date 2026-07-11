package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;

class JumpGateScanner {
	
	// inputs
	private IBlockAccess blockAccess;
	private int minX, minY, minZ;
	private int maxX, maxY, maxZ;
	
	// execution
	private int x;
	private int y;
	private int z;
	private MutableBlockPos mutableBlockPos;
	
	// output
	public int volumeUsed = 0;
	
	JumpGateScanner(final IBlockAccess blockAccess,
	                final int minX, final int minY, final int minZ,
	                final int maxX, final int maxY, final int maxZ) {
		this.blockAccess = blockAccess;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		x = this.minX;
		y = this.minY;
		z = this.minZ;
		mutableBlockPos = new MutableBlockPos(x, y, z);
	}
	
	boolean tick() {
		int countBlocks = 0;
		
		try {
			while (countBlocks < WarpDriveConfig.SHIP_VOLUME_SCAN_BLOCKS_PER_TICK) {
				mutableBlockPos.setPos(x, y, z);
				final Block block = blockAccess.getBlockState(mutableBlockPos).getBlock();
				countBlocks++;
				
				// skipping vanilla air & ignored blocks
				if (block != Blocks.AIR && !Dictionary.BLOCKS_LEFTBEHIND.contains(block)) {
					volumeUsed++;
				}
				
				// loop y first to stay in same chunk, then z, then x
				y++;
				if (y > maxY) {
					y = minY;
					z++;
					if (z > maxZ) {
						z = minZ;
						x++;
						if (x > maxX) {
							return true;
						}
					}
				}
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
		return false;
	}
}

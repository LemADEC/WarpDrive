package cr0s.warpdrive.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;

public class BlockAir extends Block
{
	private final boolean TRANSPARENT_AIR = true;
	private final boolean AIR_DEBUG = false;
	private final int AIR_BLOCK_TICKS = 40;
	private IIcon[] iconBuffer;

	public BlockAir() {
		super(Material.air);
		setHardness(0.0F);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean isAir(IBlockAccess var1, int var2, int var3, int var4) {
		return true;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World var1, int var2, int var3, int var4) {
		return null;
	}

	@Override
	public boolean isReplaceable(IBlockAccess var1, int var2, int var3, int var4) {
		return true;
	}

	@Override
	public boolean canPlaceBlockAt(World var1, int var2, int var3, int var4) {
		return true;
	}

	@Override
	public boolean canCollideCheck(int var1, boolean var2) {
		return false;
	}

	@Override
	public int getRenderBlockPass() {
		return TRANSPARENT_AIR ? 1 : 0;
	}

	@Override
	public void registerBlockIcons(IIconRegister par1IconRegister) {
		if (AIR_DEBUG) {
			iconBuffer = new IIcon[16];
			iconBuffer[ 0] = par1IconRegister.registerIcon("warpdrive:airBlock0");
			iconBuffer[ 1] = par1IconRegister.registerIcon("warpdrive:airBlock1");
			iconBuffer[ 2] = par1IconRegister.registerIcon("warpdrive:airBlock2");
			iconBuffer[ 3] = par1IconRegister.registerIcon("warpdrive:airBlock3");
			iconBuffer[ 4] = par1IconRegister.registerIcon("warpdrive:airBlock4");
			iconBuffer[ 5] = par1IconRegister.registerIcon("warpdrive:airBlock5");
			iconBuffer[ 6] = par1IconRegister.registerIcon("warpdrive:airBlock6");
			iconBuffer[ 7] = par1IconRegister.registerIcon("warpdrive:airBlock7");
			iconBuffer[ 8] = par1IconRegister.registerIcon("warpdrive:airBlock8");
			iconBuffer[ 9] = par1IconRegister.registerIcon("warpdrive:airBlock9");
			iconBuffer[10] = par1IconRegister.registerIcon("warpdrive:airBlock10");
			iconBuffer[11] = par1IconRegister.registerIcon("warpdrive:airBlock11");
			iconBuffer[12] = par1IconRegister.registerIcon("warpdrive:airBlock12");
			iconBuffer[13] = par1IconRegister.registerIcon("warpdrive:airBlock13");
			iconBuffer[14] = par1IconRegister.registerIcon("warpdrive:airBlock14");
			iconBuffer[15] = par1IconRegister.registerIcon("warpdrive:airBlock15");
		} else {
			blockIcon = par1IconRegister.registerIcon("warpdrive:airBlock");
		}
	}

	@Override
	public IIcon getIcon(int side, int metadata) {
		if (AIR_DEBUG) {
			return iconBuffer[metadata];
		} else {
			return blockIcon;
		}
	}

	@Override
	public int getMobilityFlag() {
		return 1;
	}

	@Override
	public Item getItemDropped(int var1, Random var2, int var3) {
		return null;
	}

	/**
	 * Returns the quantity of items to drop on block destruction.
	 */
	@Override
	public int quantityDropped(Random par1Random) {
		return 0;
	}

	/**
	 * How many world ticks before ticking
	 */
	@Override
	public int tickRate(World par1World) {
		return AIR_BLOCK_TICKS;
	}

	/**
	 * Ticks the block if it's been scheduled
	 */
	@Override
	public void updateTick(World par1World, int x, int y, int z, Random par5Random) {
		int concentration = par1World.getBlockMetadata(x, y, z);
		boolean isInSpaceWorld = par1World.provider.dimensionId == WarpDriveConfig.G_SPACE_DIMENSION_ID || par1World.provider.dimensionId == WarpDriveConfig.G_HYPERSPACE_DIMENSION_ID;

		// Remove air block to vacuum block
		if (concentration <= 0 || !isInSpaceWorld) {
			par1World.setBlock(x, y, z, Blocks.air, 0, 3); // replace our air block to vacuum block
		} else {
			// Try to spread the air
			spreadAirBlock(par1World, x, y, z, concentration);
		}

		par1World.scheduleBlockUpdate(x, y, z, this, AIR_BLOCK_TICKS);
	}

	@Override
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
		if (AIR_DEBUG) {
			return side == 0;
		}

		Block sideBlock = world.getBlock(x, y, z);
		if (sideBlock.isAssociatedBlock(this)) { //TODO: Check if this works
			return false;
		}
		return world.isAirBlock(x, y, z);
	}

	private void spreadAirBlock(World world, int x, int y, int z, int concentration) {
		int air_count = 1;
		int empty_count = 0;
		int sum_concentration = concentration;

		// Count air in adjacent blocks
		Block xp_block = world.getBlock(x + 1, y, z);
		boolean xp_isAir = world.isAirBlock(x + 1, y, z);;
		int xp_concentration = (xp_block.isAssociatedBlock(this)) ? 0 : world.getBlockMetadata(x + 1, y, z);
		if (xp_isAir) {
			air_count++;
			if (xp_concentration > 0) {
				sum_concentration += xp_concentration;
			} else {
				empty_count++;
			}
		}
		Block xn_block = world.getBlock(x - 1, y, z);
		boolean xn_isAir = world.isAirBlock(x - 1, y, z);
		int xn_concentration = (xn_block != this) ? 0 : world.getBlockMetadata(x - 1, y, z);
		if (xn_isAir) {
			air_count++;
			if (xn_concentration > 0) {
				sum_concentration += xn_concentration;
			} else {
				empty_count++;
			}
		}
		Block yp_block = world.getBlock(x, y + 1, z);
		boolean yp_isAir = world.isAirBlock(x, y + 1, z);
		int yp_concentration = (yp_block != this) ? 0 : world.getBlockMetadata(x, y + 1, z);
		if (yp_isAir) {
			air_count++;
			if (yp_concentration > 0) {
				sum_concentration += yp_concentration;
			} else {
				empty_count++;
			}
		}
		Block yn_block = world.getBlock(x, y - 1, z);
		boolean yn_isAir = world.isAirBlock(x, y - 1, z);
		int yn_concentration = (yn_block != this) ? 0 : world.getBlockMetadata(x, y - 1, z);
		if (yn_isAir) {
			air_count++;
			if (yn_concentration > 0) {
				sum_concentration += yn_concentration;
			} else {
				empty_count++;
			}
		}
		Block zp_block = world.getBlock(x, y, z + 1);
		boolean zp_isAir = world.isAirBlock(x, y, z + 1);
		int zp_concentration = (zp_block != this) ? 0 : world.getBlockMetadata(x, y, z + 1);
		if (zp_isAir) {
			air_count++;
			if (zp_concentration > 0) {
				sum_concentration += zp_concentration;
			} else {
				empty_count++;
			}
		}
		Block zn_block = world.getBlock(x, y, z - 1);
		boolean zn_isAir = world.isAirBlock(x, y, z - 1);
		int zn_concentration = (zn_block != this) ? 0 : world.getBlockMetadata(x, y, z - 1);
		if (zn_isAir) {
			air_count++;
			if (zn_concentration > 0) {
				sum_concentration += zn_concentration;
			} else {
				empty_count++;
			}
		}

		if (empty_count > 0) {
			if (concentration < 8) {
				sum_concentration -= empty_count;
			} else if (concentration < 4) {
				sum_concentration -= empty_count + (world.rand.nextBoolean() ? 0 : empty_count);
			} else {
				sum_concentration -= 1;
			}
		}
		if (sum_concentration < 0) sum_concentration = 0;
		int mid_concentration = (int) Math.floor(sum_concentration * 1.0F / air_count);
		int new_concentration = sum_concentration - mid_concentration * (air_count - 1);
		if (new_concentration > 14) {
			new_concentration = 14;
		}
		if (concentration != new_concentration) {
			if (concentration == 15) {
				if (!xp_block.isAssociatedBlock(WarpDrive.airgenBlock) && !xn_block.isAssociatedBlock(WarpDrive.airgenBlock)
						&& !yp_block.isAssociatedBlock(WarpDrive.airgenBlock) && !yn_block.isAssociatedBlock(WarpDrive.airgenBlock)
						&& !zp_block.isAssociatedBlock(WarpDrive.airgenBlock) && !zn_block.isAssociatedBlock(WarpDrive.airgenBlock)) {
					//        			WarpDrive.debugPrint("AirGenerator not found, removing air block at " + x + ", " + y + ", " + z);
					world.setBlockMetadataWithNotify(x, y, z, 1, 2);
				} else {
					// keep the block as a source
					/*	        		WarpDrive.debugPrint("15 + "
        			  + xp_concentration + " " + xn_concentration + " "
        			  + yp_concentration + " " + yn_concentration + " "
        			  + zp_concentration + " " + zn_concentration + " = " + sum_concentration + " total, " + empty_count + " empty / " + air_count + " -> " + new_concentration);/**/
				}
			} else {
				world.setBlockMetadataWithNotify(x, y, z, new_concentration, 2);
			}
		}

		// Check and setup air to adjacent blocks
		if (xp_isAir) {
			if (xp_block.isAssociatedBlock(this)) {
				if (xp_concentration != mid_concentration && xp_concentration != 15) {
					world.setBlockMetadataWithNotify(x + 1, y, z, mid_concentration, 2);
				}
			} else {
				world.setBlock(x + 1, y, z, this, mid_concentration, 2);
			}
		}

		if (xn_isAir) {
			if (xn_block.isAssociatedBlock(this)) {
				if (xn_concentration != mid_concentration && xn_concentration != 15) {
					world.setBlockMetadataWithNotify(x - 1, y, z, mid_concentration, 2);
				}
			} else {
				world.setBlock(x - 1, y, z, this, mid_concentration, 2);
			}
		}

		if (yp_isAir) {
			if (yp_block.isAssociatedBlock(this)) {
				if (yp_concentration != mid_concentration && yp_concentration != 15) {
					world.setBlockMetadataWithNotify(x, y + 1, z, mid_concentration, 2);
				}
			} else {
				world.setBlock(x, y + 1, z, this, mid_concentration, 2);
			}
		}

		if (yn_isAir) {
			if (yn_block.isAssociatedBlock(this)) {
				if (yn_concentration != mid_concentration && yn_concentration != 15) {
					world.setBlockMetadataWithNotify(x, y - 1, z, mid_concentration, 2);
				}
			} else {
				world.setBlock(x, y - 1, z, this, mid_concentration, 2);
			}
		}

		if (zp_isAir) {
			if (zp_block.isAssociatedBlock(this)) {
				if (zp_concentration != mid_concentration && zp_concentration != 15) {
					world.setBlockMetadataWithNotify(x, y, z + 1, mid_concentration, 2);
				}
			} else {
				world.setBlock(x, y, z + 1, this, mid_concentration, 2);
			}
		}

		if (zn_isAir) {
			if (zn_block.isAssociatedBlock(this)) {
				if (zn_concentration != mid_concentration && zn_concentration != 15) {
					world.setBlockMetadataWithNotify(x, y, z - 1, mid_concentration, 2);
				}
			} else {
				world.setBlock(x, y, z - 1, this, mid_concentration, 2);
			}
		}
	}

	/**
	 * Returns if this block is collidable. Args: x, y, z
	 */
	@Override
	public boolean isCollidable() {
		return false;
	}

	@Override
	public void onBlockAdded(World par1World, int par2, int par3, int par4) {
		if (par1World.provider.dimensionId == WarpDriveConfig.G_SPACE_DIMENSION_ID || par1World.provider.dimensionId == WarpDriveConfig.G_HYPERSPACE_DIMENSION_ID) {
			par1World.scheduleBlockUpdate(par2, par3, par4, this, this.tickRate(par1World));
		} else {
			par1World.setBlockToAir(par2, par3, par4);
		}
	}
}
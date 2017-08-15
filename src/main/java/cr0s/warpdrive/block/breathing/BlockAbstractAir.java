package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.render.RenderBlockStandard;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class BlockAbstractAir extends BlockAbstractBase {
	
	@SideOnly(Side.CLIENT)
	private IIcon[] iconBuffer;
	
	public BlockAbstractAir() {
		super(Material.fire);
		setHardness(0.0F);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
		setBlockName("warpdrive.breathing.air");
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public boolean isAir(IBlockAccess blockAccess, int x, int y, int z) {
		return true;
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
	}
	
	@Override
	public boolean isReplaceable(IBlockAccess blockAccess, int x, int y, int z) {
		return true;
	}
	
	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return true;
	}
	
	@Override
	public boolean canCollideCheck(int metadata, boolean hitIfLiquid) {
		return false;
	}
	
	@Override
	public int getRenderBlockPass() {
		// 1 is required to apply alpha transparency
		return 1;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		if (WarpDriveConfig.BREATHING_AIR_BLOCK_DEBUG) {
			iconBuffer = new IIcon[16];
			iconBuffer[ 0] = iconRegister.registerIcon("warpdrive:breathing/air0");
			iconBuffer[ 1] = iconRegister.registerIcon("warpdrive:breathing/air1");
			iconBuffer[ 2] = iconRegister.registerIcon("warpdrive:breathing/air2");
			iconBuffer[ 3] = iconRegister.registerIcon("warpdrive:breathing/air3");
			iconBuffer[ 4] = iconRegister.registerIcon("warpdrive:breathing/air4");
			iconBuffer[ 5] = iconRegister.registerIcon("warpdrive:breathing/air5");
			iconBuffer[ 6] = iconRegister.registerIcon("warpdrive:breathing/air6");
			iconBuffer[ 7] = iconRegister.registerIcon("warpdrive:breathing/air7");
			iconBuffer[ 8] = iconRegister.registerIcon("warpdrive:breathing/air8");
			iconBuffer[ 9] = iconRegister.registerIcon("warpdrive:breathing/air9");
			iconBuffer[10] = iconRegister.registerIcon("warpdrive:breathing/air10");
			iconBuffer[11] = iconRegister.registerIcon("warpdrive:breathing/air11");
			iconBuffer[12] = iconRegister.registerIcon("warpdrive:breathing/air12");
			iconBuffer[13] = iconRegister.registerIcon("warpdrive:breathing/air13");
			iconBuffer[14] = iconRegister.registerIcon("warpdrive:breathing/air14");
			iconBuffer[15] = iconRegister.registerIcon("warpdrive:breathing/air15");
		} else {
			blockIcon = iconRegister.registerIcon("warpdrive:breathing/air");
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int metadata) {
		if (WarpDriveConfig.BREATHING_AIR_BLOCK_DEBUG) {
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
	public Item getItemDropped(int metadata, Random random, int fortune) {
		return null;
	}
	
	@Override
	public int quantityDropped(Random random) {
		return 0;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		if (WarpDriveConfig.BREATHING_AIR_BLOCK_DEBUG) {
			return side == 0 || side == 1;
		}
		
		final Block blockSide = blockAccess.getBlock(x, y, z);
		if (blockSide instanceof BlockAbstractAir) {
			return false;
		}
		
		return blockAccess.isAirBlock(x, y, z);
	}
	
	@Override
	public int getRenderType() {
		return RenderBlockStandard.renderId;
	}
	
	@Override
	public boolean isCollidable() {
		return false;
	}
}
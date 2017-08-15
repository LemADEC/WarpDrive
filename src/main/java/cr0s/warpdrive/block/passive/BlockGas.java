package cr0s.warpdrive.block.passive;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockGas extends Block {
	
	@SideOnly(Side.CLIENT)
	private IIcon[] iconBuffer;

	public BlockGas() {
		super(Material.fire);
		setHardness(0.0F);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
		setBlockName("warpdrive.decoration.gas");
	}
	
	@Override
	public void getSubBlocks(Item item, CreativeTabs creativeTabs, List list) {
		for (int index = 0; index < 12; index++) {
			list.add(new ItemStack(item, 1, index));
		}
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
		return 1; // transparency enabled
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		iconBuffer = new IIcon[12];
		iconBuffer[0] = iconRegister.registerIcon("warpdrive:decoration/gas-blue");
		iconBuffer[1] = iconRegister.registerIcon("warpdrive:decoration/gas-red");
		iconBuffer[2] = iconRegister.registerIcon("warpdrive:decoration/gas-green");
		iconBuffer[3] = iconRegister.registerIcon("warpdrive:decoration/gas-yellow");
		iconBuffer[4] = iconRegister.registerIcon("warpdrive:decoration/gas-dark");
		iconBuffer[5] = iconRegister.registerIcon("warpdrive:decoration/gas-darkness");
		iconBuffer[6] = iconRegister.registerIcon("warpdrive:decoration/gas-white");
		iconBuffer[7] = iconRegister.registerIcon("warpdrive:decoration/gas-milk");
		iconBuffer[8] = iconRegister.registerIcon("warpdrive:decoration/gas-orange");
		iconBuffer[9] = iconRegister.registerIcon("warpdrive:decoration/gas-siren");
		iconBuffer[10] = iconRegister.registerIcon("warpdrive:decoration/gas-gray");
		iconBuffer[11] = iconRegister.registerIcon("warpdrive:decoration/gas-violet");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int metadata) {
		return iconBuffer[metadata % iconBuffer.length];
	}

	@Override
	public int getMobilityFlag() {
		return 1;
	}

	@Override
	public Item getItemDropped(int var1, Random var2, int var3) {
		return null;
	}

	@Override
	public int quantityDropped(Random par1Random) {
		return 0;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		final Block blockSide = blockAccess.getBlock(x, y, z);
		if (blockSide.isAssociatedBlock(this)) {
			return false;
		}
		return blockAccess.isAirBlock(x, y, z);
	}

	@Override
	public boolean isCollidable() {
		return false;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		// Gas blocks are only allowed in space
		if (CelestialObjectManager.hasAtmosphere(world, x, z)) {
			world.setBlockToAir(x, y, z);
		}
	}
}
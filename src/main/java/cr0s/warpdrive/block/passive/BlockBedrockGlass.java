package cr0s.warpdrive.block.passive;

import cr0s.warpdrive.WarpDrive;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockBedrockGlass extends Block {
	
	@SideOnly(Side.CLIENT)
	private IIcon icon;
	
	public BlockBedrockGlass() {
		super(Material.fire);
		setHardness(0.0F);
		setBlockUnbreakable();
		setResistance(6000000.0F);
		setStepSound(soundTypePiston);
		disableStats();
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
		setBlockName("warpdrive.decoration.bedrock_glass");
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public boolean isAir(IBlockAccess blockAccess, int x, int y, int z) {
		return false;
	}
	
	@Override
	public boolean isReplaceable(IBlockAccess var1, int x, int y, int z) {
		return false;
	}
	
	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return true;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		icon = iconRegister.registerIcon("warpdrive:decoration/bedrock_glass");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int metadata) {
		return icon;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		return false;
	}
	
	@Override
	public int getMobilityFlag() {
		return 2;
	}
	
	@Override
	public Item getItemDropped(int var1, Random random, int var3) {
		return null;
	}
	
	@Override
	public int quantityDropped(Random random) {
		return 0;
	}
	
	@Override
	protected boolean canSilkHarvest() {
		return false;
	}
	
	@Override
	public boolean isCollidable() {
		return true;
	}
}
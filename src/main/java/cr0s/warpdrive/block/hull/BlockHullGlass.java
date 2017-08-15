package cr0s.warpdrive.block.hull;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IDamageReceiver;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;

import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockHullGlass extends BlockColored implements IBlockBase, IDamageReceiver {
	
	final byte tier;
	
	public BlockHullGlass(final byte tier) {
		super(Material.glass);
		this.tier = tier;
		setHardness(WarpDriveConfig.HULL_HARDNESS[tier - 1]);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[tier - 1] * 5 / 3);
		setStepSound(Block.soundTypeGlass);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
		setBlockName("warpdrive.hull" + tier + ".glass.");
		setBlockTextureName("warpdrive:hull/glass");
		setLightLevel(10.0F / 15.0F);
	}
	
	@Override
	public int getMobilityFlag() {
		return 2;
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public int getRenderBlockPass() {
		return 1;
	}
	
	@Override
	public byte getTier(ItemStack itemStack) {
		return tier;
	}
	
	@Override
	public EnumRarity getRarity(final ItemStack itemStack, final EnumRarity rarity) {
		switch (getTier(itemStack)) {
		case 0:	return EnumRarity.epic;
		case 1:	return EnumRarity.common;
		case 2:	return EnumRarity.uncommon;
		case 3:	return EnumRarity.rare;
		default: return rarity;
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		final Block blockSide = blockAccess.getBlock(x, y, z);
		if (blockSide.isAir(blockAccess, x, y, z)) {
			return true;
		}
		final ForgeDirection direction = ForgeDirection.getOrientation(side).getOpposite();
		if (blockSide instanceof BlockGlass || blockSide instanceof BlockHullGlass) {
			return blockAccess.getBlockMetadata(x, y, z)
				!= blockAccess.getBlockMetadata(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ);
		}
		return !blockAccess.isSideSolid(x, y, z, direction, false);
	}
	
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
	
	@Override
	public float getBlockHardness(World world, int x, int y, int z, DamageSource damageSource, int damageParameter, Vector3 damageDirection, int damageLevel) {
		// TODO: adjust hardness to damage type/color
		return WarpDriveConfig.HULL_HARDNESS[tier - 1];
	}
	
	@Override
	public int applyDamage(World world, int x, int y, int z, DamageSource damageSource, int damageParameter, Vector3 damageDirection, int damageLevel) {
		if (damageLevel <= 0) {
			return 0;
		}
		if (tier == 1) {
			world.setBlockToAir(x, y, z);
		} else {
			int metadata = world.getBlockMetadata(x, y, z);
			world.setBlock(x, y, z, WarpDrive.blockHulls_glass[tier - 2], metadata, 2);
		}
		return 0;
	}
}

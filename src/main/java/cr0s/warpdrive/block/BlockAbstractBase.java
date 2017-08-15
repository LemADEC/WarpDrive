package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

public abstract class BlockAbstractBase extends Block implements IBlockBase {
	
	protected boolean isRotating = false;
	
	protected BlockAbstractBase(Material material) {
		super(material);
		setHardness(5.0F);
		setResistance(6.0F * 5 / 3);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
	}
	
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityLiving, ItemStack itemStack) {
		super.onBlockPlacedBy(world, x, y, z, entityLiving, itemStack);
		if (isRotating) {
			final int metadata = Commons.getFacingFromEntity(entityLiving);
			if (metadata >= 0 && metadata <= 15) {
				world.setBlockMetadataWithNotify(x, y, z, metadata, 3);
			}
		}
	}
	
	@Override
	public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
		if (isRotating) {
			world.setBlockMetadataWithNotify(x, y, z, axis.ordinal(), 3);
			return true;
		}
		return false;
	}
	
	@Override
	public byte getTier(final ItemStack itemStack) {
		return 1;
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
}

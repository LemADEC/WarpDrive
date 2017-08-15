package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IBlockUpdateDetector;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.ForgeDirection;

@Optional.InterfaceList({
	@Optional.Interface(iface = "defense.api.IEMPBlock", modid = "DefenseTech"),
	@Optional.Interface(iface = "resonant.api.explosion.IEMPBlock", modid = "icbmclassic")
})
public abstract class BlockAbstractContainer extends BlockContainer implements IBlockBase, defense.api.IEMPBlock, resonant.api.explosion.IEMPBlock {
	
	protected boolean isRotating = false;
	protected boolean hasSubBlocks = false;
	
	protected BlockAbstractContainer(Material material) {
		super(material);
		setHardness(5.0F);
		setResistance(6.0F * 5 / 3);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
	}
	
	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).updatedNeighbours();
		}
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
		
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (itemStack.hasTagCompound()) {
			NBTTagCompound nbtTagCompound = (NBTTagCompound)itemStack.getTagCompound().copy();
			nbtTagCompound.setInteger("x", x);
			nbtTagCompound.setInteger("y", y);
			nbtTagCompound.setInteger("z", z);
			tileEntity.readFromNBT(nbtTagCompound);
			world.markBlockForUpdate(x, y, z);
		}
	}
	
	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		return willHarvest || super.removedByPlayer(world, player, x, y, z, false);
	}
	
	@Override
	protected void dropBlockAsItem(World world, int x, int y, int z, ItemStack itemStack) {
		if (itemStack.getItem() == Item.getItemFromBlock(this)) {
			itemStack.setItemDamage(getDamageValue(world, x, y, z));
			TileEntity tileEntity = world.getTileEntity(x, y, z);
			if (tileEntity == null) {
				WarpDrive.logger.error("Missing tile entity for " + this + " at " + world + " " + x + " " + y + " " + z);
			} else if (tileEntity instanceof TileEntityAbstractBase) {
				NBTTagCompound nbtTagCompound = new NBTTagCompound();
				((TileEntityAbstractBase) tileEntity).writeItemDropNBT(nbtTagCompound);
				itemStack.setTagCompound(nbtTagCompound);
			}
		}
		world.setBlockToAir(x, y, z);
		super.dropBlockAsItem(world, x, y, z, itemStack);
	}
	
	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer entityPlayer) {
		ItemStack itemStack = super.getPickBlock(target, world, x, y, z, entityPlayer);
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		NBTTagCompound nbtTagCompound = new NBTTagCompound();
		if (tileEntity instanceof TileEntityAbstractBase) {
			((TileEntityAbstractBase) tileEntity).writeItemDropNBT(nbtTagCompound);
			itemStack.setTagCompound(nbtTagCompound);
		}
		return itemStack;
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
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
		super.onNeighborBlockChange(world, x, y, z, block);
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).updatedNeighbours();
		}
	}
	
	@Override
	@Optional.Method(modid = "DefenseTech")
	public void onEMP(World world, int x, int y, int z, defense.api.IExplosion explosiveEMP) {
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info(String.format("EMP received @ DIM%d (%d %d %d) from %s with energy %d and radius %.1f",
			                                    world.provider.dimensionId, x, y, z,
			                                    explosiveEMP, explosiveEMP.getEnergy(), explosiveEMP.getRadius()));
		}
		// EMP tower = 3k Energy, 60 radius
		// EMP explosive = 3k Energy, 50 radius
		if (explosiveEMP.getRadius() == 60.0F) {// compensate tower stacking effect
			onEMP(world, x, y, z, 0.02F);
		} else if (explosiveEMP.getRadius() == 50.0F) {
			onEMP(world, x, y, z, 0.70F);
		} else {
			WarpDrive.logger.warn(String.format("EMP received @ DIM%d (%d %d %d) from %s with energy %d and unsupported radius %.1f",
			                                    world.provider.dimensionId, x, y, z,
			                                    explosiveEMP, explosiveEMP.getEnergy(), explosiveEMP.getRadius()));
			onEMP(world, x, y, z, 0.02F);
		}
	}
	
	@Override
	@Optional.Method(modid = "icbmclassic")
	public void onEMP(World world, int x, int y, int z, resonant.api.explosion.IExplosion explosiveEMP) {
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info(String.format("EMP received @ DIM%d (%d %d %d) from %s with energy %d and radius %.1f",
			                                    world.provider.dimensionId, x, y, z,
			                                    explosiveEMP, explosiveEMP.getEnergy(), explosiveEMP.getRadius()));
		}
		// EMP tower = 3k Energy, 60 radius
		// EMP explosive = 3k Energy, 50 radius
		if (explosiveEMP.getRadius() == 60.0F) {// compensate tower stacking effect
			onEMP(world, x, y, z, 0.02F);
		} else if (explosiveEMP.getRadius() == 50.0F) {
			onEMP(world, x, y, z, 0.70F);
		} else {
			WarpDrive.logger.warn(String.format("EMP received @ DIM%d (%d %d %d) from %s with energy %d and unsupported radius %.1f",
			                                    world.provider.dimensionId, x, y, z,
			                                    explosiveEMP, explosiveEMP.getEnergy(), explosiveEMP.getRadius()));
			onEMP(world, x, y, z, 0.02F);
		}
	}
	
	public void onEMP(World world, final int x, final int y, final int z, final float efficiency) {
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityAbstractEnergy) {
			TileEntityAbstractEnergy tileEntityAbstractEnergy = (TileEntityAbstractEnergy) tileEntity;
			if (tileEntityAbstractEnergy.energy_getMaxStorage() > 0) {
				tileEntityAbstractEnergy.energy_consume(Math.round(tileEntityAbstractEnergy.energy_getEnergyStored() * efficiency), false);
			}
		}
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

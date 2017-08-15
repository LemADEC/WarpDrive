package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IAirContainerItem;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockAirGenerator extends BlockAbstractContainer {
	
	@SideOnly(Side.CLIENT)
	private IIcon[] iconBuffer;
	
	private static final int ICON_INACTIVE_SIDE = 0;
	private static final int ICON_BOTTOM = 1;
	private static final int ICON_SIDE_ACTIVATED = 2;
	
	public BlockAirGenerator() {
		super(Material.iron);
		setBlockName("warpdrive.breathing.air_generator");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		iconBuffer = new IIcon[3];
		iconBuffer[ICON_INACTIVE_SIDE] = iconRegister.registerIcon("warpdrive:breathing/air_generator-side_inactive");
		iconBuffer[ICON_BOTTOM] = iconRegister.registerIcon("warpdrive:breathing/air_generator-connection");
		iconBuffer[ICON_SIDE_ACTIVATED] = iconRegister.registerIcon("warpdrive:breathing/air_generator-side_active");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(IBlockAccess blockAccess, int x, int y, int z, int side) {
		final int metadata = blockAccess.getBlockMetadata(x, y, z);
		/*
		if (side == 0) {
			return iconBuffer[ICON_BOTTOM];
		} else if (side == 1) {
			if (metadata == 0) {
				return iconBuffer[ICON_INACTIVE_SIDE];
			} else {
				return iconBuffer[ICON_SIDE_ACTIVATED];
			}
		}
		/**/
		
		if (metadata == 0) { // Inactive state
			return iconBuffer[ICON_INACTIVE_SIDE];
		} else if (metadata == 1) {
			return iconBuffer[ICON_SIDE_ACTIVATED];
		}
		
		return null;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int metadata) {
		return iconBuffer[ICON_SIDE_ACTIVATED];
	}
	
	@Override
	public TileEntity createNewTileEntity(World var1, int i) {
		return new TileEntityAirGenerator();
	}
	
	@Override
	public int quantityDropped(Random par1Random) {
		return 1;
	}
	
	@Override
	public Item getItemDropped(int par1, Random par2Random, int par3) {
		return Item.getItemFromBlock(this);
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return false;
		}
		
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityAirGenerator) {
			TileEntityAirGenerator airGenerator = (TileEntityAirGenerator)tileEntity;
			ItemStack itemStackHeld = entityPlayer.getHeldItem();
			if (itemStackHeld == null) {
				Commons.addChatMessage(entityPlayer, airGenerator.getStatus());
				return true;
			} else {
				Item itemHeld = itemStackHeld.getItem();
				if (itemHeld instanceof IAirContainerItem) {
					IAirContainerItem airCanister = (IAirContainerItem) itemHeld;
					if (airCanister.canContainAir(itemStackHeld) && airGenerator.energy_consume(WarpDriveConfig.BREATHING_ENERGY_PER_CANISTER, true)) {
						entityPlayer.inventory.decrStackSize(entityPlayer.inventory.currentItem, 1);
						ItemStack toAdd = airCanister.getFullAirContainer(itemStackHeld);
						if (toAdd != null) {
							if (!entityPlayer.inventory.addItemStackToInventory(toAdd)) {
								EntityItem entityItem = new EntityItem(entityPlayer.worldObj, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, toAdd);
								entityPlayer.worldObj.spawnEntityInWorld(entityItem);
							}
							((EntityPlayerMP)entityPlayer).sendContainerToPlayer(entityPlayer.inventoryContainer);
							airGenerator.energy_consume(WarpDriveConfig.BREATHING_ENERGY_PER_CANISTER, false);
						}
					}
				}
			}
		}
		
		return false;
	}
}

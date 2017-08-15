package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockForceFieldRelay extends BlockAbstractForceField {
	
	@SideOnly(Side.CLIENT)
	private IIcon[] icons;
	
	public BlockForceFieldRelay(final byte tier) {
		super(tier, Material.iron);
		isRotating = false;
		setBlockName("warpdrive.forcefield.relay" + tier);
		setBlockTextureName("warpdrive:forcefield/relay");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		icons = new IIcon[EnumForceFieldUpgrade.length + 1];
		
		for (EnumForceFieldUpgrade enumForceFieldUpgrade : EnumForceFieldUpgrade.values()) {
			if (enumForceFieldUpgrade.maxCountOnRelay > 0) {
				icons[enumForceFieldUpgrade.ordinal()] = iconRegister.registerIcon("warpdrive:forcefield/relay" + "_" + enumForceFieldUpgrade.unlocalizedName);
			} else {
				icons[enumForceFieldUpgrade.ordinal()] = iconRegister.registerIcon("warpdrive:forcefield/relay" + "_" + EnumForceFieldUpgrade.NONE.unlocalizedName);
			}
		}
		icons[EnumForceFieldUpgrade.length] = iconRegister.registerIcon("warpdrive:forcefield/relay_top");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(IBlockAccess blockAccess, int x, int y, int z, int side) {
		final TileEntity tileEntity = blockAccess.getTileEntity(x, y, z);
		if (tileEntity == null || !(tileEntity instanceof TileEntityForceFieldRelay)) {
			return icons[0];
		}
		
		if (side == 0 || side == 1) {
			return icons[EnumForceFieldUpgrade.length];
		}
		if (((TileEntityForceFieldRelay) tileEntity).isConnected){
			return icons[((TileEntityForceFieldRelay) tileEntity).getUpgrade().ordinal()];
		}
		return icons[EnumForceFieldUpgrade.NONE.ordinal()];
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int damage) {
		if (side == 0 || side == 1) {
			return icons[EnumForceFieldUpgrade.length];
		}
		if (damage >= 0 && damage < EnumForceFieldUpgrade.length) {
			return icons[damage];
		}
		return icons[0];
	}
	
	@Override
	public int damageDropped(int damage) {
		return damage;
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return false;
		}
		
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityForceFieldRelay)) {
			return false;
		}
		TileEntityForceFieldRelay tileEntityForceFieldRelay = (TileEntityForceFieldRelay) tileEntity;
		ItemStack itemStackHeld = entityPlayer.getHeldItem();
		
		// sneaking with an empty hand or an upgrade item in hand to dismount current upgrade
		if (entityPlayer.isSneaking()) {
			EnumForceFieldUpgrade enumForceFieldUpgrade = tileEntityForceFieldRelay.getUpgrade();
			if (enumForceFieldUpgrade != EnumForceFieldUpgrade.NONE) {
				if (!entityPlayer.capabilities.isCreativeMode) {
					// dismount the upgrade item
					ItemStack itemStackDrop = ItemForceFieldUpgrade.getItemStackNoCache(enumForceFieldUpgrade, 1);
					EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
					entityItem.delayBeforeCanPickup = 0;
					world.spawnEntityInWorld(entityItem);
				}
				
				tileEntityForceFieldRelay.setUpgrade(EnumForceFieldUpgrade.NONE);
				// upgrade dismounted
				Commons.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.upgrade.result.dismounted", enumForceFieldUpgrade.name()));
				
			} else {
				// no more upgrades to dismount
				Commons.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.upgrade.result.noUpgradeToDismount"));
				return true;
			}
			
		} else if (itemStackHeld == null) {// no sneaking and no item in hand to show status
			Commons.addChatMessage(entityPlayer, tileEntityForceFieldRelay.getStatus());
			return true;
			
		} else if (itemStackHeld.getItem() instanceof ItemForceFieldUpgrade) {
			// validate type
			if (EnumForceFieldUpgrade.get(itemStackHeld.getItemDamage()).maxCountOnRelay <= 0) {
				// invalid upgrade type
				Commons.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.upgrade.result.invalidRelayUpgrade"));
				return true;
			}
			
			if (!entityPlayer.capabilities.isCreativeMode) {
				// validate quantity
				if (itemStackHeld.stackSize < 1) {
					// not enough upgrade items
					Commons.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.upgrade.result.notEnoughUpgrades"));
					return true;
				}
				
				// update player inventory
				itemStackHeld.stackSize -= 1;
				
				// dismount the current upgrade item
				if (tileEntityForceFieldRelay.getUpgrade() != EnumForceFieldUpgrade.NONE) {
					ItemStack itemStackDrop = ItemForceFieldUpgrade.getItemStackNoCache(tileEntityForceFieldRelay.getUpgrade(), 1);
					EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
					entityItem.delayBeforeCanPickup = 0;
					world.spawnEntityInWorld(entityItem);
				}
			}
			
			// mount the new upgrade item
			EnumForceFieldUpgrade enumForceFieldUpgrade = EnumForceFieldUpgrade.get(itemStackHeld.getItemDamage());
			tileEntityForceFieldRelay.setUpgrade(enumForceFieldUpgrade);
			// upgrade mounted
			Commons.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.upgrade.result.mounted", enumForceFieldUpgrade.name()));
		}
		
		return false;
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileEntityForceFieldRelay();
	}
}

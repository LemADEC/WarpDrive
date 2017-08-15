package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.BlockAbstractContainer;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockWeaponController extends BlockAbstractContainer {
	
	@SideOnly(Side.CLIENT)
	private IIcon[] iconBuffer;
	
	private static final int ICON_TOP = 0;
	private static final int ICON_BOTTOM = 1;
	private static final int ICON_SIDE = 2;
	
	public BlockWeaponController() {
		super(Material.iron);
		setHardness(50.0F);
		setResistance(20.0F * 5 / 3);
		setBlockName("warpdrive.weapon.WeaponController");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		iconBuffer = new IIcon[3];
		iconBuffer[ICON_TOP] = iconRegister.registerIcon("warpdrive:movement/ship_controller-top");
		iconBuffer[ICON_BOTTOM] = iconRegister.registerIcon("warpdrive:movement/ship_controller-bottom");
		iconBuffer[ICON_SIDE] = iconRegister.registerIcon("warpdrive:weapon/weapon_controller-side");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int metadata) {
		if (side == 0) {
			return iconBuffer[ICON_BOTTOM];
		} else if (side == 1) {
			return iconBuffer[ICON_TOP];
		}
		return iconBuffer[ICON_SIDE];
	}
	
	@Override
	public TileEntity createNewTileEntity(World parWorld, int i) {
		return new TileEntityWeaponController();
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
		
		if (entityPlayer.getHeldItem() == null) {
			TileEntity tileEntity = world.getTileEntity(x, y, z);
			if (tileEntity instanceof TileEntityWeaponController) {
				Commons.addChatMessage(entityPlayer, ((TileEntityWeaponController) tileEntity).getStatus());
			} else {
				Commons.addChatMessage(entityPlayer, StatCollector.translateToLocalFormatted("warpdrive.guide.prefix",
						getLocalizedName()) + StatCollector.translateToLocalFormatted("warpdrive.error.badTileEntity"));
				WarpDrive.logger.error("Block " + this + " with invalid tile entity " + tileEntity);
			}
			return true;
		}
		
		return false;
	}
}
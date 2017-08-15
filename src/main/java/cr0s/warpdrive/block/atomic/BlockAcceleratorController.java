package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.block.BlockAbstractContainer;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockAcceleratorController extends BlockAbstractContainer {
	
	@SideOnly(Side.CLIENT)
	private IIcon[] icons;
	
	public BlockAcceleratorController() {
		super(Material.iron);
		isRotating = true;
		setBlockName("warpdrive.atomic.accelerator_controller");
		setBlockTextureName("warpdrive:atomic/accelerator_controller");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		icons = new IIcon[2];
		
		icons[0] = iconRegister.registerIcon(getTextureName() + "-off");
		icons[1] = iconRegister.registerIcon(getTextureName() + "-on");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int metadata) {
		return icons[metadata % 2];
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return false;
		}
		
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAcceleratorController)) {
			return false;
		}
		TileEntityAcceleratorController tileEntityAcceleratorController = (TileEntityAcceleratorController) tileEntity;
		ItemStack itemStackHeld = entityPlayer.getHeldItem();
		
		if (itemStackHeld == null) {// no sneaking and no item in hand => show status
			Commons.addChatMessage(entityPlayer, tileEntityAcceleratorController.getStatus());
			return true;
			
		}
		
		return false;
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileEntityAcceleratorController();
	}
}

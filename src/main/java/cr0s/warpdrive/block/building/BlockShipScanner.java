package cr0s.warpdrive.block.building;

import java.util.Random;

import cr0s.warpdrive.Commons;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.render.RenderBlockShipScanner;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockShipScanner extends BlockAbstractContainer {
	
	@SideOnly(Side.CLIENT)
	private IIcon[] iconBuffer;
	private static final int ICON_BOTTOM = 0;
	private static final int ICON_TOP = 1;
	private static final int ICON_SIDE = 2;
	private static final int ICON_BORDER = 3;
	
	public static int passCurrent;
	
	public BlockShipScanner() {
		super(Material.iron);
		setBlockName("warpdrive.building.ShipScanner");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		iconBuffer = new IIcon[16];
		iconBuffer[ICON_BOTTOM  ] = iconRegister.registerIcon("warpdrive:building/shipScannerBottom");
		iconBuffer[ICON_TOP     ] = iconRegister.registerIcon("warpdrive:building/shipScannerTop");
		iconBuffer[ICON_SIDE    ] = iconRegister.registerIcon("warpdrive:building/shipScannerSide");
		iconBuffer[ICON_BORDER  ] = iconRegister.registerIcon("warpdrive:building/shipScanner-border");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int metadata) {
		if (side == 0) {
			return iconBuffer[ICON_BOTTOM];
		}
		if (side == 1) {
			return iconBuffer[ICON_TOP];
		}
		
		return iconBuffer[ICON_SIDE];
	}
	
	@SideOnly(Side.CLIENT)
	public IIcon getBorderIcon() {
		return iconBuffer[ICON_BORDER];
	}
	
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
	
	@Override
	public int getRenderType() {
		return RenderBlockShipScanner.renderId;
	}
	
	@Override
	public int getRenderBlockPass() {
		return 1;
	}
	
	@Override
	public boolean canRenderInPass(final int pass) {
		passCurrent = pass;
		return pass == 0 || pass == 1;
	}
	
	@Override
	public int colorMultiplier(IBlockAccess blockAccess, int x, int y, int z) {
		final TileEntity tileEntity = blockAccess.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityShipScanner && ((TileEntityShipScanner) tileEntity).blockCamouflage != null) {
			return ((TileEntityShipScanner) tileEntity).colorMultiplierCamouflage;
		}
		
		return super.colorMultiplier(blockAccess, x, y, z);
	}
	
	@Override
	public int getLightValue(IBlockAccess blockAccess, int x, int y, int z) {
		final TileEntity tileEntity = blockAccess.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityShipScanner) {
			return ((TileEntityShipScanner) tileEntity).lightCamouflage;
		}
		
		return 0;
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
		
	@Override
	public TileEntity createNewTileEntity(World var1, int i) {
		return new TileEntityShipScanner();
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
			final TileEntity tileEntity = world.getTileEntity(x, y, z);
			if (tileEntity instanceof TileEntityShipScanner) {
				final Block blockAbove = world.getBlock(x, y + 2, z);
				if ( blockAbove.isAir(world, x, y + 2, z)
				  || !entityPlayer.isSneaking() ) {
					Commons.addChatMessage(entityPlayer, ((TileEntityShipScanner) tileEntity).getStatus());
					return true;
				} else if (blockAbove != this) {
					((TileEntityShipScanner) tileEntity).blockCamouflage = blockAbove;
					((TileEntityShipScanner) tileEntity).metadataCamouflage = world.getBlockMetadata(x, y + 2, z);
					((TileEntityShipScanner) tileEntity).colorMultiplierCamouflage = 0x808080; // blockAbove.colorMultiplier(world, x, y + 2, z);
					((TileEntityShipScanner) tileEntity).lightCamouflage = blockAbove.getLightValue(world, x, y + 2, z);
					tileEntity.markDirty();
					world.setBlockMetadataWithNotify(x, y, z, ((TileEntityShipScanner) tileEntity).metadataCamouflage, 2);
				} else {
					((TileEntityShipScanner) tileEntity).blockCamouflage = null;
					((TileEntityShipScanner) tileEntity).metadataCamouflage = 0;
					((TileEntityShipScanner) tileEntity).colorMultiplierCamouflage = 0;
					((TileEntityShipScanner) tileEntity).lightCamouflage = 0;
					tileEntity.markDirty();
					world.setBlockMetadataWithNotify(x, y, z, ((TileEntityShipScanner) tileEntity).metadataCamouflage, 2);
				}
			}
		}
		
		return false;
	}
}
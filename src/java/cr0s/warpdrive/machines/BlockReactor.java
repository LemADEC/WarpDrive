package cr0s.warpdrive.machines;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cr0s.warpdrive.WarpDrive;

public class BlockReactor extends BlockContainer {
    private IIcon[] iconBuffer;

    private final int ICON_SIDE_INACTIVE = 0, ICON_BOTTOM = 1, ICON_TOP = 2, ICON_SIDE_ACTIVATED = 3, ICON_SIDE_HEATED = 4;

    public BlockReactor(int texture, Material material) {
        super(material);
        setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        iconBuffer = new IIcon[5];
        iconBuffer[ICON_SIDE_INACTIVE] = par1IconRegister.registerIcon("warpdrive:coreSideInactive");
        iconBuffer[ICON_BOTTOM] = par1IconRegister.registerIcon("warpdrive:coreBottom");
        iconBuffer[ICON_TOP] = par1IconRegister.registerIcon("warpdrive:coreTop");
        iconBuffer[ICON_SIDE_ACTIVATED] = par1IconRegister.registerIcon("warpdrive:coreSideActive");
        iconBuffer[ICON_SIDE_HEATED] = par1IconRegister.registerIcon("warpdrive:coreSideHeated");
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        if (side == 0) {
            return iconBuffer[ICON_BOTTOM];
        } else if (side == 1) {
            return iconBuffer[ICON_TOP];
        }

        if (metadata == 0) { // Inactive state
            return iconBuffer[ICON_SIDE_INACTIVE];
        } else if (metadata == 1) { // Activated state
            return iconBuffer[ICON_SIDE_ACTIVATED];
        } else if (metadata == 2) { // Heated state
            return iconBuffer[ICON_SIDE_HEATED];
        } 

        return null;
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int i) {
        return new TileEntityReactor();
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    @Override
    public int quantityDropped(Random par1Random) {
        return 1;
    }

    /**
     * Returns the ID of the items to drop on destruction.
     */
    @Override
    public Item getItemDropped(int par1, Random par2Random, int par3) {
        return Item.getItemFromBlock(this);
    }

    /**
     * Called upon block activation (right click on the block.)
     */
    @Override
    public boolean onBlockActivated(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9) {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            return false;
        }

        if (par5EntityPlayer.getHeldItem() == null) {
        	TileEntity te = par1World.getTileEntity(par2, par3, par4);
        	if (te != null && te instanceof TileEntityReactor) {
                par5EntityPlayer.addChatMessage(new ChatComponentText(((TileEntityReactor)te).getStatus()));
                return true;
        	}
        }

        return false;
    }

    @Override
    public void breakBlock(World par1World, int par2, int par3, int par4, Block par5, int par6) {
        TileEntity te = par1World.getTileEntity(par2, par3, par4);
        if (te != null && te instanceof TileEntityReactor) {
            WarpDrive.warpCores.removeFromRegistry((TileEntityReactor)te);
        }

        super.breakBlock(par1World, par2, par3, par4, par5, par6);
    }
}
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

public class BlockLift extends BlockContainer
{
    private IIcon[] iconBuffer;

    public BlockLift(int texture, Material material) {
        super(material);
        setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        iconBuffer = new IIcon[6];
        iconBuffer[0] = par1IconRegister.registerIcon("warpdrive:liftSideOffline");
        iconBuffer[1] = par1IconRegister.registerIcon("warpdrive:liftSideUp");
        iconBuffer[2] = par1IconRegister.registerIcon("warpdrive:liftSideDown");
        iconBuffer[3] = par1IconRegister.registerIcon("warpdrive:liftUpInactive");
        iconBuffer[4] = par1IconRegister.registerIcon("warpdrive:liftUpOut");
        iconBuffer[5] = par1IconRegister.registerIcon("warpdrive:liftUpIn");
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
    	if (metadata > 2) {
    		return iconBuffer[0];
    	}
        if (side == 1) {
            return iconBuffer[3 + metadata];
        } else if (side == 0) {
            if (metadata == 0) {
                return iconBuffer[3];
            } else {
                return iconBuffer[6 - metadata];
            }
        }

        return iconBuffer[metadata];
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int i) {
        return new TileEntityLift();
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

        WarpEnergyTE te = (WarpEnergyTE)par1World.getTileEntity(par2, par3, par4);
        if (te != null && (par5EntityPlayer.getHeldItem() == null)) {
        	par5EntityPlayer.addChatMessage(new ChatComponentText(te.getStatus()));
            return true;
        }

        return false;
    }
}
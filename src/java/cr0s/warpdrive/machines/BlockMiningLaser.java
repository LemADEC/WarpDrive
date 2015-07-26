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

public class BlockMiningLaser extends BlockContainer {
    private IIcon[] iconBuffer;
    private final static int ICON_TOP = 5;
    public final static int ICON_IDLE = 0;
    public final static int ICON_MININGLOWPOWER = 1;
    public final static int ICON_MININGPOWERED = 2;
    public final static int ICON_SCANNINGLOWPOWER = 3;
    public final static int ICON_SCANNINGPOWERED = 4;

    public BlockMiningLaser(int texture, Material material) {
        super(material);
        setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        iconBuffer = new IIcon[16];
        // Solid textures
        iconBuffer[ICON_TOP             ] = par1IconRegister.registerIcon("warpdrive:particleBoosterTopBottom");
        iconBuffer[ICON_IDLE            ] = par1IconRegister.registerIcon("warpdrive:miningLaser_idle");
        iconBuffer[ICON_MININGLOWPOWER  ] = par1IconRegister.registerIcon("warpdrive:miningLaser_miningLowPower");
        iconBuffer[ICON_MININGPOWERED   ] = par1IconRegister.registerIcon("warpdrive:miningLaser_miningPowered");
        iconBuffer[ICON_SCANNINGLOWPOWER] = par1IconRegister.registerIcon("warpdrive:miningLaser_scanningLowPower");
        iconBuffer[ICON_SCANNINGPOWERED ] = par1IconRegister.registerIcon("warpdrive:miningLaser_scanningPowered");
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        if (side == 0 || side == 1) {
            return iconBuffer[ICON_TOP];
        }
        if (metadata < iconBuffer.length) {
        	return iconBuffer[metadata];
        } else {
        	return null;
        }
    }
    
    @Override
    public TileEntity createNewTileEntity(World var1, int i) {
        return new TileEntityMiningLaser();
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

        TileEntityMiningLaser miningLaser = (TileEntityMiningLaser)par1World.getTileEntity(par2, par3, par4);

        if (miningLaser != null && (par5EntityPlayer.getHeldItem() == null)) {
            par5EntityPlayer.addChatMessage(new ChatComponentText(miningLaser.getStatus()));
            return true;
        }

        return false;
    }
}
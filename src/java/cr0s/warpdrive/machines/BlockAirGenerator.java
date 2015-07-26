package cr0s.warpdrive.machines;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;
import cr0s.warpdrive.api.IAirCanister;

public class BlockAirGenerator extends BlockContainer
{
    private IIcon[] iconBuffer;

    private final int ICON_INACTIVE_SIDE = 0, ICON_BOTTOM = 1, ICON_SIDE_ACTIVATED = 2;

    public BlockAirGenerator(int texture, Material material)
    {
        super(material);
        setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister)
    {
        iconBuffer = new IIcon[3];
        iconBuffer[ICON_INACTIVE_SIDE] = par1IconRegister.registerIcon("warpdrive:airgenSideInactive");
        iconBuffer[ICON_BOTTOM] = par1IconRegister.registerIcon("warpdrive:contBottom");
        iconBuffer[ICON_SIDE_ACTIVATED] = par1IconRegister.registerIcon("warpdrive:airgenSideActive");
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        if (side == 0) {
            return iconBuffer[ICON_BOTTOM];
        } else if (side == 1) {
            if (metadata == 0) {
                return iconBuffer[ICON_INACTIVE_SIDE];
            } else {
                return iconBuffer[ICON_SIDE_ACTIVATED];
            }
        }

        if (metadata == 0) { // Inactive state
        	return iconBuffer[ICON_INACTIVE_SIDE];
        } else if (metadata == 1) {
            return iconBuffer[ICON_SIDE_ACTIVATED];
        }

        return null;
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int i) {
        return new TileEntityAirGenerator();
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

    @Override
    public boolean onBlockActivated(World par1World, int par2, int par3, int par4, EntityPlayer player, int par6, float par7, float par8, float par9) {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            return false;
        }
        
        WarpEnergyTE te = (WarpEnergyTE)par1World.getTileEntity(par2, par3, par4);
        if (te != null) {
        	ItemStack heldItemStack = player.getHeldItem();
        	if (heldItemStack == null) {
	        	player.addChatMessage(new ChatComponentText(te.getStatus()));
	            return true;
	        } else { 
	        	Item heldItem = heldItemStack.getItem();
	           	if (heldItem != null && (heldItem instanceof IAirCanister)) {
	           		IAirCanister airCanister = (IAirCanister)heldItem;
	           		if (airCanister.canContainAir(heldItemStack) && te.consumeEnergy(WarpDriveConfig.AG_RF_PER_CANISTER, true)) {
	           			player.inventory.decrStackSize(player.inventory.currentItem, 1);
	           			ItemStack toAdd = airCanister.fullDrop(heldItemStack);
	           			if (toAdd != null) {
	           				if (!player.inventory.addItemStackToInventory(toAdd)) {
	           					EntityItem ie = new EntityItem(player.worldObj, player.posX, player.posY, player.posZ, toAdd);
	           					player.worldObj.spawnEntityInWorld(ie);
	           				}
	           				te.consumeEnergy(WarpDriveConfig.AG_RF_PER_CANISTER, false);
	           			}
	           		}
	           	}
        	}
        }
        
        return false;
    }
}

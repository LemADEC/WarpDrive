package cr0s.warpdrive.machines;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import cr0s.warpdrive.WarpDrive;

public class BlockCloakingCoil extends Block {
    private IIcon[] iconBuffer;

    public BlockCloakingCoil(int texture, Material material) {
        super(material);
        setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        iconBuffer = new IIcon[3];
        iconBuffer[0] = par1IconRegister.registerIcon("warpdrive:cloakCoilSide");
        iconBuffer[1] = par1IconRegister.registerIcon("warpdrive:cloakCoilSideActive");
        iconBuffer[2] = par1IconRegister.registerIcon("warpdrive:cloakCoilTop");
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        if (side == 0) {
            return iconBuffer[2];
        } else if (side == 1) {
            return iconBuffer[2];
        }
    	if (metadata == 0) {
    		return iconBuffer[0];
    	} else if (metadata == 1) {
    		return iconBuffer[1];
    	} else {
    		return null;
    	}
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
}

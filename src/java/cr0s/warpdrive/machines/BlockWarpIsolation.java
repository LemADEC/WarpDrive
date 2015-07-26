package cr0s.warpdrive.machines;

import java.util.Random;

import javax.swing.Icon;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import cr0s.warpdrive.WarpDrive;

public class BlockWarpIsolation extends Block {
    private IIcon[] iconBuffer;

    public BlockWarpIsolation(int texture, Material material) {
        super(material);
        setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        iconBuffer = new IIcon[1];
        iconBuffer[0] = par1IconRegister.registerIcon("warpdrive:warpIsolation");
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        return iconBuffer[0];
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
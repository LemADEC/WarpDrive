package cr0s.warpdrive.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cr0s.warpdrive.WarpDrive;

public class BlockIridium extends Block
{
    public BlockIridium()
    {
        super(Material.rock);
        setHardness(0.8F);
		setResistance(150 * 4);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister)
    {
        this.blockIcon = par1IconRegister.registerIcon("warpdrive:iridiumSide");
    }

    @Override
    public Item getItemDropped(int var1, Random var2, int var3)
    {
        return Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(Random par1Random)
    {
        return 1;
    }
}
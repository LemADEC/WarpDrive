package cr0s.warpdrive.machines;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cr0s.warpdrive.WarpDrive;

public class BlockLaser extends BlockContainer {
    private IIcon[] iconBuffer;

    private final int ICON_SIDE = 0;

    public BlockLaser(int texture, Material material) {
        super(material);
        setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        iconBuffer = new IIcon[1];
        // Solid textures
        iconBuffer[ICON_SIDE] = par1IconRegister.registerIcon("warpdrive:laserSide");
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        return iconBuffer[ICON_SIDE];
    }

    @Override
    public TileEntity createNewTileEntity(World parWorld, int i) {
        return new TileEntityLaser();
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
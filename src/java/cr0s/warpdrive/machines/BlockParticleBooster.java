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

public class BlockParticleBooster extends BlockContainer {
    private IIcon[] iconBuffer;

    public BlockParticleBooster(int texture, Material material) {
        super(material);
        setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        iconBuffer = new IIcon[16];
        iconBuffer[ 0] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide0");
        iconBuffer[ 1] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide1");
        iconBuffer[ 2] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide2");
        iconBuffer[ 3] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide3");
        iconBuffer[ 4] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide4");
        iconBuffer[ 5] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide5");
        iconBuffer[ 6] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide6");
        iconBuffer[ 7] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide7");
        iconBuffer[ 8] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide8");
        iconBuffer[ 9] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide9");
        iconBuffer[10] = par1IconRegister.registerIcon("warpdrive:particleBoosterSide10");
        iconBuffer[11] = par1IconRegister.registerIcon("warpdrive:particleBoosterTopBottom");
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        if (side == 0 || side == 1) {
            return iconBuffer[11];
        }

        return iconBuffer[metadata];
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int i) {
        return new TileEntityParticleBooster();
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
			if (te != null && te instanceof WarpEnergyTE) {
				par5EntityPlayer.addChatMessage(new ChatComponentText(((WarpEnergyTE) te).getStatus()));
				return true;
			}
		}
		
		return false;
	}
}
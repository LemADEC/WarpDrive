package cr0s.warpdrive.machines;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CamRegistryItem;
import cr0s.warpdrive.render.ClientCameraUtils;
import cr0s.warpdrive.render.EntityCamera;

public class BlockMonitor extends BlockContainer {
    private IIcon iconFront;
    private IIcon iconBlock;

    public BlockMonitor() {
        super(Material.iron);
        setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
    }

    @Override

    /**
     * From the specified side and block metadata retrieves the blocks texture. Args: side, metadata
     */
    public IIcon getIcon(int side, int parMetadata) {
        int meta = parMetadata & 3;
        return side == 2 ? (meta == 0 ? this.iconFront : this.iconBlock) : (side == 3 ? (meta == 2 ? this.iconFront : this.iconBlock) : (side == 4 ? (meta == 3 ? this.iconFront : this.iconBlock) : (side == 5 ? (meta == 1 ? this.iconFront : this.iconBlock) : this.iconBlock)));
    }

    /**
     * When this method is called, your block should register all the icons it needs with the given IconRegister. This
     * is the only chance you get to register icons.
     */
    @Override
	public void registerBlockIcons(IIconRegister reg) {
        this.iconFront = reg.registerIcon("warpdrive:monitorFront");
        this.iconBlock = reg.registerIcon("warpdrive:monitorSide");
    }

    /**
     * Called when the block is placed in the world.
     */
    @Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityliving, ItemStack itemstack) {
        int dir = Math.round(entityliving.rotationYaw / 90.0F) & 3;
        world.setBlockMetadataWithNotify(x, y, z, dir, 3);
    }

    /**
     * Called upon block activation (right click on the block.)
     */
    @Override
	public boolean onBlockActivated(World par1World, int x, int y, int z, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9) {
    	// Monitor is only reacting client side
    	if (!FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            return false;
        }

        // Get camera frequency
        TileEntity te = par1World.getTileEntity(x, y, z);

        if (te != null && te instanceof TileEntityMonitor && (par5EntityPlayer.getHeldItem() == null)) {
            int frequency = ((TileEntityMonitor)te).getFrequency();
            CamRegistryItem cam = WarpDrive.instance.cams.getCamByFrequency(par1World, frequency);
            if (cam == null) {
            	par5EntityPlayer.addChatMessage(new ChatComponentText(getLocalizedName() + " Frequency '" + frequency + "' is invalid or camera is too far!"));
                return false;
            } else {
            	par5EntityPlayer.addChatMessage(new ChatComponentText(getLocalizedName() + " Frequency '" + frequency + "' is valid. Viewing camera at " + cam.position.chunkPosX + ", " + cam.position.chunkPosY + ", " + cam.position.chunkPosZ));
                // Spawn camera entity
                EntityCamera e = new EntityCamera(par1World, cam.position, par5EntityPlayer);
                par1World.spawnEntityInWorld(e);
                e.setPositionAndUpdate(cam.position.chunkPosX + 0.5D, cam.position.chunkPosY + 0.5D, cam.position.chunkPosZ + 0.5D);
                //e.setPositionAndRotation(camPos.x, camPos.y, camPos.z, entityplayer.rotationYaw, entityplayer.rotationPitch);
                WarpDrive.instance.overlayType = cam.type;
                ClientCameraUtils.setupViewpoint(par5EntityPlayer, e, x, y, z, this, cam.position.chunkPosX, cam.position.chunkPosY, cam.position.chunkPosZ, par1World.getBlock(cam.position.chunkPosX, cam.position.chunkPosY, cam.position.chunkPosZ));
            }
        }

        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int i) {
        return new TileEntityMonitor();
    }
}
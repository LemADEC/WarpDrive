package cr0s.warpdrive.machines;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cr0s.warpdrive.WarpDrive;

public class BlockLaserReactorMonitor extends BlockContainer {
	public BlockLaserReactorMonitor(int texture, Material material) {
		super(material);
		setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
	}

	public BlockLaserReactorMonitor(Material material) {
		this(0, material);
	}

	@Override
	public void registerBlockIcons(IIconRegister par1IconRegister) {
		// Solid textures
		blockIcon = par1IconRegister.registerIcon("warpdrive:reactorMonitor");
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int i) {
		return new TileEntityLaserReactorMonitor();
	}
}

package cr0s.warpdrive.machines;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockTransporter extends WarpBlockContainer {

	private IIcon[] iconBuffer;

	public BlockTransporter(Material par2Material) {
		super(par2Material);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int i) {
		return new TileEntityTransporter();
	}

	@Override
	public void registerBlockIcons(IIconRegister par1IconRegister) {
		iconBuffer = new IIcon[3];
		// Solid textures
		iconBuffer[0] = par1IconRegister.registerIcon("warpdrive:transporterBottom");
		iconBuffer[1] = par1IconRegister.registerIcon("warpdrive:transporterTop");
		iconBuffer[2] = par1IconRegister.registerIcon("warpdrive:transporterSide");
	}

	@Override
	public IIcon getIcon(int side, int metadata) {
		if (side == 0 || side == 1) {
			return iconBuffer[side];
		}

		return iconBuffer[2];
	}
}
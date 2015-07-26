package cr0s.warpdrive.machines;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockChunkLoader extends WarpBlockContainer {
	IIcon iconBuffer;

	public BlockChunkLoader() {
	}

	@Override
	public TileEntity createNewTileEntity(World world, int i) {
		return new TileEntityChunkLoader();
	}

	@Override
	public void registerBlockIcons(IIconRegister ir) {
		iconBuffer = ir.registerIcon("warpdrive:chunkLoader");
	}

	@Override
	public IIcon getIcon(int side, int damage) {
		return iconBuffer;
	}

}

package cr0s.warpdrive.block.atomic;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockVoidShellGlass extends BlockVoidShellPlain {
	
	public BlockVoidShellGlass() {
		super();
		setBlockName("warpdrive.atomic.void_shell_glass");
		setBlockTextureName("warpdrive:atomic/void_shell_glass");
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public int getRenderBlockPass() {
		return 1;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		final Block blockSide = blockAccess.getBlock(x, y, z);
		if (blockSide.isAir(blockAccess, x, y, z)) {
			return true;
		}
		return !(blockSide instanceof BlockElectromagnetGlass);
	}
}

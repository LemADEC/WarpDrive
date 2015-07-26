package cr0s.warpdrive.machines;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockPowerLaser extends WarpBlockContainer {
	static IIcon[] iconBuffer = new IIcon[16];

	public BlockPowerLaser() {
		setResistance(100.0F);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int i) {
		return new TileEntityPowerLaser();
	}

	private static boolean isActive(int side, int meta) {
		if (side == 3 && meta == 1) {
			return true;
		}

		if (side == 2 && meta == 2) {
			return true;
		}

		if (side == 4 && meta == 4) {
			return true;
		}

		if (side == 5 && meta == 3) {
			return true;
		}
		return false;
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		if (side == 0 || side == 1) {
			return iconBuffer[0];
		}

		if (isActive(side, meta)) {
			return iconBuffer[2];
		}

		return iconBuffer[1];
	}

	@Override
	public void registerBlockIcons(IIconRegister par1IconRegister) {
		iconBuffer[0] = par1IconRegister.registerIcon("warpdrive:powerLaserTopBottom");
		iconBuffer[1] = par1IconRegister.registerIcon("warpdrive:powerLaserSides");
		iconBuffer[2] = par1IconRegister.registerIcon("warpdrive:powerLaserActive");
	}
}
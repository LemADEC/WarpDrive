package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.WarpDrive;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockCloakingCoil extends Block {
	
	// Metadata values
	// 0 = not linked
	// 1 = inner coil passive
	// 2-7 = outer coil passive
	// 8 = (not used)
	// 9 = inner coil active
	// 10-15 = outer coil active
	
	@SideOnly(Side.CLIENT)
	private IIcon[] iconBuffer;
	
	public BlockCloakingCoil() {
		super(Material.iron);
		setHardness(3.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
		setBlockName("warpdrive.detection.CloakingCoil");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister) {
		iconBuffer = new IIcon[4];
		iconBuffer[0] = iconRegister.registerIcon("warpdrive:detection/cloaking_coil-channeling_inactive");
		iconBuffer[1] = iconRegister.registerIcon("warpdrive:detection/cloaking_coil-projecting_inactive");
		iconBuffer[2] = iconRegister.registerIcon("warpdrive:detection/cloaking_coil-channeling_active");
		iconBuffer[3] = iconRegister.registerIcon("warpdrive:detection/cloaking_coil-projecting_active");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(final IBlockAccess blockAccess, final int x, final int y, final int z, final int side) {
		final int metadata = blockAccess.getBlockMetadata(x, y, z);
		
		// not linked or in inventory
		if (metadata == 0) {
			return iconBuffer[1];
		}
		
		// inner coils
		if (metadata == 1) {
			return iconBuffer[0];
		} else if (metadata == 9) {
			return iconBuffer[2];
		}
		
		// outer coils
		final int direction = (metadata & 7) - 2;
		final int activeOffset = (metadata < 8) ? 0 : 2; 
		if (ForgeDirection.OPPOSITES[direction] == side) {
			return iconBuffer[    activeOffset];
		} else {
			return iconBuffer[1 + activeOffset];
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(final int side, final int metadata) {
		if (side == 3) {
			return iconBuffer[0];
		} else {
			return iconBuffer[1];
		}
	}
	
	@Override
	public int quantityDropped(final Random random) {
		return 1;
	}
}

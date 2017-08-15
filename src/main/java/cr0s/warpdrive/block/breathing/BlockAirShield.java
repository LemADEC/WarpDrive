package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.block.BlockAbstractOmnipanel;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockAirShield extends BlockAbstractOmnipanel {
	
	public BlockAirShield() {
		super(Material.cloth);
		setBlockName("warpdrive.breathing.air_shield");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(final IIconRegister iconRegister) {
		blockIcon = iconRegister.registerIcon("warpdrive:breathing/air_shield");
	}
	
	@Override
	public int damageDropped(final int metadata) {
		return metadata;
	}
	
	@Override
	public int getRenderColor(final int metadata) {
		switch (metadata) {
		case 0:
			return 0xFFFFFF;    // white
		case 1:
			return 0xFF5A02;    // orange
		case 2:
			return 0xF269FF;    // magenta
		case 3:
			return 0x80AAFF;    // light blue 
		case 4:
			return 0xFFEE3C;    // yellow
		case 5:
			return 0x90E801;    // lime green
		case 6:
			return 0xFB0680;    // pink
		case 7:
			return 0x2C2C2C;    // gray
		case 8:
			return 0x686868;    // light gray
		case 9:
		default:
			return 0x0FD7FF;    // SciFi cyan
		case 10:
			return 0x5D1072;    // purple
		case 11:
			return 0x4351CC;    // blue
		case 12:
			return 0x99572E;    // brown
		case 13:
			return 0x75993C;    // green
		case 14:
			return 0xCC4d41;    // red
		case 15:
			return 0x080808;    // black
		}
	}
	
	@Override
	public int colorMultiplier(final IBlockAccess blockAccess, final int x, final int y, final int z) {
		final int metadata = blockAccess.getBlockMetadata(x, y, z);
		return getRenderColor(metadata);
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(final World world, final int x, final int y, final int z) {
		return null;
	}
	
	@Override
	public boolean canCollideCheck(final int metadata, final boolean hitIfLiquid) {
		return !hitIfLiquid;
	}
}
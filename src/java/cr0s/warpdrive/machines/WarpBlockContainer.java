package cr0s.warpdrive.machines;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockUpdateDetector;

public abstract class WarpBlockContainer extends BlockContainer {
	protected WarpBlockContainer() {
		super(Material.iron);
	}

	protected WarpBlockContainer(Material m) {
		super(m);
		setHardness(0.5F);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.warpdriveTab);
	}

	@Override
	public void onBlockAdded(World w, int x, int y, int z) {
		super.onBlockAdded(w, x, y, z);
		TileEntity te = w.getTileEntity(x, y, z);
		if (te instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) te).updatedNeighbours();
		}
	}

	/*
	 * FIXME untested
	 *
	 * @Override public boolean onBlockActivated(World world, int x, int y, int
	 * z, EntityPlayer player, int par6, float par7, float par8, float par9) {
	 * if (FMLCommonHandler.instance().getEffectiveSide().isClient()) { return
	 * false; }
	 *
	 * boolean hasResponse = false; TileEntity te = world.getBlockTileEntity(x,
	 * y, z); if(te != null && te instanceof IUpgradable) { IUpgradable
	 * upgradable = (IUpgradable)te; ItemStack is =
	 * player.inventory.getCurrentItem(); if(is != null) { Item i =
	 * is.getItem(); if(i instanceof ItemWarpUpgrade) {
	 * if(upgradable.takeUpgrade
	 * (EnumUpgradeTypes.values()[is.getItemDamage()],false)) {
	 * if(!player.capabilities.isCreativeMode)
	 * player.inventory.decrStackSize(player.inventory.currentItem, 1);
	 * player.addChatMessage("Upgrade accepted"); } else {
	 * player.addChatMessage("Upgrade declined"); } hasResponse = true; } } }
	 *
	 * return hasResponse; } /*
	 */

	@Override
	public void onNeighborBlockChange(World w, int x, int y, int z, Block b) {
		super.onNeighborBlockChange(w, x, y, z, b);
		TileEntity te = w.getTileEntity(x, y, z);
		if (te instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) te).updatedNeighbours();
		}
	}
}

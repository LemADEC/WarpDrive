package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.common.util.ForgeDirection;

public abstract class TileEntityAbstractMiner extends TileEntityAbstractLaser {
	// machine type
	protected ForgeDirection laserOutputSide = ForgeDirection.NORTH;
	
	// machine state
	protected boolean		 enableSilktouch = false;
	
	// pre-computation
	protected Vector3        laserOutput = null;
	
	public TileEntityAbstractMiner() {
		super();
	}
	
	@Override
	public void validate() {
		super.validate();
		laserOutput = new Vector3(this).translate(0.5D).translate(laserOutputSide, 0.5D);
	}
	
	protected void stop() {
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(this + " Stop requested");
		}
	}
	
	protected void harvestBlock(VectorI valuable) {
		Block block = worldObj.getBlock(valuable.x, valuable.y, valuable.z);
		if (block == null || block.isAir(worldObj, valuable.x, valuable.y, valuable.z)) {
			return;
		}
		int blockMeta = worldObj.getBlockMetadata(valuable.x, valuable.y, valuable.z);
		if (block instanceof BlockLiquid) {
			// Evaporate fluid
			worldObj.playSoundEffect(valuable.x + 0.5D, valuable.y + 0.5D, valuable.z + 0.5D, "random.fizz", 0.5F,
					2.6F + (worldObj.rand.nextFloat() - worldObj.rand.nextFloat()) * 0.8F);
			
			// remove without updating neighbours @TODO: add proper pump upgrade
			worldObj.setBlock(valuable.x, valuable.y, valuable.z, Blocks.air, 0, 2);
		} else {
			List<ItemStack> itemStacks = getItemStackFromBlock(valuable.x, valuable.y, valuable.z, block, blockMeta);
			if (addToConnectedInventories(itemStacks)) {
				stop();
			}
			// standard harvest block effect
			worldObj.playAuxSFXAtEntity(null, 2001, valuable.x, valuable.y, valuable.z, Block.getIdFromBlock(block) + (blockMeta << 12));
			
			// remove while updating neighbours
			worldObj.setBlock(valuable.x, valuable.y, valuable.z, Blocks.air, 0, 3);
		}
	}
	
	private List<ItemStack> getItemStackFromBlock(int x, int y, int z, Block block, int blockMeta) {
		if (block == null) {
			WarpDrive.logger.error(this + " Invalid block at " + x + " " + y + " " + z);
			return null;
		}
		if (enableSilktouch) {
			boolean isSilkHarvestable = false;
			try {
				isSilkHarvestable = block.canSilkHarvest(worldObj, null, x, y, z, blockMeta);
			} catch (Exception exception) {// protect in case the mined block is corrupted
				exception.printStackTrace();
			}
			if (isSilkHarvestable) {
				ArrayList<ItemStack> isBlock = new ArrayList<>();
				isBlock.add(new ItemStack(block, 1, blockMeta));
				return isBlock;
			}
		}
		
		try {
			return block.getDrops(worldObj, x, y, z, blockMeta, 0);
		} catch (Exception exception) {// protect in case the mined block is corrupted
			exception.printStackTrace();
			return null;
		}
	}
	
	// NBT DATA
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		enableSilktouch = tag.getBoolean("enableSilktouch");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setBoolean("enableSilktouch", enableSilktouch);
	}
}

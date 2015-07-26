package cr0s.warpdrive.machines;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;

import java.util.HashSet;
import java.util.Set;

import cpw.mods.fml.common.FMLCommonHandler;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.PacketHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;
import cr0s.warpdrive.item.ItemReactorLaserFocus;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.common.ForgeDirection;

public class TileEntityLaserReactorMonitor extends TileEntityAbstractLaser {
	private final int workRate = 10;
	private int ticks = 0;
	
	private Set<Object> findReactors() {//returns either IReactor or IReactorChamber tile entity
		int[] xD = {-2, 2, 0, 0, 0, 0};
		int[] yD = { 0, 0,-2, 2, 0, 0};
		int[] zD = { 0, 0, 0, 0,-2, 2};
		Set<Object> output = new HashSet<Object>();
		for(int i = 0; i < xD.length; i++) {
			int xO = xCoord + xD[i];
			int yO = yCoord + yD[i];
			int zO = zCoord + zD[i];
			TileEntity te = worldObj.getBlockTileEntity(xO, yO, zO);
			if(te == null)
				continue;
			
			if (te instanceof IReactor) {
				output.add(te);
			} else if(te instanceof IReactorChamber) {
				IReactor reactor = ((IReactorChamber)te).getReactor();
				if(reactor == null)
					continue;
				
				ChunkCoordinates coords = reactor.getPosition();
				
				if(Math.abs(coords.posX - xCoord) == 1)
					continue;
				if(Math.abs(coords.posY - yCoord) == 1)
					continue;
				if(Math.abs(coords.posZ - zCoord) == 1)
					continue;
				
				output.add(te);
			}
		}
		return output;
	}
	
	private boolean coolReactor(IReactor react) {
		boolean didCoolReactor = false;
		for(int x = 0; x < 9; x++) {
			for(int y = 0; y < 6; y++) {
				ItemStack item = react.getItemAt(x, y);
				if (item != null) {
					if(item.getItem() instanceof ItemReactorLaserFocus) {
						int heat = item.getItemDamage();
						int heatRemoval = (int) Math.floor(Math.min(getEnergyStored() / WarpDriveConfig.RM_EU_PER_HEAT, heat));
						if (heatRemoval > 0) {
							didCoolReactor = true;
							consumeEnergy((int) Math.ceil(heatRemoval * WarpDriveConfig.RM_EU_PER_HEAT), false);
							item.setItemDamage(heat - heatRemoval);
						}
					}
				}
			}
		}
		return didCoolReactor;
	}
	
	@Override
	public void updateEntity() {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			return;
		}
		super.updateEntity();
		
		ticks++;
		if (ticks > workRate)  {
			ticks = 0;
			Vector3 myPos = new Vector3(this).translate(0.5);
			Set<Object> reactors = findReactors();
			if(reactors.size() == 0)
				return;
			
			for(Object o : reactors)
			{
				IReactor react = null;
				if(o instanceof TileEntity)
				{
					if(o instanceof IReactor)
						react = (IReactor)o;
					else if(o instanceof IReactorChamber)
						react = ((IReactorChamber)o).getReactor();
					if(react != null)
					{
						if(coolReactor(react))
						{
							TileEntity te = (TileEntity)o;
							PacketHandler.sendBeamPacket(worldObj, myPos, new Vector3(te.xCoord,te.yCoord,te.zCoord).translate(0.5D), 0f, 0.8f, 1f, 20, 0, 20);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
	}
	
	@Override
	public boolean shouldChunkLoad() {
		return false;
	}

	@Override
	public int getMaxEnergyStored() {
		return WarpDriveConfig.RM_MAX_ENERGY;
	}

	@Override
	public int getMaxSafeInput() {
		return Integer.MAX_VALUE;
	}
    
    @Override
    public boolean canInputEnergy(ForgeDirection from) {
    	return true;
    }
}

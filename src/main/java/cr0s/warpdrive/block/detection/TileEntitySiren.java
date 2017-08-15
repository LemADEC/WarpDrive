package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractBase;
import cr0s.warpdrive.client.SirenSound;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntitySiren extends TileEntityAbstractBase {
	
	public enum SirenState {
		STARTING, STARTED, STOPPING, STOPPED
	}
	
	private SirenState state = SirenState.STOPPED;
	private boolean isRaidSiren = false;
	private float range = 0.0F;
	private int timeToLastUpdate = 0;
	
	@SideOnly(Side.CLIENT)
	private SirenSound sound;
	
	public TileEntitySiren() {
		super();
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		range = BlockSiren.getRange(getBlockMetadata());
		isRaidSiren = BlockSiren.getIsRaid(getBlockMetadata());
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		/* Updating the sound too quickly breaks Minecraft's sounds handler.
		 * Therefore, we only update our sound once every 0.5 seconds.
		 * It's less responsive like this, but doesn't completely freak out when
		 * spamming the redstone on and off. */
		
		if (this.timeToLastUpdate <= 0) {
			this.timeToLastUpdate = 10;
		} else {
			this.timeToLastUpdate--;
			return;
		}
        
		if (!hasWorldObj() || !worldObj.isRemote) {
		    return;
        }
		if (sound == null) {
		    setSound();
        }

		// Siren sound logic.
		switch (this.state) {
			case STOPPED:
				if (this.isPlaying()) {
					this.state = SirenState.STOPPING;
				}
				if (this.isPowered()) {
					this.state = SirenState.STARTING;
				}
				break;
            
			case STARTING:
				if (this.startSound()) {
					this.state = SirenState.STARTED;
				} else {
					this.state = SirenState.STOPPING;
				}
				break;
            
			case STARTED:
				if (!this.isPowered()) {
					this.state = SirenState.STOPPING;
				} else if (!this.isPlaying()) {
					this.state = SirenState.STARTING;
				}
				break;
            
			case STOPPING:
				if (this.isPlaying()) {
					this.stopSound();
				} else {
					this.state = SirenState.STOPPED;
				}
				break;
            
			default:
				if (this.isPlaying()) {
					this.state = SirenState.STOPPING;
				} else {
					this.state = SirenState.STOPPED;
				}
				break;
		}
	}
    
	// Stops the siren when the chunk is unloaded.
	@Override
	public void onChunkUnload() {
		if (worldObj.isRemote && this.isPlaying()) {
		    stopSound();
        }
		super.onChunkUnload();
	}
    
	// Stops the siren when the TileEntity object is invalidated.
	@Override
	public void invalidate() {
		if (worldObj.isRemote && isPlaying()) {
		    stopSound();
        }
		super.invalidate();
	}
    
	// Create a new SirenSound object that the siren will use.
	@SideOnly(Side.CLIENT)
	private void setSound() {
		String resource = WarpDrive.MODID + ":siren_" + (isRaidSiren ? "raid" : "industrial");
		sound = new SirenSound(new ResourceLocation(resource), range, xCoord, yCoord, zCoord);
	}
    
	// Forces the siren to start playing its sound;
	@SideOnly(Side.CLIENT)
    private boolean startSound() {
		if (!isPlaying()) {
			try {
				Minecraft.getMinecraft().getSoundHandler().playSound(sound);
				return true;
			} catch (IllegalArgumentException e) {
				return false;
			}
		} else {
			return true;
		}
	}
    
	// Forces the siren to stop playing its sound.
	@SideOnly(Side.CLIENT)
	void stopSound() {
		Minecraft.getMinecraft().getSoundHandler().stopSound(sound);
	}
    
	// Checks if the siren is currently playing its sound.
	@SideOnly(Side.CLIENT)
	boolean isPlaying() {
		return Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(sound);
	}
    
	// Checks if the siren is being powered by redstone.
	private boolean isPowered() {
		return worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
	}
}

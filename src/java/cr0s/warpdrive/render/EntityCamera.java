package cr0s.warpdrive.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.FMLCommonHandler;
import cr0s.warpdrive.PacketHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;

public final class EntityCamera extends EntityLivingBase
{
    public int xCoord;
    public int yCoord;
    public int zCoord;

    private EntityPlayer player;

    private Minecraft mc = Minecraft.getMinecraft();

    private int dx = 0, dy = 0, dz = 0;
    private int zoomNumber = 0;

    private int closeWaitTicks = 0;
    private int zoomWaitTicks = 0;
    private int fireWaitTicks = 0;
    private boolean isActive = true;
    private int bootUpTicks = 20;

    public EntityCamera(World world) {
        super(world);
		// TODO Auto-generated constructor stub
	}
    
	private boolean isCentered = true;

    public EntityCamera(World world, ChunkPosition pos, EntityPlayer player) {
        super(world);
        this.setInvisible(true);
        int x = pos.chunkPosX;
        int y = pos.chunkPosY;
        int z = pos.chunkPosZ;
        this.xCoord = x;
        this.posX = x;
        this.yCoord = y;
        this.posY = y;
        this.zCoord = z;
        this.posZ = z;
        this.player = player;
    }
    
    private void closeCamera() {
    	if (!isActive) {
    		return;
    	}
    	    	
		ClientCameraUtils.resetViewpoint();
    	worldObj.removeEntity(this);
    	isActive = false;
    }
    
    @Override
    public void onEntityUpdate() {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
        	if (player == null || player.isDead) {
        		WarpDrive.debugPrint("" + this + " Player is null or dead, closing camera...");
        		closeCamera();
        		return;
        	}
        	if (!ClientCameraUtils.isValidContext(worldObj)) {
        		WarpDrive.debugPrint("" + this + " Invalid context, closing camera...");
        		closeCamera();
        		return;
        	}
        	
        	Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
            mc.renderViewEntity.rotationYaw = player.rotationYaw;
            //mc.renderViewEntity.rotationYawHead = player.rotationYawHead;
            mc.renderViewEntity.rotationPitch = player.rotationPitch;

            WarpDrive.instance.debugMessage = "Mouse " + Mouse.isButtonDown(0) + " " + Mouse.isButtonDown(1) + " " + Mouse.isButtonDown(2) + " " + Mouse.isButtonDown(3)
            			+ "\nBackspace " + Keyboard.isKeyDown(Keyboard.KEY_BACKSLASH) + " Space " + Keyboard.isKeyDown(Keyboard.KEY_SPACE) + " Shift " + "";
            // Perform zoom
            if (Mouse.isButtonDown(0)) {// FIXME merge: main is using right click with Mouse.isButtonDown(1), branch is using left click
            	zoomWaitTicks++;
            	if (zoomWaitTicks >= 2) {
            		zoomWaitTicks = 0;
            		zoom();
                }
            } else {
            	zoomWaitTicks = 0;
            }

            if (bootUpTicks > 0) {
            	bootUpTicks--;
            } else {
	            if (Mouse.isButtonDown(1)) {
	            	closeWaitTicks++;
	            	if (closeWaitTicks >= 2) {
	            		closeWaitTicks = 0;
	    	    		closeCamera();
	            	}
	            } else {
	            	closeWaitTicks = 0;
	            }
            }
            
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {// FIXME merge: main is using left click with Mouse.isButtonDown(0), branch is using space bar
            	fireWaitTicks++;
            	if (fireWaitTicks >= 2) {
                    fireWaitTicks = 0;

                    // Make a shoot with camera-laser
                    if (block.isAssociatedBlock(WarpDrive.laserCamBlock)) {
                    	PacketHandler.sendLaserTargetingPacket(xCoord, yCoord, zCoord, mc.renderViewEntity.rotationYaw, mc.renderViewEntity.rotationPitch);
                    }
            	}
            } else {
                fireWaitTicks = 0;
            }
            
            GameSettings gamesettings = mc.gameSettings;
            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                dy = -1;
            } else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                dy = 2;
            } else if (Keyboard.isKeyDown(gamesettings.keyBindLeft.getKeyCode())) {
                dz = -1;
            } else if (Keyboard.isKeyDown(gamesettings.keyBindRight.getKeyCode())) {
                dz = 1;
            } else if (Keyboard.isKeyDown(gamesettings.keyBindForward.getKeyCode())) {
                dx = 1;
            } else if (Keyboard.isKeyDown(gamesettings.keyBindBack.getKeyCode())) {
                dx = -1;
            } else if (Keyboard.isKeyDown(Keyboard.KEY_C)) { // centering view
                dx = 0;
                dy = 0;
                dz = 0;
            	isCentered = !isCentered;
				return;
            }

			if (isCentered) {
				setPosition(xCoord + 0.5D, yCoord + 0.75D, zCoord + 0.5D);	
			} else {
				setPosition(xCoord + dx, yCoord + dy, zCoord + dz);
            }
        }
    }

    @Override
    public void onUpdate() {
    	super.onUpdate();
        this.motionX = this.motionY = this.motionZ = 0.0D;
    }
    
    public void zoom() {
    	/*//TODO: Find 1.7 version of EnumOptions
        if (zoomNumber == 0) {
            mc.gameSettings.setOptionFloatValue(EnumOptions.FOV, -0.75F);
            mc.gameSettings.setOptionFloatValue(EnumOptions.SENSITIVITY, 0.4F);
        } else if (zoomNumber == 1) {
            mc.gameSettings.setOptionFloatValue(EnumOptions.FOV, -1.25F);
            mc.gameSettings.setOptionFloatValue(EnumOptions.SENSITIVITY, 0.3F);
        } else if (zoomNumber == 2) {
            mc.gameSettings.setOptionFloatValue(EnumOptions.FOV, -1.6F);
            mc.gameSettings.setOptionFloatValue(EnumOptions.SENSITIVITY, 0.15F);
        } else if (zoomNumber == 3) {
            mc.gameSettings.setOptionFloatValue(EnumOptions.FOV, WarpDrive.normalFOV);
            mc.gameSettings.setOptionFloatValue(EnumOptions.SENSITIVITY, WarpDrive.normalSensitivity);
        }*/
        zoomNumber = (zoomNumber + 1) % 4;
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return false;
    }

    @Override
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.attackDamage).setBaseValue(1.0D);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        xCoord = nbttagcompound.getInteger("x");
        yCoord = nbttagcompound.getInteger("y");
        zCoord = nbttagcompound.getInteger("z");
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbttagcompound)
    {
        nbttagcompound.setInteger("x", xCoord);
        nbttagcompound.setInteger("y", yCoord);
        nbttagcompound.setInteger("z", zCoord);
    }

    @Override
    public ItemStack getHeldItem() {
        return null;
    }

    @Override
    public void setCurrentItemOrArmor(int i, ItemStack itemstack) {
    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return null;
    }

	@Override
	public ItemStack getEquipmentInSlot(int i) {
		return null;
	}
}
package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.network.PacketHandler;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class EntityCamera extends EntityLivingBase {
	
	// entity coordinates (x, y, z) are dynamically changed by player
	
	// camera block coordinates are fixed
	private int cameraX;
	private int cameraY;
	private int cameraZ;
	
	private EntityPlayer player;
	private float playerYawLast;
	private float playerPitchLast;
	
	private int dx = 0, dy = 0, dz = 0;
	
	private int closeWaitTicks = 0;
	private int zoomWaitTicks = 0;
	private int fireWaitTicks = 0;
	private boolean isActive = true;
	private int bootUpTicks = 20;
	
	private boolean isCentered = true;
	
	// required for registration?
	public EntityCamera(final World world) {
		super(world);
	}
	
	public EntityCamera(final World world, final int x, final int y, final int z, final EntityPlayer player) {
		super(world);
		
		posX = x;
		posY = y;
		posZ = z;
		cameraX = x;
		cameraY = y;
		cameraZ = z;
		this.player = player;
		playerYawLast = player.rotationYaw;
		playerPitchLast = player.rotationPitch;
	}
		
	@Override
	protected void entityInit() {
		super.entityInit();
		setInvisible(true);
		// set viewpoint inside camera
		noClip = true;
	}
	
	// set viewpoint inside camera
	@Override
	public float getEyeHeight() {
		return 1.62F;
	}
	
	// override to skip the block bounding override on client side
	@Override
	public void setPositionAndRotation(final double x, final double y, final double z, final float yaw, final float pitch) {
		//	super.setPositionAndRotation(x, y, z, yaw, pitch);
		this.setPosition(x, y, z);
		this.setRotation(yaw, pitch);
	}
	
	private void closeCamera() {
		if (!isActive) {
			return;
		}
		
		PacketHandler.sendCameraOrientationPacket(ClientCameraHandler.blockPosCheck1, new BlockPos(cameraX, cameraY, cameraZ), rotationYaw, rotationPitch);
		ClientCameraHandler.resetViewpoint();
		world.removeEntity(this);
		isActive = false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void onEntityUpdate() {
		if (world.isRemote) {
			if (player == null || player.isDead) {
				WarpDrive.logger.error(String.format("%s Player is null or dead, closing camera...",
				                                     this));
				closeCamera();
				return;
			}
			if (!ClientCameraHandler.isValidContext(world)) {
				WarpDrive.logger.error(String.format("%s Invalid context, closing camera...",
				                                     this));
				closeCamera();
				return;
			}
			
			final float deltaYaw = MathHelper.wrapDegrees(player.rotationYaw - playerYawLast);
			final float deltaPitch = player.rotationPitch - playerPitchLast;
			rotationYaw = MathHelper.wrapDegrees(rotationYaw + deltaYaw);
			rotationPitch = MathHelper.clamp(rotationPitch + deltaPitch, -90.0F, 90.0F);
			playerYawLast = player.rotationYaw;
			playerPitchLast = player.rotationPitch;

			final Block block = world.getBlockState(new BlockPos(cameraX, cameraY, cameraZ)).getBlock();
			final Minecraft mc = Minecraft.getMinecraft();
			
			ClientCameraHandler.overlayLoggingMessage = "Mouse " + Mouse.isButtonDown(0) + " " + Mouse.isButtonDown(1) + " " + Mouse.isButtonDown(2) + " " + Mouse.isButtonDown(3)
			                                          + "\nBackspace " + Keyboard.isKeyDown(Keyboard.KEY_BACKSLASH)
			                                          + " Space " + Keyboard.isKeyDown(Keyboard.KEY_SPACE)
			                                          + " Shift " + "";
			// Perform zoom
			if (Mouse.isButtonDown(0)) {
				zoomWaitTicks++;
				if (zoomWaitTicks >= 2) {
					zoomWaitTicks = 0;
					ClientCameraHandler.zoom();
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
			
			if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
				fireWaitTicks++;
				if (fireWaitTicks >= 2) {
					fireWaitTicks = 0;
					
					// Make a shoot with camera-laser
					if (block.isAssociatedBlock(WarpDrive.blockLaserCamera)) {
						PacketHandler.sendLaserTargetingPacket(cameraX, cameraY, cameraZ, mc.getRenderViewEntity().rotationYaw, mc.getRenderViewEntity().rotationPitch);
					}
				}
			} else {
				fireWaitTicks = 0;
			}
			
			if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
				dy = -1;
			} else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
				dy = 2;
			} else if (Commons.isKeyPressed(mc.gameSettings.keyBindLeft)) {
				dz = -1;
			} else if (Commons.isKeyPressed(mc.gameSettings.keyBindRight)) {
				dz = 1;
			} else if (Commons.isKeyPressed(mc.gameSettings.keyBindForward)) {
				dx = 1;
			} else if (Commons.isKeyPressed(mc.gameSettings.keyBindBack)) {
				dx = -1;
			} else if (Keyboard.isKeyDown(Keyboard.KEY_C)) { // centering view
				dx = 0;
				dy = 0;
				dz = 0;
				isCentered = !isCentered;
				return;
			}
			
			if (isCentered) {
				setPosition(cameraX + 0.5D, cameraY + 0.5D, cameraZ + 0.5D);
			} else {
				setPosition(cameraX + dx, cameraY + dy, cameraZ + dz);
			}
		}
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		motionX = motionY = motionZ = 0.0D;
	}
	
	@Override
	public boolean shouldRenderInPass(final int pass) {
		return false;
	}
	
	/*
	// Item no clip
	@Override
	protected boolean func_145771_j(double par1, double par3, double par5) {
		// Clipping is fine, don't move me
		return false;
	}
	/**/
	
	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return Block.NULL_AABB;
	}
	/*
	@Override
	public AxisAlignedBB getEntityBoundingBox() {
		return null;
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return null;
	}
	/**/
	@Override
	public boolean canBePushed() {
		return false;
	}
	
	@Nonnull
	@Override
	public EnumHandSide getPrimaryHand() {
		return EnumHandSide.RIGHT;
	}
	
	@Override
	public void move(final MoverType type, final double x, final double y, final double z) {
	}
	
	@Override
	public void readEntityFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		// nothing to save, skip ancestor call
		cameraX = tagCompound.getInteger("x");
		cameraY = tagCompound.getInteger("y");
		cameraZ = tagCompound.getInteger("z");
	}
	
	@Override
	public void writeEntityToNBT(final NBTTagCompound nbttagcompound) {
		// nothing to save, skip ancestor call
		nbttagcompound.setInteger("x", cameraX);
		nbttagcompound.setInteger("y", cameraY);
		nbttagcompound.setInteger("z", cameraZ);
	}
	
	@Nonnull
	@Override
	public Iterable<ItemStack> getArmorInventoryList() {
		return Collections.emptyList();
	}
	
	@Nonnull
	@Override
	public ItemStack getItemStackFromSlot(@Nonnull final EntityEquipmentSlot slotIn) {
		return ItemStack.EMPTY;
	}
	
	@Override
	public void setItemStackToSlot(@Nonnull final EntityEquipmentSlot slotIn, @Nullable final ItemStack itemStack) {
		
	}
}

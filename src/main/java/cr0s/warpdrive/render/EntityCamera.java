package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.client.ClientProxy;
import cr0s.warpdrive.network.PacketHandler;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

public final class EntityCamera extends LivingEntity {
	
	public static final EntityType<EntityCamera> TYPE;
	public static final Vec3d VECTOR_NO_MOTION = new Vec3d(0.0D, 0.0D, 0.0D);
	
	// persistent properties
	// (none)
	
	// computed properties
	// entity coordinates (x, y, z) are dynamically changed by player
	
	// camera block coordinates are fixed
	private int cameraX;
	private int cameraY;
	private int cameraZ;
	
	private PlayerEntity player;
	
	private int dx = 0, dy = 0, dz = 0;
	
	private int closeWaitTicks = 0;
	private int zoomWaitTicks = 0;
	private int fireWaitTicks = 0;
	private boolean isActive = true;
	private int bootUpTicks = 20;
	
	private boolean isCentered = true;
	
	static {
		TYPE = EntityType.Builder.<EntityCamera>create(EntityCamera::new, EntityClassification.MISC)
				       .setTrackingRange(300)
				       .setUpdateInterval(1)
				       .setShouldReceiveVelocityUpdates(false)
				       .build("entity_camera");
		TYPE.setRegistryName(WarpDrive.MODID, "entity_camera");
	}
	
	public EntityCamera(final EntityType<EntityCamera> entityType, final World world) {
		super(entityType, world);
	}
	
	public EntityCamera(final World world, final int x, final int y, final int z, final PlayerEntity player) {
		super(TYPE, world);
		
		setRawPosition(x, y, z);
		cameraX = x;
		cameraY = y;
		cameraZ = z;
		this.player = player;
	}
		
	@Override
	protected void registerData() {
		super.registerData();
		setInvisible(true);
		// set viewpoint inside camera
		noClip = true;
	}
	
	// set viewpoint inside camera
	@Override
	public float getEyeHeight(@Nonnull final Pose pose) {
		return 1.62F;
	}
	
	@Nonnull
	@Override
	public IPacket<?> createSpawnPacket() {
		return new SSpawnObjectPacket(this);
	}
	
	private void closeCamera() {
		if (!isActive) {
			return;
		}
		
		ClientCameraHandler.resetViewpoint();
		remove();
		isActive = false;
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void tick() {
		if (world.isRemote()) {
			if ( player == null
			  || !player.isAlive() ) {
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
			
			final Block block = world.getBlockState(new BlockPos(cameraX, cameraY, cameraZ)).getBlock();
			final Minecraft mc = Minecraft.getInstance();
			if (mc.getRenderViewEntity() != null) {
				mc.getRenderViewEntity().rotationYaw = player.rotationYaw;
				// mc.renderViewEntity.rotationYawHead = player.rotationYawHead;
				mc.getRenderViewEntity().rotationPitch = player.rotationPitch;
			}
			
			ClientCameraHandler.overlayLoggingMessage = "ZoomIn/Out with " + ClientProxy.keyBindingCameraZoomIn.getKey().getTranslationKey() + "/" + ClientProxy.keyBindingCameraZoomOut.getKey().getTranslationKey()
			                                          + "\nShoot with " + ClientProxy.keyBindingCameraShoot.getKey().getTranslationKey();
			// Perform zoom
			if (ClientProxy.keyBindingCameraZoomIn.isKeyDown()) {
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
				if (ClientProxy.keyBindingCameraZoomOut.isKeyDown()) {
					closeWaitTicks++;
					if (closeWaitTicks >= 2) {
						closeWaitTicks = 0;
						closeCamera();
					}
				} else {
					closeWaitTicks = 0;
				}
			}
			
			if (ClientProxy.keyBindingCameraShoot.isKeyDown()) {
				fireWaitTicks++;
				if (fireWaitTicks >= 2) {
					fireWaitTicks = 0;
					
					// Make a shoot with camera-laser
					if (block == WarpDrive.blockLaserCamera) {
						PacketHandler.sendLaserTargetingPacket(cameraX, cameraY, cameraZ, mc.getRenderViewEntity().rotationYaw, mc.getRenderViewEntity().rotationPitch);
					}
				}
			} else {
				fireWaitTicks = 0;
			}
			
			if (mc.gameSettings.keyBindDrop.isKeyDown()) {
				dy = -1;
			} else if (mc.gameSettings.keyBindJump.isKeyDown()) {
				dy = 2;
			} else if (mc.gameSettings.keyBindLeft.isKeyDown()) {
				dz = -1;
			} else if (mc.gameSettings.keyBindRight.isKeyDown()) {
				dz = 1;
			} else if (mc.gameSettings.keyBindForward.isKeyDown()) {
				dx = 1;
			} else if (mc.gameSettings.keyBindBack.isKeyDown()) {
				dx = -1;
			} else if (ClientProxy.keyBindingCameraCenter.isKeyDown()) { // centering view
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
		
		setMotion(VECTOR_NO_MOTION);
	}
	
	@Override
	public boolean isInRangeToRenderDist(double distance) {
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
		return null;
	}
	
	@Override
	public boolean canBePushed() {
		return false;
	}
	
	@Nonnull
	@Override
	public HandSide getPrimaryHand() {
		return HandSide.RIGHT;
	}
	
	@Override
	public void move(@Nonnull final MoverType type, @Nonnull final Vec3d position) {
	}
	
	@Override
	public void readAdditional(@Nonnull final CompoundNBT tagCompound) {
		// nothing to save, skip ancestor call
		cameraX = tagCompound.getInt("x");
		cameraY = tagCompound.getInt("y");
		cameraZ = tagCompound.getInt("z");
	}
	
	@Override
	public void writeAdditional(final CompoundNBT nbttagcompound) {
		// nothing to save, skip ancestor call
		nbttagcompound.putInt("x", cameraX);
		nbttagcompound.putInt("y", cameraY);
		nbttagcompound.putInt("z", cameraZ);
	}
	
	@Nonnull
	@Override
	public Iterable<ItemStack> getArmorInventoryList() {
		return Collections.emptyList();
	}
	
	@Nonnull
	@Override
	public ItemStack getItemStackFromSlot(@Nonnull final EquipmentSlotType slotIn) {
		return ItemStack.EMPTY;
	}
	
	@Override
	public void setItemStackToSlot(@Nonnull final EquipmentSlotType slotIn, @Nullable final ItemStack itemStack) {
		
	}
}
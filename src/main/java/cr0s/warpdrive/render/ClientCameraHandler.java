package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumCameraType;

import javax.annotation.Nonnull;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientCameraHandler {
	public static boolean isOverlayEnabled = false;
	
	public static EnumCameraType overlayType = null;
	public static int zoomIndex = 0;
	public static String overlayLoggingMessage = "";
	public static double originalFOV = 70.0D;
	public static double originalSensitivity = 100.0D;
	
	public static PlayerEntity entityPlayer;
	public static DimensionType dimensionType = null;
	public static BlockPos blockPosCheck1, blockPosCheck2;
	public static BlockState blockStateCheck1, blockStateCheck2;
	
	public ClientCameraHandler() {
		final Minecraft mc = Minecraft.getInstance();
		
		if (WarpDriveConfig.LOGGING_CAMERA) {
			WarpDrive.logger.info(String.format("FOV is %.3f Sensitivity is %.3f",
			                                    mc.gameSettings.fov, mc.gameSettings.mouseSensitivity));
		}
	}
	
	public static void setupViewpoint(final EnumCameraType enumCameraType, final PlayerEntity entityPlayer, final float initialYaw, final float initialPitch,
	                                  final BlockPos blockPosMonitor, final BlockState blockStateMonitor,
	                                  final BlockPos blockPosCamera, final BlockState blockStateCamera) {
		final Minecraft mc = Minecraft.getInstance();
		
		if (entityPlayer == null) {
			WarpDrive.logger.error("setupViewpoint with null player => denied");
			return;
		}
		
		// Save initial state
		originalFOV = mc.gameSettings.fov;
		originalSensitivity = mc.gameSettings.mouseSensitivity;
		overlayType = enumCameraType;
		ClientCameraHandler.entityPlayer = entityPlayer;
		dimensionType = entityPlayer.world.getDimension().getType();
		blockPosCheck1 = blockPosMonitor;
		blockStateCheck1 = blockStateMonitor;
		blockPosCheck2 = blockPosCamera;
		blockStateCheck2 = blockStateCamera;
		
		// Spawn camera entity
		// note: as a client-only entity, it must go through ClientWorld.addEntity(id, entity)
		final EntityCamera entityCamera = new EntityCamera(entityPlayer.world, blockPosCamera.getX(), blockPosCamera.getY(), blockPosCamera.getZ(), entityPlayer);
		((ClientWorld) entityPlayer.world).addEntity(entityCamera.getEntityId(), entityCamera);
		// entityCamera.setPositionAndUpdate(camera_x + 0.5D, camera_y + 0.5D, camera_z + 0.5D);
		entityCamera.setLocationAndAngles(blockPosCamera.getX() + 0.5D, blockPosCamera.getY() + 0.5D, blockPosCamera.getZ() + 0.5D, initialYaw, initialPitch);
		
		// Update view
		if (WarpDriveConfig.LOGGING_CAMERA) {
			WarpDrive.logger.info(String.format("Setting viewpoint to %s", entityCamera));
		}
		mc.setRenderViewEntity(entityCamera);
		mc.gameSettings.thirdPersonView = 0;
		refreshViewPoint();
		isOverlayEnabled = true;
		
		// TODO MC1.15 camera keyboard
		// Keyboard.enableRepeatEvents(true);
	}
	
	private static void refreshViewPoint() {
		final Minecraft mc = Minecraft.getInstance();
		
		switch (zoomIndex) {
		case 0:
			mc.gameSettings.fov = originalFOV;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 2.0F;
			break;
			
		case 1:
			mc.gameSettings.fov = originalFOV / 1.5F;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 3.0F;
			break;
			
		case 2:
			mc.gameSettings.fov = originalFOV / 3.0F;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 6.0F;
			break;
			
		case 3:
			mc.gameSettings.fov = originalFOV / 4.5F;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 9.0F;
			break;
			
		default:
			mc.gameSettings.fov = originalFOV;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 2.0F;
			break;
		}
	}
	
	public static void zoom() {
		zoomIndex = (zoomIndex + 1) % 4;
		refreshViewPoint();
		if (WarpDriveConfig.LOGGING_CAMERA) {
			final Minecraft mc = Minecraft.getInstance();
			assert mc.player != null;
			mc.player.sendChatMessage("changed to FOV " + mc.gameSettings.fov + " mouseSensitivity " + mc.gameSettings.mouseSensitivity);
		}
	}
	
	public static void resetViewpoint() {
		final Minecraft mc = Minecraft.getInstance();
		if (entityPlayer != null) {
			mc.setRenderViewEntity(entityPlayer);
			entityPlayer = null;
			if (WarpDriveConfig.LOGGING_CAMERA) {
				WarpDrive.logger.info("Resetting viewpoint");
			}
		} else {
			WarpDrive.logger.error("resetting viewpoint with invalid player entity?");
		}
		
		// TODO MC1.15 camera keyboard
		// Keyboard.enableRepeatEvents(false);
		
		isOverlayEnabled = false;
		mc.gameSettings.thirdPersonView = 0;
		mc.gameSettings.fov = originalFOV;
		mc.gameSettings.mouseSensitivity = originalSensitivity;
		
		entityPlayer = null;
		dimensionType = null;
	}
	
	public static boolean isValidContext(final World world) {
		if (world == null || world.getDimension().getType() != dimensionType) {
			return false;
		}
		if (world.getBlockState(blockPosCheck1).getBlock() != blockStateCheck1.getBlock()) {
			WarpDrive.logger.error(String.format("Checking camera viewpoint, found invalid block1 %s",
			                                     Commons.format(world, blockPosCheck1)));
			return false;
		}
		if (world.getBlockState(blockPosCheck2).getBlock() != blockStateCheck2.getBlock()) {
			WarpDrive.logger.error(String.format("Checking camera viewpoint, found invalid block2 %s",
			                                     Commons.format(world, blockPosCheck2)));
			return false;
		}
		return true;
	}
	
	@SubscribeEvent
	public void onEvent(@Nonnull final LoggedOutEvent event) {
		if (isOverlayEnabled) {
			resetViewpoint();
		}
	}
}
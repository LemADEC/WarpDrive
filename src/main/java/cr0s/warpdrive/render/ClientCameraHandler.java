package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumCameraType;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class ClientCameraHandler {
	public static boolean isOverlayEnabled = false;
	
	public static EnumCameraType overlayType = null;
	public static int zoomIndex = 0;
	public static String overlayLoggingMessage = "";
	public static float originalFOV = 70.0F;
	public static float originalSensitivity = 100.0F;
	
	public static EntityPlayer entityPlayer;
	public static int dimensionId = -666;
	public static BlockPos blockPosCheck1, blockPosCheck2;
	public static IBlockState blockStateCheck1, blockStateCheck2;
	
	public ClientCameraHandler() {
		final Minecraft mc = Minecraft.getMinecraft();
		
		if (WarpDriveConfig.LOGGING_CAMERA) {
			WarpDrive.logger.info(String.format("FOV is %.3f Sensitivity is %.3f",
			                                    mc.gameSettings.fovSetting, mc.gameSettings.mouseSensitivity));
		}
	}
	
	public static void setupViewpoint(final EnumCameraType enumCameraType, final EntityPlayer entityPlayer, final float initialYaw, final float initialPitch,
	                                  final BlockPos blockPosMonitor, final IBlockState blockStateMonitor,
	                                  final BlockPos blockPosCamera, final IBlockState blockStateCamera) {
		final Minecraft mc = Minecraft.getMinecraft();
		
		if (entityPlayer == null) {
			WarpDrive.logger.error("setupViewpoint with null player => denied");
			return;
		}
		
		// Save initial state
		originalFOV = mc.gameSettings.fovSetting;
		originalSensitivity = mc.gameSettings.mouseSensitivity;
		overlayType = enumCameraType;
		ClientCameraHandler.entityPlayer = entityPlayer;
		dimensionId = entityPlayer.world.provider.getDimension();
		blockPosCheck1 = blockPosMonitor;
		blockStateCheck1 = blockStateMonitor;
		blockPosCheck2 = blockPosCamera;
		blockStateCheck2 = blockStateCamera;
		
		// Spawn camera entity
		final EntityCamera entityCamera = new EntityCamera(entityPlayer.world, blockPosCamera.getX(), blockPosCamera.getY(), blockPosCamera.getZ(), entityPlayer);
		entityPlayer.world.spawnEntity(entityCamera);
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
		
		Keyboard.enableRepeatEvents(true);
	}
	
	private static void refreshViewPoint() {
		final Minecraft mc = Minecraft.getMinecraft();
		
		switch (zoomIndex) {
		case 0:
			mc.gameSettings.fovSetting = originalFOV;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 2.0F;
			break;
			
		case 1:
			mc.gameSettings.fovSetting = originalFOV / 1.5F;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 3.0F;
			break;
			
		case 2:
			mc.gameSettings.fovSetting = originalFOV / 3.0F;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 6.0F;
			break;
			
		case 3:
			mc.gameSettings.fovSetting = originalFOV / 4.5F;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 9.0F;
			break;
			
		default:
			mc.gameSettings.fovSetting = originalFOV;
			mc.gameSettings.mouseSensitivity = originalSensitivity / 2.0F;
			break;
		}
	}
	
	public static void zoom() {
		zoomIndex = (zoomIndex + 1) % 4;
		refreshViewPoint();
		if (WarpDriveConfig.LOGGING_CAMERA) {
			final Minecraft mc = Minecraft.getMinecraft();
			assert mc.player != null;
			mc.player.sendChatMessage("changed to fovSetting " + mc.gameSettings.fovSetting + " mouseSensitivity " + mc.gameSettings.mouseSensitivity);
		}
	}
	
	public static void resetViewpoint() {
		final Minecraft mc = Minecraft.getMinecraft();
		if (entityPlayer != null) {
			mc.setRenderViewEntity(entityPlayer);
			entityPlayer = null;
			if (WarpDriveConfig.LOGGING_CAMERA) {
				WarpDrive.logger.info("Resetting viewpoint");
			}
		} else {
			WarpDrive.logger.error("resetting viewpoint with invalid player entity?");
		}
		
		Keyboard.enableRepeatEvents(false);
		
		isOverlayEnabled = false;
		mc.gameSettings.thirdPersonView = 0;
		mc.gameSettings.fovSetting = originalFOV;
		mc.gameSettings.mouseSensitivity = originalSensitivity;
		
		entityPlayer = null;
		dimensionId = -666;
	}
	
	public static boolean isValidContext(final World world) {
		if (world == null || world.provider.getDimension() != dimensionId) {
			return false;
		}
		if (!world.getBlockState(blockPosCheck1).getBlock().isAssociatedBlock(blockStateCheck1.getBlock())) {
			WarpDrive.logger.error(String.format("Checking camera viewpoint, found invalid block1 %s",
			                                     Commons.format(world, blockPosCheck1)));
			return false;
		}
		if (!world.getBlockState(blockPosCheck2).getBlock().isAssociatedBlock(blockStateCheck2.getBlock())) {
			WarpDrive.logger.error(String.format("Checking camera viewpoint, found invalid block2 %s",
			                                     Commons.format(world, blockPosCheck2)));
			return false;
		}
		return true;
	}
	
	@SubscribeEvent
	public void onEvent(final MouseEvent event) {
		if ( isOverlayEnabled
		  && (event.getButton() == 0 || event.getButton() == 1) ) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onEvent(final RenderHandEvent event) {
		if (isOverlayEnabled) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onEvent(final ClientDisconnectionFromServerEvent event) {
		if (isOverlayEnabled) {
			resetViewpoint();
		}
	}
}

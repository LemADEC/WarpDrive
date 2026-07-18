package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import com.mojang.blaze3d.systems.RenderSystem;

@OnlyIn(Dist.CLIENT)
public class RenderOverlayLocation {
	
	private static final Minecraft minecraft = Minecraft.getInstance();
	
	private void renderLocation(final int widthScreen, final int heightScreen) {
		// get player
		final PlayerEntity entityPlayer = minecraft.player;
		if (entityPlayer == null) {
			return;
		}
		final int x = MathHelper.floor(entityPlayer.getPosX());
		final int z = MathHelper.floor(entityPlayer.getPosZ());
		
		// get celestial object
		String name = Commons.format(entityPlayer.world);
		String description = "";
		final CelestialObject celestialObject = CelestialObjectManager.get(entityPlayer.world, x, z);
		if (celestialObject != null) {
			if (!celestialObject.getDisplayName().isEmpty()) {
				name = celestialObject.getDisplayName();
			}
		    description = celestialObject.getDescription();
		}
		
		// start rendering
		RenderSystem.enableBlend();
		minecraft.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
		
		// show current location name & description
		RenderCommons.drawText(widthScreen, heightScreen, name, description,
		                                  WarpDriveConfig.CLIENT_LOCATION_SCALE,
		                                  WarpDriveConfig.CLIENT_LOCATION_NAME_PREFIX,
		                                  WarpDriveConfig.CLIENT_LOCATION_BACKGROUND_COLOR,
		                                  WarpDriveConfig.CLIENT_LOCATION_TEXT_COLOR,
		                                  WarpDriveConfig.CLIENT_LOCATION_HAS_SHADOW,
		                                  WarpDriveConfig.CLIENT_LOCATION_SCREEN_ALIGNMENT,
		                                  WarpDriveConfig.CLIENT_LOCATION_SCREEN_OFFSET_X,
		                                  WarpDriveConfig.CLIENT_LOCATION_SCREEN_OFFSET_Y,
		                                  WarpDriveConfig.CLIENT_LOCATION_TEXT_ALIGNMENT,
		                                  WarpDriveConfig.CLIENT_LOCATION_WIDTH_RATIO,
		                                  WarpDriveConfig.CLIENT_LOCATION_WIDTH_MIN);
		
		// @TODO: show orbiting planet?
		
		// close rendering
		// (done by RenderSystem & TextureManager)
	}
	
	@SubscribeEvent
	public void onRender(final RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.HOTBAR) {
			renderLocation(event.getWindow().getScaledWidth(), event.getWindow().getScaledHeight());
		}
	}
}
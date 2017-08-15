package cr0s.warpdrive.render;

import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

@SideOnly(Side.CLIENT)
public class RenderOverlayLocation {
	
	private static Minecraft minecraft = Minecraft.getMinecraft();
	
	private void renderLocation(final int widthScreen, final int heightScreen) {
		// get player
		EntityPlayer entityPlayer = minecraft.thePlayer;
		if (entityPlayer == null) {
			return;
		}
		final int x = MathHelper.floor_double(entityPlayer.posX);
		final int z = MathHelper.floor_double(entityPlayer.posZ);
		
		// get celestial object
		String name = entityPlayer.worldObj.provider.getDimensionName();
		String description = "";
		final CelestialObject celestialObject = CelestialObjectManager.get(entityPlayer.worldObj, x, z);
		if (celestialObject != null) {
			if (!celestialObject.getDisplayName().isEmpty()) {
				name = celestialObject.getDisplayName();
			}
		    description = celestialObject.getDescription();
		}
		
		// start rendering
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		// show current location name & description
		RenderCommons.drawText(widthScreen, heightScreen, name, description,
		                                  WarpDriveConfig.CLIENT_LOCATION_SCALE,
		                                  WarpDriveConfig.CLIENT_LOCATION_FORMAT_TITLE,
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
		minecraft.getTextureManager().bindTexture(Gui.icons);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	@SubscribeEvent
	public void onRender(RenderGameOverlayEvent.Pre event) {
		if (event.type == ElementType.HOTBAR) {
			renderLocation(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
		}
	}
}
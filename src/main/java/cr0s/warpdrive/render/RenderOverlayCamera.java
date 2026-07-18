package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumCameraType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;

@OnlyIn(Dist.CLIENT)
public class RenderOverlayCamera {
	
	private static final int ANIMATION_FRAMES = 200;
	
	private final Minecraft minecraft = Minecraft.getInstance();
	private int frameCount = 0;
	
	private void renderOverlay(final int scaledWidth, final int scaledHeight) {
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.disableAlphaTest();
		
		try {
			final String strHelp;
			if (ClientCameraHandler.overlayType == EnumCameraType.SIMPLE_CAMERA) {
				minecraft.getTextureManager().bindTexture(new ResourceLocation("warpdrive", "textures/block/detection/camera-overlay.png"));
				strHelp = "Left click to zoom / Right click to exit";
			} else {
				minecraft.getTextureManager().bindTexture(new ResourceLocation("warpdrive", "textures/block/weapon/laser_camera-overlay.png"));
				strHelp = "Left click to zoom / Right click to exit / Space to fire";
			}
			
			final Tessellator tessellator = Tessellator.getInstance();
			final BufferBuilder vertexBuffer = tessellator.getBuffer();
			
			vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
			vertexBuffer.pos(       0.0D, scaledHeight, -90.0D).tex(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
			vertexBuffer.pos(scaledWidth, scaledHeight, -90.0D).tex(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
			vertexBuffer.pos(scaledWidth,         0.0D, -90.0D).tex(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
			vertexBuffer.pos(       0.0D,         0.0D, -90.0D).tex(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
			tessellator.draw();
			
			frameCount++;
			if (frameCount >= ANIMATION_FRAMES) {
				frameCount = 0;
			}
			final float time = Math.abs(frameCount * 2.0F / ANIMATION_FRAMES - 1.0F);
			final int color = (RenderCommons.colorGradient(time, 0x40, 0xA0) << 16)
			                + (RenderCommons.colorGradient(time, 0x80, 0x00) << 8)
			                +  RenderCommons.colorGradient(time, 0x80, 0xFF);
			minecraft.fontRenderer.drawStringWithShadow(strHelp,
			                                            (scaledWidth - minecraft.fontRenderer.getStringWidth(strHelp)) / 2.0F,
			                                            (int)(scaledHeight * 0.19F) - minecraft.fontRenderer.FONT_HEIGHT,
			                                            color);
			
			final String strZoom = "Zoom " + (ClientCameraHandler.originalFOV / minecraft.gameSettings.fov) + "x";
			minecraft.fontRenderer.drawStringWithShadow(strZoom,
			                                            (int) (scaledWidth  * 0.91F) - minecraft.fontRenderer.getStringWidth(strZoom),
			                                            (int) (scaledHeight * 0.81F),
			                                            0x40A080);
			
			if (WarpDriveConfig.LOGGING_CAMERA) {
				minecraft.fontRenderer.drawStringWithShadow(ClientCameraHandler.overlayLoggingMessage,
				                                            (scaledWidth - minecraft.fontRenderer.getStringWidth(ClientCameraHandler.overlayLoggingMessage)) / 2.0F,
				                                            (int) (scaledHeight * 0.19F),
				                                            0xFF008F);
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
		
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableAlphaTest();
	}
	
	@SubscribeEvent
	public void onRender(final RenderGameOverlayEvent.Pre event) {
		if (ClientCameraHandler.isOverlayEnabled) {
			switch (event.getType()) {
			case HELMET:
				renderOverlay(event.getWindow().getScaledWidth(), event.getWindow().getScaledHeight());
				break;
				
			case AIR:
			case ARMOR:
			case BOSSHEALTH:
			case BOSSINFO:
			case CROSSHAIRS:
			case EXPERIENCE:
			case FOOD:
			case HEALTH:
			case HEALTHMOUNT:
			case HOTBAR:
			case TEXT:
				// Don't render inventory/stats GUI parts
				if (event.isCancelable()) {
					event.setCanceled(true);
				}
				break;
				
			default:
				// Keep other GUI parts: PORTAL, JUMPBAR, CHAT, PLAYER_LIST, DEBUG, POTION_ICONS, SUBTITLES, FPS_GRAPH, VIGNETTE
				break;
			}
		}
	}
}
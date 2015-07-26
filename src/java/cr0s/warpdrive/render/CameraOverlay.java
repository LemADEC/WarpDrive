package cr0s.warpdrive.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;

public class CameraOverlay
{
    private Minecraft mc;
    private int frameCount = 0;
    private static int ANIMATION_FRAMES = 200;

    public CameraOverlay(Minecraft parMinecraft) {
        mc = parMinecraft;
    }
    
    private static int colorGradient(float gradient, int start, int end) {
    	return Math.max(0, Math.min(255, start + Math.round(gradient * (end - start))));
    }

    protected void renderOverlay(int scaledWidth, int scaledHeight) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        try {
	        String text;
	        if (WarpDrive.instance.overlayType == 0) {
	            mc.getTextureManager().bindTexture(new ResourceLocation("warpdrive", "textures/blocks/camOverlay.png"));
	            text = "Left click to zoom / Right click to exit";
	        } else {
	            mc.getTextureManager().bindTexture(new ResourceLocation("warpdrive", "textures/blocks/lasercamOverlay.png"));
	            text = "Left click to zoom / Right click to exit / Space to fire";
	        }
	
	        Tessellator tessellator = Tessellator.instance;
	        tessellator.startDrawingQuads();
	        tessellator.addVertexWithUV(       0.0D, scaledHeight, -90.0D, 0.0D, 1.0D);
	        tessellator.addVertexWithUV(scaledWidth, scaledHeight, -90.0D, 1.0D, 1.0D);
	        tessellator.addVertexWithUV(scaledWidth,         0.0D, -90.0D, 1.0D, 0.0D);
	        tessellator.addVertexWithUV(       0.0D,         0.0D, -90.0D, 0.0D, 0.0D);
	        tessellator.draw();
	        
	        frameCount++;
	        if (frameCount >= ANIMATION_FRAMES) {
	        	frameCount = 0;
	        }
	        float time = Math.abs(frameCount * 2.0F / ANIMATION_FRAMES - 1.0F);
	        int color = (colorGradient(time, 0x40, 0xA0) << 16) + (colorGradient(time, 0x80, 0x00) << 8) + colorGradient(time, 0x80, 0xFF);
	        mc.fontRenderer.drawString(text, (scaledWidth - mc.fontRenderer.getStringWidth(text)) / 2, 30, color, true);
	        if (WarpDriveConfig.G_DEBUGMODE) {
	        	mc.fontRenderer.drawString(WarpDrive.instance.debugMessage, (scaledWidth - mc.fontRenderer.getStringWidth(WarpDrive.instance.debugMessage)) / 2, 40, 0xFF008F, true);
	        }
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Pre event) {
        if (WarpDrive.instance.isOverlayEnabled) {
            if (event.type == ElementType.HELMET) {
                renderOverlay(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
            } else if (event.type == ElementType.EXPERIENCE || event.type == ElementType.HOTBAR || event.type == ElementType.ARMOR
            		|| event.type == ElementType.HEALTH || event.type == ElementType.HEALTHMOUNT || event.type == ElementType.FOOD
            		|| event.type == ElementType.BOSSHEALTH || event.type == ElementType.TEXT || event.type == ElementType.AIR) {
            	// Don't render other GUI parts
            	if (event.isCancelable()) {
                	event.setCanceled(true);
                }
            }
        }
    }
}
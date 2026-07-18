package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.data.EnumDisplayAlignment;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.TranslationTextComponent;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;

@OnlyIn(Dist.CLIENT)
public class RenderCommons {
	
	private static final Minecraft minecraft = Minecraft.getInstance();
	private static final int TEXT_BORDER = 2;
	private static final float SCALE_UV = 0.00390625F;  // 1/256
	
	protected static int colorGradient(final float gradient, final int start, final int end) {
		return Math.max(0, Math.min(255, start + Math.round(gradient * (end - start))));
	}
	
	// from net.minecraft.client.gui.Gui
	protected static void drawTexturedModalRect(final int x, final int y, final int u, final int v,
	                                            final int sizeX, final int sizeY, final int zLevel) {
		drawTexturedModalRect(x, y, u, v, sizeX, sizeY, zLevel, 1.0F, 1.0F, 1.0F, 1.0F);
	}
	protected static void drawTexturedModalRect(final int x, final int y, final int u, final int v,
	                                            final int sizeX, final int sizeY, final int zLevel,
	                                            final float red, final float green, final float blue, final float alpha) {
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		vertexBuffer.pos( x         , (y + sizeY), zLevel).tex(SCALE_UV * u          , SCALE_UV * (v + sizeY)).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos((x + sizeX), (y + sizeY), zLevel).tex(SCALE_UV * (u + sizeX), SCALE_UV * (v + sizeY)).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos((x + sizeX),  y         , zLevel).tex(SCALE_UV * (u + sizeX), SCALE_UV * v).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos( x         ,  y         , zLevel).tex(SCALE_UV * u          , SCALE_UV * v).color(red, green, blue, alpha).endVertex();
		tessellator.draw();
	}
	
	public static int drawSplashAlarm(final int scaledWidth, final int scaledHeight, final String title, final String message) {
		// compute animation clock
		final double cycle = ((System.nanoTime() / 1000L) % 0x200000) / (double) 0x200000;
		
		// start rendering
		RenderSystem.pushMatrix();
		RenderSystem.scalef(2.0F, 2.0F, 0.0F);
		
		int y = scaledHeight / 10;
		
		// bold title, single line, centered, with shadows
		final String textTitle = Commons.updateEscapeCodes("§l" + new TranslationTextComponent(title).getFormattedText());
		minecraft.fontRenderer.drawStringWithShadow(textTitle,
		                                            scaledWidth / 4.0F - minecraft.fontRenderer.getStringWidth(textTitle) / 2.0F,
		                                            y - minecraft.fontRenderer.FONT_HEIGHT,
		                                            Commons.colorARGBtoInt(230, 255, 32, 24) );
		
		// normal message, multi-lines, centered, without shadows
		final String textMessage = Commons.updateEscapeCodes(new TranslationTextComponent(message).getFormattedText());
		final int alpha = 160 + (int) (85.0D * Math.sin(cycle * 2 * Math.PI));
		
		final List<String> listMessages = minecraft.fontRenderer.listFormattedStringToWidth(textMessage, scaledWidth / 2);
		for (final String textLine : listMessages) {
			minecraft.fontRenderer.drawString(textLine,
			                                  scaledWidth / 4.0F - minecraft.fontRenderer.getStringWidth(textLine) / 2.0F,
			                                  y,
			                                  Commons.colorARGBtoInt(alpha, 192, 64, 48) );
			y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		
		// close rendering
		RenderSystem.popMatrix();
		return alpha;
	}
	
	public static void drawText(final int screen_width, final int screen_height, final String text,
	                           final float scale, final String formatPrefix, final int colorBackground, final int colorText, final boolean hasShadow,
	                           final EnumDisplayAlignment enumScreenAnchor, final int xOffset, final int yOffset,
	                           final EnumDisplayAlignment enumTextAlignment, final float widthTextRatio, final int widthTextMin) {
		// prepare the string box content and dimensions
		final String text_formatted = Commons.updateEscapeCodes(formatPrefix + new TranslationTextComponent(text).getFormattedText());
		final int scaled_box_width = Math.max(widthTextMin, Math.round(widthTextRatio * screen_width)) + 2 * TEXT_BORDER;
		
		final List<String> listLines = minecraft.fontRenderer.listFormattedStringToWidth(text_formatted, scaled_box_width - 2 * TEXT_BORDER);
		final int scaled_box_height = listLines.size() * minecraft.fontRenderer.FONT_HEIGHT + 2 * TEXT_BORDER;
		
		// compute the position
		final int screen_text_x = Math.round(screen_width  * enumScreenAnchor.xRatio + xOffset - enumTextAlignment.xRatio * scaled_box_width  * scale);
		final int screen_text_y = Math.round(screen_height * enumScreenAnchor.yRatio + yOffset - enumTextAlignment.yRatio * scaled_box_height * scale);
		
		// start rendering
		RenderSystem.pushMatrix();
		RenderSystem.scalef(scale, scale, 0.0F);
		final int scaled_box_x  = Math.round(screen_text_x / scale - TEXT_BORDER);
		final int scaled_box_y  = Math.round(screen_text_y / scale - TEXT_BORDER);
		final int scaled_text_x = Math.round(screen_text_x / scale);
		int scaled_text_y       = Math.round(screen_text_y / scale);
		
		// draw background box
		RenderSystem.disableTexture();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		
		final float red   = (colorBackground >> 16 & 255) / 255.0F;
		final float blue  = (colorBackground >> 8  & 255) / 255.0F;
		final float green = (colorBackground       & 255) / 255.0F;
		final float alpha = (colorBackground >> 24 & 255) / 255.0F;
		
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y + scaled_box_height, -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y + scaled_box_height, -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y                    , -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y                    , -90.0D).color(red, green, blue, alpha).endVertex();
		tessellator.draw();
		RenderSystem.enableTexture();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		
		// draw text
		for (final String textLine : listLines) {
			if (hasShadow) {
				minecraft.fontRenderer.drawStringWithShadow(textLine, scaled_text_x, scaled_text_y, colorText);
			} else {
				minecraft.fontRenderer.drawString(textLine, scaled_text_x, scaled_text_y, colorText);
			}
			scaled_text_y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		
		// close rendering
		RenderSystem.popMatrix();
	}
	
	public static void drawText(final int screen_width, final int screen_height, final String textHeader, final String textContent,
	                            final float scale, final String prefixHeader, final int colorBackground, final int colorText, final boolean hasHeaderShadow,
	                            @Nonnull final EnumDisplayAlignment enumScreenAnchor, final int xOffset, final int yOffset,
	                            @Nonnull final EnumDisplayAlignment enumTextAlignment, final float widthTextRatio, final int widthTextMin) {
		// prepare the string box content and dimensions
		final String header_formatted  = Commons.updateEscapeCodes(prefixHeader + new TranslationTextComponent(textHeader).getFormattedText());
		final String content_formatted = Commons.updateEscapeCodes(new TranslationTextComponent(textContent).getFormattedText());
		final int scaled_box_width = Math.max(widthTextMin, Math.round(widthTextRatio * screen_width)) + 2 * TEXT_BORDER;
		
		final List<String> listHeaderLines = 
			header_formatted.isEmpty() ? new ArrayList<>(0)
			                           : minecraft.fontRenderer.listFormattedStringToWidth(header_formatted, scaled_box_width - 2 * TEXT_BORDER);
		final List<String> listContentLines =
			content_formatted.isEmpty() ? new ArrayList<>(0)
		                                : minecraft.fontRenderer.listFormattedStringToWidth(content_formatted, scaled_box_width - 2 * TEXT_BORDER);
		final boolean hasTileAndContent = listHeaderLines.size() > 0 && listContentLines.size() > 0;
		final int scaled_box_height = (listHeaderLines.size() + listContentLines.size()) * minecraft.fontRenderer.FONT_HEIGHT
		                            + (hasTileAndContent ? 3 : 1) * TEXT_BORDER;
		
		// compute the position
		final int screen_text_x = Math.round(screen_width  * enumScreenAnchor.xRatio + xOffset - enumTextAlignment.xRatio * scaled_box_width  * scale);
		final int screen_text_y = Math.round(screen_height * enumScreenAnchor.yRatio + yOffset - enumTextAlignment.yRatio * scaled_box_height * scale);
		
		// start rendering
		RenderSystem.pushMatrix();
		RenderSystem.scalef(scale, scale, 0.0F);
		final int scaled_box_x  = Math.round(screen_text_x / scale - TEXT_BORDER);
		final int scaled_box_y  = Math.round(screen_text_y / scale - TEXT_BORDER);
		final int scaled_text_x = Math.round(screen_text_x / scale);
		int scaled_text_y       = Math.round(screen_text_y / scale);
		
		// draw background box
		RenderSystem.disableTexture();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		
		final float red   = (colorBackground >> 16 & 0xFF) / 255.0F;
		final float green = (colorBackground >> 8  & 0xFF) / 255.0F;
		final float blue  = (colorBackground       & 0xFF) / 255.0F;
		final float alpha = (colorBackground >> 24 & 0xFF) / 255.0F;
		
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y + scaled_box_height, -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y + scaled_box_height, -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y                    , -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y                    , -90.0D).color(red, green, blue, alpha).endVertex();
		tessellator.draw();
		RenderSystem.enableTexture();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		
		// draw text
		for (final String textLine : listHeaderLines) {
			if (hasHeaderShadow) {
				minecraft.fontRenderer.drawStringWithShadow(textLine, scaled_text_x, scaled_text_y, colorText);
			} else {
				minecraft.fontRenderer.drawString(textLine, scaled_text_x, scaled_text_y, colorText);
			}
			scaled_text_y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		if (hasTileAndContent) {
			scaled_text_y += TEXT_BORDER;
		}
		for (final String textLine : listContentLines) {
			minecraft.fontRenderer.drawString(textLine, scaled_text_x, scaled_text_y, colorText);
			scaled_text_y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		
		// close rendering
		// RenderSystem.enableTexture();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.popMatrix();
	}
}
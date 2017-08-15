package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.data.EnumDisplayAlignment;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBeacon;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockDragonEgg;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderCommons {
	
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	
	protected static int colorGradient(final float gradient, final int start, final int end) {
		return Math.max(0, Math.min(255, start + Math.round(gradient * (end - start))));
	}
	
	// from net.minecraft.client.gui.Gui
	private static final float scaleUV = 0.00390625F;  // 1/256
	protected static void drawTexturedModalRect(final int x, final int y, final int u, final int v, final int sizeX, final int sizeY, final int zLevel) {
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV( x         , (y + sizeY), zLevel, scaleUV * u          , scaleUV * (v + sizeY));
		tessellator.addVertexWithUV((x + sizeX), (y + sizeY), zLevel, scaleUV * (u + sizeX), scaleUV * (v + sizeY));
		tessellator.addVertexWithUV((x + sizeX),  y         , zLevel, scaleUV * (u + sizeX), scaleUV *  v         );
		tessellator.addVertexWithUV( x         ,  y         , zLevel, scaleUV * u          , scaleUV *  v         );
		tessellator.draw();
	}
	
	public static int drawSplashAlarm(final int scaledWidth, final int scaledHeight, final String title, final String message) {
		// compute animation clock
		final double cycle = ((System.nanoTime() / 1000) % 0x200000) / (double) 0x200000;
		
		// start rendering
		GL11.glPushMatrix();
		GL11.glScalef(2.0F, 2.0F, 0.0F);
		
		int y = scaledHeight / 10;
		
		// bold title, single line, centered, with shadows
		final String textTitle = Commons.updateEscapeCodes("§l" + StatCollector.translateToLocal(title));
		minecraft.fontRenderer.drawString(textTitle,
		                                  scaledWidth / 4 - minecraft.fontRenderer.getStringWidth(textTitle) / 2,
		                                  y - minecraft.fontRenderer.FONT_HEIGHT,
		                                  Commons.colorARGBtoInt(230, 255, 32, 24),
		                                  true);
		
		// normal message, multi-lines, centered, without shadows
		final String textMessage = Commons.updateEscapeCodes(StatCollector.translateToLocal(message));
		final int alpha = 160 + (int) (85.0D * Math.sin(cycle * 2 * Math.PI));
		
		@SuppressWarnings("unchecked")
		final List<String> listMessages = minecraft.fontRenderer.listFormattedStringToWidth(textMessage, scaledWidth / 2);
		for (final String textLine : listMessages) {
			minecraft.fontRenderer.drawString(textLine,
			                                  scaledWidth / 4 - minecraft.fontRenderer.getStringWidth(textLine) / 2,
			                                  y,
			                                  Commons.colorARGBtoInt(alpha, 192, 64, 48),
			                                  false);
			y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		
		// close rendering
		GL11.glPopMatrix();
		return alpha;
	}
	
	private static final int TEXT_BORDER = 2;
	public static void drawText(final int screen_width, final int screen_height, final String text,
	                           final float scale, final String formatPrefix, final int colorBackground, final int colorText, final boolean hasShadow,
	                           final EnumDisplayAlignment enumScreenAnchor, final int xOffset, final int yOffset,
	                           final EnumDisplayAlignment enumTextAlignment, final float widthTextRatio, final int widthTextMin) {
		// prepare the string box content and dimensions
		final String text_formatted = Commons.updateEscapeCodes(formatPrefix + StatCollector.translateToLocal(text));
		final int scaled_box_width = Math.max(widthTextMin, Math.round(widthTextRatio * screen_width)) + 2 * TEXT_BORDER;
		
		@SuppressWarnings("unchecked")
		final List<String> listLines = minecraft.fontRenderer.listFormattedStringToWidth(text_formatted, scaled_box_width - 2 * TEXT_BORDER);
		final int scaled_box_height = listLines.size() * minecraft.fontRenderer.FONT_HEIGHT + 2 * TEXT_BORDER;
		
		// compute the position
		final int screen_text_x = Math.round(screen_width  * enumScreenAnchor.xRatio + xOffset - enumTextAlignment.xRatio * scaled_box_width  * scale);
		final int screen_text_y = Math.round(screen_height * enumScreenAnchor.yRatio + yOffset - enumTextAlignment.yRatio * scaled_box_height * scale);
		
		// start rendering
		GL11.glPushMatrix();
		GL11.glScalef(scale, scale, 0.0F);
		final int scaled_box_x  = Math.round(screen_text_x / scale - TEXT_BORDER);
		final int scaled_box_y  = Math.round(screen_text_y / scale - TEXT_BORDER);
		final int scaled_text_x = Math.round(screen_text_x / scale);
		int scaled_text_y       = Math.round(screen_text_y / scale);
		
		// draw background box
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		final byte red   = (byte) (colorBackground >> 16 & 255);
		final byte blue  = (byte) (colorBackground >> 8 & 255);
		final byte green = (byte) (colorBackground & 255);
		final byte alpha = (byte) (colorBackground >> 24 & 255);
		GL11.glColor4b(red, blue, green, alpha);
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertex(scaled_box_x                   , scaled_box_y + scaled_box_height, -90.0D);
		tessellator.addVertex(scaled_box_x + scaled_box_width, scaled_box_y + scaled_box_height, -90.0D);
		tessellator.addVertex(scaled_box_x + scaled_box_width, scaled_box_y                    , -90.0D);
		tessellator.addVertex(scaled_box_x                   , scaled_box_y                    , -90.0D);
		tessellator.draw();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		
		// draw text
		for (final String textLine : listLines) {
			minecraft.fontRenderer.drawString(textLine, scaled_text_x, scaled_text_y, colorText, hasShadow);
			scaled_text_y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		
		// close rendering
		GL11.glPopMatrix();
	}
	
	public static void drawText(final int screen_width, final int screen_height, final String textHeader, final String textContent,
	                           final float scale, final String formatHeader, final int colorBackground, final int colorText, final boolean hasHeaderShadow,
	                           final EnumDisplayAlignment enumScreenAnchor, final int xOffset, final int yOffset,
	                           final EnumDisplayAlignment enumTextAlignment, final float widthTextRatio, final int widthTextMin) {
		// prepare the string box content and dimensions
		final String header_formatted  = Commons.updateEscapeCodes(String.format(StatCollector.translateToLocal(textHeader), formatHeader));
		final String content_formatted = Commons.updateEscapeCodes(StatCollector.translateToLocal(textContent));
		final int scaled_box_width = Math.max(widthTextMin, Math.round(widthTextRatio * screen_width)) + 2 * TEXT_BORDER;
		
		@SuppressWarnings("unchecked")
		final List<String> listHeaderLines = 
			header_formatted.isEmpty() ? new ArrayList<>(0)
			                           : minecraft.fontRenderer.listFormattedStringToWidth(header_formatted, scaled_box_width - 2 * TEXT_BORDER);
		@SuppressWarnings("unchecked")
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
		GL11.glPushMatrix();
		GL11.glScalef(scale, scale, 0.0F);
		final int scaled_box_x  = Math.round(screen_text_x / scale - TEXT_BORDER);
		final int scaled_box_y  = Math.round(screen_text_y / scale - TEXT_BORDER);
		final int scaled_text_x = Math.round(screen_text_x / scale);
		int scaled_text_y       = Math.round(screen_text_y / scale);
		
		// draw background box
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		final byte red   = (byte) (colorBackground >> 16 & 255);
		final byte blue  = (byte) (colorBackground >> 8 & 255);
		final byte green = (byte) (colorBackground & 255);
		final byte alpha = (byte) (colorBackground >> 24 & 255);
		GL11.glColor4b(red, blue, green, alpha);
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertex(scaled_box_x                   , scaled_box_y + scaled_box_height, -90.0D);
		tessellator.addVertex(scaled_box_x + scaled_box_width, scaled_box_y + scaled_box_height, -90.0D);
		tessellator.addVertex(scaled_box_x + scaled_box_width, scaled_box_y                    , -90.0D);
		tessellator.addVertex(scaled_box_x                   , scaled_box_y                    , -90.0D);
		tessellator.draw();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		
		// draw text
		for (final String textLine : listHeaderLines) {
			minecraft.fontRenderer.drawString(textLine, scaled_text_x, scaled_text_y, colorText, hasHeaderShadow);
			scaled_text_y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		if (hasTileAndContent) {
			scaled_text_y += TEXT_BORDER;
		}
		for (final String textLine : listContentLines) {
			minecraft.fontRenderer.drawString(textLine, scaled_text_x, scaled_text_y, colorText, false);
			scaled_text_y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		
		// close rendering
		GL11.glPopMatrix();
	}
	
	public static boolean renderWorldBlockCamouflaged(final int x, final int y, final int z, final Block blockDefault, final RenderBlocks renderer, final int renderType, final Block blockCamouflage) {
		if (renderType >= 0) {
			try {
				blockCamouflage.setBlockBoundsBasedOnState(renderer.blockAccess, x, y, z);
				renderer.setRenderBoundsFromBlock(blockCamouflage);
				
				switch (renderType) {
				case 0 : renderer.renderStandardBlock(blockCamouflage, x, y, z); break;
				case 1 : renderer.renderCrossedSquares(blockCamouflage, x, y, z); break;
				case 2 : renderer.renderBlockTorch(blockCamouflage, x, y, z); break;
				case 3 : renderer.renderBlockFire((BlockFire)blockCamouflage, x, y, z); break;
				// case 4 : renderer.renderBlockLiquid(blockCamouflage, x, y, z); break; // not working due to material check of neighbours during computation
				case 5 : renderer.renderBlockRedstoneWire(blockCamouflage, x, y, z); break;
				case 6 : renderer.renderBlockCrops(blockCamouflage, x, y, z); break;
				// case 7 : renderer.renderBlockDoor(blockCamouflage, x, y, z); break; // not working and doesn't make sense
				case 9 : renderer.renderBlockMinecartTrack((BlockRailBase)blockCamouflage, x, y, z); break;
				case 10 : renderer.renderBlockStairs((BlockStairs)blockCamouflage, x, y, z); break;
				case 11 : renderer.renderBlockFence((BlockFence)blockCamouflage, x, y, z); break;
				case 12 : renderer.renderBlockLever(blockCamouflage, x, y, z); break;
				case 13 : renderer.renderBlockCactus(blockCamouflage, x, y, z); break;
				case 14 : renderer.renderBlockBed(blockCamouflage, x, y, z); break;
				case 15 : renderer.renderBlockRepeater((BlockRedstoneRepeater)blockCamouflage, x, y, z); break;
				case 16 : renderer.renderPistonBase(blockCamouflage, x, y, z, false); break;
				case 17 : renderer.renderPistonExtension(blockCamouflage, x, y, z, true); break;
				case 18 : renderer.renderBlockPane((BlockPane)blockCamouflage, x, y, z); break;
				// 19 is stem
				case 20 : renderer.renderBlockVine(blockCamouflage, x, y, z); break;
				case 21 : renderer.renderBlockFenceGate((BlockFenceGate)blockCamouflage, x, y, z); break;
				// 22 is chest
				case 23 : renderer.renderBlockLilyPad(blockCamouflage, x, y, z); break;
				case 24 : renderer.renderBlockCauldron((BlockCauldron)blockCamouflage, x, y, z); break;
				case 25 : renderer.renderBlockBrewingStand((BlockBrewingStand)blockCamouflage, x, y, z); break;
				case 26 : renderer.renderBlockEndPortalFrame((BlockEndPortalFrame)blockCamouflage, x, y, z); break;
				case 27 : renderer.renderBlockDragonEgg((BlockDragonEgg)blockCamouflage, x, y, z); break;
				case 28 : renderer.renderBlockCocoa((BlockCocoa)blockCamouflage, x, y, z); break;
				case 29 : renderer.renderBlockTripWireSource(blockCamouflage, x, y, z); break;
				case 30 : renderer.renderBlockTripWire(blockCamouflage, x, y, z); break;
				case 31 : renderer.renderBlockLog(blockCamouflage, x, y, z); break;
				case 32 : renderer.renderBlockWall((BlockWall)blockCamouflage, x, y, z); break;
				case 33 : renderer.renderBlockFlowerpot((BlockFlowerPot)blockCamouflage, x, y, z); break; // won't render content due to tileEntity access
				case 34 : renderer.renderBlockBeacon((BlockBeacon)blockCamouflage, x, y, z); break;
				case 35 : renderer.renderBlockAnvil((BlockAnvil)blockCamouflage, x, y, z); break;
				case 36 : renderer.renderBlockRedstoneDiode((BlockRedstoneDiode)blockCamouflage, x, y, z); break;
				case 37 : renderer.renderBlockRedstoneComparator((BlockRedstoneComparator)blockCamouflage, x, y, z); break;
				case 38 : renderer.renderBlockHopper((BlockHopper)blockCamouflage, x, y, z); break;
				case 39 : renderer.renderBlockQuartz(blockCamouflage, x, y, z); break;
				// 40 is double plant
				case 41 : renderer.renderBlockStainedGlassPane(blockCamouflage, x, y, z); break;
				default:
					// blacklist the faulty block
					WarpDrive.logger.error("Disabling camouflage with block " + Block.blockRegistry.getNameForObject(blockCamouflage) + " due to invalid renderType " + renderType);
					Dictionary.BLOCKS_NOCAMOUFLAGE.add(blockCamouflage);
					return false;
				}
			} catch(Exception exception) {
				exception.printStackTrace();
				
				// blacklist the faulty block
				WarpDrive.logger.error("Disabling camouflage block " + Block.blockRegistry.getNameForObject(blockCamouflage) + " due to previous exception");
				Dictionary.BLOCKS_NOCAMOUFLAGE.add(blockCamouflage);
				
				// render normal default block
				renderer.renderStandardBlock(blockDefault, x, y, z);
				// renderer.renderBlockAsItem(blockCamouflage, metaCamouflage, 1);
			}
			return true;
		}
		
		return renderer.renderStandardBlock(blockDefault, x, y, z);
	}
}
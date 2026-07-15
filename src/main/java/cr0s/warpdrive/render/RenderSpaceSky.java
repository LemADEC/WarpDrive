package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObject.RenderData;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.Vector3;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.SkyRenderHandler;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;

public class RenderSpaceSky implements SkyRenderHandler {
	
	private static RenderSpaceSky INSTANCE = null;
	
	// player distance for transitions
	private static final double PLANET_FAR = 1786.0D;
	private static final double PLANET_APPROACHING = 512.0D;
	private static final double PLANET_ORBIT = 128.0D;
	
	// render distance for objects
	private static final float  BOX_RENDER_RANGE = 100.0F;
	
	// call lists
	private static VertexBuffer vboStars;
	private static final VertexFormat vertexFormatStars = DefaultVertexFormats.POSITION_COLOR;
	private static float starBrightness = 0.0F;
	private static final float ALPHA_TOLERANCE = 1.0F / 256.0F;
	
	private static final VertexFormat vertexFormatPlanes = DefaultVertexFormats.POSITION_COLOR;
	private static final VertexBuffer vboUpperPlane;
	private static final VertexBuffer vboLowerPlane;
	
	static {
		// pre-generate sky objects
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		
		vboUpperPlane = new VertexBuffer(vertexFormatPlanes);
		final int stepSize = 64;
		final int nbSteps = 256 / stepSize + 2;
		float y = 16.0F;
		for (int x = -stepSize * nbSteps; x <= stepSize * nbSteps; x += stepSize) {
			for (int z = -stepSize * nbSteps; z <= stepSize * nbSteps; z += stepSize) {
				vertexBuffer.begin(GL11.GL_QUADS, vertexFormatPlanes);
				vertexBuffer.pos(x           , y, z           ).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
				vertexBuffer.pos(x + stepSize, y, z           ).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
				vertexBuffer.pos(x + stepSize, y, z + stepSize).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
				vertexBuffer.pos(x           , y, z + stepSize).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
				vertexBuffer.finishDrawing();
			}
		}
		vboUpperPlane.upload(vertexBuffer);
		
		vboLowerPlane = new VertexBuffer(vertexFormatPlanes);
		y = -16.0F;
		for (int x = -stepSize * nbSteps; x <= stepSize * nbSteps; x += stepSize) {
			for (int z = -stepSize * nbSteps; z <= stepSize * nbSteps; z += stepSize) {
				vertexBuffer.begin(GL11.GL_QUADS, vertexFormatPlanes);
				vertexBuffer.pos(x + stepSize, y, z           ).color(0.30F, 0.30F, 0.30F, 1.00F).endVertex();
				vertexBuffer.pos(x           , y, z           ).color(0.30F, 0.30F, 0.30F, 1.00F).endVertex();
				vertexBuffer.pos(x           , y, z + stepSize).color(0.30F, 0.30F, 0.30F, 1.00F).endVertex();
				vertexBuffer.pos(x + stepSize, y, z + stepSize).color(0.30F, 0.30F, 0.30F, 1.00F).endVertex();
				vertexBuffer.finishDrawing();
			}
		}
		vboLowerPlane.upload(vertexBuffer);
	}
	
	// private final ResourceLocation textureStar = new ResourceLocation("warpdrive:textures/celestial/star_yellow.png");
	// private final ResourceLocation texturePlanet = new ResourceLocation("warpdrive:textures/celestial/planet_green.png");
	
	public static RenderSpaceSky getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RenderSpaceSky();
		}
		return INSTANCE;
	}
	
	@Override
	public void render(final int ticks, final float partialTicks, @Nonnull final MatrixStack matrixStack,
	                   @Nonnull final ClientWorld world, @Nonnull final Minecraft mc) {
		assert mc.player != null;
		final Vec3d vec3Player = mc.player.getEyePosition(partialTicks);
		final CelestialObject celestialObject = CelestialObjectManager.get(world);
		
		final Tessellator tessellator = Tessellator.getInstance();
		
		RenderSystem.depthMask(false);
		
		// draw sky box
		if ( celestialObject != null
		  && celestialObject.boxTextures != null
		  && celestialObject.boxTextures.length > 0 ) {
			renderSkyBox(tessellator, matrixStack, celestialObject.boxTextures, celestialObject.boxBrightness, celestialObject.boxRepeat);
		}
		
		RenderSystem.disableTexture();
		
		// draw upper sky plane
		/*
		final Vec3d skyColor = getCustomSkyColor();
		float skyColorRed   = (float) skyColor.x * (1 - world.getStarBrightness(partialTicks) * 2);
		float skyColorGreen = (float) skyColor.y * (1 - world.getStarBrightness(partialTicks) * 2);
		float skyColorBlue  = (float) skyColor.z * (1 - world.getStarBrightness(partialTicks) * 2);
		
		if (mc.gameSettings.anaglyph) {
			final float red2   = (skyColorRed * 30.0F + skyColorGreen * 59.0F + skyColorBlue * 11.0F) / 100.0F;
			final float green2 = (skyColorRed * 30.0F + skyColorGreen * 70.0F) / 100.0F;
			final float blue2  = (skyColorRed * 30.0F + skyColorBlue * 70.0F) / 100.0F;
			skyColorRed = red2;
			skyColorGreen = green2;
			skyColorBlue = blue2;
		}
		
		// RenderSystem.enableFog();
		RenderSystem.callList(callListUpperPlane);
		// RenderSystem.disableFog();
		/**/
		
		// compute global alpha
		final float alphaBase = 1.0F; // - world.getRainStrength(partialTicks);
		
		// draw stars
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		RenderSystem.disableAlphaTest();
		float starBrightness = 0.2F;
		starBrightness = world.getStarBrightness(partialTicks);
		if (starBrightness > 0.0F && celestialObject != null) {
			renderStars_cached(matrixStack, alphaBase * starBrightness);
		}
		
		// enable texture with alpha blending
		RenderSystem.enableTexture();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.disableAlphaTest();
		
		// Star
		/*
		renderStar(tessellator, textureStar, starBrightness,
		           255 / 255.0F,
		           194 / 255.0F,
		           180 / 255.0F,
		           1.0F );
		/**/
		
		// CelestialObject
		/*
		{
			final double planetScale = 10.0D;
			final double planetRange = 140.0D;
			final float planetRotation = (float) (world.getSpawnPoint().getZ() - mc.player.getPosZ()) * 0.1F;
			
			RenderSystem.pushMatrix();
			RenderSystem.scalef(0.6F, 0.6F, 0.6F);
			RenderSystem.rotatef(planetRotation, 1.0F, 0.0F, 0.0F);
			RenderSystem.rotatef(190F, 1.0F, 0.0F, 0.0F);
			
			Minecraft.getInstance().getTextureManager().bindTexture(texturePlanet);
			
			// world.getMoonPhase();
			final BufferBuilder vertexBuffer = tessellator.getBuffer();
			vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
			vertexBuffer.pos(-planetScale, planetRange, -planetScale).tex(0, 1).color(1.0F, 0.0F, 1.0F, 1.0F).endVertex();
			vertexBuffer.pos( planetScale, planetRange, -planetScale).tex(1, 1).color(1.0F, 0.0F, 1.0F, 1.0F).endVertex();
			vertexBuffer.pos( planetScale, planetRange,  planetScale).tex(1, 0).color(1.0F, 0.0F, 1.0F, 1.0F).endVertex();
			vertexBuffer.pos(-planetScale, planetRange,  planetScale).tex(0, 0).color(1.0F, 0.0F, 1.0F, 1.0F).endVertex();
			tessellator.draw();
			RenderSystem.scalef(1.0F, 1.0F, 1.0F);
			RenderSystem.popMatrix();
		}
		/**/
		
		// Planets
		if (celestialObject != null && celestialObject.opacityCelestialObjects > 0.0F) {
			final Vector3 vectorPlayer = GlobalRegionManager.getUniversalCoordinates(celestialObject, vec3Player.x, vec3Player.y, vec3Player.z);
			for (final CelestialObject celestialObjectChild : CelestialObjectManager.getRenderChildren(celestialObject.id)) {
				renderCelestialObject(tessellator,
				                      celestialObjectChild,
				                      celestialObject.opacityCelestialObjects,
				                      vectorPlayer);
			}
		}
		
		// final double playerAltitude = mc.player.getPositionEyes(partialTicks).yCoord - world.getHorizon();
		
		// stratosphere box
		/*
		float var10;
		float var11;
		float var12;
		if (playerAltitude < 0.0D) {
			RenderSystem.pushMatrix();
			RenderSystem.translatef(0.0F, 12.0F, 0.0F);
			RenderSystem.callList(callListLowerPlane);
			RenderSystem.popMatrix();
			var10 = 1.0F;
			var11 = -((float) (playerAltitude + 65.0D));
			var12 = -var10;
			RenderSystem.color(255, 128, 0, 255);
			final BufferBuilder vertexBuffer = tessellator.getBuffer();
			vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
			vertexBuffer.pos(-var10, var11,  var10).endVertex();
			vertexBuffer.pos( var10, var11,  var10).endVertex();
			vertexBuffer.pos( var10, var12,  var10).endVertex();
			vertexBuffer.pos(-var10, var12,  var10).endVertex();
			vertexBuffer.pos(-var10, var12, -var10).endVertex();
			vertexBuffer.pos( var10, var12, -var10).endVertex();
			vertexBuffer.pos( var10, var11, -var10).endVertex();
			vertexBuffer.pos(-var10, var11, -var10).endVertex();
			vertexBuffer.pos( var10, var12, -var10).endVertex();
			vertexBuffer.pos( var10, var12,  var10).endVertex();
			vertexBuffer.pos( var10, var11,  var10).endVertex();
			vertexBuffer.pos( var10, var11, -var10).endVertex();
			vertexBuffer.pos(-var10, var11, -var10).endVertex();
			vertexBuffer.pos(-var10, var11,  var10).endVertex();
			vertexBuffer.pos(-var10, var12,  var10).endVertex();
			vertexBuffer.pos(-var10, var12, -var10).endVertex();
			vertexBuffer.pos(-var10, var12, -var10).endVertex();
			vertexBuffer.pos(-var10, var12,  var10).endVertex();
			vertexBuffer.pos( var10, var12,  var10).endVertex();
			vertexBuffer.pos( var10, var12, -var10).endVertex();
			tessellator.draw();
		}
		/**/
		/*
		// draw lower sky plane relative to horizon
		RenderSystem.pushMatrix();
		Minecraft.getInstance().getTextureManager().bindTexture(texturePlanet);
		
		// RenderSystem.translatef(0.0F, (float)(16.0D - playerAltitude), 0.0F);
		RenderSystem.callList(callListLowerPlane);
		RenderSystem.popMatrix();
		/**/
		
		
		RenderSystem.disableBlend();
		RenderSystem.enableAlphaTest();
		RenderSystem.enableFog();
		
		RenderSystem.enableTexture();
		RenderSystem.depthMask(true);
	}
	
	// renderSkyBox is loosely inspired by vanilla sky rendering in The End dimension (WorldRenderer::renderSkyEnd)
	private static void renderSkyBox(@Nonnull final Tessellator tessellator, @Nonnull final MatrixStack matrixStack,
	                                 @Nonnull final ResourceLocation[] textureSkyBox, final float brightness, final int countTextureRepeat) {
		final BufferBuilder bufferbuilder = tessellator.getBuffer();
		final float maxUV = countTextureRepeat * 1.0F;
		
		RenderSystem.disableFog();
		RenderSystem.disableAlphaTest();
		RenderSystem.enableTexture();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
		
		final Matrix4f matrix4f = matrixStack.getLast().getMatrix();
		
		// bottom
		Minecraft.getInstance().getTextureManager().bindTexture(textureSkyBox[0]);
		bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F,  0.0F).endVertex();
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F, maxUV).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV, maxUV).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV,  0.0F).endVertex();
		tessellator.draw();
		
		// front
		if (textureSkyBox.length > 1) {
			Minecraft.getInstance().getTextureManager().bindTexture(textureSkyBox[1]);
		}
		bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F,  0.0F).endVertex();
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F, maxUV).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV, maxUV).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV,  0.0F).endVertex();
		tessellator.draw();
		
		// back
		if (textureSkyBox.length > 1) {
			Minecraft.getInstance().getTextureManager().bindTexture(textureSkyBox[2]);
		}
		bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F,  0.0F).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F, maxUV).endVertex();
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV, maxUV).endVertex();
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV,  0.0F).endVertex();
		tessellator.draw();
		
		// top
		if (textureSkyBox.length > 1) {
			Minecraft.getInstance().getTextureManager().bindTexture(textureSkyBox[3]);
		}
		bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F, maxUV).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV, maxUV).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV,  0.0F).endVertex();
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F,  0.0F).endVertex();
		tessellator.draw();
		
		// right
		if (textureSkyBox.length > 1) {
			Minecraft.getInstance().getTextureManager().bindTexture(textureSkyBox[4]);
		}
		bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F,  0.0F).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F, maxUV).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV, maxUV).endVertex();
		bufferbuilder.pos(matrix4f,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV,  0.0F).endVertex();
		tessellator.draw();
		
		// left
		if (textureSkyBox.length > 1) {
			Minecraft.getInstance().getTextureManager().bindTexture(textureSkyBox[5]);
		}
		bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F,  0.0F).endVertex();
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex( 0.0F, maxUV).endVertex();
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV, maxUV).endVertex();
		bufferbuilder.pos(matrix4f, -BOX_RENDER_RANGE,  BOX_RENDER_RANGE, -BOX_RENDER_RANGE).color(brightness, brightness, brightness, 1.0F).tex(maxUV,  0.0F).endVertex();
		tessellator.draw();
		
		RenderSystem.enableAlphaTest();
	}
	
	private static void renderStar(@Nonnull final Tessellator tessellator, @Nonnull final ResourceLocation texture, final float brightness,
	                               final float red, final float green, final float blue, final float alpha) {
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		
		final double starScale = 40.0D;     // Vanilla is 30.0D
		final double starRange = 150.0D;    // Vanilla is 100.0D
		final float celestialAngle_rad = 0.3F; // Vanilla is world.getCelestialAngle(partialTicks)
		final float redActual   = red * brightness;
		final float greenActual = green * brightness;
		final float blueActual  = blue * brightness;
		
		RenderSystem.pushMatrix();
		RenderSystem.rotatef(-90.0F, 0.0F, 1.0F, 0.0F);
		RenderSystem.rotatef(celestialAngle_rad * 360.0F, 1.0F, 0.0F, 0.0F);
		
		// auras
		double size;
		RenderSystem.disableTexture();
		
		// Small aura
		vertexBuffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
		vertexBuffer.pos(0.0D, 100.0D, 0.0D).color(redActual, greenActual, blueActual, alpha * 2.0F / brightness).endVertex();
		size = starScale * 1.125D;
		vertexBuffer.pos(-size       , starRange, -size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(    0       , starRange, -size * 1.5D).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos( size       , starRange, -size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos( size * 1.5D, starRange,     0       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos( size       , starRange,  size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(    0       , starRange,  size * 1.5D).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(-size       , starRange,  size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(-size * 1.5D, starRange,     0       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(-size       , starRange, -size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		tessellator.draw();
		
		// Large aura
		vertexBuffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
		vertexBuffer.pos(0.0D, 100.0D, 0.0D).color(redActual, greenActual, blueActual, alpha * brightness).endVertex();
		size = starScale * 1.250D;
		vertexBuffer.pos(-size       , starRange, -size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(    0       , starRange, -size * 1.5D).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos( size       , starRange, -size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos( size * 1.5D, starRange,     0       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos( size       , starRange,  size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(    0       , starRange,  size * 1.5D).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(-size       , starRange,  size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(-size * 1.5D, starRange,     0       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		vertexBuffer.pos(-size       , starRange, -size       ).color(redActual, greenActual, blueActual, 0.01F).endVertex();
		tessellator.draw();
		
		// Texture
		RenderSystem.enableTexture();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.disableAlphaTest();
		
		Minecraft.getInstance().getTextureManager().bindTexture(texture);
		
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		vertexBuffer.pos(-starScale, starRange, -starScale).color(redActual, greenActual, blueActual, alpha).tex(0.0F, 0.0F).endVertex();
		vertexBuffer.pos( starScale, starRange, -starScale).color(redActual, greenActual, blueActual, alpha).tex(1.0F, 0.0F).endVertex();
		vertexBuffer.pos( starScale, starRange,  starScale).color(redActual, greenActual, blueActual, alpha).tex(1.0F, 1.0F).endVertex();
		vertexBuffer.pos(-starScale, starRange,  starScale).color(redActual, greenActual, blueActual, alpha).tex(0.0F, 1.0F).endVertex();
		tessellator.draw();
		
		RenderSystem.popMatrix();
	}
	
	private static void renderCelestialObject(final Tessellator tessellator, final CelestialObject celestialObject,
	                                          final float alphaSky, final Vector3 vectorPlayer) {
		// @TODO compute relative coordinates for rendering on celestialObject
		
		// get universal coordinates
		final Vector3 vectorCenter = GlobalRegionManager.getUniversalCoordinates(
				celestialObject,
		        celestialObject.dimensionCenterX,
		        64,
		        celestialObject.dimensionCenterZ );
		final Vector3 vectorBorderPos = GlobalRegionManager.getUniversalCoordinates(
				celestialObject,
				celestialObject.dimensionCenterX + celestialObject.borderRadiusX,
				64,
				celestialObject.dimensionCenterZ + celestialObject.borderRadiusZ );
		if (vectorCenter == null || vectorBorderPos == null) {// probably an invalid celestial object tree
			return;
		}
		final double borderRadiusX = vectorBorderPos.x - vectorCenter.x;
		final double borderRadiusZ = vectorBorderPos.z - vectorCenter.z;
		
		// compute distances
		final double distanceToBorder;
		{
			final double dx = Math.abs(vectorPlayer.x - vectorCenter.x) - borderRadiusX;
			final double dz = Math.abs(vectorPlayer.z - vectorCenter.z) - borderRadiusZ;
			// are we in orbit?
			if ((dx <= 0.0D) && (dz <= 0.0D)) {
				distanceToBorder = 0.0D;
			} else {
				// do the maths
				final double dxOutside = Math.max(0.0D, dx);
				final double dzOutside = Math.max(0.0D, dz);
				distanceToBorder = Math.sqrt(dxOutside * dxOutside + dzOutside * dzOutside);
			}
		}
		
		final double distanceToCenterX = vectorCenter.x - vectorPlayer.x;
		final double distanceToCenterZ = vectorCenter.z - vectorPlayer.z;
		final double distanceToCenter = Math.sqrt(distanceToCenterX * distanceToCenterX + distanceToCenterZ * distanceToCenterZ);
		
		// transition values
		// distance              far   approaching  orbit
		// world border         1.000     1.000     1.000
		// PLANET_FAR           1.000     1.000     1.000
		// PLANET_APPROACHING   0.000     1.000     1.000
		// PLANET_ORBIT         0.000     0.000     1.000
		// in orbit             0.000     0.000     0.000
		final double transitionFar         = (Math.max(PLANET_APPROACHING, Math.min(PLANET_FAR, distanceToBorder)) - PLANET_APPROACHING) / (PLANET_FAR - PLANET_APPROACHING);
		final double transitionApproaching = (Math.max(PLANET_ORBIT, Math.min(PLANET_APPROACHING, distanceToBorder)) - PLANET_ORBIT) / (PLANET_APPROACHING - PLANET_ORBIT);
		final double transitionOrbit       = Math.max(0.0D, Math.min(PLANET_ORBIT, distanceToBorder)) / PLANET_ORBIT;
		
		// relative position above celestialObject
		final double offsetX = (1.0 - transitionOrbit) * (distanceToCenterX / borderRadiusX);
		final double offsetZ = (1.0 - transitionOrbit) * (distanceToCenterZ / borderRadiusZ);
		
		// simulating a non-planar universe...
		// TODO MC1.15 celestial rendering customization
		final double planetY_far = 0; // (celestialObject.dimensionId + 99 % 100 - 50) * Math.log(distanceToCenter) / 1.0D;
		final double planetY = planetY_far * transitionApproaching;
		
		// render range is only used for Z-ordering
		double renderRange = 9.0D + 0.5D * (distanceToCenter / Math.max(borderRadiusX, borderRadiusZ));
		
		// render size is 1 at space border range
		// render size is 10 at approaching range
		// render size is 90 at orbit range
		// render size is min(1000, celestialObject border) at orbit range
		final double renderSize = 5.00D / 1000.0D * Math.min(1000.0D, Math.max(borderRadiusX, borderRadiusZ)) * (1.0D - transitionOrbit)
								+ 2.50D * (transitionOrbit < 1.0D ? transitionOrbit : (1.0D - transitionApproaching))
								+ 0.25D * (transitionApproaching < 1.0D ? transitionApproaching : (1.0D - transitionFar))
								+ 0.10D * transitionFar;
		
		// angles
		final double angleH = Math.atan2(distanceToCenterX, distanceToCenterZ);
		final double angleV_far = Math.atan2(distanceToCenter, planetY);
		final double angleV = Math.PI * (1.0D - transitionOrbit) + angleV_far * transitionOrbit;
		// TODO MC1.15 celestial rendering customization
		// final double angleS = 0.15D * celestialObject.dimensionId * transitionApproaching // + (world.getGameTime() + partialTicks) * Math.PI / 6000.0D;
		// 					+ angleH * (1.0D - transitionApproaching);
		final double angleS = 0.15D * transitionApproaching // + (world.getGameTime() + partialTicks) * Math.PI / 6000.0D;
		                    + angleH * (1.0D - transitionApproaching);
		
		if ( WarpDriveConfig.LOGGING_RENDERING
		  && (celestialObject.isSpace() || celestialObject.isHyperspace())
		  && (System.currentTimeMillis() / 10) % 100 == 0) {
			WarpDrive.logger.info(String.format("transition Far %.2f Approaching %.2f Orbit %.2f distanceToCenter %.3f %.3f offset %.3f %.3f angle H %.3f V_far %.3f V %.3f S %.3f",
				transitionFar, transitionApproaching, transitionOrbit, distanceToCenterX, distanceToCenterZ, offsetX, offsetZ, angleH, angleV_far, angleV, angleS));
		}
		
		// pre-computations
		final double sinH = Math.sin(angleH);
		final double cosH = Math.cos(angleH);
		final double sinV = Math.sin(angleV);
		final double cosV = Math.cos(angleV);
		final double sinS = Math.sin(angleS);
		final double cosS = Math.cos(angleS);
		
		RenderSystem.pushMatrix();
		
		// RenderSystem.enableBlend();    // by caller
		final double time = System.currentTimeMillis() / 1000.0D;
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		for (final RenderData renderData : celestialObject.setRenderData) {
			// compute texture offsets for clouds animation 
			final float offsetU = (float) ( Math.signum(renderData.periodU) * ((time / Math.abs(renderData.periodU)) % 1.0D) );
			final float offsetV = (float) ( Math.signum(renderData.periodV) * ((time / Math.abs(renderData.periodV)) % 1.0D) );
			
			// apply rendering parameters
			if (renderData.texture != null) {
				RenderSystem.enableTexture();
				Minecraft.getInstance().getTextureManager().bindTexture(renderData.resourceLocation);
				vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
			} else {
				RenderSystem.disableTexture();
				vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			}
			if (renderData.isAdditive) {
				RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
			} else {
				RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			}
			
			// draw current layer
			for (int indexVertex = 0; indexVertex < 4; indexVertex++) {
				final double offset1 = ((indexVertex & 2) - 1) * renderSize;
				final double offset2 = ((indexVertex + 1 & 2) - 1) * renderSize;
				final double valV = offset1 * cosS - offset2 * sinS;
				final double valH = offset2 * cosS + offset1 * sinS;
				final double y = valV * sinV + renderRange * cosV;
				final double valD = renderRange * sinV - valV * cosV;
				final double x = valD * sinH - valH * cosH + renderSize * offsetX;
				final double z = valH * sinH + valD * cosH + renderSize * offsetZ;
				vertexBuffer.pos(x, y, z)
				            .color(renderData.red, renderData.green, renderData.blue, renderData.alpha * alphaSky);
				if (renderData.texture != null) {
					vertexBuffer.tex((indexVertex & 2) / 2 + offsetU, (indexVertex + 1 & 2) / 2 + offsetV);
				}
				vertexBuffer.endVertex();
			}
			tessellator.draw();
			
			// slight offset to get volumetric illusion
			renderRange -= 0.25D;
		}
		
		// restore settings
		RenderSystem.enableTexture();
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		
		RenderSystem.popMatrix();
	}
	
	private void renderStars_direct(@Nonnull final BufferBuilder bufferBuilder, final float brightness) {
		final Random rand = new Random(10842L);
		final boolean hasMoreStars = rand.nextBoolean() || rand.nextBoolean();
		bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		
		final double renderRangeMax = 10.0D;
		for (int indexStars = 0; indexStars < (hasMoreStars ? 20000 : 2000); indexStars++) {
			double randomX;
			double randomY;
			double randomZ;
			double randomLength;
			do {
				randomX = rand.nextDouble() * 2.0D - 1.0D;
				randomY = rand.nextDouble() * 2.0D - 1.0D;
				randomZ = rand.nextDouble() * 2.0D - 1.0D;
				randomLength = randomX * randomX + randomY * randomY + randomZ * randomZ;
			} while (randomLength >= 1.0D || randomLength <= 0.90D);
			
			final double renderSize = 0.020F + 0.0025F * Math.log(1.1D - rand.nextDouble());
			
			// forcing Z-order
			randomLength = 1.0D / Math.sqrt(randomLength);
			randomX *= randomLength;
			randomY *= randomLength;
			randomZ *= randomLength;
			
			// scaling
			final double x0 = randomX * renderRangeMax;
			final double y0 = randomY * renderRangeMax;
			final double z0 = randomZ * renderRangeMax;
			
			// angles
			final double angleH = Math.atan2(randomX, randomZ);
			final double angleV = Math.atan2(Math.sqrt(randomX * randomX + randomZ * randomZ), randomY);
			final double angleS = rand.nextDouble() * Math.PI * 2.0D;
			
			// colorization
			final int rgb = getStarColorRGB(rand);
			final float fRed   = brightness * ((rgb >> 16) & 0xFF) / 255.0F;
			final float fGreen = brightness * ((rgb >> 8) & 0xFF) / 255.0F;
			final float fBlue  = brightness * (rgb & 0xFF) / 255.0F;
			final float fAlpha = 1.0F;
			
			// pre-computations
			final double sinH = Math.sin(angleH);
			final double cosH = Math.cos(angleH);
			final double sinV = Math.sin(angleV);
			final double cosV = Math.cos(angleV);
			final double sinS = Math.sin(angleS);
			final double cosS = Math.cos(angleS);
			
			for (int indexVertex = 0; indexVertex < 4; indexVertex++) {
				final double valZero = 0.0D;
				final double offset1 = ((indexVertex     & 2) - 1) * renderSize;
				final double offset2 = ((indexVertex + 1 & 2) - 1) * renderSize;
				final double valV = offset1 * cosS - offset2 * sinS;
				final double valH = offset2 * cosS + offset1 * sinS;
				final double y1 = valV * sinV + valZero * cosV;
				final double valD = valZero * sinV - valV * cosV;
				final double x1 = valD * sinH - valH * cosH;
				final double z1 = valH * sinH + valD * cosH;
				bufferBuilder.pos(x0 + x1, y0 + y1, z0 + z1).color(fRed, fGreen, fBlue, fAlpha).endVertex();
			}
		}
		bufferBuilder.finishDrawing();
		
	}
	
	private void renderStars_cached(@Nonnull final MatrixStack matrixStack, final float brightness) {
		if (Math.abs(starBrightness - brightness) > ALPHA_TOLERANCE) {
			starBrightness = brightness;
			RenderSystem.pushMatrix();
			
			final Tessellator tessellator = Tessellator.getInstance();
			final BufferBuilder bufferBuilder = tessellator.getBuffer();
			vboStars = new VertexBuffer(DefaultVertexFormats.POSITION_COLOR);
			renderStars_direct(bufferBuilder, brightness);
			vboStars.upload(bufferBuilder);
			
			RenderSystem.popMatrix();
		}
		
		RenderSystem.enableFog();
		matrixStack.push();
		
		matrixStack.translate(0.0D, 128.0D, 0.0D);
		vboStars.bindBuffer();
		vertexFormatStars.setupBufferState(0L);
		vboStars.draw(matrixStack.getLast().getMatrix(), 7);
		VertexBuffer.unbindBuffer();
		vertexFormatStars.clearBufferState();
		
		matrixStack.pop();
	}
	
	// colorization loosely inspired from Hertzsprung-Russell diagram
	// (we're using it for non-star objects too, so yeah...)
	private static int getStarColorRGB(@Nonnull final Random rand) {
		final double colorType = rand.nextDouble();
		final float hue;
		final float saturation;
		float brightness = 1.0F - 0.8F * rand.nextFloat();  // distance effect
		
		if (colorType <= 0.08D) {// 8% light blue (young star)
			hue = 0.48F + 0.08F * rand.nextFloat();
			saturation = 0.18F + 0.22F * rand.nextFloat();
			
		} else if (colorType <= 0.24D) {// 22% pure white (early age)
			hue = 0.126F + 0.040F * rand.nextFloat();
			saturation = 0.00F + 0.15F * rand.nextFloat();
			brightness *= 0.95F;
			
		} else if (colorType <= 0.45D) {// 21% yellow white
			hue = 0.126F + 0.040F * rand.nextFloat();
			saturation = 0.15F + 0.15F * rand.nextFloat();
			brightness *= 0.90F;
			
		} else if (colorType <= 0.67D) {// 22% yellow
			hue = 0.126F + 0.040F * rand.nextFloat();
			saturation = 0.80F + 0.15F * rand.nextFloat();
			if (rand.nextInt(3) == 1) {// yellow giant
				brightness *= 0.90F;
			} else {
				brightness *= 0.85F;
			}
			
		} else if (colorType <= 0.92D) {// 25% orange
			hue = 0.055F + 0.055F * rand.nextFloat();
			saturation = 0.85F + 0.15F * rand.nextFloat();
			if (rand.nextInt(3) == 1) {// (orange giant)
				brightness *= 0.90F;
			} else {
				brightness *= 0.80F;
			}
			
		} else {// red (mostly giants)
			hue = 0.95F + 0.05F * rand.nextFloat();
			if (rand.nextInt(3) == 1) {// (red giant)
				saturation = 0.80F + 0.20F * rand.nextFloat();
				brightness *= 0.95F;
			} else {
				saturation = 0.70F + 0.20F * rand.nextFloat();
				brightness *= 0.65F;
			}
		}
		return Color.HSBtoRGB(hue, saturation, brightness);
	}
}
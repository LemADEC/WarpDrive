package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.energy.TileEntityEnanReactorCore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.model.data.EmptyModelData;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

public class TileEntityEnanReactorCoreRenderer extends TileEntityRenderer<TileEntityEnanReactorCore> {

	// the reactor is emissive (self-illuminated), so we render it fully lit (sky 15, block 15) like the 1.12.2 getCombinedLight(pos, 15)
	private static final int LIGHT_FULLBRIGHT = 0x00F0_00F0;

	private static List<BakedQuad> quadsCore;
	private static List<BakedQuad> quadsMatter;
	private static List<BakedQuad> quadsSurface;
	private static List<BakedQuad> quadsShield;
	
	public TileEntityEnanReactorCoreRenderer(@Nonnull final TileEntityRendererDispatcher rendererDispatcher) {
		super(rendererDispatcher);
	}
	
	// Grabs the baked quads of our 4 OBJ models, lazily on the first render.
	// The forge:obj loader already baked them and stitched their textures into the block atlas.
	private void updateQuads() {
		final ModelManager modelManager = Minecraft.getInstance().getModelManager();
		quadsCore    = getQuads(modelManager, "block/energy/reactor_core");
		quadsMatter  = getQuads(modelManager, "block/energy/reactor_matter");
		quadsSurface = getQuads(modelManager, "block/energy/reactor_surface");
		quadsShield  = getQuads(modelManager, "block/energy/reactor_shield");
	}
	
	@Nonnull
	private static List<BakedQuad> getQuads(@Nonnull final ModelManager modelManager, @Nonnull final String path) {
		final IBakedModel bakedModel = modelManager.getModel(new ResourceLocation(WarpDrive.MODID, path));
		if ( bakedModel == null
		  || bakedModel == modelManager.getMissingModel() ) {
			WarpDrive.logger.warn(String.format("Reactor model %s failed to bake (%s) - torus won't render",
			                                    path, bakedModel == null ? "null" : "missing" ));
			return java.util.Collections.emptyList();
		}
		return bakedModel.getQuads(null, null, new Random(0L), EmptyModelData.INSTANCE);
	}
	
	// Replays a list of baked quads through the current matrix into the given vertex builder.
	private static void renderQuads(@Nonnull final List<BakedQuad> quads, @Nonnull final MatrixStack matrixStack,
	                                @Nonnull final IVertexBuilder vertexBuilder, final int combinedLight, final int combinedOverlay) {
		final MatrixStack.Entry matrixEntry = matrixStack.getLast();
		for (final BakedQuad quad : quads) {
			// Reads the BLOCK-format quad data and emits position + color + uv + overlay + lightmap + (matrix-transformed) normal,
			// so the RenderType handles depth/cull/blend correctly.
			vertexBuilder.addQuad(matrixEntry, quad, 1.0F, 1.0F, 1.0F, combinedLight, combinedOverlay);
		}
	}
	
	@Override
	public void render(@Nonnull final TileEntityEnanReactorCore tileEntityEnanReactorCore,
	                   final float partialTicks, @Nonnull final MatrixStack matrixStack,
	                   @Nonnull final IRenderTypeBuffer renderTypeBuffer, final int combinedLightIn, final int combinedOverlayIn) {
		if ( tileEntityEnanReactorCore.getWorld() == null
		  || !tileEntityEnanReactorCore.getWorld().isAreaLoaded(tileEntityEnanReactorCore.getPos(), 3) ) {
			return;
		}
		if (quadsCore == null) {
			updateQuads();
		}
		if (quadsCore.isEmpty()) {
			return; // Model is not baked (yet) -> nothing to render
		}
		
		final double yCore = tileEntityEnanReactorCore.client_yCore + partialTicks * tileEntityEnanReactorCore.client_yCoreSpeed_mPerTick;
		matrixStack.push();
		matrixStack.translate(0.5D, yCore, 0.5D);
		
		// Render the core (opaque, lit by the surrounding world)
		final float rotationCore = tileEntityEnanReactorCore.client_rotationCore_deg + partialTicks * tileEntityEnanReactorCore.client_rotationSpeedCore_degPerTick;
		matrixStack.push();
		matrixStack.rotate(Vector3f.YP.rotationDegrees(rotationCore));
		renderQuads(quadsCore, matrixStack,
		            renderTypeBuffer.getBuffer(RenderType.getEntitySolid(PlayerContainer.LOCATION_BLOCKS_TEXTURE)),
		            LIGHT_FULLBRIGHT, combinedOverlayIn);
		matrixStack.pop();
		
		// Render the matter plasma + its transparent surface
		if (tileEntityEnanReactorCore.client_radiusMatter_m > 0.0F) {
			final float radiusMatter = tileEntityEnanReactorCore.client_radiusMatter_m + partialTicks * tileEntityEnanReactorCore.client_radiusSpeedMatter_mPerTick;
			final float heightMatter = Math.max(1.0F, radiusMatter * 1.70F);
			final IVertexBuilder vertexBuilderTranslucent = renderTypeBuffer.getBuffer(ReactorRenderType.REACTOR_EMISSIVE);
			
			// Matter model, slightly smaller
			matrixStack.push();
			matrixStack.scale(radiusMatter * 0.95F, heightMatter * 0.90F, radiusMatter * 0.95F);
			final float rotationMatter = tileEntityEnanReactorCore.client_rotationMatter_deg + (partialTicks - 0.75F) * tileEntityEnanReactorCore.client_rotationSpeedMatter_degPerTick;
			matrixStack.rotate(Vector3f.YP.rotationDegrees(rotationMatter));
			renderQuads(quadsMatter, matrixStack, vertexBuilderTranslucent, LIGHT_FULLBRIGHT, combinedOverlayIn);
			matrixStack.pop();
			
			// Surface model (transparent surface)
			matrixStack.push();
			matrixStack.scale(radiusMatter, heightMatter, radiusMatter);
			final float rotationSurface = tileEntityEnanReactorCore.client_rotationSurface_deg + partialTicks * tileEntityEnanReactorCore.client_rotationSpeedSurface_degPerTick;
			matrixStack.rotate(Vector3f.YP.rotationDegrees(rotationSurface));
			renderQuads(quadsSurface, matrixStack, vertexBuilderTranslucent, LIGHT_FULLBRIGHT, combinedOverlayIn);
			matrixStack.pop();
		}
		
		// Render the shield, slightly bigger
		if (tileEntityEnanReactorCore.client_radiusShield_m > 0.0F) {
			final float radiusShield = tileEntityEnanReactorCore.client_radiusShield_m + partialTicks * tileEntityEnanReactorCore.client_radiusSpeedShield_mPerTick;
			final float heightShield = Math.max(0.75F, radiusShield * 0.70F);
			matrixStack.push();
			matrixStack.scale(radiusShield, heightShield, radiusShield);
			matrixStack.rotate(Vector3f.YP.rotationDegrees(rotationCore));
			renderQuads(quadsShield, matrixStack,
			            renderTypeBuffer.getBuffer(ReactorRenderType.REACTOR_EMISSIVE),
			            LIGHT_FULLBRIGHT, combinedOverlayIn);
			matrixStack.pop();
		}
		
		matrixStack.pop();
	}
	
	@Override
	public boolean isGlobalRenderer(@Nonnull final TileEntityEnanReactorCore tileEntityEnanReactorCore) {
		return true;
	}
}

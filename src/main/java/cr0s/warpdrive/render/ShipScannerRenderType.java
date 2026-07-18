package cr0s.warpdrive.render;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.inventory.container.PlayerContainer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

// Additive glowing overlay drawn THROUGH walls (blendFunc SRC_ALPHA/ONE + disableDepth + disableLighting).
// - CULL_ENABLED: the model draws both exterior AND interior faces so the frame is visible from inside and outside;
//   culling shows one face per viewpoint (still see-through: near exterior + far interior).
// - SRC_ALPHA_ADDITIVE_TRANSPARENCY (custom): additive, but scaled by source alpha -> the texture's vertical alpha
//   gradient shows as a vertical fade (vanilla ADDITIVE_TRANSPARENCY is ONE/ONE and would flatten it).
// - DEPTH_ALWAYS (GL_ALWAYS): depth test always passes -> the scan frame is visible through blocks (intentional overlay).
// - COLOR_WRITE: don't pollute the depth buffer.
// - DIFFUSE_LIGHTING_DISABLED + rendered fully-bright: uniform glow.
// Extends RenderType only to reach the protected RenderState shards + the public makeType factory; never instantiated.
public abstract class ShipScannerRenderType extends RenderType {
	
	private static final RenderState.TransparencyState SRC_ALPHA_ADDITIVE_TRANSPARENCY = new RenderState.TransparencyState(
			"warpdrive:src_alpha_additive",
			() -> {
				RenderSystem.enableBlend();
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			},
			() -> {
				RenderSystem.disableBlend();
				RenderSystem.defaultBlendFunc();
			} );
	
	public static final RenderType SHIP_SCANNER = makeType(
			"warpdrive:ship_scanner_border",
			DefaultVertexFormats.ENTITY, 7, 256, false, false,
			RenderType.State.getBuilder()
					.texture(new RenderState.TextureState(PlayerContainer.LOCATION_BLOCKS_TEXTURE, false, false))
					.transparency(SRC_ALPHA_ADDITIVE_TRANSPARENCY)
					.diffuseLighting(DIFFUSE_LIGHTING_DISABLED)
					.alpha(DEFAULT_ALPHA)
					.cull(CULL_ENABLED)
					.depthTest(DEPTH_ALWAYS)
					.writeMask(COLOR_WRITE)
					.lightmap(LIGHTMAP_ENABLED)
					.overlay(OVERLAY_ENABLED)
					.build(true) );
	
	private ShipScannerRenderType(final String name, final VertexFormat format, final int drawMode, final int bufferSize,
	                              final boolean useDelegate, final boolean needsSorting, final Runnable setupTask, final Runnable clearTask) {
		super(name, format, drawMode, bufferSize, useDelegate, needsSorting, setupTask, clearTask);
	}
}

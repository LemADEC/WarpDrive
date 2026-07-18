package cr0s.warpdrive.render;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.inventory.container.PlayerContainer;

// Extends RenderType only to reach the protected RenderState shards (TRANSLUCENT_TRANSPARENCY, DIFFUSE_LIGHTING_DISABLED,
// CULL_DISABLED, ...) and the public makeType factory. Never instantiated - the private constructor just satisfies javac.
public abstract class ReactorRenderType extends RenderType {
	
	// Emissive translucent - same as RenderType.getEntityTranslucent() but with diffuse lighting DISABLED
	// (blend on + disableLighting, cull left enabled, depth untouched).
	// Diffuse-off keeps the model uniformly glowing (diffuse darkened faces per-normal as the torus rotated).
	// Cull stays ENABLED.
	// Depth write stays ON (default), so the torus/shield correctly occlude the stabilization lasers behind them.
	// needsSorting=false: enables us to draw translucent in the right order (matter -> surface -> shield) without mixing the shells.
	public static final RenderType REACTOR_EMISSIVE = makeType(
			"warpdrive:reactor_emissive",
			DefaultVertexFormats.ENTITY, 7, 256, true, false,
			RenderType.State.getBuilder()
					.texture(new RenderState.TextureState(PlayerContainer.LOCATION_BLOCKS_TEXTURE, false, false))
					.transparency(TRANSLUCENT_TRANSPARENCY)
					.diffuseLighting(DIFFUSE_LIGHTING_DISABLED)
					.alpha(DEFAULT_ALPHA)
					.cull(CULL_ENABLED)
					.lightmap(LIGHTMAP_ENABLED)
					.overlay(OVERLAY_ENABLED)
					.build(true) );
	
	private ReactorRenderType(final String name, final VertexFormat format, final int drawMode, final int bufferSize,
	                          final boolean useDelegate, final boolean needsSorting, final Runnable setupTask, final Runnable clearTask) {
		super(name, format, drawMode, bufferSize, useDelegate, needsSorting, setupTask, clearTask);
	}
}

package cr0s.warpdrive.render;

import cr0s.warpdrive.block.building.TileEntityShipScanner;

import javax.annotation.Nonnull;
import java.util.List;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

import net.minecraftforge.client.model.data.EmptyModelData;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

public class TileEntityShipScannerRenderer extends TileEntityRenderer<TileEntityShipScanner> {
	
	// Scanner frame is emissive, so we render it fully lit (sky 15, block 15)
	private static final int LIGHT_FULLBRIGHT = 0x00F0_00F0;
	
	// Model is fixed, so we bake it once
	private static List<BakedQuad> bakedQuads;
	
	public TileEntityShipScannerRenderer(@Nonnull final TileEntityRendererDispatcher rendererDispatcher) {
		super(rendererDispatcher);
	}
	
	@Override
	public void render(@Nonnull final TileEntityShipScanner tileEntityShipScanner,
	                   final float partialTicks, @Nonnull final MatrixStack matrixStack,
	                   @Nonnull final IRenderTypeBuffer renderTypeBuffer, final int combinedLightIn, final int combinedOverlayIn) {
		if ( tileEntityShipScanner.getWorld() == null
		  || !tileEntityShipScanner.getWorld().isAreaLoaded(tileEntityShipScanner.getPos(), 1) ) {
			return;
		}
		if (bakedQuads == null) {
			bakedQuads = new BakedModelShipScanner().getQuads(null, null, tileEntityShipScanner.getWorld().rand, EmptyModelData.INSTANCE);
		}
		
		// Model is already in the TESR matrix (block-local coordinates / corner origin), so there's no extra translation.
		final IVertexBuilder vertexBuilder = renderTypeBuffer.getBuffer(ShipScannerRenderType.SHIP_SCANNER);
		final MatrixStack.Entry matrixEntry = matrixStack.getLast();
		for (final BakedQuad bakedQuad : bakedQuads) {
			// Texture defines color and gradient, not the model, hence using 7-arg addQuad with white color & no mulcolor
			// Full-bright + additive + through walls via the RenderType.
			vertexBuilder.addQuad(matrixEntry, bakedQuad, 1.0F, 1.0F, 1.0F, LIGHT_FULLBRIGHT, combinedOverlayIn);
		}
	}
	
	@Override
	public boolean isGlobalRenderer(@Nonnull final TileEntityShipScanner tileEntityShipScanner) {
		return true;
	}
}

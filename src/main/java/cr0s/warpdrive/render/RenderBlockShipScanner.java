package cr0s.warpdrive.render;

import cr0s.warpdrive.block.building.BlockShipScanner;
import cr0s.warpdrive.block.building.TileEntityShipScanner;
import cr0s.warpdrive.config.Dictionary;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

public class RenderBlockShipScanner implements ISimpleBlockRenderingHandler {
	
	public static int renderId = 0;
	public static RenderBlockShipScanner instance = new RenderBlockShipScanner();
	
	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
		final float intensity = 1.0F;
		
		// simplified copy from RenderBlocks.renderBlockAsItem()
		final Tessellator tessellator = Tessellator.instance;
		
		if (renderer.useInventoryTint) {
			final int color = block.getRenderColor(metadata);
			final float red = (float)(color >> 16 & 255) / 255.0F;
			final float green = (float)(color >> 8 & 255) / 255.0F;
			final float blue = (float)(color & 255) / 255.0F;
			GL11.glColor4f(red * intensity, green * intensity, blue * intensity, 1.0F);
		}
		
		renderer.setRenderBoundsFromBlock(block);
		
		block.setBlockBoundsForItemRender();
		renderer.setRenderBoundsFromBlock(block);
		GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
		
		tessellator.startDrawingQuads();
		tessellator.setNormal(0.0F, -1.0F, 0.0F);
		renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 0, metadata));
		tessellator.draw();
		
		tessellator.startDrawingQuads();
		tessellator.setNormal(0.0F, 1.0F, 0.0F);
		renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 1, metadata));
		tessellator.draw();
		
		tessellator.startDrawingQuads();
		tessellator.setNormal(0.0F, 0.0F, -1.0F);
		renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 2, metadata));
		tessellator.draw();
		
		tessellator.startDrawingQuads();
		tessellator.setNormal(0.0F, 0.0F, 1.0F);
		renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 3, metadata));
		tessellator.draw();
		
		tessellator.startDrawingQuads();
		tessellator.setNormal(-1.0F, 0.0F, 0.0F);
		renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 4, metadata));
		tessellator.draw();
		
		tessellator.startDrawingQuads();
		tessellator.setNormal(1.0F, 0.0F, 0.0F);
		renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 5, metadata));
		tessellator.draw();
		
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
	}
	
	@Override
	public boolean renderWorldBlock(IBlockAccess blockAccess, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		if (!(block instanceof BlockShipScanner)) {
			return false;
		}
		final TileEntity tileEntity = blockAccess.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityShipScanner)) {
			return false;
		}
		
		if (BlockShipScanner.passCurrent == 0) {
			// render the controlling block
			int renderType = -1;
			final Block blockCamouflage = ((TileEntityShipScanner) tileEntity).blockCamouflage;
			if (blockCamouflage != null && !Dictionary.BLOCKS_NOCAMOUFLAGE.contains(blockCamouflage)) {
				renderType = blockCamouflage.getRenderType();
			}
			
			return RenderCommons.renderWorldBlockCamouflaged(x, y, z, block, renderer, renderType, blockCamouflage);
		}
		
		
		// check vertical offset
		final Block blockAbove = blockAccess.getBlock(x, y + 1, z);
		final boolean isHidden = !blockAbove.isAir(blockAccess, x, y + 1, z)
		                         && blockAbove.isBlockSolid(blockAccess, x, y + 1, z, blockAccess.getBlockMetadata(x, y + 1, z));
		
		// render borders
		final Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(200); // block.getMixedBrightnessForBlock(blockAccess, x, y, z));
		
		// apply coloring
		final int colorMultiplier = 0xFFFFFF; // block.colorMultiplier(blockAccess, x, y, z);
		final float fRed   = (float) (colorMultiplier >> 16 & 255) / 255.0F;
		final float fGreen = (float) (colorMultiplier >> 8 & 255) / 255.0F;
		final float fBlue  = (float) (colorMultiplier & 255) / 255.0F;
		tessellator.setColorOpaque_F(fRed, fGreen, fBlue);
		
		// get icons
		final IIcon iconBorder = ((BlockShipScanner) block).getBorderIcon();
		
		// pre-compute dimensions
		final int intRadius = 1;
		final double radius = intRadius + 0.0D;
		final int size = 1 + 2 * intRadius;
		
		// pre-compute coordinates
		final double dX_min = x + 0.0D - radius;
		final double dX_max = x + 1.0D + radius;
		final double dY_min = y + (isHidden ? 1.999D : 0.999D);
		final double dY_max = dY_min + 1.0D;
		final double dZ_min = z + 0.0D - radius;
		final double dZ_max = z + 1.0D + radius;
		
		final double dU_min = iconBorder.getMinU();
		final double dU_max = iconBorder.getMaxU();
		
		final double dV_min = iconBorder.getMinV();
		final double dV_max = iconBorder.getMaxV();
		
		// start drawing
		for (int index = 0; index < size; index++) {
			final double offsetMin = index == 0 ? 0.0D : 0.001D;
			final double offsetMax = index == size - 1 ? 0.0D : 0.001D;
			
			// draw exterior faces
			tessellator.addVertexWithUV(dX_min + index + 1, dY_max, dZ_min - offsetMax, dU_max, dV_min);
			tessellator.addVertexWithUV(dX_min + index + 1, dY_min, dZ_min - offsetMax, dU_max, dV_max);
			tessellator.addVertexWithUV(dX_min + index    , dY_min, dZ_min - offsetMin, dU_min, dV_max);
			tessellator.addVertexWithUV(dX_min + index    , dY_max, dZ_min - offsetMin, dU_min, dV_min);
			
			tessellator.addVertexWithUV(dX_max - index - 1, dY_max, dZ_max + offsetMax, dU_max, dV_min);
			tessellator.addVertexWithUV(dX_max - index - 1, dY_min, dZ_max + offsetMax, dU_max, dV_max);
			tessellator.addVertexWithUV(dX_max - index    , dY_min, dZ_max + offsetMin, dU_min, dV_max);
			tessellator.addVertexWithUV(dX_max - index    , dY_max, dZ_max + offsetMin, dU_min, dV_min);
			
			tessellator.addVertexWithUV(dX_min - offsetMin, dY_max, dZ_min + index    , dU_max, dV_min);
			tessellator.addVertexWithUV(dX_min - offsetMin, dY_min, dZ_min + index    , dU_max, dV_max);
			tessellator.addVertexWithUV(dX_min - offsetMax, dY_min, dZ_min + index + 1, dU_min, dV_max);
			tessellator.addVertexWithUV(dX_min - offsetMax, dY_max, dZ_min + index + 1, dU_min, dV_min);
			
			tessellator.addVertexWithUV(dX_max + offsetMin, dY_max, dZ_max - index    , dU_max, dV_min);
			tessellator.addVertexWithUV(dX_max + offsetMin, dY_min, dZ_max - index    , dU_max, dV_max);
			tessellator.addVertexWithUV(dX_max + offsetMax, dY_min, dZ_max - index - 1, dU_min, dV_max);
			tessellator.addVertexWithUV(dX_max + offsetMax, dY_max, dZ_max - index - 1, dU_min, dV_min);
			
			// draw interior faces
			tessellator.addVertexWithUV(dX_min + index    , dY_max, dZ_min + offsetMin, dU_min, dV_min);
			tessellator.addVertexWithUV(dX_min + index    , dY_min, dZ_min + offsetMin, dU_min, dV_max);
			tessellator.addVertexWithUV(dX_min + index + 1, dY_min, dZ_min + offsetMax, dU_max, dV_max);
			tessellator.addVertexWithUV(dX_min + index + 1, dY_max, dZ_min + offsetMax, dU_max, dV_min);
			
			tessellator.addVertexWithUV(dX_max - index    , dY_max, dZ_max - offsetMin, dU_min, dV_min);
			tessellator.addVertexWithUV(dX_max - index    , dY_min, dZ_max - offsetMin, dU_min, dV_max);
			tessellator.addVertexWithUV(dX_max - index - 1, dY_min, dZ_max - offsetMax, dU_max, dV_max);
			tessellator.addVertexWithUV(dX_max - index - 1, dY_max, dZ_max - offsetMax, dU_max, dV_min);
			
			tessellator.addVertexWithUV(dX_min + offsetMax, dY_max, dZ_min + index + 1, dU_min, dV_min);
			tessellator.addVertexWithUV(dX_min + offsetMax, dY_min, dZ_min + index + 1, dU_min, dV_max);
			tessellator.addVertexWithUV(dX_min + offsetMin, dY_min, dZ_min + index    , dU_max, dV_max);
			tessellator.addVertexWithUV(dX_min + offsetMin, dY_max, dZ_min + index    , dU_max, dV_min);
			
			tessellator.addVertexWithUV(dX_max - offsetMax, dY_max, dZ_max - index - 1, dU_min, dV_min);
			tessellator.addVertexWithUV(dX_max - offsetMax, dY_min, dZ_max - index - 1, dU_min, dV_max);
			tessellator.addVertexWithUV(dX_max - offsetMin, dY_min, dZ_max - index    , dU_max, dV_max);
			tessellator.addVertexWithUV(dX_max - offsetMin, dY_max, dZ_max - index    , dU_max, dV_min);
		}
		
		return true;
	}
	
	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return true;
	}
	
	@Override
	public int getRenderId() {
		return renderId;
	}
}

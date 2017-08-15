package cr0s.warpdrive.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

// wrapper to native classes so renderId is non-zero so we don't render faces when player camera is inside the block
public class RenderBlockStandard implements ISimpleBlockRenderingHandler {
	
	public static int renderId = 0;
	public static RenderBlockStandard instance = new RenderBlockStandard();
	
	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
		// not supposed to happen
	}
	
	@Override
	public boolean renderWorldBlock(IBlockAccess blockAccess, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		return renderer.renderStandardBlock(block, x, y, z);
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

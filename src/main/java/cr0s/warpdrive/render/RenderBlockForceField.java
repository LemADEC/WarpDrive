package cr0s.warpdrive.render;

import cr0s.warpdrive.block.forcefield.TileEntityForceField;
import cr0s.warpdrive.config.Dictionary;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

// wrapper to camouflage block
public class RenderBlockForceField implements ISimpleBlockRenderingHandler {
	
	public static int renderId = 0;
	public static RenderBlockForceField instance = new RenderBlockForceField();
	
	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
		// this is not supposed to happen
		//noinspection ConstantConditions
		assert(false);
	}
	
	@Override
	public boolean renderWorldBlock(IBlockAccess blockAccess, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		final TileEntity tileEntity = blockAccess.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityForceField)) {
			return false;
		}
		
		int renderType = -1;
		Block blockCamouflage = ((TileEntityForceField) tileEntity).cache_blockCamouflage;
		if (blockCamouflage != null && !Dictionary.BLOCKS_NOCAMOUFLAGE.contains(blockCamouflage)) {
			renderType = blockCamouflage.getRenderType();
		}
		
		return RenderCommons.renderWorldBlockCamouflaged(x, y, z, block, renderer, renderType, blockCamouflage);
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

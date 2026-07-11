package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockJumpGateCore extends BlockAbstractContainer {
	
	public BlockJumpGateCore(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.movement.jump_gate_core." + enumTier.getName());
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityJumpGateCore();
	}
}
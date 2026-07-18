package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.event.ModelHandler;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

public class BlockEnanReactorCore extends BlockAbstractContainer {
	
	public static final IntegerProperty ENERGY = IntegerProperty.create("energy", 0, 3);
	public static final IntegerProperty INSTABILITY = IntegerProperty.create("stability", 0, 3);
	
	public BlockEnanReactorCore(@Nonnull final String registryName, @Nonnull final EnumTier enumTier) {
		super(getDefaultProperties(null), registryName, enumTier);
		
		setDefaultState(getStateContainer().getBaseState()
				                .with(ENERGY, 0)
				                .with(INSTABILITY, 0)
		               );
	}
	
	@Override
	protected void fillStateContainer(@Nonnull final Builder<Block, BlockState> builder) {
		super.fillStateContainer(builder);
		builder.add(ENERGY);
		builder.add(INSTABILITY);
	}
	
	// TileEntityEnanReactorCoreRenderer requires its 4 OBJ models to be preloaded.
	// The models are shared across advanced and superior tiers, so we only register them once.
	@OnlyIn(Dist.CLIENT)
	@Override
	public void modelInitialisation() {
		super.modelInitialisation();
		
		if (enumTier == EnumTier.ADVANCED) {
			ModelHandler.registerSpecialModel(new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_core"));
			ModelHandler.registerSpecialModel(new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_matter"));
			ModelHandler.registerSpecialModel(new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_surface"));
			ModelHandler.registerSpecialModel(new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_shield"));
		}
	}
}
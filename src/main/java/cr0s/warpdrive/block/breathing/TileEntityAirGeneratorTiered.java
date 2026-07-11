package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.block.TileEntityAbstractEnergyConsumer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.event.ChunkHandler;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class TileEntityAirGeneratorTiered extends TileEntityAbstractEnergyConsumer {
	
	// persistent properties
	// (none)
	
	// computed properties
	private int tickUpdate;
	
	public TileEntityAirGeneratorTiered() {
		super();
		
		peripheralName = "warpdriveAirGenerator";
		doRequireUpgradeToInterface();
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.BREATHING_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()],
		                     4096, 0,
		                     "HV", 2, "HV", 0);
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		tickUpdate = world.rand.nextInt(WarpDriveConfig.BREATHING_AIR_GENERATION_TICKS);
	}
	
	@Override
	public void update() {
		final IBlockState blockState = world.getBlockState(pos);
		if (isInvalidBlockState(blockState, BlockAirGeneratorTiered.class, BlockProperties.ACTIVE, BlockProperties.FACING)) {
			return;
		}

		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		tickUpdate--;
		if (tickUpdate >= 0) {
			return;
		}
		tickUpdate = WarpDriveConfig.BREATHING_AIR_GENERATION_TICKS;
		
		// Air generator works only in space & hyperspace
		if (CelestialObjectManager.hasAtmosphere(world, pos.getX(), pos.getZ())) {
			updateBlockState(blockState, BlockProperties.ACTIVE, false);
			return;
		}
		
		final boolean isActive = releaseAir(blockState.getValue(BlockProperties.FACING));
		updateBlockState(blockState, BlockProperties.ACTIVE, isActive);
	}
	
	private boolean releaseAir(final EnumFacing direction) {
		final BlockPos posDirection = pos.offset(direction);
		
		// reject cables or signs in front of the fan (it's inconsistent and not really supported)
		if (!world.isAirBlock(posDirection)) {
			return false;
		}
		
		// get the state object
		// assume it works when chunk isn't loaded
		final StateAir stateAir = ChunkHandler.getStateAir(world, posDirection.getX(), posDirection.getY(), posDirection.getZ());
		if (stateAir == null) {
			return true;
		}
		stateAir.updateBlockCache(world);
		
		// only accept air block (i.e. rejecting the dictionary blacklist)
		if (!stateAir.isAir()) {
			return false;
		}
		
		if (isEnabled) {
			final int energy_cost = !stateAir.isAirSource() ? WarpDriveConfig.BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER[enumTier.getIndex()]
			                                                : WarpDriveConfig.BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[enumTier.getIndex()];
			if (energy_consume(energy_cost, true)) {// enough energy
				final short range = (short) (WarpDriveConfig.BREATHING_AIR_GENERATION_RANGE_BLOCKS_BY_TIER[enumTier.getIndex()] - 1);
				stateAir.setAirSource(world, direction, range);
				energy_consume(energy_cost, false);
				return true;
			}
		}
		
		// disabled or low energy => remove air block
		if (stateAir.concentration > 4) {
			stateAir.setConcentration(world, (byte) (stateAir.concentration / 2));
		} else if (stateAir.concentration > 0) {
			stateAir.removeAirSource(world);
		}
		return false;
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		final double energyRequired_newAir  = WarpDriveConfig.BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER     [enumTier.getIndex()] / (double) WarpDriveConfig.BREATHING_AIR_GENERATION_TICKS;
		final double energyRequired_refresh = WarpDriveConfig.BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK_BY_TIER[enumTier.getIndex()] / (double) WarpDriveConfig.BREATHING_AIR_GENERATION_TICKS;
		return new Object[] {
				true,
				EnergyWrapper.convert((long) Math.ceil(energyRequired_newAir  * 100.0D), units) / 100.0F,
				EnergyWrapper.convert((long) Math.ceil(energyRequired_refresh * 100.0D), units) / 100.0F };
	}
}

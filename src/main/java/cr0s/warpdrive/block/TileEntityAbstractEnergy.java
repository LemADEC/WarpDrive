package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnergyWrapper;

import gregtech.api.capability.IEnergyContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;


/*
    public static BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    public static BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    public static long castToLong(BigInteger value) {
        return value.compareTo(LONG_MAX) >= 0 ? Long.MAX_VALUE : value.compareTo(LONG_MIN) <= 0 ? Long.MIN_VALUE : value.longValue();
    }
*/

public abstract class TileEntityAbstractEnergy extends TileEntityAbstractEnergyBase {
	
	// static properties
	@CapabilityInject(IEnergyStorage.class)
	public static Capability<IEnergyStorage> FE_CAPABILITY_ENERGY = null;
	@CapabilityInject(IEnergyContainer.class)
	public static Capability<IEnergyContainer> GT_CAPABILITY_ENERGY_CONTAINER = null;
	
	// block parameters constants
	private long energyMaxStorage;
	private int IC2_sinkTier;
	private int IC2_sourceTier;
	private int FE_fluxRateInput;
	private int FE_fluxRateOutput;
	private int GT_voltageInput;
	private int GT_amperageInput;
	private int GT_voltageOutput;
	private int GT_amperageOutput;
	protected boolean isEnergyLostWhenBroken = true;
	
	// persistent properties
	private long energyStored_internal = 0;
	
	// computed properties
	@SuppressWarnings("unchecked")
	private final LazyOptional<IEnergyStorage>[]    FE_energyStorages = new LazyOptional[Direction.values().length + 1];
	private final LazyOptional<?>[]                 GT_energyContainers = new LazyOptional[Direction.values().length + 1];
	private boolean                                 IC2_isAddedToEnergyNet = false;
	private long                                    IC2_timeAddedToEnergyNet = Long.MIN_VALUE;
	private int                                     scanTickCount = WarpDriveConfig.ENERGY_SCAN_INTERVAL_TICKS;
	
	private final IEnergyStorage[]                  FE_energyReceivers = new IEnergyStorage[Direction.values().length + 1];
	
	public TileEntityAbstractEnergy(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		// at base construction, we disable all input/output and allow infinite storage
		// we need to know the tier before setting things up, so we do the actual setup in onConstructed()
		energy_setParameters(Integer.MAX_VALUE,
		                     0, 0,
		                     "HV", 0, "HV", 0);
		
		// addMethods(new String[] { });
	}
	
	protected void energy_setParameters(final long energyMaxStorage,
	                                    final int fluxRateInput, final int fluxRateOutput,
	                                    final String nameTierInput, final int amperageInput,
	                                    final String nameTierOutput, final int amperageOutput) {
		this.energyMaxStorage = energyMaxStorage;
		FE_fluxRateInput = fluxRateInput;
		FE_fluxRateOutput = fluxRateOutput;
		IC2_sinkTier = EnergyWrapper.EU_getTierByName(nameTierInput);
		IC2_sourceTier = EnergyWrapper.EU_getTierByName(nameTierOutput);
		GT_voltageInput = 8 * (int) Math.pow(4, IC2_sinkTier);
		GT_amperageInput = amperageInput;
		GT_voltageOutput = 8 * (int) Math.pow(4, IC2_sourceTier);
		GT_amperageOutput = amperageOutput;
	}
	
	private boolean GT_isEnergyContainer(@Nonnull final Capability<?> capability) {
		return capability == GT_CAPABILITY_ENERGY_CONTAINER;
	}
	
	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability, @Nullable final Direction facing) {
		if (energy_getMaxStorage() != 0) {
			if ( WarpDriveConfig.ENERGY_ENABLE_FE
			  && capability == FE_CAPABILITY_ENERGY ) {
				LazyOptional<IEnergyStorage> energyStorage = FE_energyStorages[Commons.getOrdinal(facing)];
				if (energyStorage == null) {
					energyStorage = LazyOptional.of(() -> new IEnergyStorage() {
						
						@Override
						public int receiveEnergy(final int maxReceive, final boolean simulate) {
							return FE_receiveEnergy(facing, maxReceive, simulate);
						}
						
						@Override
						public int extractEnergy(final int maxExtract, final boolean simulate) {
							return FE_extractEnergy(facing, maxExtract, simulate);
						}
						
						@Override
						public int getEnergyStored() {
							return canExtract() || canReceive() ? EnergyWrapper.convertInternalToFE_floor(energy_getEnergyStored()) : 0;
						}
						
						@Override
						public int getMaxEnergyStored() {
							return canExtract() || canReceive() ? EnergyWrapper.convertInternalToFE_floor(energy_getMaxStorage()) : 0;
						}
						
						@Override
						public boolean canExtract() {
							return energy_canOutput(facing);
						}
						
						@Override
						public boolean canReceive() {
							return energy_canInput(facing);
						}
					});
					if (WarpDriveConfig.LOGGING_ENERGY) {
						WarpDrive.logger.info(String.format("%s IEnergyStorage(%s) capability created!",
						                                    this, facing));
					}
					FE_energyStorages[Commons.getOrdinal(facing)] = energyStorage;
				}
				return FE_energyStorages[Commons.getOrdinal(facing)].cast();
			}
			
			if ( WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
			  && WarpDriveConfig.isGregtechLoaded
			  && GT_isEnergyContainer(capability) ) {
				/* TODO MC1.15 enable GT support once it's updated
				return GT_getEnergyContainer(capability, facing);
				*/
			}
		}
		return super.getCapability(capability, facing);
	}
	
	/* TODO MC1.15 enable GT support once it's updated
	private <T> LazyOptional<T> GT_getEnergyContainer(@Nonnull final Capability<T> capability, @Nullable final Direction facing) {
		assert capability == GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
		
		IEnergyContainer energyContainer = (IEnergyContainer) GT_energyContainers[Commons.getOrdinal(facing)];
		if (energyContainer == null) {
			energyContainer = new IEnergyContainer() {
				
				@Override
				public long acceptEnergyFromNetwork(final Direction side, final long voltage, final long amperage) {
					if (!inputsEnergy(side)) {
						return 0L;
					}
					if (voltage > getInputVoltage()) {
						if (Commons.throttleMe(toString())) {
							WarpDrive.logger.info(String.format("Overvoltage detected at %s input side %s: %d > %d",
							                                    this, side, voltage, getInputVoltage()));
						}
						
						final int tier = GTUtility.getTierByVoltage(voltage);
						GT_applyOvervoltageEffects(tier);
						
						return Math.min(amperage, getInputAmperage());
					}
					
					final long energyMaxToAccept_GT = EnergyWrapper.convertInternalToGT_ceil(energy_getMaxStorage() - energy_getEnergyStored());
					final long amperageMaxToAccept_GT = Math.min(energyMaxToAccept_GT / voltage, Math.min(amperage, getInputAmperage()));
					if (amperageMaxToAccept_GT <= 0) {
						return 0L;
					}
					
					energyStored_internal += EnergyWrapper.convertGTtoInternal_floor(voltage * amperageMaxToAccept_GT);
					return amperageMaxToAccept_GT;
				}
				
				@Override
				public boolean inputsEnergy(final Direction side) {
					return energy_canInput(side);
				}
				
				@Override
				public boolean outputsEnergy(final Direction side) {
					return energy_canOutput(side);
				}
				
				@Override
				public long changeEnergy(final long differenceAmount) {
					
					final long energyMaxToRemove_GT = EnergyWrapper.convertInternalToGT_ceil(energy_getEnergyStored());
					final long energyMaxToAccept_GT = EnergyWrapper.convertInternalToGT_ceil(energy_getMaxStorage() - energy_getEnergyStored());
					final long energyToAccept_GT = Math.max(-energyMaxToRemove_GT, Math.min(energyMaxToAccept_GT, differenceAmount));
					
					energyStored_internal += EnergyWrapper.convertGTtoInternal_floor(energyToAccept_GT);
					return energyToAccept_GT;
				}
				
				@Override
				public long getEnergyStored() {
					return outputsEnergy(facing) || inputsEnergy(facing) ? EnergyWrapper.convertInternalToGT_floor(energy_getEnergyStored()) : 0;
				}
				
				@Override
				public long getEnergyCapacity() {
					return outputsEnergy(facing) || inputsEnergy(facing) ? EnergyWrapper.convertInternalToGT_floor(energy_getMaxStorage()) : 0;
				}
				
				@Override
				public long getOutputAmperage() {
					return GT_amperageOutput;
				}
				
				@Override
				public long getOutputVoltage() {
					return GT_voltageOutput;
				}
				
				@Override
				public long getInputAmperage() {
					return GT_amperageInput;
				}
				
				@Override
				public long getInputVoltage() {
					return GT_voltageInput;
				}
				
				@Override
				public long getInputPerSec() {
					return inputsEnergy(facing) ? (long) GT_amperageInput * GT_voltageInput / 20 : 0L;
				}
				
				@Override
				public long getOutputPerSec() {
					return outputsEnergy(facing) ? (long) GT_amperageOutput * GT_voltageOutput / 20 : 0L;
				}
			};
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("%s IEnergyContainer capability created!",
				                                    this));
			}
			GT_energyContainers[Commons.getOrdinal(facing)] = energyContainer;
		}
		return LazyOptional.of(() -> (T) GT_energyContainers[Commons.getOrdinal(facing)]);
	}
	
	private void GT_applyOvervoltageEffects(final int tier) {
		assert world != null;
		final int radius = 3;
		if (WarpDriveConfig.ENERGY_OVERVOLTAGE_SHOCK_FACTOR > 0) {
			// light up area with particles
			final Vector3 v3Entity = new Vector3();
			final Vector3 v3Direction = new Vector3();
			for (int count = 0; count < 3; count++) {
				v3Direction.x = 2 * (world.rand.nextDouble() - 0.5D);
				v3Direction.y = 2 * (world.rand.nextDouble() - 0.5D);
				v3Direction.z = 2 * (world.rand.nextDouble() - 0.5D);
				final double range = radius * (0.4D + 0.6D * world.rand.nextDouble());
				v3Entity.x = pos.getX() + 0.5D + range * v3Direction.x + (world.rand.nextDouble() - 0.5D);
				v3Entity.y = pos.getY() + 0.5D + range * v3Direction.y + (world.rand.nextDouble() - 0.5D);
				v3Entity.z = pos.getZ() + 0.5D + range * v3Direction.z + (world.rand.nextDouble() - 0.5D);
				v3Direction.scale(0.15D);
				PacketHandler.sendSpawnParticlePacket(world, "fireworksSpark", (byte) 1, v3Entity, v3Direction,
				                                      0.20F + 0.30F * world.rand.nextFloat(), 0.50F + 0.15F * world.rand.nextFloat(), 0.75F + 0.25F * world.rand.nextFloat(),
				                                      0.10F + 0.20F * world.rand.nextFloat(), 0.10F + 0.30F * world.rand.nextFloat(), 0.20F + 0.10F * world.rand.nextFloat(),
				                                      32);
			}
			
			// attack all entities in range
			final List<LivingEntity> entityLivingBases = world.getEntitiesWithinAABB(
					LivingEntity.class,
					new AxisAlignedBB(pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
					                  pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1));
			for (final LivingEntity entityLivingBase : entityLivingBases) {
				if ( !entityLivingBase.isAlive()
				  || !entityLivingBase.attackable()
				  || ( entityLivingBase instanceof PlayerEntity
				    && ((PlayerEntity) entityLivingBase).abilities.disableDamage ) ) {
					continue;
				}
				
				entityLivingBase.attackEntityFrom(WarpDrive.damageShock, tier * WarpDriveConfig.ENERGY_OVERVOLTAGE_SHOCK_FACTOR);
			}
		}
		
		if (WarpDriveConfig.ENERGY_OVERVOLTAGE_EXPLOSION_FACTOR > 0) {
			world.removeBlock(pos, false);
			
			world.createExplosion(
					null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
					tier * WarpDriveConfig.ENERGY_OVERVOLTAGE_EXPLOSION_FACTOR, true, Mode.DESTROY );
		}
	}
	*/
	
	public long energy_getEnergyStored() {
		return Commons.clamp(0L, energy_getMaxStorage(), energyStored_internal);
	}
	
	// Methods to override
	
	/**
	 * Return the maximum amount of energy that can be stored (measured in internal energy units).
	 */
	public long energy_getMaxStorage() {
		return energyMaxStorage;
	}
	
	/**
	 * Return the maximum amount of energy that can be output (measured in internal energy units).
	 */
	public int energy_getPotentialOutput() {
		return 0;
	}
	
	/**
	 * Remove energy from storage, called after actual output happened (measured in internal energy units).
	 * Override this to use custom storage or measure output statistics.
	 */
	protected void energy_outputDone(final long energyOutput_internal) {
		energy_consume(energyOutput_internal);
	}
	
	/**
	 * Should return true if that direction can receive energy.
	 */
	@SuppressWarnings("UnusedParameters")
	public boolean energy_canInput(final Direction from) {
		return false;
	}
	
	/**
	 * Should return true if that direction can output energy.
	 */
	@SuppressWarnings("UnusedParameters")
	public boolean energy_canOutput(final Direction to) {
		return false;
	}
	
	/**
	 * Consume energy from storage for internal usage or after outputting (measured in internal energy units).
	 * Override this to use custom storage or measure energy consumption statistics (internal usage or output).
	 */
	public boolean energy_consume(final long amount_internal, final boolean simulate) {
		if (energy_getEnergyStored() >= amount_internal) {
			if (!simulate) {
				energy_consume(amount_internal);
			}
			return true;
		}
		return false;
	}
	public void energy_consume(final long amount_internal) {
		energyStored_internal -= amount_internal;
	}
	
	@Override
	public void setDebugValues() {
		super.setDebugValues();
		energyStored_internal = energyMaxStorage;
	}
	
	@Override
	protected WarpDriveText getEnergyStatusText() {
		final WarpDriveText text = new WarpDriveText();
		// skip when energy is non applicable
		final long energy_maxStorage = energy_getMaxStorage();
		if (energy_maxStorage == 0L) {
			return text;
		}
		
		// report energy level
		EnergyWrapper.formatAndAppendCharge(text, energy_getEnergyStored(), energy_maxStorage, null);
		
		// report energy tiers
		if (energy_canInput(null)) {
			EnergyWrapper.formatAndAppendInputRate(text, GT_amperageInput, GT_voltageInput, IC2_sinkTier, FE_fluxRateInput, null);
		}
		if (energy_canOutput(null)) {
			EnergyWrapper.formatAndAppendOutputRate(text, GT_amperageOutput, GT_voltageOutput, IC2_sourceTier, FE_fluxRateOutput, null);
		}
		
		return text;
	}
	
	// Minecraft overrides
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		// disable energy storage by default, children should call energy_setParameters() now
		energyMaxStorage = 0;
	}
	
	@Override
	protected void onFirstTick() {
		super.onFirstTick();
		
		assert world != null;
		if (world.isRemote()) {
			return;
		}
		
		// RedstoneFlux and Forge energy
		if (WarpDriveConfig.ENERGY_ENABLE_FE) {
			FE_scanForEnergyReceivers();
		}
	}
	
	@Override
	public void tick() {
		super.tick();
		
		assert world != null;
		if (world.isRemote()) {
			return;
		}
		
		// IndustrialCraft2
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && WarpDriveConfig.isIndustrialCraft2Loaded ) {
			IC2_addToEnergyNet();
		}
		
		// RedstoneFlux & ForgeEnergy
		if (WarpDriveConfig.ENERGY_ENABLE_FE) {
			scanTickCount--;
			if (scanTickCount <= 0) {
				scanTickCount = WarpDriveConfig.ENERGY_SCAN_INTERVAL_TICKS;
				if (FE_fluxRateOutput > 0) {
					FE_scanForEnergyReceivers();
				}
			}
			
			if (FE_fluxRateOutput > 0) {
				if (WarpDriveConfig.ENERGY_ENABLE_FE) {
					FE_outputEnergy();
				}
			}
		}
	}
	
	@Override
	public void onChunkUnloaded() {
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			IC2_removeFromEnergyNet();
		}
		
		super.onChunkUnloaded();
	}
	
	@Override
	public void remove() {
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			IC2_removeFromEnergyNet();
		}
		
		super.remove();
	}
	
	// EnergyBase override
	@Override
	public Object[] getEnergyStatus() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				EnergyWrapper.convert(energy_getEnergyStored(), units),
				EnergyWrapper.convert(energy_getMaxStorage(), units),
				units };
	}
	
	/* TODO MC1.15 enable IC2 support once it's updated
	// IndustrialCraft IEnergyAcceptor interface
	@Override
	public boolean acceptsEnergyFrom(final IEnergyEmitter emitter, final Direction from) {
		final boolean accepts = WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		                     && energy_canInput(from);
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]acceptsEnergyFrom(%s, %s) => %s",
			                                    this, emitter, from, accepts ));
		}
		return accepts;
	}
	
	// IndustrialCraft IEnergySink interface
	@Override
	public int getSinkTier() {
		final int tier = WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		              && energy_getMaxStorage() > 0
		               ? IC2_sinkTier : 0;
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]getSinkTier() => %d",
			                                    this, tier ));
		}
		return tier;
	}
	
	@Override
	public double getDemandedEnergy() {
		final double demanded_EU = Math.max(0.0D, EnergyWrapper.convertInternalToEU_floor(energy_getMaxStorage() - energy_getEnergyStored()));
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]getDemandedEnergy() => %.2f EU",
			                                    this, demanded_EU ));
		}
		return demanded_EU;
	}
	
	@Override
	public double injectEnergy(final Direction from, final double amount_EU, final double voltage) {
		double amountLeftOver_EU = amount_EU;
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && energy_canInput(from.getOpposite()) ) {
			long leftover_internal = 0;
			energyStored_internal += EnergyWrapper.convertEUtoInternal_floor(amount_EU);
			
			if (energyStored_internal > energy_getMaxStorage()) {
				leftover_internal = (energyStored_internal - energy_getMaxStorage());
				energyStored_internal = energy_getMaxStorage();
			}
			
			amountLeftOver_EU = EnergyWrapper.convertInternalToEU_floor(leftover_internal);
		}
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]injectEnergy(%s, %.2f EU, %.1f) => %.2f EU",
			                                    this, from, amount_EU, voltage, amountLeftOver_EU ));
		}
		return amountLeftOver_EU;
	}
	
	// IndustrialCraft IEnergyEmitter interface
	@Override
	public boolean emitsEnergyTo(final IEnergyAcceptor receiver, final Direction to) {
		final boolean emits = WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		                   && energy_canOutput(to);
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]emitsEnergyTo(%s, %s) => %s",
			                                    this, receiver, to, emits ));
		}
		return emits;
	}
	
	// IndustrialCraft IEnergySource interface
	@Override
	public int getSourceTier() {
		final int tier = WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		              && energy_getMaxStorage() > 0
		               ? IC2_sourceTier : 0;
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]getSourceTier() => %d",
			                                    this, tier ));
		}
		// IC2 caps at tier 4 (EV), cables burn & block explodes after that, so we might as well enforce it
		return Math.min(4, tier);
	}
	
	@Override
	public double getOfferedEnergy() {
		double offered_EU = 0.0D;
		if (WarpDriveConfig.ENERGY_ENABLE_IC2_EU) {
			offered_EU = EnergyWrapper.convertInternalToEU_floor(energy_getPotentialOutput());
		}
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]getOfferedEnergy() => %.2f EU",
			                                    this, offered_EU ));
		}
		return offered_EU;
	}
	
	@Override
	public void drawEnergy(final double amount_EU) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]drawEnergy(%.2f EU)",
			                                    this, amount_EU));
		}
		energy_outputDone(EnergyWrapper.convertEUtoInternal_ceil(amount_EU));
	}
	*/
	
	// IndustrialCraft compatibility methods
	private void IC2_addToEnergyNet() {
		assert world != null;
		if ( !world.isRemote()
		  && !IC2_isAddedToEnergyNet ) {
			IC2_timeAddedToEnergyNet = world.getGameTime();
			/* TODO MC1.15 enable IC2 support once it's updated
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
			*/
			IC2_isAddedToEnergyNet = true;
		}
	}
	private void IC2_removeFromEnergyNet() {
		assert world != null;
		if ( !world.isRemote()
		  && IC2_isAddedToEnergyNet ) {
			/* TODO MC1.15 enable IC2 support once it's updated
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			*/
			IC2_isAddedToEnergyNet = false;
		}
	}
	
	// ForgeEnergy compatibility methods
	private int FE_receiveEnergy(final Direction from, final int maxReceive_FE, final boolean simulate) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s FE_receiveEnergy(%s, %d, %s)",
			                                    this, from, maxReceive_FE, simulate));
		}
		if (!energy_canInput(from)) {
			return 0;
		}
		
		final long energyMaxStored_internal = energy_getMaxStorage();
		if (energyMaxStored_internal == 0L) {
			return 0;
		}
		final long energyStored_internal = energy_getEnergyStored();
		final int energyMaxToAdd_FE = EnergyWrapper.convertInternalToFE_ceil(energyMaxStored_internal - energyStored_internal);
		
		final int energyToAdd_FE = Math.min(maxReceive_FE, energyMaxToAdd_FE);
		if (WarpDriveConfig.LOGGING_ENERGY) {
			final int energyMaxStored_FE = EnergyWrapper.convertInternalToFE_floor(energyMaxStored_internal);
			final int energyStored_FE = EnergyWrapper.convertInternalToFE_floor(energyStored_internal);
			WarpDrive.logger.info(String.format("%s FE_receiveEnergy(%s, %d, %s) adding %s to %d / %s FE",
			                                    this, from, maxReceive_FE, simulate, energyToAdd_FE, energyStored_FE, energyMaxStored_FE));
		}
		if (!simulate) {
			this.energyStored_internal += EnergyWrapper.convertFEtoInternal_floor(energyToAdd_FE);
		}
		
		return energyToAdd_FE;
	}
	
	private int FE_extractEnergy(final Direction from, final int maxExtract_FE, final boolean simulate) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s FE_extractEnergy(%s, %d, %s)",
			                                    this, from, maxExtract_FE, simulate));
		}
		if (!energy_canOutput(from)) {
			return 0;
		}
		
		final long potentialEnergyOutput_internal = energy_getPotentialOutput();
		final long energyExtracted_internal = Math.min(EnergyWrapper.convertFEtoInternal_ceil(maxExtract_FE), potentialEnergyOutput_internal);
		if (!simulate) {
			energy_outputDone(energyExtracted_internal);
		}
		return EnergyWrapper.convertInternalToFE_floor(energyExtracted_internal);
	}
	
	private void FE_outputEnergy(final Direction to, @Nonnull final IEnergyStorage energyStorage) {
		if (!energy_canOutput(to)) {
			return;
		}
		final long potentialEnergyOutput_internal = energy_getPotentialOutput();
		if (potentialEnergyOutput_internal > 0) {
			final int potentialEnergyOutput_FE = EnergyWrapper.convertInternalToFE_floor(potentialEnergyOutput_internal);
			final int energyToOutput_FE = energyStorage.receiveEnergy(potentialEnergyOutput_FE, true);
			if (energyToOutput_FE > 0) {
				final int energyOutputted_FE = energyStorage.receiveEnergy(energyToOutput_FE, false);
				energy_outputDone(EnergyWrapper.convertFEtoInternal_ceil(energyOutputted_FE));
			}
		}
	}
	
	private void FE_outputEnergy() {
		for (final Direction to : Direction.values()) {
			final IEnergyStorage energyStorage = FE_energyReceivers[Commons.getOrdinal(to)];
			if (energyStorage != null) {
				FE_outputEnergy(to, energyStorage);
			}
		}
	}
	
	private void FE_addEnergyReceiver(@Nonnull final Direction to, final TileEntity tileEntity) {
		if (tileEntity != null) {
			final LazyOptional<IEnergyStorage> optEnergyStorage = tileEntity.getCapability(FE_CAPABILITY_ENERGY, to.getOpposite());
			optEnergyStorage.ifPresent(energyStorage -> {
				if (energyStorage.canReceive()) {
					if (FE_energyReceivers[Commons.getOrdinal(to)] != energyStorage) {
						FE_energyReceivers[Commons.getOrdinal(to)] = energyStorage;
					}
				} else {
					FE_energyReceivers[Commons.getOrdinal(to)] = null;
				}
			});
		}
	}
	
	private void FE_scanForEnergyReceivers() {
		assert WarpDriveConfig.ENERGY_ENABLE_FE;
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s FE_scanForEnergyReceivers()",
			                                    this));
		}
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(pos);
		for (final Direction to : Direction.values()) {
			if (energy_canOutput(to)) {
				mutableBlockPos.setPos(
						pos.getX() + to.getXOffset(),
						pos.getY() + to.getYOffset(),
						pos.getZ() + to.getZOffset() );
				assert world != null;
				final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
				FE_addEnergyReceiver(to, tileEntity);
			}
		}
	}
	
	
	// Forge overrides
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		energyStored_internal = tagCompound.getLong(EnergyWrapper.TAG_ENERGY);
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		
		tagCompound.putLong(EnergyWrapper.TAG_ENERGY, energyStored_internal);
		return tagCompound;
	}
	
	@Override
	public CompoundNBT writeItemDropNBT(CompoundNBT tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		if (isEnergyLostWhenBroken) {
			tagCompound.remove(EnergyWrapper.TAG_ENERGY);
		}
		return tagCompound;
	}
	
	// WarpDrive overrides
	protected void energy_refreshConnections() {
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			assert world != null;
			// IC2 EnergyNet throws a block update during the next world tick following its EnergyTileLoadEvent
			// Ignoring this specific block update prevents us to do a constant refresh of the energy net
			if (world.getGameTime() - IC2_timeAddedToEnergyNet > 1) {
				IC2_removeFromEnergyNet();
			}
		}
		scanTickCount = -1;
	}
	
	@Override
	public void onEMP(final float efficiency) {
		if (energy_getMaxStorage() > 0) {
			energy_consume(Math.round(energy_getEnergyStored() * efficiency), false);
		}
	}
}
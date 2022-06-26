package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.api.IParticleContainerItem;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.api.Particle;
import cr0s.warpdrive.api.ParticleRegistry;
import cr0s.warpdrive.api.ParticleStack;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.AcceleratorControlParameter;
import cr0s.warpdrive.data.AcceleratorSetup;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.InventoryWrapper;
import cr0s.warpdrive.data.ParticleBunch;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.TrajectoryPoint;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.network.PacketHandler;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion.Mode;

import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.FakePlayer;

public class TileEntityAcceleratorCore extends TileEntityAbstractEnergyCoreOrController implements IGlobalRegionProvider {
	
	private static final int      ACCELERATOR_COOLDOWN_TICKS = 300;
	private static final int      ACCELERATOR_GUIDE_UPDATE_TICKS = 300;
	private static final double   ACCELERATOR_AMBIENT_TEMPERATURE_K = 300.0;
	private static final double   ACCELERATOR_AMBIENT_WARMING_RATE = 0.01;
	private static final double   ACCELERATOR_TEMPERATURE_TOLERANCE_K = 0.05;
	public static final double[]  PARTICLE_BUNCH_ENERGY_MINIMUM = { 0.1D,  0.8D,   8.0D };    // 20% overlap tolerance between tiers
	public static final double[]  PARTICLE_BUNCH_ENERGY_MAXIMUM = { 1.0D, 10.0D, 100.0D };
	
	// Linear length = 48, 192, 2048
	// Energy factor per magnet = ((Energy max / Energy min) ^ (1 / Linear length) - 1) / 2
	public static final double[]  PARTICLE_BUNCH_ENERGY_FACTOR_PER_MAGNET = { 0.024570, 0.0060324, 0.0005625 };
	
	// Ring radius min = 7, 15, 31
	// Turn coefficient max is < (Energy max / Energy min) ^ (- min radius / Linear length)
	// Turn coefficient min is > (Energy max / Energy min) ^ (- (min radius - 1) / Linear length)
	public static final double[]  PARTICLE_BUNCH_TURN_COEFFICIENTS_AT_MIN_ENERGY = { 0.7866, 0.8555, 0.9678 }; 
	public static final double[]  PARTICLE_BUNCH_TURN_COEFFICIENTS_AT_MAX_ENERGY = { 0.7504, 0.8455, 0.9677 }; 
	
	public static final double[]  PARTICLE_BUNCH_ENERGY_TO_SPEEDS_X = { 0.1D, 1.0, 10.0, 100.0 };
	public static final double[]  PARTICLE_BUNCH_ENERGY_TO_SPEEDS_Y = { 0.01, 0.6,  1.5,   3.0 };
	
	public static final double[]  PARTICLE_BUNCH_ENERGY_TO_EXPLOSION_STRENGTH_X = {
		0.0D,
		PARTICLE_BUNCH_ENERGY_MAXIMUM[0],
		PARTICLE_BUNCH_ENERGY_MAXIMUM[1],
		PARTICLE_BUNCH_ENERGY_MAXIMUM[2],
		PARTICLE_BUNCH_ENERGY_MAXIMUM[2] * 2.0D
	};
	public static final double[]  PARTICLE_BUNCH_ENERGY_TO_EXPLOSION_STRENGTH_Y = { 0.0D, 1.5D, 3.0D, 6.0D, 10.0D };
	
	private static final double[] ACCELERATOR_COLLISION_ENERGY_TO_PARTICLE_INDEX_X = {
		0.0D,
		PARTICLE_BUNCH_ENERGY_MAXIMUM[0] * 0.4D,
		PARTICLE_BUNCH_ENERGY_MAXIMUM[1] * 0.5D,
		PARTICLE_BUNCH_ENERGY_MAXIMUM[2] * 0.5D,
		PARTICLE_BUNCH_ENERGY_MAXIMUM[2] * 1.5D,
		PARTICLE_BUNCH_ENERGY_MAXIMUM[2] * 2.0D
	};
	private static final double[] ACCELERATOR_COLLISION_ENERGY_TO_PARTICLE_INDEX_Y = { 0.0D, 1.0D, 2.0D, 3.0D, 4.0D, 5.0D };
	private static final Particle[] ACCELERATOR_COLLISION_PARTICLES = { null, ParticleRegistry.ION, ParticleRegistry.PROTON, ParticleRegistry.ANTIMATTER, ParticleRegistry.STRANGE_MATTER };
	
	// persistent properties
	private final Collection<ParticleBunch> setParticleBunches = new CopyOnWriteArraySet<>();
	private double temperatureCurrent_K = ACCELERATOR_AMBIENT_TEMPERATURE_K;
	private final Map<Integer, AcceleratorControlParameter> mapControlParameters = new HashMap<>();
	private int injectionPeriodTicks = 60;
	private int injectionTicks = 0;
	private int indexNextInjector = 0;
	private boolean legacy_isOn = false;
	
	// computed properties
	private int cooldownTicks;
	private int guideTicks;
	protected boolean isPowered = true;
	private AcceleratorSetup acceleratorSetup;
	
	
	public TileEntityAcceleratorCore(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		peripheralName = "warpdriveAccelerator";
		addMethods(new String[] {
			"enable",
			"getControlPoints",
			"getControlPointsCount",
			"getControlPoint",
			"getParameters",
			"getParametersControlChannels",
			"parameter",
			"injectionPeriod",
			"state"
		});
		CC_scripts = Collections.singletonList("startup");
	}
	
	@Override
	protected void onFirstTick() {
		super.onFirstTick();
		cooldownTicks = 0;
		guideTicks = ACCELERATOR_GUIDE_UPDATE_TICKS;
	}
	
	@Override
	public void tick() {
		super.tick();
		
		assert world != null;
		if (world.isRemote()) {
			return;
		}
		
		assert acceleratorSetup != null;
		
		// update counters
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
		if (guideTicks > 0) {
			guideTicks--;
		}
		
		// Evaluate current state
		final int tierCurrentTemperature;
		if (temperatureCurrent_K <= WarpDriveConfig.ACCELERATOR_TEMPERATURES_K[1]) {
			tierCurrentTemperature = 3;
		} else if (temperatureCurrent_K <= WarpDriveConfig.ACCELERATOR_TEMPERATURES_K[0]) {
			tierCurrentTemperature = 2;
		} else {
			tierCurrentTemperature = 1;
		}
		
		// Powered ?
		reportJammed(acceleratorSetup);
		
		int energyRequired;
		final boolean needsCooling = acceleratorSetup.temperatureTarget_K < temperatureCurrent_K;
		// compute cooling energy
		if (needsCooling) {
			energyRequired = (int)Math.round(acceleratorSetup.temperature_coolingEnergyCost_perTick);
		} else {
			energyRequired = (int)Math.round(acceleratorSetup.temperatures_sustainEnergyCost_perTick[tierCurrentTemperature - 1]);
		}
		// add acceleration energy
		energyRequired += acceleratorSetup.particleEnergy_energyCost_perTick * (0.1D + 1.0D * setParticleBunches.size());
		final int energyPotential = acceleratorSetup.energy_getPotentialOutput();
		isPowered = energyRequired > 0 && energyPotential >= energyRequired;
		
		if (!acceleratorSetup.isLoaded()) {
			cooldownTicks = ACCELERATOR_COOLDOWN_TICKS;
			return;
		}
		
		final boolean isEnabledAndValid = isEnabled && isAssemblyValid;
		final boolean isOn = isEnabledAndValid && cooldownTicks <= 0 && isPowered;
		updateBlockState(null, BlockProperties.ACTIVE, isOn);
		if (isOn) {
			// power on transition
			if (!legacy_isOn) {
				updateChillers(acceleratorSetup, true, needsCooling, false);
				if (WarpDriveConfig.LOGGING_ACCELERATOR) {
					WarpDrive.logger.info(this + " starting up...");
				}
				legacy_isOn = true;
				
			} else if ((world.getGameTime() + Math.abs(pos.getX() - pos.getZ())) % 20 == 0) {
				// intermittent update to recover block states
				updateChillers(acceleratorSetup, true, needsCooling, false);
			}
			cooldownTicks = 0;
			
			// consume energy
			acceleratorSetup.energy_consume(energyRequired);
			
			// update temperature
			if (acceleratorSetup.temperatureTarget_K <= temperatureCurrent_K) {
				temperatureCurrent_K = updateTemperature(
					temperatureCurrent_K,
					acceleratorSetup.temperatures_cooling_K_perTick[tierCurrentTemperature - 1],
					acceleratorSetup.temperatureTarget_K);
			} else {
				temperatureCurrent_K = updateTemperature(
					temperatureCurrent_K,
					ACCELERATOR_AMBIENT_WARMING_RATE,
					acceleratorSetup.temperatureTarget_K);
			}
			if (needsCooling && temperatureCurrent_K <= acceleratorSetup.temperatureTarget_K) {
				sendEvent("acceleratorCoolingDone");
			}
			
			// inject new particles
			injectionTicks--;
			if ( injectionTicks <= 0
			  && !needsCooling && setParticleBunches.size() < WarpDriveConfig.ACCELERATOR_MAX_PARTICLE_BUNCHES
			  && !acceleratorSetup.mapInjectors.isEmpty() ) {
				injectionTicks = injectionPeriodTicks;
				final int countInjectors = acceleratorSetup.keyInjectors.length;
				if (indexNextInjector < countInjectors) {
					final Integer controlChannel = acceleratorSetup.keyInjectors[indexNextInjector];
					final AcceleratorControlParameter acceleratorControlParameter = mapControlParameters.get(controlChannel);
					if (acceleratorControlParameter == null || acceleratorControlParameter.isEnabled) {
						onInject(acceleratorSetup.mapInjectors.get(controlChannel));
					}
				} else {
					// invalid setup => force a reset
					rebootAccelerator(acceleratorSetup,false, true);
				}
				indexNextInjector = (indexNextInjector + 1) % countInjectors;
			}
			
			// run simulation
			doSimulate(needsCooling ? null : acceleratorSetup);
			
		} else {
			// update blocks during power off transition or intermittently
			if (legacy_isOn) {
				rebootAccelerator(acceleratorSetup,false, false);
				if (WarpDriveConfig.LOGGING_ACCELERATOR) {
					WarpDrive.logger.info(this + " shutting down...");
				}
				legacy_isOn = false;
				cooldownTicks = ACCELERATOR_COOLDOWN_TICKS;
				guideTicks = 0;
				
			} else if ((world.getGameTime() + Math.abs(pos.getX() - pos.getZ())) % 20 == 0) {
				// intermittent update to recover block states
				updateChillers(acceleratorSetup, false, false, false);
			}
			
			// update temperature
			temperatureCurrent_K = updateTemperature(
					temperatureCurrent_K,
					ACCELERATOR_AMBIENT_WARMING_RATE,
					ACCELERATOR_AMBIENT_TEMPERATURE_K);
			
			if (isEnabledAndValid) {
				if (guideTicks <= 0) {
					guideTicks = ACCELERATOR_GUIDE_UPDATE_TICKS;
					
					final WarpDriveText text = getStatusPrefix();
					final String energyUnits = energy_getDisplayUnits();
					if (energyRequired > acceleratorSetup.energy_getMaxStorage()) {
						text.append(Commons.getStyleWarning(), "warpdrive.accelerator.guide.low_power.not_enough_storage",
						            EnergyWrapper.format(energyRequired, energyUnits),
						            EnergyWrapper.format(acceleratorSetup.energy_getMaxStorage(), energyUnits),
						            energyUnits);
					} else if (acceleratorSetup.setChillers.isEmpty()) {
						text.append(Commons.getStyleWarning(), "warpdrive.accelerator.guide.no_chiller");
					} else if (setParticleBunches.isEmpty()) {
						text.append(Commons.getStyleWarning(), "warpdrive.accelerator.guide.low_power.no_particles",
						            EnergyWrapper.format(energyRequired, energyUnits),
						            EnergyWrapper.format(energyPotential, energyUnits),
						            energyUnits);
					} else {
						text.append(Commons.getStyleWarning(), "warpdrive.accelerator.guide.low_power.accelerating",
						            EnergyWrapper.format(energyRequired, energyUnits),
						            EnergyWrapper.format(energyPotential, energyUnits),
						            energyUnits);
					}
					
					final AxisAlignedBB axisalignedbb = new AxisAlignedBB(pos.getX() - 10, pos.getY() - 10, pos.getZ() - 10,
					                                                      pos.getX() + 10, pos.getY() + 10, pos.getZ() + 10 );
					final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
					
					for (final Entity entity : list) {
						if ( !(entity instanceof PlayerEntity)
						  || entity instanceof FakePlayer ) {
							continue;
						}
						
						Commons.addChatMessage(entity, text);
					}
				}
			}
			
			// simulate all particles
			doSimulate(null);
		}
	}
	
	private static double updateTemperature(final double actual_K, final double rate, final double target_K) {
		final double delta_K = target_K - actual_K;
		if (Math.abs(delta_K) < ACCELERATOR_TEMPERATURE_TOLERANCE_K) {
			return target_K;
		} else {
			//noinspection StatementWithEmptyBody
			if (WarpDrive.isDev && WarpDriveConfig.LOGGING_ACCELERATOR) {
				// WarpDrive.logger.info("Accelerator temperature current " + actual_K + " rate " + rate + " target " + target_K);
			}
			return actual_K + rate * Math.signum(delta_K) * Math.sqrt(Math.abs(delta_K) / ACCELERATOR_AMBIENT_TEMPERATURE_K);
		}
	}
	
	boolean isOn() {
		return legacy_isOn;
	}
	
	private void reportJammed(@Nonnull final AcceleratorSetup acceleratorSetup) {
		assert world != null
		    && !world.isRemote();
		
		if (acceleratorSetup.setJammed.isEmpty()) {
			return;
		}
		
		for (final VectorI vector : acceleratorSetup.setJammed) {
			PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
			                                      new Vector3(vector.x + 0.5D, vector.y + 0.5D, vector.z + 0.5D),
			                                      new Vector3(0.0D, 0.0D, 0.0D),
			                                      1.0F, 1.0F, 1.0F,
			                                      1.0F, 1.0F, 1.0F,
			                                      32);
		}
	}
	
	private void updateChillers(final AcceleratorSetup acceleratorSetup, final boolean isOn, final boolean needsCooling, final boolean isChunkLoading) {
		if ( world == null
		  || world.isRemote()
		  || acceleratorSetup == null ) {
			return;
		}
		
		if (!acceleratorSetup.setChillers.isEmpty()) {
			for (final BlockPos blockPos : acceleratorSetup.setChillers) {
				if (!isChunkLoading) {
					if (!(world.isBlockLoaded(blockPos))) {// chunk is not loaded, skip it
						continue;
					}
				}
				final BlockState blockState = world.getBlockState(blockPos);
				
				if (blockState.getBlock() instanceof BlockChiller) {
					if (!blockState.getProperties().contains(BlockProperties.ACTIVE)) {
						WarpDrive.logger.error(String.format("Invalid blockstate property for BlockChiller %s %s %s, please report to mod author",
						                                     blockState, blockState.getBlock(), Commons.format(world, blockPos) ));
					}
					if (isOn && needsCooling) {
						if (!blockState.get(BlockProperties.ACTIVE)) {
							world.setBlockState(blockPos, blockState.with(BlockProperties.ACTIVE, true));
						}
					} else {
						if (blockState.get(BlockProperties.ACTIVE)) {
							world.setBlockState(blockPos, blockState.with(BlockProperties.ACTIVE, false));
						}
					}
				}
			}
		}
	}
	
	// inject a particle bunch
	private void onInject(@Nonnull final VectorI vInjector) {
		assert world != null;
		assert setParticleBunches.size() < WarpDriveConfig.ACCELERATOR_MAX_PARTICLE_BUNCHES;
		
		final TileEntity tileEntity = vInjector.getTileEntity(world);
		if (!(tileEntity instanceof TileEntityParticlesInjector)) {
			if (WarpDriveConfig.LOGGING_ACCELERATOR) {
				WarpDrive.logger.info(String.format("%s Unable to inject with missing injector %s %s",
				                                    this, tileEntity, Commons.format(world, pos) ));
			}
			markDirtyAssembly();
			return;
		}
		if (!((TileEntityParticlesInjector) tileEntity).getIsEnabled()) {
			return;
		}
		
		// find consumable
		final Collection<Object> inventories = InventoryWrapper.getConnectedInventories(tileEntity.getWorld(), tileEntity.getPos());
		if (inventories.isEmpty()) {
			PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
			                                      new Vector3(vInjector.x + 0.5D, vInjector.y + 0.5D, vInjector.z + 0.5D),
			                                      new Vector3(0.0D, 0.0D, 0.0D),
			                                      1.0F, 1.0F, 1.0F,
			                                      1.0F, 1.0F, 1.0F,
			                                      32);
			return;
		}
		
		int slotIndex = 0;
		boolean found = false;
		ItemStack itemStack;
		Object inventory = null;
		for (final Object inventoryLoop : inventories) {
			if (!found) {
				slotIndex = 0;
			}
			final int sizeInventory = InventoryWrapper.getSize(inventoryLoop);
			while (slotIndex < sizeInventory && !found) {
				itemStack = InventoryWrapper.getStackInSlot(inventoryLoop, slotIndex);
				if (itemStack.isEmpty()) {
					slotIndex++;
					continue;
				}
				final Block blockFromItem = Block.getBlockFromItem(itemStack.getItem());
				if (blockFromItem == Blocks.AIR) {
					slotIndex++;
					continue;
				}
				
				found = true;
				inventory = inventoryLoop;
			}
		}
		
		// no valid item found, moving on...
		if (inventory == null) {
			if (WarpDriveConfig.LOGGING_ACCELERATOR) {
				WarpDrive.logger.debug(this + " No valid item found to inject");
			}
			PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
			                                      new Vector3(vInjector.x + 0.5D, vInjector.y + 0.5D, vInjector.z + 0.5D),
			                                      new Vector3(0.0D, 0.0D, 0.0D),
			                                      1.0F, 1.0F, 1.0F,
			                                      1.0F, 1.0F, 1.0F,
			                                      32);
			return;
		}
		//noinspection ConstantConditions
		assert(found);
		
		// find a connected void shell
		Direction directionStart = Direction.NORTH;
		VectorI vPosition = vInjector;
		for (final Direction directionCheck : Direction.values()) {
			if (directionCheck.getYOffset() != 0) {
				continue;
			}
			vPosition = vInjector.clone(directionCheck);
			final Block block = vPosition.getBlock(world);
			if (block instanceof BlockVoidShellPlain) {
				directionStart = directionCheck;
				break;
			}
		}
		
		// consume item
		InventoryWrapper.decrStackSize(inventory, slotIndex, 1);
		
		// add particle bunch
		final Vector3 v3Start = new Vector3(directionStart);
		setParticleBunches.add(new ParticleBunch(vPosition.x, vPosition.y, vPosition.z, directionStart, v3Start));
		
		// visual effect
		if (WarpDriveConfig.LOGGING_ACCELERATOR) {
			WarpDrive.logger.info(this + " injecting from " + vInjector);
		}
		final Vector3 v3Speed = v3Start.clone().invert();
		PacketHandler.sendSpawnParticlePacket(world, "mobSpell", (byte) 10,
			new Vector3(vInjector.x + 0.5D * (1 + v3Speed.x + world.rand.nextGaussian()),
			            vInjector.y + 0.5D * (1 + v3Speed.y + world.rand.nextGaussian()),
			            vInjector.z + 0.5D * (1 + v3Speed.z + world.rand.nextGaussian())),
			v3Speed,
			0.70F, 0.70F, 0.90F,
			// 0.20F + 0.10F * world.rand.nextFloat(), 0.90F + 0.10F * world.rand.nextFloat(), 0.40F + 0.15F * world.rand.nextFloat(),
			0.0F, 0.0F, 0.0F, 32);
		sendEvent("particleBunchInjected");
	}
	
	// simulate a particle bunch
	private void doSimulate(final AcceleratorSetup acceleratorSetup) {
		assert world != null;
		if (setParticleBunches.isEmpty()) {
			return;
		}
		
		// update particle bunches
		for (final ParticleBunch particleBunch : setParticleBunches) {
			final boolean isAlive = particleBunch.onUpdate(world, mapControlParameters, acceleratorSetup);
			if (!isAlive) {
				setParticleBunches.remove(particleBunch);
			}
		}
		
		// skip colliders simulation if setup is invalid
		if (acceleratorSetup == null) {
			return;
		}
		
		// update colliders
		final Collection<ParticleBunch> setInRange = new ArrayList<>(setParticleBunches.size());
		boolean hasCollided;
		for (final TrajectoryPoint trajectoryPointCollider : acceleratorSetup.listColliders) {
			final AcceleratorControlParameter acceleratorControlParameter = mapControlParameters.get(trajectoryPointCollider.controlChannel);
			if (acceleratorControlParameter != null && !acceleratorControlParameter.isEnabled) {
				continue;
			}
			final double threshold = acceleratorControlParameter == null ? WarpDriveConfig.ACCELERATOR_THRESHOLD_DEFAULT : acceleratorControlParameter.threshold;
			final double energyThreshold = threshold * PARTICLE_BUNCH_ENERGY_MAXIMUM[trajectoryPointCollider.getTier() - 1];
			final AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
				trajectoryPointCollider.x + 0.5F - 2.0F, trajectoryPointCollider.y + 0.5F - 2.0F, trajectoryPointCollider.z + 0.5F - 2.0F,
				trajectoryPointCollider.x + 0.5F + 2.0F, trajectoryPointCollider.y + 0.5F + 2.0F, trajectoryPointCollider.z + 0.5F + 2.0F );
			setInRange.clear();
			hasCollided = false;
			
			// collect all applicable particles bunches, look for simple collision
			for (final ParticleBunch particleBunch : setParticleBunches) {
				if (hasCollided) {
					break;
				}
				if ( axisAlignedBB.minX <= particleBunch.x && particleBunch.x <= axisAlignedBB.maxX 
				  && axisAlignedBB.minY <= particleBunch.y && particleBunch.y <= axisAlignedBB.maxY
				  && axisAlignedBB.minZ <= particleBunch.z && particleBunch.z <= axisAlignedBB.maxZ ) {
					// particle bunch is in collider range
					if (particleBunch.energy >= energyThreshold) {
						// enough energy with one particle => stop here
						doCollision(world.rand, trajectoryPointCollider, particleBunch, null);
						setParticleBunches.remove(particleBunch);
						particleBunch.onCollided(world, trajectoryPointCollider);
						hasCollided = true;
					} else {
						setInRange.add(particleBunch);
					}
				}
			}
			
			// look for head to head collisions
			if (!hasCollided && setInRange.size() > 1) {
				for (final ParticleBunch particleBunch1 : setInRange) {
					if (hasCollided) {
						break;
					}
					for (final ParticleBunch particleBunch2 : setInRange) {
						if (hasCollided) {
							break;
						}
						// check that we have enough energy for head to head collision, and both are moving in opposing direction
						// (we don't care if they pass each others this tick or next one)
						if ( (particleBunch1.energy + particleBunch2.energy >= energyThreshold)
						  && (particleBunch1.directionCurrentMotion.getOpposite() == particleBunch2.directionCurrentMotion) ) {
							doCollision(world.rand, trajectoryPointCollider, particleBunch1, particleBunch2);
							setParticleBunches.remove(particleBunch1);
							particleBunch1.onCollided(world, trajectoryPointCollider);
							setParticleBunches.remove(particleBunch2);
							particleBunch2.onCollided(world, trajectoryPointCollider);
							hasCollided = true;
						}
					}
				}
			}
		}
	}
	
	private void doCollision(@Nonnull final Random random, @Nonnull final TrajectoryPoint trajectoryPointCollider,
	                         @Nonnull final ParticleBunch particleBunch1, final ParticleBunch particleBunch2) {
		assert world != null;
		final double energyTotal = particleBunch1.energy + (particleBunch2 != null ? particleBunch2.energy : 0.0D);
		final int tier = trajectoryPointCollider.getTier();
		
		final double indexParticle = Commons.interpolate(ACCELERATOR_COLLISION_ENERGY_TO_PARTICLE_INDEX_X, ACCELERATOR_COLLISION_ENERGY_TO_PARTICLE_INDEX_Y, energyTotal);
		final Particle particle = ACCELERATOR_COLLISION_PARTICLES[(int) Math.floor(indexParticle)];
		final double energyExtra = indexParticle - Math.floor(indexParticle);
		final int quantityGenerated = Math.max(0, (int) Math.round((10.0D + 5.0D * random.nextGaussian()) * (1.0 - 0.2 * tier) * (0.5D + 2 * energyExtra)));
		
		// store results
		int overflow = quantityGenerated;
		if (particle != null) {
			final Collection<Object> inventories = InventoryWrapper.getConnectedInventories(world, trajectoryPointCollider.vControlPoint.getBlockPos());
			final ParticleStack particleStack = addParticleToInventories(new ParticleStack(particle, quantityGenerated), inventories);
			overflow = particleStack == null || particleStack.isEmpty() ? 0 : particleStack.getAmount();
		}
		
		// sound & visual effects
		final SoundEvent soundEvent = tier == 1 ? SoundEvents.COLLISION_LOW : tier == 2 ? SoundEvents.COLLISION_MEDIUM : SoundEvents.COLLISION_HIGH;
		world.playSound(null, trajectoryPointCollider.getBlockPos(), soundEvent, SoundCategory.BLOCKS,
		                0.5F + (float) energyExtra, 0.85F + 0.50F * (float) energyExtra + 0.10F * world.rand.nextFloat());
		if (overflow > 0) {
			final float strength = 3.0F + tier;
			world.createExplosion(null,
			                      trajectoryPointCollider.x + 0.5D, trajectoryPointCollider.y + 0.5D, trajectoryPointCollider.z + 0.5D,
				                  strength, true, Mode.BREAK );
			
		} else {
			for (int countParticles = 0; countParticles < 5; countParticles++) {
				PacketHandler.sendSpawnParticlePacket(world, "explosionNormal", (byte) 5,
					new Vector3(trajectoryPointCollider.x + 0.5D + 2.5D * world.rand.nextGaussian(),
					            trajectoryPointCollider.y + 0.5D + 2.5D * world.rand.nextGaussian(),
					            trajectoryPointCollider.z + 0.5D + 2.5D * world.rand.nextGaussian()),
					new Vector3(0.15D * world.rand.nextGaussian(),
					            0.15D * world.rand.nextGaussian(),
					            0.15D * world.rand.nextGaussian()),
					0.70F, 0.70F, 0.90F,
					0.0F, 0.0F, 0.0F, 32);
			}
		}
		
		sendEvent("particleBunchCollided");
	}
	
	protected ParticleStack addParticleToInventories(final ParticleStack particleStack, final Collection<Object> inventories) {
		if (particleStack == null || particleStack.isEmpty()) {
			return null;
		}
		ParticleStack particleStackLeft = particleStack.copy();
		for (final Object inventory : inventories) {
			particleStackLeft = addParticleToInventory(particleStackLeft, inventory);
			if (particleStackLeft == null || particleStackLeft.isEmpty()) {
				return null;
			}
		}
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info(this + " Overflow detected");
		}
		return particleStackLeft;
	}
	
	private static ParticleStack addParticleToInventory(final ParticleStack particleStack, final Object inventory) {
		if (particleStack == null || particleStack.isEmpty()) {
			return null;
		}
		
		final ParticleStack particleStackLeft = particleStack.copy();
		int transfer;
		
		if (inventory != null) {
			final int sizeInventory = InventoryWrapper.getSize(inventory);
			// fill existing containers first
			for (int i = 0; i < sizeInventory; i++) {
				final ItemStack itemStack = InventoryWrapper.getStackInSlot(inventory, i);
				if (itemStack == null || !(itemStack.getItem() instanceof IParticleContainerItem)) {
					continue;
				}
				final IParticleContainerItem particleContainerItem = (IParticleContainerItem) itemStack.getItem();
				if (particleContainerItem.isEmpty(itemStack)) {
					continue;
				}
				transfer = particleContainerItem.fill(itemStack, particleStackLeft, true);
				particleStackLeft.fill(-transfer);
				if (particleStackLeft.isEmpty()) {
					return null;
				}
			}
			
			// put remaining in empty containers
			for (int i = 0; i < sizeInventory; i++) {
				final ItemStack itemStack = InventoryWrapper.getStackInSlot(inventory, i);
				if (itemStack == null || !(itemStack.getItem() instanceof IParticleContainerItem)) {
					continue;
				}
				final IParticleContainerItem particleContainerItem = (IParticleContainerItem) itemStack.getItem();
				if (!particleContainerItem.isEmpty(itemStack)) {
					continue;
				}
				transfer = particleContainerItem.fill(itemStack, particleStackLeft, true);
				particleStackLeft.fill(-transfer);
				if (particleStackLeft.isEmpty()) {
					return null;
				}
			}
		}
		
		return particleStackLeft;
	}
	
	private void rebootAccelerator(final AcceleratorSetup acceleratorSetup, final boolean isChunkLoading, final boolean isLeaking) {
		if (world == null || world.isRemote()) {
			return;
		}
		
		// WarpDrive.logger.info(this + " rebootAccelerator");
		
		markDirtyGlobalRegion();
		updateChillers(acceleratorSetup, false, false, isChunkLoading);
		legacy_isOn = false;
		if (isLeaking) {
			temperatureCurrent_K = ACCELERATOR_AMBIENT_TEMPERATURE_K;
			if (isEnabled) {
				sendEvent("acceleratorCoolingReset");
			}
		}
	}
	
	@Override
	public WarpDriveText getStatusHeader() {
		return super.getStatusHeader(); // @TODO
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		
		final ListNBT tagListParticleBunches = tagCompound.getList("particleBunches", Constants.NBT.TAG_COMPOUND);
		setParticleBunches.clear();
		for (int index = 0; index < tagListParticleBunches.size(); index++) {
			final ParticleBunch particleBunch = new ParticleBunch(tagListParticleBunches.getCompound(index));
			setParticleBunches.add(particleBunch);
		}
		
		if (tagCompound.contains("temperatureCurrent")) {
			temperatureCurrent_K = tagCompound.getDouble("temperatureCurrent");
		}
		
		final ListNBT tagListControlParameters = tagCompound.getList("controlParameters", Constants.NBT.TAG_COMPOUND);
		mapControlParameters.clear();
		for (int index = 0; index < tagListControlParameters.size(); index++) {
			final AcceleratorControlParameter acceleratorControlParameter = new AcceleratorControlParameter(tagListControlParameters.getCompound(index));
			mapControlParameters.put(acceleratorControlParameter.controlChannel, acceleratorControlParameter);
		}
		
		injectionPeriodTicks = tagCompound.getInt("injectionPeriod");
		if (tagCompound.contains("injectionTicks")) {
			injectionTicks = tagCompound.getInt("injectionTicks");
		}
		if (tagCompound.contains("nextInjector")) {
			indexNextInjector = tagCompound.getInt("nextInjector");
		}
		if (tagCompound.contains("isOn")) {
			legacy_isOn = tagCompound.getBoolean("isOn");
		}
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull final CompoundNBT tagCompound) {
		super.write(tagCompound);
		
		final ListNBT tagListParticleBunches = new ListNBT();
		for (final ParticleBunch particleBunch : setParticleBunches) {
			final CompoundNBT tagCompoundParticleBunch = new CompoundNBT();
			particleBunch.write(tagCompoundParticleBunch);
			tagListParticleBunches.add(tagCompoundParticleBunch);
		}
		tagCompound.put("particleBunches", tagListParticleBunches);
		
		tagCompound.putDouble("temperatureCurrent", temperatureCurrent_K);
		
		final ListNBT tagListControlParameters = new ListNBT();
		for (final AcceleratorControlParameter acceleratorControlParameter : mapControlParameters.values()) {
			final CompoundNBT tagCompoundControlParameter = new CompoundNBT();
			acceleratorControlParameter.write(tagCompoundControlParameter);
			tagListControlParameters.add(tagCompoundControlParameter);
		}
		tagCompound.put("controlParameters", tagListControlParameters);
		
		tagCompound.putInt("injectionPeriod", injectionPeriodTicks);
		tagCompound.putInt("injectionTicks", injectionTicks);
		tagCompound.putInt("nextInjector", indexNextInjector);
		tagCompound.putBoolean("isOn", legacy_isOn);
		
		return tagCompound;
	}
	
	@Override
	public CompoundNBT writeItemDropNBT(final CompoundNBT tagCompound) {
		super.writeItemDropNBT(tagCompound);
		tagCompound.remove("temperatureCurrent");
		tagCompound.remove("injectionTicks");
		tagCompound.remove("nextInjector");
		tagCompound.remove("isOn");
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		final CompoundNBT tagCompound = super.getUpdateTag();
		
		tagCompound.putBoolean("isPowered", isPowered);
		
		return tagCompound;
	}
	
	@Override
	public void onDataPacket(@Nonnull final NetworkManager networkManager, @Nonnull final SUpdateTileEntityPacket packet) {
		super.onDataPacket(networkManager, packet);
		
		final CompoundNBT tagCompound = packet.getNbtCompound();
		
		isPowered = tagCompound.getBoolean("isPowered");
	}
	
	@Override
	public void remove() {
		assert world != null;
		if (!world.isRemote()) {
			if (acceleratorSetup == null) {
				final WarpDriveText textAssemblyValid = new WarpDriveText();
				doScanAssembly(true, textAssemblyValid);
			}
			rebootAccelerator(acceleratorSetup, true, true);
		}
		super.remove();
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		assert world != null;
		final boolean isValid = super.doScanAssembly(isDirty, textReason);
		
		// note: acceleratorSetup is null when loading chunk, always defined otherwise
		final AcceleratorSetup legacy_acceleratorSetup = acceleratorSetup;
		if ( isDirty
		  || acceleratorSetup == null
		  || acceleratorSetup.isDirty() ) {
			acceleratorSetup = new AcceleratorSetup(world.getDimension().getType().getRegistryName(), pos);
			if (!acceleratorSetup.getAssemblyStatus(textReason)) {
				if (WarpDriveConfig.LOGGING_ACCELERATOR) {
					WarpDrive.logger.info(String.format("%s invalid accelerator setup: %s",
					                                    this, textReason.getString() ));
				}
				// don't return false, so the player can still enable the accelerator "at their own risk"
			} else {
				if (WarpDriveConfig.LOGGING_ACCELERATOR) {
					WarpDrive.logger.info(String.format("%s valid accelerator setup",
					                                    this ));
				}
			}
		} else {
			acceleratorSetup.getAssemblyStatus(textReason);
		}
		
		// reset accelerator in case of major changes
		if (isDirty) {
			if ( legacy_acceleratorSetup != null
			  && acceleratorSetup.isMajorChange(legacy_acceleratorSetup) ) {
				if (WarpDriveConfig.LOGGING_ACCELERATOR) {
					WarpDrive.logger.info(this + " rebooting due to major change...");
				}
				rebootAccelerator(legacy_acceleratorSetup, true, true);
			}
			sendEvent("acceleratorUpdated");
		}
		
		return isValid;
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		// no operation
	}
	
	@Override
	public long energy_getEnergyStored() {
		if (!hasWorld()) {
			return 0;
		}
		if (acceleratorSetup == null) {
			return 0;
		}
		return acceleratorSetup.energy_getEnergyStored();
	}
	
	@Override
	public long energy_getMaxStorage() {
		if (!hasWorld()) {
			return 0;
		}
		if (acceleratorSetup == null) {
			return 0;
		}
		return acceleratorSetup.energy_getMaxStorage();
	}
	
	// OpenComputer callback methods
	@Callback(direct = true)
	public Object[] getControlPoints(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getControlPoints();
	}
	
	@Callback(direct = true)
	public Object[] getControlPointsCount(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getControlPointsCount();
	}
	
	@Callback(direct = true)
	public Object[] injectionPeriod(final Context context, final Arguments arguments) {
		return injectionPeriod(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] getParameters(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getParameters();
	}
	
	@Callback(direct = true)
	public Object[] getParametersControlChannels(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getParametersControlChannels();
	}
	
	@Callback(direct = true)
	public Object[] parameter(final Context context, final Arguments arguments) {
		return parameter(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] getControlPoint(final Context context, final Arguments arguments) {
		return getControlPoint(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		if (acceleratorSetup == null) {
			return new Object[] { 0, 0, 0, 0 };
		}
		
		final String units = energy_getDisplayUnits();
		
		final long energyCoolingCost_perTick = EnergyWrapper.convert(Math.round( acceleratorSetup.temperature_coolingEnergyCost_perTick
		                                                                       + acceleratorSetup.particleEnergy_energyCost_perTick * 0.1D ), units);
		
		// Evaluate current state
		final int tierTargetTemperature;
		if (acceleratorSetup.temperatureTarget_K <= WarpDriveConfig.ACCELERATOR_TEMPERATURES_K[1]) {
			tierTargetTemperature = 3;
		} else if (acceleratorSetup.temperatureTarget_K <= WarpDriveConfig.ACCELERATOR_TEMPERATURES_K[0]) {
			tierTargetTemperature = 2;
		} else {
			tierTargetTemperature = 1;
		}
		
		final long energySustainCost_perTick = EnergyWrapper.convert(Math.round( acceleratorSetup.temperatures_sustainEnergyCost_perTick[tierTargetTemperature - 1]
		                                                                       + acceleratorSetup.particleEnergy_energyCost_perTick
		                                                                       * 0.1D ), units);
		final long energySingleCost_perTick = EnergyWrapper.convert(Math.round( acceleratorSetup.temperatures_sustainEnergyCost_perTick[tierTargetTemperature - 1]
		                                                                      + acceleratorSetup.particleEnergy_energyCost_perTick
		                                                                      * 1.1D ), units);
		final long energyMaxCost_perTick = EnergyWrapper.convert(Math.round( acceleratorSetup.temperatures_sustainEnergyCost_perTick[tierTargetTemperature - 1]
		                                                                   + acceleratorSetup.particleEnergy_energyCost_perTick
		                                                                   * (0.1D + WarpDriveConfig.ACCELERATOR_MAX_PARTICLE_BUNCHES) ), units);
		
		return new Object[] { energyCoolingCost_perTick, energySustainCost_perTick, energySingleCost_perTick, energyMaxCost_perTick };
	}
	
	private Object[] getControlPoints() {
		if (acceleratorSetup != null) {
			return acceleratorSetup.getControlPoints();
		}
		return new Object[] { };
	}
	
	@Nonnull
	private Object[] getControlPointsCount() {
		if ( acceleratorSetup != null
		  && acceleratorSetup.isAssemblyValid() ) {
			final Object[][] controlPoints = acceleratorSetup.getControlPoints();
			return new Integer[] { controlPoints.length };
		}
		return new Integer[] { -1 };
	}
	
	@Nonnull
	private Object[] getControlPoint(@Nonnull final Object[] arguments) {
		if (acceleratorSetup == null) {
			return new Object[] { false, "No accelerator setup" };
		}
		if (arguments.length != 1 || arguments[0] == null) {
			return new Object[] { false, "Expecting 1 argument: Integer index" };
		}
		
		final int index;
		try {
			index = Commons.toInt(arguments[0]);
		} catch (final Exception exception) {
			return new Object[] { false, "Integer expected for 1st argument" };
		}
		final Object[][] controlPoints = acceleratorSetup.getControlPoints();
		if (index < 0 || index >= controlPoints.length) {
			return new Object[] { false, "Index out of range" };
		}
		assert(controlPoints[index].length == 7);
		return new Object[] { true,
				controlPoints[index][0], controlPoints[index][1], controlPoints[index][2],
				controlPoints[index][3], controlPoints[index][4], controlPoints[index][5], controlPoints[index][6] };
	}
	
	private Object[] getParameters() {
		final Object[] results = new Object[mapControlParameters.size()];
		int index = 0;
		for (final AcceleratorControlParameter acceleratorControlParameter : mapControlParameters.values()) {
			results[index++] = new Object[] { acceleratorControlParameter.controlChannel, acceleratorControlParameter.isEnabled, acceleratorControlParameter.threshold, acceleratorControlParameter.description };
		}
		return results;
	}
	
	@Nonnull
	private Object[] getParametersControlChannels() {
		return mapControlParameters.keySet().toArray();
	}
	
	@Nonnull
	private Object[] parameter(@Nonnull final Object[] arguments) {
		if (arguments.length == 0 || arguments.length > 4) {
			return new Object[] { false, "Expecting 1 to 4 arguments: Integer controlChannel, Boolean isEnabled, Double threshold, String description" };
		}
		
		final int controlChannel;
		try {
			controlChannel = Commons.clamp(IControlChannel.CONTROL_CHANNEL_MIN, IControlChannel.CONTROL_CHANNEL_MAX, Commons.toInt(arguments[0]));
		} catch(final Exception exception) {
			return new Object[] { false, "Integer expected for 1st argument" };
		}
		AcceleratorControlParameter acceleratorControlParameter = mapControlParameters.get(controlChannel);
		if (acceleratorControlParameter == null) {
			acceleratorControlParameter = new AcceleratorControlParameter(controlChannel);
		}
		
		if (arguments.length == 1) {// reading value
			return new Object[] { true, controlChannel, acceleratorControlParameter.isEnabled, acceleratorControlParameter.threshold, acceleratorControlParameter.description };
		}
		
		// isEnabled
		if (arguments[1] != null) {
			final boolean isEnabled_new;
			try {
				isEnabled_new = Commons.toBool(arguments[1]);
			} catch (final Exception exception) {
				return new Object[] { false, "Boolean expected for 2nd argument" };
			}
			acceleratorControlParameter.isEnabled = isEnabled_new;
		}
		
		// threshold
		if (arguments.length >= 3 && arguments[2] != null) {
			final double threshold_new;
			try {
				threshold_new = Commons.clamp(0.0D, 2.0D, Commons.toDouble(arguments[2]));
			} catch (final Exception exception) {
				return new Object[] { false, "Double expected for 3rd argument" };
			}
			acceleratorControlParameter.threshold = threshold_new;
		}
		
		// description
		if (arguments.length >= 4 && arguments[3] != null) {
			final String description_new;
			try {
				description_new = (String) arguments[3];
			} catch (final Exception exception) {
				return new Object[] { false, "String expected for 4th argument" };
			}
			acceleratorControlParameter.description = description_new;
		}
		
		mapControlParameters.put(controlChannel, acceleratorControlParameter);
		
		return new Object[] { true, controlChannel, acceleratorControlParameter.isEnabled, acceleratorControlParameter.threshold, acceleratorControlParameter.description };
	}
	
	@Nonnull
	private Object[] injectionPeriod(@Nonnull final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			final double injectionPeriod_new;
			try {
				injectionPeriod_new = Commons.toDouble(arguments[0]);
			} catch (final Exception exception) {
				return new Double[] { injectionPeriodTicks / 20.0D };
			}
			injectionPeriodTicks = Commons.clamp(1, 18000, (int) Math.round(injectionPeriod_new * 20.0D) );
		}
		return new Double[] { injectionPeriodTicks / 20.0D };
	}
	
	private Object[] state() {
		final String units = energy_getDisplayUnits();
		final long energy = EnergyWrapper.convert(acceleratorSetup.energy_getEnergyStored(), units);
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isEnabled, isPowered, energy, temperatureCurrent_K, acceleratorSetup.temperatureTarget_K };
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "enable":
			return enable(arguments);
		
		case "getControlPoints":
			return getControlPoints();
		
		case "getControlPointsCount":
			return getControlPointsCount();
		
		case "getControlPoint":
			return getControlPoint(arguments);
		
		case "getParameters":
			return getParameters();
		
		case "getParametersControlChannels":
			return getParametersControlChannels();
		
		case "parameter":
			return parameter(arguments);
		
		case "injectionPeriod":
			return injectionPeriod(arguments);
		
		case "state":
			return state();
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
	
	// IGlobalRegionProvider overrides
	@Override
	public EnumGlobalRegionType getGlobalRegionType() {
		return EnumGlobalRegionType.ACCELERATOR;
	}
	
	@Override
	public AxisAlignedBB getGlobalRegionArea() {
		if (acceleratorSetup == null) {
			return null;
		}
		return acceleratorSetup.getBoundingBox();
	}
	
	@Override
	public int getMass() {
		if (acceleratorSetup != null) {
			return acceleratorSetup.getMass();
		}
		return 0;
	}
	
	@Override
	public double getIsolationRate() {
		return 0.0;
	}
	
	@Override
	public String getSignatureName() {
		return null;
	}
	
	@Override
	public boolean onBlockUpdatingInArea(@Nullable final Entity entity, final BlockPos blockPos, final BlockState blockState) {
		// skip in case of explosion, etc.
		if (isDirtyAssembly()) {
			return true;
		}
		
		// check for significant change
		// (we don't check the controller itself: it'll be triggered in remove() and we don't want to reevaluate the setup at that point)
		if ( blockState.getBlock() instanceof BlockAbstractAccelerator
		  || blockState.getBlock() instanceof BlockCapacitor ) {
			if (WarpDriveConfig.LOGGING_ACCELERATOR) {
				WarpDrive.logger.info(String.format("onBlockUpdatingInArea %s %s",
				                                    blockState,
				                                    Commons.format(world, blockPos) ));
			}
			markDirtyAssembly();
		}
		return true;
	}
}

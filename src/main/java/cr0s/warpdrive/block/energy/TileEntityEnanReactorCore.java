package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.ReactorFace;
import cr0s.warpdrive.data.EnumReactorOutputMode;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.Arrays;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityEnanReactorCore extends TileEntityEnanReactorController implements IGlobalRegionProvider {
	
	// generation & instability is 'per tick'
	private static final double INSTABILITY_MIN = 0.004D;
	private static final double INSTABILITY_MAX = 0.060D;
	
	// laser stabilization is per shot
	// target is to consume 10% max output power every second, hence 2.5% per side
	// laser efficiency is 33% at 16% power (target spot), 50% at 24% power, 84% at 50% power, etc.
	// 10% * 20 * PR_MAX_GENERATION / (4 * 0.16) => ~200kRF => ~ max laser energy
	private static final double PR_MAX_LASER_ENERGY = 200000.0D;
	private static final double PR_MAX_LASER_EFFECT = INSTABILITY_MAX * 20 / 0.33D;
	
	// radius scaling so the model doesn't 'eat' the stabilization lasers
	private static final float MATTER_SURFACE_MIN = 0.25F;
	private static final float MATTER_SURFACE_FACTOR = 1.15F;
	
	
	// persistent properties
	private EnumReactorOutputMode enumReactorOutputMode = EnumReactorOutputMode.OFF;
	private int outputThreshold = 0;
	private double instabilityTarget = 50.0D;
	private int stabilizerEnergy = 10000;
	
	private int containedEnergy = 0;
	private final double[] instabilityValues = new double[ReactorFace.maxInstabilities]; // no instability  = 0, explosion = 100
	
	// computed properties
	private boolean hold = true; // hold updates and power output until reactor is controlled (i.e. don't explode on chunk-loading while computer is booting)
	private AxisAlignedBB aabbRender = null;
	private Vector3 vCenter = null;
	private AxisAlignedBB cache_aabbArea;
	private boolean isFirstException = true;
	private int energyStored_max;
	private int generation_offset;
	private int generation_range;
	
	private int updateTicks = 0;
	
	private float lasersReceived = 0;
	private int lastGenerationRate = 0;
	private int releasedThisTick = 0; // amount of energy released during current tick update
	private long releasedThisCycle = 0; // amount of energy released during current cycle
	private long energyReleasedLastCycle = 0;
	
	// client properties
	public float client_yCore = 0.0F;
	public float client_yCoreSpeed_mPerTick = 0.0F;
	public float client_rotationCore_deg = 0.0F;
	public float client_rotationSpeedCore_degPerTick = 2.0F;
	public float client_rotationMatter_deg = 0.0F;
	public float client_rotationSpeedMatter_degPerTick = 2.0F;
	public float client_rotationSurface_deg = 0.0F;
	public float client_rotationSpeedSurface_degPerTick = 2.0F;
	public float client_radiusMatter_m = 0.0F;
	public float client_radiusSpeedMatter_mPerTick = 0.0F;
	public float client_radiusShield_m = 0.0F;
	public float client_radiusSpeedShield_mPerTick = 0.0F;
	
	@SuppressWarnings("unchecked")
	private final WeakReference<TileEntityEnanReactorLaser>[] weakTileEntityLasers = (WeakReference<TileEntityEnanReactorLaser>[]) Array.newInstance(WeakReference.class, ReactorFace.maxInstabilities);
	
	public TileEntityEnanReactorCore() {
		super();
		
		peripheralName = "warpdriveEnanReactorCore";
		
		// disable reactor by default
		isEnabled = false;
	}
	
	@Override
	public void onConstructed() {
		super.onConstructed();
		
		energyStored_max  = WarpDriveConfig.ENAN_REACTOR_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()];
		generation_offset = WarpDriveConfig.ENAN_REACTOR_GENERATION_MIN_RF_BY_TIER[enumTier.getIndex()];
		generation_range  = WarpDriveConfig.ENAN_REACTOR_GENERATION_MAX_RF_BY_TIER[enumTier.getIndex()] - generation_offset;
		
		energy_setParameters(EnergyWrapper.convertRFtoInternal_floor(energyStored_max),
		                     262144, 262144,
		                     "HV", 0, "LuV", 2);
		
		vCenter = new Vector3(this).translate(0.5D);
		switch (enumTier) {
		case BASIC:
		default:
			break;
		case ADVANCED:
			vCenter.y += 3;
			break;
		case SUPERIOR:
			vCenter.y += 4;
			break;
		}
	}
	
	@Nonnull
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		if (aabbRender == null) {
			final double radiusMatterMax = isFirstTick() ? 3.0D : vCenter.y - pos.getY();
			aabbRender = new AxisAlignedBB(
					pos.getX() - radiusMatterMax       , pos.getY()                      , pos.getZ() - radiusMatterMax       ,
					pos.getX() + radiusMatterMax + 1.0D, pos.getY() + 2 * radiusMatterMax, pos.getZ() + radiusMatterMax + 1.0D );
		}
		return aabbRender;
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		// we start at 0.5F to have a small animation on block placement
		client_yCore = containedEnergy == 0 ? 0.5F : (float) vCenter.y - pos.getY();
		client_yCoreSpeed_mPerTick = 0.0F;
		
		client_rotationCore_deg = world.rand.nextFloat() * 360.0F;
		client_rotationSpeedCore_degPerTick = 0.05F * (float) instabilityValues[0];
		
		client_rotationMatter_deg = world.rand.nextFloat() * 360.0F;
		client_rotationSpeedMatter_degPerTick = client_rotationSpeedCore_degPerTick * 0.98F;
		
		client_rotationSurface_deg = world.rand.nextFloat() * 360.0F;
		client_rotationSpeedSurface_degPerTick = client_rotationSpeedMatter_degPerTick;
		
		client_radiusMatter_m = 0.0F;
		client_radiusSpeedMatter_mPerTick = 0.0F;
		
		client_radiusShield_m = containedEnergy <= 10000 ? 0.0F : (float) (vCenter.y - pos.getY() - 1.0F);
		client_radiusSpeedShield_mPerTick = 0.0F;
		
		// force a new render bounding box in case render happened too soon
		aabbRender = null;
	}
	
	private void client_update() {
		float instabilityAverage = 0.0F;
		final ReactorFace[] reactorFaces = ReactorFace.getLasers(enumTier);
		for (final ReactorFace reactorFace : reactorFaces) {
			instabilityAverage += (float) instabilityValues[reactorFace.indexStability];
		}
		instabilityAverage /= reactorFaces.length;
		
		final float radiusArea = (float) (vCenter.y - pos.getY() - 1.0F);
		final float yCoreTarget = containedEnergy == 0 ? 1.0F : (radiusArea + 1.0F);
		final float rotationSpeedTarget_degPerTick = 0.05F * instabilityAverage;
		final float radiusMatterMax = radiusArea - 0.10F;
		final float radiusMatterTarget = containedEnergy <= 10000 ? 0.0F : MATTER_SURFACE_MIN + (radiusMatterMax - MATTER_SURFACE_MIN) / MATTER_SURFACE_FACTOR
		                                                                                      * (float) Math.pow(containedEnergy / (float) energyStored_max, 0.3333D);
		final float radiusShieldTarget = containedEnergy <= 1000 ? 0.0F : Math.min(radiusArea - 0.05F, (float) Math.ceil(radiusMatterTarget * 3.0F + 0.8F) / 3.0F);
		
		// linear shield growth
		client_radiusShield_m += client_radiusSpeedShield_mPerTick;
		final float radiusShieldDelta = radiusShieldTarget - client_radiusShield_m;
		client_radiusSpeedShield_mPerTick = Math.signum(radiusShieldDelta) * Math.min(0.015F, Math.abs(radiusShieldDelta));
		
		// elastic rotation
		client_rotationCore_deg = (client_rotationCore_deg + client_rotationSpeedCore_degPerTick) % 360.0F;
		client_rotationSpeedCore_degPerTick = 0.975F * client_rotationSpeedCore_degPerTick
		                                    + 0.025F * rotationSpeedTarget_degPerTick;
		client_rotationMatter_deg = (client_rotationMatter_deg + client_rotationSpeedMatter_degPerTick) % 360.0F;
		client_rotationSpeedMatter_degPerTick = 0.985F * client_rotationSpeedMatter_degPerTick
		                                      + 0.015F * rotationSpeedTarget_degPerTick;
		client_rotationSurface_deg = (client_rotationSurface_deg + client_rotationSpeedSurface_degPerTick) % 360.0F;
		client_rotationSpeedSurface_degPerTick = 0.990F * client_rotationSpeedSurface_degPerTick
		                                       + 0.010F * rotationSpeedTarget_degPerTick;
		
		// linear radius
		client_radiusMatter_m += client_radiusSpeedMatter_mPerTick;
		final float radiusMatterDelta = radiusMatterTarget - client_radiusMatter_m;
		client_radiusSpeedMatter_mPerTick = Math.signum(radiusMatterDelta) * Math.min(0.05F, Math.abs(radiusMatterDelta));
		
		// linear position
		client_yCore += client_yCoreSpeed_mPerTick;
		final float yDelta = yCoreTarget - client_yCore;
		client_yCoreSpeed_mPerTick = Math.signum(yDelta) * Math.min(0.05F, Math.abs(yDelta));
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			client_update();
			return;
		}
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("updateTicks %d releasedThisTick %6d lasersReceived %.5f releasedThisCycle %6d containedEnergy %8d",
			                                    updateTicks, releasedThisTick, lasersReceived, releasedThisCycle, containedEnergy ));
		}
		releasedThisTick = 0;
		
		lasersReceived = Math.max(0.0F, lasersReceived - 0.05F);
		updateTicks--;
		if (updateTicks > 0) {
			return;
		}
		updateTicks = WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS;
		energyReleasedLastCycle = releasedThisCycle;
		releasedThisCycle = 0;
		
		refreshBlockState();
		
		if (!hold) {// still loading/booting => hold simulation
			// unstable at all time
			if (shouldExplode()) {
				explode();
			}
			increaseInstability();
			
			generateEnergy();
			
			runControlLoop();
		}
		
		sendEvent("reactorPulse", lastGenerationRate);
	}
	
	private void increaseInstability() {
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			// increase instability
			final int indexStability = reactorFace.indexStability;
			if (containedEnergy > 2000) {
				final double amountToIncrease = WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS
						* Math.max(INSTABILITY_MIN, INSTABILITY_MAX * Math.pow((world.rand.nextDouble() * containedEnergy) / energyStored_max, 0.1));
				if (WarpDriveConfig.LOGGING_ENERGY) {
					WarpDrive.logger.info(String.format("increaseInstability %.5f",
					                                    amountToIncrease));
				}
				instabilityValues[indexStability] += amountToIncrease;
			} else {
				// when charge is extremely low, reactor is naturally stabilizing, to avoid infinite decay
				final double amountToDecrease = WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * Math.max(INSTABILITY_MIN, instabilityValues[indexStability] * 0.02D);
				instabilityValues[indexStability] = Math.max(0.0D, instabilityValues[indexStability] - amountToDecrease);
			}
		}
	}
	
	void decreaseInstability(@Nonnull final ReactorFace reactorFace, final int energy) {
		if (reactorFace.indexStability < 0) {
			return;
		}
		
		final int amount = EnergyWrapper.convertInternalToRF_floor(energy);
		if (amount <= 1) {
			return;
		}
		
		lasersReceived = Math.min(10.0F, lasersReceived + 1.0F / WarpDriveConfig.ENAN_REACTOR_MAX_LASERS_PER_SECOND[enumTier.getIndex()]);
		double nospamFactor = 1.0D;
		if (lasersReceived > 1.0F) {
			nospamFactor = 0.5;
			world.newExplosion(null,
			                   pos.getX() + reactorFace.x - reactorFace.facingLaserProperty.getXOffset(),
			                   pos.getY() + reactorFace.y - reactorFace.facingLaserProperty.getYOffset(),
			                   pos.getZ() + reactorFace.z - reactorFace.facingLaserProperty.getZOffset(),
			                   1.0F, false, false);
		}
		final double normalisedAmount = Math.min(1.0D, Math.max(0.0D, amount / PR_MAX_LASER_ENERGY)); // 0.0 to 1.0
		final double baseLaserEffect = 0.5D + 0.5D * Math.cos( Math.PI * Math.log10(0.1D + 0.9D * normalisedAmount) ); // 0.0 to 1.0
		final double randomVariation = 0.8D + 0.4D * world.rand.nextDouble(); // ~1.0
		final double amountToRemove = PR_MAX_LASER_EFFECT * baseLaserEffect * randomVariation * nospamFactor;
		
		final int indexStability = reactorFace.indexStability;
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			if (indexStability == 3) {
				WarpDrive.logger.info(String.format("Instability on %s decreased by %.1f/%.1f after consuming %d/%.1f laserReceived is %.1f hence nospamFactor is %.3f",
				                                    reactorFace, amountToRemove, PR_MAX_LASER_EFFECT,
				                                    amount, PR_MAX_LASER_ENERGY, lasersReceived, nospamFactor));
			}
		}
		
		instabilityValues[indexStability] = Math.max(0, instabilityValues[indexStability] - amountToRemove);
	}
	
	private void generateEnergy() {
		double stabilityOffset = 0.5;
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			stabilityOffset *= Math.max(0.01D, instabilityValues[reactorFace.indexStability] / 100.0D);
		}
		
		if (isEnabled) {// producing, instability increases output, you want to take the risk
			final int amountToGenerate = (int) Math.ceil(WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * (0.5D + stabilityOffset)
					* ( generation_offset
					  + generation_range * Math.pow(containedEnergy / (double) energyStored_max, 0.6D)));
			containedEnergy = Math.min(containedEnergy + amountToGenerate, energyStored_max);
			lastGenerationRate = amountToGenerate / WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS;
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("Generated %d", amountToGenerate));
			}
		} else {// decaying over 20s without producing power, you better have power for those lasers
			final int amountToDecay = (int) (WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS * (1.0D - stabilityOffset) * (generation_offset + containedEnergy * 0.01D));
			containedEnergy = Math.max(0, containedEnergy - amountToDecay);
			lastGenerationRate = 0;
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("Decayed %d", amountToDecay));
			}
		}
	}
	
	private void runControlLoop() {
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			if (instabilityValues[reactorFace.indexStability] > instabilityTarget) {
				final TileEntityEnanReactorLaser tileEntityEnanReactorLaser = getLaser(reactorFace);
				if (tileEntityEnanReactorLaser != null) {
					if (tileEntityEnanReactorLaser.stabilize(stabilizerEnergy) == -stabilizerEnergy) {
						// chunk isn't updating properly => protect the reactor
						hold = true;
						// report to console
						if (Commons.throttleMe(String.format("Reactor simulation hold %s", Commons.format(world, pos)))) {
							WarpDrive.logger.warn(String.format("Reactor %s simulation is now on hold for %d ticks due to partial loading of laser %s",
							                                    Commons.format(world, pos), updateTicks,
							                                    Commons.format(world, tileEntityEnanReactorLaser.getPos()) ));
						}
					}
				}
			}
		}
		// chunk isn't updating properly => protect the reactor
		if (hold) {
			// delay simulation for a few seconds
			updateTicks = Math.max(updateTicks, WarpDriveConfig.ENAN_REACTOR_FREEZE_INTERVAL_TICKS);
			// force an assembly check
			markDirtyAssembly();
			// protect the reactor
			for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
				if (instabilityValues[reactorFace.indexStability] > instabilityTarget) {
					instabilityValues[reactorFace.indexStability] = instabilityTarget;
				}
			}
		}
	}
	
	@Nullable
	private TileEntityEnanReactorLaser getLaser(@Nonnull final ReactorFace reactorFace) {
		final WeakReference<TileEntityEnanReactorLaser> weakTileEntityLaser = weakTileEntityLasers[reactorFace.indexStability];
		TileEntityEnanReactorLaser tileEntityEnanReactorLaser;
		if (weakTileEntityLaser != null) {
			tileEntityEnanReactorLaser = weakTileEntityLaser.get();
			if ( tileEntityEnanReactorLaser != null
			  && !tileEntityEnanReactorLaser.isInvalid() ) {
				return tileEntityEnanReactorLaser;
			}
		}
		final TileEntity tileEntity = world.getTileEntity(
				pos.add(reactorFace.x, reactorFace.y, reactorFace.z));
		if (tileEntity instanceof TileEntityEnanReactorLaser) {
			tileEntityEnanReactorLaser =(TileEntityEnanReactorLaser) tileEntity;
			weakTileEntityLasers[reactorFace.indexStability] = new WeakReference<>(tileEntityEnanReactorLaser);
			return tileEntityEnanReactorLaser;
		}
		return null;
	}
	
	Vector3 getCenter() {
		finishConstruction();
		return vCenter;
	}
	
	private boolean shouldExplode() {
		boolean exploding = false;
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			exploding = exploding || (instabilityValues[reactorFace.indexStability] >= 100);
		}
		exploding &= (world.rand.nextInt(4) == 2);
		
		if (exploding) {
			final StringBuilder statusLasers = new StringBuilder();
			final MutableBlockPos mutableBlockPos = new MutableBlockPos();
			for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
				long energyStored = -1L;
				int countLaserMediums = 0;
				mutableBlockPos.setPos(
						pos.getX() + reactorFace.x,
						pos.getY() + reactorFace.y,
						pos.getZ() + reactorFace.z );
				final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
				if (tileEntity instanceof TileEntityEnanReactorLaser) {
					try {
						energyStored = ((TileEntityEnanReactorLaser) tileEntity).laserMedium_getEnergyStored(true);
						countLaserMediums = ((TileEntityEnanReactorLaser) tileEntity).laserMedium_getCount();
					} catch (final Exception exception) {
						if (isFirstException) {
							exception.printStackTrace(WarpDrive.printStreamError);
							isFirstException = false;
						}
						WarpDrive.logger.error(String.format("%s tileEntity is %s",
						                                     this, tileEntity ));
					}
					statusLasers.append(String.format("\n- face %s has reached instability %.2f while laser %s has %d energy available with %d laser medium(s)",
					                                  reactorFace.name,
					                                  instabilityValues[reactorFace.indexStability],
					                                  Commons.format(world, mutableBlockPos),
					                                  energyStored,
					                                  countLaserMediums ));
				} else {
					statusLasers.append(String.format("\n- face %s has reached instability %.2f while laser is missing in action",
					                                 reactorFace.name,
					                                 instabilityValues[reactorFace.indexStability] ));
				}
			}
			
			WarpDrive.logger.info(String.format("%s Explosion triggered\n" +
			                                    "Energy stored is %d, Laser received is %.2f, Reactor is %s\n" +
			                                    "Output mode %s %d, Stability target %.1f, Laser amount %d%s",
			                                    this,
			                                    containedEnergy, lasersReceived, isEnabled ? "ENABLED" : "DISABLED",
			                                    enumReactorOutputMode, outputThreshold, 100.0D - instabilityTarget, stabilizerEnergy,
			                                    statusLasers.toString() ));
			isEnabled = false;
		}
		return exploding;
	}
	
	private void explode() {
		// remove blocks randomly up to x blocks around (breaking whatever protection is there)
		final double normalizedEnergy = containedEnergy / (double) energyStored_max;
		final double factorEnergy = Math.pow(normalizedEnergy, 0.125);
		final int radius = (int) Math.round( WarpDriveConfig.ENAN_REACTOR_EXPLOSION_MAX_RADIUS_BY_TIER[enumTier.getIndex()]
		                                   * factorEnergy );
		final double chanceOfRemoval = WarpDriveConfig.ENAN_REACTOR_EXPLOSION_MAX_REMOVAL_CHANCE_BY_TIER[enumTier.getIndex()]
		                             * factorEnergy;
		WarpDrive.logger.info(String.format("%s Explosion radius is %d, Chance of removal is %.3f",
		                                    this, radius, chanceOfRemoval ));
		if (radius > 1) {
			final MutableBlockPos mutableBlockPos = new MutableBlockPos(pos);
			final Explosion explosion = new Explosion(world, null, vCenter.x, vCenter.y, vCenter.z, radius, true, true);
			final float explosionResistanceThreshold = Blocks.OBSIDIAN.getExplosionResistance(world, mutableBlockPos, null, explosion);
			for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
				for (int y = pos.getY() - radius; y <= pos.getY() + radius; y++) {
					for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
						if (z != pos.getZ() || y != pos.getY() || x != pos.getX()) {
							if (world.rand.nextDouble() < chanceOfRemoval) {
								mutableBlockPos.setPos(x, y, z);
								final IBlockState blockState = world.getBlockState(mutableBlockPos);
								final float explosionResistanceActual = blockState.getBlock().getExplosionResistance(world, mutableBlockPos, null, explosion);
								if (explosionResistanceActual >= explosionResistanceThreshold) {
									WarpDrive.logger.debug(String.format("%s De-materializing %s %s",
									                                     this, blockState, Commons.format(world, mutableBlockPos) ));
									world.setBlockToAir(mutableBlockPos);
								}
							}
						}
					}
				}
			}
		}
		
		// remove reactor
		world.setBlockToAir(pos);
		
		// set a few augmented TnT around reactor core
		final int countExplosions = WarpDriveConfig.ENAN_REACTOR_EXPLOSION_COUNT_BY_TIER[enumTier.getIndex()];
		final float strengthMin = WarpDriveConfig.ENAN_REACTOR_EXPLOSION_STRENGTH_MIN_BY_TIER[enumTier.getIndex()];
		final int strengthRange = (int) Math.ceil(WarpDriveConfig.ENAN_REACTOR_EXPLOSION_STRENGTH_MAX_BY_TIER[enumTier.getIndex()] - strengthMin);
		for (int i = 0; i < countExplosions; i++) {
			world.newExplosion(null,
			                   pos.getX() + world.rand.nextInt(3) - 1.5D,
			                   pos.getY() + world.rand.nextInt(3) - 0.5D,
			                   pos.getZ() + world.rand.nextInt(3) - 1.5D,
				               strengthMin + world.rand.nextInt(strengthRange), true, true);
		}
	}
	
	private void refreshBlockState() {
		double maxInstability = 0.0D;
		for (final Double instability : instabilityValues) {
			if (instability > maxInstability) {
				maxInstability = instability;
			}
		}
		final int instabilityNibble = (int) Math.max(0, Math.min(3, Math.round(maxInstability / 25.0D)));
		final int energyNibble = (int) Math.max(0, Math.min(3, Math.round(4.0D * containedEnergy / energyStored_max)));
		
		final IBlockState blockState = world.getBlockState(pos);
		if (isInvalidBlockState(blockState, BlockEnanReactorCore.class, BlockEnanReactorCore.ENERGY, BlockEnanReactorCore.INSTABILITY)) {
			return;
		}
		final IBlockState blockStateNew = blockState.getBlock().getDefaultState()
		                                                .withProperty(BlockEnanReactorCore.ENERGY, energyNibble)
		                                                .withProperty(BlockEnanReactorCore.INSTABILITY, instabilityNibble);
		updateBlockState(null, blockStateNew);
		
		world.notifyBlockUpdate(pos, blockStateNew, blockStateNew, 3);
	}
	
	@Override
	public void onBlockBroken(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		// disconnect the stabilization lasers
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			if (reactorFace.indexStability < 0) {
				continue;
			}
			
			mutableBlockPos.setPos(blockPos.getX() + reactorFace.x,
			                       blockPos.getY() + reactorFace.y,
			                       blockPos.getZ() + reactorFace.z );
			final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
			if ( tileEntity instanceof TileEntityEnanReactorLaser
			  && ((TileEntityEnanReactorLaser) tileEntity).getReactorFace() == reactorFace ) {
				((TileEntityEnanReactorLaser) tileEntity).setReactorFace(ReactorFace.UNKNOWN, null);
			}
		}
		
		super.onBlockBroken(world, blockPos, blockState);
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		boolean isValid = super.doScanAssembly(isDirty, textReason);
		
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
		
		// first check if we have the required 'air' blocks
		for (final ReactorFace reactorFace : ReactorFace.get(enumTier)) {
			assert reactorFace.enumTier == enumTier;
			if (reactorFace.indexStability < 0) {
				mutableBlockPos.setPos(pos.getX() + reactorFace.x,
				                       pos.getY() + reactorFace.y,
				                       pos.getZ() + reactorFace.z);
				final IBlockState blockState = world.getBlockState(mutableBlockPos);
				final boolean isAir = blockState.getBlock().isAir(blockState, world, mutableBlockPos);
				if (!isAir) {
					textReason.append(Commons.getStyleWarning(), "warpdrive.enan_reactor.status_line.non_air_block",
					                  Commons.format(world, mutableBlockPos) );
					isValid = false;
					PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
					                                      new Vector3(mutableBlockPos.getX() + 0.5D, mutableBlockPos.getY() + 0.5D, mutableBlockPos.getZ() + 0.5D),
					                                      new Vector3(0.0D, 0.0D, 0.0D),
					                                      1.0F, 1.0F, 1.0F,
					                                      1.0F, 1.0F, 1.0F,
					                                      32);
				}
			}
		}
		
		// then update the stabilization lasers accordingly
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			mutableBlockPos.setPos(pos.getX() + reactorFace.x,
			                       pos.getY() + reactorFace.y,
			                       pos.getZ() + reactorFace.z);
			final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
			if (tileEntity instanceof TileEntityEnanReactorLaser) {
				((TileEntityEnanReactorLaser) tileEntity).setReactorFace(reactorFace, this);
			} else {
				textReason.append(Commons.getStyleWarning(), "warpdrive.enan_reactor.status_line.missing_stabilization_laser",
				                  Commons.format(world, mutableBlockPos) );
				isValid = false;
				PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
				                                      new Vector3(mutableBlockPos.getX() + 0.5D, mutableBlockPos.getY() + 0.5D, mutableBlockPos.getZ() + 0.5D),
				                                      new Vector3(0.0D, 0.0D, 0.0D),
				                                      1.0F, 1.0F, 1.0F,
				                                      1.0F, 1.0F, 1.0F,
				                                      32);
			}
		}
		
		return isValid;
	}
	
	// IGlobalRegionProvider overrides
	@Override
	public EnumGlobalRegionType getGlobalRegionType() {
		return EnumGlobalRegionType.REACTOR;
	}
	
	@Override
	public AxisAlignedBB getGlobalRegionArea() {
		if (cache_aabbArea == null) {
			int minX = 0;
			int minY = 0;
			int minZ = 0;
			int maxX = 0;
			int maxY = 0;
			int maxZ = 0;
			for (final ReactorFace reactorFace : ReactorFace.getLasers(EnumTier.get(getTierIndex()))) {
				minX = Math.min(minX, reactorFace.x);
				minY = Math.min(minY, reactorFace.y);
				minZ = Math.min(minZ, reactorFace.z);
				maxX = Math.max(maxX, reactorFace.x);
				maxY = Math.max(maxY, reactorFace.y);
				maxZ = Math.max(maxZ, reactorFace.z);
			}
			cache_aabbArea = new AxisAlignedBB(
					pos.getX() + minX       , pos.getY() + minY       , pos.getZ() + minZ       ,
					pos.getX() + maxX + 1.0D, pos.getY() + maxY + 1.0D, pos.getZ() + maxZ + 1.0D );
		}
		return cache_aabbArea;
	}
	
	@Override
	public int getMass() {
		return ReactorFace.getLasers(EnumTier.get(getTierIndex())).length;
	}
	
	@Override
	public double getIsolationRate() {
		return 1.0D;
	}
	
	@Override
	public boolean onBlockUpdatingInArea(@Nullable final Entity entity, final BlockPos blockPos, final IBlockState blockState) {
		// energy ducts are updating quite frequently, so we explicitly check for reactor faces here
		finishConstruction();
		for (final ReactorFace reactorFace : ReactorFace.get(enumTier)) {
			if ( blockPos.getX() == pos.getX() + reactorFace.x
			  && blockPos.getY() == pos.getY() + reactorFace.y
			  && blockPos.getZ() == pos.getZ() + reactorFace.z ) {
				markDirtyAssembly();
				break;
			}
		}
		
		return true;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyStatus() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				EnergyWrapper.convert(containedEnergy, units),
				EnergyWrapper.convert(energyStored_max, units),
				energy_getDisplayUnits(),
				0,
				EnergyWrapper.convert(energyReleasedLastCycle, units) / WarpDriveConfig.ENAN_REACTOR_UPDATE_INTERVAL_TICKS };
	}
	
	@Override
	public Double[] getInstabilities() {
		// computer is alive => start updating reactor
		hold = false;
		
		final ReactorFace[] lasers = ReactorFace.getLasers(enumTier);
		final Double[] result = new Double[lasers.length];
		for (final ReactorFace reactorFace : lasers) {
			final double value = instabilityValues[reactorFace.indexStability];
			result[reactorFace.indexStability] = value;
		}
		return result;
	}
	
	@Override
	public Double[] instabilityTarget(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			final double instabilityTargetRequested;
			try {
				instabilityTargetRequested = Commons.toDouble(arguments[0]);
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on instabilityTarget(): Double expected for 1st argument %s",
					                                     this, arguments[0]));
				}
				return new Double[] { instabilityTarget };
			}
			
			instabilityTarget = Commons.clamp(0.0D, 100.0D, instabilityTargetRequested);
		}
		return new Double[] { instabilityTarget };
	}
	
	@Override
	public Object[] outputMode(final Object[] arguments) {
		if ( arguments.length == 2
		  && arguments[0] != null ) {
			final EnumReactorOutputMode enumReactorOutputModeRequested;
			try {
				enumReactorOutputModeRequested = EnumReactorOutputMode.byName(arguments[0].toString());
				if (enumReactorOutputModeRequested == null) {
					throw new NullPointerException();
				}
			} catch (final Exception exception) {
				final String message = String.format("%s LUA error on outputMode(): enum(%s) expected for 1st argument %s",
				                                     this, Arrays.toString(EnumReactorOutputMode.values()), arguments[0]);
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(message);
				}
				return new Object[] { enumReactorOutputMode.getName(), outputThreshold };
			}
			
			final int outputThresholdRequested;
			try {
				outputThresholdRequested = Commons.toInt(arguments[1]);
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on outputMode(): integer expected for 2nd argument %s",
					                                     this, arguments[0]));
				}
				return new Object[] { enumReactorOutputMode.getName(), outputThreshold };
			}
			
			enumReactorOutputMode = enumReactorOutputModeRequested;
			outputThreshold = outputThresholdRequested;
		}
		return new Object[] { enumReactorOutputMode.getName(), outputThreshold };
	}
	
	@Override
	public Object[] stabilizerEnergy(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			final int stabilizerEnergyRequested;
			try {
				stabilizerEnergyRequested = Commons.toInt(arguments[0]);
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on stabilizerEnergy(): Integer expected for 1st argument %s",
					                                     this, arguments[0]));
				}
				return new Object[] { stabilizerEnergy };
			}
			
			stabilizerEnergy = Commons.clamp(0, Integer.MAX_VALUE, stabilizerEnergyRequested);
		}
		return new Object[] { stabilizerEnergy };
	}
	
	@Override
	public Object[] state() {
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isEnabled, containedEnergy, enumReactorOutputMode.getName(), outputThreshold };
	}
	
	// POWER INTERFACES
	@Override
	public int energy_getPotentialOutput() {
		if (hold) {// still loading/booting => hold output
			return 0;
		}
		
		// restrict max output rate to twice the generation
		final int capacity = Math.max(0, 2 * lastGenerationRate - releasedThisTick);
		
		int result = 0;
		if (enumReactorOutputMode == EnumReactorOutputMode.UNLIMITED) {
			result = Math.min(Math.max(0, containedEnergy), capacity);
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("PotentialOutput Manual %d RF (%d internal) capacity %d",
				                                    result, EnergyWrapper.convertRFtoInternal_floor(result), capacity));
			}
		} else if (enumReactorOutputMode == EnumReactorOutputMode.ABOVE) {
			result = Math.min(Math.max(0, lastGenerationRate - outputThreshold), capacity);
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("PotentialOutput Above %d RF (%d internal) capacity %d",
				                                    result, EnergyWrapper.convertRFtoInternal_floor(result), capacity));
			}
		} else if (enumReactorOutputMode == EnumReactorOutputMode.AT_RATE) {
			final int remainingRate = Math.max(0, outputThreshold - releasedThisTick);
			result = Math.min(Math.max(0, containedEnergy), Math.min(remainingRate, capacity));
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("PotentialOutput Rated %d RF (%d internal) remainingRate %d RF/t capacity %d",
				                                    result, EnergyWrapper.convertRFtoInternal_floor(result), remainingRate, capacity));
			}
		}
		return (int) EnergyWrapper.convertRFtoInternal_floor(result);
	}
	
	@Override
	public boolean energy_canOutput(final EnumFacing from) {
		if (enumTier == EnumTier.BASIC) {
			return from == null
			    || from.equals(EnumFacing.UP)
			    || from.equals(EnumFacing.DOWN);
		}
		return from == null
		    || !from.equals(EnumFacing.UP);
	}
	
	@Override
	protected void energy_outputDone(final long energyOutput_internal) {
		final long energyOutput_RF = EnergyWrapper.convertInternalToRF_ceil(energyOutput_internal);
		containedEnergy -= energyOutput_RF;
		if (containedEnergy < 0) {
			containedEnergy = 0;
		}
		releasedThisTick += energyOutput_RF;
		releasedThisCycle += energyOutput_RF;
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("OutputDone %d (%d RF)",
			                                    energyOutput_internal, energyOutput_RF));
		}
	}
	
	@Override
	public long energy_getEnergyStored() {
		return Commons.clamp(0L, energy_getMaxStorage(), EnergyWrapper.convertRFtoInternal_floor(containedEnergy));
	}
	
	// Forge overrides
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setString("outputMode", enumReactorOutputMode.getName());
		tagCompound.setInteger("outputThreshold", outputThreshold);
		tagCompound.setDouble("instabilityTarget", instabilityTarget);
		tagCompound.setInteger("stabilizerEnergy", stabilizerEnergy);
		
		tagCompound.setInteger("energy", containedEnergy);
		final NBTTagCompound tagCompoundInstability = new NBTTagCompound();
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			tagCompoundInstability.setDouble(reactorFace.name, instabilityValues[reactorFace.indexStability]);
		}
		tagCompound.setTag("instability", tagCompoundInstability);
		return tagCompound;
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		// skip empty NBT on placement to use defaults values
		if ( (world == null || !world.isRemote)
		  && !tagCompound.hasKey("outputMode") ) {
			return;
		}
		
		enumReactorOutputMode = EnumReactorOutputMode.byName(tagCompound.getString("outputMode"));
		if (enumReactorOutputMode == null) {
			enumReactorOutputMode = EnumReactorOutputMode.OFF;
		}
		outputThreshold = tagCompound.getInteger("outputThreshold");
		instabilityTarget = tagCompound.getDouble("instabilityTarget");
		stabilizerEnergy = tagCompound.getInteger("stabilizerEnergy");
		
		containedEnergy = tagCompound.getInteger("energy");
		final NBTTagCompound tagCompoundInstability = tagCompound.getCompoundTag("instability");
		// tier isn't defined yet, so we check all candidates
		for (final ReactorFace reactorFace : ReactorFace.getLasers()) {
			if (reactorFace.indexStability < 0) {
				continue;
			}
			if (tagCompoundInstability.hasKey(reactorFace.name)) {
				instabilityValues[reactorFace.indexStability] = tagCompoundInstability.getDouble(reactorFace.name);
			}
		}
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		tagCompound.removeTag("outputMode");
		tagCompound.removeTag("outputThreshold");
		tagCompound.removeTag("instabilityTarget");
		tagCompound.removeTag("stabilizerEnergy");
		
		tagCompound.removeTag("energy");
		tagCompound.removeTag("instability");
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = super.getUpdateTag();
		
		tagCompound.removeTag("outputMode");
		tagCompound.removeTag("outputThreshold");
		tagCompound.removeTag("instabilityTarget");
		tagCompound.removeTag("stabilizerEnergy");
		
		return tagCompound;
	}
	
	@Override
	public void setDebugValues() {
		super.setDebugValues();
		containedEnergy = energyStored_max;
		for (final ReactorFace reactorFace : ReactorFace.getLasers(enumTier)) {
			instabilityValues[reactorFace.indexStability] = 50.0D;
		}
	}
}

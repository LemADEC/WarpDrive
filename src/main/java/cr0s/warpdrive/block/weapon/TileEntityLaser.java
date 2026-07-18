package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IDamageReceiver;
import cr0s.warpdrive.block.TileEntityAbstractLaser;
import cr0s.warpdrive.block.force_field.BlockForceField;
import cr0s.warpdrive.block.force_field.TileEntityForceField;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.ForceFieldSetup;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.entity.EntityLaserExploder;
import cr0s.warpdrive.network.PacketHandler;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.Explosion;
import net.minecraft.world.Explosion.Mode;
import net.minecraft.world.World;

public class TileEntityLaser extends TileEntityAbstractLaser implements IBeamFrequency {
	
	private float yaw, pitch; // laser direction
	
	protected int beamFrequency = -1;
	private float r, g, b; // beam color (corresponds to frequency)
	
	private boolean isEmitting = false;
	
	private int delayTicks = 0;
	private int energyFromOtherBeams = 0;
	
	private enum ScanResultType {
		IDLE("IDLE"), BLOCK("BLOCK"), NONE("NONE");
		
		public final String name;
		
		ScanResultType(final String name) {
			this.name = name;
		}
	}
	private ScanResultType scanResult_type = ScanResultType.IDLE;
	private BlockPos scanResult_position = null;
	private String scanResult_blockUnlocalizedName;
	private String scanResult_blockProperties;
	private float scanResult_blockResistance = -2;
	
	public TileEntityLaser(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
		
		peripheralName = "warpdriveLaser";
		addMethods(new String[] {
			"emitBeam",
			"beamFrequency",
			"getScanResult"
		});
		doRequireUpgradeToInterface();
		
		laserMedium_maxCount = WarpDriveConfig.LASER_CANNON_MAX_MEDIUMS_COUNT;
	}
	
	@Override
	public void tick() {
		super.tick();
		
		// Frequency is not set
		if ( !IBeamFrequency.isValid(beamFrequency) ) {
			return;
		}
		
		delayTicks++;
		if ( isEmitting
		  && ( (beamFrequency != IBeamFrequency.BEAM_FREQUENCY_SCANNING && delayTicks > WarpDriveConfig.LASER_CANNON_EMIT_FIRE_DELAY_TICKS)
		    || (beamFrequency == IBeamFrequency.BEAM_FREQUENCY_SCANNING && delayTicks > WarpDriveConfig.LASER_CANNON_EMIT_SCAN_DELAY_TICKS) )) {
			delayTicks = 0;
			isEmitting = false;
			final int beamEnergy = Math.min(
					laserMedium_consumeUpTo(Integer.MAX_VALUE, false) + MathHelper.floor(energyFromOtherBeams * WarpDriveConfig.LASER_CANNON_BOOSTER_BEAM_ENERGY_EFFICIENCY),
					WarpDriveConfig.LASER_CANNON_MAX_LASER_ENERGY);
			emitBeam(beamEnergy);
			energyFromOtherBeams = 0;
			sendEvent("laserSend", beamFrequency, beamEnergy);
		}
	}
	
	public void initiateBeamEmission(final float parYaw, final float parPitch) {
		yaw = parYaw;
		pitch = parPitch;
		delayTicks = 0;
		isEmitting = true;
	}
	
	private void addBeamEnergy(final int amount) {
		if (isEmitting) {
			energyFromOtherBeams += amount;
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info(String.format("%s Added boosting energy %d for a total accumulation of %d",
				                                    this, amount, energyFromOtherBeams));
			}
		} else {
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.warn(String.format("%s Ignored boosting energy %d",
				                                    this, amount));
			}
		}
	}
	
	// loosely based on World.rayTraceBlocks on MC1.7.10 or older
	// - replaced byte b0 with Direction enum
	// - inverted 2nd flag
	// - added force field pass through based on beamFrequency
	// - increased max range from 200 to laser limit
	// - code cleanup
	@Nullable
	public static BlockRayTraceResult rayTraceBlocks(@Nonnull final World world, @Nonnull final Vec3d vSource, @Nonnull final Vec3d vTarget,
	                                                 final int beamFrequency, final boolean checkBlockWithoutBoundingBox, final boolean returnLastUncollidableBlock) {
		// validate parameters
		if (Double.isNaN(vSource.x) || Double.isNaN(vSource.y) || Double.isNaN(vSource.z)) {
			return null;
		}
		
		if (Double.isNaN(vTarget.x) || Double.isNaN(vTarget.y) || Double.isNaN(vTarget.z)) {
			return null;
		}
		
		// check collision at source
		final int xSource = MathHelper.floor(vSource.x);
		final int ySource = MathHelper.floor(vSource.y);
		final int zSource = MathHelper.floor(vSource.z);
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(xSource, ySource, zSource);
		final BlockState blockStateSource = world.getBlockState(mutableBlockPos);
		
		if ((checkBlockWithoutBoundingBox || !blockStateSource.getCollisionShape(world, mutableBlockPos, ISelectionContext.dummy()).isEmpty())) {
			final BlockRayTraceResult rayTraceResult = blockStateSource.getRaytraceShape(world, mutableBlockPos).rayTrace(vSource, vTarget, mutableBlockPos);
			if (rayTraceResult != null) {
				return rayTraceResult;
			}
		}
		
		// loop positions along trajectory
		final int xTarget = MathHelper.floor(vTarget.x);
		final int yTarget = MathHelper.floor(vTarget.y);
		final int zTarget = MathHelper.floor(vTarget.z);
		
		final Vector3 v3Current = new Vector3(vSource.x, vSource.y, vSource.z);
		int xCurrent = xSource;
		int yCurrent = ySource;
		int zCurrent = zSource;
		BlockRayTraceResult rayTraceResultMissed = null;
		
		int countLoop = WarpDriveConfig.LASER_CANNON_RANGE_MAX * 2;
		while (countLoop-- >= 0) {
			// sanity check
			if (Double.isNaN(v3Current.x) || Double.isNaN(v3Current.y) || Double.isNaN(v3Current.z)) {
				WarpDrive.logger.error(String.format("Critical error while ray tracing blocks from %s to %s in %s",
				                                     vSource, vTarget, Commons.format(world)));
				return null;
			}
			
			// check arrival
			if (xCurrent == xTarget && yCurrent == yTarget && zCurrent == zTarget) {
				return returnLastUncollidableBlock ? rayTraceResultMissed : null;
			}
			
			// propose 1 block step along each axis
			boolean hasOffsetX = true;
			boolean hasOffsetY = true;
			boolean hasOffsetZ = true;
			double xProposed = 999.0D;
			double yProposed = 999.0D;
			double zProposed = 999.0D;
			
			if (xTarget > xCurrent) {
				xProposed = xCurrent + 1.0D;
			} else if (xTarget < xCurrent) {
				xProposed = xCurrent + 0.0D;
			} else {
				hasOffsetX = false;
			}
			
			if (yTarget > yCurrent) {
				yProposed = yCurrent + 1.0D;
			} else if (yTarget < yCurrent) {
				yProposed = yCurrent + 0.0D;
			} else {
				hasOffsetY = false;
			}
			
			if (zTarget > zCurrent) {
				zProposed = zCurrent + 1.0D;
			} else if (zTarget < zCurrent) {
				zProposed = zCurrent + 0.0D;
			} else {
				hasOffsetZ = false;
			}
			
			// compute normalized movement
			double xDeltaNormalized = 999.0D;
			double yDeltaNormalized = 999.0D;
			double zDeltaNormalized = 999.0D;
			final double xDeltaToTarget = vTarget.x - v3Current.x;
			final double yDeltaToTarget = vTarget.y - v3Current.y;
			final double zDeltaToTarget = vTarget.z - v3Current.z;
			
			if (hasOffsetX) {
				xDeltaNormalized = (xProposed - v3Current.x) / xDeltaToTarget;
				if (xDeltaNormalized == -0.0D) {
					xDeltaNormalized = -1.0E-4D;
				}
			}
			
			if (hasOffsetY) {
				yDeltaNormalized = (yProposed - v3Current.y) / yDeltaToTarget;
				if (yDeltaNormalized == -0.0D) {
					yDeltaNormalized = -1.0E-4D;
				}
			}
			
			if (hasOffsetZ) {
				zDeltaNormalized = (zProposed - v3Current.z) / zDeltaToTarget;
				if (zDeltaNormalized == -0.0D) {
					zDeltaNormalized = -1.0E-4D;
				}
			}
			
			// move along shortest axis
			final Direction enumFacing;
			if (xDeltaNormalized < yDeltaNormalized && xDeltaNormalized < zDeltaNormalized) {
				enumFacing = xTarget > xSource ? Direction.WEST : Direction.EAST;
				v3Current.x = xProposed;
				v3Current.y = v3Current.y + yDeltaToTarget * xDeltaNormalized;
				v3Current.z = v3Current.z + zDeltaToTarget * xDeltaNormalized;
			} else if (yDeltaNormalized < zDeltaNormalized) {
				enumFacing = yTarget > ySource ? Direction.DOWN : Direction.UP;
				v3Current.x = v3Current.x + xDeltaToTarget * yDeltaNormalized;
				v3Current.y = yProposed;
				v3Current.z = v3Current.z + zDeltaToTarget * yDeltaNormalized;
			} else {
				enumFacing = zTarget > zSource ? Direction.NORTH : Direction.SOUTH;
				v3Current.x = v3Current.x + xDeltaToTarget * zDeltaNormalized;
				v3Current.y = v3Current.y + yDeltaToTarget * zDeltaNormalized;
				v3Current.z = zProposed;
			}
			
			// round to block position
			xCurrent = MathHelper.floor(v3Current.x) - (enumFacing == Direction.EAST ? 1 : 0);
			yCurrent = MathHelper.floor(v3Current.y) - (enumFacing == Direction.UP ? 1 : 0);
			zCurrent = MathHelper.floor(v3Current.z) - (enumFacing == Direction.SOUTH ? 1 : 0);
			
			// get current block
			final BlockState blockStateCurrent = world.getBlockState(mutableBlockPos.setPos(xCurrent, yCurrent, zCurrent));
			
			// allow passing through force fields with same beam frequency
			if (blockStateCurrent.getBlock() instanceof BlockForceField) {
				final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
				if (tileEntity instanceof TileEntityForceField) {
					final ForceFieldSetup forceFieldSetup = ((TileEntityForceField) tileEntity).getForceFieldSetup();
					if (forceFieldSetup == null) {
						// projector not loaded yet, consider it jammed by default
						WarpDrive.logger.warn(String.format("Laser beam stopped by non-loaded force field projector at %s", tileEntity));
					} else {
						if (forceFieldSetup.beamFrequency == beamFrequency) {// pass-through force field
							if (WarpDriveConfig.LOGGING_WEAPON) {
								WarpDrive.logger.info(String.format("Laser beam passing through force field %s", tileEntity));
							}
							continue;
						}
					}
				}
			}
			
			if ( checkBlockWithoutBoundingBox
			  || blockStateCurrent.getMaterial() == Material.PORTAL
			  || !blockStateCurrent.getCollisionShape(world, mutableBlockPos, ISelectionContext.dummy()).isEmpty()) {
				final BlockRayTraceResult rayTraceResult = blockStateCurrent.getRaytraceShape(world, mutableBlockPos).rayTrace(v3Current.toVec3d(), vTarget, mutableBlockPos);
				if (rayTraceResult != null) {
					return rayTraceResult;
				} else {
					rayTraceResultMissed = BlockRayTraceResult.createMiss(v3Current.toVec3d(), enumFacing, mutableBlockPos);
				}
			}
		}
		
		return returnLastUncollidableBlock ? rayTraceResultMissed : null;
	}
	
	private void emitBeam(final int beamEnergy) {
		assert world != null;
		int energy = beamEnergy;
		
		final int beamLengthBlocks = Commons.clamp(0, WarpDriveConfig.LASER_CANNON_RANGE_MAX, energy / 200);
		
		if (energy == 0 || beamFrequency > 65000 || beamFrequency <= 0) {
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info(this + " Beam canceled (energy " + energy + " over " + beamLengthBlocks + " blocks, beamFrequency " + beamFrequency + ")");
			}
			return;
		}
		
		final float yawZ = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
		final float yawX = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
		final float pitchHorizontal = -MathHelper.cos(-pitch * 0.017453292F);
		final float pitchVertical = MathHelper.sin(-pitch * 0.017453292F);
		final float directionX = yawX * pitchHorizontal;
		final float directionZ = yawZ * pitchHorizontal;
		final Vector3 vDirection = new Vector3(directionX, pitchVertical, directionZ);
		final Vector3 vSource = new Vector3(this).translate(0.5D).translate(vDirection);
		final Vector3 vReachPoint = vSource.clone().translateFactor(vDirection, beamLengthBlocks);
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info(this + " Energy " + energy + " over " + beamLengthBlocks + " blocks"
					+ ", Orientation " + yaw + " " + pitch
					+ ", Direction " + vDirection
					+ ", From " + vSource + " to " + vReachPoint);
		}
		
		playSoundCorrespondsEnergy(energy);
		
		final Entity entityExploder = new EntityLaserExploder(world, pos);
		
		// This is a scanning beam, do not deal damage to block nor entity
		if (beamFrequency == IBeamFrequency.BEAM_FREQUENCY_SCANNING) {
			final BlockRayTraceResult mopResult = rayTraceBlocks(world, vSource.toVec3d(), vReachPoint.toVec3d(),
			                                                     beamFrequency, true, false );
			
			scanResult_blockUnlocalizedName = null;
			scanResult_blockProperties = null;
			scanResult_blockResistance = -2;
			if (mopResult != null) {
				scanResult_type = ScanResultType.BLOCK;
				scanResult_position = mopResult.getPos();
				final BlockState blockState = world.getBlockState(scanResult_position);
				scanResult_blockUnlocalizedName = blockState.getBlock().getTranslationKey();
				scanResult_blockProperties = blockState.toString().replace(scanResult_blockUnlocalizedName, "");
				final Explosion explosion = new Explosion(world, entityExploder,
				                                          scanResult_position.getX(), scanResult_position.getY(), scanResult_position.getZ(),
				                                          1, true, Mode.DESTROY );
				scanResult_blockResistance = blockState.getExplosionResistance(world, scanResult_position, entityExploder, explosion);
				PacketHandler.sendBeamPacket(world, vSource, new Vector3(mopResult.getHitVec()), r, g, b, 50, energy, 200);
			} else {
				scanResult_type = ScanResultType.NONE;
				scanResult_position = vReachPoint.getBlockPos();
				PacketHandler.sendBeamPacket(world, vSource, vReachPoint, r, g, b, 50, energy, 200);
			}
			
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info(String.format("Scan result type %s %s block %s@%s resistance %.1f",
				                                    scanResult_type.name, Commons.format(world, scanResult_position),
				                                    scanResult_blockUnlocalizedName, scanResult_blockProperties, scanResult_blockResistance));
			}
			
			sendEvent("laserScanning",
					scanResult_type.name, scanResult_position.getX(), scanResult_position.getY(), scanResult_position.getZ(),
					scanResult_blockUnlocalizedName, scanResult_blockProperties, scanResult_blockResistance);
			return;
		}
		
		// get colliding entities
		final TreeMap<Double, EntityRayTraceResult> entityHits = raytraceEntities(vSource.clone(), vDirection.clone(), beamLengthBlocks);
		
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info(String.format("Entity hits are (%d) %s",
			                                    (entityHits == null) ? 0 : entityHits.size(), entityHits ));
		}
		
		Vector3 vHitPoint = vReachPoint.clone();
		double distanceTravelled = 0.0D; // distance traveled from beam sender to previous hit if there were any
		for (int passedBlocks = 0; passedBlocks < beamLengthBlocks; passedBlocks++) {
			// Get next block hit
			final BlockRayTraceResult blockHit = rayTraceBlocks(world, vSource.toVec3d(), vReachPoint.toVec3d(),
			                                                    beamFrequency, true, false );
			double blockHitDistance = beamLengthBlocks + 0.1D;
			if (blockHit != null) {
				blockHitDistance = blockHit.getHitVec().distanceTo(vSource.toVec3d());
			}
			
			// Apply effect to entities
			if (entityHits != null) {
				for (final Entry<Double, EntityRayTraceResult> entityHitEntry : entityHits.entrySet()) {
					final double entityHitDistance = entityHitEntry.getKey();
					// ignore entities behind walls
					if (entityHitDistance >= blockHitDistance) {
						break;
					}
					
					// only hits entities with health or whitelisted
					final EntityRayTraceResult entityHit = entityHitEntry.getValue();
					if (entityHit == null) {
						continue;
					}
					LivingEntity entity = null;
					if (entityHit.getEntity() instanceof LivingEntity) {
						entity = (LivingEntity) entityHit.getEntity();
						if (WarpDriveConfig.LOGGING_WEAPON) {
							WarpDrive.logger.info(String.format("Entity is a valid target (living) %s", entity));
						}
					} else {
						if (!Dictionary.isNonLivingTarget(entityHit.getEntity())) {
							if (WarpDriveConfig.LOGGING_WEAPON) {
								WarpDrive.logger.info(String.format("Entity is an invalid target (non-living %s) %s",
								                                    Dictionary.getId(entityHit.getEntity()),
								                                    entityHit.getEntity() ));
							}
							// remove entity from hit list
							entityHits.put(entityHitDistance, null);
							continue;
						}
						if (WarpDriveConfig.LOGGING_WEAPON) {
							WarpDrive.logger.info(String.format("Entity is a valid target (non-living %s) %s",
							                                    Dictionary.getId(entityHit.getEntity()),
							                                    entityHit.getEntity() ));
						}
					}
					
					// Consume energy
					energy *= getTransmittance(entityHitDistance - distanceTravelled);
					energy -= WarpDriveConfig.LASER_CANNON_ENTITY_HIT_ENERGY;
					distanceTravelled = entityHitDistance;
					vHitPoint = new Vector3(entityHit.getHitVec());
					if (energy <= 0) {
						break;
					}
					
					// apply effects
					entityHit.getEntity().setFire(WarpDriveConfig.LASER_CANNON_ENTITY_HIT_SET_ON_FIRE_SECONDS);
					if (entity != null) {
						final float damage = (float) Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_ENTITY_HIT_MAX_DAMAGE,
								WarpDriveConfig.LASER_CANNON_ENTITY_HIT_BASE_DAMAGE + energy / (double) WarpDriveConfig.LASER_CANNON_ENTITY_HIT_ENERGY_PER_DAMAGE);
						entity.attackEntityFrom(DamageSource.IN_FIRE, damage);
					} else {
						entityHit.getEntity().remove();
					}
					
					if (energy > WarpDriveConfig.LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_THRESHOLD) {
						final float strength = (float) Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_ENTITY_HIT_EXPLOSION_MAX_STRENGTH,
							  WarpDriveConfig.LASER_CANNON_ENTITY_HIT_EXPLOSION_BASE_STRENGTH + energy / (double) WarpDriveConfig.LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_PER_STRENGTH);
						world.createExplosion(entityExploder, entityHit.getEntity().getPosX(), entityHit.getEntity().getPosY(), entityHit.getEntity().getPosZ(),
						                   strength, true, Mode.DESTROY );
					}
					
					// remove entity from hit list
					entityHits.put(entityHitDistance, null);
				}
				if (energy <= 0) {
					break;
				}
			}
			
			// Laser went too far or no block hit
			if (blockHitDistance >= beamLengthBlocks || blockHit == null) {
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info(String.format("No more blocks to hit or too far: blockHitDistance is %.1f, blockHit is %s",
					                                    blockHitDistance, blockHit));
				}
				vHitPoint = vReachPoint;
				break;
			}
			
			final BlockState blockState = world.getBlockState(blockHit.getPos());
			// get hardness and blast resistance
			float hardness = -2.0F;
			try {
				hardness = blockState.getBlockHardness(world, blockHit.getPos());
			} catch (final Exception exception) {
				if (Commons.throttleMe("TileEntityLaser.getBlockHardness")) {
					exception.printStackTrace(WarpDrive.printStreamError);
					WarpDrive.logger.error(String.format("Unable to access block hardness value of %s",
					                                     blockState.getBlock() ));
				}
			}
			if (blockState.getBlock() instanceof IDamageReceiver) {
				hardness = ((IDamageReceiver) blockState.getBlock()).getBlockHardness(blockState, world, blockHit.getPos(),
					WarpDrive.damageLaser, beamFrequency, vDirection, energy);
			}				
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info(String.format("Block collision found %s with block %s of hardness %.2f",
				                                    Commons.format(world, blockHit.getPos()),
				                                    blockState.getBlock().getRegistryName(), hardness));
			}
			
			// check area protection
			if (isBlockBreakCanceled(null, world, blockHit.getPos())) {
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info(String.format("Laser weapon cancelled %s",
					                                    Commons.format(world, blockHit.getPos())));
				}
				vHitPoint = new Vector3(blockHit.getHitVec());
				break;
			}
			
			// Boost a laser if it uses same beam frequency
			if ( blockState.getBlock() == WarpDrive.blockLaser
			  || blockState.getBlock() == WarpDrive.blockLaserCamera ) {
				final TileEntityLaser tileEntityLaser = (TileEntityLaser) world.getTileEntity(blockHit.getPos());
				if (tileEntityLaser != null && tileEntityLaser.getBeamFrequency() == beamFrequency) {
					tileEntityLaser.addBeamEnergy(energy);
					vHitPoint = new Vector3(blockHit.getHitVec());
					break;
				}
			}
			
			// explode on unbreakable blocks
			if (hardness < 0.0F) {
				final float strength = (float) Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_MAX_STRENGTH,
					WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_BASE_STRENGTH + energy / (double) WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_ENERGY_PER_STRENGTH);
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info(String.format("Explosion triggered with strength %.1f", strength));
				}
				world.createExplosion(entityExploder, blockHit.getHitVec().x, blockHit.getHitVec().y, blockHit.getHitVec().z,
				                   strength, true, Mode.DESTROY );
				vHitPoint = new Vector3(blockHit.getHitVec());
				break;
			}
			
			// Compute parameters
			final int energyCost = Commons.clamp(WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ENERGY_MIN, WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ENERGY_MAX,
					Math.round(hardness * WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ENERGY_PER_BLOCK_HARDNESS) );
			final double absorptionChance = Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ABSORPTION_MAX,
					hardness * WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ABSORPTION_PER_BLOCK_HARDNESS );
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info(String.format("Block energy cost is %d with %.1f %% of absorption",
				                                    energyCost, absorptionChance * 100.0D));
			}
			
			// apply environmental absorption
			energy *= getTransmittance(blockHitDistance - distanceTravelled);
			
			do {
				// Consume energy
				energy -= energyCost;
				distanceTravelled = blockHitDistance;
				vHitPoint = new Vector3(blockHit.getHitVec());
				if (energy <= 0) {
					if (WarpDriveConfig.LOGGING_WEAPON) {
						WarpDrive.logger.info("Beam died out of energy");
					}
					break;
				}
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info(String.format("Beam energy down to %d", energy));
				}
				
				// apply chance of absorption
				if (world.rand.nextDouble() > absorptionChance) {
					break;
				}
			} while (true);
			if (energy <= 0) {
				break;
			}
			
			// add 'explode' effect with the beam color
			// world.createExplosion(null, blockHit.blockX, blockHit.blockY, blockHit.blockZ, 4, true, true);
			final Vector3 origin = new Vector3(
				blockHit.getPos().getX() -0.3D * vDirection.x + world.rand.nextFloat() - world.rand.nextFloat(),
				blockHit.getPos().getY() -0.3D * vDirection.y + world.rand.nextFloat() - world.rand.nextFloat(),
				blockHit.getPos().getZ() -0.3D * vDirection.z + world.rand.nextFloat() - world.rand.nextFloat());
			final Vector3 direction = new Vector3(
				-0.2D * vDirection.x + 0.05 * (world.rand.nextFloat() - world.rand.nextFloat()),
				-0.2D * vDirection.y + 0.05 * (world.rand.nextFloat() - world.rand.nextFloat()),
				-0.2D * vDirection.z + 0.05 * (world.rand.nextFloat() - world.rand.nextFloat()));
			PacketHandler.sendSpawnParticlePacket(world, "explosionNormal", (byte) 5, origin, direction, r, g, b, r, g, b, 96);
			
			// apply custom damages
			if (blockState.getBlock() instanceof IDamageReceiver) {
				energy = ((IDamageReceiver) blockState.getBlock()).applyDamage(blockState, world, blockHit.getPos(),
				                                                               WarpDrive.damageLaser, beamFrequency, vDirection, energy);
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info(String.format("IDamageReceiver damage applied, remaining energy is %d", energy));
				}
				if (energy <= 0) {
					break;
				}
			}
			
			if (hardness >= WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_HARDNESS_THRESHOLD) {
				final float strength = (float) Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_MAX_STRENGTH,
						WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_BASE_STRENGTH + energy / (double) WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_ENERGY_PER_STRENGTH);
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info(String.format("Explosion triggered with strength %.1f", strength));
				}
				world.createExplosion(entityExploder, blockHit.getHitVec().x, blockHit.getHitVec().y, blockHit.getHitVec().z,
				                   strength, true, Mode.DESTROY );
				world.setBlockState(blockHit.getPos(), world.rand.nextBoolean() ? Blocks.FIRE.getDefaultState() : Blocks.AIR.getDefaultState());
			} else {
				world.removeBlock(blockHit.getPos(), false);
			}
		}
		
		PacketHandler.sendBeamPacket(world, new Vector3(this).translate(0.5D).translate(vDirection.scale(0.5D)), vHitPoint, r, g, b, 50, energy,
				beamLengthBlocks);
	}
	
	private double getTransmittance(final double distance) {
		if (distance <= 0) {
			return 1.0D;
		}
		final double attenuation;
		if (CelestialObjectManager.hasAtmosphere(world, pos.getX(), pos.getZ())) {
			attenuation = WarpDriveConfig.LASER_CANNON_ENERGY_ATTENUATION_PER_AIR_BLOCK;
		} else {
			attenuation = WarpDriveConfig.LASER_CANNON_ENERGY_ATTENUATION_PER_VOID_BLOCK;
		}
		final double transmittance = Math.exp(- attenuation * distance);
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info(String.format("Transmittance over %.1f blocks is %.3f",
			                                    distance, transmittance));
		}
		return transmittance;
	}
	
	@Nullable
	private TreeMap<Double, EntityRayTraceResult> raytraceEntities(@Nonnull final Vector3 vSource, @Nonnull final Vector3 vDirection, final double reachDistance) {
		assert world != null;
		final double raytraceTolerance = 2.0D;
		
		// Pre-computation
		final Vec3d vec3Source = vSource.toVec3d();
		final Vec3d vec3Target = new Vec3d(
				vec3Source.x + vDirection.x * reachDistance,
				vec3Source.y + vDirection.y * reachDistance,
				vec3Source.z + vDirection.z * reachDistance);
		
		// Get all possible entities
		final AxisAlignedBB boxToScan = new AxisAlignedBB(
				Math.min(pos.getX() - raytraceTolerance, vec3Target.x - raytraceTolerance),
				Math.min(pos.getY() - raytraceTolerance, vec3Target.y - raytraceTolerance),
				Math.min(pos.getZ() - raytraceTolerance, vec3Target.z - raytraceTolerance),
				Math.max(pos.getX() + raytraceTolerance, vec3Target.x + raytraceTolerance),
				Math.max(pos.getY() + raytraceTolerance, vec3Target.y + raytraceTolerance),
				Math.max(pos.getZ() + raytraceTolerance, vec3Target.z + raytraceTolerance));
		final List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, boxToScan);
		
		if (entities.isEmpty()) {
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info("No entity on trajectory (box)");
			}
			return null;
		}
		
		// Pick the closest one on trajectory
		final HashMap<Double, EntityRayTraceResult> entityHits = new HashMap<>(entities.size());
		for (final Entity entity : entities) {
			if ( entity != null
			  && entity.canBeCollidedWith() ) {
				final double border = entity.getCollisionBorderSize();
				final AxisAlignedBB aabbEntity = entity.getBoundingBox().expand(border, border, border);
				final Optional<Vec3d> optionalHitVector = aabbEntity.rayTrace(vec3Source, vec3Target);
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info(String.format("Checking %s boundingBox %s border %s aabbEntity %s hitMOP %s",
					                                    entity, aabbEntity, border, aabbEntity, optionalHitVector));
				}
				if (optionalHitVector.isPresent()) {
					final EntityRayTraceResult mopEntity = new EntityRayTraceResult(entity, optionalHitVector.get());
					double distance = vec3Source.distanceTo(optionalHitVector.get());
					if (entityHits.containsKey(distance)) {
						distance += world.rand.nextDouble() / 10.0D;
					}
					entityHits.put(distance, mopEntity);
				}
			}
		}
		
		if (entityHits.isEmpty()) {
			return null;
		}
		
		return new TreeMap<>(entityHits);
	}
	
	@Override
	public int getBeamFrequency() {
		return beamFrequency;
	}
	
	@Override
	public void setBeamFrequency(final int parBeamFrequency) {
		if ( beamFrequency != parBeamFrequency
		  && IBeamFrequency.isValid(parBeamFrequency) ) {
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(this + " Beam frequency set from " + beamFrequency + " to " + parBeamFrequency);
			}
			beamFrequency = parBeamFrequency;
		}
		final Vector3 vRGB = IBeamFrequency.getBeamColor(beamFrequency);
		r = (float) vRGB.x;
		g = (float) vRGB.y;
		b = (float) vRGB.z;
	}
	
	private void playSoundCorrespondsEnergy(final int energy) {
		assert world != null;
		if (energy <= 500000) {
			world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.HOSTILE, 1.0F, 1.0F);
		} else if (energy <= 1000000) {
			world.playSound(null, pos, SoundEvents.LASER_MEDIUM, SoundCategory.HOSTILE, 1.0F, 1.0F);
		} else {
			world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.HOSTILE, 1.0F, 1.0F);
		}
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		setBeamFrequency(tagCompound.getInt(IBeamFrequency.BEAM_FREQUENCY_TAG));
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		if (IBeamFrequency.isValid(beamFrequency)) {
			tagCompound.putInt(IBeamFrequency.BEAM_FREQUENCY_TAG, beamFrequency);
		}
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		final CompoundNBT tagCompound = super.getUpdateTag();
		
		tagCompound.remove(IBeamFrequency.BEAM_FREQUENCY_TAG);
		
		return tagCompound;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] { true,
		                      EnergyWrapper.convert(WarpDriveConfig.LASER_CANNON_MAX_LASER_ENERGY, units) };
	}
	
	public Object[] beamFrequency(@Nonnull final Object[] arguments) {
		if (arguments.length == 1) {
			setBeamFrequency(Commons.toInt(arguments[0]));
		}
		return new Integer[] { getBeamFrequency() };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	public Object[] emitBeam(final Context context, final Arguments arguments) {
		return emitBeam(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] beamFrequency(final Context context, final Arguments arguments) {
		return beamFrequency(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	public Object[] getScanResult(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getScanResult();
	}
	
	private Object[] emitBeam(@Nonnull final Object[] arguments) {
		try {
			final float newYaw, newPitch;
			if (arguments.length == 2) {
				newYaw = Commons.toFloat(arguments[0]);
				newPitch = Commons.toFloat(arguments[1]);
				initiateBeamEmission(newYaw, newPitch);
			} else if (arguments.length == 3) {
				final float deltaX = -Commons.toFloat(arguments[0]);
				final float deltaY = -Commons.toFloat(arguments[1]);
				final float deltaZ = Commons.toFloat(arguments[2]);
				final double horizontalDistance = MathHelper.sqrt(deltaX * deltaX + deltaZ * deltaZ);
				newYaw = (float) (Math.atan2(deltaX, deltaZ) * 180.0D / Math.PI);
				newPitch = (float) (Math.atan2(deltaY, horizontalDistance) * 180.0D / Math.PI);
				initiateBeamEmission(newYaw, newPitch);
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			return new Object[] { false };
		}
		return new Object[] { true };
	}
	
	private Object[] getScanResult() {
		if (scanResult_type != ScanResultType.IDLE) {
			try {
				final Object[] info = { scanResult_type.name,
						scanResult_position.getX(), scanResult_position.getY(), scanResult_position.getZ(),
						scanResult_blockUnlocalizedName, scanResult_blockProperties, scanResult_blockResistance };
				scanResult_type = ScanResultType.IDLE;
				scanResult_position = null;
				scanResult_blockUnlocalizedName = null;
				scanResult_blockProperties = null;
				scanResult_blockResistance = -2;
				return info;
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				return new Object[] { COMPUTER_ERROR_TAG, 0, 0, 0, null, 0, -3 };
			}
		} else {
			return new Object[] { scanResult_type.name, 0, 0, 0, null, 0, -1 };
		}
	}
	
	// ComputerCraft IDynamicPeripheral methods
	@Override
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "beamFrequency":
			return beamFrequency(arguments);
			
		case "emitBeam":  // emitBeam(yaw, pitch) or emitBeam(deltaX, deltaY, deltaZ)
			return emitBeam(arguments);
			
		case "getScanResult":
			return getScanResult();
			
		default:
			return super.CC_callMethod(methodName, arguments);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s Beam %d %s",
		                     getClass().getSimpleName(),
		                     beamFrequency,
		                     Commons.format(world, pos));
	}
}
package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IDamageReceiver;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.block.weapon.BlockLaserCamera;
import cr0s.warpdrive.block.weapon.TileEntityLaserCamera;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;

import cpw.mods.fml.common.Optional;

public class TileEntityLaser extends TileEntityAbstractLaser implements IBeamFrequency {
	
	private int legacyVideoChannel = -1;
	private boolean legacyCheck = !(this instanceof TileEntityLaserCamera);
	
	private float yaw, pitch; // laser direction
	
	protected int beamFrequency = -1;
	private float r, g, b; // beam color (corresponds to frequency)
	
	private boolean isEmitting = false;
	
	private int delayTicks = 0;
	private int energyFromOtherBeams = 0;
	
	private enum ScanResultType {
		IDLE("IDLE"), BLOCK("BLOCK"), NONE("NONE");
		
		public final String name;
		
		ScanResultType(String name) {
			this.name = name;
		}
	}
	private ScanResultType scanResult_type = ScanResultType.IDLE;
	private VectorI scanResult_position = null;
	private String scanResult_blockUnlocalizedName;
	private int scanResult_blockMetadata = 0;
	private float scanResult_blockResistance = -2;
	
	public TileEntityLaser() {
		super();
		
		peripheralName = "warpdriveLaser";
		addMethods(new String[] {
			"emitBeam",
			"beamFrequency",
			"getScanResult"
		});
		laserMediumMaxCount = WarpDriveConfig.LASER_CANNON_MAX_MEDIUMS_COUNT;
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		// Legacy tile entity
		if (legacyCheck) {
			if (worldObj.getBlock(xCoord, yCoord, zCoord) instanceof BlockLaserCamera) {
				try {
					WarpDrive.logger.info("Self-upgrading legacy tile entity " + this);
					NBTTagCompound nbtOld = new NBTTagCompound();
					writeToNBT(nbtOld);
					TileEntityLaserCamera newTileEntity = new TileEntityLaserCamera(); // id has changed, we can't directly call createAndLoadEntity
					newTileEntity.readFromNBT(nbtOld);
					newTileEntity.setWorldObj(worldObj);
					newTileEntity.validate();
					invalidate();
					worldObj.removeTileEntity(xCoord, yCoord, zCoord);
					worldObj.setTileEntity(xCoord, yCoord, zCoord, newTileEntity);
					newTileEntity.setVideoChannel(legacyVideoChannel);
				} catch (Exception exception) {
					exception.printStackTrace();
				}
			}
			legacyCheck = false;
		}
		
		// Frequency is not set
		if (beamFrequency <= 0 || beamFrequency > IBeamFrequency.BEAM_FREQUENCY_MAX) {
			return;
		}
		
		delayTicks++;
		if ( isEmitting
		  && ( (beamFrequency != BEAM_FREQUENCY_SCANNING && delayTicks > WarpDriveConfig.LASER_CANNON_EMIT_FIRE_DELAY_TICKS)
		    || (beamFrequency == BEAM_FREQUENCY_SCANNING && delayTicks > WarpDriveConfig.LASER_CANNON_EMIT_SCAN_DELAY_TICKS))) {
			delayTicks = 0;
			isEmitting = false;
			int beamEnergy = Math.min(
					consumeCappedEnergyFromLaserMediums(Integer.MAX_VALUE, false) + MathHelper.floor_double(energyFromOtherBeams * WarpDriveConfig.LASER_CANNON_BOOSTER_BEAM_ENERGY_EFFICIENCY),
					WarpDriveConfig.LASER_CANNON_MAX_LASER_ENERGY);
			emitBeam(beamEnergy);
			energyFromOtherBeams = 0;
			sendEvent("laserSend", beamFrequency, beamEnergy);
		}
	}
	
	public void initiateBeamEmission(float parYaw, float parPitch) {
		yaw = parYaw;
		pitch = parPitch;
		delayTicks = 0;
		isEmitting = true;
	}
	
	private void addBeamEnergy(int amount) {
		if (isEmitting) {
			energyFromOtherBeams += amount;
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info(this + " Added energy " + amount);
			}
		} else {
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info(this + " Ignored energy " + amount);
			}
		}
	}
	
	private void emitBeam(int beamEnergy) {
		int energy = beamEnergy;
		
		int beamLengthBlocks = Commons.clamp(0, WarpDriveConfig.LASER_CANNON_RANGE_MAX, energy / 200);
		
		if (energy == 0 || beamFrequency > 65000 || beamFrequency <= 0) {
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info(this + " Beam canceled (energy " + energy + " over " + beamLengthBlocks + " blocks, beamFrequency " + beamFrequency + ")");
			}
			return;
		}
		
		float yawZ = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
		float yawX = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
		float pitchHorizontal = -MathHelper.cos(-pitch * 0.017453292F);
		float pitchVertical = MathHelper.sin(-pitch * 0.017453292F);
		float directionX = yawX * pitchHorizontal;
		float directionZ = yawZ * pitchHorizontal;
		Vector3 vDirection = new Vector3(directionX, pitchVertical, directionZ);
		Vector3 vSource = new Vector3(this).translate(0.5D).translate(vDirection);
		Vector3 vReachPoint = vSource.clone().translateFactor(vDirection, beamLengthBlocks);
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info(this + " Energy " + energy + " over " + beamLengthBlocks + " blocks"
					+ ", Orientation " + yaw + " " + pitch
					+ ", Direction " + vDirection
					+ ", From " + vSource + " to " + vReachPoint);
		}
		
		playSoundCorrespondsEnergy(energy);
		
		// This is a scanning beam, do not deal damage to block nor entity
		if (beamFrequency == BEAM_FREQUENCY_SCANNING) {
			MovingObjectPosition mopResult = worldObj.rayTraceBlocks(vSource.toVec3(), vReachPoint.toVec3());
			
			scanResult_blockUnlocalizedName = null;
			scanResult_blockMetadata = 0;
			scanResult_blockResistance = -2;
			if (mopResult != null) {
				scanResult_type = ScanResultType.BLOCK;
				scanResult_position = new VectorI(mopResult.blockX, mopResult.blockY, mopResult.blockZ);
				Block block = worldObj.getBlock(scanResult_position.x, scanResult_position.y, scanResult_position.z);
				if (block != null) {
					scanResult_blockUnlocalizedName = block.getUnlocalizedName();
					scanResult_blockMetadata = worldObj.getBlockMetadata(scanResult_position.x, scanResult_position.y, scanResult_position.z);
					scanResult_blockResistance = block.getExplosionResistance(null);
				}
				PacketHandler.sendBeamPacket(worldObj, vSource, new Vector3(mopResult.hitVec), r, g, b, 50, energy, 200);
			} else {
				scanResult_type = ScanResultType.NONE;
				scanResult_position = new VectorI(vReachPoint.intX(), vReachPoint.intY(), vReachPoint.intZ());
				PacketHandler.sendBeamPacket(worldObj, vSource, vReachPoint, r, g, b, 50, energy, 200);
			}
			
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info("Scan result type " + scanResult_type.name
					+ " at " + scanResult_position.x + " " + scanResult_position.y + " " + scanResult_position.z
					+ " block " + scanResult_blockUnlocalizedName + " " + scanResult_blockMetadata + " resistance " + scanResult_blockResistance);
			}
			
			sendEvent("laserScanning",
					scanResult_type.name, scanResult_position.x, scanResult_position.y, scanResult_position.z,
					scanResult_blockUnlocalizedName, scanResult_blockMetadata, scanResult_blockResistance);
			return;
		}
		
		// get colliding entities
		TreeMap<Double, MovingObjectPosition> entityHits = raytraceEntities(vSource.clone(), vDirection.clone(), beamLengthBlocks);
		
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info("Entity hits are (" + ((entityHits == null) ? 0 : entityHits.size()) + ") " + entityHits);
		}
		
		Vector3 vHitPoint = vReachPoint.clone();
		double distanceTravelled = 0.0D; // distance traveled from beam sender to previous hit if there were any
		for (int passedBlocks = 0; passedBlocks < beamLengthBlocks; passedBlocks++) {
			// Get next block hit
			MovingObjectPosition blockHit = worldObj.rayTraceBlocks(vSource.toVec3(), vReachPoint.toVec3());
			double blockHitDistance = beamLengthBlocks + 0.1D;
			if (blockHit != null) {
				blockHitDistance = blockHit.hitVec.distanceTo(vSource.toVec3());
			}
			
			// Apply effect to entities
			if (entityHits != null) {
				for (Entry<Double, MovingObjectPosition> entityHitEntry : entityHits.entrySet()) {
					double entityHitDistance = entityHitEntry.getKey();
					// ignore entities behind walls
					if (entityHitDistance >= blockHitDistance) {
						break;
					}
					
					// only hits entities with health or whitelisted
					MovingObjectPosition mopEntity = entityHitEntry.getValue();
					if (mopEntity == null) {
						continue;
					}
					EntityLivingBase entity = null;
					if (mopEntity.entityHit instanceof EntityLivingBase) {
						entity = (EntityLivingBase) mopEntity.entityHit;
						if (WarpDriveConfig.LOGGING_WEAPON) {
							WarpDrive.logger.info("Entity is a valid target (living) " + entity);
						}
					} else {
						String entityId = EntityList.getEntityString(mopEntity.entityHit);
						if (!Dictionary.ENTITIES_NONLIVINGTARGET.contains(entityId)) {
							if (WarpDriveConfig.LOGGING_WEAPON) {
								WarpDrive.logger.info("Entity is an invalid target (non-living " + entityId + ") " + mopEntity.entityHit);
							}
							// remove entity from hit list
							entityHits.put(entityHitDistance, null);
							continue;
						}
						if (WarpDriveConfig.LOGGING_WEAPON) {
							WarpDrive.logger.info("Entity is a valid target (non-living " + entityId + ") " + mopEntity.entityHit);
						}
					}
					
					// Consume energy
					energy *= getTransmittance(entityHitDistance - distanceTravelled);
					energy -= WarpDriveConfig.LASER_CANNON_ENTITY_HIT_ENERGY;
					distanceTravelled = entityHitDistance;
					vHitPoint = new Vector3(mopEntity.hitVec);
					if (energy <= 0) {
						break;
					}
					
					// apply effects
					mopEntity.entityHit.setFire(WarpDriveConfig.LASER_CANNON_ENTITY_HIT_SET_ON_FIRE_SECONDS);
					if (entity != null) {
						float damage = (float) Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_ENTITY_HIT_MAX_DAMAGE,
								WarpDriveConfig.LASER_CANNON_ENTITY_HIT_BASE_DAMAGE + energy / WarpDriveConfig.LASER_CANNON_ENTITY_HIT_ENERGY_PER_DAMAGE);
						entity.attackEntityFrom(DamageSource.inFire, damage);
					} else {
						mopEntity.entityHit.setDead();
					}
					
					if (energy > WarpDriveConfig.LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_THRESHOLD) {
						float strength = (float) Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_ENTITY_HIT_EXPLOSION_MAX_STRENGTH,
							  WarpDriveConfig.LASER_CANNON_ENTITY_HIT_EXPLOSION_BASE_STRENGTH + energy / WarpDriveConfig.LASER_CANNON_ENTITY_HIT_EXPLOSION_ENERGY_PER_STRENGTH); 
						worldObj.newExplosion(null, mopEntity.entityHit.posX, mopEntity.entityHit.posY, mopEntity.entityHit.posZ, strength, true, true);
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
					WarpDrive.logger.info("No more blocks to hit or too far: blockHitDistance is " + blockHitDistance + ", blockHit is " + blockHit);
				}
				vHitPoint = vReachPoint;
				break;
			}
			
			Block block = worldObj.getBlock(blockHit.blockX, blockHit.blockY, blockHit.blockZ);
			// int blockMeta = worldObj.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
			// get hardness and blast resistance
			float hardness = -2.0F;
			if (WarpDrive.fieldBlockHardness != null) {
				// WarpDrive.fieldBlockHardness.setAccessible(true);
				try {
					hardness = (float)WarpDrive.fieldBlockHardness.get(block);
				} catch (IllegalArgumentException | IllegalAccessException exception) {
					exception.printStackTrace();
					WarpDrive.logger.error("Unable to access block hardness value of " + block);
				}
			}
			if (block instanceof IDamageReceiver) {
				hardness = ((IDamageReceiver)block).getBlockHardness(worldObj, blockHit.blockX, blockHit.blockY, blockHit.blockZ,
					WarpDrive.damageLaser, beamFrequency, vDirection, energy);
			}				
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info("Block collision found at " + blockHit.blockX + " " + blockHit.blockY + " " + blockHit.blockZ
						+ " with block " + block + " of hardness " + hardness);
			}
			
			// check area protection
			if (isBlockBreakCanceled(null, worldObj, blockHit.blockX, blockHit.blockY, blockHit.blockZ)) {
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info("Laser weapon cancelled at (" + blockHit.blockX + " " + blockHit.blockY + " " + blockHit.blockZ + ")");
				}
				vHitPoint = new Vector3(blockHit.hitVec);
				break;
			}
			
			// Boost a laser if it uses same beam frequency
			if (block.isAssociatedBlock(WarpDrive.blockLaser) || block.isAssociatedBlock(WarpDrive.blockLaserCamera)) {
				TileEntityLaser tileEntityLaser = (TileEntityLaser) worldObj.getTileEntity(blockHit.blockX, blockHit.blockY, blockHit.blockZ);
				if (tileEntityLaser != null && tileEntityLaser.getBeamFrequency() == beamFrequency) {
					tileEntityLaser.addBeamEnergy(energy);
					vHitPoint = new Vector3(blockHit.hitVec);
					break;
				}
			}
			
			// explode on unbreakable blocks
			if (hardness < 0.0F) {
				float strength = (float) Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_MAX_STRENGTH,
					WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_BASE_STRENGTH + energy / WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_ENERGY_PER_STRENGTH);
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info("Explosion triggered with strength " + strength);
				}
				worldObj.newExplosion(null, blockHit.blockX, blockHit.blockY, blockHit.blockZ, strength, true, true);
				vHitPoint = new Vector3(blockHit.hitVec);
				break;
			}
			
			// Compute parameters
			int energyCost = Commons.clamp(WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ENERGY_MIN, WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ENERGY_MAX,
					Math.round(hardness * WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ENERGY_PER_BLOCK_HARDNESS));
			double absorptionChance = Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ABSORPTION_MAX,
					hardness * WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ABSORPTION_PER_BLOCK_HARDNESS);
			
			do {
				// Consume energy
				energy *= getTransmittance(blockHitDistance - distanceTravelled);
				energy -= energyCost;
				distanceTravelled = blockHitDistance;
				vHitPoint = new Vector3(blockHit.hitVec);
				if (energy <= 0) {
					if (WarpDriveConfig.LOGGING_WEAPON) {
						WarpDrive.logger.info("Beam died out of energy");
					}
					break;
				}
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info("Beam energy down to " + energy);
				}
				
				// apply chance of absorption
				if (worldObj.rand.nextDouble() > absorptionChance) {
					break;
				}
			} while (true);
			if (energy <= 0) {
				break;
			}
			
			// add 'explode' effect with the beam color
			// worldObj.newExplosion(null, blockHit.blockX, blockHit.blockY, blockHit.blockZ, 4, true, true);
			Vector3 origin = new Vector3(
				blockHit.blockX -0.3D * vDirection.x + worldObj.rand.nextFloat() - worldObj.rand.nextFloat(),
				blockHit.blockY -0.3D * vDirection.y + worldObj.rand.nextFloat() - worldObj.rand.nextFloat(),
				blockHit.blockZ -0.3D * vDirection.z + worldObj.rand.nextFloat() - worldObj.rand.nextFloat());
			Vector3 direction = new Vector3(
				-0.2D * vDirection.x + 0.05 * (worldObj.rand.nextFloat() - worldObj.rand.nextFloat()),
				-0.2D * vDirection.y + 0.05 * (worldObj.rand.nextFloat() - worldObj.rand.nextFloat()),
				-0.2D * vDirection.z + 0.05 * (worldObj.rand.nextFloat() - worldObj.rand.nextFloat()));
			PacketHandler.sendSpawnParticlePacket(worldObj, "explode", (byte) 5, origin, direction, r, g, b, r, g, b, 96);
			
			// apply custom damages
			if (block instanceof IDamageReceiver) {
				energy = ((IDamageReceiver)block).applyDamage(worldObj, blockHit.blockX, blockHit.blockY, blockHit.blockZ,
					WarpDrive.damageLaser, beamFrequency, vDirection, energy);
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info("IDamageReceiver damage applied, remaining energy is " + energy);
				}
				if (energy <= 0) {
					break;
				}
			}
			
			if (hardness >= WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_HARDNESS_THRESHOLD) {
				float strength = (float) Commons.clamp(0.0D, WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_MAX_STRENGTH,
						WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_BASE_STRENGTH + energy / WarpDriveConfig.LASER_CANNON_BLOCK_HIT_EXPLOSION_ENERGY_PER_STRENGTH); 
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info("Explosion triggered with strength " + strength);
				}
				worldObj.newExplosion(null, blockHit.blockX, blockHit.blockY, blockHit.blockZ, strength, true, true);
				worldObj.setBlock(blockHit.blockX, blockHit.blockY, blockHit.blockZ, (worldObj.rand.nextBoolean()) ? Blocks.fire : Blocks.air);
			} else {
				worldObj.setBlockToAir(blockHit.blockX, blockHit.blockY, blockHit.blockZ);
			}
		}
		
		PacketHandler.sendBeamPacket(worldObj, new Vector3(this).translate(0.5D).translate(vDirection.scale(0.5D)), vHitPoint, r, g, b, 50, energy,
				beamLengthBlocks);
	}
	
	private double getTransmittance(final double distance) {
		if (distance <= 0) {
			return 1.0D;
		}
		double attenuation;
		if (CelestialObjectManager.hasAtmosphere(worldObj, xCoord, zCoord)) {
			attenuation = WarpDriveConfig.LASER_CANNON_ENERGY_ATTENUATION_PER_AIR_BLOCK;
		} else {
			attenuation = WarpDriveConfig.LASER_CANNON_ENERGY_ATTENUATION_PER_VOID_BLOCK;
		}
		double transmittance = Math.exp(- attenuation * distance);
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info("Transmittance over " + distance + " blocks is " + transmittance);
		}
		return transmittance;
	}
	
	private TreeMap<Double, MovingObjectPosition> raytraceEntities(Vector3 vSource, Vector3 vDirection, double reachDistance) {
		final double raytraceTolerance = 2.0D;
		
		// Pre-computation
		Vec3 vec3Source = vSource.toVec3();
		Vec3 vec3Target = Vec3.createVectorHelper(
				vec3Source.xCoord + vDirection.x * reachDistance,
				vec3Source.yCoord + vDirection.y * reachDistance,
				vec3Source.zCoord + vDirection.z * reachDistance);
		
		// Get all possible entities
		AxisAlignedBB boxToScan = AxisAlignedBB.getBoundingBox(
				Math.min(xCoord - raytraceTolerance, vec3Target.xCoord - raytraceTolerance),
				Math.min(yCoord - raytraceTolerance, vec3Target.yCoord - raytraceTolerance),
				Math.min(zCoord - raytraceTolerance, vec3Target.zCoord - raytraceTolerance),
				Math.max(xCoord + raytraceTolerance, vec3Target.xCoord + raytraceTolerance),
				Math.max(yCoord + raytraceTolerance, vec3Target.yCoord + raytraceTolerance),
				Math.max(zCoord + raytraceTolerance, vec3Target.zCoord + raytraceTolerance));
		@SuppressWarnings("unchecked")
		List<Entity> entities = worldObj.getEntitiesWithinAABBExcludingEntity(null, boxToScan);
		
		if (entities == null || entities.isEmpty()) {
			if (WarpDriveConfig.LOGGING_WEAPON) {
				WarpDrive.logger.info("No entity on trajectory (box)");
			}
			return null;
		}
		
		// Pick the closest one on trajectory
		HashMap<Double, MovingObjectPosition> entityHits = new HashMap<>(entities.size());
		for (Entity entity : entities) {
			if (entity != null && entity.canBeCollidedWith() && entity.boundingBox != null) {
				double border = entity.getCollisionBorderSize();
				AxisAlignedBB aabbEntity = entity.boundingBox.expand(border, border, border);
				MovingObjectPosition hitMOP = aabbEntity.calculateIntercept(vec3Source, vec3Target);
				if (WarpDriveConfig.LOGGING_WEAPON) {
					WarpDrive.logger.info("Checking " + entity + " boundingBox " + entity.boundingBox + " border " + border + " aabbEntity " + aabbEntity + " hitMOP " + hitMOP);
				}
				if (hitMOP != null) {
					MovingObjectPosition mopEntity = new MovingObjectPosition(entity);
					mopEntity.hitVec = hitMOP.hitVec;
					double distance = vec3Source.distanceTo(hitMOP.hitVec);
					if (entityHits.containsKey(distance)) {
						distance += worldObj.rand.nextDouble() / 10.0D;
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
		if (beamFrequency != parBeamFrequency && (parBeamFrequency <= BEAM_FREQUENCY_MAX) && (parBeamFrequency > BEAM_FREQUENCY_MIN)) {
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(this + " Beam frequency set from " + beamFrequency + " to " + parBeamFrequency);
			}
			beamFrequency = parBeamFrequency;
		}
		final Vector3 vRGB = IBeamFrequency.getBeamColor(beamFrequency);
		r = (float)vRGB.x;
		g = (float)vRGB.y;
		b = (float)vRGB.z;
	}
	
	protected String getBeamFrequencyStatus() {
		if (beamFrequency == -1) {
			return StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.statusLine.undefined");
		} else if (beamFrequency < 0) {
			return StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.statusLine.invalid", beamFrequency );
		} else {
			return StatCollector.translateToLocalFormatted("warpdrive.beam_frequency.statusLine.valid", beamFrequency );
		}
	}
	
	@Override
	public String getStatus() {
		if (worldObj == null || !worldObj.isRemote) {
			return super.getStatus()
			       + "\n" + getBeamFrequencyStatus();
		} else {
			return super.getStatus();
		}
	}
	
	private void playSoundCorrespondsEnergy(int energy) {
		if (energy <= 500000) {
			worldObj.playSoundEffect(xCoord + 0.5f, yCoord - 0.5f, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
		} else if (energy > 500000 && energy <= 1000000) {
			worldObj.playSoundEffect(xCoord + 0.5f, yCoord - 0.5f, zCoord + 0.5f, "warpdrive:midlaser", 4F, 1F);
		} else if (energy > 1000000) {
			worldObj.playSoundEffect(xCoord + 0.5f, yCoord - 0.5f, zCoord + 0.5f, "warpdrive:hilaser", 4F, 1F);
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		setBeamFrequency(tag.getInteger(BEAM_FREQUENCY_TAG));
		legacyVideoChannel = tag.getInteger("cameraFrequency") + tag.getInteger(IVideoChannel.VIDEO_CHANNEL_TAG);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger(BEAM_FREQUENCY_TAG, beamFrequency);
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
	}
	
	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] emitBeam(Context context, Arguments arguments) {
		return emitBeam(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] beamFrequency(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setBeamFrequency(arguments.checkInteger(0));
		}
		return new Integer[] { beamFrequency };
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getScanResult(Context context, Arguments arguments) {
		return getScanResult();
	}
	
	private Object[] emitBeam(Object[] arguments) {
		try {
			float newYaw, newPitch;
			if (arguments.length == 2) {
				newYaw = Commons.toFloat(arguments[0]);
				newPitch = Commons.toFloat(arguments[1]);
				initiateBeamEmission(newYaw, newPitch);
			} else if (arguments.length == 3) {
				float deltaX = -Commons.toFloat(arguments[0]);
				float deltaY = -Commons.toFloat(arguments[1]);
				float deltaZ = Commons.toFloat(arguments[2]);
				double horizontalDistance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
				//noinspection SuspiciousNameCombination
				newYaw = (float) (Math.atan2(deltaX, deltaZ) * 180.0D / Math.PI);
				newPitch = (float) (Math.atan2(deltaY, horizontalDistance) * 180.0D / Math.PI);
				initiateBeamEmission(newYaw, newPitch);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			return new Object[] { false };
		}
		return new Object[] { true };
	}
	
	private Object[] getScanResult() {
		if (scanResult_type != ScanResultType.IDLE) {
			try {
				Object[] info = { scanResult_type.name,
						scanResult_position.x, scanResult_position.y, scanResult_position.z,
						scanResult_blockUnlocalizedName, scanResult_blockMetadata, scanResult_blockResistance };
				scanResult_type = ScanResultType.IDLE;
				scanResult_position = null;
				scanResult_blockUnlocalizedName = null;
				scanResult_blockMetadata = 0;
				scanResult_blockResistance = -2;
				return info;
			} catch (Exception exception) {
				exception.printStackTrace();
				return new Object[] { COMPUTER_ERROR_TAG, 0, 0, 0, null, 0, -3 };
			}
		} else {
			return new Object[] { scanResult_type.name, 0, 0, 0, null, 0, -1 };
		}
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		switch (methodName) {
		case "emitBeam":  // emitBeam(yaw, pitch) or emitBeam(deltaX, deltaY, deltaZ)
			return emitBeam(arguments);
			
		case "position":
			return new Integer[]{ xCoord, yCoord, zCoord };
			
		case "beamFrequency":
			if (arguments.length == 1) {
				setBeamFrequency(Commons.toInt(arguments[0]));
			}
			return new Integer[]{ beamFrequency };
			
		case "getScanResult":
			return getScanResult();
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s Beam \'%d\' @ \'%s\' (%d %d %d)", getClass().getSimpleName(),
			beamFrequency, worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(), xCoord, yCoord, zCoord);
	}
}
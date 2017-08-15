package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IForceFieldUpgrade;
import cr0s.warpdrive.api.IForceFieldUpgradeEffector;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;

import java.util.HashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.world.World;

public enum EnumForceFieldUpgrade implements IForceFieldUpgrade, IForceFieldUpgradeEffector {
	//            Upgrade         - Compatibility -  ----- Value -----  -- Scan speed --  -- Place speed --  --------- Energy costs ---------  comment
	//            name            projector   relay    incr.       cap  minimum  maximum   minimum  maximum  startup   scan    place    entity  
	NONE         ("none"         ,        0,      0,    0.0F,     0.0F,  0.000F,  0.000F,   0.000F,  0.000F,    0.0F, 0.000F,  0.000F,    0.0F, "n/a"),
	ATTRACTION   ("attraction"   ,        0,      1,    1.0F,     4.0F,  0.000F,  0.000F,   0.000F,  0.000F,   50.0F, 0.150F,  0.000F,    8.0F, "value is acceleration"),
	BREAKING     ("breaking"     ,        0,      1,    1.0F,    25.0F,  0.400F,  0.500F,   0.020F,  0.150F,  700.0F, 0.080F,  4.000F,    0.0F, "value is hardness level"),
	CAMOUFLAGE   ("camouflage"   ,        0,      1,    1.0F,     3.0F,  0.600F,  0.850F,   0.700F,  0.950F, 1000.0F, 3.000F,  7.000F,    0.0F, "value is boolean"),
	COOLING      ("cooling"      ,        3,      1,   30.0F,   300.0F,  0.000F,  0.000F,   0.900F,  0.900F,  150.0F, 0.060F,  1.500F,   40.0F, "value is heat units"),
	FUSION       ("fusion"       ,        1,      1,    1.0F,     1.0F,  0.000F,  0.000F,   0.000F,  0.000F, 1000.0F, 0.040F,  0.150F,    0.0F, "value is boolean"),
	HEATING      ("heating"      ,        3,      1,  100.0F, 10000.0F,  0.000F,  0.000F,   0.900F,  0.900F,  150.0F, 0.300F,  3.000F,   25.0F, "value is heat units"),
	INVERSION    ("inversion"    ,        1,      0,    1.0F,     1.0F,  1.250F,  1.250F,   0.000F,  0.000F, 1500.0F, 0.150F,  0.150F,   20.0F, "value is boolean"),
	ITEM_PORT    ("item_port"    ,        0,      1,    1.0F,    10.0F,  0.000F,  0.000F,   0.950F,  0.900F,   50.0F, 0.120F,  0.500F,    2.0F, "value is boolean"),
	PUMPING      ("pumping"      ,        0,      1, 1000.0F, 50000.0F,  0.800F,  1.000F,   0.400F,  1.000F,  800.0F, 0.150F,  4.500F,    0.0F, "value is viscosity"),
	RANGE        ("range"        ,        4,      1,    8.0F,   128.0F,  1.150F,  0.450F,   1.150F,  0.450F,   10.0F, 0.300F,  0.750F,   12.0F, "value is bonus blocks"),
	REPULSION    ("repulsion"    ,        0,      1,    1.0F,     4.0F,  0.000F,  0.000F,   0.000F,  0.000F,   50.0F, 0.150F,  0.000F,    5.0F, "value is acceleration"),
	ROTATION     ("rotation"     ,        1,      0,    1.0F,     1.0F,  0.000F,  0.000F,   0.000F,  0.000F,  100.0F, 0.000F,  0.000F,    0.0F, "value is boolean"),
	SHOCK        ("shock"        ,        3,      1,    1.0F,    10.0F,  0.800F,  0.800F,   0.800F,  0.800F,  300.0F, 0.600F,  4.000F,   30.0F, "value is damage points"),
	SILENCER     ("silencer"     ,        1,      0,    1.0F,     1.0F,  0.000F,  0.000F,   0.000F,  0.000F,    0.0F, 0.120F,  0.620F,    0.0F, "value is boolean"),
	SPEED        ("speed"        ,        4,      1,    1.0F,    20.0F,  1.250F,  6.000F,   1.200F,  5.000F,  200.0F, 0.135F,  1.250F,   15.0F, "value is not used (just a counter)"),
	STABILIZATION("stabilization",        0,      1,    1.0F,     9.0F,  0.250F,  0.850F,   0.025F,  0.450F,  400.0F, 0.050F, 73.600F,    0.0F, "value is boolean"),
	THICKNESS    ("thickness"    ,        5,      1,    0.2F,     1.0F,  0.800F,  1.600F,   0.900F,  1.500F,  100.0F, 0.400F,  2.200F,    5.0F, "value is bonus ratio"),
	TRANSLATION  ("translation"  ,        1,      0,    1.0F,     1.0F,  0.000F,  0.000F,   0.000F,  0.000F,  100.0F, 0.000F,  0.000F,    0.0F, "value is boolean"),
	;
	
	public final String unlocalizedName;
	public final int maxCountOnProjector;
	public final int maxCountOnRelay;
	private final float upgradeValue;
	private final float upgradeValueMax;
	private final float scanSpeedOffset;
	private final float scanSpeedSlope;
	private final float placeSpeedOffset;
	private final float placeSpeedSlope;
	private final float startupEnergyCost;
	private final float scanEnergyCost;
	private final float placeEnergyCost;
	private final float entityEffectEnergyCost;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumForceFieldUpgrade> ID_MAP = new HashMap<>();
	
	static {
		length = EnumForceFieldUpgrade.values().length;
		for (EnumForceFieldUpgrade forceFieldShapeType : values()) {
			ID_MAP.put(forceFieldShapeType.ordinal(), forceFieldShapeType);
		}
	}
	
	EnumForceFieldUpgrade(final String unlocalizedName, final int allowOnProjector, final int maxCountOnRelay,
	                      final float upgradeValue, final float upgradeValueMax,
	                      final float scanSpeedMinimum, final float scanSpeedMaximum, final float placeSpeedMinimum, final float placeSpeedMaximum,
	                      final float startupEnergyCost, final float scanEnergyCost, final float placeEnergyCost, final float entityEffectEnergyCost,
	                      final String comment) {
		this.unlocalizedName = unlocalizedName;
		this.maxCountOnProjector = allowOnProjector;
		this.maxCountOnRelay = maxCountOnRelay;
		
		this.upgradeValue = upgradeValue;
		this.upgradeValueMax = upgradeValueMax;
		
		this.scanSpeedSlope = (upgradeValueMax == upgradeValue) ? 0.0F : (scanSpeedMaximum - scanSpeedMinimum) / (upgradeValueMax - upgradeValue);
		this.scanSpeedOffset = scanSpeedMinimum - scanSpeedSlope * upgradeValue;
		this.placeSpeedSlope = (upgradeValueMax == upgradeValue) ? 0.0F : (placeSpeedMaximum - placeSpeedMinimum) / (upgradeValueMax - upgradeValue);
		this.placeSpeedOffset = placeSpeedMinimum - placeSpeedSlope * upgradeValue;
		
		this.startupEnergyCost = startupEnergyCost / (upgradeValue != 0.0F ? upgradeValue : 1.0F);
		this.scanEnergyCost = scanEnergyCost / (upgradeValue != 0.0F ? upgradeValue : 1.0F);
		this.placeEnergyCost = placeEnergyCost / (upgradeValue != 0.0F ? upgradeValue : 1.0F);
		this.entityEffectEnergyCost = entityEffectEnergyCost / (upgradeValue != 0.0F ? upgradeValue : 1.0F);
		assert(!comment.isEmpty());
	}
	
	@Override
	public String toString() {
		return name();
	}
	
	@Nonnull
	public static EnumForceFieldUpgrade get(final int damage) {
		EnumForceFieldUpgrade enumForceFieldUpgrade = ID_MAP.get(damage);
		return enumForceFieldUpgrade == null ? EnumForceFieldUpgrade.NONE : enumForceFieldUpgrade;
	}
	
	@Override
	public IForceFieldUpgradeEffector getUpgradeEffector() {
		return this;
	}
	
	@Override
	public float getUpgradeValue() {
		return upgradeValue;
	}
	
	@Override
	public float getScaledValue(final float ratio, final float upgradeValue) {
		return ratio * Math.min(upgradeValueMax, upgradeValue);
	}
	
	@Override
	public float getScanSpeedFactor(final float scaledValue) {
		return scanSpeedOffset + scanSpeedSlope * scaledValue;
	}
	
	@Override
	public float getPlaceSpeedFactor(final float scaledValue) {
		return placeSpeedOffset + placeSpeedSlope * scaledValue;
	}
	
	@Override
	public float getStartupEnergyCost(final float scaledValue) {
		return startupEnergyCost * scaledValue;
	}
	
	@Override
	public float getScanEnergyCost(final float scaledValue) {
		return scanEnergyCost * scaledValue;
	}
	
	@Override
	public float getPlaceEnergyCost(final float scaledValue) {
		return placeEnergyCost * scaledValue;
	}
	
	@Override
	public float getEntityEffectEnergyCost(final float scaledValue) { return entityEffectEnergyCost * scaledValue; }
	
	
	@Override
	public int onEntityEffect(final float scaledValue, World world, final int projectorX, final int projectorY, final int projectorZ,
	                          final int blockX, final int blockY, final int blockZ, Entity entity) {
		if (scaledValue == 0.0F) {
			return 0;
		}
		
		// common particle effects properties
		Vector3 v3Projector = new Vector3(projectorX + 0.5D, projectorY + 0.5D, projectorZ + 0.5D);
		double distanceCollision = v3Projector.distanceTo_square(new Vector3(blockX + 0.5D, blockY + 0.5D, blockZ + 0.5D));
		double distanceEntity = v3Projector.distanceTo_square(entity);
		Vector3 v3Entity = new Vector3(entity);
		Vector3 v3Direction = new Vector3(entity).subtract(v3Projector).normalize();
		v3Projector.translateFactor(v3Direction, 0.6D);
		v3Entity.translateFactor(v3Direction, -0.6D);
		
		// entity classification
		int entityLevel = 0;
		if (!entity.isDead) {
			if (entity instanceof EntityPlayer) {
				entityLevel = 4;
			} else if ( entity instanceof EntityMob
				     || entity instanceof EntityGolem
				     || entity instanceof EntityFireball 
				     || entity instanceof EntityTNTPrimed
				     || entity instanceof EntityThrowable
				     || entity instanceof EntityMinecart ) {
				entityLevel = 3;
			} else if ( entity instanceof EntityLivingBase
				     || entity instanceof EntityXPOrb
				     || entity instanceof EntityBoat ) {
				entityLevel = 2;
			} else if ( entity instanceof EntityItem
				     || entity instanceof EntityArrow
				     || entity instanceof EntityFallingBlock
				     || entity instanceof EntityFireworkRocket ) {
				entityLevel = 1;
			}
		}
		
		double speed = Math.abs(scaledValue) / (entityLevel == 0 ? 2 : entityLevel) * ForceFieldSetup.FORCEFIELD_ACCELERATION_FACTOR;
		Vector3 v3Motion = v3Direction.clone().scale(speed); // new Vector3(entity.motionX, entity.motionY, entity.motionZ);
		/*
		if (WarpDriveConfig.LOGGING_FORCEFIELD && WarpDrive.isDev) {
			WarpDrive.logger.info(this + " scaledValue " + scaledValue + " entityLevel " + entityLevel + " speed " + speed
				                      + " entity " + v3Entity + " projector " + v3Projector + " direction " + v3Direction + " motion " + v3Motion + " entity " + entity);
		}
		/**/
		
		// apply damages and particle effects
		switch(this) {
		case ATTRACTION:
			if (scaledValue <= 0.1F || entityLevel > scaledValue) {
				return 0;
			}
			v3Motion.invert();
			entity.fallDistance = 0.0F;
			entity.addVelocity(v3Motion.x, v3Motion.y, v3Motion.z);
			
			// pass through forcefield
			if (distanceCollision <= distanceEntity) {
				if (entity instanceof EntityLivingBase) {
					((EntityLivingBase)entity).setPositionAndUpdate(
						entity.posX - v3Direction.x * 2.0D,
						entity.posY - v3Direction.y * 2.0D,
						entity.posZ - v3Direction.z * 2.0D);
				} else {
					entity.setPosition(
						entity.posX - v3Direction.x * 2.0D,
						entity.posY - v3Direction.y * 2.0D,
						entity.posZ - v3Direction.z * 2.0D);
				}
				v3Entity.translateFactor(v3Direction, 2.0D);
			} else if (entity instanceof EntityPlayer) {
				((EntityLivingBase)entity).setPositionAndUpdate(
					entity.posX - v3Direction.x * 0.4D,
					entity.posY - v3Direction.y * 0.4D,
					entity.posZ - v3Direction.z * 0.4D);
			}
			
			// visual effect
			PacketHandler.sendBeamPacket(world, v3Entity, v3Projector,
				0.2F, 0.4F, 0.7F, 10, 0, 50);
			return 10;
		
		case REPULSION:
			if (scaledValue >= -0.1F || entityLevel > Math.abs(scaledValue)) {
				return 0;
			}
			entity.fallDistance = 0.0F;
			entity.addVelocity(v3Motion.x, v3Motion.y, v3Motion.z);
			
			// pass through forcefield
			if (distanceCollision >= distanceEntity) {
				if (entity instanceof EntityLivingBase) {
					((EntityLivingBase)entity).setPositionAndUpdate(
						entity.posX + v3Direction.x * 2.0D,
						entity.posY + v3Direction.y * 2.0D,
						entity.posZ + v3Direction.z * 2.0D);
				} else {
					entity.setPosition(
						entity.posX + v3Direction.x * 2.0D,
						entity.posY + v3Direction.y * 2.0D,
						entity.posZ + v3Direction.z * 2.0D);
				}
				v3Entity.translateFactor(v3Direction, 2.0D);
			} else if (entity instanceof EntityPlayer) {
				((EntityLivingBase)entity).setPositionAndUpdate(
					entity.posX + v3Direction.x * 0.4D,
					entity.posY + v3Direction.y * 0.4D,
					entity.posZ + v3Direction.z * 0.4D);
			}
			
			// visual effect
			PacketHandler.sendBeamPacket(world, v3Projector, v3Entity,
				0.2F, 0.4F, 0.7F, 10, 0, 50);
			return 10;
		
		case COOLING:
			if (scaledValue >= 295.0F || !(entity instanceof EntityLivingBase) || entityLevel <= 0) {
				return 0;
			}
			entity.attackEntityFrom(WarpDrive.damageCold, (300 - scaledValue) / 10);
			
			// visual effect
			v3Direction.scale(0.20D);
			PacketHandler.sendBeamPacket(world, v3Projector, v3Entity,
				0.25F, 0.38F, 0.75F, 10, 0, 50);
			PacketHandler.sendSpawnParticlePacket(world, "snowshovel", (byte) 5, v3Entity, v3Direction,
				0.20F + 0.10F * world.rand.nextFloat(), 0.25F + 0.25F * world.rand.nextFloat(), 0.60F + 0.30F * world.rand.nextFloat(),
				0.0F, 0.0F, 0.0F, 32);
			return 10;
		
		case HEATING:
			if (scaledValue <= 305.0F || !(entity instanceof EntityLivingBase) || entityLevel <= 0) {
				return 0;
			}
			if (!entity.isImmuneToFire()) {
				entity.setFire(1);
			}
			entity.attackEntityFrom(WarpDrive.damageWarm, (scaledValue - 300) / 100);
			
			// visual effect
			v3Direction.scale(0.20D);
			PacketHandler.sendBeamPacket(world, v3Projector, v3Entity,
				0.95F, 0.52F, 0.38F, 10, 0, 50);
			PacketHandler.sendSpawnParticlePacket(world, "snowshovel", (byte) 5, v3Entity, v3Direction,
				0.90F + 0.10F * world.rand.nextFloat(), 0.35F + 0.25F * world.rand.nextFloat(), 0.30F + 0.15F * world.rand.nextFloat(),
				0.0F, 0.0F, 0.0F, 32);
			/*
			v3Direction.scale(0.10D);
			PacketHandler.sendSpawnParticlePacket(world, "flame", v3Projector, v3Direction,
				0.85F, 0.75F, 0.75F,
				0.0F, 0.0F, 0.0F, 32);
			/**/
			return 10;
		
		case SHOCK:
			if (scaledValue <= 0 || !(entity instanceof EntityLivingBase) || entityLevel <= 0) {
				return 0;
			}
			entity.attackEntityFrom(WarpDrive.damageShock, Math.abs(scaledValue));
			
			// visual effect
			v3Direction.scale(0.15D);
			PacketHandler.sendBeamPacket(world, v3Projector, v3Entity,
				0.35F, 0.57F, 0.87F, 10, 0, 50);
			PacketHandler.sendSpawnParticlePacket(world, "fireworksSpark", (byte) 5, v3Entity, v3Direction,
				0.20F + 0.30F * world.rand.nextFloat(), 0.50F + 0.15F * world.rand.nextFloat(), 0.75F + 0.25F * world.rand.nextFloat(),
				0.10F + 0.20F * world.rand.nextFloat(), 0.10F + 0.30F * world.rand.nextFloat(), 0.20F + 0.10F * world.rand.nextFloat(),
				32);
			return 10;
		
		default:
			return 0;
		}
	}
}

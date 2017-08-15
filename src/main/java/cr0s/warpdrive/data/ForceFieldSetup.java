package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IForceFieldShape;
import cr0s.warpdrive.api.IForceFieldUpgrade;
import cr0s.warpdrive.api.IForceFieldUpgradeEffector;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldProjector;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class ForceFieldSetup extends GlobalPosition {
	private static final float FORCEFIELD_BASE_SCAN_SPEED_BLOCKS_PER_SECOND = 100;
	private static final float FORCEFIELD_BASE_PLACE_SPEED_BLOCKS_PER_SECOND = 20;
	private static final float FORCEFIELD_MAX_SCAN_SPEED_BLOCKS_PER_SECOND = 10000;
	private static final float FORCEFIELD_MAX_PLACE_SPEED_BLOCKS_PER_SECOND = 4000;
	private static final float FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_PROJECTOR_TIER = 0.50F;
	public static final float FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_RELAY_TIER = 0.25F;
	public static final double FORCEFIELD_ACCELERATION_FACTOR = 0.16D;
	public static final int FORCEFIELD_RELAY_RANGE = 20;
	private static final int FORCEFIELD_MAX_FACTOR_ENTITY_COST = 5;
	private static final double FORCEFIELD_TAU_FACTOR_ENTITY_COST = - Math.log(ForceFieldSetup.FORCEFIELD_MAX_FACTOR_ENTITY_COST);
	
	public final int beamFrequency;
	public final byte tier;
	public final Set<TileEntityForceFieldProjector> projectors = new HashSet<>();
	private Block blockCamouflage;
	private int metadataCamouflage;
	private int colorMultiplierCamouflage;
	private int lightCamouflage;
	private static final List<Integer> ALLOWED_RENDER_TYPES = Arrays.asList(
		0, 1, 2, 3, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 21, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 41);
	private final HashMap<IForceFieldUpgradeEffector, Float> upgrades = new HashMap<>(EnumForceFieldUpgrade.length);
	public final Collection<IInventory> inventories = new ArrayList<>(12);
	
	public float scanSpeed;
	public float placeSpeed;
	public double startupEnergyCost;
	public double scanEnergyCost;
	public double placeEnergyCost;
	private double entityEnergyCost;
	
	public float breaking_maxHardness;
	public float temperatureLevel;
	public float accelerationLevel;
	public boolean hasCollection;
	public boolean hasFusion;
	public boolean isInverted;
	public boolean hasStabilize;
	public float thickness;
	public float pumping_maxViscosity;
	
	// Projector provided properties
	public float rotationYaw;
	public float rotationPitch;
	public float rotationRoll;
	public VectorI vTranslation = new VectorI(0, 0, 0);
	public final VectorI vMin = new VectorI(-8, -8, -8);
	public final VectorI vMax = new VectorI( 8,  8,  8);
	public IForceFieldShape shapeProvider;
	public boolean isDoubleSided = true;
	
	public ForceFieldSetup(final int dimensionId, final int x, final int y, final int z, final byte tier, final int beamFrequency) {
		super(dimensionId, x, y, z);
		this.tier = tier;
		this.beamFrequency = beamFrequency;
		refresh();
		if (WarpDriveConfig.LOGGING_FORCEFIELD) {
			WarpDrive.logger.info(String.format("Force field projector energy costs: startup %.3f / %d scan %.3f place %.3f entity %.3f"
				                                    + " speeds: scan %.3f place %.3f"
				                                    + " sustain cost: scan %.3f place %.3f",
				startupEnergyCost, Math.round(startupEnergyCost + placeEnergyCost * placeSpeed * TileEntityForceFieldProjector.PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F),
				scanEnergyCost, placeEnergyCost, entityEnergyCost,
				scanSpeed, placeSpeed,
				(scanEnergyCost * scanSpeed), (placeEnergyCost * placeSpeed)));
		}
	}
	
	public boolean isAccessGranted(EntityPlayer entityPlayer, EnumPermissionNode enumPermissionNode) {
		return false; // TODO
	}
	
	private static boolean isValidCamouflage(final Block block) {
		return block != null && block != Blocks.air && ALLOWED_RENDER_TYPES.contains(block.getRenderType()) && !Dictionary.BLOCKS_NOCAMOUFLAGE.contains(block);
	}
	
	public Block getCamouflageBlock() {
		if (isValidCamouflage(blockCamouflage)) {
			return blockCamouflage;
		}
		return null;
	}
	
	public int getCamouflageMetadata() {
		if (isValidCamouflage(blockCamouflage)) {
			return metadataCamouflage;
		}
		return 0;
	}
	
	public int getCamouflageColorMultiplier() {
		if (isValidCamouflage(blockCamouflage)) {
			return colorMultiplierCamouflage;
		}
		return 0;
	}
	
	public int getCamouflageLight() {
		if (isValidCamouflage(blockCamouflage)) {
			return lightCamouflage;
		}
		return 0;
	}
	
	private float getScaledUpgrade(final IForceFieldUpgradeEffector effector) {
		Float scaledValue = upgrades.get(effector);
		return scaledValue == null ? 0.0F : scaledValue;
	}
	
	private void refresh() {
		Set<TileEntity> tileEntities = ForceFieldRegistry.getTileEntities(beamFrequency, getWorldServerIfLoaded(), x, y, z);
		HashMap<IForceFieldUpgradeEffector, Float> upgradeValues = new HashMap<>(EnumForceFieldUpgrade.length);
		Vector3 v3Min = new Vector3(-1.0D, -1.0D, -1.0D);
		Vector3 v3Max = new Vector3( 1.0D,  1.0D,  1.0D);
		Vector3 v3Translation = new Vector3(0.0D, 0.0D, 0.0D);
		
		for (TileEntity tileEntity : tileEntities) {
			// only consider same dimension
			if (tileEntity.getWorldObj() == null || tileEntity.getWorldObj().provider.dimensionId != dimensionId) {
				continue;
			}
			// projectors
			if (tileEntity instanceof TileEntityForceFieldProjector) {
				TileEntityForceFieldProjector projector = (TileEntityForceFieldProjector) tileEntity;
				if (tileEntity.xCoord == x && tileEntity.yCoord == y && tileEntity.zCoord == z) {
					shapeProvider = projector.getShapeProvider();
					isDoubleSided = projector.isDoubleSided;
					vTranslation = new VectorI(projector);
					rotationYaw = projector.getRotationYaw();
					rotationPitch = projector.getRotationPitch();
					rotationRoll = projector.getRotationRoll();
					v3Min = projector.getMin();
					v3Max = projector.getMax();
					v3Translation = projector.getTranslation();
					for (Entry<Object, Integer> entry : projector.getUpgradesOfType(null).entrySet()) {
						if (entry.getKey() instanceof IForceFieldUpgrade) {
							IForceFieldUpgradeEffector upgradeEffector = ((IForceFieldUpgrade)entry.getKey()).getUpgradeEffector();
							if (upgradeEffector != null) {
								Float currentValue = upgradeValues.get(upgradeEffector);
								if (currentValue == null) {
									currentValue = 0.0F;
								}
								float addedValue = ((IForceFieldUpgrade)entry.getKey()).getUpgradeValue() * entry.getValue();
								addedValue *= 1 + (tier - 1) * FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_PROJECTOR_TIER;
								upgradeValues.put(upgradeEffector, currentValue + addedValue);
							}
						}
					}
					
				} else {
					if ((((TileEntityForceFieldProjector) tileEntity).isEnabled)
						&& (((TileEntityForceFieldProjector) tileEntity).isCalculated())
						&& (((TileEntityForceFieldProjector) tileEntity).isValid())) {
						projectors.add((TileEntityForceFieldProjector) tileEntity);
					}
				}
			}
			// upgrade blocks (namely, relays)
			if (tileEntity instanceof IForceFieldUpgrade) {
				IForceFieldUpgradeEffector upgradeEffector = ((IForceFieldUpgrade)tileEntity).getUpgradeEffector();
				if (upgradeEffector != null) {
					Float currentValue = upgradeValues.get(upgradeEffector);
					if (currentValue == null) {
						currentValue = 0.0F;
					}
					upgradeValues.put(upgradeEffector, currentValue + ((IForceFieldUpgrade)tileEntity).getUpgradeValue());
					
					// camouflage identification
					if (upgradeEffector == EnumForceFieldUpgrade.CAMOUFLAGE) {
						Block blockCandidate = tileEntity.getWorldObj().getBlock(tileEntity.xCoord, tileEntity.yCoord + 1, tileEntity.zCoord);
						if (isValidCamouflage(blockCandidate)) {
							blockCamouflage = blockCandidate;
							metadataCamouflage = tileEntity.getWorldObj().getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord + 1, tileEntity.zCoord);
							colorMultiplierCamouflage = 0x808080; // blockCandidate.colorMultiplier(tileEntity.getWorldObj(), tileEntity.xCoord, tileEntity.yCoord + 1, tileEntity.zCoord);
							lightCamouflage = blockCandidate.getLightValue(tileEntity.getWorldObj(), tileEntity.xCoord, tileEntity.yCoord + 1, tileEntity.zCoord);
						}
					}
					
					// container identification
					if (upgradeEffector == EnumForceFieldUpgrade.ITEM_PORT) {
						inventories.addAll(Commons.getConnectedInventories(tileEntity));
					}
				}
			}
		}
		
		// set default coefficients, depending on projector
		scanSpeed = FORCEFIELD_BASE_SCAN_SPEED_BLOCKS_PER_SECOND * (isDoubleSided ? 2.1F : 1.0F);
		placeSpeed = FORCEFIELD_BASE_PLACE_SPEED_BLOCKS_PER_SECOND * (isDoubleSided ? 2.1F : 1.0F);
		startupEnergyCost = 60.0F + 20.0F * tier;
		scanEnergyCost = 0.4F + 0.4F * tier;
		placeEnergyCost = 3.0F + 3.0F * tier;
		entityEnergyCost = 2.0F;
		if (isDoubleSided) {
			scanSpeed *= 2.1F;
			placeSpeed *= 2.1F;
			startupEnergyCost += 20.0F * tier;
			scanEnergyCost *= 0.45F;
			placeEnergyCost *= 0.45F;
			entityEnergyCost *= 0.45F;
		}
		
		// apply scaling
		float speedRatio;
		for (Map.Entry<IForceFieldUpgradeEffector, Float> entry : upgradeValues.entrySet()) {
			float scaledValue = entry.getKey().getScaledValue(1.0F, entry.getValue());
			if (scaledValue != 0.0F) {
				upgrades.put(entry.getKey(), scaledValue);
				
				speedRatio = entry.getKey().getScanSpeedFactor(scaledValue);
				if (speedRatio > 0.0F) {
					scanSpeed *= speedRatio;
				}
				speedRatio = entry.getKey().getPlaceSpeedFactor(scaledValue);
				if (speedRatio > 0.0F) {
					placeSpeed *= speedRatio;
				}
				
				startupEnergyCost += entry.getKey().getStartupEnergyCost(scaledValue);
				scanEnergyCost += entry.getKey().getScanEnergyCost(scaledValue);
				placeEnergyCost += entry.getKey().getPlaceEnergyCost(scaledValue);
				entityEnergyCost += entry.getKey().getEntityEffectEnergyCost(scaledValue);
			}
		}
		
		// finalize coefficients
		scanSpeed = Math.min(FORCEFIELD_MAX_SCAN_SPEED_BLOCKS_PER_SECOND, scanSpeed);
		placeSpeed = Math.min(FORCEFIELD_MAX_PLACE_SPEED_BLOCKS_PER_SECOND, placeSpeed);
		
		// acceleration is a compound of attraction and repulsion
		accelerationLevel = getScaledUpgrade(EnumForceFieldUpgrade.ATTRACTION) - getScaledUpgrade(EnumForceFieldUpgrade.REPULSION);
		
		// collection, fusion, inversion and stabilize just needs to be defined
		hasCollection = getScaledUpgrade(EnumForceFieldUpgrade.ITEM_PORT) > 0.0F && accelerationLevel > 1.0F;
		hasFusion = getScaledUpgrade(EnumForceFieldUpgrade.FUSION) > 0.0F;
		isInverted = getScaledUpgrade(EnumForceFieldUpgrade.INVERSION) > 0.0F;
		hasStabilize = getScaledUpgrade(EnumForceFieldUpgrade.STABILIZATION) > 0.0F && accelerationLevel < 1.0F;
		
		// temperature is a compound of cooling and heating
		temperatureLevel = Math.max(0.1F, 300.0F + getScaledUpgrade(EnumForceFieldUpgrade.HEATING) - getScaledUpgrade(EnumForceFieldUpgrade.COOLING));
		
		// disintegration, pump and thickness is the actual value
		breaking_maxHardness = getScaledUpgrade(EnumForceFieldUpgrade.BREAKING);
		pumping_maxViscosity = getScaledUpgrade(EnumForceFieldUpgrade.PUMPING);
		thickness = 1.0F + getScaledUpgrade(EnumForceFieldUpgrade.THICKNESS);
		
		// range is maximum distance
		double range = getScaledUpgrade(EnumForceFieldUpgrade.RANGE);
		if (range == 0.0D) {
			range = 8.0D;
			v3Min = new Vector3(-1.0D, -1.0D, -1.0D);
			v3Max = new Vector3( 1.0D,  1.0D,  1.0D);
		}
		if (hasFusion || isInverted) {
			range = Math.min(64.0D, range);
		}
		vMin.x = (int) Math.round(Math.min(0.0D, Math.max(-1.0D, v3Min.x)) * range);
		vMin.y = (int) Math.round(Math.min(0.0D, Math.max(-1.0D, v3Min.y)) * range);
		vMin.z = (int) Math.round(Math.min(0.0D, Math.max(-1.0D, v3Min.z)) * range);
		vMax.x = (int) Math.round(Math.min(1.0D, Math.max( 0.0D, v3Max.x)) * range);
		vMax.y = (int) Math.round(Math.min(1.0D, Math.max( 0.0D, v3Max.y)) * range);
		vMax.z = (int) Math.round(Math.min(1.0D, Math.max( 0.0D, v3Max.z)) * range);
		vTranslation.x += (int) Math.round(Math.min(1.0D, Math.max(-1.0D, v3Translation.x)) * range);
		vTranslation.y += (int) Math.round(Math.min(1.0D, Math.max(-1.0D, v3Translation.y)) * range);
		vTranslation.z += (int) Math.round(Math.min(1.0D, Math.max(-1.0D, v3Translation.z)) * range);
	}
	
	public double getEntityEnergyCost(final int countEntityInteractions) {
		return entityEnergyCost * FORCEFIELD_MAX_FACTOR_ENTITY_COST * Math.exp(FORCEFIELD_TAU_FACTOR_ENTITY_COST / countEntityInteractions);
	}
	
	public int onEntityEffect(World world, final int blockX, final int blockY, final int blockZ, Entity entity) {
		int countdown = 0;
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityForceFieldProjector) {
			if (((TileEntityForceFieldProjector)tileEntity).onEntityInteracted(entity.getUniqueID())) {
				for (Map.Entry<IForceFieldUpgradeEffector, Float> entry : upgrades.entrySet()) {
					Float value = entry.getValue();
					if (entry.getKey() == EnumForceFieldUpgrade.COOLING || entry.getKey() == EnumForceFieldUpgrade.HEATING) {
						value = temperatureLevel;
					} else if (entry.getKey() == EnumForceFieldUpgrade.ATTRACTION || entry.getKey() == EnumForceFieldUpgrade.REPULSION) {
						value = accelerationLevel;
					}
					countdown += entry.getKey().onEntityEffect(value, world, x, y, z, blockX, blockY, blockZ, entity);
				}
			}
		}
		return countdown;
	}
	
	public double applyDamage(World world, final DamageSource damageSource, final double damageLevel) {
		assert(damageSource != null);
		TileEntity tileEntity = world.getTileEntity(this.x, this.y, this.z);
		if (tileEntity instanceof TileEntityForceFieldProjector) {
			double scaledDamage = damageLevel * entityEnergyCost / 2000.0D;
			((TileEntityForceFieldProjector)tileEntity).onEnergyDamage(scaledDamage);
			return 0.0D;
		}
		return damageLevel;
	}
	
	@Override
	public String toString() {
		return String.format("%s @ DIM%d (%d %d %d) (%d %d %d) -> (%d %d %d)",
			getClass().getSimpleName(), dimensionId,
			x, y, z,
			vMin.x, vMin.y, vMin.z,
			vMax.x, vMax.y, vMax.z);
	}
}

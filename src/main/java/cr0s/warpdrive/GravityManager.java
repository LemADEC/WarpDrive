package cr0s.warpdrive;

import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.StarMapRegistry;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

public class GravityManager {
	
	private static final double OVERWORLD_ENTITY_GRAVITY = 0.080000000000000002D;	// Default value from Vanilla
	private static final double OVERWORLD_ITEM_GRAVITY = 0.039999999105930328D;	// Default value from Vanilla
	private static final double OVERWORLD_ITEM_GRAVITY2 = 0.9800000190734863D;	// Default value from Vanilla
	private static final double HYPERSPACE_FIELD_ENTITY_GRAVITY = 0.035D;
	private static final double HYPERSPACE_VOID_ENTITY_JITTER = 0.005D;
	private static final double SPACE_FIELD_ENTITY_GRAVITY = 0.025D;
	private static final double SPACE_FIELD_ITEM_GRAVITY = 0.02D;
	private static final double SPACE_FIELD_ITEM_GRAVITY2 = 0.60D;
	private static final double SPACE_VOID_GRAVITY = 0.001D;
	private static final double SPACE_VOID_GRAVITY_JETPACK_SNEAK = 0.02D;
	private static final double SPACE_VOID_GRAVITY_RAW_SNEAK = 0.005D; // 0.001 = no mvt
	
	@SuppressWarnings("unused") // Core mod
	public static double getGravityForEntity(Entity entity) {
		
		final double gravity = StarMapRegistry.getGravity(entity);
		if (gravity == CelestialObject.GRAVITY_NONE) {
			return SPACE_VOID_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_NORMAL) {
			return OVERWORLD_ENTITY_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_LEGACY_SPACE || gravity == CelestialObject.GRAVITY_LEGACY_HYPERSPACE) {
			// Is entity in hyper-space?
			final boolean inHyperspace = gravity == CelestialObject.GRAVITY_LEGACY_HYPERSPACE;
			
			if (isEntityInGraviField(entity)) {
				if (inHyperspace) {
					return HYPERSPACE_FIELD_ENTITY_GRAVITY;
				} else {
					return SPACE_FIELD_ENTITY_GRAVITY;
				}
			} else {
				final double jitter = inHyperspace ? (entity.worldObj.rand.nextDouble() - 0.5D) * 2.0D * HYPERSPACE_VOID_ENTITY_JITTER : 0.0D;
				if (entity instanceof EntityPlayer) {
					EntityPlayer player = (EntityPlayer) entity;
					
					if (player.isSneaking()) {
						for (int i = 0; i < 4; i++) {
							ItemStack armor = player.getCurrentArmor(i);
							if (armor != null) {
								if (Dictionary.ITEMS_FLYINSPACE.contains(armor.getItem())) {
									return SPACE_VOID_GRAVITY_JETPACK_SNEAK;
								}
							}
						}
						return SPACE_VOID_GRAVITY_RAW_SNEAK;
					} else {
						// FIXME: compensate jetpack
					}
				}
				
				return SPACE_VOID_GRAVITY + jitter;
			}
		}
		
		return gravity * OVERWORLD_ENTITY_GRAVITY;
	}
	
	@SuppressWarnings("unused") // Core mod
	public static double getItemGravity(EntityItem entity) {
		final double gravity = StarMapRegistry.getGravity(entity);
		if (gravity == CelestialObject.GRAVITY_NONE) {
			return SPACE_VOID_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_NORMAL) {
			return OVERWORLD_ITEM_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_LEGACY_SPACE || gravity == CelestialObject.GRAVITY_LEGACY_HYPERSPACE) {
			if (isEntityInGraviField(entity)) {
				return SPACE_FIELD_ITEM_GRAVITY;
			} else {
				return SPACE_VOID_GRAVITY;
			}
		} 
		
		return gravity * OVERWORLD_ITEM_GRAVITY;
	}
	
	@SuppressWarnings("unused") // Core mod
	public static double getItemGravity2(EntityItem entity) {
		final double gravity = StarMapRegistry.getGravity(entity);
		if (gravity == CelestialObject.GRAVITY_NONE) {
			return SPACE_VOID_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_NORMAL) {
			return OVERWORLD_ITEM_GRAVITY2;
		}
		
		if (gravity == CelestialObject.GRAVITY_LEGACY_SPACE || gravity == CelestialObject.GRAVITY_LEGACY_HYPERSPACE) {
			if (isEntityInGraviField(entity)) {
				return SPACE_FIELD_ITEM_GRAVITY2;
			} else {
				return SPACE_VOID_GRAVITY;
			}
		}
		
		return gravity * OVERWORLD_ITEM_GRAVITY2;
	}
	
	public static boolean isEntityInGraviField(Entity entity) {
		final int y = MathHelper.floor_double(entity.posY);
		final int x = MathHelper.floor_double(entity.posX);
		final int z = MathHelper.floor_double(entity.posZ);
		final int CHECK_DISTANCE = 20;
		
		// Search non-air blocks under player
		for (int ny = y; ny > (y - CHECK_DISTANCE); ny--) {
			if (!entity.worldObj.isAirBlock(x, ny, z)) {
				final Block block = entity.worldObj.getBlock(x, ny, z);
				final AxisAlignedBB axisAlignedBB = block.getCollisionBoundingBoxFromPool(entity.worldObj, x, ny, z);
				if (axisAlignedBB != null && axisAlignedBB.getAverageEdgeLength() > 0.90D) {
					return true;
				}
			}
		}
		
		return false;
	}
}

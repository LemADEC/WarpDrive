package cr0s.warpdrive;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

public class GravityManager
{
    private static double OVERWORLD_ENTITY_GRAVITY = 0.080000000000000002D;	// Default value from Vanilla
    private static double OVERWORLD_ITEM_GRAVITY = 0.039999999105930328D;	// Default value from Vanilla
    private static double OVERWORLD_ITEM_GRAVITY2 = 0.9800000190734863D;	// Default value from Vanilla
    private static double HYPERSPACE_FIELD_ENTITY_GRAVITY = 0.035D;
    private static double HYPERSPACE_VOID_ENTITY_JITTER = 0.005D;
    private static double SPACE_FIELD_ENTITY_GRAVITY = 0.025D; // Lem 0.08D
    private static double SPACE_FIELD_ITEM_GRAVITY = 0.02D;	// Lem 0.04D
    private static double SPACE_FIELD_ITEM_GRAVITY2 = 0.60D; // Lem 0.9800000190734863D
    private static double SPACE_VOID_GRAVITY = 0.001D; // Lem 0.0001D
    private static double SPACE_VOID_GRAVITY_JETPACKSNEAK = 0.02D; // Lem 0.01D
    private static double SPACE_VOID_GRAVITY_RAWSNEAK = 0.005D; // Lem 0.01D		0.001 = no mvt

    public static double getGravityForEntity(EntityLivingBase entity)
    {
        // Is entity in space or hyper-space?
        boolean inSpace = entity.worldObj.provider.dimensionId == WarpDriveConfig.G_SPACE_DIMENSION_ID;
        boolean inHyperspace = entity.worldObj.provider.dimensionId == WarpDriveConfig.G_HYPERSPACE_DIMENSION_ID;
        // entity.ticksExisted

        if (inSpace || inHyperspace)
        {
            boolean insideGravField = isEntityInGraviField(entity);
            	
            if (insideGravField)
            {
            	if (inSpace)
            		return SPACE_FIELD_ENTITY_GRAVITY;
            	else
            		return HYPERSPACE_FIELD_ENTITY_GRAVITY;
            }
            else
            {
            	double jitter = (entity.worldObj.rand.nextDouble() - 0.5D) * 2.0D * HYPERSPACE_VOID_ENTITY_JITTER;
            	if (inSpace)
            		jitter = 0.0D;
                if (entity instanceof EntityPlayer)
                {
                    EntityPlayer player = (EntityPlayer)entity;

                    if (player.isSneaking())
                    {
                        if (player.getCurrentArmor(2) != null && WarpDriveConfig.jetpacks.contains(player.getCurrentArmor(2)))
                        {
                            return SPACE_VOID_GRAVITY_JETPACKSNEAK;
                        }
                        else
                        {
                        	return SPACE_VOID_GRAVITY_RAWSNEAK;
                        }
                    }
                    else
                    {
                    	// FIXME: compensate jetpack
                    }
                }

                return SPACE_VOID_GRAVITY + jitter;
            }
        }

        return OVERWORLD_ENTITY_GRAVITY;
    }

    public static double getItemGravity(EntityItem entity)
    {
        if (entity.worldObj.provider.dimensionId == WarpDriveConfig.G_SPACE_DIMENSION_ID || entity.worldObj.provider.dimensionId == WarpDriveConfig.G_HYPERSPACE_DIMENSION_ID)
        {
            if (isEntityInGraviField(entity))
            {
                return SPACE_FIELD_ITEM_GRAVITY;
            }
            else
            {
                return SPACE_VOID_GRAVITY;
            }
        }
        else
        {
            return OVERWORLD_ITEM_GRAVITY; // On Earth
        }
    }

    public static double getItemGravity2(EntityItem entity)
    {
        if (entity.worldObj.provider.dimensionId == WarpDriveConfig.G_SPACE_DIMENSION_ID || entity.worldObj.provider.dimensionId == WarpDriveConfig.G_HYPERSPACE_DIMENSION_ID)
        {
            if (isEntityInGraviField(entity))
            {
                return SPACE_FIELD_ITEM_GRAVITY2;
            }
            else
            {
                return SPACE_VOID_GRAVITY;
            }
        }
        else
        {
            return OVERWORLD_ITEM_GRAVITY2;
        }
    }

    public static boolean isEntityInGraviField(Entity e)
    {
        int y = MathHelper.floor_double(e.posY);
        int x = MathHelper.floor_double(e.posX);
        int z = MathHelper.floor_double(e.posZ);
        final int CHECK_DISTANCE = 20;

        // Search non-air blocks under player
        for (int ny = y; ny > (y - CHECK_DISTANCE); ny--)
        {
            if (!e.worldObj.isAirBlock(x, ny, z))
            {
                return true;
            }
        }

        return false;
    }
}

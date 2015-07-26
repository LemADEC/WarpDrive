package cr0s.warpdrive.world;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cr0s.warpdrive.WarpDrive;

public final class EntityStarCore extends Entity
{
    public int xCoord;
    public int yCoord;
    public int zCoord;

    private int radius;

    private final int KILL_RADIUS = 60;
    private final int BURN_RADIUS = 200;
    //private final int ROCKET_INTERCEPT_RADIUS = 100; //disabled
    private boolean isLogged = false;

    private final int ENTITY_ACTION_INTERVAL = 10; // ticks

    private int ticks = 0;

    public EntityStarCore(World world)
    {
        super(world);
    }

    public EntityStarCore(World world, int x, int y, int z, int radius)
    {
        super(world);
        this.xCoord = x;
        this.posX = x;
        this.yCoord = y;
        this.posY = y;
        this.zCoord = z;
        this.posZ = z;
        this.radius = radius;
    }

    private void actionToEntitiesNearStar()
    {
        int xmax, ymax, zmax;
        int xmin, ymin, zmin;
        final int MAX_RANGE = this.radius + KILL_RADIUS + BURN_RADIUS;// + ROCKET_INTERCEPT_RADIUS;
        final int KILL_RANGESQ = (this.radius + KILL_RADIUS) * (this.radius + KILL_RADIUS);
        final int BURN_RANGESQ = (this.radius + KILL_RADIUS + BURN_RADIUS) * (this.radius + KILL_RADIUS + BURN_RADIUS);
        xmin = xCoord - MAX_RANGE;
        xmax = xCoord + MAX_RANGE;

        zmin = zCoord - MAX_RANGE;
        zmax = zCoord + MAX_RANGE;

        ymin = yCoord - MAX_RANGE;
        ymax = yCoord + MAX_RANGE;
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(xmin, ymin, zmin, xmax, ymax, zmax);
        List list = worldObj.getEntitiesWithinAABBExcludingEntity(this, aabb);

        if (!isLogged)
        {
        	isLogged = true;
        	WarpDrive.debugPrint(this + ": Capture range: " + MAX_RANGE
	        		+ " X: " + xmin + " to " + xmax + " Y: " + ymin + " to " + ymax + " Z: " + zmin + " to " + zmax);
        }
        for (Object o : list)
        {
            if (o == null || !(o instanceof Entity))
            {
                continue;
            }

            if (o instanceof EntityLivingBase)
            {
                EntityLivingBase entity = (EntityLivingBase)o;

                //System.out.println("Found: " + entity.getEntityName() + " distance: " + entity.getDistanceToEntity(this));
                if (entity.getDistanceSqToEntity(this) <= KILL_RANGESQ)
                {
                    // 100% kill, ignores any protection
                    entity.attackEntityFrom(DamageSource.onFire, 9000);
                }
                else if (entity.getDistanceSqToEntity(this) <= BURN_RANGESQ)
                {
                	if (entity instanceof EntityPlayer)
                    {
                        EntityPlayer player = (EntityPlayer)entity;
                        if (player.capabilities.isCreativeMode)
                        	continue;
                    }
                    // burn entity to 100 seconds
                   	if (!entity.isImmuneToFire())
                   		entity.setFire(100);
                    entity.attackEntityFrom(DamageSource.onFire, 1);
                }
            }/* else { // Intercept ICBM rocket and kill

                Entity entity = (Entity) o;
                if (entity.getDistanceToEntity(this) <= (this.radius + ROCKET_INTERCEPT_RADIUS)) {
                    System.out.println("[SC] Intercepted entity: " + entity.getEntityName());
                    worldObj.removeEntity(entity);
                }
            }*/
        }
    }

    public void killEntity()
    {
        worldObj.removeEntity(this);
    }

    @Override
    public void onUpdate()
    {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient())
        {
            return;
        }

        if (++ticks > ENTITY_ACTION_INTERVAL)
        {
            ticks = 0;
            actionToEntitiesNearStar();
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        this.xCoord = nbttagcompound.getInteger("x");
        this.yCoord = nbttagcompound.getInteger("y");
        this.zCoord = nbttagcompound.getInteger("z");
        this.radius = nbttagcompound.getInteger("radius");
    }

    @Override
    protected void entityInit()
    {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbttagcompound)
    {
        nbttagcompound.setInteger("x", this.xCoord);
        nbttagcompound.setInteger("y", this.yCoord);
        nbttagcompound.setInteger("z", this.zCoord);
        nbttagcompound.setInteger("radius", this.radius);
    }
    
    @Override
    public boolean shouldRenderInPass(int pass)
    {
        return false;
    }    
}
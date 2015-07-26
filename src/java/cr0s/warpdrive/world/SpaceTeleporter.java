package cr0s.warpdrive.world;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class SpaceTeleporter extends Teleporter
{
    int x, y, z;
    int orientation;
    World world;

    public SpaceTeleporter(WorldServer par1WorldServer, int orientation, int x, int y, int z)
    {
        super(par1WorldServer);
        this.orientation = orientation;
        this.x = x;
        this.y = y;
        this.z = z;
        world = par1WorldServer;
    }

    /**
     * Create a new portal near an entity.
     */
    @Override
    public void placeInPortal(Entity par1Entity, double par2, double par4, double par6, float par8)
    {
        //EntityPlayer player = (EntityPlayer) par1Entity;
        //player.setWorld(world);
        //player.setPositionAndUpdate(x, y, z);
    }

    @Override
    public boolean makePortal(Entity par1Entity)
    {
        return false;
    }

    @Override
    public boolean placeInExistingPortal(Entity par1Entity, double par2, double par4, double par6, float par8)
    {
        return false;
    }
}

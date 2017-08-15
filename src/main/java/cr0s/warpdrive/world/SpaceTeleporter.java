package cr0s.warpdrive.world;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class SpaceTeleporter extends Teleporter {
	
	final int x;
	final int y;
	final int z;
	final int orientation;
	final World world;
	
	public SpaceTeleporter(WorldServer worldServer, int orientation, int x, int y, int z) {
		super(worldServer);
		this.orientation = orientation;
		this.x = x;
		this.y = y;
		this.z = z;
		world = worldServer;
	}
	
	@Override
	public void placeInPortal(Entity entity, double x, double y, double z, float rotationYaw) {
		//EntityPlayer player = (EntityPlayer) entity;
		//player.setWorld(world);
		//player.setPositionAndUpdate(x, y, z);
	}
	
	@Override
	public boolean placeInExistingPortal(Entity entity, double x, double y, double z, float rotationYaw) {
		return true;
	}
	
	@Override
	public boolean makePortal(Entity entity) {
		return true;
	}
	
	@Override
	public void removeStalePortalLocations(long time) {
		// do nothing
	}
}

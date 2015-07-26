package cr0s.warpdrive.data;

import java.util.LinkedList;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.sun.media.jfxmedia.logging.Logger;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cr0s.warpdrive.PacketHandler;

/**
 * Cloak manager stores cloaking devices covered areas
 *
 * @author Cr0s
 *
 */

public class CloakManager {

	private LinkedList<CloakedArea> cloaks;

	public CloakManager() {
		this.cloaks = new LinkedList<CloakedArea>();
	}

	public boolean isCloaked(int dimensionID, int x, int y, int z) {
		for (CloakedArea area : this.cloaks) {
			if (area.dimensionId != dimensionID) {
				continue;
			}

			if (area.aabb.minX <= x && area.aabb.maxX >= x && area.aabb.minY <= y && area.aabb.maxY >= y && area.aabb.minZ <= z && area.aabb.maxZ >= z) {
				return true;
			}
		}

		return false;
	}

	public boolean checkChunkLoaded(EntityPlayerMP player, int chunkPosX, int chunkPosZ) {
		for (CloakedArea area : this.cloaks) {
			if (area.dimensionId != player.worldObj.provider.dimensionId) {
				continue;
			}

			if (area.aabb.minX <= (chunkPosX << 4 + 15) && area.aabb.maxX >= (chunkPosX << 4) && area.aabb.minZ <= (chunkPosZ << 4 + 15)
					&& area.aabb.maxZ >= (chunkPosZ << 4)) {
				PacketHandler.sendCloakPacket(player, area.aabb, area.tier, false);
			}
		}

		return false;
	}

	public boolean isAreaExists(World worldObj, int x, int y, int z) {
		return (getCloakedArea(worldObj, x, y, z) != null);
	}

	public void addCloakedAreaWorld(World worldObj, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int x, int y, int z, byte tier) {
		cloaks.add(new CloakedArea(worldObj, x, y, z, AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ), tier));
	}

	public void removeCloakedArea(World worldObj, int x, int y, int z) {
		int index = 0;
		for (int i = 0; i < this.cloaks.size(); i++) {
			if (this.cloaks.get(i).coreX == x && this.cloaks.get(i).coreY == y && this.cloaks.get(i).coreZ == z
					&& this.cloaks.get(i).dimensionId == worldObj.provider.dimensionId) {
				this.cloaks.get(i).sendCloakPacketToPlayersEx(true); // send
				// info
				// about
				// collapsing
				// cloaking
				// field
				index = i;
				break;
			}
		}

		cloaks.remove(index);
	}

	public CloakedArea getCloakedArea(World worldObj, int x, int y, int z) {
		for (CloakedArea area : this.cloaks) {
			if (area.coreX == x && area.coreY == y && area.coreZ == z && area.dimensionId == worldObj.provider.dimensionId)
				return area;
		}

		return null;
	}

	public void updatePlayer(EntityPlayer player) {
		for (CloakedArea area : this.cloaks) {
			area.updatePlayer(player);
		}
	}

	public static Packet getPacketForThisEntity(Entity e) {
		if (e.isDead) {
			Logger.logMsg(Logger.WARNING, "Fetching addPacket for removed entity");
		}

		Packet pkt = FMLNetworkHandler.getEntitySpawningPacket(e);
		if (pkt != null) {
			return pkt;
		}

		return null;

		// TODO: Major, redo networking
		/*
		 * if (e instanceof EntityItem) { return new Packet23VehicleSpawn(e, 2,
		 * 1); } else if (e instanceof EntityPlayerMP) { return new
		 * Packet20NamedEntitySpawn((EntityPlayer) e); } else if (e instanceof
		 * EntityMinecart) { EntityMinecart entityminecart = (EntityMinecart) e;
		 * return new Packet23VehicleSpawn(e, 10,
		 * entityminecart.getMinecartType()); } else if (e instanceof
		 * EntityBoat) { return new Packet23VehicleSpawn(e, 1); } else if (!(e
		 * instanceof IAnimals) && !(e instanceof EntityDragon)) { if (e
		 * instanceof EntityFishHook) { EntityPlayer entityplayer =
		 * ((EntityFishHook) e).angler; return new Packet23VehicleSpawn(e, 90,
		 * entityplayer != null ? entityplayer.entityId : e.entityId); } else if
		 * (e instanceof EntityArrow) { Entity entity = ((EntityArrow)
		 * e).shootingEntity; return new Packet23VehicleSpawn(e, 60, entity !=
		 * null ? entity.entityId : e.entityId); } else if (e instanceof
		 * EntitySnowball) { return new Packet23VehicleSpawn(e, 61); } else if
		 * (e instanceof EntityPotion) { return new Packet23VehicleSpawn(e, 73,
		 * ((EntityPotion) e).getPotionDamage()); } else if (e instanceof
		 * EntityExpBottle) { return new Packet23VehicleSpawn(e, 75); } else if
		 * (e instanceof EntityEnderPearl) { return new Packet23VehicleSpawn(e,
		 * 65); } else if (e instanceof EntityEnderEye) { return new
		 * Packet23VehicleSpawn(e, 72); } else if (e instanceof
		 * EntityFireworkRocket) { return new Packet23VehicleSpawn(e, 76); }
		 * else { Packet23VehicleSpawn packet23vehiclespawn;
		 * 
		 * if (e instanceof EntityFireball) { EntityFireball entityfireball =
		 * (EntityFireball) e; packet23vehiclespawn = null; byte b0 = 63;
		 * 
		 * if (e instanceof EntitySmallFireball) { b0 = 64; } else if (e
		 * instanceof EntityWitherSkull) { b0 = 66; }
		 * 
		 * if (entityfireball.shootingEntity != null) { packet23vehiclespawn =
		 * new Packet23VehicleSpawn(e, b0, ((EntityFireball)
		 * e).shootingEntity.entityId); } else { packet23vehiclespawn = new
		 * Packet23VehicleSpawn(e, b0, 0); }
		 * 
		 * packet23vehiclespawn.speedX = (int) (entityfireball.accelerationX *
		 * 8000.0D); packet23vehiclespawn.speedY = (int)
		 * (entityfireball.accelerationY * 8000.0D); packet23vehiclespawn.speedZ
		 * = (int) (entityfireball.accelerationZ * 8000.0D); return
		 * packet23vehiclespawn; } else if (e instanceof EntityEgg) { return new
		 * Packet23VehicleSpawn(e, 62); } else if (e instanceof EntityTNTPrimed)
		 * { return new Packet23VehicleSpawn(e, 50); } else if (e instanceof
		 * EntityEnderCrystal) { return new Packet23VehicleSpawn(e, 51); } else
		 * if (e instanceof EntityFallingSand) { EntityFallingSand
		 * entityfallingsand = (EntityFallingSand) e; return new
		 * Packet23VehicleSpawn(e, 70, entityfallingsand |
		 * entityfallingsand.metadata << 16); } else if (e instanceof
		 * EntityPainting) { return new Packet25EntityPainting((EntityPainting)
		 * e); } else if (e instanceof EntityItemFrame) { EntityItemFrame
		 * entityitemframe = (EntityItemFrame) e; packet23vehiclespawn = new
		 * Packet23VehicleSpawn(e, 71, entityitemframe.hangingDirection);
		 * packet23vehiclespawn.xPosition =
		 * MathHelper.floor_float(entityitemframe.xPosition * 32);
		 * packet23vehiclespawn.yPosition =
		 * MathHelper.floor_float(entityitemframe.yPosition * 32);
		 * packet23vehiclespawn.zPosition =
		 * MathHelper.floor_float(entityitemframe.zPosition * 32); return
		 * packet23vehiclespawn; } else if (e instanceof EntityLeashKnot) {
		 * EntityLeashKnot entityleashknot = (EntityLeashKnot) e;
		 * packet23vehiclespawn = new Packet23VehicleSpawn(e, 77);
		 * packet23vehiclespawn.xPosition =
		 * MathHelper.floor_float(entityleashknot.xPosition * 32);
		 * packet23vehiclespawn.yPosition =
		 * MathHelper.floor_float(entityleashknot.yPosition * 32);
		 * packet23vehiclespawn.zPosition =
		 * MathHelper.floor_float(entityleashknot.zPosition * 32); return
		 * packet23vehiclespawn; } else if (e instanceof EntityXPOrb) { return
		 * new Packet26EntityExpOrb((EntityXPOrb) e); } else { throw new
		 * IllegalArgumentException("Don\'t know how to add " + e.getClass() +
		 * "!"); } } } else { return new Packet24MobSpawn((EntityLivingBase) e);
		 * }
		 */
	}
}

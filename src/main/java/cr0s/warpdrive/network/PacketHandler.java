package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CloakedArea;
import cr0s.warpdrive.data.Vector3;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class PacketHandler {
	private static final SimpleNetworkWrapper simpleNetworkManager = NetworkRegistry.INSTANCE.newSimpleChannel(WarpDrive.MODID);
	private static Method EntityTrackerEntry_getPacketForThisEntity;
	
	public static void init() {
		// Forge packets
		simpleNetworkManager.registerMessage(MessageBeamEffect.class          , MessageBeamEffect.class          , 0, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageClientSync.class          , MessageClientSync.class          , 2, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageCloak.class               , MessageCloak.class               , 3, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageSpawnParticle.class       , MessageSpawnParticle.class       , 4, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageVideoChannel.class        , MessageVideoChannel.class        , 5, Side.CLIENT);
		
		simpleNetworkManager.registerMessage(MessageTargeting.class           , MessageTargeting.class           , 100, Side.SERVER);
		simpleNetworkManager.registerMessage(MessageClientValidation.class    , MessageClientValidation.class    , 101, Side.SERVER);
		
		// Entity packets for 'uncloaking' entities
		try {
			EntityTrackerEntry_getPacketForThisEntity = Class.forName("net.minecraft.entity.EntityTrackerEntry").getDeclaredMethod("func_151260_c"); 
			EntityTrackerEntry_getPacketForThisEntity.setAccessible(true);
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}
	
	// Beam effect sent to client side
	public static void sendBeamPacket(World worldObj, Vector3 source, Vector3 target, float red, float green, float blue, int age, int energy, int radius) {
		assert(!worldObj.isRemote);
		
		MessageBeamEffect messageBeamEffect = new MessageBeamEffect(source, target, red, green, blue, age, energy);
		
		// small beam are sent relative to beam center
		if (source.distanceTo_square(target) < 3600 /* 60 * 60 */) {
			simpleNetworkManager.sendToAllAround(messageBeamEffect, new TargetPoint(
					worldObj.provider.dimensionId, (source.x + target.x) / 2, (source.y + target.y) / 2, (source.z + target.z) / 2, radius));
		} else {// large beam are sent from both ends
			if (true) {
				List<EntityPlayerMP> playerEntityList = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
				int dimensionId = worldObj.provider.dimensionId;
				int radius_square = radius * radius;
				for (int index = 0; index < playerEntityList.size(); index++) {
					final EntityPlayerMP entityplayermp = playerEntityList.get(index);
					
					if (entityplayermp.dimension == dimensionId) {
						Vector3 player = new Vector3(entityplayermp);
						if (source.distanceTo_square(player) < radius_square || target.distanceTo_square(player) < radius_square) {
							simpleNetworkManager.sendTo(messageBeamEffect, entityplayermp);
						}
					}
				}
			} else {
				simpleNetworkManager.sendToAllAround(messageBeamEffect, new TargetPoint(
						worldObj.provider.dimensionId, source.x, source.y, source.z, radius));
				simpleNetworkManager.sendToAllAround(messageBeamEffect, new TargetPoint(
						worldObj.provider.dimensionId, target.x, target.y, target.z, radius));
			}
		}
	}
	
	public static void sendBeamPacketToPlayersInArea(World worldObj, Vector3 source, Vector3 target, float red, float green, float blue, int age, int energy, AxisAlignedBB aabb) {
		assert(!worldObj.isRemote);
		
		MessageBeamEffect messageBeamEffect = new MessageBeamEffect(source, target, red, green, blue, age, energy);
		// Send packet to all players within cloaked area
		List<Entity> list = worldObj.getEntitiesWithinAABB(EntityPlayerMP.class, aabb);
		for (Entity entity : list) {
			if (entity != null && entity instanceof EntityPlayerMP) {
				PacketHandler.simpleNetworkManager.sendTo(messageBeamEffect, (EntityPlayerMP) entity);
			}
		}
	}
	
	// Forced particle effect sent to client side
	public static void sendSpawnParticlePacket(World worldObj, final String type, final byte quantity,
	                                           final Vector3 origin, final Vector3 direction,
	                                           final float baseRed, final float baseGreen, final float baseBlue,
	                                           final float fadeRed, final float fadeGreen, final float fadeBlue,
	                                           final int radius) {
		assert(!worldObj.isRemote);
		
		MessageSpawnParticle messageSpawnParticle = new MessageSpawnParticle(
			type, quantity, origin, direction, baseRed, baseGreen, baseBlue, fadeRed, fadeGreen, fadeBlue);
		
		// small beam are sent relative to beam center
		simpleNetworkManager.sendToAllAround(messageSpawnParticle, new TargetPoint(
				worldObj.provider.dimensionId, origin.x, origin.y, origin.z, radius));
		
		if (WarpDriveConfig.LOGGING_EFFECTS) {
			WarpDrive.logger.info(String.format("Sent particle effect '%s' x %d from %s toward %s as RGB %.2f %.2f %.2f fading to %.2f %.2f %.2f",
				type, quantity, origin, direction, baseRed, baseGreen, baseBlue, fadeRed, fadeGreen, fadeBlue));
		}
	}
	
	// Monitor/Laser/Camera updating its video channel to client side
	public static void sendVideoChannelPacket(int dimensionId, int xCoord, int yCoord, int zCoord, int videoChannel) {
		MessageVideoChannel messageVideoChannel = new MessageVideoChannel(xCoord, yCoord, zCoord, videoChannel);
		simpleNetworkManager.sendToAllAround(messageVideoChannel, new TargetPoint(dimensionId, xCoord, yCoord, zCoord, 100));
		if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
			WarpDrive.logger.info("Sent video channel packet (" + xCoord + " " + yCoord + " " + zCoord + ") video channel " + videoChannel);
		}
	}
	
	// LaserCamera shooting at target (client -> server)
	public static void sendLaserTargetingPacket(int x, int y, int z, float yaw, float pitch) {
		MessageTargeting messageTargeting = new MessageTargeting(x, y, z, yaw, pitch);
		simpleNetworkManager.sendToServer(messageTargeting);
		if (WarpDriveConfig.LOGGING_TARGETING) {
			WarpDrive.logger.info("Sent targeting packet (" + x + " " + y + " " + z + ") yaw " + yaw + " pitch " + pitch);
		}
	}
	
	// Sending cloaking area definition (server -> client)
	public static void sendCloakPacket(final EntityPlayerMP entityPlayerMP, final CloakedArea area, final boolean decloak) {
		final MessageCloak messageCloak = new MessageCloak(area, decloak);
		simpleNetworkManager.sendTo(messageCloak, entityPlayerMP);
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info("Sent cloak packet (area " + area + " decloak " + decloak + ")");
		}
	}
	
	public static void sendClientSync(final EntityPlayerMP entityPlayerMP, final CelestialObject celestialObject) {
		if (WarpDriveConfig.LOGGING_CLIENT_SYNCHRONIZATION) {
			WarpDrive.logger.info(String.format("PacketHandler.sendClientSync %s", entityPlayerMP));
		}
		final MessageClientSync messageClientSync = new MessageClientSync(entityPlayerMP, celestialObject);
		simpleNetworkManager.sendTo(messageClientSync, entityPlayerMP);
	}
	
	public static Packet getPacketForThisEntity(Entity entity) {
		EntityTrackerEntry entry = new EntityTrackerEntry(entity, 0, 0, false);
		try {
			return (Packet) EntityTrackerEntry_getPacketForThisEntity.invoke(entry);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return null;
	}
}
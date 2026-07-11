package cr0s.warpdrive.network;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CloakManager;
import cr0s.warpdrive.data.CloakedArea;
import cr0s.warpdrive.data.GlobalPosition;
import cr0s.warpdrive.data.MovingEntity;
import cr0s.warpdrive.data.Vector3;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
	
	private static final SimpleNetworkWrapper simpleNetworkManager = NetworkRegistry.INSTANCE.newSimpleChannel(WarpDrive.MODID);
	private static Method EntityTrackerEntry_getPacketForThisEntity;
	private static final ConcurrentHashMap<UUID, Integer> shipNavigationMapVersions = new ConcurrentHashMap<>();
	
	public static void init() {
		// Forge packets
		simpleNetworkManager.registerMessage(MessageBeamEffect.class          , MessageBeamEffect.class          , 0, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageClientSync.class          , MessageClientSync.class          , 2, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageCloak.class               , MessageCloak.class               , 3, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageSpawnParticle.class       , MessageSpawnParticle.class       , 4, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageVideoChannel.class        , MessageVideoChannel.class        , 5, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageTransporterEffect.class   , MessageTransporterEffect.class   , 6, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageClientTileEntitySync.class, MessageClientTileEntitySync.class, 7, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageShipNavigation.class      , MessageShipNavigation.class      , 8, Side.CLIENT);
		simpleNetworkManager.registerMessage(MessageShipNavigationMap.class   , MessageShipNavigationMap.class   , 9, Side.CLIENT);
		
		simpleNetworkManager.registerMessage(MessageTargeting.class           , MessageTargeting.class           , 100, Side.SERVER);
		simpleNetworkManager.registerMessage(MessageClientValidation.class    , MessageClientValidation.class    , 101, Side.SERVER);
		simpleNetworkManager.registerMessage(MessageClientUnseating.class     , MessageClientUnseating.class     , 102, Side.SERVER);
		simpleNetworkManager.registerMessage(MessageShipNavigationAction.class, MessageShipNavigationAction.class, 103, Side.SERVER);
		
		// Entity packets for 'uncloaking' entities
		try {
			EntityTrackerEntry_getPacketForThisEntity = ReflectionHelper.findMethod(
				EntityTrackerEntry.class, "createSpawnPacket", "func_151260_c");
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}
	
	// Beam effect sent to client side
	public static void sendBeamPacket(@Nonnull final World world, final Vector3 v3Source, final Vector3 v3Target,
	                                  final float red, final float green, final float blue,
	                                  final int age, final int energy, final int radius) {
		assert !world.isRemote;
		
		final MessageBeamEffect messageBeamEffect = new MessageBeamEffect(v3Source, v3Target, red, green, blue, age);
		
		// get cloaked area
		final CloakedArea cloakedArea = CloakManager.getContainingArea(world, v3Source.getBlockPos(), v3Target.getBlockPos());
		
		// send beam from both ends
		assert world.getMinecraftServer() != null;
		final List<EntityPlayerMP> playerEntityList = world.getMinecraftServer().getPlayerList().getPlayers();
		final int dimensionId = world.provider.getDimension();
		final long radius_square = (long) radius * radius;
		for (final EntityPlayerMP entityPlayerMP : playerEntityList) {
			// is it out of range?
			if ( entityPlayerMP.world == null
			  || entityPlayerMP.world.provider.getDimension() != dimensionId
			  || ( v3Source.distanceTo_square(entityPlayerMP) > radius_square
			    && v3Target.distanceTo_square(entityPlayerMP) > radius_square ) ) {
				continue;
			}
			// is it cloaked?
			if ( cloakedArea != null
			  && !cloakedArea.isBlockWithinArea(entityPlayerMP.getPosition()) ) {
				continue;
			}
			simpleNetworkManager.sendTo(messageBeamEffect, entityPlayerMP);
		}
	}
	
	public static void sendBeamPacketToPlayersInArea(@Nonnull final World world, final Vector3 source, final Vector3 target,
	                                                 final float red, final float green, final float blue,
	                                                 final int age, final AxisAlignedBB aabb) {
		assert !world.isRemote;
		
		final MessageBeamEffect messageBeamEffect = new MessageBeamEffect(source, target, red, green, blue, age);
		
		// Send packet to all players within cloaked area
		assert world.getMinecraftServer() != null;
		final List<EntityPlayerMP> playerEntityList = world.getMinecraftServer().getPlayerList().getPlayers();
		final int dimensionId = world.provider.getDimension();
		for (final EntityPlayerMP entityPlayerMP : playerEntityList) {
			if (entityPlayerMP.dimension == dimensionId) {
				if (aabb.intersects(entityPlayerMP.getEntityBoundingBox())) {
					simpleNetworkManager.sendTo(messageBeamEffect, entityPlayerMP);
				}
			}
		}
	}
	
	// Scanning effect sent to client side
	public static void sendScanningPacket(@Nonnull final World world,
	                                      final int xMin, final int yMin, final int zMin,
	                                      final int xMax, final int yMax, final int zMax,
	                                      final float red, final float green, final float blue,
	                                      final int age) {
		assert !world.isRemote;
		
		final Vector3 vMinMin = new Vector3(xMin, yMin, zMin);
		final Vector3 vMaxMin = new Vector3(xMax, yMin, zMin);
		final Vector3 vMaxMax = new Vector3(xMax, yMin, zMax);
		final Vector3 vMinMax = new Vector3(xMin, yMin, zMax);
		
		sendBeamPacket(world, vMinMin, vMaxMin, red, green, blue, age, 0, 50);
		sendBeamPacket(world, vMaxMin, vMaxMax, red, green, blue, age, 0, 50);
		sendBeamPacket(world, vMaxMax, vMinMax, red, green, blue, age, 0, 50);
		sendBeamPacket(world, vMinMax, vMinMin, red, green, blue, age, 0, 50);
	}
	
	// Forced particle effect sent to client side
	public static void sendSpawnParticlePacket(final World world, final String type, final byte quantity,
	                                           final Vector3 origin, final Vector3 direction,
	                                           final float baseRed, final float baseGreen, final float baseBlue,
	                                           final float fadeRed, final float fadeGreen, final float fadeBlue,
	                                           final int radius) {
		assert !world.isRemote;
		
		final MessageSpawnParticle messageSpawnParticle = new MessageSpawnParticle(
			type, quantity, origin, direction, baseRed, baseGreen, baseBlue, fadeRed, fadeGreen, fadeBlue);
		
		// get cloaked area
		final CloakedArea cloakedArea = CloakManager.getContainingArea(world, origin.getBlockPos(), null);
		
		// send particle to players in range and the same cloak
		assert world.getMinecraftServer() != null;
		final List<EntityPlayerMP> playerEntityList = world.getMinecraftServer().getPlayerList().getPlayers();
		final long radius_square = (long) radius * radius;
		for (final EntityPlayerMP entityPlayerMP : playerEntityList) {
			// is it out of range?
			if ( entityPlayerMP.world == null
			  || entityPlayerMP.world.provider.getDimension() != world.provider.getDimension()
			  || origin.distanceTo_square(entityPlayerMP) > radius_square ) {
				continue;
			}
			// is it cloaked?
			if ( cloakedArea != null
			  && !cloakedArea.isBlockWithinArea(entityPlayerMP.getPosition()) ) {
				continue;
			}
			simpleNetworkManager.sendTo(messageSpawnParticle, entityPlayerMP);
		}
		
		if (WarpDriveConfig.LOGGING_EFFECTS) {
			WarpDrive.logger.info(String.format("Sent particle effect '%s' x %d from %s toward %s as RGB %.2f %.2f %.2f fading to %.2f %.2f %.2f",
				type, quantity, origin, direction, baseRed, baseGreen, baseBlue, fadeRed, fadeGreen, fadeBlue));
		}
	}
	
	// Transporter effect sent to client side
	public static void sendTransporterEffectPacket(final World world, final GlobalPosition globalPositionLocal, final GlobalPosition globalPositionRemote, final double lockStrength,
	                                               final Collection<MovingEntity> movingEntitiesLocal, final Collection<MovingEntity> movingEntitiesRemote,
	                                               final int tickEnergizing, final int tickCooldown, final int radius) {
		assert !world.isRemote;
		
		final MessageTransporterEffect messageTransporterEffectLocal = new MessageTransporterEffect(
				true, globalPositionLocal, movingEntitiesLocal,
				lockStrength, tickEnergizing, tickCooldown);
		final MessageTransporterEffect messageTransporterEffectRemote = new MessageTransporterEffect(
				false, globalPositionRemote, movingEntitiesRemote,
				lockStrength, tickEnergizing, tickCooldown);
		
		// check both ends to send packet
		assert world.getMinecraftServer() != null;
		final List<EntityPlayerMP> playerEntityList = world.getMinecraftServer().getPlayerList().getPlayers();
		final long radius_square = (long) radius * radius;
		for (final EntityPlayerMP entityPlayerMP : playerEntityList) {
			if ( globalPositionLocal != null
			  && globalPositionLocal.distance2To(entityPlayerMP) < radius_square ) {
				simpleNetworkManager.sendTo(messageTransporterEffectLocal, entityPlayerMP);
			}
			if ( globalPositionRemote != null
			  && globalPositionRemote.distance2To(entityPlayerMP) < radius_square ) {
				simpleNetworkManager.sendTo(messageTransporterEffectRemote, entityPlayerMP);
			}
		}
	}
	
	// Forces a full tile entity re-sync to nearby clients
	public static void sendTileEntitySyncToClients(final World world, final BlockPos blockPos, final NBTTagCompound tagCompound) {
		if (world.isRemote) {
			return;
		}
		final MessageClientTileEntitySync message = new MessageClientTileEntitySync(blockPos, tagCompound);
		simpleNetworkManager.sendToAllAround(message, new TargetPoint(world.provider.getDimension(), blockPos.getX(), blockPos.getY(), blockPos.getZ(), 256));
	}
	
	// Monitor/Laser/Camera updating its video channel to client side
	public static void sendVideoChannelPacket(final World world, final BlockPos blockPos, final int videoChannel) {
		final MessageVideoChannel messageVideoChannel = new MessageVideoChannel(blockPos, videoChannel);
		simpleNetworkManager.sendToAllAround(messageVideoChannel, new TargetPoint(world.provider.getDimension(), blockPos.getX(),blockPos.getY(), blockPos.getZ(), 100));
		if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
			WarpDrive.logger.info(String.format("Sent video channel packet at %s videoChannel %d",
			                                    Commons.format(world, blockPos), videoChannel));
		}
	}
	
	// LaserCamera shooting at target (client -> server)
	public static void sendLaserTargetingPacket(final int x, final int y, final int z, final float yaw, final float pitch) {
		final MessageTargeting messageTargeting = new MessageTargeting(x, y, z, yaw, pitch);
		simpleNetworkManager.sendToServer(messageTargeting);
		if (WarpDriveConfig.LOGGING_TARGETING) {
			WarpDrive.logger.info(String.format("Sent targeting packet (%d %d %d) yaw %.3f pitch %.3f",
			                                    x, y, z, yaw, pitch));
		}
	}
	
	// Sending cloaking area definition (server -> client)
	public static void sendCloakPacket(final EntityPlayerMP entityPlayerMP, final CloakedArea area, final boolean isUncloaking) {
		final MessageCloak messageCloak = new MessageCloak(area, isUncloaking);
		simpleNetworkManager.sendTo(messageCloak, entityPlayerMP);
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("Sent cloak packet (area %s isUncloaking %s)",
			                                    area, isUncloaking));
		}
	}
	
	public static void sendClientSync(final EntityPlayerMP entityPlayerMP, final CelestialObject celestialObject) {
		if (WarpDriveConfig.LOGGING_CLIENT_SYNCHRONIZATION) {
			WarpDrive.logger.info(String.format("PacketHandler.sendClientSync %s",
			                                    entityPlayerMP));
		}
		final MessageClientSync messageClientSync = new MessageClientSync(entityPlayerMP, celestialObject);
		simpleNetworkManager.sendTo(messageClientSync, entityPlayerMP);
	}
	
	public static void sendShipNavigationPacket(final EntityPlayerMP entityPlayerMP, final NBTTagCompound tagCompound) {
		final MessageShipNavigation messageShipNavigation = new MessageShipNavigation(tagCompound);
		simpleNetworkManager.sendTo(messageShipNavigation, entityPlayerMP);
	}
	
	public static void sendShipNavigationMapPacket(final EntityPlayerMP entityPlayerMP, final NBTTagCompound tagCompound) {
		sendShipNavigationMapPacket(entityPlayerMP, tagCompound, false);
	}
	
	public static void sendShipNavigationMapPacket(final EntityPlayerMP entityPlayerMP, final NBTTagCompound tagCompound, final boolean force) {
		final int mapVersion = tagCompound.getInteger("mapVersion");
		final Integer mapVersionPrevious = force ? null : shipNavigationMapVersions.put(entityPlayerMP.getUniqueID(), mapVersion);
		if (!force && mapVersionPrevious != null && mapVersionPrevious == mapVersion) {
			return;
		}
		if (force) {
			shipNavigationMapVersions.put(entityPlayerMP.getUniqueID(), mapVersion);
		}
		final MessageShipNavigationMap messageShipNavigationMap = new MessageShipNavigationMap(tagCompound);
		simpleNetworkManager.sendTo(messageShipNavigationMap, entityPlayerMP);
	}

	// Build and send the static celestial map only when this player's cached version is stale.
	// Cheap to call on every action/refresh: getMapVersion() is a small hash, the full snapshot is only built on a miss.
	public static void sendShipNavigationMapIfChanged(final EntityPlayerMP entityPlayerMP) {
		final int mapVersion = cr0s.warpdrive.block.movement.ShipNavigationHelper.getMapVersion();
		final Integer mapVersionPrevious = shipNavigationMapVersions.get(entityPlayerMP.getUniqueID());
		if (mapVersionPrevious != null && mapVersionPrevious == mapVersion) {
			return;
		}
		shipNavigationMapVersions.put(entityPlayerMP.getUniqueID(), mapVersion);
		final MessageShipNavigationMap messageShipNavigationMap = new MessageShipNavigationMap(cr0s.warpdrive.block.movement.ShipNavigationHelper.buildStaticMapSnapshot());
		simpleNetworkManager.sendTo(messageShipNavigationMap, entityPlayerMP);
	}

	public static void sendShipNavigationAction(final byte action, final int dimensionId,
	                                            final BlockPos blockPosCore, final BlockPos blockPosAccess,
	                                            final String targetId) {
		final MessageShipNavigationAction messageShipNavigationAction = new MessageShipNavigationAction(action, dimensionId, blockPosCore, blockPosAccess, targetId);
		simpleNetworkManager.sendToServer(messageShipNavigationAction);
	}
	
	public static void sendShipNavigationAction(final byte action, final int dimensionId,
	                                            final BlockPos blockPosCore, final BlockPos blockPosAccess,
	                                            final NBTTagCompound payload) {
		final MessageShipNavigationAction messageShipNavigationAction = new MessageShipNavigationAction(action, dimensionId, blockPosCore, blockPosAccess, payload);
		simpleNetworkManager.sendToServer(messageShipNavigationAction);
	}
	
	public static Packet<?> getPacketForThisEntity(final Entity entity) {
		// skip buggy entities
		if (Dictionary.isNoReveal(entity)) {
			return null;
		}
		
		final EntityTrackerEntry entry = new EntityTrackerEntry(entity, 0, 0, 0, false);
		try {
			return (Packet<?>) EntityTrackerEntry_getPacketForThisEntity.invoke(entry);
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
		WarpDrive.logger.error(String.format("Unable to get packet for entity %s, consider adding the NoReveal tag to entities with id %s.",
		                                     entity, Dictionary.getId(entity) ));
		Dictionary.addToNoReveal(entity);
		return null;
	}
	
	public static void revealEntityToPlayer(final Entity entity, final EntityPlayerMP entityPlayerMP) {
		try {
			if (entityPlayerMP.connection == null) {
				WarpDrive.logger.warn(String.format("Unable to reveal entity %s to player %s: no connection",
				                                    entity, entityPlayerMP));
				return;
			}
			final Packet<?> packet = getPacketForThisEntity(entity);
			if (packet == null) {
				// note: error is already logged by getPacketForThisEntity()
				return;
			}
			if (WarpDriveConfig.LOGGING_CLOAKING) {
				WarpDrive.logger.info(String.format("Revealing entity %s with patcket %s",
				                                    entity, packet));
			}
			entityPlayerMP.connection.sendPacket(packet);
			
			if (!entity.getDataManager().isEmpty()) {
				entityPlayerMP.connection.sendPacket(new SPacketEntityMetadata(entity.getEntityId(), entity.getDataManager(), true));
			}
			
			if (entity instanceof EntityLivingBase) {
				final AttributeMap attributemap = (AttributeMap) ((EntityLivingBase) entity).getAttributeMap();
				final Collection<IAttributeInstance> collection = attributemap.getWatchedAttributes();
				
				if (!collection.isEmpty()) {
					entityPlayerMP.connection.sendPacket(new SPacketEntityProperties(entity.getEntityId(), collection));
				}
				
				// if (((EntityLivingBase)this.trackedEntity).isElytraFlying()) ... (we always send velocity information)
			}
			
			if (!(packet instanceof SPacketSpawnMob)) {
				entityPlayerMP.connection.sendPacket(new SPacketEntityVelocity(entity.getEntityId(), entity.motionX, entity.motionY, entity.motionZ));
			}
			
			if (entity instanceof EntityLivingBase) {
				for (final EntityEquipmentSlot entityequipmentslot : EntityEquipmentSlot.values()) {
					final ItemStack itemstack = ((EntityLivingBase) entity).getItemStackFromSlot(entityequipmentslot);
					
					if (!itemstack.isEmpty()) {
						entityPlayerMP.connection.sendPacket(new SPacketEntityEquipment(entity.getEntityId(), entityequipmentslot, itemstack));
					}
				}
			}
			
			if (entity instanceof EntityPlayer) {
				final EntityPlayer entityplayer = (EntityPlayer) entity;
				
				if (entityplayer.isPlayerSleeping()) {
					entityPlayerMP.connection.sendPacket(new SPacketUseBed(entityplayer, new BlockPos(entity)));
				}
			}
			
			if (entity instanceof EntityLivingBase) {
				final EntityLivingBase entitylivingbase = (EntityLivingBase) entity;
				
				for (final PotionEffect potioneffect : entitylivingbase.getActivePotionEffects()) {
					entityPlayerMP.connection.sendPacket(new SPacketEntityEffect(entity.getEntityId(), potioneffect));
				}
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
	}
	
	// Player dismounting from its seat (client -> server)
	public static void sendUnseating() {
		final MessageClientUnseating messageClientUnseating = new MessageClientUnseating();
		simpleNetworkManager.sendToServer(messageClientUnseating);
		if (WarpDriveConfig.LOGGING_CAMERA) {
			WarpDrive.logger.info("Sent unseating packet");
		}
	}
}

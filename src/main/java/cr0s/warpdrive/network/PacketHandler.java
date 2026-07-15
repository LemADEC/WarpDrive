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
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class PacketHandler {
	
	private static final SimpleChannel SIMPLE_CHANNEL = NetworkRegistry.ChannelBuilder
			                                                    .named(new ResourceLocation(WarpDrive.MODID, "network"))
			                                                    .networkProtocolVersion(() -> WarpDrive.PROTOCOL_VERSION)
			                                                    .clientAcceptedVersions(WarpDrive.PROTOCOL_VERSION::equals)
			                                                    .serverAcceptedVersions(WarpDrive.PROTOCOL_VERSION::equals)
			                                                    .simpleChannel();
	
	public interface IMessage {
		
		void encode(@Nonnull final PacketBuffer buffer);
		
		void decode(@Nonnull final PacketBuffer buffer);
		
		IMessage process(@Nonnull final Context context);
	}
	
	private static <T extends IMessage> void registerMessage(@Nonnull final Class<T> packetType, final int messageId, @Nonnull final NetworkDirection networkDirection) {
		SIMPLE_CHANNEL.registerMessage(messageId,
		                               packetType,
		                               IMessage::encode,
		                               (buf) -> {
		                                     	T t;
		                                     	try {
		                                     		t = packetType.newInstance();
		                                     	} catch (InstantiationException | IllegalAccessException e) {
		                                     		e.printStackTrace(WarpDrive.printStreamError);
		                                     		return null;
		                                     	}
		                                     	t.decode(buf);
		                                     	return t;
		                                     },
		                               (t, contextSupplier) -> {
		                                        final Context context = contextSupplier.get();
		                                        final IMessage response = t.process(context);
		                                        if (response != null) {
		                                        	SIMPLE_CHANNEL.sendTo(response, context.getNetworkManager(), context.getDirection().reply());
		                                        }
		                                        context.setPacketHandled(true);
		                                     },
		                               Optional.of(networkDirection));
	}
	
	public static void init() {
		// Forge packets
		registerMessage(MessageBeamEffect.class          , 0, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(MessageClientSync.class          , 2, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(MessageCloak.class               , 3, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(MessageSpawnParticle.class       , 4, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(MessageVideoChannel.class        , 5, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(MessageTransporterEffect.class   , 6, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(MessageClientTileEntitySync.class, 7, NetworkDirection.PLAY_TO_CLIENT);
		
		registerMessage(MessageTargeting.class           , 100, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(MessageClientValidation.class    , 101, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(MessageClientUnseating.class     , 102, NetworkDirection.PLAY_TO_SERVER);
	}
	
	private static void sendToPlayer(@Nonnull final IMessage message, @Nonnull ServerPlayerEntity entityServerPlayer) {
		SIMPLE_CHANNEL.sendTo(message,
		                      entityServerPlayer.connection.getNetworkManager(),
		                      NetworkDirection.PLAY_TO_CLIENT );
	}
	
	private static void sendToPlayers(@Nonnull final IMessage message, @Nonnull final World world,
	                                  @Nonnull final Vector3 v3Source, @Nullable final Vector3 v3Target, final int radius) {
		final MinecraftServer server = world.getServer();
		assert server != null;
		final List<ServerPlayerEntity> serverPlayerEntities = server.getPlayerList().getPlayers();
		final DimensionType dimensionType = world.getDimension().getType();
		final int radius_square = radius * radius;
		for (final ServerPlayerEntity entityServerPlayer : serverPlayerEntities) {
			if (entityServerPlayer.dimension == dimensionType) {
				if ( v3Source.distanceTo_square(entityServerPlayer) < radius_square
				  || ( v3Target != null
				    && v3Target.distanceTo_square(entityServerPlayer) < radius_square ) ) {
					sendToPlayer(message, entityServerPlayer);
				}
			}
		}
	}
	
	// Beam effect sent to client side
	public static void sendBeamPacket(@Nonnull final World world, final Vector3 v3Source, final Vector3 v3Target,
	                                  final float red, final float green, final float blue,
	                                  final int age, final int energy, final int radius) {
		assert !world.isRemote();
		
		final MessageBeamEffect messageBeamEffect = new MessageBeamEffect(v3Source, v3Target, red, green, blue, age);
		
		// get cloaked area
		final CloakedArea cloakedArea = CloakManager.getContainingArea(world, v3Source.getBlockPos(), v3Target.getBlockPos());
		
		// send beam from both ends
		assert world.getServer() != null;
		final List<ServerPlayerEntity> playerEntityList = world.getServer().getPlayerList().getPlayers();
		final DimensionType dimensionType = world.getDimension().getType();
		final long radius_square = (long) radius * radius;
		for (final ServerPlayerEntity entityPlayerMP : playerEntityList) {
			// is it out of range?
			if ( entityPlayerMP.world == null
			  || entityPlayerMP.world.getDimension().getType() != dimensionType
			  || ( v3Source.distanceTo_square(entityPlayerMP) > radius_square
			    && v3Target.distanceTo_square(entityPlayerMP) > radius_square ) ) {
				continue;
			}
			// is it cloaked?
			if ( cloakedArea != null
			  && !cloakedArea.isBlockWithinArea(entityPlayerMP.getPosition()) ) {
				continue;
			}
			sendToPlayer(messageBeamEffect, entityPlayerMP);
		}
	}
	
	public static void sendBeamPacketToPlayersInArea(@Nonnull final World world, final Vector3 source, final Vector3 target,
	                                                 final float red, final float green, final float blue,
	                                                 final int age, final AxisAlignedBB aabb) {
		assert !world.isRemote();
		
		final MessageBeamEffect messageBeamEffect = new MessageBeamEffect(source, target, red, green, blue, age);
		
		// Send packet to all players within cloaked area
		final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
		assert server != null;
		final List<ServerPlayerEntity> entityServerPlayers = server.getPlayerList().getPlayers();
		final DimensionType dimensionType = world.getDimension().getType();
		for (final ServerPlayerEntity entityServerPlayer : entityServerPlayers) {
			if (entityServerPlayer.dimension == dimensionType) {
				if (aabb.intersects(entityServerPlayer.getBoundingBox())) {
					sendToPlayer(messageBeamEffect, entityServerPlayer);
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
		assert !world.isRemote();
		
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
	public static void sendSpawnParticlePacket(@Nonnull final World world, final String type, final byte quantity,
	                                           final Vector3 origin, final Vector3 direction,
	                                           final float baseRed, final float baseGreen, final float baseBlue,
	                                           final float fadeRed, final float fadeGreen, final float fadeBlue,
	                                           final int radius) {
		assert !world.isRemote();
		
		final MessageSpawnParticle messageSpawnParticle = new MessageSpawnParticle(
			type, quantity, origin, direction, baseRed, baseGreen, baseBlue, fadeRed, fadeGreen, fadeBlue);
		
		// get cloaked area
		final CloakedArea cloakedArea = CloakManager.getContainingArea(world, origin.getBlockPos(), null);
		
		// send particle to players in range and the same cloak
		assert world.getServer() != null;
		final List<ServerPlayerEntity> playerEntityList = world.getServer().getPlayerList().getPlayers();
		final long radius_square = (long) radius * radius;
		for (final ServerPlayerEntity entityPlayerMP : playerEntityList) {
			// is it out of range?
			if ( entityPlayerMP.world == null
			  || entityPlayerMP.world.getDimension().getType() != world.getDimension().getType()
			  || origin.distanceTo_square(entityPlayerMP) > radius_square ) {
				continue;
			}
			// is it cloaked?
			if ( cloakedArea != null
			  && !cloakedArea.isBlockWithinArea(entityPlayerMP.getPosition()) ) {
				continue;
			}
			sendToPlayer(messageSpawnParticle, entityPlayerMP);
		}
		
		if (WarpDriveConfig.LOGGING_EFFECTS) {
			WarpDrive.logger.info(String.format("Sent particle effect '%s' x %d from %s toward %s as RGB %.2f %.2f %.2f fading to %.2f %.2f %.2f",
				type, quantity, origin, direction, baseRed, baseGreen, baseBlue, fadeRed, fadeGreen, fadeBlue));
		}
	}
	
	// Transporter effect sent to client side
	public static void sendTransporterEffectPacket(@Nonnull final World world, final GlobalPosition globalPositionLocal, final GlobalPosition globalPositionRemote, final double lockStrength,
	                                               final Collection<MovingEntity> movingEntitiesLocal, final Collection<MovingEntity> movingEntitiesRemote,
	                                               final int tickEnergizing, final int tickCooldown, final int radius) {
		assert !world.isRemote();
		
		final MessageTransporterEffect messageTransporterEffectLocal = new MessageTransporterEffect(
				true, globalPositionLocal, movingEntitiesLocal,
				lockStrength, tickEnergizing, tickCooldown );
		final MessageTransporterEffect messageTransporterEffectRemote = new MessageTransporterEffect(
				false, globalPositionRemote, movingEntitiesRemote,
				lockStrength, tickEnergizing, tickCooldown );
		
		// check both ends to send packet
		final MinecraftServer server = world.getServer();
		assert server != null;
		final List<ServerPlayerEntity> serverPlayerEntities = server.getPlayerList().getPlayers();
		final long radius_square = (long) radius * radius;
		for (final ServerPlayerEntity entityServerPlayer : serverPlayerEntities) {
			if ( globalPositionLocal != null
			  && globalPositionLocal.distance2To(entityServerPlayer) < radius_square ) {
				sendToPlayer(messageTransporterEffectLocal, entityServerPlayer);
			}
			if ( globalPositionRemote != null
			  && globalPositionRemote.distance2To(entityServerPlayer) < radius_square ) {
				sendToPlayer(messageTransporterEffectRemote, entityServerPlayer);
			}
		}
	}
	
	// Forces a full tile entity re-sync to nearby clients
	public static void sendTileEntitySyncToClients(final World world, final BlockPos blockPos, final CompoundNBT tagCompound) {
		if (world.isRemote()) {
			return;
		}
		final MessageClientTileEntitySync message = new MessageClientTileEntitySync(blockPos, tagCompound);
		sendToPlayers(message, world, new Vector3(blockPos.getX() + 0.5D,
		                                          blockPos.getY() + 0.5D,
		                                          blockPos.getZ() + 0.5D), null, 256 );
	}
	
	// Monitor/Laser/Camera updating its video channel to client side
	public static void sendVideoChannelPacket(final World world, final BlockPos blockPos, final int videoChannel) {
		final MessageVideoChannel messageVideoChannel = new MessageVideoChannel(blockPos, videoChannel);
		sendToPlayers(messageVideoChannel, world, new Vector3(blockPos.getX() + 0.5D,
		                                                      blockPos.getY() + 0.5D,
		                                                      blockPos.getZ() + 0.5D), null, 100 );
		if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
			WarpDrive.logger.info(String.format("Sent video channel packet at %s videoChannel %d",
			                                    Commons.format(world, blockPos), videoChannel));
		}
	}
	
	// LaserCamera shooting at target (client -> server)
	public static void sendLaserTargetingPacket(final int x, final int y, final int z, final float yaw, final float pitch) {
		final MessageTargeting messageTargeting = new MessageTargeting(x, y, z, yaw, pitch);
		SIMPLE_CHANNEL.sendToServer(messageTargeting);
		if (WarpDriveConfig.LOGGING_TARGETING) {
			WarpDrive.logger.info(String.format("Sent targeting packet (%d %d %d) yaw %.3f pitch %.3f",
			                                    x, y, z, yaw, pitch));
		}
	}
	
	// Sending cloaking area definition (server -> client)
	public static void sendCloakPacket(final ServerPlayerEntity entityServerPlayer, final CloakedArea area, final boolean isUncloaking) {
		final MessageCloak messageCloak = new MessageCloak(area, isUncloaking);
		sendToPlayer(messageCloak, entityServerPlayer);
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("Sent cloak packet (area %s isUncloaking %s)",
			                                    area, isUncloaking));
		}
	}
	
	public static void sendClientSync(final ServerPlayerEntity entityServerPlayer, final CelestialObject celestialObject) {
		if (WarpDriveConfig.LOGGING_CLIENT_SYNCHRONIZATION) {
			WarpDrive.logger.info(String.format("PacketHandler.sendClientSync %s",
			                                    entityServerPlayer));
		}
		final MessageClientSync messageClientSync = new MessageClientSync(celestialObject);
		sendToPlayer(messageClientSync, entityServerPlayer);
	}
	
	public static IPacket<?> getPacketForThisEntity(final Entity entity) {
		// skip buggy entities
		if (Dictionary.isNoReveal(entity)) {
			return null;
		}
		
		try {
            return entity.createSpawnPacket();
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
		WarpDrive.logger.error(String.format("Unable to get packet for entity %s, consider adding the NoReveal tag to entities with id %s.",
		                                     entity, Dictionary.getId(entity) ));
		Dictionary.addToNoReveal(entity);
		return null;
	}
	
	public static void revealEntityToPlayer(final Entity entity, final ServerPlayerEntity entityServerPlayer) {
		try {
			if (entityServerPlayer.connection == null) {
				WarpDrive.logger.warn(String.format("Unable to reveal entity %s to player %s: no connection",
				                                    entity, entityServerPlayer));
				return;
			}
			final IPacket<?> packet = getPacketForThisEntity(entity);
			if (packet == null) {
				// note: error is already logged by getPacketForThisEntity()
				return;
			}
			if (WarpDriveConfig.LOGGING_CLOAKING) {
				WarpDrive.logger.info(String.format("Revealing entity %s with patcket %s",
				                                    entity, packet));
			}
			entityServerPlayer.connection.sendPacket(packet);
			
			if (!entity.getDataManager().isEmpty()) {
				entityServerPlayer.connection.sendPacket(new SEntityMetadataPacket(entity.getEntityId(), entity.getDataManager(), true));
			}
			
			if (entity instanceof LivingEntity) {
				final AttributeMap attributemap = (AttributeMap) ((LivingEntity) entity).getAttributes();
				final Collection<IAttributeInstance> collection = attributemap.getWatchedAttributes();
				
				if (!collection.isEmpty()) {
					entityServerPlayer.connection.sendPacket(new SEntityPropertiesPacket(entity.getEntityId(), collection));
				}
				
				// if (((EntityLivingBase)this.trackedEntity).isElytraFlying()) ... (we always send velocity information)
			}
			
			if (!(packet instanceof SSpawnMobPacket)) {
				entityServerPlayer.connection.sendPacket(new SEntityVelocityPacket(entity.getEntityId(), entity.getMotion()));
			}
			
			if (entity instanceof LivingEntity) {
				for (final EquipmentSlotType entityequipmentslot : EquipmentSlotType.values()) {
					final ItemStack itemstack = ((LivingEntity) entity).getItemStackFromSlot(entityequipmentslot);
					
					if (!itemstack.isEmpty()) {
						entityServerPlayer.connection.sendPacket(new SEntityEquipmentPacket(entity.getEntityId(), entityequipmentslot, itemstack));
					}
				}
			}
			
			if (entity instanceof LivingEntity) {
				final LivingEntity entityliving = (LivingEntity) entity;
				
				for (final EffectInstance potioneffect : entityliving.getActivePotionEffects()) {
					entityServerPlayer.connection.sendPacket(new SPlayEntityEffectPacket(entity.getEntityId(), potioneffect));
				}
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
	}
	
	// Player dismounting from its seat (client -> server)
	public static void sendUnseating() {
		final MessageClientUnseating messageClientUnseating = new MessageClientUnseating();
		SIMPLE_CHANNEL.sendToServer(messageClientUnseating);
		if (WarpDriveConfig.LOGGING_CAMERA) {
			WarpDrive.logger.info("Sent unseating packet");
		}
	}
}
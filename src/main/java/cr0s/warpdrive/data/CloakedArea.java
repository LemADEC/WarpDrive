package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.network.PacketHandler;
import cr0s.warpdrive.render.EntityFXBeam;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class CloakedArea {
	
	public ResourceLocation dimensionId;
	public BlockPos blockPosCore;
	public int minX, minY, minZ;
	public int maxX, maxY, maxZ;
	private final CopyOnWriteArraySet<UUID> playersInArea;
	public boolean isFullyTransparent;
	public BlockState blockStateFog;
	
	public CloakedArea(@Nullable final World world,
	                   @Nonnull final ResourceLocation dimensionId, @Nonnull final BlockPos blockPosCore, final boolean isFullyTransparent,
	                   final int minX, final int minY, final int minZ,
	                   final int maxX, final int maxY, final int maxZ) {
		this.dimensionId = dimensionId;
		this.blockPosCore = blockPosCore;
		this.isFullyTransparent = isFullyTransparent;
		
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		
		this.playersInArea = new CopyOnWriteArraySet<>();
		
		if (world != null) {
			try {
				// Add all players currently inside the field
				final List<PlayerEntity> list = world.getEntitiesWithinAABB(ServerPlayerEntity.class, new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
				for (final PlayerEntity player : list) {
					addPlayer(player.getUniqueID());
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
			}
		}
		
		if (!isFullyTransparent) {
			blockStateFog = WarpDrive.blockGas[EnumGasColor.DARKNESS.ordinal()].getDefaultState();
		} else {
			blockStateFog = Blocks.AIR.getDefaultState();
		}
	}
	
	public boolean isPlayerListedInArea(final UUID uuidPlayer) {
		return playersInArea.contains(uuidPlayer);
	}
	
	private void removePlayer(final UUID uuidPlayer) {
		playersInArea.remove(uuidPlayer);
	}
	
	private void addPlayer(final UUID uuidPlayer) {
		playersInArea.add(uuidPlayer);
	}
	
	public boolean isEntityWithinArea(@Nonnull final LivingEntity entity) {
		return (minX <= entity.getPosX() && (maxX + 1) > entity.getPosX()
			 && minY <= (entity.getPosY() + entity.getHeight()) && (maxY + 1) > entity.getPosY()
			 && minZ <= entity.getPosZ() && (maxZ + 1) > entity.getPosZ());
	}
	
	public boolean isBlockWithinArea(@Nonnull final BlockPos blockPos) {
		return (minX <= blockPos.getX() && (maxX + 1) > blockPos.getX()
			 && minY <= blockPos.getY() && (maxY + 1) > blockPos.getY()
			 && minZ <= blockPos.getZ() && (maxZ + 1) > blockPos.getZ());
	}
	
	// Sending only if field changes: sets up or collapsing
	public void sendCloakPacketToPlayersEx(final boolean isUncloaking) {
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("sendCloakPacketToPlayersEx isUncloaking %s", isUncloaking));
		}
		final int RADIUS = 250;
		
		final double midX = minX + (Math.abs(maxX - minX) / 2.0D);
		final double midY = minY + (Math.abs(maxY - minY) / 2.0D);
		final double midZ = minZ + (Math.abs(maxZ - minZ) / 2.0D);
		
		final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
		for (final ServerPlayerEntity entityServerPlayer : server.getPlayerList().getPlayers()) {
			if (dimensionId.equals(entityServerPlayer.dimension.getRegistryName())) {
				final double dX = midX - entityServerPlayer.getPosX();
				final double dY = midY - entityServerPlayer.getPosY();
				final double dZ = midZ - entityServerPlayer.getPosZ();
				
				if (Math.abs(dX) < RADIUS && Math.abs(dY) < RADIUS && Math.abs(dZ) < RADIUS) {
					if (isUncloaking) {
						PacketHandler.sendCloakPacket(entityServerPlayer, this, true);
						revealChunksToPlayer(entityServerPlayer);
						revealEntitiesToPlayer(entityServerPlayer);
					} else if (!isEntityWithinArea(entityServerPlayer)) {
						PacketHandler.sendCloakPacket(entityServerPlayer, this, false);
					}
				}
			}
		}
	}
	
	public void updatePlayer(final ServerPlayerEntity entityServerPlayer) {
		if (isEntityWithinArea(entityServerPlayer)) {
			if (!isPlayerListedInArea(entityServerPlayer.getUniqueID())) {
				if (WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info(String.format("%s Player %s has entered",
					                                    this, entityServerPlayer.getName()));
				}
				addPlayer(entityServerPlayer.getUniqueID());
				revealChunksToPlayer(entityServerPlayer);
				revealEntitiesToPlayer(entityServerPlayer);
				PacketHandler.sendCloakPacket(entityServerPlayer, this, false);
			}
		} else {
			if (isPlayerListedInArea(entityServerPlayer.getUniqueID())) {
				if (WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info(String.format("%s Player %s has left",
					                                    this, entityServerPlayer.getName()));
				}
				removePlayer(entityServerPlayer.getUniqueID());
				final IPacket<?> packetToSend = entityServerPlayer.createSpawnPacket();
				if (packetToSend != null) {
                    final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
				    server.getPlayerList()
				          .sendToAllNearExcept(
						                entityServerPlayer,
						                entityServerPlayer.getPosX(), entityServerPlayer.getPosY(), entityServerPlayer.getPosZ(),
						                100,
						                entityServerPlayer.world.getDimension().getType(),
						                packetToSend);
				}
				PacketHandler.sendCloakPacket(entityServerPlayer, this, false);
			}
		}
	}
	
	public void revealChunksToPlayer(final PlayerEntity player) {
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			 WarpDrive.logger.info(String.format("%s Revealing cloaked blocks to player %s",
			                                     this, player.getName()));
		}
		final int minY_clamped = Math.max(0, minY);
		final int maxY_clamped = Math.min(255, maxY);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY_clamped; y <= maxY_clamped; y++) {
					BlockPos blockPos = new BlockPos(x, y, z);
					BlockState blockState = player.world.getBlockState(blockPos);
					if (blockState.getBlock() != Blocks.AIR) {
						player.world.notifyBlockUpdate(blockPos, blockState, blockState, 3);
						
						JumpBlock.refreshBlockStateOnClient(player.world, new BlockPos(x, y, z));
					}
				}
			}
		}
		
		/*
		final ArrayList<Chunk> chunksToSend = new ArrayList<Chunk>();
		
		for (int x = minX >> 4; x <= maxX >> 4; x++) {
			for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
				chunksToSend.add(p.world.getChunk(x, z));
			}
		}
		
		//System.out.println("[Cloak] Sending " + chunksToSend.size() + " chunks to player " + p.username);
		((EntityPlayerMP) p).connection.sendPacketToPlayer(new Packet56MapChunks(chunksToSend));
		
		//System.out.println("[Cloak] Sending decloak packet to player " + p.username);
		area.sendCloakPacketToPlayer(p, true);
		// decloak = true
		
		/**/
	}
	
	public void revealEntitiesToPlayer(final ServerPlayerEntity entityServerPlayer) {
		final List<Entity> list = entityServerPlayer.world.getEntitiesWithinAABBExcludingEntity(entityServerPlayer, new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
		
		for (final Entity entity : list) {
			PacketHandler.revealEntityToPlayer(entity, entityServerPlayer);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public void clientCloak(@Nonnull final ClientPlayerEntity player) {
		assert Commons.isSafeThread();
		
		// Hide the blocks within area
		if (WarpDriveConfig.LOGGING_CLOAKING) { WarpDrive.logger.info("Refreshing cloaked blocks..."); }
		final World world = player.getEntityWorld();
		final int minY_clamped = Math.max(0, minY);
		final int maxY_clamped = Math.min(255, maxY);
		for (final BlockPos mutableBlockPos : BlockPos.Mutable.getAllInBoxMutable(minX, minY_clamped, minZ, maxX, maxY_clamped, maxZ)) {
			final BlockState blockState = world.getBlockState(mutableBlockPos);
			if (blockState.getBlock() != Blocks.AIR) {
				world.setBlockState(mutableBlockPos, blockStateFog, 4);
			}
		}
		
		// Force a render refresh since light engine is async now
		Minecraft.getInstance().worldRenderer.markBlockRangeForRenderUpdate(
			minX - 1, Math.max(  0, minY - 1), minZ - 1,
			maxX + 1, Math.min(255, maxY + 1), maxZ + 1);
		
		// Hide any entities inside area
		if (WarpDriveConfig.LOGGING_CLOAKING) { WarpDrive.logger.info("Refreshing cloaked entities..."); }
		final AxisAlignedBB aabb = new AxisAlignedBB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(player, aabb);
		for (final Entity entity : list) {
			entity.remove();
			((ClientWorld) world).removeEntityFromWorld(entity.getEntityId());
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public void clientDecloak() {
		final World world = Minecraft.getInstance().world;
		assert world != null;
		Minecraft.getInstance().worldRenderer.markBlockRangeForRenderUpdate(
			minX - 1, Math.max(  0, minY - 1), minZ - 1,
			maxX + 1, Math.min(255, maxY + 1), maxZ + 1);
		
		// Make some graphics
		final int numLasers = 80 + world.rand.nextInt(50);
		
		final double centerX = (minX + maxX) / 2.0D;
		final double centerY = (minY + maxY) / 2.0D;
		final double centerZ = (minZ + maxZ) / 2.0D;
		final double radiusX = (maxX - minX) / 2.0D + 5.0D;
		final double radiusY = (maxY - minY) / 2.0D + 5.0D;
		final double radiusZ = (maxZ - minZ) / 2.0D + 5.0D;
		
		for (int i = 0; i < numLasers; i++) {
			Minecraft.getInstance().particles.addEffect(new EntityFXBeam(world,
				new Vector3(
					centerX + radiusX * world.rand.nextGaussian(),
					centerY + radiusY * world.rand.nextGaussian(),
					centerZ + radiusZ * world.rand.nextGaussian()),
				new Vector3(
					centerX + radiusX * world.rand.nextGaussian(),
					centerY + radiusY * world.rand.nextGaussian(),
					centerZ + radiusZ * world.rand.nextGaussian()),
				world.rand.nextFloat(), world.rand.nextFloat(), world.rand.nextFloat(),
				60 + world.rand.nextInt(60)));
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s @ %s (%d %d %d) (%d %d %d) -> (%d %d %d)",
			getClass().getSimpleName(), dimensionId,
			blockPosCore.getX(), blockPosCore.getY(), blockPosCore.getZ(),
			minX, minY, minZ,
			maxX, maxY, maxZ);
	}
}

package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.network.PacketHandler;
import cr0s.warpdrive.render.EntityFXBeam;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CloakedArea {
	
	public int dimensionId = -666;
	public int coreX, coreY, coreZ;
	public int minX, minY, minZ;
	public int maxX, maxY, maxZ;
	private LinkedList<String> playersInArea;
	public byte tier = 0;
	public Block fogBlock;
	public int fogMetadata;
	
	public CloakedArea(final World worldObj,
			final int dimensionId, final int x, final int y, final int z, final byte tier,
			final int minX, final int minY, final int minZ,
			final int maxX, final int maxY, final int maxZ) {
		this.dimensionId = dimensionId;
		this.coreX = x;
		this.coreY = y;
		this.coreZ = z;
		this.tier = tier;
		
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		
		this.playersInArea = new LinkedList<>();
		
		if (worldObj != null) {
			try {
				// Add all players currently inside the field
				final List<EntityPlayer> list = worldObj.getEntitiesWithinAABB(EntityPlayerMP.class, AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
				for (final EntityPlayer player : list) {
					addPlayer(player.getCommandSenderName());
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
		
		if (tier == 1) {
			fogBlock = WarpDrive.blockGas;
			fogMetadata = 5;
		} else {
			fogBlock = Blocks.air;
			fogMetadata = 0;
		}
	}
	
	public boolean isPlayerListedInArea(final String username) {
		for (final String playerInArea : playersInArea) {
			if (playerInArea.equals(username)) {
				return true;
			}
		}
		
		return false;
	}
	
	private void removePlayer(final String username) {
		for (int i = 0; i < playersInArea.size(); i++) {
			if (playersInArea.get(i).equals(username)) {
				playersInArea.remove(i);
				return;
			}
		}
	}
	
	private void addPlayer(final String username) {
		if (!isPlayerListedInArea(username)) {
			playersInArea.add(username);
		}
	}
	
	public boolean isEntityWithinArea(final EntityLivingBase entity) {
		return (minX <= entity.posX && (maxX + 1) > entity.posX
			 && minY <= (entity.posY + entity.height) && (maxY + 1) > entity.posY
			 && minZ <= entity.posZ && (maxZ + 1) > entity.posZ);
	}
	
	public boolean isBlockWithinArea(final int x, final int y, final int z) {
		return (minX <= x && (maxX + 1) > x
			 && minY <= y && (maxY + 1) > y
			 && minZ <= z && (maxZ + 1) > z);
	}
	
	// Sending only if field changes: sets up or collapsing
	public void sendCloakPacketToPlayersEx(final boolean decloak) {
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info("sendCloakPacketToPlayersEx " + decloak);
		}
		final int RADIUS = 250;
		
		final double midX = minX + (Math.abs(maxX - minX) / 2.0D);
		final double midY = minY + (Math.abs(maxY - minY) / 2.0D);
		final double midZ = minZ + (Math.abs(maxZ - minZ) / 2.0D);
		
		for (int j = 0; j < MinecraftServer.getServer().getConfigurationManager().playerEntityList.size(); j++) {
			final EntityPlayerMP entityPlayerMP = (EntityPlayerMP) MinecraftServer.getServer().getConfigurationManager().playerEntityList.get(j);
			
			if (entityPlayerMP.dimension == dimensionId) {
				final double dX = midX - entityPlayerMP.posX;
				final double dY = midY - entityPlayerMP.posY;
				final double dZ = midZ - entityPlayerMP.posZ;
				
				if (Math.abs(dX) < RADIUS && Math.abs(dY) < RADIUS && Math.abs(dZ) < RADIUS) {
					if (decloak) {
						PacketHandler.sendCloakPacket(entityPlayerMP, this, true);
						revealChunksToPlayer(entityPlayerMP);
						revealEntitiesToPlayer(entityPlayerMP);
					} else if (!isEntityWithinArea(entityPlayerMP)) {
						PacketHandler.sendCloakPacket(entityPlayerMP, this, false);
					}
				}
			}
		}
	}
	
	public void updatePlayer(final EntityPlayerMP EntityPlayerMP) {
		if (isEntityWithinArea(EntityPlayerMP)) {
			if (!isPlayerListedInArea(EntityPlayerMP.getCommandSenderName())) {
				if (WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info(this + " Player " + EntityPlayerMP.getCommandSenderName() + " has entered");
				}
				addPlayer(EntityPlayerMP.getCommandSenderName());
				revealChunksToPlayer(EntityPlayerMP);
				revealEntitiesToPlayer(EntityPlayerMP);
				PacketHandler.sendCloakPacket(EntityPlayerMP, this, false);
			}
		} else {
			if (isPlayerListedInArea(EntityPlayerMP.getCommandSenderName())) {
				if (WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info(this + " Player " + EntityPlayerMP.getCommandSenderName() + " has left");
				}
				removePlayer(EntityPlayerMP.getCommandSenderName());
				MinecraftServer
						.getServer()
						.getConfigurationManager()
						.sendToAllNearExcept(EntityPlayerMP, EntityPlayerMP.posX, EntityPlayerMP.posY, EntityPlayerMP.posZ, 100, EntityPlayerMP.worldObj.provider.dimensionId,
								PacketHandler.getPacketForThisEntity(EntityPlayerMP));
				PacketHandler.sendCloakPacket(EntityPlayerMP, this, false);
			}
		}
	}
	
	public void revealChunksToPlayer(final EntityPlayer player) {
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			 WarpDrive.logger.info(this + " Revealing cloaked blocks to player " + player.getCommandSenderName());
		}
		final int minY_clamped = Math.max(0, minY);
		final int maxY_clamped = Math.min(255, maxY);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY_clamped; y <= maxY_clamped; y++) {
					if (player.worldObj.getBlock(x, y, z) != Blocks.air) {
						player.worldObj.markBlockForUpdate(x, y, z);
						
						JumpBlock.refreshBlockStateOnClient(player.worldObj, x, y, z);
					}
				}
			}
		}
		
		/*
		final ArrayList<Chunk> chunksToSend = new ArrayList<Chunk>();
		
		for (int x = minX >> 4; x <= maxX >> 4; x++) {
			for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
				chunksToSend.add(p.worldObj.getChunkFromChunkCoords(x, z));
			}
		}
		
		//System.out.println("[Cloak] Sending " + chunksToSend.size() + " chunks to player " + p.username);
		((EntityPlayerMP) p).playerNetServerHandler.sendPacketToPlayer(new Packet56MapChunks(chunksToSend));
		
		//System.out.println("[Cloak] Sending decloak packet to player " + p.username);
		area.sendCloakPacketToPlayer(p, true);
		// decloak = true
		
		/**/
	}
	
	public void revealEntitiesToPlayer(final EntityPlayer player) {
		final List<Entity> list = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
		
		for (final Entity entity : list) {
			final Packet packet = PacketHandler.getPacketForThisEntity(entity);
			if (packet != null) {
				if (WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.warn("Revealing entity " + entity + " with packet " + packet);
				}
				((EntityPlayerMP) player).playerNetServerHandler.sendPacket(packet);
			} else if (WarpDriveConfig.LOGGING_CLOAKING) {
				WarpDrive.logger.warn("Revealing entity " + entity + " fails: null packet");
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void clientCloak() {
		final EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		
		// Hide the blocks within area
		if (WarpDriveConfig.LOGGING_CLOAKING) { WarpDrive.logger.info("Refreshing cloaked blocks..."); }
		final World world = player.getEntityWorld();
		int minY_clamped = Math.max(0, minY);
		int maxY_clamped = Math.min(255, maxY);
		for (int y = minY_clamped; y <= maxY_clamped; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = world.getBlock(x, y, z);
					if (!block.isAssociatedBlock(Blocks.air)) {
						world.setBlock(x, y, z, fogBlock, fogMetadata, 4);
					}
				}
			}
		}
		
		// Hide any entities inside area
		if (WarpDriveConfig.LOGGING_CLOAKING) { WarpDrive.logger.info("Refreshing cloaked entities..."); }
		final AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(player, aabb);
		for (final Entity entity : list) {
			world.removeEntity(entity);
			((WorldClient) world).removeEntityFromWorld(entity.getEntityId());
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void clientDecloak() {
		final World world = Minecraft.getMinecraft().theWorld;
		world.markBlockRangeForRenderUpdate(minX - 1, Math.max(0, minY - 1), minZ - 1, maxX + 1, Math.min(255, maxY + 1), maxZ + 1);

		// Make some graphics
		int numLasers = 80 + world.rand.nextInt(50);
		
		final double centerX = (minX + maxX) / 2.0D;
		final double centerY = (minY + maxY) / 2.0D;
		final double centerZ = (minZ + maxZ) / 2.0D;
		final double radiusX = (maxX - minX) / 2.0D + 5.0D;
		final double radiusY = (maxY - minY) / 2.0D + 5.0D;
		final double radiusZ = (maxZ - minZ) / 2.0D + 5.0D;
		
		for (int i = 0; i < numLasers; i++) {
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(new EntityFXBeam(world,
				new Vector3(
					centerX + radiusX * world.rand.nextGaussian(),
					centerY + radiusY * world.rand.nextGaussian(),
					centerZ + radiusZ * world.rand.nextGaussian()),
				new Vector3(
					centerX + radiusX * world.rand.nextGaussian(),
					centerY + radiusY * world.rand.nextGaussian(),
					centerZ + radiusZ * world.rand.nextGaussian()),
				world.rand.nextFloat(), world.rand.nextFloat(), world.rand.nextFloat(),
				60 + world.rand.nextInt(60), 100));
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s @ DIM%d (%d %d %d) (%d %d %d) -> (%d %d %d)",
			getClass().getSimpleName(), dimensionId,
			coreX, coreY, coreZ,
			minX, minY, minZ,
			maxX, maxY, maxZ);
	}
}

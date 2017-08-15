package cr0s.warpdrive.event;

import cr0s.warpdrive.BreathingManager;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.network.PacketHandler;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

/**
*
* @author LemADEC
*/
public class WorldHandler {
	
	//TODO: register as event receiver
	public void onChunkLoaded(ChunkWatchEvent event) {
		ChunkCoordIntPair chunk = event.chunk;
		
		// Check chunk for locating in cloaked areas
		WarpDrive.logger.info("onChunkLoaded " + chunk.chunkXPos + " " + chunk.chunkZPos);
		WarpDrive.cloaks.onChunkLoaded(event.player, chunk.chunkXPos, chunk.chunkZPos);
		
		/*
		List<Chunk> list = new ArrayList<Chunk>();
		list.add(c);
		
		// Send obscured chunk
		System.out.println("[Cloak] Sending to player " + p.username + " obscured chunk at (" + chunk.chunkXPos + "; " + chunk.chunkZPos + ")");
		((EntityPlayerMP)p).playerNetServerHandler.sendPacketToPlayer(new Packet56MapChunks(list));
		*/
	}
	
	// Server side
	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event){
		if (event.world.isRemote) {
			return;
		}			
		// WarpDrive.logger.info("onEntityJoinWorld " + event.entity);
		if (event.entity instanceof EntityLivingBase) {
			final EntityLivingBase entityLivingBase = (EntityLivingBase) event.entity;
			final int x = MathHelper.floor_double(event.entity.posX);
			final int y = MathHelper.floor_double(event.entity.posY);
			final int z = MathHelper.floor_double(event.entity.posZ);
			final CelestialObject celestialObject = CelestialObjectManager.get(event.world, x, z);
			
			if (event.entity instanceof EntityPlayerMP) {
				WarpDrive.cloaks.onPlayerJoinWorld((EntityPlayerMP) event.entity, event.world);
				PacketHandler.sendClientSync((EntityPlayerMP) event.entity, celestialObject);
				
			} else {
				if (celestialObject == null) {
					// unregistered dimension => exit
					return;
				}
				if (event.entity.ticksExisted > 5) {
					// just changing dimension
					return;
				}
				if (!celestialObject.hasAtmosphere()) {
					final boolean canJoin = BreathingManager.onLivingJoinEvent(entityLivingBase, x, y, z);
					if (!canJoin) {
						event.setCanceled(true);
					}
				}
				if (!celestialObject.isInsideBorder(event.entity.posX, event.entity.posZ)) {
					event.setCanceled(true);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		WarpDrive.logger.info(String.format("onPlayerChangedDimension %s %d -> %d",
		                                    event.player.getCommandSenderName(), event.fromDim, event.toDim ));
		WarpDrive.cloaks.onPlayerJoinWorld((EntityPlayerMP) event.player, ((EntityPlayerMP) event.player).worldObj);
	}
	
	// Client side
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientConnectedToServer(ClientConnectedToServerEvent event) {
		// WarpDrive.logger.info("onClientConnectedToServer connectionType " + event.connectionType + " isLocal " + event.isLocal);
		WarpDrive.cloaks.onClientChangingDimension();
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onWorldUnload(WorldEvent.Unload event) {
		// WarpDrive.logger.info("onWorldUnload world " + event.world);
		WarpDrive.cloaks.onClientChangingDimension();
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		if (event.side != Side.SERVER || event.phase != Phase.END) {
			return;
		}
		
		AbstractSequencer.updateTick();
	}
	
	@SubscribeEvent
	public void onBlockUpdated(BlockEvent blockEvent) {
		if (WarpDriveConfig.LOGGING_BREAK_PLACE && WarpDrive.isDev) {
			WarpDrive.logger.info("onBlockUpdate args " + blockEvent.block + "@" + blockEvent.blockMetadata
			                      + " actual " + blockEvent.world.getBlock(blockEvent.x, blockEvent.y, blockEvent.z)
			                      + "@" + blockEvent.world.getBlockMetadata(blockEvent.x, blockEvent.y, blockEvent.z));
		}
		WarpDrive.starMap.onBlockUpdated(blockEvent.world, blockEvent.x, blockEvent.y, blockEvent.z, blockEvent.block, blockEvent.blockMetadata);
		ChunkHandler.onBlockUpdated(blockEvent.world, blockEvent.x, blockEvent.y, blockEvent.z);
	}
}

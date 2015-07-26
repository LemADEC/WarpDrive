package cr0s.warpdrive;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.event.world.ChunkWatchEvent;

public class CloakChunkWatcher {
	
	//TODO: register as event reciever
	public void chunkLoaded(ChunkWatchEvent event) {
		ChunkCoordIntPair chunk = event.chunk;
		
		// Check chunk for locating in cloaked areas
		WarpDrive.cloaks.checkChunkLoaded(event.player, chunk.chunkXPos, chunk.chunkZPos);
		
		/*List<Chunk> list = new ArrayList<Chunk>();
		list.add(c);
		
		// Send obscured chunk
		System.out.println("[Cloak] Sending to player " + p.username + " obscured chunk at (" + chunk.chunkXPos + "; " + chunk.chunkZPos + ")");
		((EntityPlayerMP)p).playerNetServerHandler.sendPacketToPlayer(new Packet56MapChunks(list));*/
	}
}

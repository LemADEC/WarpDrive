package cr0s.warpdrive.machines;

import java.util.ArrayList;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import com.google.common.collect.ImmutableSet;

import cr0s.warpdrive.WarpDrive;

public abstract class WarpChunkTE extends WarpEnergyTE
{
	private ArrayList<Ticket> ticketList = new ArrayList<Ticket>();
	
	public abstract boolean shouldChunkLoad();
	protected ChunkCoordIntPair minChunk = null;
	protected ChunkCoordIntPair maxChunk = null;
	
	protected boolean areChunksLoaded = false;

	// OVERRIDES
	@Override
	public void updateEntity() {
		if (shouldChunkLoad() != areChunksLoaded) {
			refreshLoading();
		}
		
		if (shouldChunkLoad()) {
			handleLoadedTick();
		}
	}
	
	public void handleLoadedTick() {

	}
	
	public synchronized void refreshLoading(boolean force) {
		boolean loadRequested = shouldChunkLoad();
		if (ticketList.size() != 0) {
			if (loadRequested && (!areChunksLoaded || force)) {
				int ticketSize = ticketList.get(0).getMaxChunkListDepth();
				ArrayList<ChunkCoordIntPair> chunkList = getChunksToLoad();
				int numTicketsRequired = (int) Math.ceil((double) chunkList.size() / ticketSize); // FIXME there should be only one ticket per requesting TileEntity
				if (ticketList.size() != numTicketsRequired) {
					for(int i = ticketList.size(); i < numTicketsRequired; i++) { 
						WarpDrive.instance.getTicket(this);
					}
				}
				
				int tickNum = 0;
				int chunkInTicket = 0;
				
				Ticket t = ticketList.get(0);
				for(ChunkCoordIntPair chunk:chunkList) {
					if (chunkInTicket >= ticketSize) {
						chunkInTicket = 0;
						tickNum++;
						t = ticketList.get(tickNum);
					}
					
					WarpDrive.debugPrint("Attempting to force chunk" + chunk);
					ForgeChunkManager.forceChunk(t, chunk);
					chunkInTicket++;
				}
				areChunksLoaded = true;
			} else if(!loadRequested) {
				for(Ticket ticket:ticketList) {
					ImmutableSet<ChunkCoordIntPair> chunks = ticket.getChunkList();
					for(ChunkCoordIntPair chunk:chunks) {
						ForgeChunkManager.unforceChunk(ticket, chunk);
					}
					
					ForgeChunkManager.releaseTicket(ticket);
				}
				ticketList.clear();
				areChunksLoaded = false;
			}
		} else if(loadRequested) {
			WarpDrive.instance.registerChunkLoadTE(this);
		}
	}
	
	public void refreshLoading() {
		refreshLoading(false);
	}
	
	public void giveTicket(Ticket t) {
		NBTTagCompound nbt = t.getModData();
		nbt.setInteger("ticketWorldObj", worldObj.provider.dimensionId);
		nbt.setInteger("ticketX", xCoord);
		nbt.setInteger("ticketY", yCoord);
		nbt.setInteger("ticketZ", zCoord);
		ticketList.add(t);
	}
	
	private static int dX(int dir)
	{
		if (dir == 1)
			return 1;
		else if (dir == 3)
			return -1;
		return 0;
	}
	
	private static int dZ(int dir)
	{
		if (dir == 0)
			return 1;
		else if (dir == 2)
			return -1;
		return 0;
	}
	
	public ArrayList<ChunkCoordIntPair> getChunksFromCentre(ChunkCoordIntPair chunkA,ChunkCoordIntPair chunkB)
	{
		if(!shouldChunkLoad())
			return null;
		int minX = Math.min(chunkA.chunkXPos, chunkB.chunkXPos);
		int maxX = Math.max(chunkA.chunkXPos, chunkB.chunkXPos);
		int minZ = Math.min(chunkA.chunkZPos, chunkB.chunkZPos);
		int maxZ = Math.max(chunkA.chunkZPos, chunkB.chunkZPos);
		WarpDrive.debugPrint("From " + minX + "," + minZ + " to " + maxX + "," + maxZ);
		
		//REMOVE ODD SIZES
		int deltaX = (maxX - minX + 1);
		int deltaZ = (maxZ - minZ + 1);
		WarpDrive.debugPrint("Allocating Block: " + deltaX + "," + deltaZ);
		
		maxX = minX + deltaX - 1;
		maxZ = minZ + deltaZ - 1;
		WarpDrive.debugPrint("From " + minX + "," + minZ + " to " + maxX + "," + maxZ);
		
		int maxEnts = (deltaX) * (deltaZ);
		ArrayList<ChunkCoordIntPair> chunkList = new ArrayList<ChunkCoordIntPair>(maxEnts);
		
		int dir = 1;
		int x = minX;
		int z = maxZ;
		for(int i=0;i<maxEnts;i++)
		{
			chunkList.add(new ChunkCoordIntPair(x,z));
			int dX = dX(dir);
			int dZ = dZ(dir);
			if(x+dX > maxX || x+dX < minX || z+dZ > maxZ || z+dZ < minZ)
			{
				dir++;
				if(dir >= 4)
					dir = 0;
				dX = dX(dir);
				dZ = dZ(dir);
				
				if(dX == 1)
					minX++;
				if(dX == -1)
					maxX--;
				if(dZ == 1)
					minZ++;
				if(dZ == -1)
					maxZ--;
				
			}
			x += dX;
			z += dZ;
		}
		
		return chunkList;
	}
	@Override
	public void writeToNBT(NBTTagCompound t)
	{
		super.writeToNBT(t);
		if(minChunk == null)
			minChunk = worldObj.getChunkFromBlockCoords(xCoord, zCoord).getChunkCoordIntPair();
		
		if(maxChunk == null)
			maxChunk = worldObj.getChunkFromBlockCoords(xCoord, zCoord).getChunkCoordIntPair();
		t.setInteger("minChunkX", minChunk.chunkXPos);
		t.setInteger("minChunkZ", minChunk.chunkZPos);
		t.setInteger("maxChunkX", maxChunk.chunkXPos);
		t.setInteger("maxChunkZ", maxChunk.chunkZPos);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound t)
	{
		super.readFromNBT(t);
		if(t.hasKey("minChunkX"))
		{
			int mx = t.getInteger("minChunkX");
			int mz = t.getInteger("minChunkZ");
			minChunk = new ChunkCoordIntPair(mx,mz);
			mx = t.getInteger("maxChunkX");
			mz = t.getInteger("maxChunkZ");
			maxChunk = new ChunkCoordIntPair(mx,mz);
		}
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
		for(Ticket t : ticketList) {
			ForgeChunkManager.releaseTicket(t);
		}
	}
	
	public ArrayList<ChunkCoordIntPair> getChunksToLoad()
	{
		if(minChunk == null || maxChunk == null)
		{
			ArrayList<ChunkCoordIntPair> chunkList = new ArrayList<ChunkCoordIntPair>(1);
			chunkList.add(this.worldObj.getChunkFromBlockCoords(xCoord, zCoord).getChunkCoordIntPair());
			return chunkList;
		}
		return getChunksFromCentre(minChunk,maxChunk);
	}
}

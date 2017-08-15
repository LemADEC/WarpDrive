package cr0s.warpdrive.world;

import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.structures.Orb.OrbShell;
import cr0s.warpdrive.config.structures.OrbInstance;
import cr0s.warpdrive.data.JumpBlock;

import java.util.ArrayList;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/*
 2014-06-07 21:41:45 [Infos] [STDOUT] Generating star (class 0) at -579 257 1162
 2014-06-07 21:41:45 [Infos] [Minecraft-Client] [CHAT] /generate: generating star at -579, 257, 1162
 2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saved 310248 blocks
 2014-06-07 21:41:45 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 95.646ms, total: 95.646ms
 2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saved 23706 blocks
 2014-06-07 21:41:45 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 15.427ms, total: 15.427ms

 2014-06-07 21:42:03 [Infos] [STDOUT] Generating star (class 1) at -554 257 1045
 2014-06-07 21:42:03 [Infos] [Minecraft-Client] [CHAT] /generate: generating star at -554, 257, 1045
 2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saved 1099136 blocks
 2014-06-07 21:42:03 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 37.404ms, total: 37.404ms
 2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saved 50646 blocks
 2014-06-07 21:42:03 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 34.369ms, total: 34.369ms

 2014-06-07 21:42:39 [Infos] [STDOUT] Generating star (class 2) at -404 257 978
 2014-06-07 21:42:39 [Infos] [Minecraft-Client] [CHAT] /generate: generating star at -404, 257, 978
 2014-06-07 21:42:39 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:42:39 [Infos] [STDOUT] [ESG] Saved 2144432 blocks
 2014-06-07 21:42:39 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 85.523ms, total: 85.523ms
 2014-06-07 21:42:39 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:42:40 [Infos] [STDOUT] [ESG] Saved 76699 blocks
 2014-06-07 21:42:40 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 9.286ms, total: 9.286ms

 */
public final class EntitySphereGen extends Entity {
	public int xCoord;
	public int yCoord;
	public int zCoord;
	
	private int radius;
	private int gasColor;
	
	private static final int BLOCKS_PER_TICK = 5000;
	
	private static final int STATE_SAVING = 0;
	private static final int STATE_SETUP = 1;
	private static final int STATE_DELETE = 2;
	private static final int STATE_STOP = 3;
	private int state = STATE_DELETE;
	private int ticksDelay = 0;
	
	private int currentIndex = 0;
	private int pregenSize = 0;
	
	private ArrayList<JumpBlock> blocks;
	private ArrayList<Boolean> isSurfaces;
	private OrbInstance orbInstance;
	private boolean replace;
	
	public EntitySphereGen(World world) {
		super(world);
	}
	
	public EntitySphereGen(World world, int x, int y, int z, OrbInstance orbInstance, boolean replace) {
		super(world);
		this.xCoord = x;
		this.posX = x;
		this.yCoord = y;
		this.posY = y;
		this.zCoord = z;
		this.posZ = z;
		this.orbInstance = orbInstance;
		this.gasColor = worldObj.rand.nextInt(12);
		this.radius = orbInstance.getTotalThickness();
		
		this.state = STATE_SAVING;
		this.pregenSize = (int) Math.ceil(Math.PI * 4.0F / 3.0F * Math.pow(radius + 1, 3));
		blocks = new ArrayList<>(this.pregenSize);
		isSurfaces = new ArrayList<>(this.pregenSize);
		this.ticksDelay = world.rand.nextInt(60);
		this.replace = replace;
	}
	
	public void killEntity() {
		this.state = STATE_STOP;
		int minY_clamped = Math.max(0, yCoord - radius);
		int maxY_clamped = Math.min(255, yCoord + radius);
		for (int x = xCoord - radius; x <= xCoord + radius; x++) {
			for (int z = zCoord - radius; z <= zCoord + radius; z++) {
				for (int y = minY_clamped; y <= maxY_clamped; y++) {
					if (worldObj.getBlock(x, y, z) != Blocks.air) {
						worldObj.markBlockForUpdate(x, y, z);
					}
				}
			}
		}
		worldObj.removeEntity(this);
	}
	
	@Override
	public void onUpdate() {
		if (worldObj.isRemote) {
			return;
		}
		 
		if (ticksDelay > 0) {
			ticksDelay--;
			return;
		}
		
		switch (state) {
		case STATE_SAVING:
			tickScheduleBlocks();
			this.state = STATE_SETUP;
			break;
		
		case STATE_SETUP:
			if (currentIndex >= blocks.size() - 1)
				this.state = STATE_DELETE;
			else
				tickPlaceBlocks();
			break;
		
		case STATE_DELETE:
			currentIndex = 0;
			killEntity();
			break;
		
		default:
			WarpDrive.logger.error(this + " Invalid state " + state + ". Killing entity...");
			killEntity();
			break;
		}
	}
	
	private void tickPlaceBlocks() {
		int blocksToMove = Math.min(BLOCKS_PER_TICK, blocks.size() - currentIndex);
		LocalProfiler.start("[EntitySphereGen] Placing blocks from " + currentIndex + " to " + (currentIndex + blocksToMove) + "/" + blocks.size());
		int notifyFlag;
		
		for (int index = 0; index < blocksToMove; index++) {
			if (currentIndex >= blocks.size())
				break;
			notifyFlag = (currentIndex % 1024 == 0 ? 2 : 2);
			final JumpBlock jumpBlock = blocks.get(currentIndex);
			if (isSurfaces.get(currentIndex)) {
				worldObj.setBlock(jumpBlock.x, jumpBlock.y, jumpBlock.z, jumpBlock.block, jumpBlock.blockMeta, notifyFlag);
			} else {
				JumpBlock.setBlockNoLight(worldObj, jumpBlock.x, jumpBlock.y, jumpBlock.z, jumpBlock.block, jumpBlock.blockMeta, notifyFlag);
			}
			currentIndex++;
		}
		
		LocalProfiler.stop();
	}
	
	private void tickScheduleBlocks() {
		LocalProfiler.start("[EntitySphereGen] Saving blocks, radius " + radius);
		
		// square radius from center of block
		double sqRadiusHigh = (radius + 0.5D) * (radius + 0.5D);
		double sqRadiusLow = (radius - 0.5D) * (radius - 0.5D);
		
		// sphere
		int ceilRadius = radius + 1;
		
		// Pass the cube and check points for sphere equation x^2 + y^2 + z^2 = r^2
		for (int x = 0; x <= ceilRadius; x++) {
			double x2 = (x + 0.5D) * (x + 0.5D);
			for (int y = 0; y <= ceilRadius; y++) {
				double x2y2 = x2 + (y + 0.5D) * (y + 0.5D);
				for (int z = 0; z <= ceilRadius; z++) {
					double sqRange = x2y2 + (z + 0.5D) * (z + 0.5D); // Square distance from current position to center
					
					// Skip too far blocks
					if (sqRange > sqRadiusHigh) {
						continue;
					}
					final boolean isSurface = sqRange > sqRadiusLow;
					
					// Add blocks to memory
					OrbShell orbShell = orbInstance.getShellForSqRadius(sqRange);
					// WarpDrive.logger.info("sqRange " + sqRange + " sqRadius " + sqRadius);
					addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord + x, yCoord + y, zCoord + z));
					addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord - x, yCoord + y, zCoord + z));
					addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord + x, yCoord - y, zCoord + z));
					addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord + x, yCoord + y, zCoord - z));
					addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord - x, yCoord - y, zCoord + z));
					addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord + x, yCoord - y, zCoord - z));
					addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord - x, yCoord + y, zCoord - z));
					addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord - x, yCoord - y, zCoord - z));
				}
			}
		}
		if (blocks != null && blocks.size() > pregenSize) {
			WarpDrive.logger.warn("[EntitySphereGen] Saved " + blocks.size() + " blocks (estimated to " + pregenSize + ")");
		}
		LocalProfiler.stop();
	}
	
	private void addBlock(final boolean isSurface, final JumpBlock jumpBlock) {
		if (blocks == null) {
			return;
		}
		// Replace water with random gas (ship in moon)
		if (worldObj.getBlock(jumpBlock.x, jumpBlock.y, jumpBlock.z).isAssociatedBlock(Blocks.water)) {
			if (worldObj.rand.nextInt(50) != 1) {
				jumpBlock.block = WarpDrive.blockGas;
				jumpBlock.blockMeta = gasColor;
			}
			blocks.add(jumpBlock);
			isSurfaces.add(isSurface);
			return;
		}
		// Do not replace existing blocks if fillingSphere is true
		if (!replace && !worldObj.isAirBlock(jumpBlock.x, jumpBlock.y, jumpBlock.z)) {
			return;
		}
		blocks.add(jumpBlock);
		isSurfaces.add(isSurface);
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		// FIXME not implemented
	}
	
	@Override
	protected void entityInit() {
		noClip = true;
	}
	
	// override to skip the block bounding override on client side
	@Override
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int p_70056_9_) {
		//	super.setPositionAndRotation2(x, y, z, yaw, pitch, p_70056_9_);
		this.setPosition(x, y, z);
		this.setRotation(yaw, pitch);
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
		// FIXME not implemented
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return false;
	}
}
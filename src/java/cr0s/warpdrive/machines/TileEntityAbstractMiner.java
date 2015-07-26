package cr0s.warpdrive.machines;

import java.util.ArrayList;
import java.util.List;

import appeng.api.IAEItemStack;
import appeng.api.Util;
import appeng.api.WorldCoord;
import appeng.api.events.GridTileLoadEvent;
import appeng.api.events.GridTileUnloadEvent;
import appeng.api.me.tiles.IGridMachine;
import appeng.api.me.tiles.ITileCable;
import appeng.api.me.util.IGridInterface;
import appeng.api.me.util.IMEInventoryHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFluid;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.PacketHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;

public abstract class TileEntityAbstractMiner extends TileEntityAbstractLaser implements IGridMachine, ITileCable
{
	//FOR STORAGE
	private boolean silkTouch = false;
	private int fortuneLevel = 0;
	
	private TileEntityParticleBooster booster = null;
	private Vector3 minerVector;
	
	Boolean powerStatus = false;
	private IGridInterface grid;
	private boolean isMEReady = false;
	
	abstract boolean	canSilkTouch();
	abstract int		minFortune();
	abstract int		maxFortune();
	abstract double		laserBelow();
	
	abstract float		getColorR();
	abstract float		getColorG();
	abstract float		getColorB();
	
	public TileEntityAbstractMiner()
	{
		super();
		fixMinerVector();
	}
	
	private void fixMinerVector()
	{
		if(minerVector == null)
			minerVector = new Vector3(xCoord,yCoord-laserBelow(),zCoord);
		minerVector.x = xCoord;
		minerVector.y = yCoord - (laserBelow());
		minerVector.z = zCoord;
		minerVector.translate(0.5);
	}
	
	private List<ItemStack> getItemStackFromBlock(int i, int j, int k, int blockID, int blockMeta)
	{
		Block block = Block.blocksList[blockID];
		if (block == null)
			return null;
		if (silkTouch(blockID))
		{
			if (block.canSilkHarvest(worldObj, null, i, j, k, blockMeta))
			{
				ArrayList<ItemStack> t = new ArrayList<ItemStack>();
				t.add(new ItemStack(blockID, 1, blockMeta));
				return t;
			}
		}
		return block.getBlockDropped(worldObj, i, j, k, blockMeta, fortuneLevel);
	}
	
	protected boolean isOnEarth()
	{
		return worldObj.provider.dimensionId == 0;
	}
	
	private IInventory findChest() {
		TileEntity result = null;
		
		for(int i = 0; i < 6; i++) {
			Vector3 sideOffset = adjacentSideOffsets[i];
			result = worldObj.getBlockTileEntity(xCoord + sideOffset.intX(), yCoord + sideOffset.intY(), zCoord + sideOffset.intZ());
			if (result != null && !(result instanceof TileEntityAbstractMiner) && (result instanceof IInventory)) {
				return (IInventory) result;
			}
		}
		return null;
	}
	
	//GETTERSETTERS
	
	protected int fortune()
	{
		return fortuneLevel;
	}
	
	protected boolean silkTouch()
	{
		return silkTouch;
	}
	
	protected boolean silkTouch(int blockID)
	{
		return silkTouch();
	}
	
	protected boolean silkTouch(boolean b)
	{
		silkTouch = canSilkTouch() && b;
		return silkTouch();
	}
	
	protected boolean silkTouch(Object o)
	{
		return silkTouch(toBool(o));
	}
	
	protected int fortune(int f)
	{
		try
		{
			fortuneLevel = clamp(f,minFortune(),maxFortune());
		}
		catch(NumberFormatException e)
		{
			fortuneLevel = minFortune();
		}
		return fortune();
	}
	
	protected TileEntityParticleBooster booster()
	{
		if(booster == null)
			findFirstBooster();
		return booster;
	}
	
	protected int energy() {
		TileEntityParticleBooster te = booster();
		if (te != null) {
			return te.getEnergyStored();
		}
		return 0;
	}
	
	//DATA RET
	
	protected int calculateLayerCost()
	{
		return isOnEarth() ? WarpDriveConfig.ML_EU_PER_LAYER_EARTH : WarpDriveConfig.ML_EU_PER_LAYER_SPACE;
	}
	
	protected int calculateBlockCost()
	{
		return calculateBlockCost(0);
	}
	
	protected int calculateBlockCost(int blockID)
	{
		int enPerBlock = isOnEarth() ? WarpDriveConfig.ML_EU_PER_BLOCK_EARTH : WarpDriveConfig.ML_EU_PER_BLOCK_SPACE;
		if(silkTouch(blockID))
			return (int) Math.round(enPerBlock * WarpDriveConfig.ML_EU_MUL_SILKTOUCH);
		return (int) Math.round(enPerBlock * (Math.pow(WarpDriveConfig.ML_EU_MUL_FORTUNE, fortune())));
	}
	
	protected boolean isRoomForHarvest()
	{
		if(isMEReady && grid != null)
			return true;
		
		IInventory inv = findChest();
		if(inv != null)
		{
			int size = inv.getSizeInventory();
			for(int i=0;i<size;i++)
				if(inv.getStackInSlot(i) == null)
					return true;
		}
		return false;
	}

	private boolean canDig(int blockID, int x, int y, int z) {// not used
		// ignore air & fluids
		if (!WarpDriveConfig.isAirBlock(worldObj, blockID, x, y, z) && Block.blocksList[blockID] != null && !(Block.blocksList[blockID] instanceof BlockFluid)) {
			return false;
		}
		// check blacklist
		if (blockID == Block.bedrock) {
			return false;
		}
		if (WarpDriveConfig.forceFieldBlocks.contains(blockID)) {
//			isMining = false;
			return false;
		}
		// check whitelist
		// WarpDriveConfig.MinerOres.contains(blockID) then true ?
		else if (blockID == WarpDriveConfig.GT_Granite || blockID == WarpDriveConfig.GT_Ores || blockID == WarpDriveConfig.iridiumBlockID) {
			return true;
		}
		// check default
		else if ( (Block.blocksList[blockID] != null) && (Block.blocksList[blockID].blockResistance <= Block.obsidian.blockResistance) ) {
			return true;
		}
		return false;
	}
	
	//MINING FUNCTIONS
	
	protected void laserBlock(Vector3 valuable)
	{
		fixMinerVector();
		float r = getColorR();
		float g = getColorG();
		float b = getColorB();
		PacketHandler.sendBeamPacket(worldObj, minerVector, valuable.clone().translate(0.5D), r, g, b, 2 * WarpDriveConfig.ML_MINE_DELAY_TICKS, 0, 50);
		//worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
	}
	
	private void mineBlock(Vector3 valuable,int blockID, int blockMeta)
	{
		laserBlock(valuable);
		worldObj.playAuxSFXAtEntity(null, 2001, valuable.intX(), valuable.intY(), valuable.intZ(), blockID + (blockMeta << 12));
		worldObj.setBlockToAir(valuable.intX(), valuable.intY(), valuable.intZ());
	}
	
	protected boolean harvestBlock(Vector3 valuable)
	{
		int blockID = worldObj.getBlockId(valuable.intX(), valuable.intY(), valuable.intZ());
		int blockMeta = worldObj.getBlockMetadata(valuable.intX(), valuable.intY(), valuable.intZ());
		if (blockID != Block.waterMoving && blockID != Block.waterStill && blockID != Block.lavaMoving && blockID != Block.lavaStill)
		{
			boolean didPlace = true;
			List<ItemStack> stacks = getItemStackFromBlock(valuable.intX(), valuable.intY(), valuable.intZ(), blockID, blockMeta);
			if (stacks != null)
			{
				for (ItemStack stack : stacks)
				{
					didPlace = didPlace && dumpToInv(stack) == stack.stackSize;
				}
			}
			mineBlock(valuable,blockID,blockMeta);
			return didPlace;
		}
		else if (blockID == Block.waterMoving || blockID == Block.waterStill)
		// Evaporate water
			worldObj.playSoundEffect(valuable.intX() + 0.5D, valuable.intY() + 0.5D, valuable.intZ() + 0.5D, "random.fizz", 0.5F, 2.6F + (worldObj.rand.nextFloat() - worldObj.rand.nextFloat()) * 0.8F);
		worldObj.setBlockToAir(valuable.intX(), valuable.intY(), valuable.intZ());
		return true;
	}
	
	protected int dumpToInv(ItemStack item)
	{
		if (grid != null)
			return putInGrid(item);
		else
			return putInChest(findChest(), item);
	}
	
	private int putInGrid(ItemStack itemStackSource)
	{
		int transferred = 0;
		if(isMEReady && grid != null)
		{
			IMEInventoryHandler cellArray = grid.getCellArray();
			if (cellArray != null)
			{
				IAEItemStack ret = cellArray.addItems(Util.createItemStack(itemStackSource));
				if (ret != null)
					transferred = (int) ret.getStackSize();
			}
		}
		return transferred;
	}

	private static int putInChest(IInventory inventory, ItemStack itemStackSource)
	{
		if (inventory == null || itemStackSource == null)
		{
			return 0;
		}

		int transferred = 0;

		for (int i = 0; i < inventory.getSizeInventory(); i++)
		{
			if (!inventory.isItemValidForSlot(i, itemStackSource))
			{
				continue;
			}

			ItemStack itemStack = inventory.getStackInSlot(i);

			if (itemStack == null || !itemStack.isItemEqual(itemStackSource))
			{
				continue;
			}

			int transfer = Math.min(itemStackSource.stackSize - transferred, itemStack.getMaxStackSize() - itemStack.stackSize);
			itemStack.stackSize += transfer;
			transferred += transfer;

			if (transferred == itemStackSource.stackSize)
			{
				return transferred;
			}
		}

		for (int i = 0; i < inventory.getSizeInventory(); i++)
		{
			if (!inventory.isItemValidForSlot(i, itemStackSource))
			{
				continue;
			}

			ItemStack itemStack = inventory.getStackInSlot(i);

			if (itemStack != null)
			{
				continue;
			}

			int transfer = Math.min(itemStackSource.stackSize - transferred, itemStackSource.getMaxStackSize());
			ItemStack dest = copyWithSize(itemStackSource, transfer);
			inventory.setInventorySlotContents(i, dest);
			transferred += transfer;

			if (transferred == itemStackSource.stackSize)
			{
				return transferred;
			}
		}

		return transferred;
	}
	
	protected boolean consumeEnergyFromBooster(int requiredEnergy, boolean simulate)
	{
		TileEntityParticleBooster te = booster();
		if (te != null) {
			return te.consumeEnergy(requiredEnergy, simulate);
		}
		return false;
	}
	
	private TileEntityParticleBooster findFirstBooster()
	{
		TileEntity result;
		int[] xPos = {1,-1,0,0,0,0};
		int[] yPos = {0,0,-1,1,0,0};
		int[] zPos = {0,0,0,0,-1,1};
		
		for(int i=0;i<6;i++)
		{
			result = worldObj.getBlockTileEntity(xCoord + xPos[i], yCoord + yPos[i], zCoord + zPos[i]);
	
			if (result != null && result instanceof TileEntityParticleBooster)
			{
				booster = (TileEntityParticleBooster) result;
				return (TileEntityParticleBooster) result;
			}
		}
		booster = null;
		return null;
	}
	
	protected void defineMiningArea(int xSize,int zSize)
	{
		int xmax, zmax, x1, x2, z1, z2;
		int xmin, zmin;
		x1 = xCoord + xSize / 2;
		x2 = xCoord - xSize / 2;

		if (x1 < x2)
		{
			xmin = x1;
			xmax = x2;
		}
		else
		{
			xmin = x2;
			xmax = x1;
		}

		z1 = zCoord + zSize / 2;
		z2 = zCoord - zSize / 2;

		if (z1 < z2)
		{
			zmin = z1;
			zmax = z2;
		}
		else
		{
			zmin = z2;
			zmax = z1;
		}
		
		defineMiningArea(xmin,zmin,xmax,zmax);
	}
	
	protected void defineMiningArea(int minX, int minZ, int maxX, int maxZ)
	{
		if(worldObj == null)
			return;
		ChunkCoordIntPair a = worldObj.getChunkFromBlockCoords(minX, minZ).getChunkCoordIntPair();
		ChunkCoordIntPair b = worldObj.getChunkFromBlockCoords(maxX, maxZ).getChunkCoordIntPair();
		if(minChunk != null && a.equals(minChunk))
			if(maxChunk != null && b.equals(maxChunk))
				return;
		if(minChunk != null && b.equals(minChunk))
			if(maxChunk != null && a.equals(maxChunk))
				return;
		minChunk = a;
		maxChunk = b;
		refreshLoading(true);
	}
	
	private static ItemStack copyWithSize(ItemStack itemStack, int newSize)
	{
		ItemStack ret = itemStack.copy();
		ret.stackSize = newSize;
		return ret;
	}
	
	//NBT DATA
	@Override
	public void readFromNBT(NBTTagCompound tag)
	{
		super.readFromNBT(tag);
		silkTouch = tag.getBoolean("silkTouch");
		fortuneLevel = tag.getInteger("fortuneLevel");
		
		minerVector.x = xCoord;
		minerVector.y = yCoord - (laserBelow());
		minerVector.z = zCoord;
		minerVector = minerVector.translate(0.5);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag)
	{
		super.writeToNBT(tag);
		tag.setBoolean("silkTouch", silkTouch);
		tag.setInteger("fortuneLevel", fortuneLevel);
	}
	
	//AE INTERFACE
	@Override
	public void setNetworkReady( boolean isReady )
	{
		isMEReady = isReady;
	}
	
	@Override
	public boolean isMachineActive()
	{
		return isMEReady;
	}
	
	@Override
	public float getPowerDrainPerTick()
	{
		return 1;
	}

	@Override
	public void validate()
	{
		super.validate();
		MinecraftForge.EVENT_BUS.post(new GridTileLoadEvent(this, worldObj, getLocation()));
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		MinecraftForge.EVENT_BUS.post(new GridTileUnloadEvent(this, worldObj, getLocation()));
	}

	@Override
	public WorldCoord getLocation()
	{
		return new WorldCoord(xCoord, yCoord, zCoord);
	}

	@Override
	public boolean isValid()
	{
		return true;
	}

	@Override
	public void setPowerStatus(boolean hasPower)
	{
		powerStatus = hasPower;
	}

	@Override
	public boolean isPowered()
	{
		return powerStatus;
	}
	
	@Override
	public IGridInterface getGrid()
	{
		return grid;
	}

	@Override
	public void setGrid(IGridInterface gi)
	{
		grid = gi;
	}
	
	@Override
	public boolean coveredConnections()
	{
		return true;
	}

	@Override
	public World getWorld()
	{
		return worldObj;
	}
	
}

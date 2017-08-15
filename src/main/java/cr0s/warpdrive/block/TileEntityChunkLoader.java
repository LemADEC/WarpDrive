package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.UpgradeType;
import cr0s.warpdrive.item.ItemUpgrade;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;

import cpw.mods.fml.common.Optional;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityChunkLoader extends TileEntityAbstractChunkLoading {
	private boolean isActive = false;
	private boolean isEnabled = false;

	private boolean initialised = false;
	private ChunkCoordIntPair myChunk;

	int negDX, posDX, negDZ, posDZ;
	int area = 1;

	public TileEntityChunkLoader() {
		super();
		IC2_sinkTier = 2;
		IC2_sourceTier = 2;
		negDX = 0;
		negDZ = 0;
		posDX = 0;
		posDZ = 0;
		peripheralName = "warpdriveChunkloader";
		addMethods(new String[] {
				"enable",
				"bounds",
				"radius",				
				"upgrades",
				"getEnergyRequired"
		});
		
		setUpgradeMaxCount(ItemUpgrade.getItemStack(UpgradeType.Energy), 2);
		setUpgradeMaxCount(ItemUpgrade.getItemStack(UpgradeType.Power), 2);
	}
	
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.CL_MAX_ENERGY;
	}
	
	@Override
	public boolean energy_canInput(ForgeDirection from) {
		return true;
	}
	
	public long energy_getEnergyRequired() {
		return area * WarpDriveConfig.CL_RF_PER_CHUNKTICK;
	}
	
	@Override
	public boolean shouldChunkLoad()
	{
		return isEnabled && isActive;
	}
	
	@Override
	public void updateEntity()
	{
		super.updateEntity();

		if(!initialised)
		{
			initialised = true;
			myChunk = worldObj.getChunkFromBlockCoords(xCoord, zCoord).getChunkCoordIntPair();
			changedDistance();
		}

		if(isEnabled)
		{
			isActive = energy_consume(energy_getEnergyRequired(), false);
		}
		else
		{
			isActive = energy_consume(energy_getEnergyRequired(), true);
		}
	}
	
	private void setBounds(int negX, int posX, int negZ, int posZ) {
		negDX = - Commons.clamp(0, WarpDriveConfig.CL_MAX_DISTANCE, Math.abs(negX));
		posDX =   Commons.clamp(0, WarpDriveConfig.CL_MAX_DISTANCE, Math.abs(posX));
		negDZ = - Commons.clamp(0, WarpDriveConfig.CL_MAX_DISTANCE, Math.abs(negZ));
		posDZ =   Commons.clamp(0, WarpDriveConfig.CL_MAX_DISTANCE, Math.abs(posZ));
	}
		
	private void changedDistance()
	{
		if(worldObj == null) {
			return;
		}
		if (myChunk == null) {
			Chunk aChunk = worldObj.getChunkFromBlockCoords(xCoord, zCoord);
			if (aChunk != null) {
				myChunk = aChunk.getChunkCoordIntPair();
			} else {
				return;
			}
		}

		minChunk = new ChunkCoordIntPair(myChunk.chunkXPos + negDX, myChunk.chunkZPos + negDZ);
		maxChunk = new ChunkCoordIntPair(myChunk.chunkXPos + posDX, myChunk.chunkZPos + posDZ);
		area = (posDX - negDX + 1) * (posDZ - negDZ + 1);
		refreshLoading(true);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		setBounds(nbt.getInteger("negDX"), nbt.getInteger("posDX"), nbt.getInteger("negDZ"), nbt.getInteger("posDZ"));
		changedDistance();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setInteger("negDX", negDX);
		nbt.setInteger("negDZ", negDZ);
		nbt.setInteger("posDX", posDX);
		nbt.setInteger("posDZ", posDZ);
	}
	
	//Common LUA functions
	public Object[] enable(Object[] arguments) {
		if (arguments.length == 1)
			isEnabled = Commons.toBool(arguments[0]);
		return new Object[]{shouldChunkLoad()};
	}
	
	public Object[] bounds(Object[] arguments) {
		if (arguments.length == 4) {
			setBounds(Commons.toInt(arguments[0]), Commons.toInt(arguments[1]), Commons.toInt(arguments[2]), Commons.toInt(arguments[3]));
			changedDistance();
		}
		return new Object[]{negDX, posDX, negDZ, posDZ};
	}
	
	public Object[] radius(Object[] arguments) {
		if (arguments.length == 1) {
			int dist = Commons.toInt(arguments[0]);
			setBounds(dist,dist,dist,dist);
			changedDistance();
			return new Object[]{true};
		}
		return new Object[]{false};
	}
	
	public Object[] upgrades(Object[] arguments) {
		return new Object[] { getUpgradesAsString() };
	}
	
	public Object[] getEnergyRequired(Object[] arguments) {
		return new Object[] { energy_getEnergyRequired() };
	}

	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] radius(Context context, Arguments arguments) {
		return radius(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] bounds(Context context, Arguments arguments) {
		return bounds(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] enable(Context context, Arguments arguments) {
		return enable(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] upgrades(Context context, Arguments arguments) {
		return upgrades(argumentsOCtoCC(arguments));
	}
	
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] getEnergyRequired(Context context, Arguments arguments) {
		return getEnergyRequired(argumentsOCtoCC(arguments));
	}	
	
	//CC method
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		final String methodName = getMethodName(method);
		
		switch (methodName) {
			case "radius":
				return radius(arguments);
			case "bounds":
				return bounds(arguments);
			case "enable":
				return enable(arguments);
			case "upgrades":
				return upgrades(arguments);
			case "getEnergyRequired":
				return getEnergyRequired(arguments);
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
}

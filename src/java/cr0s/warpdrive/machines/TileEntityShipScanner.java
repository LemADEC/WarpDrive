package cr0s.warpdrive.machines;

import ic2.api.energy.tile.IEnergyTile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import cr0s.warpdrive.PacketHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;
import cr0s.warpdrive.data.JumpBlock;
import cr0s.warpdrive.data.Vector3;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;

public class TileEntityShipScanner extends WarpEnergyTE {
	private boolean isActive = false;
	private TileEntityReactor core = null;

	int laserTicks = 0;
	int scanTicks = 0;
	int deployDelayTicks = 0;

	int warpCoreSearchTicks = 0;

	private String schematicFileName;

	private JumpBlock[] blocksToDeploy; // JumpBlock class stores a basic
	// information about block
	private int currentDeployIndex;
	private int blocksToDeployCount;
	private boolean isDeploying = false;

	private int targetX, targetY, targetZ;

	public TileEntityShipScanner() {
		super();
		peripheralName = "shipscanner";
		methodsArray = new String[] { "scan", "fileName", "getEnergyLevel", "deploy", "state" };
	}

	@Override
	public void updateEntity() {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
			return;

		super.updateEntity();

		if (++warpCoreSearchTicks > 20) {
			core = searchWarpCore();
			warpCoreSearchTicks = 0;
		}

		// Warp core is not found
		if (!isDeploying && core == null) {
			setActive(false); // disable scanner
			return;
		}

		if (!isActive) {// inactive
			if (++laserTicks > 20) {
				PacketHandler.sendBeamPacket(worldObj, new Vector3(this).translate(0.5D), new Vector3(core.xCoord, core.yCoord, core.zCoord).translate(0.5D),
						0f, 1f, 0f, 40, 0, 100);
				laserTicks = 0;
			}
		} else if (!isDeploying) {// active and scanning
			if (++laserTicks > 5) {
				laserTicks = 0;

				for (int i = 0; i < core.maxX - core.minX; i++) {
					int x = core.minX + i;
					int randomZ = core.minZ + worldObj.rand.nextInt(core.maxZ - core.minZ);

					worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
					float r = 0.0f, g = 0.0f, b = 0.0f;

					switch (worldObj.rand.nextInt(6)) {
					case 0:
						r = 1.0f;
						g = b = 0;
						break;

					case 1:
						r = b = 0;
						g = 1.0f;
						break;

					case 2:
						r = g = 0;
						b = 1.0f;
						break;

					case 3:
						r = b = 0.5f;
						g = 0;
						break;

					case 4:
						r = g = 1.0f;
						b = 0;
						break;

					case 5:
						r = 1.0f;
						b = 0.5f;
						g = 0f;
					}

					PacketHandler.sendBeamPacket(worldObj, new Vector3(this).translate(0.5D), new Vector3(x, core.maxY, randomZ).translate(0.5D), r, g, b, 15,
							0, 100);
				}
			}

			if (++scanTicks > 20 * (1 + core.shipVolume / 10)) {
				setActive(false); // disable scanner
				scanTicks = 0;
			}
		} else {// active and deploying
			if (++deployDelayTicks < 20)
				return;

			deployDelayTicks = 0;

			int blocks = Math.min(WarpDriveConfig.G_BLOCKS_PER_TICK, blocksToDeployCount - currentDeployIndex);

			if (blocks == 0) {
				isDeploying = false;
				setActive(false); // disable scanner
				return;
			}

			for (int index = 0; index < blocks; index++) {
				if (currentDeployIndex >= blocksToDeployCount) {
					isDeploying = false;
					setActive(false); // disable scanner
					break;
				}

				// Deploy single block
				JumpBlock jb = blocksToDeploy[currentDeployIndex];

				if (jb != null && !jb.block.isAssociatedBlock(Blocks.bedrock) && !WarpDriveConfig.scannerIgnoreBlocks.contains(jb.block)
						&& worldObj.isAirBlock(targetX + jb.x, targetY + jb.y, targetZ + jb.z)) {
					jb.deploy(worldObj, targetX, targetY, targetZ);

					if (worldObj.rand.nextInt(100) <= 10) {
						worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);

						PacketHandler.sendBeamPacket(worldObj, new Vector3(this).translate(0.5D),
								new Vector3(targetX + jb.x, targetY + jb.y, targetZ + jb.z).translate(0.5D), 0f, 1f, 0f, 15, 0, 100);
					}
				}

				currentDeployIndex++;
			}
		}
	}

	private void setActive(boolean newState) {
		isActive = newState;
		if ((getBlockMetadata() == 1) == newState) {
			worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, isActive ? 1 : 0, 2);
		}
	}

	private TileEntityReactor searchWarpCore() {
		StringBuilder reason = new StringBuilder();
		TileEntityReactor result = null;

		// Search for warp cores above
		for (int newY = yCoord + 1; newY <= 255; newY++) {
			if (worldObj.getBlock(xCoord, newY, zCoord).isAssociatedBlock(WarpDrive.warpCore)) { // found
				// warp
				// core
				// above
				result = (TileEntityReactor) worldObj.getTileEntity(xCoord, newY, zCoord);

				if (result != null) {
					if (!result.validateShipSpatialParameters(reason)) { // If
						// we
						// can't
						// refresh
						// ship's
						// spatial
						// parameters
						result = null;
					}
				}

				break;
			}
		}

		return result;
	}

	private int getScanningEnergyCost(int size) {
		if (WarpDriveConfig.SS_EU_PER_BLOCK_SCAN > 0) {
			return size * WarpDriveConfig.SS_EU_PER_BLOCK_SCAN;
		} else {
			return WarpDriveConfig.SS_MAX_ENERGY_VALUE;
		}
	}

	private int getDeploymentEnergyCost(int size) {
		if (WarpDriveConfig.SS_EU_PER_BLOCK_DEPLOY > 0) {
			return size * WarpDriveConfig.SS_EU_PER_BLOCK_DEPLOY;
		} else {
			return WarpDriveConfig.SS_MAX_ENERGY_VALUE;
		}
	}

	private boolean saveShipToSchematic(String fileName, StringBuilder reason) {
		NBTTagCompound schematic = new NBTTagCompound();

		short width = (short) (core.maxX - core.minX + 1);
		short length = (short) (core.maxZ - core.minZ + 1);
		short height = (short) (core.maxY - core.minY + 1);

		if (width <= 0 || length <= 0 || height <= 0) {
			reason.append("Invalid ship dimensions, nothing to scan");
			return false;
		}

		schematic.setShort("Width", width);
		schematic.setShort("Length", length);
		schematic.setShort("Height", height);

		int size = width * length * height;

		// Consume energy
		if (!consumeEnergy(getScanningEnergyCost(size), false)) {
			reason.append("Insufficient energy (" + getScanningEnergyCost(size) + " required)");
			return false;
		}

		byte localBlocks[] = new byte[size];
		byte localMetadata[] = new byte[size];
		byte extraBlocks[] = new byte[size];
		byte extraBlocksNibble[] = new byte[(int) Math.ceil(size / 2.0)];
		boolean extra = false;

		NBTTagList tileEntitiesList = new NBTTagList();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				for (int z = 0; z < length; z++) {
					Block block = worldObj.getBlock(core.minX + x, core.minY + y, core.minZ + z);

					// Do not scan air, bedrock and specified forbidden blocks
					// (like ore or Warp-Cores)
					if (worldObj.isAirBlock(core.minX + x, core.minY + y, core.minZ + z) || block.isAssociatedBlock(Blocks.bedrock)
							|| WarpDriveConfig.scannerIgnoreBlocks.contains(block)) {
						block = Blocks.air;
					}

					localBlocks[x + (y * length + z) * width] = (byte) block.getUnlocalizedName();
					localMetadata[x + (y * length + z) * width] = (byte) worldObj.getBlockMetadata(core.minX + x, core.minY + y, core.minZ + z);
					if ((extraBlocks[x + (y * length + z) * width] = (byte) (block >> 8)) > 0) {
						extra = true;
					}

					if (!block.isAssociatedBlock(Blocks.air)) {
						TileEntity te = worldObj.getTileEntity(core.minX + x, core.minY + y, core.minZ + z);
						if (te != null) {
							try {
								NBTTagCompound tileTag = new NBTTagCompound();
								te.writeToNBT(tileTag);

								// Clear inventory.
								if (te instanceof IInventory) {
									TileEntity tmp_te = TileEntity.createAndLoadEntity(tileTag);
									if (tmp_te instanceof IInventory) {
										for (int i = 0; i < ((IInventory) tmp_te).getSizeInventory(); i++) {
											((IInventory) tmp_te).setInventorySlotContents(i, null);
										}
									}
									tmp_te.writeToNBT(tileTag);
								}

								// Remove energy from energy storages
								if (te instanceof IEnergyTile) {
									// IC2
									if (tileTag.hasKey("energy")) {
										tileTag.setInteger("energy", 0);
									}
									// Gregtech
									if (tileTag.hasKey("mStoredEnergy")) {
										tileTag.setInteger("mStoredEnergy", 0);
									}
								}

								// Transform TE's coordinates from local axis to
								// .schematic offset-axis
								tileTag.setInteger("x", te.xCoord - core.minX);
								tileTag.setInteger("y", te.yCoord - core.minY);
								tileTag.setInteger("z", te.zCoord - core.minZ);

								tileEntitiesList.appendTag(tileTag);
							} catch (Exception exception) {
								exception.printStackTrace();
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < extraBlocksNibble.length; i++) {
			if (i * 2 + 1 < extraBlocks.length) {
				extraBlocksNibble[i] = (byte) ((extraBlocks[i * 2 + 0] << 4) | extraBlocks[i * 2 + 1]);
			} else {
				extraBlocksNibble[i] = (byte) (extraBlocks[i * 2 + 0] << 4);
			}
		}

		schematic.setString("Materials", "Alpha");
		schematic.setByteArray("Blocks", localBlocks);
		schematic.setByteArray("Data", localMetadata);

		if (extra)
			schematic.setByteArray("AddBlocks", extraBlocksNibble);

		schematic.setTag("Entities", new NBTTagList()); // don't save entities
		schematic.setTag("TileEntities", tileEntitiesList);

		writeNBTToFile(fileName, schematic);

		return true;
	}

	private void writeNBTToFile(String fileName, NBTTagCompound nbttagcompound) {
		WarpDrive.print(this + " Filename: " + fileName);

		try {
			File file = new File(fileName);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileOutputStream fileoutputstream = new FileOutputStream(file);

			CompressedStreamTools.writeCompressed(nbttagcompound, fileoutputstream);

			fileoutputstream.close();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	// Begins ship scan
	private boolean scanShip(StringBuilder reason) {
		// Enable scanner
		setActive(true);
		File f = new File(WarpDriveConfig.G_SCHEMALOCATION);
		if (!f.exists() || !f.isDirectory()) {
			f.mkdirs();
		}

		// Generate unique file name
		do {
			schematicFileName = (new StringBuilder().append(core.coreFrequency).append(System.currentTimeMillis()).append(".schematic")).toString();
		} while (new File(WarpDriveConfig.G_SCHEMALOCATION + "/" + schematicFileName).exists());

		if (!saveShipToSchematic(WarpDriveConfig.G_SCHEMALOCATION + "/" + schematicFileName, reason)) {
			return false;
		}
		reason.append(schematicFileName);
		return true;
	}

	private static NBTTagCompound readNBTFromFile(String fileName) {
		try {
			File file = new File(fileName);
			if (!file.exists()) {
				return null;
			}

			FileInputStream fileinputstream = new FileInputStream(file);
			NBTTagCompound nbttagcompound = CompressedStreamTools.readCompressed(fileinputstream);

			fileinputstream.close();

			return nbttagcompound;
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		return null;
	}

	// Returns error code and reason string
	private int deployShip(String fileName, int offsetX, int offsetY, int offsetZ, StringBuilder reason) {
		// Load schematic
		NBTTagCompound schematic = readNBTFromFile(WarpDriveConfig.G_SCHEMALOCATION + "/" + fileName);
		if (schematic == null) {
			reason.append("Schematic not found or unknow error reading it.");
			return -1;
		}

		// Compute geometry
		short width = schematic.getShort("Width");
		short height = schematic.getShort("Height");
		short length = schematic.getShort("Length");

		targetX = xCoord + offsetX;
		targetY = yCoord + offsetY;
		targetZ = zCoord + offsetZ;
		blocksToDeployCount = width * height * length;

		// Validate context
		{
			// Check distance
			double dX = xCoord - targetX;
			double dY = yCoord - targetY;
			double dZ = zCoord - targetZ;
			double distance = MathHelper.sqrt_double(dX * dX + dY * dY + dZ * dZ);

			if (distance > WarpDriveConfig.SS_MAX_DEPLOY_RADIUS_BLOCKS) {
				reason.append("Cannot deploy ship so far away from scanner.");
				return 5;
			}

			// Consume energy
			if (!consumeEnergy(getDeploymentEnergyCost(blocksToDeployCount), false)) {
				reason.append("Insufficient energy (" + getDeploymentEnergyCost(blocksToDeployCount) + " required)");
				return 1;
			}

			// Check specified area for occupation by blocks
			// If specified area occupied, break deploying with error message
			int occupiedBlockCount = 0;
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					for (int z = 0; z < length; z++) {
						if (!worldObj.isAirBlock(targetX + x, targetY + y, targetZ + z))
							occupiedBlockCount++;
					}
				}
			}
			if (occupiedBlockCount > 0) {
				reason.append("Deploying area occupied with " + occupiedBlockCount + " blocks. Can't deploy ship.");
				return 2;
			}
		}

		// Set deployment variables
		blocksToDeploy = new JumpBlock[blocksToDeployCount];
		isDeploying = true;
		currentDeployIndex = 0;

		// Read blocks and TileEntities from NBT to internal storage array
		byte localBlocks[] = schematic.getByteArray("Blocks");
		byte localMetadata[] = schematic.getByteArray("Data");

		byte extraBlocks[] = null;
		byte extraBlocksNibble[] = null;
		if (schematic.hasKey("AddBlocks")) {
			extraBlocksNibble = schematic.getByteArray("AddBlocks");
			extraBlocks = new byte[extraBlocksNibble.length * 2];
			for (int i = 0; i < extraBlocksNibble.length; i++) {
				extraBlocks[i * 2 + 0] = (byte) ((extraBlocksNibble[i] >> 4) & 0xF);
				extraBlocks[i * 2 + 1] = (byte) (extraBlocksNibble[i] & 0xF);
			}
		} else if (schematic.hasKey("Add")) {
			extraBlocks = schematic.getByteArray("Add");
		}

		// Load Tile Entities
		NBTTagCompound[] tileEntities = new NBTTagCompound[blocksToDeployCount];
		NBTTagList tileEntitiesList = schematic.getTagList("TileEntities", 0); //TODO: 0 is not corect

		for (int i = 0; i < tileEntitiesList.tagCount(); i++) {
			NBTTagCompound teTag = tileEntitiesList.getCompoundTagAt(i);
			int teX = teTag.getInteger("x");
			int teY = teTag.getInteger("y");
			int teZ = teTag.getInteger("z");

			tileEntities[teX + (teY * length + teZ) * width] = teTag;
		}

		// Create list of blocks to deploy
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				for (int z = 0; z < length; z++) {
					int index = x + (y * length + z) * width;
					JumpBlock jb = new JumpBlock();

					jb.x = x;
					jb.y = y;
					jb.z = z;
					jb.block = (localBlocks[index]) & 0xFF;
					if (extraBlocks != null) {
						jb.block |= ((extraBlocks[index]) & 0xFF) << 8;
					}
					jb.blockMeta = (localMetadata[index]) & 0xFF;
					jb.blockNBT = tileEntities[index];

					if (jb.block != 0 && Block.blocksList[jb.block] != null) {
						if (tileEntities[index] == null) {
							WarpDrive.debugPrint("[ShipScanner] Adding block to deploy: " + Block.blocksList[jb.block.getUnlocalizedName() + " (no tile entity)");
						} else {
							WarpDrive.debugPrint("[ShipScanner] Adding block to deploy: " + Block.blocksList[jb.block].getUnlocalizedName() + " with tile entity " + tileEntities[index].getString("id"));
						}

						blocksToDeploy[index] = jb;
					} else {
						jb = null;

						blocksToDeploy[index] = jb;
					}
				}
			}
		}

		setActive(true);
		reason.append("Ship deploying...");
		return 3;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
	}

	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] scan(Context context, Arguments arguments) {
		return scan(argumentsOCtoCC(arguments));
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] filename(Context context, Arguments arguments) {
		return filename(argumentsOCtoCC(arguments));
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] deploy(Context context, Arguments arguments) {
		return deploy(argumentsOCtoCC(arguments));
	}

	private Object[] scan(Object[] arguments) {
		// Already scanning?
		if (isActive) {
			return new Object[] { false, 0, "Already active" };
		}

		if (core == null) {
			return new Object[] { false, 1, "Warp-Core not found" };
		} else if (!consumeEnergy(getScanningEnergyCost(core.shipVolume), true)) {
			return new Object[] { false, 2, "Not enough energy!" };
		} else {
			StringBuilder reason = new StringBuilder();
			boolean success = scanShip(reason);
			return new Object[] { success, 3, reason.toString() };
		}
	}

	private Object[] filename(Object[] arguments) {
		if (isActive && !schematicFileName.isEmpty()) {
			if (isDeploying) {
				return new Object[] { "Deployment in progress. Please wait..." };
			} else {
				return new Object[] { "Scan in progress. Please wait..." };
			}
		}

		return new Object[] { schematicFileName };
	}

	private Object[] deploy(Object[] arguments) {
		if (arguments.length == 4) {
			String fileName = (String) arguments[0];
			int x = toInt(arguments[1]);
			int y = toInt(arguments[2]);
			int z = toInt(arguments[3]);

			if (!new File(WarpDriveConfig.G_SCHEMALOCATION + "/" + fileName).exists()) {
				return new Object[] { 0, "Specified .schematic file was not found!" };
			} else {
				StringBuilder reason = new StringBuilder();
				int result = deployShip(fileName, x, y, z, reason);
				return new Object[] { result, reason.toString() };
			}
		} else {
			return new Object[] { 4, "Invalid arguments count, you need .schematic file name, offsetX, offsetY and offsetZ!" };
		}
	}

	private Object[] state(Object[] arguments) {
		if (!isActive) {
			return new Object[] { false, "IDLE", 0, 0 };
		} else if (!isDeploying) {
			return new Object[] { true, "Scanning", 0, 0 };
		} else {
			return new Object[] { true, "Deploying", currentDeployIndex, blocksToDeployCount };
		}
	}

	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		String methodName = methodsArray[method];
		if (methodName.equals("scan")) {
			return scan(arguments);

		} else if (methodName.equals("fileName")) {
			return filename(arguments);

		} else if (methodName.equals("getEnergyLevel")) {
			return getEnergyLevel();

		} else if (methodName.equals("deploy")) {// deploy(schematicFileName,
			// offsetX, offsetY,
			// offsetZ)
			return deploy(arguments);

		} else if (methodName.equals("state")) {
			return state(arguments);
		}

		return null;
	}

	// IEnergySink methods implementation
	@Override
	public int getMaxEnergyStored() {
		return WarpDriveConfig.SS_MAX_ENERGY_VALUE;
	}

	@Override
	public boolean canInputEnergy(ForgeDirection from) {
		return true;
	}

	@Override
	public int getSinkTier() {
		// TODO Arbitrarily chosen value
		return 3;
	}

	@Override
	public int getSourceTier() {
		// TODO Arbitrarily chosen value
		return 3;
	}
}

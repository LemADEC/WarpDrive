package cr0s.warpdrive.machines;

import gravisuite.redpower.WorldCoord;

import java.util.ArrayList;
import java.util.List;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import appeng.api.networking.IGridCache;
import appeng.api.networking.IGridNode;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import cr0s.warpdrive.PacketHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;

public class TileEntityMiningLaser extends WarpInterfacedTE implements IGridNode, ITileCable {
	private Boolean powerStatus = false;
	private IGridCache grid;

	private int dx, dz, dy;

	private boolean isMining() {
		return currentState != STATE_IDLE;
	}

	private boolean isQuarry = false;
	private boolean enableSilktouch = false;
	private boolean AENetworkReady = false;

	private int delayTicksWarmup = 0;
	private int delayTicksScan = 0;
	private int delayTicksMine = 0;
	private final int STATE_IDLE = 0;
	private final int STATE_WARMUP = 1;
	private final int STATE_SCANNING = 2;
	private final int STATE_MINING = 3;
	private int currentState = 0; // 0 - scan next layer, 1 - collect valuables
	private boolean enoughPower = false;
	private int currentLayer;

	private ArrayList<Vector3> valuablesInLayer = new ArrayList<Vector3>();
	private int valuableIndex = 0;

	private int layerOffset = 1;

	public TileEntityMiningLaser() {
		super();
		peripheralName = "mininglaser";
		methodsArray = new String[] { "mine", "stop", "isMining", "quarry", "state", "offset" };
	}

	@Override
	public void updateEntity() {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			return;
		}
		if (currentState == STATE_IDLE) {
			delayTicksWarmup = 0;
			delayTicksScan = 0;
			delayTicksMine = 0;
			updateMetadata(BlockMiningLaser.ICON_IDLE);
			return;
		}

		boolean isOnEarth = (worldObj.provider.dimensionId == 0);

		Vector3 minerVector = new Vector3(xCoord + 0.5D, yCoord, zCoord + 0.5D);

		if (currentState == STATE_WARMUP) { // warming up
			delayTicksWarmup++;
			updateMetadata(BlockMiningLaser.ICON_SCANNINGLOWPOWER);
			if (delayTicksWarmup >= WarpDriveConfig.ML_WARMUP_DELAY_TICKS) {
				delayTicksScan = 0;
				currentState = STATE_SCANNING;
				updateMetadata(BlockMiningLaser.ICON_SCANNINGLOWPOWER);
				return;
			}
		} else if (currentState == STATE_SCANNING) { // scanning
			delayTicksScan++;
			if (delayTicksScan == 1) {
				// check power level
				enoughPower = consumeEnergyPacketFromBooster(isOnEarth ? WarpDriveConfig.ML_EU_PER_LAYER_EARTH : WarpDriveConfig.ML_EU_PER_LAYER_SPACE, true);
				if (!enoughPower) {
					updateMetadata(BlockMiningLaser.ICON_SCANNINGLOWPOWER);
					delayTicksScan = 0;
					return;
				} else {
					updateMetadata(BlockMiningLaser.ICON_SCANNINGPOWERED);
				}
				// show current layer
				int age = Math.max(40, 5 * WarpDriveConfig.ML_SCAN_DELAY_TICKS);
				double xmax = xCoord + WarpDriveConfig.ML_MAX_RADIUS + 1.0D;
				double xmin = xCoord - WarpDriveConfig.ML_MAX_RADIUS + 0.0D;
				double zmax = zCoord + WarpDriveConfig.ML_MAX_RADIUS + 1.0D;
				double zmin = zCoord - WarpDriveConfig.ML_MAX_RADIUS + 0.0D;
				double y = currentLayer + 1.0D;
				PacketHandler.sendBeamPacket(worldObj, new Vector3(xmin, y, zmin), new Vector3(xmax, y, zmin), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(worldObj, new Vector3(xmax, y, zmin), new Vector3(xmax, y, zmax), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(worldObj, new Vector3(xmax, y, zmax), new Vector3(xmin, y, zmax), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(worldObj, new Vector3(xmin, y, zmax), new Vector3(xmin, y, zmin), 0.3F, 0.0F, 1.0F, age, 0, 50);
			} else if (delayTicksScan >= WarpDriveConfig.ML_SCAN_DELAY_TICKS) {
				delayTicksScan = 0;
				if (currentLayer <= 0) {
					stop();
					return;
				}
				// consume power
				enoughPower = consumeEnergyPacketFromBooster(isOnEarth ? WarpDriveConfig.ML_EU_PER_LAYER_EARTH : WarpDriveConfig.ML_EU_PER_LAYER_SPACE, false);
				if (!enoughPower) {
					updateMetadata(BlockMiningLaser.ICON_SCANNINGLOWPOWER);
					return;
				} else {
					updateMetadata(BlockMiningLaser.ICON_SCANNINGPOWERED);
				}
				// scan
				scanLayer();
				if (valuablesInLayer.size() > 0) {
					int r = (int) Math.ceil(WarpDriveConfig.ML_MAX_RADIUS / 2.0D);
					int offset = (yCoord - currentLayer) % (2 * r);
					int age = Math.max(20, Math.round(2.5F * WarpDriveConfig.ML_SCAN_DELAY_TICKS));
					double y = currentLayer + 1.0D;
					PacketHandler.sendBeamPacket(worldObj, minerVector, new Vector3(xCoord - r + offset, y, zCoord + r).translate(0.3D), 0.0F, 0.0F, 1.0F, age,
							0, 50);
					PacketHandler.sendBeamPacket(worldObj, minerVector, new Vector3(xCoord + r, y, zCoord + r - offset).translate(0.3D), 0.0F, 0.0F, 1.0F, age,
							0, 50);
					PacketHandler.sendBeamPacket(worldObj, minerVector, new Vector3(xCoord + r - offset, y, zCoord - r).translate(0.3D), 0.0F, 0.0F, 1.0F, age,
							0, 50);
					PacketHandler.sendBeamPacket(worldObj, minerVector, new Vector3(xCoord - r, y, zCoord - r + offset).translate(0.3D), 0.0F, 0.0F, 1.0F, age,
							0, 50);
					worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:hilaser", 4F, 1F);
					delayTicksMine = 0;
					currentState = STATE_MINING;
					updateMetadata(BlockMiningLaser.ICON_MININGPOWERED);
					return;
				} else {
					worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
					currentLayer--;
				}
			}
		} else if (currentState == STATE_MINING) { // mining
			delayTicksMine++;
			if (delayTicksMine >= WarpDriveConfig.ML_MINE_DELAY_TICKS) {
				delayTicksMine = 0;

				if (valuableIndex >= valuablesInLayer.size()) {
					delayTicksScan = 0;
					currentState = STATE_SCANNING;
					updateMetadata(BlockMiningLaser.ICON_SCANNINGPOWERED);
					// rescan same layer
					scanLayer();
					if (valuablesInLayer.size() <= 0) {
						currentLayer--;
					}
					return;
				}

				// consume power
				enoughPower = consumeEnergyPacketFromBooster(isOnEarth ? WarpDriveConfig.ML_EU_PER_BLOCK_EARTH : WarpDriveConfig.ML_EU_PER_BLOCK_SPACE, false);
				if (!enoughPower) {
					updateMetadata(BlockMiningLaser.ICON_MININGLOWPOWER);
					return;
				} else {
					updateMetadata(BlockMiningLaser.ICON_MININGPOWERED);
				}

				// System.out.println("[ML] Mining: " + (valuableIndex + 1) +
				// "/" + valuablesInLayer.size());
				Vector3 valuable = valuablesInLayer.get(valuableIndex);
				valuableIndex++;
				// Mine valuable ore
				Block block = worldObj.getBlock(valuable.intX(), valuable.intY(), valuable.intZ());
				// Skip if block is too hard or its empty block (check again in
				// case it changed)
				if (!canDig(block, valuable.intX(), valuable.intY(), valuable.intZ())) {
					delayTicksMine = Math.round(WarpDriveConfig.ML_MINE_DELAY_TICKS * 0.8F);
				}
				int age = Math.max(10, Math.round((4 + worldObj.rand.nextFloat()) * WarpDriveConfig.ML_MINE_DELAY_TICKS));
				PacketHandler.sendBeamPacket(worldObj, minerVector, new Vector3(valuable.intX(), valuable.intY(), valuable.intZ()).translate(0.5D), 1.0F, 1.0F,
						0.0F, age, 0, 50);
				worldObj.playSoundEffect(xCoord + 0.5f, yCoord, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
				harvestBlock(valuable);
			}
		}
	}

	private void updateMetadata(int metadata) {
		Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
		if (block.isAssociatedBlock(WarpDrive.miningLaserBlock) && getBlockMetadata() != metadata) {
			worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, metadata, 2);
		}
	}

	private void stop() {
		// WarpDrive.debugPrint("" + this + " Stop requested");
		currentState = STATE_IDLE;
		updateMetadata(BlockMiningLaser.ICON_IDLE);
	}

	private boolean canDig(Block block, int x, int y, int z) {
		// ignore air
		if (worldObj.isAirBlock(x, y, z)) {
			return false;
		}
		// check blacklist
		if (block.isAssociatedBlock(Blocks.bedrock)) {
			return false;
		}
		if (WarpDriveConfig.forceFieldBlocks.contains(block)) {
			stop();
			return false;
		}
		// check whitelist
		if (WarpDriveConfig.minerOres.contains(block)) {
			return true;
		}
		// check default //TODO: i dont think resistence is appropriate for
		// mining.
		if (block.getExplosionResistance(null) <= Blocks.obsidian.getExplosionResistance(null)) {
			return true;
		}
		// WarpDrive.debugPrint("" + this + " Rejecting " + blockID + " at (" +
		// x + ", " + y + ", " + z + ")");
		return false;
	}

	private void harvestBlock(Vector3 valuable) {
		Block block = worldObj.getBlock(valuable.intX(), valuable.intY(), valuable.intZ());
		int blockMeta = worldObj.getBlockMetadata(valuable.intX(), valuable.intY(), valuable.intZ());
		if (block != null && (block instanceof BlockLiquid)) {
			// Evaporate fluid
			worldObj.playSoundEffect(valuable.intX() + 0.5D, valuable.intY() + 0.5D, valuable.intZ() + 0.5D, "random.fizz", 0.5F,
					2.6F + (worldObj.rand.nextFloat() - worldObj.rand.nextFloat()) * 0.8F);
		} else {
			List<ItemStack> stacks = getItemStackFromBlock(valuable.intX(), valuable.intY(), valuable.intZ(), block, blockMeta);
			if (stacks != null) {
				boolean overflow = false;
				int qtyLeft = 0;
				ItemStack stackLeft = null;
				for (ItemStack stack : stacks) {
					qtyLeft = putInGrid(stack);
					/*
					 * if (qtyLeft > 0) { // FIXME: untested stackLeft =
					 * copyWithSize(stack, qtyLeft); qtyLeft =
					 * putInPipe(stackLeft); }/*
					 */
					if (qtyLeft > 0) {
						stackLeft = copyWithSize(stack, qtyLeft);
						qtyLeft = putInChest(findChest(), stackLeft);
					}
					if (qtyLeft > 0) {
						WarpDrive.debugPrint("" + this + " Overflow detected");
						overflow = true;
						int transfer;
						while (qtyLeft > 0) {
							transfer = Math.min(qtyLeft, stack.getMaxStackSize());
							ItemStack dropItemStack = copyWithSize(stack, transfer);
							EntityItem itemEnt = new EntityItem(worldObj, xCoord + 0.5D, yCoord + 1.0D, zCoord + 0.5D, dropItemStack);
							worldObj.spawnEntityInWorld(itemEnt);
							qtyLeft -= transfer;
						}
					}
				}
				if (overflow) {
					stop();
				}
			}
			// standard harvest block effect //TODO: Update
			// worldObj.playAuxSFXAtEntity(null, 2001, valuable.intX(),
			// valuable.intY(), valuable.intZ(), block + (blockMeta << 12));
		}
		worldObj.setBlockToAir(valuable.intX(), valuable.intY(), valuable.intZ());
	}

	private IInventory findChest() {
		TileEntity result = null;
		result = worldObj.getTileEntity(xCoord + 1, yCoord, zCoord);
		if (result != null && result instanceof IInventory) {
			return (IInventory) result;
		}

		result = worldObj.getTileEntity(xCoord - 1, yCoord, zCoord);
		if (result != null && result instanceof IInventory) {
			return (IInventory) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord, zCoord + 1);
		if (result != null && result instanceof IInventory) {
			return (IInventory) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord, zCoord - 1);
		if (result != null && result instanceof IInventory) {
			return (IInventory) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord + 1, zCoord);
		if (result != null && result instanceof IInventory) {
			return (IInventory) result;
		}

		return null;
	}

	public List<ItemStack> getItemStackFromBlock(int i, int j, int k, Block block, int blockMeta) {

		if (block == null) {
			return null;
		}
		if (enableSilktouch) {
			boolean isSilkHarvestable = false;
			try {
				isSilkHarvestable = block.canSilkHarvest(worldObj, null, i, j, k, blockMeta);
			} catch (Exception e) {// protect in case the mined block is
				// corrupted
				e.printStackTrace();
			}
			if (isSilkHarvestable) {
				if (WarpDriveConfig.ML_DEUTERIUM_MUL_SILKTOUCH <= 0) {
					ArrayList<ItemStack> isBlock = new ArrayList<ItemStack>();
					isBlock.add(new ItemStack(block, 1, blockMeta));
					return isBlock;
				} else {
					if (grid != null && AENetworkReady) {
						IMEInventoryHandler cellArray = grid.getCellArray();
						if (cellArray != null) {
							int consume = isQuarry ? 15 : 1000;

							IAEItemStack entryToAEIS1 = null;
							long contained1 = 0;
							if (WarpDriveConfig.AEExtra_fluidDrive != null) {
								entryToAEIS1 = Util.createItemStack(new ItemStack(WarpDriveConfig.AEExtra_fluidDrive, consume, FluidRegistry
										.getFluidID("deuterium")));
								contained1 = cellArray.countOfItemType(entryToAEIS1);
							}
							IAEItemStack entryToAEIS2 = null;
							long contained2 = 0;
							if (WarpDriveConfig.IC2_fluidCell != null) {
								entryToAEIS2 = Util
										.createItemStack(new ItemStack(WarpDriveConfig.IC2_fluidCell, consume, FluidRegistry.getFluidID("deuterium")));
								contained2 = cellArray.countOfItemType(entryToAEIS2);
							}
							IAEItemStack entryToAEIS3 = null;
							long contained3 = 0;
							if (WarpDriveConfig.AS_deuteriumCell != 0) {
								entryToAEIS3 = Util.createItemStack(new ItemStack(WarpDriveConfig.AS_deuteriumCell, consume, FluidRegistry
										.getFluidID("deuterium")));
								contained3 = cellArray.countOfItemType(entryToAEIS3);
							}

							if (contained1 + contained2 + contained3 >= consume) {
								if (contained1 > 0) {
									cellArray.extractItems(entryToAEIS1);
								}
								if (contained2 > 0 && contained1 < consume) {
									entryToAEIS2 = Util.createItemStack(new ItemStack(WarpDriveConfig.IC2_fluidCell, (int) (consume - contained2),
											FluidRegistry.getFluidID("deuterium")));
									cellArray.extractItems(entryToAEIS2);
								}
								if (contained3 > 0 && contained1 + contained2 < consume) {
									entryToAEIS3 = Util.createItemStack(new ItemStack(WarpDriveConfig.AS_deuteriumCell,
											(int) (consume - contained1 - contained2), FluidRegistry.getFluidID("deuterium")));
									cellArray.extractItems(entryToAEIS3);
								}

								ArrayList<ItemStack> isBlock = new ArrayList<ItemStack>();
								isBlock.add(new ItemStack(block, 1, blockMeta));
								return isBlock;
							}
						}
					} else {
						// Missing AE connection
					}
				}
			}
		}

		try {
			return block.getDrops(worldObj, i, j, k, blockMeta, 0);
		} catch (Exception e) {// protect in case the mined block is corrupted
			e.printStackTrace();
			return null;
		}
	}

	private int putInGrid(ItemStack itemStackSource) {
		int qtyLeft = itemStackSource.stackSize;
		if (grid != null && AENetworkReady) {
			IMEInventoryHandler cellArray = grid.getCellArray();
			if (cellArray != null) {
				IAEItemStack ret = cellArray.addItems(Util.createItemStack(itemStackSource));
				if (ret != null) {
					qtyLeft = (int) ret.getStackSize();
				} else {
					qtyLeft = 0;
				}
			}
		}
		return qtyLeft;
	}

	private int putInPipe(ItemStack itemStackSource) {
		ItemStack itemStackLeft = itemStackSource.copy();
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			TileEntity te = worldObj.getTileEntity(xCoord + direction.offsetX, yCoord + direction.offsetY, zCoord + direction.offsetZ);
			if (te != null && te instanceof IItemConduit) {
				WarpDrive.debugPrint("dumping to pipe");
				itemStackLeft = ((IItemConduit) te).insertItem(direction.getOpposite(), itemStackLeft);
				if (itemStackLeft == null) {
					return 0;
				}
			}
		}
		return itemStackLeft.stackSize;
	}

	private int putInChest(IInventory inventory, ItemStack itemStackSource) {
		if (itemStackSource == null) {
			return 0;
		}

		int qtyLeft = itemStackSource.stackSize;
		int transfer;

		if (inventory != null) {
			// fill existing stacks first
			for (int i = 0; i < inventory.getSizeInventory(); i++) {
				if (!inventory.isItemValidForSlot(i, itemStackSource)) {
					continue;
				}

				ItemStack itemStack = inventory.getStackInSlot(i);
				if (itemStack == null || !itemStack.isItemEqual(itemStackSource)) {
					continue;
				}

				transfer = Math.min(qtyLeft, itemStack.getMaxStackSize() - itemStack.stackSize);
				itemStack.stackSize += transfer;
				qtyLeft -= transfer;
				if (qtyLeft <= 0) {
					return 0;
				}
			}

			// put remaining in empty slot
			for (int i = 0; i < inventory.getSizeInventory(); i++) {
				if (!inventory.isItemValidForSlot(i, itemStackSource)) {
					continue;
				}

				ItemStack itemStack = inventory.getStackInSlot(i);
				if (itemStack != null) {
					continue;
				}

				transfer = Math.min(qtyLeft, itemStackSource.getMaxStackSize());
				ItemStack dest = copyWithSize(itemStackSource, transfer);
				inventory.setInventorySlotContents(i, dest);
				qtyLeft -= transfer;

				if (qtyLeft <= 0) {
					return 0;
				}
			}
		}

		return qtyLeft;
	}

	public static ItemStack copyWithSize(ItemStack itemStack, int newSize) {
		ItemStack ret = itemStack.copy();
		ret.stackSize = newSize;
		return ret;
	}

	private void scanLayer() {
		// System.out.println("Scanning layer");
		valuablesInLayer.clear();
		valuableIndex = 0;
		int radius, x, z;
		Block block;
		int xmax, zmax;
		int xmin, zmin;

		// Search for valuable blocks
		x = xCoord;
		z = zCoord;
		block = worldObj.getBlock(x, currentLayer, z);
		if (canDig(block, x, currentLayer, z)) {
			if (isQuarry || WarpDriveConfig.minerOres.contains(block)) {// Quarry
				// collects
				// all
				// blocks
				// or
				// only
				// collect
				// valuables
				// blocks
				valuablesInLayer.add(new Vector3(x, currentLayer, z));
			}
		}
		for (radius = 1; radius <= WarpDriveConfig.ML_MAX_RADIUS; radius++) {
			xmax = xCoord + radius;
			xmin = xCoord - radius;
			zmax = zCoord + radius;
			zmin = zCoord - radius;
			x = xCoord;
			z = zmin;
			for (; x <= xmax; x++) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (isQuarry || WarpDriveConfig.minerOres.contains(block)) {// Quarry
						// collects
						// all
						// blocks
						// or
						// only
						// collect
						// valuables
						// blocks
						valuablesInLayer.add(new Vector3(x, currentLayer, z));
					}
				}
			}
			x = xmax;
			z++;
			for (; z <= zmax; z++) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (isQuarry || WarpDriveConfig.minerOres.contains(block)) {// Quarry
						// collects
						// all
						// blocks
						// or
						// only
						// collect
						// valuables
						// blocks
						valuablesInLayer.add(new Vector3(x, currentLayer, z));
					}
				}
			}
			x--;
			z = zmax;
			for (; x >= xmin; x--) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (isQuarry || WarpDriveConfig.minerOres.contains(block)) {// Quarry
						// collects
						// all
						// blocks
						// or
						// only
						// collect
						// valuables
						// blocks
						valuablesInLayer.add(new Vector3(x, currentLayer, z));
					}
				}
			}
			x = xmin;
			z--;
			for (; z > zmin; z--) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (isQuarry || WarpDriveConfig.minerOres.contains(block)) {// Quarry
						// collects
						// all
						// blocks
						// or
						// only
						// collect
						// valuables
						// blocks
						valuablesInLayer.add(new Vector3(x, currentLayer, z));
					}
				}
			}
			x = xmin;
			z = zmin;
			for (; x < xCoord; x++) {
				block = worldObj.getBlock(x, currentLayer, z);
				if (canDig(block, x, currentLayer, z)) {
					if (isQuarry || WarpDriveConfig.minerOres.contains(block)) {// Quarry
						// collects
						// all
						// blocks
						// or
						// only
						// collect
						// valuables
						// blocks
						valuablesInLayer.add(new Vector3(x, currentLayer, z));
					}
				}
			}
		}

		// System.out.println("" + this + " Found " + valuablesInLayer.size() +
		// " valuables");
	}

	private int getEnergyLevel() {
		TileEntityParticleBooster booster = findFirstBooster();
		if (booster != null) {
			return booster.getEnergyStored();
		} else {
			return 0;
		}
	}

	private boolean consumeEnergyPacketFromBooster(int amount, boolean simulate) {
		TileEntityParticleBooster booster = findFirstBooster();
		if (booster != null) {
			return booster.consumeEnergy(amount, simulate);
		} else {
			return false;
		}
	}

	private TileEntityParticleBooster findFirstBooster() {// FIXME: merge me...
		TileEntity result;
		result = worldObj.getTileEntity(xCoord + 1, yCoord, zCoord);

		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = 1;
			dz = 0;
			dy = 0;
			return (TileEntityParticleBooster) result;
		}

		result = worldObj.getTileEntity(xCoord - 1, yCoord, zCoord);

		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = -1;
			dz = 0;
			dy = 0;
			return (TileEntityParticleBooster) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord, zCoord + 1);

		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = 0;
			dz = 1;
			dy = 0;
			return (TileEntityParticleBooster) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord, zCoord - 1);

		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = 0;
			dz = -1;
			dy = 0;
			return (TileEntityParticleBooster) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord + 1, zCoord);

		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = 0;
			dz = 0;
			dy = 1;
			return (TileEntityParticleBooster) result;
		}

		return null;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		currentState = tag.getInteger("currentState");
		isQuarry = tag.getBoolean("isQuarry");
		currentLayer = tag.getInteger("currentLayer");
		enableSilktouch = tag.getBoolean("enableSilktouch");
		if (currentState == STATE_MINING) {
			scanLayer();
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("currentState", currentState);
		tag.setBoolean("isQuarry", isQuarry);
		tag.setInteger("currentLayer", currentLayer);
		tag.setBoolean("enableSilktouch", enableSilktouch);
	}

	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] mine(Context context, Arguments arguments) {
		return mine(argumentsOCtoCC(arguments));
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] stop(Context context, Arguments arguments) {
		stop();
		return null;
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] isMining(Context context, Arguments arguments) {
		return new Boolean[] { isMining() };
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] quarry(Context context, Arguments arguments) {
		return quarry(argumentsOCtoCC(arguments));
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] state(Context context, Arguments arguments) {
		return state(argumentsOCtoCC(arguments));
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] offset(Context context, Arguments arguments) {
		return offset(argumentsOCtoCC(arguments));
	}

	private Object[] mine(Object[] arguments) {
		if (isMining()) {
			return new Boolean[] { false };
		}

		isQuarry = false;
		delayTicksWarmup = 0;
		currentState = STATE_WARMUP;
		currentLayer = yCoord - layerOffset - 1;
		enableSilktouch = (arguments.length == 1 && (WarpDriveConfig.ML_DEUTERIUM_MUL_SILKTOUCH <= 0 || FluidRegistry.isFluidRegistered("deuterium")));
		return new Boolean[] { true };
	}

	private Object[] quarry(Object[] arguments) {
		if (isMining()) {
			return new Boolean[] { false };
		}

		isQuarry = true;
		delayTicksScan = 0;
		currentState = STATE_WARMUP;
		currentLayer = yCoord - layerOffset - 1;
		enableSilktouch = (arguments.length == 1 && (WarpDriveConfig.ML_DEUTERIUM_MUL_SILKTOUCH <= 0 || FluidRegistry.isFluidRegistered("deuterium")));
		return new Boolean[] { true };
	}

	private Object[] state(Object[] arguments) {
		int energy = getEnergyLevel();
		String status = getStatus();
		Integer retValuablesInLayer, retValuablesMined;
		if (isMining()) {
			retValuablesInLayer = valuablesInLayer.size();
			retValuablesMined = valuableIndex;

			return new Object[] { status, energy, currentLayer, retValuablesMined, retValuablesInLayer };
		}
		return new Object[] { status, energy, currentLayer, 0, 0 };
	}

	private Object[] offset(Object[] arguments) {
		if (arguments.length == 1) {
			try {
				layerOffset = Math.min(256, Math.abs(toInt(arguments[0])));
			} catch (Exception e) {
				return new Integer[] { layerOffset };
			}
		}
		return new Integer[] { layerOffset };
	}

	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public void attach(IComputerAccess computer) {
		super.attach(computer);
		if (WarpDriveConfig.G_LUA_SCRIPTS != WarpDriveConfig.LUA_SCRIPTS_NONE) {
			computer.mount("/mininglaser", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/mininglaser"));
			if (WarpDriveConfig.G_LUA_SCRIPTS == WarpDriveConfig.LUA_SCRIPTS_ALL) {
				computer.mount("/mine", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/mininglaser/mine"));
				computer.mount("/stop", ComputerCraftAPI.createResourceMount(WarpDrive.class, "warpdrive", "lua/mininglaser/stop"));
			}
		}
	}

	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception {
		String methodName = methodsArray[method];
		if (methodName.equals("mine")) {
			return mine(arguments);

		} else if (methodName.equals("stop")) {
			stop();

		} else if (methodName.equals("isMining")) {
			return new Boolean[] { isMining() };

		} else if (methodName.equals("quarry")) {
			return quarry(arguments);

		} else if (methodName.equals("state")) { // State is: state, energy,
			// currentLayer,
			// valuablesMined,
			// valuablesInLayer =
			// getMinerState()
			return state(arguments);

		} else if (methodName.equals("offset")) {
			return offset(arguments);
		}
		return null;
	}

	public String getStatus() {
		int energy = 0;
		energy = getEnergyLevel();
		String state = "IDLE (not mining)";
		if (currentState == STATE_IDLE) {
			state = "IDLE (not mining)";
		} else if (currentState == STATE_WARMUP) {
			state = "Warming up...";
		} else if (currentState == STATE_SCANNING) {
			if (isQuarry) {
				state = "Scanning all";
			} else {
				state = "Scanning ores";
			}
		} else if (currentState == STATE_MINING) {
			if (isQuarry) {
				state = "Mining all";
			} else {
				state = "Mining ores";
			}
			if (enableSilktouch) {
				state = state + " using Deuterium";
			}
		}
		if (energy <= 0) {
			state = state + " - Out of energy";
		} else if (((currentState == STATE_SCANNING) || (currentState == STATE_MINING)) && !enoughPower) {
			state = state + " - Not enough power";
		}
		return state;
	}

	// Applied Energistics
	@Override
	public float getPowerDrainPerTick() {
		return 1;
	}

	@Override
	public void validate() {
		super.validate();
		MinecraftForge.EVENT_BUS.post(new GridTileLoadEvent(this, worldObj, getLocation()));
	}

	@Override
	public void invalidate() {
		super.invalidate();
		MinecraftForge.EVENT_BUS.post(new GridTileUnloadEvent(this, worldObj, getLocation()));
	}

	@Override
	public WorldCoord getLocation() {
		return new WorldCoord(xCoord, yCoord, zCoord);
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void setPowerStatus(boolean hasPower) {
		powerStatus = hasPower;
	}

	@Override
	public boolean isPowered() {
		return powerStatus;
	}

	@Override
	public IGridInterface getGrid() {
		return grid;
	}

	@Override
	public void setGrid(IGridInterface parGrid) {
		grid = parGrid;
	}

	@Override
	public World getWorld() {
		return worldObj;
	}

	@Override
	public boolean coveredConnections() {
		return true;
	}

	@Override
	public void setNetworkReady(boolean isReady) {
		AENetworkReady = isReady;
	}

	@Override
	public boolean isMachineActive() {
		return isMining();
	}

	@Override
	public String toString() {
		return String.format("%s @ \'%s\' %d, %d, %d",
				new Object[] { getClass().getSimpleName(), worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(), Integer.valueOf(xCoord),
						Integer.valueOf(yCoord), Integer.valueOf(zCoord) });
	}
}

package cr0s.warpdrive.block;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockUpdateDetector;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public abstract class TileEntityAbstractBase extends TileEntity implements IBlockUpdateDetector {
	
	private boolean isFirstTick = true;
	private boolean isDirty = false;
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		if (isFirstTick) {
			isFirstTick = false;
			onFirstUpdateTick();
		}
		
		if (isDirty) {
			markDirty();
		}
	}
	
	protected void onFirstUpdateTick() {
		// No operation
	}
	
	@Override
	public void updatedNeighbours() {
	}
	
	protected void updateMetadata(int metadata) {
		if (getBlockMetadata() != metadata) {
			worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, metadata, 2);
		}
	}
	
	@Override
	public void markDirty() {
		if ( hasWorldObj()
		  && Commons.isSafeThread() ) {
			super.markDirty();
			isDirty = false;
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			WarpDrive.starMap.onBlockUpdated(worldObj, xCoord, yCoord, zCoord, getBlockType(), getBlockMetadata());
		} else {
			isDirty = true;
		}
	}
	
	// Inventory management methods
	
	protected boolean addToConnectedInventories(final ItemStack itemStack) {
		List<ItemStack> itemStacks = new ArrayList<>(1);
		itemStacks.add(itemStack);
		return addToInventories(itemStacks, Commons.getConnectedInventories(this));
	}
	
	protected boolean addToConnectedInventories(final List<ItemStack> itemStacks) {
		return addToInventories(itemStacks, Commons.getConnectedInventories(this));
	}
	
	protected boolean addToInventories(final List<ItemStack> itemStacks, final Collection<IInventory> inventories) {
		boolean overflow = false;
		if (itemStacks != null) {
			for (ItemStack itemStack : itemStacks) {
				if (itemStack.getItem() == null) {
					WarpDrive.logger.error(this + "Invalid itemStack with null item...");
					continue;
				}
				int qtyLeft = itemStack.stackSize;
				ItemStack itemStackLeft = itemStack.copy();
				for (IInventory inventory : inventories) {
					qtyLeft = addToInventory(itemStack, inventory);
					if (qtyLeft > 0) {
						itemStackLeft.stackSize = qtyLeft;
					} else {
						break;
					}
				}
				if (qtyLeft > 0) {
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.info(this + " Overflow detected");
					}
					overflow = true;
					int transfer;
					while (qtyLeft > 0) {
						transfer = Math.min(qtyLeft, itemStackLeft.getMaxStackSize());
						ItemStack itemStackDrop = Commons.copyWithSize(itemStackLeft, transfer);
						EntityItem entityItem = new EntityItem(worldObj, xCoord + 0.5D, yCoord + 1.0D, zCoord + 0.5D, itemStackDrop);
						worldObj.spawnEntityInWorld(entityItem);
						qtyLeft -= transfer;
					}
				}
			}
		}
		return overflow;
	}
	
	private static int addToInventory(final ItemStack itemStackSource, IInventory inventory) {
		if (itemStackSource == null || itemStackSource.getItem() == null) {
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
				ItemStack dest = Commons.copyWithSize(itemStackSource, transfer);
				inventory.setInventorySlotContents(i, dest);
				qtyLeft -= transfer;
				
				if (qtyLeft <= 0) {
					return 0;
				}
			}
		}
		
		return qtyLeft;
	}
	
	
	// area protection
	protected boolean isBlockBreakCanceled(final UUID uuidPlayer, World world, final int eventX, final int eventY, final int eventZ) {
		return CommonProxy.isBlockBreakCanceled(uuidPlayer, xCoord, yCoord, zCoord, world, eventX, eventY, eventZ);
	}
	
	protected boolean isBlockPlaceCanceled(final UUID uuidPlayer, World world, final int eventX, final int eventY, final int eventZ, final Block block, final int metadata) {
		return CommonProxy.isBlockPlaceCanceled(uuidPlayer, xCoord, yCoord, zCoord, world, eventX, eventY, eventZ, block, metadata);
	}
	
	// saved properties
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		if (tag.hasKey("upgrades")) {
			NBTTagCompound nbtTagCompoundUpgrades = tag.getCompoundTag("upgrades");
			Set<String> keys = nbtTagCompoundUpgrades.func_150296_c();
			for (String key : keys) {
				Object object = getUpgradeFromString(key);
				int value = nbtTagCompoundUpgrades.getByte(key);
				if (object == null) {
					WarpDrive.logger.error("Found an unknown upgrade named '" + key + "' in " + this);
					object = key;
				}
				installedUpgrades.put(object, value);
			}
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		if (!installedUpgrades.isEmpty()) {
			NBTTagCompound nbtTagCompoundUpgrades = new NBTTagCompound();
			for (Entry<Object, Integer> entry : installedUpgrades.entrySet()) {
				String key = getUpgradeAsString(entry.getKey());
				nbtTagCompoundUpgrades.setByte(key, (byte)(int)entry.getValue());
			}
			tagCompound.setTag("upgrades", nbtTagCompoundUpgrades);
		}
	}
	
	public NBTTagCompound writeItemDropNBT(NBTTagCompound nbtTagCompound) {
		writeToNBT(nbtTagCompound);
		nbtTagCompound.removeTag("x");
		nbtTagCompound.removeTag("y");
		nbtTagCompound.removeTag("z");
		return nbtTagCompound;
	}
	
	// status
	protected String getUpgradeStatus() {
		String strUpgrades = getUpgradesAsString();
		if (strUpgrades.isEmpty()) {
			return StatCollector.translateToLocalFormatted("warpdrive.upgrade.statusLine.none",
				strUpgrades);
		} else {
			return StatCollector.translateToLocalFormatted("warpdrive.upgrade.statusLine.valid",
				strUpgrades);
		}
	}
	
	protected String getStatusPrefix() {
		if (worldObj == null) {
			return "";
		} else {
			ItemStack itemStack = new ItemStack(Item.getItemFromBlock(getBlockType()), 1, getBlockMetadata());
			return StatCollector.translateToLocalFormatted("warpdrive.guide.prefix", StatCollector.translateToLocalFormatted(itemStack.getUnlocalizedName() + ".name"));
		}
	}
	
	public String getStatusHeader() {
		return "";
	}
	
	public String getStatus() {
		return getStatusPrefix()
		     + getStatusHeader();
	}
	
	public String getStatusHeaderInPureText() {
		return Commons.removeFormatting( getStatusHeader() );
	}
	
	// upgrade system
	private final HashMap<Object, Integer> installedUpgrades = new HashMap<>(10);
	private final HashMap<Object, Integer> maxUpgrades = new HashMap<>(10);
	public boolean hasUpgrade(final Object upgrade) {
		return getUpgradeCount(upgrade) > 0;
	}
	
	private String getUpgradeAsString(Object object) {
		if (object instanceof Item) {
			return Item.itemRegistry.getNameForObject(object);
		} else if (object instanceof Block) {
			return Block.blockRegistry.getNameForObject(object);
		} else if (object instanceof ItemStack) {
			return Item.itemRegistry.getNameForObject(((ItemStack) object).getItem()) + ":" + ((ItemStack) object).getItemDamage();
		} else {
			return object.toString();
		}
	}
	
	private Object getUpgradeFromString(String name) {
		for (Object object : maxUpgrades.keySet()) {
			if (getUpgradeAsString(object).equals(name)) {
				return object;
			}
		}
		return null;
	}
	
	public Object getFirstUpgradeOfType(final Class clazz, final Object defaultValue) {
		for (Object object : installedUpgrades.keySet()) {
			if (clazz != null && clazz.isInstance(object)) {
				return object;
			}
		}
		return defaultValue;
	}
	
	public Map<Object, Integer> getUpgradesOfType(final Class clazz) {
		if (clazz == null) {
			return installedUpgrades;
		}
		Map<Object, Integer> mapResult = new HashMap<>(installedUpgrades.size());
		for (Entry<Object, Integer> entry : installedUpgrades.entrySet()) {
			if (clazz.isInstance(entry.getKey())) {
				mapResult.put(entry.getKey(), entry.getValue());
			}
		}
		return mapResult;
	}
	
	public int getUpgradeCount(final Object upgrade) {
		Integer value = installedUpgrades.get(upgrade);
		return value == null ? 0 : value;
	}
	
	public int getUpgradeMaxCount(final Object upgrade) {
		Integer value = maxUpgrades.get(upgrade);
		return value == null ? 0 : value;
	}
	
	protected String getUpgradesAsString() {
		String message = "";
		for (Entry<Object, Integer> entry : installedUpgrades.entrySet()) {
			if (!message.isEmpty()) {
				message += ", ";
			}
			Object key = entry.getKey();
			String keyName = key.toString();
			if (key instanceof Item) {
				keyName = ((Item) key).getUnlocalizedName();
			} else if (key instanceof Block) {
				keyName = ((Block) key).getUnlocalizedName();
			}
			if (entry.getValue() == 1) {
				message += keyName;
			} else {
				message += entry.getValue() + " x " + keyName;
			}
		}
		return message;
	}
	
	protected void setUpgradeMaxCount(final Object upgrade, final int value) {
		maxUpgrades.put(upgrade, value);
	}
	
	public boolean canUpgrade(final Object upgrade) {
		return getUpgradeMaxCount(upgrade) >= getUpgradeCount(upgrade) + 1;
	}
	
	public boolean mountUpgrade(final Object upgrade) {
		if (canUpgrade(upgrade)) {
			installedUpgrades.put(upgrade, getUpgradeCount(upgrade) + 1);
			markDirty();
			return true;
		}
		return false;
	}
	
	public boolean dismountUpgrade(final Object upgrade) {
		int count = getUpgradeCount(upgrade);
		if (count > 1) {
			installedUpgrades.put(upgrade, count - 1);
			markDirty();
			return true;
			
		} else if (count > 0) {
			installedUpgrades.remove(upgrade);
			markDirty();
			return true;
		}
		return false;
	}
}

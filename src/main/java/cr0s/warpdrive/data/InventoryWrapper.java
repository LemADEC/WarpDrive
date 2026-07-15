package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

// note: unlike Forge's InvWrapper this won't create ItemStack all around, saving us a bit of lag from GC and capabilities

public class InventoryWrapper {
	
	// constants
	// public static final String TAG_INVENTORY = "inventory";
	
	// WarpDrive methods
	public static Object getInventory(final TileEntity tileEntity, final Direction facing) {
		if (tileEntity instanceof IInventory) {
			return tileEntity;
		}
		
		if (tileEntity != null) {
			return tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing).orElse(null);
		}
		
		return null;
	}
	
	public static @Nonnull Collection<Object> getConnectedInventories(final World world, final BlockPos blockPos) {
		final Collection<Object> result = new ArrayList<>(6);
		final Collection<IItemHandler> resultCapabilities = new ArrayList<>(6);
		final BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
		
		for(final Direction side : Direction.values()) {
			mutableBlockPos.setPos(blockPos.getX() + side.getXOffset(),
			                       blockPos.getY() + side.getYOffset(),
			                       blockPos.getZ() + side.getZOffset() );
			final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
			
			if (tileEntity != null) {
				final IItemHandler itemHandler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(EmptyHandler.INSTANCE);
				if (itemHandler.getSlots() > 0) {
					resultCapabilities.add(itemHandler);
				} else if (tileEntity instanceof IInventory) {
					result.add(tileEntity);
				}
			}
		}
		
		// IItemHandler is causing more memory allocations, so we're prioritizing the interface approach
		result.addAll(resultCapabilities);
		return result;
	}
	
	public static boolean addToConnectedInventories(final World world, final BlockPos blockPos, final ItemStack itemStack) {
		final List<ItemStack> itemStacks = new ArrayList<>(1);
		itemStacks.add(itemStack);
		final Collection<Object> inventories = getConnectedInventories(world, blockPos);
		return addToInventories(world, blockPos, inventories, itemStacks);
	}
	
	public static boolean addToConnectedInventories(final World world, final BlockPos blockPos, final List<ItemStack> itemStacks) {
		final Collection<Object> inventories = getConnectedInventories(world, blockPos);
		return addToInventories(world, blockPos, inventories, itemStacks);
	}
	
	public static boolean addToInventories(final World world, final BlockPos blockPos,
	                                       final Collection<Object> inventories, final List<ItemStack> itemStacks) {
		boolean overflow = false;
		if (itemStacks != null) {
			for (final ItemStack itemStack : itemStacks) {
				if (itemStack.isEmpty()) {
					WarpDrive.logger.error(String.format("Invalid empty itemStack %s",
					                                     Commons.format(world, blockPos) ));
					continue;
				}
				int qtyLeft = itemStack.getCount();
				ItemStack itemStackLeft = itemStack;
				for (final Object inventory : inventories) {
					if (inventory instanceof IInventory) {
						qtyLeft = addToInventory(itemStack, (IInventory) inventory);
					} else if (inventory instanceof IItemHandler) {
						qtyLeft = addToInventory(itemStack, (IItemHandler) inventory);
					} else {
						if (Commons.throttleMe("addToInventories")){
							WarpDrive.logger.error(String.format("Invalid inventory type %s of class %s at %s, please report to mod author",
							                                     inventory, inventory.getClass(), Commons.format(world, blockPos) ));
							break;
						}
					}
					if (qtyLeft > 0) {
						if (itemStackLeft == itemStack) {
							itemStackLeft = itemStack.copy(); // due to capabilities handling, copy is very slow, so we call it only when strictly necessary
						}
						itemStackLeft.setCount(qtyLeft);
					} else {
						break;
					}
				}
				if (qtyLeft > 0) {
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.info(String.format("Overflow detected at %s",
						                                    Commons.format(world, blockPos) ));
					}
					overflow = true;
					int transfer;
					while (qtyLeft > 0) {
						transfer = Math.min(qtyLeft, itemStackLeft.getMaxStackSize());
						final ItemStack itemStackDrop = Commons.copyWithSize(itemStackLeft, transfer);
						final ItemEntity entityItem = new ItemEntity(world, blockPos.getX() + 0.5D, blockPos.getY() + 1.0D, blockPos.getZ() + 0.5D, itemStackDrop);
						world.addEntity(entityItem);
						qtyLeft -= transfer;
					}
				}
			}
		}
		return overflow;
	}
	
	private static int addToInventory(final ItemStack itemStackSource, final IInventory inventory) {
		if (itemStackSource == null || itemStackSource.isEmpty()) {
			return 0;
		}
		
		int qtyLeft = itemStackSource.getCount();
		if (inventory == null) {
			return qtyLeft;
		}
		
		// fill existing stacks first
		for (int indexSlot = 0; indexSlot < inventory.getSizeInventory(); indexSlot++) {
			if (!inventory.isItemValidForSlot(indexSlot, itemStackSource)) {
				continue;
			}
			
			final ItemStack itemStack = inventory.getStackInSlot(indexSlot);
			if ( itemStack.isEmpty()
			  || !itemStack.isItemEqual(itemStackSource) ) {
				continue;
			}
			
			final int transfer = Math.min(Math.min(qtyLeft, itemStack.getMaxStackSize() - itemStack.getCount()), inventory.getInventoryStackLimit());
			itemStack.grow(transfer);
			qtyLeft -= transfer;
			if (qtyLeft <= 0) {
				return 0;
			}
		}
		
		// put remaining in an empty slot
		for (int indexSlot = 0; indexSlot < inventory.getSizeInventory(); indexSlot++) {
			if (!inventory.isItemValidForSlot(indexSlot, itemStackSource)) {
				continue;
			}
			
			final ItemStack itemStack = inventory.getStackInSlot(indexSlot);
			if (!itemStack.isEmpty()) {
				continue;
			}
			
			final int transfer = Math.min(Math.min(qtyLeft, itemStackSource.getMaxStackSize()), inventory.getInventoryStackLimit());
			final ItemStack dest = Commons.copyWithSize(itemStackSource, transfer);
			inventory.setInventorySlotContents(indexSlot, dest);
			qtyLeft -= transfer;
			
			if (qtyLeft <= 0) {
				return 0;
			}
		}
		
		return qtyLeft;
	}
	
	private static int addToInventory(final ItemStack itemStackSource, final IItemHandler itemHandler) {
		if (itemStackSource == null || itemStackSource.isEmpty()) {
			return 0;
		}
		
		if (itemHandler == null) {
			return itemStackSource.getCount();
		}
		ItemStack itemStackLeft = itemStackSource;
		
		// fill existing stacks first
		for (int indexSlot = 0; indexSlot < itemHandler.getSlots(); indexSlot++) {
			if (!itemHandler.isItemValid(indexSlot, itemStackSource)) {
				continue;
			}
			
			final ItemStack itemStack = itemHandler.getStackInSlot(indexSlot);
			if ( itemStack.isEmpty()
			  || !itemStack.isItemEqual(itemStackSource) ) {
				continue;
			}
			
			itemStackLeft = itemHandler.insertItem(indexSlot, itemStackLeft, false);
			if (itemStackLeft.getCount() <= 0) {
				return 0;
			}
		}
		
		// put remaining in an empty slot
		for (int indexSlot = 0; indexSlot < itemHandler.getSlots(); indexSlot++) {
			if (!itemHandler.isItemValid(indexSlot, itemStackSource)) {
				continue;
			}
			
			final ItemStack itemStack = itemHandler.getStackInSlot(indexSlot);
			if (!itemStack.isEmpty()) {
				continue;
			}
			
			itemStackLeft = itemHandler.insertItem(indexSlot, itemStackSource, false);
			if (itemStackLeft.getCount() <= 0) {
				return 0;
			}
		}
		
		return itemStackLeft.getCount();
	}
	
	public static int getSize(final Object inventory) {
		if (inventory instanceof IInventory) {
			return ((IInventory) inventory).getSizeInventory();
		} else if (inventory instanceof IItemHandler) {
			return ((IItemHandler) inventory).getSlots();
		} else if (inventory instanceof ItemStack) {
			return 1;
		} else {
			WarpDrive.logger.error(String.format("Invalid inventory type, please report to mod author: %s",
			                                     inventory ));
			return 0;
		}
	}
	
	public static ItemStack getStackInSlot(final Object inventory, final int indexSlot) {
		if (inventory instanceof IInventory) {
			return ((IInventory) inventory).getStackInSlot(indexSlot);
		} else if (inventory instanceof IItemHandler) {
			return ((IItemHandler) inventory).getStackInSlot(indexSlot);
		} else if (inventory instanceof ItemStack) {
			return (ItemStack) inventory;
		} else {
			WarpDrive.logger.error(String.format("Invalid inventory type, please report to mod author: %s",
			                                     inventory ));
			return ItemStack.EMPTY;
		}
	}
	
	public static boolean isItemValid(final Object inventory, final int indexSlot, final ItemStack itemStack) {
		if (inventory instanceof IInventory) {
			return ((IInventory) inventory).isItemValidForSlot(indexSlot, itemStack);
		} else if (inventory instanceof IItemHandler) {
			return ((IItemHandler) inventory).isItemValid(indexSlot, itemStack);
		} else {
			WarpDrive.logger.error(String.format("Invalid inventory type, please report to mod author: %s",
			                                     inventory ));
			return false;
		}
	}
	
	public static void insertItem(final Object inventory, final int indexSlot, final ItemStack itemStack) {
		if (inventory instanceof IInventory) {
			final ItemStack itemStackExisting = ((IInventory) inventory).getStackInSlot(indexSlot);
			if (itemStackExisting.isEmpty()) {
				((IInventory) inventory).setInventorySlotContents(indexSlot, itemStack);
			} else {
				WarpDrive.logger.error(String.format("Invalid inventory slot %d for insertion of %s, inventory should be empty first, please report to mod author: %s",
				                                     indexSlot, itemStack, inventory));
				if (inventory instanceof TileEntity) {
					final World world = ((TileEntity) inventory).getWorld();
					assert world != null;
					final BlockPos blockPos = ((TileEntity) inventory).getPos();
					final ItemEntity entityItem = new ItemEntity(world, blockPos.getX() + 0.5D, blockPos.getY() + 1.0D, blockPos.getZ() + 0.5D, itemStack);
					world.addEntity(entityItem);
				}
			}
		} else if (inventory instanceof IItemHandler) {
			((IItemHandler) inventory).insertItem(indexSlot, itemStack, false);
		} else {
			WarpDrive.logger.error(String.format("Invalid inventory type, please report to mod author: %s",
			                                     inventory ));
		}
	}
	
	public static void decrStackSize(final Object inventory, final int indexSlot, final int quantity) {
		if (inventory instanceof IInventory) {
			((IInventory) inventory).decrStackSize(indexSlot, quantity);
		} else if (inventory instanceof IItemHandler) {
			((IItemHandler) inventory).extractItem(indexSlot, quantity, false);
		} else {
			WarpDrive.logger.error(String.format("Invalid inventory type, please report to mod author: %s",
			                                     inventory ));
		}
	}
}
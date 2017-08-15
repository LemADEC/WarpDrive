package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import net.minecraftforge.common.util.ForgeDirection;

public class CommandDump extends CommandBase {
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Override
	public String getCommandName() {
		return "wdump";
	}
	
	@Override
	public void processCommand(ICommandSender commandSender, String[] params) {
		if (commandSender == null) { return; }
		final World world = commandSender.getEntityWorld();
		final ChunkCoordinates coordinates = commandSender.getPlayerCoordinates();
		
		if (world == null || coordinates == null) {
			Commons.addChatMessage(commandSender, "* wdump: unknown world or coordinates, probably an invalid command sender in action here.");
			return;
		}
		int x = coordinates.posX;
		int y = coordinates.posY;
		int z = coordinates.posZ;
		
		// parse arguments
		if (params.length != 0) {
			Commons.addChatMessage(commandSender, getCommandUsage(commandSender));
			return;
		}
		
		// validate
		IInventory inventory = null;
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			inventory = getInventory(world, x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ);
			if (inventory != null) {
				x += direction.offsetX;
				y += direction.offsetY;
				z += direction.offsetZ;
				break;
			}
		}
		if (inventory == null) {
			Commons.addChatMessage(commandSender, "§c/wdump: no container found around player");
			return;
		}
		
		// actually dump
		WarpDrive.logger.info(String.format("Dumping content from container at %s (%d %d %d):", world.provider.getDimensionName(), x, y, z));
		for (int indexSlot = 0; indexSlot < inventory.getSizeInventory(); indexSlot++) {
			final ItemStack itemStack = inventory.getStackInSlot(indexSlot);
			if (itemStack != null) {
				final UniqueIdentifier uniqueIdentifier = GameRegistry.findUniqueIdentifierFor(itemStack.getItem());
				final String stringDamage = itemStack.getItemDamage() == 0 ? "" : String.format(" damage=\"%d\"", itemStack.getItemDamage());
				final String stringNBT = !itemStack.hasTagCompound() ? "" : String.format(" nbt=\"%s\"", itemStack.getTagCompound());
				WarpDrive.logger.info(String.format("Slot %3d is <loot item=\"%s:%s\"%s minQuantity=\"%d\" minQuantity=\"%d\"%s weight=\"1\" /><!-- %s -->",
				                                    indexSlot,
				                                    uniqueIdentifier.modId, uniqueIdentifier.name,
				                                    stringDamage,
				                                    itemStack.stackSize, itemStack.stackSize,
				                                    stringNBT,
				                                    itemStack.getDisplayName()));
			}
		}
	}
	
	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "/wdump: write loot table in console for item container below or next to player";
	}
	
	private IInventory getInventory(final World world, final int x, final int y, final int z) {
		final Block block = world.getBlock(x, y, z);
		if (block instanceof ITileEntityProvider) {
			final int metadata = world.getBlockMetadata(x, y, z);
			if (block.hasTileEntity(metadata)) {
				final TileEntity tileEntity = world.getTileEntity(x, y, z);
				if (tileEntity instanceof IInventory) {
					if (((IInventory) tileEntity).getSizeInventory() > 0) {
						return (IInventory) tileEntity;
					}
				}
			}
		}
		return null;
	}
}

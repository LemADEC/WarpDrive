package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;

public class CommandBed extends CommandBase {
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Override
	public String getCommandName() {
		return "bed";
	}
	
	@Override
	public void processCommand(ICommandSender commandSender, String[] params) {
		if (commandSender == null) { return; }
		
		// parse arguments
		//noinspection StatementWithEmptyBody
		EntityPlayerMP[] entityPlayerMPs = null;
		if (params.length == 0) {
			if (commandSender instanceof EntityPlayerMP) {
				entityPlayerMPs = new EntityPlayerMP[1];
				entityPlayerMPs[0] = (EntityPlayerMP) commandSender;
			} else {
				Commons.addChatMessage(commandSender, String.format("§c/%s: use as a player or provide a player name",
				                                                    getCommandName()));
				return;
			}
			
		} else if (params.length == 1) {
			if ( params[0].equalsIgnoreCase("help")
			  || params[0].equalsIgnoreCase("?") ) {
				Commons.addChatMessage(commandSender, getCommandUsage(commandSender));
				return;
			}
			if ( commandSender instanceof EntityPlayerMP
			  && !((EntityPlayerMP) commandSender).capabilities.isCreativeMode ) {
				Commons.addChatMessage(commandSender, getCommandUsage(commandSender));
				return;
			}
			
			EntityPlayerMP[] entityPlayerMPs_found = Commons.getOnlinePlayerByNameOrSelector(commandSender, params[0]);
			if (entityPlayerMPs_found != null) {
				entityPlayerMPs = entityPlayerMPs_found;
			} else if (commandSender instanceof EntityPlayerMP) {
				entityPlayerMPs = new EntityPlayerMP[1];
				entityPlayerMPs[0] = (EntityPlayerMP) commandSender;
			} else {
				Commons.addChatMessage(commandSender, String.format("§c/%s: player not found '%s'",
				                                                    getCommandName(), params[0]));
				return;
			}
		}
		
		assert (entityPlayerMPs != null);
		for (final EntityPlayerMP entityPlayerMP : entityPlayerMPs) {
			final ChunkCoordinates bedLocation = entityPlayerMP.getBedLocation(entityPlayerMP.worldObj.provider.dimensionId);
			if (bedLocation == null) {
				Commons.addChatMessage(entityPlayerMP, String.format("§cTeleportation failed:\nyou need to set your bed location in %s",
				                                                     entityPlayerMP.worldObj.provider.getDimensionName()));
				if (params.length != 0) {
					Commons.addChatMessage(commandSender, String.format("§cTeleportation failed for player %s:\nplayer needs to set his/her bed location in %s",
					                                                    entityPlayerMP.getCommandSenderName(),
					                                                    entityPlayerMP.worldObj.provider.getDimensionName()));
				}
				continue;
			}
			
			final Block block = entityPlayerMP.worldObj.getBlock(bedLocation.posX, bedLocation.posY, bedLocation.posZ);
			if (!(block instanceof BlockBed)) {
				Commons.addChatMessage(entityPlayerMP, String.format("§cTeleportation failed:\nyour bed is no longer there in %s",
				                                                     entityPlayerMP.worldObj.provider.getDimensionName()));
				if (params.length != 0) {
					Commons.addChatMessage(commandSender, String.format("§cTeleportation failed for player %s:\nbed is no longer there in %s",
					                                                    entityPlayerMP.getCommandSenderName(),
					                                                    entityPlayerMP.worldObj.provider.getDimensionName()));
				}
				continue;
			}
			
			entityPlayerMP.setPositionAndUpdate(bedLocation.posX + 0.5D, bedLocation.posY + 0.5D, bedLocation.posZ + 0.5D);
			
			Commons.addChatMessage(entityPlayerMP, String.format("Teleporting to (%d %d %d)",
			                                                     bedLocation.posX, bedLocation.posY, bedLocation.posZ));
			if (params.length != 0) {
				Commons.addChatMessage(commandSender, String.format("Teleporting player %s to %s @ (%d %d %d)",
				                                                    entityPlayerMP.getCommandSenderName(),
				                                                    entityPlayerMP.worldObj.provider.getDimensionName(),
				                                                    bedLocation.posX, bedLocation.posY, bedLocation.posZ));
			}
		}
	}
	
	@Override
	public String getCommandUsage(final ICommandSender commandSender) {
		return getCommandName() + " (<playerName>)"
		       + "\nplayerName: name of the player home to find. Exact casing is required.";
	}
}

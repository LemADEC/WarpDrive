package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class CommandFind extends CommandBase {
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Override
	public String getCommandName() {
		return "wfind";
	}
	
	@Override
	public void processCommand(ICommandSender commandSender, String[] params) {
		if (commandSender == null) { return; }
		
		// parse arguments
		//noinspection StatementWithEmptyBody
		String nameToken = "";
		if (params.length == 0) {
			Commons.addChatMessage(commandSender, getCommandUsage(commandSender));
			return;
		} else if (params.length == 1) {
			if (params[0].equalsIgnoreCase("help") || params[0].equalsIgnoreCase("?")) {
				Commons.addChatMessage(commandSender, getCommandUsage(commandSender));
				return;
			}
			nameToken = params[0];
		} else {
			final StringBuilder nameBuilder = new StringBuilder();
			for (final String param : params) {
				if (nameBuilder.length() > 0) {
					nameBuilder.append(" ");
				}
				nameBuilder.append(param);
			}
			nameToken = nameBuilder.toString();
		}
		
		final String result = WarpDrive.starMap.find(nameToken);
		Commons.addChatMessage(commandSender, result);
	}
	
	@Override
	public String getCommandUsage(final ICommandSender commandSender) {
		return getCommandName() + " (<shipName>)"
		       + "\nshipName: name of the ship to find. Exact casing is preferred.";
	}
}

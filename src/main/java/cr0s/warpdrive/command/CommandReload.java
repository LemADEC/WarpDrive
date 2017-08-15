package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class CommandReload extends CommandBase {
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Override
	public String getCommandName() {
		return "wreload";
	}
	
	@Override
	public void processCommand(ICommandSender sender, String[] params) {
		if (sender == null) { return; } 
		
		WarpDriveConfig.reload();
		Commons.addChatMessage(sender, "§aWarpDrive configuration has been reloaded.\n§aUse at your own risk!");
	}
	
	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "/wreload";
	}
}

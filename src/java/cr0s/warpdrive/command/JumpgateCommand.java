package cr0s.warpdrive.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import cr0s.warpdrive.WarpDrive;

public class JumpgateCommand extends CommandBase {
	@Override
	public int getRequiredPermissionLevel() {
		return 4;
	}

	@Override
	public String getCommandName() {
		return "jumpgates";
	}

	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "Lists jumpgates";
	}

	@Override
	public void processCommand(ICommandSender icommandsender, String[] astring) {
		EntityPlayerMP player = (EntityPlayerMP) icommandsender;
		player.addChatMessage(new ChatComponentText(WarpDrive.jumpgates.commaList()));
	}
}

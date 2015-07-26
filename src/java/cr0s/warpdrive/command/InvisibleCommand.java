package cr0s.warpdrive.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.sun.media.jfxmedia.logging.Logger;

public class InvisibleCommand extends CommandBase {
	@Override
	public int getRequiredPermissionLevel() {
		return 4;
	}

	@Override
	public String getCommandName() {
		return "invisible";
	}

	@Override
	public void processCommand(ICommandSender icommandsender, String[] astring) {
		EntityPlayerMP player = (EntityPlayerMP) icommandsender;

		if (astring.length >= 1) {
			Logger.logMsg(Logger.INFO, "/invisible: setting invisible to " + astring[0]);
			// player =
			// MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername(astring[0]);
			// TODO: Fix
		}

		if (player == null) {
			return;
		}

		// Toggle invisibility
		player.setInvisible(!player.isInvisible());
	}

	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "/invisible [player]";
	}
}

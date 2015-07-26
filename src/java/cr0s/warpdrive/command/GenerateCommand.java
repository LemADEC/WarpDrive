package cr0s.warpdrive.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;

import com.sun.media.jfxmedia.logging.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;
import cr0s.warpdrive.world.JumpgateGenerator;
import cr0s.warpdrive.world.WorldGenSmallShip;
import cr0s.warpdrive.world.WorldGenStation;

/*
 *   /generate <structure>
 *   Possible structures:
 *   moon, ship, asteroid, astfield, gascloud, star
 */

public class GenerateCommand extends CommandBase {
	@Override
	public String getCommandName() {
		return "generate";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public String getCommandUsage(ICommandSender par1ICommandSender) {
		return "/" + getCommandName() + " <structure>\nPossible structures: moon, ship, asteroid, astfield, gascloud, star <class>, jumpgate <name>";
	}

	@Override
	public void processCommand(ICommandSender icommandsender, String[] params) {
		EntityPlayerMP player = (EntityPlayerMP) icommandsender;
		if (params.length > 0) {
			String struct = params[0];

			// Reject command, if player is not in space
			if (player.dimension != WarpDriveConfig.G_SPACE_DIMENSION_ID && (!"ship".equals(struct))) {
				player.addChatMessage(new ChatComponentText("* generate: this structure is only allowed in space!"));
				return;
			}

			int x = MathHelper.floor_double(player.posX);
			int y = MathHelper.floor_double(player.posY);
			int z = MathHelper.floor_double(player.posZ);

			if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
				if (struct.equals("moon")) {
					Logger.logMsg(Logger.INFO, "/generate: generating moon at " + x + ", " + (y - 16) + ", " + z);
					WarpDrive.instance.spaceWorldGenerator.generateMoon(player.worldObj, x, y - 16, z);
				} else if (struct.equals("ship")) {
					Logger.logMsg(Logger.INFO, "/generate: generating NPC ship at " + x + ", " + y + ", " + z);
					new WorldGenSmallShip(false).generate(player.worldObj, player.worldObj.rand, x, y, z);
				} else if (struct.equals("station")) {
					Logger.logMsg(Logger.INFO, "/generate: generating station at " + x + ", " + y + ", " + z);
					new WorldGenStation(false).generate(player.worldObj, player.worldObj.rand, x, y, z);
				} else if (struct.equals("asteroid")) {
					Logger.logMsg(Logger.INFO, "/generate: generating asteroid at " + x + ", " + (y - 10) + ", " + z);
					WarpDrive.instance.spaceWorldGenerator.generateRandomAsteroid(player.worldObj, x, y - 10, z, 6, 11);
				} else if (struct.equals("astfield")) {
					Logger.logMsg(Logger.INFO, "/generate: generating asteroid field at " + x + ", " + y + ", " + z);
					WarpDrive.instance.spaceWorldGenerator.generateAsteroidField(player.worldObj, x, y, z);
				} else if (struct.equals("gascloud")) {
					Logger.logMsg(Logger.INFO, "/generate: generating gas cloud at " + x + ", " + y + ", " + z);
					WarpDrive.instance.spaceWorldGenerator.generateGasCloudOfColor(player.worldObj, x, y, z, 15, 20, player.worldObj.rand.nextInt(12));
				} else if (struct.equals("star")) {
					Logger.logMsg(Logger.INFO, "/generate: generating star at " + x + ", " + y + ", " + z);
					Integer type = (params.length > 1) ? Integer.parseInt(params[1]) : -1; // Lem
					WarpDrive.instance.spaceWorldGenerator.generateStar(player.worldObj, x, y, z, type); // Lem
				} else if (struct.equals("jumpgate")) {
					if (params.length == 2) {
						Logger.logMsg(Logger.INFO, "/generate: creating jumpgate at " + x + ", " + y + ", " + z);

						if (WarpDrive.jumpgates.addGate(params[1], x, y, z)) {
							JumpgateGenerator.generate(player.worldObj, x, Math.min(y, 255 - JumpgateGenerator.GATE_SIZE_HALF - 1), z);
						} else {
							Logger.logMsg(Logger.INFO, "/generate: jumpgate '" + params[1] + "' already exists.");
						}
					}
				} else {
					player.addChatMessage(new ChatComponentText(getCommandUsage(icommandsender)));
				}
			}
		} else {
			player.addChatMessage(new ChatComponentText(getCommandUsage(icommandsender)));
		}
	}
}

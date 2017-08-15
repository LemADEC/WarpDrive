package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.StarMapRegistry;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.world.SpaceTeleporter;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

public class CommandSpace extends CommandBase {
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Override
	public String getCommandName() {
		return "space";
	}
	
	@Override
	public void processCommand(ICommandSender commandSender, String[] params) {
		if (commandSender == null) { return; } 
		MinecraftServer server = MinecraftServer.getServer();
		
		// set defaults
		int dimensionIdTarget = Integer.MAX_VALUE;
		
		EntityPlayerMP[] entityPlayerMPs = null;
		if (commandSender instanceof EntityPlayerMP) {
			entityPlayerMPs = new EntityPlayerMP[1];
			entityPlayerMPs[0] = (EntityPlayerMP) commandSender;
		}
		
		// parse arguments
		//noinspection StatementWithEmptyBody
		if (params.length == 0) {
			// nop
		} else if (params.length == 1) {
			if (params[0].equalsIgnoreCase("help") || params[0].equalsIgnoreCase("?")) {
				Commons.addChatMessage(commandSender, getCommandUsage(commandSender));
				return;
			}
			EntityPlayerMP[] entityPlayerMPs_found = Commons.getOnlinePlayerByNameOrSelector(commandSender, params[0]);
			if (entityPlayerMPs_found != null) {
				entityPlayerMPs = entityPlayerMPs_found;
			} else if (commandSender instanceof EntityPlayer) {
				dimensionIdTarget = StarMapRegistry.getDimensionId(params[0], (EntityPlayer) commandSender);
			} else {
				Commons.addChatMessage(commandSender, "§c/space: player not found '" + params[0] + "'");
				return;
			}
			
		} else if (params.length == 2) {
			EntityPlayerMP[] entityPlayerMPs_found = Commons.getOnlinePlayerByNameOrSelector(commandSender, params[0]);
			if (entityPlayerMPs_found != null) {
				entityPlayerMPs = entityPlayerMPs_found;
			} else {
				Commons.addChatMessage(commandSender, "§c/space: player not found '" + params[0] + "'");
				return;
			}
			dimensionIdTarget = StarMapRegistry.getDimensionId(params[1], entityPlayerMPs[0]);
			
		} else {
			Commons.addChatMessage(commandSender, "§c/space: too many arguments " + params.length);
			return;
		}
		
		// check player
		if (entityPlayerMPs == null || entityPlayerMPs.length <= 0) {
			Commons.addChatMessage(commandSender, "§c/space: undefined player");
			return;
		}
		
		for (EntityPlayerMP entityPlayerMP : entityPlayerMPs) {
			// toggle between overworld and space if no dimension was provided
			int xTarget = MathHelper.floor_double(entityPlayerMP.posX);
			int yTarget = Math.min(255, Math.max(0, MathHelper.floor_double(entityPlayerMP.posY)));
			int zTarget = MathHelper.floor_double(entityPlayerMP.posZ);
			final CelestialObject celestialObjectCurrent = CelestialObjectManager.get(entityPlayerMP.worldObj, (int) entityPlayerMP.posX, (int) entityPlayerMP.posZ);
			if (dimensionIdTarget == Integer.MAX_VALUE) {
				if (celestialObjectCurrent == null) {
					Commons.addChatMessage(commandSender, 
						String.format("§c/space: player %s is in unknown dimension %d.\n§bTry specifying an explicit target dimension instead.",
							    entityPlayerMP.getCommandSenderName(), entityPlayerMP.worldObj.provider.dimensionId));
					continue;
				}
				if ( celestialObjectCurrent.isSpace()
				  || celestialObjectCurrent.isHyperspace() ) {
					// in space or hyperspace => move to closest child
					final CelestialObject celestialObjectChild = CelestialObjectManager.getClosestChild(entityPlayerMP.worldObj, (int) entityPlayerMP.posX, (int) entityPlayerMP.posZ);
					if (celestialObjectChild == null) {
						dimensionIdTarget = 0;
					} else if (celestialObjectChild.isVirtual()) {
						Commons.addChatMessage(commandSender,
							String.format("§c/space: player %s can't go to %s.\n§cThis is a virtual celestial object.\n§bTry specifying an explicit target dimension instead.",
								entityPlayerMP.getCommandSenderName(), celestialObjectChild.getDisplayName()));
						continue;
					} else {
						dimensionIdTarget = celestialObjectChild.dimensionId;
						final VectorI vEntry = celestialObjectChild.getEntryOffset();
						xTarget += vEntry.x;
						yTarget += vEntry.y;
						zTarget += vEntry.z;
					}
				} else {
					// on a planet => move to space
					if ( celestialObjectCurrent.parent == null
					  || celestialObjectCurrent.parent.isVirtual() ) {
						dimensionIdTarget = 0;
						
					} else {
						dimensionIdTarget = celestialObjectCurrent.parent.dimensionId;
						final VectorI vEntry = celestialObjectCurrent.getEntryOffset();
						xTarget -= vEntry.x;
						yTarget -= vEntry.y;
						zTarget -= vEntry.z;
					}
				}
				
			} else {
				// adjust offset when it's directly above or below us
				if ( celestialObjectCurrent != null
				  && celestialObjectCurrent.parent != null
				  && celestialObjectCurrent.parent.dimensionId == dimensionIdTarget ) {// moving to parent explicitly
					final VectorI vEntry = celestialObjectCurrent.getEntryOffset();
					xTarget -= vEntry.x;
					yTarget -= vEntry.y;
					zTarget -= vEntry.z;
				} else {
					final CelestialObject celestialObjectChild = CelestialObjectManager.getClosestChild(entityPlayerMP.worldObj, (int) entityPlayerMP.posX, (int) entityPlayerMP.posZ);
					if ( celestialObjectChild != null
					  && celestialObjectChild.dimensionId == dimensionIdTarget ) {// moving to child explicitly
						final VectorI vEntry = celestialObjectChild.getEntryOffset();
						xTarget += vEntry.x;
						yTarget += vEntry.y;
						zTarget += vEntry.z;
					}
				}
			}
			
			// get target celestial object
			final CelestialObject celestialObjectTarget = CelestialObjectManager.get(false, dimensionIdTarget, xTarget, zTarget);
			
			// force to center if we're outside the border
			if ( celestialObjectTarget != null
			  && !celestialObjectTarget.isInsideBorder(xTarget, zTarget) ) {
				// outside 
				xTarget = celestialObjectTarget.dimensionCenterX;
				zTarget = celestialObjectTarget.dimensionCenterZ;
			}
			
			// get target world
			final WorldServer worldTarget = server.worldServerForDimension(dimensionIdTarget);
			if (worldTarget == null) {
				Commons.addChatMessage(commandSender, "§c/space: undefined dimension '" + dimensionIdTarget + "'");
				continue;
			}
			
			// inform player
			String message = "§aTeleporting player " + entityPlayerMP.getCommandSenderName() + " to dimension " + dimensionIdTarget + "..."; // + ":" + worldTarget.getWorldInfo().getWorldName();
			Commons.addChatMessage(commandSender, message);
			WarpDrive.logger.info(message);
			if (commandSender != entityPlayerMP) {
				Commons.addChatMessage(entityPlayerMP, commandSender.getCommandSenderName() + " is teleporting you to dimension " + dimensionIdTarget); // + ":" + worldTarget.getWorldInfo().getWorldName());
			}
			
			// find a good spot
			
			if ( (worldTarget.isAirBlock(xTarget, yTarget - 1, zTarget) && !entityPlayerMP.capabilities.allowFlying)
			  || !worldTarget.isAirBlock(xTarget, yTarget, zTarget)
			  || !worldTarget.isAirBlock(xTarget, yTarget + 1, zTarget) ) {// non solid ground and can't fly, or inside blocks
				yTarget = worldTarget.getTopSolidOrLiquidBlock(xTarget, zTarget) + 1;
				if (yTarget == 0) {
					yTarget = 128;
				} else {
					for (int safeY = yTarget - 3; safeY > Math.max(1, yTarget - 20); safeY--) {
						if (!worldTarget.isAirBlock(xTarget, safeY - 1, zTarget)
						  && worldTarget.isAirBlock(xTarget, safeY    , zTarget)
						  && worldTarget.isAirBlock(xTarget, safeY + 1, zTarget)) {
							yTarget = safeY;
							break;
						}
					}
				}
			}
			
			// actual teleportation
			final SpaceTeleporter teleporter = new SpaceTeleporter(worldTarget, 0, xTarget, yTarget, zTarget);
			server.getConfigurationManager().transferPlayerToDimension(entityPlayerMP, dimensionIdTarget, teleporter);
			entityPlayerMP.setPositionAndUpdate(xTarget + 0.5D, yTarget + 0.2D, zTarget + 0.5D);
			entityPlayerMP.sendPlayerAbilities();
		}
	}
	
	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "/space (<playerName>) ([overworld|nether|end|theend|space|hyper|hyperspace|<dimensionId>])";
	}
	
}

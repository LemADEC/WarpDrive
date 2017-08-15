package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.structures.AbstractStructure;
import cr0s.warpdrive.config.structures.StructureManager;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.world.JumpgateGenerator;
import cr0s.warpdrive.world.WorldGenSmallShip;
import cr0s.warpdrive.world.WorldGenStation;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import cpw.mods.fml.common.FMLCommonHandler;

public class CommandGenerate extends CommandBase {
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
	public void processCommand(ICommandSender commandSender, String[] params) {
		World world = commandSender.getEntityWorld();
		ChunkCoordinates coordinates = commandSender.getPlayerCoordinates();
		
		if (world == null || coordinates == null) {
			Commons.addChatMessage(commandSender, "* generate: unknown world or coordinates, probably an invalid command sender in action here.");
			return;
		}
		int x = coordinates.posX;
		int y = coordinates.posY;
		int z = coordinates.posZ;
		
		if (params.length <= 0 || params.length == 3 || params.length > 5) {
			Commons.addChatMessage(commandSender, getCommandUsage(commandSender));
			return;
		}
		
		if (params.length > 3) {
			x = AdjustAxis(x, params[params.length - 3]);
			y = AdjustAxis(y, params[params.length - 2]);
			z = AdjustAxis(z, params[params.length - 1]);
		}
		
		String structure = params[0];
		
		// Reject command, if player is not in space
		if (!CelestialObjectManager.isInSpace(world, x, z) && (!"ship".equals(structure))) {
			Commons.addChatMessage(commandSender, "* generate: this structure is only allowed in space!");
			return;
		}
		
		if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
			String name = (params.length > 1) ? params[1] : null;
			switch (structure) {
				case "ship":
					WarpDrive.logger.info("/generate: generating NPC ship at " + x + ", " + y + ", " + z);
					new WorldGenSmallShip(false, true).generate(world, world.rand, x, y, z);
					break;
				case "station":
					WarpDrive.logger.info("/generate: generating station at " + x + ", " + y + ", " + z);
					new WorldGenStation(false).generate(world, world.rand, x, y, z);
					break;
				case "asteroid":
					generateStructure(commandSender, StructureManager.GROUP_ASTEROIDS, name, world, x, y - 10, z);
					break;
				case "astfield":
					generateStructure(commandSender, StructureManager.GROUP_ASTEROIDS_FIELDS, name, world, x, y, z);
					break;
				case "gascloud":
					generateStructure(commandSender, StructureManager.GROUP_GAS_CLOUDS, name, world, x, y, z);
					break;
				case "moon":
					generateStructure(commandSender, StructureManager.GROUP_MOONS, name, world, x, y - 16, z);
					break;
				case "star":
					generateStructure(commandSender, StructureManager.GROUP_STARS, name, world, x, y, z);
					break;
				case "jumpgate":
					if (params.length != 2) {
						Commons.addChatMessage(commandSender, "Missing jumpgate name");
					} else {
						WarpDrive.logger.info("/generate: creating jumpgate at " + x + ", " + y + ", " + z);

						if (WarpDrive.jumpgates.addGate(params[1], x, y, z)) {
							JumpgateGenerator.generate(world, x, Math.min(y, 255 - JumpgateGenerator.GATE_SIZE_HALF - 1), z);
						} else {
							WarpDrive.logger.info("/generate: jumpgate '" + params[1] + "' already exists.");
						}
					}
					break;
				default:
					Commons.addChatMessage(commandSender, getCommandUsage(commandSender));
					break;
			}
		}
	}
	
	private int AdjustAxis(final int axis, final String param) {
		if (param.isEmpty() || param.equals("~")) {
			return axis;
		}
		
		if (param.charAt(0) == '~') {
			return axis + Integer.parseInt(param.substring(1));
		} else {
			return Integer.parseInt(param);
		}
	}
	
	private void generateStructure(ICommandSender commandSender, final String group, final String name, World world, final int x, final int y, final int z) {
		AbstractStructure structure = StructureManager.getStructure(world.rand, group, name);
		if (structure == null) {
			Commons.addChatMessage(commandSender, "Invalid " + group + " '" + name + "', try one of the followings:\n" + StructureManager.getStructureNames(group));
		} else {
			WarpDrive.logger.info("/generate: Generating " + group + ":" + structure.getName() + " at " + x + " " + y + " " + z);
			structure.generate(world, world.rand, x, y, z);
			
			// do a weak attempt to extract player (ideally, it should be delayed after generation, but that's too complicated)
			if (commandSender instanceof EntityPlayerMP) {
				int newY = y + 1;
				while (newY < 256 && !world.isAirBlock(x, newY, z)) {
					newY++;
				}
				EntityPlayerMP player = (EntityPlayerMP)commandSender;
				player.setPosition(player.posX, newY, player.posZ);
			}
		}
	}
}

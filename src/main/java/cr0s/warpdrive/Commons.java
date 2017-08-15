package cr0s.warpdrive;

import cr0s.warpdrive.data.VectorI;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Common static methods
 */
public class Commons {
	
	private static final String CHAR_FORMATTING = "" + (char)167;
	
	public static String updateEscapeCodes(final String message) {
		return message
		       .replace("§", CHAR_FORMATTING)
		       .replace("\\n", "\n")
		       .replace(CHAR_FORMATTING + "r", CHAR_FORMATTING + "7")
		       .replaceAll("\u00A0", " ");  // u00A0 is 'NO-BREAK SPACE'
	}
	
	public static String removeFormatting(final String message) {
		return updateEscapeCodes(message)
		       .replaceAll("(" + CHAR_FORMATTING + ".)", "");
	}
	
	private static boolean isFormatColor(char chr) {
		return chr >= 48 && chr <= 57
		    || chr >= 97 && chr <= 102
		    || chr >= 65 && chr <= 70;
	}
	
	private static boolean isFormatSpecial(char chr) {
		return chr >= 107 && chr <= 111
		    || chr >= 75 && chr <= 79
		    || chr == 114
		    || chr == 82;
	}
	
	// inspired by FontRender.getFormatFromString
	private static String getFormatFromString(final String message) {
		final int indexLastChar = message.length() - 1;
		StringBuilder result = new StringBuilder();
		int indexEscapeCode = -1;
		while ((indexEscapeCode = message.indexOf(167, indexEscapeCode + 1)) != -1) {
			if (indexEscapeCode < indexLastChar) {
				final char chr = message.charAt(indexEscapeCode + 1);
				
				if (isFormatColor(chr)) {
					result = new StringBuilder("\u00a7" + chr);
				} else if (isFormatSpecial(chr)) {
					result.append("\u00a7").append(chr);
				}
			}
		}
		
		return result.toString();
	}
	
	public static void addChatMessage(final ICommandSender commandSender, final String message) {
		if (commandSender == null) {
			WarpDrive.logger.error("Unable to send message to NULL commandSender: " + message);
			return;
		}
		final String[] lines = updateEscapeCodes(message).split("\n");
		String format = "";
		getFormatFromString(lines[0]);
		for (String line : lines) {
			final String formattedLine = format + line;
			commandSender.addChatMessage(new ChatComponentText(formattedLine));
			format = getFormatFromString(formattedLine);
		}
		
		// logger.info(message);
	}
	
	// add tooltip information with text formatting and line splitting
	// will ensure it fits on minimum screen width
	public static void addTooltip(final List<String> list, final String tooltip) {
		final String[] split = updateEscapeCodes(tooltip).split("\n");
		for (String line : split) {
			String lineRemaining = line;
			String formatNextLine = "";
			while (!lineRemaining.isEmpty()) {
				int indexToCut = formatNextLine.length();
				int displayLength = 0;
				int length = lineRemaining.length();
				while (indexToCut < length && displayLength <= 38) {
					if (lineRemaining.charAt(indexToCut) == (char) 167 && indexToCut + 1 < length) {
						indexToCut++;
					} else {
						displayLength++;
					}
					indexToCut++;
				}
				if (indexToCut < length) {
					indexToCut = lineRemaining.substring(0, indexToCut).lastIndexOf(' ');
					if (indexToCut == -1 || indexToCut == 0) {// no space available, show the whole line 'as is'
						list.add(lineRemaining);
						lineRemaining = "";
					} else {// cut at last space
						list.add(lineRemaining.substring(0, indexToCut).replaceAll("\u00A0", " "));
						
						// compute remaining format
						int index = formatNextLine.length();
						while (index <= indexToCut) {
							if (lineRemaining.charAt(index) == (char) 167 && index + 1 < indexToCut) {
								index++;
								formatNextLine += CHAR_FORMATTING + lineRemaining.charAt(index);
							}
							index++;
						}
						
						// cut for next line, recovering current format
						lineRemaining = formatNextLine + " " + lineRemaining.substring(indexToCut + 1);
					}
				} else {
					list.add(lineRemaining.replaceAll("\u00A0", " "));
					lineRemaining = "";
				}
			}
		}
	}
	
	public static Field getField(Class<?> clazz, String deobfuscatedName, String obfuscatedName) {
		Field fieldToReturn = null;
		
		try {
			fieldToReturn = clazz.getDeclaredField(deobfuscatedName);
		} catch (Exception exception1) {
			try {
				fieldToReturn = clazz.getDeclaredField(obfuscatedName);
			} catch (Exception exception2) {
				exception2.printStackTrace();
				String map = "";
				for(Field fieldDeclared : clazz.getDeclaredFields()) {
					if (!map.isEmpty()) {
						map += ", ";
					}
					map += fieldDeclared.getName();
				}
				WarpDrive.logger.error(String.format("Unable to find %1$s field in %2$s class. Available fields are: %3$s",
						deobfuscatedName, clazz.toString(), map));
			}
		}
		if (fieldToReturn != null) {
			fieldToReturn.setAccessible(true);
		}
		return fieldToReturn;
	}
	
	public static String format(final long value) {
		// alternate: BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_EVEN).toPlainString(),
		return String.format("%,d", Math.round(value));
	}
	
	public static String format(final Object[] arguments) {
		final StringBuilder result = new StringBuilder();
		if (arguments != null && arguments.length > 0) {
			for (final Object argument : arguments) {
				if (result.length() > 0) {
					result.append(", ");
				}
				if (argument instanceof String) {
					result.append("\"").append(argument).append("\"");
				} else {
					result.append(argument);
				}
			}
		}
		return result.toString();
	}
	
	public static ItemStack copyWithSize(ItemStack itemStack, int newSize) {
		final ItemStack ret = itemStack.copy();
		ret.stackSize = newSize;
		return ret;
	}
	
	public static Collection<IInventory> getConnectedInventories(TileEntity tileEntityConnection) {
		final Collection<IInventory> result = new ArrayList<>(6);
		
		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
			final TileEntity tileEntity = tileEntityConnection.getWorldObj().getTileEntity(
				tileEntityConnection.xCoord + side.offsetX, tileEntityConnection.yCoord + side.offsetY, tileEntityConnection.zCoord + side.offsetZ);
			if (tileEntity != null && (tileEntity instanceof IInventory)) {
				result.add((IInventory) tileEntity);
				
				if (tileEntity instanceof TileEntityChest) {
					final TileEntityChest tileEntityChest = (TileEntityChest) tileEntity;
					tileEntityChest.checkForAdjacentChests();
					if (tileEntityChest.adjacentChestXNeg != null) {
						result.add(tileEntityChest.adjacentChestXNeg);
					} else if (tileEntityChest.adjacentChestXPos != null) {
						result.add(tileEntityChest.adjacentChestXPos);
					} else if (tileEntityChest.adjacentChestZNeg != null) {
						result.add(tileEntityChest.adjacentChestZNeg);
					} else if (tileEntityChest.adjacentChestZPos != null) {
						result.add(tileEntityChest.adjacentChestZPos);
					}
				}
			}
		}
		return result;
	}
	
	
	// searching methods
	
	public static final ForgeDirection[] UP_DIRECTIONS = { ForgeDirection.UP, ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST };
	public static final ForgeDirection[] HORIZONTAL_DIRECTIONS = { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST };
	public static final ForgeDirection[] VERTICAL_DIRECTIONS = { ForgeDirection.UP, ForgeDirection.DOWN };
	
	public static Set<VectorI> getConnectedBlocks(World world, final VectorI start, final ForgeDirection[] directions, final Set<Block> whitelist, final int maxRange, final VectorI... ignore) {
		return getConnectedBlocks(world, Collections.singletonList(start), directions, whitelist, maxRange, ignore);
	}
	
	public static Set<VectorI> getConnectedBlocks(World world, final Collection<VectorI> start, final ForgeDirection[] directions, final Set<Block> whitelist, final int maxRange, final VectorI... ignore) {
		final Set<VectorI> toIgnore = new HashSet<>();
		if (ignore != null) {
			toIgnore.addAll(Arrays.asList(ignore));
		}
		
		Set<VectorI> toIterate = new HashSet<>();
		toIterate.addAll(start);
		
		Set<VectorI> toIterateNext;
		
		final Set<VectorI> iterated = new HashSet<>();
		
		int range = 0;
		while(!toIterate.isEmpty() && range < maxRange) {
			toIterateNext = new HashSet<>();
			for (VectorI current : toIterate) {
				if (whitelist.contains(current.getBlock_noChunkLoading(world))) {
					iterated.add(current);
				}
				
				for(ForgeDirection direction : directions) {
					VectorI next = current.clone(direction);
					if (!iterated.contains(next) && !toIgnore.contains(next) && !toIterate.contains(next) && !toIterateNext.contains(next)) {
						if (whitelist.contains(next.getBlock_noChunkLoading(world))) {
							toIterateNext.add(next);
						}
					}
				}
			}
			toIterate = toIterateNext;
			range++;
		}
		
		return iterated;
	}
	
	
	// data manipulation methods
	
	public static int toInt(double d) {
		return (int) Math.round(d);
	}
	
	public static int toInt(Object object) {
		return toInt(toDouble(object));
	}
	
	public static double toDouble(Object object) {
		assert(!(object instanceof Object[]));
		return Double.parseDouble(object.toString());
	}
	
	public static float toFloat(Object object) {
		assert(!(object instanceof Object[]));
		return Float.parseFloat(object.toString());
	}
	
	public static boolean toBool(Object object) {
		if (object == null) {
			 return false;
		}
		assert(!(object instanceof Object[]));
		if (object instanceof Boolean) {
			 return ((Boolean) object);
		}
		final String string = object.toString();
		return string.equals("true") || string.equals("1.0") || string.equals("1") || string.equals("y") || string.equals("yes");
	}
	
	public static int clamp(final int min, final int max, final int value) {
		return Math.min(max, Math.max(value, min));
	}
	
	public static long clamp(final long min, final long max, final long value) {
		return Math.min(max, Math.max(value, min));
	}
	
	public static float clamp(final float min, final float max, final float value) {
		return Math.min(max, Math.max(value, min));
	}
	
	public static double clamp(final double min, final double max, final double value) {
		return Math.min(max, Math.max(value, min));
	}
	
	// clamping while keeping the sign
	public static float clampMantisse(final float min, final float max, final float value) {
		return Math.min(max, Math.max(Math.abs(value), min)) * Math.signum(value == 0.0F ? 1.0F : value);
	}
	
	// clamping while keeping the sign
	public static double clampMantisse(final double min, final double max, final double value) {
		return Math.min(max, Math.max(Math.abs(value), min)) * Math.signum(value == 0.0D ? 1.0D : value);
	}
	
	public static int randomRange(Random random, final int min, final int max) {
		return min + ((max - min > 0) ? random.nextInt(max - min + 1) : 0);
	}
	
	public static double randomRange(Random random, final double min, final double max) {
		return min + ((max - min > 0) ? random.nextDouble() * (max - min) : 0);
	}
	
	
	// configurable interpolation
	
	public static double interpolate(final double[] xValues, final double[] yValues, final double xInput) {
		if (WarpDrive.isDev) {
			assert(xValues.length == yValues.length);
			assert(xValues.length > 1);
		}
		
		// clamp to minimum
		if (xInput < xValues[0]) {
			return yValues[0];
		}
		
		for(int index = 0; index < xValues.length - 1; index++) {
			if (xInput < xValues[index + 1]) {
				return interpolate(xValues[index], yValues[index], xValues[index + 1], yValues[index + 1], xInput);
			}
		}
		
		// clamp to maximum
		return yValues[yValues.length - 1];
	}
	
	private static double interpolate(final double xMin, final double yMin, final double xMax, final double yMax, final double x) {
		return yMin + (x - xMin) * (yMax - yMin) / (xMax - xMin);
	}
	
	public static ForgeDirection getHorizontalDirectionFromEntity(final EntityLivingBase entityLiving) {
		if (entityLiving != null) {
			final int direction = Math.round(entityLiving.rotationYaw / 90.0F) & 3;
			switch (direction) {
			default:
			case 0:
				return ForgeDirection.NORTH;
			case 1:
				return ForgeDirection.EAST;
			case 2:
				return ForgeDirection.SOUTH;
			case 3:
				return ForgeDirection.WEST;
			}
		}
		return ForgeDirection.NORTH;
	}
	
	public static int getFacingFromEntity(final EntityLivingBase entityLiving) {
		if (entityLiving != null) {
			int metadata;
			if (entityLiving.rotationPitch > 65) {
				metadata = 1;
			} else if (entityLiving.rotationPitch < -65) {
				metadata = 0;
			} else {
				int direction = Math.round(entityLiving.rotationYaw / 90.0F) & 3;
				switch (direction) {
					case 0:
						metadata = 2;
						break;
					case 1:
						metadata = 5;
						break;
					case 2:
						metadata = 3;
						break;
					case 3:
						metadata = 4;
						break;
					default:
						metadata = 2;
						break;
				}
			}
			return metadata;
		}
		return 0;
	}
	
	public static boolean isSafeThread() {
		final String name = Thread.currentThread().getName();
		return name.equals("Server thread") || name.equals("Client thread");
	}
	
	// loosely inspired by crunchify
	public static void dumpAllThreads() {
		final StringBuilder stringBuilder = new StringBuilder();
		final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
		for (ThreadInfo threadInfo : threadInfos) {
			stringBuilder.append('"');
			stringBuilder.append(threadInfo.getThreadName());
			stringBuilder.append("\"\n\tjava.lang.Thread.State: ");
			stringBuilder.append(threadInfo.getThreadState());
			final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
			for (final StackTraceElement stackTraceElement : stackTraceElements) {
				stringBuilder.append("\n\t\tat ");
				stringBuilder.append(stackTraceElement);
			}
			stringBuilder.append("\n\n");
		}
		WarpDrive.logger.error(stringBuilder.toString());
	}
	
	public static void writeNBTToFile(final String fileName, final NBTTagCompound tagCompound) {
		if (WarpDrive.isDev) {
			WarpDrive.logger.info("writeNBTToFile " + fileName);
		}
		
		try {
			final File file = new File(fileName);
			if (!file.exists()) {
				//noinspection ResultOfMethodCallIgnored
				file.createNewFile();
			}
			
			final FileOutputStream fileoutputstream = new FileOutputStream(file);
			
			CompressedStreamTools.writeCompressed(tagCompound, fileoutputstream);
			
			fileoutputstream.close();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	public static NBTTagCompound readNBTFromFile(final String fileName) {
		if (WarpDrive.isDev) {
			WarpDrive.logger.info("readNBTFromFile " + fileName);
		}
		
		try {
			final File file = new File(fileName);
			if (!file.exists()) {
				return null;
			}
			
			final FileInputStream fileinputstream = new FileInputStream(file);
			final NBTTagCompound tagCompound = CompressedStreamTools.readCompressed(fileinputstream);
			
			fileinputstream.close();
			
			return tagCompound;
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		
		return null;
	}
	
	public static EntityPlayerMP[] getOnlinePlayerByNameOrSelector(ICommandSender sender, final String playerNameOrSelector) {
		@SuppressWarnings("unchecked")
		List<EntityPlayer> onlinePlayers = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
		for (EntityPlayer onlinePlayer : onlinePlayers) {
			if (onlinePlayer.getCommandSenderName().equalsIgnoreCase(playerNameOrSelector) && onlinePlayer instanceof EntityPlayerMP) {
				return new EntityPlayerMP[]{ (EntityPlayerMP)onlinePlayer };
			}
		}
		
		EntityPlayerMP[] entityPlayerMPs_found = PlayerSelector.matchPlayers(sender, playerNameOrSelector);
		if (entityPlayerMPs_found != null && entityPlayerMPs_found.length > 0) {
			return entityPlayerMPs_found.clone();
		}
		
		return null;
	}
	
	public static int colorARGBtoInt(final int alpha, final int red, final int green, final int blue) {
		return (clamp(0, 255, alpha) << 24)
		     + (clamp(0, 255, red  ) << 16)
			 + (clamp(0, 255, green) <<  8)
			 +  clamp(0, 255, blue );
	}
}

package cr0s.warpdrive;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public class MetaRotations {
	private File metaRotationsDirectory;
	// TODO: Test. Stores bblock with unlocalized name as key
	public Map<String, BlockMetaRotation> metaRotationMap = new HashMap<String, BlockMetaRotation>();

	public int getRotatedMeta(int block, int meta, int rotate) {
		if (rotate == 0) {
			return meta;
		} else {
			BlockMetaRotation rotation = metaRotationMap.get(Integer.valueOf(block));
			return rotation == null ? meta : rotation.getRotatedMeta(meta, rotate);
		}
	}

	public void addMetaRotation(Block block, int bitmask, int... metarotation) {
		this.metaRotationMap.put(block.getUnlocalizedName(), new BlockMetaRotation(block, metarotation, bitmask));
	}

	public void parseMetaRotations(BufferedReader reader) throws IOException {
		int lineno = 0;
		String line;

		while ((line = reader.readLine()) != null) {
			++lineno;

			if (!line.startsWith("#") && line.length() != 0) {
				int mask = -1;
				int[] rot = new int[4];
				String[] as = line.split(";");

				if (as.length < 3) {
					System.out.println("Corruption in metarotation file at line " + lineno + "(Not enough parameters)");
				} else {
					String[] block = as[0].split(",");
					int[] ids = new int[block.length];

					for (int arr$ = 0; arr$ < block.length; ++arr$) {
						try {
							ids[arr$] = Integer.parseInt(block[arr$].trim());
						} catch (NumberFormatException var17) {
							String i$ = block[arr$].trim();
							Block[] blockid = Block.blocksList;
							int len$1 = blockid.length;

							for (int i$1 = 0; i$1 < len$1; ++i$1) {
								Block b = blockid[i$1];

								if (b != null && b.getUnlocalizedName().toLowerCase().equals("tile.".concat(i$.toLowerCase()))) {
									ids[arr$] = b;
								}
							}
						}
					}

					int len$;

					try {
						mask = Integer.decode(as[1].trim()).intValue();
						String[] var19 = as[2].split(",");

						for (len$ = 0; len$ < rot.length; ++len$) {
							rot[len$] = Integer.parseInt(var19[len$].trim());
						}
					} catch (NumberFormatException var16) {
						System.out.println("Curruption in metarotation file at line " + lineno + " (" + var16.getLocalizedMessage() + ")");
					}

					int[] var18 = ids;
					len$ = ids.length;

					for (int var20 = 0; var20 < len$; ++var20) {
						int var21 = var18[var20];
						this.addMetaRotation(var21, mask, rot);
					}
				}
			}
		}
	}

	public void setConfigDirectory(File configdirectory) {
		this.metaRotationsDirectory = new File(configdirectory, "WarpDrive");

		if (!this.metaRotationsDirectory.isDirectory()) {
			this.metaRotationsDirectory.mkdirs();
		}
	}

	public void readMetaRotationFiles() {
		if (this.metaRotationsDirectory == null) {
			throw new NullPointerException("Config folder has not been initialized");
		} else {
			this.metaRotationMap.clear();
			File defaultfile = new File(this.metaRotationsDirectory, "default.mrot");
			BufferedWriter writer = null;

			try {
				defaultfile.createNewFile();
				writer = new BufferedWriter(new FileWriter(defaultfile));
				writer.write("#----------------#\n");
				writer.write("# VANILLA BLOCKS #\n");
				writer.write("#----------------#\n");
				writer.write("# Default vanilla block meta rotations\n");
				writer.write("# This file will be overwritten every start, changes will not be implemented!\n");
				writer.write("# blocknames/blockIDs; bitmask; 4 metadata values in the clockwise rotation order\n");
				writer.write("\n");
				writer.write("# Pumpkin & Lantern\n");
				writer.write("86, 87; 0x3; 0, 1, 2, 3;\n");
				writer.write("\n");
				writer.write("# Stairs\n");
				writer.write("53, 67, 108, 109, 114, 128, 134, 135, 136, 156; 0x3; 2, 1, 3, 0;\n");
				writer.write("\n");
				writer.write("# Torches, levers and buttons\n");
				writer.write("50, 69, 75, 76, 77, 143; 0x7; 4, 1, 3, 2;\n");
				writer.write("\n");
				writer.write("# Sign\n");
				writer.write("68; 0x7; 3, 4, 2, 5;\n");
				writer.write("\n");
				writer.write("# Log\n");
				writer.write("17; 0xC; 4, 8, 4, 8;\n");
				writer.write("\n");
				writer.write("# Quarts pillar\n");
				writer.write("155; 0x7; 3, 4, 3, 4;\n");
				writer.write("\n");
				writer.write("# Ladder\n");
				writer.write("65; 0x7; 3, 4, 2, 5;\n");
				writer.write("\n# Fence gate\n");
				writer.write("107; 0x3; 0, 1, 2, 3;\n");
				writer.write("\n# Furnace, dispenser, chest\n");
				writer.write("61, 62, 23, 54; 0x7; 2, 5, 3, 4;\n");
				writer.write("\n# Redstone repeater\n");
				writer.write("93, 94; 0x3; 0, 1, 2, 3;\n");
				writer.write("\n# Doors\n");
				writer.write("64, 71; 0x3; 0, 1, 2, 3;\n");
				writer.write("\n# Bed\n");
				writer.write("26; 0x3; 0, 1, 2, 3;\n");
				writer.write("\n# AS stuff\n");
				writer.write("3769, 3779, 3772; 0x3; 2, 1, 3, 0;\n");
			} catch (IOException var33) {
				var33.printStackTrace();
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException var30) {
						var30.printStackTrace();
					}
				}
			}

			File[] files = this.metaRotationsDirectory.listFiles(new MetaRotations$1(this));
			File[] arr$ = files;
			int len$ = files.length;

			for (int i$ = 0; i$ < len$; ++i$) {
				File f = arr$[i$];

				try {
					this.readMetaRotationFile(f);
				} catch (IOException var34) {
					var34.printStackTrace();
				}
			}
		}
	}

	public void readMetaRotationFile(File file) throws IOException {
		System.out.println("Reading metarotation file " + file.getAbsolutePath());
		BufferedReader reader = new BufferedReader(new FileReader(file));
		this.parseMetaRotations(reader);
		reader.close();
	}

	public static int rotate90(int type, int parData) {
		int data = parData;
		switch (type) {
		case BlmockID.MINECART_TRACKS:
			switch (data) {
			case 6:
				return 7;
			case 7:
				return 8;
			case 8:
				return 9;
			case 9:
				return 6;
			}
			/* FALL-THROUGH */

		case BlockID.POWERED_RAIL:
		case BlockID.DETECTOR_RAIL:
		case BlockID.ACTIVATOR_RAIL:
			switch (data & 0x7) {
			case 0:
				return 1 | (data & ~0x7);
			case 1:
				return 0 | (data & ~0x7);
			case 2:
				return 5 | (data & ~0x7);
			case 3:
				return 4 | (data & ~0x7);
			case 4:
				return 2 | (data & ~0x7);
			case 5:
				return 3 | (data & ~0x7);
			}
			break;

		case BlockID.WOODEN_STAIRS:
		case BlockID.COBBLESTONE_STAIRS:
		case BlockID.BRICK_STAIRS:
		case BlockID.STONE_BRICK_STAIRS:
		case BlockID.NETHER_BRICK_STAIRS:
		case BlockID.SANDSTONE_STAIRS:
		case BlockID.SPRUCE_WOOD_STAIRS:
		case BlockID.BIRCH_WOOD_STAIRS:
		case BlockID.JUNGLE_WOOD_STAIRS:
		case BlockID.QUARTZ_STAIRS:
			switch (data) {
			case 0:
				return 2;
			case 1:
				return 3;
			case 2:
				return 1;
			case 3:
				return 0;
			case 4:
				return 6;
			case 5:
				return 7;
			case 6:
				return 5;
			case 7:
				return 4;
			}
			break;

		case BlockID.COCOA_PLANT:
		case BlockID.TRIPWIRE_HOOK:
			int extra = data & ~0x3;
			int withoutFlags = data & 0x3;
			switch (withoutFlags) {
			case 0:
				return 1 | extra;
			case 1:
				return 2 | extra;
			case 2:
				return 3 | extra;
			case 3:
				return 0 | extra;
			}
			break;

		case BlockID.SIGN_POST:
			return (data + 4) % 16;

		case BlockID.LADDER:
		case BlockID.WALL_SIGN:
		case BlockID.FURNACE:
		case BlockID.BURNING_FURNACE:
		case BlockID.ENDER_CHEST:
		case BlockID.TRAPPED_CHEST:
		case BlockID.HOPPER:
			switch (data) {
			case 2:
				return 5;
			case 3:
				return 4;
			case 4:
				return 2;
			case 5:
				return 3;
			}
			break;

		case BlockID.DROPPER:
			int dispPower = data & 0x8;
			switch (data & ~0x8) {
			case 2:
				return 5 | dispPower;
			case 3:
				return 4 | dispPower;
			case 4:
				return 2 | dispPower;
			case 5:
				return 3 | dispPower;
			}
			break;

		case BlockID.LOG:
			if (data >= 4 && data <= 11)
				data ^= 0xc;
			break;

		case BlockID.COMPARATOR_OFF:
		case BlockID.COMPARATOR_ON:
			int dir = data & 0x03;
			int delay = data - dir;
			switch (dir) {
			case 0:
				return 1 | delay;
			case 1:
				return 2 | delay;
			case 2:
				return 3 | delay;
			case 3:
				return 0 | delay;
			}
			break;

		case BlockID.TRAP_DOOR:
			int withoutOrientation = data & ~0x3;
			int orientation = data & 0x3;
			switch (orientation) {
			case 0:
				return 3 | withoutOrientation;
			case 1:
				return 2 | withoutOrientation;
			case 2:
				return 0 | withoutOrientation;
			case 3:
				return 1 | withoutOrientation;
			}
			break;

		case BlockID.PISTON_BASE:
		case BlockID.PISTON_STICKY_BASE:
		case BlockID.PISTON_EXTENSION:
			final int rest = data & ~0x7;
			switch (data & 0x7) {
			case 2:
				return 5 | rest;
			case 3:
				return 4 | rest;
			case 4:
				return 2 | rest;
			case 5:
				return 3 | rest;
			}
			break;

		case BlockID.BROWN_MUSHROOM_CAP:
		case BlockID.RED_MUSHROOM_CAP:
			if (data >= 10)
				return data;
			return (data * 3) % 10;

		case BlockID.VINE:
			return ((data << 1) | (data >> 3)) & 0xf;

		case BlockID.FENCE_GATE:
			return ((data + 1) & 0x3) | (data & ~0x3);

		case BlockID.ANVIL:
			return data ^ 0x1;

		case BlockID.HAY_BLOCK:
			if (data == 4)
				return 8;
			else if (data == 8)
				return 4;
			else
				return 0; // sanitize extraneous data values since hay blocks
			// are weird

		}

		return data;
	}

	public static boolean examForIC2Machine(TileEntity te) {
		if (te == null) {
			return false;
		}

		// System.out.println("Testing for IC2 machine " +
		// te.getClass().getSimpleName());
		Class c = te.getClass().getSuperclass();
		// System.out.println("Superclass name: " + c.getName());
		return (c == null) ? false
				: (c.getName().contains("ic2.core.block.generator.tileentity") || c.getName().contains("ic2.core.block.wiring.TileEntity") || c.getName()
						.contains("ic2.core.block.machine.tileentity"));
	}

	public static short rotateIC2MachineFacing90Reverse(short facing) {
		switch (facing) // 3 5 2 4
		{
		case 3:
			return 5;

		case 5:
			return 2;

		case 2:
			return 4;

		case 4:
			return 3;
		}
		return facing;
	}

	public static boolean examForAEMachine(TileEntity te) {
		if (te == null) {
			return false;
		}

		// System.out.println("Testing for AE machine " +
		// te.getClass().getSimpleName());
		Class c = te.getClass().getSuperclass();
		// System.out.println("Superclass name: " + c.getName());
		return (c == null) ? false : (c.getName().contains("appeng.me.basetiles") || c.getName().contains("appeng.me.tile") || c.getName().contains(
				"appeng.common.base.AppEngTile"));
	}

	public static int rotateAEMachineFacing90Reverse(int facing) {
		switch (facing) // 0 4 2 1
		{
		case 0:
			return 4;

		case 4:
			return 2;

		case 2:
			return 1;

		case 1:
			return 0;
		}

		return facing;
	}

	public static int rotateAECableFacing90Reverse(int facing) {
		switch (facing) // 3 5 2 4
		{
		case 3:
			return 5;

		case 5:
			return 2;

		case 2:
			return 4;

		case 4:
			return 3;
		}

		return facing;
	}

	public static int getCCSubtypeFromMetadata(int metadata) {
		return metadata >= 2 && metadata <= 5 ? 0 : (metadata >= 2 && (metadata < 6 || metadata > 9) ? (metadata == 10 ? 2 : (metadata == 11 ? 3 : 4)) : 1);
	}

	public static int getCCDirectionFromMetadata(int metadata) {
		return metadata >= 2 && metadata <= 5 ? metadata : (metadata <= 9 ? (metadata < 2 ? metadata : metadata - 4) : 2);
	}

	public static int rotateCCBlock90Reverse(int dir) {
		switch (dir) {
		case 4:
			return 3;
		case 3:
			return 5;
		case 5:
			return 2;
		case 2:
			return 4;
		}

		return dir;
	}

	public static int rotateComputer90Reverse(int meta) {
		int typeMeta = meta & 0x8;

		switch (meta - typeMeta) {
		case 4:
			return typeMeta + 3;
		case 3:
			return typeMeta + 5;
		case 5:
			return typeMeta + 2;
		case 2:
			return typeMeta + 4;
		}

		return meta;
	}

	/**
	 * Rotate a block's data value -90 degrees
	 * (north<-east<-south<-west<-north);
	 *
	 * @param type
	 * @param data
	 * @return
	 */
	public static int rotate90Reverse(int type, int parData) {
		int data = parData;
		switch (type) {
		case BlockID.MINECART_TRACKS:
			switch (data) {
			case 7:
				return 6;
			case 8:
				return 7;
			case 9:
				return 8;
			case 6:
				return 9;
			}
			/* FALL-THROUGH */

		case BlockID.POWERED_RAIL:
		case BlockID.DETECTOR_RAIL:
		case BlockID.ACTIVATOR_RAIL:
			int power = data & ~0x7;
			switch (data & 0x7) {
			case 1:
				return 0 | power;
			case 0:
				return 1 | power;
			case 5:
				return 2 | power;
			case 4:
				return 3 | power;
			case 2:
				return 4 | power;
			case 3:
				return 5 | power;
			}
			break;

		case BlockID.WOODEN_STAIRS:
		case BlockID.COBBLESTONE_STAIRS:
		case BlockID.BRICK_STAIRS:
		case BlockID.STONE_BRICK_STAIRS:
		case BlockID.NETHER_BRICK_STAIRS:
		case BlockID.SANDSTONE_STAIRS:
		case BlockID.SPRUCE_WOOD_STAIRS:
		case BlockID.BIRCH_WOOD_STAIRS:
		case BlockID.JUNGLE_WOOD_STAIRS:
		case BlockID.QUARTZ_STAIRS:
			switch (data) {
			case 2:
				return 0;
			case 3:
				return 1;
			case 1:
				return 2;
			case 0:
				return 3;
			case 6:
				return 4;
			case 7:
				return 5;
			case 5:
				return 6;
			case 4:
				return 7;
			}
			break;

		case BlockID.WOODEN_DOOR:
		case BlockID.IRON_DOOR:
		case BlockID.COCOA_PLANT:
		case BlockID.TRIPWIRE_HOOK:
			int extra = data & ~0x3;
			int withoutFlags = data & 0x3;
			switch (withoutFlags) {
			case 1:
				return 0 | extra;
			case 2:
				return 1 | extra;
			case 3:
				return 2 | extra;
			case 0:
				return 3 | extra;
			}
			break;

		case BlockID.SIGN_POST:
			return (data + 12) % 16;

		case BlockID.LADDER:
		case BlockID.WALL_SIGN:
		case BlockID.FURNACE:
		case BlockID.BURNING_FURNACE:
		case BlockID.ENDER_CHEST:
		case BlockID.TRAPPED_CHEST:
		case BlockID.HOPPER:
			switch (data) {
			case 5:
				return 2;
			case 4:
				return 3;
			case 2:
				return 4;
			case 3:
				return 5;
			}
			break;

		case BlockID.DROPPER:
			int dispPower = data & 0x8;
			switch (data & ~0x8) {
			case 5:
				return 2 | dispPower;
			case 4:
				return 3 | dispPower;
			case 2:
				return 4 | dispPower;
			case 3:
				return 5 | dispPower;
			}
			break;

		case BlockID.LOG:
			if (data >= 4 && data <= 11)
				data ^= 0xc;
			break;

		case BlockID.COMPARATOR_OFF:
		case BlockID.COMPARATOR_ON:
			int dir = data & 0x03;
			int delay = data - dir;
			switch (dir) {
			case 1:
				return 0 | delay;
			case 2:
				return 1 | delay;
			case 3:
				return 2 | delay;
			case 0:
				return 3 | delay;
			}
			break;

		case BlockID.TRAP_DOOR:
			int withoutOrientation = data & ~0x3;
			int orientation = data & 0x3;
			switch (orientation) {
			case 3:
				return 0 | withoutOrientation;
			case 2:
				return 1 | withoutOrientation;
			case 0:
				return 2 | withoutOrientation;
			case 1:
				return 3 | withoutOrientation;
			}

		case BlockID.PISTON_BASE:
		case BlockID.PISTON_STICKY_BASE:
		case BlockID.PISTON_EXTENSION:
			System.out.println("Flipping piston...");
			final int rest = data & ~0x7;
			switch (data & 0x7) {
			case 5:
				return 2 | rest;
			case 4:
				return 3 | rest;
			case 2:
				return 4 | rest;
			case 3:
				return 5 | rest;
			}
			break;

		case BlockID.BROWN_MUSHROOM_CAP:
		case BlockID.RED_MUSHROOM_CAP:
			if (data >= 10)
				return data;
			return (data * 7) % 10;

		case BlockID.VINE:
			return ((data >> 1) | (data << 3)) & 0xf;

		case BlockID.FENCE_GATE:
			return ((data + 3) & 0x3) | (data & ~0x3);

		case BlockID.ANVIL:
			return data ^ 0x1;

		case BlockID.HAY_BLOCK:
			if (data == 4)
				return 8;
			else if (data == 8)
				return 4;
			else
				return 0;

		}

		return data;
	}
}

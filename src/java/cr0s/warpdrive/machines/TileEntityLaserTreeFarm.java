package cr0s.warpdrive.machines;

import java.util.LinkedList;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.lua.ILuaContext;

public class TileEntityLaserTreeFarm extends TileEntityAbstractMiner {
	Boolean active = false;
	
	private int mode = 0;
	private boolean doLeaves = false;
	private boolean silkTouchLeaves = false;
	private boolean treeTap = false;
	
	private final int defSize = 8;
	private final int scanWait = 40;
	private final int mineWait = 4;
	private int delayMul = 4;
	
	private int totalHarvested=0;
	
	private int scan = 0;
	private int xSize = defSize;
	private int zSize = defSize;
	
	LinkedList<Vector3> logs;
	private int logIndex = 0;
	
	public TileEntityLaserTreeFarm() {
		super();
		peripheralName = "treefarmLaser";
		methodsArray = new String[] {
			"start",
			"stop",
			"area",
			"leaves",
			"silkTouch",
			"silkTouchLeaves",
			"treetap",
			"state"
		};
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if (active) {
			scan++;
			if (mode == 0) {
				if (scan >= scanWait) {
					scan = 0;
					logs = scanTrees();
					if(logs.size() > 0)
						mode = treeTap ? 2 : 1;
					logIndex = 0;
				}
			} else {
				if (scan >= mineWait * delayMul) {
					scan = 0;
					
					if (logIndex >= logs.size()) {
						mode = 0;
						return;
					}
					Vector3 pos = logs.get(logIndex);
					int blockID = worldObj.getBlockId(pos.intX(), pos.intY(), pos.intZ());
					
					if (mode == 1) {
						int cost = calculateBlockCost(blockID);
						if (consumeEnergyFromBooster(cost, true)) {
							if (isLog(blockID) || (doLeaves && isLeaf(blockID))) {
								delayMul = 1;
								if (isRoomForHarvest()) {
									if (consumeEnergyFromBooster(cost, false)) {
										if (isLog(blockID)) {
											delayMul = 4;
											totalHarvested++;
										}
										harvestBlock(pos);
									} else {
										return;
									}
								} else {
									return;
								}
							}
							logIndex++;
						}
					} else if(mode == 2) {
						int cost = calculateBlockCost(blockID);
						if (consumeEnergyFromBooster(cost, true)) {
							if (isRoomForHarvest()) {
								if (blockID == WarpDriveConfig.IC2_RubberWood) {
									int metadata = worldObj.getBlockMetadata(pos.intX(), pos.intY(), pos.intZ());
									if (metadata >= 2 && metadata <= 5) {
										WarpDrive.debugPrint("wetspot found");
										if (consumeEnergyFromBooster(cost, false)) {
											ItemStack resin = WarpDriveConfig.IC2_Resin.copy();
											resin.stackSize = (int) Math.round(Math.random() * 4);
											dumpToInv(resin);
											worldObj.setBlockMetadataWithNotify(pos.intX(), pos.intY(), pos.intZ(), metadata+6, 3);
											laserBlock(pos);
											totalHarvested++;
											delayMul = 4;
										} else {
											return;
										}
									} else {
										delayMul = 1;
									}
								} else if(isLog(blockID)) {
									if (consumeEnergyFromBooster(cost, false)) {
										delayMul = 4;
										totalHarvested++;
										harvestBlock(pos);
									} else {
										return;
									}
								} else if(isLeaf(blockID)) {
									if (consumeEnergyFromBooster(cost, true)) {
										delayMul = 1;
										harvestBlock(pos);
									} else {
										return;
									}
								}
							} else {
								return;
							}
							logIndex++;
						}
					}
				}
			}
		}
	}
	
	private static boolean isLog(int blockID) {
		return WarpDriveConfig.minerLogs.contains(blockID);
	}
	
	private static boolean isLeaf(int blockID) {
		return WarpDriveConfig.minerLeaves.contains(blockID);
	}
	
	private static void addTree(LinkedList<Vector3> list, Vector3 newTree) {
		WarpDrive.debugPrint("Adding tree position:" + newTree.x + "," + newTree.y + "," + newTree.z);
		list.add(newTree);
	}
	
	private LinkedList<Vector3> scanTrees() {
		int xmax, zmax, x1, x2, z1, z2;
		int xmin, zmin;
		x1 = xCoord + xSize / 2;
		x2 = xCoord - xSize / 2;
		xmin = Math.min(x1, x2);
		xmax = Math.max(x1, x2);

		z1 = zCoord + zSize / 2;
		z2 = zCoord - zSize / 2;
		zmin = Math.min(z1, z2);
		zmax = Math.max(z1, z2);
		
		LinkedList<Vector3> logPositions = new LinkedList<Vector3>();
		
		for(int x = xmin; x <= xmax; x++) {
			for(int z = zmin; z <= zmax; z++) {
				int blockID = worldObj.getBlockId(x, yCoord, z);
				if (isLog(blockID)) {
					Vector3 pos = new Vector3(x, yCoord, z);
					logPositions.add(pos);
					scanNearby(logPositions, x, yCoord, z, 0);
				}
			}
		}
		return logPositions;
	}
	
	private void scanNearby(LinkedList<Vector3> current, int x, int y, int z, int d) {
		int[] deltas = {0, -1, 1};
		for(int dx : deltas) {
			for(int dy = 1; dy >= 0; dy--) {
				for(int dz : deltas) {
					int blockID = worldObj.getBlockId(x+dx, y+dy, z+dz);
					if (isLog(blockID) || (doLeaves && isLeaf(blockID))) {
						Vector3 pos = new Vector3(x + dx, y + dy, z + dz);
						if (!current.contains(pos)) {
							addTree(current, pos);
							if (d < 35) {
								scanNearby(current,x+dx,y+dy,z+dz,d+1);
							}
						}
					}
				}	
			}
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("xSize", xSize);
		tag.setInteger("zSize", zSize);
		tag.setBoolean("doLeaves", doLeaves);
		tag.setBoolean("active", active);
		tag.setBoolean("treetap", treeTap);
		tag.setBoolean("silkTouchLeaves", silkTouchLeaves);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		xSize = tag.getInteger("xSize");
		zSize = tag.getInteger("zSize");
		defineMiningArea(xSize,zSize);
		
		doLeaves = tag.getBoolean("doLeaves");
		active   = tag.getBoolean("active");
		treeTap  = tag.getBoolean("treetap");
		silkTouchLeaves = tag.getBoolean("silkTouchLeaves");
	}
	
	@Override
	public boolean shouldChunkLoad() {
		return active;
	}

	// OpenComputer callback methods
	// FIXME: implement OpenComputers...

	// ComputerCraft IPeripheral methods implementation
	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception {
		String methodName = methodsArray[method];
		if (methodName.equals("start")) {
			if (!active) {
				mode = 0;
				totalHarvested = 0;
				active = true;
			}
			return new Boolean[] { true };
		} else if (methodName.equals("stop")) {
			active = false;
		} else if (methodName.equals("area")) {
			try {
				if (arguments.length == 1) {
					xSize = clamp(toInt(arguments[0]),3,WarpDriveConfig.TF_MAX_SIZE);
					zSize = xSize;
				} else if (arguments.length == 2) {
					xSize = clamp(toInt(arguments[0]),3,WarpDriveConfig.TF_MAX_SIZE);
					zSize = clamp(toInt(arguments[1]),3,WarpDriveConfig.TF_MAX_SIZE);
				}
			} catch(NumberFormatException e) {
				xSize = defSize;
				zSize = defSize;
			}
			defineMiningArea(xSize,zSize);
			return new Integer[] { xSize , zSize };
		} else if (methodName.equals("leaves")) {
			try {
				if (arguments.length > 0) {
					doLeaves = toBool(arguments[0]);
				}
			} catch(Exception e) {
				
			}
			return new Boolean[] { doLeaves };
		} else if (methodName.equals("silkTouch")) {
			try {
				silkTouch(arguments[0]);
			} catch(Exception e) {
				silkTouch(false);
			}
			return new Object[] { silkTouch() };
		} else if (methodName.equals("silkTouchLeaves")) {
			try {
				if (arguments.length >= 1) {
					silkTouchLeaves = toBool(arguments[0]);
				}
			} catch(Exception e) {
				silkTouchLeaves = false;
			}
			return new Object[] { silkTouchLeaves };
		} else if (methodName.equals("treetap")) {
			try {
				if (arguments.length >= 1) {
					treeTap = toBool(arguments[0]);
				}
			} catch(Exception e) {
				treeTap = false;
			}
			return new Object[] { treeTap };
		} else if (methodName.equals("state")) {
			String state = active ? (mode==0?"scanning" : (mode == 1 ? "harvesting" : "tapping")) : "inactive";
			return new Object[] { state, xSize, zSize, energy(), totalHarvested };
		}
		return null;
	}

	//ABSTRACT LASER IMPLEMENTATION
	@Override
	protected boolean silkTouch(int blockID) {
		if (isLeaf(blockID)) {
			return silkTouchLeaves;
		}
		return silkTouch();
	}
	
	@Override
	protected boolean canSilkTouch() {
		return true;
	}

	@Override
	protected int minFortune() {
		return 0;
	}

	@Override
	protected int maxFortune() {
		return 0;
	}

	@Override
	protected double laserBelow() {
		return -0.5;
	}

	@Override
	protected float getColorR() {
		return 0.2f;
	}

	@Override
	protected float getColorG() {
		return 0.7f;
	}

	@Override
	protected float getColorB() {
		return 0.4f;
	}
}

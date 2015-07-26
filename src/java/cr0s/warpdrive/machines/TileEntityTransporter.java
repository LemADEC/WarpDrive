package cr0s.warpdrive.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.util.ForgeDirection;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;
import cr0s.warpdrive.api.IUpgradable;
import cr0s.warpdrive.data.EnumUpgradeTypes;
import cr0s.warpdrive.data.Vector3;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;

public class TileEntityTransporter extends WarpEnergyTE implements IUpgradable {
	private double scanRange = 2;

	private int scanDist = 4;

	private double beaconEffect = 0;
	private double powerBoost = 1;
	private double baseLockStrength = -1;
	private double lockStrengthMul = 1;
	private boolean isLocked = false;

	private final static Vector3 centreOnMe = new Vector3(0.5D, 1.0D, 0.5D);
	private Vector3 sourceVec = new Vector3();
	private Vector3 destVec = new Vector3();

	private TeleporterDamage teleDam = new TeleporterDamage("teleporter");

	public TileEntityTransporter() {
		super();
		peripheralName = "transporter";
		methodsArray = new String[] { "source", "dest", "lock", "release", "lockStrength", "energize", "getEnergyLevel", "powerBoost", "energyCost",
				"upgrades", "help" };
	}

	@Override
	public void updateEntity() {
		super.updateEntity();

		if (isLocked) {
			if (lockStrengthMul > 0.8) {
				lockStrengthMul *= 0.995;
			} else {
				lockStrengthMul *= 0.98;
			}
		}
	}

	// OpenComputer callback methods
	// FIXME: implement OpenComputers...

	// ComputerCraft IPeripheral methods implementation
	private static String helpStr(Object[] function) {
		if (function != null && function.length > 0) {
			String fun = function[0].toString().toLowerCase();
			if (fun.equals("source")) {
				if (WarpDriveConfig.TR_RELATIVE_COORDS) {
					return "source(x,y,z): sets the coordinates (relative to the transporter) to teleport from\ndest(): returns the relative x,y,z coordinates of the source";
				} else {
					return "source(x,y,z): sets the absolute coordinates to teleport from\ndest(): returns the x,y,z coordinates of the source";
				}
			} else if (fun.equals("dest")) {
				if (WarpDriveConfig.TR_RELATIVE_COORDS) {
					return "dest(x,y,z): sets the coordinates (relative to the transporter) to teleport to\ndest(): returns the relative x,y,z coordinates of the destination";
				} else {
					return "dest(x,y,z): sets the absolute coordinates to teleport to\ndest(): returns the x,y,z coordinates of the destination";
				}
			} else if (fun.equals("lock")) {
				return "lock(): locks the source and dest coordinates in and returns the lock strength (float)";
			} else if (fun.equals("release")) {
				return "release(): releases the current lock";
			} else if (fun.equals("lockstrength")) {
				return "lockStrength(): returns the current lock strength (float)";
			} else if (fun.equals("energize")) {
				return "energize(): attempts to teleport all entities at source to dest. Returns the number of entities transported (-1 indicates a problem).";
			} else if (fun.equals("powerboost")) {
				return "powerBoost(boostAmount): sets the level of power to use (1 being default), returns the level of power\npowerBoost(): returns the level of power";
			} else if (fun.equals("energycost")) {
				return "energyCost(): returns the amount of energy it will take for a single entity to transport with the current settings";
			} else if (fun.equals("upgrades")) {
				return WarpDrive.defUpgradeStr;
			} else if (fun.equals("getEnergyLevel")) {
				return WarpDrive.defEnergyStr;
			}
		}
		return WarpDrive.defHelpStr;
	}

	private Object[] setVec3(boolean src, Object... arguments) {
		Vector3 vec = src ? sourceVec : destVec;

		if (vec == null) {
			Vector3 sV = WarpDriveConfig.TR_RELATIVE_COORDS ? new Vector3(this) : new Vector3(0, 0, 0);
			if (src)
				sourceVec = sV;
			else
				destVec = sV;
			vec = src ? sourceVec : destVec;
		}

		try {
			if (arguments.length >= 3) {
				unlock();
				vec.x = toDouble(arguments[0]);
				vec.y = toDouble(arguments[1]);
				vec.z = toDouble(arguments[2]);
			} else if (arguments.length == 1) {
				unlock();
				if (WarpDriveConfig.TR_RELATIVE_COORDS) {
					vec.x = centreOnMe.x;
					vec.y = centreOnMe.y;
					vec.z = centreOnMe.z;
				} else {
					vec.x = xCoord + centreOnMe.x;
					vec.y = yCoord + centreOnMe.y;
					vec.z = zCoord + centreOnMe.z;
				}
			}
		} catch (NumberFormatException e) {
			return setVec3(src, "this");
		}
		return new Object[] { vec.x, vec.y, vec.z };
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		String methodName = methodsArray[method];
		if (methodName.equals("getEnergyLevel")) {
			return new Object[] { getEnergyStored(), getMaxEnergyStored() };
		} else if (methodName.equals("source")) {
			return setVec3(true, arguments);
		} else if (methodName.equals("dest")) {
			return setVec3(false, arguments);
		} else if (methodName.equals("lock")) {
			return new Object[] { lock(sourceVec, destVec) };
		} else if (methodName.equals("release")) {
			unlock();
			return null;
		} else if (methodName.equals("lockStrength")) {
			return new Object[] { getLockStrength() };
		} else if (methodName.equals("energize")) {
			return new Object[] { energize() };
		} else if (methodName.equals("powerBoost")) {
			try {
				if (arguments.length >= 1) {
					powerBoost = clamp(toDouble(arguments[0]), 1, WarpDriveConfig.TR_MAX_BOOST_MUL);
				}
			} catch (NumberFormatException e) {
				powerBoost = 1;
			}
			return new Object[] { powerBoost };
		} else if (methodName.equals("energyCost")) {
			return new Object[] { energyCost() };
		} else if (methodName.equals("help")) {
			return new Object[] { helpStr(arguments) };
		}

		return null;
	}

	private Integer energyCost() {
		if (sourceVec != null && destVec != null) {
			return (int) Math.ceil(Math.pow(3, powerBoost - 1) * WarpDriveConfig.TR_EU_PER_METRE * sourceVec.distanceTo(destVec));
		}
		return null;
	}

	private int energize() {
		if (isLocked) {
			int count = 0;
			double ls = getLockStrength();
			WarpDrive.debugPrint("LS:" + getLockStrength());
			ArrayList<Entity> entitiesToTransport = findEntities(sourceVec, ls);
			Integer energyReq = energyCost();
			if (energyReq == null) {
				return -1;
			}
			Vector3 modDest = destVec.clone().translate(centreOnMe);
			for (Entity ent : entitiesToTransport) {
				WarpDrive.debugPrint(this + " Handling entity " + ent.getEntityId());
				if (consumeEnergy(energyReq, false)) {
					WarpDrive.debugPrint("" + this + " Energy taken");
					inflictNegativeEffect(ent, ls);
					transportEnt(ent, modDest);
					count++;
				} else {
					break;
				}
			}
			return count;
		}
		return -1;
	}

	private void transportEnt(Entity ent, Vector3 dest) {
		if (ent instanceof EntityLivingBase) {
			EntityLivingBase livingEnt = (EntityLivingBase) ent;
			if (WarpDriveConfig.TR_RELATIVE_COORDS) {
				livingEnt.setPositionAndUpdate(xCoord + dest.x, yCoord + dest.y, zCoord + dest.z);
			} else {
				livingEnt.setPositionAndUpdate(dest.x, dest.y, dest.z);
			}
		} else {
			if (WarpDriveConfig.TR_RELATIVE_COORDS) {
				ent.setPosition(xCoord + dest.x, yCoord + dest.y, zCoord + dest.z);
			} else {
				ent.setPosition(dest.x, dest.y, dest.z);
			}
		}
	}

	private void inflictNegativeEffect(Entity ent, double lockStrength) {
		double value = Math.random() + lockStrength;

		WarpDrive.debugPrint("TRANSPORTER INFLICTION: " + value);
		if (value < 0.1) {
			ent.attackEntityFrom(teleDam, 1000);
		}

		if (value < 0.2) {
			ent.attackEntityFrom(teleDam, 10);
		}

		if (value < 0.5) {
			ent.attackEntityFrom(teleDam, 1);
		}
	}

	private double beaconScan(int xV, int yV, int zV) {
		WarpDrive.debugPrint("BeaconScan:" + xV + "," + yV + "," + zV);
		double beacon = 0;
		int beaconCount = 0;
		int xL = xV - scanDist;
		int xU = xV + scanDist;
		int yL = yV - scanDist;
		int yU = yV + scanDist;
		int zL = zV - scanDist;
		int zU = zV + scanDist;
		for (int x = xL; x <= xU; x++) {
			for (int y = yL; y <= yU; y++) {
				if (y < 0 || y > 254) {
					continue;
				}

				for (int z = zL; z <= zU; z++) {
					if (!worldObj.getBlock(x, y, z).isAssociatedBlock(WarpDrive.transportBeaconBlock)) {
						continue;
					}
					double dist = 1 + Math.abs(x - xV) + Math.abs(y - yV) + Math.abs(z - zV);
					beaconCount++;
					if (worldObj.getBlockMetadata(x, y, z) == 0) {
						beacon += 1 / dist;
					} else {
						beacon -= 1 / dist;
					}
				}
			}
		}
		if (beaconCount > 0) {
			beacon /= Math.sqrt(beaconCount);
		}
		return beacon;
	}

	private double beaconScan(Vector3 s, Vector3 d) {
		s = absoluteVector(s);
		d = absoluteVector(d);
		return beaconScan(toInt(s.x), toInt(s.y), toInt(s.z)) + beaconScan(toInt(d.x), toInt(d.y), toInt(d.z));
	}

	private Vector3 absoluteVector(Vector3 a) {
		if (WarpDriveConfig.TR_RELATIVE_COORDS)
			return a.clone().translate(new Vector3(this));
		else
			return a;
	}

	private double calculatePower(Vector3 d) {
		Vector3 myCoords;
		if (WarpDriveConfig.TR_RELATIVE_COORDS)
			myCoords = centreOnMe;
		else
			myCoords = new Vector3(this).translate(centreOnMe);
		return calculatePower(myCoords, d);
	}

	private static double calculatePower(Vector3 s, Vector3 d) {
		double dist = s.distanceTo(d);
		return clamp(Math.pow(Math.E, -dist / 300), 0, 1);
	}

	private static double min(double... ds) {
		double curMin = Double.MAX_VALUE;
		for (double d : ds)
			curMin = Math.min(curMin, d);
		return curMin;
	}

	private double getLockStrength() {
		if (isLocked) {
			double upgradeBoost = 1;
			if (upgrades.containsKey(EnumUpgradeTypes.Range))
				upgradeBoost = Math.pow(1.2, upgrades.get(EnumUpgradeTypes.Range));
			return clamp(baseLockStrength * lockStrengthMul * Math.pow(2, powerBoost - 1) * upgradeBoost * (1 + beaconEffect), 0, 1);
		}
		return -1;
	}

	private void unlock() {
		isLocked = false;
		baseLockStrength = 0;
	}

	private double lock(Vector3 source, Vector3 dest) {
		if (source != null && dest != null) {
			double basePower = min(calculatePower(source), calculatePower(dest), calculatePower(source, dest));
			beaconEffect = beaconScan(source, dest);
			WarpDrive.debugPrint("BEACON:" + beaconEffect);
			baseLockStrength = basePower;
			lockStrengthMul = 1;
			isLocked = true;
			WarpDrive.debugPrint(baseLockStrength + "," + getLockStrength());
			return getLockStrength();
		} else {
			unlock();
			return 0;
		}
	}

	private AxisAlignedBB getAABB() {
		Vector3 tS = new Vector3(this);
		Vector3 bS = new Vector3(this);
		Vector3 scanPos = new Vector3(scanRange / 2, 2, scanRange / 2);
		Vector3 scanNeg = new Vector3(-scanRange / 2, -1, -scanRange / 2);
		if (WarpDriveConfig.TR_RELATIVE_COORDS) {
			tS.translate(sourceVec).translate(scanPos);
			bS.translate(sourceVec).translate(scanNeg);
		} else {
			tS = sourceVec.clone().translate(scanPos);
			bS = sourceVec.clone().translate(scanNeg);
		}
		return AxisAlignedBB.getBoundingBox(bS.x, bS.y, bS.z, tS.x, tS.y, tS.z);
	}

	private ArrayList<Entity> findEntities(Vector3 source, double lockStrength) {
		AxisAlignedBB bb = getAABB();
		WarpDrive.debugPrint("Transporter:" + bb.toString());
		List data = worldObj.getEntitiesWithinAABBExcludingEntity(null, bb);
		ArrayList<Entity> output = new ArrayList<Entity>(data.size());
		for (Object ent : data) {
			if (lockStrength >= 1 || worldObj.rand.nextDouble() < lockStrength) {// If
				// weak
				// lock,
				// don't
				// transport
				WarpDrive.debugPrint("" + this + " Entity '" + ent.toString() + "' found and added");
				if (ent instanceof Entity) {
					output.add((Entity) ent);
				}
			} else {
				WarpDrive.debugPrint("" + this + " Entity '" + ent.toString() + "' discarded");
			}
		}
		return output;
	}

	@Override
	public int getMaxEnergyStored() {
		int max = WarpDriveConfig.TR_MAX_ENERGY;
		if (upgrades.containsKey(EnumUpgradeTypes.Energy)) {
			max = (int) Math.floor(max * Math.pow(1.2, upgrades.get(EnumUpgradeTypes.Energy)));
		}
		return max;
	}

	@Override
	public boolean canInputEnergy(ForgeDirection from) {
		if (from == ForgeDirection.UP) {
			return false;
		}
		return true;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setDouble("powerBoost", powerBoost);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		powerBoost = tag.getDouble("powerBoost");
	}

	class TeleporterDamage extends DamageSource {
		protected TeleporterDamage(String par1Str) {
			super(par1Str);
		}

		@Override
		public ChatComponentText func_151519_b(EntityLivingBase e) {
			String mess = "";
			if (e instanceof EntityPlayer || e instanceof EntityPlayerMP) {
				mess = ((EntityPlayer) e).getDisplayName() + " was killed by a teleporter malfunction";
			} else {
				mess = e.toString() + " was killed by a teleporter malfunction";
			}

			WarpDrive.debugPrint(mess);
			return new ChatComponentText(mess);
		}
	}

	@Override
	public boolean takeUpgrade(EnumUpgradeTypes upgradeType, boolean simulate) {
		int max = 0;
		if (upgradeType == EnumUpgradeTypes.Energy)
			max = 2;
		else if (upgradeType == EnumUpgradeTypes.Power)
			max = 4;
		else if (upgradeType == EnumUpgradeTypes.Range)
			max = 4;

		if (max == 0)
			return false;

		if (upgrades.containsKey(upgradeType))
			if (upgrades.get(upgradeType) >= max)
				return false;

		if (!simulate) {
			int c = 0;
			if (upgrades.containsKey(upgradeType))
				c = upgrades.get(upgradeType);
			upgrades.put(upgradeType, c + 1);
		}
		return true;
	}

	@Override
	public Map<EnumUpgradeTypes, Integer> getInstalledUpgrades() {
		return upgrades;
	}

	@Override
	public int getSinkTier() {
		// TODO Auto-generated method stub
		return 2;
	}

	@Override
	public int getSourceTier() {
		// TODO Auto-generated method stub
		return 2;
	}
}

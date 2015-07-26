package cr0s.warpdrive.machines;

import java.util.List;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Optional;
import cr0s.warpdrive.PacketHandler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;

public class TileEntityLaser extends WarpInterfacedTE {
	private final int BEAM_FREQUENCY_SCANNING = 1420;
	private final int BEAM_FREQUENCY_MAX = 65000;

	private int dx, dz, dy;
	private float yaw, pitch; // laser direction

	private int beamFrequency = -1;
	private int cameraFrequency = -1;
	private float r, g, b; // beam color (corresponds to frequency)

	public boolean isEmitting = false;

	private int delayTicks = 0;
	private int energyFromOtherBeams = 0;

	private MovingObjectPosition firstHit_position = null;
	private Block firstHit_block;
	private int firstHit_blockMeta = 0;
	private float firstHit_blockResistance = -2;

	private final static int REGISTRY_UPDATE_INTERVAL_TICKS = 15 * 20;
	private final static int PACKET_SEND_INTERVAL_TICKS = 60 * 20;

	private int registryUpdateTicks = 20;
	private int packetSendTicks = 20;

	public TileEntityLaser() {
		super();
		peripheralName = "laser";
		methodsArray = new String[] { "emitBeam", // 0
				"pos", // 1
				"freq", // 2
				"getFirstHit", // 3
				"getBoosterDXDZ", // 4
				"camFreq" // 5
		};
	}

	@Override
	public void updateEntity() {
		if (isWithCamera()) {
			// Update frequency on clients (recovery mechanism, no need to go
			// too fast)
			if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
				packetSendTicks--;
				if (packetSendTicks <= 0) {
					packetSendTicks = PACKET_SEND_INTERVAL_TICKS;
					PacketHandler.sendFreqPacket(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, cameraFrequency);
				}
			} else {
				registryUpdateTicks--;
				if (registryUpdateTicks <= 0) {
					registryUpdateTicks = REGISTRY_UPDATE_INTERVAL_TICKS;
					WarpDrive.instance.cams.updateInRegistry(worldObj, new ChunkPosition(xCoord, yCoord, zCoord), cameraFrequency, 1);
				}
			}
		}

		// Frequency is not set
		if (beamFrequency <= 0 || beamFrequency > 65000) {
			return;
		}

		delayTicks++;
		if (isEmitting
				&& ((beamFrequency != BEAM_FREQUENCY_SCANNING && delayTicks > WarpDriveConfig.LE_EMIT_DELAY_TICKS) || (beamFrequency == BEAM_FREQUENCY_SCANNING && delayTicks > WarpDriveConfig.LE_EMIT_SCAN_DELAY_TICKS))) {
			delayTicks = 0;
			isEmitting = false;
			int beamEnergy = Math.min(
					this.consumeEnergyFromBoosters() + MathHelper.floor_double(energyFromOtherBeams * WarpDriveConfig.LE_COLLECT_ENERGY_MULTIPLIER),
					WarpDriveConfig.LE_MAX_LASER_ENERGY);
			emitBeam(beamEnergy);
			energyFromOtherBeams = 0;
			sendEvent("laserSend", new Object[] { beamFrequency, beamEnergy });
		}
	}

	public void initiateBeamEmission(float parYaw, float parPitch) {
		yaw = parYaw;
		pitch = parPitch;
		delayTicks = 0;
		isEmitting = true;
	}

	public void addBeamEnergy(int amount) {
		if (isEmitting) {
			energyFromOtherBeams += amount;
			WarpDrive.debugPrint(this + " Added energy " + amount);
		} else {
			WarpDrive.debugPrint(this + " Ignored energy " + amount);
		}
	}

	private int consumeEnergyFromBoosters() {
		int energyCollected = 0;

		if (findFirstBooster() != null) {
			int newX, newY, newZ;
			TileEntity te;
			for (int shift = 1; shift <= WarpDriveConfig.LE_MAX_BOOSTERS_NUMBER; shift++) {
				newX = xCoord + (dx * shift);
				newY = yCoord + (dy * shift);
				newZ = zCoord + (dz * shift);
				te = worldObj.getTileEntity(newX, newY, newZ);
				if (te != null && te instanceof TileEntityParticleBooster) {
					energyCollected += ((TileEntityParticleBooster) te).consumeAllEnergy();
				} else {
					break;
				}
			}
		}

		return energyCollected;
	}

	// TODO refactor me
	private void emitBeam(int parEnergy) {
		int energy = parEnergy; // FIXME
		// Beam power calculations
		int beamLengthBlocks = energy / WarpDriveConfig.LE_BEAM_LENGTH_PER_ENERGY_DIVIDER;

		if (energy == 0 || beamLengthBlocks < 1 || beamFrequency > 65000 || beamFrequency <= 0) {
			return;
		}

		Vector3 beamVector = new Vector3(this).translate(0.5D);
		WarpDrive.debugPrint(this + " Energy " + energy + " over " + beamLengthBlocks + " blocks, Initial beam " + beamVector);
		float yawz = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
		float yawx = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
		float pitchhorizontal = -MathHelper.cos(-pitch * 0.017453292F);
		float pitchvertical = MathHelper.sin(-pitch * 0.017453292F);
		float directionx = yawx * pitchhorizontal;
		float directionz = yawz * pitchhorizontal;
		Vector3 lookVector = new Vector3(directionx, pitchvertical, directionz);
		Vector3.translate(beamVector, lookVector);
		Vector3 reachPoint = Vector3.translate(beamVector.clone(), Vector3.scale(lookVector.clone(), beamLengthBlocks));
		WarpDrive.debugPrint(this + " Beam " + beamVector + " Look " + lookVector + " Reach " + reachPoint + " TranslatedBeam " + beamVector);
		Vector3 endPoint = reachPoint.clone();
		playSoundCorrespondsEnergy(energy);
		int distanceTravelled = 0; // distance traveled from beam sender to
		// previous hit if there were any

		// This is scanning beam, do not deal damage to blocks
		if (beamFrequency == BEAM_FREQUENCY_SCANNING) {
			firstHit_position = worldObj.rayTraceBlocks(beamVector.toVec3(), reachPoint.toVec3());

			if (firstHit_position != null) {
				firstHit_block = worldObj.getBlock(firstHit_position.blockX, firstHit_position.blockY, firstHit_position.blockZ);
				firstHit_blockMeta = worldObj.getBlockMetadata(firstHit_position.blockX, firstHit_position.blockY, firstHit_position.blockZ);
				firstHit_blockResistance = -2;
				if (firstHit_block != null) {
					firstHit_blockResistance = firstHit_block.getExplosionResistance(null); // TODO:
					// what
					// entity
					// should
					// be
					// used?
				}
				PacketHandler.sendBeamPacket(worldObj, beamVector, new Vector3(firstHit_position.hitVec), r, g, b, 50, energy, 200);
			} else {
				firstHit_block = null;
				firstHit_blockMeta = 0;
				firstHit_blockResistance = -2;
				PacketHandler.sendBeamPacket(worldObj, beamVector, reachPoint, r, g, b, 50, energy, 200);
			}

			return;
		}

		for (int passedBlocks = 0; passedBlocks < beamLengthBlocks; passedBlocks++) {
			// Get next block hit
			MovingObjectPosition hit = worldObj.rayTraceBlocks(beamVector.toVec3(), reachPoint.toVec3());
			// FIXME entity ray-tracing
			MovingObjectPosition entityHit = raytraceEntities(beamVector.clone(), lookVector.clone(), true, beamLengthBlocks);

			WarpDrive.debugPrint("Entity hit is " + entityHit);

			if (entityHit != null && entityHit.entityHit instanceof EntityLivingBase) {
				EntityLivingBase e = (EntityLivingBase) entityHit.entityHit;
				double distanceToEntity = entityHit.hitVec.distanceTo(beamVector.clone().toVec3());

				if (hit == null || (hit != null && hit.hitVec.distanceTo(beamVector.clone().toVec3()) > distanceToEntity)) {
					if (distanceToEntity <= beamLengthBlocks) {
						e.setFire(WarpDriveConfig.LE_ENTITY_HIT_SET_ON_FIRE_TIME);
						e.attackEntityFrom(DamageSource.inFire, energy / WarpDriveConfig.LE_ENTITY_HIT_DAMAGE_PER_ENERGY_DIVIDER);

						if (energy > WarpDriveConfig.LE_ENTITY_HIT_EXPLOSION_LASER_ENERGY) {
							worldObj.newExplosion(null, e.posX, e.posY, e.posZ, 4F, true, true);
						}

						// consume energy
						energy -= WarpDriveConfig.LE_ENTITY_HIT_DAMAGE_PER_ENERGY_DIVIDER + (10 * distanceToEntity);
						endPoint = new Vector3(entityHit.hitVec);
						break;
					}
				}
			}

			// Laser is missed
			if (hit == null && entityHit == null) {
				endPoint = reachPoint;
				break;
			} else if (hit != null) {
				// We got a hit block
				int distance = (int) new Vector3(hit.hitVec).distanceTo(beamVector);

				// Laser gone too far
				if (distance >= beamLengthBlocks) {
					endPoint = reachPoint;
					break;
				}

				Block block = worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ);
				// int blockMeta = worldObj.getBlockMetadata(hit.blockX,
				// hit.blockY, hit.blockZ);
				float resistance = block.getExplosionResistance(null);
				// TODO: choose entity

				if (block.isAssociatedBlock(Blocks.bedrock)) {
					endPoint = new Vector3(hit.hitVec);
					break;
				}

				// Hit is a laser head
				if (block.isAssociatedBlock(WarpDrive.laserBlock) || block.isAssociatedBlock(WarpDrive.laserCamBlock)) {
					// Compare frequencies
					TileEntityLaser tel = (TileEntityLaser) worldObj.getTileEntity(hit.blockX, hit.blockY, hit.blockZ);

					if (tel != null && tel.getBeamFrequency() == beamFrequency) {
						tel.addBeamEnergy(energy);
						endPoint = new Vector3(hit.hitVec);
						break;
					}
				}

				if (block.getMaterial() == Material.glass) {
					worldObj.setBlockToAir(hit.blockX, hit.blockY, hit.blockZ);
					endPoint = new Vector3(hit.hitVec);
				}

				energy -= WarpDriveConfig.LE_BLOCK_HIT_CONSUME_ENERGY + (resistance * WarpDriveConfig.LE_BLOCK_HIT_CONSUME_ENERGY_PER_BLOCK_RESISTANCE)
						+ ((distance - distanceTravelled) * WarpDriveConfig.LE_BLOCK_HIT_CONSUME_ENERGY_PER_DISTANCE);
				distanceTravelled = distance;
				endPoint = new Vector3(hit.hitVec);

				if (energy <= 0) {
					break;
				}

				if (resistance >= Blocks.obsidian.getExplosionResistance(null)) {// TODO:
					// choose
					// entity
					worldObj.newExplosion(null, hit.blockX, hit.blockY, hit.blockZ, 4F * (2 + (energy / 500000)), true, true);
					worldObj.setBlock(hit.blockX, hit.blockY, hit.blockZ, (worldObj.rand.nextBoolean()) ? Blocks.fire : Blocks.air);
				} else {
					worldObj.setBlockToAir(hit.blockX, hit.blockY, hit.blockZ);
				}
			}
		}

		PacketHandler.sendBeamPacket(worldObj, new Vector3(this).translate(0.5D).translate(lookVector.scale(0.5D)), endPoint, r, g, b, 50, energy,
				beamLengthBlocks);
	}

	public MovingObjectPosition raytraceEntities(Vector3 beamVec, Vector3 lookVec, boolean collisionFlag, double reachDistance) {
		MovingObjectPosition pickedEntity = null;
		Vec3 playerPosition = beamVec.toVec3();
		Vec3 playerLook = lookVec.toVec3();
		Vec3 playerViewOffset = Vec3.createVectorHelper(playerPosition.xCoord + playerLook.xCoord * reachDistance, playerPosition.yCoord + playerLook.yCoord
				* reachDistance, playerPosition.zCoord + playerLook.zCoord * reachDistance);
		double playerBorder = 1.1 * reachDistance;
		AxisAlignedBB boxToScan = WarpDrive.laserBlock.getCollisionBoundingBoxFromPool(worldObj, xCoord, yCoord, zCoord).expand(playerBorder, playerBorder,
				playerBorder);
		List entitiesHit = worldObj.getEntitiesWithinAABBExcludingEntity(null, boxToScan);
		double closestEntity = reachDistance;

		if (entitiesHit == null || entitiesHit.isEmpty()) {
			return null;
		}

		for (Entity entityHit : (Iterable<Entity>) entitiesHit) {
			if (entityHit != null && entityHit.canBeCollidedWith() && entityHit.boundingBox != null) {
				double border = entityHit.getCollisionBorderSize();
				AxisAlignedBB aabb = entityHit.boundingBox.expand(border, border, border);
				MovingObjectPosition hitMOP = aabb.calculateIntercept(playerPosition, playerViewOffset);

				if (hitMOP != null) {
					if (aabb.isVecInside(playerPosition)) {
						if (0.0D < closestEntity || closestEntity == 0.0D) {
							pickedEntity = new MovingObjectPosition(entityHit);

							if (pickedEntity != null) {
								pickedEntity.hitVec = hitMOP.hitVec;
								closestEntity = 0.0D;
							}
						}
					} else {
						double distance = playerPosition.distanceTo(hitMOP.hitVec);

						if (distance < closestEntity || closestEntity == 0.0D) {
							pickedEntity = new MovingObjectPosition(entityHit);
							pickedEntity.hitVec = hitMOP.hitVec;
							closestEntity = distance;
						}
					}
				}
			}
		}

		return pickedEntity;
	}

	public boolean isWithCamera() {
		return (getBlockType().isAssociatedBlock(WarpDrive.laserCamBlock));
	}

	public int getBeamFrequency() {
		return beamFrequency;
	}

	public void setBeamFrequency(int parBeamFrequency) {
		if (beamFrequency != parBeamFrequency && (parBeamFrequency <= BEAM_FREQUENCY_MAX) && (parBeamFrequency > 0)) {
			WarpDrive.debugPrint(this + " Beam frequency set from " + beamFrequency + " to " + parBeamFrequency);
			beamFrequency = parBeamFrequency;
		}
		updateColor();
	}

	public int getCameraFrequency() {
		return cameraFrequency;
	}

	public void setCameraFrequency(int parCameraFrequency) {
		if (cameraFrequency != parCameraFrequency) {
			WarpDrive.debugPrint(this + " Camera frequency set from " + cameraFrequency + " to " + parCameraFrequency);
			cameraFrequency = parCameraFrequency;
			// force update through main thread since CC runs on server as
			// 'client'
			packetSendTicks = 0;
			registryUpdateTicks = 0;
		}
	}

	private TileEntityParticleBooster findFirstBooster() {
		TileEntity result;
		result = worldObj.getTileEntity(xCoord + 1, yCoord, zCoord);
		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = 1;
			dy = 0;
			dz = 0;
			return (TileEntityParticleBooster) result;
		}

		result = worldObj.getTileEntity(xCoord - 1, yCoord, zCoord);
		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = -1;
			dy = 0;
			dz = 0;
			return (TileEntityParticleBooster) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord, zCoord + 1);
		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = 0;
			dy = 0;
			dz = 1;
			return (TileEntityParticleBooster) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord, zCoord - 1);
		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = 0;
			dy = 0;
			dz = -1;
			return (TileEntityParticleBooster) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord + 1, zCoord);
		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = 0;
			dy = 1;
			dz = 0;
			return (TileEntityParticleBooster) result;
		}

		result = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
		if (result != null && result instanceof TileEntityParticleBooster) {
			dx = 0;
			dy = -1;
			dz = 0;
			return (TileEntityParticleBooster) result;
		}

		return null;
	}

	private void playSoundCorrespondsEnergy(int energy) {
		if (energy <= 500000) {
			worldObj.playSoundEffect(xCoord + 0.5f, yCoord - 0.5f, zCoord + 0.5f, "warpdrive:lowlaser", 4F, 1F);
		} else if (energy > 500000 && energy <= 1000000) {
			worldObj.playSoundEffect(xCoord + 0.5f, yCoord - 0.5f, zCoord + 0.5f, "warpdrive:midlaser", 4F, 1F);
		} else if (energy > 1000000) {
			worldObj.playSoundEffect(xCoord + 0.5f, yCoord - 0.5f, zCoord + 0.5f, "warpdrive:hilaser", 4F, 1F);
		}
	}

	private void updateColor() {
		if (beamFrequency <= 0) { // invalid frequency
			r = 1.0F;
			g = 0.0F;
			b = 0.0F;
		} else if (beamFrequency <= 10000) { // red
			r = 1.0F;
			g = 0.0F;
			b = 0.0F + 0.5f * beamFrequency / 10000F;
		} else if (beamFrequency <= 20000) { // orange
			r = 1.0F;
			g = 0.0F + 1.0F * (beamFrequency - 10000F) / 10000F;
			b = 0.5F - 0.5F * (beamFrequency - 10000F) / 10000F;
		} else if (beamFrequency <= 30000) { // yellow
			r = 1.0F - 1.0F * (beamFrequency - 20000F) / 10000F;
			g = 1.0F;
			b = 0.0F;
		} else if (beamFrequency <= 40000) { // green
			r = 0.0F;
			g = 1.0F - 1.0F * (beamFrequency - 30000F) / 10000F;
			b = 0.0F + 1.0F * (beamFrequency - 30000F) / 10000F;
		} else if (beamFrequency <= 50000) { // blue
			r = 0.0F + 0.5F * (beamFrequency - 40000F) / 10000F;
			g = 0.0F;
			b = 1.0F - 0.5F * (beamFrequency - 40000F) / 10000F;
		} else if (beamFrequency <= 60000) { // violet
			r = 0.5F + 0.5F * (beamFrequency - 50000F) / 10000F;
			g = 0.0F;
			b = 0.5F - 0.5F * (beamFrequency - 50000F) / 10000F;
		} else if (beamFrequency <= BEAM_FREQUENCY_MAX) { // rainbow
			int component = Math.round(4096F * (beamFrequency - 60000F) / (BEAM_FREQUENCY_MAX - 60000F));
			r = 1.0F - 0.5F * (component & 0xF);
			g = 0.5F + 0.5F * (component >> 4 & 0xF);
			b = 0.5F + 0.5F * (component >> 8 & 0xF);
		} else { // invalid frequency
			r = 1.0F;
			g = 0.0F;
			b = 0.0F;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		setBeamFrequency(tag.getInteger("beamFrequency"));
		setCameraFrequency(tag.getInteger("cameraFrequency"));
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("beamFrequency", beamFrequency);
		tag.setInteger("cameraFrequency", cameraFrequency);
	}

	@Override
	public void invalidate() {
		WarpDrive.instance.cams.removeFromRegistry(worldObj, new ChunkPosition(xCoord, yCoord, zCoord));
		super.invalidate();
	}

	@Override
	public void onChunkUnload() {
		WarpDrive.instance.cams.removeFromRegistry(worldObj, new ChunkPosition(xCoord, yCoord, zCoord));
		super.onChunkUnload();
	}

	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] emitBeam(Context context, Arguments arguments) {
		return emitBeam(argumentsOCtoCC(arguments));
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] pos(Context context, Arguments arguments) {
		return new Integer[] { xCoord, yCoord, zCoord };
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] freq(Context context, Arguments arguments) {
		if (arguments.count() == 1) {
			setBeamFrequency(arguments.checkInteger(0));
		}
		return new Integer[] { beamFrequency };
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] getFirstHit(Context context, Arguments arguments) {
		return getFirstHit();
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] getBoosterDXDZ(Context context, Arguments arguments) {
		findFirstBooster();
		return new Integer[] { dx, dz };
	}

	@Callback
	@Optional.Method(modid = "OpenComputers")
	private Object[] CamFreq(Context context, Arguments arguments) {
		if (isWithCamera()) {
			if (arguments.count() == 1) {
				setCameraFrequency(arguments.checkInteger(0));
			}
			return new Integer[] { cameraFrequency };
		}
		return null;
	}

	private Object[] emitBeam(Object[] arguments) {
		try {
			float newYaw, newPitch;
			if (arguments.length == 2) {
				newYaw = toFloat(arguments[0]);
				newPitch = toFloat(arguments[1]);
				initiateBeamEmission(newYaw, newPitch);
			} else if (arguments.length == 3) {
				float deltaX = -toFloat(arguments[0]);
				float deltaY = -toFloat(arguments[1]);
				float deltaZ = toFloat(arguments[2]);
				double horizontalDistance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
				newYaw = (float) (Math.atan2(deltaX, deltaZ) * 180.0D / Math.PI);
				newPitch = (float) (Math.atan2(deltaY, horizontalDistance) * 180.0D / Math.PI);
				initiateBeamEmission(newYaw, newPitch);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Object[] { false };
		}
		return new Object[] { true };
	}

	private Object[] getFirstHit() {
		if (firstHit_position != null) {
			try {
				Object[] info = { firstHit_position.blockX, firstHit_position.blockY, firstHit_position.blockZ, firstHit_block, firstHit_blockMeta,
						firstHit_blockResistance };
				firstHit_position = null;
				firstHit_block = null;
				firstHit_blockMeta = 0;
				firstHit_blockResistance = -2;
				return info;
			} catch (Exception e) {
				e.printStackTrace();
				return new Integer[] { 0, 0, 0, 0, 0, -3 };
			}
		} else {
			return new Integer[] { 0, 0, 0, 0, 0, -1 };
		}
	}

	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		String methodName = methodsArray[method];
		if (methodName.equals("emitBeam")) { // emitBeam(yaw, pitch) or
			// emitBeam(deltaX, deltaY,
			// deltaZ)
			return emitBeam(arguments);
		} else if (methodName.equals("pos")) {
			return new Integer[] { xCoord, yCoord, zCoord };
		} else if (methodName.equals("freq")) {
			if (arguments.length == 1) {
				setBeamFrequency(toInt(arguments[0]));
			}
			return new Integer[] { beamFrequency };
		} else if (methodName.equals("getFirstHit")) {
			return getFirstHit();
		} else if (methodName.equals("getBoosterDXDZ")) {
			findFirstBooster();
			return new Integer[] { dx, dz };
		} else if (methodName.equals("camFreq")) { // camFreq (only for lasers
			// with cam)
			if (isWithCamera()) {
				if (arguments.length == 1) {
					setCameraFrequency(toInt(arguments[0]));
				}
				return new Integer[] { cameraFrequency };
			}
			return null;
		}
		return null;
	}

	@Override
	public String toString() {
		return String.format("%s/%d Beam \'%d\' Camera \'%d\' @ \'%s\' %d, %d, %d", new Object[] { getClass().getSimpleName(), Integer.valueOf(hashCode()),
				beamFrequency, cameraFrequency, worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(), xCoord, yCoord, zCoord });
	}
}
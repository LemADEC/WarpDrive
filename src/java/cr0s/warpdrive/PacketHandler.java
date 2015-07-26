package cr0s.warpdrive;

import ibxm.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.machines.TileEntityCamera;
import cr0s.warpdrive.machines.TileEntityLaser;
import cr0s.warpdrive.machines.TileEntityMonitor;

public class PacketHandler implements IMessageHandler {

	@Override
	public IMessage onMessage(IMessage message, MessageContext ctx) {
		if (ctx..channel.equals("WarpDriveBeam")) {
			handleBeam(packet, (EntityPlayer)player);
		} else if (packet.channel.equals("WarpDriveFreq")) {
			handleFreqUpdate(packet, (EntityPlayer)player);
		} else if (packet.channel.equals("WarpDriveLaserT")) {
			handleLaserTargeting(packet, (EntityPlayer)player);
		} else if (packet.channel.equals("WarpDriveCloaks")) {
			handleCloak(packet, (EntityPlayer)player);
		}
	}

	public static void handleCloak(Packet250CustomPayload packet, EntityPlayer player) {
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));

		try {
			// Read cloaked area parameters
			int minX = inputStream.readInt();
			int minY = inputStream.readInt();
			int minZ = inputStream.readInt();

			int maxX = inputStream.readInt();
			int maxY = inputStream.readInt();
			int maxZ = inputStream.readInt();

			boolean decloak = inputStream.readBoolean();

			byte tier = inputStream.readByte();

			// WarpDrive.debugPrint("[Cloak Packet] Received " + ((decloak) ?
			// "DEcloaked" : "cloaked") + "area: (" + minX + "; " + minY + "; "
			// + minZ + ") -> (" + maxX + "; " + maxY + "; " + maxZ + ")");

			if (minX <= player.posX && (maxX + 1) > player.posY && minY <= player.posZ && (maxY + 1) > player.posX && minZ <= player.posY
					&& (maxZ + 1) > player.posZ) {
				return;
			}

			// Hide the area
			if (!decloak) {
				// WarpDrive.debugPrint("[Cloak Packet] Removing " + size +
				// " blocks...");

				// Now hide the blocks within area
				World worldObj = player.worldObj;
				int cloakBlockID = (tier == 1) ? WarpDriveConfig.gasID : 0;
				int cloakBlockMetadata = (tier == 1) ? 5 : 0;
				int minYmap = Math.max(0, minY);
				int maxYmap = Math.min(255, maxY);
				for (int y = minYmap; y <= maxYmap; y++) {
					for (int x = minX; x <= maxX; x++) {
						for (int z = minZ; z <= maxZ; z++) {
							int blockID = worldObj.getBlockId(x, y, z);
							if (blockID != 0) {
								boolean isSafe = true;
								if (blockID == WarpDriveConfig.CC_peripheral) {
									int blockMetadata = worldObj.getBlockMetadata(x, y, z);
									isSafe = (blockMetadata != 2 && blockMetadata != 4);
								}
								if (isSafe) {
									worldObj.setBlock(x, y, z, cloakBlockID, cloakBlockMetadata, 4);
								}
							}
						}
					}
				}

				// WarpDrive.debugPrint("[Cloak Packet] Removing entity...");
				// Hide any entities inside area
				AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
				List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(player, aabb);
				for (Entity e : list) {
					worldObj.removeEntity(e);
					((WorldClient) worldObj).removeEntityFromWorld(e.entityId);
				}
			} else { // reveal the area
				player.worldObj.markBlockRangeForRenderUpdate(minX - 1, Math.max(0, minY - 1), minZ - 1, maxX + 1, Math.min(255, maxY + 1), maxZ + 1);

				// Make some graphics
				int numLasers = 80 + player.worldObj.rand.nextInt(50);

				double centerX = (minX + maxX) / 2.0D;
				double centerY = (minY + maxY) / 2.0D;
				double centerZ = (minZ + maxZ) / 2.0D;
				double radiusX = (maxX - minX) / 2.0D + 5.0D;
				double radiusY = (maxY - minY) / 2.0D + 5.0D;
				double radiusZ = (maxZ - minZ) / 2.0D + 5.0D;

				for (int i = 0; i < numLasers; i++) {
					WarpDrive.proxy.renderBeam(player.worldObj, new Vector3(centerX + radiusX * player.worldObj.rand.nextGaussian(), centerY + radiusY
							* player.worldObj.rand.nextGaussian(), centerZ + radiusZ * player.worldObj.rand.nextGaussian()), new Vector3(centerX + radiusX
							* player.worldObj.rand.nextGaussian(), centerY + radiusY * player.worldObj.rand.nextGaussian(), centerZ + radiusZ
							* player.worldObj.rand.nextGaussian()), player.worldObj.rand.nextFloat(), player.worldObj.rand.nextFloat(),
							player.worldObj.rand.nextFloat(), 60 + player.worldObj.rand.nextInt(60), 100);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void handleLaserTargeting(Packet250CustomPayload packet, EntityPlayer player) {
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));

		try {
			int x = inputStream.readInt();
			int y = inputStream.readInt();
			int z = inputStream.readInt();
			float yaw = inputStream.readFloat();
			float pitch = inputStream.readFloat();
			WarpDrive.debugPrint("Received target packet: (" + x + "; " + y + "; " + z + ") yaw: " + yaw + " pitch: " + pitch);
			TileEntity te = player.worldObj.getBlockTileEntity(x, y, z);
			if (te != null && te instanceof TileEntityLaser) {
				TileEntityLaser laser = (TileEntityLaser) te;
				laser.initiateBeamEmission(yaw, pitch);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void handleFreqUpdate(Packet250CustomPayload packet, EntityPlayer player) {
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));

		try {
			int x = inputStream.readInt();
			int y = inputStream.readInt();
			int z = inputStream.readInt();
			int frequency = inputStream.readInt();
			// WarpDrive.debugPrint("Received frequency packet: (" + x + ", " +
			// y + ", " + z + ") frequency '" + frequency + "'");
			TileEntity te = player.worldObj.getBlockTileEntity(x, y, z);
			if (te != null) {
				if (te instanceof TileEntityMonitor) {
					((TileEntityMonitor) te).setFrequency(frequency);
				} else if (te instanceof TileEntityCamera) {
					((TileEntityCamera) te).setFrequency(frequency);
				} else if (te instanceof TileEntityLaser) {
					((TileEntityLaser) te).setCameraFrequency(frequency);
				} else {
					WarpDrive.print("Received frequency packet: (" + x + ", " + y + ", " + z + ") is not a valid tile entity");
				}
			} else {
				WarpDrive.print("Received frequency packet: (" + x + ", " + y + ", " + z + ") has no tile entity");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleBeam(Packet250CustomPayload packet, EntityPlayer player) {
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));
		Vector3 source, target;
		double sx, sy, sz;
		double tx, ty, tz;
		float r, g, b;
		int age;
		int energy;
		World worldObj = player.worldObj;

		try {
			// Read source vector
			sx = inputStream.readDouble();
			sy = inputStream.readDouble();
			sz = inputStream.readDouble();
			source = new Vector3(sx, sy, sz);
			// Read target vector
			tx = inputStream.readDouble();
			ty = inputStream.readDouble();
			tz = inputStream.readDouble();
			target = new Vector3(tx, ty, tz);
			// Read r, g, b of laser
			r = inputStream.readFloat();
			g = inputStream.readFloat();
			b = inputStream.readFloat();
			// Read age
			age = inputStream.readByte();
			// Read energy value
			energy = inputStream.readInt();

			// Render beam
			// WarpDrive.debugPrint("Received beam packet from " + source +
			// " to " + target + " as RGB " + r + " " + g + " " + b + " age " +
			// age +" energy " + energy);

			// To avoid NPE at logging in
			if (worldObj == null) {
				WarpDrive.debugPrint("WorldObj is null, ignoring beam packet");
				return;
			}

			WarpDrive.proxy.renderBeam(worldObj, source.clone(), target.clone(), r, g, b, age, energy);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	public static void sendBeamPacket(World worldObj, Vector3 source, Vector3 dest, float r, float g, float b, int age, int energy, int radius) {
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.SERVER) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
			DataOutputStream outputStream = new DataOutputStream(bos);

			try {
				// Write source vector
				outputStream.writeDouble(source.x);
				outputStream.writeDouble(source.y);
				outputStream.writeDouble(source.z);
				// Write target vector
				outputStream.writeDouble(dest.x);
				outputStream.writeDouble(dest.y);
				outputStream.writeDouble(dest.z);
				// Write r, g, b of laser
				outputStream.writeFloat(r);
				outputStream.writeFloat(g);
				outputStream.writeFloat(b);
				// Write age
				outputStream.writeByte(age);
				// Write energy value
				outputStream.writeInt(energy);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			Packet250CustomPayload packet = new Packet250CustomPayload();
			packet.channel = "WarpDriveBeam";
			packet.data = bos.toByteArray();
			packet.length = bos.size();
			if (source.distanceTo_square(dest) < 3600 /* 60 * 60 */) {
				MinecraftServer
						.getServer()
						.getConfigurationManager()
						.sendToAllNear((source.intX() + dest.intX()) / 2, (source.intY() + dest.intY()) / 2, (source.intZ() + dest.intZ()) / 2, radius,
								worldObj.provider.dimensionId, packet);
				return;
			}
			MinecraftServer.getServer().getConfigurationManager()
					.sendToAllNear(source.intX(), source.intY(), source.intZ(), radius, worldObj.provider.dimensionId, packet);

			ByteArrayOutputStream bos2 = new ByteArrayOutputStream(8);
			DataOutputStream outputStream2 = new DataOutputStream(bos2);

			try {
				// Write source vector
				outputStream2.writeDouble(source.x);
				outputStream2.writeDouble(source.y);
				outputStream2.writeDouble(source.z);
				// Write target vector
				outputStream2.writeDouble(dest.x);
				outputStream2.writeDouble(dest.y);
				outputStream2.writeDouble(dest.z);
				// Write r, g, b of laser
				outputStream2.writeFloat(r);
				outputStream2.writeFloat(g);
				outputStream2.writeFloat(b);
				// Write age
				outputStream2.writeByte(age);
				// Write energy value
				outputStream2.writeInt(energy);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			Packet250CustomPayload packet2 = new Packet250CustomPayload();
			packet2.channel = "WarpDriveBeam";
			packet2.data = bos.toByteArray();
			packet2.length = bos.size();
			MinecraftServer.getServer().getConfigurationManager()
					.sendToAllNear(dest.intX(), dest.intY(), dest.intZ(), radius, worldObj.provider.dimensionId, packet2);
		}
	}

	public static void sendFreqPacket(int dimensionId, int xCoord, int yCoord, int zCoord, int frequency) {
		if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
			DataOutputStream outputStream = new DataOutputStream(bos);

			try {
				outputStream.writeInt(xCoord);
				outputStream.writeInt(yCoord);
				outputStream.writeInt(zCoord);
				outputStream.writeInt(frequency);
			} catch (Exception e) {
				e.printStackTrace();
			}

			Packet250CustomPayload packet = new Packet250CustomPayload();
			packet.channel = "WarpDriveFreq";
			packet.data = bos.toByteArray();
			packet.length = bos.size();
			MinecraftServer.getServer().getConfigurationManager().sendToAllNear(xCoord, yCoord, zCoord, 100, dimensionId, packet);
			// WarpDrive.debugPrint("Packet '" + packet.channel + "' sent (" +
			// xCoord + ", " + yCoord + ", " + zCoord + ") '" + frequency +
			// "'");
		}
	}

	// LaserCamera shooting at target (client -> server)
	public static void sendLaserTargetingPacket(int x, int y, int z, float yaw, float pitch) {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
			DataOutputStream outputStream = new DataOutputStream(bos);

			try {
				outputStream.writeInt(x);
				outputStream.writeInt(y);
				outputStream.writeInt(z);
				outputStream.writeFloat(yaw);
				outputStream.writeFloat(pitch);
			} catch (Exception e) {
				e.printStackTrace();
			}

			Packet250CustomPayload packet = new Packet250CustomPayload();
			packet.channel = "WarpDriveLaserT";
			packet.data = bos.toByteArray();
			packet.length = bos.size();
			PacketDispatcher.sendPacketToServer(packet);
			WarpDrive.debugPrint("Packet '" + packet.channel + "' sent (" + x + ", " + y + ", " + z + ") yaw " + yaw + " pitch " + pitch);
		}
	}

	// Sending cloaking area definition (server -> client)
	public static void sendCloakPacket(EntityPlayer player, AxisAlignedBB aabb, int tier, boolean decloak) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
		DataOutputStream outputStream = new DataOutputStream(bos);

		try {
			outputStream.writeInt((int) aabb.minX);
			outputStream.writeInt((int) aabb.minY);
			outputStream.writeInt((int) aabb.minZ);

			outputStream.writeInt((int) aabb.maxX);
			outputStream.writeInt((int) aabb.maxY);
			outputStream.writeInt((int) aabb.maxZ);

			outputStream.writeBoolean(decloak);

			outputStream.writeByte(tier);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// WarpDrive.debugPrint("" + this + " Sending cloak packet to player " +
		// player.username);
		Packet250CustomPayload packet = new Packet250CustomPayload();
		packet.channel = "WarpDriveCloaks";
		packet.data = bos.toByteArray();
		packet.length = bos.size();

		((EntityPlayerMP) player).playerNetServerHandler.sendPacketToPlayer(packet);
	}
}
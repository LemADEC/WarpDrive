package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityBreakingFX;
import net.minecraft.client.particle.EntityCloudFX;
import net.minecraft.client.particle.EntityExplodeFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityFireworkSparkFX;
import net.minecraft.client.particle.EntityFlameFX;
import net.minecraft.client.particle.EntitySnowShovelFX;
import net.minecraft.client.particle.EntitySpellParticleFX;
import net.minecraft.init.Items;
import net.minecraft.world.World;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageSpawnParticle implements IMessage, IMessageHandler<MessageSpawnParticle, IMessage> {
	
	private String type;
	private byte quantity;
	private Vector3 origin;
	private Vector3 direction;
	private float baseRed;
	private float baseGreen;
	private float baseBlue;
	private float fadeRed;
	private float fadeGreen;
	private float fadeBlue;
	
	public MessageSpawnParticle() {
		// required on receiving side
	}
	
	public MessageSpawnParticle(final String type, final byte quantity, final Vector3 origin, final Vector3 direction,
	                            final float baseRed, final float baseGreen, final float baseBlue,
	                            final float fadeRed, final float fadeGreen, final float fadeBlue) {
		this.type = type;
		this.quantity = quantity;
		this.origin = origin;
		this.direction = direction;
		this.baseRed = baseRed;
		this.baseGreen = baseGreen;
		this.baseBlue = baseBlue;
		this.fadeRed = fadeRed;
		this.fadeGreen = fadeGreen;
		this.fadeBlue = fadeBlue;
	}
	
	@Override
	public void fromBytes(ByteBuf buffer) {
		int typeSize = buffer.readByte();
		type = buffer.toString(buffer.readerIndex(), typeSize, StandardCharsets.US_ASCII);
		buffer.skipBytes(typeSize);
		
		quantity = buffer.readByte();
		
		double x = buffer.readDouble();
		double y = buffer.readDouble();
		double z = buffer.readDouble();
		origin = new Vector3(x, y, z);
		
		x = buffer.readDouble();
		y = buffer.readDouble();
		z = buffer.readDouble();
		direction = new Vector3(x, y, z);
		
		baseRed = buffer.readFloat();
		baseGreen = buffer.readFloat();
		baseBlue = buffer.readFloat();
		fadeRed = buffer.readFloat();
		fadeGreen = buffer.readFloat();
		fadeBlue = buffer.readFloat();
	}
	
	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeByte(type.length());
		buffer.writeBytes(type.getBytes(StandardCharsets.US_ASCII), 0, type.length());
		buffer.writeByte(quantity);
		buffer.writeDouble(origin.x);
		buffer.writeDouble(origin.y);
		buffer.writeDouble(origin.z);
		buffer.writeDouble(direction.x);
		buffer.writeDouble(direction.y);
		buffer.writeDouble(direction.z);
		buffer.writeFloat(baseRed);
		buffer.writeFloat(baseGreen);
		buffer.writeFloat(baseBlue);
		buffer.writeFloat(fadeRed);
		buffer.writeFloat(fadeGreen);
		buffer.writeFloat(fadeBlue);
	}
	
	private int integerFromRGB(final float red, final float green, final float blue) {
		return (Math.round(red * 255.0F) << 16)
			+  (Math.round(green * 255.0F) << 8)
			+   Math.round(blue * 255.0F);
	}
	
	@SideOnly(Side.CLIENT)
	private void handle(World world) {
		// Directly spawn particle as per RenderGlobal.doSpawnParticle, bypassing range check
		// adjust color as needed
		EntityFX effect;
		double noiseLevel = direction.getMagnitude() * 0.35D;
		for (int i = 0; i < quantity; i++) {
			Vector3 directionRandomized = new Vector3(
					direction.x + noiseLevel * (world.rand.nextFloat() - world.rand.nextFloat()),
					direction.y + noiseLevel * (world.rand.nextFloat() - world.rand.nextFloat()),
					direction.z + noiseLevel * (world.rand.nextFloat() - world.rand.nextFloat()));
			switch (type) {
			default:
				WarpDrive.logger.error(String.format("Invalid particle type '%s' at %s", type, origin.toString()));
			case "explode":
				effect = new EntityExplodeFX(world, origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			
			case "fireworksSpark":
				EntityFireworkSparkFX entityFireworkSparkFX = new EntityFireworkSparkFX(world, origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z,
					                                                                       FMLClientHandler.instance().getClient().effectRenderer);
				entityFireworkSparkFX.setFadeColour(integerFromRGB(fadeRed, fadeGreen, fadeBlue));
				effect = entityFireworkSparkFX;
				break;
			
			case "flame":
				effect = new EntityFlameFX(world, origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			
			case "snowballpoof":
				effect = new EntityBreakingFX(world, origin.x, origin.y, origin.z, Items.snowball);
				break;
			
			case "snowshovel":
				effect = new EntitySnowShovelFX(world, origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			
			case "mobSpell":
				effect = new EntitySpellParticleFX(world, origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			
			case "cloud":
				effect = new EntityCloudFX(world, origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			} 
			if (baseRed >= 0.0F && baseGreen >= 0.0F && baseBlue >= 0.0F) {
				effect.setRBGColorF(baseRed, baseGreen, baseBlue);
			}
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(effect);
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(MessageSpawnParticle messageSpawnParticle, MessageContext context) {
		// skip in case player just logged in
		if (Minecraft.getMinecraft().theWorld == null) {
			WarpDrive.logger.error("WorldObj is null, ignoring particle packet");
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_EFFECTS) {
			WarpDrive.logger.info("Received particle effect '%s' x %d from %s towards %s as RGB %.2f %.2f %.2f fading to %.2f %.2f %.2f",
				messageSpawnParticle.type, messageSpawnParticle.quantity, messageSpawnParticle.origin, messageSpawnParticle.direction,
				messageSpawnParticle.baseRed, messageSpawnParticle.baseGreen, messageSpawnParticle.baseBlue,
				messageSpawnParticle.fadeRed, messageSpawnParticle.fadeGreen, messageSpawnParticle.fadeBlue);
		}
		
		messageSpawnParticle.handle(Minecraft.getMinecraft().theWorld);
		
		return null;	// no response
	}
}

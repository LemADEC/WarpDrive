package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import io.netty.buffer.ByteBuf;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class MessageTargeting implements IMessage, IMessageHandler<MessageTargeting, IMessage> {
	
	private int x;
	private int y;
	private int z;
	private float yaw;
	private float pitch;
	
	public MessageTargeting() {
		// required on receiving side
	}
	
	public MessageTargeting(final int x, final int y, final int z, final float yaw, final float pitch) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		x = buffer.readInt();
		y = buffer.readInt();
		z = buffer.readInt();
		yaw = buffer.readFloat();
		pitch = buffer.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeInt(x);
		buffer.writeInt(y);
		buffer.writeInt(z);
		buffer.writeFloat(yaw);
		buffer.writeFloat(pitch);
	}
	
	private void handle(World worldObj) {
		TileEntity te = worldObj.getTileEntity(x, y, z);
		if (te != null && te instanceof TileEntityLaser) {
			TileEntityLaser laser = (TileEntityLaser) te;
			laser.initiateBeamEmission(yaw, pitch);
		}
	}
	
	@Override
	public IMessage onMessage(MessageTargeting targetingMessage, MessageContext context) {
		if (WarpDriveConfig.LOGGING_TARGETING) {
			WarpDrive.logger.info("Received target packet: (" + targetingMessage.x + " " + targetingMessage.y + " " + targetingMessage.z
				+ ") yaw: " + targetingMessage.yaw + " pitch: " + targetingMessage.pitch);
		}
		
		targetingMessage.handle(context.getServerHandler().playerEntity.worldObj);
        
		return null;	// no response
	}
}

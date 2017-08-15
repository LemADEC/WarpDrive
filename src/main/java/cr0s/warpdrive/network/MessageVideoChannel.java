package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.config.WarpDriveConfig;
import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageVideoChannel implements IMessage, IMessageHandler<MessageVideoChannel, IMessage> {
	private int x;
	private int y;
	private int z;
	private int videoChannel;
	
	public MessageVideoChannel() {
		// required on receiving side
	}
	
	public MessageVideoChannel(final int x, final int y, final int z, final int videoChannel) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.videoChannel = videoChannel;
	}
	
	@Override
	public void fromBytes(ByteBuf buffer) {
		x = buffer.readInt();
		y = buffer.readInt();
		z = buffer.readInt();
		videoChannel = buffer.readInt();
	}
	
	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeInt(x);
		buffer.writeInt(y);
		buffer.writeInt(z);
		buffer.writeInt(videoChannel);
	}
	
	@SideOnly(Side.CLIENT)
	private void handle(World worldObj) {
		TileEntity tileEntity = worldObj.getTileEntity(x, y, z);
		if (tileEntity != null) {
			if (tileEntity instanceof IVideoChannel) {
				((IVideoChannel) tileEntity).setVideoChannel(videoChannel);
			} else {
				WarpDrive.logger.error("Received video channel packet: (" + x + " " + y + " " + z + ") is not a valid tile entity");
			}
		} else {
			WarpDrive.logger.error("Received video channel packet: (" + x + " " + y + " " + z + ") has no tile entity");
		}
 	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(MessageVideoChannel videoChannelMessage, MessageContext context) {
		// skip in case player just logged in
		if (Minecraft.getMinecraft().theWorld == null) {
			WarpDrive.logger.error("WorldObj is null, ignoring video channel packet");
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
			WarpDrive.logger.info("Received video channel packet: (" + videoChannelMessage.x + " " + videoChannelMessage.y + " " + videoChannelMessage.z + ") videoChannel '" + videoChannelMessage.videoChannel + "'");
		}
		
		videoChannelMessage.handle(Minecraft.getMinecraft().theWorld);
		
		return null;	// no response
	}
}

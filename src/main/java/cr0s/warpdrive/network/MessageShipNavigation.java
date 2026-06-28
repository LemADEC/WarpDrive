package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageShipNavigation implements IMessage, IMessageHandler<MessageShipNavigation, IMessage> {
	
	private NBTTagCompound tagCompound;
	
	@SuppressWarnings("unused")
	public MessageShipNavigation() {
		// required on receiving side
	}
	
	public MessageShipNavigation(final NBTTagCompound tagCompound) {
		this.tagCompound = tagCompound;
	}
	
	@Override
	public void fromBytes(final ByteBuf buffer) {
		tagCompound = ByteBufUtils.readTag(buffer);
	}
	
	@Override
	public void toBytes(final ByteBuf buffer) {
		ByteBufUtils.writeTag(buffer, tagCompound);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(final MessageShipNavigation messageShipNavigation, final MessageContext context) {
		if (WarpDriveConfig.LOGGING_CLIENT_SYNCHRONIZATION) {
			WarpDrive.logger.info(String.format("Received ship navigation packet: %s", messageShipNavigation.tagCompound));
		}
		WarpDrive.proxy.openShipNavigationGui(messageShipNavigation.tagCompound);
		return null;
	}
}

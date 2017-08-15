package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.core.ClassTransformer;
import io.netty.buffer.ByteBuf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class MessageClientValidation implements IMessage, IMessageHandler<MessageClientValidation, IMessage> {
	
	private String mapClass;
	
	public MessageClientValidation() {
		// required on receiving side
	}
	
	@Override
	public void fromBytes(ByteBuf buffer) {
		final int size = buffer.readInt();
		mapClass = buffer.toString(buffer.readerIndex(), size, Charset.forName("UTF8"));
		buffer.readerIndex(buffer.readerIndex() + size);
	}
	
	@Override
	public void toBytes(ByteBuf buffer) {
		final String mapClassFull = ClassTransformer.getClientValidation();
		final String mapClassTruncated = mapClassFull.substring(0, Math.min(32700, mapClassFull.length()));
		final byte[] bytesString = mapClassTruncated.getBytes(Charset.forName("UTF8"));
		buffer.writeInt(bytesString.length);
		buffer.writeBytes(bytesString);
	}
	
	private void handle(final String namePlayer) {
		try {
			if (new File("ClientValidation").exists()) {
				final String fileName = String.format("ClientValidation/%s.tsv", namePlayer);
				
				final File file = new File(fileName);
				if (!file.exists()) {
					//noinspection ResultOfMethodCallIgnored
					file.createNewFile();
				}
				
				final PrintWriter printWriter = new PrintWriter(new FileWriter(file));
				printWriter.println(mapClass);
				printWriter.close();
			}
		} catch (IOException exception) {
			exception.printStackTrace();
			WarpDrive.logger.error("Exception while saving client validation to disk");
		}
	}
	
	@Override
	public IMessage onMessage(MessageClientValidation targetingMessage, MessageContext context) {
		if (WarpDrive.isDev) {
			WarpDrive.logger.info("Received client validation packet from %s",
			                      context.getServerHandler().playerEntity.getCommandSenderName());
		}
		
		targetingMessage.handle(context.getServerHandler().playerEntity.getCommandSenderName());
        
		return null;	// no response
	}
}

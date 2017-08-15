package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.common.util.Constants.NBT;

public class MessageClientSync implements IMessage, IMessageHandler<MessageClientSync, IMessage> {
	
	private NBTTagCompound nbtTagCompound;
	
	public MessageClientSync() {
		// required on receiving side
	}
	
	public MessageClientSync(final EntityPlayerMP entityPlayerMP, final CelestialObject celestialObject) {
		nbtTagCompound = new NBTTagCompound();
		nbtTagCompound.setTag("celestialObjects"     , CelestialObjectManager.writeClientSync(entityPlayerMP, celestialObject));
		nbtTagCompound.setTag("items_breathingHelmet", Dictionary.writeItemsToNBT(Dictionary.ITEMS_BREATHING_HELMET));
		nbtTagCompound.setTag("items_flyInSpace"     , Dictionary.writeItemsToNBT(Dictionary.ITEMS_FLYINSPACE));
		nbtTagCompound.setTag("items_noFallDamage"   , Dictionary.writeItemsToNBT(Dictionary.ITEMS_NOFALLDAMAGE));
	}
	
	@Override
	public void fromBytes(ByteBuf buffer) {
		nbtTagCompound = ByteBufUtils.readTag(buffer);
	}
	
	@Override
	public void toBytes(ByteBuf buffer) {
		ByteBufUtils.writeTag(buffer, nbtTagCompound);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(MessageClientSync messageClientSync, MessageContext context) {
		// skip in case player just logged in
		if (Minecraft.getMinecraft().theWorld == null) {
			WarpDrive.logger.error("WorldObj is null, ignoring client synchronization packet");
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_CLIENT_SYNCHRONIZATION) {
			WarpDrive.logger.info(String.format("Received client synchronization packet: %s",
			                                    messageClientSync.nbtTagCompound));
		}
		
		try {
			CelestialObjectManager.readClientSync(messageClientSync.nbtTagCompound.getTagList("celestialObjects", NBT.TAG_COMPOUND));
			Dictionary.ITEMS_BREATHING_HELMET = Dictionary.readItemsFromNBT(messageClientSync.nbtTagCompound.getTagList("items_breathingHelmet", NBT.TAG_STRING));
			Dictionary.ITEMS_FLYINSPACE       = Dictionary.readItemsFromNBT(messageClientSync.nbtTagCompound.getTagList("items_flyInSpace"     , NBT.TAG_STRING));
			Dictionary.ITEMS_NOFALLDAMAGE     = Dictionary.readItemsFromNBT(messageClientSync.nbtTagCompound.getTagList("items_noFallDamage"   , NBT.TAG_STRING));
		} catch (Exception exception) {
			exception.printStackTrace();
			WarpDrive.logger.error(String.format("Fails to parse client synchronization packet %s", messageClientSync.nbtTagCompound));
		}
		
		return new MessageClientValidation();
	}
}

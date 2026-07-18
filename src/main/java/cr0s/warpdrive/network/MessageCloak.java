package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CloakedArea;
import cr0s.warpdrive.network.PacketHandler.IMessage;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class MessageCloak implements IMessage {
	
	private int coreX;
	private int coreY;
	private int coreZ;
	private int minX;
	private int minY;
	private int minZ;
	private int maxX;
	private int maxY;
	private int maxZ;
	private boolean isFullyTransparent;
	private boolean isUncloaking;

	@SuppressWarnings("unused")
	public MessageCloak() {
		// required on receiving side
	}
	
	public MessageCloak(final CloakedArea area, final boolean isUncloaking) {
		this.coreX = area.blockPosCore.getX();
		this.coreY = area.blockPosCore.getY();
		this.coreZ = area.blockPosCore.getZ();
		this.minX = area.minX;
		this.minY = area.minY;
		this.minZ = area.minZ;
		this.maxX = area.maxX;
		this.maxY = area.maxY;
		this.maxZ = area.maxZ;
		this.isFullyTransparent = area.isFullyTransparent;
		this.isUncloaking = isUncloaking;
	}
	
	@Override
	public void decode(@Nonnull final PacketBuffer buffer) {
		coreX = buffer.readInt();
		coreY = buffer.readInt();
		coreZ = buffer.readInt();
		minX = buffer.readInt();
		minY = buffer.readInt();
		minZ = buffer.readInt();
		maxX = buffer.readInt();
		maxY = buffer.readInt();
		maxZ = buffer.readInt();
		isFullyTransparent = buffer.readBoolean();
		isUncloaking = buffer.readBoolean();
	}

	@Override
	public void encode(@Nonnull final PacketBuffer buffer) {
		buffer.writeInt(coreX);
		buffer.writeInt(coreY);
		buffer.writeInt(coreZ);
		buffer.writeInt(minX);
		buffer.writeInt(minY);
		buffer.writeInt(minZ);
		buffer.writeInt(maxX);
		buffer.writeInt(maxY);
		buffer.writeInt(maxZ);
		buffer.writeBoolean(isFullyTransparent);
		buffer.writeBoolean(isUncloaking);
	}
	
	@OnlyIn(Dist.CLIENT)
	private void handle(final ClientPlayerEntity player) {
		if (isUncloaking) {
			// reveal the area
			WarpDrive.cloaks.removeCloakedArea(player.world.getDimension().getType().getRegistryName(), new BlockPos(coreX, coreY, coreZ));
		} else { 
			// Hide the area
			WarpDrive.cloaks.updateCloakedArea(player.world, new BlockPos(coreX, coreY, coreZ), isFullyTransparent,
			                                   minX, minY, minZ, maxX, maxY, maxZ);
		}
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public IMessage process(@Nonnull final Context context) {
		// skip in case player just logged in
		if (Minecraft.getInstance().world == null) {
			WarpDrive.logger.error("WorldObj is null, ignoring cloak packet");
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("Received cloak packet: %s area (%d %d %d) -> (%d %d %d) tier %d",
			                                    ((isUncloaking) ? "UNCLOAKING" : "cloaking"),
			                                    minX, minY, minZ,
			                                    maxX, maxY, maxZ, isFullyTransparent ? 2 : 1));
		}
		
		final ClientPlayerEntity player = Minecraft.getInstance().player;
		assert player != null;
		if ( minX <= player.getPosX() && (maxX + 1) > player.getPosX()
		  && minY <= player.getPosY() && (maxY + 1) > player.getPosY()
		  && minZ <= player.getPosZ() && (maxZ + 1) > player.getPosZ()) {
			return null;
		}
		handle(player);
		
		return null;	// no response
	}
}

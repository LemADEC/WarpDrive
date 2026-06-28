package cr0s.warpdrive.network;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.movement.ShipMovementPreview;
import cr0s.warpdrive.block.movement.ShipNavigationHelper;
import cr0s.warpdrive.block.movement.TileEntityShipController;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageShipNavigationAction implements IMessage, IMessageHandler<MessageShipNavigationAction, IMessage> {
	
	public static final byte ACTION_REFRESH = 0;
	public static final byte ACTION_PLAN = 1;
	public static final byte ACTION_ENGAGE = 2;
	public static final byte ACTION_CANCEL = 3;
	public static final byte ACTION_UPDATE_SETTINGS = 4;
	public static final byte ACTION_PREVIEW_MOVEMENT = 5;
	public static final byte ACTION_EXECUTE_MOVEMENT = 6;
	public static final byte ACTION_SET_MODE = 7;
	public static final byte ACTION_PAUSE = 8;
	public static final byte ACTION_RESUME = 9;
	public static final byte ACTION_STEP = 10;
	
	private byte action;
	private int dimensionId;
	private int x;
	private int y;
	private int z;
	private int accessX;
	private int accessY;
	private int accessZ;
	private String targetId;
	private NBTTagCompound payload;
	private static final ConcurrentHashMap<String, Long> ACTION_THROTTLE_MS = new ConcurrentHashMap<>();
	private static final int THROTTLE_MAP_SOFT_CAP = 4096;
	private static final long THROTTLE_ENTRY_TTL_MS = 60_000L;
	
	@SuppressWarnings("unused")
	public MessageShipNavigationAction() {
		// required on receiving side
	}
	
	public MessageShipNavigationAction(final byte action, final int dimensionId,
	                                   final BlockPos blockPosCore, final BlockPos blockPosAccess,
	                                   final String targetId) {
		this.action = action;
		this.dimensionId = dimensionId;
		x = blockPosCore.getX();
		y = blockPosCore.getY();
		z = blockPosCore.getZ();
		accessX = blockPosAccess.getX();
		accessY = blockPosAccess.getY();
		accessZ = blockPosAccess.getZ();
		this.targetId = targetId == null ? "" : targetId;
		payload = new NBTTagCompound();
	}
	
	public MessageShipNavigationAction(final byte action, final int dimensionId,
	                                   final BlockPos blockPosCore, final BlockPos blockPosAccess,
	                                   final NBTTagCompound payload) {
		this(action, dimensionId, blockPosCore, blockPosAccess, "");
		this.payload = payload == null ? new NBTTagCompound() : payload;
	}
	
	@Override
	public void fromBytes(final ByteBuf buffer) {
		action = buffer.readByte();
		dimensionId = buffer.readInt();
		x = buffer.readInt();
		y = buffer.readInt();
		z = buffer.readInt();
		accessX = buffer.readInt();
		accessY = buffer.readInt();
		accessZ = buffer.readInt();
		targetId = ByteBufUtils.readUTF8String(buffer);
		payload = ByteBufUtils.readTag(buffer);
		if (payload == null) {
			payload = new NBTTagCompound();
		}
	}
	
	@Override
	public void toBytes(final ByteBuf buffer) {
		buffer.writeByte(action);
		buffer.writeInt(dimensionId);
		buffer.writeInt(x);
		buffer.writeInt(y);
		buffer.writeInt(z);
		buffer.writeInt(accessX);
		buffer.writeInt(accessY);
		buffer.writeInt(accessZ);
		ByteBufUtils.writeUTF8String(buffer, targetId);
		ByteBufUtils.writeTag(buffer, payload == null ? new NBTTagCompound() : payload);
	}
	
	@Override
	public IMessage onMessage(final MessageShipNavigationAction message, final MessageContext context) {
		final EntityPlayerMP entityPlayerMP = context.getServerHandler().player;
		entityPlayerMP.getServerWorld().addScheduledTask(() -> handle(message, entityPlayerMP));
		return null;
	}
	
	@SuppressWarnings("PMD.NPathComplexity")
	private static void handle(final MessageShipNavigationAction message, final EntityPlayerMP entityPlayerMP) {
		if ( message.action < ACTION_REFRESH
		  || message.action > ACTION_STEP ) {
			WarpDrive.logger.warn(String.format("Ignoring unknown ship navigation action %d from %s",
			                                    message.action, entityPlayerMP));
			return;
		}
		final WorldServer worldServer = DimensionManager.getWorld(message.dimensionId);
		if (worldServer == null) {
			WarpDrive.logger.warn(String.format("Ignoring ship navigation action %d for unloaded dimension %d from %s",
			                                    message.action, message.dimensionId, entityPlayerMP));
			return;
		}
		if (entityPlayerMP.dimension != message.dimensionId) {
			WarpDrive.logger.warn(String.format("Ignoring remote ship navigation action %d in dimension %d from %s in dimension %d",
			                                    message.action, message.dimensionId, entityPlayerMP, entityPlayerMP.dimension));
			return;
		}
		final BlockPos blockPos = new BlockPos(message.x, message.y, message.z);
		final BlockPos blockPosAccess = new BlockPos(message.accessX, message.accessY, message.accessZ);
		if (entityPlayerMP.getDistanceSq(blockPosAccess) > 64.0D * 64.0D) {
			WarpDrive.logger.warn(String.format("Ignoring distant ship navigation action %d for access %s from %s",
			                                    message.action, Commons.format(worldServer, blockPosAccess), entityPlayerMP));
			return;
		}
		if ( !worldServer.isBlockLoaded(blockPos, false)
		  || !worldServer.isBlockLoaded(blockPosAccess, false) ) {
			WarpDrive.logger.warn(String.format("Ignoring ship navigation action %d for unloaded core/access %s/%s from %s",
			                                    message.action, Commons.format(worldServer, blockPos),
			                                    Commons.format(worldServer, blockPosAccess), entityPlayerMP));
			return;
		}
		final TileEntity tileEntity = worldServer.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityShipCore)) {
			WarpDrive.logger.warn(String.format("Ignoring ship navigation action %d for invalid core %s from %s",
			                                    message.action, Commons.format(worldServer, blockPos), entityPlayerMP));
			return;
		}
		
		final TileEntityShipCore shipCore = (TileEntityShipCore) tileEntity;
		if (!isValidAccess(worldServer, blockPos, blockPosAccess, shipCore)) {
			WarpDrive.logger.warn(String.format("Ignoring ship navigation action %d for unauthorized access %s to core %s from %s",
			                                    message.action, Commons.format(worldServer, blockPosAccess),
			                                    Commons.format(worldServer, blockPos), entityPlayerMP));
			return;
		}
		if (!shipCore.isCrewMember(entityPlayerMP)) {
			Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.denied"));
			return;
		}
		if (isThrottled(message, entityPlayerMP, blockPos)) {
			return;
		}
		String notice = "";
		ShipMovementPreview preview = null;
		switch (message.action) {
		case ACTION_PLAN:
			notice = ShipNavigationHelper.setDestination(entityPlayerMP, shipCore, message.targetId)
			       ? "warpdrive.navigation.notice.destination_plotted" : "warpdrive.navigation.notice.unable_to_plot";
			break;

		case ACTION_ENGAGE:
			notice = ShipNavigationHelper.engage(entityPlayerMP, shipCore)
			       ? "warpdrive.navigation.notice.engaging_leg" : "warpdrive.navigation.notice.unable_to_engage";
			break;

		case ACTION_CANCEL:
			notice = ShipNavigationHelper.cancel(entityPlayerMP, shipCore)
			       ? "warpdrive.navigation.notice.route_cancelled" : "warpdrive.navigation.notice.unable_to_cancel";
			break;

		case ACTION_REFRESH:
			shipCore.invalidateNavigationCache();
			notice = "";
			break;

		case ACTION_UPDATE_SETTINGS:
			ShipNavigationHelper.applySettings(shipCore, message.payload);
			notice = "warpdrive.navigation.notice.settings_updated";
			break;

		case ACTION_SET_MODE:
			ShipNavigationHelper.setAutopilotMode(entityPlayerMP, shipCore, message.targetId);
			notice = "warpdrive.navigation.notice.mode_set";
			break;

		case ACTION_PAUSE:
			ShipNavigationHelper.pause(entityPlayerMP, shipCore);
			notice = "warpdrive.navigation.notice.paused";
			break;

		case ACTION_RESUME:
			ShipNavigationHelper.resume(entityPlayerMP, shipCore);
			notice = "warpdrive.navigation.notice.resumed";
			break;

		case ACTION_STEP:
			notice = ShipNavigationHelper.step(entityPlayerMP, shipCore)
			       ? "warpdrive.navigation.notice.engaging_leg" : "warpdrive.navigation.notice.unable_to_engage";
			break;

		case ACTION_PREVIEW_MOVEMENT:
			preview = ShipNavigationHelper.previewMovement(shipCore, message.payload);
			notice = preview.canEngage ? "warpdrive.navigation.notice.preview_ready"
			       : preview.blockerKey.isEmpty() ? "warpdrive.navigation.notice.unable_to_execute" : preview.blockerKey;
			break;

		case ACTION_EXECUTE_MOVEMENT:
			ShipNavigationHelper.applySettings(shipCore, message.payload);
			preview = ShipNavigationHelper.previewMovement(shipCore, message.payload);
			if (ShipNavigationHelper.executeMovement(shipCore, message.payload, preview)) {
				notice = "warpdrive.navigation.notice.command_accepted";
			} else {
				notice = preview.blockerKey.isEmpty() ? "warpdrive.navigation.notice.unable_to_execute" : preview.blockerKey;
				if (!preview.blockerKey.isEmpty()) {
					Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), preview.blockerKey));
				}
			}
			break;

		default:
			WarpDrive.logger.warn(String.format("Ignoring unknown ship navigation action %d from %s",
			                                    message.action, entityPlayerMP));
			return;
		}
		
		// the static celestial map only changes when the celestial config reloads, so only rebuild/resend it
		// when this player's cached version is out of date (avoids rebuilding it on every periodic refresh)
		PacketHandler.sendShipNavigationMapIfChanged(entityPlayerMP);
		final NBTTagCompound tagCompound = ShipNavigationHelper.buildSnapshot(entityPlayerMP, shipCore, blockPosAccess, notice, preview);
		PacketHandler.sendShipNavigationPacket(entityPlayerMP, tagCompound);
	}
	
	private static boolean isThrottled(final MessageShipNavigationAction message,
	                                   final EntityPlayerMP entityPlayerMP,
	                                   final BlockPos blockPosCore) {
		final long intervalMs;
		switch (message.action) {
		case ACTION_PLAN:
			intervalMs = 300L;
			break;
		case ACTION_REFRESH:
			intervalMs = 250L;
			break;
		case ACTION_ENGAGE:
		case ACTION_CANCEL:
		case ACTION_STEP:
		case ACTION_SET_MODE:
		case ACTION_PAUSE:
		case ACTION_RESUME:
			intervalMs = 150L;
			break;
		case ACTION_PREVIEW_MOVEMENT:
			intervalMs = 150L;
			break;
		case ACTION_UPDATE_SETTINGS:
			intervalMs = 0L;
			break;
		case ACTION_EXECUTE_MOVEMENT:
			intervalMs = 250L;
			break;
		default:
			intervalMs = 0L;
			break;
		}
		if (intervalMs <= 0L) {
			return false;
		}
		final String key = entityPlayerMP.getUniqueID() + ":" + message.action + ":" + message.dimensionId + ":"
		                 + blockPosCore.getX() + ":" + blockPosCore.getY() + ":" + blockPosCore.getZ() + ":" + message.targetId;
		final long now = System.currentTimeMillis();
		final Long last = ACTION_THROTTLE_MS.put(key, now);
		// bound the static map: evict stale entries once it grows past a soft cap so it can't leak
		// for players/cores that come and go (entries older than the TTL can never throttle anything)
		if (ACTION_THROTTLE_MS.size() > THROTTLE_MAP_SOFT_CAP) {
			ACTION_THROTTLE_MS.values().removeIf(timestamp -> now - timestamp > THROTTLE_ENTRY_TTL_MS);
		}
		return last != null && now - last < intervalMs;
	}
	
	private static boolean isValidAccess(final WorldServer worldServer, final BlockPos blockPosCore,
	                                     final BlockPos blockPosAccess, final TileEntityShipCore shipCore) {
		if (blockPosCore.equals(blockPosAccess)) {
			return true;
		}
		final TileEntity tileEntityAccess = worldServer.getTileEntity(blockPosAccess);
		if (!(tileEntityAccess instanceof TileEntityShipController)) {
			return false;
		}
		return ((TileEntityShipController) tileEntityAccess).getLinkedShipCoreRefresh() == shipCore;
	}
}

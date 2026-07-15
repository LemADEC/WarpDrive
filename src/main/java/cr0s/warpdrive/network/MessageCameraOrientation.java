package cr0s.warpdrive.network;

import cr0s.warpdrive.api.ICamera;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.block.detection.TileEntityMonitor;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageCameraOrientation implements IMessage, IMessageHandler<MessageCameraOrientation, IMessage> {

	private BlockPos blockPosMonitor;
	private BlockPos blockPosCamera;
	private float yaw;
	private float pitch;

	@SuppressWarnings("unused")
	public MessageCameraOrientation() {
		// required on receiving side
	}

	public MessageCameraOrientation(final BlockPos blockPosMonitor, final BlockPos blockPosCamera, final float yaw, final float pitch) {
		this.blockPosMonitor = blockPosMonitor;
		this.blockPosCamera = blockPosCamera;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	@Override
	public void fromBytes(final ByteBuf buffer) {
		blockPosMonitor = BlockPos.fromLong(buffer.readLong());
		blockPosCamera = BlockPos.fromLong(buffer.readLong());
		yaw = buffer.readFloat();
		pitch = buffer.readFloat();
	}

	@Override
	public void toBytes(final ByteBuf buffer) {
		buffer.writeLong(blockPosMonitor.toLong());
		buffer.writeLong(blockPosCamera.toLong());
		buffer.writeFloat(yaw);
		buffer.writeFloat(pitch);
	}

	private void handle(final EntityPlayerMP entityPlayerMP) {
		if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
			return;
		}

		final WorldServer world = entityPlayerMP.getServerWorld();
		if ( !world.isBlockLoaded(blockPosMonitor, false)
		  || !world.isBlockLoaded(blockPosCamera, false)
		  || entityPlayerMP.getDistanceSq(blockPosMonitor.getX() + 0.5D, blockPosMonitor.getY() + 0.5D, blockPosMonitor.getZ() + 0.5D) > 64.0D ) {
			return;
		}

		final TileEntity tileEntityMonitor = world.getTileEntity(blockPosMonitor);
		final TileEntity tileEntityCamera = world.getTileEntity(blockPosCamera);
		if ( !(tileEntityMonitor instanceof TileEntityMonitor)
		  || !(tileEntityCamera instanceof ICamera) ) {
			return;
		}

		final int videoChannel = ((TileEntityMonitor) tileEntityMonitor).getVideoChannel();
		final ICamera camera = (ICamera) tileEntityCamera;
		if ( !IVideoChannel.isValid(videoChannel)
		  || videoChannel != camera.getVideoChannel() ) {
			return;
		}

		camera.setCameraOrientation(MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0F, 90.0F));
	}

	@Override
	public IMessage onMessage(final MessageCameraOrientation message, final MessageContext context) {
		final EntityPlayerMP entityPlayerMP = context.getServerHandler().player;
		entityPlayerMP.getServerWorld().addScheduledTask(() -> message.handle(entityPlayerMP));
		return null;
	}
}

package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Force a full re-synchronization of a single tile entity to the client, using the same
 * getUpdateTag()/handleUpdateTag() path a chunk reload would use. This is needed after a ship
 * move because the vanilla per-block update only carries getUpdatePacket(), which some mods
 * (e.g. GregTech) leave empty unless one of their own values actually changed.
 */
public class MessageClientTileEntitySync implements IMessage, IMessageHandler<MessageClientTileEntitySync, IMessage> {

	private int x;
	private int y;
	private int z;
	private NBTTagCompound tagCompound;

	@SuppressWarnings("unused")
	public MessageClientTileEntitySync() {
		// required on receiving side
	}

	public MessageClientTileEntitySync(final BlockPos blockPos, final NBTTagCompound tagCompound) {
		this.x = blockPos.getX();
		this.y = blockPos.getY();
		this.z = blockPos.getZ();
		this.tagCompound = tagCompound;
	}

	@Override
	public void fromBytes(final ByteBuf buffer) {
		x = buffer.readInt();
		y = buffer.readInt();
		z = buffer.readInt();
		tagCompound = ByteBufUtils.readTag(buffer);
	}

	@Override
	public void toBytes(final ByteBuf buffer) {
		buffer.writeInt(x);
		buffer.writeInt(y);
		buffer.writeInt(z);
		ByteBufUtils.writeTag(buffer, tagCompound);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(final MessageClientTileEntitySync message, final MessageContext context) {
		final World world = Minecraft.getMinecraft().world;
		if (world == null) {
			return null;
		}

		final BlockPos blockPos = new BlockPos(message.x, message.y, message.z);
		if (!world.isBlockLoaded(blockPos)) {
			return null;
		}

		// when the area is cloaked, the block is fog/air with no matching tile entity, so this safely no-ops
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity != null) {
			try {
				tileEntity.handleUpdateTag(message.tagCompound);
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
			}
		}

		return null;
	}
}

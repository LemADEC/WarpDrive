package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.network.PacketHandler.IMessage;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent.Context;

/**
 * Force a full re-synchronization of a single tile entity to the client, using the same
 * getUpdateTag()/handleUpdateTag() path a chunk reload would use. This is needed after a ship
 * move because the vanilla per-block update only carries getUpdatePacket(), which some mods
 * (e.g. GregTech) leave empty unless one of their own values actually changed.
 */
public class MessageClientTileEntitySync implements IMessage {

	private BlockPos blockPos;
	private CompoundNBT tagCompound;

	@SuppressWarnings("unused")
	public MessageClientTileEntitySync() {
		// required on receiving side
	}

	public MessageClientTileEntitySync(final BlockPos blockPos, final CompoundNBT tagCompound) {
		// defensive copy: a MutableBlockPos would be stored by reference and serialized later in encode(),
		// so a caller reusing/mutating it after construction would corrupt this message (toImmutable() is a no-op for a plain BlockPos)
		this.blockPos = blockPos.toImmutable();
		this.tagCompound = tagCompound;
	}

	@Override
	public void decode(@Nonnull final PacketBuffer buffer) {
		blockPos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
		tagCompound = buffer.readCompoundTag();
	}

	@Override
	public void encode(@Nonnull final PacketBuffer buffer) {
		buffer.writeInt(blockPos.getX());
		buffer.writeInt(blockPos.getY());
		buffer.writeInt(blockPos.getZ());
		buffer.writeCompoundTag(tagCompound);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public IMessage process(@Nonnull final Context context) {
		// skip in case player just logged in
		final World world = Minecraft.getInstance().world;
		if (world == null) {
			return null;
		}
		if (!world.isBlockLoaded(blockPos)) {
			return null;
		}

		// when the area is cloaked, the block is fog/air with no matching tile entity, so this safely no-ops
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity != null) {
			try {
				tileEntity.handleUpdateTag(tagCompound);
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
			}
		}

		return null;	// no response
	}
}

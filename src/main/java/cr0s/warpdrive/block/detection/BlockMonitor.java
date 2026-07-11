package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ICamera;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.BlockAbstractRotatingContainer;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CameraRegistryItem;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.render.ClientCameraHandler;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockMonitor extends BlockAbstractRotatingContainer {
	
	public BlockMonitor(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.detection.monitor");
	}

	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityMonitor();
	}

	@Override
	public boolean onBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		// Monitor is only reacting client side
		if ( !world.isRemote
		  || enumHand != EnumHand.MAIN_HAND ) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		
		if ( itemStackHeld.isEmpty()
		  && enumFacing == blockState.getValue(BlockProperties.FACING) ) {
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			if (tileEntity instanceof TileEntityMonitor) {
				// validate video channel
				final int videoChannel = ((TileEntityMonitor) tileEntity).getVideoChannel();
				if ( !IVideoChannel.isValid(videoChannel) ) {
					Commons.addChatMessage(entityPlayer, ((TileEntityMonitor) tileEntity).getStatus());
					return true;
				}
				
				// validate camera
				final CameraRegistryItem camera = WarpDrive.cameras.getCameraByVideoChannel(world, videoChannel);
				if ( camera == null
				  || entityPlayer.isSneaking() ) {
					Commons.addChatMessage(entityPlayer, ((TileEntityMonitor) tileEntity).getStatus());
					return true;
				}
				
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.monitor.viewing_camera",
				                                                       new WarpDriveText(Commons.getStyleValue(), videoChannel),
				                                                       camera.blockPos.getX(),
				                                                       camera.blockPos.getY(),
				                                                       camera.blockPos.getZ() ));
				final TileEntity tileEntityCamera = world.getTileEntity(camera.blockPos);
				final ICamera cameraTile = tileEntityCamera instanceof ICamera ? (ICamera) tileEntityCamera : null;
				ClientCameraHandler.setupViewpoint(
						camera.type, entityPlayer,
						cameraTile != null && cameraTile.hasCameraOrientation() ? cameraTile.getCameraYaw() : entityPlayer.rotationYaw,
						cameraTile != null && cameraTile.hasCameraOrientation() ? cameraTile.getCameraPitch() : entityPlayer.rotationPitch,
						blockPos, blockState,
						camera.blockPos, world.getBlockState(camera.blockPos));
				return true;
			}
		}
		
		return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
	}
}

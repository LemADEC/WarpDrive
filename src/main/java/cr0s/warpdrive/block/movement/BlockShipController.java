package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.data.EnumShipCommand;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public class BlockShipController extends BlockAbstractContainer {
	
	public static final PropertyEnum<EnumShipCommand> COMMAND = PropertyEnum.create("command", EnumShipCommand.class);
	
	public BlockShipController(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.movement.ship_controller." + enumTier.getName());
		
		setDefaultState(getDefaultState()
				                .withProperty(COMMAND, EnumShipCommand.OFFLINE)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, COMMAND);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(COMMAND, EnumShipCommand.get(metadata));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(COMMAND).ordinal();
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityShipController();
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockController(this);
	}
	
	@Override
	public boolean canPlaceBlockOnSide(@Nonnull final World world, @Nonnull final BlockPos blockPos, final EnumFacing side) {
		// ancestor
		if (!super.canPlaceBlockOnSide(world, blockPos, side)) {
			return false;
		}
		if (side == EnumFacing.UP || side == EnumFacing.DOWN) {
			// highlight the breaking change when going from 1.7.10 to 1.10+
			final MutableBlockPos mutableBlockPos = new MutableBlockPos(blockPos);
			for (final EnumFacing enumFacing : EnumFacing.HORIZONTALS) {
				mutableBlockPos.setPos(
						blockPos.getX() + enumFacing.getXOffset(),
						blockPos.getY() + enumFacing.getYOffset(),
						blockPos.getZ() + enumFacing.getZOffset());
				if (world.getBlockState(mutableBlockPos).getBlock() instanceof BlockShipCore) {
					final EntityPlayer entityPlayer = world.getClosestPlayer(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D, 5, false);
					if (entityPlayer != null) {
						Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "tile.warpdrive.movement.ship_controller.away_from_core"));
					}
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean onBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (enumHand != EnumHand.MAIN_HAND) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		if (!itemStackHeld.isEmpty() || entityPlayer.isSneaking()) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityShipController)) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		if (!world.isRemote) {
			final TileEntityShipCore tileEntityShipCore = ((TileEntityShipController) tileEntity).getLinkedShipCoreRefresh();
			if (tileEntityShipCore == null) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.no_core"));
			} else if (!tileEntityShipCore.isCrewMember((EntityPlayerMP) entityPlayer)) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.denied"));
			} else {
				PacketHandler.sendShipNavigationMapPacket((EntityPlayerMP) entityPlayer, ShipNavigationHelper.buildStaticMapSnapshot(), true);
				final NBTTagCompound tagCompound = ShipNavigationHelper.buildSnapshot((EntityPlayerMP) entityPlayer, tileEntityShipCore, blockPos, "");
				PacketHandler.sendShipNavigationPacket((EntityPlayerMP) entityPlayer, tagCompound);
			}
		}
		return true;
	}
}

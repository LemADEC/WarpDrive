package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class BlockShipCore extends BlockAbstractContainer {
	
	public BlockShipCore(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.movement.ship_core." + enumTier.getName());
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false)
				                .withProperty(BlockProperties.FACING_HORIZONTAL, EnumFacing.NORTH)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE, BlockProperties.FACING_HORIZONTAL);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(BlockProperties.ACTIVE, (metadata & 0x8) != 0)
				       .withProperty(BlockProperties.FACING_HORIZONTAL, EnumFacing.byIndex(Commons.clamp(2, 5, metadata & 0x7)));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return (blockState.getValue(BlockProperties.ACTIVE) ? 0x8 : 0x0)
		     | (blockState.getValue(BlockProperties.FACING_HORIZONTAL).getIndex());
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityShipCore();
	}
	
	@Override
	public void getDrops(@Nonnull final NonNullList<ItemStack> itemStacks, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos,
	                     @Nonnull final IBlockState blockState, final int fortune) {
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityShipCore) {
			if (((TileEntityShipCore) tileEntity).jumpCount == 0) {
				super.getDrops(itemStacks, blockAccess, blockPos, blockState, fortune);
				return;
			}
		}
		if (blockAccess instanceof WorldServer) {
			final WorldServer worldServer = (WorldServer) blockAccess;
			final EntityPlayer entityPlayer = CommonProxy.getFakePlayer(null, worldServer, blockPos);
			// trigger explosion
			final EntityTNTPrimed entityTNTPrimed = new EntityTNTPrimed(worldServer,
				blockPos.getX() + 0.5F, blockPos.getY() + 0.5F, blockPos.getZ() + 0.5F, entityPlayer);
			entityTNTPrimed.setFuse(10 + worldServer.rand.nextInt(10));
			worldServer.spawnEntity(entityTNTPrimed);
			
			// get a chance to get the drops
			itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1));
			if (fortune > 0 && worldServer.rand.nextBoolean()) {
				itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1));
			}
			if (fortune > 1 && worldServer.rand.nextBoolean()) {
				itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 1));
			}
			if (fortune > 1 & worldServer.rand.nextBoolean()) {
				itemStacks.add(ItemComponent.getItemStackNoCache(EnumComponentType.POWER_INTERFACE, 1));
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public float getPlayerRelativeBlockHardness(@Nonnull final IBlockState blockState, @Nonnull final EntityPlayer entityPlayer,
	                                            @Nonnull final World world, @Nonnull final BlockPos blockPos) {
		boolean willBreak = true;
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityShipCore) {
			if (((TileEntityShipCore)tileEntity).jumpCount == 0) {
				willBreak = false;
			}
		}
		return (willBreak ? 0.02F : 1.0F) * super.getPlayerRelativeBlockHardness(blockState, entityPlayer, world, blockPos);
	}
	
	@Override
	public boolean onBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (enumHand != EnumHand.MAIN_HAND) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityShipCore)) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		final TileEntityShipCore tileEntityShipCore = (TileEntityShipCore) tileEntity;
		
		if (itemStackHeld.isEmpty()) {
			if ( world.isRemote
			  && entityPlayer.isSneaking() ) {
				tileEntityShipCore.showBoundingBox = !tileEntityShipCore.showBoundingBox;
				if (tileEntityShipCore.showBoundingBox) {
					world.playSound(null, blockPos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4.0F, 2.0F);
				} else {
					world.playSound(null, blockPos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4.0F, 1.4F);
				}
				Commons.addChatMessage(entityPlayer, tileEntityShipCore.getBoundingBoxStatus());
				return true;
				
			} else if ( !world.isRemote
			         && !entityPlayer.isSneaking() ) {
				if (!tileEntityShipCore.isCrewMember((EntityPlayerMP) entityPlayer)) {
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.navigation.denied"));
					return true;
				}
				PacketHandler.sendShipNavigationMapPacket((EntityPlayerMP) entityPlayer, ShipNavigationHelper.buildStaticMapSnapshot(), true);
				final NBTTagCompound tagCompound = ShipNavigationHelper.buildSnapshot((EntityPlayerMP) entityPlayer, tileEntityShipCore, blockPos, "");
				PacketHandler.sendShipNavigationPacket((EntityPlayerMP) entityPlayer, tagCompound);
				return true;
			}
		}
		
		return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
	}
	
	@Override
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		Commons.addTooltip(list, new TextComponentTranslation("tile.warpdrive.movement.ship_core.tooltip.constrains",
		                                                      new WarpDriveText(Commons.getStyleValue(), WarpDriveConfig.SHIP_SIZE_MAX_PER_SIDE_BY_TIER[enumTier.getIndex()]),
		                                                      new WarpDriveText(Commons.getStyleValue(), WarpDriveConfig.SHIP_MASS_MIN_BY_TIER[enumTier.getIndex()]),
		                                                      new WarpDriveText(Commons.getStyleValue(), WarpDriveConfig.SHIP_MASS_MAX_BY_TIER[enumTier.getIndex()]),
		                                                      new WarpDriveText(Commons.getStyleValue(), WarpDriveConfig.SHIP_MASS_MAX_ON_PLANET_SURFACE),
		                                                      new WarpDriveText(Commons.getStyleValue(), WarpDriveConfig.SHIP_MASS_MIN_FOR_HYPERSPACE) ).getFormattedText());
	}
}

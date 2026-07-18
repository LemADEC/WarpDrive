package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IBlockUpdateDetector;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumHorizontalSpinning;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.BlockProperties;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.block.BlockRenderType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

import net.minecraft.block.ContainerBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ConcurrentModificationException;

public abstract class BlockAbstractContainer extends ContainerBlock implements IBlockBase {
	
	private static long timeUpdated = -1L;
	private static DimensionType dimensionTypeUpdated = null;
	private static int xUpdated = Integer.MAX_VALUE;
	private static int yUpdated = Integer.MAX_VALUE;
	private static int zUpdated = Integer.MAX_VALUE;
	
	protected EnumTier enumTier;
	protected boolean ignoreFacingOnPlacement = false;
	
	@Nonnull
	protected static Block.Properties getDefaultProperties(@Nonnull final Material material, @Nonnull final MaterialColor materialColor) {
		return Block.Properties.create(material, materialColor)
		                       .hardnessAndResistance(5.0F, 6.0F)
		                       .sound(SoundType.METAL);
	}
	
	@Nonnull
	protected static Block.Properties getDefaultProperties(@Nullable final Material material) {
		final Material materialToUse = material == null ? Material.IRON : material;
		return getDefaultProperties(materialToUse, materialToUse.getColor());
	}
	
	protected BlockAbstractContainer(@Nonnull final Block.Properties blockProperties, @Nonnull final String registryName, @Nonnull final EnumTier enumTier) {
		super(blockProperties);
		
		this.enumTier = enumTier;
		
		setRegistryName(registryName);
		WarpDrive.register(this);
	}
	
	@Nullable
	@Override
	public BlockItem createItemBlock() {
		return new ItemBlockAbstractBase(this);
	}
	
	@Nullable
	@Override
	public TileEntity createNewTileEntity(@Nonnull final IBlockReader blockReader) {
		return TileEntityAbstractBase.createNewTileEntity(this);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public BlockRenderType getRenderType(@Nonnull final BlockState blockState) {
		return BlockRenderType.MODEL;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onBlockAdded(@Nonnull final BlockState blockStateNew, @Nonnull final World world, @Nonnull final BlockPos blockPos,
	                         @Nonnull final BlockState blockStateOld, final boolean isMoving) {
		super.onBlockAdded(blockStateNew, world, blockPos, blockStateOld, isMoving);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).onBlockUpdateDetected(blockPos);
		}
	}
	
	@Nullable
	@Override
	public BlockState getStateForPlacement(@Nonnull final BlockItemUseContext blockItemUseContext) {
		final BlockState blockState = super.getStateForPlacement(blockItemUseContext);
		if (blockState == null) {
			return null;
		}
		
		if (!ignoreFacingOnPlacement) {
			// start with the most complex: spinning down/up & rotating otherwise
			if (blockState.getProperties().contains(BlockProperties.HORIZONTAL_SPINNING)) {
				final Direction enumFacing;
				final Direction enumSpinning;
				if (blockState.isOpaqueCube(blockItemUseContext.getWorld(), blockItemUseContext.getPos())) {
					enumFacing = Commons.getFacingFromEntity(blockItemUseContext.getPlayer());
					enumSpinning = Commons.getHorizontalDirectionFromEntity(blockItemUseContext.getPlayer()).getOpposite();
				} else {
					enumFacing = blockItemUseContext.getFace();
					enumSpinning = Commons.getHorizontalDirectionFromEntity(blockItemUseContext.getPlayer());
				}
				final EnumHorizontalSpinning enumHorizontalSpinning = EnumHorizontalSpinning.get(enumFacing, enumSpinning);
				return blockState.with(BlockProperties.HORIZONTAL_SPINNING, enumHorizontalSpinning);
			}
			
			// then 6 sided rotating
			if (blockState.getProperties().contains(BlockProperties.FACING)) {
				if (blockState.isOpaqueCube(blockItemUseContext.getWorld(), blockItemUseContext.getPos())) {
					final Direction enumFacing = Commons.getFacingFromEntity(blockItemUseContext.getPlayer());
					return blockState.with(BlockProperties.FACING, enumFacing);
				} else {
					return blockState.with(BlockProperties.FACING, blockItemUseContext.getFace());
				}
			}
			
			// finally, only rotating horizontally
			if (blockState.getProperties().contains(BlockProperties.FACING_HORIZONTAL)) {
				final Direction enumFacing = Commons.getHorizontalDirectionFromEntity(blockItemUseContext.getPlayer());
				if (blockState.isOpaqueCube(blockItemUseContext.getWorld(), blockItemUseContext.getPos())) {
					return blockState.with(BlockProperties.FACING_HORIZONTAL, enumFacing.getOpposite());
				} else {
					return blockState.with(BlockProperties.FACING_HORIZONTAL, enumFacing);
				}
			}
		}
		return blockState;
	}
	
	@Override
	public void onBlockPlacedBy(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final BlockState blockState,
	                            @Nullable final LivingEntity entityLivingBase, @Nonnull final ItemStack itemStack) {
		super.onBlockPlacedBy(world, blockPos, blockState, entityLivingBase, itemStack);
		
		// set inherited properties
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		assert tileEntity instanceof TileEntityAbstractBase;
		if (itemStack.getTag() != null) {
			final CompoundNBT tagCompound = itemStack.getTag().copy();
			tagCompound.putInt("x", blockPos.getX());
			tagCompound.putInt("y", blockPos.getY());
			tagCompound.putInt("z", blockPos.getZ());
			tileEntity.read(tagCompound);
			tileEntity.markDirty();
			world.notifyBlockUpdate(blockPos, blockState, blockState, 3);
		}
	}
	
	/* TODO MC1.15 save TileEntity NBT when broken
	// willHarvest was true during the call to removedPlayer so TileEntity is still there when drops will be computed hereafter
	@Override
	public void getDrops(@Nonnull final NonNullList<ItemStack> drops,
	                     @Nonnull final IWorldReader worldReader, @Nonnull final BlockPos blockPos, @Nonnull final BlockState blockState,
	                     final int fortune) {
		final TileEntity tileEntity = worldReader.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityAbstractBase)) {
			WarpDrive.logger.error(String.format("Missing tile entity for %s %s, reverting to vanilla getDrops logic",
			                                     this, Commons.format(worldReader, blockPos)));
			super.getDrops(drops, worldReader, blockPos, blockState, fortune);
			return;
		}
		
		final Random rand = worldReader instanceof World ? ((World) worldReader).rand : RANDOM;
		final int count = quantityDropped(blockState, fortune, rand);
		for (int i = 0; i < count; i++) {
			final Item item = this.getItemDropped(blockState, rand, fortune);
			if (item != Items.AIR) {
				final ItemStack itemStack = new ItemStack(item, 1, damageDropped(blockState));
				final CompoundNBT tagCompound = new CompoundNBT();
				((TileEntityAbstractBase) tileEntity).writeItemDropNBT(tagCompound);
				if (!tagCompound.isEmpty()) {
					itemStack.setTag(tagCompound);
				}
				drops.add(itemStack);
			}
		}
	}
	*/
	
	@Override
	public void onReplaced(@Nonnull final BlockState blockStateOld, @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final BlockState blockStateNew, final boolean isMoving) {
		// Filter to only keep block changes
		if (blockStateOld.getBlock() != blockStateNew.getBlock()) {
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			if (tileEntity instanceof TileEntityAbstractBase) {
				((TileEntityAbstractBase) tileEntity).onBlockBroken(blockStateOld, world, blockPos, blockStateNew);
			}
		}
		super.onReplaced(blockStateOld, world, blockPos, blockStateNew, isMoving);
	}
	
	@Nonnull
	@Override
	public ItemStack getPickBlock(@Nonnull final BlockState blockState, @Nullable final RayTraceResult target,
	                              @Nonnull final IBlockReader world, @Nonnull final BlockPos blockPos, @Nullable final PlayerEntity entityPlayer) {
		// note: target and entityPlayer should be Nonnull but the base game & most mods never use those parameters.
		// Consequently, this method is frequently called with nulls...
		final ItemStack itemStack = super.getPickBlock(blockState, target, world, blockPos, entityPlayer);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		final CompoundNBT tagCompound = new CompoundNBT();
		if (tileEntity instanceof TileEntityAbstractBase) {
			((TileEntityAbstractBase) tileEntity).writeItemDropNBT(tagCompound);
			if (!tagCompound.isEmpty()) {
				itemStack.setTag(tagCompound);
			}
		}
		return itemStack;
	}
	
	@Override
	public BlockState rotate(final BlockState blockState, @Nonnull final IWorld world, @Nonnull final BlockPos blockPos, @Nonnull final Rotation axis) {
		// already handled by vanilla
		final BlockState blockState_new = super.rotate(blockState, world, blockPos, axis);
		if (blockState_new != blockState) {
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			if (tileEntity instanceof TileEntityAbstractMachine) {
				((TileEntityAbstractMachine) tileEntity).markDirtyAssembly();
			}
		}
		return blockState_new;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void neighborChanged(@Nonnull final BlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos,
	                            @Nonnull final Block block, @Nonnull final BlockPos blockPosFrom, boolean isMoving) {
		super.neighborChanged(blockState, world, blockPos, block, blockPosFrom, isMoving);
		onBlockUpdateDetected(world, blockPos, blockPosFrom);
	}
	
	// Triggers on server side when placing a comparator compatible block
	// May trigger twice for the same placement action (placing a vanilla chest)
	// Triggers on server side when removing a comparator compatible block
	// Triggers on both sides when removing a TileEntity
	// (by extension, it'll trigger twice for the same placement of a TileEntity with comparator output)
	@Override
	public void onNeighborChange(@Nonnull final BlockState blockState, @Nonnull final IWorldReader worldReader, @Nonnull final BlockPos blockPos,
	                             @Nonnull final BlockPos blockPosNeighbor) {
		super.onNeighborChange(blockState, worldReader, blockPos, blockPosNeighbor);
		onBlockUpdateDetected(worldReader, blockPos, blockPosNeighbor);
	}
	
	@Override
	public void observedNeighborChange(@Nonnull final BlockState observerState, @Nonnull final World world, @Nonnull final BlockPos blockPosObserver,
	                                   @Nonnull final Block blockChanged, @Nonnull final BlockPos blockPosChanged) {
		super.observedNeighborChange(observerState, world, blockPosObserver, blockChanged, blockPosChanged);
		onBlockUpdateDetected(world, blockPosObserver, blockPosChanged);
	}
	
	// due to our redirection, this may trigger up to 6 times for the same event (for example, when placing a chest)
	protected void onBlockUpdateDetected(@Nonnull final IWorldReader worldReader, @Nonnull final BlockPos blockPos, @Nonnull final BlockPos blockPosUpdated) {
		if (!Commons.isSafeThread()) {
			if (WarpDriveConfig.LOGGING_PROFILING_THREAD_SAFETY) {
				final Block blockNeighbor = worldReader.getBlockState(blockPosUpdated).getBlock();
				final ResourceLocation registryName = blockNeighbor.getRegistryName();
				WarpDrive.logger.error(String.format("Bad multithreading detected from mod %s %s, please report to mod author",
				                                     registryName == null ? blockNeighbor : registryName.getNamespace(),
				                                     Commons.format(worldReader, blockPosUpdated)));
				new ConcurrentModificationException().printStackTrace(WarpDrive.printStreamError);
			}
			return;
		}
		
		// try reducing duplicated events
		// note: this is just a fast check, notably, this won't cover placing a block in between 2 of ours
		if (worldReader instanceof World) {
			final World world = (World) worldReader;
			if ( timeUpdated == world.getGameTime()
			  && dimensionTypeUpdated == world.getDimension().getType()
			  && xUpdated == blockPos.getX()
			  && yUpdated == blockPos.getY()
			  && zUpdated == blockPos.getZ() ) {
				return;
			}
			timeUpdated = world.getGameTime();
			dimensionTypeUpdated = world.getDimension().getType();
			xUpdated = blockPos.getX();
			yUpdated = blockPos.getY();
			zUpdated = blockPos.getZ();
		}
		
		final TileEntity tileEntity = worldReader.getTileEntity(blockPos);
		if ( tileEntity == null
		  || tileEntity.getWorld() == null
		  || tileEntity.getWorld().isRemote() ) {
			return;
		}
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).onBlockUpdateDetected(blockPosUpdated);
		}
	}
	
	public void onEMP(@Nonnull final World world, @Nonnull final BlockPos blockPos, final float efficiency) {
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityAbstractEnergy) {
			final TileEntityAbstractEnergy tileEntityAbstractEnergy = (TileEntityAbstractEnergy) tileEntity;
			if (tileEntityAbstractEnergy.energy_getMaxStorage() > 0) {
				tileEntityAbstractEnergy.energy_consume(Math.round(tileEntityAbstractEnergy.energy_getEnergyStored() * efficiency), false);
			}
		}
	}
	
	@Nonnull
	@Override
	public EnumTier getTier() {
		return enumTier;
	}
	
	@Nonnull
	@Override
	public Rarity getRarity() {
		return getTier().getRarity();
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public ActionResultType onBlockActivated(@Nonnull final BlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos,
	                                         @Nonnull final PlayerEntity entityPlayer, @Nonnull final Hand enumHand,
	                                         @Nonnull final BlockRayTraceResult blockRaytraceResult) {
		final ActionResultType result = BlockAbstractBase.onCommonBlockActivated(blockState, world, blockPos, entityPlayer, enumHand, blockRaytraceResult);
		if (result == ActionResultType.SUCCESS) {
			return result;
		}
		
		return super.onBlockActivated(blockState, world, blockPos, entityPlayer, enumHand, blockRaytraceResult);
	}
}
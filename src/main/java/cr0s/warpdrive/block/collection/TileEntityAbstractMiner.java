package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.block.TileEntityAbstractLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.FluidWrapper;
import cr0s.warpdrive.data.InventoryWrapper;
import cr0s.warpdrive.data.TankWrapper;
import cr0s.warpdrive.data.Vector3;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import net.minecraftforge.common.IPlantable;
import net.minecraft.block.IGrowable;
import net.minecraft.fluid.Fluid;
import net.minecraftforge.fluids.FluidStack;

public abstract class TileEntityAbstractMiner extends TileEntityAbstractLaser {
	
	// machine type
	protected Direction      laserOutputSide = Direction.NORTH;
	
	// machine state
	protected boolean		 enableSilktouch = false;
	protected Item           itemTool = Items.DIAMOND_PICKAXE;
	protected ItemStack      itemStackToolNoSilktouch = null;
	protected ItemStack      itemStackToolWithSilktouch = null;
	
	// pre-computation
	protected Vector3        laserOutput = null;
	
	public TileEntityAbstractMiner(@Nonnull final IBlockBase blockBase) {
		super(blockBase);
	}
	
	@Override
	protected void onFirstTick() {
		super.onFirstTick();
		laserOutput = new Vector3(this).translate(0.5D).translate(laserOutputSide, 0.5D);
	}
	
	protected void harvestBlock(@Nonnull final BlockPos blockPos, @Nonnull final BlockState blockState) {
		assert world != null;
		if (blockState.getBlock().isAir(blockState, world, blockPos)) {
			return;
		}
		
		final Fluid fluid = FluidWrapper.getFluid(blockState);
		if (fluid != null) {// (this is a fluid block)
			if ( WarpDriveConfig.MINING_LASER_PUMP_UPGRADE_HARVEST_FLUID
			  && FluidWrapper.isSourceBlock(world, blockPos, blockState) ) {// (fluid collection is enabled & it's a source block)
				final Collection<Object> connectedTanks = TankWrapper.getConnectedTanks(world, pos);
				if (!connectedTanks.isEmpty()) {// (at least 1 tank is connected)
					final FluidStack fluidStack = new FluidStack(fluid, 1000);
					
					final boolean fluidOverflowed = TankWrapper.addToTanks(world, pos, connectedTanks, fluidStack);
					if (fluidOverflowed) {
						// Assume player wants to collect the fluid, hence stop the mining in case of overflow
						setIsEnabled(false);
					}
					
					// Collect Fluid
					world.playSound( null, blockPos, fluid.getAttributes().getFillSound(), SoundCategory.BLOCKS, 0.5F,
					                 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F );
				}
			} else {
				// Evaporate fluid
				world.playSound( null, blockPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
				                 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F );
			}
			world.playSound(null, blockPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
					2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
			// remove without updating neighbours
			world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 2);
			
		} else {
			final NonNullList<ItemStack> itemStackDrops = getItemStackFromBlock(blockPos, blockState);
			
			final PlayerEntity entityPlayer = CommonProxy.getFakePlayer(null, (ServerWorld) world, blockPos);
			net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(itemStackDrops, getWorld(), blockPos, blockState,
			                                                               0, 1.0f, true, entityPlayer);
			
			// standard harvest block effect
			world.playEvent(2001, blockPos, Block.getStateId(blockState));
			
			// remove while updating neighbours
			world.removeBlock(blockPos, false); // setBlockState(blockPos, Blocks.AIR.getDefaultState(), 3);
			
			// try to replant the crop
			if ( itemStackDrops != null
			  && blockState.getBlock() instanceof IGrowable ) {
				for (final ItemStack itemStackPlant : itemStackDrops) {
					if (itemStackPlant.getItem() instanceof IPlantable) {
						final IPlantable plantable = (IPlantable) itemStackPlant.getItem();
						final BlockState blockStatePlant = plantable.getPlant(world, blockPos);
						if (WarpDriveConfig.LOGGING_COLLECTION) {
							WarpDrive.logger.info(String.format("Drop includes %s which is plantable %s as block %s",
							                                    itemStackPlant, plantable, blockStatePlant));
						}
						final BlockPos blockPosSoil = blockPos.down();
						final BlockState blockStateSoil = getWorld().getBlockState(blockPosSoil);
						if (!blockStateSoil.getBlock().canSustainPlant(blockStateSoil, world, blockPosSoil, Direction.UP, plantable)) {
							continue;
						}
						
						if (!blockStatePlant.isValidPosition(world, blockPos)) {
							continue;
						}
						
						// (we're sticking to harvesting effects)
						world.setBlockState(blockPos, blockStatePlant, 3);
						
						// refresh the drops
						itemStackDrops.remove(itemStackPlant);
						itemStackPlant.shrink(1);
						if (!itemStackPlant.isEmpty()) {
							itemStackDrops.add(itemStackPlant);
						}
						break;
					}
				}
			}
			
			if (InventoryWrapper.addToConnectedInventories(world, pos, itemStackDrops)) {
				setIsEnabled(false);
			}
		}
	}
	
	@Nullable
	private NonNullList<ItemStack> getItemStackFromBlock(final BlockPos blockPos, final BlockState blockState) {
		assert world != null;
		if (blockState == null) {
			WarpDrive.logger.error(String.format("%s Invalid block %s",
			                                     this, Commons.format(world, blockPos)));
			return null;
		}
		final NonNullList<ItemStack> itemStackDrops = NonNullList.create();
		try {
			if (itemStackToolNoSilktouch == null) {
				itemStackToolNoSilktouch = new ItemStack(itemTool, 1);
				itemStackToolNoSilktouch.addEnchantment(Enchantments.UNBREAKING, 1000);
				itemStackToolWithSilktouch = new ItemStack(itemTool, 1);
				itemStackToolWithSilktouch.addEnchantment(Enchantments.UNBREAKING, 1000);
				itemStackToolWithSilktouch.addEnchantment(Enchantments.SILK_TOUCH, 1);
			}
			final ItemStack itemStackTool = enableSilktouch ? itemStackToolWithSilktouch : itemStackToolNoSilktouch;
			
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			
			itemStackDrops.addAll(Block.getDrops(blockState, (ServerWorld) world, blockPos, tileEntity, null, itemStackTool));
		} catch (final Exception exception) {// protect in case the mined block is corrupted
			exception.printStackTrace(WarpDrive.printStreamError);
			return null;
		}
		
		return itemStackDrops;
	}
	
	// NBT DATA
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		enableSilktouch = tagCompound.getBoolean("enableSilktouch");
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		tagCompound.putBoolean("enableSilktouch", enableSilktouch);
		return tagCompound;
	}
}

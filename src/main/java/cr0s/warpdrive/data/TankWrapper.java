package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// this is almost a copy of InventoryWrapper, but for fluids.

public class TankWrapper {
	
	// WarpDrive methods
	public static boolean isTank(final TileEntity tileEntity, final Direction facing) {
		boolean isTank = false;
		
		if (tileEntity instanceof IFluidTank) {
			isTank = true;
		}
		
		if ( !isTank
		   && tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing).isPresent() ) {
			isTank = true;
		}
		
		return isTank;
	}
	
	public static Object getTank(final TileEntity tileEntity, final Direction facing) {
		if (tileEntity instanceof IFluidTank) {
			return tileEntity;
		}
		
		if (tileEntity != null) {
			return tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing).orElse(null);
		}
		
		return null;
	}
	
	public static @Nonnull Collection<Object> getConnectedTanks(final World world, final BlockPos blockPos) {
		final Collection<Object> result = new ArrayList<>(6);
		final Collection<IFluidHandler> resultCapabilities = new ArrayList<>(6);
		final Mutable mutableBlockPos = new Mutable();
		
		for (final Direction side : Direction.values()) {
			mutableBlockPos.setPos(blockPos.getX() + side.getXOffset(),
			blockPos.getY() + side.getYOffset(),
			blockPos.getZ() + side.getZOffset());
			final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
			
			if (tileEntity instanceof IFluidTank) {
				result.add(tileEntity);
			} else if (tileEntity != null) {
				final IFluidHandler fluidHandler = tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).orElse(null);
				if (fluidHandler != null) {
					resultCapabilities.add(fluidHandler);
				}
			}
		}
		
		result.addAll(resultCapabilities);
		return result;
	}
	
	public static boolean addToConnectedTanks(final World world, final BlockPos blockPos, final FluidStack fluidStack) {
		final List<FluidStack> fluidStacks = new ArrayList<>(1);
		fluidStacks.add(fluidStack);
		return addToConnectedTanks(world, blockPos, fluidStacks);
	}
	
	public static boolean addToConnectedTanks(final World world, final BlockPos blockPos, final List<FluidStack> fluidStacks) {
		final Collection<Object> inventories = getConnectedTanks(world, blockPos);
		return addToTanks(world, blockPos, inventories, fluidStacks);
	}
	
	public static boolean addToTanks(final World world, final BlockPos blockPos,
	                                 final Collection<Object> tanks, final FluidStack fluidStack) {
		final List<FluidStack> fluidStacks = new ArrayList<>(1);
		fluidStacks.add(fluidStack);
		
		return addToTanks(world, blockPos, tanks, fluidStacks);
	}
	public static boolean addToTanks(final World world, final BlockPos blockPos,
	                                 final Collection<Object> tanks, final List<FluidStack> fluidStacks) {
		boolean overflow = false;
		if (fluidStacks != null) {
			for (final FluidStack fluidStack : fluidStacks) {
				int qtyFilled = 0;
				for (final Object tank : tanks) {
					if (tank instanceof IFluidTank) {
						qtyFilled = ((IFluidTank) tank).fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
					} else if (tank instanceof IFluidHandler) {
						qtyFilled = ((IFluidHandler) tank).fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
					} else {
						if (Commons.throttleMe("addToTanks")){
							WarpDrive.logger.error(String.format("Invalid fluid tank type %s of class %s at %s, please report to mod author",
							                                     tank, tank.getClass(), Commons.format(world, blockPos) ));
							break;
						}
					}
					if (fluidStack.getAmount() > qtyFilled) {
						fluidStack.setAmount(fluidStack.getAmount() - qtyFilled);
					} else {
						break;
					}
				}
				if (fluidStack.getAmount() > qtyFilled) {
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.info(String.format("Tank overflow detected at %s",
						                                    Commons.format(world, blockPos)));
					}
					overflow = true;
				}
			}
		}
		return overflow;
	}
	
}

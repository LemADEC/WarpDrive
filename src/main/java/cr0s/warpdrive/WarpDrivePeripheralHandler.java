package cr0s.warpdrive;

import cr0s.warpdrive.block.TileEntityAbstractInterfaced;
import cr0s.warpdrive.config.WarpDriveConfig;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;

import javax.annotation.Nonnull;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import net.minecraftforge.common.util.LazyOptional;

public class WarpDrivePeripheralHandler implements IPeripheralProvider {
	
	public void register() {
		ComputerCraftAPI.registerPeripheralProvider(this);
	}
	
	@Nonnull
	@Override
	public LazyOptional<IPeripheral> getPeripheral(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final Direction side) {
		// ensure we only cover our own blocks
		final TileEntity tileEntity = world.getTileEntity(new BlockPos(blockPos));
		if (tileEntity instanceof TileEntityAbstractInterfaced) {
			if (WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.info(String.format("[CC] IPeripheralProvider.getPeripheral %s %s %s",
				                                    Commons.format(world, blockPos), side, tileEntity ));
			}
			// return the tile entity's IDynamicPeripheral, NOT (IPeripheral) tileEntity: in 1.15 the TE no longer
			// implements IPeripheral (the peripheral is a separate object), so the old cast threw ClassCastException.
			return ((TileEntityAbstractInterfaced) tileEntity).CC_getPeripheral();
		}
		return LazyOptional.empty();
	}
}
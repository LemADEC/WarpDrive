package cr0s.warpdrive;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;

public class WarpDrivePeripheralHandler implements IPeripheralProvider {
	@Override
	public IPeripheral getPeripheral(World world, int x, int y, int z, int side) {
//		WarpDrive.debugPrint("Checking Peripheral at " + x + ", " + y + ", " + z);
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof IPeripheral && ((IPeripheral) te).getType() != null) {
			return (IPeripheral)te;
		}
		return null;
	}
}
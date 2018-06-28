package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.block.TileEntityAbstractInterfaced;

import javax.annotation.Nonnull;
import java.util.Collections;

import net.minecraft.nbt.NBTTagCompound;

public class TileEntityWeaponController extends TileEntityAbstractInterfaced {
	
	public TileEntityWeaponController() {
		super();
		
		peripheralName = "warpdriveWeaponController";
		addMethods(new String[] {
		});
		CC_scripts = Collections.singletonList("startup");
	}
	
	@Override
	public void update() {
		super.update();
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(final NBTTagCompound tagCompound) {
		return super.writeToNBT(tagCompound);
	}
}